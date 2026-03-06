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
  const userIsAuthenticated = !!ExtJS.useUser();
  const { state } = useCurrentStateAndParams();
  const visibilityRequirements = state.data?.visibilityRequirements;
  const [lastAuthenticationChange, setLastAuthenticationChange] = useState(null);

  useEffect(() => {
    const permissionsController = Ext.getApplication().getController("Permissions");
    const handleChange = () => setLastAuthenticationChange(new Date());
    permissionsController.on("changed", handleChange);

    return () => {
      permissionsController.un("changed", handleChange);
    };
  }, []);

  useEffect(() => {
    const shouldRedirect = !isVisible(visibilityRequirements) && !userIsAuthenticated;
    const timer = setTimeout(() => {
      if (shouldRedirect) {
        console.debug("Redirecting to welcome page. Not enough permissions");
        clearUnsavedChanges();
        router.stateService.go("browse.welcome");
      }
    }, PERMISSIONS_UPDATE_DELAY_MS);

    return () => clearTimeout(timer);
  }, [lastAuthenticationChange, userIsAuthenticated, visibilityRequirements]);
}

