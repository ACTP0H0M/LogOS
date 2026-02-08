from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Tuple


def _normalize(label: str) -> str:
    return label.strip().lower()


@dataclass
class Symbol:
    id: int
    name: str
    kind: str = "unknown"
    description: str = ""
    aliases: List[str] = field(default_factory=list)


@dataclass
class Link:
    id: int
    source: int
    target: int
    relation: str
    generality: float = 0.0
    actuality: float = 1.0
    created_at: str = ""
    evidence: List[int] = field(default_factory=list)


@dataclass
class Branch:
    id: int
    logos: List[int] = field(default_factory=list)
    links: List[int] = field(default_factory=list)


class KnowledgeBase:
    """Symbolic hypergraph with lightweight ontology."""

    def __init__(self) -> None:
        self._symbols_by_id: Dict[int, Symbol] = {}
        self._symbols_by_name: Dict[str, Symbol] = {}
        self._links_by_id: Dict[int, Link] = {}
        self._links_from: Dict[int, List[int]] = {}
        self._links_to: Dict[int, List[int]] = {}
        self._branches_by_id: Dict[int, Branch] = {}
        self._next_symbol_id = 0
        self._next_link_id = 0
        self._next_branch_id = 0
        self._category_ids: Dict[str, int] = {}
        self._relation_names: List[str] = []
        self._ensure_base_categories()
        self._ensure_special_symbols()
        self._ensure_relation_ontology()

    @staticmethod
    def _encode_branch_id(branch_id: int) -> int:
        return -1 - branch_id

    @staticmethod
    def _decode_branch_id(node_id: int) -> Optional[int]:
        if node_id < 0:
            return -1 - node_id
        return None

    def branch_node_id(self, branch_id: int) -> int:
        return self._encode_branch_id(branch_id)

    def is_branch_node(self, node_id: int) -> bool:
        return node_id < 0

    def _ensure_base_categories(self) -> None:
        for name in ("#ENTITY", "#PROPERTY", "#VERB", "#RELATION", "#LOCATION", "#TIME", "#STATE"):
            symbol = self._create_symbol(name, kind="category")
            self._category_ids[_normalize(name)] = symbol.id

    def _ensure_special_symbols(self) -> None:
        self.set_symbol_kind("#SELF", "entity")
        self.set_symbol_kind("#USER", "entity")
        self.set_symbol_kind("#NOW", "time")
        self.add_alias("#USER", "user")
        self.add_alias("#SELF", "logos")
        self.add_alias("#NOW", "now")

    def _ensure_relation_ontology(self) -> None:
        relation_names = [
            "is_a",
            "is",
            "do",
            "did",
            "have",
            "is_needed_to",
            "causes",
            "because",
            "because_of",
            "what",
            "whom",
            "opposite_to",
            "prep",
            "if",
            "and",
            "or",
            "can_do",
            "can_have",
            "is_component_of",
            "how",
            "when",
            "how_long",
            "unit",
            "where_dir",
            "where_loc",
            "parallel_to",
            "task_link",
            "method_link",
            "in_order_to",
            "has_property",
        ]
        self._relation_names = relation_names
        for name in relation_names:
            symbol = self.ensure_symbol(name, kind="relation")
            symbol.description = "relation"

    def _create_symbol(self, name: str, kind: str = "unknown", symbol_id: Optional[int] = None) -> Symbol:
        sym_id = self._next_symbol_id if symbol_id is None else symbol_id
        if symbol_id is None:
            self._next_symbol_id += 1
        else:
            self._next_symbol_id = max(self._next_symbol_id, symbol_id + 1)
        symbol = Symbol(id=sym_id, name=name.strip(), kind=kind)
        self._symbols_by_id[symbol.id] = symbol
        self._symbols_by_name[_normalize(symbol.name)] = symbol
        return symbol

    def ensure_symbol(self, name: str, kind: Optional[str] = None) -> Symbol:
        key = _normalize(name)
        if key in self._symbols_by_name:
            symbol = self._symbols_by_name[key]
            if name.strip() and any(char.isupper() for char in name) and symbol.name.islower():
                symbol.name = name.strip()
            if kind and symbol.kind == "unknown":
                symbol.kind = kind
                self._link_to_category(symbol, kind)
            return symbol
        symbol = self._create_symbol(name, kind=kind or "unknown")
        if kind and kind != "category":
            self._link_to_category(symbol, kind)
        return symbol

    def _link_to_category(self, symbol: Symbol, kind: str) -> None:
        parent = self._category_for_kind(kind)
        if parent:
            if not self._has_link(symbol.id, parent.id, "is_a"):
                self.add_link(symbol.id, parent.id, "is_a", generality=1.0, actuality=1.0)

    def _has_link(self, source_id: int, target_id: int, relation: str) -> bool:
        for link_id in self._links_from.get(source_id, []):
            link = self._links_by_id.get(link_id)
            if link and link.target == target_id and link.relation == relation:
                return True
        return False

    def _category_for_kind(self, kind: str) -> Optional[Symbol]:
        mapping = {
            "entity": "#ENTITY",
            "property": "#PROPERTY",
            "relation": "#RELATION",
            "action": "#VERB",
            "verb": "#VERB",
            "location": "#LOCATION",
            "time": "#TIME",
            "state": "#STATE",
            "category": "#ENTITY",
        }
        name = mapping.get(kind)
        if not name:
            return None
        return self._symbols_by_name.get(_normalize(name))

    def set_symbol_kind(self, name: str, kind: str) -> Symbol:
        symbol = self.ensure_symbol(name)
        symbol.kind = kind
        self._link_to_category(symbol, kind)
        return symbol

    def add_alias(self, name: str, alias: str) -> None:
        symbol = self.ensure_symbol(name)
        if alias not in symbol.aliases:
            symbol.aliases.append(alias)
            self._symbols_by_name[_normalize(alias)] = symbol

    def add_link(
        self,
        source_id: int,
        target_id: int,
        relation: str,
        generality: float = 0.0,
        actuality: float = 1.0,
        created_at: Optional[str] = None,
        evidence: Optional[List[int]] = None,
        link_id: Optional[int] = None,
    ) -> Link:
        if link_id is None:
            link_id = self._next_link_id
            self._next_link_id += 1
        else:
            self._next_link_id = max(self._next_link_id, link_id + 1)
        timestamp = created_at or datetime.utcnow().isoformat(timespec="seconds") + "Z"
        link = Link(
            id=link_id,
            source=source_id,
            target=target_id,
            relation=relation,
            generality=generality,
            actuality=actuality,
            created_at=timestamp,
            evidence=list(evidence or []),
        )
        self._links_by_id[link.id] = link
        self._links_from.setdefault(source_id, []).append(link.id)
        self._links_to.setdefault(target_id, []).append(link.id)
        return link

    def add_branch(self, logos: List[int], links: List[int], branch_id: Optional[int] = None) -> Branch:
        if branch_id is None:
            branch_id = self._next_branch_id
            self._next_branch_id += 1
        else:
            self._next_branch_id = max(self._next_branch_id, branch_id + 1)
        branch = Branch(id=branch_id, logos=list(logos), links=list(links))
        self._branches_by_id[branch.id] = branch
        return branch

    def add_prep_branch(
        self,
        obj_id: int,
        prep_id: int,
        generality: float = 0.2,
        actuality: float = 1.0,
    ) -> Tuple[Branch, Link]:
        prep_link = self.add_link(obj_id, prep_id, "prep", generality=generality, actuality=actuality)
        branch = self.add_branch([obj_id, prep_id], [prep_link.id])
        return branch, prep_link

    def add_relation_to_branch(
        self,
        subject: str,
        relation: str,
        branch_id: int,
        generality: float = 0.2,
        actuality: float = 1.0,
    ) -> Link:
        subject_symbol = self.ensure_symbol(subject, kind="entity")
        return self.add_link(
            subject_symbol.id,
            self.branch_node_id(branch_id),
            relation,
            generality=generality,
            actuality=actuality,
        )

    def add_is_a(self, child: str, parent: str, generality: float = 1.0, child_kind: Optional[str] = None) -> Link:
        if child_kind:
            child_symbol = self.ensure_symbol(child, kind=child_kind)
        else:
            child_symbol = self.ensure_symbol(child)
            if child_symbol.kind == "unknown":
                child_symbol.kind = "entity"
                self._link_to_category(child_symbol, "entity")
        parent_symbol = self.ensure_symbol(parent, kind="category")
        return self.add_link(child_symbol.id, parent_symbol.id, "is_a", generality=generality, actuality=1.0)

    def add_property(self, subject: str, prop: str, generality: float = 0.2, actuality: float = 1.0) -> Link:
        subject_symbol = self.ensure_symbol(subject, kind="entity")
        prop_symbol = self.ensure_symbol(prop, kind="property")
        return self.add_link(
            subject_symbol.id,
            prop_symbol.id,
            "has_property",
            generality=generality,
            actuality=actuality,
        )

    def add_is(self, subject: str, descriptor: str, generality: float = 0.2, actuality: float = 1.0) -> Link:
        subject_symbol = self.ensure_symbol(subject, kind="entity")
        desc_symbol = self.ensure_symbol(descriptor, kind="property")
        return self.add_link(
            subject_symbol.id,
            desc_symbol.id,
            "is",
            generality=generality,
            actuality=actuality,
        )

    def add_relation(
        self,
        subject: str,
        relation: str,
        obj: str,
        generality: float = 0.2,
        actuality: float = 1.0,
    ) -> Link:
        subject_symbol = self.ensure_symbol(subject, kind="entity")
        object_symbol = self.ensure_symbol(obj, kind="entity")
        return self.add_link(
            subject_symbol.id,
            object_symbol.id,
            relation,
            generality=generality,
            actuality=actuality,
        )

    def symbol_by_name(self, name: str) -> Optional[Symbol]:
        return self._symbols_by_name.get(_normalize(name))

    def symbol_by_id(self, symbol_id: int) -> Optional[Symbol]:
        return self._symbols_by_id.get(symbol_id)

    def link_by_id(self, link_id: int) -> Optional[Link]:
        return self._links_by_id.get(link_id)

    def links_from(self, symbol_id: int) -> List[Link]:
        return [self._links_by_id[lk_id] for lk_id in self._links_from.get(symbol_id, [])]

    def links_to(self, symbol_id: int) -> List[Link]:
        return [self._links_by_id[lk_id] for lk_id in self._links_to.get(symbol_id, [])]

    def links(self) -> List[Link]:
        return list(self._links_by_id.values())

    def branches(self) -> List[Branch]:
        return list(self._branches_by_id.values())

    def branch_by_id(self, branch_id: int) -> Optional[Branch]:
        return self._branches_by_id.get(branch_id)

    def branch_by_node_id(self, node_id: int) -> Optional[Branch]:
        branch_id = self._decode_branch_id(node_id)
        if branch_id is None:
            return None
        return self._branches_by_id.get(branch_id)

    def remove_link(self, link_id: int) -> bool:
        link = self._links_by_id.pop(link_id, None)
        if not link:
            return False
        if link.source in self._links_from:
            self._links_from[link.source] = [lid for lid in self._links_from[link.source] if lid != link_id]
        if link.target in self._links_to:
            self._links_to[link.target] = [lid for lid in self._links_to[link.target] if lid != link_id]
        return True

    def touch_link(self, link_id: int, delta: float = 0.05) -> None:
        link = self._links_by_id.get(link_id)
        if link:
            link.actuality = min(1.0, link.actuality + delta)

    def decay_links(self, rate: float = 0.01) -> None:
        for link in self._links_by_id.values():
            link.actuality = max(0.0, link.actuality - rate)

    def to_dict(self) -> Dict[str, object]:
        return {
            "symbols": [
                {
                    "id": sym.id,
                    "name": sym.name,
                    "kind": sym.kind,
                    "description": sym.description,
                    "aliases": list(sym.aliases),
                }
                for sym in self._symbols_by_id.values()
            ],
            "links": [
                {
                    "id": link.id,
                    "source": link.source,
                    "target": link.target,
                    "relation": link.relation,
                    "generality": link.generality,
                    "actuality": link.actuality,
                    "created_at": link.created_at,
                    "evidence": list(link.evidence),
                }
                for link in self._links_by_id.values()
            ],
            "branches": [
                {
                    "id": br.id,
                    "logos": list(br.logos),
                    "links": list(br.links),
                }
                for br in self._branches_by_id.values()
            ],
            "metadata": {
                "next_symbol_id": self._next_symbol_id,
                "next_link_id": self._next_link_id,
                "next_branch_id": self._next_branch_id,
            },
        }

    @classmethod
    def from_dict(cls, payload: Dict[str, object]) -> "KnowledgeBase":
        kb = cls()
        kb._symbols_by_id.clear()
        kb._symbols_by_name.clear()
        kb._links_by_id.clear()
        kb._links_from.clear()
        kb._links_to.clear()
        kb._branches_by_id.clear()
        kb._next_symbol_id = 0
        kb._next_link_id = 0
        kb._next_branch_id = 0
        kb._category_ids.clear()

        for sym in payload.get("symbols", []):
            symbol = kb._create_symbol(
                sym["name"],
                kind=sym.get("kind", "unknown"),
                symbol_id=sym["id"],
            )
            symbol.description = sym.get("description", "")
            symbol.aliases = list(sym.get("aliases", []))
            for alias in symbol.aliases:
                kb._symbols_by_name[_normalize(alias)] = symbol

        for link in payload.get("links", []):
            kb.add_link(
                link["source"],
                link["target"],
                link["relation"],
                generality=link.get("generality", 0.0),
                actuality=link.get("actuality", 1.0),
                created_at=link.get("created_at") or "legacy",
                evidence=list(link.get("evidence", [])),
                link_id=link["id"],
            )

        for br in payload.get("branches", []):
            kb.add_branch(br.get("logos", []), br.get("links", []), branch_id=br["id"])

        metadata = payload.get("metadata", {})
        kb._next_symbol_id = max(kb._next_symbol_id, metadata.get("next_symbol_id", kb._next_symbol_id))
        kb._next_link_id = max(kb._next_link_id, metadata.get("next_link_id", kb._next_link_id))
        kb._next_branch_id = max(kb._next_branch_id, metadata.get("next_branch_id", kb._next_branch_id))

        kb._category_ids = {
            _normalize(sym.name): sym.id
            for sym in kb._symbols_by_id.values()
            if sym.kind == "category"
        }
        if not kb._category_ids:
            kb._ensure_base_categories()
        kb._ensure_special_symbols()
        kb._ensure_relation_ontology()
        return kb

    @classmethod
    def from_legacy_files(cls, base_dir: Path) -> "KnowledgeBase":
        kb = cls()
        logos_path = base_dir / "logos.txt"
        links_path = base_dir / "links.txt"
        branches_path = base_dir / "branches.txt"
        if not logos_path.exists() or not links_path.exists():
            return kb

        kb._symbols_by_id.clear()
        kb._symbols_by_name.clear()
        kb._links_by_id.clear()
        kb._links_from.clear()
        kb._links_to.clear()
        kb._branches_by_id.clear()
        kb._next_symbol_id = 0
        kb._next_link_id = 0
        kb._next_branch_id = 0
        kb._category_ids.clear()

        for line in logos_path.read_text(encoding="utf-8").splitlines():
            parts = [p for p in line.split("/") if p]
            if len(parts) < 2:
                continue
            name, raw_id = parts[0], parts[1]
            try:
                sym_id = int(raw_id)
            except ValueError:
                continue
            kb._create_symbol(name, kind="unknown", symbol_id=sym_id)

        for line in links_path.read_text(encoding="utf-8").splitlines():
            parts = [p for p in line.split("/") if p]
            if len(parts) < 6:
                continue
            try:
                source_id = int(parts[0])
                target_id = int(parts[1])
                relation = parts[2]
                generality = float(parts[3])
                actuality = float(parts[4])
                link_id = int(parts[5])
            except ValueError:
                continue
            if source_id not in kb._symbols_by_id or target_id not in kb._symbols_by_id:
                continue
            kb.add_link(
                source_id,
                target_id,
                relation,
                generality=generality,
                actuality=actuality,
                created_at="legacy",
                link_id=link_id,
            )

        if branches_path.exists():
            for line in branches_path.read_text(encoding="utf-8").splitlines():
                parts = [p for p in line.split("/") if p]
                if not parts:
                    continue
                name_token = parts[0]
                try:
                    branch_id = int(name_token.replace("#BRANCH", ""))
                except ValueError:
                    branch_id = None
                logos: List[int] = []
                links: List[int] = []
                for index, part in enumerate(parts[1:], start=1):
                    try:
                        value = int(part)
                    except ValueError:
                        continue
                    if index % 2 == 1:
                        logos.append(value)
                    else:
                        links.append(value)
                kb.add_branch(logos, links, branch_id=branch_id)

        kb._category_ids = {
            _normalize(sym.name): sym.id
            for sym in kb._symbols_by_id.values()
            if sym.name.startswith("#")
        }
        if not kb._category_ids:
            kb._ensure_base_categories()
        kb._ensure_special_symbols()
        kb._ensure_relation_ontology()
        return kb

    def recent_links(self, limit: int = 10) -> List[Link]:
        return sorted(self._links_by_id.values(), key=lambda lk: lk.id, reverse=True)[:limit]

    def node_label(self, node_id: int, wrap_branch: bool = True) -> str:
        if self.is_branch_node(node_id):
            branch = self.branch_by_node_id(node_id)
            if not branch:
                return "(branch)" if wrap_branch else "branch"
            text = self.branch_sentence(branch)
            return f"({text})" if wrap_branch else text
        sym = self.symbol_by_id(node_id)
        return sym.name if sym else "something"

    def node_kind(self, node_id: int) -> str:
        if self.is_branch_node(node_id):
            return "branch"
        sym = self.symbol_by_id(node_id)
        return sym.kind if sym else "unknown"

    def node_label_with_id(self, node_id: int, wrap_branch: bool = True) -> str:
        if self.is_branch_node(node_id):
            branch_id = self._decode_branch_id(node_id)
            branch = self.branch_by_node_id(node_id)
            inner = self.branch_sentence_with_ids(branch) if branch else "branch"
            label = f"branch:{branch_id} {inner}".strip()
            return f"({label})" if wrap_branch else label
        sym = self.symbol_by_id(node_id)
        if not sym:
            return "unknown"
        return f"{sym.name}:{sym.id}"

    def branch_sentence_with_ids(self, branch: Optional[Branch]) -> str:
        if not branch:
            return ""
        tokens: List[str] = []
        for idx, logo_id in enumerate(branch.logos):
            tokens.append(self.node_label_with_id(logo_id, wrap_branch=True))
            if idx < len(branch.links):
                link = self._links_by_id.get(branch.links[idx])
                if link:
                    tokens.append(link.relation)
        return " ".join(t for t in tokens if t).strip()

    def branch_sentence(self, branch: Branch) -> str:
        tokens: List[str] = []
        for idx, logo_id in enumerate(branch.logos):
            tokens.append(self.node_label(logo_id, wrap_branch=True))
            if idx < len(branch.links):
                link = self._links_by_id.get(branch.links[idx])
                if link:
                    tokens.append(link.relation)
        return " ".join(t for t in tokens if t).strip()

    def link_sentence(self, link: Link) -> str:
        source_label = self.node_label(link.source, wrap_branch=True)
        target_label = self.node_label(link.target, wrap_branch=True)
        return f"{source_label} {link.relation} {target_label}"
