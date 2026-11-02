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
import { useCdnDefaults } from '../useCdnDefaults';

const mockRestClient = {
  get: jest.fn(),
  put: jest.fn(),
};

jest.mock('../../../../../../../interface/api', () => ({
  ...jest.requireActual('../../../../../../../interface/api'),
  restClient: {
    get: (...args: unknown[]) => mockRestClient.get(...args),
    put: (...args: unknown[]) => mockRestClient.put(...args),
  },
  parseApiError: jest.fn((err) => ({
    status: err?.response?.status ?? 0,
    message: err?.response?.data?.message || err?.message || 'An error occurred',
  })),
}));

const ENDPOINT = '/service/rest/v1/repositories/global-distribution/settings';

describe('useCdnDefaults', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('fetches the current default on mount', async () => {
    mockRestClient.get.mockResolvedValueOnce({ defaultForNewRepositories: true });

    const { result } = renderHook(() => useCdnDefaults());

    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });

    expect(result.current.defaultForNewRepositories).toBe(true);
    expect(result.current.isLoading).toBe(false);
    expect(result.current.hasLoaded).toBe(true);
    expect(result.current.error).toBeNull();
    expect(mockRestClient.get).toHaveBeenCalledWith(ENDPOINT);
  });

  it('optimistically updates on setDefault and calls PUT', async () => {
    mockRestClient.get.mockResolvedValueOnce({ defaultForNewRepositories: false });
    mockRestClient.put.mockResolvedValueOnce(undefined);

    const { result } = renderHook(() => useCdnDefaults());

    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });

    expect(result.current.defaultForNewRepositories).toBe(false);

    await act(async () => {
      await result.current.setDefault(true);
    });

    expect(result.current.defaultForNewRepositories).toBe(true);
    expect(mockRestClient.put).toHaveBeenCalledWith(ENDPOINT, { defaultForNewRepositories: true });
  });

  it('rolls back on PUT failure', async () => {
    mockRestClient.get.mockResolvedValueOnce({ defaultForNewRepositories: false });
    mockRestClient.put.mockRejectedValueOnce({ response: { status: 500 } });

    const { result } = renderHook(() => useCdnDefaults());

    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });

    expect(result.current.defaultForNewRepositories).toBe(false);

    await act(async () => {
      await result.current.setDefault(true);
    });

    expect(result.current.defaultForNewRepositories).toBe(false);
    expect(result.current.error).not.toBeNull();
    expect(result.current.error?.status).toBe(500);
  });

  it('exposes a 404 status via error for consumers to act on', async () => {
    mockRestClient.get.mockRejectedValueOnce({ response: { status: 404 } });

    const { result } = renderHook(() => useCdnDefaults());

    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });

    expect(result.current.isLoading).toBe(false);
    expect(result.current.error).not.toBeNull();
    expect(result.current.error?.status).toBe(404);
  });

  it('sets hasLoaded to true after successful GET', async () => {
    mockRestClient.get.mockResolvedValueOnce({ defaultForNewRepositories: false });

    const { result } = renderHook(() => useCdnDefaults());

    expect(result.current.hasLoaded).toBe(false);

    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });

    expect(result.current.hasLoaded).toBe(true);
  });

  it('keeps hasLoaded false after failed GET', async () => {
    mockRestClient.get.mockRejectedValueOnce({ response: { status: 500 } });

    const { result } = renderHook(() => useCdnDefaults());

    expect(result.current.hasLoaded).toBe(false);

    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });

    expect(result.current.hasLoaded).toBe(false);
    expect(result.current.error).not.toBeNull();
  });

  it('does not call the Enterprise-only endpoint when disabled', async () => {
    const { result } = renderHook(() => useCdnDefaults({ enabled: false }));

    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });

    expect(mockRestClient.get).not.toHaveBeenCalled();
    expect(result.current.isLoading).toBe(false);
    expect(result.current.hasLoaded).toBe(false);
    expect(result.current.error).toBeNull();
    expect(result.current.defaultForNewRepositories).toBe(false);
  });

  it('fetches once enabled turns true', async () => {
    mockRestClient.get.mockResolvedValueOnce({ defaultForNewRepositories: true });

    const { result, rerender } = renderHook(
      ({ enabled }: { enabled: boolean }) => useCdnDefaults({ enabled }),
      { initialProps: { enabled: false } },
    );
    expect(mockRestClient.get).not.toHaveBeenCalled();

    rerender({ enabled: true });
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });

    expect(mockRestClient.get).toHaveBeenCalledWith(ENDPOINT);
    expect(result.current.defaultForNewRepositories).toBe(true);
    expect(result.current.hasLoaded).toBe(true);
    expect(result.current.isLoading).toBe(false);
  });

  it('clears loading when enabled turns false mid-flight', async () => {
    mockRestClient.get.mockReturnValueOnce(new Promise(() => {}));

    const { result, rerender } = renderHook(
      ({ enabled }: { enabled: boolean }) => useCdnDefaults({ enabled }),
      { initialProps: { enabled: true } },
    );

    expect(result.current.isLoading).toBe(true);

    rerender({ enabled: false });
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });

    expect(result.current.isLoading).toBe(false);
  });

  it('clears a previous error when enabled turns false', async () => {
    mockRestClient.get.mockRejectedValueOnce({ response: { status: 404 } });

    const { result, rerender } = renderHook(
      ({ enabled }: { enabled: boolean }) => useCdnDefaults({ enabled }),
      { initialProps: { enabled: true } },
    );

    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });

    expect(result.current.error).not.toBeNull();

    rerender({ enabled: false });
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });

    expect(result.current.error).toBeNull();
    expect(result.current.isLoading).toBe(false);
  });
});
