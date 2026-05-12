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

import { useAnonymousApi } from '../useAnonymousApi';

// Mock the REST API from @sonatype/nexus-ui-plugin
const mockRestClient = {
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
};

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  restClient: {
    get: (...args: unknown[]) => mockRestClient.get(...args),
    post: (...args: unknown[]) => mockRestClient.post(...args),
    put: (...args: unknown[]) => mockRestClient.put(...args),
    delete: (...args: unknown[]) => mockRestClient.delete(...args),
  },
  parseApiError: jest.fn((err) => ({
    message: err?.response?.data?.message || err?.message || 'An error occurred',
    status: err?.response?.status,
  })),
  APIConstants: {
    REST: {
      INTERNAL: {
        REALMS_TYPES: 'service/rest/internal/ui/realms/types',
        ANONYMOUS_SETTINGS: 'service/rest/internal/ui/anonymous-settings',
      },
    },
  },
}));

describe('useAnonymousApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('fetchRealmTypes', () => {
    it('fetches realm types successfully', async () => {
      const mockRealmTypes = [
        { id: 'NexusAuthorizingRealm', name: 'Local Authorizing Realm' },
        { id: 'LdapRealm', name: 'LDAP Realm' },
      ];

      mockRestClient.get.mockResolvedValueOnce(mockRealmTypes);

      const { result } = renderHook(() => useAnonymousApi());

      let realmTypes;
      await act(async () => {
        realmTypes = await result.current.fetchRealmTypes();
      });

      expect(mockRestClient.get).toHaveBeenCalledWith('service/rest/internal/ui/realms/types');
      expect(realmTypes).toEqual(mockRealmTypes);
    });

    it('returns empty array when response is not an array', async () => {
      mockRestClient.get.mockResolvedValueOnce(null);

      const { result } = renderHook(() => useAnonymousApi());

      let realmTypes;
      await act(async () => {
        realmTypes = await result.current.fetchRealmTypes();
      });

      expect(realmTypes).toEqual([]);
    });

    it('throws error when request fails', async () => {
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
      mockRestClient.get.mockRejectedValueOnce({ message: 'Network error' });

      const { result } = renderHook(() => useAnonymousApi());

      await expect(result.current.fetchRealmTypes()).rejects.toThrow('Network error');
      consoleSpy.mockRestore();
    });
  });

  describe('fetchSettings', () => {
    it('fetches anonymous settings successfully', async () => {
      const mockSettings = {
        enabled: true,
        userId: 'anonymous',
        realmName: 'NexusAuthorizingRealm',
      };

      mockRestClient.get.mockResolvedValueOnce(mockSettings);

      const { result } = renderHook(() => useAnonymousApi());

      let settings;
      await act(async () => {
        settings = await result.current.fetchSettings();
      });

      expect(mockRestClient.get).toHaveBeenCalledWith('service/rest/internal/ui/anonymous-settings');
      expect(settings).toEqual(mockSettings);
    });

    it('throws error when request fails', async () => {
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
      mockRestClient.get.mockRejectedValueOnce({ message: 'Network error' });

      const { result } = renderHook(() => useAnonymousApi());

      await expect(result.current.fetchSettings()).rejects.toThrow('Network error');
      consoleSpy.mockRestore();
    });
  });

  describe('saveSettings', () => {
    it('saves settings successfully', async () => {
      const mockSettings = {
        enabled: true,
        userId: 'anonymous',
        realmName: 'NexusAuthorizingRealm',
      };

      mockRestClient.put.mockResolvedValueOnce(mockSettings);

      const { result } = renderHook(() => useAnonymousApi());

      let savedSettings;
      await act(async () => {
        savedSettings = await result.current.saveSettings(mockSettings);
      });

      expect(mockRestClient.put).toHaveBeenCalledWith('service/rest/internal/ui/anonymous-settings', {
        ...mockSettings,
        userId: 'anonymous',
      });
      expect(savedSettings).toEqual(mockSettings);
      expect(result.current.loading).toBe(false);
      expect(result.current.error).toBeNull();
    });

    it('trims userId before saving', async () => {
      const mockSettings = {
        enabled: true,
        userId: '  guest  ',
        realmName: 'NexusAuthorizingRealm',
      };

      mockRestClient.put.mockResolvedValueOnce({ ...mockSettings, userId: 'guest' });

      const { result } = renderHook(() => useAnonymousApi());

      await act(async () => {
        await result.current.saveSettings(mockSettings);
      });

      expect(mockRestClient.put).toHaveBeenCalledWith('service/rest/internal/ui/anonymous-settings', {
        ...mockSettings,
        userId: 'guest',
      });
    });

    it('sets loading state during save', async () => {
      mockRestClient.put.mockImplementation(() => new Promise((resolve) => setTimeout(() => resolve({}), 100)));

      const { result } = renderHook(() => useAnonymousApi());

      let savePromise: Promise<any>;
      act(() => {
        savePromise = result.current.saveSettings({
          enabled: true,
          userId: 'anonymous',
          realmName: 'NexusAuthorizingRealm',
        });
      });

      expect(result.current.loading).toBe(true);

      await act(async () => {
        await savePromise;
      });

      expect(result.current.loading).toBe(false);
    });

    it('sets error when save fails', async () => {
      mockRestClient.put.mockRejectedValueOnce({
        response: { data: { message: 'Invalid configuration' } },
      });

      const { result } = renderHook(() => useAnonymousApi());

      await act(async () => {
        try {
          await result.current.saveSettings({
            enabled: true,
            userId: '',
            realmName: 'NexusAuthorizingRealm',
          });
        } catch (e) {
          // Expected to throw
        }
      });

      expect(result.current.error).toBe('Invalid configuration');
    });

    it('uses default error message when no specific message provided', async () => {
      mockRestClient.put.mockRejectedValueOnce({});

      const { result } = renderHook(() => useAnonymousApi());

      await act(async () => {
        try {
          await result.current.saveSettings({
            enabled: true,
            userId: 'anonymous',
            realmName: 'NexusAuthorizingRealm',
          });
        } catch (e) {
          // Expected to throw
        }
      });

      // parseApiError returns 'An error occurred' when no message is provided
      expect(result.current.error).toBe('An error occurred');
    });
  });

  describe('setError', () => {
    it('allows clearing the error', async () => {
      mockRestClient.put.mockRejectedValueOnce(new Error('Test error'));

      const { result } = renderHook(() => useAnonymousApi());

      await act(async () => {
        try {
          await result.current.saveSettings({
            enabled: true,
            userId: 'anonymous',
            realmName: 'test',
          });
        } catch (e) {
          // Expected
        }
      });

      expect(result.current.error).not.toBeNull();

      act(() => {
        result.current.setError(null);
      });

      expect(result.current.error).toBeNull();
    });
  });
});


