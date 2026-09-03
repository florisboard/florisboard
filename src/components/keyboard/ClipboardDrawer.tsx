import React, { useState, useEffect } from 'react';
import { ArrowLeft, Pin, Trash2, Check, Copy } from 'lucide-react';
import { clipboardManager } from '../../services/clipboardManager';
import { ClipboardItem, ThemeConfig } from '../../types/keyboard';

interface ClipboardDrawerProps {
  theme: ThemeConfig;
  onPasteText: (text: string) => void;
  onClose: () => void;
}

export const ClipboardDrawer: React.FC<ClipboardDrawerProps> = ({
  theme,
  onPasteText,
  onClose
}) => {
  const [clips, setClips] = useState<ClipboardItem[]>([]);

  const refreshClips = () => {
    setClips(clipboardManager.getItems());
  };

  useEffect(() => {
    refreshClips();
  }, []);

  const handleTogglePin = (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    clipboardManager.togglePin(id);
    refreshClips();
  };

  const handleDelete = (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    clipboardManager.deleteClip(id);
    refreshClips();
  };

  const handleClearUnpinned = () => {
    clipboardManager.clearUnpinned();
    refreshClips();
  };

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
        className="flex items-center justify-between px-3 py-2 border-b"
        style={{ borderColor: theme.keyBorder, backgroundColor: theme.smartbarBg }}
      >
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={onClose}
            className="p-1 rounded-lg hover:opacity-80 active:scale-95 transition-all cursor-pointer"
            style={{ color: theme.accent }}
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <span className="font-semibold text-sm">FlorisBoard Clipboard</span>
        </div>

        <button
          type="button"
          onClick={handleClearUnpinned}
          className="text-xs px-2.5 py-1 rounded-md hover:opacity-80 transition-opacity cursor-pointer flex items-center gap-1"
          style={{
            backgroundColor: `${theme.bgKeySpecial}`,
            color: theme.textSecondary
          }}
          title="Clear all unpinned clips"
        >
          <Trash2 className="w-3.5 h-3.5" />
          <span>Clear unpinned</span>
        </button>
      </div>

      {/* Clips List */}
      <div className="flex-1 overflow-y-auto p-2 space-y-2 no-scrollbar">
        {clips.length === 0 ? (
          <div className="h-full flex flex-col items-center justify-center text-center opacity-60 py-8">
            <Copy className="w-8 h-8 mb-2 opacity-40" />
            <p className="text-xs">Clipboard is empty</p>
            <p className="text-[11px] opacity-70 mt-1">Copied text will automatically appear here</p>
          </div>
        ) : (
          clips.map((clip) => (
            <div
              key={clip.id}
              onClick={() => onPasteText(clip.text)}
              className="group p-2.5 rounded-xl border flex items-start justify-between gap-2 hover:opacity-95 active:scale-[0.99] transition-all cursor-pointer"
              style={{
                backgroundColor: theme.bgKey,
                borderColor: clip.pinned ? theme.accent : theme.keyBorder
              }}
            >
              <div className="flex-1 min-w-0">
                <p className="text-xs sm:text-sm line-clamp-2 break-all select-text font-sans">
                  {clip.text}
                </p>
                <span className="text-[10px] opacity-50 block mt-1 font-mono">
                  {new Date(clip.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                  {clip.pinned && ' • Pinned'}
                </span>
              </div>

              <div className="flex items-center gap-1 shrink-0">
                <button
                  type="button"
                  onClick={(e) => handleTogglePin(clip.id, e)}
                  className="p-1.5 rounded-lg hover:opacity-80 active:scale-90 transition-all cursor-pointer"
                  style={{
                    color: clip.pinned ? theme.accent : theme.textSecondary,
                    backgroundColor: clip.pinned ? `${theme.accent}20` : 'transparent'
                  }}
                  title={clip.pinned ? "Unpin item" : "Pin item to keep forever"}
                >
                  <Pin className="w-3.5 h-3.5" />
                </button>
                <button
                  type="button"
                  onClick={(e) => handleDelete(clip.id, e)}
                  className="p-1.5 rounded-lg hover:text-rose-400 active:scale-90 transition-all cursor-pointer"
                  style={{ color: theme.textSecondary }}
                  title="Delete clip"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};
