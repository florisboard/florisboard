import React, { useState, useRef, useEffect } from 'react';
import { KeyDefinition } from '../../data/keyboardLayouts';
import { ThemeConfig, SubkeysDisplayMode } from '../../types/keyboard';

interface KeyButtonProps {
  keyDef: KeyDefinition;
  theme: ThemeConfig;
  subkeysDisplay: SubkeysDisplayMode;
  keyRadius: number;
  keyBorders: boolean;
  keyElevation: boolean;
  showKeyPressPopup: boolean;
  isShifted: boolean;
  isCapsLock: boolean;
  onKeyPress: (code: string) => void;
  onKeyLongPress?: (subChar: string) => void;
  disabled?: boolean;
}

export const KeyButton: React.FC<KeyButtonProps> = ({
  keyDef,
  theme,
  subkeysDisplay,
  keyRadius,
  keyBorders,
  keyElevation,
  showKeyPressPopup,
  isShifted,
  isCapsLock,
  onKeyPress,
  onKeyLongPress,
  disabled
}) => {
  const [isPressed, setIsPressed] = useState(false);
  const [showSubkeyPopup, setShowSubkeyPopup] = useState(false);
  const [selectedSubkeyIndex, setSelectedSubkeyIndex] = useState(0);

  const longPressTimerRef = useRef<NodeJS.Timeout | null>(null);
  const pressStartTimeRef = useRef<number>(0);

  const isSpecial = keyDef.type && keyDef.type !== 'char';
  const isShift = keyDef.type === 'shift';
  const isBackspace = keyDef.type === 'backspace';

  // Compute displayed label based on shift/caps
  let displayLabel = keyDef.label;
  if (!keyDef.type || keyDef.type === 'char') {
    if (isShifted || isCapsLock) {
      displayLabel = displayLabel.toUpperCase();
    } else {
      displayLabel = displayLabel.toLowerCase();
    }
  }

  const subkeys = keyDef.subKeys || (keyDef.subLabel ? [keyDef.subLabel] : []);

  const handlePointerDown = (e: React.PointerEvent) => {
    if (disabled) return;
    setIsPressed(true);
    pressStartTimeRef.current = Date.now();

    // Trigger haptic if available in browser
    if ('vibrate' in navigator) {
      try {
        navigator.vibrate(10);
      } catch {
        // ignore
      }
    }

    if (subkeys.length > 0) {
      longPressTimerRef.current = setTimeout(() => {
        setShowSubkeyPopup(true);
        setSelectedSubkeyIndex(0);
        if ('vibrate' in navigator) {
          try {
            navigator.vibrate(25);
          } catch {
            // ignore
          }
        }
      }, 350);
    }
  };

  const handlePointerMove = (e: React.PointerEvent) => {
    if (!showSubkeyPopup || subkeys.length === 0) return;
    const rect = e.currentTarget.getBoundingClientRect();
    const relX = e.clientX - rect.left;
    // Calculate which subkey candidate is closest
    const subkeyWidth = rect.width / subkeys.length;
    const index = Math.max(0, Math.min(subkeys.length - 1, Math.floor(relX / subkeyWidth)));
    setSelectedSubkeyIndex(index);
  };

  const handlePointerUp = () => {
    if (longPressTimerRef.current) {
      clearTimeout(longPressTimerRef.current);
      longPressTimerRef.current = null;
    }

    if (showSubkeyPopup && subkeys.length > 0) {
      const selected = subkeys[selectedSubkeyIndex];
      if (selected && onKeyLongPress) {
        onKeyLongPress(selected);
      }
      setShowSubkeyPopup(false);
    } else if (isPressed) {
      onKeyPress(keyDef.code);
    }

    setIsPressed(false);
  };

  const handlePointerCancel = () => {
    if (longPressTimerRef.current) {
      clearTimeout(longPressTimerRef.current);
      longPressTimerRef.current = null;
    }
    setIsPressed(false);
    setShowSubkeyPopup(false);
  };

  useEffect(() => {
    return () => {
      if (longPressTimerRef.current) {
        clearTimeout(longPressTimerRef.current);
      }
    };
  }, []);

  const flexBasis = keyDef.widthMultiplier ? `${keyDef.widthMultiplier * 9.5}%` : '9.5%';

  // Determine button background
  let bgStyle = isSpecial ? theme.bgKeySpecial : theme.bgKey;
  if (isPressed) bgStyle = theme.bgKeyActive;
  if (isShift && (isShifted || isCapsLock)) {
    bgStyle = theme.accent;
  }

  const textColor = isShift && (isShifted || isCapsLock)
    ? theme.accentText
    : isSpecial
    ? theme.textSecondary
    : theme.textPrimary;

  return (
    <div
      className="relative flex-1 min-w-0 h-11 sm:h-12 select-none touch-manipulation flex items-center justify-center m-[2px]"
      style={{
        flexGrow: keyDef.widthMultiplier || 1,
        maxWidth: keyDef.type === 'space' ? '50%' : undefined
      }}
    >
      {/* FlorisBoard Key Press Magnifier / Popup Bubble */}
      {showKeyPressPopup && isPressed && !showSubkeyPopup && !isSpecial && (
        <div
          className="absolute -top-12 left-1/2 -translate-x-1/2 w-12 h-14 rounded-t-xl rounded-b-md shadow-2xl flex flex-col items-center justify-start pt-1.5 z-40 pointer-events-none transition-all"
          style={{
            backgroundColor: theme.popupBg,
            color: theme.popupText,
            border: `1px solid ${theme.keyBorder}`
          }}
        >
          <span className="text-2xl font-semibold leading-none">{displayLabel}</span>
          {keyDef.subLabel && (
            <span className="text-[10px] opacity-70 mt-1 font-mono">{keyDef.subLabel}</span>
          )}
        </div>
      )}

      {/* FlorisBoard Extended Subkeys Long-Press Popup */}
      {showSubkeyPopup && subkeys.length > 0 && (
        <div
          className="absolute -top-14 left-1/2 -translate-x-1/2 flex items-center gap-1.5 px-2 py-1.5 rounded-xl shadow-2xl z-50 transition-all border animate-in fade-in zoom-in-95 duration-100"
          style={{
            backgroundColor: theme.popupBg,
            borderColor: theme.accent,
            color: theme.popupText
          }}
        >
          {subkeys.map((sub, idx) => {
            const isSelected = idx === selectedSubkeyIndex;
            return (
              <div
                key={idx}
                className="w-8 h-9 rounded-lg flex items-center justify-center text-lg font-medium transition-colors"
                style={{
                  backgroundColor: isSelected ? theme.accent : 'transparent',
                  color: isSelected ? theme.accentText : theme.popupText
                }}
              >
                {isShifted || isCapsLock ? sub.toUpperCase() : sub}
              </div>
            );
          })}
        </div>
      )}

      {/* Actual Key Body */}
      <button
        type="button"
        id={`key-${keyDef.code}`}
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
        onPointerCancel={handlePointerCancel}
        className={`w-full h-full relative flex items-center justify-center transition-all duration-75 active:scale-[0.97] outline-none select-none cursor-pointer ${
          keyElevation ? 'shadow-xs' : ''
        }`}
        style={{
          borderRadius: `${keyRadius}px`,
          backgroundColor: bgStyle,
          color: textColor,
          border: keyBorders ? `1px solid ${theme.keyBorder}` : 'none'
        }}
      >
        {/* Subkey Hint Label (FlorisBoard signature corner sub-label) */}
        {subkeysDisplay === 'always' && keyDef.subLabel && !isSpecial && (
          <span
            className="absolute top-0.5 right-1 text-[9px] font-sans pointer-events-none opacity-60 font-semibold"
            style={{ color: theme.textSecondary }}
          >
            {keyDef.subLabel}
          </span>
        )}

        {/* Shift CapsLock Dot indicator (FlorisBoard detail) */}
        {isShift && isCapsLock && (
          <span
            className="absolute top-1 left-1.5 w-1.5 h-1.5 rounded-full"
            style={{ backgroundColor: theme.accentText }}
          />
        )}

        {/* Main Key Label or Icon */}
        <span
          className={`font-sans tracking-wide leading-none ${
            isSpecial ? 'text-sm font-semibold' : 'text-lg sm:text-xl font-normal'
          }`}
        >
          {displayLabel}
        </span>
      </button>
    </div>
  );
};
