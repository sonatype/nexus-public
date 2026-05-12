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

import type { Role } from '../../../security/roles/types';

import { buildRoleByIdMap, collectPrivilegeIdsForRole } from './rolePrivilegeCoverage';

/**
 * Qualifying roles sorted for grant recommendations: fewer effective privileges first (more specific).
 * {@code allRoles} must include nested roles referenced by {@code qualifying} for accurate counts.
 */
export function rankRolesForGrantRecommendations(qualifying: Role[], allRoles: Role[]): Role[] {
  const roleById = buildRoleByIdMap(allRoles);
  return [...qualifying].sort((a, b) => {
    const ca = collectPrivilegeIdsForRole(a.id, roleById).size;
    const cb = collectPrivilegeIdsForRole(b.id, roleById).size;
    if (ca !== cb) {
      return ca - cb;
    }
    return a.name.localeCompare(b.name);
  });
}
