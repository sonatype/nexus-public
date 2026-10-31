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

const SWAGGER_METHODS = ['get', 'post', 'put', 'delete', 'patch', 'head', 'options'] as const;

type SwaggerMethod = (typeof SWAGGER_METHODS)[number];

export interface SwaggerOperationDoc {
  operationId?: string;
  summary?: string;
  description?: string;
  tags?: string[];
}

export interface MergedApiEndpoint {
  /** Uppercase HTTP method */
  httpMethod: string;
  /** Key as in {@code swagger.paths} (e.g. {@code /v1/repositories}) */
  swaggerPathKey: string;
  /** Full path including {@code /service/rest} prefix */
  fullPath: string;
  operationId?: string;
  summary?: string;
  description?: string;
  /** Primary tag for grouping (Swagger first, else permission tag) */
  tag: string;
  permission: ApiEndpointPermissionDto | null;
}

export function normalizeFullApiPath(path: string): string {
  let p = path.replace(/\/{2,}/g, '/');
  if (p.length > 1 && p.endsWith('/')) {
    p = p.slice(0, -1);
  }
  return p;
}

export function joinSwaggerBasePath(basePath: string, pathKey: string): string {
  const base = (basePath || '/service/rest').replace(/\/$/, '');
  const key = pathKey.startsWith('/') ? pathKey : `/${pathKey}`;
  return normalizeFullApiPath(`${base}${key}`);
}

export function permissionLookupKey(httpMethod: string, fullPath: string): string {
  return `${httpMethod.toUpperCase()}|${normalizeFullApiPath(fullPath)}`;
}

export function buildPermissionIndex(
  endpoints: ApiEndpointPermissionDto[]
): Map<string, ApiEndpointPermissionDto> {
  const map = new Map<string, ApiEndpointPermissionDto>();
  for (const e of endpoints) {
    map.set(permissionLookupKey(e.httpMethod, e.pathPattern), e);
  }
  return map;
}

function isRecord(v: unknown): v is Record<string, unknown> {
  return typeof v === 'object' && v !== null && !Array.isArray(v);
}

/**
 * Flattens Swagger 2.0 paths + attaches permission rows when paths align with the registry.
 */
export function mergeSwaggerAndPermissions(
  swagger: unknown,
  permissionEndpoints: ApiEndpointPermissionDto[]
): MergedApiEndpoint[] {
  const index = buildPermissionIndex(permissionEndpoints);
  const out: MergedApiEndpoint[] = [];

  if (!(isRecord(swagger) && isRecord(swagger.paths))) {
    for (const p of permissionEndpoints) {
      out.push({
        httpMethod: p.httpMethod.toUpperCase(),
        swaggerPathKey: stripServiceRest(p.pathPattern),
        fullPath: normalizeFullApiPath(p.pathPattern),
        operationId: undefined,
        summary: p.description ?? undefined,
        description: p.description ?? undefined,
        tag: p.tag || 'Other',
        permission: p,
      });
    }
    return sortMerged(out);
  }

  const basePath = typeof swagger.basePath === 'string' ? swagger.basePath : '/service/rest';
  const paths = swagger.paths as Record<string, Record<string, unknown>>;

  for (const [pathKey, pathItem] of Object.entries(paths)) {
    if (!isRecord(pathItem)) {
      continue;
    }
    for (const m of SWAGGER_METHODS) {
      const op = pathItem[m];
      if (!isRecord(op)) {
        continue;
      }
      const methodUpper = m.toUpperCase();
      const fullPath = joinSwaggerBasePath(basePath, pathKey);
      const perm = index.get(permissionLookupKey(methodUpper, fullPath)) ?? null;
      const opTyped = op as SwaggerOperationDoc;
      const tag =
        (opTyped.tags?.[0]) || perm?.tag || inferTagFromPath(pathKey) || 'Other';
      out.push({
        httpMethod: methodUpper,
        swaggerPathKey: pathKey.startsWith('/') ? pathKey : `/${pathKey}`,
        fullPath,
        operationId: opTyped.operationId,
        summary: opTyped.summary,
        description: opTyped.description,
        tag,
        permission: perm,
      });
    }
  }

  return sortMerged(out);
}

function stripServiceRest(fullPath: string): string {
  const prefix = '/service/rest';
  const n = normalizeFullApiPath(fullPath);
  if (n.startsWith(prefix)) {
    const rest = n.slice(prefix.length);
    return rest.startsWith('/') ? rest : `/${rest}`;
  }
  return n.startsWith('/') ? n : `/${n}`;
}

function inferTagFromPath(pathKey: string): string {
  const parts = pathKey.split('/').filter(Boolean);
  if (parts.length >= 2 && parts[0] === 'v1') {
    return `${parts[1].replace(/-/g, ' ')}`.replace(/\b\w/g, (c) => c.toUpperCase());
  }
  return 'Other';
}

function sortMerged(rows: MergedApiEndpoint[]): MergedApiEndpoint[] {
  return [...rows].sort((a, b) => {
    const tc = a.tag.localeCompare(b.tag);
    if (tc !== 0) {
      return tc;
    }
    return a.fullPath.localeCompare(b.fullPath) || a.httpMethod.localeCompare(b.httpMethod);
  });
}

/**
 * Single-operation Swagger 2 document for embedded Swagger UI.
 */
export function sliceSwaggerSpec(
  swagger: Record<string, unknown>,
  swaggerPathKey: string,
  methodLower: SwaggerMethod
): Record<string, unknown> {
  const clone = JSON.parse(JSON.stringify(swagger)) as Record<string, unknown>;
  const paths = isRecord(clone.paths) ? clone.paths : {};
  const pathItem = isRecord(paths[swaggerPathKey]) ? (paths[swaggerPathKey] as Record<string, unknown>) : null;
  if (!pathItem) {
    return clone;
  }
  const slimItem: Record<string, unknown> = { ...pathItem };
  for (const m of SWAGGER_METHODS) {
    if (m !== methodLower) {
      delete slimItem[m];
    }
  }
  clone.paths = { [swaggerPathKey]: slimItem };
  return clone;
}
