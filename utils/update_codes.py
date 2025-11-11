#!/usr/bin/env python3
"""
utils/update_codes.py

Versão otimizada focada em eficiência, uso de recursos e robustez.

Principais otimizações:
- Evita I/O e escrita quando não há alterações.
- Escrita atômica e backup somente quando necessário.
- Uso de referências locais/loops indexados para micro-otimização.
- Suporte opcional a grapheme clusters (se regex/grapheme instalados).
- Liberação explícita de grandes objetos e chamada de gc.collect() após uso.
- Logging configurável (em vez de prints).
- Mensagens informativas e contáveis (total/atualizadas).
- Pequenas otimizações no cálculo de codepoints (list comprehensions locais).
"""

from __future__ import annotations
import argparse
import json
import logging
import os
import sys
import tempfile
import shutil
import gc
from typing import Any, Dict, List, Optional, Union

# Optional grapheme support
_GRAPHEME_IMPL = None
try:
    import regex as _regex  # type: ignore
    _GRAPHEME_IMPL = "regex"
except Exception:
    try:
        import grapheme as _grapheme  # type: ignore
        _GRAPHEME_IMPL = "grapheme"
    except Exception:
        _GRAPHEME_IMPL = None

logger = logging.getLogger(__name__)

# File size threshold (bytes) for recommending streaming approach
RECOMMEND_STREAMING_BYTES = 50 * 1024 * 1024  # 50 MiB


def get_grapheme_clusters(s: str) -> List[str]:
    if not s:
        return []
    if _GRAPHEME_IMPL == "regex":
        return _regex.findall(r"\X", s)
    if _GRAPHEME_IMPL == "grapheme":
        return list(_grapheme.graphemes(s))
    return list(s)


def compute_code_from_label(label: Union[str, int, None], mode: str = "compat", use_grapheme: bool = False) -> Dict[str, Any]:
    out: Dict[str, Any] = {}
    if label is None:
        out["code"] = None
        return out
    if isinstance(label, int):
        out["code"] = label
        return out
    if not isinstance(label, str) or label == "":
        out["code"] = None
        return out

    if use_grapheme:
        clusters = get_grapheme_clusters(label)
        # flatten clusters into codepoints list (keeps order)
        flat: List[int] = []
        for cluster in clusters:
            # list comprehension for speed
            flat.extend([ord(ch) for ch in cluster])
    else:
        flat = [ord(ch) for ch in label]

    if not flat:
        out["code"] = None
        return out

    first = flat[0]
    if mode == "first":
        out["code"] = first
    elif mode == "list":
        out["code"] = first
        out["codes"] = flat
    elif mode == "hex":
        out["code"] = hex(first)
        if len(flat) > 1:
            out["codes_hex"] = [hex(cp) for cp in flat]
    else:  # compat
        out["code"] = first
        if len(flat) > 1:
            out["codes"] = flat

    return out


def update_layout_file(
    path: str,
    dry_run: bool = False,
    backup: bool = True,
    mode: str = "compat",
    indent: Optional[int] = 2,
    use_grapheme: bool = False,
    show_recommend_streaming: bool = True,
) -> int:
    """
    Atualiza o arquivo JSON do layout. Retorna o número de teclas atualizadas.
    Observação: para arquivos muito grandes, considere implementar processamento em streaming (ijson).
    """
    if not os.path.isfile(path):
        raise FileNotFoundError(f"Arquivo não encontrado: {path}")

    # Recommend streaming approach for very large files (user can decide to install ijson and ask for streaming)
    try:
        fsize = os.path.getsize(path)
        if show_recommend_streaming and fsize >= RECOMMEND_STREAMING_BYTES:
            logger.info(
                "Arquivo grande detectado (%.1f MiB). Para reduzir uso de memória, considere usar processamento por streaming (ex.: ijson).",
                fsize / (1024 * 1024),
            )
    except Exception:
        # ignore size detection failures
        fsize = 0

    # leitura
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)

    arrangement = data.get("arrangement")
    if not isinstance(arrangement, list):
        raise ValueError("JSON não contém 'arrangement' como lista no nível superior.")

    updated = 0
    total = 0

    # micro-optimizations: local refs
    arr = arrangement
    arr_len = len(arr)

    # iterate by index to avoid iterator overhead when modifying in-place
    for i in range(arr_len):
        row = arr[i]
        if not isinstance(row, list):
            continue
        row_len = len(row)
        for j in range(row_len):
            key = row[j]
            total += 1
            if not isinstance(key, dict):
                continue
            label = key.get("label")
            new = compute_code_from_label(label, mode=mode, use_grapheme=use_grapheme)

            # existing values
            existing_code = key.get("code")
            existing_codes = key.get("codes")

            # decide quickly whether to update (fast comparisons)
            need_update = False
            nc = new.get("code", None)
            if existing_code != nc:
                need_update = True
            elif "codes" in new:
                if existing_codes != new["codes"]:
                    need_update = True

            if need_update:
                # apply updates in-place
                if "code" in new:
                    arr[i][j]["code"] = new["code"]
                if "codes" in new:
                    arr[i][j]["codes"] = new["codes"]
                if "codes_hex" in new:
                    arr[i][j]["codes_hex"] = new["codes_hex"]
                updated += 1

    # If dry-run, do not write anything
    if dry_run:
        logger.info("Dry-run: %d de %d teclas precisariam ser atualizadas em '%s'.", updated, total, path)
        # release references
        del data
        del arr
        gc.collect()
        return updated

    # if no updates, skip backup and writing (saves I/O)
    if updated == 0:
        logger.info("Nenhuma alteração necessária para '%s'. Saindo sem gravar.", path)
        # free memory
        del data
        del arr
        gc.collect()
        return 0

    # backup
    if backup:
        bak_path = f"{path}.bak"
        try:
            shutil.copy2(path, bak_path)
            logger.info("Backup criado: %s", bak_path)
        except Exception:
            logger.exception("Falha ao criar backup (continuando): %s", bak_path)

    # atomic write to temporary file then replace
    dir_name = os.path.dirname(path) or "."
    tmp_path = None
    try:
        with tempfile.NamedTemporaryFile(mode="w", encoding="utf-8", dir=dir_name, delete=False, prefix=".tmp_update_codes_") as tmpf:
            tmp_path = tmpf.name
            # write JSON with chosen indent; ensure_ascii=False to preserve unicode
            json.dump(data, tmpf, ensure_ascii=False, indent=indent)
            tmpf.flush()
            os.fsync(tmpf.fileno())
        # replace atomically
        os.replace(tmp_path, path)
        logger.info("Arquivo atualizado: %s (%d de %d teclas modificadas)", path, updated, total)
    except Exception:
        logger.exception("Erro durante escrita atômica; tentando remover temporário.")
        if tmp_path and os.path.exists(tmp_path):
            try:
                os.remove(tmp_path)
            except Exception:
                logger.exception("Falha ao remover temporário: %s", tmp_path)
        raise
    finally:
        # free large objects and run GC to promptly release memory back to OS where possible
        try:
            del data
            del arr
            gc.collect()
        except Exception:
            pass

    return updated


def parse_args(argv: Optional[List[str]] = None) -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Atualiza 'code' (e 'codes') das teclas em um layout JSON.")
    p.add_argument("file", help="Caminho do arquivo JSON de layout.")
    p.add_argument("--dry-run", action="store_true", help="Somente mostrar alterações; não gravar.")
    p.add_argument("--no-backup", action="store_true", help="Não criar arquivo .bak antes de sobrescrever.")
    p.add_argument("--mode", choices=["compat", "first", "list", "hex"], default="compat", help="Modo de geração de 'code'/'codes'.")
    p.add_argument("--indent", type=int, default=2, help="Indentação JSON de saída (0 = minified).")
    p.add_argument("--grapheme", action="store_true", help="Usar grapheme clusters (se suporte disponível) ao gerar codes.")
    p.add_argument("--verbose", "-v", action="count", default=0, help="Aumentar verbosidade (use -v, -vv).")
    return p.parse_args(argv)


def configure_logging(verbosity: int) -> None:
    level = logging.WARNING
    if verbosity >= 2:
        level = logging.DEBUG
    elif verbosity == 1:
        level = logging.INFO
    logging.basicConfig(level=level, format="%(levelname)s: %(message)s")


def main(argv: Optional[List[str]] = None) -> int:
    args = parse_args(argv)
    configure_logging(args.verbose)
    if args.grapheme and _GRAPHEME_IMPL is None:
        logger.warning("Opção --grapheme ativada, mas nenhuma implementação de grapheme foi encontrada. Usando fallback por codepoints.")
    try:
        update_layout_file(
            args.file,
            dry_run=args.dry_run,
            backup=not args.no_backup,
            mode=args.mode,
            indent=(None if args.indent == 0 else args.indent),
            use_grapheme=(args.grapheme and _GRAPHEME_IMPL is not None),
        )
        return 0
    except Exception as exc:
        logger.exception("Erro: %s", exc)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
