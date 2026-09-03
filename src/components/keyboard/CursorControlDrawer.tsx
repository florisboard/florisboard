import React, { useState } from 'react';
import {
  ArrowLeft,
  ChevronLeft,
  ChevronRight,
  ChevronUp,
  ChevronDown,
  ChevronsLeft,
  ChevronsRight,
  Scissors,
  Copy,
  Clipboard,
  CheckSquare,
  Delete
} from 'lucide-react';
import { ThemeConfig } from '../../types/keyboard';

interface CursorControlDrawerProps {
  theme: ThemeConfig;
  onMoveCursor: (direction: 'left' | 'right' | 'up' | 'down' | 'start' | 'end', selectMode: boolean) => void;
  onSelectAll: () => void;
  onCut: () => void;
  onCopy: () => void;
  onPaste: () => void;
  onBackspace: () => void;
  onClose: () => void;
}

export const CursorControlDrawer: React.FC<CursorControlDrawerProps> = ({
  theme,
  onMoveCursor,
  onSelectAll,
  onCut,
  onCopy,
  onPaste,
  onBackspace,
  onClose
}) => {
  const [isSelectMode, setIsSelectMode] = useState(false);

  return (
    <div
      className="w-full h-64 flex flex-col select-none transition-colors border-t"
      style={{
        backgroundColor: theme.bgKeyboard,
        borderColor: theme.keyBorder,
        color: theme.textPrimary
      }}
    >
      {/* Header */}
      <div
        className="flex items-center justify-between px-3 py-1.5 border-b"
        style={{ borderColor: theme.keyBorder, backgroundColor: theme.smartbarBg }}
      >
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 rounded-lg hover:opacity-80 active:scale-95 transition-all cursor-pointer"
            style={{ color: theme.accent }}
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <span className="font-semibold text-sm">Text Editing & Cursor</span>
        </div>

        {/* Toggle Select Mode */}
        <button
          type="button"
          onClick={() => setIsSelectMode(!isSelectMode)}
          className="text-xs px-3 py-1 rounded-full font-medium transition-all flex items-center gap-1.5 cursor-pointer border"
          style={{
            backgroundColor: isSelectMode ? theme.accent : theme.bgKeySpecial,
            borderColor: isSelectMode ? theme.accent : theme.keyBorder,
            color: isSelectMode ? theme.accentText : theme.textPrimary
          }}
        >
          <CheckSquare className="w-3.5 h-3.5" />
          <span>Select: {isSelectMode ? 'ON' : 'OFF'}</span>
        </button>
      </div>

      {/* Control Pad Grid */}
      <div className="flex-1 p-3 flex flex-col justify-between">
        {/* Top Action Row: Select All, Cut, Copy, Paste */}
        <div className="grid grid-cols-4 gap-2">
          <button
            type="button"
            onClick={onSelectAll}
            className="h-9 rounded-lg border flex items-center justify-center gap-1 text-xs font-medium hover:opacity-90 active:scale-95 transition-all cursor-pointer"
            style={{
              backgroundColor: theme.bgKey,
              borderColor: theme.keyBorder,
              color: theme.textPrimary
            }}
          >
            <CheckSquare className="w-3.5 h-3.5" />
            <span>Select All</span>
          </button>
          <button
            type="button"
            onClick={onCut}
            className="h-9 rounded-lg border flex items-center justify-center gap-1 text-xs font-medium hover:opacity-90 active:scale-95 transition-all cursor-pointer"
            style={{
              backgroundColor: theme.bgKey,
              borderColor: theme.keyBorder,
              color: theme.textPrimary
            }}
          >
            <Scissors className="w-3.5 h-3.5" />
            <span>Cut</span>
          </button>
          <button
            type="button"
            onClick={onCopy}
            className="h-9 rounded-lg border flex items-center justify-center gap-1 text-xs font-medium hover:opacity-90 active:scale-95 transition-all cursor-pointer"
            style={{
              backgroundColor: theme.bgKey,
              borderColor: theme.keyBorder,
              color: theme.textPrimary
            }}
          >
            <Copy className="w-3.5 h-3.5" />
            <span>Copy</span>
          </button>
          <button
            type="button"
            onClick={onPaste}
            className="h-9 rounded-lg border flex items-center justify-center gap-1 text-xs font-medium hover:opacity-90 active:scale-95 transition-all cursor-pointer"
            style={{
              backgroundColor: theme.bgKey,
              borderColor: theme.keyBorder,
              color: theme.textPrimary
            }}
          >
            <Clipboard className="w-3.5 h-3.5" />
            <span>Paste</span>
          </button>
        </div>

        {/* Center: D-pad & Navigation */}
        <div className="flex items-center justify-center gap-3 my-1">
          {/* Jump Start */}
          <button
            type="button"
            onClick={() => onMoveCursor('start', isSelectMode)}
            className="w-11 h-11 rounded-xl border flex items-center justify-center hover:opacity-90 active:scale-95 transition-all cursor-pointer"
            style={{
              backgroundColor: theme.bgKeySpecial,
              borderColor: theme.keyBorder,
              color: theme.textPrimary
            }}
            title="Jump to Start of text"
          >
            <ChevronsLeft className="w-5 h-5" />
          </button>

          {/* D-Pad Arrows Cluster */}
          <div className="flex flex-col items-center gap-1.5">
            <button
              type="button"
              onClick={() => onMoveCursor('up', isSelectMode)}
              className="w-12 h-10 rounded-xl border flex items-center justify-center hover:opacity-90 active:scale-95 transition-all cursor-pointer"
              style={{
                backgroundColor: theme.bgKey,
                borderColor: theme.keyBorder,
                color: theme.textPrimary
              }}
            >
              <ChevronUp className="w-6 h-6" />
            </button>

            <div className="flex items-center gap-1.5">
              <button
                type="button"
                onClick={() => onMoveCursor('left', isSelectMode)}
                className="w-12 h-10 rounded-xl border flex items-center justify-center hover:opacity-90 active:scale-95 transition-all cursor-pointer"
                style={{
                  backgroundColor: theme.bgKey,
                  borderColor: theme.keyBorder,
                  color: theme.textPrimary
                }}
              >
                <ChevronLeft className="w-6 h-6" />
              </button>

              <div
                className="w-10 h-10 rounded-xl flex items-center justify-center text-xs font-bold font-mono opacity-50"
                style={{ backgroundColor: `${theme.bgKeySpecial}80` }}
              >
                PAD
              </div>

              <button
                type="button"
                onClick={() => onMoveCursor('right', isSelectMode)}
                className="w-12 h-10 rounded-xl border flex items-center justify-center hover:opacity-90 active:scale-95 transition-all cursor-pointer"
                style={{
                  backgroundColor: theme.bgKey,
                  borderColor: theme.keyBorder,
                  color: theme.textPrimary
                }}
              >
                <ChevronRight className="w-6 h-6" />
              </button>
            </div>

            <button
              type="button"
              onClick={() => onMoveCursor('down', isSelectMode)}
              className="w-12 h-10 rounded-xl border flex items-center justify-center hover:opacity-90 active:scale-95 transition-all cursor-pointer"
              style={{
                backgroundColor: theme.bgKey,
                borderColor: theme.keyBorder,
                color: theme.textPrimary
              }}
            >
              <ChevronDown className="w-6 h-6" />
            </button>
          </div>

          {/* Jump End */}
          <button
            type="button"
            onClick={() => onMoveCursor('end', isSelectMode)}
            className="w-11 h-11 rounded-xl border flex items-center justify-center hover:opacity-90 active:scale-95 transition-all cursor-pointer"
            style={{
              backgroundColor: theme.bgKeySpecial,
              borderColor: theme.keyBorder,
              color: theme.textPrimary
            }}
            title="Jump to End of text"
          >
            <ChevronsRight className="w-5 h-5" />
          </button>

          {/* Backspace shortcut in editing panel */}
          <button
            type="button"
            onClick={onBackspace}
            className="w-11 h-11 rounded-xl border flex items-center justify-center hover:opacity-90 active:scale-95 transition-all cursor-pointer ml-1"
            style={{
              backgroundColor: theme.bgKeySpecial,
              borderColor: theme.keyBorder,
              color: theme.textPrimary
            }}
            title="Backspace"
          >
            <Delete className="w-5 h-5" />
          </button>
        </div>
      </div>
    </div>
  );
};
