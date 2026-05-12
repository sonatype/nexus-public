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
import { useUserTokensApi } from '../useUserTokensApi';
import { UserTokenSettings, DEFAULT_USER_TOKEN_SETTINGS } from '../types';

const mockGet = jest.fn();
const mockPut = jest.fn();
const mockDelete = jest.fn();

// Mock the REST API from @/utils/api
jest.mock('@/utils/api', () => ({
  restClient: {
    get: (...args: unknown[]) => mockGet(...args),
    put: (...args: unknown[]) => mockPut(...args),
    delete: (...args: unknown[]) => mockDelete(...args),
  },
  parseApiError: jest.fn((err: unknown) => {
    const error = err as { response?: { status?: number; data?: { message?: string } }; message?: string };
    return {
      message: error?.response?.data?.message || error?.message || 'Failed to load user token settings',
      status: error?.response?.status,
    };
  }),
}));

const mockSettings: UserTokenSettings = {
  enabled: true,
  protectContent: false,
  expirationEnabled: true,
  expirationDays: 30,
};

describe('useUserTokensApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockGet.mockReset();
    mockPut.mockReset();
    mockDelete.mockReset();
  });

  describe('fetchSettings', () => {
    it('fetches user token settings successfully', async () => {
      mockGet.mockResolvedValue(mockSettings);

      const { result } = renderHook(() => useUserTokensApi());

      let settings;
      await act(async () => {
        settings = await result.current.fetchSettings();
      });

      expect(settings).toEqual(mockSettings);
      expect(mockGet).toHaveBeenCalledWith('/service/rest/v1/security/user-tokens');
    });

    it('returns default settings when API returns empty response', async () => {
      mockGet.mockResolvedValue(null);

      const { result } = renderHook(() => useUserTokensApi());

      let settings;
      await act(async () => {
        settings = await result.current.fetchSettings();
      });

      expect(settings).toEqual(DEFAULT_USER_TOKEN_SETTINGS);
    });

    it('sets loading state while fetching', async () => {
      mockGet.mockImplementation(() => new Promise((resolve) => {
        setTimeout(() => resolve(mockSettings), 100);
      }));

      const { result } = renderHook(() => useUserTokensApi());

      // Initial state
      expect(result.current.loading).toBe(false);

      // Start fetching
      let fetchPromise: Promise<UserTokenSettings>;
      act(() => {
        fetchPromise = result.current.fetchSettings();
      });

      // Loading should be true
      expect(result.current.loading).toBe(true);

      // Wait for completion
      await act(async () => {
        await fetchPromise;
      });

      // Loading should be false
      expect(result.current.loading).toBe(false);
    });

    it('sets error state on failure', async () => {
      mockGet.mockRejectedValue({
        response: { data: { message: 'Access denied' } },
      });

      const { result } = renderHook(() => useUserTokensApi());

      await expect(result.current.fetchSettings()).rejects.toThrow('Access denied');

      await waitFor(() => {
        expect(result.current.error).toBe('Access denied');
      });
    });

    it('uses generic error message when no message provided', async () => {
      mockGet.mockRejectedValue({});

      const { result } = renderHook(() => useUserTokensApi());

      await expect(result.current.fetchSettings()).rejects.toThrow('Failed to load user token settings');

      await waitFor(() => {
        expect(result.current.error).toBe('Failed to load user token settings');
      });
    });

    it('returns default settings silently on 404 (feature not licensed on cloud)', async () => {
      mockGet.mockRejectedValue({ response: { status: 404 } });

      const { result } = renderHook(() => useUserTokensApi());

      let settings;
      await act(async () => {
        settings = await result.current.fetchSettings();
      });

      // Should NOT set an error — 404 means feature not licensed, not an error state
      expect(settings).toEqual(DEFAULT_USER_TOKEN_SETTINGS);
      expect(result.current.error).toBeNull();
    });
  });

  describe('saveSettings', () => {
    it('saves user token settings successfully', async () => {
      const updatedSettings: UserTokenSettings = {
        enabled: true,
        protectContent: true,
        expirationEnabled: true,
        expirationDays: 60,
      };

      mockPut.mockResolvedValue(updatedSettings);

      const { result } = renderHook(() => useUserTokensApi());

      let savedSettings;
      await act(async () => {
        savedSettings = await result.current.saveSettings(updatedSettings);
      });

      expect(savedSettings).toEqual(updatedSettings);
      expect(mockPut).toHaveBeenCalledWith(
        '/service/rest/v1/security/user-tokens',
        updatedSettings
      );
    });

    it('sets loading state while saving', async () => {
      mockPut.mockImplementation(() => new Promise((resolve) => {
        setTimeout(() => resolve(mockSettings), 100);
      }));

      const { result } = renderHook(() => useUserTokensApi());

      expect(result.current.loading).toBe(false);

      let savePromise: Promise<UserTokenSettings>;
      act(() => {
        savePromise = result.current.saveSettings(mockSettings);
      });

      expect(result.current.loading).toBe(true);

      await act(async () => {
        await savePromise;
      });

      expect(result.current.loading).toBe(false);
    });

    it('sets error state on save failure', async () => {
      mockPut.mockRejectedValue({
        response: { data: { message: 'Invalid settings' } },
      });

      const { result } = renderHook(() => useUserTokensApi());

      await expect(result.current.saveSettings(mockSettings)).rejects.toThrow('Invalid settings');

      await waitFor(() => {
        expect(result.current.error).toBe('Invalid settings');
      });
    });

    it('clears previous error before saving', async () => {
      // First, cause an error
      mockPut.mockRejectedValueOnce({
        response: { data: { message: 'First error' } },
      });

      const { result } = renderHook(() => useUserTokensApi());

      await expect(result.current.saveSettings(mockSettings)).rejects.toThrow();
      
      await waitFor(() => {
        expect(result.current.error).toBe('First error');
      });

      // Then, make a successful call
      mockPut.mockResolvedValueOnce(mockSettings);

      await act(async () => {
        await result.current.saveSettings(mockSettings);
      });

      // Error should be cleared
      await waitFor(() => {
        expect(result.current.error).toBeNull();
      });
    });
  });

  describe('resetAllTokens', () => {
    it('resets all user tokens successfully', async () => {
      mockDelete.mockResolvedValue(undefined);

      const { result } = renderHook(() => useUserTokensApi());

      await act(async () => {
        await result.current.resetAllTokens();
      });

      expect(mockDelete).toHaveBeenCalledWith('/service/rest/v1/security/user-tokens');
    });

    it('sets loading state while resetting', async () => {
      mockDelete.mockImplementation(() => new Promise((resolve) => {
        setTimeout(() => resolve(undefined), 100);
      }));

      const { result } = renderHook(() => useUserTokensApi());

      expect(result.current.loading).toBe(false);

      let resetPromise: Promise<void>;
      act(() => {
        resetPromise = result.current.resetAllTokens();
      });

      expect(result.current.loading).toBe(true);

      await act(async () => {
        await resetPromise;
      });

      expect(result.current.loading).toBe(false);
    });

    it('sets error state on reset failure', async () => {
      mockDelete.mockRejectedValue({
        response: { data: { message: 'Permission denied' } },
      });

      const { result } = renderHook(() => useUserTokensApi());

      await expect(result.current.resetAllTokens()).rejects.toThrow('Permission denied');

      await waitFor(() => {
        expect(result.current.error).toBe('Permission denied');
      });
    });

    it('uses generic error message when no message provided', async () => {
      mockDelete.mockRejectedValue({ message: 'Network error' });

      const { result } = renderHook(() => useUserTokensApi());

      await expect(result.current.resetAllTokens()).rejects.toThrow('Network error');

      await waitFor(() => {
        expect(result.current.error).toBe('Network error');
      });
    });
  });

  describe('setError', () => {
    it('allows manual error setting', async () => {
      const { result } = renderHook(() => useUserTokensApi());

      expect(result.current.error).toBeNull();

      act(() => {
        result.current.setError('Manual error');
      });

      expect(result.current.error).toBe('Manual error');
    });

    it('allows clearing error', async () => {
      mockGet.mockRejectedValue({
        response: { data: { message: 'API error' } },
      });

      const { result } = renderHook(() => useUserTokensApi());

      await expect(result.current.fetchSettings()).rejects.toThrow();
      
      await waitFor(() => {
        expect(result.current.error).toBe('API error');
      });

      act(() => {
        result.current.setError(null);
      });

      expect(result.current.error).toBeNull();
    });
  });

  describe('initial state', () => {
    it('starts with loading false and no error', () => {
      const { result } = renderHook(() => useUserTokensApi());

      expect(result.current.loading).toBe(false);
      expect(result.current.error).toBeNull();
    });
  });
});


