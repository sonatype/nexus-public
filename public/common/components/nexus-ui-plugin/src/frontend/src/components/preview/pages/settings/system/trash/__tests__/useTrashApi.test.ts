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
import {
  mapToDeletedItem,
  SoftDeletedEntityXO,
  useDeletedItems,
  useDeletedItemsFacets,
  usePurgeItems,
  useRestoreItems,
} from '../useTrashApi';

// Mock the API module - path should match the import in the source file
jest.mock('../../../../../../../interface/api', () => ({
  API_BASE: '/service/rest',
  restClient: {
    get: jest.fn(),
    post: jest.fn(),
    delete: jest.fn(),
  },
}));

// Import after mocking
import { restClient } from '../../../../../../../interface/api';

const mockGet = restClient.get as jest.MockedFunction<typeof restClient.get>;
const mockPost = restClient.post as jest.MockedFunction<typeof restClient.post>;

describe('mapToDeletedItem', () => {
  function makeXo(overrides: Partial<SoftDeletedEntityXO> = {}): SoftDeletedEntityXO {
    return {
      entityType: 'component',
      identifier: 'id-1',
      displayName: 'org.example:foo:1.0.0',
      deletedOn: '2026-06-01T10:00:00Z',
      deletedBy: 'admin',
      metadata: { repositoryName: 'maven-releases', format: 'maven2' },
      ...overrides,
    };
  }

  it('uses metadata.repositoryName for component entities', () => {
    const item = mapToDeletedItem(makeXo({ entityType: 'component' }), 30);
    expect(item.repository).toBe('maven-releases');
  });

  it('maps deletedByName when the server resolved a display name', () => {
    const item = mapToDeletedItem(
      makeXo({ deletedBy: 'jsmith@example.com', deletedByName: 'John Smith' }),
      30,
    );
    expect(item.deletedBy).toBe('jsmith@example.com');
    expect(item.deletedByName).toBe('John Smith');
  });

  it('leaves deletedByName undefined when the server returns null/omits it', () => {
    expect(mapToDeletedItem(makeXo({ deletedByName: null }), 30).deletedByName).toBeUndefined();
    expect(mapToDeletedItem(makeXo({}), 30).deletedByName).toBeUndefined();
  });

  it('uses metadata.repositoryName for asset entities', () => {
    const item = mapToDeletedItem(
      makeXo({
        entityType: 'asset',
        displayName: '/org/example/foo-1.0.0.jar',
        metadata: { repositoryName: 'maven-releases', path: '/org/example/foo-1.0.0.jar' },
      }),
      30,
    );
    expect(item.repository).toBe('maven-releases');
  });

  it('maps parentComponentInTrash for asset entries and leaves it undefined otherwise', () => {
    const inTrash = mapToDeletedItem(makeXo({ entityType: 'asset', parentComponentInTrash: true }), 30);
    expect(inTrash.parentComponentInTrash).toBe(true);
    const live = mapToDeletedItem(makeXo({ entityType: 'asset', parentComponentInTrash: false }), 30);
    expect(live.parentComponentInTrash).toBe(false);
    const missing = mapToDeletedItem(makeXo({ entityType: 'asset' }), 30);
    expect(missing.parentComponentInTrash).toBeUndefined();
  });

  it('uses the entity name as the repository column for repository entities', () => {
    const item = mapToDeletedItem(
      makeXo({
        entityType: 'repository',
        displayName: 'maven-releases',
        // Repository entities do not carry repositoryName in metadata —
        // the entity itself IS the repository.
        metadata: {},
      }),
      30,
    );
    expect(item.repository).toBe('maven-releases');
    expect(item.entityType).toBe('repository');
  });

  it('falls back to component (and leaves repository undefined) for descoped entity types', () => {
    // blob-store/tag/user were descoped from the backend; any such value is treated as component.
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    try {
      const userItem = mapToDeletedItem(
        makeXo({ entityType: 'user', displayName: 'jdoe', metadata: {} }),
        30,
      );
      expect(userItem.entityType).toBe('component');
      expect(userItem.repository).toBeUndefined();

      const tagItem = mapToDeletedItem(
        makeXo({ entityType: 'tag', displayName: 'release-2026-Q2', metadata: {} }),
        30,
      );
      expect(tagItem.entityType).toBe('component');
      expect(tagItem.repository).toBeUndefined();
    } finally {
      warnSpy.mockRestore();
    }
  });
});

describe('useDeletedItems URL serialization', () => {
  beforeEach(() => {
    mockGet.mockReset();
    mockGet.mockResolvedValue({ items: [], total: 0, offset: 0, limit: 25 });
  });

  it('serializes entityTypes array as repeated query params', async () => {
    renderHook(() => useDeletedItems({
      offset: 0, limit: 25, sort: 'deletedOn:desc',
      entityTypes: ['asset', 'component'],
    }));

    await waitFor(() => expect(mockGet).toHaveBeenCalled());
    const url = mockGet.mock.calls[0][0];
    expect(url).toMatch(/[?&]entityType=asset(&|$)/);
    expect(url).toMatch(/[?&]entityType=component(&|$)/);
  });

  it('omits empty arrays from URL', async () => {
    renderHook(() => useDeletedItems({
      offset: 0, limit: 25, sort: 'deletedOn:desc',
      entityTypes: [],
      formats: [],
      deletedBy: [],
    }));

    await waitFor(() => expect(mockGet).toHaveBeenCalled());
    const url = mockGet.mock.calls[0][0];
    expect(url).not.toMatch(/entityType=/);
    expect(url).not.toMatch(/format=/);
    expect(url).not.toMatch(/deletedBy=/);
  });

  it('combines all three filter axes plus q', async () => {
    renderHook(() => useDeletedItems({
      offset: 0, limit: 25, sort: 'deletedOn:desc',
      entityTypes: ['asset'],
      formats: ['maven2'],
      deletedBy: ['admin'],
      q: 'log4j',
    }));

    await waitFor(() => expect(mockGet).toHaveBeenCalled());
    const url = mockGet.mock.calls[0][0];
    expect(url).toMatch(/entityType=asset/);
    expect(url).toMatch(/format=maven2/);
    expect(url).toMatch(/deletedBy=admin/);
    expect(url).toMatch(/q=log4j/);
  });
});

describe('useRestoreItems partial failure', () => {
  beforeEach(() => {
    mockPost.mockReset();
  });

  it('resolves with the successful/failed split instead of throwing when some items fail', async () => {
    mockPost.mockResolvedValue({
      successful: [{ entityType: 'component', identifier: 'ok-1', error: null }],
      failed: [
        { entityType: 'component', identifier: 'bad-1', error: "Parent repository 'r' is not available." },
        { entityType: 'component', identifier: 'bad-2', error: "Parent repository 'r' is not available." },
      ],
    });

    const { result } = renderHook(() => useRestoreItems());

    let response: Awaited<ReturnType<typeof result.current.restore>> | undefined;
    await act(async () => {
      response = await result.current.restore([
        { id: 'component::ok-1', entityType: 'component' },
        { id: 'component::bad-1', entityType: 'component' },
        { id: 'component::bad-2', entityType: 'component' },
      ]);
    });

    expect(response).toEqual({
      successful: [{ entityType: 'component', identifier: 'ok-1', error: null }],
      failed: [
        { entityType: 'component', identifier: 'bad-1', error: "Parent repository 'r' is not available." },
        { entityType: 'component', identifier: 'bad-2', error: "Parent repository 'r' is not available." },
      ],
    });
  });

  it('still rejects when the HTTP request itself fails', async () => {
    mockPost.mockRejectedValue(new Error('network down'));

    const { result } = renderHook(() => useRestoreItems());

    await act(async () => {
      await expect(
        result.current.restore([{ id: 'component::ok-1', entityType: 'component' }]),
      ).rejects.toThrow('network down');
    });
  });
});

describe('usePurgeItems partial failure', () => {
  beforeEach(() => {
    mockPost.mockReset();
  });

  it('resolves with the successful/failed split instead of throwing when some items fail', async () => {
    mockPost.mockResolvedValue({
      successful: [{ entityType: 'asset', identifier: 'ok-1', error: null }],
      failed: [{ entityType: 'asset', identifier: 'bad-1', error: 'boom' }],
    });

    const { result } = renderHook(() => usePurgeItems());

    let response: Awaited<ReturnType<typeof result.current.purge>> | undefined;
    await act(async () => {
      response = await result.current.purge([
        { id: 'asset::ok-1', entityType: 'asset' },
        { id: 'asset::bad-1', entityType: 'asset' },
      ]);
    });

    expect(response).toEqual({
      successful: [{ entityType: 'asset', identifier: 'ok-1', error: null }],
      failed: [{ entityType: 'asset', identifier: 'bad-1', error: 'boom' }],
    });
  });
});

describe('useDeletedItemsFacets', () => {
  beforeEach(() => {
    mockGet.mockReset();
    mockGet.mockResolvedValue({
      entityTypes: { asset: 12, component: 3 },
      formats: { maven2: 9 },
      deletedBy: [{ name: 'admin', count: 8 }],
    });
  });

  it('hits /facets endpoint', async () => {
    renderHook(() => useDeletedItemsFacets(0));
    await waitFor(() => expect(mockGet).toHaveBeenCalled());
    expect(mockGet.mock.calls[0][0]).toMatch(/\/soft-delete\/facets/);
  });

  it('refetches when refetchKey changes', async () => {
    const { rerender } = renderHook(({ k }: { k: number }) => useDeletedItemsFacets(k),
      { initialProps: { k: 0 } });
    await waitFor(() => expect(mockGet).toHaveBeenCalledTimes(1));

    rerender({ k: 1 });
    await waitFor(() => expect(mockGet).toHaveBeenCalledTimes(2));
  });

  it('forwards deletedByPrefix when supplied', async () => {
    renderHook(() => useDeletedItemsFacets(0, 'ad', 20));
    await waitFor(() => expect(mockGet).toHaveBeenCalled());
    const url = mockGet.mock.calls[0][0];
    expect(url).toMatch(/deletedByPrefix=ad/);
    expect(url).toMatch(/deletedByLimit=20/);
  });
});
