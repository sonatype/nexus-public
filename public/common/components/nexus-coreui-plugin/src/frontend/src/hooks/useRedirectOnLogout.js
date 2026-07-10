/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import { useCurrentStateAndParams, useRouter } from "@uirouter/react";
import { useEffect, useState } from "react";
import { ExtJS, isVisible } from '@sonatype/nexus-ui-plugin';
import { isExtJSLoaded, onExtJSLoad } from '../utils/extJsLoader';

/**
 * Checks if a state or any of its ancestors requires authentication/permissions.
 * Unlike isVisible() from NavigationUtils, this does NOT use the NXSESSIONID cookie
 * fallback — it simply detects whether the route has permission-type requirements.
 * This is appropriate for the logout hook because we already know from ExtJS.useUser()
 * that the user is no longer authenticated.
 *
 * Note: edition-only constraints (e.g., PRO-only routes) are intentionally not checked
 * here because they don't require authentication — a route locked to PRO edition is
 * still accessible anonymously if the instance is PRO.
 */
function hasPermissionRequirements(state) {
  let current = state;
  while (current && current.name) {
    const reqs = current.data?.visibilityRequirements;
    if (reqs) {
      const { permissions, requiresPermission, requiresAnyPermission,
              permissionPrefix, permissionPrefixes, requiresUser } = reqs;
      if (permissions || requiresPermission || requiresAnyPermission ||
          permissionPrefix || permissionPrefixes || requiresUser) {
        return true;
      }
    }
    current = current.parent;
  }
  return false;
}

const PERMISSIONS_UPDATE_DELAY_MS = 100;

function clearUnsavedChanges() {
  // React-side unsaved changes guard relies on a global window.dirty array.
  // When a logout happens we always want to discard that state so it can't block navigation.
  if (typeof window !== 'undefined') {
    window.dirty = [];
  }
}

/**
 * Custom hook that handles redirecting the user to the welcome page
 * if the current route becomes inaccessible after logout (i.e., it is no longer visible).
 *
 * It listens for permission changes via the ExtJS PermissionsController,
 * and checks whether the current route still satisfies its visibility requirements
 * once the user is no longer authenticated.
 *
 * A short delay is introduced to ensure that the permissions and visibility state
 * have been fully updated before performing the redirect. This helps prevent
 * premature navigation, which was observed outside of debug mode.
 */
export function useRedirectOnLogout() {
  const router = useRouter();
  // Always call the hook; ExtJS.useUser is now guarded internally.
  const userIsAuthenticated = !!ExtJS.useUser();
  const { state } = useCurrentStateAndParams();
  const [lastAuthenticationChange, setLastAuthenticationChange] = useState(null);

  useEffect(() => {
    let unmounted = false;
    let registeredController = null;
    let registeredHandler = null;

    // Only set up ExtJS listeners after ExtJS is loaded
    if (!isExtJSLoaded()) {
      // Wait for ExtJS to load, then set up listeners
      onExtJSLoad(() => {
        if (unmounted || !window.Ext?.getApplication) return;
        const permissionsController = window.Ext.getApplication().getController("Permissions");
        registeredHandler = () => setLastAuthenticationChange(new Date());
        registeredController = permissionsController;
        permissionsController.on("changed", registeredHandler);
      });
      return () => {
        unmounted = true;
        if (registeredController && registeredHandler) {
          registeredController.un("changed", registeredHandler);
        }
      };
    }

    const permissionsController = window.Ext.getApplication().getController("Permissions");
    const handleChange = () => setLastAuthenticationChange(new Date());
    registeredController = permissionsController;
    registeredHandler = handleChange;
    permissionsController.on("changed", handleChange);

    return () => {
      unmounted = true;
      if (registeredController && registeredHandler) {
        registeredController.un("changed", registeredHandler);
      }
    };
  }, []);

  useEffect(() => {
    const timer = setTimeout(() => {
      // Read router.globals.$current inside the timer so we evaluate the actual state
      // at redirect time, not when the effect was scheduled. This avoids a stale closure
      // if the user navigates away within the PERMISSIONS_UPDATE_DELAY_MS window.
      // We use $current (a StateObject) because it has the resolved .parent chain needed
      // by hasPermissionRequirements(); useCurrentStateAndParams returns a StateDeclaration
      // which lacks .parent.
      const currentState = router.globals.$current;
      const shouldRedirect = !userIsAuthenticated
          && hasPermissionRequirements(currentState)
          && !isVisible(currentState?.data?.visibilityRequirements);
      if (shouldRedirect) {
        console.debug("Redirecting to login page with return URL. Not enough permissions");
        clearUnsavedChanges();

        const url = router.urlService.url();
        if (url) {
          const returnTo = btoa(`#${url}`);
          router.stateService.go('login', { returnTo });
        } else {
          router.stateService.go('login');
        }
      }
    }, PERMISSIONS_UPDATE_DELAY_MS);

    return () => clearTimeout(timer);
  }, [lastAuthenticationChange, userIsAuthenticated, state, router]);
}

