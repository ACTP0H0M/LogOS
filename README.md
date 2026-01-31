# LogOS (Rewrite)

LogOS is an experiment in **symbolic, resource-efficient conversational reasoning**. The original Java prototype explored NLP and hypergraph ideas inspired by cognitive architectures (Soar, OpenCog, NARS). This rewrite focuses on the same spirit with a much lighter, browser-based interface and a modular symbolic core.

## Goals

- Prioritize symbolic reasoning and hypergraph-style memory.
- Use deep learning **only where it is strictly necessary**.
- Keep the system small, inspectable, and efficient.

## Quick start

> Requires Python 3.11+.

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

Open <http://localhost:8000> in your browser.

## Optional: classical NLP (StanfordNLP / Stanza)

LogOS can optionally attach a classical NLP frontend (POS + dependency tree + constituency tree) using `stanza`.

Install:

```bash
pip install -r requirements-nlp.txt
python -c "import stanza; stanza.download('en')"
```

When the models are present, LogOS will automatically store parse trees for each remembered user statement in `data/episodic.json`.

## Module map (rewrite)

| Module | Purpose |
| --- | --- |
| `app/main.py` | FastAPI server that hosts the browser UI and exposes `/api/chat`. |
| `logos/engine.py` | Orchestrates parsing, memory updates, and response selection. |
| `logos/nlp.py` | Lightweight parsing and intent extraction (no heavy ML). |
| `logos/memory.py` | Stores symbolic facts with indexing for efficient lookup. |
| `logos/hypergraph.py` | Minimal hypergraph structure to store relations. |
| `logos/reasoner.py` | Rule-based response generation over symbolic memory. |
| `logos/state.py` | Conversation state (name, mood, recent topics). |
| `web/` | Static browser UI for the chat experience. |

## Architecture notes

- The chat loop is designed to stay cheap: tokenization, heuristics, and symbolic memory are enough for many conversational goals.
- The system prefers explicit clarification over guessing; uncertain facts are stored with low confidence until confirmed.
- You can integrate deeper NLP or embeddings later, but only where symbolic reasoning fails.

## Persistent memory

The Python rewrite now persists symbolic knowledge across runs:

- `data/long_term.json` stores the ontology + hypergraph (logos, links, branches).
- `data/episodic.json` stores interaction episodes with extracted facts.
- `data/procedural.json` stores simple procedural rules.

On first run, the system seeds the long-term memory from `logos.txt`, `links.txt`, and `branches.txt` if they exist.

## Legacy code

The original Java prototype lives under `src/` and remains available for reference. The rewrite lives in the top-level `app/`, `logos/`, and `web/` folders.
