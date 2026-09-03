export type KeyboardLayoutType = 'qwerty' | 'qwertz' | 'azerty' | 'colemak' | 'dvorak';

export type KeyboardMode = 'alpha' | 'symbols1' | 'symbols2' | 'numpad';

export type AutoCorrectAggressiveness = 'off' | 'modest' | 'aggressive' | 'very_aggressive';

export type SubkeysDisplayMode = 'always' | 'long_press' | 'never';

export interface ThemeConfig {
  id: string;
  name: string;
  isDark: boolean;
  bgMain: string;
  bgKeyboard: string;
  bgKey: string;
  bgKeySpecial: string;
  bgKeyActive: string;
  textPrimary: string;
  textSecondary: string;
  accent: string;
  accentText: string;
  keyBorder: string;
  smartbarBg: string;
  smartbarText: string;
  popupBg: string;
  popupText: string;
  trailColor: string;
}

export interface HeliBoardTypingSettings {
  // HeliBoard Dictionary & Suggestions
  wordSuggestions: boolean;
  nextWordSuggestions: boolean;
  autoCorrection: AutoCorrectAggressiveness;
  learnUserWords: boolean;
  blockOffensiveWords: boolean;
  suggestContactNames: boolean;
  autoCapitalization: 'off' | 'sentences' | 'all_words';
  doubleSpacePeriod: boolean;
  autoSpaceAfterPunctuation: boolean;
  expandShortcuts: boolean;
  suggestionCount: number; // 3, 5, 7
  minWordLengthForLearning: number;
}

export interface FlorisBoardUiSettings {
  // FlorisBoard UI & Customization
  layout: KeyboardLayoutType;
  showNumberRow: boolean;
  subkeysDisplay: SubkeysDisplayMode;
  keyRadius: number; // in px: 0, 4, 8, 12, 16
  keyBorders: boolean;
  keyElevation: boolean;
  showKeyPressPopup: boolean;
  keyboardHeightScale: number; // 85% to 125%
  themeId: string;
  vibrationFeedback: boolean;
  soundFeedback: boolean;
  oneHandedMode: 'off' | 'left' | 'right';
  oneHandedScale: number;
}

export interface FlorisBoardGestureSettings {
  // FlorisBoard Gestures
  spacebarCursorGlide: boolean;
  spacebarGlideSensitivity: number; // 1 to 5
  deleteSwipe: boolean;
  deleteSwipeSensitivity: number; // 1 to 5
  glideTyping: boolean;
  gestureTrailFadeDuration: number; // ms
  gestureTrailWidth: number;
  fastSwipeThreshold: number;
}

export interface ClipboardItem {
  id: string;
  text: string;
  timestamp: number;
  pinned: boolean;
}

export interface UserDictionaryEntry {
  id: string;
  word: string;
  frequency: number; // 1 - 255
  shortcut?: string;
  locale: string;
  isCustom: boolean;
  dateAdded: number;
}

export interface WordSuggestion {
  word: string;
  isAutoCorrect: boolean;
  isLearned: boolean;
  isExactMatch: boolean;
  frequency: number;
  confidence: number;
}

export interface AppSettings {
  typing: HeliBoardTypingSettings;
  ui: FlorisBoardUiSettings;
  gestures: FlorisBoardGestureSettings;
  incognitoMode: boolean;
}
