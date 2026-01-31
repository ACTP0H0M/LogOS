from __future__ import annotations

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

def tokenize_with_case(text: str) -> List[str]:
    return [token.strip(".,!?;:") for token in text.split() if token.strip(".,!?;:")]


def _strip_articles(tokens: List[str]) -> List[str]:
    return [token for token in tokens if token not in ARTICLES]

def _strip_determiners(tokens: List[str]) -> List[str]:
    return [
        token
        for token in tokens
        if token not in ARTICLES
        and token not in DEMONSTRATIVES
        and token not in INTERROGATIVE_ADJ
        and token not in POSSESSIVE_ADJ
    ]

def _case_preserve(tokens_case: List[str], tokens_lower: List[str], filtered_lower: List[str]) -> List[str]:
    if not filtered_lower:
        return []
    result: List[str] = []
    index = 0
    for token_case, token_lower in zip(tokens_case, tokens_lower):
        if index >= len(filtered_lower):
            break
        if token_lower == filtered_lower[index]:
            result.append(token_case)
            index += 1
    return result

def _case_for_head(tokens_case: List[str], tokens_lower: List[str], head_lower: Optional[str]) -> Optional[str]:
    if not head_lower:
        return None
    for token_case, token_lower in zip(reversed(tokens_case), reversed(tokens_lower)):
        if token_lower == head_lower:
            return token_case
    return head_lower


def _head_and_modifiers(tokens: List[str]) -> Tuple[Optional[str], List[str]]:
    cleaned = _strip_determiners(tokens)
    if not cleaned:
        return None, []
    if "of" in cleaned and len(cleaned) > 1:
        return " ".join(cleaned), []
    head = cleaned[-1]
    modifiers = cleaned[:-1]
    return head, modifiers


def _join(tokens: List[str]) -> str:
    return " ".join(tokens).strip()


def parse_utterance(text: str) -> ParsedUtterance:
    raw = text.strip()
    tokens_case = tokenize_with_case(raw)
    tokens = [token.lower() for token in tokens_case]
    if not tokens:
        return ParsedUtterance(kind="empty", raw=raw)

    lowered = raw.lower()
    if any(phrase in lowered for phrase in CORRECTION_PHRASES):
        return ParsedUtterance(kind="correction", raw=raw)

    if lowered.endswith("?") or tokens[0] in {"what", "who", "where", "tell"}:
        subject = None
        if "about" in tokens:
            idx = tokens.index("about")
            subject = _join(tokens_case[idx + 1 :])
        elif tokens[0] in {"what", "who"} and "is" in tokens:
            idx = tokens.index("is")
            subject = _join(tokens_case[idx + 1 :])
        elif tokens[:3] == ["what", "is", "your"] and len(tokens) >= 4:
            subject = "your " + tokens_case[3]
        elif tokens[:3] == ["what", "is", "my"] and len(tokens) >= 4:
            subject = "my " + tokens_case[3]
        elif tokens[:2] == ["who", "am"] and len(tokens) >= 3:
            subject = "user"
        elif tokens[:2] == ["who", "are"] and len(tokens) >= 3:
            subject = tokens_case[2]
        else:
            subject = tokens_case[-1]
        return ParsedUtterance(kind="query", subject=subject, raw=raw)

    for copula in COPULAS:
        if copula in tokens[1:-1]:
            idx = tokens.index(copula)
            subject_tokens = tokens_case[:idx]
            subject_tokens_lower = tokens[:idx]
            predicate_tokens = tokens_case[idx + 1 :]
            predicate_tokens_lower = tokens[idx + 1 :]
            had_article = False
            if predicate_tokens_lower and predicate_tokens_lower[0] in ARTICLES:
                had_article = True
                predicate_tokens = predicate_tokens[1:]
                predicate_tokens_lower = predicate_tokens_lower[1:]

            if predicate_tokens_lower and predicate_tokens_lower[0] in DEMONSTRATIVES:
                predicate_tokens = predicate_tokens[1:]
                predicate_tokens_lower = predicate_tokens_lower[1:]

            if predicate_tokens_lower and predicate_tokens_lower[0] in INTERROGATIVE_ADJ:
                predicate_tokens = predicate_tokens[1:]
                predicate_tokens_lower = predicate_tokens_lower[1:]

            if predicate_tokens_lower and predicate_tokens_lower[0] in POSSESSIVE_ADJ:
                predicate_tokens = predicate_tokens[1:]
                predicate_tokens_lower = predicate_tokens_lower[1:]

            subject_lower = _strip_determiners(subject_tokens_lower)
            subject, subject_modifiers = _head_and_modifiers(subject_lower)
            if not subject and any(token in DEMONSTRATIVES for token in subject_tokens_lower):
                for token_case, token_lower in zip(subject_tokens, subject_tokens_lower):
                    if token_lower in DEMONSTRATIVES:
                        subject = token_case
                        break
            if subject and " " in subject:
                subject_case_tokens = _case_preserve(subject_tokens, subject_tokens_lower, subject_lower)
                subject_case = _join(subject_case_tokens).strip()
            if subject_case:
                subject = subject_case
            possessor_case = None
            if subject_tokens_lower and subject_tokens_lower[0] in POSSESSIVE_ADJ:
                possessor_case = subject_tokens[0]
            if possessor_case and subject:
                head_case = _case_for_head(
                    subject_tokens[1:],
                    subject_tokens_lower[1:],
                    subject.lower() if subject else None,
                )
                if head_case:
                    subject = f"{possessor_case} {head_case}"
                else:
                    subject = f"{possessor_case} {subject}"
            else:
                subject_case = _case_for_head(subject_tokens, subject_tokens_lower, subject.lower() if subject else None)
                if subject_case:
                    subject = subject_case

            relations: List[Tuple[str, str]] = []
            temporal_tokens = [token for token in predicate_tokens_lower if token in TEMPORAL_ADVERBS]
            if temporal_tokens:
                if "currently" in temporal_tokens or "now" in temporal_tokens:
                    relations.append(("when", "#NOW"))
                predicate_tokens = [
                    token
                    for token, token_lower in zip(predicate_tokens, predicate_tokens_lower)
                    if token_lower not in TEMPORAL_ADVERBS
                ]
                predicate_tokens_lower = [t for t in predicate_tokens_lower if t not in TEMPORAL_ADVERBS]

            if any(token in PREPOSITIONS for token in predicate_tokens_lower):
                for prep in PREPOSITIONS:
                    if prep in predicate_tokens_lower:
                        prep_idx = predicate_tokens_lower.index(prep)
                        left_tokens = predicate_tokens[:prep_idx]
                        right_tokens = predicate_tokens[prep_idx + 1 :]
                        left_lower = [t.lower() for t in left_tokens]
                        right_lower = [t.lower() for t in right_tokens]
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
                        attributes = [mod for mod in obj_modifiers if mod]
                        if obj and had_article:
                            return ParsedUtterance(
                                kind="definition",
                                subject=subject,
                                obj=obj,
                                attributes=attributes,
                                relations=relations,
                                raw=raw,
                            )
                        return ParsedUtterance(
                            kind="statement",
                            subject=subject,
                            obj=obj,
                            attributes=attributes,
                            relations=relations,
                            raw=raw,
                        )

            if had_article:
                obj_lower = _strip_determiners([t.lower() for t in predicate_tokens])
                obj, obj_modifiers = _head_and_modifiers(obj_lower)
                if obj and " " in obj:
                    obj_case_tokens = _case_preserve(predicate_tokens, [t.lower() for t in predicate_tokens], obj_lower)
                    obj_case = _join(obj_case_tokens).strip()
                    if obj_case:
                        obj = obj_case
                else:
                    obj_case = _case_for_head(predicate_tokens, [t.lower() for t in predicate_tokens], obj.lower() if obj else None)
                    if obj_case:
                        obj = obj_case
                return ParsedUtterance(
                    kind="definition",
                    subject=subject,
                    obj=obj,
                    attributes=obj_modifiers,
                    raw=raw,
                )
            obj_lower = _strip_determiners([t.lower() for t in predicate_tokens])
            obj, obj_modifiers = _head_and_modifiers(obj_lower)
            if obj and " " in obj:
                obj_case_tokens = _case_preserve(predicate_tokens, [t.lower() for t in predicate_tokens], obj_lower)
                obj_case = _join(obj_case_tokens).strip()
                if obj_case:
                    obj = obj_case
            else:
                obj_case = _case_for_head(predicate_tokens, [t.lower() for t in predicate_tokens], obj.lower() if obj else None)
                if obj_case:
                    obj = obj_case
            return ParsedUtterance(
                kind="statement",
                subject=subject,
                obj=obj,
                attributes=subject_modifiers,
                raw=raw,
            )

    if tokens[0] == "there" and len(tokens) > 2 and tokens[1] in {"is", "are"}:
        remainder = tokens_case[2:]
        remainder_lower = tokens[2:]
        relation = None
        if any(token in PREPOSITIONS for token in remainder_lower):
            for idx, token in enumerate(remainder_lower):
                if token in PREPOSITIONS:
                    relation = token
                    subject_tokens = remainder[:idx]
                    object_tokens = remainder[idx + 1 :]
                    subject, attributes = _head_and_modifiers([t.lower() for t in subject_tokens])
                    obj, _ = _head_and_modifiers([t.lower() for t in object_tokens])
                    subject_case = _join(subject_tokens).strip()
                    obj_case = _join(object_tokens).strip()
                    if subject_case and subject_case.lower() != subject:
                        subject = subject_case
                    if obj_case and obj_case.lower() != obj:
                        obj = obj_case
                    relations = [(relation, obj)] if obj else []
                    return ParsedUtterance(
                        kind="statement",
                        subject=subject,
                        attributes=attributes,
                        relations=relations,
                        raw=raw,
                    )
        subject, attributes = _head_and_modifiers([t.lower() for t in remainder])
        subject_case = _join(remainder).strip()
        if subject_case and subject_case.lower() != subject:
            subject = subject_case
        return ParsedUtterance(kind="statement", subject=subject, attributes=attributes, raw=raw)

    if "has" in tokens or "have" in tokens:
        verb = "has" if "has" in tokens else "have"
        idx = tokens.index(verb)
        subject_tokens = tokens_case[:idx]
        object_tokens = tokens_case[idx + 1 :]
        subject, attributes = _head_and_modifiers([t.lower() for t in subject_tokens])
        obj, _ = _head_and_modifiers([t.lower() for t in object_tokens])
        subject_case = _join(subject_tokens).strip()
        obj_case = _join(object_tokens).strip()
        if subject_case and subject_case.lower() != subject:
            subject = subject_case
        if obj_case and obj_case.lower() != obj:
            obj = obj_case
        relations = [("have", obj)] if obj else []
        return ParsedUtterance(
            kind="statement",
            subject=subject,
            attributes=attributes,
            relations=relations,
            raw=raw,
        )

    return ParsedUtterance(kind="freeform", raw=raw)
