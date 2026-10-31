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

import { interpret } from 'xstate';
import { waitFor } from 'xstate/lib/waitFor';

import { tagsListMachine } from '../tagsListMachine';
import type { TagWithCount } from '../../tags.types';

// Mock the API
jest.mock('../../tags.api', () => ({
  fetchTagsFiltered: jest.fn(),
}));

import { fetchTagsFiltered } from '../../tags.api';

const mockFetchTagsFiltered = fetchTagsFiltered as jest.MockedFunction<typeof fetchTagsFiltered>;

describe('tagsListMachine', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Default: resolve with an empty page so the initial load settles in `ready`.
    mockFetchTagsFiltered.mockResolvedValue({ items: [], totalCount: 0 });
  });

  it('should start in loading state', () => {
    const state = tagsListMachine.initialState;

    expect(state.matches('loading')).toBe(true);
    expect(state.context.loading).toBe(true);
  });

  it('should have correct initial context', () => {
    expect(tagsListMachine.initialState.context).toEqual({
      tags: [],
      loading: true,
      error: null,
      filters: {
        nameFilter: '',
        componentCountRanges: [],
        activityDays: [],
      },
      sortField: 'name',
      sortDirection: 'asc',
      currentPage: 0,
      pageSize: 20,
      totalItems: 0,
      totalUnfilteredItems: null,
    });
  });

  it('should transition from loading to ready on successful fetch', async () => {
    const mockTags: TagWithCount[] = [
      { name: 'tag-1', componentCount: 10, firstCreated: '2024-01-01', lastUpdated: '2024-01-15' },
      { name: 'tag-2', componentCount: 5, firstCreated: '2024-02-01', lastUpdated: '2024-02-15' },
    ];

    mockFetchTagsFiltered.mockResolvedValue({ items: mockTags, totalCount: 2 });

    const service = interpret(tagsListMachine).start();
    await waitFor(service, (state) => state.matches('ready'));

    expect(service.state.context.tags).toEqual(mockTags);
    expect(service.state.context.totalItems).toBe(2);
    expect(service.state.context.loading).toBe(false);

    service.stop();
  });

  it('should transition from loading to error on failed fetch', async () => {
    mockFetchTagsFiltered.mockRejectedValue(new Error('Network error'));

    const service = interpret(tagsListMachine).start();
    await waitFor(service, (state) => state.matches('error'));

    expect(service.state.context.error).toBe('Network error');
    expect(service.state.context.loading).toBe(false);

    service.stop();
  });

  it('should handle RETRY event from error state', async () => {
    mockFetchTagsFiltered.mockRejectedValueOnce(new Error('Network error'));

    const service = interpret(tagsListMachine).start();
    await waitFor(service, (state) => state.matches('error'));

    // Subsequent fetch succeeds (default mock), so RETRY should recover to `ready`.
    service.send({ type: 'RETRY' });
    await waitFor(service, (state) => state.matches('ready'));

    expect(service.state.matches('ready')).toBe(true);

    service.stop();
  });

  it('should handle SET_FILTERS event', async () => {
    const service = interpret(tagsListMachine).start();
    await waitFor(service, (state) => state.matches('ready'));

    // Move off page 0 first so the reset-to-0 behaviour is observable.
    service.send({ type: 'SET_PAGE', page: 3 });
    await waitFor(service, (state) => state.matches('ready'));

    service.send({
      type: 'SET_FILTERS',
      filters: {
        nameFilter: 'test',
        componentCountRanges: ['1-10'],
        activityDays: [30],
      },
    });
    await waitFor(service, (state) => state.matches('ready'));

    expect(service.state.context.filters.nameFilter).toBe('test');
    expect(service.state.context.filters.componentCountRanges).toEqual(['1-10']);
    expect(service.state.context.filters.activityDays).toEqual([30]);
    expect(service.state.context.currentPage).toBe(0); // Should reset to 0

    service.stop();
  });

  it('should handle SET_PAGE event', async () => {
    const service = interpret(tagsListMachine).start();
    await waitFor(service, (state) => state.matches('ready'));

    service.send({ type: 'SET_PAGE', page: 3 });
    await waitFor(service, (state) => state.matches('ready'));

    expect(service.state.context.currentPage).toBe(3);

    service.stop();
  });

  it('should handle SET_PAGE_SIZE event', async () => {
    const service = interpret(tagsListMachine).start();
    await waitFor(service, (state) => state.matches('ready'));

    // Move off page 0 first so the reset-to-0 behaviour is observable.
    service.send({ type: 'SET_PAGE', page: 5 });
    await waitFor(service, (state) => state.matches('ready'));

    service.send({ type: 'SET_PAGE_SIZE', pageSize: 50 });
    await waitFor(service, (state) => state.matches('ready'));

    expect(service.state.context.pageSize).toBe(50);
    expect(service.state.context.currentPage).toBe(0); // Should reset to 0

    service.stop();
  });

  it('should handle TOGGLE_SORT event with same field', async () => {
    const service = interpret(tagsListMachine).start();
    await waitFor(service, (state) => state.matches('ready'));

    // Toggling sort on the current field (name/asc) should flip direction.
    service.send({ type: 'TOGGLE_SORT', field: 'name' });
    await waitFor(service, (state) => state.matches('ready'));

    expect(service.state.context.sortField).toBe('name');
    expect(service.state.context.sortDirection).toBe('desc');
    expect(service.state.context.currentPage).toBe(0); // Should reset to 0

    service.stop();
  });

  it('should handle TOGGLE_SORT event with different field', async () => {
    const service = interpret(tagsListMachine).start();
    await waitFor(service, (state) => state.matches('ready'));

    // Toggling sort on a different field should reset direction to asc.
    service.send({ type: 'TOGGLE_SORT', field: 'componentCount' });
    await waitFor(service, (state) => state.matches('ready'));

    expect(service.state.context.sortField).toBe('componentCount');
    expect(service.state.context.sortDirection).toBe('asc');
    expect(service.state.context.currentPage).toBe(0); // Should reset to 0

    service.stop();
  });

  it('should handle REFRESH event from ready state', async () => {
    const service = interpret(tagsListMachine).start();
    await waitFor(service, (state) => state.matches('ready'));

    service.send({ type: 'REFRESH' });

    // REFRESH re-enters the loading state synchronously (before the fetch resolves).
    expect(service.state.matches('loading')).toBe(true);

    service.stop();
  });

  it('should capture totalUnfilteredItems on unfiltered fetch', async () => {
    const mockTags: TagWithCount[] = [
      { name: 'tag-1', componentCount: 10, firstCreated: '2024-01-01', lastUpdated: '2024-01-15' },
    ];

    mockFetchTagsFiltered.mockResolvedValue({ items: mockTags, totalCount: 1 });

    const service = interpret(tagsListMachine).start();
    await waitFor(service, (state) => state.matches('ready'));

    expect(service.state.context.totalUnfilteredItems).toBe(1);

    service.stop();
  });
});
