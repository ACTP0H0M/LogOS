from __future__ import annotations

from dataclasses import dataclass, field
from typing import Dict, Iterable, List, Optional, Sequence, Tuple

from .knowledge import Branch, KnowledgeBase, Link, Symbol
from .memory_store import EpisodicMemory, ProceduralMemory
from .state import ConversationState, Clarification


@dataclass
class Problem:
    type: str
    severity: float
    internal: bool = True
    link_ids: List[int] = field(default_factory=list)
    node_ids: List[int] = field(default_factory=list)
    solved: bool = False


@dataclass
class Thought:
    text: str = ""
    priority: float = 0.0
    source_problem: Optional[Problem] = None
    solution_link_ids: List[int] = field(default_factory=list)
    solution_node_ids: List[int] = field(default_factory=list)


class Reasoner:
    """Symbolic response generation over ontology + memory."""

    _PROBLEM_SEVERITY: Dict[str, float] = {
        "CONTRADICTION": 0.9,
        "MISSING_RESOURCE": 0.8,
        "UNKNOWN_REASON": 0.5,
        "UNKNOWN_PURPOSE": 0.4,
        "NO_INHERITANCE": 0.7,
        "NO_DESCRIPTIONS": 0.4,
        "UNKNOWN_ACTION_REQUIREMENTS": 0.9,
        "UNKNOWN_PLACE": 0.8,
        "UNKNOWN_TIME_FUTURE": 0.5,
        "UNKNOWN_TIME_PAST": 0.5,
        "UNKNOWN_METHOD": 0.8,
        "UNKNOWN_PROPERTY": 0.6,
        "UNKNOWN_OBJECT": 0.7,
        "UNKNOWN_ACTION": 0.7,
        "UNKNOWN_SUBJECT": 0.7,
        "PHILOSOPHICAL_QUESTION": 0.5,
        "COMMAND": 1.0,
    }
    _SPATIAL_PREPOSITIONS = {
        "in",
        "on",
        "at",
        "under",
        "above",
        "near",
        "behind",
        "between",
        "in front of",
        "opposite to",
        "out",
        "inside",
        "outside",
        "over",
    }
    _VAGUE_LOCATION_WORDS = {"nearby", "close", "far", "above", "below", "left", "right", "here", "there"}
    _UNINFORMATIVE_VERBS = {"is", "are", "was", "were", "have", "had"}
    _DIRECT_LOCATION_RELATIONS = {
        "where_loc",
        "where_dir",
        "in",
        "on",
        "at",
        "under",
        "above",
        "near",
        "behind",
        "between",
        "inside",
        "outside",
        "over",
    }
    _ONTOLOGY_META_SYMBOLS = {"#ENTITY", "#PROPERTY", "#VERB", "#RELATION", "#LOCATION", "#TIME", "#STATE"}
    _CURIOSITY_SKIP_SYMBOLS = {"#SELF", "#USER", "#NOW", "#UNKNOWN"}

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
        self._trust_in_new = 0.6
        self._used_curiosity_texts: List[str] = []

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

    def find_internal_problems(self, actuality_min: float = 0.2) -> List[Problem]:
        """Port of Java ProblemFinder internal heuristics over the current KnowledgeBase."""
        problems: List[Problem] = []
        seen: set[Tuple[str, Tuple[int, ...], Tuple[int, ...]]] = set()

        actual_nodes = self._actual_nodes(actuality_min)
        for node_id in actual_nodes:
            outlinks = self._knowledge.links_from(node_id)
            for link in outlinks:
                if self._skip_problem_link_target(link):
                    continue

                contradictions = self._contradicting_links(link, outlinks)
                if contradictions:
                    contra = contradictions[0]
                    self._add_problem(
                        problems,
                        seen,
                        Problem(
                            type="CONTRADICTION",
                            severity=self._severity("CONTRADICTION"),
                            internal=True,
                            link_ids=[link.id, contra.id],
                        ),
                    )

                if not link.evidence:
                    self._add_problem(
                        problems,
                        seen,
                        Problem(
                            type="UNKNOWN_REASON",
                            severity=self._severity("UNKNOWN_REASON"),
                            internal=True,
                            link_ids=[link.id],
                        ),
                    )

                if link.relation == "task_link" and link.generality > 0:
                    missing = self._missing_task_resources(link)
                    if missing:
                        self._add_problem(
                            problems,
                            seen,
                            Problem(
                                type="MISSING_RESOURCE",
                                severity=self._severity("MISSING_RESOURCE"),
                                internal=True,
                                link_ids=[link.id],
                                node_ids=missing,
                            ),
                        )
                    if not self._positive_links_by_name(self._knowledge.links_from(link.target), "method_link"):
                        self._add_problem(
                            problems,
                            seen,
                            Problem(
                                type="UNKNOWN_METHOD",
                                severity=self._severity("UNKNOWN_METHOD"),
                                internal=True,
                                node_ids=[link.target],
                            ),
                        )

            self._find_node_level_problems(node_id, outlinks, problems, seen)

        problems.sort(key=lambda p: (-p.severity, p.type, tuple(sorted(p.link_ids)), tuple(sorted(p.node_ids))))
        return problems

    def solve_problem(self, problem: Problem) -> Thought:
        """Port of Java ProblemSolver internal behaviors (prompting + some auto-resolution)."""
        thought = Thought(priority=problem.severity, source_problem=problem)
        if not problem.internal:
            thought.text = "I do not have a method for solving this problem yet."
            return thought

        if problem.type == "CONTRADICTION":
            self._solve_internal_contradiction(problem, thought)
            return thought

        if problem.type == "MISSING_RESOURCE":
            resource_label = self._missing_resource_label(problem)
            thought.text = f"I don't have {resource_label}."
            return thought

        if problem.type == "UNKNOWN_REASON":
            link = self._first_problem_link(problem)
            if not link:
                thought.text = "Why?"
                return thought
            if link.evidence:
                evidence_links = [self._knowledge.link_by_id(lid) for lid in link.evidence]
                evidence_links = [lk for lk in evidence_links if lk is not None]
                if evidence_links:
                    parts = [self._translate_link_for_human(lk) for lk in evidence_links[:2]]
                    if len(parts) == 1:
                        thought.text = f"Because {parts[0]}."
                    else:
                        thought.text = f"Because {parts[0]} and {parts[1]}."
                    problem.solved = True
                    thought.solution_link_ids = [lk.id for lk in evidence_links[:2]]
                    return thought
            thought.text = f"Why {self._translate_link_for_human(link)}?"
            return thought

        if problem.type == "UNKNOWN_PURPOSE":
            node = self._first_problem_node(problem)
            if node is not None:
                if self._knowledge.is_branch_node(node):
                    thought.text = f"What is the purpose of {self._node_phrase(node)}?"
                else:
                    name = self._knowledge.node_label(node, wrap_branch=False)
                    thought.text = f"What is the purpose of {name}ing?"
            return thought

        if problem.type == "NO_INHERITANCE":
            node = self._first_problem_node(problem)
            if node is not None:
                thought.text = f"What kind of thing is {self._node_phrase(node)}?"
            return thought

        if problem.type == "NO_DESCRIPTIONS":
            node = self._first_problem_node(problem)
            if node is not None:
                thought.text = f"What is {self._node_phrase(node)} like?"
            return thought

        if problem.type == "UNKNOWN_ACTION_REQUIREMENTS":
            node = self._first_problem_node(problem)
            if node is not None:
                if self._knowledge.is_branch_node(node):
                    thought.text = f"What is required for {self._node_phrase(node)}?"
                else:
                    thought.text = f"What is required to {self._knowledge.node_label(node, wrap_branch=False)}?"
            return thought

        if problem.type == "UNKNOWN_PLACE":
            node = self._first_problem_node(problem)
            if node is not None:
                thought.text = self._where_prompt_for_node(node)
            return thought

        if problem.type == "UNKNOWN_METHOD":
            node = self._first_problem_node(problem)
            if node is not None:
                thought.text = f"How can I {self._node_phrase(node)}?"
            return thought

        if problem.type == "UNKNOWN_PROPERTY":
            node = self._first_problem_node(problem)
            if node is not None:
                thought.text = f"What is {self._node_phrase(node)} like?"
            return thought

        if problem.type == "UNKNOWN_OBJECT":
            node = self._first_problem_node(problem)
            if node is not None:
                verb = self._knowledge.node_label(node, wrap_branch=False)
                thought.text = f"What is the object of {verb}?"
            return thought

        if problem.type == "UNKNOWN_ACTION":
            node = self._first_problem_node(problem)
            if node is not None:
                thought.text = f"What does {self._node_phrase(node)} do?"
            return thought

        if problem.type == "UNKNOWN_SUBJECT":
            if problem.node_ids:
                labels = [self._node_phrase(n) for n in problem.node_ids[:2]]
                thought.text = "Who " + " ".join(labels) + "?"
            return thought

        thought.text = "I have no methods for solving this problem yet."
        return thought

    def next_curiosity_thought(self, actuality_min: float = 0.2) -> Optional[Thought]:
        problems = self.find_internal_problems(actuality_min=actuality_min)
        if not problems:
            return None

        first_repeat: Optional[Thought] = None
        for problem in problems:
            thought = self.solve_problem(problem)
            if not thought.text:
                continue
            normalized = " ".join(thought.text.split())
            if normalized and normalized not in self._used_curiosity_texts:
                self._used_curiosity_texts.append(normalized)
                if len(self._used_curiosity_texts) > 64:
                    self._used_curiosity_texts.pop(0)
                return thought
            if first_repeat is None:
                first_repeat = thought
        return first_repeat

    def curiosity_prompt(self, actuality_min: float = 0.2) -> Optional[str]:
        thought = self.next_curiosity_thought(actuality_min=actuality_min)
        if not thought or not thought.text:
            return None
        return thought.text

    def _severity(self, problem_type: str) -> float:
        return self._PROBLEM_SEVERITY.get(problem_type, 0.5)

    def _problem_signature(self, problem: Problem) -> Tuple[str, Tuple[int, ...], Tuple[int, ...]]:
        return (
            problem.type,
            tuple(sorted(problem.link_ids)),
            tuple(sorted(problem.node_ids)),
        )

    def _add_problem(
        self,
        problems: List[Problem],
        seen: set[Tuple[str, Tuple[int, ...], Tuple[int, ...]]],
        problem: Problem,
    ) -> None:
        sig = self._problem_signature(problem)
        if sig in seen:
            return
        seen.add(sig)
        problems.append(problem)

    def _actual_nodes(self, actuality_min: float) -> List[int]:
        node_ids: set[int] = set()
        for link in self._knowledge.links():
            if link.actuality < actuality_min:
                continue
            node_ids.add(link.source)
            node_ids.add(link.target)
        return sorted(node_ids)

    def _positive_links(self, links: Iterable[Link]) -> List[Link]:
        return [lk for lk in links if lk.generality > 0]

    def _positive_links_by_name(self, links: Iterable[Link], relation: str) -> List[Link]:
        return [lk for lk in links if lk.relation == relation and lk.generality > 0]

    def _skip_problem_link_target(self, link: Link) -> bool:
        label = self._knowledge.node_label(link.target, wrap_branch=False)
        return label in {"#ENTITY", "#VERB", "#PROPERTY"}

    def _contradicting_links(self, link: Link, outlinks: Sequence[Link]) -> List[Link]:
        sign = link.generality > 0
        matches = []
        for other in outlinks:
            if other.id == link.id:
                continue
            if other.relation != link.relation:
                continue
            if other.target != link.target:
                continue
            if (other.generality > 0) == sign:
                continue
            matches.append(other)
        matches.sort(key=lambda lk: lk.id)
        return matches

    def _missing_task_resources(self, task_link: Link) -> List[int]:
        reqs = [
            lk.source
            for lk in self._knowledge.links_to(task_link.target)
            if lk.relation == "is_needed_to" and lk.generality > 0
        ]
        actor_has = {
            self._knowledge.node_label(lk.target, wrap_branch=False).lower()
            for lk in self._knowledge.links_from(task_link.source)
            if lk.relation == "have" and lk.generality > 0
        }
        missing = []
        for req in reqs:
            req_name = self._knowledge.node_label(req, wrap_branch=False).lower()
            if req_name not in actor_has:
                missing.append(req)
        return missing

    def _find_node_level_problems(
        self,
        node_id: int,
        outlinks: List[Link],
        problems: List[Problem],
        seen: set[Tuple[str, Tuple[int, ...], Tuple[int, ...]]],
    ) -> None:
        if self._is_meta_ontology_node(node_id) or self._is_curiosity_builtin_node(node_id):
            return
        is_branch = self._knowledge.is_branch_node(node_id)
        is_entity = self._node_has_category(node_id, "#ENTITY")
        is_verb = self._node_has_category(node_id, "#VERB")

        if is_branch or is_verb:
            has_purpose = any(lk.relation == "in_order_to" and lk.generality > 0 for lk in outlinks)
            if not has_purpose:
                self._add_problem(
                    problems,
                    seen,
                    Problem(
                        type="UNKNOWN_PURPOSE",
                        severity=self._severity("UNKNOWN_PURPOSE"),
                        internal=True,
                        node_ids=[node_id],
                    ),
                )

        if is_entity:
            positive_is_a = [
                lk
                for lk in outlinks
                if lk.relation == "is_a"
                and lk.generality > 0
                and self._knowledge.node_label(lk.target, wrap_branch=False) != "#ENTITY"
            ]
            if not positive_is_a:
                self._add_problem(
                    problems,
                    seen,
                    Problem(
                        type="NO_INHERITANCE",
                        severity=self._severity("NO_INHERITANCE"),
                        internal=True,
                        node_ids=[node_id],
                    ),
                )

            if not any(lk.relation == "is" for lk in outlinks):
                self._add_problem(
                    problems,
                    seen,
                    Problem(
                        type="NO_DESCRIPTIONS",
                        severity=self._severity("NO_DESCRIPTIONS"),
                        internal=True,
                        node_ids=[node_id],
                    ),
                )

        if is_verb:
            name = self._knowledge.node_label(node_id, wrap_branch=False).lower()
            if name not in self._UNINFORMATIVE_VERBS:
                has_req = any(lk.relation == "is_needed_to" for lk in self._knowledge.links_to(node_id))
                if not has_req:
                    self._add_problem(
                        problems,
                        seen,
                        Problem(
                            type="UNKNOWN_ACTION_REQUIREMENTS",
                            severity=self._severity("UNKNOWN_ACTION_REQUIREMENTS"),
                            internal=True,
                            node_ids=[node_id],
                        ),
                    )

        if is_branch:
            branch = self._knowledge.branch_by_node_id(node_id)
            if branch and self._branch_contains_relation(branch, {"do", "did"}, positive_only=True):
                has_req = any(lk.relation == "is_needed_to" for lk in self._knowledge.links_to(node_id))
                if not has_req:
                    self._add_problem(
                        problems,
                        seen,
                        Problem(
                            type="UNKNOWN_ACTION_REQUIREMENTS",
                            severity=self._severity("UNKNOWN_ACTION_REQUIREMENTS"),
                            internal=True,
                            node_ids=[node_id],
                        ),
                    )

        if (is_entity or is_branch) and not is_verb:
            if not self._node_has_location(node_id, outlinks, for_verb=False):
                self._add_problem(
                    problems,
                    seen,
                    Problem(
                        type="UNKNOWN_PLACE",
                        severity=self._severity("UNKNOWN_PLACE"),
                        internal=True,
                        node_ids=[node_id],
                    ),
                )

        if is_verb:
            if not self._node_has_location(node_id, outlinks, for_verb=True):
                self._add_problem(
                    problems,
                    seen,
                    Problem(
                        type="UNKNOWN_PLACE",
                        severity=self._severity("UNKNOWN_PLACE"),
                        internal=True,
                        node_ids=[node_id],
                    ),
                )

        if is_entity or is_branch:
            if not self._node_has_property(node_id, outlinks):
                self._add_problem(
                    problems,
                    seen,
                    Problem(
                        type="UNKNOWN_PROPERTY",
                        severity=self._severity("UNKNOWN_PROPERTY"),
                        internal=True,
                        node_ids=[node_id],
                    ),
                )

        if is_verb:
            has_object = any(lk.relation in {"what", "whom"} for lk in outlinks)
            if not has_object:
                self._add_problem(
                    problems,
                    seen,
                    Problem(
                        type="UNKNOWN_OBJECT",
                        severity=self._severity("UNKNOWN_OBJECT"),
                        internal=True,
                        node_ids=[node_id],
                    ),
                )

        if is_entity or is_branch:
            has_action = any(lk.relation in {"do", "did"} for lk in outlinks)
            if not has_action:
                self._add_problem(
                    problems,
                    seen,
                    Problem(
                        type="UNKNOWN_ACTION",
                        severity=self._severity("UNKNOWN_ACTION"),
                        internal=True,
                        node_ids=[node_id],
                    ),
                )

    def _node_has_category(self, node_id: int, category_name: str) -> bool:
        if self._knowledge.is_branch_node(node_id):
            return False
        symbol = self._knowledge.symbol_by_id(node_id)
        if not symbol:
            return False
        if symbol.name == category_name:
            return True
        category = self._knowledge.symbol_by_name(category_name)
        if not category:
            return False
        return any(
            lk.relation == "is_a" and lk.target == category.id
            for lk in self._knowledge.links_from(node_id)
        )

    def _is_meta_ontology_node(self, node_id: int) -> bool:
        if self._knowledge.is_branch_node(node_id):
            return False
        label = self._knowledge.node_label(node_id, wrap_branch=False)
        return label in self._ONTOLOGY_META_SYMBOLS

    def _is_curiosity_builtin_node(self, node_id: int) -> bool:
        if self._knowledge.is_branch_node(node_id):
            return False
        label = self._knowledge.node_label(node_id, wrap_branch=False)
        return label in self._CURIOSITY_SKIP_SYMBOLS

    def _branch_contains_relation(self, branch: Branch, names: set[str], positive_only: bool = False) -> bool:
        for link_id in branch.links:
            link = self._knowledge.link_by_id(link_id)
            if not link:
                continue
            if link.relation not in names:
                continue
            if positive_only and link.generality <= 0:
                continue
            return True
        return False

    def _branch_has_spatial_prep(self, branch: Branch) -> bool:
        for link_id in branch.links:
            link = self._knowledge.link_by_id(link_id)
            if not link or link.relation != "prep":
                continue
            prep = self._knowledge.node_label(link.target, wrap_branch=False).lower()
            if prep in self._SPATIAL_PREPOSITIONS:
                return True
        return False

    def _node_has_location(self, node_id: int, outlinks: List[Link], *, for_verb: bool) -> bool:
        for lk in outlinks:
            if lk.generality <= 0:
                continue
            if lk.relation in self._DIRECT_LOCATION_RELATIONS:
                return True

        relation_name = "how" if for_verb else "is"
        for lk in outlinks:
            if lk.relation != relation_name or lk.generality <= 0:
                continue
            if self._knowledge.is_branch_node(lk.target):
                branch = self._knowledge.branch_by_node_id(lk.target)
                if branch and self._branch_has_spatial_prep(branch):
                    return True
            else:
                target = self._knowledge.node_label(lk.target, wrap_branch=False).lower()
                if target in self._VAGUE_LOCATION_WORDS:
                    return True
                if self._knowledge.node_kind(lk.target) == "location":
                    return True
        return False

    def _node_has_property(self, node_id: int, outlinks: List[Link]) -> bool:
        for lk in outlinks:
            if lk.generality <= 0:
                continue
            if lk.relation == "has_property":
                return True
            if lk.relation != "is":
                continue
            if self._knowledge.node_kind(lk.target) == "property":
                return True
            if self._knowledge.is_branch_node(lk.target):
                branch = self._knowledge.branch_by_node_id(lk.target)
                if not branch:
                    continue
                for member in branch.logos:
                    if self._knowledge.node_kind(member) == "property":
                        return True
        return False

    def _first_problem_link(self, problem: Problem) -> Optional[Link]:
        for link_id in problem.link_ids:
            link = self._knowledge.link_by_id(link_id)
            if link is not None:
                return link
        return None

    def _first_problem_node(self, problem: Problem) -> Optional[int]:
        return problem.node_ids[0] if problem.node_ids else None

    def _solve_internal_contradiction(self, problem: Problem, thought: Thought) -> None:
        if len(problem.link_ids) < 2:
            return
        l1 = self._knowledge.link_by_id(problem.link_ids[0])
        l2 = self._knowledge.link_by_id(problem.link_ids[1])
        if not l1 or not l2:
            return

        keep, remove = (l1, l2)
        if (l2.actuality, l2.id) > (l1.actuality, l1.id):
            keep, remove = (l2, l1)

        keep.generality = (self._trust_in_new * keep.generality) + ((1.0 - self._trust_in_new) * remove.generality)
        keep.actuality = max(keep.actuality, remove.actuality)
        self._knowledge.remove_link(remove.id)

        problem.solved = True
        thought.solution_link_ids = [keep.id]
        thought.text = ""

    def _missing_resource_label(self, problem: Problem) -> str:
        if problem.node_ids:
            return self._knowledge.node_label(problem.node_ids[0], wrap_branch=False)
        task_link = self._first_problem_link(problem)
        if task_link:
            missing = self._missing_task_resources(task_link)
            if missing:
                return self._knowledge.node_label(missing[0], wrap_branch=False)
        return "that resource"

    def _node_phrase(self, node_id: int, with_article: bool = False) -> str:
        text = self._knowledge.node_label(node_id, wrap_branch=False)
        if text == "#SELF":
            return "I"
        if text == "#USER":
            return "you"
        if not with_article:
            return text
        return self._with_indefinite_article(text)

    def _where_prompt_for_node(self, node_id: int) -> str:
        text = self._knowledge.node_label(node_id, wrap_branch=False)
        if text == "#SELF":
            return "Where am I?"
        if text == "#USER":
            return "Where are you?"
        return f"Where is {self._node_phrase(node_id, with_article=True)}?"

    def _translate_link_for_human(self, link: Link) -> str:
        src = self._knowledge.node_label(link.source, wrap_branch=False)
        trg = self._knowledge.node_label(link.target, wrap_branch=False)

        if link.relation == "prep":
            src, trg = trg, src

        if src == "#SELF":
            src = "I"
        elif src == "#USER":
            src = "you"
        if trg == "#SELF":
            trg = "I"
        elif trg == "#USER":
            trg = "you"

        relation = self._beautify_link(link)
        if not relation:
            return f"{src} {trg}".strip()
        return f"{src}{relation}{trg}".strip()

    def _beautify_link(self, link: Link) -> str:
        plus = {
            "is_a": " is a ",
            "is_component_of": " is a part of ",
            "is": " is ",
            "can_be": " can be ",
            "can_do": " can ",
            "is_needed_to": " is necessary to ",
            "do": " ",
            "did": " ",
            "have": " have ",
            "equals_to": " is ",
            "whom": " ",
            "what": " ",
            "similar_to": " is similar to ",
            "how": " ",
            "prep": " ",
            "where_loc": " is in ",
            "where_dir": " goes to ",
            "has_property": " is ",
        }
        minus = {
            "is_a": " isn't a ",
            "is": " isn't ",
            "is_needed_to": " isn't necessary to ",
            "can_do": " can't ",
            "can_be": " can't be ",
            "have": " don't have ",
            "equals_to": " isn't ",
            "is_component_of": " isn't a part of ",
            "similar_to": " isn't similar to ",
            "do": " don't ",
            "did": " didn't ",
            "how": " not ",
            "has_property": " is not ",
        }
        # Keep Java's adjective shortcut: omit "is" before simple property-like targets.
        if link.relation == "is" and not self._knowledge.is_branch_node(link.target):
            if self._knowledge.node_kind(link.target) == "property":
                return " "
        mapping = plus if link.generality > 0 else minus
        return mapping.get(link.relation, f" {link.relation} ")

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
        curiosity = self.curiosity_prompt(actuality_min=0.2)
        if curiosity:
            return curiosity
        if self._state.user_name:
            return (
                f"That sounds interesting, {self._state.user_name}. "
                "I am still learning, so could you rephrase that as a simple fact or question?"
            )
        return (
            "That sounds interesting. I am still learning, so please tell me a simple fact, "
            "ask a question, or define a term."
        )
