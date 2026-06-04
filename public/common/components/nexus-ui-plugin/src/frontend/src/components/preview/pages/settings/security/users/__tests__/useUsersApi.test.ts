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

import { renderHook, act, waitFor } from '@testing-library/react';
import { useUsersApi } from '../useUsersApi';

const mockGet = jest.fn();
const mockPost = jest.fn();
const mockPut = jest.fn();
const mockDelete = jest.fn();

// Mock the REST API from @/utils/api
jest.mock('../../../../../../../interface/api', () => ({
  ...jest.requireActual('../../../../../../../interface/api'),
  restClient: {
    get: (...args: unknown[]) => mockGet(...args),
    post: (...args: unknown[]) => mockPost(...args),
    put: (...args: unknown[]) => mockPut(...args),
    delete: (...args: unknown[]) => mockDelete(...args),
  },
  parseApiError: jest.fn((err) => ({
    message: err?.response?.data?.message || err?.message || 'An error occurred',
  })),
  urlBuilder: {
    ...jest.requireActual('../../../../../../../interface/api').urlBuilder,
    users: {
      ...jest.requireActual('../../../../../../../interface/api').urlBuilder.users,
      invite: () => '/service/rest/v1/security/users/invite',
    },
  },
}));

describe('useUsersApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('fetchSources', () => {
    it('fetches user sources successfully', async () => {
      const mockSources = [
        { id: 'default', name: 'Local' },
        { id: 'LDAP', name: 'LDAP Server' },
      ];

      mockGet.mockResolvedValue(mockSources);

      const { result } = renderHook(() => useUsersApi());

      let sources;
      await act(async () => {
        sources = await result.current.fetchSources();
      });

      expect(sources).toEqual(mockSources);
      expect(mockGet).toHaveBeenCalledWith('/service/rest/v1/security/user-sources');
    });

    it('handles error when fetching sources fails', async () => {
      mockGet.mockRejectedValue(new Error('Network error'));

      const { result } = renderHook(() => useUsersApi());

      await expect(result.current.fetchSources()).rejects.toThrow('Network error');
    });

    it('returns empty array when response is not an array', async () => {
      mockGet.mockResolvedValue(null);

      const { result } = renderHook(() => useUsersApi());

      let sources;
      await act(async () => {
        sources = await result.current.fetchSources();
      });

      expect(sources).toEqual([]);
    });
  });

  describe('fetchUsers', () => {
    it('fetches users with default filter', async () => {
      const mockUsers = [
        { userId: 'admin', firstName: 'Admin', lastName: 'User', source: 'default', status: 'active', roles: [] },
        { userId: 'developer', firstName: 'Dev', lastName: 'User', source: 'default', status: 'active', roles: [] },
      ];

      mockGet.mockResolvedValue(mockUsers);

      const { result } = renderHook(() => useUsersApi());

      let users;
      await act(async () => {
        users = await result.current.fetchUsers();
      });

      expect(users).toHaveLength(2);
      expect(mockGet).toHaveBeenCalled();
    });

    it('fetches users with custom filter', async () => {
      const mockUsers = [{ userId: 'admin', firstName: 'Admin', lastName: 'User', source: 'LDAP', status: 'active', roles: [] }];

      mockGet.mockResolvedValue(mockUsers);

      const { result } = renderHook(() => useUsersApi());

      await act(async () => {
        await result.current.fetchUsers('admin', 'LDAP');
      });

      expect(mockGet).toHaveBeenCalled();
    });
  });

  describe('fetchUser', () => {
    it('fetches single user by ID', async () => {
      const mockUser = {
        userId: 'admin',
        source: 'default',
        firstName: 'Admin',
        lastName: 'User',
        emailAddress: 'admin@example.com',
        status: 'active',
        roles: [],
      };

      mockGet.mockResolvedValue([mockUser]);

      const { result } = renderHook(() => useUsersApi());

      let user;
      await act(async () => {
        user = await result.current.fetchUser('admin', 'default');
      });

      expect(user).toBeTruthy();
      expect(user?.userId).toBe('admin');
    });

    it('returns null when user not found', async () => {
      mockGet.mockResolvedValue([]);

      const { result } = renderHook(() => useUsersApi());

      let user;
      await act(async () => {
        user = await result.current.fetchUser('nonexistent', 'default');
      });

      expect(user).toBeNull();
    });
  });

  describe('createUser', () => {
    it('creates user successfully', async () => {
      const newUser = {
        userId: 'newuser',
        firstName: 'New',
        lastName: 'User',
        emailAddress: 'new@example.com',
        password: 'password123',
        status: true,
        roles: ['nx-admin'],
      };

      mockPost.mockResolvedValue(newUser);

      const { result } = renderHook(() => useUsersApi());

      await act(async () => {
        await result.current.createUser(newUser);
      });

      expect(mockPost).toHaveBeenCalledWith(
        '/service/rest/v1/security/users',
        expect.objectContaining({
          userId: 'newuser',
          firstName: 'New',
          lastName: 'User',
          emailAddress: 'new@example.com',
          password: 'password123',
          status: 'active',
          roles: ['nx-admin'],
        })
      );
    });

    it('throws error on create failure', async () => {
      mockPost.mockRejectedValue({
        response: { data: { message: 'User already exists' } },
      });

      const { result } = renderHook(() => useUsersApi());

      await expect(
        result.current.createUser({
          userId: 'admin',
          firstName: 'Admin',
          lastName: 'User',
          emailAddress: 'admin@example.com',
          status: true,
          roles: ['nx-admin'],
        })
      ).rejects.toThrow('User already exists');
    });
  });

  describe('updateUser', () => {
    it('updates local user successfully', async () => {
      const updatedUser = {
        userId: 'admin',
        firstName: 'Updated',
        lastName: 'Admin',
        emailAddress: 'admin@example.com',
        status: true,
        roles: ['nx-admin'],
      };

      mockPut.mockResolvedValue(updatedUser);

      const { result } = renderHook(() => useUsersApi());

      await act(async () => {
        await result.current.updateUser('admin', updatedUser, 'default');
      });

      expect(mockPut).toHaveBeenCalledWith(
        '/service/rest/v1/security/users/admin',
        expect.objectContaining({
          firstName: 'Updated',
          lastName: 'Admin',
          source: 'default',
        })
      );
    });

    it('includes source field in local user PUT payload', async () => {
      const userData = {
        userId: 'testuser',
        firstName: 'Test',
        lastName: 'User',
        emailAddress: 'test@example.com',
        status: true,
        roles: ['nx-anonymous'],
      };

      mockPut.mockResolvedValue(userData);

      const { result } = renderHook(() => useUsersApi());

      await act(async () => {
        await result.current.updateUser('testuser', userData, 'default');
      });

      const putPayload = mockPut.mock.calls[0][1];
      expect(putPayload).toHaveProperty('source', 'default');
    });

    it('updates external user role mappings via REST', async () => {
      const updatedUser = {
        userId: 'ldapuser',
        firstName: 'LDAP',
        lastName: 'User',
        emailAddress: 'ldap@example.com',
        status: true,
        roles: ['nx-admin', 'nx-developer'],
      };

      const restResponse = {
        userId: 'ldapuser',
        firstName: 'LDAP',
        lastName: 'User',
        emailAddress: 'ldap@example.com',
        status: 'active',
        roles: ['nx-admin', 'nx-developer'],
        source: 'LDAP',
      };

      mockPut.mockResolvedValue(restResponse);

      const { result } = renderHook(() => useUsersApi());

      await act(async () => {
        await result.current.updateUser('ldapuser', updatedUser, 'LDAP');
      });

      expect(mockPut).toHaveBeenCalledWith(
        expect.stringContaining('ldapuser'),
        expect.objectContaining({
          userId: 'ldapuser',
          source: 'LDAP',
          roles: ['nx-admin', 'nx-developer'],
        })
      );
    });
  });

  describe('deleteUser', () => {
    it('deletes user successfully', async () => {
      mockDelete.mockResolvedValue(undefined);

      const { result } = renderHook(() => useUsersApi());

      await act(async () => {
        await result.current.deleteUser('testuser');
      });

      expect(mockDelete).toHaveBeenCalledWith('/service/rest/v1/security/users/testuser');
    });

    it('throws error on delete failure', async () => {
      mockDelete.mockRejectedValue({
        response: { data: { message: 'Cannot delete admin user' } },
      });

      const { result } = renderHook(() => useUsersApi());

      await expect(result.current.deleteUser('admin')).rejects.toThrow(
        'Cannot delete admin user'
      );
    });
  });

  describe('changePassword', () => {
    it('changes password successfully', async () => {
      mockPut.mockResolvedValue(undefined);

      const { result } = renderHook(() => useUsersApi());

      await act(async () => {
        await result.current.changePassword('admin', 'newpassword123');
      });

      expect(mockPut).toHaveBeenCalledWith(
        '/service/rest/v1/security/users/admin/change-password',
        'newpassword123',
        { headers: { 'Content-Type': 'text/plain' } }
      );
    });
  });

  describe('resetUserToken', () => {
    it('resets user token successfully', async () => {
      mockDelete.mockResolvedValue(undefined);

      const { result } = renderHook(() => useUsersApi());

      await act(async () => {
        await result.current.resetUserToken('admin', 'default');
      });

      expect(mockDelete).toHaveBeenCalledWith(
        '/service/rest/v1/security/users/admin/default/user-token-reset'
      );
    });
  });

  describe('inviteUser', () => {
    it('posts to invite endpoint with correct payload', async () => {
      mockPost.mockResolvedValue(undefined);

      const { result } = renderHook(() => useUsersApi());

      await result.current.inviteUser({
        firstName: 'John',
        lastName: 'Smith',
        email: 'jsmith@example.com',
      });

      expect(mockPost).toHaveBeenCalledWith(
        '/service/rest/v1/security/users/invite',
        { firstName: 'John', lastName: 'Smith', email: 'jsmith@example.com' }
      );
    });

    it('sets loading state during invite', async () => {
      let resolvePost!: () => void;
      mockPost.mockReturnValue(new Promise((resolve) => { resolvePost = resolve; }));

      const { result } = renderHook(() => useUsersApi());

      result.current.inviteUser({ firstName: 'John', lastName: 'Smith', email: 'jsmith@example.com' });

      await waitFor(() => expect(result.current.loading).toBe(true));
      resolvePost();
      await waitFor(() => expect(result.current.loading).toBe(false));
    });

    it('throws and sets error on invite failure', async () => {
      mockPost.mockRejectedValue({
        response: { data: { message: 'User already exists' } },
      });

      const { result } = renderHook(() => useUsersApi());

      await expect(
        result.current.inviteUser({ firstName: 'John', lastName: 'Smith', email: 'jsmith@example.com' })
      ).rejects.toThrow('User already exists');

      await waitFor(() => expect(result.current.error).toBe('User already exists'));
    });
  });
});
