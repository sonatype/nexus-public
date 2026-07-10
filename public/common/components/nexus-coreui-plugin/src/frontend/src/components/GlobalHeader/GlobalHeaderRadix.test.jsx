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
import {render, screen, act} from '@testing-library/react';
import '@testing-library/jest-dom';
import {TooltipProvider} from '@radix-ui/react-tooltip';
import {Theme} from '@radix-ui/themes';

let mockUser = {authenticated: true};
let mockStatus = {edition: 'PRO'};
let mockStateValues = {
  'nexus-context-path': '',
  anonymousEnabled: true,
  loggedInEnabled: true,
};

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    useUser: () => mockUser,
    useStatus: () => mockStatus,
    state: () => ({
      getValue: (key, defaultVal) => mockStateValues[key] ?? defaultVal,
    }),
    checkPermission: () => true,
    isExtJsRendered: () => false,
    showSuccessMessage: jest.fn(),
  },
  handleExtJsUnsavedChanges: (_ctrl, fn) => fn(),
}));

jest.mock('@uirouter/react', () => ({
  useRouter: () => ({
    stateService: {go: jest.fn()},
    stateRegistry: {get: () => ({data: {visibilityRequirements: []}})},
  }),
  useCurrentStateAndParams: () => ({state: {name: 'browse.welcome'}, params: {}}),
}));

jest.mock('../../contexts/ThemeContext', () => ({
  useTheme: () => ({effectiveTheme: 'light', setTheme: jest.fn()}),
}));

jest.mock('./SearchRadix', () => () => <div data-testid="search-radix" />);
jest.mock('./SystemStatusRadix', () => () => <div data-testid="system-status" />);
jest.mock('../ThemeSwitcher/ThemeSwitcher', () => () => null);
jest.mock('./HelpMenuRadix', () => () => <div data-testid="help-menu" />);
jest.mock('./LoginAndUserButtonRadix', () => () => <div data-testid="login-btn" />);
jest.mock('../../routerConfig/routerUtils', () => ({refreshReactPage: jest.fn()}));
jest.mock('../../hooks/useSideNavbarCollapsedState', () => () => [true]);

import GlobalHeaderRadix from './GlobalHeaderRadix';

function renderHeader(props) {
  return render(
    <Theme>
      <TooltipProvider>
        <GlobalHeaderRadix {...props} />
      </TooltipProvider>
    </Theme>
  );
}

describe('GlobalHeaderRadix', () => {
  beforeEach(() => {
    mockUser = {authenticated: true};
    mockStatus = {edition: 'PRO'};
    mockStateValues = {
      'nexus-context-path': '',
      anonymousEnabled: true,
      loggedInEnabled: true,
    };
    window.location.hash = '';
  });

  it('renders the header', () => {
    renderHeader();
    expect(screen.getByTestId('search-radix')).toBeInTheDocument();
  });

  it('shows UI toggle when user has access', () => {
    renderHeader();
    expect(screen.getByText('Switch to Nexus One UI')).toBeInTheDocument();
  });

  it('hides toggle for unauthenticated user with anonymous disabled', () => {
    mockUser = {authenticated: false};
    mockStateValues.anonymousEnabled = false;

    renderHeader();

    expect(screen.queryByText('Switch to Nexus One UI')).not.toBeInTheDocument();
  });

  describe('race condition fix (bug gyca)', () => {
    it('keeps toggle visible when loggedInEnabled flashes false during login', () => {
      const {rerender} = renderHeader();
      expect(screen.getByText('Switch to Nexus One UI')).toBeInTheDocument();

      mockStateValues.loggedInEnabled = false;
      mockUser = {authenticated: true, id: 'admin'};

      rerender(<Theme><TooltipProvider><GlobalHeaderRadix /></TooltipProvider></Theme>);

      expect(screen.getByText('Switch to Nexus One UI')).toBeInTheDocument();
    });

    it('keeps toggle visible during route transitions that reset ExtJS state', () => {
      const {rerender} = renderHeader();
      expect(screen.getByText('Switch to Nexus One UI')).toBeInTheDocument();

      mockStateValues.loggedInEnabled = false;
      rerender(<Theme><TooltipProvider><GlobalHeaderRadix /></TooltipProvider></Theme>);
      expect(screen.getByText('Switch to Nexus One UI')).toBeInTheDocument();

      mockStateValues.loggedInEnabled = true;
      rerender(<Theme><TooltipProvider><GlobalHeaderRadix /></TooltipProvider></Theme>);
      expect(screen.getByText('Switch to Nexus One UI')).toBeInTheDocument();
    });

    it('hides toggle on logout when anonymous access is disabled', () => {
      const {rerender} = renderHeader();
      expect(screen.getByText('Switch to Nexus One UI')).toBeInTheDocument();

      mockUser = {authenticated: false};
      mockStateValues.anonymousEnabled = false;

      rerender(<Theme><TooltipProvider><GlobalHeaderRadix /></TooltipProvider></Theme>);

      expect(screen.queryByText('Switch to Nexus One UI')).not.toBeInTheDocument();
    });

    it('keeps toggle visible on logout when anonymous access is enabled', () => {
      const {rerender} = renderHeader();
      expect(screen.getByText('Switch to Nexus One UI')).toBeInTheDocument();

      mockUser = {authenticated: false};
      mockStateValues.anonymousEnabled = true;

      rerender(<Theme><TooltipProvider><GlobalHeaderRadix /></TooltipProvider></Theme>);

      expect(screen.getByText('Switch to Nexus One UI')).toBeInTheDocument();
    });

    it('does not redirect out of preview during flag flaps', () => {
      window.location.hash = 'preview/browse/welcome';

      const {rerender} = renderHeader();

      mockStateValues.loggedInEnabled = false;
      rerender(<Theme><TooltipProvider><GlobalHeaderRadix /></TooltipProvider></Theme>);

      expect(window.location.hash).toBe('#preview/browse/welcome');
    });
  });
});
