from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Optional, Tuple

from .knowledge import KnowledgeBase
from .memory_store import EpisodicMemory, MemoryStore, ProceduralMemory
from .nlp import ParsedUtterance, parse_utterance, tokenize
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
            clarification = self._state.next_clarification()
            if clarification and clarification.role == "correction":
                reply = self._handle_correction_response(message)
            else:
                reply = self._handle_clarification(message)
        else:
            utterance = parse_utterance(message)
            if utterance.kind == "correction":
                reply = self._handle_correction_request(message)
            elif utterance.kind == "definition":
                reply = self._learn_definition(utterance)
            elif utterance.kind == "statement":
                reply = self._learn_statement(utterance)
            elif utterance.kind == "query":
                resolved = self._resolve_query_subject(utterance.subject)
                reply = self._reasoner.respond_to_query(resolved)
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

        possessor, possessed = self._split_possessive(subject)
        if possessor and possessed:
            if possessed.lower() == "name" and utterance.obj:
                name_value = self._resolve_pronoun(utterance.obj)
                if name_value:
                    self._knowledge.ensure_symbol(name_value, kind="entity")
                    link = self._knowledge.add_relation(possessor, "name", name_value, generality=0.8, actuality=0.95)
                    created_links.append(link.id)
                    self._episodic.add_event(utterance.raw, created_links, new_symbol_ids)
                    self._state.last_added_links = list(created_links)
                    self._state.last_utterance = utterance.raw
                    return f"Okay, I'll remember that {possessor} name is {name_value}."
            have_link = self._knowledge.add_relation(possessor, "have", possessed, generality=0.6, actuality=0.9)
            created_links.append(have_link.id)
            subject = possessed
        subject_symbol = self._knowledge.ensure_symbol(subject, kind="entity")
        if subject_symbol.id not in new_symbol_ids:
            new_symbol_ids.append(subject_symbol.id)
        self._state.remember_topic(subject)

        unknowns = self._collect_unknowns(subject, utterance, new_symbol_ids, created_links)
        self._episodic.add_event(utterance.raw, created_links, new_symbol_ids)
        self._state.last_added_links = list(created_links)
        self._state.last_utterance = utterance.raw
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
                link = self._knowledge.add_property(subject, attr, generality=0.1, actuality=0.4)
            else:
                link = self._knowledge.add_property(subject, attr, generality=0.4, actuality=0.9)
            created_links.append(link.id)

        if utterance.obj:
            obj = self._resolve_pronoun(utterance.obj)
            obj_symbol = self._knowledge.symbol_by_name(obj) if obj else None
            if obj_symbol and obj_symbol.kind == "category":
                link = self._knowledge.add_is_a(subject, obj)
                created_links.append(link.id)
            else:
                if not obj_symbol and obj:
                    obj_symbol = self._knowledge.ensure_symbol(obj, kind="property")
                    new_symbol_ids.append(obj_symbol.id)
                    unknowns.append(
                        Clarification(
                            term=obj,
                            role="property",
                            context=f"{subject} is {obj}",
                        )
                    )
                    link = self._knowledge.add_is(subject, obj, generality=0.1, actuality=0.4)
                else:
                    link = self._knowledge.add_is(subject, obj, generality=0.4, actuality=0.9)
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
                link = self._knowledge.add_relation(subject, rel, obj, generality=0.1, actuality=0.4)
            else:
                link = self._knowledge.add_relation(subject, rel, obj, generality=0.4, actuality=0.9)
            created_links.append(link.id)

        return unknowns

    def _learn_definition(self, utterance: ParsedUtterance) -> str:
        subject = self._resolve_pronoun(utterance.subject)
        obj = self._resolve_pronoun(utterance.obj)
        if not subject or not obj:
            return "Can you restate that definition?"

        self._state.remember_topic(subject)
        new_symbol_ids: List[int] = []
        created_links: List[int] = []
        parent_kind = self._infer_kind_from_parent(obj)
        if parent_kind:
            self._knowledge.set_symbol_kind(subject, parent_kind)
        link = self._knowledge.add_is_a(subject, obj, child_kind=parent_kind)
        created_links.append(link.id)

        extra = ParsedUtterance(
            kind="statement",
            subject=subject,
            obj=None,
            attributes=utterance.attributes,
            relations=utterance.relations,
            raw=utterance.raw,
        )
        unknowns = self._collect_unknowns(subject, extra, new_symbol_ids, created_links)

        self._episodic.add_event(utterance.raw, created_links, new_symbol_ids)
        self._state.last_added_links = list(created_links)
        self._state.last_utterance = utterance.raw
        if unknowns:
            for clarification in unknowns:
                self._state.queue_clarification(clarification)
            prompt = self._reasoner.pending_clarification_prompt(self._state.next_clarification())
            return prompt
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

    def _handle_correction_request(self, message: str) -> str:
        if not self._state.last_added_links:
            return "What specifically was incorrect? Please point to the fact."
        clarification = Clarification(
            term="correction",
            role="correction",
            context=self._state.last_utterance or "the last statement",
            link_ids=list(self._state.last_added_links),
        )
        self._state.queue_clarification(clarification)
        context = f" ({clarification.context})" if clarification.context else ""
        return (
            "What exactly was incorrect" + context + "? "
            "You can name the part that is wrong (e.g., 'yellow', 'banana', or 'on table')."
        )

    def _handle_correction_response(self, message: str) -> str:
        clarification = self._state.pop_clarification()
        if not clarification:
            return "Thanks for clarifying."

        response = message.strip().lower()
        if response in {"all", "everything", "all of it"}:
            removed = self._remove_links(clarification.link_ids)
            return f"Okay, I removed {removed} fact(s) from memory."

        utterance = parse_utterance(message)
        removed = self._remove_links(self._match_links_for_correction(clarification.link_ids, message))
        if removed == 0:
            self._state.queue_clarification(clarification)
            return "I couldn't tell which fact you meant. Which part was incorrect?"

        if utterance.kind in {"statement", "definition"}:
            if utterance.kind == "definition":
                follow_up = self._learn_definition(utterance)
            else:
                follow_up = self._learn_statement(utterance)
            return f"Thanks. I removed {removed} fact(s). {follow_up}"

        return f"Thanks. I removed {removed} fact(s) from memory."

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
            return "where_loc"
        return relation

    def _resolve_pronoun(self, token: Optional[str]) -> Optional[str]:
        if not token:
            return None
        lowered = token.lower()
        if lowered in {"i", "me", "myself"}:
            return "#USER"
        if lowered in {"you", "yourself"}:
            return "#SELF"
        if lowered in {"this", "that", "these", "those"}:
            if self._state.last_topics:
                return self._state.last_topics[-1]
        return token

    def _resolve_query_subject(self, subject: Optional[str]) -> str:
        if not subject:
            return ""
        lowered = subject.lower().strip()
        if lowered in {"you", "your", "yourself"}:
            return "#SELF"
        if lowered in {"me", "my", "myself"}:
            return "#USER"
        if lowered.startswith("your "):
            return "#SELF " + subject[5:]
        if lowered.startswith("my "):
            return "#USER " + subject[3:]
        return subject

    def _split_possessive(self, subject: str) -> Tuple[Optional[str], Optional[str]]:
        lowered = subject.lower().strip()
        if lowered.startswith("my "):
            return "#USER", subject[3:].strip()
        if lowered.startswith("your "):
            return "#SELF", subject[5:].strip()
        if lowered.startswith("our "):
            return "#USER", subject[4:].strip()
        return None, None

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

    def _match_links_for_correction(self, link_ids: List[int], message: str) -> List[int]:
        tokens = set(tokenize(message))
        matches: List[int] = []
        for link_id in link_ids:
            link = self._knowledge.link_by_id(link_id)
            if not link:
                continue
            source = self._knowledge.symbol_by_id(link.source)
            target = self._knowledge.symbol_by_id(link.target)
            source_name = source.name.lower() if source else ""
            target_name = target.name.lower() if target else ""
            relation = link.relation.lower()
            if source_name in tokens or target_name in tokens or relation in tokens:
                matches.append(link_id)
                continue
            if any(token in source_name for token in tokens) or any(token in target_name for token in tokens):
                matches.append(link_id)
        return matches

    def _remove_links(self, link_ids: List[int]) -> int:
        removed = 0
        for link_id in link_ids:
            if self._knowledge.remove_link(link_id):
                removed += 1
        return removed
