from __future__ import annotations

from dataclasses import dataclass
from typing import Dict

from .hypergraph import Hypergraph
from .memory import Fact, Memory
from .nlp import ParsedIntent, extract_intent
from .reasoner import Reasoner
from .state import ConversationState


@dataclass
class ChatResponse:
    reply: str
    facts: Dict[str, str]


class ChatEngine:
    """Coordinates parsing, memory updates, and symbolic reasoning."""

    def __init__(self) -> None:
        self._state = ConversationState()
        self._memory = Memory()
        self._graph = Hypergraph()
        self._reasoner = Reasoner(self._memory, self._state)

    def process(self, message: str) -> ChatResponse:
        intent = extract_intent(message)
        if intent.kind in {"introduce", "state", "preference", "remember"}:
            reply = self._handle_fact(intent)
        elif intent.kind == "query" and intent.subject:
            reply = self._reasoner.respond_to_query(intent.subject)
        elif intent.kind == "freeform":
            reply = self._handle_freeform(message)
        else:
            reply = self._reasoner.small_talk()

        return ChatResponse(reply=reply, facts=self._facts_snapshot())

    def _handle_fact(self, intent: ParsedIntent) -> str:
        subject = intent.subject or "user"
        relation = intent.relation or "says"
        obj = intent.obj or ""
        fact = Fact(subject=subject, relation=relation, obj=obj)
        self._memory.add_fact(fact)
        self._graph.add_edge(relation, [subject, obj])

        if relation == "name" and subject == "user":
            self._state.user_name = obj
            return f"Nice to meet you, {obj}."
        if relation == "likes":
            self._state.remember_topic(obj)
            return f"I will remember that you like {obj}."
        if relation == "is" and subject == "user":
            self._state.mood = obj
            return f"Thanks for sharing that you are {obj}."
        return "Got it. I have stored that as a symbolic fact."

    def _handle_freeform(self, message: str) -> str:
        tokens = [token for token in message.split() if token]
        if tokens:
            self._state.remember_topic(tokens[-1])
        return "I am tracking the topic. You can tell me a fact or ask what I know about something."

    def _facts_snapshot(self) -> Dict[str, str]:
        return {f"{fact.subject}:{index}": f"{fact.relation} {fact.obj}" for index, fact in enumerate(self._memory.all_facts())}
