from __future__ import annotations

from dataclasses import dataclass, field
from typing import Dict, Iterable, List


@dataclass(frozen=True)
class Node:
    label: str


@dataclass
class Edge:
    relation: str
    nodes: List[Node] = field(default_factory=list)
    confidence: float = 1.0


class Hypergraph:
    """Lightweight hypergraph for symbolic relations."""

    def __init__(self) -> None:
        self._nodes: Dict[str, Node] = {}
        self._edges: List[Edge] = []

    def node(self, label: str) -> Node:
        key = label.strip().lower()
        if key not in self._nodes:
            self._nodes[key] = Node(label=label.strip())
        return self._nodes[key]

    def add_edge(self, relation: str, node_labels: Iterable[str], confidence: float = 1.0) -> Edge:
        nodes = [self.node(label) for label in node_labels]
        edge = Edge(relation=relation, nodes=nodes, confidence=confidence)
        self._edges.append(edge)
        return edge

    def edges(self) -> List[Edge]:
        return list(self._edges)
