from __future__ import annotations

from dataclasses import dataclass, field
from typing import List, Optional, Tuple

STOP_WORDS = {"a", "an", "the", "to", "of", "and", "in", "on", "for", "with", "there"}
ARTICLES = {"a", "an", "the"}
COPULAS = {"is", "are", "was", "were", "be", "been"}
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


@dataclass
class ParsedUtterance:
    kind: str
    subject: Optional[str] = None
    obj: Optional[str] = None
    attributes: List[str] = field(default_factory=list)
    relations: List[Tuple[str, str]] = field(default_factory=list)
    raw: str = ""


def tokenize(text: str) -> List[str]:
    return [token.strip(".,!?;:").lower() for token in text.split() if token.strip(".,!?;:")]


def _strip_articles(tokens: List[str]) -> List[str]:
    return [token for token in tokens if token not in ARTICLES]


def _head_and_modifiers(tokens: List[str]) -> Tuple[Optional[str], List[str]]:
    cleaned = _strip_articles(tokens)
    if not cleaned:
        return None, []
    head = cleaned[-1]
    modifiers = cleaned[:-1]
    return head, modifiers


def _join(tokens: List[str]) -> str:
    return " ".join(tokens).strip()


def parse_utterance(text: str) -> ParsedUtterance:
    raw = text.strip()
    tokens = tokenize(raw)
    if not tokens:
        return ParsedUtterance(kind="empty", raw=raw)

    lowered = raw.lower()

    if lowered.endswith("?") or tokens[0] in {"what", "who", "where", "tell"}:
        subject = None
        if "about" in tokens:
            idx = tokens.index("about")
            subject = _join(tokens[idx + 1 :])
        elif tokens[0] in {"what", "who"} and "is" in tokens:
            idx = tokens.index("is")
            subject = _join(tokens[idx + 1 :])
        elif tokens[:2] == ["who", "am"] and len(tokens) >= 3:
            subject = "user"
        else:
            subject = tokens[-1]
        return ParsedUtterance(kind="query", subject=subject, raw=raw)

    if " is a " in lowered or " is an " in lowered:
        left, _, right = lowered.partition(" is ")
        subject = left.strip()
        obj = right.replace("a ", "").replace("an ", "").strip()
        return ParsedUtterance(kind="definition", subject=subject, obj=obj, raw=raw)

    if tokens[0] == "there" and len(tokens) > 2 and tokens[1] in {"is", "are"}:
        remainder = tokens[2:]
        relation = None
        if any(token in PREPOSITIONS for token in remainder):
            for idx, token in enumerate(remainder):
                if token in PREPOSITIONS:
                    relation = token
                    subject_tokens = remainder[:idx]
                    object_tokens = remainder[idx + 1 :]
                    subject, attributes = _head_and_modifiers(subject_tokens)
                    obj, _ = _head_and_modifiers(object_tokens)
                    relations = [(relation, obj)] if obj else []
                    return ParsedUtterance(
                        kind="statement",
                        subject=subject,
                        attributes=attributes,
                        relations=relations,
                        raw=raw,
                    )
        subject, attributes = _head_and_modifiers(remainder)
        return ParsedUtterance(kind="statement", subject=subject, attributes=attributes, raw=raw)

    for copula in COPULAS:
        if copula in tokens[1:-1]:
            idx = tokens.index(copula)
            subject_tokens = tokens[:idx]
            predicate_tokens = tokens[idx + 1 :]
            subject, attributes = _head_and_modifiers(subject_tokens)
            obj, obj_modifiers = _head_and_modifiers(predicate_tokens)
            if obj and obj_modifiers:
                return ParsedUtterance(
                    kind="statement",
                    subject=subject,
                    obj=obj,
                    attributes=attributes,
                    relations=[("has_property", modifier) for modifier in obj_modifiers],
                    raw=raw,
                )
            return ParsedUtterance(
                kind="statement",
                subject=subject,
                obj=obj,
                attributes=attributes,
                raw=raw,
            )

    if "has" in tokens or "have" in tokens:
        verb = "has" if "has" in tokens else "have"
        idx = tokens.index(verb)
        subject_tokens = tokens[:idx]
        object_tokens = tokens[idx + 1 :]
        subject, attributes = _head_and_modifiers(subject_tokens)
        obj, _ = _head_and_modifiers(object_tokens)
        relations = [("has", obj)] if obj else []
        return ParsedUtterance(
            kind="statement",
            subject=subject,
            attributes=attributes,
            relations=relations,
            raw=raw,
        )

    return ParsedUtterance(kind="freeform", raw=raw)

