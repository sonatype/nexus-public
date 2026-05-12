/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import React, {createContext, useContext, useEffect, useState, useCallback} from 'react';

/**
 * ThemeContext provides theme management for the application.
 *
 * Phase 2: Design System Foundation - Implements dark mode via CSS variables.
 * Supports three theme modes:
 * - 'light': Force light theme
 * - 'dark': Force dark theme
 * - 'system': Follow system preference (default)
 *
 * Theme preference is persisted to localStorage.
 * Theme is applied via [data-theme] attribute on document root.
 */

const THEME_STORAGE_KEY = 'nexus-theme-preference';
const THEMES = {
  LIGHT: 'light',
  DARK: 'dark',
  SYSTEM: 'system'
};

const ThemeContext = createContext({
  theme: THEMES.SYSTEM,
  effectiveTheme: THEMES.LIGHT,
  setTheme: () => {},
  toggleTheme: () => {}
});

/**
 * Gets the current system color scheme preference.
 * @returns {'light' | 'dark'}
 */
function getSystemTheme() {
  if (typeof window === 'undefined') {
    return THEMES.LIGHT;
  }
  return window.matchMedia('(prefers-color-scheme: dark)').matches
    ? THEMES.DARK
    : THEMES.LIGHT;
}

/**
 * Gets the stored theme preference from localStorage.
 * @returns {'light' | 'dark' | 'system'}
 */
function getStoredTheme() {
  if (typeof window === 'undefined') {
    return THEMES.LIGHT;
  }
  try {
    const stored = localStorage.getItem(THEME_STORAGE_KEY);
    if (stored && Object.values(THEMES).includes(stored)) {
      return stored;
    }
  } catch (error) {
    console.warn('Failed to read theme preference from localStorage:', error);
  }
  // Default to light - dark mode is only available in Preview UI
  return THEMES.LIGHT;
}

/**
 * Stores the theme preference to localStorage.
 * @param {string} theme
 */
function storeTheme(theme) {
  if (typeof window === 'undefined') {
    return;
  }
  try {
    localStorage.setItem(THEME_STORAGE_KEY, theme);
  } catch (error) {
    console.warn('Failed to save theme preference to localStorage:', error);
  }
}

/**
 * Applies the theme to the document root and body.
 * Also syncs Radix UI theme classes/attributes for portaled components (Select, Dialog, etc.)
 * @param {string} theme - 'light' or 'dark'
 */
function applyTheme(theme) {
  if (typeof document === 'undefined') {
    return;
  }
  // Apply our custom theme attribute for CSS variables
  document.documentElement.setAttribute('data-theme', theme);
  
  // Sync Radix UI theme for portaled components (Select, Dialog, etc.)
  // Radix portals render at body level and need these to inherit the correct theme
  
  // 1. Set Radix appearance classes on body
  if (theme === THEMES.DARK) {
    document.body.classList.add('dark');
    document.body.classList.remove('light');
  } else {
    document.body.classList.add('light');
    document.body.classList.remove('dark');
  }
  
  // 2. Ensure body has radix-themes class so Radix CSS selectors work
  document.body.classList.add('radix-themes');
  
  // 3. Set data-* attributes that Radix may use for theming
  document.body.setAttribute('data-is-root-theme', 'true');
  document.body.setAttribute('data-accent-color', 'green');
  document.body.setAttribute('data-gray-color', 'slate');
  document.body.setAttribute('data-radius', 'medium');
}

/**
 * Resolves the effective theme based on user preference.
 * If theme is 'system', returns the system preference.
 * @param {string} theme - User's theme preference
 * @returns {'light' | 'dark'}
 */
function resolveEffectiveTheme(theme) {
  if (theme === THEMES.SYSTEM) {
    return getSystemTheme();
  }
  return theme;
}

export function ThemeProvider({ children }) {
  const [theme, setThemeState] = useState(() => getStoredTheme());
  const [effectiveTheme, setEffectiveTheme] = useState(() =>
    resolveEffectiveTheme(getStoredTheme())
  );

  // Apply theme to document root whenever effectiveTheme changes.
  // Dark mode only applies on Preview UI routes -- Default UI (ExtJS/RSC)
  // does not support dark mode and must always render in light mode.
  // Login route always uses light since Heritage login doesn't support dark.
  useEffect(() => {
    const hash = typeof window !== 'undefined' ? window.location.hash : '';
    const isPreviewUI = hash.includes('preview/');
    const isLoginRoute = hash.includes('login');
    const appliedTheme = isPreviewUI && !isLoginRoute ? effectiveTheme : THEMES.LIGHT;
    applyTheme(appliedTheme);

    // Re-check on hash changes (user toggles between Default/Preview)
    // When switching to Preview UI, restore the persisted theme preference.
    // Login and Heritage routes always use light.
    function onHashChange() {
      const hash = window.location.hash;
      const preview = hash.includes('preview/');
      const login = hash.includes('login');
      if (preview && !login) {
        const stored = getStoredTheme();
        const resolved = resolveEffectiveTheme(stored);
        setThemeState(stored);
        setEffectiveTheme(resolved);
        applyTheme(resolved);
      } else {
        applyTheme(THEMES.LIGHT);
      }
    }
    window.addEventListener('hashchange', onHashChange);
    return () => window.removeEventListener('hashchange', onHashChange);
  }, [effectiveTheme]);

  // Listen for system theme changes when in 'system' mode
  useEffect(() => {
    if (theme !== THEMES.SYSTEM) {
      return;
    }

    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');

    function handleChange(e) {
      const newSystemTheme = e.matches ? THEMES.DARK : THEMES.LIGHT;
      setEffectiveTheme(newSystemTheme);
    }

    // Modern browsers
    if (mediaQuery.addEventListener) {
      mediaQuery.addEventListener('change', handleChange);
      return () => mediaQuery.removeEventListener('change', handleChange);
    }
    // Legacy browsers
    else if (mediaQuery.addListener) {
      mediaQuery.addListener(handleChange);
      return () => mediaQuery.removeListener(handleChange);
    }
  }, [theme]);

  const setTheme = useCallback((newTheme) => {
    if (!Object.values(THEMES).includes(newTheme)) {
      console.warn(`Invalid theme: ${newTheme}. Must be one of: ${Object.values(THEMES).join(', ')}`);
      return;
    }

    setThemeState(newTheme);
    storeTheme(newTheme);

    const resolved = resolveEffectiveTheme(newTheme);
    setEffectiveTheme(resolved);
  }, []);

  const toggleTheme = useCallback(() => {
    // Toggle between light and dark (skip system)
    const newTheme = effectiveTheme === THEMES.LIGHT ? THEMES.DARK : THEMES.LIGHT;
    setTheme(newTheme);
  }, [effectiveTheme, setTheme]);

  const value = {
    theme,           // User's preference: 'light', 'dark', or 'system'
    effectiveTheme,  // Actual theme applied: 'light' or 'dark'
    setTheme,        // Set theme preference
    toggleTheme      // Toggle between light and dark
  };

  return (
    <ThemeContext.Provider value={value}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
}

export { THEMES };
export default ThemeContext;
