from __future__ import annotations

import os
from contextlib import contextmanager
from dataclasses import dataclass, field
from typing import List, Optional, Tuple

STOP_WORDS = {"a", "an", "the", "to", "of", "and", "in", "on", "for", "with", "there"}
ARTICLES = {"a", "an", "the"}
COPULAS = {"is", "are", "was", "were", "be", "been", "am"}
DEMONSTRATIVES = {"this", "that", "these", "those"}
INTERROGATIVE_ADJ = {"which", "what"}
POSSESSIVE_ADJ = {"my", "your", "his", "her", "its", "our", "their"}
TEMPORAL_ADVERBS = {"currently", "now", "today", "yesterday", "tomorrow"}
CORRECTION_PHRASES = {
    "incorrect",
    "wrong",
    "not correct",
    "not right",
    "false",
    "that's wrong",
    "that is wrong",
    "that's incorrect",
    "that is incorrect",
}
PREPOSITIONS = {
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
REQUEST_PREFIX_PHRASES = (
    ("would", "you", "be", "so", "kind"),
    ("could", "you"),
    ("would", "you"),
    ("can", "you"),
    ("please",),
    ("kindly",),
)
COMMAND_LEAD_VERBS = {
    "arrange",
    "book",
    "build",
    "buy",
    "create",
    "design",
    "find",
    "get",
    "make",
    "organize",
    "plan",
    "prepare",
    "show",
    "write",
}
COMMAND_FILLER_TOKENS = {"please", "kindly", "just"}
COMMAND_INDIRECT_OBJECTS = {"me", "us"}

_NLP_DEBUG = os.getenv("LOGOS_NLP_DEBUG", "1").strip().lower() not in {"", "0", "false", "no", "off"}
_NLP_DEBUG_INDENT = 0


def _dbg(message: str) -> None:
    if not _NLP_DEBUG:
        return
    print(("  " * _NLP_DEBUG_INDENT) + message)


@contextmanager
def _dbg_scope(title: str):
    global _NLP_DEBUG_INDENT
    _dbg(title)
    _NLP_DEBUG_INDENT += 1
    try:
        yield
    finally:
        _NLP_DEBUG_INDENT = max(0, _NLP_DEBUG_INDENT - 1)


@dataclass
class ParsedUtterance:
    kind: str
    subject: Optional[str] = None
    query_relation: Optional[str] = None
    query_verb: Optional[str] = None
    obj: Optional[str] = None
    attributes: List[str] = field(default_factory=list)
    relations: List[Tuple[str, str]] = field(default_factory=list)
    relation_modifiers: List[List[str]] = field(default_factory=list)
    passive_verb: Optional[str] = None
    passive_agent: Optional[str] = None
    passive_patient: Optional[str] = None
    passive_loc: Optional[Tuple[str, str]] = None
    active_verb: Optional[str] = None
    active_object: Optional[str] = None
    active_object_modifiers: List[str] = field(default_factory=list)
    command_request: Optional[str] = None
    command_verb: Optional[str] = None
    command_object: Optional[str] = None
    raw: str = ""


def tokenize(text: str) -> List[str]:
    with _dbg_scope("tokenize()"):
        _dbg(f"input: {text!r}")
        tokens = [token.strip(".,!?;:").lower() for token in text.split() if token.strip(".,!?;:")]
        _dbg(f"tokens: {tokens}")
        return tokens

def tokenize_with_case(text: str) -> List[str]:
    with _dbg_scope("tokenize_with_case()"):
        _dbg(f"input: {text!r}")
        tokens = [token.strip(".,!?;:") for token in text.split() if token.strip(".,!?;:")]
        _dbg(f"tokens_case: {tokens}")
        return tokens


def _strip_articles(tokens: List[str]) -> List[str]:
    with _dbg_scope("_strip_articles()"):
        _dbg(f"in: {tokens}")
        stripped = [token for token in tokens if token not in ARTICLES]
        _dbg(f"out: {stripped}")
        return stripped

def _strip_determiners(tokens: List[str]) -> List[str]:
    with _dbg_scope("_strip_determiners()"):
        _dbg(f"in: {tokens}")
        stripped = [
            token
            for token in tokens
            if token not in ARTICLES
            and token not in DEMONSTRATIVES
            and token not in INTERROGATIVE_ADJ
            and token not in POSSESSIVE_ADJ
        ]
        _dbg(f"out: {stripped}")
        return stripped

def _case_preserve(tokens_case: List[str], tokens_lower: List[str], filtered_lower: List[str]) -> List[str]:
    with _dbg_scope("_case_preserve()"):
        _dbg(f"tokens_case: {tokens_case}")
        _dbg(f"tokens_lower: {tokens_lower}")
        _dbg(f"filtered_lower: {filtered_lower}")
        if not filtered_lower:
            _dbg("filtered_lower empty -> []")
            return []
        result: List[str] = []
        index = 0
        for token_case, token_lower in zip(tokens_case, tokens_lower):
            if index >= len(filtered_lower):
                break
            if token_lower == filtered_lower[index]:
                result.append(token_case)
                index += 1
        _dbg(f"result: {result}")
        return result

def _case_for_head(tokens_case: List[str], tokens_lower: List[str], head_lower: Optional[str]) -> Optional[str]:
    with _dbg_scope("_case_for_head()"):
        _dbg(f"tokens_case: {tokens_case}")
        _dbg(f"tokens_lower: {tokens_lower}")
        _dbg(f"head_lower: {head_lower!r}")
        if not head_lower:
            _dbg("no head_lower -> None")
            return None
        for token_case, token_lower in zip(reversed(tokens_case), reversed(tokens_lower)):
            if token_lower == head_lower:
                _dbg(f"matched head token_case: {token_case!r}")
                return token_case
        _dbg("no case match -> returning head_lower")
        return head_lower


def _head_and_modifiers(tokens: List[str]) -> Tuple[Optional[str], List[str]]:
    with _dbg_scope("_head_and_modifiers()"):
        _dbg(f"in: {tokens}")
        cleaned = _strip_determiners(tokens)
        _dbg(f"cleaned: {cleaned}")
        if not cleaned:
            _dbg("no cleaned tokens -> (None, [])")
            return None, []
        if "of" in cleaned and len(cleaned) > 1:
            phrase = " ".join(cleaned)
            _dbg(f"of-phrase -> head: {phrase!r}, modifiers: []")
            return phrase, []
        head = cleaned[-1]
        modifiers = cleaned[:-1]
        _dbg(f"head: {head!r}")
        _dbg(f"modifiers: {modifiers}")
        return head, modifiers


def _join(tokens: List[str]) -> str:
    with _dbg_scope("_join()"):
        _dbg(f"in: {tokens}")
        joined = " ".join(tokens).strip()
        _dbg(f"out: {joined!r}")
        return joined


def _detect_command(tokens_case: List[str], tokens_lower: List[str]) -> Optional[Tuple[Optional[str], str, Optional[str]]]:
    with _dbg_scope("_detect_command()"):
        _dbg(f"tokens_case: {tokens_case}")
        _dbg(f"tokens_lower: {tokens_lower}")
        if not tokens_lower:
            _dbg("no tokens -> None")
            return None

        request_phrase: Optional[str] = None
        verb_idx = 0
        for phrase in sorted(REQUEST_PREFIX_PHRASES, key=len, reverse=True):
            phrase_len = len(phrase)
            if tokens_lower[:phrase_len] == list(phrase):
                request_phrase = _join(tokens_case[:phrase_len])
                verb_idx = phrase_len
                _dbg(f"matched request phrase: {request_phrase!r}")
                break

        while verb_idx < len(tokens_lower) and tokens_lower[verb_idx] in COMMAND_FILLER_TOKENS:
            verb_idx += 1

        if request_phrase and verb_idx < len(tokens_lower) and tokens_lower[verb_idx] == "you":
            verb_idx += 1

        if verb_idx >= len(tokens_lower):
            _dbg("no verb candidate left -> None")
            return None

        verb_lower = tokens_lower[verb_idx]
        if verb_idx + 1 < len(tokens_lower) and tokens_lower[verb_idx + 1] in COPULAS:
            _dbg("verb candidate followed by copula -> not a command")
            return None

        if request_phrase:
            if verb_lower in ARTICLES or verb_lower in PREPOSITIONS or verb_lower in COPULAS:
                _dbg("request phrase found but no usable verb after it")
                return None
        elif verb_lower not in COMMAND_LEAD_VERBS:
            _dbg(f"leading token {verb_lower!r} not in imperative seed list")
            return None

        obj_tokens_case = tokens_case[verb_idx + 1 :]
        obj_tokens_lower = tokens_lower[verb_idx + 1 :]
        if len(obj_tokens_lower) > 1 and obj_tokens_lower[0] in COMMAND_INDIRECT_OBJECTS:
            obj_tokens_case = obj_tokens_case[1:]
            obj_tokens_lower = obj_tokens_lower[1:]

        obj = _join(obj_tokens_case) if obj_tokens_case else None
        _dbg(f"command request: {request_phrase!r}")
        _dbg(f"command verb: {tokens_case[verb_idx]!r}")
        _dbg(f"command object: {obj!r}")
        return request_phrase, tokens_case[verb_idx], obj


def parse_utterance(text: str) -> ParsedUtterance:
    with _dbg_scope("parse_utterance()"):
        _dbg(f"input: {text!r}")
        raw = text.strip()
        _dbg(f"raw: {raw!r}")
        tokens_case = tokenize_with_case(raw)
        tokens = [token.lower() for token in tokens_case]
        _dbg(f"tokens_case: {tokens_case}")
        _dbg(f"tokens_lower: {tokens}")

        if not tokens:
            _dbg("no tokens -> kind=empty")
            return ParsedUtterance(kind="empty", raw=raw)

        lowered = raw.lower()
        matched_corrections = [phrase for phrase in CORRECTION_PHRASES if phrase in lowered]
        if matched_corrections:
            _dbg(f"correction phrase detected: {matched_corrections}")
            return ParsedUtterance(kind="correction", raw=raw)

        command = _detect_command(tokens_case, tokens)
        if command:
            request_phrase, command_verb, command_object = command
            _dbg("command detected")
            return ParsedUtterance(
                kind="command",
                command_request=request_phrase,
                command_verb=command_verb,
                command_object=command_object,
                raw=raw,
            )

        if lowered.endswith("?") or tokens[0] in {"what", "who", "where", "tell"}:
            _dbg("query detected")
            subject = None
            query_relation = None
            query_verb = None
            if "about" in tokens:
                idx = tokens.index("about")
                _dbg(f"pattern: about @ {idx}")
                subject = _join(tokens_case[idx + 1 :])
            elif tokens[0] == "where" and len(tokens) >= 3 and tokens[1] in COPULAS:
                _dbg("pattern: where is/are <X>")
                subject = _join(tokens_case[2:])
                query_relation = "where_loc"
            elif tokens[0] in {"what", "who"} and "is" in tokens:
                idx = tokens.index("is")
                _dbg(f"pattern: {tokens[0]} is @ {idx}")
                subject = _join(tokens_case[idx + 1 :])
            elif tokens[:3] == ["what", "is", "your"] and len(tokens) >= 4:
                _dbg("pattern: what is your <X>")
                subject = "your " + tokens_case[3]
            elif tokens[:3] == ["what", "is", "my"] and len(tokens) >= 4:
                _dbg("pattern: what is my <X>")
                subject = "my " + tokens_case[3]
            elif tokens[:2] == ["who", "am"] and len(tokens) >= 3:
                _dbg("pattern: who am ...")
                subject = "user"
            elif tokens[:2] == ["who", "are"] and len(tokens) >= 3:
                _dbg("pattern: who are <X>")
                subject = tokens_case[2]
            elif tokens[:2] == ["who", "did"] and len(tokens) >= 4:
                _dbg("pattern: who did <verb> <X>")
                query_relation = "who_did"
                query_verb = tokens_case[2]
                subject = _join(tokens_case[3:])
            elif tokens[0] == "who" and len(tokens) >= 3 and tokens[1] not in COPULAS:
                _dbg("pattern: who <verb> <X>")
                query_relation = "who_did"
                query_verb = tokens_case[1]
                subject = _join(tokens_case[2:])
            else:
                _dbg("pattern: fallback (last token)")
                subject = tokens_case[-1]
            _dbg(f"query subject: {subject!r}")
            _dbg(f"query relation: {query_relation!r}")
            _dbg(f"query verb: {query_verb!r}")
            return ParsedUtterance(
                kind="query",
                subject=subject,
                query_relation=query_relation,
                query_verb=query_verb,
                raw=raw,
            )

        # Existential "There is/are ..." should be handled before generic copular parsing.
        if tokens[0] == "there" and len(tokens) > 2 and tokens[1] in {"is", "are"}:
            _dbg("handling existential 'there is/are' ...")
            remainder_case = tokens_case[2:]
            remainder_lower = tokens[2:]
            _dbg(f"remainder_case: {remainder_case}")
            _dbg(f"remainder_lower: {remainder_lower}")
            if any(token in PREPOSITIONS for token in remainder_lower):
                preps = [t for t in remainder_lower if t in PREPOSITIONS]
                _dbg(f"detected prepositions: {preps}")
                for idx, token in enumerate(remainder_lower):
                    if token in PREPOSITIONS:
                        _dbg(f"using preposition {token!r} @ {idx}")
                        prep = token
                        subject_tokens_case = remainder_case[:idx]
                        subject_tokens_lower = remainder_lower[:idx]
                        object_tokens_case = remainder_case[idx + 1 :]
                        object_tokens_lower = remainder_lower[idx + 1 :]
                        _dbg(f"subject_tokens_case: {subject_tokens_case}")
                        _dbg(f"subject_tokens_lower: {subject_tokens_lower}")
                        _dbg(f"object_tokens_case: {object_tokens_case}")
                        _dbg(f"object_tokens_lower: {object_tokens_lower}")

                        subj_lower = _strip_determiners(subject_tokens_lower)
                        subject, attributes = _head_and_modifiers(subj_lower)
                        if subject and " " in subject:
                            subj_case_tokens = _case_preserve(subject_tokens_case, subject_tokens_lower, subj_lower)
                            subj_case = _join(subj_case_tokens).strip()
                        else:
                            subj_case = _case_for_head(subject_tokens_case, subject_tokens_lower, subject)
                        if subj_case:
                            subject = subj_case

                        obj_lower = _strip_determiners(object_tokens_lower)
                        obj, _ = _head_and_modifiers(obj_lower)
                        if obj and " " in obj:
                            obj_case_tokens = _case_preserve(object_tokens_case, object_tokens_lower, obj_lower)
                            obj_case = _join(obj_case_tokens).strip()
                        else:
                            obj_case = _case_for_head(object_tokens_case, object_tokens_lower, obj)
                        if obj_case:
                            obj = obj_case

                        relations = [(prep, obj)] if obj else []
                        _dbg(f"existential parsed subject: {subject!r}")
                        _dbg(f"existential parsed attributes: {attributes}")
                        _dbg(f"existential parsed relations: {relations}")
                        return ParsedUtterance(
                            kind="statement",
                            subject=subject,
                            attributes=attributes,
                            relations=relations,
                            raw=raw,
                        )

            subj_lower = _strip_determiners(remainder_lower)
            subject, attributes = _head_and_modifiers(subj_lower)
            if subject and " " in subject:
                subj_case_tokens = _case_preserve(remainder_case, remainder_lower, subj_lower)
                subj_case = _join(subj_case_tokens).strip()
            else:
                subj_case = _case_for_head(remainder_case, remainder_lower, subject)
            if subj_case:
                subject = subj_case
            _dbg(f"existential parsed subject (no preposition): {subject!r}")
            _dbg(f"existential parsed attributes (no preposition): {attributes}")
            return ParsedUtterance(kind="statement", subject=subject, attributes=attributes, raw=raw)

        # Passive voice: "<subject> was/were <past participle> (by <agent>) (in <place>)"
        passive_copulas = {"was", "were", "is", "are"}
        irregular_participles = {
            "born",
            "known",
            "made",
            "given",
            "taken",
            "seen",
            "built",
            "written",
            "driven",
            "created",
        }
        for copula in passive_copulas:
            if copula in tokens[1:-1]:
                idx = tokens.index(copula)
                if idx + 1 >= len(tokens):
                    continue
                verb_idx = idx + 1
                if tokens[verb_idx] in {"been", "being"} and verb_idx + 1 < len(tokens):
                    verb_idx += 1
                if verb_idx >= len(tokens):
                    continue
                verb_token = tokens[verb_idx]
                verb_case = tokens_case[verb_idx]
                by_idx = tokens.index("by") if "by" in tokens else -1
                has_by = by_idx > verb_idx
                is_participle = verb_token.endswith("ed") or verb_token in irregular_participles
                if not has_by and not is_participle:
                    continue

                subject_tokens = tokens_case[:idx]
                subject_case = _join(subject_tokens).strip()
                subject = subject_case if subject_case else None

                agent = None
                if has_by:
                    agent_tokens_case = tokens_case[by_idx + 1 :]
                    agent_tokens_lower = tokens[by_idx + 1 :]
                    agent_head, _ = _head_and_modifiers(agent_tokens_lower)
                    agent_case = _case_for_head(agent_tokens_case, agent_tokens_lower, agent_head)
                    agent = agent_case or (agent_head or None)

                loc = None
                for prep in PREPOSITIONS:
                    if prep not in tokens:
                        continue
                    prep_idx = tokens.index(prep)
                    if prep_idx <= verb_idx:
                        continue
                    if prep == "by":
                        continue
                    end_idx = by_idx if has_by and by_idx > prep_idx else len(tokens)
                    loc_tokens_case = tokens_case[prep_idx + 1 : end_idx]
                    loc_tokens_lower = tokens[prep_idx + 1 : end_idx]
                    loc_head, _ = _head_and_modifiers(loc_tokens_lower)
                    loc_case = _case_for_head(loc_tokens_case, loc_tokens_lower, loc_head)
                    loc_obj = loc_case or (loc_head or None)
                    if loc_obj:
                        loc = (prep, loc_obj)
                        break

                _dbg("parsed passive voice")
                _dbg(f"passive subject: {subject!r}")
                _dbg(f"passive verb: {verb_case!r}")
                _dbg(f"passive agent: {agent!r}")
                _dbg(f"passive loc: {loc}")
                return ParsedUtterance(
                    kind="statement",
                    subject=subject,
                    passive_verb=verb_case,
                    passive_agent=agent,
                    passive_patient=subject,
                    passive_loc=loc,
                    raw=raw,
                )

        # Simple active SVO: "<subject> <verb> <object>"
        if len(tokens) >= 3 and tokens[0] not in {"there"}:
            subject_token_count = 1
            if (
                len(tokens) >= 4
                and (tokens[0] in ARTICLES or tokens[0] in DEMONSTRATIVES or tokens[0] in POSSESSIVE_ADJ)
            ):
                subject_token_count = 2

            verb_idx = subject_token_count
            if len(tokens) >= verb_idx + 2 and (
                not any(token in COPULAS for token in tokens[1:])
                and
                tokens[verb_idx] not in COPULAS
                and tokens[verb_idx] not in {"has", "have"}
                and tokens[verb_idx] not in PREPOSITIONS
            ):
                subject_tokens = tokens_case[:subject_token_count]
                subject_tokens_lower = tokens[:subject_token_count]
                verb_token_case = tokens_case[verb_idx]
                object_tokens = tokens_case[verb_idx + 1 :]

                subject_head, _ = _head_and_modifiers(subject_tokens_lower)
                subject_case = _case_for_head(subject_tokens, subject_tokens_lower, subject_head)
                if subject_tokens_lower and subject_tokens_lower[0] in POSSESSIVE_ADJ and subject_case:
                    subject_case = f"{subject_tokens[0]} {subject_case}"
                if not subject_case:
                    subject_case = _join(subject_tokens).strip()

                object_lower = [t.lower() for t in object_tokens]
                obj_head, obj_modifiers = _head_and_modifiers(object_lower)
                obj_case = _case_for_head(object_tokens, object_lower, obj_head)
                obj_modifiers_case = _case_preserve(object_tokens, object_lower, obj_modifiers)

                _dbg("parsed active SVO")
                _dbg(f"active subject: {subject_case!r}")
                _dbg(f"active verb: {verb_token_case!r}")
                _dbg(f"active object: {obj_case!r}")
                return ParsedUtterance(
                    kind="statement",
                    subject=subject_case,
                    active_verb=verb_token_case,
                    active_object=obj_case,
                    active_object_modifiers=obj_modifiers_case,
                    raw=raw,
                )

        for copula in COPULAS:
            if copula in tokens[1:-1]:
                idx = tokens.index(copula)
                _dbg(f"copula detected: {copula!r} @ {idx}")
                subject_tokens = tokens_case[:idx]
                subject_tokens_lower = tokens[:idx]
                predicate_tokens = tokens_case[idx + 1 :]
                predicate_tokens_lower = tokens[idx + 1 :]
                _dbg(f"subject_tokens_case: {subject_tokens}")
                _dbg(f"subject_tokens_lower: {subject_tokens_lower}")
                _dbg(f"predicate_tokens_case: {predicate_tokens}")
                _dbg(f"predicate_tokens_lower: {predicate_tokens_lower}")

                had_article = False
                if predicate_tokens_lower and predicate_tokens_lower[0] in ARTICLES:
                    had_article = True
                    _dbg(f"predicate article: {predicate_tokens_lower[0]!r} (definition candidate)")
                    predicate_tokens = predicate_tokens[1:]
                    predicate_tokens_lower = predicate_tokens_lower[1:]

                if predicate_tokens_lower and predicate_tokens_lower[0] in DEMONSTRATIVES:
                    predicate_tokens = predicate_tokens[1:]
                    predicate_tokens_lower = predicate_tokens_lower[1:]
                    _dbg("stripped demonstrative from predicate")

                if predicate_tokens_lower and predicate_tokens_lower[0] in INTERROGATIVE_ADJ:
                    predicate_tokens = predicate_tokens[1:]
                    predicate_tokens_lower = predicate_tokens_lower[1:]
                    _dbg("stripped interrogative adjective from predicate")

                if predicate_tokens_lower and predicate_tokens_lower[0] in POSSESSIVE_ADJ:
                    predicate_tokens = predicate_tokens[1:]
                    predicate_tokens_lower = predicate_tokens_lower[1:]
                    _dbg("stripped possessive adjective from predicate")

                subject_lower = _strip_determiners(subject_tokens_lower)
                subject, subject_modifiers = _head_and_modifiers(subject_lower)
                _dbg(f"subject head: {subject!r}")
                _dbg(f"subject modifiers: {subject_modifiers}")
                if not subject and any(token in DEMONSTRATIVES for token in subject_tokens_lower):
                    for token_case, token_lower in zip(subject_tokens, subject_tokens_lower):
                        if token_lower in DEMONSTRATIVES:
                            subject = token_case
                            _dbg(f"subject fallback to demonstrative: {subject!r}")
                            break
                subject_case: Optional[str] = None
                if subject and " " in subject:
                    subject_case_tokens = _case_preserve(subject_tokens, subject_tokens_lower, subject_lower)
                    subject_case = _join(subject_case_tokens).strip()
                if subject_case:
                    subject = subject_case
                    _dbg(f"subject case-preserved: {subject!r}")
                possessor_case = None
                if subject_tokens_lower and subject_tokens_lower[0] in POSSESSIVE_ADJ:
                    possessor_case = subject_tokens[0]
                if possessor_case and subject:
                    if " " in subject:
                        subject = f"{possessor_case} {subject}"
                    else:
                        head_case = _case_for_head(
                            subject_tokens[1:],
                            subject_tokens_lower[1:],
                            subject.lower() if subject else None,
                        )
                        subject = f"{possessor_case} {head_case or subject}"
                    _dbg(f"subject possessive: {subject!r}")
                elif subject and not subject_case:
                    subject_case = _case_for_head(subject_tokens, subject_tokens_lower, subject.lower())
                    if subject_case:
                        subject = subject_case
                        _dbg(f"subject head case: {subject!r}")

                relations: List[Tuple[str, str]] = []
                temporal_tokens = [token for token in predicate_tokens_lower if token in TEMPORAL_ADVERBS]
                if temporal_tokens:
                    _dbg(f"temporal tokens: {temporal_tokens}")
                    if "currently" in temporal_tokens or "now" in temporal_tokens:
                        relations.append(("when", "#NOW"))
                    predicate_tokens = [
                        token
                        for token, token_lower in zip(predicate_tokens, predicate_tokens_lower)
                        if token_lower not in TEMPORAL_ADVERBS
                    ]
                    predicate_tokens_lower = [t for t in predicate_tokens_lower if t not in TEMPORAL_ADVERBS]
                    _dbg(f"predicate_tokens_case (after temporal strip): {predicate_tokens}")
                    _dbg(f"predicate_tokens_lower (after temporal strip): {predicate_tokens_lower}")

                if any(token in PREPOSITIONS for token in predicate_tokens_lower):
                    _dbg("predicate contains preposition(s)")
                    for prep in PREPOSITIONS:
                        if prep in predicate_tokens_lower:
                            prep_idx = predicate_tokens_lower.index(prep)
                            _dbg(f"using preposition {prep!r} @ {prep_idx}")
                            left_tokens = predicate_tokens[:prep_idx]
                            right_tokens = predicate_tokens[prep_idx + 1 :]
                            left_lower = [t.lower() for t in left_tokens]
                            right_lower = [t.lower() for t in right_tokens]
                            _dbg(f"left_tokens_case: {left_tokens}")
                            _dbg(f"left_tokens_lower: {left_lower}")
                            _dbg(f"right_tokens_case: {right_tokens}")
                            _dbg(f"right_tokens_lower: {right_lower}")

                            obj_lower = _strip_determiners(left_lower)
                            obj, obj_modifiers = _head_and_modifiers(obj_lower)
                            if obj and " " in obj:
                                obj_case_tokens = _case_preserve(left_tokens, left_lower, obj_lower)
                                obj_case = _join(obj_case_tokens).strip()
                                if obj_case:
                                    obj = obj_case
                            else:
                                obj_case = _case_for_head(left_tokens, left_lower, obj.lower() if obj else None)
                                if obj_case:
                                    obj = obj_case

                            loc_lower = _strip_determiners(right_lower)
                            loc, _ = _head_and_modifiers(loc_lower)
                            if loc and " " in loc:
                                loc_case_tokens = _case_preserve(right_tokens, right_lower, loc_lower)
                                loc_case = _join(loc_case_tokens).strip()
                                if loc_case:
                                    loc = loc_case
                            else:
                                loc_case = _case_for_head(right_tokens, right_lower, loc.lower() if loc else None)
                                if loc_case:
                                    loc = loc_case

                            if loc:
                                relations.append((prep, loc))
                            attributes = [mod for mod in subject_modifiers if mod] + [mod for mod in obj_modifiers if mod]
                            _dbg(f"relations: {relations}")
                            _dbg(f"attributes: {attributes}")
                            if obj and had_article:
                                _dbg("returning kind=definition (had_article + prepositional)")
                                return ParsedUtterance(
                                    kind="definition",
                                    subject=subject,
                                    obj=obj,
                                    attributes=attributes,
                                    relations=relations,
                                    raw=raw,
                                )
                            _dbg("returning kind=statement (prepositional)")
                            return ParsedUtterance(
                                kind="statement",
                                subject=subject,
                                obj=obj,
                                attributes=attributes,
                                relations=relations,
                                raw=raw,
                            )

                if had_article:
                    _dbg("had_article without preposition -> returning kind=definition")
                    obj_lower = _strip_determiners([t.lower() for t in predicate_tokens])
                    obj, obj_modifiers = _head_and_modifiers(obj_lower)
                    if obj and " " in obj:
                        obj_case_tokens = _case_preserve(predicate_tokens, [t.lower() for t in predicate_tokens], obj_lower)
                        obj_case = _join(obj_case_tokens).strip()
                        if obj_case:
                            obj = obj_case
                    else:
                        obj_case = _case_for_head(
                            predicate_tokens,
                            [t.lower() for t in predicate_tokens],
                            obj.lower() if obj else None,
                        )
                        if obj_case:
                            obj = obj_case
                    return ParsedUtterance(
                        kind="definition",
                        subject=subject,
                        obj=obj,
                        attributes=obj_modifiers,
                        raw=raw,
                    )

                _dbg("returning kind=statement (copular)")
                obj_lower = _strip_determiners([t.lower() for t in predicate_tokens])
                obj, obj_modifiers = _head_and_modifiers(obj_lower)
                if obj and " " in obj:
                    obj_case_tokens = _case_preserve(predicate_tokens, [t.lower() for t in predicate_tokens], obj_lower)
                    obj_case = _join(obj_case_tokens).strip()
                    if obj_case:
                        obj = obj_case
                else:
                    obj_case = _case_for_head(
                        predicate_tokens,
                        [t.lower() for t in predicate_tokens],
                        obj.lower() if obj else None,
                    )
                    if obj_case:
                        obj = obj_case
                return ParsedUtterance(
                    kind="statement",
                    subject=subject,
                    obj=obj,
                    attributes=subject_modifiers,
                    raw=raw,
                )

        if "has" in tokens or "have" in tokens:
            verb = "has" if "has" in tokens else "have"
            idx = tokens.index(verb)
            _dbg(f"possession verb detected: {verb!r} @ {idx}")
            subject_tokens = tokens_case[:idx]
            object_tokens = tokens_case[idx + 1 :]
            subject, attributes = _head_and_modifiers([t.lower() for t in subject_tokens])
            obj, obj_modifiers = _head_and_modifiers([t.lower() for t in object_tokens])
            subject_case = _join(subject_tokens).strip()
            obj_case = _case_for_head(
                object_tokens,
                [t.lower() for t in object_tokens],
                obj.lower() if obj else None,
            )
            obj_modifiers_case = _case_preserve(
                object_tokens,
                [t.lower() for t in object_tokens],
                obj_modifiers,
            )
            if subject_case and subject_case.lower() != subject:
                subject = subject_case
            if obj_case:
                obj = obj_case
            relations = [("have", obj)] if obj else []
            relation_modifiers = [obj_modifiers_case] if obj else []
            _dbg(f"parsed subject: {subject!r}")
            _dbg(f"parsed attributes: {attributes}")
            _dbg(f"parsed relations: {relations}")
            return ParsedUtterance(
                kind="statement",
                subject=subject,
                attributes=attributes,
                relations=relations,
                relation_modifiers=relation_modifiers,
                raw=raw,
            )

        _dbg("no pattern matched -> kind=freeform")
        return ParsedUtterance(kind="freeform", raw=raw)
