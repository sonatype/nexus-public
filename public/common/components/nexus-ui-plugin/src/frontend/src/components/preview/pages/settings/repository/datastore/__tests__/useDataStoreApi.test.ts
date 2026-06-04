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

import { useDataStoreApi } from '../useDataStoreApi';
import { DataStoreConfig } from '../types';

// Mock the REST API at the path the source uses
// Variables used in mock must have 'mock' prefix for hoisting
const mockRestClient = {
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
};

jest.mock('../../../../../../../interface/api', () => ({
  ...jest.requireActual('../../../../../../../interface/api'),
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

const mockConfig: DataStoreConfig = {
  name: 'nexus',
  source: 'local',
  type: 'jdbc',
  jdbcUrl: 'jdbc:postgresql://localhost:5432/nexus',
  username: 'nexus_user',
  schema: 'nexus',
  maximumConnectionPool: 100,
  advanced: '',
};

describe('useDataStoreApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('fetchConfig', () => {
    it('fetches datastore configuration successfully', async () => {
      mockRestClient.get.mockResolvedValue(mockConfig);

      const { result } = renderHook(() => useDataStoreApi());

      let fetchedConfig: DataStoreConfig | undefined;
      await act(async () => {
        fetchedConfig = await result.current.fetchConfig();
      });

      expect(mockRestClient.get).toHaveBeenCalledWith('/service/rest/internal/ui/datastore');
      expect(fetchedConfig).toEqual(mockConfig);
    });

    it('returns default config when response is empty', async () => {
      mockRestClient.get.mockResolvedValue(null);

      const { result } = renderHook(() => useDataStoreApi());

      let fetchedConfig: DataStoreConfig | undefined;
      await act(async () => {
        fetchedConfig = await result.current.fetchConfig();
      });

      expect(fetchedConfig).toEqual({
        jdbcUrl: '',
        username: '',
        schema: '',
        maximumConnectionPool: 10,
        advanced: '',
      });
    });

    it('handles API error', async () => {
      const errorMessage = 'Server error';
      mockRestClient.get.mockRejectedValue(new Error(errorMessage));

      const { result } = renderHook(() => useDataStoreApi());

      await expect(result.current.fetchConfig()).rejects.toThrow(errorMessage);
    });

    it('extracts error message from response data', async () => {
      mockRestClient.get.mockRejectedValue({
        response: {
          data: { message: 'Custom error message' }
        }
      });

      const { result } = renderHook(() => useDataStoreApi());

      await expect(result.current.fetchConfig()).rejects.toThrow('Custom error message');
    });
  });

  describe('updateConfig', () => {
    it('updates configuration successfully', async () => {
      const updatedConfig = { ...mockConfig, maximumConnectionPool: 200 };
      mockRestClient.put.mockResolvedValue(updatedConfig);

      const { result } = renderHook(() => useDataStoreApi());

      let returnedConfig: DataStoreConfig | undefined;
      await act(async () => {
        returnedConfig = await result.current.updateConfig(updatedConfig);
      });

      expect(mockRestClient.put).toHaveBeenCalledWith('/service/rest/internal/ui/datastore', updatedConfig);
      expect(returnedConfig).toEqual(updatedConfig);
    });

    it('sets loading state during update', async () => {
      let resolvePromise: (value: any) => void;
      const pendingPromise = new Promise((resolve) => {
        resolvePromise = resolve;
      });
      mockRestClient.put.mockReturnValue(pendingPromise as Promise<any>);

      const { result } = renderHook(() => useDataStoreApi());

      // Start update
      act(() => {
        result.current.updateConfig(mockConfig);
      });

      expect(result.current.loading).toBe(true);

      // Resolve the promise
      await act(async () => {
        resolvePromise!(mockConfig);
      });

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });
    });

    it('handles validation error from API', async () => {
      const errorMessage = 'Invalid pool size';
      mockRestClient.put.mockRejectedValue(new Error(errorMessage));

      const { result } = renderHook(() => useDataStoreApi());

      await act(async () => {
        await expect(result.current.updateConfig(mockConfig)).rejects.toThrow(errorMessage);
      });
      await waitFor(() => {
        expect(result.current.error).toBe(errorMessage);
      });
    });

    it('extracts error message from response', async () => {
      mockRestClient.put.mockRejectedValue({
        response: {
          data: { message: 'Validation failed' }
        }
      });

      const { result } = renderHook(() => useDataStoreApi());

      await act(async () => {
        await expect(result.current.updateConfig(mockConfig)).rejects.toThrow('Validation failed');
      });
      await waitFor(() => {
        expect(result.current.error).toBe('Validation failed');
      });
    });

    it('clears error on successful update', async () => {
      mockRestClient.put.mockResolvedValue(mockConfig);

      const { result } = renderHook(() => useDataStoreApi());

      // Set initial error
      act(() => {
        result.current.setError('Previous error');
      });
      expect(result.current.error).toBe('Previous error');

      // Update successfully
      await act(async () => {
        await result.current.updateConfig(mockConfig);
      });

      expect(result.current.error).toBeNull();
    });
  });

  describe('setError', () => {
    it('sets error state', () => {
      const { result } = renderHook(() => useDataStoreApi());

      act(() => {
        result.current.setError('Custom error');
      });

      expect(result.current.error).toBe('Custom error');
    });

    it('clears error when set to null', () => {
      const { result } = renderHook(() => useDataStoreApi());

      act(() => {
        result.current.setError('Initial error');
      });
      expect(result.current.error).toBe('Initial error');

      act(() => {
        result.current.setError(null);
      });
      expect(result.current.error).toBeNull();
    });
  });

  describe('loading state', () => {
    it('initializes with loading false', () => {
      const { result } = renderHook(() => useDataStoreApi());
      expect(result.current.loading).toBe(false);
    });
  });
});
