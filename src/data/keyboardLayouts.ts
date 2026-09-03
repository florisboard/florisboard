import { KeyboardLayoutType } from '../types/keyboard';

export interface KeyDefinition {
  code: string;
  label: string;
  subLabel?: string;
  subKeys?: string[];
  type?: 'char' | 'shift' | 'backspace' | 'enter' | 'mode' | 'space' | 'comma' | 'period' | 'emoji' | 'action';
  widthMultiplier?: number; // relative width in row (default 1)
}

export const SUBKEYS_MAP: Record<string, string[]> = {
  'q': ['1', 'q'],
  'w': ['2', 'w'],
  'e': ['3', 'e', 'é', 'è', 'ê', 'ë', 'ē', 'ė', 'ę'],
  'r': ['4', 'r'],
  't': ['5', 't', 'þ'],
  'y': ['6', 'y', 'ÿ'],
  'u': ['7', 'u', 'ú', 'ù', 'û', 'ü', 'ū'],
  'i': ['8', 'i', 'í', 'ì', 'î', 'ï', 'ī'],
  'o': ['9', 'o', 'ó', 'ò', 'ô', 'ö', 'ō', 'œ', 'ø'],
  'p': ['0', 'p'],
  'a': ['@', 'a', 'á', 'à', 'â', 'ä', 'æ', 'ã', 'å', 'ā'],
  's': ['#', 's', 'ß', 'ś', 'š'],
  'd': ['$', 'd', 'ð'],
  'f': ['%', 'f'],
  'g': ['&', 'g'],
  'h': ['-', 'h'],
  'j': ['+', 'j'],
  'k': ['(', 'k'],
  'l': [')', 'l'],
  'z': ['*', 'z', 'ź', 'ż', 'ž'],
  'x': ['"', 'x'],
  'c': ["'", 'c', 'ç', 'ć', 'č'],
  'v': [':', 'v'],
  'b': [';', 'b'],
  'n': ['!', 'n', 'ñ', 'ń'],
  'm': ['?', 'm'],
  '.': [',', '?', '!', ':', ';', '-', '_', '@', '#', '/']
};

export const NUMBER_ROW: KeyDefinition[] = [
  { code: '1', label: '1', subLabel: '¹' },
  { code: '2', label: '2', subLabel: '²' },
  { code: '3', label: '3', subLabel: '³' },
  { code: '4', label: '4', subLabel: '⁴' },
  { code: '5', label: '5', subLabel: '½' },
  { code: '6', label: '6', subLabel: '⅓' },
  { code: '7', label: '7', subLabel: '¼' },
  { code: '8', label: '8', subLabel: '⅛' },
  { code: '9', label: '9', subLabel: '±' },
  { code: '0', label: '0', subLabel: '⁰' }
];

export const LAYOUT_DEFINITIONS: Record<KeyboardLayoutType, KeyDefinition[][]> = {
  qwerty: [
    [
      { code: 'q', label: 'q', subLabel: '1', subKeys: SUBKEYS_MAP['q'] },
      { code: 'w', label: 'w', subLabel: '2', subKeys: SUBKEYS_MAP['w'] },
      { code: 'e', label: 'e', subLabel: '3', subKeys: SUBKEYS_MAP['e'] },
      { code: 'r', label: 'r', subLabel: '4', subKeys: SUBKEYS_MAP['r'] },
      { code: 't', label: 't', subLabel: '5', subKeys: SUBKEYS_MAP['t'] },
      { code: 'y', label: 'y', subLabel: '6', subKeys: SUBKEYS_MAP['y'] },
      { code: 'u', label: 'u', subLabel: '7', subKeys: SUBKEYS_MAP['u'] },
      { code: 'i', label: 'i', subLabel: '8', subKeys: SUBKEYS_MAP['i'] },
      { code: 'o', label: 'o', subLabel: '9', subKeys: SUBKEYS_MAP['o'] },
      { code: 'p', label: 'p', subLabel: '0', subKeys: SUBKEYS_MAP['p'] }
    ],
    [
      { code: 'a', label: 'a', subLabel: '@', subKeys: SUBKEYS_MAP['a'] },
      { code: 's', label: 's', subLabel: '#', subKeys: SUBKEYS_MAP['s'] },
      { code: 'd', label: 'd', subLabel: '$', subKeys: SUBKEYS_MAP['d'] },
      { code: 'f', label: 'f', subLabel: '%', subKeys: SUBKEYS_MAP['f'] },
      { code: 'g', label: 'g', subLabel: '&', subKeys: SUBKEYS_MAP['g'] },
      { code: 'h', label: 'h', subLabel: '-', subKeys: SUBKEYS_MAP['h'] },
      { code: 'j', label: 'j', subLabel: '+', subKeys: SUBKEYS_MAP['j'] },
      { code: 'k', label: 'k', subLabel: '(', subKeys: SUBKEYS_MAP['k'] },
      { code: 'l', label: 'l', subLabel: ')', subKeys: SUBKEYS_MAP['l'] }
    ],
    [
      { code: 'shift', label: '⇧', type: 'shift', widthMultiplier: 1.4 },
      { code: 'z', label: 'z', subLabel: '*', subKeys: SUBKEYS_MAP['z'] },
      { code: 'x', label: 'x', subLabel: '"', subKeys: SUBKEYS_MAP['x'] },
      { code: 'c', label: 'c', subLabel: "'", subKeys: SUBKEYS_MAP['c'] },
      { code: 'v', label: 'v', subLabel: ':', subKeys: SUBKEYS_MAP['v'] },
      { code: 'b', label: 'b', subLabel: ';', subKeys: SUBKEYS_MAP['b'] },
      { code: 'n', label: 'n', subLabel: '!', subKeys: SUBKEYS_MAP['n'] },
      { code: 'm', label: 'm', subLabel: '?', subKeys: SUBKEYS_MAP['m'] },
      { code: 'backspace', label: '⌫', type: 'backspace', widthMultiplier: 1.4 }
    ]
  ],
  qwertz: [
    [
      { code: 'q', label: 'q', subLabel: '1', subKeys: SUBKEYS_MAP['q'] },
      { code: 'w', label: 'w', subLabel: '2', subKeys: SUBKEYS_MAP['w'] },
      { code: 'e', label: 'e', subLabel: '3', subKeys: SUBKEYS_MAP['e'] },
      { code: 'r', label: 'r', subLabel: '4', subKeys: SUBKEYS_MAP['r'] },
      { code: 't', label: 't', subLabel: '5', subKeys: SUBKEYS_MAP['t'] },
      { code: 'z', label: 'z', subLabel: '6', subKeys: SUBKEYS_MAP['z'] },
      { code: 'u', label: 'u', subLabel: '7', subKeys: SUBKEYS_MAP['u'] },
      { code: 'i', label: 'i', subLabel: '8', subKeys: SUBKEYS_MAP['i'] },
      { code: 'o', label: 'o', subLabel: '9', subKeys: SUBKEYS_MAP['o'] },
      { code: 'p', label: 'p', subLabel: '0', subKeys: SUBKEYS_MAP['p'] }
    ],
    [
      { code: 'a', label: 'a', subLabel: '@', subKeys: SUBKEYS_MAP['a'] },
      { code: 's', label: 's', subLabel: '#', subKeys: SUBKEYS_MAP['s'] },
      { code: 'd', label: 'd', subLabel: '$', subKeys: SUBKEYS_MAP['d'] },
      { code: 'f', label: 'f', subLabel: '%', subKeys: SUBKEYS_MAP['f'] },
      { code: 'g', label: 'g', subLabel: '&', subKeys: SUBKEYS_MAP['g'] },
      { code: 'h', label: 'h', subLabel: '-', subKeys: SUBKEYS_MAP['h'] },
      { code: 'j', label: 'j', subLabel: '+', subKeys: SUBKEYS_MAP['j'] },
      { code: 'k', label: 'k', subLabel: '(', subKeys: SUBKEYS_MAP['k'] },
      { code: 'l', label: 'l', subLabel: ')', subKeys: SUBKEYS_MAP['l'] }
    ],
    [
      { code: 'shift', label: '⇧', type: 'shift', widthMultiplier: 1.4 },
      { code: 'y', label: 'y', subLabel: '*', subKeys: SUBKEYS_MAP['y'] },
      { code: 'x', label: 'x', subLabel: '"', subKeys: SUBKEYS_MAP['x'] },
      { code: 'c', label: 'c', subLabel: "'", subKeys: SUBKEYS_MAP['c'] },
      { code: 'v', label: 'v', subLabel: ':', subKeys: SUBKEYS_MAP['v'] },
      { code: 'b', label: 'b', subLabel: ';', subKeys: SUBKEYS_MAP['b'] },
      { code: 'n', label: 'n', subLabel: '!', subKeys: SUBKEYS_MAP['n'] },
      { code: 'm', label: 'm', subLabel: '?', subKeys: SUBKEYS_MAP['m'] },
      { code: 'backspace', label: '⌫', type: 'backspace', widthMultiplier: 1.4 }
    ]
  ],
  azerty: [
    [
      { code: 'a', label: 'a', subLabel: '1', subKeys: SUBKEYS_MAP['a'] },
      { code: 'z', label: 'z', subLabel: '2', subKeys: SUBKEYS_MAP['z'] },
      { code: 'e', label: 'e', subLabel: '3', subKeys: SUBKEYS_MAP['e'] },
      { code: 'r', label: 'r', subLabel: '4', subKeys: SUBKEYS_MAP['r'] },
      { code: 't', label: 't', subLabel: '5', subKeys: SUBKEYS_MAP['t'] },
      { code: 'y', label: 'y', subLabel: '6', subKeys: SUBKEYS_MAP['y'] },
      { code: 'u', label: 'u', subLabel: '7', subKeys: SUBKEYS_MAP['u'] },
      { code: 'i', label: 'i', subLabel: '8', subKeys: SUBKEYS_MAP['i'] },
      { code: 'o', label: 'o', subLabel: '9', subKeys: SUBKEYS_MAP['o'] },
      { code: 'p', label: 'p', subLabel: '0', subKeys: SUBKEYS_MAP['p'] }
    ],
    [
      { code: 'q', label: 'q', subLabel: '@', subKeys: SUBKEYS_MAP['q'] },
      { code: 's', label: 's', subLabel: '#', subKeys: SUBKEYS_MAP['s'] },
      { code: 'd', label: 'd', subLabel: '$', subKeys: SUBKEYS_MAP['d'] },
      { code: 'f', label: 'f', subLabel: '%', subKeys: SUBKEYS_MAP['f'] },
      { code: 'g', label: 'g', subLabel: '&', subKeys: SUBKEYS_MAP['g'] },
      { code: 'h', label: 'h', subLabel: '-', subKeys: SUBKEYS_MAP['h'] },
      { code: 'j', label: 'j', subLabel: '+', subKeys: SUBKEYS_MAP['j'] },
      { code: 'k', label: 'k', subLabel: '(', subKeys: SUBKEYS_MAP['k'] },
      { code: 'l', label: 'l', subLabel: ')', subKeys: SUBKEYS_MAP['l'] },
      { code: 'm', label: 'm', subLabel: '?', subKeys: SUBKEYS_MAP['m'] }
    ],
    [
      { code: 'shift', label: '⇧', type: 'shift', widthMultiplier: 1.4 },
      { code: 'w', label: 'w', subLabel: '*', subKeys: SUBKEYS_MAP['w'] },
      { code: 'x', label: 'x', subLabel: '"', subKeys: SUBKEYS_MAP['x'] },
      { code: 'c', label: 'c', subLabel: "'", subKeys: SUBKEYS_MAP['c'] },
      { code: 'v', label: 'v', subLabel: ':', subKeys: SUBKEYS_MAP['v'] },
      { code: 'b', label: 'b', subLabel: ';', subKeys: SUBKEYS_MAP['b'] },
      { code: 'n', label: 'n', subLabel: '!', subKeys: SUBKEYS_MAP['n'] },
      { code: 'backspace', label: '⌫', type: 'backspace', widthMultiplier: 1.4 }
    ]
  ],
  colemak: [
    [
      { code: 'q', label: 'q', subLabel: '1' },
      { code: 'w', label: 'w', subLabel: '2' },
      { code: 'f', label: 'f', subLabel: '3' },
      { code: 'p', label: 'p', subLabel: '4' },
      { code: 'g', label: 'g', subLabel: '5' },
      { code: 'j', label: 'j', subLabel: '6' },
      { code: 'l', label: 'l', subLabel: '7' },
      { code: 'u', label: 'u', subLabel: '8' },
      { code: 'y', label: 'y', subLabel: '9' },
      { code: ';', label: ';', subLabel: '0' }
    ],
    [
      { code: 'a', label: 'a', subLabel: '@' },
      { code: 'r', label: 'r', subLabel: '#' },
      { code: 's', label: 's', subLabel: '$' },
      { code: 't', label: 't', subLabel: '%' },
      { code: 'd', label: 'd', subLabel: '&' },
      { code: 'h', label: 'h', subLabel: '-' },
      { code: 'n', label: 'n', subLabel: '+' },
      { code: 'e', label: 'e', subLabel: '(' },
      { code: 'i', label: 'i', subLabel: ')' },
      { code: 'o', label: 'o', subLabel: ':' }
    ],
    [
      { code: 'shift', label: '⇧', type: 'shift', widthMultiplier: 1.4 },
      { code: 'z', label: 'z', subLabel: '*' },
      { code: 'x', label: 'x', subLabel: '"' },
      { code: 'c', label: 'c', subLabel: "'" },
      { code: 'v', label: 'v', subLabel: '/' },
      { code: 'b', label: 'b', subLabel: '!' },
      { code: 'k', label: 'k', subLabel: '?' },
      { code: 'm', label: 'm', subLabel: ',' },
      { code: 'backspace', label: '⌫', type: 'backspace', widthMultiplier: 1.4 }
    ]
  ],
  dvorak: [
    [
      { code: "'", label: "'", subLabel: '1' },
      { code: ',', label: ',', subLabel: '2' },
      { code: '.', label: '.', subLabel: '3' },
      { code: 'p', label: 'p', subLabel: '4' },
      { code: 'y', label: 'y', subLabel: '5' },
      { code: 'f', label: 'f', subLabel: '6' },
      { code: 'g', label: 'g', subLabel: '7' },
      { code: 'c', label: 'c', subLabel: '8' },
      { code: 'r', label: 'r', subLabel: '9' },
      { code: 'l', label: 'l', subLabel: '0' }
    ],
    [
      { code: 'a', label: 'a', subLabel: '@' },
      { code: 'o', label: 'o', subLabel: '#' },
      { code: 'e', label: 'e', subLabel: '$' },
      { code: 'u', label: 'u', subLabel: '%' },
      { code: 'i', label: 'i', subLabel: '&' },
      { code: 'd', label: 'd', subLabel: '-' },
      { code: 'h', label: 'h', subLabel: '+' },
      { code: 't', label: 't', subLabel: '(' },
      { code: 'n', label: 'n', subLabel: ')' },
      { code: 's', label: 's', subLabel: ':' }
    ],
    [
      { code: 'shift', label: '⇧', type: 'shift', widthMultiplier: 1.4 },
      { code: ';', label: ';', subLabel: '*' },
      { code: 'q', label: 'q', subLabel: '"' },
      { code: 'j', label: 'j', subLabel: '!' },
      { code: 'k', label: 'k', subLabel: '?' },
      { code: 'x', label: 'x', subLabel: '/' },
      { code: 'b', label: 'b', subLabel: '=' },
      { code: 'm', label: 'm', subLabel: '\\' },
      { code: 'w', label: 'w', subLabel: '_' },
      { code: 'v', label: 'v', subLabel: 'z' },
      { code: 'z', label: 'z' },
      { code: 'backspace', label: '⌫', type: 'backspace', widthMultiplier: 1.4 }
    ]
  ]
};

export const SYMBOLS_1_ROWS: KeyDefinition[][] = [
  [
    { code: '1', label: '1' },
    { code: '2', label: '2' },
    { code: '3', label: '3' },
    { code: '4', label: '4' },
    { code: '5', label: '5' },
    { code: '6', label: '6' },
    { code: '7', label: '7' },
    { code: '8', label: '8' },
    { code: '9', label: '9' },
    { code: '0', label: '0' }
  ],
  [
    { code: '@', label: '@' },
    { code: '#', label: '#' },
    { code: '$', label: '$' },
    { code: '%', label: '%' },
    { code: '&', label: '&' },
    { code: '-', label: '-' },
    { code: '+', label: '+' },
    { code: '(', label: '(' },
    { code: ')', label: ')' },
    { code: '/', label: '/' }
  ],
  [
    { code: 'mode_symbols2', label: '=\\<', type: 'mode', widthMultiplier: 1.4 },
    { code: '*', label: '*' },
    { code: '"', label: '"' },
    { code: "'", label: "'" },
    { code: ':', label: ':' },
    { code: ';', label: ';' },
    { code: '!', label: '!' },
    { code: '?', label: '?' },
    { code: 'backspace', label: '⌫', type: 'backspace', widthMultiplier: 1.4 }
  ]
];

export const SYMBOLS_2_ROWS: KeyDefinition[][] = [
  [
    { code: '~', label: '~' },
    { code: '`', label: '`' },
    { code: '|', label: '|' },
    { code: '•', label: '•' },
    { code: '√', label: '√' },
    { code: 'π', label: 'π' },
    { code: '÷', label: '÷' },
    { code: '×', label: '×' },
    { code: '¶', label: '¶' },
    { code: '∆', label: '∆' }
  ],
  [
    { code: '£', label: '£' },
    { code: '€', label: '€' },
    { code: '¥', label: '¥' },
    { code: '¢', label: '¢' },
    { code: '^', label: '^' },
    { code: '°', label: '°' },
    { code: '=', label: '=' },
    { code: '{', label: '{' },
    { code: '}', label: '}' },
    { code: '\\', label: '\\' }
  ],
  [
    { code: 'mode_symbols1', label: '?123', type: 'mode', widthMultiplier: 1.4 },
    { code: '%', label: '%' },
    { code: '<', label: '<' },
    { code: '>', label: '>' },
    { code: '[', label: '[' },
    { code: ']', label: ']' },
    { code: '©', label: '©' },
    { code: '®', label: '®' },
    { code: 'backspace', label: '⌫', type: 'backspace', widthMultiplier: 1.4 }
  ]
];
