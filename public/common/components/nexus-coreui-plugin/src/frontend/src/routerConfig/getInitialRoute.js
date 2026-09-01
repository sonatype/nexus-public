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
import {ExtJS, resolveDefaultLandingRoute, USER_REQUESTED_LEGACY_KEY} from '@sonatype/nexus-ui-plugin';
import {ROUTE_NAMES} from './routeNames/routeNames';

/**
 * Determines the initial route based on anonymous access and Preview UI settings.
 *
 * bootstrapFromREST() seeds window.NX.State before this runs, so
 * ExtJS.state().getValue() returns real data. The try/catch is a safety net in
 * case REST bootstrap failed.
 *
 * When anonymous access is enabled, honors the "Default to Nexus One UI" setting
 * (gated by anonymous Preview access and the user_requested_legacy session flag)
 * so anonymous users land directly on the Preview dashboard instead of flashing
 * Classic first.
 */
export function getInitialRoute() {
  try {
    const state = ExtJS.state();
    if (state && typeof state.getValue === 'function') {
      const anonUser = state.getValue('anonymousUsername');
      // Only use ExtJS state if it returned a real value (not the fallback null)
      if (anonUser !== null && anonUser !== undefined) {
        if (!anonUser) {
          return ROUTE_NAMES.LOGIN;
        }
        return resolveDefaultLandingRoute({
          defaultToPreviewUi: state.getValue('defaultToPreviewUi', false) ?? false,
          canAccessPreviewUi: state.getValue('anonymousEnabled', false) ?? false,
          userRequestedLegacy: sessionStorage.getItem(USER_REQUESTED_LEGACY_KEY) === 'true'
        });
      }
    }
  }
  catch {
    // ExtJS not ready
  }
  return ROUTE_NAMES.BROWSE.WELCOME.ROOT;
}
