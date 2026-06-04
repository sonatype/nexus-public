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

import { renderHook, waitFor } from '@testing-library/react';

import type { MergedApiEndpoint } from '../../utils/mergeSwaggerPermissions';

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
import { useViewAsUserAccess } from '../useViewAsUserAccess';

const mockPost = restClient.post as jest.MockedFunction<typeof restClient.post>;

function endpoint(overrides: Partial<MergedApiEndpoint> = {}): MergedApiEndpoint {
  return {
    httpMethod: 'GET',
    swaggerPathKey: '/v1/status',
    fullPath: '/service/rest/v1/status',
    tag: 'Status',
    permission: null,
    ...overrides,
  };
}

describe('useViewAsUserAccess', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('returns null accessById when disabled', () => {
    const ep = [endpoint()];
    const { result } = renderHook(() =>
      useViewAsUserAccess(ep, 'otherUser', 'currentUser', false)
    );
    expect(result.current.accessById).toBeNull();
    expect(result.current.loading).toBe(false);
    expect(result.current.error).toBeNull();
  });

  it('returns null accessById when viewAsUserId is null', () => {
    const ep = [endpoint()];
    const { result } = renderHook(() =>
      useViewAsUserAccess(ep, null, 'currentUser', true)
    );
    expect(result.current.accessById).toBeNull();
    expect(result.current.loading).toBe(false);
  });

  it('returns null accessById when viewing as yourself', () => {
    const ep = [endpoint()];
    const { result } = renderHook(() =>
      useViewAsUserAccess(ep, 'admin', 'admin', true)
    );
    expect(result.current.accessById).toBeNull();
    expect(result.current.loading).toBe(false);
  });

  it('returns empty map for zero endpoints', async () => {
    const ep: MergedApiEndpoint[] = [];
    const { result } = renderHook(() =>
      useViewAsUserAccess(ep, 'viewer', 'admin', true)
    );

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.accessById).toEqual({});
    expect(mockPost).not.toHaveBeenCalled();
  });

  it('fetches access for each endpoint when viewing as another user', async () => {
    mockPost.mockResolvedValue({ hasAccess: true } as any);

    const ep = [endpoint()];
    const { result } = renderHook(() =>
      useViewAsUserAccess(ep, 'otherUser', 'currentUser', true)
    );

    expect(result.current.loading).toBe(true);

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.accessById).toBeDefined();
    expect(Object.keys(result.current.accessById!)).toHaveLength(1);
    expect(mockPost).toHaveBeenCalledTimes(1);
  });

  it('maps hasAccess=true to granted and hasAccess=false to denied', async () => {
    mockPost
      .mockResolvedValueOnce({ hasAccess: true } as any)
      .mockResolvedValueOnce({ hasAccess: false } as any);

    const ep = [
      endpoint({ httpMethod: 'GET', fullPath: '/service/rest/v1/status' }),
      endpoint({ httpMethod: 'POST', fullPath: '/service/rest/v1/repositories' }),
    ];

    const { result } = renderHook(() =>
      useViewAsUserAccess(ep, 'viewer', 'admin', true)
    );

    await waitFor(() => expect(result.current.loading).toBe(false));

    const access = result.current.accessById!;
    expect(access['GET|/service/rest/v1/status']).toBe('granted');
    expect(access['POST|/service/rest/v1/repositories']).toBe('denied');
  });

  it('maps failed individual checks to unknown', async () => {
    mockPost
      .mockResolvedValueOnce({ hasAccess: true } as any)
      .mockRejectedValueOnce(new Error('forbidden'));

    const ep = [
      endpoint({ httpMethod: 'GET', fullPath: '/service/rest/v1/status' }),
      endpoint({ httpMethod: 'POST', fullPath: '/service/rest/v1/repositories' }),
    ];

    const { result } = renderHook(() =>
      useViewAsUserAccess(ep, 'viewer', 'admin', true)
    );

    await waitFor(() => expect(result.current.loading).toBe(false));

    const access = result.current.accessById!;
    expect(access['GET|/service/rest/v1/status']).toBe('granted');
    expect(access['POST|/service/rest/v1/repositories']).toBe('unknown');
  });

  it('sends userId, endpoint, and method in each POST', async () => {
    mockPost.mockResolvedValue({ hasAccess: true } as any);

    const ep = [endpoint({ httpMethod: 'DELETE', fullPath: '/service/rest/v1/repos/{id}' })];
    const { result } = renderHook(() =>
      useViewAsUserAccess(ep, 'testUser', 'admin', true)
    );

    await waitFor(() => expect(result.current.loading).toBe(false));

    const [, payload] = mockPost.mock.calls[0];
    expect(payload).toEqual(
      expect.objectContaining({
        userId: 'testUser',
        endpoint: '/service/rest/v1/repos/{id}',
        method: 'DELETE',
      })
    );
  });
});
