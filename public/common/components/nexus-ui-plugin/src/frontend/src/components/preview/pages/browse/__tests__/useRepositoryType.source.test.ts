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

/**
 * The repository type that gates Delete must come from a listing
 * every *browsing* user can read, because the gate treats an unresolved type as
 * "not deletable".
 *
 * The two candidate listings are filtered by different server-side permissions:
 *
 *  - `service/rest/v1/repositories` — `RepositoriesResource.getRepositories()` is
 *    `@RequiresUser` and filters through
 *    `RepositoryManagerRESTAdapterImpl.getRepositories()` →
 *    `repositoryPermissionChecker.userCanBrowseRepositories(...)`, i.e. the same
 *    *browse* permission that let the user reach the repository in the tree.
 *  - `service/rest/internal/ui/repositories/details` —
 *    `RepositoryInternalResource.getRepositoryDetails()` filters through
 *    `userHasRepositoryAdminPermission(repository, READ)`, an *administration*
 *    permission (`nx-repository-admin:<format>:<name>:read`).
 *
 * A delete-capable non-administrator therefore receives an empty details listing
 * while browsing and deleting normally. Reading the type from that listing would
 * resolve `undefined` and hide Delete permanently for precisely the users the
 * gate exists to serve, so the type must be resolved from the browse-gated listing.
 *
 * These tests pin the source rather than the transport: they assert which URL is
 * read, and that the non-administrator case resolves.
 */

jest.mock('../../../../../interface/api', () => ({
  restClient: {
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
    delete: jest.fn(),
  },
  parseApiError: (err: { message?: string }) => ({ message: err?.message ?? 'An error occurred' }),
  ENDPOINTS: {
    REPOSITORIES: '/service/rest/v1/repositories',
    REPOSITORIES_DETAILS: '/service/rest/internal/ui/repositories/details',
  },
  urlBuilder: {
    components: { get: (id: string) => id, delete: (id: string) => id },
    assets: { get: (id: string) => id, delete: (id: string) => id },
    query: (baseUrl: string) => baseUrl,
  },
  encodeRepositoryItemId: (_repositoryName: string, rawId: string) => rawId,
}));

import { renderHook, waitFor } from '@testing-library/react';

import { ENDPOINTS, restClient } from '../../../../../interface/api';
import { __resetRepositoryTypeCache, useRepositoryType } from '../useRepositoryType';

const mockRestClient = restClient as jest.Mocked<typeof restClient>;

describe('useRepositoryType source', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    __resetRepositoryTypeCache();
  });

  it('reads the browse-gated repository listing, not the admin-gated details listing', async () => {
    mockRestClient.get.mockResolvedValue([
      { name: 'maven-releases', type: 'hosted', format: 'maven2' },
    ] as never);

    const { result } = renderHook(() => useRepositoryType('maven-releases'));

    await waitFor(() => expect(result.current).toBe('hosted'));
    expect(mockRestClient.get).toHaveBeenCalledWith(ENDPOINTS.REPOSITORIES);
    expect(mockRestClient.get).not.toHaveBeenCalledWith(ENDPOINTS.REPOSITORIES_DETAILS);
  });

  it('resolves the type for a delete-capable non-administrator, whose details listing is empty', async () => {
    // Reproduces the live response pair observed for a principal without
    // nx-repository-admin:...:read: the browse-gated listing describes the
    // repository, the admin-gated details listing returns [].
    mockRestClient.get.mockImplementation((url: string) =>
      Promise.resolve(
        url === ENDPOINTS.REPOSITORIES ? [{ name: 'raw-hosted', type: 'hosted', format: 'raw' }] : []
      ) as never
    );

    const { result } = renderHook(() => useRepositoryType('raw-hosted'));

    await waitFor(() => expect(result.current).toBe('hosted'));
  });

  it('reports group repositories as such so the gate can still deny them', async () => {
    // Fail-closed must keep working: the browse-gated listing carries the same
    // hosted/proxy/group values, so a group is denied on its type, not on a
    // failure to resolve it.
    mockRestClient.get.mockResolvedValue([
      { name: 'maven-public', type: 'group', format: 'maven2' },
    ] as never);

    const { result } = renderHook(() => useRepositoryType('maven-public'));

    await waitFor(() => expect(result.current).toBe('group'));
  });

  it('still refuses to resolve a type when that listing fails', async () => {
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    mockRestClient.get.mockRejectedValue(new Error('boom'));

    const { result } = renderHook(() => useRepositoryType('maven-releases'));

    await waitFor(() => expect(mockRestClient.get).toHaveBeenCalled());
    expect(result.current).toBeUndefined();
    consoleSpy.mockRestore();
  });
});
