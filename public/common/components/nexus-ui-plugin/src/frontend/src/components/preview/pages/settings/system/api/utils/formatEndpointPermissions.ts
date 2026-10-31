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

import type { MergedApiEndpoint } from './mergeSwaggerPermissions';

const NO_MAPPED_TEXT = 'None mapped (see documentation)';
const NO_ANONYMOUS_TEXT = 'None (may allow anonymous)';
const AND_JOIN = ' AND ';
const OR_JOIN = ' OR ';

/** Logical operator combining an endpoint's permission requirements. */
export type PermissionLogical = 'AND' | 'OR';

/** User-facing label for how required permissions combine. */
export type PermissionRequirementMode = 'ANY' | 'ALL';

/**
 * Returns the logical operator that combines this endpoint's permission requirements.
 * The Nexus convention is that the first requirement's `logical` field decides how the
 * entire set combines. Defaults to 'AND' when no permissions are present, so callers
 * that do not early-return on the empty case still get a sensible value.
 */
export function permissionLogical(
  permission: ApiEndpointPermissionDto | null | undefined
): PermissionLogical {
  return permission?.permissions?.[0]?.logical === 'OR' ? 'OR' : 'AND';
}

/**
 * User-facing 'ANY' / 'ALL' label for the logical mode combining this endpoint's
 * permission requirements. Backed by {@link permissionLogical}.
 */
export function permissionRequirementMode(
  permission: ApiEndpointPermissionDto | null | undefined
): PermissionRequirementMode {
  return permissionLogical(permission) === 'OR' ? 'ANY' : 'ALL';
}

/**
 * Renders the endpoint's permission requirements as a human-readable "Required" string:
 * permission strings joined by AND/OR based on the row's logical operator, or a placeholder
 * when no permissions are mapped.
 */
export function formatEndpointPermissions(row: MergedApiEndpoint): string {
  const reqs = row.permission?.permissions ?? [];
  if (reqs.length === 0) {
    return row.permission?.authenticated ? NO_MAPPED_TEXT : NO_ANONYMOUS_TEXT;
  }
  const separator = permissionLogical(row.permission) === 'OR' ? OR_JOIN : AND_JOIN;
  return reqs.map((r) => r.permission).join(separator);
}
