import React, { useState } from 'react';
import {
  Clipboard,
  SlidersHorizontal,
  Settings,
  Smile,
  ChevronRight,
  ChevronLeft,
  EyeOff,
  Palette,
  Move,
  Smartphone,
  CheckCircle2,
  Trash2,
  Volume2
} from 'lucide-react';
import { ThemeConfig, WordSuggestion } from '../../types/keyboard';

interface SmartbarProps {
  theme: ThemeConfig;
  suggestions: WordSuggestion[];
  onSelectSuggestion: (word: string) => void;
  onLongPressSuggestion?: (suggestion: WordSuggestion) => void;
  onOpenClipboard: () => void;
  onOpenCursorControl: () => void;
  onOpenSettings: () => void;
  onOpenEmoji: () => void;
  onCycleTheme: () => void;
  onToggleOneHanded: () => void;
  oneHandedMode: 'off' | 'left' | 'right';
  incognitoMode: boolean;
  onToggleIncognito: () => void;
  isTypingActive: boolean;
}

export const Smartbar: React.FC<SmartbarProps> = ({
  theme,
  suggestions,
  onSelectSuggestion,
  onLongPressSuggestion,
  onOpenClipboard,
  onOpenCursorControl,
  onOpenSettings,
  onOpenEmoji,
  onCycleTheme,
  onToggleOneHanded,
  oneHandedMode,
  incognitoMode,
  onToggleIncognito,
  isTypingActive
}) => {
  const [showActions, setShowActions] = useState(false);
  const [activeLongPressWord, setActiveLongPressWord] = useState<WordSuggestion | null>(null);

  // By default, if user is actively typing with suggestions, show suggestions; otherwise user can toggle
  const displaySuggestions = !showActions && suggestions.length > 0;

  return (
    <div
      className="w-full h-11 px-2 flex items-center justify-between relative border-b select-none transition-colors"
      style={{
        backgroundColor: theme.smartbarBg,
        borderColor: theme.keyBorder,
        color: theme.smartbarText
      }}
    >
      {/* Mode Switch Toggle Button (FlorisBoard arrow / 4-dots toggle) */}
      <button
        type="button"
        id="smartbar-mode-toggle"
        onClick={() => setShowActions(!showActions)}
        className="w-8 h-8 rounded-full flex items-center justify-center hover:opacity-80 active:scale-95 transition-all mr-1 cursor-pointer shrink-0"
        style={{
          backgroundColor: showActions ? theme.accent : 'transparent',
          color: showActions ? theme.accentText : theme.smartbarText
        }}
        title={showActions ? "Show suggestions" : "Quick actions"}
      >
        {showActions ? <ChevronLeft className="w-4 h-4" /> : <ChevronRight className="w-4 h-4" />}
      </button>

      {/* Incognito Indicator Badge */}
      {incognitoMode && (
        <div
          className="flex items-center gap-1 px-1.5 py-0.5 rounded-full text-[10px] font-mono mr-1 shrink-0"
          style={{
            backgroundColor: `${theme.accent}20`,
            color: theme.accent
          }}
          title="Incognito Active: Zero learning & no telemetry"
        >
          <EyeOff className="w-3 h-3" />
          <span className="hidden sm:inline">Private</span>
        </div>
      )}

      {/* Content Area: HeliBoard Suggestions Strip OR FlorisBoard Action Strip */}
      {displaySuggestions ? (
        <div className="flex-1 flex items-center justify-around overflow-x-auto no-scrollbar px-1 gap-1">
          {suggestions.map((sug, idx) => {
            const isCenterCandidate = idx === 0 || (suggestions.length >= 3 && idx === 1);
            return (
              <button
                key={`${sug.word}-${idx}`}
                type="button"
                id={`suggestion-btn-${idx}`}
                onClick={() => onSelectSuggestion(sug.word)}
                onContextMenu={(e) => {
                  e.preventDefault();
                  if (onLongPressSuggestion) onLongPressSuggestion(sug);
                }}
                className={`flex-1 min-w-[70px] max-w-[180px] h-8 px-2 rounded-md flex items-center justify-center text-center truncate cursor-pointer transition-all hover:opacity-90 active:scale-95 ${
                  sug.isAutoCorrect
                    ? 'font-bold tracking-wide'
                    : isCenterCandidate
                    ? 'font-semibold'
                    : 'font-normal opacity-90'
                }`}
                style={{
                  backgroundColor: sug.isAutoCorrect ? `${theme.accent}25` : 'transparent',
                  color: sug.isAutoCorrect ? theme.accent : theme.smartbarText
                }}
              >
                <span className="truncate text-sm sm:text-base">
                  {sug.word}
                </span>

                {/* Subtle indicator if this is a learned user word */}
                {sug.isLearned && (
                  <span
                    className="ml-1 w-1.5 h-1.5 rounded-full shrink-0"
                    style={{ backgroundColor: theme.accent }}
                    title="Learned word"
                  />
                )}
              </button>
            );
          })}
        </div>
      ) : (
        /* FlorisBoard Quick Action Toolbar */
        <div className="flex-1 flex items-center justify-around px-1">
          {/* Clipboard Manager */}
          <button
            type="button"
            id="smartbar-clipboard-btn"
            onClick={onOpenClipboard}
            className="p-1.5 rounded-lg hover:opacity-80 active:scale-95 transition-all cursor-pointer"
            title="Clipboard"
          >
            <Clipboard className="w-4 h-4" />
          </button>

          {/* Text Editing & Cursor D-pad */}
          <button
            type="button"
            id="smartbar-cursor-btn"
            onClick={onOpenCursorControl}
            className="p-1.5 rounded-lg hover:opacity-80 active:scale-95 transition-all cursor-pointer"
            title="Cursor control"
          >
            <Move className="w-4 h-4" />
          </button>

          {/* Emoji & Kaomoji */}
          <button
            type="button"
            id="smartbar-emoji-btn"
            onClick={onOpenEmoji}
            className="p-1.5 rounded-lg hover:opacity-80 active:scale-95 transition-all cursor-pointer"
            title="Emoji"
          >
            <Smile className="w-4 h-4" />
          </button>

          {/* Theme Quick Switcher */}
          <button
            type="button"
            id="smartbar-theme-btn"
            onClick={onCycleTheme}
            className="p-1.5 rounded-lg hover:opacity-80 active:scale-95 transition-all cursor-pointer"
            title={`Theme (${theme.name})`}
          >
            <Palette className="w-4 h-4" />
          </button>

          {/* One-Handed Mode Toggle */}
          <button
            type="button"
            id="smartbar-onehanded-btn"
            onClick={onToggleOneHanded}
            className="p-1.5 rounded-lg hover:opacity-80 active:scale-95 transition-all cursor-pointer"
            title={`One-handed (${oneHandedMode})`}
          >
            <Smartphone
              className={`w-4 h-4 ${oneHandedMode !== 'off' ? 'text-cyan-400' : ''}`}
            />
          </button>

          {/* Incognito / Privacy Toggle */}
          <button
            type="button"
            id="smartbar-incognito-btn"
            onClick={onToggleIncognito}
            className={`p-1.5 rounded-lg hover:opacity-80 active:scale-95 transition-all cursor-pointer ${
              incognitoMode ? 'bg-cyan-500/20 text-cyan-400' : ''
            }`}
            title={incognitoMode ? "Incognito active" : "Incognito mode"}
          >
            <EyeOff className="w-4 h-4" />
          </button>

          {/* Settings */}
          <button
            type="button"
            id="smartbar-settings-btn"
            onClick={onOpenSettings}
            className="p-1.5 rounded-lg hover:opacity-80 active:scale-95 transition-all cursor-pointer"
            title="Settings"
          >
            <Settings className="w-4 h-4" />
          </button>
        </div>
      )}
    </div>
  );
};
