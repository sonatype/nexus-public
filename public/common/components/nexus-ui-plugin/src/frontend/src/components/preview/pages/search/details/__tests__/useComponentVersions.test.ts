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

import { SEARCH_DEBOUNCE_MS, useComponentVersions } from '../useComponentVersions';
import { fetchComponentVersions } from '../../core/componentVersionsApi';

jest.mock('../../core/componentVersionsApi');
const mockedFetch = fetchComponentVersions as jest.MockedFunction<typeof fetchComponentVersions>;

const GA_ID = 'maven2:org.test:artifact';

describe('useComponentVersions', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedFetch.mockResolvedValue({
      items: [
        { version: '1.0.10', lastUpdated: '2026-02-01T00:00:00Z', repositories: ['releases'] },
      ],
      total: 95,
      page: 0,
      size: 20,
    });
  });

  it('loads the first page eagerly and exposes a 1-based current page', async () => {
    const { result } = renderHook(() => useComponentVersions({ gaId: GA_ID }));

    await waitFor(() => expect(result.current.versions).toHaveLength(1));
    expect(result.current.currentPage).toBe(1);
    expect(result.current.total).toBe(95);
    expect(result.current.totalPages).toBe(5); // ceil(95 / 20)
    expect(mockedFetch.mock.calls[0][0].page).toBe(0);
    expect(mockedFetch.mock.calls[0][0].format).toBe('maven2');
    expect(mockedFetch.mock.calls[0][0].group).toBe('org.test');
    expect(mockedFetch.mock.calls[0][0].name).toBe('artifact');
  });

  it('exposes the latched newest version and unfiltered total alongside the current page', async () => {
    const { result } = renderHook(() => useComponentVersions({ gaId: GA_ID }));
    await waitFor(() => expect(result.current.versions).toHaveLength(1));

    expect(result.current.newestVersion).toBe('1.0.10');
    expect(result.current.totalVersions).toBe(95);

    // Sorting ascending moves the visible page but must not move either latched value.
    mockedFetch.mockResolvedValue({
      items: [{ version: '0.0.1', lastUpdated: '2026-01-01T00:00:00Z', repositories: ['releases'] }],
      total: 95,
      page: 0,
      size: 20,
    });
    act(() => result.current.onSortChange('version', 'asc'));

    await waitFor(() => expect(result.current.versions[0].version).toBe('0.0.1'));
    expect(result.current.newestVersion).toBe('1.0.10');
    expect(result.current.totalVersions).toBe(95);
  });

  it('converts a 1-based page change to the 0-based API value', async () => {
    const { result } = renderHook(() => useComponentVersions({ gaId: GA_ID }));
    await waitFor(() => expect(result.current.versions).toHaveLength(1));

    act(() => result.current.onPageChange(3));

    await waitFor(() => expect(mockedFetch).toHaveBeenCalledTimes(2));
    expect(mockedFetch.mock.calls[1][0].page).toBe(2);
    await waitFor(() => expect(result.current.currentPage).toBe(3));
  });

  it('exposes a total of zero pages without crashing when there are no versions', async () => {
    mockedFetch.mockResolvedValue({ items: [], total: 0, page: 0, size: 20 });
    const { result } = renderHook(() => useComponentVersions({ gaId: GA_ID }));

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.totalPages).toBe(0);
    expect(result.current.versions).toEqual([]);
  });

  it('surfaces an error and clears it on retry', async () => {
    mockedFetch.mockRejectedValueOnce(new Error('nope'));
    const { result } = renderHook(() => useComponentVersions({ gaId: GA_ID }));

    await waitFor(() => expect(result.current.error).toBeTruthy());

    act(() => result.current.retry());
    await waitFor(() => expect(result.current.error).toBeNull());
    expect(result.current.total).toBe(95);
  });

  it('debounces search query changes before re-querying the server', async () => {
    jest.useFakeTimers();
    try {
      const { result } = renderHook(() => useComponentVersions({ gaId: GA_ID }));
      await act(async () => {
        await Promise.resolve();
      });
      expect(mockedFetch).toHaveBeenCalledTimes(1);

      act(() => result.current.onSearchQueryChange('2'));
      act(() => result.current.onSearchQueryChange('2.1'));
      expect(mockedFetch).toHaveBeenCalledTimes(1);

      // still nothing one tick before the delay elapses
      act(() => jest.advanceTimersByTime(SEARCH_DEBOUNCE_MS - 1));
      expect(mockedFetch).toHaveBeenCalledTimes(1);

      act(() => jest.advanceTimersByTime(1));
      await act(async () => {
        await Promise.resolve();
      });

      // only the final keystroke is queried, not one request per character
      expect(mockedFetch).toHaveBeenCalledTimes(2);
      expect(mockedFetch.mock.calls[1][0].versionFilter).toBe('2.1');
      expect(mockedFetch.mock.calls[1][0].page).toBe(0);
    } finally {
      jest.useRealTimers();
    }
  });

  it('clears a still-pending keystroke timer on unmount', async () => {
    // Asserted on clearTimeout rather than on fetch: XState silently drops send() against a
    // stopped machine, so a fetch-count assertion would pass with or without the cleanup and
    // prove nothing.
    jest.useFakeTimers();
    const setSpy = jest.spyOn(global, 'setTimeout');
    const clearSpy = jest.spyOn(global, 'clearTimeout');
    try {
      const { result, unmount } = renderHook(() => useComponentVersions({ gaId: GA_ID }));
      await act(async () => {
        await Promise.resolve();
      });

      setSpy.mockClear();
      act(() => result.current.onSearchQueryChange('2.1'));
      const debounceTimerId = setSpy.mock.results.at(-1)?.value;
      expect(debounceTimerId).toBeDefined();

      unmount();

      expect(clearSpy).toHaveBeenCalledWith(debounceTimerId);
    } finally {
      setSpy.mockRestore();
      clearSpy.mockRestore();
      jest.useRealTimers();
    }
  });

  it('does not issue a redundant fetch when onPageChange(1) follows a size change that already reset to page 0', async () => {
    // Reproduces TablePagination.handleItemsPerPageChange, which calls
    // onItemsPerPageChange(newSize) followed unconditionally by onPageChange(1).
    const { result } = renderHook(() => useComponentVersions({ gaId: GA_ID }));
    await waitFor(() => expect(result.current.versions).toHaveLength(1));
    expect(mockedFetch).toHaveBeenCalledTimes(1);

    act(() => {
      result.current.onItemsPerPageChange(50);
      result.current.onPageChange(1);
    });

    await waitFor(() => expect(mockedFetch).toHaveBeenCalledTimes(2));
    // Only the SET_SIZE-triggered fetch fired; the redundant onPageChange(1) was a no-op.
    expect(mockedFetch.mock.calls[1][0].size).toBe(50);
    expect(mockedFetch.mock.calls[1][0].page).toBe(0);
  });

  it('does not issue a redundant fetch when the size changes while on a page other than the first', async () => {
    // The same TablePagination sequence as above, but starting from page 3. A guard that
    // compares against a render-cycle value of page cannot see SET_SIZE's reset to 0, so it
    // lets the redundant onPageChange(1) through — the machine must own the comparison.
    const { result } = renderHook(() => useComponentVersions({ gaId: GA_ID }));
    await waitFor(() => expect(result.current.versions).toHaveLength(1));

    act(() => result.current.onPageChange(3));
    await waitFor(() => expect(result.current.currentPage).toBe(3));
    expect(mockedFetch).toHaveBeenCalledTimes(2);

    act(() => {
      result.current.onItemsPerPageChange(50);
      result.current.onPageChange(1);
    });

    await waitFor(() => expect(result.current.itemsPerPage).toBe(50));
    expect(mockedFetch).toHaveBeenCalledTimes(3);
    expect(mockedFetch.mock.calls[2][0].size).toBe(50);
    expect(mockedFetch.mock.calls[2][0].page).toBe(0);
  });

  it('parses a namespace-less gaId (e.g. npm/raw) without a group filter', async () => {
    const { result } = renderHook(() => useComponentVersions({ gaId: 'npm:lodash' }));
    await waitFor(() => expect(result.current.versions).toHaveLength(1));

    expect(mockedFetch.mock.calls[0][0].format).toBe('npm');
    expect(mockedFetch.mock.calls[0][0].name).toBe('lodash');
    expect(mockedFetch.mock.calls[0][0].group).toBeUndefined();
  });
});
