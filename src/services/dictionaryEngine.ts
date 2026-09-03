import {
  BASE_VOCABULARY,
  BASE_BIGRAMS,
  DEFAULT_SHORTCUTS,
  OFFENSIVE_WORDS,
  KEY_COORDINATES
} from '../data/dictionaryData';
import { UserDictionaryEntry, WordSuggestion, HeliBoardTypingSettings } from '../types/keyboard';

class TrieNode {
  children: Map<string, TrieNode> = new Map();
  isEndOfWord: boolean = false;
  frequency: number = 0;
  word: string = '';
}

export class HeliBoardDictionaryEngine {
  private root: TrieNode = new TrieNode();
  private userDictionary: Map<string, UserDictionaryEntry> = new Map();
  private userBigrams: Map<string, Map<string, number>> = new Map();
  private shortcuts: Map<string, string> = new Map();
  private blockedWords: Set<string> = new Set(OFFENSIVE_WORDS);
  private isIncognito: boolean = false;

  private static STORAGE_USER_DICT_KEY = 'heliboard_user_dictionary';
  private static STORAGE_USER_BIGRAMS_KEY = 'heliboard_user_bigrams';
  private static STORAGE_SHORTCUTS_KEY = 'heliboard_custom_shortcuts';
  private static STORAGE_BLOCKED_KEY = 'heliboard_blocked_words';

  constructor() {
    this.initBaseDictionary();
    this.loadPersistedUserData();
  }

  private initBaseDictionary() {
    for (const [word, freq] of BASE_VOCABULARY) {
      this.insertTrie(word.toLowerCase(), freq);
    }
    for (const [shortcut, expansion] of Object.entries(DEFAULT_SHORTCUTS)) {
      this.shortcuts.set(shortcut.toLowerCase(), expansion);
    }
  }

  private insertTrie(word: string, frequency: number) {
    let current = this.root;
    for (const char of word) {
      if (!current.children.has(char)) {
        current.children.set(char, new TrieNode());
      }
      current = current.children.get(char)!;
    }
    current.isEndOfWord = true;
    current.frequency = Math.max(current.frequency, frequency);
    current.word = word;
  }

  private rebuildTrie() {
    this.root = new TrieNode();
    for (const [word, frequency] of BASE_VOCABULARY) {
      this.insertTrie(word.toLowerCase(), frequency);
    }
    for (const entry of this.userDictionary.values()) {
      this.insertTrie(entry.word.toLowerCase(), entry.frequency);
    }
  }

  public setIncognito(enabled: boolean) {
    this.isIncognito = enabled;
  }

  public getIsIncognito(): boolean {
    return this.isIncognito;
  }

  // Persisted offline data handling
  private loadPersistedUserData() {
    try {
      const storedDict = localStorage.getItem(HeliBoardDictionaryEngine.STORAGE_USER_DICT_KEY);
      if (storedDict) {
        const parsed: UserDictionaryEntry[] = JSON.parse(storedDict);
        for (const entry of parsed) {
          this.userDictionary.set(entry.word.toLowerCase(), entry);
          this.insertTrie(entry.word.toLowerCase(), entry.frequency);
          if (entry.shortcut) {
            this.shortcuts.set(entry.shortcut.toLowerCase(), entry.word);
          }
        }
      }

      const storedShortcuts = localStorage.getItem(HeliBoardDictionaryEngine.STORAGE_SHORTCUTS_KEY);
      if (storedShortcuts) {
        const parsed: Record<string, string> = JSON.parse(storedShortcuts);
        for (const [k, v] of Object.entries(parsed)) {
          this.shortcuts.set(k.toLowerCase(), v);
        }
      }

      const storedBlocked = localStorage.getItem(HeliBoardDictionaryEngine.STORAGE_BLOCKED_KEY);
      if (storedBlocked) {
        const parsed: string[] = JSON.parse(storedBlocked);
        for (const w of parsed) {
          this.blockedWords.add(w.toLowerCase());
        }
      }

      const storedBigrams = localStorage.getItem(HeliBoardDictionaryEngine.STORAGE_USER_BIGRAMS_KEY);
      if (storedBigrams) {
        const parsed: Record<string, Record<string, number>> = JSON.parse(storedBigrams);
        for (const [prev, nextMap] of Object.entries(parsed)) {
          const map = new Map<string, number>();
          for (const [next, count] of Object.entries(nextMap)) {
            map.set(next, count);
          }
          this.userBigrams.set(prev, map);
        }
      }
    } catch {
      // Local storage might be restricted or empty
    }
  }

  private saveUserDictionary() {
    if (this.isIncognito) return;
    try {
      const entries = Array.from(this.userDictionary.values());
      localStorage.setItem(HeliBoardDictionaryEngine.STORAGE_USER_DICT_KEY, JSON.stringify(entries));
    } catch {
      // Storage quota or error
    }
  }

  private saveUserBigrams() {
    if (this.isIncognito) return;
    try {
      const obj: Record<string, Record<string, number>> = {};
      for (const [prev, map] of this.userBigrams.entries()) {
        obj[prev] = Object.fromEntries(map.entries());
      }
      localStorage.setItem(HeliBoardDictionaryEngine.STORAGE_USER_BIGRAMS_KEY, JSON.stringify(obj));
    } catch {
      // Storage quota or error
    }
  }

  // Dynamic Word Learning (HeliBoard feature)
  public learnWord(word: string, prevWord?: string, settings?: HeliBoardTypingSettings) {
    if (this.isIncognito) return;
    if (settings && !settings.learnUserWords) return;

    const cleanWord = word.trim().toLowerCase();
    if (!cleanWord || cleanWord.length < (settings?.minWordLengthForLearning || 2)) return;
    if (this.blockedWords.has(cleanWord)) return;
    if (/\d/.test(cleanWord)) return; // Don't learn numbers

    // Update user vocabulary
    const existing = this.userDictionary.get(cleanWord);
    if (existing) {
      existing.frequency = Math.min(255, existing.frequency + 10);
      this.insertTrie(cleanWord, existing.frequency);
    } else {
      const entry: UserDictionaryEntry = {
        id: 'usr_' + Date.now() + '_' + Math.random().toString(36).substring(2, 7),
        word: cleanWord,
        frequency: 180, // High starting frequency for newly learned user words
        locale: 'en-US',
        isCustom: false,
        dateAdded: Date.now()
      };
      this.userDictionary.set(cleanWord, entry);
      this.insertTrie(cleanWord, entry.frequency);
    }
    this.saveUserDictionary();

    // Update bigram transition (HeliBoard next-word learning)
    if (prevWord) {
      const cleanPrev = prevWord.trim().toLowerCase();
      if (cleanPrev && !this.blockedWords.has(cleanPrev)) {
        let followers = this.userBigrams.get(cleanPrev);
        if (!followers) {
          followers = new Map<string, number>();
          this.userBigrams.set(cleanPrev, followers);
        }
        const currCount = followers.get(cleanWord) || 0;
        followers.set(cleanWord, currCount + 1);
        this.saveUserBigrams();
      }
    }
  }

  // Spatial Keyboard Distance calculation
  private getSpatialDistance(c1: string, c2: string): number {
    if (c1 === c2) return 0;
    const p1 = KEY_COORDINATES[c1];
    const p2 = KEY_COORDINATES[c2];
    if (!p1 || !p2) return 2.5; // Unknown keys penalty
    const dx = p1.x - p2.x;
    const dy = p1.y - p2.y;
    return Math.sqrt(dx * dx + dy * dy);
  }

  // Calculate fuzzy match score between input and dictionary candidate
  private calculateTypoScore(input: string, candidate: string): number {
    const m = input.length;
    const n = candidate.length;
    if (Math.abs(m - n) > 2) return 999; // Length mismatch too large

    let cost = 0;
    const minLen = Math.min(m, n);

    // Initial prefix weight
    if (input[0] !== candidate[0]) {
      cost += this.getSpatialDistance(input[0], candidate[0]) * 1.8;
    }

    for (let i = 0; i < minLen; i++) {
      const chIn = input[i];
      const chCand = candidate[i];
      if (chIn !== chCand) {
        // Spatial distance penalty
        cost += this.getSpatialDistance(chIn, chCand);
      }
    }

    // Length difference penalty
    cost += Math.abs(m - n) * 1.5;

    // Transposition check (e.g. "teh" -> "the")
    for (let i = 0; i < minLen - 1; i++) {
      if (input[i] === candidate[i + 1] && input[i + 1] === candidate[i]) {
        cost -= 0.8; // Reward transposition
      }
    }

    return cost;
  }

  // HeliBoard Prediction Engine
  public getSuggestions(
    currentWord: string,
    precedingText: string,
    settings: HeliBoardTypingSettings
  ): WordSuggestion[] {
    if (!settings.wordSuggestions) return [];

    const suggestions: WordSuggestion[] = [];
    const lowerCurrent = currentWord.trim().toLowerCase();
    const seenWords = new Set<string>();

    // 1. Shortcut Expansion check (HeliBoard feature)
    if (settings.expandShortcuts && lowerCurrent && this.shortcuts.has(lowerCurrent)) {
      const expansion = this.shortcuts.get(lowerCurrent)!;
      suggestions.push({
        word: expansion,
        isAutoCorrect: true,
        isLearned: true,
        isExactMatch: false,
        frequency: 255,
        confidence: 1.0
      });
      seenWords.add(expansion.toLowerCase());
    }

    // 2. Next-word prediction if currentWord is empty
    if (!lowerCurrent) {
      if (settings.nextWordSuggestions && precedingText) {
        const words = precedingText.trim().split(/\s+/);
        if (words.length > 0) {
          const lastWord = words[words.length - 1].toLowerCase().replace(/[^a-z']/g, '');
          const prevTwo = words.length >= 2
            ? `${words[words.length - 2].toLowerCase().replace(/[^a-z']/g, '')} ${lastWord}`
            : '';

          // Check user learned bigrams first
          const userFollowers = this.userBigrams.get(lastWord);
          if (userFollowers) {
            const sortedUser = Array.from(userFollowers.entries()).sort((a, b) => b[1] - a[1]);
            for (const [w] of sortedUser) {
              if (!seenWords.has(w) && (!settings.blockOffensiveWords || !this.blockedWords.has(w))) {
                suggestions.push({
                  word: w,
                  isAutoCorrect: false,
                  isLearned: true,
                  isExactMatch: false,
                  frequency: 240,
                  confidence: 0.95
                });
                seenWords.add(w);
                if (suggestions.length >= settings.suggestionCount) break;
              }
            }
          }

          // Check base bigrams
          const trigramMatches = prevTwo ? BASE_BIGRAMS[prevTwo] : null;
          if (trigramMatches) {
            for (const [w, freq] of trigramMatches) {
              if (!seenWords.has(w) && (!settings.blockOffensiveWords || !this.blockedWords.has(w))) {
                suggestions.push({
                  word: w,
                  isAutoCorrect: false,
                  isLearned: false,
                  isExactMatch: false,
                  frequency: freq,
                  confidence: 0.9
                });
                seenWords.add(w);
                if (suggestions.length >= settings.suggestionCount) break;
              }
            }
          }

          const bigramMatches = BASE_BIGRAMS[lastWord];
          if (bigramMatches && suggestions.length < settings.suggestionCount) {
            for (const [w, freq] of bigramMatches) {
              if (!seenWords.has(w) && (!settings.blockOffensiveWords || !this.blockedWords.has(w))) {
                suggestions.push({
                  word: w,
                  isAutoCorrect: false,
                  isLearned: false,
                  isExactMatch: false,
                  frequency: freq,
                  confidence: 0.85
                });
                seenWords.add(w);
                if (suggestions.length >= settings.suggestionCount) break;
              }
            }
          }
        }
      }

      // Keep the prediction strip empty when next-word prediction is disabled.
      if (suggestions.length === 0 && settings.nextWordSuggestions) {
        const starters = ['I', 'The', 'How', 'What', 'Can', 'Thanks', 'Let', 'Please'];
        for (const s of starters) {
          suggestions.push({
            word: s,
            isAutoCorrect: false,
            isLearned: false,
            isExactMatch: false,
            frequency: 200,
            confidence: 0.7
          });
          if (suggestions.length >= settings.suggestionCount) break;
        }
      }

      return suggestions.slice(0, settings.suggestionCount);
    }

    // 3. Exact match check
    const isUserLearned = this.userDictionary.has(lowerCurrent);

    // 4. Prefix search in Trie
    const prefixMatches: { word: string; frequency: number }[] = [];
    let currNode = this.root;
    let foundPrefix = true;
    for (const ch of lowerCurrent) {
      if (!currNode.children.has(ch)) {
        foundPrefix = false;
        break;
      }
      currNode = currNode.children.get(ch)!;
    }

    if (foundPrefix) {
      this.collectTrieWords(currNode, prefixMatches, 30);
    }

    // Sort prefix matches by frequency
    prefixMatches.sort((a, b) => b.frequency - a.frequency);

    // 5. Typo correction / Fuzzy search using Spatial Keyboard Proximity
    const fuzzyCandidates: { word: string; score: number; frequency: number }[] = [];
    if (settings.autoCorrection !== 'off') {
      const searchPool = Array.from(this.userDictionary.keys()).concat(
        BASE_VOCABULARY.map(([w]) => w)
      );

      for (const cand of searchPool) {
        if (cand === lowerCurrent) continue;
        const dist = this.calculateTypoScore(lowerCurrent, cand);
        const threshold = settings.autoCorrection === 'very_aggressive' ? 3.8
          : settings.autoCorrection === 'aggressive' ? 3.0
          : 2.2;

        if (dist <= threshold) {
          const userEnt = this.userDictionary.get(cand);
          const freq = userEnt ? userEnt.frequency + 30 : 150;
          fuzzyCandidates.push({ word: cand, score: dist, frequency: freq });
        }
      }

      fuzzyCandidates.sort((a, b) => {
        // Combined score: lower distance + higher frequency
        const scoreA = a.score * 50 - a.frequency;
        const scoreB = b.score * 50 - b.frequency;
        return scoreA - scoreB;
      });
    }

    // 6. Assemble suggestions
    // Best candidate logic:
    // If exact prefix matches and starts with word, check if there's a strong fuzzy or prefix
    let bestWord = lowerCurrent;
    let isAutoCorrect = false;

    if (fuzzyCandidates.length > 0 && settings.autoCorrection !== 'off') {
      const topFuzzy = fuzzyCandidates[0];
      // Only autocorrect if user made a likely mistake (not in dictionary) or exact distance is very small
      const exactInDict = prefixMatches.some(m => m.word === lowerCurrent);
      if (!exactInDict && topFuzzy.score < 2.0) {
        bestWord = topFuzzy.word;
        isAutoCorrect = true;
      } else if (prefixMatches.length > 0) {
        bestWord = prefixMatches[0].word;
      }
    } else if (prefixMatches.length > 0) {
      bestWord = prefixMatches[0].word;
    }

    // Preserve user casing if capitalized
    const isFirstUpper = currentWord[0] === currentWord[0].toUpperCase() && currentWord[0] !== currentWord[0].toLowerCase();
    const formatCase = (w: string) => isFirstUpper ? w.charAt(0).toUpperCase() + w.slice(1) : w;

    // Center / Top Candidate
    if (bestWord) {
      const formatted = formatCase(bestWord);
      suggestions.push({
        word: formatted,
        isAutoCorrect: isAutoCorrect,
        isLearned: this.userDictionary.has(bestWord.toLowerCase()),
        isExactMatch: bestWord.toLowerCase() === lowerCurrent,
        frequency: 250,
        confidence: isAutoCorrect ? 0.92 : 0.88
      });
      seenWords.add(formatted.toLowerCase());
    }

    // User raw input as an option (left or right) so user can always reject autocorrect
    if (!seenWords.has(lowerCurrent)) {
      suggestions.push({
        word: currentWord,
        isAutoCorrect: false,
        isLearned: isUserLearned,
        isExactMatch: true,
        frequency: 200,
        confidence: 0.8
      });
      seenWords.add(lowerCurrent);
    }

    // Add prefix completions
    for (const match of prefixMatches) {
      const formatted = formatCase(match.word);
      if (!seenWords.has(formatted.toLowerCase()) && (!settings.blockOffensiveWords || !this.blockedWords.has(match.word))) {
        suggestions.push({
          word: formatted,
          isAutoCorrect: false,
          isLearned: this.userDictionary.has(match.word),
          isExactMatch: false,
          frequency: match.frequency,
          confidence: 0.75
        });
        seenWords.add(formatted.toLowerCase());
        if (suggestions.length >= settings.suggestionCount) break;
      }
    }

    // Add fuzzy corrections
    for (const cand of fuzzyCandidates) {
      const formatted = formatCase(cand.word);
      if (!seenWords.has(formatted.toLowerCase()) && (!settings.blockOffensiveWords || !this.blockedWords.has(cand.word))) {
        suggestions.push({
          word: formatted,
          isAutoCorrect: true,
          isLearned: this.userDictionary.has(cand.word),
          isExactMatch: false,
          frequency: cand.frequency,
          confidence: Math.max(0.4, 0.9 - cand.score * 0.2)
        });
        seenWords.add(formatted.toLowerCase());
        if (suggestions.length >= settings.suggestionCount) break;
      }
    }

    return suggestions.slice(0, settings.suggestionCount);
  }

  public getWordContext(textBeforeCursor: string): { currentWord: string; precedingText: string; previousWord?: string } {
    const wordsBefore = textBeforeCursor.split(/\s+/);
    const currentWord = wordsBefore[wordsBefore.length - 1] || '';
    const precedingText = textBeforeCursor.slice(0, textBeforeCursor.length - currentWord.length);
    const precedingWords = precedingText.trim().split(/\s+/).filter(Boolean);
    const previousWord = precedingWords[precedingWords.length - 1];

    return { currentWord, precedingText, previousWord };
  }

  public getAutoCorrection(currentWord: string, precedingText: string, settings: HeliBoardTypingSettings): string | null {
    if (!currentWord || settings.autoCorrection === 'off') return null;
    const suggestion = this.getSuggestions(currentWord, precedingText, settings)[0];
    if (!suggestion?.isAutoCorrect || suggestion.word.toLowerCase() === currentWord.toLowerCase()) return null;
    return suggestion.word;
  }

  // HeliBoard Gesture / Glide Typing Decoder
  public decodeGlideTrail(keySequence: string[]): string | null {
    if (!keySequence || keySequence.length < 2) return null;
    const startChar = keySequence[0].toLowerCase();
    const endChar = keySequence[keySequence.length - 1].toLowerCase();

    // Find candidate words in dictionary that start with startChar and end with endChar
    const candidates: { word: string; score: number }[] = [];
    const pool = Array.from(this.userDictionary.keys()).concat(
      BASE_VOCABULARY.map(([w]) => w)
    );

    for (const word of pool) {
      if (word.length < 3) continue;
      if (word[0] !== startChar || word[word.length - 1] !== endChar) continue;

      // Check if candidate word characters appear in sequence along the key path
      let keyIdx = 0;
      let matchedCount = 0;
      for (let i = 0; i < word.length; i++) {
        const char = word[i];
        while (keyIdx < keySequence.length) {
          if (keySequence[keyIdx].toLowerCase() === char) {
            matchedCount++;
            keyIdx++;
            break;
          }
          keyIdx++;
        }
      }

      if (matchedCount >= word.length - 1) {
        const lengthDiff = Math.abs(keySequence.length - word.length);
        const userBonus = this.userDictionary.has(word) ? 40 : 0;
        candidates.push({
          word,
          score: matchedCount * 10 - lengthDiff + userBonus
        });
      }
    }

    if (candidates.length === 0) {
      // Fallback: return simple string of distinct traversed keys
      return keySequence.join('');
    }

    candidates.sort((a, b) => b.score - a.score);
    return candidates[0].word;
  }

  private collectTrieWords(node: TrieNode, results: { word: string; frequency: number }[], limit: number) {
    if (results.length >= limit) return;
    if (node.isEndOfWord) {
      results.push({ word: node.word, frequency: node.frequency });
    }
    for (const child of node.children.values()) {
      this.collectTrieWords(child, results, limit);
      if (results.length >= limit) break;
    }
  }

  // Dictionary management APIs
  public getUserDictionaryEntries(): UserDictionaryEntry[] {
    return Array.from(this.userDictionary.values()).sort((a, b) => b.frequency - a.frequency);
  }

  public addUserWord(word: string, shortcut?: string, frequency: number = 200) {
    const clean = word.trim().toLowerCase();
    if (!clean) return;
    const entry: UserDictionaryEntry = {
      id: 'custom_' + Date.now(),
      word: clean,
      shortcut: shortcut?.trim().toLowerCase(),
      frequency,
      locale: 'en-US',
      isCustom: true,
      dateAdded: Date.now()
    };
    this.userDictionary.set(clean, entry);
    this.insertTrie(clean, frequency);
    if (entry.shortcut) {
      this.shortcuts.set(entry.shortcut, clean);
    }
    this.saveUserDictionary();
  }

  public removeUserWord(word: string) {
    const clean = word.trim().toLowerCase();
    const entry = this.userDictionary.get(clean);
    if (entry?.shortcut) {
      this.shortcuts.delete(entry.shortcut);
    }
    this.userDictionary.delete(clean);
    this.rebuildTrie();
    this.saveUserDictionary();
  }

  public clearAllUserData() {
    this.userDictionary.clear();
    this.userBigrams.clear();
    this.shortcuts.clear();
    localStorage.removeItem(HeliBoardDictionaryEngine.STORAGE_USER_DICT_KEY);
    localStorage.removeItem(HeliBoardDictionaryEngine.STORAGE_USER_BIGRAMS_KEY);
    localStorage.removeItem(HeliBoardDictionaryEngine.STORAGE_SHORTCUTS_KEY);
    this.initBaseDictionary();
  }

  public exportUserDataJson(): string {
    return JSON.stringify({
      dictionary: Array.from(this.userDictionary.values()),
      shortcuts: Object.fromEntries(this.shortcuts.entries()),
      blocked: Array.from(this.blockedWords)
    }, null, 2);
  }

  public importUserDataJson(jsonStr: string): boolean {
    try {
      const data = JSON.parse(jsonStr);
      if (Array.isArray(data.dictionary)) {
        for (const entry of data.dictionary) {
          if (entry.word) {
            this.userDictionary.set(entry.word.toLowerCase(), entry);
            this.insertTrie(entry.word.toLowerCase(), entry.frequency || 180);
          }
        }
        this.saveUserDictionary();
      }
      if (data.shortcuts && typeof data.shortcuts === 'object') {
        for (const [k, v] of Object.entries(data.shortcuts)) {
          this.shortcuts.set(k.toLowerCase(), String(v));
        }
      }
      return true;
    } catch {
      return false;
    }
  }

  public getShortcutsList(): { shortcut: string; expansion: string }[] {
    return Array.from(this.shortcuts.entries()).map(([shortcut, expansion]) => ({
      shortcut,
      expansion
    }));
  }

  public getBlockedWordsList(): string[] {
    return Array.from(this.blockedWords);
  }

  public toggleBlockWord(word: string) {
    const clean = word.trim().toLowerCase();
    if (this.blockedWords.has(clean)) {
      this.blockedWords.delete(clean);
    } else {
      this.blockedWords.add(clean);
    }
    try {
      localStorage.setItem(
        HeliBoardDictionaryEngine.STORAGE_BLOCKED_KEY,
        JSON.stringify(Array.from(this.blockedWords))
      );
    } catch {
      // Storage error
    }
  }
}

// Global Singleton instance for seamless state across components
export const dictionaryEngine = new HeliBoardDictionaryEngine();
