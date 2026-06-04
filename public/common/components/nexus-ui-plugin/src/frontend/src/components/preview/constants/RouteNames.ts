/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the Eclipse Foundation.
 * All other trademarks are the property of their respective owners.
 */

/**
 * Preview ROUTE_NAMES dictionary.
 *
 * Layers preview-specific route names over the base nexus-ui-plugin RouteNames.
 * Preview components should import from here (directly, or as PreviewRouteNames
 * from the @sonatype/nexus-ui-plugin package barrel) so they remain portable
 * across plugins.
 *
 * Preview-only entries live under PREVIEW_* keys so they don't collide with
 * classic entries of the same feature name in each plugin's browseRouteNames.js.
 * Example: ROUTE_NAMES.BROWSE.PREVIEW_MALICIOUS_PACKAGES.ROOT resolves to the
 * 'preview.browse.malicious-packages' state, while the classic
 * ROUTE_NAMES.BROWSE.MALICIOUS_PACKAGES.ROOT (defined in the plugin) resolves
 * to 'browse.malicious-packages'.
 *
 * Each plugin's routerConfig/routeNames/routeNames.js merges these on top of
 * its own plugin-specific admin/browse/user route names. Plugin-specific
 * classic route names MUST NOT be added here — they belong in the plugin's
 * own browseRouteNames/adminRouteNames/userRouteNames.
 */
import { mergeDeepRight } from 'ramda';
import { RouteNames as BaseRouteNames } from '../../../constants/RouteNames';

export const ROUTE_NAMES = mergeDeepRight(BaseRouteNames, {
  BROWSE: {
    WELCOME: {
      ROOT: 'browse.welcome',
      TITLE: 'Dashboard',
    },
    PREVIEW_MALICIOUS_PACKAGES: {
      ROOT: 'preview.browse.malicious-packages',
      TITLE: 'Malicious Packages',
    },
    PREVIEW_PROTECT: {
      ROOT: 'preview.browse.protect',
      TITLE: 'Protect',
    },
    PREVIEW_MALWARERISK: {
      ROOT: 'preview.browse.malwarerisk',
      TITLE: 'Malware Risk',
    },
  },
});
