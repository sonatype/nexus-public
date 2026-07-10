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
import {
  NxButton,
  NxFontAwesomeIcon,
  NxStatefulNavigationDropdown,
  NxNavigationDropdown,
  NxTextLink,
  NxH4,
  useToggle,
  NxDropdown
} from '@sonatype/react-shared-components';
import { ExtJS, useIsVisible } from '@sonatype/nexus-ui-plugin';
import { faUser } from '@fortawesome/free-solid-svg-icons';
import { useRouter, useSref } from '@uirouter/react';
import useHasUser from '../../hooks/useHasUser';
import { ROUTE_NAMES } from '../../routerConfig/routeNames/routeNames';
import UIStrings from '../../constants/UIStrings';

export default function LoginAndUserButton() {
  const signInTitle = "Log In"
  const hasUser = useHasUser();
  const router = useRouter();

  if (hasUser) {
    return <ProfileMenu />
  } else {
    return <NxButton
        title={signInTitle}
        aria-label={signInTitle}
        variant="icon-only" onClick={onSignInClick}
        data-analytics-id="nxrm-global-header-login"
    >
      <NxFontAwesomeIcon icon={faUser} />
    </NxButton>
  }

  function onSignInClick() {
    // Keep original requested URL and then encode to Base64
    const url = router.urlService.url();
    if (url) {
      const returnTo = btoa(`#${url}`);
      router.stateService.go(ROUTE_NAMES.LOGIN, { returnTo });
    } else {
      router.stateService.go(ROUTE_NAMES.LOGIN);
    }
  }
}

function ProfileMenu() {
  const { USER } = ROUTE_NAMES;

  const userTokenRouteName = USER.USER_TOKEN;
  const userNugetApiTokenName = USER.NUGETAPITOKEN;

  const userId = ExtJS.useState(() => ExtJS.state().getValue('user')?.id);

  const [isOpen, onToggleCollapse] = useToggle(false);

  const router = useRouter();

  const userTokenRouteState = router.stateRegistry.get(userTokenRouteName);
  const isUserTokenEnabled = useIsVisible(userTokenRouteState.data.visibilityRequirements);

  const userNugetApiTokenState = router.stateRegistry.get(userNugetApiTokenName);
  const isUserNugetApiTokenEnabled = useIsVisible(userNugetApiTokenState.data.visibilityRequirements);

  const { href: userAccountHref } = useSref(USER.ACCOUNT);
  const { href: userNugetApiTokenHref } = useSref(USER.NUGETAPITOKEN);
  const { href: userTokenHref } = useSref(userTokenRouteName);

  return (
      <NxStatefulNavigationDropdown
          isOpen={isOpen}
          onToggleCollapse={onToggleCollapse}
          icon={faUser}
          title="Account settings, tokens and keys"
          data-analytics-id="nxrm-global-header-profile-menu"
      >
        <NxNavigationDropdown.MenuHeader>
          <NxH4>
            <NxFontAwesomeIcon className="nxrm-global-header-profile-menu-user-icon" icon={faUser}/>
            {userId}
          </NxH4>
        </NxNavigationDropdown.MenuHeader>

        <NxTextLink
            href={userAccountHref}
            className="nx-dropdown-link"
            data-analytics-id="nxrm-global-header-profile-menu-my-profile"
        >
          {UIStrings.USER_ACCOUNT.MENU.text}
        </NxTextLink>

        {isUserNugetApiTokenEnabled && (
        <NxTextLink
          href={userNugetApiTokenHref}
          className='nx-dropdown-link'
          data-analytics-id='nxrm-global-header-profile-menu-nuget-api-key'
        >
          NuGet API Key
        </NxTextLink>
      )}

        { isUserTokenEnabled
            ? (<NxTextLink
                href={userTokenHref}
                className="nx-dropdown-link"
                data-analytics-id="nxrm-global-header-profile-menu-user-token"
            >
              User Token
            </NxTextLink>)
            : null
        }

        <NxDropdown.Divider />

        <button onClick={onSingOutClick} className="nx-dropdown-button">
          Log Out
        </button>
      </NxStatefulNavigationDropdown>);

  function onSingOutClick() {
    // Reset to light mode on logout since Default UI doesn't support dark mode
    // This ensures consistent state when user logs back in
    try {
      localStorage.setItem('nexus-theme-preference', 'light');
      document.documentElement.setAttribute('data-theme', 'light');
    } catch (e) {
      // Ignore localStorage errors
    }
    ExtJS.signOut();
  }
}
