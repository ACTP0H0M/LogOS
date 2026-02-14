from __future__ import annotations

from typing import List, Optional, Tuple

from .knowledge import KnowledgeBase, Link, Symbol
from .memory_store import EpisodicMemory, ProceduralMemory
from .state import ConversationState, Clarification


class Reasoner:
    """Symbolic response generation over ontology + memory."""

    def __init__(
        self,
        knowledge: KnowledgeBase,
        episodic: EpisodicMemory,
        procedural: ProceduralMemory,
        state: ConversationState,
    ) -> None:
        self._knowledge = knowledge
        self._episodic = episodic
        self._procedural = procedural
        self._state = state

    def respond_to_query(self, subject: str, relation: Optional[str] = None, verb: Optional[str] = None) -> str:
        if not subject:
            return "What would you like to know?"
        normalized_subject = self._normalize_query_subject(subject)
        if relation in {"where_loc", "where_dir"}:
            return self._describe_location_query(normalized_subject, relation)
        if relation == "who_did" and verb:
            return self._describe_actor_query(normalized_subject, verb)
        if subject == "__meta__":
            return self._describe_memory()
        owner, attribute = self._parse_possessive_query(normalized_subject)
        if owner and attribute:
            return self._describe_possessive(owner, attribute)
        symbol = self._knowledge.symbol_by_name(normalized_subject)
        if not symbol:
            return f"I do not know what {self._subject_label(normalized_subject)} is yet."
        return self._describe_symbol(symbol)

    def _describe_symbol(self, symbol: Symbol) -> str:
        outgoing = self._knowledge.links_from(symbol.id)
        if not outgoing:
            return f"I only know that {symbol.name} exists."

        buckets: List[str] = []
        parents = [self._link_target_name(lk) for lk in outgoing if lk.relation == "is_a"]
        if parents:
            unique_parents = sorted(set(parents))
            concrete_parents = [parent for parent in unique_parents if not parent.startswith("#")]
            shown_parents = concrete_parents or unique_parents
            buckets.append(f"{symbol.name} is a {', '.join(shown_parents)}")

        properties = [self._link_target_name(lk) for lk in outgoing if lk.relation == "has_property"]
        if properties:
            buckets.append(f"{symbol.name} has properties {', '.join(sorted(set(properties)))}")

        locations = [
            f"{lk.relation} {self._link_target_name(lk)}"
            for lk in outgoing
            if lk.relation.startswith("located_")
            or lk.relation in {"in", "on", "at", "under", "above", "near", "where_loc", "where_dir"}
        ]
        if locations:
            buckets.append(f"{symbol.name} is {', '.join(locations)}")

        other_links = [
            f"{lk.relation} {self._link_target_name(lk)}"
            for lk in outgoing
            if lk.relation not in {"is_a", "has_property", "where_loc", "where_dir"}
            and not lk.relation.startswith("located_")
        ]
        if other_links:
            buckets.append(f"{symbol.name} {', '.join(other_links)}")

        return " | ".join(buckets)

    def _describe_location_query(self, subject: str, relation: str) -> str:
        symbol = self._knowledge.symbol_by_name(subject)
        if not symbol:
            return f"I do not know where {subject} is yet."

        outgoing = self._knowledge.links_from(symbol.id)
        location_links = [lk for lk in outgoing if lk.relation == relation]
        if not location_links and relation == "where_loc":
            location_links = [
                lk
                for lk in outgoing
                if lk.relation in {"in", "on", "at", "under", "above", "near", "behind", "between", "inside", "outside", "over"}
            ]
        if not location_links:
            return f"I do not know where {self._subject_label(subject)} is yet."

        link = max(location_links, key=lambda lk: lk.id)
        where_phrase = self._location_phrase_from_link(link)
        if not where_phrase:
            return f"I do not know where {self._subject_label(subject)} is yet."
        return f"{self._subject_verb(subject)} {where_phrase}."

    def _location_phrase_from_link(self, link: Link) -> Optional[str]:
        if link.relation in {"in", "on", "at", "under", "above", "near", "behind", "between", "inside", "outside", "over"}:
            target = self._knowledge.node_label(link.target, wrap_branch=False)
            return f"{link.relation} {self._with_indefinite_article(target)}"

        if link.relation != "where_loc":
            return None

        branch = self._knowledge.branch_by_node_id(link.target)
        if branch and len(branch.logos) >= 2 and branch.links:
            prep_link = self._knowledge.link_by_id(branch.links[0])
            obj_label = self._knowledge.node_label(branch.logos[0], wrap_branch=False)
            prep_label = self._knowledge.node_label(branch.logos[1], wrap_branch=False)
            if prep_link and prep_link.relation == "prep":
                return f"{prep_label} {self._with_indefinite_article(obj_label)}"

        target = self._knowledge.node_label(link.target, wrap_branch=False)
        return f"in {self._with_indefinite_article(target)}"

    def _describe_actor_query(self, subject: str, verb: str) -> str:
        target_symbol = self._knowledge.symbol_by_name(subject)
        if not target_symbol:
            return f"I do not know who {verb} {self._object_label(subject)} yet."

        verb_symbol = self._knowledge.symbol_by_name(verb)
        action_links = self._knowledge.links_to(target_symbol.id)
        matching_whom = [
            lk
            for lk in action_links
            if lk.relation == "whom"
            and (
                (verb_symbol is not None and lk.source == verb_symbol.id)
                or (
                    verb_symbol is None
                    and self._knowledge.node_label(lk.source, wrap_branch=False).lower() == verb.lower()
                )
            )
        ]
        if not matching_whom:
            return f"I do not know who {verb} {self._object_label(subject)} yet."

        best_do_link = None
        for whom_link in matching_whom:
            for branch in self._knowledge.branches():
                if whom_link.id not in branch.links:
                    continue
                branch_node_id = self._knowledge.branch_node_id(branch.id)
                incoming = self._knowledge.links_to(branch_node_id)
                for do_link in incoming:
                    if do_link.relation in {"do", "did"}:
                        if best_do_link is None or do_link.id > best_do_link.id:
                            best_do_link = do_link

        if not best_do_link:
            return f"I do not know who {verb} {self._object_label(subject)} yet."

        actor = self._subject_label(self._knowledge.node_label(best_do_link.source, wrap_branch=False))
        actor_sentence = actor[0].upper() + actor[1:] if actor else actor
        verb_phrase = verb.lower()
        return f"{actor_sentence} {verb_phrase} {self._object_label(subject)}."

    def _subject_verb(self, subject: str) -> str:
        if subject == "#SELF":
            return "I am"
        if subject == "#USER":
            return "You are"
        return f"{subject} is"

    def _subject_label(self, subject: str) -> str:
        if subject == "#SELF":
            return "I"
        if subject == "#USER":
            return "you"
        return subject

    def _normalize_query_subject(self, subject: str) -> str:
        cleaned = subject.strip()
        if not cleaned or cleaned.startswith("#"):
            return cleaned
        tokens = cleaned.split()
        if len(tokens) <= 1:
            return cleaned
        first = tokens[0].lower()
        if first in {"a", "an", "the", "this", "that", "these", "those"}:
            return " ".join(tokens[1:])
        return cleaned

    def _object_label(self, obj: str) -> str:
        if obj == "#SELF":
            return "me"
        if obj == "#USER":
            return "you"
        return obj

    def _with_indefinite_article(self, text: str) -> str:
        stripped = text.strip()
        if not stripped:
            return stripped
        lowered = stripped.lower()
        if stripped.startswith("#") or lowered.startswith(("a ", "an ", "the ")):
            return stripped
        if " " in stripped:
            return stripped
        article = "an" if lowered[0] in {"a", "e", "i", "o", "u"} else "a"
        return f"{article} {stripped}"

    def _link_target_name(self, link: Link) -> str:
        return self._knowledge.node_label(link.target, wrap_branch=True)

    def _parse_possessive_query(self, subject: str) -> Tuple[Optional[str], Optional[str]]:
        lowered = subject.strip().lower()
        if lowered.startswith("#user "):
            return "#USER", subject[6:].strip()
        if lowered.startswith("#self "):
            return "#SELF", subject[6:].strip()
        return None, None

    def _describe_possessive(self, owner: str, attribute: str) -> str:
        owner_symbol = self._knowledge.symbol_by_name(owner)
        if not owner_symbol:
            return f"I do not know who {owner} is."
        attr_lower = attribute.lower()
        links = self._knowledge.links_from(owner_symbol.id)
        if attr_lower == "name":
            names = [self._link_target_name(lk) for lk in links if lk.relation == "name"]
            if names:
                return f"{self._owner_pronoun(owner)} name is {names[-1]}."
            return f"I do not know {self._owner_pronoun(owner)} name yet."
        for link in links:
            if link.relation == attribute:
                return f"{self._owner_pronoun(owner)} {attribute} is {self._link_target_name(link)}."
        return f"I do not know {self._owner_pronoun(owner)} {attribute} yet."

    def _owner_pronoun(self, owner: str) -> str:
        if owner == "#SELF":
            return "my"
        return "your"

    def _describe_memory(self) -> str:
        recent = self._knowledge.recent_links(limit=8)
        if not recent:
            return "I do not know anything yet."
        lines = [self._knowledge.link_sentence(link) for link in recent]
        joined = "; ".join(lines)
        return f"Here are some recent things I learned: {joined}"

    def pending_clarification_prompt(self, clarification: Clarification) -> str:
        if clarification.role == "kind":
            return (
                f"Is '{clarification.term}' #ENTITY, #PROPERTY, #VERB, #RELATION, #LOCATION, #TIME, or #STATE?"
            )
        if clarification.role == "lemma":
            return (
                f"What is the base form of '{clarification.term}'? "
                "Use underscores for multi-word verbs (e.g., give_birth)."
            )
        if clarification.role == "tense":
            return f"Is '{clarification.term}' in the present tense or past tense?"
        if clarification.role == "transitive":
            return f"Is '{clarification.term}' transitive (takes an object) or intransitive?"
        base = f"I don't know what '{clarification.term}' means yet or I haven't encountered this word yet."
        context = f" You used it in: {clarification.context}."
        hint = " Can you provide the base form of this word, define it (e.g., \"X is a kind of Y\"), or give a short example?"
        return base + context + hint

    def small_talk(self) -> str:
        if self._state.user_name:
            return (
                f"That sounds interesting, {self._state.user_name}. "
                "I am still learning, so could you rephrase that as a simple fact or question?"
            )
        return (
            "That sounds interesting. I am still learning, so please tell me a simple fact, "
            "ask a question, or define a term."
        )
