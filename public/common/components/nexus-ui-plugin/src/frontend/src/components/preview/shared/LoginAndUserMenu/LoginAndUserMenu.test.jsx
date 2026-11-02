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
import {render, screen, fireEvent, act} from '@testing-library/react';
import '@testing-library/jest-dom';

const ROUTES = {
  login: 'login',
  userAccount: 'user.account',
  userNugetApiToken: 'user.nugetapitoken',
  userToken: 'user.user-token',
  previewUserAccount: 'preview.user.account',
  previewUserNugetApiToken: 'preview.user.nugetapitoken',
  previewUserToken: 'preview.user.usertoken',
};
const ACCOUNT_MENU_LABEL = 'My Account';

let mockHasUser = false;
let mockUserId = null;
let mockStateThrows = false;
let mockEffectiveTheme = 'light';
const mockSignOut = jest.fn();
const mockSetTheme = jest.fn();
const mockStateServiceGo = jest.fn();
const mockUrlServiceUrl = jest.fn();
// Keyed by route name so each DropdownMenu item's own useIsVisible() call can differ.
let mockVisibilityByRoute = {};

jest.mock('../hooks', () => ({
  useHasUser: () => mockHasUser,
}));

jest.mock('../../../../interface/ExtJS', () => ({
  state: () => {
    if (mockStateThrows) throw new Error('state unavailable during logout');
    return {getValue: (key) => (key === 'user' ? {id: mockUserId} : undefined)};
  },
  signOut: () => mockSignOut(),
}));

jest.mock('../../../../router/useIsVisible', () => (visibilityRequirements) =>
  mockVisibilityByRoute[visibilityRequirements?.route] ?? false
);

jest.mock('../../../../contexts/ThemeContext', () => ({
  useTheme: () => ({setTheme: mockSetTheme, effectiveTheme: mockEffectiveTheme}),
  THEMES: {LIGHT: 'light', DARK: 'dark'},
}));

jest.mock('../Tooltip', () => ({
  Tooltip: ({children}) => <>{children}</>,
  usePortalContainer: () => null,
}));

jest.mock('@uirouter/react', () => ({
  useRouter: () => ({
    urlService: {url: () => mockUrlServiceUrl()},
    stateService: {go: (...args) => mockStateServiceGo(...args)},
    stateRegistry: {
      get: (routeName) => ({data: {visibilityRequirements: {route: routeName}}}),
    },
  }),
  useSref: (routeName) => ({href: routeName ? `#${routeName}` : undefined}),
}));

jest.mock('lucide-react', () => ({
  User: () => <span data-testid="icon-user" />,
  Key: () => <span data-testid="icon-key" />,
  LogOut: () => <span data-testid="icon-logout" />,
  Sun: () => <span data-testid="icon-sun" />,
  MoonStar: () => <span data-testid="icon-moonstar" />,
  Repeat: () => <span data-testid="icon-repeat" />,
}));

/*
 * jsdom does not drive Radix DropdownMenu's open/close reliably (pointer-capture),
 * so the menu content never mounts on a trigger click. Substitute inline renderers
 * for each DropdownMenu part - same approach as HelpMenu.test.jsx - keeping the
 * assertions focused on this component's own structure rather than Radix's
 * open-state machinery.
 */
jest.mock('@radix-ui/themes', () => {
  const React = require('react');
  const Root = ({children}) => <div>{children}</div>;
  const Trigger = ({children}) => <>{children}</>;
  const Content = ({children}) => <div role="menu">{children}</div>;
  const Item = ({children, onSelect, onClick, asChild, ...rest}) => {
    const handler = onClick ?? onSelect;
    if (asChild) return React.cloneElement(children, {role: 'menuitem', ...rest});
    return (
      <button type="button" role="menuitem" onClick={handler} {...rest}>
        {children}
      </button>
    );
  };
  const Separator = () => <hr />;
  const Label = ({children}) => <div>{children}</div>;
  return {
    DropdownMenu: {Root, Trigger, Content, Item, Separator, Label},
    Flex: ({children}) => <span>{children}</span>,
    IconButton: ({children, ...rest}) => (
      <button type="button" {...rest}>
        {children}
      </button>
    ),
    Text: ({children}) => <span>{children}</span>,
  };
});

import LoginAndUserMenu from './LoginAndUserMenu';

function renderMenu(props) {
  return render(
    <LoginAndUserMenu routes={ROUTES} accountMenuLabel={ACCOUNT_MENU_LABEL} {...props} />
  );
}

describe('LoginAndUserMenu', () => {
  beforeEach(() => {
    mockHasUser = false;
    mockUserId = null;
    mockStateThrows = false;
    mockEffectiveTheme = 'light';
    mockVisibilityByRoute = {
      [ROUTES.userToken]: true,
      [ROUTES.userNugetApiToken]: true,
    };
    mockSignOut.mockClear();
    mockSetTheme.mockClear();
    mockStateServiceGo.mockClear();
    mockUrlServiceUrl.mockReset();
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    document.body.classList.remove('dark', 'light');
    window.location.hash = '';
  });

  describe('logged-out state', () => {
    it('renders an icon-only Log In button', () => {
      renderMenu();
      const button = screen.getByRole('button', {name: 'Log In'});
      expect(button).toHaveAttribute('aria-label', 'Log In');
      expect(screen.getByTestId('icon-user')).toBeInTheDocument();
    });

    it('does not render the account dropdown', () => {
      renderMenu();
      expect(screen.queryByRole('menu')).not.toBeInTheDocument();
    });

    it('navigates to login with a base64 returnTo when a current URL exists', () => {
      mockUrlServiceUrl.mockReturnValue('/browse/repositories');
      renderMenu();

      fireEvent.click(screen.getByRole('button', {name: 'Log In'}));

      expect(mockStateServiceGo).toHaveBeenCalledWith(ROUTES.login, {
        returnTo: expect.any(String),
      });
      const returnTo = mockStateServiceGo.mock.calls[0][1].returnTo;
      expect(atob(returnTo)).toBe('#/browse/repositories');
    });

    it('navigates to login without a returnTo when there is no current URL', () => {
      mockUrlServiceUrl.mockReturnValue('');
      renderMenu();

      fireEvent.click(screen.getByRole('button', {name: 'Log In'}));

      expect(mockStateServiceGo).toHaveBeenCalledWith(ROUTES.login);
    });
  });

  describe('logged-in state', () => {
    beforeEach(() => {
      mockHasUser = true;
      mockUserId = 'jbloggs';
    });

    it('renders the trigger with the reconciled cloud wording', () => {
      renderMenu();
      const button = screen.getByRole('button', {name: 'Account settings, tokens and keys'});
      expect(button).toHaveAttribute('data-testid', 'user-menu');
      expect(button).toHaveAttribute('data-analytics-id', 'nxrm-header-user-menu');
    });

    it('shows the user id in the menu header', () => {
      renderMenu();
      expect(screen.getByText('jbloggs')).toBeInTheDocument();
    });

    it('renders the Account item using the host-supplied label and href', () => {
      renderMenu();
      const accountItem = screen.getByRole('menuitem', {name: ACCOUNT_MENU_LABEL});
      expect(accountItem).toHaveAttribute('href', `#${ROUTES.userAccount}`);
    });

    it('renders Log Out', () => {
      renderMenu();
      expect(screen.getByRole('menuitem', {name: /log out/i})).toBeInTheDocument();
    });

    it('shows NuGet API Key and User Token when their routes are visible', () => {
      mockVisibilityByRoute = {[ROUTES.userToken]: true, [ROUTES.userNugetApiToken]: true};
      renderMenu();
      expect(screen.getByRole('menuitem', {name: /nuget api key/i})).toBeInTheDocument();
      expect(screen.getByRole('menuitem', {name: /user token/i})).toBeInTheDocument();
    });

    it('hides NuGet API Key when its route is not visible', () => {
      mockVisibilityByRoute = {[ROUTES.userToken]: true, [ROUTES.userNugetApiToken]: false};
      renderMenu();
      expect(screen.queryByRole('menuitem', {name: /nuget api key/i})).not.toBeInTheDocument();
    });

    it('hides User Token when its route is not visible', () => {
      mockVisibilityByRoute = {[ROUTES.userToken]: false, [ROUTES.userNugetApiToken]: true};
      renderMenu();
      expect(screen.queryByRole('menuitem', {name: /user token/i})).not.toBeInTheDocument();
    });

    it('does not crash when ExtJS.state() throws (e.g. mid-logout)', () => {
      mockStateThrows = true;
      expect(() => renderMenu()).not.toThrow();
    });
  });

  describe('sign out', () => {
    beforeEach(() => {
      mockHasUser = true;
      mockUserId = 'jbloggs';
      document.documentElement.setAttribute('data-theme', 'dark');
      document.body.classList.add('dark');
      localStorage.setItem('nexus-theme-preference', 'dark');
    });

    it('resets the DOM theme attribute, body classes and localStorage to light before signing out', () => {
      renderMenu();
      fireEvent.click(screen.getByRole('menuitem', {name: /log out/i}));

      expect(document.documentElement.getAttribute('data-theme')).toBe('light');
      expect(document.body.classList.contains('light')).toBe(true);
      expect(document.body.classList.contains('dark')).toBe(false);
      expect(localStorage.getItem('nexus-theme-preference')).toBe('light');
    });

    it('calls setTheme("light") and ExtJS.signOut()', () => {
      renderMenu();
      fireEvent.click(screen.getByRole('menuitem', {name: /log out/i}));

      expect(mockSetTheme).toHaveBeenCalledWith('light');
      expect(mockSignOut).toHaveBeenCalledTimes(1);
    });
  });

  describe('isPreviewUI hash derivation', () => {
    beforeEach(() => {
      mockHasUser = true;
      mockUserId = 'jbloggs';
    });

    it('links to the Classic/Default route when the hash is not under #preview', () => {
      window.location.hash = '#browse/welcome';
      renderMenu();
      expect(screen.getByRole('menuitem', {name: ACCOUNT_MENU_LABEL})).toHaveAttribute(
        'href',
        `#${ROUTES.userAccount}`
      );
    });

    it('links to the Preview route when the hash is under #preview', () => {
      window.location.hash = '#preview/browse/welcome';
      renderMenu();
      expect(screen.getByRole('menuitem', {name: ACCOUNT_MENU_LABEL})).toHaveAttribute(
        'href',
        `#${ROUTES.previewUserAccount}`
      );
    });

    it('links to the exact Preview root (no trailing segment) as a Preview route too', () => {
      window.location.hash = '#preview';
      renderMenu();
      expect(screen.getByRole('menuitem', {name: ACCOUNT_MENU_LABEL})).toHaveAttribute(
        'href',
        `#${ROUTES.previewUserAccount}`
      );
    });

    it('re-derives on hashchange without remounting', () => {
      window.location.hash = '#browse/welcome';
      renderMenu();
      expect(screen.getByRole('menuitem', {name: ACCOUNT_MENU_LABEL})).toHaveAttribute(
        'href',
        `#${ROUTES.userAccount}`
      );

      act(() => {
        window.location.hash = '#preview/browse/welcome';
        window.dispatchEvent(new Event('hashchange'));
      });

      expect(screen.getByRole('menuitem', {name: ACCOUNT_MENU_LABEL})).toHaveAttribute(
        'href',
        `#${ROUTES.previewUserAccount}`
      );
    });
  });

  describe('folded header controls (NEXUS-55145)', () => {
    beforeEach(() => {
      mockHasUser = true;
      mockUserId = 'jbloggs';
    });

    it('shouldNotRenderThemeItemByDefault', () => {
      renderMenu();
      expect(screen.queryByRole('menuitem', {name: /mode/i})).not.toBeInTheDocument();
    });

    it('shouldNotRenderUiSwitchItemByDefault', () => {
      renderMenu();
      expect(
        screen.queryByRole('menuitem', {name: /switch to classic ui/i})
      ).not.toBeInTheDocument();
    });

    it('shouldRenderDarkModeItemWhenThemeItemShownInLightMode', () => {
      mockEffectiveTheme = 'light';
      renderMenu({showThemeItem: true});
      expect(screen.getByRole('menuitem', {name: /dark mode/i})).toBeInTheDocument();
    });

    it('shouldRenderLightModeItemWhenThemeItemShownInDarkMode', () => {
      mockEffectiveTheme = 'dark';
      renderMenu({showThemeItem: true});
      expect(screen.getByRole('menuitem', {name: /light mode/i})).toBeInTheDocument();
    });

    it('shouldToggleToDarkFromLightWhenThemeItemClicked', () => {
      mockEffectiveTheme = 'light';
      renderMenu({showThemeItem: true});
      fireEvent.click(screen.getByRole('menuitem', {name: /dark mode/i}));
      expect(mockSetTheme).toHaveBeenCalledWith('dark');
    });

    it('shouldToggleToLightFromDarkWhenThemeItemClicked', () => {
      mockEffectiveTheme = 'dark';
      renderMenu({showThemeItem: true});
      fireEvent.click(screen.getByRole('menuitem', {name: /light mode/i}));
      expect(mockSetTheme).toHaveBeenCalledWith('light');
    });

    it('shouldRenderUiSwitchItemWhenShown', () => {
      renderMenu({showUiSwitchItem: true, onUiSwitch: jest.fn()});
      expect(
        screen.getByRole('menuitem', {name: /switch to classic ui/i})
      ).toBeInTheDocument();
    });

    it('shouldInvokeOnUiSwitchWhenUiSwitchItemClicked', () => {
      const onUiSwitch = jest.fn();
      renderMenu({showUiSwitchItem: true, onUiSwitch});
      fireEvent.click(screen.getByRole('menuitem', {name: /switch to classic ui/i}));
      expect(onUiSwitch).toHaveBeenCalledTimes(1);
    });

    it('shouldStillRenderAccountAndLogOutWhenControlsFolded', () => {
      renderMenu({showThemeItem: true, showUiSwitchItem: true, onUiSwitch: jest.fn()});
      expect(screen.getByRole('menuitem', {name: ACCOUNT_MENU_LABEL})).toBeInTheDocument();
      expect(screen.getByRole('menuitem', {name: /log out/i})).toBeInTheDocument();
    });

    it('shouldNotRenderNexusOneSwitchItemByDefault', () => {
      renderMenu();
      expect(screen.queryByRole('menuitem', {name: /switch to nexus one ui/i})).not.toBeInTheDocument();
    });

    it('shouldRenderNexusOneSwitchItemWhenShown', () => {
      renderMenu({showNexusOneSwitchItem: true, onNexusOneSwitch: jest.fn()});
      expect(screen.getByRole('menuitem', {name: /switch to nexus one ui/i})).toBeInTheDocument();
    });

    it('shouldInvokeOnNexusOneSwitchWhenItemClicked', () => {
      const onNexusOneSwitch = jest.fn();
      renderMenu({showNexusOneSwitchItem: true, onNexusOneSwitch});
      fireEvent.click(screen.getByRole('menuitem', {name: /switch to nexus one ui/i}));
      expect(onNexusOneSwitch).toHaveBeenCalledTimes(1);
    });
  });
});
