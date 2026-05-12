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

import type { MergedApiEndpoint } from '../mergeSwaggerPermissions';
import {
  extractQueryFromLocationHash,
  filterEndpointsByPermissionSubstring,
  findEndpointByDeepLink,
  parseApiModuleHashParams,
  parseEndpointDeepLinkParam,
} from '../apiModuleDeepLinkParams';

function row(method: string, path: string, perms: string[]): MergedApiEndpoint {
  return {
    httpMethod: method,
    swaggerPathKey: '/x',
    fullPath: path,
    tag: 'T',
    permission: {
      httpMethod: method,
      pathPattern: path,
      permissions: perms.map((permission) => ({ permission, logical: 'AND' as const })),
      description: null,
      tag: null,
      authenticated: true,
    },
  };
}

describe('apiModuleDeepLinkParams', () => {
  it('extracts query from hash', () => {
    expect(extractQueryFromLocationHash('#/preview/browse/api?user=admin')).toBe('user=admin');
    expect(extractQueryFromLocationHash('#/path')).toBe('');
  });

  it('parses hash params', () => {
    expect(parseApiModuleHashParams('#/preview/browse/api?user=a&role=r')).toEqual({
      user: 'a',
      role: 'r',
      permission: null,
      endpoint: null,
    });
    expect(parseApiModuleHashParams('#?permission=nexus:foo')).toEqual({
      user: null,
      role: null,
      permission: 'nexus:foo',
      endpoint: null,
    });
  });

  it('parses endpoint deep link', () => {
    expect(parseEndpointDeepLinkParam('GET:%2Fv1%2Frepositories')).toEqual({
      method: 'GET',
      fullPath: '/service/rest/v1/repositories',
    });
    expect(parseEndpointDeepLinkParam('POST:/service/rest/v1/foo')).toEqual({
      method: 'POST',
      fullPath: '/service/rest/v1/foo',
    });
    expect(parseEndpointDeepLinkParam('bad')).toBeNull();
  });

  it('filters by permission substring', () => {
    const endpoints = [
      row('GET', '/service/rest/a', ['nexus:repos:read']),
      row('DELETE', '/service/rest/b', ['nexus:other:delete']),
    ];
    const out = filterEndpointsByPermissionSubstring(endpoints, 'repos');
    expect(out).toHaveLength(1);
    expect(out[0].fullPath).toBe('/service/rest/a');
  });

  it('finds endpoint by deep link param', () => {
    const endpoints = [row('GET', '/service/rest/v1/repositories', ['x'])];
    const found = findEndpointByDeepLink(endpoints, 'GET:%2Fv1%2Frepositories');
    expect(found?.httpMethod).toBe('GET');
  });
});
