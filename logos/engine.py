from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Optional, Tuple

from .knowledge import KnowledgeBase
from .memory_store import EpisodicMemory, MemoryStore, ProceduralMemory
from .nlp import ParsedUtterance, parse_utterance
from .reasoner import Reasoner
from .state import Clarification, ConversationState


@dataclass
class ChatResponse:
    reply: str
    facts: Dict[str, str]


class ChatEngine:
    """Coordinates parsing, memory updates, and symbolic reasoning."""

    def __init__(self) -> None:
        base_dir = Path(__file__).resolve().parent.parent
        data_dir = base_dir / "data"
        self._store = MemoryStore(data_dir, legacy_dir=base_dir)
        self._knowledge, self._episodic, self._procedural = self._store.load()
        self._state = ConversationState()
        self._reasoner = Reasoner(self._knowledge, self._episodic, self._procedural, self._state)

    def process(self, message: str) -> ChatResponse:
        reply = ""
        if self._state.pending_clarifications:
            reply = self._handle_clarification(message)
        else:
            utterance = parse_utterance(message)
            if utterance.kind == "definition":
                reply = self._learn_definition(utterance)
            elif utterance.kind == "statement":
                reply = self._learn_statement(utterance)
            elif utterance.kind == "query":
                reply = self._reasoner.respond_to_query(utterance.subject or "")
            else:
                reply = self._reasoner.small_talk()

        self._store.save(self._knowledge, self._episodic, self._procedural)
        return ChatResponse(reply=reply, facts=self._facts_snapshot())

    def _learn_statement(self, utterance: ParsedUtterance) -> str:
        subject = self._resolve_pronoun(utterance.subject)
        if not subject:
            return "I could not find a subject in that statement."

        new_symbol_ids: List[int] = []
        created_links: List[int] = []

        subject_symbol = self._knowledge.ensure_symbol(subject, kind="entity")
        if subject_symbol.id not in new_symbol_ids:
            new_symbol_ids.append(subject_symbol.id)

        unknowns = self._collect_unknowns(subject, utterance, new_symbol_ids, created_links)
        self._episodic.add_event(utterance.raw, created_links, new_symbol_ids)
        self._knowledge.decay_links(rate=0.01)

        if unknowns:
            for clarification in unknowns:
                self._state.queue_clarification(clarification)
            prompt = self._reasoner.pending_clarification_prompt(self._state.next_clarification())
            return prompt

        return self._acknowledge_statement(subject, utterance)

    def _collect_unknowns(
        self,
        subject: str,
        utterance: ParsedUtterance,
        new_symbol_ids: List[int],
        created_links: List[int],
    ) -> List[Clarification]:
        unknowns: List[Clarification] = []

        for attr in utterance.attributes:
            attr_symbol = self._knowledge.symbol_by_name(attr)
            if not attr_symbol:
                attr_symbol = self._knowledge.ensure_symbol(attr)
                new_symbol_ids.append(attr_symbol.id)
                unknowns.append(
                    Clarification(
                        term=attr,
                        role="property",
                        context=f"{subject} has_property {attr}",
                    )
                )
            link = self._knowledge.add_property(subject, attr)
            created_links.append(link.id)

        if utterance.obj:
            obj = self._resolve_pronoun(utterance.obj)
            obj_symbol = self._knowledge.symbol_by_name(obj) if obj else None
            if obj_symbol and obj_symbol.kind == "category":
                link = self._knowledge.add_is_a(subject, obj)
                created_links.append(link.id)
            elif obj_symbol and obj_symbol.kind == "property":
                link = self._knowledge.add_property(subject, obj)
                created_links.append(link.id)
            else:
                if not obj_symbol and obj:
                    obj_symbol = self._knowledge.ensure_symbol(obj)
                    new_symbol_ids.append(obj_symbol.id)
                    unknowns.append(
                        Clarification(
                            term=obj,
                            role="property",
                            context=f"{subject} has_property {obj}",
                        )
                    )
                link = self._knowledge.add_property(subject, obj)
                created_links.append(link.id)

        for relation, obj in utterance.relations:
            if not obj:
                continue
            rel = self._normalize_relation(relation)
            obj_symbol = self._knowledge.symbol_by_name(obj)
            if not obj_symbol:
                obj_symbol = self._knowledge.ensure_symbol(obj, kind="entity")
                new_symbol_ids.append(obj_symbol.id)
                unknowns.append(
                    Clarification(
                        term=obj,
                        role="entity",
                        context=f"{subject} {rel} {obj}",
                    )
                )
            link = self._knowledge.add_relation(subject, rel, obj)
            created_links.append(link.id)

        return unknowns

    def _learn_definition(self, utterance: ParsedUtterance) -> str:
        subject = self._resolve_pronoun(utterance.subject)
        obj = self._resolve_pronoun(utterance.obj)
        if not subject or not obj:
            return "Can you restate that definition?"

        parent_kind = self._infer_kind_from_parent(obj)
        if parent_kind:
            self._knowledge.set_symbol_kind(subject, parent_kind)
        self._knowledge.add_is_a(subject, obj, child_kind=parent_kind)
        self._episodic.add_event(utterance.raw, [], [])
        return f"Okay, I'll remember that {subject} is a kind of {obj}."

    def _handle_clarification(self, message: str) -> str:
        clarification = self._state.pop_clarification()
        if not clarification:
            return "Thanks. Let me know if you want to add more."

        utterance = parse_utterance(message)
        if utterance.kind == "definition" and utterance.obj:
            parent_kind = self._infer_kind_from_parent(utterance.obj)
            if parent_kind:
                self._knowledge.set_symbol_kind(clarification.term, parent_kind)
            self._knowledge.add_is_a(clarification.term, utterance.obj, child_kind=parent_kind)
            return f"Got it. {clarification.term} is a kind of {utterance.obj}."

        kind, parent = self._classify_answer(message)
        if kind:
            self._knowledge.set_symbol_kind(clarification.term, kind)
            if parent:
                self._knowledge.add_is_a(clarification.term, parent)
            return f"Thanks. I'll treat '{clarification.term}' as a {kind}."

        self._state.queue_clarification(clarification)
        return f"I still need a hint about '{clarification.term}'. Is it an entity, property, or action?"

    def _classify_answer(self, text: str) -> Tuple[Optional[str], Optional[str]]:
        lowered = text.lower()
        if "color" in lowered:
            return "property", "color"
        if "property" in lowered or "attribute" in lowered or "quality" in lowered:
            return "property", "property"
        if "place" in lowered or "location" in lowered:
            return "location", "location"
        if "action" in lowered or "verb" in lowered:
            return "action", "action"
        if "thing" in lowered or "object" in lowered or "entity" in lowered:
            return "entity", "entity"
        return None, None

    def _infer_kind_from_parent(self, parent: str) -> Optional[str]:
        lowered = parent.lower()
        if lowered in {"color", "property", "attribute"}:
            return "property"
        if lowered in {"place", "location"}:
            return "location"
        if lowered in {"action", "verb"}:
            return "action"
        if lowered in {"state", "mood"}:
            return "state"
        if lowered in {"entity", "object", "thing"}:
            return "entity"
        return None

    def _normalize_relation(self, relation: str) -> str:
        if relation in {"on", "in", "at", "under", "above", "near", "behind", "between", "inside", "outside", "over"}:
            return f"located_{relation}"
        return relation

    def _resolve_pronoun(self, token: Optional[str]) -> Optional[str]:
        if not token:
            return None
        lowered = token.lower()
        if lowered in {"i", "me", "myself"}:
            return "user"
        if lowered in {"you", "yourself"}:
            return "logos"
        return token

    def _acknowledge_statement(self, subject: str, utterance: ParsedUtterance) -> str:
        pieces = []
        if utterance.attributes:
            pieces.append(f"{subject} has properties {', '.join(utterance.attributes)}")
        if utterance.obj:
            pieces.append(f"{subject} is {utterance.obj}")
        for relation, obj in utterance.relations:
            if obj:
                pieces.append(f"{subject} {relation} {obj}")
        if not pieces:
            return "Understood."
        return "I learned that " + "; ".join(pieces) + "."

    def _facts_snapshot(self) -> Dict[str, str]:
        recent_links = self._knowledge.recent_links(limit=6)
        snapshot = {}
        for index, link in enumerate(recent_links):
            snapshot[f"fact:{index}"] = self._knowledge.link_sentence(link)
        return snapshot
