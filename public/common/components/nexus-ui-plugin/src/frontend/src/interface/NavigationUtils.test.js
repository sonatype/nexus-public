/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import { isVisible } from './NavigationUtils';

describe('NavigationUtils', () => {
  let mockNX;

  beforeEach(() => {
    // Mock the global NX object
    mockNX = {
      app: {
        Application: {
          bundleActive: jest.fn(() => true),
        },
      },
      State: {
        getValue: jest.fn((key, defaultValue) => {
          if (key === 'browseableformats') {
            return ['maven2', 'npm', 'docker'];
          }
          return defaultValue;
        }),
        getEdition: jest.fn(() => 'PRO'),
      },
      Permissions: {
        permissions: {},
        check: jest.fn((permission) => {
          return mockNX.Permissions.permissions[permission] === true;
        }),
      },
      Security: {
        hasUser: jest.fn(() => true),
      },
    };
    global.NX = mockNX;
  });

  afterEach(() => {
    delete global.NX;
    jest.clearAllMocks();
  });

  describe('permissionPrefix', () => {
    it('should return true when user has permission matching the prefix', () => {
      mockNX.Permissions.permissions = {
        'nexus:repository-view:maven2:my-repo:browse': true,
        'nexus:repository-view:npm:npm-proxy:read': true,
      };

      const result = isVisible({
        permissionPrefix: 'nexus:repository-view',
      });

      expect(result).toBe(true);
    });

    it('should return false when user has no permission matching the prefix', () => {
      mockNX.Permissions.permissions = {
        'nexus:selectors:read': true,
        'nexus:admin:read': true,
      };

      const result = isVisible({
        permissionPrefix: 'nexus:repository-view',
      });

      expect(result).toBe(false);
    });

    it('should return false when user has permission with similar but non-matching prefix', () => {
      mockNX.Permissions.permissions = {
        'nexus:repository-admin:maven2:my-repo:browse': true,
      };

      const result = isVisible({
        permissionPrefix: 'nexus:repository-view',
      });

      expect(result).toBe(false);
    });
  });

  describe('permissionPrefixes (NEXUS-50552 fix)', () => {
    it('should return true when user has permission matching the first prefix', () => {
      mockNX.Permissions.permissions = {
        'nexus:repository-view:maven2:my-repo:browse': true,
      };

      const result = isVisible({
        permissionPrefixes: ['nexus:repository-view', 'nexus:repository-content-selector'],
      });

      expect(result).toBe(true);
    });

    it('should return true when user has permission matching the second prefix', () => {
      mockNX.Permissions.permissions = {
        'nexus:repository-content-selector:my-selector:maven2:my-repo:browse': true,
      };

      const result = isVisible({
        permissionPrefixes: ['nexus:repository-view', 'nexus:repository-content-selector'],
      });

      expect(result).toBe(true);
    });

    it('should return true when user has permissions matching both prefixes', () => {
      mockNX.Permissions.permissions = {
        'nexus:repository-view:maven2:my-repo:browse': true,
        'nexus:repository-content-selector:my-selector:maven2:another-repo:browse': true,
      };

      const result = isVisible({
        permissionPrefixes: ['nexus:repository-view', 'nexus:repository-content-selector'],
      });

      expect(result).toBe(true);
    });

    it('should return false when user has no permissions matching any prefix', () => {
      mockNX.Permissions.permissions = {
        'nexus:selectors:read': true,
        'nexus:admin:read': true,
      };

      const result = isVisible({
        permissionPrefixes: ['nexus:repository-view', 'nexus:repository-content-selector'],
      });

      expect(result).toBe(false);
    });

    it('should return false when permissionPrefixes is an empty array', () => {
      mockNX.Permissions.permissions = {
        'nexus:repository-view:maven2:my-repo:browse': true,
      };

      const result = isVisible({
        permissionPrefixes: [],
      });

      expect(result).toBe(false);
    });

    it('should return true when user has content selector permission with browse action', () => {
      // Test case specifically for NEXUS-50552: Content Selector privileges with browse action
      mockNX.Permissions.permissions = {
        'nexus:repository-content-selector:my-content-selector:maven2:maven-central:browse': true,
      };

      const result = isVisible({
        permissionPrefixes: ['nexus:repository-view', 'nexus:repository-content-selector'],
      });

      expect(result).toBe(true);
    });

    it('should return true when user has content selector permission with read action', () => {
      mockNX.Permissions.permissions = {
        'nexus:repository-content-selector:my-content-selector:npm:npm-proxy:read': true,
      };

      const result = isVisible({
        permissionPrefixes: ['nexus:repository-view', 'nexus:repository-content-selector'],
      });

      expect(result).toBe(true);
    });
  });

  describe('Browse route visibility requirements (NEXUS-50552)', () => {
    it('should show Browse menu when user has repository-view permission', () => {
      mockNX.Permissions.permissions = {
        'nexus:repository-view:maven2:my-repo:browse': true,
      };

      // Simulate the actual visibility requirements from browseRoutes.js
      const result = isVisible({
        anonymousAccessOrHasUser: true,
        permissionPrefixes: ['nexus:repository-view', 'nexus:repository-content-selector'],
        statesEnabled: [
          {
            key: 'browseableformats',
            defaultValue: [],
          },
        ],
      });

      expect(result).toBe(true);
    });

    it('should show Browse menu when user has content-selector permission', () => {
      mockNX.Permissions.permissions = {
        'nexus:repository-content-selector:my-selector:maven2:my-repo:browse': true,
      };

      // Simulate the actual visibility requirements from browseRoutes.js
      const result = isVisible({
        anonymousAccessOrHasUser: true,
        permissionPrefixes: ['nexus:repository-view', 'nexus:repository-content-selector'],
        statesEnabled: [
          {
            key: 'browseableformats',
            defaultValue: [],
          },
        ],
      });

      expect(result).toBe(true);
    });

    it('should NOT show Browse menu when user has no browse-related permissions', () => {
      mockNX.Permissions.permissions = {
        'nexus:selectors:read': true,
        'nexus:admin:read': true,
      };

      // Simulate the actual visibility requirements from browseRoutes.js
      const result = isVisible({
        anonymousAccessOrHasUser: true,
        permissionPrefixes: ['nexus:repository-view', 'nexus:repository-content-selector'],
        statesEnabled: [
          {
            key: 'browseableformats',
            defaultValue: [],
          },
        ],
      });

      expect(result).toBe(false);
    });
  });

  describe('backward compatibility', () => {
    it('should work when both permissionPrefix and permissionPrefixes are absent', () => {
      mockNX.Permissions.permissions = {
        'nexus:repository-view:maven2:my-repo:browse': true,
      };

      const result = isVisible({
        // No permission checks
      });

      expect(result).toBe(true);
    });

    it('should still support single permissionPrefix for existing routes', () => {
      mockNX.Permissions.permissions = {
        'nexus:repository-admin:maven2:my-repo:read': true,
      };

      const result = isVisible({
        permissionPrefix: 'nexus:repository-admin',
      });

      expect(result).toBe(true);
    });
  });

  describe('edge cases', () => {
    it('should handle missing NX.Permissions gracefully', () => {
      delete global.NX.Permissions;

      const result = isVisible({
        permissionPrefixes: ['nexus:repository-view'],
      });

      expect(result).toBe(false);
    });

    it('should handle missing NX.Permissions.permissions gracefully', () => {
      delete global.NX.Permissions.permissions;

      const result = isVisible({
        permissionPrefixes: ['nexus:repository-view'],
      });

      expect(result).toBe(false);
    });

    it('should handle empty permissions object', () => {
      mockNX.Permissions.permissions = {};

      const result = isVisible({
        permissionPrefixes: ['nexus:repository-view', 'nexus:repository-content-selector'],
      });

      expect(result).toBe(false);
    });

    it('should handle permissions with false values', () => {
      mockNX.Permissions.permissions = {
        'nexus:repository-view:maven2:my-repo:browse': false,
        'nexus:repository-content-selector:selector:maven2:repo:browse': false,
      };

      const result = isVisible({
        permissionPrefixes: ['nexus:repository-view', 'nexus:repository-content-selector'],
      });

      expect(result).toBe(false);
    });
  });
});
