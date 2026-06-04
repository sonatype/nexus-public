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

import type { ApiPermissionsResponseDto } from '../../types';

jest.mock('../../../../../../../../interface/api', () => ({
  ...jest.requireActual('../../../../../../../../interface/api'),
  restClient: {
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
    delete: jest.fn(),
  },
}));

import { restClient } from '../../../../../../../../interface/api';
import { useEndpointPermissions } from '../useEndpointPermissions';

const mockGet = restClient.get as jest.MockedFunction<typeof restClient.get>;

const MOCK_RESPONSE: ApiPermissionsResponseDto = {
  endpoints: [
    {
      httpMethod: 'GET',
      pathPattern: '/service/rest/v1/status',
      permissions: [],
      description: 'Health check',
      tag: 'Status',
      authenticated: false,
    },
  ],
  generatedAt: '2026-03-24T00:00:00Z',
  totalEndpoints: 1,
  unmappedEndpoints: 0,
};

describe('useEndpointPermissions', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('fetches permissions on mount and returns data', async () => {
    mockGet.mockResolvedValueOnce(MOCK_RESPONSE);

    const { result } = renderHook(() => useEndpointPermissions());

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.data).toEqual(MOCK_RESPONSE);
    expect(result.current.error).toBeNull();
    expect(mockGet).toHaveBeenCalled();
  });

  it('uses session cache on subsequent mounts without refetching', () => {
    const { result } = renderHook(() => useEndpointPermissions());

    expect(result.current.loading).toBe(false);
    expect(result.current.data).toEqual(MOCK_RESPONSE);
    expect(mockGet).not.toHaveBeenCalled();
  });

  it('exposes a refetch function that reloads data', async () => {
    const { result } = renderHook(() => useEndpointPermissions());
    await waitFor(() => expect(result.current.loading).toBe(false));

    const updated: ApiPermissionsResponseDto = { ...MOCK_RESPONSE, totalEndpoints: 5 };
    mockGet.mockResolvedValueOnce(updated);

    await act(async () => {
      await result.current.refetch();
    });

    expect(result.current.data).toEqual(updated);
  });

  it('sets error state on fetch failure via refetch', async () => {
    const { result } = renderHook(() => useEndpointPermissions());
    await waitFor(() => expect(result.current.loading).toBe(false));

    mockGet.mockRejectedValueOnce(new Error('Network error'));

    await act(async () => {
      await result.current.refetch();
    });

    expect(result.current.data).toBeNull();
    expect(result.current.error).toBe('Network error');
  });

  it('uses fallback error message for non-Error throws', async () => {
    const { result } = renderHook(() => useEndpointPermissions());
    await waitFor(() => expect(result.current.loading).toBe(false));

    mockGet.mockRejectedValueOnce('something');

    await act(async () => {
      await result.current.refetch();
    });

    expect(result.current.error).toBe('Failed to load API permissions');
  });
});
