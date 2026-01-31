from __future__ import annotations

from dataclasses import dataclass, field
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
        self._ensure_base_categories()

    def _ensure_base_categories(self) -> None:
        for name in ("#ENTITY", "#PROPERTY", "#RELATION", "#ACTION", "#LOCATION", "#TIME", "#STATE"):
            symbol = self._create_symbol(name, kind="category")
            self._category_ids[_normalize(name)] = symbol.id

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
            self.add_link(symbol.id, parent.id, "is_a", generality=1.0, actuality=1.0)

    def _category_for_kind(self, kind: str) -> Optional[Symbol]:
        mapping = {
            "entity": "#ENTITY",
            "property": "#PROPERTY",
            "relation": "#RELATION",
            "action": "#ACTION",
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
        evidence: Optional[List[int]] = None,
        link_id: Optional[int] = None,
    ) -> Link:
        if link_id is None:
            link_id = self._next_link_id
            self._next_link_id += 1
        else:
            self._next_link_id = max(self._next_link_id, link_id + 1)
        link = Link(
            id=link_id,
            source=source_id,
            target=target_id,
            relation=relation,
            generality=generality,
            actuality=actuality,
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

    def add_property(self, subject: str, prop: str, generality: float = 0.2) -> Link:
        subject_symbol = self.ensure_symbol(subject, kind="entity")
        prop_symbol = self.ensure_symbol(prop, kind="property")
        return self.add_link(subject_symbol.id, prop_symbol.id, "has_property", generality=generality, actuality=1.0)

    def add_relation(self, subject: str, relation: str, obj: str, generality: float = 0.2) -> Link:
        subject_symbol = self.ensure_symbol(subject, kind="entity")
        object_symbol = self.ensure_symbol(obj, kind="entity")
        return self.add_link(subject_symbol.id, object_symbol.id, relation, generality=generality, actuality=1.0)

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
        return kb

    def recent_links(self, limit: int = 10) -> List[Link]:
        return sorted(self._links_by_id.values(), key=lambda lk: lk.id, reverse=True)[:limit]

    def link_sentence(self, link: Link) -> str:
        source = self.symbol_by_id(link.source)
        target = self.symbol_by_id(link.target)
        if not source or not target:
            return link.relation
        return f"{source.name} {link.relation} {target.name}"
