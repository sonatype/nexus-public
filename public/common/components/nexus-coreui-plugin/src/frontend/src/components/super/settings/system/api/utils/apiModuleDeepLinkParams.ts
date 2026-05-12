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

import type { MergedApiEndpoint } from './mergeSwaggerPermissions';
import { normalizeFullApiPath } from './mergeSwaggerPermissions';

/** Matches {@link org.sonatype.nexus.security.internal.rest.ApiAccessCheckXo} id fields. */
export const DEEP_LINK_ID_PATTERN = /^[a-zA-Z0-9._-]{1,200}$/;

export interface ApiModuleHashParams {
  user: string | null;
  role: string | null;
  permission: string | null;
  endpoint: string | null;
}

export function extractQueryFromLocationHash(hash: string): string {
  const h = hash.startsWith('#') ? hash.slice(1) : hash;
  const q = h.indexOf('?');
  return q >= 0 ? h.slice(q + 1) : '';
}

export function parseApiModuleHashParams(hash: string): ApiModuleHashParams {
  const qs = extractQueryFromLocationHash(hash);
  const sp = new URLSearchParams(qs);
  const user = sp.get('user');
  const role = sp.get('role');
  const permission = sp.get('permission');
  const endpoint = sp.get('endpoint');
  return {
    user: user && user.trim() ? user.trim() : null,
    role: role && role.trim() ? role.trim() : null,
    permission: permission && permission.trim() ? permission.trim() : null,
    endpoint: endpoint && endpoint.trim() ? endpoint.trim() : null,
  };
}

/**
 * {@code endpoint} query: {@code METHOD:path} where path may be URL-encoded (e.g. {@code GET:%2Fv1%2Frepositories}).
 */
export function parseEndpointDeepLinkParam(raw: string): { method: string; fullPath: string } | null {
  let s = raw.trim();
  if (!s) {
    return null;
  }
  try {
    s = decodeURIComponent(s);
  } catch {
    return null;
  }
  const colon = s.indexOf(':');
  if (colon <= 0) {
    return null;
  }
  const method = s.slice(0, colon).trim().toUpperCase();
  let path = s.slice(colon + 1).trim();
  if (!method || !path) {
    return null;
  }
  if (!path.startsWith('/')) {
    path = `/${path}`;
  }
  if (!path.startsWith('/service/rest')) {
    path = `/service/rest${path.startsWith('/') ? path : `/${path}`}`;
  }
  return { method, fullPath: normalizeFullApiPath(path) };
}

export function filterEndpointsByPermissionSubstring(
  endpoints: MergedApiEndpoint[],
  permissionNeedle: string | null
): MergedApiEndpoint[] {
  if (!permissionNeedle?.trim()) {
    return endpoints;
  }
  const q = permissionNeedle.trim().toLowerCase();
  return endpoints.filter((row) => {
    const perms = row.permission?.permissions ?? [];
    return perms.some((p) => p.permission.toLowerCase().includes(q));
  });
}

export function findEndpointByDeepLink(
  endpoints: MergedApiEndpoint[],
  endpointParam: string | null
): MergedApiEndpoint | null {
  if (!endpointParam) {
    return null;
  }
  const parsed = parseEndpointDeepLinkParam(endpointParam);
  if (!parsed) {
    return null;
  }
  return (
    endpoints.find(
      (e) => e.httpMethod.toUpperCase() === parsed.method && normalizeFullApiPath(e.fullPath) === parsed.fullPath
    ) ?? null
  );
}
