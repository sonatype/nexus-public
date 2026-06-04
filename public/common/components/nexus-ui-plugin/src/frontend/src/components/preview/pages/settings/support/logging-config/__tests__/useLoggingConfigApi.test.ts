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

import { useLoggingConfigApi } from '../useLoggingConfigApi';
import { LOGGING_CONFIG_API } from '../types';

const mockRestClient = {
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
};

// Mock the local API module used by useLoggingConfigApi
jest.mock('../../../../../../../interface/api', () => ({
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
}));

describe('useLoggingConfigApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('fetchLoggers', () => {
    it('fetches loggers successfully', async () => {
      const mockLoggers = [
        { name: 'ROOT', level: 'INFO', override: false },
        { name: 'org.sonatype', level: 'DEBUG', override: true },
      ];
      mockRestClient.get.mockResolvedValueOnce(mockLoggers);

      const { result } = renderHook(() => useLoggingConfigApi());

      let loggers: any;
      await act(async () => {
        loggers = await result.current.fetchLoggers();
      });

      expect(mockRestClient.get).toHaveBeenCalledWith(LOGGING_CONFIG_API.LIST);
      expect(loggers).toEqual(mockLoggers);
    });

    it('handles fetch error', async () => {
      const errorMessage = 'Network Error';
      mockRestClient.get.mockRejectedValueOnce(new Error(errorMessage));

      const { result } = renderHook(() => useLoggingConfigApi());

      await act(async () => {
        await expect(result.current.fetchLoggers()).rejects.toThrow();
      });

      expect(result.current.error).toBe(errorMessage);
    });

    it('returns empty array when response is not an array', async () => {
      mockRestClient.get.mockResolvedValueOnce(null);

      const { result } = renderHook(() => useLoggingConfigApi());

      let loggers: any;
      await act(async () => {
        loggers = await result.current.fetchLoggers();
      });

      expect(loggers).toEqual([]);
    });
  });

  describe('fetchLogger', () => {
    it('fetches single logger successfully', async () => {
      const mockLogger = { name: 'org.sonatype', level: 'DEBUG', override: true };
      mockRestClient.get.mockResolvedValueOnce(mockLogger);

      const { result } = renderHook(() => useLoggingConfigApi());

      let logger: any;
      await act(async () => {
        logger = await result.current.fetchLogger('org.sonatype');
      });

      expect(mockRestClient.get).toHaveBeenCalledWith(
        LOGGING_CONFIG_API.GET('org.sonatype')
      );
      expect(logger).toEqual(mockLogger);
    });

    it('handles fetch single logger error', async () => {
      const errorMessage = 'Logger not found';
      mockRestClient.get.mockRejectedValueOnce(new Error(errorMessage));

      const { result } = renderHook(() => useLoggingConfigApi());

      await act(async () => {
        await expect(result.current.fetchLogger('unknown')).rejects.toThrow(errorMessage);
      });
    });
  });

  describe('updateLogger', () => {
    it('updates logger successfully', async () => {
      mockRestClient.put.mockResolvedValueOnce({});

      const { result } = renderHook(() => useLoggingConfigApi());

      await act(async () => {
        await result.current.updateLogger('org.sonatype', 'DEBUG');
      });

      expect(mockRestClient.put).toHaveBeenCalledWith(
        LOGGING_CONFIG_API.UPDATE('org.sonatype'),
        { level: 'DEBUG' }
      );
    });

    it('handles update error', async () => {
      const errorMessage = 'Failed to update';
      mockRestClient.put.mockRejectedValueOnce(new Error(errorMessage));

      const { result } = renderHook(() => useLoggingConfigApi());

      await act(async () => {
        await expect(result.current.updateLogger('test', 'DEBUG')).rejects.toThrow();
      });

      expect(result.current.error).toBe(errorMessage);
    });
  });

  describe('resetLogger', () => {
    it('resets logger successfully', async () => {
      mockRestClient.post.mockResolvedValueOnce({});

      const { result } = renderHook(() => useLoggingConfigApi());

      await act(async () => {
        await result.current.resetLogger('org.sonatype');
      });

      expect(mockRestClient.post).toHaveBeenCalledWith(
        LOGGING_CONFIG_API.RESET('org.sonatype')
      );
    });

    it('handles reset error', async () => {
      const errorMessage = 'Failed to reset';
      mockRestClient.post.mockRejectedValueOnce(new Error(errorMessage));

      const { result } = renderHook(() => useLoggingConfigApi());

      await act(async () => {
        await expect(result.current.resetLogger('test')).rejects.toThrow();
      });

      expect(result.current.error).toBe(errorMessage);
    });
  });

  describe('resetAllLoggers', () => {
    it('resets all loggers successfully', async () => {
      mockRestClient.post.mockResolvedValueOnce({});

      const { result } = renderHook(() => useLoggingConfigApi());

      await act(async () => {
        await result.current.resetAllLoggers();
      });

      expect(mockRestClient.post).toHaveBeenCalledWith(LOGGING_CONFIG_API.RESET_ALL);
    });

    it('handles reset all error', async () => {
      const errorMessage = 'Failed to reset all';
      mockRestClient.post.mockRejectedValueOnce(new Error(errorMessage));

      const { result } = renderHook(() => useLoggingConfigApi());

      await act(async () => {
        await expect(result.current.resetAllLoggers()).rejects.toThrow();
      });

      expect(result.current.error).toBe(errorMessage);
    });
  });

  describe('setError', () => {
    it('can set and clear error', async () => {
      const { result } = renderHook(() => useLoggingConfigApi());

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


