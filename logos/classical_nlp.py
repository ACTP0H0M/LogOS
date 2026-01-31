from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Dict, List, Optional


@dataclass(frozen=True)
class TokenAnalysis:
    text: str
    lemma: str
    upos: str
    xpos: str
    head: int
    deprel: str


@dataclass(frozen=True)
class SentenceAnalysis:
    tokens: List[TokenAnalysis]
    constituency: Optional[str] = None


@dataclass(frozen=True)
class DocumentAnalysis:
    sentences: List[SentenceAnalysis]
    source: str

    def to_dict(self) -> Dict[str, Any]:
        return {
            "source": self.source,
            "sentences": [
                {
                    "tokens": [
                        {
                            "text": tok.text,
                            "lemma": tok.lemma,
                            "upos": tok.upos,
                            "xpos": tok.xpos,
                            "head": tok.head,
                            "deprel": tok.deprel,
                        }
                        for tok in sent.tokens
                    ],
                    "constituency": sent.constituency,
                }
                for sent in self.sentences
            ],
        }


class ClassicalNLP:
    """Optional classical NLP frontend (StanfordNLP/Stanza).

    This module is intentionally optional: if `stanza` or its models are not
    installed, LogOS should still run using heuristic parsing.
    """

    def __init__(self) -> None:
        self._pipeline = None
        self._pipeline_error: Optional[str] = None

    def available(self) -> bool:
        self._ensure_pipeline()
        return self._pipeline is not None

    def analyze(self, text: str) -> Optional[DocumentAnalysis]:
        self._ensure_pipeline()
        if not self._pipeline:
            return None
        try:
            doc = self._pipeline(text)
        except Exception:
            return None

        sentences: List[SentenceAnalysis] = []
        for sent in getattr(doc, "sentences", []) or []:
            tokens: List[TokenAnalysis] = []
            for word in getattr(sent, "words", []) or []:
                tokens.append(
                    TokenAnalysis(
                        text=getattr(word, "text", ""),
                        lemma=getattr(word, "lemma", getattr(word, "text", "")),
                        upos=getattr(word, "upos", ""),
                        xpos=getattr(word, "xpos", ""),
                        head=int(getattr(word, "head", 0)),
                        deprel=getattr(word, "deprel", ""),
                    )
                )
            constituency = None
            try:
                constituency = str(getattr(sent, "constituency", None) or None)
            except Exception:
                constituency = None
            sentences.append(SentenceAnalysis(tokens=tokens, constituency=constituency))
        return DocumentAnalysis(sentences=sentences, source="stanza")

    def _ensure_pipeline(self) -> None:
        if self._pipeline is not None or self._pipeline_error is not None:
            return
        try:
            import stanza  # type: ignore
        except Exception as exc:
            self._pipeline_error = f"stanza import failed: {exc}"
            return

        try:
            # This will fail cleanly if the English models are not installed yet.
            self._pipeline = stanza.Pipeline(
                lang="en",
                processors="tokenize,pos,lemma,depparse,constituency",
                tokenize_no_ssplit=False,
                use_gpu=False,
                verbose=False,
            )
        except Exception as exc:
            self._pipeline_error = f"stanza pipeline init failed: {exc}"

