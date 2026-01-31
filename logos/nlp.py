from __future__ import annotations

from dataclasses import dataclass
from typing import List, Optional

STOP_WORDS = {"a", "an", "the", "to", "of", "and", "in", "on", "for", "with"}


@dataclass(frozen=True)
class ParsedIntent:
    kind: str
    subject: Optional[str] = None
    relation: Optional[str] = None
    obj: Optional[str] = None


def tokenize(text: str) -> List[str]:
    return [token.strip(".,!?;:").lower() for token in text.split() if token.strip(".,!?;:")]


def extract_intent(text: str) -> ParsedIntent:
    lowered = text.strip().lower()

    if lowered.startswith("my name is "):
        return ParsedIntent(kind="introduce", subject="user", relation="name", obj=text.strip()[11:])

    if lowered.startswith("i am "):
        return ParsedIntent(kind="state", subject="user", relation="is", obj=text.strip()[5:])

    if lowered.startswith("i like "):
        return ParsedIntent(kind="preference", subject="user", relation="likes", obj=text.strip()[7:])

    if lowered.startswith("remember "):
        payload = text.strip()[9:]
        return ParsedIntent(kind="remember", subject="user", relation="notes", obj=payload)

    if lowered.startswith("what do you know about "):
        subject = text.strip()[23:]
        return ParsedIntent(kind="query", subject=subject)

    if lowered in {"who am i", "who am i?"}:
        return ParsedIntent(kind="query", subject="user")

    tokens = [token for token in tokenize(text) if token not in STOP_WORDS]
    if len(tokens) >= 3 and tokens[0] == "i":
        return ParsedIntent(kind="state", subject="user", relation=tokens[1], obj=" ".join(tokens[2:]))

    return ParsedIntent(kind="freeform")
