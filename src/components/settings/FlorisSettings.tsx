import React, { useState } from 'react';
import {
  ArrowLeft,
  BookOpen,
  Sliders,
  Palette,
  Move,
  Layout,
  Clipboard,
  ShieldCheck,
  Zap,
  Sparkles,
  Check,
  RotateCcw,
  EyeOff,
  Trash2,
  HardDrive
} from 'lucide-react';
import { AppSettings, ThemeConfig } from '../../types/keyboard';
import { THEME_PRESETS } from '../../themes/themePresets';
import { DictionaryEditorModal } from './DictionaryEditorModal';
import { dictionaryEngine } from '../../services/dictionaryEngine';
import { DEFAULT_APP_SETTINGS } from '../../data/defaultSettings';

interface FlorisSettingsProps {
  settings: AppSettings;
  theme: ThemeConfig;
  onUpdateSettings: (newSettings: AppSettings) => void;
  onSelectTheme: (themeId: string) => void;
  onBack: () => void;
}

export const FlorisSettings: React.FC<FlorisSettingsProps> = ({
  settings,
  theme,
  onUpdateSettings,
  onSelectTheme,
  onBack
}) => {
  const [activeCategory, setActiveCategory] = useState<
    'typing' | 'gestures' | 'layout' | 'themes' | 'smartbar' | 'clipboard' | 'privacy'
  >('typing');

  const [isDictionaryModalOpen, setIsDictionaryModalOpen] = useState(false);
  const [resetConfirm, setResetConfirm] = useState(false);

  const updateTyping = (key: keyof AppSettings['typing'], val: any) => {
    onUpdateSettings({
      ...settings,
      typing: { ...settings.typing, [key]: val }
    });
  };

  const updateUi = (key: keyof AppSettings['ui'], val: any) => {
    onUpdateSettings({
      ...settings,
      ui: { ...settings.ui, [key]: val }
    });
  };

  const updateGestures = (key: keyof AppSettings['gestures'], val: any) => {
    onUpdateSettings({
      ...settings,
      gestures: { ...settings.gestures, [key]: val }
    });
  };

  const handleResetDefaults = () => {
    onUpdateSettings(DEFAULT_APP_SETTINGS);
    setResetConfirm(false);
  };

  const [clearedNotice, setClearedNotice] = useState(false);

  const handleClearLearnedData = () => {
    dictionaryEngine.clearAllUserData();
    setClearedNotice(true);
    setTimeout(() => setClearedNotice(false), 2000);
  };

  return (
    <div
      className="w-full h-full flex flex-col sm:flex-row overflow-hidden select-none transition-colors"
      style={{
        backgroundColor: theme.bgMain,
        color: theme.textPrimary
      }}
    >
      {/* Settings Navigation Sidebar */}
      <div
        className="w-full sm:w-64 border-r flex flex-col shrink-0"
        style={{ borderColor: theme.keyBorder, backgroundColor: theme.bgKeyboard }}
      >
        {/* Top Header */}
        <div
          className="p-4 border-b flex items-center justify-between"
          style={{ borderColor: theme.keyBorder }}
        >
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={onBack}
              className="p-1.5 rounded-lg hover:opacity-80 active:scale-95 transition-all cursor-pointer"
              style={{ color: theme.accent }}
              title="Back"
            >
              <ArrowLeft className="w-5 h-5" />
            </button>
            <div>
              <h1 className="font-bold text-sm leading-none">FlorisBoard</h1>
              <span className="text-[10px] opacity-60 font-mono">Settings</span>
            </div>
          </div>
        </div>

        {/* Categories Menu */}
        <div className="flex-1 overflow-y-auto p-2 space-y-1">
          <button
            type="button"
            onClick={() => setActiveCategory('typing')}
            className={`w-full p-2.5 rounded-xl text-left text-xs font-semibold flex items-center gap-2.5 transition-all cursor-pointer ${
              activeCategory === 'typing' ? 'shadow-xs' : 'opacity-70 hover:opacity-100'
            }`}
            style={{
              backgroundColor: activeCategory === 'typing' ? theme.accent : 'transparent',
              color: activeCategory === 'typing' ? theme.accentText : theme.textPrimary
            }}
          >
            <BookOpen className="w-4 h-4 shrink-0" />
            <div className="flex-1 min-w-0">
              <div className="truncate">Dictionary & Typing</div>
              <div className="text-[10px] opacity-75 font-normal">Predictions, autocorrect & words</div>
            </div>
          </button>

          <button
            type="button"
            onClick={() => setActiveCategory('gestures')}
            className={`w-full p-2.5 rounded-xl text-left text-xs font-semibold flex items-center gap-2.5 transition-all cursor-pointer ${
              activeCategory === 'gestures' ? 'shadow-xs' : 'opacity-70 hover:opacity-100'
            }`}
            style={{
              backgroundColor: activeCategory === 'gestures' ? theme.accent : 'transparent',
              color: activeCategory === 'gestures' ? theme.accentText : theme.textPrimary
            }}
          >
            <Move className="w-4 h-4 shrink-0" />
            <div className="flex-1 min-w-0">
              <div className="truncate">Gestures & Swipes</div>
              <div className="text-[10px] opacity-75 font-normal">Spacebar glide & delete swipe</div>
            </div>
          </button>

          <button
            type="button"
            onClick={() => setActiveCategory('layout')}
            className={`w-full p-2.5 rounded-xl text-left text-xs font-semibold flex items-center gap-2.5 transition-all cursor-pointer ${
              activeCategory === 'layout' ? 'shadow-xs' : 'opacity-70 hover:opacity-100'
            }`}
            style={{
              backgroundColor: activeCategory === 'layout' ? theme.accent : 'transparent',
              color: activeCategory === 'layout' ? theme.accentText : theme.textPrimary
            }}
          >
            <Layout className="w-4 h-4 shrink-0" />
            <div className="flex-1 min-w-0">
              <div className="truncate">Layout & Subkeys</div>
              <div className="text-[10px] opacity-75 font-normal">QWERTY, numbers, popup size</div>
            </div>
          </button>

          <button
            type="button"
            onClick={() => setActiveCategory('themes')}
            className={`w-full p-2.5 rounded-xl text-left text-xs font-semibold flex items-center gap-2.5 transition-all cursor-pointer ${
              activeCategory === 'themes' ? 'shadow-xs' : 'opacity-70 hover:opacity-100'
            }`}
            style={{
              backgroundColor: activeCategory === 'themes' ? theme.accent : 'transparent',
              color: activeCategory === 'themes' ? theme.accentText : theme.textPrimary
            }}
          >
            <Palette className="w-4 h-4 shrink-0" />
            <div className="flex-1 min-w-0">
              <div className="truncate">Themes & Monet</div>
              <div className="text-[10px] opacity-75 font-normal">Material You & custom styles</div>
            </div>
          </button>

          <button
            type="button"
            onClick={() => setActiveCategory('smartbar')}
            className={`w-full p-2.5 rounded-xl text-left text-xs font-semibold flex items-center gap-2.5 transition-all cursor-pointer ${
              activeCategory === 'smartbar' ? 'shadow-xs' : 'opacity-70 hover:opacity-100'
            }`}
            style={{
              backgroundColor: activeCategory === 'smartbar' ? theme.accent : 'transparent',
              color: activeCategory === 'smartbar' ? theme.accentText : theme.textPrimary
            }}
          >
            <Sliders className="w-4 h-4 shrink-0" />
            <div className="flex-1 min-w-0">
              <div className="truncate">Smartbar Options</div>
              <div className="text-[10px] opacity-75 font-normal">Candidate bar & actions</div>
            </div>
          </button>

          <button
            type="button"
            onClick={() => setActiveCategory('clipboard')}
            className={`w-full p-2.5 rounded-xl text-left text-xs font-semibold flex items-center gap-2.5 transition-all cursor-pointer ${
              activeCategory === 'clipboard' ? 'shadow-xs' : 'opacity-70 hover:opacity-100'
            }`}
            style={{
              backgroundColor: activeCategory === 'clipboard' ? theme.accent : 'transparent',
              color: activeCategory === 'clipboard' ? theme.accentText : theme.textPrimary
            }}
          >
            <Clipboard className="w-4 h-4 shrink-0" />
            <div className="flex-1 min-w-0">
              <div className="truncate">Clipboard History</div>
              <div className="text-[10px] opacity-75 font-normal">In-keyboard clips & pins</div>
            </div>
          </button>

          <button
            type="button"
            onClick={() => setActiveCategory('privacy')}
            className={`w-full p-2.5 rounded-xl text-left text-xs font-semibold flex items-center gap-2.5 transition-all cursor-pointer ${
              activeCategory === 'privacy' ? 'shadow-xs' : 'opacity-70 hover:opacity-100'
            }`}
            style={{
              backgroundColor: activeCategory === 'privacy' ? theme.accent : 'transparent',
              color: activeCategory === 'privacy' ? theme.accentText : theme.textPrimary
            }}
          >
            <ShieldCheck className="w-4 h-4 shrink-0" />
            <div className="flex-1 min-w-0">
              <div className="truncate">Privacy & Offline</div>
              <div className="text-[10px] opacity-75 font-normal">Zero-telemetry audit</div>
            </div>
          </button>
        </div>

        {/* Bottom Reset Button */}
        <div className="p-3 border-t" style={{ borderColor: theme.keyBorder }}>
          {resetConfirm ? (
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={handleResetDefaults}
                className="flex-1 py-1.5 px-2 bg-rose-500 hover:bg-rose-600 text-white rounded-lg text-xs font-semibold cursor-pointer"
              >
                Confirm Reset
              </button>
              <button
                type="button"
                onClick={() => setResetConfirm(false)}
                className="py-1.5 px-2 rounded-lg text-xs border cursor-pointer opacity-80"
              >
                Cancel
              </button>
            </div>
          ) : (
            <button
              type="button"
              onClick={() => setResetConfirm(true)}
              className="w-full py-2 px-3 rounded-lg text-xs font-medium border flex items-center justify-center gap-1.5 opacity-70 hover:opacity-100 transition-opacity cursor-pointer"
              style={{ borderColor: theme.keyBorder }}
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>Reset all to default</span>
            </button>
          )}
        </div>
      </div>

      {/* Main Settings Content Panel */}
      <div className="flex-1 overflow-y-auto p-4 sm:p-6 space-y-6">
        {/* SECTION: Typing & Dictionary */}
        {activeCategory === 'typing' && (
          <div className="space-y-6 max-w-2xl">
            <div>
              <h2 className="text-xl font-bold flex items-center gap-2">
                <BookOpen className="w-5 h-5" style={{ color: theme.accent }} />
                <span>Dictionary & Typing</span>
              </h2>
              <p className="text-xs opacity-75 mt-1 leading-relaxed">
                Offline predictive suggestions, dynamic n-gram learning, typo distance model, and privacy-first dictionary.
              </p>
            </div>

            {/* Quick Action to Open Dictionary Manager */}
            <div
              className="p-4 rounded-2xl border flex items-center justify-between"
              style={{ backgroundColor: theme.bgKeyboard, borderColor: theme.keyBorder }}
            >
              <div>
                <h4 className="font-semibold text-sm">Personal Dictionary & Shortcuts</h4>
                <p className="text-xs opacity-70">
                  Manage user words, learned vocabulary, text expansions (e.g. omw), and blacklists.
                </p>
              </div>
              <button
                type="button"
                onClick={() => setIsDictionaryModalOpen(true)}
                className="px-4 py-2 rounded-xl text-xs font-semibold shadow-sm hover:opacity-90 active:scale-95 transition-all cursor-pointer shrink-0"
                style={{ backgroundColor: theme.accent, color: theme.accentText }}
              >
                Manage Dictionary
              </button>
            </div>

            {/* Settings Cards */}
            <div
              className="rounded-2xl border divide-y overflow-hidden"
              style={{ backgroundColor: theme.bgKeyboard, borderColor: theme.keyBorder }}
            >
              {/* Word Suggestions */}
              <label className="p-4 flex items-center justify-between cursor-pointer hover:bg-black/5">
                <div>
                  <div className="font-semibold text-sm">Show Word Suggestions</div>
                  <div className="text-xs opacity-65">Display predictive candidates in the Smartbar</div>
                </div>
                <input
                  type="checkbox"
                  checked={settings.typing.wordSuggestions}
                  onChange={(e) => updateTyping('wordSuggestions', e.target.checked)}
                  className="w-5 h-5 accent-cyan-500 rounded cursor-pointer"
                />
              </label>

              {/* Next Word Suggestions (HeliBoard Bigram Model) */}
              <label className="p-4 flex items-center justify-between cursor-pointer hover:bg-black/5">
                <div>
                  <div className="font-semibold text-sm">Next-Word Suggestions (Bigrams)</div>
                  <div className="text-xs opacity-65">
                    Predictive transition model anticipates subsequent words
                  </div>
                </div>
                <input
                  type="checkbox"
                  checked={settings.typing.nextWordSuggestions}
                  onChange={(e) => updateTyping('nextWordSuggestions', e.target.checked)}
                  className="w-5 h-5 accent-cyan-500 rounded cursor-pointer"
                />
              </label>

              {/* Auto-Correction Aggressiveness */}
              <div className="p-4 space-y-2">
                <div className="font-semibold text-sm">Auto-Correction Sensitivity</div>
                <div className="text-xs opacity-65 mb-2">
                  Spatial keyboard distance error modeling for typo correction
                </div>
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                  {(['off', 'modest', 'aggressive', 'very_aggressive'] as const).map((mode) => (
                    <button
                      key={mode}
                      type="button"
                      onClick={() => updateTyping('autoCorrection', mode)}
                      className={`py-2 px-3 rounded-xl text-xs font-semibold capitalize border transition-all cursor-pointer ${
                        settings.typing.autoCorrection === mode ? 'shadow-xs' : 'opacity-70'
                      }`}
                      style={{
                        backgroundColor: settings.typing.autoCorrection === mode ? theme.accent : theme.bgKey,
                        borderColor: settings.typing.autoCorrection === mode ? theme.accent : theme.keyBorder,
                        color: settings.typing.autoCorrection === mode ? theme.accentText : theme.textPrimary
                      }}
                    >
                      {mode.replace('_', ' ')}
                    </button>
                  ))}
                </div>
              </div>

              {/* Dynamic User Word Learning */}
              <label className="p-4 flex items-center justify-between cursor-pointer hover:bg-black/5">
                <div>
                  <div className="font-semibold text-sm">Offline Word Learning</div>
                  <div className="text-xs opacity-65">
                    Automatically learns novel vocabulary and transition frequencies strictly locally
                  </div>
                </div>
                <input
                  type="checkbox"
                  checked={settings.typing.learnUserWords}
                  onChange={(e) => updateTyping('learnUserWords', e.target.checked)}
                  className="w-5 h-5 accent-cyan-500 rounded cursor-pointer"
                />
              </label>

              {/* Expand Shortcuts */}
              <label className="p-4 flex items-center justify-between cursor-pointer hover:bg-black/5">
                <div>
                  <div className="font-semibold text-sm">Expand Text Shortcuts</div>
                  <div className="text-xs opacity-65">
                    Replaces abbreviations like &quot;omw&quot; with &quot;On my way!&quot;
                  </div>
                </div>
                <input
                  type="checkbox"
                  checked={settings.typing.expandShortcuts}
                  onChange={(e) => updateTyping('expandShortcuts', e.target.checked)}
                  className="w-5 h-5 accent-cyan-500 rounded cursor-pointer"
                />
              </label>

              {/* Block Offensive Words */}
              <label className="p-4 flex items-center justify-between cursor-pointer hover:bg-black/5">
                <div>
                  <div className="font-semibold text-sm">Block Offensive Words</div>
                  <div className="text-xs opacity-65">Filter profanities from suggestions strip</div>
                </div>
                <input
                  type="checkbox"
                  checked={settings.typing.blockOffensiveWords}
                  onChange={(e) => updateTyping('blockOffensiveWords', e.target.checked)}
                  className="w-5 h-5 accent-cyan-500 rounded cursor-pointer"
                />
              </label>

              {/* Double Space Period */}
              <label className="p-4 flex items-center justify-between cursor-pointer hover:bg-black/5">
                <div>
                  <div className="font-semibold text-sm">Double Space Period</div>
                  <div className="text-xs opacity-65">Double-tapping spacebar inserts &quot;. &quot;</div>
                </div>
                <input
                  type="checkbox"
                  checked={settings.typing.doubleSpacePeriod}
                  onChange={(e) => updateTyping('doubleSpacePeriod', e.target.checked)}
                  className="w-5 h-5 accent-cyan-500 rounded cursor-pointer"
                />
              </label>

              {/* Auto Capitalization */}
              <div className="p-4 space-y-2">
                <div className="font-semibold text-sm">Auto-Capitalization</div>
                <div className="flex gap-2">
                  {(['off', 'sentences', 'all_words'] as const).map((opt) => (
                    <button
                      key={opt}
                      type="button"
                      onClick={() => updateTyping('autoCapitalization', opt)}
                      className={`flex-1 py-1.5 rounded-lg text-xs capitalize border font-medium cursor-pointer ${
                        settings.typing.autoCapitalization === opt ? 'font-bold' : 'opacity-70'
                      }`}
                      style={{
                        backgroundColor: settings.typing.autoCapitalization === opt ? theme.accent : theme.bgKey,
                        borderColor: settings.typing.autoCapitalization === opt ? theme.accent : theme.keyBorder,
                        color: settings.typing.autoCapitalization === opt ? theme.accentText : theme.textPrimary
                      }}
                    >
                      {opt.replace('_', ' ')}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* SECTION: Gestures (FlorisBoard Gestures) */}
        {activeCategory === 'gestures' && (
          <div className="space-y-6 max-w-2xl">
            <div>
              <h2 className="text-xl font-bold flex items-center gap-2">
                <Move className="w-5 h-5" style={{ color: theme.accent }} />
                <span>FlorisBoard Signature Gestures</span>
              </h2>
              <p className="text-xs opacity-75 mt-1 leading-relaxed">
                Hardware-accelerated touch gestures preserved from FlorisBoard, including spacebar cursor gliding and delete swiping.
              </p>
            </div>

            <div
              className="rounded-2xl border divide-y overflow-hidden"
              style={{ backgroundColor: theme.bgKeyboard, borderColor: theme.keyBorder }}
            >
              {/* Spacebar Cursor Glide */}
              <div className="p-4 space-y-2">
                <label className="flex items-center justify-between cursor-pointer">
                  <div>
                    <div className="font-semibold text-sm">Spacebar Cursor Glide</div>
                    <div className="text-xs opacity-65">
                      Slide left/right on spacebar to scrub cursor smoothly through text
                    </div>
                  </div>
                  <input
                    type="checkbox"
                    checked={settings.gestures.spacebarCursorGlide}
                    onChange={(e) => updateGestures('spacebarCursorGlide', e.target.checked)}
                    className="w-5 h-5 accent-cyan-500 rounded cursor-pointer"
                  />
                </label>

                {settings.gestures.spacebarCursorGlide && (
                  <div className="pt-2">
                    <div className="flex justify-between text-xs opacity-75 mb-1 font-mono">
                      <span>Glide Sensitivity</span>
                      <span>Level {settings.gestures.spacebarGlideSensitivity} / 5</span>
                    </div>
                    <input
                      type="range"
                      min={1}
                      max={5}
                      value={settings.gestures.spacebarGlideSensitivity}
                      onChange={(e) => updateGestures('spacebarGlideSensitivity', Number(e.target.value))}
                      className="w-full accent-cyan-500 cursor-pointer"
                    />
                  </div>
                )}
              </div>

              {/* Delete Swipe Left */}
              <div className="p-4 space-y-2">
                <label className="flex items-center justify-between cursor-pointer">
                  <div>
                    <div className="font-semibold text-sm">Backspace Swipe-to-Delete</div>
                    <div className="text-xs opacity-65">
                      Swipe left from backspace to quickly erase words in sequence
                    </div>
                  </div>
                  <input
                    type="checkbox"
                    checked={settings.gestures.deleteSwipe}
                    onChange={(e) => updateGestures('deleteSwipe', e.target.checked)}
                    className="w-5 h-5 accent-cyan-500 rounded cursor-pointer"
                  />
                </label>

                {settings.gestures.deleteSwipe && (
                  <div className="pt-2">
                    <div className="flex justify-between text-xs opacity-75 mb-1 font-mono">
                      <span>Swipe Sensitivity</span>
                      <span>Level {settings.gestures.deleteSwipeSensitivity} / 5</span>
                    </div>
                    <input
                      type="range"
                      min={1}
                      max={5}
                      value={settings.gestures.deleteSwipeSensitivity}
                      onChange={(e) => updateGestures('deleteSwipeSensitivity', Number(e.target.value))}
                      className="w-full accent-cyan-500 cursor-pointer"
                    />
                  </div>
                )}
              </div>

              {/* Glide / Swipe Typing */}
              <div className="p-4 space-y-3">
                <label className="flex items-center justify-between cursor-pointer">
                  <div>
                    <div className="font-semibold text-sm">Glide / Gesture Typing</div>
                    <div className="text-xs opacity-65">
                      Draw a continuous trail across letters to type words
                    </div>
                  </div>
                  <input
                    type="checkbox"
                    checked={settings.gestures.glideTyping}
                    onChange={(e) => updateGestures('glideTyping', e.target.checked)}
                    className="w-5 h-5 accent-cyan-500 rounded cursor-pointer"
                  />
                </label>

                {settings.gestures.glideTyping && (
                  <div className="space-y-2 pt-2">
                    <div className="flex justify-between text-xs opacity-75 font-mono">
                      <span>Trail Fade Time: {settings.gestures.gestureTrailFadeDuration}ms</span>
                    </div>
                    <input
                      type="range"
                      min={150}
                      max={600}
                      step={25}
                      value={settings.gestures.gestureTrailFadeDuration}
                      onChange={(e) => updateGestures('gestureTrailFadeDuration', Number(e.target.value))}
                      className="w-full accent-cyan-500 cursor-pointer"
                    />

                    <div className="flex justify-between text-xs opacity-75 font-mono">
                      <span>Trail Width: {settings.gestures.gestureTrailWidth}px</span>
                    </div>
                    <input
                      type="range"
                      min={2}
                      max={8}
                      value={settings.gestures.gestureTrailWidth}
                      onChange={(e) => updateGestures('gestureTrailWidth', Number(e.target.value))}
                      className="w-full accent-cyan-500 cursor-pointer"
                    />
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        {/* SECTION: Layout & Subkeys (FlorisBoard Layouts) */}
        {activeCategory === 'layout' && (
          <div className="space-y-6 max-w-2xl">
            <div>
              <h2 className="text-xl font-bold flex items-center gap-2">
                <Layout className="w-5 h-5" style={{ color: theme.accent }} />
                <span>Layout & Key Styling</span>
              </h2>
              <p className="text-xs opacity-75 mt-1 leading-relaxed">
                Full FlorisBoard visual customization for key shapes, subkeys, and number rows.
              </p>
            </div>

            <div
              className="rounded-2xl border divide-y overflow-hidden"
              style={{ backgroundColor: theme.bgKeyboard, borderColor: theme.keyBorder }}
            >
              {/* Keyboard Layout Type */}
              <div className="p-4 space-y-2">
                <div className="font-semibold text-sm">Keyboard Layout</div>
                <div className="grid grid-cols-3 sm:grid-cols-5 gap-2">
                  {(['qwerty', 'qwertz', 'azerty', 'colemak', 'dvorak'] as const).map((l) => (
                    <button
                      key={l}
                      type="button"
                      onClick={() => updateUi('layout', l)}
                      className={`py-2 px-2 rounded-xl text-xs font-semibold uppercase border transition-all cursor-pointer ${
                        settings.ui.layout === l ? 'shadow-xs' : 'opacity-70'
                      }`}
                      style={{
                        backgroundColor: settings.ui.layout === l ? theme.accent : theme.bgKey,
                        borderColor: settings.ui.layout === l ? theme.accent : theme.keyBorder,
                        color: settings.ui.layout === l ? theme.accentText : theme.textPrimary
                      }}
                    >
                      {l}
                    </button>
                  ))}
                </div>
              </div>

              {/* Subkeys Display Mode */}
              <div className="p-4 space-y-2">
                <div className="font-semibold text-sm">Subkey Hints Display</div>
                <div className="text-xs opacity-65 mb-2">Show secondary characters in key corner</div>
                <div className="grid grid-cols-3 gap-2">
                  {(['always', 'long_press', 'never'] as const).map((m) => (
                    <button
                      key={m}
                      type="button"
                      onClick={() => updateUi('subkeysDisplay', m)}
                      className={`py-2 px-2 rounded-xl text-xs font-semibold capitalize border transition-all cursor-pointer ${
                        settings.ui.subkeysDisplay === m ? 'shadow-xs' : 'opacity-70'
                      }`}
                      style={{
                        backgroundColor: settings.ui.subkeysDisplay === m ? theme.accent : theme.bgKey,
                        borderColor: settings.ui.subkeysDisplay === m ? theme.accent : theme.keyBorder,
                        color: settings.ui.subkeysDisplay === m ? theme.accentText : theme.textPrimary
                      }}
                    >
                      {m.replace('_', ' ')}
                    </button>
                  ))}
                </div>
              </div>

              {/* Dedicated Number Row */}
              <label className="p-4 flex items-center justify-between cursor-pointer hover:bg-black/5">
                <div>
                  <div className="font-semibold text-sm">Dedicated Number Row</div>
                  <div className="text-xs opacity-65">Persistent row for 0-9 digits at the top</div>
                </div>
                <input
                  type="checkbox"
                  checked={settings.ui.showNumberRow}
                  onChange={(e) => updateUi('showNumberRow', e.target.checked)}
                  className="w-5 h-5 accent-cyan-500 rounded cursor-pointer"
                />
              </label>

              {/* Show Key Press Popup Bubble */}
              <label className="p-4 flex items-center justify-between cursor-pointer hover:bg-black/5">
                <div>
                  <div className="font-semibold text-sm">Key Press Magnifier Popup</div>
                  <div className="text-xs opacity-65">Show enlarged preview bubble above pressed key</div>
                </div>
                <input
                  type="checkbox"
                  checked={settings.ui.showKeyPressPopup}
                  onChange={(e) => updateUi('showKeyPressPopup', e.target.checked)}
                  className="w-5 h-5 accent-cyan-500 rounded cursor-pointer"
                />
              </label>

              {/* Key Corner Radius */}
              <div className="p-4 space-y-2">
                <div className="flex justify-between text-sm font-semibold">
                  <span>Key Corner Radius</span>
                  <span className="font-mono text-xs">{settings.ui.keyRadius}px</span>
                </div>
                <input
                  type="range"
                  min={0}
                  max={16}
                  step={2}
                  value={settings.ui.keyRadius}
                  onChange={(e) => updateUi('keyRadius', Number(e.target.value))}
                  className="w-full accent-cyan-500 cursor-pointer"
                />
              </div>

              {/* Key Borders */}
              <label className="p-4 flex items-center justify-between cursor-pointer hover:bg-black/5">
                <div>
                  <div className="font-semibold text-sm">Key Borders</div>
                  <div className="text-xs opacity-65">Subtle border line around each key</div>
                </div>
                <input
                  type="checkbox"
                  checked={settings.ui.keyBorders}
                  onChange={(e) => updateUi('keyBorders', e.target.checked)}
                  className="w-5 h-5 accent-cyan-500 rounded cursor-pointer"
                />
              </label>

              {/* Key Height Scale */}
              <div className="p-4 space-y-2">
                <div className="flex justify-between text-sm font-semibold">
                  <span>Keyboard Height Scaling</span>
                  <span className="font-mono text-xs">{settings.ui.keyboardHeightScale}%</span>
                </div>
                <input
                  type="range"
                  min={85}
                  max={120}
                  value={settings.ui.keyboardHeightScale}
                  onChange={(e) => updateUi('keyboardHeightScale', Number(e.target.value))}
                  className="w-full accent-cyan-500 cursor-pointer"
                />
              </div>
            </div>
          </div>
        )}

        {/* SECTION: Themes & Styling (FlorisBoard Themes) */}
        {activeCategory === 'themes' && (
          <div className="space-y-6 max-w-2xl">
            <div>
              <h2 className="text-xl font-bold flex items-center gap-2">
                <Palette className="w-5 h-5" style={{ color: theme.accent }} />
                <span>FlorisBoard Theme Presets & Monet</span>
              </h2>
              <p className="text-xs opacity-75 mt-1 leading-relaxed">
                Material You dynamic theming and high-contrast OLED styles.
              </p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {THEME_PRESETS.map((t) => {
                const isCurrent = t.id === theme.id;
                return (
                  <div
                    key={t.id}
                    onClick={() => onSelectTheme(t.id)}
                    className={`p-3.5 rounded-2xl border flex flex-col justify-between gap-3 cursor-pointer transition-all hover:scale-[1.01] ${
                      isCurrent ? 'ring-2' : 'opacity-90'
                    }`}
                    style={{
                      backgroundColor: t.bgKeyboard,
                      borderColor: isCurrent ? t.accent : t.keyBorder,
                      ringColor: t.accent
                    }}
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-semibold text-sm" style={{ color: t.textPrimary }}>
                        {t.name}
                      </span>
                      {isCurrent && (
                        <span
                          className="px-2 py-0.5 rounded-full text-[10px] font-bold flex items-center gap-1"
                          style={{ backgroundColor: t.accent, color: t.accentText }}
                        >
                          <Check className="w-3 h-3" /> Active
                        </span>
                      )}
                    </div>

                    {/* Mini Keyboard preview swatch */}
                    <div
                      className="p-2 rounded-xl border flex items-center justify-between gap-1"
                      style={{ backgroundColor: t.bgMain, borderColor: t.keyBorder }}
                    >
                      <div
                        className="w-8 h-6 rounded flex items-center justify-center text-xs font-bold"
                        style={{ backgroundColor: t.bgKey, color: t.textPrimary }}
                      >
                        Q
                      </div>
                      <div
                        className="w-8 h-6 rounded flex items-center justify-center text-xs font-bold"
                        style={{ backgroundColor: t.bgKey, color: t.textPrimary }}
                      >
                        W
                      </div>
                      <div
                        className="w-8 h-6 rounded flex items-center justify-center text-xs font-bold"
                        style={{ backgroundColor: t.bgKey, color: t.textPrimary }}
                      >
                        E
                      </div>
                      <div
                        className="flex-1 h-6 rounded flex items-center justify-center text-[10px] font-semibold"
                        style={{ backgroundColor: t.accent, color: t.accentText }}
                      >
                        ↵
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* SECTION: Smartbar Options */}
        {activeCategory === 'smartbar' && (
          <div className="space-y-6 max-w-2xl">
            <div>
              <h2 className="text-xl font-bold flex items-center gap-2">
                <Sliders className="w-5 h-5" style={{ color: theme.accent }} />
                <span>FlorisBoard Smartbar</span>
              </h2>
              <p className="text-xs opacity-75 mt-1 leading-relaxed">
                Configure candidate suggestions and quick action tools.
              </p>
            </div>

            <div
              className="rounded-2xl border divide-y overflow-hidden"
              style={{ backgroundColor: theme.bgKeyboard, borderColor: theme.keyBorder }}
            >
              <div className="p-4 space-y-2">
                <div className="font-semibold text-sm">Suggestions Strip Capacity</div>
                <div className="text-xs opacity-65 mb-2">Number of words simultaneously shown</div>
                <div className="flex gap-2">
                  {[3, 5, 7].map((cnt) => (
                    <button
                      key={cnt}
                      type="button"
                      onClick={() => updateTyping('suggestionCount', cnt)}
                      className={`flex-1 py-2 rounded-xl text-xs font-semibold border cursor-pointer ${
                        settings.typing.suggestionCount === cnt ? 'shadow-xs' : 'opacity-70'
                      }`}
                      style={{
                        backgroundColor: settings.typing.suggestionCount === cnt ? theme.accent : theme.bgKey,
                        borderColor: settings.typing.suggestionCount === cnt ? theme.accent : theme.keyBorder,
                        color: settings.typing.suggestionCount === cnt ? theme.accentText : theme.textPrimary
                      }}
                    >
                      {cnt} candidates
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* SECTION: Clipboard History */}
        {activeCategory === 'clipboard' && (
          <div className="space-y-6 max-w-2xl">
            <div>
              <h2 className="text-xl font-bold flex items-center gap-2">
                <Clipboard className="w-5 h-5" style={{ color: theme.accent }} />
                <span>FlorisBoard Clipboard History</span>
              </h2>
              <p className="text-xs opacity-75 mt-1 leading-relaxed">
                Manage in-keyboard clipboard retention and permanent item pins.
              </p>
            </div>

            <div
              className="p-4 rounded-2xl border space-y-3"
              style={{ backgroundColor: theme.bgKeyboard, borderColor: theme.keyBorder }}
            >
              <h4 className="text-sm font-semibold">Offline Clipboard Storage</h4>
              <p className="text-xs opacity-70">
                Copied text is cached strictly on-device. Pinned clips are retained indefinitely until manually deleted.
              </p>
            </div>
          </div>
        )}

        {/* SECTION: Privacy & Security */}
        {activeCategory === 'privacy' && (
          <div className="space-y-6 max-w-2xl">
            <div>
              <h2 className="text-xl font-bold flex items-center gap-2">
                <ShieldCheck className="w-5 h-5" style={{ color: theme.accent }} />
                <span>Privacy & Offline Security</span>
              </h2>
              <p className="text-xs opacity-75 mt-1 leading-relaxed">
                Strict offline architecture: 0 network requests, 0 telemetry beacons, and 100% private on-device operation.
              </p>
            </div>

            <div
              className="p-5 rounded-2xl border space-y-4"
              style={{ backgroundColor: `${theme.accent}15`, borderColor: theme.accent }}
            >
              <div className="flex items-center gap-2">
                <ShieldCheck className="w-6 h-6" style={{ color: theme.accent }} />
                <h3 className="font-bold text-base">Privacy Guarantee</h3>
              </div>
              <ul className="text-xs space-y-2 opacity-90 list-disc list-inside">
                <li>Zero network permissions required or used.</li>
                <li>All keystrokes, learned words, and bigrams remain in local browser storage only.</li>
                <li>Incognito mode completely bypasses clipboard and word learning.</li>
                <li>Full control to backup, restore, or wipe local user dictionaries at any time.</li>
              </ul>
            </div>

            <div
              className="p-4 rounded-2xl border space-y-3"
              style={{ backgroundColor: theme.bgKeyboard, borderColor: theme.keyBorder }}
            >
              <h4 className="font-semibold text-sm text-rose-400 flex items-center gap-2">
                <Trash2 className="w-4 h-4" />
                <span>Erase Learned History</span>
              </h4>
              <p className="text-xs opacity-70">
                Permanently purge all learned user words, bigram frequency links, and typing statistics.
              </p>
              <button
                type="button"
                onClick={handleClearLearnedData}
                className="px-4 py-2 bg-rose-500 hover:bg-rose-600 text-white rounded-xl text-xs font-semibold cursor-pointer transition-colors"
              >
                Clear Learned History
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Dictionary Editor Modal */}
      <DictionaryEditorModal
        theme={theme}
        isOpen={isDictionaryModalOpen}
        onClose={() => setIsDictionaryModalOpen(false)}
        onDictionaryUpdated={() => {}}
      />
    </div>
  );
};
