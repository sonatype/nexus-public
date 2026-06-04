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

import type { ApiEndpointPermissionDto } from '../../types';
import { computeEndpointAccess } from '../endpointAccess';

function perm(p: Partial<ApiEndpointPermissionDto>): ApiEndpointPermissionDto {
  return {
    httpMethod: 'GET',
    pathPattern: '/x',
    permissions: [],
    description: null,
    tag: null,
    authenticated: false,
    ...p,
  };
}

describe('computeEndpointAccess', () => {
  it('public unauthenticated without permissions is granted', () => {
    expect(computeEndpointAccess(perm({ authenticated: false, permissions: [] }), {})).toBe('granted');
  });

  it('AND all required', () => {
    const meta = perm({
      authenticated: true,
      permissions: [
        { permission: 'nexus:a:read', logical: 'AND' },
        { permission: 'nexus:b:read', logical: 'AND' },
      ],
    });
    expect(
      computeEndpointAccess(meta, { 'nexus:a:read': true, 'nexus:b:read': true })
    ).toBe('granted');
    expect(computeEndpointAccess(meta, { 'nexus:a:read': true })).toBe('partial');
    expect(computeEndpointAccess(meta, {})).toBe('denied');
  });

  it('OR any', () => {
    const meta = perm({
      authenticated: true,
      permissions: [
        { permission: 'nexus:a:read', logical: 'OR' },
        { permission: 'nexus:b:read', logical: 'OR' },
      ],
    });
    expect(computeEndpointAccess(meta, { 'nexus:b:read': true })).toBe('granted');
    expect(computeEndpointAccess(meta, {})).toBe('denied');
  });
});
