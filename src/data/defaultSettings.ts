import { AppSettings } from '../types/keyboard';

export const DEFAULT_APP_SETTINGS: AppSettings = {
  typing: {
    wordSuggestions: true,
    nextWordSuggestions: true,
    autoCorrection: 'modest',
    learnUserWords: true,
    blockOffensiveWords: true,
    suggestContactNames: true,
    autoCapitalization: 'sentences',
    doubleSpacePeriod: true,
    autoSpaceAfterPunctuation: true,
    expandShortcuts: true,
    suggestionCount: 3,
    minWordLengthForLearning: 2
  },
  ui: {
    layout: 'qwerty',
    showNumberRow: false,
    subkeysDisplay: 'always',
    keyRadius: 8,
    keyBorders: true,
    keyElevation: true,
    showKeyPressPopup: true,
    keyboardHeightScale: 100,
    themeId: 'monet-dynamic',
    vibrationFeedback: true,
    soundFeedback: false,
    oneHandedMode: 'off',
    oneHandedScale: 85
  },
  gestures: {
    spacebarCursorGlide: true,
    spacebarGlideSensitivity: 3,
    deleteSwipe: true,
    deleteSwipeSensitivity: 3,
    glideTyping: true,
    gestureTrailFadeDuration: 250,
    gestureTrailWidth: 4,
    fastSwipeThreshold: 40
  },
  incognitoMode: false
};
