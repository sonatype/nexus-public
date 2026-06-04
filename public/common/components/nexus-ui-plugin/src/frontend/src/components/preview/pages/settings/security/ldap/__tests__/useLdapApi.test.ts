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

// Mock the REST API from @/utils/api directly
jest.mock('../../../../../../../interface/api', () => ({
  ...jest.requireActual('../../../../../../../interface/api'),
  restClient: {
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
    delete: jest.fn(),
  },
  parseApiError: jest.fn((err: any) => ({
    message: err?.response?.data?.message || err?.message || 'Error',
    status: err?.response?.status,
  })),
}));

import { useLdapApi } from '../useLdapApi';
import { LdapServer } from '../types';
import { restClient, urlBuilder, parseApiError } from '../../../../../../../interface/api';

// Get mock references
const mockRestClient = restClient as jest.Mocked<typeof restClient>;
const mockUrlBuilder = urlBuilder as jest.Mocked<typeof urlBuilder>;
const mockParseApiError = parseApiError as jest.MockedFunction<typeof parseApiError>;

describe('useLdapApi', () => {
  // REST API response format (different field names from UI format)
  const mockRestServer = {
    id: 'server1',
    order: 1,
    name: 'Test LDAP',
    protocol: 'ldap' as const,
    useTrustStore: false,
    host: 'ldap.example.com',
    port: 389,
    searchBase: 'dc=example,dc=com',
    authScheme: 'SIMPLE', // REST uses UPPERCASE
    connectionTimeoutSeconds: 30, // REST field name
    connectionRetryDelaySeconds: 300, // REST field name
    maxIncidentsCount: 3,
    userSubtree: false,
    userObjectClass: 'inetOrgPerson',
    userIdAttribute: 'uid',
    userRealNameAttribute: 'cn',
    userEmailAddressAttribute: 'mail',
    ldapGroupsAsRoles: false,
    groupSubtree: false,
  };

  // UI format (what the hook returns)
  const mockUiServer: LdapServer = {
    id: 'server1',
    order: 1,
    name: 'Test LDAP',
    protocol: 'ldap',
    useTrustStore: false,
    host: 'ldap.example.com',
    port: 389,
    searchBase: 'dc=example,dc=com',
    authScheme: 'simple', // UI uses lowercase
    connectionTimeout: 30, // UI field name
    connectionRetryDelay: 300, // UI field name
    maxIncidentsCount: 3,
    userSubtree: false,
    userObjectClass: 'inetOrgPerson',
    userIdAttribute: 'uid',
    userRealNameAttribute: 'cn',
    userEmailAddressAttribute: 'mail',
    ldapGroupsAsRoles: false,
    groupSubtree: false,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('initial state', () => {
    it('returns initial state with loading false', () => {
      const { result } = renderHook(() => useLdapApi());

      expect(result.current.loading).toBe(false);
      expect(result.current.error).toBe(null);
    });
  });

  describe('fetchServers', () => {
    it('fetches servers successfully using REST API', async () => {
      mockRestClient.get.mockResolvedValueOnce([mockRestServer]);

      const { result } = renderHook(() => useLdapApi());

      let servers: LdapServer[] = [];
      await act(async () => {
        servers = await result.current.fetchServers();
      });

      expect(servers).toHaveLength(1);
      expect(servers[0].name).toBe('Test LDAP');
      expect(servers[0].authScheme).toBe('simple'); // Transformed from SIMPLE
      expect(servers[0].connectionTimeout).toBe(30); // Transformed from connectionTimeoutSeconds
      expect(mockRestClient.get).toHaveBeenCalledWith('/service/rest/v1/security/ldap');
    });

    it('does not toggle loading during fetch', async () => {
      mockRestClient.get.mockResolvedValueOnce([]);

      const { result } = renderHook(() => useLdapApi());

      const promise = result.current.fetchServers();
      expect(result.current.loading).toBe(false);
      await promise;
    });

    it('throws error on fetch failure', async () => {
      const error = new Error('Network error');
      mockRestClient.get.mockRejectedValueOnce(error);

      const { result } = renderHook(() => useLdapApi());

      await expect(result.current.fetchServers()).rejects.toThrow('Network error');
    });
  });

  describe('fetchServer', () => {
    it('fetches a single server by name using REST API', async () => {
      mockRestClient.get.mockResolvedValueOnce(mockRestServer);

      const { result } = renderHook(() => useLdapApi());

      let server: LdapServer | null = null;
      await act(async () => {
        server = await result.current.fetchServer('Test LDAP');
      });

      expect(server).not.toBeNull();
      expect(server!.name).toBe('Test LDAP');
      expect(mockRestClient.get).toHaveBeenCalledWith('/service/rest/v1/security/ldap/Test%20LDAP');
    });

    it('returns null for 404 not found', async () => {
      const error = { response: { status: 404 }, message: 'Not found' };
      mockRestClient.get.mockRejectedValueOnce(error);
      mockParseApiError.mockReturnValueOnce({ message: 'Not found', status: 404 });

      const { result } = renderHook(() => useLdapApi());

      let server: LdapServer | null = null;
      await act(async () => {
        server = await result.current.fetchServer('NonExistent');
      });

      expect(server).toBeNull();
    });
  });

  describe('createServer', () => {
    it('creates a new server using REST API', async () => {
      mockRestClient.post.mockResolvedValueOnce(undefined); // POST returns 201 no body
      mockRestClient.get.mockResolvedValueOnce(mockRestServer); // Fetch after create

      const { result } = renderHook(() => useLdapApi());

      let created: LdapServer | undefined;
      await act(async () => {
        created = await result.current.createServer(mockUiServer);
      });

      expect(created).toBeDefined();
      expect(created!.name).toBe('Test LDAP');
      expect(mockRestClient.post).toHaveBeenCalledWith(
        '/service/rest/v1/security/ldap',
        expect.objectContaining({
          name: 'Test LDAP',
          authScheme: 'SIMPLE', // Transformed to REST format
          connectionTimeoutSeconds: 30, // REST field name
        })
      );
    });

    it('sets loading state during create', async () => {
      mockRestClient.post.mockResolvedValueOnce(undefined);
      mockRestClient.get.mockResolvedValueOnce(mockRestServer);

      const { result } = renderHook(() => useLdapApi());

      expect(result.current.loading).toBe(false);

      const promise = act(async () => {
        await result.current.createServer(mockUiServer);
      });

      await promise;
      expect(result.current.loading).toBe(false);
    });
  });

  describe('updateServer', () => {
    it('updates an existing server using REST API', async () => {
      mockRestClient.put.mockResolvedValueOnce(undefined); // PUT returns 204 no body
      mockRestClient.get.mockResolvedValueOnce(mockRestServer); // Fetch after update

      const { result } = renderHook(() => useLdapApi());

      await act(async () => {
        await result.current.updateServer(mockUiServer);
      });

      expect(mockRestClient.put).toHaveBeenCalledWith(
        expect.stringContaining('/service/rest/v1/security/ldap/'),
        expect.objectContaining({
          name: 'Test LDAP',
          authScheme: 'SIMPLE',
        })
      );
    });
  });

  describe('deleteServer', () => {
    it('deletes a server by name using REST API', async () => {
      mockRestClient.delete.mockResolvedValueOnce(undefined);

      const { result } = renderHook(() => useLdapApi());

      await act(async () => {
        await result.current.deleteServer('Test LDAP');
      });

      expect(mockRestClient.delete).toHaveBeenCalledWith('/service/rest/v1/security/ldap/Test%20LDAP');
    });
  });

  describe('changeOrder', () => {
    it('changes server order using REST API', async () => {
      mockRestClient.post.mockResolvedValueOnce(undefined);

      const { result } = renderHook(() => useLdapApi());
      const newOrder = ['server2', 'server1'];

      await act(async () => {
        await result.current.changeOrder(newOrder);
      });

      expect(mockRestClient.post).toHaveBeenCalledWith(
        '/service/rest/v1/security/ldap/change-order',
        newOrder
      );
    });
  });

  describe('clearCache', () => {
    it('calls clear cache API using REST', async () => {
      mockRestClient.delete.mockResolvedValueOnce(undefined);

      const { result } = renderHook(() => useLdapApi());

      await act(async () => {
        await result.current.clearCache();
      });

      expect(mockRestClient.delete).toHaveBeenCalledWith('/service/rest/v1/security/ldap/cache');
    });
  });

  describe('verifyConnection', () => {
    it('verifies connection using REST API', async () => {
      mockRestClient.post.mockResolvedValueOnce(undefined);

      const { result } = renderHook(() => useLdapApi());

      await act(async () => {
        await result.current.verifyConnection(mockUiServer);
      });

      expect(mockRestClient.post).toHaveBeenCalledWith(
        '/service/rest/v1/security/ldap/verify-connection',
        expect.objectContaining({
          name: 'Test LDAP',
          authScheme: 'SIMPLE',
        })
      );
    });

    it('throws error on verification failure', async () => {
      const error = new Error('Connection failed');
      mockRestClient.post.mockRejectedValueOnce(error);

      const { result } = renderHook(() => useLdapApi());

      await expect(
        act(async () => {
          await result.current.verifyConnection(mockUiServer);
        })
      ).rejects.toThrow('Connection failed');
    });
  });

  describe('verifyUserMapping', () => {
    it('verifies user mapping and returns users using REST API', async () => {
      const mockUsers = [
        { username: 'user1', realName: 'User One', email: 'user1@example.com' },
      ];

      mockRestClient.post.mockResolvedValueOnce(mockUsers);

      const { result } = renderHook(() => useLdapApi());

      let users: any[] = [];
      await act(async () => {
        users = await result.current.verifyUserMapping(mockUiServer);
      });

      expect(users).toEqual(mockUsers);
      expect(mockRestClient.post).toHaveBeenCalledWith(
        '/service/rest/v1/security/ldap/verify-user-mapping',
        expect.any(Object)
      );
    });
  });

  describe('verifyLogin', () => {
    it('verifies user login credentials using REST API', async () => {
      mockRestClient.post.mockResolvedValueOnce(undefined);

      const { result } = renderHook(() => useLdapApi());

      await act(async () => {
        await result.current.verifyLogin(mockUiServer, 'testuser', 'password123');
      });

      expect(mockRestClient.post).toHaveBeenCalledWith(
        '/service/rest/v1/security/ldap/verify-login',
        expect.objectContaining({
          testUsername: 'testuser',
          testPassword: 'password123',
        })
      );
    });
  });

  describe('fetchTemplates', () => {
    it('fetches LDAP templates using REST API', async () => {
      const mockTemplates = [
        { id: 'openldap', name: 'OpenLDAP' },
        { id: 'active_directory', name: 'Active Directory' },
      ];

      mockRestClient.get.mockResolvedValueOnce(mockTemplates);

      const { result } = renderHook(() => useLdapApi());

      let templates: any[] = [];
      await act(async () => {
        templates = await result.current.fetchTemplates();
      });

      expect(templates).toEqual(mockTemplates);
      expect(mockRestClient.get).toHaveBeenCalledWith('/service/rest/v1/security/ldap/templates');
    });
  });

  describe('setError', () => {
    it('manually sets error state', () => {
      const { result } = renderHook(() => useLdapApi());

      act(() => {
        result.current.setError('Custom error message');
      });

      expect(result.current.error).toBe('Custom error message');
    });

    it('clears error when set to null', () => {
      const { result } = renderHook(() => useLdapApi());

      act(() => {
        result.current.setError('Error');
        result.current.setError(null);
      });

      expect(result.current.error).toBe(null);
    });
  });
});
