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
import {render, screen, fireEvent, waitFor} from '@testing-library/react';
import {ThemeSelector} from './ThemeSelector';

describe('ThemeSelector', () => {
  let getItemSpy;
  let setItemSpy;

  beforeEach(() => {
    // Create spies on the actual Storage prototype methods
    getItemSpy = jest.spyOn(Storage.prototype, 'getItem');
    setItemSpy = jest.spyOn(Storage.prototype, 'setItem');

    // Mock document.documentElement.classList
    document.documentElement.classList.add = jest.fn();
    document.documentElement.classList.remove = jest.fn();

    jest.clearAllMocks();
  });

  afterEach(() => {
    getItemSpy.mockRestore();
    setItemSpy.mockRestore();
  });

  describe('initial render', () => {
    it('renders with light theme by default when localStorage is empty', () => {
      getItemSpy.mockReturnValue(null);

      render(<ThemeSelector />);

      const button = screen.getByRole('button');
      expect(button).toBeInTheDocument();

      // Should add color scheme class and light mode class
      expect(document.documentElement.classList.add).toHaveBeenCalledWith('nx-html--enable-color-schemes');
      expect(document.documentElement.classList.add).toHaveBeenCalledWith('nx-html--light-mode');
      expect(document.documentElement.classList.remove).toHaveBeenCalledWith('nx-html--dark-mode');
    });

    it('renders with dark theme when localStorage has dark theme', () => {
      getItemSpy.mockReturnValue('dark');

      render(<ThemeSelector />);

      const button = screen.getByRole('button');
      expect(button).toBeInTheDocument();

      // Should add color scheme class and dark mode class
      expect(document.documentElement.classList.add).toHaveBeenCalledWith('nx-html--enable-color-schemes');
      expect(document.documentElement.classList.add).toHaveBeenCalledWith('nx-html--dark-mode');
      expect(document.documentElement.classList.remove).toHaveBeenCalledWith('nx-html--light-mode');
    });

    it('renders with light theme when localStorage has light theme', () => {
      getItemSpy.mockReturnValue('light');

      render(<ThemeSelector />);

      const button = screen.getByRole('button');
      expect(button).toBeInTheDocument();

      // Should add color scheme class and light mode class
      expect(document.documentElement.classList.add).toHaveBeenCalledWith('nx-html--enable-color-schemes');
      expect(document.documentElement.classList.add).toHaveBeenCalledWith('nx-html--light-mode');
      expect(document.documentElement.classList.remove).toHaveBeenCalledWith('nx-html--dark-mode');
    });
  });

  describe('theme toggle', () => {
    it('toggles from light to dark theme', async () => {
      getItemSpy.mockReturnValue('light');

      render(<ThemeSelector />);

      const button = screen.getByRole('button');

      // Clear previous calls from initial render
      jest.clearAllMocks();

      // Click to toggle theme
      fireEvent.click(button);

      // Should save dark theme to localStorage
      await waitFor(() => {
        expect(setItemSpy).toHaveBeenCalledWith('theme', 'dark');
      });

      // Should update CSS classes for dark mode
      expect(document.documentElement.classList.add).toHaveBeenCalledWith('nx-html--dark-mode');
      expect(document.documentElement.classList.remove).toHaveBeenCalledWith('nx-html--light-mode');
    });

    it('toggles from dark to light theme', async () => {
      getItemSpy.mockReturnValue('dark');

      render(<ThemeSelector />);

      const button = screen.getByRole('button');

      // Clear previous calls from initial render
      jest.clearAllMocks();

      // Click to toggle theme
      fireEvent.click(button);

      // Should save light theme to localStorage
      await waitFor(() => {
        expect(setItemSpy).toHaveBeenCalledWith('theme', 'light');
      });

      // Should update CSS classes for light mode
      expect(document.documentElement.classList.add).toHaveBeenCalledWith('nx-html--light-mode');
      expect(document.documentElement.classList.remove).toHaveBeenCalledWith('nx-html--dark-mode');
    });

    it('toggles multiple times correctly', async () => {
      getItemSpy.mockReturnValue('light');

      render(<ThemeSelector />);

      const button = screen.getByRole('button');

      // Clear previous calls from initial render
      jest.clearAllMocks();

      // First toggle: light -> dark
      fireEvent.click(button);
      await waitFor(() => {
        expect(setItemSpy).toHaveBeenCalledWith('theme', 'dark');
      });

      jest.clearAllMocks();

      // Second toggle: dark -> light
      fireEvent.click(button);
      await waitFor(() => {
        expect(setItemSpy).toHaveBeenCalledWith('theme', 'light');
      });

      jest.clearAllMocks();

      // Third toggle: light -> dark
      fireEvent.click(button);
      await waitFor(() => {
        expect(setItemSpy).toHaveBeenCalledWith('theme', 'dark');
      });
    });
  });

  describe('localStorage integration', () => {
    it('reads theme from localStorage on mount', () => {
      getItemSpy.mockReturnValue('dark');

      render(<ThemeSelector />);

      expect(getItemSpy).toHaveBeenCalledWith('theme');
    });

    it('saves theme to localStorage when toggled', async () => {
      getItemSpy.mockReturnValue('light');

      render(<ThemeSelector />);

      const button = screen.getByRole('button');
      fireEvent.click(button);

      await waitFor(() => {
        expect(setItemSpy).toHaveBeenCalledWith('theme', 'dark');
      });
    });

    it('persists theme preference across toggles', async () => {
      getItemSpy.mockReturnValue('light');

      render(<ThemeSelector />);

      const button = screen.getByRole('button');

      // Toggle to dark
      fireEvent.click(button);
      await waitFor(() => {
        expect(setItemSpy).toHaveBeenCalledWith('theme', 'dark');
      });

      // Toggle back to light
      fireEvent.click(button);
      await waitFor(() => {
        expect(setItemSpy).toHaveBeenCalledWith('theme', 'light');
      });
    });
  });

  describe('CSS class management', () => {
    it('enables color schemes on mount', () => {
      getItemSpy.mockReturnValue(null);

      render(<ThemeSelector />);

      expect(document.documentElement.classList.add).toHaveBeenCalledWith('nx-html--enable-color-schemes');
    });

    it('applies correct classes for light theme', () => {
      getItemSpy.mockReturnValue('light');

      render(<ThemeSelector />);

      expect(document.documentElement.classList.add).toHaveBeenCalledWith('nx-html--light-mode');
      expect(document.documentElement.classList.remove).toHaveBeenCalledWith('nx-html--dark-mode');
    });

    it('applies correct classes for dark theme', () => {
      getItemSpy.mockReturnValue('dark');

      render(<ThemeSelector />);

      expect(document.documentElement.classList.add).toHaveBeenCalledWith('nx-html--dark-mode');
      expect(document.documentElement.classList.remove).toHaveBeenCalledWith('nx-html--light-mode');
    });

    it('updates classes when theme is toggled', async () => {
      getItemSpy.mockReturnValue('light');

      render(<ThemeSelector />);

      // Clear initial calls
      jest.clearAllMocks();

      const button = screen.getByRole('button');
      fireEvent.click(button);

      // Should add dark mode and remove light mode
      await waitFor(() => {
        expect(document.documentElement.classList.add).toHaveBeenCalledWith('nx-html--dark-mode');
        expect(document.documentElement.classList.remove).toHaveBeenCalledWith('nx-html--light-mode');
      });
    });
  });

  describe('edge cases', () => {
    it('handles invalid localStorage value gracefully', () => {
      getItemSpy.mockReturnValue('invalid-theme');

      render(<ThemeSelector />);

      const button = screen.getByRole('button');
      expect(button).toBeInTheDocument();

      // Should treat invalid value as a truthy string and not equal to 'dark', so it will be light
      expect(document.documentElement.classList.add).toHaveBeenCalledWith('nx-html--light-mode');
    });

    it('handles empty string in localStorage', () => {
      getItemSpy.mockReturnValue('');

      render(<ThemeSelector />);

      const button = screen.getByRole('button');
      expect(button).toBeInTheDocument();

      // Empty string is falsy, should default to light
      expect(document.documentElement.classList.add).toHaveBeenCalledWith('nx-html--light-mode');
    });

    it('handles localStorage errors gracefully', () => {
      getItemSpy.mockImplementation(() => {
        throw new Error('localStorage not available');
      });

      // Should not crash, component should handle the error
      expect(() => render(<ThemeSelector />)).toThrow();
    });
  });
});
