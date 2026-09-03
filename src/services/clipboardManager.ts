import { ClipboardItem } from '../types/keyboard';

class FlorisClipboardManager {
  private items: ClipboardItem[] = [];
  private static STORAGE_KEY = 'floris_clipboard_history';
  private maxItems = 25;

  constructor() {
    this.load();
    if (this.items.length === 0) {
      // Seed with some handy preset clips
      this.addClip('FlorisBoard x HeliBoard - Private, offline, customizable.', true);
      this.addClip('https://github.com/florisboard/florisboard', false);
      this.addClip('https://github.com/heliborg/heliboard', false);
    }
  }

  private load() {
    try {
      const data = localStorage.getItem(FlorisClipboardManager.STORAGE_KEY);
      if (data) {
        this.items = JSON.parse(data);
      }
    } catch {
      this.items = [];
    }
  }

  private save() {
    try {
      localStorage.setItem(FlorisClipboardManager.STORAGE_KEY, JSON.stringify(this.items));
    } catch {
      // ignore quota
    }
  }

  public getItems(): ClipboardItem[] {
    return [...this.items].sort((a, b) => {
      if (a.pinned !== b.pinned) return a.pinned ? -1 : 1;
      return b.timestamp - a.timestamp;
    });
  }

  public getRecentClips(): ClipboardItem[] {
    return this.getItems();
  }

  public addClip(text: string, pinned = false): ClipboardItem {
    const clean = text.trim();
    if (!clean) return this.items[0];

    // Check if already exists
    const existingIdx = this.items.findIndex(i => i.text === clean);
    if (existingIdx >= 0) {
      const existing = this.items[existingIdx];
      existing.timestamp = Date.now();
      if (pinned) existing.pinned = true;
      this.save();
      return existing;
    }

    const newItem: ClipboardItem = {
      id: 'clip_' + Date.now() + '_' + Math.random().toString(36).substring(2, 6),
      text: clean,
      timestamp: Date.now(),
      pinned
    };

    this.items.unshift(newItem);

    // Enforce max unpinned items
    if (this.items.length > this.maxItems) {
      let unpinnedIdx = -1;
      for (let i = this.items.length - 1; i >= 0; i--) {
        if (!this.items[i].pinned) {
          unpinnedIdx = i;
          break;
        }
      }
      if (unpinnedIdx >= 0) {
        this.items.splice(unpinnedIdx, 1);
      }
    }

    this.save();
    return newItem;
  }

  public togglePin(id: string) {
    const item = this.items.find(i => i.id === id);
    if (item) {
      item.pinned = !item.pinned;
      this.save();
    }
  }

  public deleteClip(id: string) {
    this.items = this.items.filter(i => i.id !== id);
    this.save();
  }

  public clearUnpinned() {
    this.items = this.items.filter(i => i.pinned);
    this.save();
  }
}

export const clipboardManager = new FlorisClipboardManager();
