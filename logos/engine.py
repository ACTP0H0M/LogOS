from __future__ import annotations

import os
from contextlib import contextmanager
from contextvars import ContextVar
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Optional, Tuple
from uuid import uuid4

from .knowledge import KnowledgeBase
from .memory_store import EpisodicMemory, MemoryStore, ProceduralMemory
from .nlp import (
    ARTICLES,
    COPULAS,
    DEMONSTRATIVES,
    INTERROGATIVE_ADJ,
    POSSESSIVE_ADJ,
    PREPOSITIONS,
    STOP_WORDS,
    TEMPORAL_ADVERBS,
    ParsedUtterance,
    parse_utterance,
    tokenize,
)
from .reasoner import Reasoner
from .state import Clarification, ConversationState


def _parse_int_env(name: str, default: int) -> int:
    raw = os.getenv(name, "").strip()
    if not raw:
        return default
    try:
        return int(raw)
    except ValueError:
        return default


_ENGINE_DEBUG_LEVEL = _parse_int_env("LOGOS_ENGINE_DEBUG_LEVEL", 1)
_ENGINE_DEBUG = (
    _ENGINE_DEBUG_LEVEL > 0
    and os.getenv("LOGOS_ENGINE_DEBUG", "1").strip().lower() not in {"", "0", "false", "no", "off"}
)
_ENGINE_DEBUG_INDENT: ContextVar[int] = ContextVar("logos_engine_debug_indent", default=0)
_ENGINE_TRACE_ID: ContextVar[str] = ContextVar("logos_engine_trace_id", default="-")

_PRONOUNS = {
    "i",
    "me",
    "my",
    "myself",
    "you",
    "your",
    "yourself",
    "we",
    "us",
    "our",
    "ourselves",
    "they",
    "them",
    "their",
    "theirs",
    "themselves",
    "he",
    "him",
    "his",
    "she",
    "her",
    "hers",
    "it",
    "its",
}

_RESERVED_TOKENS = (
    STOP_WORDS
    | ARTICLES
    | COPULAS
    | DEMONSTRATIVES
    | INTERROGATIVE_ADJ
    | POSSESSIVE_ADJ
    | TEMPORAL_ADVERBS
    | PREPOSITIONS
)


def _short(text: str, max_len: int = 200) -> str:
    cleaned = " ".join((text or "").split())
    if len(cleaned) <= max_len:
        return cleaned
    return cleaned[: max_len - 3] + "..."


def _dbg(message: str, level: int = 1) -> None:
    if not _ENGINE_DEBUG or level > _ENGINE_DEBUG_LEVEL:
        return
    indent = _ENGINE_DEBUG_INDENT.get()
    trace_id = _ENGINE_TRACE_ID.get()
    prefix = f"[engine {trace_id}] "
    print(prefix + ("  " * indent) + message)


@contextmanager
def _dbg_scope(title: str, level: int = 1):
    if not _ENGINE_DEBUG or level > _ENGINE_DEBUG_LEVEL:
        yield
        return
    _dbg(title, level=level)
    token = _ENGINE_DEBUG_INDENT.set(_ENGINE_DEBUG_INDENT.get() + 1)
    try:
        yield
    finally:
        _ENGINE_DEBUG_INDENT.reset(token)


@contextmanager
def _dbg_trace(trace_id: Optional[str] = None):
    if not _ENGINE_DEBUG:
        yield
        return
    token = _ENGINE_TRACE_ID.set(trace_id or uuid4().hex[:6])
    try:
        yield
    finally:
        _ENGINE_TRACE_ID.reset(token)


@dataclass
class ChatResponse:
    reply: str
    facts: Dict[str, str]
    links: List[Dict[str, object]]


class ChatEngine:
    """Coordinates parsing, memory updates, and symbolic reasoning."""

    def __init__(self) -> None:
        with _dbg_scope("ChatEngine.__init__()"):
            base_dir = Path(__file__).resolve().parent.parent
            data_dir = base_dir / "data"
            _dbg(f"base_dir: {base_dir}")
            _dbg(f"data_dir: {data_dir}")
            self._store = MemoryStore(data_dir, legacy_dir=base_dir)
            self._knowledge, self._episodic, self._procedural = self._store.load()
            _dbg(f"episodic events loaded: {len(self._episodic.events)}")
            _dbg(f"procedural rules loaded: {len(self._procedural.rules)}")
            self._save_current_session = False
            self._state = ConversationState()
            self._reasoner = Reasoner(self._knowledge, self._episodic, self._procedural, self._state)

    def process(self, message: str) -> ChatResponse:
        with _dbg_trace(), _dbg_scope("ChatEngine.process()"):
            _dbg(f"message: {message!r}")
            _dbg(f"pending clarifications: {len(self._state.pending_clarifications)}")
            _dbg(f"recent topics: {self._state.last_topics}")

            reply = ""
            if self._state.pending_clarifications:
                clarification = self._state.next_clarification()
                _dbg(f"next clarification: {clarification.role if clarification else None} ({clarification.term if clarification else None})")
                if clarification and clarification.role == "correction":
                    reply = self._handle_correction_response(message)
                else:
                    reply = self._handle_clarification(message)
            else:
                with _dbg_scope("parse_utterance(message)"):
                    utterance = parse_utterance(message)
                _dbg(
                    "parsed utterance -> "
                    f"kind={utterance.kind!r}, subject={utterance.subject!r}, obj={utterance.obj!r}, "
                    f"attributes={utterance.attributes}, relations={utterance.relations}"
                )

                if utterance.kind == "correction":
                    reply = self._handle_correction_request(message)
                elif utterance.kind == "definition":
                    reply = self._learn_definition(utterance)
                elif utterance.kind == "statement":
                    reply = self._learn_statement(utterance)
                elif utterance.kind == "query":
                    resolved = self._resolve_query_subject(utterance.subject)
                    _dbg(f"resolved query subject: {resolved!r}")
                    reply = self._reasoner.respond_to_query(resolved)
                else:
                    reply = self._reasoner.small_talk()

            _dbg(f"reply: {_short(reply)!r}")

            if self._save_current_session:
                _dbg("saving current session ...")
                self._store.save(self._knowledge, self._episodic, self._procedural)
            else:
                _dbg("save_current_session disabled")

            facts = self._facts_snapshot()
            links = self._links_snapshot()
            _dbg(f"facts snapshot: {len(facts)} items", level=2)
            _dbg(f"links snapshot: {len(links)} items", level=2)
            return ChatResponse(reply=reply, facts=facts, links=links)

    def _learn_statement(self, utterance: ParsedUtterance, parse_payload: Optional[Dict[str, object]] = None) -> str:
        with _dbg_scope("ChatEngine._learn_statement()"):
            _dbg(
                "utterance -> "
                f"subject={utterance.subject!r}, obj={utterance.obj!r}, "
                f"attributes={utterance.attributes}, relations={utterance.relations}"
            )
            subject = self._resolve_pronoun(utterance.subject)
            _dbg(f"resolved subject: {subject!r}")
            if not subject:
                _dbg("no subject found -> abort")
                return "I could not find a subject in that statement."

            new_symbol_ids: List[int] = []
            created_links: List[int] = []

            possessor, possessed = self._split_possessive(subject)
            _dbg(f"possessive split: possessor={possessor!r} possessed={possessed!r}")
            if possessor and possessed:
                if possessed.lower() == "name" and utterance.obj:
                    name_value = self._resolve_pronoun(utterance.obj)
                    _dbg(f"name assignment detected -> value={name_value!r}")
                    if name_value:
                        sym = self._knowledge.ensure_symbol(name_value, kind="entity")
                        _dbg(f"ensure_symbol(name_value) -> id={sym.id} kind={sym.kind!r} name={sym.name!r}")
                        link = self._knowledge.add_relation(
                            possessor,
                            "name",
                            name_value,
                            generality=0.8,
                            actuality=0.95,
                        )
                        created_links.append(link.id)
                        _dbg(f"add_relation: {possessor!r} -name-> {name_value!r} (link_id={link.id})")
                        event = self._episodic.add_event(utterance.raw, created_links, new_symbol_ids, parse=parse_payload)
                        _dbg(f"episodic.add_event -> event_id={event.id} links={created_links} new_symbols={new_symbol_ids}")
                        self._state.last_added_links = list(created_links)
                        self._state.last_utterance = utterance.raw
                        _dbg(f"state.last_added_links <- {self._state.last_added_links}")
                        _dbg(f"state.last_utterance <- {_short(self._state.last_utterance or '')!r}")
                        return f"Okay, I'll remember that {possessor} name is {name_value}."

                have_link = self._knowledge.add_relation(possessor, "have", possessed, generality=0.6, actuality=0.9)
                created_links.append(have_link.id)
                _dbg(f"add_relation: {possessor!r} -have-> {possessed!r} (link_id={have_link.id})")
                subject = possessed
                _dbg(f"subject rewritten to possessed: {subject!r}")

            unknown_terms = self._unknown_terms(utterance)
            subject_symbol = self._ensure_symbol_optional_kind(subject, "entity", unknown_terms)
            if subject_symbol:
                _dbg(
                    f"ensure_symbol(subject) -> id={subject_symbol.id} kind={subject_symbol.kind!r} name={subject_symbol.name!r}"
                )
            if subject_symbol and subject_symbol.id not in new_symbol_ids:
                new_symbol_ids.append(subject_symbol.id)
            _dbg(f"new_symbol_ids: {new_symbol_ids}")

            self._state.remember_topic(subject)
            _dbg(f"remember_topic -> last_topics={self._state.last_topics}")

            unknowns = self._collect_unknowns(
                subject,
                utterance,
                new_symbol_ids,
                created_links,
                parse_payload=parse_payload,
                unknown_terms=unknown_terms,
            )
            _dbg(f"_collect_unknowns -> {len(unknowns)} clarification(s)")
            _dbg(f"created_links: {created_links}")
            _dbg(f"new_symbol_ids: {new_symbol_ids}")

            event = self._episodic.add_event(utterance.raw, created_links, new_symbol_ids, parse=parse_payload)
            _dbg(f"episodic.add_event -> event_id={event.id} links={created_links} new_symbols={new_symbol_ids}")
            self._state.last_added_links = list(created_links)
            self._state.last_utterance = utterance.raw
            _dbg(f"state.last_added_links <- {self._state.last_added_links}")
            _dbg(f"state.last_utterance <- {_short(self._state.last_utterance or '')!r}")
            self._knowledge.decay_links(rate=0.01)
            _dbg("knowledge.decay_links(rate=0.01)")

            if unknowns:
                for clarification in unknowns:
                    _dbg(f"queue clarification -> term={clarification.term!r} role={clarification.role!r} context={clarification.context!r}")
                    self._state.queue_clarification(clarification)
                prompt = self._reasoner.pending_clarification_prompt(self._state.next_clarification())
                _dbg(f"pending clarification prompt: {_short(prompt)!r}")
                return prompt

            ack = self._acknowledge_statement(subject, utterance)
            _dbg(f"acknowledge: {_short(ack)!r}")
            return ack

    def _collect_unknowns(
        self,
        subject: str,
        utterance: ParsedUtterance,
        new_symbol_ids: List[int],
        created_links: List[int],
        parse_payload: Optional[Dict[str, object]] = None,
        unknown_terms: Optional[set[str]] = None,
    ) -> List[Clarification]:
        with _dbg_scope("ChatEngine._collect_unknowns()", level=2):
            unknowns: List[Clarification] = []
            _dbg(f"subject: {subject!r}", level=2)
            _dbg(
                f"utterance parts: attrs={len(utterance.attributes)} obj={utterance.obj!r} relations={len(utterance.relations)}",
                level=2,
            )

            unknown_terms = unknown_terms or self._unknown_terms(utterance)
            for term in sorted(unknown_terms):
                unknowns.append(Clarification(term=term, role="kind", context=utterance.raw))
            _dbg(f"unknown terms: {sorted(unknown_terms)}", level=2)

            subject_symbol = self._ensure_symbol_optional_kind(subject, "entity", unknown_terms)
            if subject_symbol and subject_symbol.id not in new_symbol_ids:
                new_symbol_ids.append(subject_symbol.id)

            for attr in utterance.attributes:
                with _dbg_scope(f"attr: {attr!r}", level=2):
                    attr_symbol = self._knowledge.symbol_by_name(attr)
                    attr_unknown = self._term_is_unknown(attr, unknown_terms) if attr_symbol is None else attr_symbol.kind == "unknown"
                    if not attr_symbol:
                        inferred = None if attr_unknown else self._infer_kind_from_parse(attr, parse_payload) or "property"
                        _dbg(f"unknown attr symbol -> ensure_symbol(kind={inferred!r})", level=2)
                        attr_symbol = self._knowledge.ensure_symbol(attr, kind=inferred)
                        new_symbol_ids.append(attr_symbol.id)
                    else:
                        _dbg(f"known attr symbol -> id={attr_symbol.id} kind={attr_symbol.kind!r}", level=2)

                    if subject_symbol and attr_symbol:
                        generality = 0.1 if attr_unknown else 0.4
                        actuality = 0.4 if attr_unknown else 0.9
                        link = self._knowledge.add_link(
                            subject_symbol.id,
                            attr_symbol.id,
                            "has_property",
                            generality=generality,
                            actuality=actuality,
                        )
                        _dbg(f"add_link has_property -> link_id={link.id}", level=2)
                        created_links.append(link.id)

            if utterance.obj:
                with _dbg_scope("copular object", level=2):
                    obj = self._resolve_pronoun(utterance.obj)
                    _dbg(f"resolved obj: {obj!r}", level=2)
                    obj_symbol = self._knowledge.symbol_by_name(obj) if obj else None
                    if obj_symbol:
                        _dbg(f"obj_symbol -> id={obj_symbol.id} kind={obj_symbol.kind!r} name={obj_symbol.name!r}", level=2)
                    else:
                        _dbg("obj_symbol -> None", level=2)

                    obj_unknown = self._term_is_unknown(obj or "", unknown_terms) if obj_symbol is None else obj_symbol.kind == "unknown"

                    if obj_symbol and obj_symbol.kind == "category":
                        link = self._knowledge.add_is_a(subject, obj)
                        created_links.append(link.id)
                        _dbg(f"add_is_a -> link_id={link.id}", level=2)
                    else:
                        if not obj_symbol and obj:
                            inferred = None if obj_unknown else self._infer_kind_from_parse(obj, parse_payload) or "property"
                            _dbg(f"unknown obj symbol -> ensure_symbol(kind={inferred!r})", level=2)
                            obj_symbol = self._knowledge.ensure_symbol(obj, kind=inferred)
                            new_symbol_ids.append(obj_symbol.id)
                        if subject_symbol and obj_symbol:
                            generality = 0.1 if obj_unknown else 0.4
                            actuality = 0.4 if obj_unknown else 0.9
                            link = self._knowledge.add_link(
                                subject_symbol.id,
                                obj_symbol.id,
                                "is",
                                generality=generality,
                                actuality=actuality,
                            )
                            created_links.append(link.id)
                            _dbg(f"add_link is -> link_id={link.id}", level=2)

            relation_modifiers = utterance.relation_modifiers or []
            for index, (relation, obj) in enumerate(utterance.relations):
                if not obj:
                    continue
                modifiers = relation_modifiers[index] if index < len(relation_modifiers) else []
                with _dbg_scope(f"relation: {relation!r}", level=2):
                    rel = self._normalize_relation(relation)
                    _dbg(f"normalized relation: {rel!r}", level=2)
                    _dbg(f"obj: {obj!r}", level=2)
                    obj_symbol = self._knowledge.symbol_by_name(obj)
                    obj_unknown = self._term_is_unknown(obj, unknown_terms) if obj_symbol is None else obj_symbol.kind == "unknown"
                    if not obj_symbol:
                        inferred = None if obj_unknown else self._infer_kind_for_relation_object(rel)
                        inferred = inferred or (None if obj_unknown else self._infer_kind_from_parse(obj, parse_payload)) or (
                            None if obj_unknown else "entity"
                        )
                        _dbg(f"unknown relation obj -> ensure_symbol(kind={inferred!r})", level=2)
                        obj_symbol = self._knowledge.ensure_symbol(obj, kind=inferred)
                        new_symbol_ids.append(obj_symbol.id)
                        relation_generality = 0.1
                        relation_actuality = 0.4
                    else:
                        _dbg(f"known relation obj -> id={obj_symbol.id} kind={obj_symbol.kind!r}", level=2)
                        relation_generality = 0.4 if not obj_unknown else 0.1
                        relation_actuality = 0.9 if not obj_unknown else 0.4

                    if modifiers and obj_symbol:
                        property_links: List[int] = []
                        for modifier in modifiers:
                            mod_symbol = self._knowledge.symbol_by_name(modifier)
                            mod_unknown = self._term_is_unknown(modifier, unknown_terms) if mod_symbol is None else mod_symbol.kind == "unknown"
                            if not mod_symbol:
                                inferred = None if mod_unknown else self._infer_kind_from_parse(modifier, parse_payload) or "property"
                                mod_symbol = self._knowledge.ensure_symbol(modifier, kind=inferred)
                                new_symbol_ids.append(mod_symbol.id)
                            link = self._knowledge.add_link(
                                obj_symbol.id,
                                mod_symbol.id,
                                "is",
                                generality=relation_generality,
                                actuality=relation_actuality,
                            )
                            created_links.append(link.id)
                            property_links.append(link.id)
                            _dbg(f"add_link is (modifier) -> link_id={link.id}", level=2)

                        if property_links:
                            first_link = self._knowledge.link_by_id(property_links[0])
                            branch = None
                            if first_link:
                                branch = self._knowledge.add_branch(
                                    [first_link.source, first_link.target],
                                    [first_link.id],
                                )
                                _dbg(f"add_branch (modifier) -> branch_id={branch.id}", level=2)
                            if branch and subject_symbol:
                                link = self._knowledge.add_link(
                                    subject_symbol.id,
                                    self._knowledge.branch_node_id(branch.id),
                                    rel,
                                    generality=relation_generality,
                                    actuality=relation_actuality,
                                )
                                _dbg(f"add_link (to branch) -> link_id={link.id}", level=2)
                                created_links.append(link.id)
                            continue

                    if rel == "where_loc" and relation != rel and obj_symbol:
                        prep_symbol = self._knowledge.symbol_by_name(relation)
                        if not prep_symbol:
                            _dbg(f"unknown preposition -> ensure_symbol(kind='relation')", level=2)
                            prep_symbol = self._knowledge.ensure_symbol(relation, kind="relation")
                            new_symbol_ids.append(prep_symbol.id)
                        branch, prep_link = self._knowledge.add_prep_branch(
                            obj_symbol.id,
                            prep_symbol.id,
                            generality=relation_generality,
                            actuality=relation_actuality,
                        )
                        created_links.append(prep_link.id)
                        _dbg(f"add_prep_branch -> branch_id={branch.id} prep_link_id={prep_link.id}", level=2)
                        if subject_symbol:
                            link = self._knowledge.add_link(
                                subject_symbol.id,
                                self._knowledge.branch_node_id(branch.id),
                                rel,
                                generality=relation_generality,
                                actuality=relation_actuality,
                            )
                            _dbg(f"add_link (to branch) -> link_id={link.id}", level=2)
                            created_links.append(link.id)
                    else:
                        if subject_symbol and obj_symbol:
                            link = self._knowledge.add_link(
                                subject_symbol.id,
                                obj_symbol.id,
                                rel,
                                generality=relation_generality,
                                actuality=relation_actuality,
                            )
                            _dbg(f"add_link relation -> link_id={link.id}", level=2)
                            created_links.append(link.id)

            _dbg(f"unknown clarifications generated: {len(unknowns)}", level=2)
            return unknowns

    def _unknown_terms(self, utterance: ParsedUtterance) -> set[str]:
        tokens = tokenize(utterance.raw or "")
        unknown: set[str] = set()
        for token in tokens:
            if not token:
                continue
            if token in _RESERVED_TOKENS or token in _PRONOUNS:
                continue
            if token.isdigit():
                continue
            symbol = self._knowledge.symbol_by_name(token)
            if symbol and symbol.kind != "unknown":
                continue
            unknown.add(token)
        return unknown

    def _term_is_unknown(self, term: str, unknown_terms: set[str]) -> bool:
        if not term:
            return False
        for token in tokenize(term):
            if token in unknown_terms:
                return True
        return False

    def _ensure_symbol_optional_kind(
        self,
        term: str,
        kind: Optional[str],
        unknown_terms: set[str],
    ) -> Optional[object]:
        if not term:
            return None
        if self._term_is_unknown(term, unknown_terms):
            return self._knowledge.ensure_symbol(term)
        return self._knowledge.ensure_symbol(term, kind=kind)

    def _learn_definition(self, utterance: ParsedUtterance, parse_payload: Optional[Dict[str, object]] = None) -> str:
        with _dbg_scope("ChatEngine._learn_definition()"):
            _dbg(
                "utterance -> "
                f"subject={utterance.subject!r}, obj={utterance.obj!r}, "
                f"attributes={utterance.attributes}, relations={utterance.relations}"
            )
            subject = self._resolve_pronoun(utterance.subject)
            obj = self._resolve_pronoun(utterance.obj)
            _dbg(f"resolved subject: {subject!r}")
            _dbg(f"resolved obj: {obj!r}")
            if not subject or not obj:
                _dbg("missing subject/obj -> restate requested")
                return "Can you restate that definition?"

            self._state.remember_topic(subject)
            _dbg(f"remember_topic -> last_topics={self._state.last_topics}")

            new_symbol_ids: List[int] = []
            created_links: List[int] = []
            parent_kind = self._infer_kind_from_parent(obj)
            _dbg(f"infer_kind_from_parent({obj!r}) -> {parent_kind!r}")
            if parent_kind:
                sym = self._knowledge.set_symbol_kind(subject, parent_kind)
                _dbg(f"set_symbol_kind -> id={sym.id} kind={sym.kind!r} name={sym.name!r}")
            link = self._knowledge.add_is_a(subject, obj, child_kind=parent_kind)
            created_links.append(link.id)
            _dbg(f"add_is_a: {subject!r} -> {obj!r} (link_id={link.id}, child_kind={parent_kind!r})")

            extra = ParsedUtterance(
                kind="statement",
                subject=subject,
                obj=None,
                attributes=utterance.attributes,
                relations=utterance.relations,
                raw=utterance.raw,
            )
            unknown_terms = self._unknown_terms(extra)
            unknowns = self._collect_unknowns(
                subject,
                extra,
                new_symbol_ids,
                created_links,
                parse_payload=parse_payload,
                unknown_terms=unknown_terms,
            )
            _dbg(f"_collect_unknowns -> {len(unknowns)} clarification(s)")

            event = self._episodic.add_event(utterance.raw, created_links, new_symbol_ids, parse=parse_payload)
            _dbg(f"episodic.add_event -> event_id={event.id} links={created_links} new_symbols={new_symbol_ids}")
            self._state.last_added_links = list(created_links)
            self._state.last_utterance = utterance.raw
            _dbg(f"state.last_added_links <- {self._state.last_added_links}")
            _dbg(f"state.last_utterance <- {_short(self._state.last_utterance or '')!r}")

            if unknowns:
                for clarification in unknowns:
                    _dbg(f"queue clarification -> term={clarification.term!r} role={clarification.role!r} context={clarification.context!r}")
                    self._state.queue_clarification(clarification)
                prompt = self._reasoner.pending_clarification_prompt(self._state.next_clarification())
                _dbg(f"pending clarification prompt: {_short(prompt)!r}")
                return prompt

            reply = f"Okay, I'll remember that {subject} is a kind of {obj}."
            _dbg(f"reply: {_short(reply)!r}")
            return reply

    def _handle_clarification(self, message: str) -> str:
        with _dbg_scope("ChatEngine._handle_clarification()"):
            _dbg(f"message: {message!r}")
            clarification = self._state.pop_clarification()
            if not clarification:
                _dbg("no pending clarification -> nothing to handle")
                return "Thanks. Let me know if you want to add more."
            _dbg(
                "popped clarification -> "
                f"term={clarification.term!r} role={clarification.role!r} context={clarification.context!r} link_ids={clarification.link_ids}"
            )

            with _dbg_scope("parse_utterance(message)"):
                utterance = parse_utterance(message)
            _dbg(f"clarification response parsed -> kind={utterance.kind!r} obj={utterance.obj!r}")

            if utterance.kind == "definition" and utterance.obj:
                parent_kind = self._infer_kind_from_parent(utterance.obj)
                _dbg(f"infer_kind_from_parent({utterance.obj!r}) -> {parent_kind!r}")
                if parent_kind:
                    sym = self._knowledge.set_symbol_kind(clarification.term, parent_kind)
                    _dbg(f"set_symbol_kind({clarification.term!r}) -> id={sym.id} kind={sym.kind!r}")
                link = self._knowledge.add_is_a(clarification.term, utterance.obj, child_kind=parent_kind)
                _dbg(f"add_is_a -> link_id={link.id}")
                if self._state.pending_clarifications:
                    prompt = self._reasoner.pending_clarification_prompt(self._state.next_clarification())
                    return f"Got it. {clarification.term} is a kind of {utterance.obj}. {prompt}"
                return f"Got it. {clarification.term} is a kind of {utterance.obj}."

            kind, parent = self._classify_answer(message)
            _dbg(f"classify_answer -> kind={kind!r} parent={parent!r}")
            if kind:
                sym = self._knowledge.set_symbol_kind(clarification.term, kind)
                _dbg(f"set_symbol_kind({clarification.term!r}) -> id={sym.id} kind={sym.kind!r}")
                if clarification.role != "kind" and parent:
                    link = self._knowledge.add_is_a(clarification.term, parent)
                    _dbg(f"add_is_a({clarification.term!r}, {parent!r}) -> link_id={link.id}")
                if self._state.pending_clarifications:
                    prompt = self._reasoner.pending_clarification_prompt(self._state.next_clarification())
                    return f"Thanks. I'll treat '{clarification.term}' as a {kind}. {prompt}"
                return f"Thanks. I'll treat '{clarification.term}' as a {kind}."

            _dbg("clarification response not understood -> re-queue")
            self._state.queue_clarification(clarification)
            return f"I still need a hint about '{clarification.term}'. Is it an entity, property, or action?"

    def _handle_correction_request(self, message: str) -> str:
        with _dbg_scope("ChatEngine._handle_correction_request()"):
            _dbg(f"message: {message!r}")
            if not self._state.last_added_links:
                _dbg("no last_added_links -> cannot target correction")
                return "What specifically was incorrect? Please point to the fact."
            clarification = Clarification(
                term="correction",
                role="correction",
                context=self._state.last_utterance or "the last statement",
                link_ids=list(self._state.last_added_links),
            )
            self._state.queue_clarification(clarification)
            _dbg(f"queued correction clarification -> link_ids={clarification.link_ids} context={_short(clarification.context)!r}")
            context = f" ({clarification.context})" if clarification.context else ""
            return (
                "What exactly was incorrect" + context + "? "
                "You can name the part that is wrong (e.g., 'yellow', 'banana', or 'on table')."
            )

    def _handle_correction_response(self, message: str) -> str:
        with _dbg_scope("ChatEngine._handle_correction_response()"):
            _dbg(f"message: {message!r}")
            clarification = self._state.pop_clarification()
            if not clarification:
                _dbg("no clarification to respond to")
                return "Thanks for clarifying."
            _dbg(f"popped correction clarification -> link_ids={clarification.link_ids} context={_short(clarification.context)!r}")

            response = message.strip().lower()
            if response in {"all", "everything", "all of it"}:
                _dbg("correction targets: all")
                removed = self._remove_links(clarification.link_ids)
                _dbg(f"removed links: {removed}")
                return f"Okay, I removed {removed} fact(s) from memory."

            with _dbg_scope("parse_utterance(message)"):
                utterance = parse_utterance(message)
            _dbg(f"correction follow-up utterance kind: {utterance.kind!r}")

            match_ids = self._match_links_for_correction(clarification.link_ids, message)
            _dbg(f"matched links for correction: {match_ids}")
            removed = self._remove_links(match_ids)
            _dbg(f"removed links: {removed}")
            if removed == 0:
                _dbg("no links removed -> re-queue clarification")
                self._state.queue_clarification(clarification)
                return "I couldn't tell which fact you meant. Which part was incorrect?"

            if utterance.kind in {"statement", "definition"}:
                _dbg("learning replacement info after correction")
                if utterance.kind == "definition":
                    follow_up = self._learn_definition(utterance)
                else:
                    follow_up = self._learn_statement(utterance)
                return f"Thanks. I removed {removed} fact(s). {follow_up}"

            return f"Thanks. I removed {removed} fact(s) from memory."

    def _classify_answer(self, text: str) -> Tuple[Optional[str], Optional[str]]:
        with _dbg_scope("ChatEngine._classify_answer()", level=2):
            _dbg(f"text: {text!r}", level=2)
            lowered = text.lower()
            if "#relation" in lowered or "relation" in lowered:
                _dbg("classified: relation/relation", level=2)
                return "relation", "#RELATION"
            if "#time" in lowered or "time" in lowered or "temporal" in lowered:
                _dbg("classified: time/time", level=2)
                return "time", "#TIME"
            if "#state" in lowered or "state" in lowered or "condition" in lowered:
                _dbg("classified: state/state", level=2)
                return "state", "#STATE"
            if "color" in lowered:
                _dbg("classified: property/color", level=2)
                return "property", "color"
            if "property" in lowered or "attribute" in lowered or "quality" in lowered:
                _dbg("classified: property/property", level=2)
                return "property", "#PROPERTY"
            if "place" in lowered or "location" in lowered:
                _dbg("classified: location/location", level=2)
                return "location", "#LOCATION"
            if "action" in lowered or "verb" in lowered:
                _dbg("classified: action/action", level=2)
                return "action", "#VERB"
            if "thing" in lowered or "object" in lowered or "entity" in lowered:
                _dbg("classified: entity/entity", level=2)
                return "entity", "#ENTITY"
            _dbg("classified: None", level=2)
            return None, None

    def _infer_kind_from_parent(self, parent: str) -> Optional[str]:
        with _dbg_scope("ChatEngine._infer_kind_from_parent()", level=2):
            _dbg(f"parent: {parent!r}", level=2)
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
        with _dbg_scope("ChatEngine._normalize_relation()", level=2):
            _dbg(f"relation: {relation!r}", level=2)
            if relation in {
                "on",
                "in",
                "at",
                "under",
                "above",
                "near",
                "behind",
                "between",
                "inside",
                "outside",
                "over",
            }:
                _dbg("normalized -> where_loc", level=2)
                return "where_loc"
            _dbg("normalized -> unchanged", level=2)
            return relation

    def _resolve_pronoun(self, token: Optional[str]) -> Optional[str]:
        with _dbg_scope("ChatEngine._resolve_pronoun()", level=2):
            _dbg(f"token: {token!r}", level=2)
            if not token:
                return None
            lowered = token.lower()
            if lowered in {"i", "me", "myself"}:
                return "#USER"
            if lowered in {"you", "yourself"}:
                return "#SELF"
            if lowered in {"this", "that", "these", "those"}:
                if self._state.last_topics:
                    resolved = self._state.last_topics[-1]
                    _dbg(f"demonstrative -> last topic: {resolved!r}", level=2)
                    return resolved
            return token

    def _resolve_query_subject(self, subject: Optional[str]) -> str:
        with _dbg_scope("ChatEngine._resolve_query_subject()", level=2):
            _dbg(f"subject: {subject!r}", level=2)
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
        with _dbg_scope("ChatEngine._split_possessive()", level=2):
            _dbg(f"subject: {subject!r}", level=2)
            lowered = subject.lower().strip()
            if lowered.startswith("my "):
                return "#USER", subject[3:].strip()
            if lowered.startswith("your "):
                return "#SELF", subject[5:].strip()
            if lowered.startswith("our "):
                return "#USER", subject[4:].strip()
            return None, None

    def _infer_kind_for_relation_object(self, relation: str) -> Optional[str]:
        with _dbg_scope("ChatEngine._infer_kind_for_relation_object()", level=2):
            _dbg(f"relation: {relation!r}", level=2)
            if relation in {"where_loc", "where_dir"}:
                return "location"
            if relation in {"when"}:
                return "time"
            return None

    def _infer_kind_from_parse(self, term: str, parse_payload: Optional[Dict[str, object]]) -> Optional[str]:
        with _dbg_scope("ChatEngine._infer_kind_from_parse()", level=2):
            _dbg(f"term: {term!r}", level=2)
            if not parse_payload:
                _dbg("parse_payload missing -> None", level=2)
                return None
            term_lower = term.lower()
            for sent in parse_payload.get("sentences", []) or []:
                for tok in sent.get("tokens", []) or []:
                    text = str(tok.get("text", "")).strip()
                    if text.lower() != term_lower:
                        continue
                    upos = str(tok.get("upos", "")).upper()
                    _dbg(f"matched token -> upos={upos!r}", level=2)
                    if upos in {"NOUN", "PROPN", "PRON"}:
                        return "entity"
                    if upos in {"ADJ"}:
                        return "property"
                    if upos in {"VERB", "AUX"}:
                        return "action"
                    if upos in {"ADV"}:
                        return "property"
                    if upos in {"NUM"}:
                        return "entity"
            return None

    def _acknowledge_statement(self, subject: str, utterance: ParsedUtterance) -> str:
        with _dbg_scope("ChatEngine._acknowledge_statement()", level=2):
            _dbg(f"subject: {subject!r}", level=2)
            _dbg(f"utterance: attrs={utterance.attributes} obj={utterance.obj!r} relations={utterance.relations}", level=2)
            pieces: List[str] = []
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
        with _dbg_scope("ChatEngine._facts_snapshot()", level=2):
            recent_links = self._knowledge.recent_links(limit=6)
            _dbg(f"recent_links(limit=6) -> {len(recent_links)}", level=2)
            snapshot: Dict[str, str] = {}
            for index, link in enumerate(recent_links):
                sentence = self._knowledge.link_sentence(link)
                snapshot[f"fact:{index}"] = sentence
                _dbg(f"fact:{index} -> {_short(sentence)!r}", level=2)
            return snapshot

    def _links_snapshot(self) -> List[Dict[str, object]]:
        with _dbg_scope("ChatEngine._links_snapshot()", level=2):
            links = sorted(self._knowledge.links(), key=lambda lk: lk.id)
            _dbg(f"knowledge.links() -> {len(links)}", level=2)
            payload: List[Dict[str, object]] = []
            for link in links:
                payload.append(
                    {
                        "id": link.id,
                        "source": link.source,
                        "source_label": self._knowledge.node_label_with_id(link.source, wrap_branch=True),
                        "relation": link.relation,
                        "target": link.target,
                        "target_label": self._knowledge.node_label_with_id(link.target, wrap_branch=True),
                        "generality": link.generality,
                        "actuality": link.actuality,
                        "created_at": link.created_at,
                    }
                )
            return payload

    def _graph_snapshot(self, max_links: int = 120, max_nodes: int = 80) -> Dict[str, object]:
        with _dbg_scope("ChatEngine._graph_snapshot()", level=2):
            # Keep the browser-side visualization cheap by sending a recent subgraph.
            _dbg(f"params: max_links={max_links} max_nodes={max_nodes}", level=2)
            links = self._knowledge.recent_links(limit=max_links)
            _dbg(f"recent_links(limit={max_links}) -> {len(links)}", level=2)
            node_ids = set()
            for lk in links:
                node_ids.add(lk.source)
                node_ids.add(lk.target)
            node_ids = list(node_ids)[:max_nodes]
            node_id_set = set(node_ids)
            link_payload = [
                {
                    "id": lk.id,
                    "source": lk.source,
                    "target": lk.target,
                    "relation": lk.relation,
                    "generality": lk.generality,
                    "actuality": lk.actuality,
                }
                for lk in links
                if lk.source in node_id_set and lk.target in node_id_set
            ]
            node_payload: List[Dict[str, object]] = []
            for node_id in node_ids:
                label = self._knowledge.node_label(node_id, wrap_branch=True)
                kind = self._knowledge.node_kind(node_id)
                node_payload.append({"id": node_id, "label": label, "kind": kind})
            _dbg(f"graph payload -> nodes={len(node_payload)} links={len(link_payload)}", level=2)
            return {"nodes": node_payload, "links": link_payload}

    def _match_links_for_correction(self, link_ids: List[int], message: str) -> List[int]:
        with _dbg_scope("ChatEngine._match_links_for_correction()", level=2):
            _dbg(f"candidate link_ids: {link_ids}", level=2)
            tokens = set(tokenize(message))
            _dbg(f"message tokens: {sorted(tokens)}", level=2)
            matches: List[int] = []
            for link_id in link_ids:
                link = self._knowledge.link_by_id(link_id)
                if not link:
                    _dbg(f"link_id={link_id} -> missing", level=2)
                    continue
                source_name = self._knowledge.node_label(link.source, wrap_branch=False).lower()
                target_name = self._knowledge.node_label(link.target, wrap_branch=False).lower()
                relation = link.relation.lower()
                if source_name in tokens or target_name in tokens or relation in tokens:
                    _dbg(f"match link_id={link_id} by exact token", level=2)
                    matches.append(link_id)
                    continue
                if any(token in source_name for token in tokens) or any(token in target_name for token in tokens):
                    _dbg(f"match link_id={link_id} by substring", level=2)
                    matches.append(link_id)
            _dbg(f"matches: {matches}", level=2)
            return matches

    def _remove_links(self, link_ids: List[int]) -> int:
        with _dbg_scope("ChatEngine._remove_links()", level=2):
            _dbg(f"removing link_ids: {link_ids}", level=2)
            removed = 0
            for link_id in link_ids:
                ok = self._knowledge.remove_link(link_id)
                _dbg(f"remove_link({link_id}) -> {ok}", level=2)
                if ok:
                    removed += 1
            _dbg(f"removed total: {removed}", level=2)
            return removed
