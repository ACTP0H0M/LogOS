from __future__ import annotations

from typing import List, Tuple

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

    def respond_to_query(self, subject: str) -> str:
        if not subject:
            return "What would you like to know?"
        if subject == "__meta__":
            return self._describe_memory()
        owner, attribute = self._parse_possessive_query(subject)
        if owner and attribute:
            return self._describe_possessive(owner, attribute)
        symbol = self._knowledge.symbol_by_name(subject)
        if not symbol:
            return f"I do not know what {subject} is yet."
        return self._describe_symbol(symbol)

    def _describe_symbol(self, symbol: Symbol) -> str:
        outgoing = self._knowledge.links_from(symbol.id)
        if not outgoing:
            return f"I only know that {symbol.name} exists."

        buckets: List[str] = []
        parents = [self._link_target_name(lk) for lk in outgoing if lk.relation == "is_a"]
        if parents:
            buckets.append(f"{symbol.name} is a {', '.join(sorted(set(parents)))}")

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

    def _link_target_name(self, link: Link) -> str:
        target = self._knowledge.symbol_by_id(link.target)
        return target.name if target else "something"

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
        base = f"I don't know what '{clarification.term}' means yet."
        context = f" You used it in: {clarification.context}."
        hint = " Can you define it (e.g., \"X is a kind of Y\"), or give a short example?"
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
