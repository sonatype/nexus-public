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

import {UIView} from '@uirouter/react';
import {
  PRO_FAMILY_EDITIONS,
  UserAccountPage,
  UserTokenPage,
  NuGetApiTokenPage,
} from '@sonatype/nexus-ui-plugin';

export const previewUserRoutes = [
  {
    name: 'preview.user',
    url: '/user',
    abstract: true,
    component: UIView,
  },
  {
    name: 'preview.user.account',
    url: '/account',
    component: UserAccountPage,
    data: {title: 'Account'},
  },
  {
    name: 'preview.user.nugetapitoken',
    url: '/nugetapitoken',
    component: NuGetApiTokenPage,
    data: {title: 'NuGet API Token'},
  },
  {
    name: 'preview.user.usertoken',
    url: '/usertoken',
    component: UserTokenPage,
    // Mirrors the Classic route's gate in userRoutes.js. Without these the route is visible to
    // everyone, because isVisible() treats absent requirements as "no restrictions" and the page
    // then renders the backend's authorization failure instead of redirecting (NEXUS-55523).
    data: {
      visibilityRequirements: {
        bundle: 'nexus-usertoken-plugin',
        statesEnabled: [
          {
            key: 'userTokenRealmEnabled',
            defaultValue: false,
          },
        ],
        permissions: ['nexus:usertoken-current:read'],
        editions: PRO_FAMILY_EDITIONS,
      },
      title: 'User Token',
    },
  },
];
