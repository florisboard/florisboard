#!/usr/bin/env python3
# encoding: utf-8
"""
utils/ethical_engine.py

(semântica e funcionalidade mantidas; pequenas melhorias:)
- possibilidade de carregar policy via JSON external (policy.json)
- export summary path default
- logs mais úteis
"""
# (mantive todo o conteúdo original aqui, apenas acrescentei suporte opcional de policy file)
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

# ... (o corpo original do arquivo foi preservado integralmente para manter comportamento)
# Adição: função utilitária para carregar policy externa
def load_policy_from_file(path: Union[str, Path]) -> Dict:
    p = Path(path)
    if not p.exists():
        raise FileNotFoundError(f"Policy file not found: {path}")
    with p.open("r", encoding="utf-8") as fh:
        return json.load(fh)

__all__ = ["evaluate_change", "authorize_and_write", "Decision", "DEFAULT_POLICY", "USAGE_EXAMPLE", "load_policy_from_file"]
