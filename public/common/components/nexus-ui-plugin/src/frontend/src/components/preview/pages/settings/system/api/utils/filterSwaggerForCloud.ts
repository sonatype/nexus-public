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

/**
 * Path keys as they appear in Nexus Swagger 2.0 {@code paths} (relative to {@code basePath}, e.g. {@code /v1/status}).
 * These APIs are not applicable in Repository Cloud; hide them from the preview API documentation UI.
 */
const SELF_HOSTED_ONLY_PATH_PREFIXES: readonly string[] = [
  '/v1/blobstores',
  '/v1/system/node',
  '/v1/nodes',
  '/v1/ssl',
  '/v1/ldap',
  '/v1/security/saml',
  '/v1/security/anonymous',
  '/v1/system/license',
  '/v1/email',
];

function normalizePathKey(key: string): string {
  const k = key.trim();
  if (k.length === 0) {
    return '';
  }
  return k.startsWith('/') ? k : `/${k}`;
}

export function isSelfHostedOnlySwaggerPath(pathKey: string): boolean {
  const k = normalizePathKey(pathKey);
  return SELF_HOSTED_ONLY_PATH_PREFIXES.some((prefix) => k === prefix || k.startsWith(`${prefix}/`));
}

function isRecord(v: unknown): v is Record<string, unknown> {
  return typeof v === 'object' && v !== null && !Array.isArray(v);
}

/**
 * Returns a shallow copy of the spec with self-hosted-only paths removed (for cloud UI).
 */
export function filterSwaggerSpecForCloud(spec: Record<string, unknown>): Record<string, unknown> {
  const paths = spec.paths;
  if (!isRecord(paths)) {
    return spec;
  }
  const nextPaths: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(paths)) {
    if (!isSelfHostedOnlySwaggerPath(key)) {
      nextPaths[key] = value;
    }
  }
  return { ...spec, paths: nextPaths };
}
