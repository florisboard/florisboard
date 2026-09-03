import React, { useState } from 'react';
import { ArrowLeft, Delete, Smile, Sparkles, MessageCircle } from 'lucide-react';
import { ThemeConfig } from '../../types/keyboard';

interface EmojiPickerProps {
  theme: ThemeConfig;
  onSelectEmoji: (emoji: string) => void;
  onBackspace: () => void;
  onClose: () => void;
}

const EMOJI_CATEGORIES = [
  {
    name: 'Smileys',
    emojis: ['😀', '😃', '😄', '😁', '😆', '😅', '😂', '🤣', '😊', '😇', '🙂', '🙃', '😉', '😌', '😍', '🥰', '😘', '😗', '😙', '😚', '😋', '😛', '😜', '🤪', '😝', '🤑', '🤗', '🤭', '🤫', '🤔', '🤐', '🤨', '😐', '😑', '😶', '😏', '😒', '🙄', '😬', '🤥', '😌', '😔', '😪', '🤤', '😴', '😷', '🤒', '🤕', '🤢', '🤮', '🤧', '🥵', '🥶', '🥴', '😵', '🤯', '🤠', '🥳', '😎', '🤓', '🧐']
  },
  {
    name: 'Gestures',
    emojis: ['👍', '👎', '👌', '✌️', '🤞', '🤟', '🤘', '🤙', '👈', '👉', '👆', '🖕', '👇', '☝️', '✋', '🤚', '🖐️', '🖖', '👋', '🤙', '💪', '🙏', '👏', '🤝', '🙌', '👐', '🤲', '✍️', '💅', '🤳']
  },
  {
    name: 'Hearts & Fire',
    emojis: ['❤️', '🧡', '💛', '💚', '💙', '💜', '🖤', '🤍', '🤎', '💔', '❣️', '💕', '💞', '💓', '💗', '💖', '💘', '💝', '💟', '🔥', '✨', '⚡', '💥', '⭐', '🌟', '💫', '🎉', '🎊']
  }
];

const KAOMOJI_CATEGORIES = [
  {
    name: 'Happy & Love',
    items: [
      '(◕‿◕)', '(｡♥‿♥｡)', '(✿◠‿◠)', '(ᵔᴥᵔ)', '(*^▽^*)', '(ﾉ◕ヮ◕)ﾉ*:･ﾟ✧',
      '(づ｡◕‿‿◕｡)づ', '(♥ω♥*)', '(｡・//ε//・｡)', '(◕ᴗ◕✿)', '(^_-)-☆'
    ]
  },
  {
    name: 'Action & Shrug',
    items: [
      '¯\\_(ツ)_/¯', '(╯°□°)╯︵ ┻━┻', '┬─┬ノ( º _ ºノ)', '(ง\'̀-\'́)ง',
      'ᕦ(ò_óˇ)ᕤ', '(☞ﾟヮﾟ)☞', '☜(ﾟヮﾟ☜)', '(ノಠ益ಠ)ノ彡┻━┻', '(•_•) ( •_•)>⌐■-■ (⌐■_■)'
    ]
  },
  {
    name: 'Sad & Confused',
    items: [
      '(ಥ﹏ಥ)', '(╯︵╰,)', '(╥﹏╥)', '(´；ω；`)', '(•ิ_•ิ)?', '(ಠ_ಠ)',
      'ಠ_ಠ', '(⊙_☉)', '⊙﹏⊙', '(つд⊂)', '(-_-;)'
    ]
  }
];

const EMOTICONS = [
  ':-)', ':-(', ';-)', ':-D', ':-P', ':-O', ':-*', '<3', '</3', ':-/', ':-|', ':-X',
  ':-B', 'O:-)', ']:->', ':-S', '8-)', ':-$'
];

export const EmojiPicker: React.FC<EmojiPickerProps> = ({
  theme,
  onSelectEmoji,
  onBackspace,
  onClose
}) => {
  const [activeTab, setActiveTab] = useState<'emoji' | 'kaomoji' | 'emoticon'>('emoji');

  return (
    <div
      className="w-full h-64 flex flex-col select-none transition-colors border-t"
      style={{
        backgroundColor: theme.bgKeyboard,
        borderColor: theme.keyBorder,
        color: theme.textPrimary
      }}
    >
      {/* Top Header Tabs */}
      <div
        className="flex items-center justify-between px-2 py-1.5 border-b"
        style={{ borderColor: theme.keyBorder, backgroundColor: theme.smartbarBg }}
      >
        <button
          type="button"
          onClick={onClose}
          className="p-1.5 rounded-lg hover:opacity-80 active:scale-95 transition-all cursor-pointer"
          style={{ color: theme.accent }}
        >
          <ArrowLeft className="w-5 h-5" />
        </button>

        <div className="flex items-center gap-1">
          <button
            type="button"
            onClick={() => setActiveTab('emoji')}
            className={`px-3 py-1 rounded-full text-xs font-medium transition-all cursor-pointer ${
              activeTab === 'emoji' ? 'font-semibold' : 'opacity-70'
            }`}
            style={{
              backgroundColor: activeTab === 'emoji' ? theme.accent : 'transparent',
              color: activeTab === 'emoji' ? theme.accentText : theme.textPrimary
            }}
          >
            Emoji
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('kaomoji')}
            className={`px-3 py-1 rounded-full text-xs font-medium transition-all cursor-pointer ${
              activeTab === 'kaomoji' ? 'font-semibold' : 'opacity-70'
            }`}
            style={{
              backgroundColor: activeTab === 'kaomoji' ? theme.accent : 'transparent',
              color: activeTab === 'kaomoji' ? theme.accentText : theme.textPrimary
            }}
          >
            Kaomoji
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('emoticon')}
            className={`px-3 py-1 rounded-full text-xs font-medium transition-all cursor-pointer ${
              activeTab === 'emoticon' ? 'font-semibold' : 'opacity-70'
            }`}
            style={{
              backgroundColor: activeTab === 'emoticon' ? theme.accent : 'transparent',
              color: activeTab === 'emoticon' ? theme.accentText : theme.textPrimary
            }}
          >
            :- )
          </button>
        </div>

        <button
          type="button"
          onClick={onBackspace}
          className="p-1.5 rounded-lg hover:opacity-80 active:scale-95 transition-all cursor-pointer"
          style={{ color: theme.textSecondary }}
        >
          <Delete className="w-5 h-5" />
        </button>
      </div>

      {/* Main List Area */}
      <div className="flex-1 overflow-y-auto p-2 no-scrollbar">
        {activeTab === 'emoji' && (
          <div className="space-y-3">
            {EMOJI_CATEGORIES.map((cat) => (
              <div key={cat.name}>
                <div
                  className="text-[11px] font-semibold uppercase tracking-wider mb-1 px-1 opacity-60"
                  style={{ color: theme.textSecondary }}
                >
                  {cat.name}
                </div>
                <div className="grid grid-cols-8 sm:grid-cols-10 gap-1">
                  {cat.emojis.map((emoji, idx) => (
                    <button
                      key={idx}
                      type="button"
                      onClick={() => onSelectEmoji(emoji)}
                      className="h-10 rounded-lg flex items-center justify-center text-2xl hover:scale-110 active:scale-90 transition-transform cursor-pointer"
                      style={{ backgroundColor: `${theme.bgKey}40` }}
                    >
                      {emoji}
                    </button>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}

        {activeTab === 'kaomoji' && (
          <div className="space-y-3">
            {KAOMOJI_CATEGORIES.map((cat) => (
              <div key={cat.name}>
                <div
                  className="text-[11px] font-semibold uppercase tracking-wider mb-1 px-1 opacity-60"
                  style={{ color: theme.textSecondary }}
                >
                  {cat.name}
                </div>
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-1.5">
                  {cat.items.map((kaomoji, idx) => (
                    <button
                      key={idx}
                      type="button"
                      onClick={() => onSelectEmoji(kaomoji)}
                      className="h-9 px-2 rounded-lg flex items-center justify-center text-xs font-mono truncate hover:opacity-90 active:scale-95 transition-all cursor-pointer border"
                      style={{
                        backgroundColor: theme.bgKey,
                        borderColor: theme.keyBorder,
                        color: theme.textPrimary
                      }}
                    >
                      {kaomoji}
                    </button>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}

        {activeTab === 'emoticon' && (
          <div className="grid grid-cols-3 sm:grid-cols-4 gap-2 pt-2">
            {EMOTICONS.map((emo, idx) => (
              <button
                key={idx}
                type="button"
                onClick={() => onSelectEmoji(emo)}
                className="h-10 px-2 rounded-lg flex items-center justify-center text-sm font-mono font-medium hover:opacity-90 active:scale-95 transition-all cursor-pointer border"
                style={{
                  backgroundColor: theme.bgKey,
                  borderColor: theme.keyBorder,
                  color: theme.textPrimary
                }}
              >
                {emo}
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
