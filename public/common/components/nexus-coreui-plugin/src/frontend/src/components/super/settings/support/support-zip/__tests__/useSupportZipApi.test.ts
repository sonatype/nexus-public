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
import { SUPPORT_ZIP_API, DEFAULT_SUPPORT_ZIP_PARAMS } from '../types';

// Mock the REST API from @sonatype/nexus-ui-plugin
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  restClient: {
    post: jest.fn(),
  },
  parseApiError: jest.fn((err: any) => ({ message: err.message || 'Unknown error' })),
}));

import { restClient } from '@/utils/api';
const mockedRestClient = restClient as jest.Mocked<typeof restClient>;

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
      // restClient returns data directly, not { data: ... }
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
      const errorMessage = 'Failed to create ZIP';
      mockedRestClient.post.mockRejectedValueOnce(new Error(errorMessage));

      const { result } = renderHook(() => useSupportZipApi());

      await act(async () => {
        await expect(
          result.current.createSupportZip(DEFAULT_SUPPORT_ZIP_PARAMS)
        ).rejects.toThrow(errorMessage);
      });

      expect(result.current.error).toBe(errorMessage);
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
        // restClient returns data directly
        resolvePromise!({ file: 'test.zip', name: 'test', size: '1MB', truncated: false });
        await createPromise;
      });

      expect(result.current.loading).toBe(false);
    });
  });

  describe('createHaSupportZips', () => {
    it('creates HA support ZIPs successfully', async () => {
      const mockResponses = [
        { file: '/path/to/node1.zip', name: 'node1.zip', size: '10 MB', truncated: false },
        { file: '/path/to/node2.zip', name: 'node2.zip', size: '12 MB', truncated: false },
      ];
      // restClient returns data directly, not { data: ... }
      mockedRestClient.post.mockResolvedValueOnce(mockResponses);

      const { result } = renderHook(() => useSupportZipApi());

      let responses: any;
      await act(async () => {
        responses = await result.current.createHaSupportZips(DEFAULT_SUPPORT_ZIP_PARAMS);
      });

      expect(mockedRestClient.post).toHaveBeenCalledWith(
        SUPPORT_ZIP_API.CREATE_HA,
        DEFAULT_SUPPORT_ZIP_PARAMS
      );
      expect(responses).toEqual(mockResponses);
    });

    it('handles HA create error', async () => {
      const errorMessage = 'Failed to create HA ZIPs';
      mockedRestClient.post.mockRejectedValueOnce(new Error(errorMessage));

      const { result } = renderHook(() => useSupportZipApi());

      await act(async () => {
        await expect(
          result.current.createHaSupportZips(DEFAULT_SUPPORT_ZIP_PARAMS)
        ).rejects.toThrow(errorMessage);
      });

      expect(result.current.error).toBe(errorMessage);
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

