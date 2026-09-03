import React, { useState, useRef, useEffect, useCallback } from 'react';
import {
  Settings,
  Palette,
  RotateCcw,
  Copy,
  Check,
  Trash2,
  ChevronDown,
  Keyboard as KeyboardIcon,
  Sliders,
  Sparkles
} from 'lucide-react';
import { Smartbar } from '../keyboard/Smartbar';
import { KeyboardLayout } from '../keyboard/KeyboardLayout';
import { EmojiPicker } from '../keyboard/EmojiPicker';
import { ClipboardDrawer } from '../keyboard/ClipboardDrawer';
import { CursorControlDrawer } from '../keyboard/CursorControlDrawer';
import { DictionaryEditorModal } from '../settings/DictionaryEditorModal';
import { AppSettings, ThemeConfig, WordSuggestion } from '../../types/keyboard';
import { dictionaryEngine } from '../../services/dictionaryEngine';
import { clipboardManager } from '../../services/clipboardManager';
import { THEME_PRESETS } from '../../themes/themePresets';

interface DevicePreviewProps {
  settings: AppSettings;
  theme: ThemeConfig;
  onUpdateSettings: (newSettings: AppSettings) => void;
  onSelectTheme: (themeId: string) => void;
  onOpenSettings: () => void;
}

export const DevicePreview: React.FC<DevicePreviewProps> = ({
  settings,
  theme,
  onUpdateSettings,
  onSelectTheme,
  onOpenSettings
}) => {
  // Test text area state
  const [testText, setTestText] = useState('FlorisBoard test input');
  const [cursorPosition, setCursorPosition] = useState(testText.length);
  const [isKeyboardVisible, setIsKeyboardVisible] = useState(true);

  // Active keyboard sub-drawer: null, 'emoji', 'clipboard', 'cursor'
  const [activeDrawer, setActiveDrawer] = useState<'emoji' | 'clipboard' | 'cursor' | null>(null);

  // Suggestions state from dictionary engine
  const [suggestions, setSuggestions] = useState<WordSuggestion[]>([]);
  const [copiedNotification, setCopiedNotification] = useState(false);
  const [isDictionaryModalOpen, setIsDictionaryModalOpen] = useState(false);

  // Sync incognito mode
  useEffect(() => {
    dictionaryEngine.setIncognito(settings.incognitoMode);
  }, [settings.incognitoMode]);

  // Query suggestions based on text before cursor
  const refreshSuggestions = useCallback((text: string, cursorIdx: number) => {
    const beforeCursor = text.slice(0, cursorIdx);
    const { currentWord, precedingText } = dictionaryEngine.getWordContext(beforeCursor);

    const candidates = dictionaryEngine.getSuggestions(currentWord, precedingText, settings.typing);
    setSuggestions(candidates);
  }, [settings.typing]);

  useEffect(() => {
    refreshSuggestions(testText, cursorPosition);
  }, [testText, cursorPosition, refreshSuggestions]);

  // Insert character or word
  const handleInsertText = (str: string) => {
    const before = testText.slice(0, cursorPosition);
    const after = testText.slice(cursorPosition);
    const context = dictionaryEngine.getWordContext(before);
    const isSeparator = str === ' ' || str === '.' || str === ',' || str === '!' || str === '?' || str === '\n';
    let insertedText = str;
    let textBeforeInsert = before;

    if (str === ' ' && settings.typing.doubleSpacePeriod && /\s$/.test(before) && /\S\s$/.test(before)) {
      textBeforeInsert = before.slice(0, -1);
      insertedText = '. ';
    } else if (isSeparator && settings.typing.autoSpaceAfterPunctuation && /[.!?,]$/.test(before) && str !== '\n') {
      insertedText = ` ${str}`;
    }

    if (str.length === 1 && /[a-z]/i.test(str) && settings.typing.autoCapitalization !== 'off') {
      const startsSentence = !before.trim() || /[.!?]\s*$/.test(before);
      const shouldCapitalize = settings.typing.autoCapitalization === 'all_words' || startsSentence;
      if (shouldCapitalize) insertedText = str.toUpperCase();
    }

    const correction = isSeparator && context.currentWord
      ? dictionaryEngine.getAutoCorrection(context.currentWord, context.precedingText, settings.typing)
      : null;
    if (correction) {
      textBeforeInsert = before.slice(0, before.length - context.currentWord.length) + correction;
    }

    const newText = textBeforeInsert + insertedText + after;
    const newPos = textBeforeInsert.length + insertedText.length;

    setTestText(newText);
    setCursorPosition(newPos);

    if (isSeparator) {
      const learnedWord = correction || context.currentWord;
      if (learnedWord && !settings.incognitoMode) {
        dictionaryEngine.learnWord(learnedWord, context.previousWord, settings.typing);
      }
    }
  };

  // Backspace handler
  const handleBackspace = () => {
    if (cursorPosition === 0) return;
    const before = testText.slice(0, cursorPosition - 1);
    const after = testText.slice(cursorPosition);
    setTestText(before + after);
    setCursorPosition(cursorPosition - 1);
  };

  // Delete word handler (for swipe left gesture)
  const handleDeleteWord = () => {
    if (cursorPosition === 0) return;
    const before = testText.slice(0, cursorPosition);
    const after = testText.slice(cursorPosition);
    const match = before.match(/(\s*\S+|\s+)$/);
    if (!match) return;

    const deleteLen = match[0].length;
    const newText = before.slice(0, before.length - deleteLen) + after;
    const newPos = Math.max(0, cursorPosition - deleteLen);

    setTestText(newText);
    setCursorPosition(newPos);
  };

  // Enter handler
  const handleEnter = () => {
    handleInsertText('\n');
  };

  // Cursor movement handler
  const handleMoveCursorDelta = (delta: number) => {
    setCursorPosition(prev => Math.max(0, Math.min(testText.length, prev + delta)));
  };

  // Candidate selection handler
  const handleSelectSuggestion = (word: string) => {
    const before = testText.slice(0, cursorPosition);
    const after = testText.slice(cursorPosition);
    const { currentWord, previousWord } = dictionaryEngine.getWordContext(before);

    const newBefore = before.slice(0, before.length - currentWord.length) + word + ' ';
    const newText = newBefore + after;
    const newPos = newBefore.length;

    setTestText(newText);
    setCursorPosition(newPos);

    if (!settings.incognitoMode) {
      dictionaryEngine.learnWord(word, previousWord, settings.typing);
    }
  };

  // Cycle through themes
  const handleCycleTheme = () => {
    const idx = THEME_PRESETS.findIndex(t => t.id === theme.id);
    const nextTheme = THEME_PRESETS[(idx + 1) % THEME_PRESETS.length];
    if (nextTheme) {
      onSelectTheme(nextTheme.id);
    }
  };

  // Toggle one-handed mode
  const handleToggleOneHanded = () => {
    const current = settings.ui.oneHandedMode;
    const next = current === 'off' ? 'right' : current === 'right' ? 'left' : 'off';
    onUpdateSettings({
      ...settings,
      ui: { ...settings.ui, oneHandedMode: next }
    });
  };

  // Toggle incognito mode
  const handleToggleIncognito = () => {
    onUpdateSettings({
      ...settings,
      incognitoMode: !settings.incognitoMode
    });
  };

  const handleCopyText = async () => {
    if (!testText) return;
    try {
      await navigator.clipboard.writeText(testText);
      clipboardManager.addClip(testText);
      setCopiedNotification(true);
      setTimeout(() => setCopiedNotification(false), 1800);
    } catch {
      clipboardManager.addClip(testText);
    }
  };

  return (
    <div
      className="w-full h-full flex flex-col justify-between select-none transition-colors overflow-hidden"
      style={{
        backgroundColor: theme.bgMain,
        color: theme.textPrimary
      }}
    >
      {/* Top App Bar (FlorisBoard Header) */}
      <header
        className="w-full h-14 px-4 border-b flex items-center justify-between shrink-0 transition-colors"
        style={{
          backgroundColor: theme.bgKeyboard,
          borderColor: theme.keyBorder
        }}
      >
        <div className="flex items-center gap-3">
          <div
            className="w-8 h-8 rounded-xl flex items-center justify-center font-bold text-sm shadow-xs"
            style={{
              backgroundColor: theme.accent,
              color: theme.accentText
            }}
          >
            F
          </div>
          <div>
            <h1 className="font-semibold text-sm sm:text-base leading-tight">FlorisBoard</h1>
            <p className="text-[11px] opacity-60 leading-none">v0.4.0 • Active</p>
          </div>
        </div>

        <div className="flex items-center gap-1.5">
          {/* Theme Quick Switcher */}
          <button
            type="button"
            id="top-theme-btn"
            onClick={handleCycleTheme}
            className="p-2 rounded-xl border hover:opacity-80 active:scale-95 transition-all cursor-pointer text-xs flex items-center gap-1.5"
            style={{
              borderColor: theme.keyBorder,
              backgroundColor: theme.bgKey
            }}
            title={`Switch Theme (Current: ${theme.name})`}
          >
            <Palette className="w-4 h-4" />
            <span className="hidden sm:inline font-medium">{theme.name}</span>
          </button>

          {/* Settings Button */}
          <button
            type="button"
            id="top-settings-btn"
            onClick={onOpenSettings}
            className="p-2 rounded-xl border hover:opacity-80 active:scale-95 transition-all cursor-pointer flex items-center gap-1.5 text-xs font-semibold"
            style={{
              backgroundColor: theme.accent,
              color: theme.accentText,
              borderColor: 'transparent'
            }}
          >
            <Settings className="w-4 h-4" />
            <span className="hidden sm:inline">Settings</span>
          </button>
        </div>
      </header>

      {/* Main Content Area: Test Input Field (Material 3 Style) */}
      <main className="flex-1 w-full max-w-3xl mx-auto p-4 sm:p-6 flex flex-col justify-start overflow-y-auto">
        <div className="w-full flex items-center justify-between mb-2">
          <label className="text-xs font-semibold uppercase tracking-wider opacity-60">
            Keyboard Test Area
          </label>
          <div className="flex items-center gap-2">
            {testText && (
              <>
                <button
                  type="button"
                  onClick={handleCopyText}
                  className="text-xs flex items-center gap-1 opacity-70 hover:opacity-100 cursor-pointer transition-opacity"
                  title="Copy text"
                >
                  {copiedNotification ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                  <span>{copiedNotification ? 'Copied' : 'Copy'}</span>
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setTestText('');
                    setCursorPosition(0);
                  }}
                  className="text-xs flex items-center gap-1 opacity-70 hover:opacity-100 cursor-pointer transition-opacity text-rose-400"
                  title="Clear text"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                  <span>Clear</span>
                </button>
              </>
            )}
          </div>
        </div>

        {/* Interactive Text Display Container */}
        <div
          onClick={() => setIsKeyboardVisible(true)}
          className="w-full min-h-[120px] max-h-[220px] p-4 rounded-2xl border text-base font-sans leading-relaxed whitespace-pre-wrap overflow-y-auto relative cursor-text transition-all focus-within:ring-2"
          style={{
            backgroundColor: theme.bgKeyboard,
            borderColor: theme.keyBorder
          }}
        >
          {testText.length > 0 ? (
            <>
              {testText.slice(0, cursorPosition)}
              <span
                className="inline-block w-0.5 h-5 align-middle animate-pulse mx-[1px]"
                style={{ backgroundColor: theme.accent }}
              />
              {testText.slice(cursorPosition)}
            </>
          ) : (
            <span className="opacity-40 italic flex items-center gap-1.5 text-sm">
              <span
                className="inline-block w-0.5 h-5 align-middle animate-pulse"
                style={{ backgroundColor: theme.accent }}
              />
              Tap here to test keyboard input...
            </span>
          )}
        </div>

        {/* Keyboard Visibility / Quick Toggle Bar */}
        <div className="w-full flex items-center justify-between mt-3 text-xs opacity-60">
          <span>Tap anywhere on the field to focus</span>
          <button
            type="button"
            onClick={() => setIsKeyboardVisible(!isKeyboardVisible)}
            className="flex items-center gap-1 hover:opacity-100 cursor-pointer transition-opacity"
          >
            <KeyboardIcon className="w-3.5 h-3.5" />
            <span>{isKeyboardVisible ? 'Hide Keyboard' : 'Show Keyboard'}</span>
          </button>
        </div>
      </main>

      {/* LOWER SECTION: FlorisBoard Keyboard Interface */}
      {isKeyboardVisible && (
        <footer
          className="w-full shrink-0 flex flex-col border-t transition-colors shadow-2xl"
          style={{
            borderColor: theme.keyBorder,
            backgroundColor: theme.bgKeyboard
          }}
        >
          {/* Smartbar */}
          <Smartbar
            theme={theme}
            suggestions={suggestions}
            onSelectSuggestion={handleSelectSuggestion}
            onOpenClipboard={() => setActiveDrawer(activeDrawer === 'clipboard' ? null : 'clipboard')}
            onOpenCursorControl={() => setActiveDrawer(activeDrawer === 'cursor' ? null : 'cursor')}
            onOpenSettings={onOpenSettings}
            onOpenEmoji={() => setActiveDrawer(activeDrawer === 'emoji' ? null : 'emoji')}
            onCycleTheme={handleCycleTheme}
            onToggleOneHanded={handleToggleOneHanded}
            oneHandedMode={settings.ui.oneHandedMode}
            incognitoMode={settings.incognitoMode}
            onToggleIncognito={handleToggleIncognito}
            isTypingActive={testText.length > 0}
          />

          {/* Sub-Drawers */}
          {activeDrawer === 'emoji' && (
            <EmojiPicker
              theme={theme}
              onSelectEmoji={(emoji) => handleInsertText(emoji)}
              onClose={() => setActiveDrawer(null)}
            />
          )}

          {activeDrawer === 'clipboard' && (
            <ClipboardDrawer
              theme={theme}
              onPasteClip={(clipText) => handleInsertText(clipText)}
              onClose={() => setActiveDrawer(null)}
            />
          )}

          {activeDrawer === 'cursor' && (
            <CursorControlDrawer
              theme={theme}
              onMoveCursor={handleMoveCursorDelta}
              onSelectAll={() => {}}
              onCopy={handleCopyText}
              onCut={() => {
                handleCopyText();
                setTestText('');
                setCursorPosition(0);
              }}
              onPaste={async () => {
                const recent = clipboardManager.getRecentClips();
                if (recent.length > 0 && recent[0]) {
                  handleInsertText(recent[0].text);
                }
              }}
              onClose={() => setActiveDrawer(null)}
            />
          )}

          {/* Main Keyboard Grid */}
          {!activeDrawer && (
            <div
              className={`w-full flex ${
                settings.ui.oneHandedMode === 'left'
                  ? 'justify-start'
                  : settings.ui.oneHandedMode === 'right'
                  ? 'justify-end'
                  : 'justify-center'
              }`}
            >
              <div
                style={{
                  width:
                    settings.ui.oneHandedMode !== 'off'
                      ? `${settings.ui.oneHandedScale}%`
                      : '100%',
                  maxWidth: '720px'
                }}
              >
                <KeyboardLayout
                  settings={settings}
                  theme={theme}
                  onInsertText={handleInsertText}
                  onBackspace={handleBackspace}
                  onDeleteWord={handleDeleteWord}
                  onEnter={handleEnter}
                  onMoveCursorDelta={handleMoveCursorDelta}
                  onOpenEmoji={() => setActiveDrawer('emoji')}
                />
              </div>
            </div>
          )}
        </footer>
      )}

      {/* Dictionary Modal */}
      <DictionaryEditorModal
        theme={theme}
        isOpen={isDictionaryModalOpen}
        onClose={() => setIsDictionaryModalOpen(false)}
        onDictionaryUpdated={() => refreshSuggestions(testText, cursorPosition)}
      />
    </div>
  );
};
