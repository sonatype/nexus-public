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
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import {createRouter} from '@sonatype/nexus-ui-plugin';
import {ROUTE_NAMES} from './routeNames/routeNames';
import {browseRoutes} from './routes/browseRoutes';
import {adminRoutes} from './routes/adminRoutes';
import {userRoutes} from './routes/userRoutes';
import {loginRoutes} from "./routes/loginRoutes";
import {previewAdminRoutes, sonatypeInternalTestRoutes} from './routes/previewAdminRoutes';
import {previewBrowseRoutes} from './routes/previewBrowseRoutes';
import {previewUserRoutes} from './routes/previewUserRoutes';
import {MissingRoutePage} from '../components/super/pages/MissingRoutePage/MissingRoutePage';

export function getRouter() {
  const initialRoute = getInitialRoute();

  const menuRoutes = [
    // DEFAULT UI (current - unchanged)
    ...browseRoutes,
    ...adminRoutes,
    ...userRoutes,
    ...loginRoutes,

    // PREVIEW UI (new - Phase 4.0)
    ...previewBrowseRoutes,
    ...previewAdminRoutes,
    ...previewUserRoutes,

    // SONATYPE INTERNAL TEST PAGES — standalone (no Settings sidebar) at preview.test* level
    // Gate: SONATYPE_INTERNAL build flag OR localStorage flag
    ...((typeof __SONATYPE_INTERNAL__ !== 'undefined' && __SONATYPE_INTERNAL__)
      || (typeof localStorage !== 'undefined' && localStorage.getItem('SONATYPE_INTERNAL') === 'true')
      ? sonatypeInternalTestRoutes
      : []),
  ];

  const missingRoute = {
    name: ROUTE_NAMES.MISSING_ROUTE,
    url: '404',
    component: MissingRoutePage,
    data: {
      visibilityRequirements: {}
    }
  };

  const router = createRouter({initialRoute, menuRoutes, missingRoute});
  
  return router;
}

/**
 * Determines the initial route based on anonymous access settings.
 *
 * Phase 2: bootstrapFromREST() seeds window.NX.State before this runs,
 * so ExtJS.state().getValue() returns real data from REST.
 * The try/catch is a safety net in case REST bootstrap failed.
 */
function getInitialRoute() {
  try {
    const state = ExtJS.state();
    if (state && typeof state.getValue === 'function') {
      const anonUser = state.getValue('anonymousUsername');
      // Only use ExtJS state if it returned a real value (not the fallback null)
      if (anonUser !== null && anonUser !== undefined) {
        return anonUser ? ROUTE_NAMES.BROWSE.WELCOME.ROOT : ROUTE_NAMES.LOGIN;
      }
    }
  } catch {
    // ExtJS not ready
  }
  return ROUTE_NAMES.BROWSE.WELCOME.ROOT;
}
