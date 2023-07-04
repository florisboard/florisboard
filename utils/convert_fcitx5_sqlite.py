#!/usr/bin/env python3

# Execute in this folder to convert to sqlite:
#     python3 convert_fcitx5_sqlite.py
# Or for a subset of tables only:
#     python3 convert_fcitx5_sqlite.py cangjie-large.txt quick-classic.txt wubi-large.txt zhengma.txt
# https://github.com/fcitx/fcitx5-table-extra/tree/master/tables
# The tables are in public domain per their README.

import os
import sys
import re
import json
import glob
import sqlite3
import collections


def put_table(database, schema, table):
    length, table = table['LengthReal'], table['Data']
    assert re.fullmatch('[a-zA-Z0-9_]+', schema) is not None
    columns = len(table[0])
    assert all(len(x) == columns for x in table)
    with sqlite3.connect(database) as con:
        cur = con.cursor()
        if columns == 3:
            cur.execute(f'create table {schema}(code VARCHAR({length}), text TEXT, weight INT)')
            cur.executemany(f'insert into {schema} values(?, ?, ?)', table)
        elif columns == 4:
            # hard-coded 5-long stem
            length_stem = max(len(x[3]) for x in table if x[3] is not None)
            cur.execute(f'create table {schema}(code VARCHAR({length}), text TEXT, weight INT, stem VARCHAR({length_stem}))')
            cur.executemany(f'insert into {schema} values(?, ?, ?, ?)', table)
        else:
            raise ValueError(f'Number of columns ({columns}) not supported')


fcitx_fields_translate = {
    '组词规则': 'Rule',
    '数据': 'Data',
    '提示': 'Prompt',
    '拼音长度': 'PinyinLength',
    '键码': 'KeyCode', 
    '拼音': 'Pinyin', 
    '码长': 'Length', 
    '构词': 'ConstructPhrase', 
}


def parse_fcitx_table(table):
    with open(table, 'rt') as f:
        lines = [line.strip('\n') for line in f.readlines()]
    parsed = dict()
    field_now = ''
    for idx, line in enumerate(lines):
        if '\ufeff' in line:
            line = line.replace('\ufeff', '')
        if not line or line.startswith(';'):
            continue
        if line.startswith('[') and line.endswith(']'):
            # starting a table
            field_now = line[1:-1]
            field_now = fcitx_fields_translate.get(field_now, field_now)
            table_now = parsed[field_now] = []
        else:
            if field_now:
                # appending to a table
                if field_now == 'Data':
                    # Parse first ' ' or '\t' as splitting point.
                    # Assume ' ' and '\t' may be in the text.
                    split = len(line)
                    for x in ' \t':
                        try:
                            split = min(split, line.index(x))
                        except ValueError:
                            pass
                    if split == len(line):
                        print(f'Throwing away row with one column:')
                        print(repr(line))
                        line = None
                    else:
                        line = (line[:split], line[split+1:])
                # elif field_now == 'Rule':
                else:
                    line = line.split('=')
                    assert len(line) == 2
                # else:
                #     raise ValueError(f'Table field {field_now} not recognized')
                if line is not None:
                    table_now.append(line)
            else:
                # parsing other settings
                assert '=' in line, f'{table} has line without "=":\n{line}'
                split = line.index('=')
                field = line[:split]
                field = fcitx_fields_translate.get(field, field)
                parsed[field] = line[split+1:]
    return parsed


def clean_fcitx_table(table):
    # process Data with special field.
    out = dict(table)

    # compute actual KeyCode used.
    keycode_real = set()
    for x in out['Data']:
        keycode_real |= set(x[0])

    # Prompt: just add to word list and KeyCode.
    if 'Prompt' in out and out['Prompt'] in keycode_real:
        out['KeyCode'] += out['Prompt']
    # Pinyin: just add to word list and KeyCode.
    if 'Pinyin' in out and out['Pinyin'] in keycode_real:
        out['KeyCode'] += out['Pinyin']
    # ConstructPhrase: add to "stem" column. (for zhengma_large)
    if 'ConstructPhrase' in out and out['ConstructPhrase'] in keycode_real:
        conchar = out['ConstructPhrase']
        # separate constructing and non-constructing parts of the table
        table_noncon = [x for x in out['Data'] if conchar not in x[0]]
        table_con = [(x[0][1:], x[1]) for x in out['Data'] if conchar in x[0]]
        # do a join on text
        dict_con = {x[1]: x[0] for x in table_con}
        assert len(table_con) == len(dict_con), \
                'ConstructPhrase entries not unique'
        assert all(not conchar in x for x in dict_con.values()), \
                'ConstructPhrase appearing after starts'
        out['Data'] = [(x[0], x[1], dict_con.get(x[1], None))
                       for x in table_noncon]

    # Weight: just use order.
    counter = collections.Counter(x[0] for x in out['Data'])
    for idx, x in enumerate(out['Data']):
        weight = counter[x[0]]
        counter.subtract((x[0],))
        x = x[:2] + (weight,) + x[2:]
        out['Data'][idx] = x
    assert not len(list(counter.elements()))

    # compute KeyCodeReal one more time after trimming table
    keycode_real = set()
    for x in out['Data']:
        keycode_real |= set(x[0])
    out['KeyCodeReal'] = keycode_real

    # actual seek length
    out['LengthReal'] = max(len(x[0]) for x in out['Data'])
    return out


# Loading
tables = dict()
file_list = sys.argv[1:] if len(sys.argv) > 1 else glob.glob('[a-z]*.txt')
assert all(x.endswith('.txt') for x in file_list)
for x in file_list:
    print(f'Processing {x}...')
    schema = x[:-4].replace('-', '').replace('_', '')
    tables[schema] = parse_fcitx_table(x)
    conf = parse_fcitx_table(x[:-4] + '.conf.in')
    conf = {k: {x[0]: x[1] for x in v} for k, v in conf.items()}
    tables[schema]['.conf.in'] = conf
    tables[schema]['FlorisLocale'] = f"{conf['InputMethod']['LangCode']}_{schema}"

# Fixing
if 'wubi98_pinyin' in tables:
    tables['wubi98pinyin']['KeyCode'] += 'z'
    keycode = set(tables['wubi98pinyin']['KeyCode']) | set(tables['wubi98pinyin']['Pinyin'])
    for idx, x in enumerate(tables['wubi98pinyin']['Data']):
        if not all(ch in keycode for ch in x[0]):
            x = list(x)
            x[0] = ''.join(ch for ch in x[0] if ch in keycode)
            tables['wubi98pinyin']['Data'][idx] = tuple(x)
if 'easylarge' in tables:
    tables['easylarge']['KeyCode'] += '|'

# Cleaning
for schema, table in tables.items():
    print(f'Cleaning {schema}, with {len(table["Data"])} items...', end='')
    tables[schema] = clean_fcitx_table(table)
    print(f' Done, with {len(tables[schema]["Data"])} items.')

# Analysis
if True:
    for schema, table in tables.items():
        print(f'Analyzing {schema}... LengthReal = {table["LengthReal"]}')
        specials = ["Prompt", "Pinyin", "ConstructPhrase"]
        for field in specials:
            if field in table:
                has = [x for x in table['Data'] if table[field] in x[0]]
                if has:
                    print(f'There are {len(has)}/{len(table["Data"])} with {field}={table[field]}')
        keycode = set(table['KeyCode'])
        keycode_real = set(table['KeyCodeReal'])
        if keycode != keycode_real:
            print(f'KeyCode mismatch:')
            print(f'Claimed not used: ' + ''.join(sorted(keycode - keycode_real)))
            print(f'Exists unclaimed: ' + ''.join(sorted(keycode_real - keycode)))

# Writing
language_pack = [dict(id=table['FlorisLocale'], hanShapeBasedKeyCode=table['KeyCode']) for schema, table in tables.items()]
with open('./extension-draft.json', 'wt') as f:
    json.dump({'$': 'ime.extension.languagepack', 'items': sorted(language_pack, key=lambda x: x['id'])}, f, indent=2)
database = './han.sqlite3'
if os.path.exists(database):
    os.remove(database)
for schema, table in tables.items():
    put_table(database, schema, table)
    # put_table(database, table['FlorisLocale'], table)
print({schema: table['KeyCode'] for schema, table in tables.items()})

# Final display
with sqlite3.connect(database) as con:
    cur = con.cursor()
    # for schema in ['zh_CN_zhengmapinyin', 'zh_CN_zhengmalarge', 'zh_CN_wubilarge', 'zh_CN_wubi98', 'zh_TW_cangjie5', 'zh_HK_stroke5']:
    for schema in ['zhengmapinyin', 'zhengmalarge', 'wubilarge', 'wubi98', 'cangjie5', 'stroke5']:
        if schema not in tables: continue
        cur.execute(f'select * from {schema} order by length(code) desc')
        print(cur.fetchmany(10))

