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
            if lk.relation.startswith("located_") or lk.relation in {"in", "on", "at", "under", "above", "near"}
        ]
        if locations:
            buckets.append(f"{symbol.name} is {', '.join(locations)}")

        other_links = [
            f"{lk.relation} {self._link_target_name(lk)}"
            for lk in outgoing
            if lk.relation not in {"is_a", "has_property"}
            and not lk.relation.startswith("located_")
        ]
        if other_links:
            buckets.append(f"{symbol.name} {', '.join(other_links)}")

        return " | ".join(buckets)

    def _link_target_name(self, link: Link) -> str:
        target = self._knowledge.symbol_by_id(link.target)
        return target.name if target else "something"

    def pending_clarification_prompt(self, clarification: Clarification) -> str:
        base = f"I don't know what '{clarification.term}' is yet."
        context = f" You used it in: {clarification.context}."
        hint = " Is it a property, an entity, or an action?"
        return base + context + hint

    def small_talk(self) -> str:
        if self._state.user_name:
            return f"How is your day going, {self._state.user_name}?"
        return "Tell me a fact, ask a question, or define a term."

