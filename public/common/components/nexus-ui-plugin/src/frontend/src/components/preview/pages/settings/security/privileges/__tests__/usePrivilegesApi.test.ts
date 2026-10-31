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
import { usePrivilegesApi } from '../usePrivilegesApi';
import { Privilege, PrivilegeType, PRIVILEGE_TYPES } from '../types';

// Create mock functions
const mockRestClientGet = jest.fn();
const mockRestClientPost = jest.fn();
const mockRestClientPut = jest.fn();
const mockRestClientDelete = jest.fn();

// Mock the REST API utilities
jest.mock('../../../../../../../interface/api', () => ({
  restClient: {
    get: (...args) => mockRestClientGet(...args),
    post: (...args) => mockRestClientPost(...args),
    put: (...args) => mockRestClientPut(...args),
    delete: (...args) => mockRestClientDelete(...args),
  },
  ENDPOINTS: {
    PRIVILEGES: '/service/rest/v1/security/privileges',
  },
  urlBuilder: {
    privileges: {
      get: (name) => `/service/rest/v1/security/privileges/${name}`,
      createByType: (type) => `/service/rest/v1/security/privileges/${type}`,
      update: (type, name) => `/service/rest/v1/security/privileges/${type}/${name}`,
      delete: (name) => `/service/rest/v1/security/privileges/${name}`,
    },
  },
  parseApiError: (err) => ({
    message: err?.response?.data?.message || err?.message || 'Unknown error',
    status: err?.response?.status || 0,
  }),
}));

// REST API response format (different from ExtDirect)
const mockRestPrivileges = [
  {
    name: 'nx-all',
    description: 'All permissions',
    type: 'wildcard',
    readOnly: true,
    pattern: 'nexus:*',
  },
  {
    name: 'nx-search-read',
    description: 'Search read permission',
    type: 'application',
    readOnly: true,
    domain: 'search',
    actions: ['READ'],
  },
];

const mockPrivilegeTypes: PrivilegeType[] = [
  { id: 'application', name: 'Application', formFields: null },
  { id: 'wildcard', name: 'Wildcard', formFields: null },
  { id: 'repository-view', name: 'Repository View', formFields: null },
];

describe('usePrivilegesApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('fetchPrivileges', () => {
    it('should fetch privileges successfully', async () => {
      mockRestClientGet.mockResolvedValueOnce(mockRestPrivileges);

      const { result } = renderHook(() => usePrivilegesApi());

      let response: { data: Privilege[]; total: number } = { data: [], total: 0 };
      await act(async () => {
        response = await result.current.fetchPrivileges();
      });

      expect(response.data).toHaveLength(2);
      expect(response.data[0].name).toBe('nx-all');
      expect(response.data[1].name).toBe('nx-search-read');
      expect(response.total).toBe(2);
      expect(mockRestClientGet).toHaveBeenCalledWith('/service/rest/v1/security/privileges');
    });

    it('should handle empty response', async () => {
      mockRestClientGet.mockResolvedValueOnce([]);

      const { result } = renderHook(() => usePrivilegesApi());

      let response: { data: Privilege[]; total: number } = { data: [], total: 0 };
      await act(async () => {
        response = await result.current.fetchPrivileges();
      });

      expect(response.data).toEqual([]);
      expect(response.total).toBe(0);
    });

    it('should apply client-side filtering', async () => {
      mockRestClientGet.mockResolvedValueOnce(mockRestPrivileges);

      const { result } = renderHook(() => usePrivilegesApi());

      let response: { data: Privilege[]; total: number } = { data: [], total: 0 };
      await act(async () => {
        response = await result.current.fetchPrivileges('search');
      });

      // Should only return nx-search-read since filter matches
      expect(response.data).toHaveLength(1);
      expect(response.data[0].name).toBe('nx-search-read');
      expect(response.total).toBe(1);
    });

    it('should apply client-side sorting', async () => {
      mockRestClientGet.mockResolvedValueOnce(mockRestPrivileges);

      const { result } = renderHook(() => usePrivilegesApi());

      let response: { data: Privilege[]; total: number } = { data: [], total: 0 };
      await act(async () => {
        response = await result.current.fetchPrivileges('', 'name', 'DESC');
      });

      // DESC sort should put nx-search-read first (s > a alphabetically)
      expect(response.data[0].name).toBe('nx-search-read');
      expect(response.data[1].name).toBe('nx-all');
    });

    it('should apply client-side pagination', async () => {
      mockRestClientGet.mockResolvedValueOnce(mockRestPrivileges);

      const { result } = renderHook(() => usePrivilegesApi());

      let response: { data: Privilege[]; total: number } = { data: [], total: 0 };
      await act(async () => {
        response = await result.current.fetchPrivileges('', 'name', 'ASC', 0, 1);
      });

      expect(response.data).toHaveLength(1);
      expect(response.total).toBe(2); // Total is still 2, but only 1 returned due to limit
    });
  });

  describe('fetchPrivilegeTypes', () => {
    it('should fetch privilege types successfully', async () => {
      mockRestClientGet.mockResolvedValueOnce(mockPrivilegeTypes);

      const { result } = renderHook(() => usePrivilegesApi());

      let types: PrivilegeType[] = [];
      await act(async () => {
        types = await result.current.fetchPrivilegeTypes();
      });

      expect(types).toEqual(mockPrivilegeTypes);
      expect(mockRestClientGet).toHaveBeenCalledWith('/service/rest/internal/ui/privileges/types');
    });
  });

  describe('fetchPrivilegeReferences', () => {
    it('should fetch privilege references successfully', async () => {
      mockRestClientGet.mockResolvedValueOnce(mockRestPrivileges);

      const { result } = renderHook(() => usePrivilegesApi());

      let refs: { id: string; name: string }[] = [];
      await act(async () => {
        refs = await result.current.fetchPrivilegeReferences();
      });

      expect(refs).toEqual([
        { id: 'nx-all', name: 'nx-all' },
        { id: 'nx-search-read', name: 'nx-search-read' },
      ]);
      expect(mockRestClientGet).toHaveBeenCalledWith('/service/rest/v1/security/privileges');
    });
  });

  describe('findPrivilege', () => {
    it('should find a privilege by ID', async () => {
      mockRestClientGet.mockResolvedValueOnce(mockRestPrivileges[0]);

      const { result } = renderHook(() => usePrivilegesApi());

      let privilege: Privilege | null = null;
      await act(async () => {
        privilege = await result.current.findPrivilege('nx-all');
      });

      expect(privilege?.name).toBe('nx-all');
      expect(privilege?.type).toBe('wildcard');
      expect(mockRestClientGet).toHaveBeenCalledWith('/service/rest/v1/security/privileges/nx-all');
    });

    it('should return null if privilege not found', async () => {
      const notFoundError = { response: { status: 404, data: { message: 'Not found' } } };
      mockRestClientGet.mockRejectedValueOnce(notFoundError);

      const { result } = renderHook(() => usePrivilegesApi());

      let privilege: Privilege | null = null;
      await act(async () => {
        privilege = await result.current.findPrivilege('nonexistent');
      });

      expect(privilege).toBeNull();
    });
  });

  describe('createPrivilege', () => {
    it('should create a privilege successfully', async () => {
      const newPrivilege = {
        name: 'test-privilege',
        description: 'Test Description',
        type: PRIVILEGE_TYPES.WILDCARD,
        properties: { pattern: 'test:*' },
      };

      const createdRest = {
        name: 'test-privilege',
        description: 'Test Description',
        type: 'wildcard',
        readOnly: false,
        pattern: 'test:*',
      };

      mockRestClientPost.mockResolvedValueOnce(undefined); // POST returns nothing
      mockRestClientGet.mockResolvedValueOnce(createdRest); // GET fetches the created privilege

      const { result } = renderHook(() => usePrivilegesApi());

      await act(async () => {
        await result.current.createPrivilege(newPrivilege);
      });

      expect(mockRestClientPost).toHaveBeenCalledWith(
        '/service/rest/v1/security/privileges/wildcard',
        expect.objectContaining({ name: 'test-privilege', pattern: 'test:*' })
      );
      expect(result.current.loading).toBe(false);
    });

    it('should set error on failure', async () => {
      mockRestClientPost.mockRejectedValueOnce({ message: 'Creation failed' });

      const { result } = renderHook(() => usePrivilegesApi());

      await act(async () => {
        try {
          await result.current.createPrivilege({
            name: 'test',
            description: '',
            type: 'wildcard',
            properties: {},
          });
        } catch (_e) {
          // Expected error
        }
      });

      expect(result.current.error).toBe('Creation failed');
    });
  });

  describe('updatePrivilege', () => {
    it('should update a privilege successfully', async () => {
      const updatedPrivilege = {
        id: 'test-privilege',
        name: 'test-privilege',
        description: 'Updated Description',
        type: PRIVILEGE_TYPES.WILDCARD,
        properties: { pattern: 'test:*' },
        version: '1',
      };

      const updatedRest = {
        name: 'test-privilege',
        description: 'Updated Description',
        type: 'wildcard',
        readOnly: false,
        pattern: 'test:*',
      };

      mockRestClientPut.mockResolvedValueOnce(undefined); // PUT returns nothing
      mockRestClientGet.mockResolvedValueOnce(updatedRest); // GET fetches the updated privilege

      const { result } = renderHook(() => usePrivilegesApi());

      await act(async () => {
        await result.current.updatePrivilege(updatedPrivilege);
      });

      expect(mockRestClientPut).toHaveBeenCalledWith(
        '/service/rest/v1/security/privileges/wildcard/test-privilege',
        expect.objectContaining({ name: 'test-privilege', description: 'Updated Description' })
      );
    });
  });

  describe('deletePrivilege', () => {
    it('should delete a privilege successfully', async () => {
      mockRestClientDelete.mockResolvedValueOnce(undefined);

      const { result } = renderHook(() => usePrivilegesApi());

      await act(async () => {
        await result.current.deletePrivilege('test-privilege');
      });

      expect(mockRestClientDelete).toHaveBeenCalledWith('/service/rest/v1/security/privileges/test-privilege');
    });

    it('should set error on deletion failure', async () => {
      mockRestClientDelete.mockRejectedValueOnce({ message: 'Deletion failed' });

      const { result } = renderHook(() => usePrivilegesApi());

      await act(async () => {
        try {
          await result.current.deletePrivilege('test-privilege');
        } catch (_e) {
          // Expected error
        }
      });

      expect(result.current.error).toBe('Deletion failed');
    });
  });

  describe('setError', () => {
    it('should allow setting and clearing error', () => {
      const { result } = renderHook(() => usePrivilegesApi());

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
});
