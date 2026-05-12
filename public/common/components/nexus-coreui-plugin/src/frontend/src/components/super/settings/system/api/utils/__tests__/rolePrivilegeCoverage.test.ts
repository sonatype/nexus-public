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

import type { Privilege } from '../../../../security/privileges/types';
import type { Role } from '../../../../security/roles/types';
import {
  buildPrivilegeByIdMap,
  buildRoleByIdMap,
  collectPrivilegeIdsForRole,
  roleSatisfiesRequiredPermissions,
} from '../rolePrivilegeCoverage';

function role(id: string, privileges: string[], nested: string[] = []): Role {
  return {
    id,
    version: '1',
    source: 'default',
    name: id,
    description: '',
    readOnly: false,
    privileges,
    roles: nested,
  };
}

function priv(id: string, permission: string): Privilege {
  return {
    id,
    version: '1',
    name: id,
    description: '',
    type: 'wildcard',
    readOnly: false,
    properties: {},
    permission,
  };
}

describe('rolePrivilegeCoverage', () => {
  it('collects nested role privileges', () => {
    const roles = [role('a', ['p1'], ['b']), role('b', ['p2'], [])];
    const map = buildRoleByIdMap(roles);
    const ids = collectPrivilegeIdsForRole('a', map);
    expect(ids.has('p1')).toBe(true);
    expect(ids.has('p2')).toBe(true);
  });

  it('AND requires every permission', () => {
    const roles = [role('r1', ['px', 'py'], [])];
    const privs = [priv('px', 'nexus:a:read'), priv('py', 'nexus:b:read')];
    const rmap = buildRoleByIdMap(roles);
    const pmap = buildPrivilegeByIdMap(privs);
    expect(
      roleSatisfiesRequiredPermissions('r1', rmap, pmap, ['nexus:a:read', 'nexus:b:read'], 'AND')
    ).toBe(true);
    expect(roleSatisfiesRequiredPermissions('r1', rmap, pmap, ['nexus:a:read', 'nexus:z:read'], 'AND')).toBe(false);
  });

  it('OR requires any permission', () => {
    const roles = [role('r1', ['px'], [])];
    const privs = [priv('px', 'nexus:*')];
    const rmap = buildRoleByIdMap(roles);
    const pmap = buildPrivilegeByIdMap(privs);
    expect(
      roleSatisfiesRequiredPermissions('r1', rmap, pmap, ['nexus:a:read', 'nexus:b:read'], 'OR')
    ).toBe(true);
  });
});
