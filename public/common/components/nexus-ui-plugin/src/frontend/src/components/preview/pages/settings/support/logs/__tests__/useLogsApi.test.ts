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

import { useLogsApi } from '../useLogsApi';
import { LOGS_API } from '../types';

const mockRestClient = {
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
};

// Mock the local API module used by useLogsApi
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

describe('useLogsApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('fetchLogs', () => {
    it('fetches log files successfully', async () => {
      const mockLogs = [
        { fileName: 'nexus.log', size: 1024, lastModified: 1699999999999 },
        { fileName: 'request.log', size: 2048, lastModified: 1699888888888 },
      ];
      mockRestClient.get.mockResolvedValueOnce(mockLogs);

      const { result } = renderHook(() => useLogsApi());

      let logs: any;
      await act(async () => {
        logs = await result.current.fetchLogs();
      });

      expect(mockRestClient.get).toHaveBeenCalledWith(LOGS_API.LIST);
      expect(logs).toEqual(mockLogs);
    });

    it('handles fetch error', async () => {
      const errorMessage = 'Network Error';
      mockRestClient.get.mockRejectedValueOnce(new Error(errorMessage));

      const { result } = renderHook(() => useLogsApi());

      await act(async () => {
        await expect(result.current.fetchLogs()).rejects.toThrow();
      });

      expect(result.current.error).toBe(errorMessage);
    });

    it('returns empty array when response is not an array', async () => {
      mockRestClient.get.mockResolvedValueOnce(null);

      const { result } = renderHook(() => useLogsApi());

      let logs: any;
      await act(async () => {
        logs = await result.current.fetchLogs();
      });

      expect(logs).toEqual([]);
    });
  });

  describe('fetchLogContent', () => {
    it('fetches log content successfully', async () => {
      const mockContent = '2024-01-01 12:00:00 INFO  [main] - Test log message';
      mockRestClient.get.mockResolvedValueOnce(mockContent);

      const { result } = renderHook(() => useLogsApi());

      let content: string = '';
      await act(async () => {
        content = await result.current.fetchLogContent('nexus.log', -25000);
      });

      expect(mockRestClient.get).toHaveBeenCalledWith(
        LOGS_API.VIEW('nexus.log'),
        { params: { bytesCount: -25000 } }
      );
      expect(content).toBe(mockContent);
    });

    it('handles fetch content error', async () => {
      const errorMessage = 'Failed to load log';
      mockRestClient.get.mockRejectedValueOnce(new Error(errorMessage));

      const { result } = renderHook(() => useLogsApi());

      await act(async () => {
        await expect(result.current.fetchLogContent('nexus.log')).rejects.toThrow(errorMessage);
      });
    });
  });

  describe('insertMark', () => {
    it('inserts mark successfully', async () => {
      mockRestClient.post.mockResolvedValueOnce({});

      const { result } = renderHook(() => useLogsApi());

      await act(async () => {
        await result.current.insertMark('TEST_MARK');
      });

      expect(mockRestClient.post).toHaveBeenCalledWith(
        LOGS_API.MARK,
        'TEST_MARK',
        { headers: { 'Content-Type': 'text/plain' } }
      );
    });

    it('handles insert mark error', async () => {
      const errorMessage = 'Failed to insert mark';
      mockRestClient.post.mockRejectedValueOnce(new Error(errorMessage));

      const { result } = renderHook(() => useLogsApi());

      await act(async () => {
        await expect(result.current.insertMark('TEST')).rejects.toThrow();
      });

      expect(result.current.error).toBe(errorMessage);
    });
  });

  describe('getDownloadUrl', () => {
    it('returns correct download URL', () => {
      const { result } = renderHook(() => useLogsApi());

      const url = result.current.getDownloadUrl('nexus.log');

      expect(url).toBe('/service/rest/internal/logging/logs/nexus.log');
    });

    it('encodes special characters in filename', () => {
      const { result } = renderHook(() => useLogsApi());

      const url = result.current.getDownloadUrl('test log file.log');

      expect(url).toBe('/service/rest/internal/logging/logs/test%20log%20file.log');
    });
  });

  describe('setError', () => {
    it('can set and clear error', async () => {
      const { result } = renderHook(() => useLogsApi());

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


