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
import { useCrowdApi } from '../useCrowdApi';

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
}));

const CROWD_URL = '/service/rest/v1/security/atlassian-crowd';

describe('useCrowdApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  const baseConfig = {
    enabled: true,
    realmActive: true,
    applicationName: 'nexus',
    applicationPassword: 'secret',
    serverUrl: 'https://crowd.example.com',
    timeout: 30,
    useTrustStoreForUrl: false,
  };

  it('fetches Crowd config successfully', async () => {
    mockRestClient.get.mockResolvedValue(baseConfig);

    const { result } = renderHook(() => useCrowdApi());

    let config;
    await act(async () => {
      config = await result.current.fetchConfig();
    });

    expect(config).toEqual(expect.objectContaining(baseConfig));
    expect(mockRestClient.get).toHaveBeenCalledWith(CROWD_URL);
  });

  it('throws error when fetch fails', async () => {
    const errorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    mockRestClient.get.mockRejectedValue({ response: { data: { message: 'Not authorized' } } });

    const { result } = renderHook(() => useCrowdApi());

    await expect(result.current.fetchConfig()).rejects.toThrow('Not authorized');
    errorSpy.mockRestore();
  });

  it('saves Crowd config successfully', async () => {
    mockRestClient.put.mockResolvedValue({});

    const { result } = renderHook(() => useCrowdApi());

    await act(async () => {
      await result.current.saveConfig(baseConfig);
    });

    expect(mockRestClient.put).toHaveBeenCalledWith(CROWD_URL, baseConfig);
  });

  it('verifies connection successfully', async () => {
    mockRestClient.post.mockResolvedValue({});

    const { result } = renderHook(() => useCrowdApi());

    await act(async () => {
      await result.current.verifyConnection(baseConfig);
    });

    expect(mockRestClient.post).toHaveBeenCalledWith(`${CROWD_URL}/verify-connection`, baseConfig);
  });

  it('returns message from verifyConnection failure', async () => {
    mockRestClient.post.mockRejectedValue({ response: { data: { message: 'Connection refused' } } });

    const { result } = renderHook(() => useCrowdApi());

    await act(async () => {
      await expect(result.current.verifyConnection(baseConfig)).rejects.toThrow('Connection refused');
    });
  });

  it('clears cache successfully', async () => {
    mockRestClient.post.mockResolvedValue({});

    const { result } = renderHook(() => useCrowdApi());

    await act(async () => {
      await result.current.clearCache();
    });

    expect(mockRestClient.post).toHaveBeenCalledWith(`${CROWD_URL}/clear-cache`);
  });

  it('sets loading state during save', async () => {
    let resolvePromise: (value: unknown) => void;
    const pendingPromise = new Promise((resolve) => {
      resolvePromise = resolve;
    });
    mockRestClient.put.mockReturnValueOnce(pendingPromise as any);

    const { result } = renderHook(() => useCrowdApi());

    expect(result.current.loading).toBe(false);

    let savePromise: Promise<void>;
    await act(async () => {
      savePromise = result.current.saveConfig(baseConfig);
    });

    await waitFor(() => {
      expect(result.current.loading).toBe(true);
    });

    await act(async () => {
      resolvePromise!({});
      await savePromise!;
    });

    expect(result.current.loading).toBe(false);
  });

  it('sets error state on save failure', async () => {
    mockRestClient.put.mockRejectedValue({ message: 'Save failed' });

    const { result } = renderHook(() => useCrowdApi());

    await act(async () => {
      await expect(result.current.saveConfig(baseConfig)).rejects.toThrow('Save failed');
    });
    await waitFor(() => {
      expect(result.current.error).toBe('Save failed');
    });
  });

  it('clears error with setError', () => {
    const { result } = renderHook(() => useCrowdApi());

    act(() => {
      result.current.setError('Test error');
    });

    act(() => {
      result.current.setError(null);
    });

    expect(result.current.error).toBeNull();
  });
});

