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
  urlBuilder,
  encodeRepositoryItemId,
  decodeRepositoryItemId,
  API_BASE,
  API_V1,
  API_INTERNAL,
  API_INTERNAL_UI,
  ENDPOINTS,
} from '../rest-client';

describe('rest-client', () => {
  describe('API Constants', () => {
    it('defines correct base paths', () => {
      expect(API_BASE).toBe('/service/rest');
      expect(API_V1).toBe('/service/rest/v1');
      expect(API_INTERNAL).toBe('/service/rest/internal');
      expect(API_INTERNAL_UI).toBe('/service/rest/internal/ui');
    });

    it('defines security endpoints', () => {
      expect(ENDPOINTS.PRIVILEGES).toBe('/service/rest/v1/security/privileges');
      expect(ENDPOINTS.ROLES).toBe('/service/rest/v1/security/roles');
      expect(ENDPOINTS.USERS).toBe('/service/rest/v1/security/users');
      expect(ENDPOINTS.REALMS).toBe('/service/rest/v1/security/realms');
      expect(ENDPOINTS.ANONYMOUS).toBe('/service/rest/v1/security/anonymous');
      expect(ENDPOINTS.LDAP).toBe('/service/rest/v1/security/ldap');
    });

    it('defines repository endpoints', () => {
      expect(ENDPOINTS.REPOSITORIES).toBe('/service/rest/v1/repositories');
      expect(ENDPOINTS.COMPONENTS).toBe('/service/rest/v1/components');
      expect(ENDPOINTS.ASSETS).toBe('/service/rest/v1/assets');
      expect(ENDPOINTS.ROUTING_RULES).toBe('/service/rest/v1/routing-rules');
    });

    it('defines dynamic browse endpoint', () => {
      expect(ENDPOINTS.REPOSITORY_BROWSE('maven-central')).toBe(
        '/service/rest/v1/repositories/maven-central/browse'
      );
      expect(ENDPOINTS.REPOSITORY_BROWSE('repo/with/slash')).toBe(
        '/service/rest/v1/repositories/repo%2Fwith%2Fslash/browse'
      );
    });

    it('defines search endpoints', () => {
      expect(ENDPOINTS.SEARCH).toBe('/service/rest/v1/search');
      expect(ENDPOINTS.SEARCH_ASSETS).toBe('/service/rest/v1/search/assets');
    });

    it('defines system endpoints', () => {
      expect(ENDPOINTS.CAPABILITIES).toBe('/service/rest/v1/capabilities');
      expect(ENDPOINTS.TASKS).toBe('/service/rest/v1/tasks');
      expect(ENDPOINTS.EMAIL).toBe('/service/rest/v1/email');
      expect(ENDPOINTS.HTTP_SETTINGS).toBe('/service/rest/v1/http');
    });

    it('defines internal UI endpoints', () => {
      expect(ENDPOINTS.PRIVILEGE_TYPES).toBe('/service/rest/internal/ui/privileges/types');
      expect(ENDPOINTS.ROLE_SOURCES).toBe('/service/rest/internal/ui/roles/sources');
      expect(ENDPOINTS.BROWSE).toBe('/service/rest/internal/ui/browse');
      expect(ENDPOINTS.NODES).toBe('/service/rest/internal/ui/nodes');
    });

    it('defines dynamic health check endpoint', () => {
      expect(ENDPOINTS.HEALTH_CHECK).toBe('/service/rest/internal/ui/healthcheck');
      expect(ENDPOINTS.HEALTH_CHECK_SUMMARY).toBe('/service/rest/internal/ui/healthcheck/summary');
      expect(ENDPOINTS.HEALTH_CHECK_ANALYZE('maven-central')).toBe(
        '/service/rest/v1/repositories/maven-central/health-check'
      );
    });

    it('defines IQ audit endpoints', () => {
      expect(ENDPOINTS.IQ_AUDIT).toBe('/service/rest/v1/iq/audit');
      expect(ENDPOINTS.IQ_AUDIT_REPO('maven-central')).toBe(
        '/service/rest/v1/iq/audit/maven-central'
      );
      expect(ENDPOINTS.IQ_CAPABILITIES).toBe('/service/rest/v1/iq/capabilities');
    });
  });

  describe('encodeRepositoryItemId / decodeRepositoryItemId', () => {
    it('encodes repository item ID to URL-safe base64 without padding', () => {
      const encoded = encodeRepositoryItemId('maven-central', '12345');
      const standardBase64 = btoa('maven-central:12345');

      // Should be URL-safe (no + or /) and without padding
      expect(encoded).not.toContain('+');
      expect(encoded).not.toContain('/');
      expect(encoded).not.toContain('=');

      // Verify roundtrip works
      const decoded = decodeRepositoryItemId(encoded);
      expect(decoded.repositoryName).toBe('maven-central');
      expect(decoded.rawId).toBe('12345');
    });

    it('decodes repository item ID from base64', () => {
      const encoded = btoa('maven-central:12345');
      const decoded = decodeRepositoryItemId(encoded);

      expect(decoded).toEqual({
        repositoryName: 'maven-central',
        rawId: '12345',
      });
    });

    it('handles colons in rawId (e.g., Maven GAV: group:artifact:version)', () => {
      const encoded = encodeRepositoryItemId('my-repo', 'abc:def');
      const decoded = decodeRepositoryItemId(encoded);

      expect(decoded.repositoryName).toBe('my-repo');
      // rawId includes everything after first colon
      expect(decoded.rawId).toBe('abc:def');
    });

    it('roundtrip encoding/decoding', () => {
      const original = { repositoryName: 'test-repo', rawId: '999' };
      const encoded = encodeRepositoryItemId(original.repositoryName, original.rawId);
      const decoded = decodeRepositoryItemId(encoded);

      expect(decoded).toEqual(original);
    });

    it('handles plus signs in rawId like Classic UI', () => {
      const rawIdWithPlus = 'v2.2.1+incompatible';
      const encoded = encodeRepositoryItemId('go-proxy', rawIdWithPlus);
      const decoded = decodeRepositoryItemId(encoded);

      expect(decoded.repositoryName).toBe('go-proxy');
      expect(decoded.rawId).toBe(rawIdWithPlus);
    });

    it('handles URL-safe base64 from Java backend (uses - instead of +)', () => {
      // Java's Base64.getUrlEncoder() produces URL-safe base64 where + becomes - and / becomes _
      // Test that we can decode Java-produced encoded IDs
      // RepositoryItemIDXO format is always "repositoryName:rawId"
      const testInput = 'my-repo:>>>fake'; // btoa('my-repo:>>>fake') = 'bXktcmVwbzo+Pj5mYWtl' contains +
      const javaEncoded = btoa(testInput)
        .replace(/\+/g, '-')  // Java URL-safe encoding
        .replace(/\//g, '_')
        .replace(/=+$/, '');  // Java without padding

      const decoded = decodeRepositoryItemId(javaEncoded);

      expect(decoded.repositoryName).toBe('my-repo');
      expect(decoded.rawId).toBe('>>>fake');
    });
  });

  describe('urlBuilder.path', () => {
    it('replaces path parameters', () => {
      const url = urlBuilder.path('/users/{id}', { id: '123' });
      expect(url).toBe('/users/123');
    });

    it('replaces multiple path parameters', () => {
      const url = urlBuilder.path('/users/{userId}/roles/{roleId}', {
        userId: '123',
        roleId: '456',
      });
      expect(url).toBe('/users/123/roles/456');
    });

    it('encodes path parameters', () => {
      const url = urlBuilder.path('/users/{id}', { id: 'user/with/slashes' });
      expect(url).toBe('/users/user%2Fwith%2Fslashes');
    });

    it('handles numeric parameters', () => {
      const url = urlBuilder.path('/items/{page}', { page: 5 });
      expect(url).toBe('/items/5');
    });
  });

  describe('urlBuilder.query', () => {
    it('builds URL with query parameters', () => {
      const url = urlBuilder.query('/search', { q: 'test', limit: 10 });
      expect(url).toBe('/search?q=test&limit=10');
    });

    it('omits undefined parameters', () => {
      const url = urlBuilder.query('/search', { q: 'test', limit: undefined });
      expect(url).toBe('/search?q=test');
    });

    it('handles boolean parameters', () => {
      const url = urlBuilder.query('/search', { active: true, deleted: false });
      expect(url).toBe('/search?active=true&deleted=false');
    });

    it('returns base URL when no params', () => {
      const url = urlBuilder.query('/search', {});
      expect(url).toBe('/search');
    });
  });

  describe('urlBuilder.privileges', () => {
    it('builds list URL', () => {
      expect(urlBuilder.privileges.list()).toBe(ENDPOINTS.PRIVILEGES);
    });

    it('builds get URL', () => {
      expect(urlBuilder.privileges.get('nx-all')).toBe(
        '/service/rest/v1/security/privileges/nx-all'
      );
    });

    it('builds create URL with type', () => {
      expect(urlBuilder.privileges.createByType('application')).toBe(
        '/service/rest/v1/security/privileges/application'
      );
    });

    it('builds update URL with type and name', () => {
      expect(urlBuilder.privileges.update('application', 'my-priv')).toBe(
        '/service/rest/v1/security/privileges/application/my-priv'
      );
    });

    it('builds delete URL', () => {
      expect(urlBuilder.privileges.delete('nx-all')).toBe(
        '/service/rest/v1/security/privileges/nx-all'
      );
    });

    it('encodes special characters', () => {
      expect(urlBuilder.privileges.get('priv/with/slash')).toBe(
        '/service/rest/v1/security/privileges/priv%2Fwith%2Fslash'
      );
    });
  });

  describe('urlBuilder.roles', () => {
    it('builds CRUD URLs', () => {
      expect(urlBuilder.roles.list()).toBe(ENDPOINTS.ROLES);
      expect(urlBuilder.roles.get('nx-admin')).toBe('/service/rest/v1/security/roles/nx-admin');
      expect(urlBuilder.roles.create()).toBe(ENDPOINTS.ROLES);
      expect(urlBuilder.roles.update('nx-admin')).toBe('/service/rest/v1/security/roles/nx-admin');
      expect(urlBuilder.roles.delete('nx-admin')).toBe('/service/rest/v1/security/roles/nx-admin');
    });
  });

  describe('urlBuilder.users', () => {
    it('builds CRUD URLs', () => {
      expect(urlBuilder.users.list()).toBe(ENDPOINTS.USERS);
      expect(urlBuilder.users.get('admin')).toBe('/service/rest/v1/security/users/admin');
      expect(urlBuilder.users.create()).toBe(ENDPOINTS.USERS);
      expect(urlBuilder.users.update('admin')).toBe('/service/rest/v1/security/users/admin');
      expect(urlBuilder.users.delete('admin')).toBe('/service/rest/v1/security/users/admin');
    });

    it('builds change password URL', () => {
      expect(urlBuilder.users.changePassword('admin')).toBe(
        '/service/rest/v1/security/users/admin/change-password'
      );
    });
  });

  describe('urlBuilder.ldap', () => {
    it('builds CRUD URLs', () => {
      expect(urlBuilder.ldap.list()).toBe(ENDPOINTS.LDAP);
      expect(urlBuilder.ldap.get('myldap')).toBe('/service/rest/v1/security/ldap/myldap');
      expect(urlBuilder.ldap.create()).toBe(ENDPOINTS.LDAP);
      expect(urlBuilder.ldap.update('myldap')).toBe('/service/rest/v1/security/ldap/myldap');
      expect(urlBuilder.ldap.delete('myldap')).toBe('/service/rest/v1/security/ldap/myldap');
    });

    it('builds utility URLs', () => {
      expect(urlBuilder.ldap.changeOrder()).toBe('/service/rest/v1/security/ldap/change-order');
      expect(urlBuilder.ldap.templates()).toBe('/service/rest/v1/security/ldap/templates');
      expect(urlBuilder.ldap.clearCache()).toBe('/service/rest/v1/security/ldap/cache');
      expect(urlBuilder.ldap.verifyConnection()).toBe(
        '/service/rest/v1/security/ldap/verify-connection'
      );
      expect(urlBuilder.ldap.verifyUserMapping()).toBe(
        '/service/rest/v1/security/ldap/verify-user-mapping'
      );
      expect(urlBuilder.ldap.verifyLogin()).toBe('/service/rest/v1/security/ldap/verify-login');
    });
  });

  describe('urlBuilder.components', () => {
    it('builds get and delete URLs', () => {
      expect(urlBuilder.components.get('abc123')).toBe('/service/rest/v1/components/abc123');
      expect(urlBuilder.components.delete('abc123')).toBe('/service/rest/v1/components/abc123');
    });
  });

  describe('urlBuilder.assets', () => {
    it('builds get and delete URLs', () => {
      expect(urlBuilder.assets.get('xyz789')).toBe('/service/rest/v1/assets/xyz789');
      expect(urlBuilder.assets.delete('xyz789')).toBe('/service/rest/v1/assets/xyz789');
    });
  });

  describe('urlBuilder.capabilities', () => {
    it('builds CRUD URLs', () => {
      expect(urlBuilder.capabilities.list()).toBe(ENDPOINTS.CAPABILITIES);
      expect(urlBuilder.capabilities.types()).toBe(ENDPOINTS.CAPABILITIES_TYPES);
      expect(urlBuilder.capabilities.get('cap1')).toBe('/service/rest/v1/capabilities/cap1');
      expect(urlBuilder.capabilities.create()).toBe(ENDPOINTS.CAPABILITIES);
      expect(urlBuilder.capabilities.update('cap1')).toBe('/service/rest/v1/capabilities/cap1');
      expect(urlBuilder.capabilities.delete('cap1')).toBe('/service/rest/v1/capabilities/cap1');
    });
  });

  describe('urlBuilder.tasks', () => {
    it('builds CRUD URLs', () => {
      expect(urlBuilder.tasks.list()).toBe(ENDPOINTS.TASKS);
      expect(urlBuilder.tasks.get('task1')).toBe('/service/rest/v1/tasks/task1');
      expect(urlBuilder.tasks.create()).toBe(ENDPOINTS.TASKS);
      expect(urlBuilder.tasks.update('task1')).toBe('/service/rest/v1/tasks/task1');
      expect(urlBuilder.tasks.delete('task1')).toBe('/service/rest/v1/tasks/task1');
    });

    it('builds action URLs', () => {
      expect(urlBuilder.tasks.run('task1')).toBe('/service/rest/v1/tasks/task1/run');
      expect(urlBuilder.tasks.stop('task1')).toBe('/service/rest/v1/tasks/task1/stop');
    });

    it('builds template URLs', () => {
      expect(urlBuilder.tasks.templates()).toBe('/service/rest/v1/tasks/templates');
      expect(urlBuilder.tasks.template('db.backup')).toBe(
        '/service/rest/v1/tasks/templates/db.backup'
      );
    });
  });

  describe('urlBuilder.email', () => {
    it('builds email configuration URLs', () => {
      expect(urlBuilder.email.get()).toBe(ENDPOINTS.EMAIL);
      expect(urlBuilder.email.update()).toBe(ENDPOINTS.EMAIL);
      expect(urlBuilder.email.delete()).toBe(ENDPOINTS.EMAIL);
      expect(urlBuilder.email.verify()).toBe(ENDPOINTS.EMAIL_VERIFY);
    });
  });

  describe('urlBuilder.tags', () => {
    it('builds CRUD URLs', () => {
      expect(urlBuilder.tags.list()).toBe(ENDPOINTS.TAGS);
      expect(urlBuilder.tags.get('release')).toBe('/service/rest/v1/tags/release');
      expect(urlBuilder.tags.create()).toBe(ENDPOINTS.TAGS);
      expect(urlBuilder.tags.update('release')).toBe('/service/rest/v1/tags/release');
      expect(urlBuilder.tags.delete('release')).toBe('/service/rest/v1/tags/release');
      expect(urlBuilder.tags.filtered()).toBe(ENDPOINTS.TAGS_FILTERED);
    });
  });
});
