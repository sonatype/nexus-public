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

import type { ApiEndpointPermissionDto } from '../types';

import { userImpliesPermission } from './shiroWildcardImplies';

export type EndpointAccessDot = 'granted' | 'denied' | 'partial' | 'unknown';

/**
 * Client-side access indicator for the current user (Nexus permission map + Shiro-style wildcards).
 */
export function computeEndpointAccess(
  meta: ApiEndpointPermissionDto | null | undefined,
  userPermissionMap: Record<string, boolean>
): EndpointAccessDot {
  if (!meta) {
    return 'unknown';
  }
  const reqs = meta.permissions ?? [];
  if (reqs.length === 0) {
    if (!meta.authenticated) {
      return 'granted';
    }
    return 'unknown';
  }

  const logical = reqs[0]?.logical === 'OR' ? 'OR' : 'AND';
  const checks = reqs.map((r) =>
    userImpliesPermission((r.permission || '').toLowerCase(), userPermissionMap)
  );

  if (logical === 'OR') {
    return checks.some(Boolean) ? 'granted' : 'denied';
  }

  const ok = checks.filter(Boolean).length;
  if (ok === reqs.length) {
    return 'granted';
  }
  if (ok === 0) {
    return 'denied';
  }
  return 'partial';
}
