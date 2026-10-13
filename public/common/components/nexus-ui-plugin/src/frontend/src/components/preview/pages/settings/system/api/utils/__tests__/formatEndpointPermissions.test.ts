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

import {
  formatEndpointPermissions,
  permissionLogical,
  permissionRequirementMode,
} from '../formatEndpointPermissions';
import type { MergedApiEndpoint } from '../mergeSwaggerPermissions';
import type { ApiEndpointPermissionDto } from '../../types';

function makeRow(overrides: Partial<MergedApiEndpoint> = {}): MergedApiEndpoint {
  return {
    httpMethod: 'GET',
    swaggerPathKey: '/v1/status',
    fullPath: '/service/rest/v1/status',
    tag: 'Status',
    permission: {
      httpMethod: 'GET',
      pathPattern: '/service/rest/v1/status',
      permissions: [],
      description: null,
      tag: null,
      authenticated: true,
    },
    ...overrides,
  };
}

describe('formatEndpointPermissions', () => {
  it('returns the anonymous placeholder when there are no permissions and the endpoint is anonymous', () => {
    const row = makeRow({
      permission: {
        httpMethod: 'GET',
        pathPattern: '/service/rest/v1/status',
        permissions: [],
        description: null,
        tag: null,
        authenticated: false,
      },
    });
    expect(formatEndpointPermissions(row)).toBe('None (may allow anonymous)');
  });

  it('returns the "None mapped" placeholder when there are no permissions and the endpoint is authenticated', () => {
    const row = makeRow();
    expect(formatEndpointPermissions(row)).toBe('None mapped (see documentation)');
  });

  it('returns the anonymous placeholder when the endpoint has no permission metadata at all', () => {
    const row = makeRow({ permission: null });
    expect(formatEndpointPermissions(row)).toBe('None (may allow anonymous)');
  });

  it('joins a single permission by itself', () => {
    const row = makeRow({
      permission: {
        httpMethod: 'GET',
        pathPattern: '/service/rest/v1/status',
        permissions: [{ permission: 'nexus:test:read', logical: 'AND' }],
        description: null,
        tag: null,
        authenticated: true,
      },
    });
    expect(formatEndpointPermissions(row)).toBe('nexus:test:read');
  });

  it('joins multiple AND permissions with " AND "', () => {
    const row = makeRow({
      permission: {
        httpMethod: 'GET',
        pathPattern: '/service/rest/v1/repositories',
        permissions: [
          { permission: 'nexus:repos:read', logical: 'AND' },
          { permission: 'nexus:repos:browse', logical: 'AND' },
        ],
        description: null,
        tag: null,
        authenticated: true,
      },
    });
    expect(formatEndpointPermissions(row)).toBe('nexus:repos:read AND nexus:repos:browse');
  });

  it('joins multiple OR permissions with " OR "', () => {
    const row = makeRow({
      permission: {
        httpMethod: 'GET',
        pathPattern: '/service/rest/v1/repositories',
        permissions: [
          { permission: 'nexus:a:read', logical: 'OR' },
          { permission: 'nexus:b:read', logical: 'OR' },
        ],
        description: null,
        tag: null,
        authenticated: true,
      },
    });
    expect(formatEndpointPermissions(row)).toBe('nexus:a:read OR nexus:b:read');
  });

  it('uses the first requirement to decide the logical join', () => {
    const row = makeRow({
      permission: {
        httpMethod: 'GET',
        pathPattern: '/service/rest/v1/x',
        permissions: [
          { permission: 'a', logical: 'OR' },
          { permission: 'b', logical: 'AND' },
        ],
        description: null,
        tag: null,
        authenticated: true,
      },
    });
    expect(formatEndpointPermissions(row)).toBe('a OR b');
  });
});

function permissionWith(
  overrides: Partial<ApiEndpointPermissionDto> = {}
): ApiEndpointPermissionDto {
  return {
    httpMethod: 'GET',
    pathPattern: '/service/rest/v1/status',
    permissions: [],
    description: null,
    tag: null,
    authenticated: true,
    ...overrides,
  };
}

describe('permissionLogical', () => {
  it('returns AND when permission is null', () => {
    expect(permissionLogical(null)).toBe('AND');
  });

  it('returns AND when permission is undefined', () => {
    expect(permissionLogical(undefined)).toBe('AND');
  });

  it('returns AND when permissions array is empty', () => {
    expect(permissionLogical(permissionWith({ permissions: [] }))).toBe('AND');
  });

  it('returns AND when the first requirement is AND', () => {
    expect(
      permissionLogical(
        permissionWith({ permissions: [{ permission: 'a', logical: 'AND' }] })
      )
    ).toBe('AND');
  });

  it('returns OR when the first requirement is OR', () => {
    expect(
      permissionLogical(
        permissionWith({ permissions: [{ permission: 'a', logical: 'OR' }] })
      )
    ).toBe('OR');
  });

  it('decides by the first requirement even when later requirements differ', () => {
    expect(
      permissionLogical(
        permissionWith({
          permissions: [
            { permission: 'a', logical: 'OR' },
            { permission: 'b', logical: 'AND' },
          ],
        })
      )
    ).toBe('OR');
  });
});

describe('permissionRequirementMode', () => {
  it('returns ALL when permission is null', () => {
    expect(permissionRequirementMode(null)).toBe('ALL');
  });

  it('returns ALL when the first requirement is AND', () => {
    expect(
      permissionRequirementMode(
        permissionWith({ permissions: [{ permission: 'a', logical: 'AND' }] })
      )
    ).toBe('ALL');
  });

  it('returns ANY when the first requirement is OR', () => {
    expect(
      permissionRequirementMode(
        permissionWith({ permissions: [{ permission: 'a', logical: 'OR' }] })
      )
    ).toBe('ANY');
  });
});
