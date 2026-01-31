from __future__ import annotations

from typing import List

from .memory import Fact, Memory
from .state import ConversationState


class Reasoner:
    """Rule-based response selection over symbolic memory."""

    def __init__(self, memory: Memory, state: ConversationState) -> None:
        self._memory = memory
        self._state = state

    def respond_to_query(self, subject: str) -> str:
        facts = self._memory.facts_about(subject)
        if not facts:
            return f"I do not have any facts about {subject} yet."
        summaries = self._summarize_facts(facts)
        return f"Here is what I know about {subject}: {summaries}"

    def _summarize_facts(self, facts: List[Fact]) -> str:
        snippets = [f"{fact.relation} {fact.obj}" for fact in facts]
        return "; ".join(snippets)

    def small_talk(self) -> str:
        if self._state.user_name:
            return f"How is your day going, {self._state.user_name}?"
        return "Tell me something about yourself."
