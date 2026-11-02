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

import React, {useState, useEffect} from 'react';
import {DropdownMenu, Flex, IconButton, Text} from '@radix-ui/themes';
import {User, Key, LogOut, Sun, MoonStar, Repeat} from 'lucide-react';
import {useRouter, useSref} from '@uirouter/react';
import ExtJS from '../../../../interface/ExtJS';
import useIsVisible from '../../../../router/useIsVisible';
import {useTheme, THEMES} from '../../../../contexts/ThemeContext';
import {Tooltip, usePortalContainer} from '../Tooltip';
import {useHasUser} from '../hooks';

import './LoginAndUserMenu.scss';

/**
 * Shared login/user-menu button for both self-hosted and cloud Preview UI headers
 * (reconciled from self-hosted's LoginAndUserButtonRadix and cloud's
 * LogInAndUserProfileMenu - NEXUS-54869).
 *
 * `routes` and `accountMenuLabel` are host-supplied: each host owns its own router
 * state tree and its own UIStrings, so route names and the "Account" menu-item label
 * are passed in rather than imported (same pattern as SearchRadix's `routes` prop).
 *
 * Icon choice: a single <User> icon is used for both the logged-in and logged-out
 * states (self-hosted's prior choice) rather than swapping to <UserCircle> once
 * authenticated (cloud's prior behavior) - consistency between states avoids an
 * icon "jump" on login/logout and matches every other icon-only header button
 * (HelpMenu, ThemeSwitcher, Refresh), none of which change icon based on state.
 *
 * @param {Object} props
 * @param {Object} props.routes - Host-specific router state names (login, account,
 *   token and NuGet-key routes, each with a Preview variant).
 * @param {string} props.accountMenuLabel - Label for the "Account" menu item (host's UIStrings.USER_ACCOUNT.MENU.text).
 * @param {boolean} [props.showThemeItem] - show a Dark/Light Mode item. Set by
 *   GlobalHeader at narrow widths where the header's own theme toggle is hidden.
 * @param {boolean} [props.showUiSwitchItem] - show a "Switch to Classic UI" item. Set
 *   by GlobalHeader at narrow widths where the header's own switch button is hidden.
 * @param {Function} [props.onUiSwitch] - invoked when the folded "Switch to Classic UI"
 *   item is selected. GlobalHeader routes this through the same feedback prompt and
 *   switch logic as the header button it replaces.
 * @param {boolean} [props.showNexusOneSwitchItem] - show a "Switch to Nexus One UI" item.
 *   Set by GlobalHeader on Classic routes at narrow widths where the header button is hidden.
 * @param {Function} [props.onNexusOneSwitch] - invoked when the folded "Switch to Nexus One
 *   UI" item is selected.
 */
export default function LoginAndUserMenu({
  routes,
  accountMenuLabel,
  showThemeItem = false,
  showUiSwitchItem = false,
  onUiSwitch,
  showNexusOneSwitchItem = false,
  onNexusOneSwitch,
}) {
  const portalContainer = usePortalContainer();
  const hasUser = useHasUser();
  const router = useRouter();
  const {effectiveTheme, setTheme} = useTheme();

  const isDarkMode = effectiveTheme === 'dark';
  const toggleTheme = () => {
    setTheme(isDarkMode ? THEMES.LIGHT : THEMES.DARK);
  };

  const [userId, setUserId] = useState(null);

  useEffect(() => {
    try {
      const state = ExtJS.state();
      if (state) {
        setUserId(state.getValue('user')?.id || null);
      }
    } catch (e) {
      setUserId(null);
    }
  }, []);

  const userTokenRouteState = router.stateRegistry.get(routes.userToken);
  const isUserTokenEnabled = useIsVisible(userTokenRouteState?.data?.visibilityRequirements);
  const userNugetApiTokenState = router.stateRegistry.get(routes.userNugetApiToken);
  const isUserNugetApiTokenEnabled = useIsVisible(userNugetApiTokenState?.data?.visibilityRequirements);

  const [hashPath, setHashPath] = useState(() => {
    if (typeof window === 'undefined') {
      return '';
    }
    return window.location.hash.replace(/^#/, '').split('?')[0] || '';
  });
  useEffect(() => {
    const syncFromLocation = () => {
      setHashPath(window.location.hash.replace(/^#/, '').split('?')[0] || '');
    };
    syncFromLocation();
    window.addEventListener('hashchange', syncFromLocation);
    window.addEventListener('popstate', syncFromLocation);
    return () => {
      window.removeEventListener('hashchange', syncFromLocation);
      window.removeEventListener('popstate', syncFromLocation);
    };
  }, []);
  const isPreviewUI = hashPath === 'preview' || hashPath.startsWith('preview/');

  const userAccountState = isPreviewUI ? routes.previewUserAccount : routes.userAccount;
  const nugetApiTokenState = isPreviewUI ? routes.previewUserNugetApiToken : routes.userNugetApiToken;
  const userTokenState = isPreviewUI ? routes.previewUserToken : routes.userToken;
  const {href: userAccountHref} = useSref(userAccountState);
  const {href: userNugetApiTokenHref} = useSref(nugetApiTokenState);
  const {href: userTokenHref} = useSref(userTokenState);

  const onSignInClick = () => {
    const url = router.urlService.url();
    if (url) {
      const returnTo = btoa(`#${url}`);
      router.stateService.go(routes.login, {returnTo});
    } else {
      router.stateService.go(routes.login);
    }
  };

  function onSignOutClick() {
    // Reset to light mode on logout - Classic/Default UI doesn't support dark mode.
    // Apply synchronously to DOM and localStorage BEFORE redirect so we never
    // flash dark theme on login/Classic UI pages.
    if (typeof document !== 'undefined') {
      document.documentElement.setAttribute('data-theme', 'light');
      document.body.classList.remove('dark');
      document.body.classList.add('light');
    }
    try {
      localStorage.setItem('nexus-theme-preference', 'light');
    } catch (e) {
      /* ignore */
    }
    setTheme('light');

    ExtJS.signOut();
  }

  if (!hasUser) {
    return (
      <Tooltip content="Log In">
        <IconButton
          variant="outline"
          size="2"
          color="gray"
          aria-label="Log In"
          onClick={onSignInClick}
        >
          <User size={16} />
        </IconButton>
      </Tooltip>
    );
  }

  return (
    <DropdownMenu.Root>
      <Tooltip content="Account settings, tokens and keys">
        <DropdownMenu.Trigger>
          <IconButton
            variant="outline"
            size="2"
            color="gray"
            aria-label="Account settings, tokens and keys"
            data-testid="user-menu"
            data-analytics-id="nxrm-header-user-menu"
          >
            <User size={16} />
          </IconButton>
        </DropdownMenu.Trigger>
      </Tooltip>

      <DropdownMenu.Content align="end" container={portalContainer} color="gray" variant="soft">
        <DropdownMenu.Label>
          <Text size="2" as="span" weight="bold" className="nxrm-login-and-user-menu__user-id">
            {userId}
          </Text>
        </DropdownMenu.Label>

        {/* The theme toggle and UI switch fold in here at narrow widths, below the
            user name, when the header's own buttons are hidden. */}
        {showThemeItem && (
          <DropdownMenu.Item
            onSelect={toggleTheme}
            data-testid="user-menu-theme-toggle"
            data-analytics-id="nxrm-header-user-menu-toggle-theme"
          >
            <Flex align="center" justify="start" gap="3">
              {isDarkMode ? <Sun size={16} /> : <MoonStar size={16} />}
              <Text size="2">{isDarkMode ? 'Light Mode' : 'Dark Mode'}</Text>
            </Flex>
          </DropdownMenu.Item>
        )}

        {showUiSwitchItem && (
          <DropdownMenu.Item
            onSelect={onUiSwitch}
            data-testid="user-menu-ui-switch"
            data-analytics-id="nxrm-header-user-menu-switch-classic"
          >
            <Flex align="center" justify="start" gap="3">
              <Repeat size={16} />
              <Text size="2">Switch to Classic UI</Text>
            </Flex>
          </DropdownMenu.Item>
        )}

        {showNexusOneSwitchItem && (
          <DropdownMenu.Item
            onSelect={onNexusOneSwitch}
            data-testid="user-menu-nexus-one-switch"
            data-analytics-id="nxrm-header-user-menu-switch-preview"
          >
            <Flex align="center" justify="start" gap="3">
              <Repeat size={16} />
              <Text size="2">Switch to Nexus One UI</Text>
            </Flex>
          </DropdownMenu.Item>
        )}

        <DropdownMenu.Separator />

        <DropdownMenu.Item asChild>
          <a href={userAccountHref} className="nxrm-internal-link">
            <Text size="2" color="gray">{accountMenuLabel}</Text>
          </a>
        </DropdownMenu.Item>

        {isUserNugetApiTokenEnabled && (
          <DropdownMenu.Item asChild>
            <a href={userNugetApiTokenHref} className="nxrm-internal-link">
              <Text size="2" color="gray">NuGet API Key</Text>
            </a>
          </DropdownMenu.Item>
        )}

        {isUserTokenEnabled && (
          <DropdownMenu.Item asChild>
            <a href={userTokenHref} className="nxrm-internal-link">
              <Flex align="center" justify="start" gap="3">
                <Text size="2">User Token</Text>
                <Key size={16} />
              </Flex>
            </a>
          </DropdownMenu.Item>
        )}

        <DropdownMenu.Separator />

        <DropdownMenu.Item onClick={onSignOutClick} data-analytics-id="nxrm-header-sign-out">
          <Flex align="center" justify="start" gap="3">
            <LogOut size={16} />
            <Text size="2">Log Out</Text>
          </Flex>
        </DropdownMenu.Item>
      </DropdownMenu.Content>
    </DropdownMenu.Root>
  );
}
