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
import { useRouter } from '@uirouter/react';
import { useIsVisible } from '../../../../interface/NavigationUtils';
import { useContextAwareRouteName } from './useContextAwareRouteName';

// Sentinel value to indicate route doesn't exist (distinct from undefined visibilityRequirements)
const ROUTE_NOT_FOUND = Symbol('ROUTE_NOT_FOUND');

/**
 * Hook to resolve route state and visibilityRequirements from a route.
 * Returns ROUTE_NOT_FOUND if the route doesn't exist, or the requirements (which may be undefined).
 */
function useRouteVisibilityRequirements(baseName: string) {
  const router = useRouter();
  const contextAwareName = useContextAwareRouteName(baseName) || '';

  try {
    // Try context-aware name first, fall back to base name
    let state = router.stateRegistry.get(contextAwareName);
    if (!state) {
      state = router.stateRegistry.get(baseName);
    }

    if (!state) {
      return ROUTE_NOT_FOUND; // Route doesn't exist
    }

    const data = state?.data || {};
    // May be undefined if route has no visibility requirements (meaning always visible)
    return data.visibilityRequirements;
  } catch {
    return ROUTE_NOT_FOUND;
  }
}

/**
 * Hook to check if a route is visible based on permissions, editions, etc.
 * Uses the reactive useIsVisible hook from nexus-ui-plugin which subscribes to
 * Permissions#changed, State#changed, and State#userchanged events.
 *
 * @param baseName - Base route name to check visibility for
 * @returns boolean indicating if the route is visible
 */
export function useRouteVisibility(baseName: string): boolean {
  const visibilityRequirements = useRouteVisibilityRequirements(baseName);
  const routeExists = visibilityRequirements !== ROUTE_NOT_FOUND;

  // useIsVisible handles permission/state change events reactively
  // If route has no requirements (undefined), useIsVisible returns true
  const isRouteVisible = useIsVisible(routeExists ? visibilityRequirements : undefined);

  // Route must exist to be visible. Missing routes return false (fail closed).
  return routeExists ? isRouteVisible : false;
}
