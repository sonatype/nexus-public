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
import { useHttpApi } from '../useHttpApi';
import { HttpConfiguration, DEFAULT_HTTP_CONFIGURATION } from '../types';

// Create mock functions
const mockRestClientGet = jest.fn();
const mockRestClientPut = jest.fn();

// Mock @/utils/api directly (source code imports from here, not from nexus-ui-plugin)
jest.mock('../../../../../../../interface/api', () => ({
  restClient: {
    get: (...args: unknown[]) => mockRestClientGet(...args),
    put: (...args: unknown[]) => mockRestClientPut(...args),
  },
  ENDPOINTS: {
    HTTP_SETTINGS: '/service/rest/v1/http',
  },
  parseApiError: (err: unknown) => ({
    message: (err as { response?: { data?: { message?: string } }; message?: string })?.response?.data?.message
      || (err as { message?: string })?.message
      || 'Unknown error',
    status: (err as { response?: { status?: number } })?.response?.status || 0,
  }),
}));

describe('useHttpApi', () => {
  const mockRestResponse = {
    userAgentSuffix: 'TestAgent',
    timeout: 30,
    retries: 3,
    httpEnabled: true,
    httpHost: 'proxy.example.com',
    httpPort: 8080,
    httpAuthEnabled: false,
    httpAuthUsername: null,
    httpAuthPassword: null,
    httpAuthNtlmHost: null,
    httpAuthNtlmDomain: null,
    httpsEnabled: false,
    httpsHost: null,
    httpsPort: null,
    httpsAuthEnabled: false,
    httpsAuthUsername: null,
    httpsAuthPassword: null,
    httpsAuthNtlmHost: null,
    httpsAuthNtlmDomain: null,
    nonProxyHosts: ['localhost', '127.0.0.1'],
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('fetchSettings', () => {
    it('should return initial state', () => {
      const { result } = renderHook(() => useHttpApi());

      expect(result.current.loading).toBe(false);
      expect(result.current.error).toBeNull();
      expect(typeof result.current.fetchSettings).toBe('function');
      expect(typeof result.current.saveSettings).toBe('function');
      expect(typeof result.current.setError).toBe('function');
    });

    it('should fetch and normalize settings successfully', async () => {
      mockRestClientGet.mockResolvedValueOnce(mockRestResponse);

      const { result } = renderHook(() => useHttpApi());

      let settings: HttpConfiguration | undefined;
      await act(async () => {
        settings = await result.current.fetchSettings();
      });

      expect(mockRestClientGet).toHaveBeenCalledWith('/service/rest/v1/http');
      expect(settings).toEqual({
        ...DEFAULT_HTTP_CONFIGURATION,
        ...mockRestResponse,
        httpEnabled: true,
        httpsEnabled: false,
        httpAuthEnabled: false,
        httpsAuthEnabled: false,
        nonProxyHosts: ['localhost', '127.0.0.1'],
      });
    });

    it('should normalize missing fields with defaults', async () => {
      const partialResponse = {
        userAgentSuffix: 'Test',
      };

      mockRestClientGet.mockResolvedValueOnce(partialResponse);

      const { result } = renderHook(() => useHttpApi());

      let settings: HttpConfiguration | undefined;
      await act(async () => {
        settings = await result.current.fetchSettings();
      });

      expect(settings).toEqual({
        ...DEFAULT_HTTP_CONFIGURATION,
        userAgentSuffix: 'Test',
        httpEnabled: false,
        httpsEnabled: false,
        httpAuthEnabled: false,
        httpsAuthEnabled: false,
        nonProxyHosts: [],
      });
    });

    it('should throw error when request fails', async () => {
      mockRestClientGet.mockRejectedValueOnce(new Error('Network error'));

      const { result } = renderHook(() => useHttpApi());

      await expect(result.current.fetchSettings()).rejects.toThrow('Network error');
    });

    it('should handle fetch failure with API error response', async () => {
      mockRestClientGet.mockRejectedValueOnce({
        response: {
          status: 500,
          data: { message: 'Internal server error' },
        },
      });

      const { result } = renderHook(() => useHttpApi());

      await expect(result.current.fetchSettings()).rejects.toThrow('Internal server error');
    });
  });

  describe('saveSettings', () => {
    it('should save settings successfully', async () => {
      const settingsToSave: HttpConfiguration = {
        ...DEFAULT_HTTP_CONFIGURATION,
        userAgentSuffix: 'UpdatedAgent',
        httpEnabled: true,
        httpHost: 'newproxy.example.com',
        httpPort: 3128,
      };

      const updatedResponse = {
        ...mockRestResponse,
        userAgentSuffix: 'UpdatedAgent',
        httpHost: 'newproxy.example.com',
        httpPort: 3128,
      };

      mockRestClientPut.mockResolvedValueOnce(undefined);
      mockRestClientGet.mockResolvedValueOnce(updatedResponse);

      const { result } = renderHook(() => useHttpApi());

      let savedSettings: HttpConfiguration | undefined;
      await act(async () => {
        savedSettings = await result.current.saveSettings(settingsToSave);
      });

      expect(mockRestClientPut).toHaveBeenCalledWith(
        '/service/rest/v1/http',
        expect.objectContaining({
          userAgentSuffix: 'UpdatedAgent',
          httpEnabled: true,
          httpHost: 'newproxy.example.com',
          httpPort: 3128,
        })
      );
      expect(result.current.loading).toBe(false);
      expect(result.current.error).toBeNull();
      expect(savedSettings?.userAgentSuffix).toBe('UpdatedAgent');
    });

    it('should set loading state during save', async () => {
      let resolvePromise: (value: void) => void;
      const promise = new Promise<void>((resolve) => {
        resolvePromise = resolve;
      });

      mockRestClientPut.mockReturnValue(promise);
      mockRestClientGet.mockResolvedValue(DEFAULT_HTTP_CONFIGURATION);

      const { result } = renderHook(() => useHttpApi());

      let savePromise: Promise<HttpConfiguration>;
      act(() => {
        savePromise = result.current.saveSettings(DEFAULT_HTTP_CONFIGURATION);
      });

      expect(result.current.loading).toBe(true);

      await act(async () => {
        resolvePromise!();
        await savePromise;
      });

      expect(result.current.loading).toBe(false);
    });

    it('should set error state when save fails', async () => {
      const errorMessage = 'Permission denied';
      mockRestClientPut.mockRejectedValueOnce(new Error(errorMessage));

      const { result } = renderHook(() => useHttpApi());

      await act(async () => {
        try {
          await result.current.saveSettings(DEFAULT_HTTP_CONFIGURATION);
        } catch {
          // Expected error
        }
      });

      expect(result.current.loading).toBe(false);
      expect(result.current.error).toBe(errorMessage);
    });

    it('should set error from API response error message', async () => {
      mockRestClientPut.mockRejectedValueOnce({
        response: {
          status: 400,
          data: { message: 'Validation failed' },
        },
      });

      const { result } = renderHook(() => useHttpApi());

      await act(async () => {
        try {
          await result.current.saveSettings(DEFAULT_HTTP_CONFIGURATION);
        } catch {
          // Expected error
        }
      });

      expect(result.current.error).toBe('Invalid HTTP settings. Please check your proxy host and port values.');
    });

    it('should include HTTP auth fields when httpAuthEnabled is true', async () => {
      const settings: HttpConfiguration = {
        ...DEFAULT_HTTP_CONFIGURATION,
        httpEnabled: true,
        httpHost: 'proxy.example.com',
        httpPort: 8080,
        httpAuthEnabled: true,
        httpAuthUsername: 'user',
        httpAuthPassword: 'pass',
        httpAuthNtlmHost: 'ntlmhost',
        httpAuthNtlmDomain: 'DOMAIN',
      };

      mockRestClientPut.mockResolvedValueOnce(undefined);
      mockRestClientGet.mockResolvedValueOnce(settings);

      const { result } = renderHook(() => useHttpApi());

      await act(async () => {
        await result.current.saveSettings(settings);
      });

      expect(mockRestClientPut).toHaveBeenCalledWith(
        '/service/rest/v1/http',
        expect.objectContaining({
          httpAuthEnabled: true,
          httpAuthUsername: 'user',
          httpAuthPassword: 'pass',
          httpAuthNtlmHost: 'ntlmhost',
          httpAuthNtlmDomain: 'DOMAIN',
        })
      );
    });

    it('should not include HTTP proxy fields when httpEnabled is false', async () => {
      const settings: HttpConfiguration = {
        ...DEFAULT_HTTP_CONFIGURATION,
        httpEnabled: false,
        httpHost: 'proxy.example.com',
        httpPort: 8080,
        httpAuthEnabled: true,
        httpAuthUsername: 'user',
      };

      mockRestClientPut.mockResolvedValueOnce(undefined);
      mockRestClientGet.mockResolvedValueOnce(settings);

      const { result } = renderHook(() => useHttpApi());

      await act(async () => {
        await result.current.saveSettings(settings);
      });

      const callArgs = mockRestClientPut.mock.calls[0][1];
      expect(callArgs.httpHost).toBeUndefined();
      expect(callArgs.httpPort).toBeUndefined();
      expect(callArgs.httpAuthEnabled).toBeUndefined();
    });

    it('should not include HTTPS fields when httpsEnabled is false', async () => {
      const settings: HttpConfiguration = {
        ...DEFAULT_HTTP_CONFIGURATION,
        httpsEnabled: false,
        httpsHost: 'secureproxy.example.com',
        httpsPort: 443,
        httpsAuthEnabled: true,
      };

      mockRestClientPut.mockResolvedValueOnce(undefined);
      mockRestClientGet.mockResolvedValueOnce(settings);

      const { result } = renderHook(() => useHttpApi());

      await act(async () => {
        await result.current.saveSettings(settings);
      });

      const callArgs = mockRestClientPut.mock.calls[0][1];
      expect(callArgs.httpsHost).toBeUndefined();
      expect(callArgs.httpsPort).toBeUndefined();
      expect(callArgs.httpsAuthEnabled).toBeUndefined();
    });

    it('should reset nonProxyHosts when no proxy is enabled', async () => {
      const settings: HttpConfiguration = {
        ...DEFAULT_HTTP_CONFIGURATION,
        httpEnabled: false,
        httpsEnabled: false,
        nonProxyHosts: ['localhost', '*.internal.com'],
      };

      mockRestClientPut.mockResolvedValueOnce(undefined);
      mockRestClientGet.mockResolvedValueOnce(settings);

      const { result } = renderHook(() => useHttpApi());

      await act(async () => {
        await result.current.saveSettings(settings);
      });

      const callArgs = mockRestClientPut.mock.calls[0][1];
      expect(callArgs.nonProxyHosts).toEqual([]);
    });

    it('should normalize empty timeout/retries to null', async () => {
      const settings: HttpConfiguration = {
        ...DEFAULT_HTTP_CONFIGURATION,
        timeout: 0,
        retries: 0,
      };

      mockRestClientPut.mockResolvedValueOnce(undefined);
      mockRestClientGet.mockResolvedValueOnce(settings);

      const { result } = renderHook(() => useHttpApi());

      await act(async () => {
        await result.current.saveSettings(settings);
      });

      const callArgs = mockRestClientPut.mock.calls[0][1];
      expect(callArgs.timeout).toBeNull();
      expect(callArgs.retries).toBeNull();
    });
  });

  describe('setError', () => {
    it('should allow clearing error state', async () => {
      mockRestClientPut.mockRejectedValueOnce(new Error('Some error'));

      const { result } = renderHook(() => useHttpApi());

      // First, trigger an error
      await act(async () => {
        try {
          await result.current.saveSettings(DEFAULT_HTTP_CONFIGURATION);
        } catch {
          // Expected error
        }
      });

      expect(result.current.error).toBe('Some error');

      // Clear the error
      act(() => {
        result.current.setError(null);
      });

      expect(result.current.error).toBeNull();
    });
  });
});
