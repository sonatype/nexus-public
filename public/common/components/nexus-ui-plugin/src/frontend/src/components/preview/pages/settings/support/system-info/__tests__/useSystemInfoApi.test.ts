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

import { useSystemInfoApi } from '../useSystemInfoApi';

// Mock the local API module used by useSystemInfoApi
const mockGet = jest.fn();
jest.mock('../../../../../../../interface/api', () => ({
  restClient: {
    get: (...args: unknown[]) => mockGet(...args),
  },
  parseApiError: (err: any) => ({
    message: err?.response?.data?.message || err?.message || 'Unknown error',
  }),
}));

describe('useSystemInfoApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('fetchSystemInfo', () => {
    it('fetches system information successfully', async () => {
      const mockData = {
        'nexus-status': { version: '3.88.0' },
      };

      mockGet.mockResolvedValueOnce(mockData);

      const { result } = renderHook(() => useSystemInfoApi());

      let data;
      await act(async () => {
        data = await result.current.fetchSystemInfo();
      });

      expect(mockGet).toHaveBeenCalledWith('service/rest/atlas/system-information');
      expect(data).toEqual(mockData);
    });

    it('sets loading state during fetch', async () => {
      let resolvePromise: (value: unknown) => void;
      const promise = new Promise((resolve) => {
        resolvePromise = resolve;
      });

      mockGet.mockReturnValueOnce(promise);

      const { result } = renderHook(() => useSystemInfoApi());

      expect(result.current.loading).toBe(false);

      act(() => {
        result.current.fetchSystemInfo();
      });

      expect(result.current.loading).toBe(true);

      await act(async () => {
        resolvePromise!({});
      });

      expect(result.current.loading).toBe(false);
    });

    it('handles fetch error', async () => {
      const errorMessage = 'Network error';
      mockGet.mockRejectedValueOnce(new Error(errorMessage));

      const { result } = renderHook(() => useSystemInfoApi());

      await act(async () => {
        try {
          await result.current.fetchSystemInfo();
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

      const { result } = renderHook(() => useSystemInfoApi());

      await act(async () => {
        try {
          await result.current.fetchSystemInfo();
        } catch (err) {
          // Expected to throw
        }
      });

      expect(result.current.error).toBe('Access denied');
    });
  });

  describe('fetchSystemInfoHA', () => {
    it('fetches HA system information successfully', async () => {
      const mockData = {
        'node-1': { 'nexus-status': { version: '3.88.0' } },
        'node-2': { 'nexus-status': { version: '3.88.0' } },
      };

      mockGet.mockResolvedValueOnce(mockData);

      const { result } = renderHook(() => useSystemInfoApi());

      let data;
      await act(async () => {
        data = await result.current.fetchSystemInfoHA();
      });

      expect(mockGet).toHaveBeenCalledWith('service/rest/beta/system/information');
      expect(data).toEqual(mockData);
    });
  });

  describe('fetchActiveNodes', () => {
    it('fetches active nodes successfully', async () => {
      const mockNodes = [
        { nodeId: 'node-1', friendlyName: 'Node 1' },
        { nodeId: 'node-2', friendlyName: 'Node 2' },
      ];

      mockGet.mockResolvedValueOnce(mockNodes);

      const { result } = renderHook(() => useSystemInfoApi());

      let nodes;
      await act(async () => {
        nodes = await result.current.fetchActiveNodes();
      });

      expect(mockGet).toHaveBeenCalledWith('service/rest/internal/ui/supportzip/activenodes');
      expect(nodes).toEqual(mockNodes);
    });

    it('returns empty array when endpoint fails', async () => {
      const debugSpy = jest.spyOn(console, 'debug').mockImplementation(() => {});
      mockGet.mockRejectedValueOnce(new Error('Not found'));

      const { result } = renderHook(() => useSystemInfoApi());

      let nodes;
      await act(async () => {
        nodes = await result.current.fetchActiveNodes();
      });

      expect(nodes).toEqual([]);
      debugSpy.mockRestore();
    });

    it('returns empty array when response is not an array', async () => {
      mockGet.mockResolvedValueOnce('invalid');

      const { result } = renderHook(() => useSystemInfoApi());

      let nodes;
      await act(async () => {
        nodes = await result.current.fetchActiveNodes();
      });

      expect(nodes).toEqual([]);
    });
  });

  describe('downloadSystemInfo', () => {
    it('creates a download link and triggers click', () => {
      const mockData = { 'nexus-status': { version: '3.88.0' } };
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

      const { result } = renderHook(() => useSystemInfoApi());

      const createElementSpy = jest.spyOn(document, 'createElement').mockReturnValue(mockLink as any);
      const appendChildSpy = jest.spyOn(document.body, 'appendChild').mockImplementation(() => mockLink as any);
      const removeChildSpy = jest.spyOn(document.body, 'removeChild').mockImplementation(() => mockLink as any);

      act(() => {
        result.current.downloadSystemInfo(mockData, 'test.json');
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
      const mockData = { 'nexus-status': { version: '3.88.0' } };
      
      global.URL.createObjectURL = jest.fn().mockReturnValue('blob:url');
      global.URL.revokeObjectURL = jest.fn();

      const mockLink = {
        href: '',
        download: '',
        click: jest.fn(),
      };

      const { result } = renderHook(() => useSystemInfoApi());

      const createElementSpy = jest.spyOn(document, 'createElement').mockReturnValue(mockLink as any);
      const appendChildSpy = jest.spyOn(document.body, 'appendChild').mockImplementation(() => mockLink as any);
      const removeChildSpy = jest.spyOn(document.body, 'removeChild').mockImplementation(() => mockLink as any);

      act(() => {
        result.current.downloadSystemInfo(mockData);
      });

      expect(mockLink.download).toBe('system-information.json');

      createElementSpy.mockRestore();
      appendChildSpy.mockRestore();
      removeChildSpy.mockRestore();
    });
  });

  describe('copyToClipboard', () => {
    it('copies data to clipboard successfully', async () => {
      const mockData = { 'nexus-status': { version: '3.88.0' } };
      const mockWriteText = jest.fn().mockResolvedValue(undefined);

      Object.assign(navigator, {
        clipboard: { writeText: mockWriteText },
      });

      const { result } = renderHook(() => useSystemInfoApi());

      let success;
      await act(async () => {
        success = await result.current.copyToClipboard(mockData);
      });

      expect(mockWriteText).toHaveBeenCalledWith(JSON.stringify(mockData, null, 2));
      expect(success).toBe(true);
    });

    it('returns false when clipboard copy fails', async () => {
      const errorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
      const mockData = { 'nexus-status': { version: '3.88.0' } };
      const mockWriteText = jest.fn().mockRejectedValue(new Error('Failed'));

      Object.assign(navigator, {
        clipboard: { writeText: mockWriteText },
      });

      const { result } = renderHook(() => useSystemInfoApi());

      let success;
      await act(async () => {
        success = await result.current.copyToClipboard(mockData);
      });

      expect(success).toBe(false);
      errorSpy.mockRestore();
    });
  });

  describe('setError', () => {
    it('can set and clear error', async () => {
      const { result } = renderHook(() => useSystemInfoApi());

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
