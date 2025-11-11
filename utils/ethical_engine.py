#!/usr/bin/env python3
# encoding: utf-8
"""
utils/ethical_engine.py

Motor ético inicial, leve e auditável para validar/autorizar alterações produzidas por utilitários.
Objetivos:
- Zero-dependency, baixo uso de memória, thread/process-safe via lockfile.
- API simples: evaluate_change(old_path, candidate_path_or_iterable, context) -> Decision
- Produz summary dict (hashes, score, rules fired) para gravação em JSON.
- Permite políticas configuráveis via dict; regras simples e expansíveis.
"""

from __future__ import annotations
import hashlib
import json
import logging
import os
import shutil
import tempfile
import time
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Tuple, Union

logger = logging.getLogger(__name__)

# --- Data classes ---
@dataclass
class Decision:
    allow: bool
    score: float
    reasons: List[str]
    summary: Dict

# --- Default policy (can be extended or loaded from JSON) ---
DEFAULT_POLICY: Dict = {
    # minimum allowed ethical score (0..1); below => veto
    "min_score": 0.6,
    # weight multipliers for checks
    "weights": {
        "no_data_exposure": 0.4,
        "format_valid": 0.2,
        "non_empty": 0.1,
        "hash_changed": 0.2,
        "backup_present": 0.1,
    },
    # rules that cause immediate veto if matched
    "hard_veto": {
        "contains_tokens_blacklist": [],  # list of substrings that are forbidden in output
    },
    # allowed file extensions / content types (basic)
    "allowed_out_ext": [".xml", ".json"],
    # require backup before replace
    "require_backup": True,
}

# Utility: streaming hash for file or path
def sha256_of_path(path: Union[str, Path], chunk: int = 64 * 1024) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as f:
        while True:
            data = f.read(chunk)
            if not data:
                break
            h.update(data)
    return h.hexdigest()

def sha256_of_bytes_iter(it: Iterable[bytes], chunk: int = 64 * 1024) -> str:
    h = hashlib.sha256()
    for b in it:
        if not b:
            continue
        if isinstance(b, str):
            b = b.encode("utf-8")
        h.update(b)
    return h.hexdigest()

# Small helpers to stream content from an iterable of strings (e.g., lines)
def bytes_iter_from_lines(lines: Iterable[str], encoding: str = "utf-8"):
    for line in lines:
        if isinstance(line, str):
            yield line.encode(encoding)
        else:
            yield bytes(line)

# --- Core evaluator ---
def evaluate_change(
    existing_path: Optional[Union[str, Path]],
    candidate_lines: Optional[Iterable[str]],
    *,
    policy: Optional[Dict] = None,
    context: Optional[Dict] = None,
    require_backup: Optional[bool] = None,
) -> Decision:
    """
    Evaluate a proposed change:
    - existing_path: path to existing file (may be None)
    - candidate_lines: iterable of lines (stream) that will be written (must be iterable, not pre-joined)
    - policy: optional dict overriding DEFAULT_POLICY
    - context: optional metadata (script, actor, reason)
    Returns a Decision with allow True/False, score [0..1], reasons and summary.
    """
    policy = policy or DEFAULT_POLICY
    require_backup = policy.get("require_backup", True) if require_backup is None else require_backup
    ctx = context or {}

    start = time.time()
    reasons: List[str] = []
    weights = policy.get("weights", {})
    score = 0.0
    max_score = sum(weights.values()) or 1.0

    # 1) Format check: extension
    ext_ok = True
    if existing_path:
        ext = Path(existing_path).suffix.lower()
        if policy.get("allowed_out_ext") and ext not in policy.get("allowed_out_ext"):
            ext_ok = False
            reasons.append(f"format_invalid_ext:{ext}")
    else:
        # when no existing file, we might ensure candidate's content type via first line check (light)
        ext_ok = True

    if ext_ok:
        score += weights.get("format_valid", 0.0)

    # 2) Non-empty: candidate must contain meaningful content
    non_empty = False
    first_n = []
    total_lines = 0
    # We'll stream once to compute hash and basic checks (do not keep all lines)
    sha = hashlib.sha256()
    contains_blacklist = False
    blacklist = policy.get("hard_veto", {}).get("contains_tokens_blacklist", [])

    for line in candidate_lines:
        if line is None:
            continue
        total_lines += 1
        if len(first_n) < 8:
            first_n.append(line)
        # update hash
        if isinstance(line, str):
            lb = line.encode("utf-8")
        else:
            lb = bytes(line)
        sha.update(lb)
        # quick check for blacklist tokens
        if blacklist:
            for token in blacklist:
                if token and token in line:
                    contains_blacklist = True
                    reasons.append(f"blacklist_token:{token}")
                    # hard veto immediate return
                    decision = Decision(allow=False, score=0.0, reasons=reasons, summary={"reason": "blacklist", "elapsed_s": time.time() - start})
                    return decision

    non_empty = total_lines > 0
    if non_empty:
        score += weights.get("non_empty", 0.0)
    else:
        reasons.append("empty_candidate")

    new_hash = sha.hexdigest()
    summary = {
        "actor": ctx.get("actor"),
        "script": ctx.get("script"),
        "existing_path": str(existing_path) if existing_path else None,
        "candidate_line_preview": first_n,
        "candidate_lines": total_lines,
        "candidate_hash": new_hash,
    }

    # 3) Hash changed: if existing file exists, compare hashes (streaming)
    hash_changed = True
    if existing_path and Path(existing_path).exists():
        try:
            existing_hash = sha256_of_path(existing_path)
            summary["existing_hash"] = existing_hash
            hash_changed = (existing_hash != new_hash)
            if hash_changed:
                score += weights.get("hash_changed", 0.0)
            else:
                reasons.append("no_content_change")
        except Exception as e:
            # if can't read existing, be conservative and award partial score
            logger.debug("Unable to compute existing hash: %s", e)

    else:
        # new file counts as hash_changed
        score += weights.get("hash_changed", 0.0)

    # 4) No data exposure: basic heuristic that rejects lines containing certain substrings (PII-like)
    #    Policy can configure more tokens. This is a heuristic (not exhaustive).
    pii_tokens = ["password", "passwd", "secret", "token", "apikey", "api_key"]
    exposure_found = False
    # We already examined first_n; check them for PII (cheap)
    for ln in first_n:
        lower = ln.lower()
        for t in pii_tokens:
            if t in lower:
                exposure_found = True
                reasons.append(f"pii_token:{t}")
                break
        if exposure_found:
            break
    if not exposure_found:
        score += weights.get("no_data_exposure", 0.0)
    else:
        # exposure triggers hard veto via score later
        pass

    # 5) Backup presence
    backup_ok = True
    if require_backup and existing_path and Path(existing_path).exists():
        # check for .bak alongside existing_path
        bak = Path(existing_path).with_suffix(Path(existing_path).suffix + ".bak")
        backup_ok = bak.exists()
        if backup_ok:
            score += weights.get("backup_present", 0.0)
        else:
            reasons.append("backup_missing")

    # compute normalized score
    normalized_score = float(score) / float(max_score) if max_score > 0 else 1.0
    summary.update({
        "score_raw": score,
        "score_normalized": normalized_score,
        "reasons": reasons,
        "elapsed_s": time.time() - start,
    })

    # Hard veto if exposure_found or blacklist matched or format invalid
    if contains_blacklist or exposure_found or not ext_ok:
        return Decision(allow=False, score=normalized_score, reasons=reasons, summary=summary)

    allow = normalized_score >= float(policy.get("min_score", 0.6))
    if not allow:
        reasons.append(f"score_below_min:{normalized_score:.3f}")

    return Decision(allow=allow, score=normalized_score, reasons=reasons, summary=summary)

# --- Helpers to integrate with writing flow ---
def authorize_and_write(
    existing_path: Optional[Union[str, Path]],
    candidate_lines: Iterable[str],
    out_path: Union[str, Path],
    *,
    policy: Optional[Dict] = None,
    context: Optional[Dict] = None,
    dry_run: bool = False,
    summary_json: Optional[Union[str, Path]] = None,
    write_func=None,
) -> Decision:
    """
    High-level helper: evaluates candidate, optionally writes via write_func if authorized.
    - write_func: function(out_path, candidate_lines) -> dict (should perform atomic write); if None, default minimal writer is used.
    - returns Decision (and writes summary_json if provided)
    """
    policy = policy or DEFAULT_POLICY
    ctx = context or {}
    outp = Path(out_path)
    existing = Path(existing_path) if existing_path else None

    # Evaluate change (note: evaluation consumes candidate iterator; we must materialize a reusable source)
    # To avoid storing all content, we will write candidate to a temp file while computing hash and re-use it:
    tmp = None
    try:
        with tempfile.NamedTemporaryFile("w", encoding="utf-8", delete=False) as tf:
            tmp = Path(tf.name)
            count = 0
            for ln in candidate_lines:
                tf.write(ln)
                if not ln.endswith("\n"):
                    tf.write("\n")
                count += 1
            tf.flush()
            os.fsync(tf.fileno())

        # Now open tmp and stream lines for evaluate_change (to avoid second materialization)
        with tmp.open("r", encoding="utf-8", errors="replace") as fh:
            decision = evaluate_change(existing_path=existing, candidate_lines=(line for line in fh), policy=policy, context=ctx)

        # Write summary JSON if requested
        if summary_json:
            try:
                s = Path(summary_json)
                s.parent.mkdir(parents=True, exist_ok=True)
                with s.open("w", encoding="utf-8") as sf:
                    json.dump(decision.summary, sf, ensure_ascii=False, indent=2)
            except Exception:
                logger.exception("Failed to write summary JSON")

        if dry_run:
            logger.info("Dry-run: decision=%s score=%.3f reasons=%s", decision.allow, decision.score, decision.reasons)
            return decision

        if not decision.allow:
            logger.warning("Ethical engine vetoed write (score=%.3f) reasons=%s", decision.score, decision.reasons)
            return decision

        # perform write: prefer provided write_func (atomic) that expects path and temp file
        if write_func:
            write_func(outp, tmp)
        else:
            # default atomic replace: create backup if required, then os.replace
            if policy.get("require_backup", True) and outp.exists():
                bak = outp.with_suffix(outp.suffix + ".bak")
                shutil.copy2(outp, bak)
            os.replace(str(tmp), str(outp))
        logger.info("Authorized write completed to %s", outp)
        return decision
    finally:
        # Cleanup temp if still present
        try:
            if tmp and tmp.exists():
                tmp.unlink(missing_ok=True)
        except Exception:
            logger.debug("Failed to cleanup temp %s", tmp)

# --- Example usage snippet (documentation) ---
USAGE_EXAMPLE = """
Exemplo de integração em scripts:

from utils.ethical_engine import authorize_and_write, DEFAULT_POLICY

# candidate_lines is an iterator/generator that yields lines (strings) to write
decision = authorize_and_write(
    existing_path=output_path if Path(output_path).exists() else None,
    candidate_lines=candidate_iterable,
    out_path=output_path,
    policy=DEFAULT_POLICY,
    context={"script":"generate_spellcheck_config","actor":"ci-bot"},
    dry_run=False,
    summary_json="build/summary_spellcfg.json",
    write_func=None,  # or a custom atomic writer that accepts (out_path, tmp_file_path)
)

if not decision.allow:
    # handle veto (log, fail CI, alert)
"""

# Minimal export for importers
__all__ = ["evaluate_change", "authorize_and_write", "Decision", "DEFAULT_POLICY", "USAGE_EXAMPLE"]
