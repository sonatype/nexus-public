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

import { useMetricHealthApi } from '../useMetricHealthApi';

// Mock the REST API at the relative paths used by the source
const mockGet = jest.fn();
jest.mock('../../../../../../../interface/api', () => ({
  restClient: {
    get: (...args: unknown[]) => mockGet(...args),
  },
  parseApiError: (err: any) => ({
    message: err?.response?.data?.message || err?.message || 'Unknown error',
  }),
}));

jest.mock('../../../../../../../constants/APIConstants', () => ({
  APIConstants: {
    REST: {
      INTERNAL: {
        GET_STATUS: 'service/rest/internal/ui/status-check',
      },
    },
  },
}));

describe('useMetricHealthApi', () => {
  const mockHealthData = {
    threadDeadlockHealthCheck: { healthy: true, message: 'No deadlocks' },
    databaseHealthCheck: { healthy: true, message: 'Database connected' },
    memoryHealthCheck: { healthy: false, message: 'Memory usage high' },
  };

  const mockNodes = [
    { nodeId: 'node-1', hostname: 'nexus-1', healthy: true },
    { nodeId: 'node-2', hostname: 'nexus-2', healthy: false },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('fetchMetricHealth', () => {
    it('fetches health checks successfully', async () => {
      mockGet.mockResolvedValueOnce(mockHealthData);

      const { result } = renderHook(() => useMetricHealthApi());

      let checks;
      await act(async () => {
        checks = await result.current.fetchMetricHealth();
      });

      expect(mockGet).toHaveBeenCalledWith('service/rest/internal/ui/status-check');
      expect(checks).toHaveLength(3);
      expect(checks).toEqual(
        expect.arrayContaining([
          expect.objectContaining({ name: 'threadDeadlockHealthCheck' }),
          expect.objectContaining({ name: 'databaseHealthCheck' }),
          expect.objectContaining({ name: 'memoryHealthCheck' }),
        ])
      );
    });

    it('sets loading state during fetch', async () => {
      mockGet.mockImplementationOnce(
        () =>
          new Promise((resolve) => {
            setTimeout(() => resolve(mockHealthData), 100);
          })
      );

      const { result } = renderHook(() => useMetricHealthApi());

      expect(result.current.loading).toBe(false);

      let promise: Promise<unknown>;
      await act(async () => {
        promise = result.current.fetchMetricHealth();
      });

      await waitFor(() => {
        expect(result.current.loading).toBe(true);
      });

      await act(async () => {
        await promise!;
      });

      expect(result.current.loading).toBe(false);
    });

    it('handles fetch error', async () => {
      const errorMessage = 'Network error';
      mockGet.mockRejectedValueOnce(new Error(errorMessage));

      const { result } = renderHook(() => useMetricHealthApi());

      await act(async () => {
        try {
          await result.current.fetchMetricHealth();
        } catch (err) {
          // Expected to throw
        }
      });

      expect(result.current.error).toBe(errorMessage);
    });

    it('handles API error response', async () => {
      mockGet.mockRejectedValueOnce({
        response: { data: { message: 'Access denied' } },
      });

      const { result } = renderHook(() => useMetricHealthApi());

      await act(async () => {
        try {
          await result.current.fetchMetricHealth();
        } catch (err) {
          // Expected to throw
        }
      });

      expect(result.current.error).toBe('Access denied');
    });

    it('handles empty response data', async () => {
      mockGet.mockResolvedValueOnce(null);

      const { result } = renderHook(() => useMetricHealthApi());

      let checks;
      await act(async () => {
        checks = await result.current.fetchMetricHealth();
      });

      expect(checks).toEqual([]);
    });
  });

  describe('fetchClusterNodes', () => {
    it('fetches cluster nodes successfully', async () => {
      mockGet.mockResolvedValueOnce(mockNodes);

      const { result } = renderHook(() => useMetricHealthApi());

      let nodes;
      await act(async () => {
        nodes = await result.current.fetchClusterNodes();
      });

      expect(mockGet).toHaveBeenCalledWith('service/rest/beta/status/check/cluster');
      expect(nodes).toEqual(mockNodes);
    });

    it('returns empty array when endpoint fails', async () => {
      const debugSpy = jest.spyOn(console, 'debug').mockImplementation(() => {});
      mockGet.mockRejectedValueOnce(new Error('Not found'));

      const { result } = renderHook(() => useMetricHealthApi());

      let nodes;
      await act(async () => {
        nodes = await result.current.fetchClusterNodes();
      });

      expect(nodes).toEqual([]);
      debugSpy.mockRestore();
    });

    it('returns empty array when response is not an array', async () => {
      mockGet.mockResolvedValueOnce('invalid');

      const { result } = renderHook(() => useMetricHealthApi());

      let nodes;
      await act(async () => {
        nodes = await result.current.fetchClusterNodes();
      });

      expect(nodes).toEqual([]);
    });
  });

  describe('fetchNodeMetricHealth', () => {
    it('fetches node health checks successfully', async () => {
      mockGet.mockResolvedValueOnce(mockHealthData);

      const { result } = renderHook(() => useMetricHealthApi());

      let checks;
      await act(async () => {
        checks = await result.current.fetchNodeMetricHealth('node-1');
      });

      expect(mockGet).toHaveBeenCalledWith('service/rest/beta/status/check/node-1');
      expect(checks).toHaveLength(3);
    });

    it('handles clustered response format with results property', async () => {
      mockGet.mockResolvedValueOnce({ results: mockHealthData, hostname: 'nexus-1' });

      const { result } = renderHook(() => useMetricHealthApi());

      let checks;
      await act(async () => {
        checks = await result.current.fetchNodeMetricHealth('node-1');
      });

      expect(checks).toHaveLength(3);
    });

    it('handles fetch error for node health', async () => {
      mockGet.mockRejectedValueOnce(new Error('Node not found'));

      const { result } = renderHook(() => useMetricHealthApi());

      await act(async () => {
        try {
          await result.current.fetchNodeMetricHealth('invalid-node');
        } catch (err) {
          // Expected to throw
        }
      });

      expect(result.current.error).toBe('Node not found');
    });
  });

  describe('downloadMetricHealth', () => {
    it('creates a download link and triggers click', () => {
      const mockChecks = [
        { name: 'test', result: { healthy: true } },
      ];
      const mockCreateObjectURL = jest.fn().mockReturnValue('blob:url');
      const mockRevokeObjectURL = jest.fn();
      const mockClick = jest.fn();

      global.URL.createObjectURL = mockCreateObjectURL;
      global.URL.revokeObjectURL = mockRevokeObjectURL;

      const mockLink = {
        href: '',
        download: '',
        click: mockClick,
      };

      const { result } = renderHook(() => useMetricHealthApi());

      const createElementSpy = jest.spyOn(document, 'createElement').mockReturnValue(mockLink as any);
      const appendChildSpy = jest.spyOn(document.body, 'appendChild').mockImplementation(() => mockLink as any);
      const removeChildSpy = jest.spyOn(document.body, 'removeChild').mockImplementation(() => mockLink as any);

      act(() => {
        result.current.downloadMetricHealth(mockChecks, 'test.json');
      });

      expect(mockCreateObjectURL).toHaveBeenCalled();
      expect(mockLink.download).toBe('test.json');
      expect(mockClick).toHaveBeenCalled();
      expect(mockRevokeObjectURL).toHaveBeenCalledWith('blob:url');

      createElementSpy.mockRestore();
      appendChildSpy.mockRestore();
      removeChildSpy.mockRestore();
    });

    it('uses default filename when not provided', () => {
      const mockChecks = [{ name: 'test', result: { healthy: true } }];

      global.URL.createObjectURL = jest.fn().mockReturnValue('blob:url');
      global.URL.revokeObjectURL = jest.fn();

      const mockLink = {
        href: '',
        download: '',
        click: jest.fn(),
      };

      const { result } = renderHook(() => useMetricHealthApi());

      const createElementSpy = jest.spyOn(document, 'createElement').mockReturnValue(mockLink as any);
      const appendChildSpy = jest.spyOn(document.body, 'appendChild').mockImplementation(() => mockLink as any);
      const removeChildSpy = jest.spyOn(document.body, 'removeChild').mockImplementation(() => mockLink as any);

      act(() => {
        result.current.downloadMetricHealth(mockChecks);
      });

      expect(mockLink.download).toBe('metric-health.json');

      createElementSpy.mockRestore();
      appendChildSpy.mockRestore();
      removeChildSpy.mockRestore();
    });

    it('converts health checks to proper JSON format', () => {
      const mockChecks = [
        { name: 'check1', result: { healthy: true, message: 'OK' } },
        { name: 'check2', result: { healthy: false, message: 'Error' } },
      ];

      let capturedBlob: Blob | null = null;
      global.URL.createObjectURL = jest.fn((blob: Blob) => {
        capturedBlob = blob;
        return 'blob:url';
      });
      global.URL.revokeObjectURL = jest.fn();

      const mockLink = {
        href: '',
        download: '',
        click: jest.fn(),
      };

      const { result } = renderHook(() => useMetricHealthApi());

      const createElementSpy = jest.spyOn(document, 'createElement').mockReturnValue(mockLink as any);
      const appendChildSpy = jest.spyOn(document.body, 'appendChild').mockImplementation(() => mockLink as any);
      const removeChildSpy = jest.spyOn(document.body, 'removeChild').mockImplementation(() => mockLink as any);

      act(() => {
        result.current.downloadMetricHealth(mockChecks);
      });

      expect(capturedBlob).not.toBeNull();
      expect(capturedBlob?.type).toBe('application/json');

      createElementSpy.mockRestore();
      appendChildSpy.mockRestore();
      removeChildSpy.mockRestore();
    });
  });

  describe('setError', () => {
    it('can set and clear error', async () => {
      const { result } = renderHook(() => useMetricHealthApi());

      expect(result.current.error).toBeNull();

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
