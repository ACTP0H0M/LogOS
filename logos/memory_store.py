from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional, Tuple
import json

from .knowledge import KnowledgeBase


@dataclass
class EpisodicEvent:
    id: int
    timestamp: str
    text: str
    link_ids: List[int] = field(default_factory=list)
    new_symbol_ids: List[int] = field(default_factory=list)
    parse: Optional[Dict[str, object]] = None


@dataclass
class EpisodicMemory:
    events: List[EpisodicEvent] = field(default_factory=list)
    _next_event_id: int = 0

    def add_event(
        self,
        text: str,
        link_ids: List[int],
        new_symbol_ids: List[int],
        parse: Optional[Dict[str, object]] = None,
    ) -> EpisodicEvent:
        event = EpisodicEvent(
            id=self._next_event_id,
            timestamp=datetime.utcnow().isoformat(timespec="seconds") + "Z",
            text=text,
            link_ids=list(link_ids),
            new_symbol_ids=list(new_symbol_ids),
            parse=parse,
        )
        self._next_event_id += 1
        self.events.append(event)
        return event

    def to_dict(self) -> Dict[str, object]:
        return {
            "events": [
                {
                    "id": ev.id,
                    "timestamp": ev.timestamp,
                    "text": ev.text,
                    "link_ids": list(ev.link_ids),
                    "new_symbol_ids": list(ev.new_symbol_ids),
                    "parse": ev.parse,
                }
                for ev in self.events
            ],
            "metadata": {"next_event_id": self._next_event_id},
        }

    @classmethod
    def from_dict(cls, payload: Dict[str, object]) -> "EpisodicMemory":
        mem = cls()
        mem.events = [
            EpisodicEvent(
                id=ev["id"],
                timestamp=ev.get("timestamp", ""),
                text=ev.get("text", ""),
                link_ids=list(ev.get("link_ids", [])),
                new_symbol_ids=list(ev.get("new_symbol_ids", [])),
                parse=ev.get("parse"),
            )
            for ev in payload.get("events", [])
        ]
        mem._next_event_id = payload.get("metadata", {}).get("next_event_id", len(mem.events))
        return mem


@dataclass
class ProceduralRule:
    id: int
    name: str
    conditions: List[Tuple[str, str, str]] = field(default_factory=list)
    conclusion: Optional[Tuple[str, str, str]] = None
    confidence: float = 1.0


@dataclass
class ProceduralMemory:
    rules: List[ProceduralRule] = field(default_factory=list)
    _next_rule_id: int = 0

    def add_rule(
        self,
        name: str,
        conditions: List[Tuple[str, str, str]],
        conclusion: Optional[Tuple[str, str, str]],
        confidence: float = 1.0,
    ) -> ProceduralRule:
        rule = ProceduralRule(
            id=self._next_rule_id,
            name=name,
            conditions=list(conditions),
            conclusion=conclusion,
            confidence=confidence,
        )
        self._next_rule_id += 1
        self.rules.append(rule)
        return rule

    def to_dict(self) -> Dict[str, object]:
        return {
            "rules": [
                {
                    "id": rule.id,
                    "name": rule.name,
                    "conditions": [list(cond) for cond in rule.conditions],
                    "conclusion": list(rule.conclusion) if rule.conclusion else None,
                    "confidence": rule.confidence,
                }
                for rule in self.rules
            ],
            "metadata": {"next_rule_id": self._next_rule_id},
        }

    @classmethod
    def from_dict(cls, payload: Dict[str, object]) -> "ProceduralMemory":
        mem = cls()
        mem.rules = [
            ProceduralRule(
                id=rule["id"],
                name=rule.get("name", ""),
                conditions=[tuple(cond) for cond in rule.get("conditions", [])],
                conclusion=tuple(rule["conclusion"]) if rule.get("conclusion") else None,
                confidence=rule.get("confidence", 1.0),
            )
            for rule in payload.get("rules", [])
        ]
        mem._next_rule_id = payload.get("metadata", {}).get("next_rule_id", len(mem.rules))
        return mem


class MemoryStore:
    def __init__(self, base_dir: Path, legacy_dir: Optional[Path] = None) -> None:
        self.base_dir = base_dir
        self.legacy_dir = legacy_dir or base_dir.parent
        self.long_term_path = base_dir / "long_term.json"
        self.episodic_path = base_dir / "episodic.json"
        self.procedural_path = base_dir / "procedural.json"
        self.base_dir.mkdir(parents=True, exist_ok=True)

    def load(self) -> Tuple[KnowledgeBase, EpisodicMemory, ProceduralMemory]:
        if self.long_term_path.exists():
            knowledge = KnowledgeBase.from_dict(self._read_json(self.long_term_path))
        else:
            knowledge = KnowledgeBase.from_legacy_files(self.legacy_dir)

        episodic = EpisodicMemory()
        if self.episodic_path.exists():
            episodic = EpisodicMemory.from_dict(self._read_json(self.episodic_path))

        procedural = ProceduralMemory()
        if self.procedural_path.exists():
            procedural = ProceduralMemory.from_dict(self._read_json(self.procedural_path))

        return knowledge, episodic, procedural

    def save(
        self,
        knowledge: KnowledgeBase,
        episodic: EpisodicMemory,
        procedural: ProceduralMemory,
    ) -> None:
        self._write_json(self.long_term_path, knowledge.to_dict())
        self._write_json(self.episodic_path, episodic.to_dict())
        self._write_json(self.procedural_path, procedural.to_dict())

    def _read_json(self, path: Path) -> Dict[str, object]:
        return json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}

    def _write_json(self, path: Path, payload: Dict[str, object]) -> None:
        path.write_text(json.dumps(payload, indent=2, ensure_ascii=True), encoding="utf-8")
