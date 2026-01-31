from __future__ import annotations

from dataclasses import dataclass, field
from typing import List


@dataclass
class ConversationState:
    user_name: str | None = None
    mood: str | None = None
    last_topics: List[str] = field(default_factory=list)

    def remember_topic(self, topic: str) -> None:
        if topic and topic not in self.last_topics:
            self.last_topics.append(topic)
        if len(self.last_topics) > 5:
            self.last_topics.pop(0)
