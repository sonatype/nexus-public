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

import { renderHook, act } from '@testing-library/react';
import { useRolesApi } from '../useRolesApi';
import { Role, NEXUS_SOURCE, formatRoleSourceDisplay } from '../types';

const mockGet = jest.fn();
const mockPost = jest.fn();
const mockPut = jest.fn();
const mockDelete = jest.fn();

// Mock the REST API from @sonatype/nexus-ui-plugin
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  restClient: {
    get: (...args: unknown[]) => mockGet(...args),
    post: (...args: unknown[]) => mockPost(...args),
    put: (...args: unknown[]) => mockPut(...args),
    delete: (...args: unknown[]) => mockDelete(...args),
  },
  parseApiError: jest.fn((err: unknown) => {
    const error = err as { response?: { status?: number; data?: { message?: string } }; message?: string };
    return {
      message: error?.response?.data?.message || error?.message || 'Error',
      status: error?.response?.status,
    };
  }),
}));

// REST API response format (different from ExtDirect)
const mockRestRoles = [
  {
    id: 'nx-admin',
    source: 'default',
    name: 'nx-admin',
    description: 'Administrator Role',
    readOnly: true,
    privileges: ['nx-all'],
    roles: [],
  },
  {
    id: 'nx-anonymous',
    source: 'default',
    name: 'nx-anonymous',
    description: 'Anonymous Role',
    readOnly: true,
    privileges: ['nx-search-read'],
    roles: [],
  },
];

// Expected UI format (converted from REST)
const mockRoles: Role[] = [
  {
    id: 'nx-admin',
    version: '1',
    source: 'Default', // Converted from 'default'
    name: 'nx-admin',
    description: 'Administrator Role',
    readOnly: true,
    privileges: ['nx-all'],
    roles: [],
  },
  {
    id: 'nx-anonymous',
    version: '1',
    source: 'Default', // Converted from 'default'
    name: 'nx-anonymous',
    description: 'Anonymous Role',
    readOnly: true,
    privileges: ['nx-search-read'],
    roles: [],
  },
];

const mockRestPrivileges = [
  { name: 'nx-all' },
  { name: 'nx-search-read' },
];

const mockPrivilegeRefs = [
  { id: 'nx-all', name: 'nx-all' },
  { id: 'nx-search-read', name: 'nx-search-read' },
];

const mockRoleRefs = [
  { id: 'nx-admin', name: 'nx-admin' },
  { id: 'nx-anonymous', name: 'nx-anonymous' },
];

const mockRestSources = [
  { id: 'LDAP', name: 'LDAP' },
];

describe('useRolesApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('fetchRoles', () => {
    it('should fetch roles successfully', async () => {
      mockGet.mockResolvedValueOnce(mockRestRoles);

      const { result } = renderHook(() => useRolesApi());

      let roles: Role[] = [];
      await act(async () => {
        roles = await result.current.fetchRoles();
      });

      expect(roles).toEqual(mockRoles);
      expect(mockGet).toHaveBeenCalledWith('/service/rest/v1/security/roles');
    });

    it('should handle empty response', async () => {
      mockGet.mockResolvedValueOnce([]);

      const { result } = renderHook(() => useRolesApi());

      let roles: Role[] = [];
      await act(async () => {
        roles = await result.current.fetchRoles();
      });

      expect(roles).toEqual([]);
    });

    it('should throw error on failure', async () => {
      mockGet.mockRejectedValueOnce(new Error('Network error'));

      const { result } = renderHook(() => useRolesApi());

      await expect(result.current.fetchRoles()).rejects.toThrow('Network error');
    });
  });

  describe('fetchRoleReferences', () => {
    it('should fetch role references successfully', async () => {
      mockGet.mockResolvedValueOnce(mockRestRoles);

      const { result } = renderHook(() => useRolesApi());

      let refs: { id: string; name: string }[] = [];
      await act(async () => {
        refs = await result.current.fetchRoleReferences();
      });

      expect(refs).toEqual(mockRoleRefs);
      expect(mockGet).toHaveBeenCalledWith('/service/rest/v1/security/roles');
    });
  });

  describe('fetchRoleSources', () => {
    it('should fetch role sources with Nexus as default', async () => {
      mockGet.mockResolvedValueOnce(mockRestSources);

      const { result } = renderHook(() => useRolesApi());

      let sources: { id: string; name: string }[] = [];
      await act(async () => {
        sources = await result.current.fetchRoleSources();
      });

      expect(sources).toContainEqual({ id: NEXUS_SOURCE, name: NEXUS_SOURCE });
      expect(sources).toContainEqual({ id: 'LDAP', name: 'LDAP' });
      expect(mockGet).toHaveBeenCalledWith('/service/rest/internal/ui/roles/sources');
    });

    it('should return only Nexus when no external sources', async () => {
      mockGet.mockResolvedValueOnce([]);

      const { result } = renderHook(() => useRolesApi());

      let sources: { id: string; name: string }[] = [];
      await act(async () => {
        sources = await result.current.fetchRoleSources();
      });

      expect(sources).toEqual([{ id: NEXUS_SOURCE, name: NEXUS_SOURCE }]);
    });
  });

  describe('fetchRolesFromSource', () => {
    it('should fetch roles from specific source', async () => {
      mockGet.mockResolvedValueOnce(mockRestRoles);

      const { result } = renderHook(() => useRolesApi());

      await act(async () => {
        await result.current.fetchRolesFromSource('Default');
      });

      // 'Default' should be converted to 'default' for REST API
      expect(mockGet).toHaveBeenCalledWith('/service/rest/v1/security/roles?source=default');
    });

    it('should pass through non-Nexus source unchanged', async () => {
      mockGet.mockResolvedValueOnce([]);

      const { result } = renderHook(() => useRolesApi());

      await act(async () => {
        await result.current.fetchRolesFromSource('LDAP');
      });

      expect(mockGet).toHaveBeenCalledWith('/service/rest/v1/security/roles?source=LDAP');
    });
  });

  describe('fetchPrivilegeReferences', () => {
    it('should fetch privilege references successfully', async () => {
      mockGet.mockResolvedValueOnce(mockRestPrivileges);

      const { result } = renderHook(() => useRolesApi());

      let refs: { id: string; name: string }[] = [];
      await act(async () => {
        refs = await result.current.fetchPrivilegeReferences();
      });

      expect(refs).toEqual(mockPrivilegeRefs);
      expect(mockGet).toHaveBeenCalledWith('/service/rest/v1/security/privileges');
    });
  });

  describe('findRole', () => {
    it('should find a role by ID', async () => {
      mockGet.mockResolvedValueOnce(mockRestRoles[0]);

      const { result } = renderHook(() => useRolesApi());

      let role: Role | null = null;
      await act(async () => {
        role = await result.current.findRole('nx-admin');
      });

      expect(role).toEqual(mockRoles[0]);
      expect(mockGet).toHaveBeenCalledWith('/service/rest/v1/security/roles/nx-admin');
    });

    it('should return null if role not found (404)', async () => {
      const notFoundError = { response: { status: 404, data: { message: 'Not found' } } };
      mockGet.mockRejectedValueOnce(notFoundError);

      const { result } = renderHook(() => useRolesApi());

      let role: Role | null = null;
      await act(async () => {
        role = await result.current.findRole('nonexistent');
      });

      expect(role).toBeNull();
    });

    it('should throw error on other failures', async () => {
      const serverError = { response: { status: 500, data: { message: 'Server error' } } };
      mockGet.mockRejectedValueOnce(serverError);

      const { result } = renderHook(() => useRolesApi());

      await expect(result.current.findRole('nx-admin')).rejects.toThrow('Server error');
    });
  });

  describe('createRole', () => {
    it('should create a role successfully', async () => {
      const newRole = {
        id: 'test-role',
        name: 'Test Role',
        description: 'Test Description',
        privileges: ['nx-search-read'],
        roles: [],
      };

      const createdRestRole = {
        ...newRole,
        source: 'default',
        readOnly: false,
      };

      mockPost.mockResolvedValueOnce(createdRestRole);

      const { result } = renderHook(() => useRolesApi());

      let createdRole: Role | null = null;
      await act(async () => {
        createdRole = await result.current.createRole(newRole);
      });

      expect(createdRole).toBeTruthy();
      expect(createdRole?.id).toBe('test-role');
      expect(createdRole?.source).toBe('Default'); // Converted from 'default'
      expect(mockPost).toHaveBeenCalledWith(
        '/service/rest/v1/security/roles',
        expect.objectContaining({ id: 'test-role', name: 'Test Role' })
      );
      expect(result.current.loading).toBe(false);
    });

    it('should set error on failure', async () => {
      mockPost.mockRejectedValueOnce(new Error('Creation failed'));

      const { result } = renderHook(() => useRolesApi());

      await act(async () => {
        try {
          await result.current.createRole({
            id: 'test',
            name: 'Test',
            description: '',
            privileges: [],
            roles: [],
          });
        } catch (e) {
          // Expected error
        }
      });

      expect(result.current.error).toBe('Creation failed');
    });
  });

  describe('updateRole', () => {
    it('should update a role successfully', async () => {
      const updatedRole = {
        id: 'test-role',
        name: 'Updated Role',
        description: 'Updated Description',
        privileges: ['nx-search-read'],
        roles: [],
        version: '1',
      };

      const updatedRestRole = {
        ...updatedRole,
        source: 'default',
        readOnly: false,
      };

      mockPut.mockResolvedValueOnce(updatedRestRole);

      const { result } = renderHook(() => useRolesApi());

      await act(async () => {
        await result.current.updateRole(updatedRole);
      });

      expect(mockPut).toHaveBeenCalledWith(
        '/service/rest/v1/security/roles/test-role',
        expect.objectContaining({ id: 'test-role', name: 'Updated Role' })
      );
    });

    it('should set error on update failure', async () => {
      mockPut.mockRejectedValueOnce(new Error('Update failed'));

      const { result } = renderHook(() => useRolesApi());

      await act(async () => {
        try {
          await result.current.updateRole({
            id: 'test-role',
            name: 'Test',
            description: '',
            privileges: [],
            roles: [],
          });
        } catch (e) {
          // Expected error
        }
      });

      expect(result.current.error).toBe('Update failed');
    });
  });

  describe('deleteRole', () => {
    it('should delete a role successfully', async () => {
      mockDelete.mockResolvedValueOnce(undefined);

      const { result } = renderHook(() => useRolesApi());

      await act(async () => {
        await result.current.deleteRole('test-role');
      });

      expect(mockDelete).toHaveBeenCalledWith('/service/rest/v1/security/roles/test-role');
    });

    it('should set error on deletion failure', async () => {
      mockDelete.mockRejectedValueOnce(new Error('Deletion failed'));

      const { result } = renderHook(() => useRolesApi());

      await act(async () => {
        try {
          await result.current.deleteRole('test-role');
        } catch (e) {
          // Expected error
        }
      });

      expect(result.current.error).toBe('Deletion failed');
    });
  });

  describe('setError', () => {
    it('should allow setting and clearing error', () => {
      const { result } = renderHook(() => useRolesApi());

      act(() => {
        result.current.setError('Test error');
      });

      expect(result.current.error).toBe('Test error');

      act(() => {
        result.current.setError(null);
      });

      expect(result.current.error).toBeNull();
    });
  });

  describe('formatRoleSourceDisplay', () => {
    it('returns "Default" for "default" source', () => {
      expect(formatRoleSourceDisplay('default')).toBe('Default');
    });

    it('returns "Default" for empty string source', () => {
      expect(formatRoleSourceDisplay('')).toBe('Default');
    });

    it('returns "Default" for "Nexus" source', () => {
      expect(formatRoleSourceDisplay('Nexus')).toBe('Default');
    });

    it('returns the source name for external sources', () => {
      expect(formatRoleSourceDisplay('LDAP')).toBe('LDAP');
      expect(formatRoleSourceDisplay('Crowd')).toBe('Crowd');
    });
  });
});
