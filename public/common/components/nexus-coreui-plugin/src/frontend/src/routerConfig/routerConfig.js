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
import {createRouter, isSonatypeDevMode, MissingRoutePage} from '@sonatype/nexus-ui-plugin';
import {ROUTE_NAMES} from './routeNames/routeNames';
import {browseRoutes} from './routes/browseRoutes';
import {adminRoutes} from './routes/adminRoutes';
import {userRoutes} from './routes/userRoutes';
import {loginRoutes} from "./routes/loginRoutes";
import {previewAdminRoutes, sonatypeInternalTestRoutes} from './routes/previewAdminRoutes';
import {previewBrowseRoutes} from './routes/previewBrowseRoutes';
import {previewUserRoutes} from './routes/previewUserRoutes';
import {getInitialRoute} from './getInitialRoute';

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
    ...(isSonatypeDevMode() ? sonatypeInternalTestRoutes : []),
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
