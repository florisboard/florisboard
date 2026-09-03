import React, { useState, useRef, useCallback } from 'react';
import {
  LAYOUT_DEFINITIONS,
  NUMBER_ROW,
  SYMBOLS_1_ROWS,
  SYMBOLS_2_ROWS,
  KeyDefinition
} from '../../data/keyboardLayouts';
import { KeyButton } from './KeyButton';
import { GestureCanvas } from './GestureCanvas';
import { ThemeConfig, AppSettings, KeyboardMode } from '../../types/keyboard';
import { dictionaryEngine } from '../../services/dictionaryEngine';
import { ChevronLeft, ChevronRight, Maximize2, Globe, CornerDownLeft } from 'lucide-react';

interface KeyboardLayoutProps {
  settings: AppSettings;
  theme: ThemeConfig;
  onInsertText: (text: string) => void;
  onBackspace: () => void;
  onDeleteWord: () => void;
  onEnter: () => void;
  onMoveCursorDelta: (delta: number) => void;
  onOpenEmoji: () => void;
}

export const KeyboardLayout: React.FC<KeyboardLayoutProps> = ({
  settings,
  theme,
  onInsertText,
  onBackspace,
  onDeleteWord,
  onEnter,
  onMoveCursorDelta,
  onOpenEmoji
}) => {
  const [mode, setMode] = useState<KeyboardMode>('alpha');
  const [isShifted, setIsShifted] = useState(false);
  const [isCapsLock, setIsCapsLock] = useState(false);
  const lastShiftTapTimeRef = useRef<number>(0);

  // Gesture state for Spacebar Glide
  const [isSpaceGliding, setIsSpaceGliding] = useState(false);
  const spaceGlideStartXRef = useRef<number>(0);
  const spaceAccumulatedDeltaRef = useRef<number>(0);

  // Gesture state for Backspace Swipe
  const [isDeleteSwiping, setIsDeleteSwiping] = useState(false);
  const deleteSwipeStartXRef = useRef<number>(0);
  const deleteSwipeWordsDeletedRef = useRef<number>(0);

  // Gesture state for Glide Typing (Swipe on keys)
  const [gesturePoints, setGesturePoints] = useState<{ x: number; y: number; time: number }[]>([]);
  const isGestureActiveRef = useRef<boolean>(false);
  const gestureKeySequenceRef = useRef<string[]>([]);
  const lastTraversedKeyRef = useRef<string>('');
  const keyboardContainerRef = useRef<HTMLDivElement | null>(null);

  const ui = settings.ui;
  const gestures = settings.gestures;

  // Key press dispatcher
  const handleKeyPress = useCallback((code: string) => {
    if (code === 'shift') {
      const now = Date.now();
      if (now - lastShiftTapTimeRef.current < 300) {
        // Double tap shift -> Toggle Caps Lock
        setIsCapsLock(prev => !prev);
        setIsShifted(false);
      } else {
        if (isCapsLock) {
          setIsCapsLock(false);
          setIsShifted(false);
        } else {
          setIsShifted(prev => !prev);
        }
      }
      lastShiftTapTimeRef.current = now;
      return;
    }

    if (code === 'backspace') {
      onBackspace();
      return;
    }

    if (code === 'enter') {
      onEnter();
      return;
    }

    if (code === 'mode_symbols1') {
      setMode('symbols1');
      return;
    }

    if (code === 'mode_symbols2') {
      setMode('symbols2');
      return;
    }

    if (code === 'mode_alpha') {
      setMode('alpha');
      return;
    }

    if (code === 'emoji') {
      onOpenEmoji();
      return;
    }

    if (code === 'space') {
      onInsertText(' ');
      return;
    }

    // Normal character insertion
    let char = code;
    if (mode === 'alpha') {
      char = (isShifted || isCapsLock) ? code.toUpperCase() : code.toLowerCase();
      // Auto unshift if shift was single tap
      if (isShifted && !isCapsLock) {
        setIsShifted(false);
      }
    }
    onInsertText(char);
  }, [mode, isShifted, isCapsLock, onBackspace, onEnter, onInsertText, onOpenEmoji]);

  // Spacebar Cursor Glide Pointer Event Handlers
  const handleSpacePointerDown = (e: React.PointerEvent) => {
    if (!gestures.spacebarCursorGlide) return;
    spaceGlideStartXRef.current = e.clientX;
    spaceAccumulatedDeltaRef.current = 0;
    setIsSpaceGliding(true);
    (e.target as HTMLElement).setPointerCapture(e.pointerId);
  };

  const handleSpacePointerMove = (e: React.PointerEvent) => {
    if (!isSpaceGliding) return;
    const currentX = e.clientX;
    const diff = currentX - spaceGlideStartXRef.current;
    const threshold = 18 / Math.max(1, gestures.spacebarGlideSensitivity);

    if (Math.abs(diff) >= threshold) {
      const step = diff > 0 ? 1 : -1;
      onMoveCursorDelta(step);
      spaceGlideStartXRef.current = currentX;
    }
  };

  const handleSpacePointerUp = (e: React.PointerEvent) => {
    if (isSpaceGliding) {
      setIsSpaceGliding(false);
      try {
        (e.target as HTMLElement).releasePointerCapture(e.pointerId);
      } catch {
        // ignore
      }
    }
  };

  // Backspace Swipe Left to Delete Pointer Handlers
  const handleBackspacePointerDown = (e: React.PointerEvent) => {
    if (!gestures.deleteSwipe) return;
    deleteSwipeStartXRef.current = e.clientX;
    deleteSwipeWordsDeletedRef.current = 0;
    setIsDeleteSwiping(true);
    (e.target as HTMLElement).setPointerCapture(e.pointerId);
  };

  const handleBackspacePointerMove = (e: React.PointerEvent) => {
    if (!isDeleteSwiping) return;
    const diff = deleteSwipeStartXRef.current - e.clientX; // Leftward distance
    const wordThreshold = 45 / Math.max(1, gestures.deleteSwipeSensitivity);

    if (diff > (deleteSwipeWordsDeletedRef.current + 1) * wordThreshold) {
      deleteSwipeWordsDeletedRef.current += 1;
      onDeleteWord();
      if ('vibrate' in navigator) {
        try {
          navigator.vibrate(15);
        } catch {}
      }
    }
  };

  const handleBackspacePointerUp = (e: React.PointerEvent) => {
    if (isDeleteSwiping) {
      setIsDeleteSwiping(false);
      try {
        (e.target as HTMLElement).releasePointerCapture(e.pointerId);
      } catch {}
    }
  };

  // Glide / Swipe Typing handlers
  const handleContainerPointerDown = (e: React.PointerEvent) => {
    if (!gestures.glideTyping || mode !== 'alpha') return;
    // Don't start glide typing if started directly on spacebar or backspace
    const target = e.target as HTMLElement;
    if (target.closest('#spacebar-btn') || target.closest('#key-backspace') || target.closest('#key-enter')) {
      return;
    }

    const rect = keyboardContainerRef.current?.getBoundingClientRect();
    if (!rect) return;

    isGestureActiveRef.current = true;
    gestureKeySequenceRef.current = [];
    lastTraversedKeyRef.current = '';

    const pt = { x: e.clientX - rect.left, y: e.clientY - rect.top, time: Date.now() };
    setGesturePoints([pt]);
  };

  const handleContainerPointerMove = (e: React.PointerEvent) => {
    if (!isGestureActiveRef.current || !gestures.glideTyping) return;
    const rect = keyboardContainerRef.current?.getBoundingClientRect();
    if (!rect) return;

    const pt = { x: e.clientX - rect.left, y: e.clientY - rect.top, time: Date.now() };
    setGesturePoints(prev => [...prev.slice(-24), pt]);

    // Check key under point
    const element = document.elementFromPoint(e.clientX, e.clientY);
    const keyEl = element?.closest('[id^="key-"]');
    if (keyEl) {
      const code = keyEl.id.replace('key-', '');
      if (code.length === 1 && code !== lastTraversedKeyRef.current && /[a-z]/i.test(code)) {
        gestureKeySequenceRef.current.push(code.toLowerCase());
        lastTraversedKeyRef.current = code;
      }
    }
  };

  const handleContainerPointerUp = () => {
    if (isGestureActiveRef.current && gestures.glideTyping) {
      isGestureActiveRef.current = false;
      const keys = gestureKeySequenceRef.current;
      if (keys.length >= 3) {
        // Decode gesture trail via HeliBoard's dictionary
        const decodedWord = dictionaryEngine.decodeGlideTrail(keys);
        if (decodedWord) {
          const wordToInsert = (isShifted || isCapsLock)
            ? decodedWord.toUpperCase()
            : decodedWord.toLowerCase();
          onInsertText(wordToInsert + ' ');
        }
      }
      setTimeout(() => setGesturePoints([]), gestures.gestureTrailFadeDuration);
    }
  };

  // Determine current active row definitions
  let activeRows: KeyDefinition[][] = [];
  if (mode === 'symbols1') {
    activeRows = SYMBOLS_1_ROWS;
  } else if (mode === 'symbols2') {
    activeRows = SYMBOLS_2_ROWS;
  } else {
    activeRows = LAYOUT_DEFINITIONS[ui.layout] || LAYOUT_DEFINITIONS.qwerty;
  }

  return (
    <div
      ref={keyboardContainerRef}
      onPointerDown={handleContainerPointerDown}
      onPointerMove={handleContainerPointerMove}
      onPointerUp={handleContainerPointerUp}
      onPointerCancel={handleContainerPointerUp}
      className="w-full relative px-1 py-1.5 flex flex-col justify-end select-none touch-none transition-colors"
      style={{
        backgroundColor: theme.bgKeyboard,
        transform: `scale(${ui.keyboardHeightScale / 100})`,
        transformOrigin: 'bottom center'
      }}
    >
      {/* Gesture Trail Ribbon Canvas */}
      {gestures.glideTyping && (
        <GestureCanvas
          points={gesturePoints}
          trailColor={theme.trailColor}
          trailWidth={gestures.gestureTrailWidth}
          fadeDuration={gestures.gestureTrailFadeDuration}
        />
      )}

      {/* Optional Dedicated Number Row (FlorisBoard option) */}
      {ui.showNumberRow && mode === 'alpha' && (
        <div className="flex w-full px-0.5 mb-0.5">
          {NUMBER_ROW.map((key) => (
            <KeyButton
              key={key.code}
              keyDef={key}
              theme={theme}
              subkeysDisplay={ui.subkeysDisplay}
              keyRadius={ui.keyRadius}
              keyBorders={ui.keyBorders}
              keyElevation={ui.keyElevation}
              showKeyPressPopup={ui.showKeyPressPopup}
              isShifted={isShifted}
              isCapsLock={isCapsLock}
              onKeyPress={handleKeyPress}
            />
          ))}
        </div>
      )}

      {/* Main Alphabetical or Symbol Rows */}
      {activeRows.map((row, rowIdx) => (
        <div key={rowIdx} className="flex w-full px-0.5 justify-center">
          {row.map((keyDef) => {
            // Special Backspace handler for swipe
            if (keyDef.type === 'backspace') {
              return (
                <div
                  key={keyDef.code}
                  className="flex-1 min-w-0 h-11 sm:h-12 m-[2px]"
                  style={{ flexGrow: keyDef.widthMultiplier || 1.4 }}
                >
                  <button
                    type="button"
                    id="key-backspace"
                    onPointerDown={handleBackspacePointerDown}
                    onPointerMove={handleBackspacePointerMove}
                    onPointerUp={handleBackspacePointerUp}
                    onClick={() => {
                      if (!isDeleteSwiping) onBackspace();
                    }}
                    className={`w-full h-full relative flex items-center justify-center transition-all duration-75 active:scale-[0.97] outline-none select-none cursor-pointer ${
                      ui.keyElevation ? 'shadow-xs' : ''
                    }`}
                    style={{
                      borderRadius: `${ui.keyRadius}px`,
                      backgroundColor: isDeleteSwiping ? `${theme.accent}30` : theme.bgKeySpecial,
                      color: isDeleteSwiping ? theme.accent : theme.textSecondary,
                      border: ui.keyBorders ? `1px solid ${theme.keyBorder}` : 'none'
                    }}
                    title="Tap to delete, swipe left to delete words"
                  >
                    <span className="font-semibold text-lg leading-none">⌫</span>
                  </button>
                </div>
              );
            }

            return (
              <KeyButton
                key={keyDef.code}
                keyDef={keyDef}
                theme={theme}
                subkeysDisplay={ui.subkeysDisplay}
                keyRadius={ui.keyRadius}
                keyBorders={ui.keyBorders}
                keyElevation={ui.keyElevation}
                showKeyPressPopup={ui.showKeyPressPopup}
                isShifted={isShifted}
                isCapsLock={isCapsLock}
                onKeyPress={handleKeyPress}
                onKeyLongPress={(subChar) => onInsertText(subChar)}
              />
            );
          })}
        </div>
      ))}

      {/* Bottom Row: Mode, Comma, Spacebar, Period, Enter */}
      <div className="flex w-full px-0.5 justify-center items-center">
        {/* Switch Symbols / Alpha mode */}
        <div className="w-14 sm:w-16 h-11 sm:h-12 m-[2px]">
          <button
            type="button"
            id="key-mode-toggle"
            onClick={() => {
              if (mode === 'alpha') setMode('symbols1');
              else setMode('alpha');
            }}
            className="w-full h-full rounded-md flex items-center justify-center font-semibold text-sm transition-all active:scale-[0.97] cursor-pointer"
            style={{
              borderRadius: `${ui.keyRadius}px`,
              backgroundColor: theme.bgKeySpecial,
              color: theme.textSecondary,
              border: ui.keyBorders ? `1px solid ${theme.keyBorder}` : 'none'
            }}
          >
            {mode === 'alpha' ? '?123' : 'ABC'}
          </button>
        </div>

        {/* Emoji Button */}
        <div className="w-10 sm:w-12 h-11 sm:h-12 m-[2px]">
          <button
            type="button"
            id="key-emoji-bottom"
            onClick={onOpenEmoji}
            className="w-full h-full rounded-md flex items-center justify-center text-lg transition-all active:scale-[0.97] cursor-pointer"
            style={{
              borderRadius: `${ui.keyRadius}px`,
              backgroundColor: theme.bgKeySpecial,
              color: theme.textSecondary,
              border: ui.keyBorders ? `1px solid ${theme.keyBorder}` : 'none'
            }}
          >
            😊
          </button>
        </div>

        {/* Comma key */}
        <div className="w-9 sm:w-11 h-11 sm:h-12 m-[2px]">
          <button
            type="button"
            id="key-comma"
            onClick={() => onInsertText(',')}
            className="w-full h-full rounded-md flex items-center justify-center text-lg font-medium transition-all active:scale-[0.97] cursor-pointer"
            style={{
              borderRadius: `${ui.keyRadius}px`,
              backgroundColor: theme.bgKey,
              color: theme.textPrimary,
              border: ui.keyBorders ? `1px solid ${theme.keyBorder}` : 'none'
            }}
          >
            ,
          </button>
        </div>

        {/* Spacebar with Glide Cursor Gesture (FlorisBoard signature) */}
        <div className="flex-1 min-w-[140px] h-11 sm:h-12 m-[2px]">
          <button
            type="button"
            id="spacebar-btn"
            onPointerDown={handleSpacePointerDown}
            onPointerMove={handleSpacePointerMove}
            onPointerUp={handleSpacePointerUp}
            onClick={() => {
              if (!isSpaceGliding) onInsertText(' ');
            }}
            className={`w-full h-full relative flex items-center justify-center transition-all active:scale-[0.98] outline-none select-none cursor-pointer ${
              ui.keyElevation ? 'shadow-xs' : ''
            }`}
            style={{
              borderRadius: `${ui.keyRadius}px`,
              backgroundColor: isSpaceGliding ? theme.bgKeyActive : theme.bgKey,
              color: theme.textSecondary,
              border: ui.keyBorders ? `1px solid ${theme.keyBorder}` : 'none'
            }}
          >
            <span className="text-xs opacity-50 font-normal pointer-events-none tracking-normal">
              English (US)
            </span>
          </button>
        </div>

        {/* Period Key with long press for punctuation */}
        <div className="w-9 sm:w-11 h-11 sm:h-12 m-[2px]">
          <button
            type="button"
            id="key-period"
            onClick={() => onInsertText('.')}
            className="w-full h-full rounded-md flex items-center justify-center text-lg font-medium transition-all active:scale-[0.97] cursor-pointer"
            style={{
              borderRadius: `${ui.keyRadius}px`,
              backgroundColor: theme.bgKey,
              color: theme.textPrimary,
              border: ui.keyBorders ? `1px solid ${theme.keyBorder}` : 'none'
            }}
          >
            .
          </button>
        </div>

        {/* Enter Action Button */}
        <div className="w-14 sm:w-16 h-11 sm:h-12 m-[2px]">
          <button
            type="button"
            id="key-enter"
            onClick={onEnter}
            className="w-full h-full rounded-md flex items-center justify-center text-base transition-all active:scale-[0.97] cursor-pointer"
            style={{
              borderRadius: `${ui.keyRadius}px`,
              backgroundColor: theme.accent,
              color: theme.accentText,
              border: ui.keyBorders ? `1px solid ${theme.keyBorder}` : 'none'
            }}
            title="Return / Enter"
          >
            <CornerDownLeft className="w-5 h-5" />
          </button>
        </div>
      </div>
    </div>
  );
};
