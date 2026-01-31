from __future__ import annotations

from dataclasses import dataclass
from typing import Dict, List


@dataclass(frozen=True)
class Fact:
    subject: str
    relation: str
    obj: str
    confidence: float = 1.0


class Memory:
    """Stores symbolic facts with simple indexing."""

    def __init__(self) -> None:
        self._facts: List[Fact] = []
        self._by_subject: Dict[str, List[Fact]] = {}

    def add_fact(self, fact: Fact) -> None:
        self._facts.append(fact)
        key = fact.subject.lower()
        self._by_subject.setdefault(key, []).append(fact)

    def facts_about(self, subject: str) -> List[Fact]:
        return list(self._by_subject.get(subject.lower(), []))

    def all_facts(self) -> List[Fact]:
        return list(self._facts)
