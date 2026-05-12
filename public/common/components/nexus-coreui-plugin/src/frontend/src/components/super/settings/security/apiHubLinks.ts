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

/** Hash target for Preview UI API hub ({@code preview.browse.api}). */
export const PREVIEW_BROWSE_API_HUB = '#/preview/browse/api';

export function apiHubHref(params: { user?: string; role?: string; permission?: string; endpoint?: string }): string {
  const sp = new URLSearchParams();
  if (params.user) {
    sp.set('user', params.user);
  }
  if (params.role) {
    sp.set('role', params.role);
  }
  if (params.permission) {
    sp.set('permission', params.permission);
  }
  if (params.endpoint) {
    sp.set('endpoint', params.endpoint);
  }
  const q = sp.toString();
  return q ? `${PREVIEW_BROWSE_API_HUB}?${q}` : PREVIEW_BROWSE_API_HUB;
}
