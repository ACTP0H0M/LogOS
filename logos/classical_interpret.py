from __future__ import annotations

from typing import Optional

from .classical_nlp import DocumentAnalysis, TokenAnalysis
from .nlp import ParsedUtterance


def interpret(text: str, analysis: DocumentAnalysis) -> Optional[ParsedUtterance]:
    """Convert a classical parse (POS + dependency tree) into a coarse intent.

    This is intentionally conservative: it only handles a few high-signal
    constructions reliably, and otherwise returns None to fall back to the
    heuristic parser.
    """

    raw = text.strip()
    if not analysis.sentences:
        return None
    sent = analysis.sentences[0]
    if not sent.tokens:
        return None

    lower_tokens = [tok.text.lower() for tok in sent.tokens]
    is_question = raw.endswith("?")

    if is_question and lower_tokens[:4] == ["what", "do", "you", "know"]:
        return ParsedUtterance(kind="query", subject="__meta__", raw=raw)

    return _interpret_copular(raw, sent.tokens)


def _interpret_copular(raw: str, tokens: list[TokenAnalysis]) -> Optional[ParsedUtterance]:
    # Find root (head == 0 is how stanza encodes it).
    root_idx = None
    for idx, tok in enumerate(tokens, start=1):
        if tok.head == 0:
            root_idx = idx
            break
    if root_idx is None:
        return None

    root = tokens[root_idx - 1]
    # Find nominal subject.
    subj_idx = None
    for idx, tok in enumerate(tokens, start=1):
        if tok.head == root_idx and tok.deprel in {"nsubj", "nsubj:pass"}:
            subj_idx = idx
            break
    if subj_idx is None:
        return None
    subject = tokens[subj_idx - 1].text

    # "a/an" determiner on the predicate noun is a strong signal of definition.
    had_indef_article = False
    for idx, tok in enumerate(tokens, start=1):
        if tok.head == root_idx and tok.deprel == "det" and tok.text.lower() in {"a", "an"}:
            had_indef_article = True
            break

    # Gather predicate modifiers (amod on root).
    attributes = [tok.text.lower() for tok in tokens if tok.head == root_idx and tok.deprel == "amod"]

    # Basic prepositional attachment: (root) --obl/nmod--> (obj), with a "case" child for the preposition.
    relations = []
    for idx, tok in enumerate(tokens, start=1):
        if tok.head == root_idx and tok.deprel in {"obl", "nmod"}:
            prep = None
            for case_tok in tokens:
                if case_tok.head == idx and case_tok.deprel == "case":
                    prep = case_tok.text.lower()
                    break
            if prep:
                relations.append((prep, tok.text))

    if root.upos in {"NOUN", "PROPN"} and had_indef_article:
        return ParsedUtterance(kind="definition", subject=subject, obj=root.text, attributes=attributes, relations=relations, raw=raw)
    if root.upos in {"ADJ"}:
        return ParsedUtterance(kind="statement", subject=subject, obj=root.text, attributes=attributes, relations=relations, raw=raw)
    return None

