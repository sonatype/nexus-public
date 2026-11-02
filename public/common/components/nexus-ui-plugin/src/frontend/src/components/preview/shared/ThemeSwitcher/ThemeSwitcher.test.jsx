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
import {render, screen, fireEvent} from '@testing-library/react';
import '@testing-library/jest-dom';
import {ThemeProvider, THEMES} from '../../../../contexts/ThemeContext';
import ThemeSwitcher from './ThemeSwitcher';

// This component is shared by cloud and self-hosted (NEXUS-54869). It renders through the
// portal-aware Tooltip from ../Tooltip rather than @radix-ui/themes directly, so tooltips
// portaled outside the Theme subtree still inherit theme variables - stub it the same way
// HelpMenu.test.jsx does, since jsdom cannot drive Radix Tooltip's hover/portal behavior.
jest.mock('../Tooltip', () => ({
  Tooltip: ({children}) => <>{children}</>,
}));

describe('ThemeSwitcher', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    // ThemeSwitcher only renders in Preview UI, so simulate that context.
    window.location.hash = '#preview/browse/welcome';
    window.matchMedia = jest.fn().mockImplementation((query) => ({
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

  const renderWithTheme = () =>
    render(
      <ThemeProvider>
        <ThemeSwitcher />
      </ThemeProvider>,
    );

  it('renders a single button with aria-label by default', () => {
    renderWithTheme();

    const button = screen.getByRole('button', {name: 'Switch to dark mode'});
    expect(button).toHaveAttribute('aria-label', 'Switch to dark mode');
  });

  it('carries the analytics id used for header telemetry', () => {
    renderWithTheme();

    expect(screen.getByRole('button', {name: 'Switch to dark mode'})).toHaveAttribute(
      'data-analytics-id',
      'nxrm-header-toggle-theme',
    );
  });

  it('toggles between light and dark on click', () => {
    renderWithTheme();

    // Start: light mode - button says "Switch to dark mode".
    const lightButton = screen.getByRole('button', {name: 'Switch to dark mode'});
    fireEvent.click(lightButton);
    expect(document.documentElement.getAttribute('data-theme')).toBe(THEMES.DARK);
    expect(screen.getByRole('button', {name: 'Switch to light mode'})).toBeInTheDocument();

    // Click: dark -> light.
    fireEvent.click(screen.getByRole('button', {name: 'Switch to light mode'}));
    expect(document.documentElement.getAttribute('data-theme')).toBe(THEMES.LIGHT);
    expect(screen.getByRole('button', {name: 'Switch to dark mode'})).toBeInTheDocument();
  });

  it('persists theme selection to localStorage', () => {
    renderWithTheme();

    fireEvent.click(screen.getByRole('button', {name: 'Switch to dark mode'}));

    expect(localStorage.getItem('nexus-theme-preference')).toBe(THEMES.DARK);
  });

  it('restores theme selection from localStorage', () => {
    localStorage.setItem('nexus-theme-preference', THEMES.DARK);

    renderWithTheme();

    expect(screen.getByRole('button', {name: 'Switch to light mode'})).toBeInTheDocument();
    expect(document.documentElement.getAttribute('data-theme')).toBe(THEMES.DARK);
  });

  it('renders the correct icon for each theme (Lucide Sun/MoonStar)', () => {
    renderWithTheme();

    const lightButton = screen.getByRole('button', {name: 'Switch to dark mode'});
    expect(lightButton.querySelector('svg')).toBeInTheDocument();

    fireEvent.click(lightButton);
    const darkButton = screen.getByRole('button', {name: 'Switch to light mode'});
    expect(darkButton.querySelector('svg')).toBeInTheDocument();
  });
});
