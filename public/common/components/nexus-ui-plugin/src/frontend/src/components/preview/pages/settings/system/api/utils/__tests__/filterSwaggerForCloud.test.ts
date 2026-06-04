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
  filterSwaggerSpecForCloud,
  isSelfHostedOnlySwaggerPath,
} from '../filterSwaggerForCloud';

describe('filterSwaggerForCloud', () => {
  describe('isSelfHostedOnlySwaggerPath', () => {
    it.each([
      ['/v1/blobstores', true],
      ['/v1/blobstores/foo', true],
      ['v1/blobstores', true],
      ['/v1/system/node', true],
      ['/v1/system/node/metadata', true],
      ['/v1/nodes', true],
      ['/v1/nodes/abc', true],
      ['/v1/ssl', true],
      ['/v1/ssl/truststore', true],
      ['/v1/ldap', true],
      ['/v1/ldap/orders', true],
      ['/v1/security/saml', true],
      ['/v1/security/saml/metadata', true],
      ['/v1/security/anonymous', true],
      ['/v1/system/license', true],
      ['/v1/email', true],
      ['/v1/email/verify', true],
      ['/v1/status', false],
      ['/v1/repositories', false],
      ['/v1/system/nodes', false],
      ['/v1/security/users', false],
    ])('path %s -> %s', (path, expected) => {
      expect(isSelfHostedOnlySwaggerPath(path)).toBe(expected);
    });
  });

  describe('filterSwaggerSpecForCloud', () => {
    it('removes excluded path keys and keeps others', () => {
      const spec = {
        swagger: '2.0',
        basePath: '/service/rest',
        paths: {
          '/v1/blobstores': { get: {} },
          '/v1/blobstores/{name}': { delete: {} },
          '/v1/status': { get: { summary: 'ok' } },
          '/v1/email': { get: {} },
        },
      };
      const out = filterSwaggerSpecForCloud(spec);
      expect(out.paths).toEqual({
        '/v1/status': { get: { summary: 'ok' } },
      });
      expect(out.swagger).toBe('2.0');
      expect(spec.paths).toHaveProperty('/v1/blobstores');
    });

    it('returns the same object reference when paths is missing', () => {
      const spec = { swagger: '2.0' };
      expect(filterSwaggerSpecForCloud(spec)).toBe(spec);
    });
  });
});
