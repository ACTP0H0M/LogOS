from __future__ import annotations

from dataclasses import dataclass, field
from typing import List, Optional


@dataclass
class Clarification:
    term: str
    role: str
    context: str
    link_ids: List[int] = field(default_factory=list)


@dataclass
class ConversationState:
    user_name: str | None = None
    mood: str | None = None
    last_topics: List[str] = field(default_factory=list)
    pending_clarifications: List[Clarification] = field(default_factory=list)
    last_added_links: List[int] = field(default_factory=list)
    last_utterance: str | None = None
    internal_question_pause_turns: int = 0
    suppressed_problem_type: str | None = None
    suppressed_problem_type_turns: int = 0
    last_curiosity_problem_type: str | None = None

    def remember_topic(self, topic: str) -> None:
        if topic and topic not in self.last_topics:
            self.last_topics.append(topic)
        if len(self.last_topics) > 5:
            self.last_topics.pop(0)

    def queue_clarification(self, clarification: Clarification) -> None:
        self.pending_clarifications.append(clarification)

    def next_clarification(self) -> Optional[Clarification]:
        if self.pending_clarifications:
            return self.pending_clarifications[0]
        return None

    def pop_clarification(self) -> Optional[Clarification]:
        if self.pending_clarifications:
            return self.pending_clarifications.pop(0)
        return None
