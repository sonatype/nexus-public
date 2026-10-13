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

import { act, renderHook, waitFor } from '@testing-library/react';

import { useRepositoryList } from '../useRepositoryList';
import { useRepositoryType, __resetRepositoryTypeCache } from '../../useRepositoryType';
import { fetchBrowsableRepositories } from '../../browse.api';

jest.mock('../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    checkPermission: jest.fn().mockReturnValue(false),
    state: jest.fn().mockReturnValue({ getValue: jest.fn().mockReturnValue({ enabled: false }) }),
    showSuccessMessage: jest.fn(),
    showErrorMessage: jest.fn(),
  },
}));

jest.mock('../../../../../../interface/api', () => {
  const actual = jest.requireActual('../../../../../../interface/api');
  return {
    ...actual,
    restClient: { get: jest.fn().mockResolvedValue([]), put: jest.fn(), post: jest.fn() },
  };
});

jest.mock('../../browse.api', () => {
  const actual = jest.requireActual('../../browse.api');
  return { ...actual, fetchBrowsableRepositories: jest.fn() };
});

const mockFetchRepositories = fetchBrowsableRepositories as jest.MockedFunction<
  typeof fetchBrowsableRepositories
>;

describe('useRepositoryList refresh (delete gating)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    __resetRepositoryTypeCache();
    mockFetchRepositories.mockResolvedValue([
      { id: 'r', name: 'repo-one', format: 'maven2', type: 'hosted' },
    ] as never);
  });

  it('invalidates the repository-type snapshot so Delete gating re-reads the type', async () => {
    // An administrator can retype (or remove) a repository while Browse is open.
    // The repository list has an explicit user refresh; if that refresh does not
    // drop the type snapshot, the list shows the new configuration while Delete
    // gating keeps answering from the stale cached type until the TTL expires.
    const typeHook = renderHook(() => useRepositoryType('repo-one'));
    await waitFor(() => expect(typeHook.result.current).toBe('hosted'));
    expect(mockFetchRepositories).toHaveBeenCalledTimes(1);

    const listHook = renderHook(() => useRepositoryList());
    await act(async () => {
      await listHook.result.current.refresh();
    });

    // The snapshot is gone, so a fresh lookup goes back to the server rather
    // than reusing the pre-refresh type.
    mockFetchRepositories.mockResolvedValue([
      { id: 'r', name: 'repo-one', format: 'maven2', type: 'group' },
    ] as never);
    const afterRefresh = renderHook(() => useRepositoryType('repo-one'));
    await waitFor(() => expect(afterRefresh.result.current).toBe('group'));
    expect(mockFetchRepositories).toHaveBeenCalledTimes(2);
  });
});
