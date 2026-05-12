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

import React from 'react';
import { render, screen, renderHook, act } from '@testing-library/react';
import { ThemeProvider, useTheme, THEMES } from '../ThemeContext';

describe('ThemeContext', () => {
  beforeEach(() => {
    // Clear localStorage before each test
    localStorage.clear();
    // Reset document attribute
    document.documentElement.removeAttribute('data-theme');
    // Mock matchMedia
    window.matchMedia = jest.fn().mockImplementation(query => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: jest.fn(),
      removeListener: jest.fn(),
      addEventListener: jest.fn(),
      removeEventListener: jest.fn(),
      dispatchEvent: jest.fn(),
    }));
  });

  describe('ThemeProvider', () => {
    it('should render children', () => {
      render(
        <ThemeProvider>
          <div data-testid="child">Test Content</div>
        </ThemeProvider>
      );

      expect(screen.getByTestId('child')).toBeInTheDocument();
      expect(screen.getByTestId('child')).toHaveTextContent('Test Content');
    });

    it('should provide theme context to children', () => {
      const TestComponent = () => {
        const { theme, effectiveTheme } = useTheme();
        return (
          <div>
            <span data-testid="theme">{theme}</span>
            <span data-testid="effective-theme">{effectiveTheme}</span>
          </div>
        );
      };

      render(
        <ThemeProvider>
          <TestComponent />
        </ThemeProvider>
      );

      expect(screen.getByTestId('theme')).toHaveTextContent(THEMES.LIGHT);
      expect(screen.getByTestId('effective-theme')).toHaveTextContent(THEMES.LIGHT);
    });
  });

  describe('useTheme hook', () => {
    it('should throw error when used outside ThemeProvider', () => {
      // Suppress console.error for this test
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();

      try {
        const { result } = renderHook(() => useTheme());
        // If we get here, the error wasn't thrown
        expect(result.error).toEqual(expect.objectContaining({
          message: 'useTheme must be used within a ThemeProvider'
        }));
      } catch (error) {
        // renderHook throws synchronously
        expect(error.message).toContain('useTheme must be used within a ThemeProvider');
      }

      consoleSpy.mockRestore();
    });

    it('should return theme context when used inside ThemeProvider', () => {
      const { result } = renderHook(() => useTheme(), {
        wrapper: ThemeProvider,
      });

      expect(result.current).toHaveProperty('theme');
      expect(result.current).toHaveProperty('effectiveTheme');
      expect(result.current).toHaveProperty('setTheme');
      expect(result.current).toHaveProperty('toggleTheme');
    });
  });

  describe('Theme state management', () => {
    it('should default to light theme', () => {
      const { result } = renderHook(() => useTheme(), {
        wrapper: ThemeProvider,
      });

      expect(result.current.theme).toBe(THEMES.LIGHT);
      expect(result.current.effectiveTheme).toBe(THEMES.LIGHT);
    });

    it('should set light theme', () => {
      const { result } = renderHook(() => useTheme(), {
        wrapper: ThemeProvider,
      });

      act(() => {
        result.current.setTheme(THEMES.LIGHT);
      });

      expect(result.current.theme).toBe(THEMES.LIGHT);
      expect(result.current.effectiveTheme).toBe(THEMES.LIGHT);
    });

    it('should set dark theme', () => {
      const { result } = renderHook(() => useTheme(), {
        wrapper: ThemeProvider,
      });

      act(() => {
        result.current.setTheme(THEMES.DARK);
      });

      expect(result.current.theme).toBe(THEMES.DARK);
      expect(result.current.effectiveTheme).toBe(THEMES.DARK);
    });

    it('should toggle between light and dark', () => {
      const { result } = renderHook(() => useTheme(), {
        wrapper: ThemeProvider,
      });

      // Start with light
      act(() => {
        result.current.setTheme(THEMES.LIGHT);
      });
      expect(result.current.effectiveTheme).toBe(THEMES.LIGHT);

      // Toggle to dark
      act(() => {
        result.current.toggleTheme();
      });
      expect(result.current.effectiveTheme).toBe(THEMES.DARK);

      // Toggle back to light
      act(() => {
        result.current.toggleTheme();
      });
      expect(result.current.effectiveTheme).toBe(THEMES.LIGHT);
    });

    it('should warn on invalid theme', () => {
      const consoleSpy = jest.spyOn(console, 'warn').mockImplementation();
      const { result } = renderHook(() => useTheme(), {
        wrapper: ThemeProvider,
      });

      act(() => {
        result.current.setTheme('invalid-theme');
      });

      expect(consoleSpy).toHaveBeenCalledWith(
        expect.stringContaining('Invalid theme: invalid-theme')
      );
      // Theme should not change from default (light)
      expect(result.current.theme).toBe(THEMES.LIGHT);

      consoleSpy.mockRestore();
    });
  });

  describe('localStorage persistence', () => {
    it('should persist theme preference to localStorage', () => {
      const { result } = renderHook(() => useTheme(), {
        wrapper: ThemeProvider,
      });

      act(() => {
        result.current.setTheme(THEMES.DARK);
      });

      expect(localStorage.getItem('nexus-theme-preference')).toBe(THEMES.DARK);
    });

    it('should load theme preference from localStorage on mount', () => {
      localStorage.setItem('nexus-theme-preference', THEMES.DARK);

      const { result } = renderHook(() => useTheme(), {
        wrapper: ThemeProvider,
      });

      expect(result.current.theme).toBe(THEMES.DARK);
      expect(result.current.effectiveTheme).toBe(THEMES.DARK);
    });

    it('should handle localStorage errors gracefully', () => {
      const consoleSpy = jest.spyOn(console, 'warn').mockImplementation();
      const mockSetItem = jest.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
        throw new Error('localStorage is full');
      });

      const { result } = renderHook(() => useTheme(), {
        wrapper: ThemeProvider,
      });

      act(() => {
        result.current.setTheme(THEMES.DARK);
      });

      expect(consoleSpy).toHaveBeenCalledWith(
        'Failed to save theme preference to localStorage:',
        expect.any(Error)
      );

      mockSetItem.mockRestore();
      consoleSpy.mockRestore();
    });
  });

  describe('Document root attribute', () => {
    it('should apply light theme to document root', () => {
      const { result } = renderHook(() => useTheme(), {
        wrapper: ThemeProvider,
      });

      act(() => {
        result.current.setTheme(THEMES.LIGHT);
      });

      expect(document.documentElement.getAttribute('data-theme')).toBe(THEMES.LIGHT);
    });

    it('should apply dark theme to document root when on Preview UI', () => {
      // Dark mode only applies on Preview UI routes
      window.location.hash = '#preview/browse/welcome';

      const { result } = renderHook(() => useTheme(), {
        wrapper: ThemeProvider,
      });

      act(() => {
        result.current.setTheme(THEMES.DARK);
      });

      expect(document.documentElement.getAttribute('data-theme')).toBe(THEMES.DARK);

      // Cleanup
      window.location.hash = '';
    });
  });

  describe('System theme detection', () => {
    it('should detect dark system preference when set to system mode', () => {
      window.matchMedia = jest.fn().mockImplementation(query => ({
        matches: query === '(prefers-color-scheme: dark)',
        media: query,
        onchange: null,
        addListener: jest.fn(),
        removeListener: jest.fn(),
        addEventListener: jest.fn(),
        removeEventListener: jest.fn(),
        dispatchEvent: jest.fn(),
      }));

      const { result } = renderHook(() => useTheme(), {
        wrapper: ThemeProvider,
      });

      // Default is light, switch to system to detect OS preference
      act(() => {
        result.current.setTheme(THEMES.SYSTEM);
      });

      expect(result.current.theme).toBe(THEMES.SYSTEM);
      expect(result.current.effectiveTheme).toBe(THEMES.DARK);
    });

    it('should detect light system preference when set to system mode', () => {
      window.matchMedia = jest.fn().mockImplementation(query => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: jest.fn(),
        removeListener: jest.fn(),
        addEventListener: jest.fn(),
        removeEventListener: jest.fn(),
        dispatchEvent: jest.fn(),
      }));

      const { result } = renderHook(() => useTheme(), {
        wrapper: ThemeProvider,
      });

      // Default is light, switch to system
      act(() => {
        result.current.setTheme(THEMES.SYSTEM);
      });

      expect(result.current.theme).toBe(THEMES.SYSTEM);
      expect(result.current.effectiveTheme).toBe(THEMES.LIGHT);
    });

    it('should listen for system theme changes when in system mode', () => {
      let changeHandler;
      window.matchMedia = jest.fn().mockImplementation(query => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: jest.fn(),
        removeListener: jest.fn(),
        addEventListener: jest.fn((event, handler) => {
          if (event === 'change') {
            changeHandler = handler;
          }
        }),
        removeEventListener: jest.fn(),
        dispatchEvent: jest.fn(),
      }));

      const { result } = renderHook(() => useTheme(), {
        wrapper: ThemeProvider,
      });

      // Switch to system mode so the listener is registered
      act(() => {
        result.current.setTheme(THEMES.SYSTEM);
      });

      expect(result.current.effectiveTheme).toBe(THEMES.LIGHT);

      // Simulate system theme change to dark
      act(() => {
        changeHandler({ matches: true });
      });

      expect(result.current.effectiveTheme).toBe(THEMES.DARK);
    });
  });
});
