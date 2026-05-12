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
import {Tooltip, usePortalContainer} from '../shared';
import {User, Key, LogOut} from 'lucide-react';
import {ExtJS, useIsVisible} from '@sonatype/nexus-ui-plugin';
import {useRouter, useSref} from '@uirouter/react';
import useHasUser from '../../hooks/useHasUser';
import {ROUTE_NAMES} from '../../routerConfig/routeNames/routeNames';
import UIStrings from '../../constants/UIStrings';
import {useTheme} from '../../contexts/ThemeContext';
import {restClient} from '../../utils/api';

/**
 * Single component with all hooks at top - no conditional component switching.
 * Fixes "Rendered fewer hooks" when hasUser changes during logout.
 */
export default function LoginAndUserButtonRadix() {
  const portalContainer = usePortalContainer();
  const hasUser = useHasUser();
  const router = useRouter();
  const {USER} = ROUTE_NAMES;
  const {setTheme} = useTheme();

  const userTokenRouteName = USER.USER_TOKEN;
  const userNugetApiTokenName = USER.NUGETAPITOKEN;
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

  const userTokenRouteState = router.stateRegistry.get(userTokenRouteName);
  const isUserTokenEnabled = useIsVisible(userTokenRouteState?.data?.visibilityRequirements);
  const userNugetApiTokenState = router.stateRegistry.get(userNugetApiTokenName);
  const isUserNugetApiTokenEnabled = useIsVisible(userNugetApiTokenState?.data?.visibilityRequirements);

  // Match GlobalHeaderRadix: Preview vs Classic is determined by the URL hash, not only
  // whether preview states are registered (fixes user menu linking to #user/... in Preview UI).
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

  const userAccountState = isPreviewUI ? 'preview.user.account' : USER.ACCOUNT;
  const nugetApiTokenState = isPreviewUI ? 'preview.user.nugetapitoken' : USER.NUGETAPITOKEN;
  const userTokenState = isPreviewUI ? 'preview.user.usertoken' : userTokenRouteName;
  const {href: userAccountHref} = useSref(userAccountState);
  const {href: userNugetApiTokenHref} = useSref(nugetApiTokenState);
  const {href: userTokenHref} = useSref(userTokenState);

  const onSignInClick = () => {
    const url = router.urlService.url();
    const returnTo = btoa(`#${url}`);
    router.stateService.go(ROUTE_NAMES.LOGIN, {returnTo});
  };

  function onSignOutClick() {
    // Reset to light mode on logout - Classic UI doesn't support dark mode.
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
      <Tooltip content="My Account">
        <DropdownMenu.Trigger>
          <IconButton variant="outline" size="2" color="gray" aria-label="My Account" data-testid="user-menu">
            <User size={16} />
          </IconButton>
        </DropdownMenu.Trigger>
      </Tooltip>

      <DropdownMenu.Content align="end" container={portalContainer} color="gray" variant="soft">
        <DropdownMenu.Label>
          <Text size="2" as="span" weight="bold" style={{color: 'var(--gray-12)'}}>
            {userId}
          </Text>
        </DropdownMenu.Label>
        <DropdownMenu.Separator />

        <DropdownMenu.Item asChild>
          <a href={userAccountHref} className="nxrm-internal-link">
            <Text size="2" color="gray">{UIStrings.USER_ACCOUNT.MENU.text}</Text>
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

        <DropdownMenu.Item onClick={onSignOutClick}>
          <Flex align="center" justify="start" gap="3">
            <LogOut size={16} />
            <Text size="2">Log Out</Text>
          </Flex>
        </DropdownMenu.Item>
      </DropdownMenu.Content>
    </DropdownMenu.Root>
  );
}

