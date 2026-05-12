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

import { isVisible } from '@sonatype/nexus-ui-plugin';
import useFilteredRoutes from "../../hooks/useFilteredRoutes";
import { ROUTE_NAMES } from "../../routerConfig/routeNames/routeNames";

/**
 * Hook to check if any route in a section is visible.
 * Uses useFilteredRoutes which internally subscribes to permission change events
 * via ExtJS.useVisiblityWithChanges.
 */
function useIsSectionVisible(section) {
  // useFilteredRoutes internally uses ExtJS.useVisiblityWithChanges, which subscribes to
  // Permissions#changed, State#changed, and State#userchanged events.
  // The isVisible check inside the filter is re-evaluated when these events fire.
  const visibleRoutes = useFilteredRoutes(
    (state) =>
      state.name.startsWith(`${section}.`) &&
      !state?.data?.visibilityRequirements?.ignoreForMenuVisibilityCheck &&
      isVisible(state.data.visibilityRequirements)
  );

  return visibleRoutes.length > 0;
}

export function useDefaultAdminRouteName() {
  const { ADMIN } = ROUTE_NAMES;

  const repoVisible = useIsSectionVisible(ADMIN.REPOSITORY.DIRECTORY);
  const securityVisible = useIsSectionVisible(ADMIN.SECURITY.DIRECTORY);
  const systemVisible = useIsSectionVisible(ADMIN.SYSTEM.DIRECTORY);
  const supportVisible = useIsSectionVisible(ADMIN.SUPPORT.DIRECTORY);
  const iqVisible = useIsSectionVisible(ADMIN.IQ.ROOT);

  if (repoVisible) {
    return ADMIN.REPOSITORY.DIRECTORY;
  }
  if (securityVisible) {
    return ADMIN.SECURITY.DIRECTORY;
  }
  if (systemVisible) {
    return ADMIN.SYSTEM.DIRECTORY;
  }
  if (supportVisible) {
    return ADMIN.SUPPORT.DIRECTORY;
  }
  if (iqVisible) {
    return ADMIN.IQ.ROOT;
  }

  return null;
}

