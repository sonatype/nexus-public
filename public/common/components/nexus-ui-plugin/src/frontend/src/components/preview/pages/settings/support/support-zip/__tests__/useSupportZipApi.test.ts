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

import { useSupportZipApi } from '../useSupportZipApi';
import { SUPPORT_ZIP_API, DEFAULT_SUPPORT_ZIP_PARAMS, NodeInfo } from '../types';

jest.mock('../../../../../../../interface/api', () => ({
  restClient: {
    get: jest.fn(),
    post: jest.fn(),
    delete: jest.fn(),
  },
  parseApiError: jest.fn((err: any) => ({ message: err?.message || 'Unknown error' })),
}));

import { restClient } from '../../../../../../../interface/api';
const mockedRestClient = restClient as jest.Mocked<typeof restClient>;

const SUPPORT_ZIP_BASE = 'service/rest/internal/ui/supportzip/';
const ACTIVE_NODES_URL = 'service/rest/internal/ui/supportzip/activenodes';
const STATUS_BASE = 'service/rest/internal/ui/supportzip/status/';
const CLEAR_BASE = 'service/rest/internal/ui/supportzip/clear/';

const sampleNode: NodeInfo = {
  nodeId: 'node-a',
  hostname: 'host-a',
  status: 'NOT_CREATED',
};

describe('useSupportZipApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('createSupportZip', () => {
    it('creates support ZIP successfully', async () => {
      const mockResponse = {
        file: '/path/to/support.zip',
        name: 'support-2024-01-01.zip',
        size: '10 MB',
        truncated: false,
      };
      mockedRestClient.post.mockResolvedValueOnce(mockResponse);

      const { result } = renderHook(() => useSupportZipApi());

      let response: any;
      await act(async () => {
        response = await result.current.createSupportZip(DEFAULT_SUPPORT_ZIP_PARAMS);
      });

      expect(mockedRestClient.post).toHaveBeenCalledWith(
        SUPPORT_ZIP_API.CREATE,
        DEFAULT_SUPPORT_ZIP_PARAMS
      );
      expect(response).toEqual(mockResponse);
    });

    it('handles create error', async () => {
      mockedRestClient.post.mockRejectedValueOnce(new Error('Failed to create ZIP'));

      const { result } = renderHook(() => useSupportZipApi());

      await act(async () => {
        await expect(
          result.current.createSupportZip(DEFAULT_SUPPORT_ZIP_PARAMS)
        ).rejects.toThrow('Failed to create ZIP');
      });

      expect(result.current.error).toBe('Failed to create ZIP');
    });

    it('sets loading state while creating', async () => {
      let resolvePromise: (value: any) => void;
      const pendingPromise = new Promise((resolve) => {
        resolvePromise = resolve;
      });
      mockedRestClient.post.mockReturnValueOnce(pendingPromise as any);

      const { result } = renderHook(() => useSupportZipApi());

      const createPromise = result.current.createSupportZip(DEFAULT_SUPPORT_ZIP_PARAMS);

      await waitFor(() => {
        expect(result.current.loading).toBe(true);
      });

      await act(async () => {
        resolvePromise!({ file: 'test.zip', name: 'test', size: '1MB', truncated: false });
        await createPromise;
      });

      expect(result.current.loading).toBe(false);
    });
  });

  describe('fetchActiveNodes', () => {
    it('GETs the active nodes endpoint', async () => {
      mockedRestClient.get.mockResolvedValueOnce([sampleNode]);

      const { result } = renderHook(() => useSupportZipApi());

      let nodes: NodeInfo[] = [];
      await act(async () => {
        nodes = await result.current.fetchActiveNodes();
      });

      expect(mockedRestClient.get).toHaveBeenCalledWith(ACTIVE_NODES_URL);
      expect(nodes).toEqual([sampleNode]);
    });

    it('captures fetch errors on the hook state', async () => {
      mockedRestClient.get.mockRejectedValueOnce(new Error('boom'));

      const { result } = renderHook(() => useSupportZipApi());

      await act(async () => {
        await expect(result.current.fetchActiveNodes()).rejects.toThrow('boom');
      });

      expect(result.current.error).toBe('boom');
    });
  });

  describe('fetchNodeStatus', () => {
    it('GETs the per-node status endpoint', async () => {
      mockedRestClient.get.mockResolvedValueOnce(sampleNode);

      const { result } = renderHook(() => useSupportZipApi());

      let node: NodeInfo;
      await act(async () => {
        node = await result.current.fetchNodeStatus('node-a');
      });

      expect(mockedRestClient.get).toHaveBeenCalledWith(`${STATUS_BASE}node-a`);
      expect(node!).toEqual(sampleNode);
    });
  });

  describe('generateForNode', () => {
    it('POSTs to the per-node supportzip endpoint with hostname in body', async () => {
      mockedRestClient.post.mockResolvedValueOnce({ ...sampleNode, status: 'CREATING' });

      const { result } = renderHook(() => useSupportZipApi());

      await act(async () => {
        await result.current.generateForNode('node-a', DEFAULT_SUPPORT_ZIP_PARAMS, 'host-a');
      });

      expect(mockedRestClient.post).toHaveBeenCalledWith(`${SUPPORT_ZIP_BASE}node-a`, {
        ...DEFAULT_SUPPORT_ZIP_PARAMS,
        hostname: 'host-a',
      });
    });
  });

  describe('clearNode', () => {
    it('DELETEs the per-node clear endpoint', async () => {
      mockedRestClient.delete.mockResolvedValueOnce(undefined);

      const { result } = renderHook(() => useSupportZipApi());

      await act(async () => {
        await result.current.clearNode('node-a');
      });

      expect(mockedRestClient.delete).toHaveBeenCalledWith(`${CLEAR_BASE}node-a`);
    });
  });

  describe('getDownloadUrl', () => {
    it('returns correct download URL', () => {
      const { result } = renderHook(() => useSupportZipApi());

      const url = result.current.getDownloadUrl('support.zip');

      expect(url).toBe('service/rest/wonderland/download/support.zip');
    });
  });

  describe('setError', () => {
    it('can set and clear error', async () => {
      const { result } = renderHook(() => useSupportZipApi());

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
