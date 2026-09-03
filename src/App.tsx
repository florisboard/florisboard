/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect } from 'react';
import { DevicePreview } from './components/sandbox/DevicePreview';
import { FlorisSettings } from './components/settings/FlorisSettings';
import { AppSettings, ThemeConfig } from './types/keyboard';
import { DEFAULT_APP_SETTINGS } from './data/defaultSettings';
import { THEME_PRESETS, DEFAULT_THEME } from './themes/themePresets';

const SETTINGS_STORAGE_KEY = 'floris_heliboard_app_settings';

export default function App() {
  const [settings, setSettings] = useState<AppSettings>(() => {
    try {
      const stored = localStorage.getItem(SETTINGS_STORAGE_KEY);
      if (stored) {
        return { ...DEFAULT_APP_SETTINGS, ...JSON.parse(stored) };
      }
    } catch {
      // LocalStorage access error
    }
    return DEFAULT_APP_SETTINGS;
  });

  const [viewMode, setViewMode] = useState<'simulator' | 'settings'>('simulator');

  // Compute active theme from settings
  const currentTheme =
    THEME_PRESETS.find(t => t.id === settings.ui.themeId) || DEFAULT_THEME;

  // Persist settings whenever changed
  useEffect(() => {
    try {
      localStorage.setItem(SETTINGS_STORAGE_KEY, JSON.stringify(settings));
    } catch {
      // Storage quota or error
    }
  }, [settings]);

  const handleUpdateSettings = (newSettings: AppSettings) => {
    setSettings(newSettings);
  };

  const handleSelectTheme = (themeId: string) => {
    setSettings(prev => ({
      ...prev,
      ui: { ...prev.ui, themeId }
    }));
  };

  return (
    <main
      className="w-screen h-screen overflow-hidden flex flex-col font-sans transition-colors"
      style={{
        backgroundColor: currentTheme.bgMain,
        color: currentTheme.textPrimary
      }}
    >
      {viewMode === 'simulator' ? (
        <DevicePreview
          settings={settings}
          theme={currentTheme}
          onUpdateSettings={handleUpdateSettings}
          onSelectTheme={handleSelectTheme}
          onOpenSettings={() => setViewMode('settings')}
        />
      ) : (
        <FlorisSettings
          settings={settings}
          theme={currentTheme}
          onUpdateSettings={handleUpdateSettings}
          onSelectTheme={handleSelectTheme}
          onBack={() => setViewMode('simulator')}
        />
      )}
    </main>
  );
}
