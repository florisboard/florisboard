import React, { useState, useEffect } from 'react';
import {
  X,
  Plus,
  Trash2,
  Download,
  Upload,
  BookOpen,
  Zap,
  ShieldAlert,
  Search,
  Check
} from 'lucide-react';
import { dictionaryEngine } from '../../services/dictionaryEngine';
import { UserDictionaryEntry, ThemeConfig } from '../../types/keyboard';

interface DictionaryEditorModalProps {
  theme: ThemeConfig;
  isOpen: boolean;
  onClose: () => void;
  onDictionaryUpdated: () => void;
}

export const DictionaryEditorModal: React.FC<DictionaryEditorModalProps> = ({
  theme,
  isOpen,
  onClose,
  onDictionaryUpdated
}) => {
  const [activeTab, setActiveTab] = useState<'words' | 'shortcuts' | 'blocked' | 'backup'>('words');
  const [entries, setEntries] = useState<UserDictionaryEntry[]>([]);
  const [shortcuts, setShortcuts] = useState<{ shortcut: string; expansion: string }[]>([]);
  const [blocked, setBlocked] = useState<string[]>([]);
  const [searchQuery, setSearchQuery] = useState('');

  // Form inputs
  const [newWord, setNewWord] = useState('');
  const [newWordShortcut, setNewWordShortcut] = useState('');
  const [newShortcutCode, setNewShortcutCode] = useState('');
  const [newShortcutText, setNewShortcutText] = useState('');
  const [newBlockedWord, setNewBlockedWord] = useState('');

  const [importJson, setImportJson] = useState('');
  const [statusMessage, setStatusMessage] = useState('');

  const refreshData = () => {
    setEntries(dictionaryEngine.getUserDictionaryEntries());
    setShortcuts(dictionaryEngine.getShortcutsList());
    setBlocked(dictionaryEngine.getBlockedWordsList());
    onDictionaryUpdated();
  };

  useEffect(() => {
    if (isOpen) {
      refreshData();
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleAddWord = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newWord.trim()) return;
    dictionaryEngine.addUserWord(newWord.trim(), newWordShortcut.trim() || undefined);
    setNewWord('');
    setNewWordShortcut('');
    refreshData();
    setStatusMessage('Word saved to private dictionary');
    setTimeout(() => setStatusMessage(''), 3000);
  };

  const handleRemoveWord = (word: string) => {
    dictionaryEngine.removeUserWord(word);
    refreshData();
  };

  const handleAddShortcut = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newShortcutCode.trim() || !newShortcutText.trim()) return;
    dictionaryEngine.addUserWord(newShortcutText.trim(), newShortcutCode.trim());
    setNewShortcutCode('');
    setNewShortcutText('');
    refreshData();
    setStatusMessage('Shortcut created');
    setTimeout(() => setStatusMessage(''), 3000);
  };

  const handleToggleBlock = (word: string) => {
    dictionaryEngine.toggleBlockWord(word);
    refreshData();
  };

  const handleAddBlockedWord = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newBlockedWord.trim()) return;
    dictionaryEngine.toggleBlockWord(newBlockedWord.trim());
    setNewBlockedWord('');
    refreshData();
  };

  const handleExport = () => {
    const jsonStr = dictionaryEngine.exportUserDataJson();
    const blob = new Blob([jsonStr], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `heliboard-dictionary-backup-${Date.now()}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const handleImport = () => {
    if (!importJson.trim()) return;
    const success = dictionaryEngine.importUserDataJson(importJson);
    if (success) {
      refreshData();
      setStatusMessage('Dictionary restored successfully!');
      setImportJson('');
    } else {
      setStatusMessage('Failed to parse JSON. Please verify format.');
    }
    setTimeout(() => setStatusMessage(''), 4000);
  };

  const filteredEntries = entries.filter(e =>
    e.word.toLowerCase().includes(searchQuery.toLowerCase()) ||
    e.shortcut?.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 bg-black/60 backdrop-blur-xs animate-in fade-in duration-150">
      <div
        className="w-full max-w-2xl max-h-[90vh] rounded-2xl shadow-2xl flex flex-col border overflow-hidden"
        style={{
          backgroundColor: theme.bgMain,
          borderColor: theme.keyBorder,
          color: theme.textPrimary
        }}
      >
        {/* Modal Header */}
        <div
          className="flex items-center justify-between px-5 py-4 border-b shrink-0"
          style={{ borderColor: theme.keyBorder }}
        >
          <div className="flex items-center gap-2">
            <BookOpen className="w-5 h-5" style={{ color: theme.accent }} />
            <h2 className="text-lg font-bold">Personal Dictionary</h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 rounded-lg hover:opacity-80 transition-opacity cursor-pointer"
            style={{ color: theme.textSecondary }}
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tab Navigation */}
        <div
          className="flex border-b px-4 gap-2 shrink-0 overflow-x-auto no-scrollbar text-sm"
          style={{ borderColor: theme.keyBorder }}
        >
          <button
            type="button"
            onClick={() => setActiveTab('words')}
            className={`py-3 px-3 font-medium border-b-2 transition-all cursor-pointer flex items-center gap-1.5 ${
              activeTab === 'words' ? 'border-current font-bold' : 'border-transparent opacity-70'
            }`}
            style={{ color: activeTab === 'words' ? theme.accent : theme.textPrimary }}
          >
            <BookOpen className="w-4 h-4" />
            <span>Learned & Custom Words ({entries.length})</span>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('shortcuts')}
            className={`py-3 px-3 font-medium border-b-2 transition-all cursor-pointer flex items-center gap-1.5 ${
              activeTab === 'shortcuts' ? 'border-current font-bold' : 'border-transparent opacity-70'
            }`}
            style={{ color: activeTab === 'shortcuts' ? theme.accent : theme.textPrimary }}
          >
            <Zap className="w-4 h-4" />
            <span>Shortcuts ({shortcuts.length})</span>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('blocked')}
            className={`py-3 px-3 font-medium border-b-2 transition-all cursor-pointer flex items-center gap-1.5 ${
              activeTab === 'blocked' ? 'border-current font-bold' : 'border-transparent opacity-70'
            }`}
            style={{ color: activeTab === 'blocked' ? theme.accent : theme.textPrimary }}
          >
            <ShieldAlert className="w-4 h-4" />
            <span>Blocked Words ({blocked.length})</span>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('backup')}
            className={`py-3 px-3 font-medium border-b-2 transition-all cursor-pointer flex items-center gap-1.5 ${
              activeTab === 'backup' ? 'border-current font-bold' : 'border-transparent opacity-70'
            }`}
            style={{ color: activeTab === 'backup' ? theme.accent : theme.textPrimary }}
          >
            <Download className="w-4 h-4" />
            <span>Import / Export</span>
          </button>
        </div>

        {/* Feedback alert */}
        {statusMessage && (
          <div
            className="px-4 py-2 text-xs font-semibold flex items-center gap-1.5 border-b"
            style={{ backgroundColor: `${theme.accent}20`, color: theme.accent, borderColor: theme.accent }}
          >
            <Check className="w-4 h-4" />
            <span>{statusMessage}</span>
          </div>
        )}

        {/* Tab Content */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4">
          {/* TAB 1: Learned & Custom Words */}
          {activeTab === 'words' && (
            <div className="space-y-4">
              {/* Add Word Form */}
              <form
                onSubmit={handleAddWord}
                className="p-3.5 rounded-xl border flex flex-col sm:flex-row gap-2"
                style={{ backgroundColor: theme.bgKeyboard, borderColor: theme.keyBorder }}
              >
                <input
                  type="text"
                  placeholder="New word (e.g. FlorisBoard)"
                  value={newWord}
                  onChange={(e) => setNewWord(e.target.value)}
                  className="flex-1 px-3 py-2 rounded-lg text-sm border outline-none font-sans"
                  style={{
                    backgroundColor: theme.bgKey,
                    borderColor: theme.keyBorder,
                    color: theme.textPrimary
                  }}
                />
                <input
                  type="text"
                  placeholder="Optional shortcut (e.g. fb)"
                  value={newWordShortcut}
                  onChange={(e) => setNewWordShortcut(e.target.value)}
                  className="w-full sm:w-44 px-3 py-2 rounded-lg text-sm border outline-none font-sans"
                  style={{
                    backgroundColor: theme.bgKey,
                    borderColor: theme.keyBorder,
                    color: theme.textPrimary
                  }}
                />
                <button
                  type="submit"
                  className="px-4 py-2 rounded-lg font-semibold text-sm flex items-center justify-center gap-1.5 hover:opacity-90 active:scale-95 transition-all cursor-pointer"
                  style={{ backgroundColor: theme.accent, color: theme.accentText }}
                >
                  <Plus className="w-4 h-4" />
                  <span>Add Word</span>
                </button>
              </form>

              {/* Search Bar */}
              <div className="relative">
                <Search className="w-4 h-4 absolute left-3 top-3 opacity-50" />
                <input
                  type="text"
                  placeholder="Search learned & custom words..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full pl-9 pr-3 py-2 rounded-lg text-sm border outline-none"
                  style={{
                    backgroundColor: theme.bgKeyboard,
                    borderColor: theme.keyBorder,
                    color: theme.textPrimary
                  }}
                />
              </div>

              {/* Words List */}
              <div className="space-y-2 max-h-72 overflow-y-auto pr-1">
                {filteredEntries.length === 0 ? (
                  <div className="text-center py-8 opacity-60 text-xs">
                    No custom or learned words found. Type in the keyboard to have words automatically learned!
                  </div>
                ) : (
                  filteredEntries.map((entry) => (
                    <div
                      key={entry.id}
                      className="p-2.5 rounded-lg border flex items-center justify-between text-sm"
                      style={{ backgroundColor: theme.bgKeyboard, borderColor: theme.keyBorder }}
                    >
                      <div>
                        <span className="font-semibold text-base">{entry.word}</span>
                        {entry.shortcut && (
                          <span
                            className="ml-2 text-xs px-2 py-0.5 rounded font-mono"
                            style={{ backgroundColor: `${theme.accent}25`, color: theme.accent }}
                          >
                            Shortcut: {entry.shortcut}
                          </span>
                        )}
                        <div className="text-[11px] opacity-60 mt-0.5 font-mono">
                          Frequency: {entry.frequency} • {entry.isCustom ? 'Custom Word' : 'Auto-Learned Word'}
                        </div>
                      </div>

                      <button
                        type="button"
                        onClick={() => handleRemoveWord(entry.word)}
                        className="p-1.5 rounded hover:text-rose-400 active:scale-90 transition-all cursor-pointer"
                        style={{ color: theme.textSecondary }}
                        title="Delete word"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}

          {/* TAB 2: Shortcuts */}
          {activeTab === 'shortcuts' && (
            <div className="space-y-4">
              <form
                onSubmit={handleAddShortcut}
                className="p-3.5 rounded-xl border flex flex-col sm:flex-row gap-2"
                style={{ backgroundColor: theme.bgKeyboard, borderColor: theme.keyBorder }}
              >
                <input
                  type="text"
                  placeholder="Shortcut code (e.g. omw)"
                  value={newShortcutCode}
                  onChange={(e) => setNewShortcutCode(e.target.value)}
                  className="w-full sm:w-36 px-3 py-2 rounded-lg text-sm border outline-none font-mono"
                  style={{
                    backgroundColor: theme.bgKey,
                    borderColor: theme.keyBorder,
                    color: theme.textPrimary
                  }}
                />
                <input
                  type="text"
                  placeholder="Expansion text (e.g. On my way!)"
                  value={newShortcutText}
                  onChange={(e) => setNewShortcutText(e.target.value)}
                  className="flex-1 px-3 py-2 rounded-lg text-sm border outline-none"
                  style={{
                    backgroundColor: theme.bgKey,
                    borderColor: theme.keyBorder,
                    color: theme.textPrimary
                  }}
                />
                <button
                  type="submit"
                  className="px-4 py-2 rounded-lg font-semibold text-sm flex items-center justify-center gap-1.5 hover:opacity-90 active:scale-95 transition-all cursor-pointer"
                  style={{ backgroundColor: theme.accent, color: theme.accentText }}
                >
                  <Plus className="w-4 h-4" />
                  <span>Save</span>
                </button>
              </form>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 max-h-72 overflow-y-auto">
                {shortcuts.map((s, idx) => (
                  <div
                    key={idx}
                    className="p-2.5 rounded-lg border flex items-center justify-between"
                    style={{ backgroundColor: theme.bgKeyboard, borderColor: theme.keyBorder }}
                  >
                    <div>
                      <span className="font-mono text-xs font-bold px-1.5 py-0.5 rounded bg-black/20 mr-2">
                        {s.shortcut}
                      </span>
                      <span className="text-sm font-medium">{s.expansion}</span>
                    </div>
                    <button
                      type="button"
                      onClick={() => handleRemoveWord(s.expansion)}
                      className="p-1 hover:text-rose-400 cursor-pointer"
                      style={{ color: theme.textSecondary }}
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* TAB 3: Blocked Words */}
          {activeTab === 'blocked' && (
            <div className="space-y-4">
              <p className="text-xs opacity-75 leading-relaxed">
                Words on this list will never be learned into your dynamic dictionary, suggested in next-word completions, or auto-corrected.
              </p>

              <form
                onSubmit={handleAddBlockedWord}
                className="flex gap-2"
              >
                <input
                  type="text"
                  placeholder="Word to blacklist..."
                  value={newBlockedWord}
                  onChange={(e) => setNewBlockedWord(e.target.value)}
                  className="flex-1 px-3 py-2 rounded-lg text-sm border outline-none"
                  style={{
                    backgroundColor: theme.bgKeyboard,
                    borderColor: theme.keyBorder,
                    color: theme.textPrimary
                  }}
                />
                <button
                  type="submit"
                  className="px-4 py-2 rounded-lg font-semibold text-sm flex items-center justify-center gap-1.5 hover:opacity-90 active:scale-95 transition-all cursor-pointer"
                  style={{ backgroundColor: theme.accent, color: theme.accentText }}
                >
                  <Plus className="w-4 h-4" />
                  <span>Block Word</span>
                </button>
              </form>

              <div className="flex flex-wrap gap-2 max-h-64 overflow-y-auto">
                {blocked.map((w, idx) => (
                  <div
                    key={idx}
                    className="flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium border"
                    style={{
                      backgroundColor: theme.bgKeyboard,
                      borderColor: theme.keyBorder,
                      color: theme.textPrimary
                    }}
                  >
                    <span>{w}</span>
                    <button
                      type="button"
                      onClick={() => handleToggleBlock(w)}
                      className="hover:text-rose-400 cursor-pointer ml-1"
                    >
                      <X className="w-3 h-3" />
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* TAB 4: Import / Export */}
          {activeTab === 'backup' && (
            <div className="space-y-4">
              <div
                className="p-4 rounded-xl border flex flex-col gap-3"
                style={{ backgroundColor: theme.bgKeyboard, borderColor: theme.keyBorder }}
              >
                <h4 className="font-semibold text-sm flex items-center gap-2">
                  <Download className="w-4 h-4" style={{ color: theme.accent }} />
                  <span>Export Dictionary & Preferences</span>
                </h4>
                <p className="text-xs opacity-75">
                  Download all learned vocabulary, n-grams, custom shortcuts, and blocked words into a standalone, offline JSON backup file.
                </p>
                <div>
                  <button
                    type="button"
                    onClick={handleExport}
                    className="px-4 py-2 rounded-lg text-xs font-semibold flex items-center gap-1.5 hover:opacity-90 transition-opacity cursor-pointer"
                    style={{ backgroundColor: theme.accent, color: theme.accentText }}
                  >
                    <Download className="w-4 h-4" />
                    <span>Download Backup (.json)</span>
                  </button>
                </div>
              </div>

              <div
                className="p-4 rounded-xl border flex flex-col gap-3"
                style={{ backgroundColor: theme.bgKeyboard, borderColor: theme.keyBorder }}
              >
                <h4 className="font-semibold text-sm flex items-center gap-2">
                  <Upload className="w-4 h-4" style={{ color: theme.accent }} />
                  <span>Import / Restore Dictionary JSON</span>
                </h4>
                <textarea
                  rows={4}
                  placeholder="Paste JSON dictionary backup here..."
                  value={importJson}
                  onChange={(e) => setImportJson(e.target.value)}
                  className="w-full p-2.5 rounded-lg text-xs font-mono border outline-none"
                  style={{
                    backgroundColor: theme.bgKey,
                    borderColor: theme.keyBorder,
                    color: theme.textPrimary
                  }}
                />
                <div>
                  <button
                    type="button"
                    onClick={handleImport}
                    className="px-4 py-2 rounded-lg text-xs font-semibold flex items-center gap-1.5 hover:opacity-90 transition-opacity cursor-pointer"
                    style={{ backgroundColor: theme.bgKeySpecial, color: theme.textPrimary }}
                  >
                    <Upload className="w-4 h-4" />
                    <span>Restore from JSON</span>
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
