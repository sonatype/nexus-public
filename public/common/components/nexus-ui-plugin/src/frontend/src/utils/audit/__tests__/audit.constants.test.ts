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
  DOMAIN_CATEGORY_MAP,
  CATEGORY_COLORS,
  CATEGORY_LABELS,
  COMMON_EVENT_TYPES,
} from '../audit.constants';
import type { AuditCategory } from '../audit.types';

describe('audit.constants', () => {
  describe('DOMAIN_CATEGORY_MAP', () => {
    describe('Security Domains', () => {
      it('should map security.user to security category', () => {
        expect(DOMAIN_CATEGORY_MAP['security.user']).toBe('security');
      });

      it('should map security.role to security category', () => {
        expect(DOMAIN_CATEGORY_MAP['security.role']).toBe('security');
      });

      it('should map security.privilege to security category', () => {
        expect(DOMAIN_CATEGORY_MAP['security.privilege']).toBe('security');
      });

      it('should map security.user-role-mapping to security category', () => {
        expect(DOMAIN_CATEGORY_MAP['security.user-role-mapping']).toBe('security');
      });

      it('should map security.anonymous to security category', () => {
        expect(DOMAIN_CATEGORY_MAP['security.anonymous']).toBe('security');
      });

      it('should map security.realm to security category', () => {
        expect(DOMAIN_CATEGORY_MAP['security.realm']).toBe('security');
      });

      it('should map security.ldap to security category', () => {
        expect(DOMAIN_CATEGORY_MAP['security.ldap']).toBe('security');
      });

      it('should map security.crowd to security category', () => {
        expect(DOMAIN_CATEGORY_MAP['security.crowd']).toBe('security');
      });

      it('should map security.sslcertificate to security category', () => {
        expect(DOMAIN_CATEGORY_MAP['security.sslcertificate']).toBe('security');
      });

      it('should map security.secrets to security category', () => {
        expect(DOMAIN_CATEGORY_MAP['security.secrets']).toBe('security');
      });

      it('should map security.jwt to security category', () => {
        expect(DOMAIN_CATEGORY_MAP['security.jwt']).toBe('security');
      });

      it('should map SamlRealm to security category', () => {
        expect(DOMAIN_CATEGORY_MAP.SamlRealm).toBe('security');
      });
    });

    describe('Repository Domains', () => {
      it('should map repository to repository category', () => {
        expect(DOMAIN_CATEGORY_MAP.repository).toBe('repository');
      });

      it('should map repository.component to repository category', () => {
        expect(DOMAIN_CATEGORY_MAP['repository.component']).toBe('repository');
      });

      it('should map repository.asset to repository category', () => {
        expect(DOMAIN_CATEGORY_MAP['repository.asset']).toBe('repository');
      });

      it('should map repository.component.tag to repository category', () => {
        expect(DOMAIN_CATEGORY_MAP['repository.component.tag']).toBe('repository');
      });

      it('should map blobstore to repository category', () => {
        expect(DOMAIN_CATEGORY_MAP.blobstore).toBe('repository');
      });
    });

    describe('Configuration Domains', () => {
      it('should map tasks to configuration category', () => {
        expect(DOMAIN_CATEGORY_MAP.tasks).toBe('configuration');
      });

      it('should map capability to configuration category', () => {
        expect(DOMAIN_CATEGORY_MAP.capability).toBe('configuration');
      });

      it('should map cleanupPolicy to configuration category', () => {
        expect(DOMAIN_CATEGORY_MAP.cleanupPolicy).toBe('configuration');
      });

      it('should map ContentSelector to configuration category', () => {
        expect(DOMAIN_CATEGORY_MAP.ContentSelector).toBe('configuration');
      });

      it('should map RoutingRule to configuration category', () => {
        expect(DOMAIN_CATEGORY_MAP.RoutingRule).toBe('configuration');
      });

      it('should map email to configuration category', () => {
        expect(DOMAIN_CATEGORY_MAP.email).toBe('configuration');
      });

      it('should map httpclient to configuration category', () => {
        expect(DOMAIN_CATEGORY_MAP.httpclient).toBe('configuration');
      });

      it('should map logging to configuration category', () => {
        expect(DOMAIN_CATEGORY_MAP.logging).toBe('configuration');
      });

      it('should map script to configuration category', () => {
        expect(DOMAIN_CATEGORY_MAP.script).toBe('configuration');
      });

      it('should map license to configuration category', () => {
        expect(DOMAIN_CATEGORY_MAP.license).toBe('configuration');
      });

      it('should map freeze to configuration category', () => {
        expect(DOMAIN_CATEGORY_MAP.freeze).toBe('configuration');
      });

      it('should map DataStore to configuration category', () => {
        expect(DOMAIN_CATEGORY_MAP.DataStore).toBe('configuration');
      });

      it('should map database-migration to configuration category', () => {
        expect(DOMAIN_CATEGORY_MAP['database-migration']).toBe('configuration');
      });

      it('should map userToken to configuration category', () => {
        expect(DOMAIN_CATEGORY_MAP.userToken).toBe('configuration');
      });

      it('should map userToken.admin to configuration category', () => {
        expect(DOMAIN_CATEGORY_MAP['userToken.admin']).toBe('configuration');
      });
    });

    describe('Protection Domains', () => {
      it('should map protection.config to protection category', () => {
        expect(DOMAIN_CATEGORY_MAP['protection.config']).toBe('protection');
      });

      it('should map malware.removal to protection category', () => {
        expect(DOMAIN_CATEGORY_MAP['malware.removal']).toBe('protection');
      });

      it('should map firewall.quarantine to protection category', () => {
        expect(DOMAIN_CATEGORY_MAP['firewall.quarantine']).toBe('protection');
      });
    });
  });

  describe('CATEGORY_COLORS', () => {
    it('should have blue color for security category', () => {
      expect(CATEGORY_COLORS.security).toBe('blue');
    });

    it('should have purple color for repository category', () => {
      expect(CATEGORY_COLORS.repository).toBe('purple');
    });

    it('should have gray color for configuration category', () => {
      expect(CATEGORY_COLORS.configuration).toBe('gray');
    });

    it('should have amber color for protection category', () => {
      expect(CATEGORY_COLORS.protection).toBe('amber');
    });

    it('should have colors for all categories', () => {
      const categories: AuditCategory[] = ['security', 'repository', 'configuration', 'protection'];
      categories.forEach((cat) => {
        expect(CATEGORY_COLORS[cat]).toBeDefined();
      });
    });
  });

  describe('CATEGORY_LABELS', () => {
    it('should have Security label', () => {
      expect(CATEGORY_LABELS.security).toBe('Security');
    });

    it('should have Repository label', () => {
      expect(CATEGORY_LABELS.repository).toBe('Repository');
    });

    it('should have Configuration label', () => {
      expect(CATEGORY_LABELS.configuration).toBe('Configuration');
    });

    it('should have Protection label', () => {
      expect(CATEGORY_LABELS.protection).toBe('Protection');
    });

    it('should have labels for all categories', () => {
      const categories: AuditCategory[] = ['security', 'repository', 'configuration', 'protection'];
      categories.forEach((cat) => {
        expect(CATEGORY_LABELS[cat]).toBeDefined();
        expect(typeof CATEGORY_LABELS[cat]).toBe('string');
      });
    });
  });

  describe('COMMON_EVENT_TYPES', () => {
    it('should include created event type', () => {
      expect(COMMON_EVENT_TYPES).toContain('created');
    });

    it('should include updated event type', () => {
      expect(COMMON_EVENT_TYPES).toContain('updated');
    });

    it('should include deleted event type', () => {
      expect(COMMON_EVENT_TYPES).toContain('deleted');
    });

    it('should include started event type', () => {
      expect(COMMON_EVENT_TYPES).toContain('started');
    });

    it('should include finished event type', () => {
      expect(COMMON_EVENT_TYPES).toContain('finished');
    });

    it('should include failed event type', () => {
      expect(COMMON_EVENT_TYPES).toContain('failed');
    });

    it('should include login event type', () => {
      expect(COMMON_EVENT_TYPES).toContain('login');
    });

    it('should include logout event type', () => {
      expect(COMMON_EVENT_TYPES).toContain('logout');
    });

    it('should have exactly 8 common event types', () => {
      expect(COMMON_EVENT_TYPES).toHaveLength(8);
    });
  });
});
