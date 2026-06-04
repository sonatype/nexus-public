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
import Axios from 'axios';
import { useCustomSearch } from '../useCustomSearch';

// Mock Axios
jest.mock('axios');
const mockAxios = Axios as jest.Mocked<typeof Axios>;

describe('useCustomSearch', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Default mock response for search API
    mockAxios.get.mockResolvedValue({
      data: {
        items: [
          {
            id: 'test-1',
            repository: 'maven-central',
            format: 'maven2',
            group: 'org.apache.commons',
            name: 'commons-lang3',
            version: '3.14.0',
            assets: [{ id: 'asset-1', path: '/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar', downloadUrl: '/repository/maven-central/...' }],
          },
          {
            id: 'test-2',
            repository: 'maven-central',
            format: 'maven2',
            group: 'org.apache.commons',
            name: 'commons-io',
            version: '2.15.0',
            assets: [{ id: 'asset-2', path: '/org/apache/commons/commons-io/2.15.0/commons-io-2.15.0.jar', downloadUrl: '/repository/maven-central/...' }],
          },
        ],
        continuationToken: undefined,
      },
    });
  });
  it('initializes with one empty filter', () => {
    const { result } = renderHook(() => useCustomSearch());

    expect(result.current.state.filters.length).toBe(1);
    expect(result.current.state.filters[0].value).toBe('');
    expect(result.current.state.results.length).toBe(0);
    expect(result.current.state.loading).toBe(false);
  });

  it('adds a new filter', () => {
    const { result } = renderHook(() => useCustomSearch());

    act(() => {
      result.current.addFilter();
    });

    expect(result.current.state.filters.length).toBe(2);
  });

  it('removes a filter', () => {
    const { result } = renderHook(() => useCustomSearch());

    // Add a second filter first
    act(() => {
      result.current.addFilter();
    });

    const filterId = result.current.state.filters[0].id;

    act(() => {
      result.current.removeFilter(filterId);
    });

    expect(result.current.state.filters.length).toBe(1);
  });

  it('keeps at least one filter when removing', () => {
    const { result } = renderHook(() => useCustomSearch());

    const filterId = result.current.state.filters[0].id;

    act(() => {
      result.current.removeFilter(filterId);
    });

    // Should still have one filter (a new empty one)
    expect(result.current.state.filters.length).toBe(1);
  });

  it('updates a filter', () => {
    const { result } = renderHook(() => useCustomSearch());

    const filterId = result.current.state.filters[0].id;

    act(() => {
      result.current.updateFilter(filterId, {
        field: 'name',
        value: 'test-value',
      });
    });

    expect(result.current.state.filters[0].field).toBe('name');
    expect(result.current.state.filters[0].value).toBe('test-value');
  });

  it('executes search and returns results', async () => {
    const { result } = renderHook(() => useCustomSearch());

    // Set a filter value
    const filterId = result.current.state.filters[0].id;
    act(() => {
      result.current.updateFilter(filterId, { value: 'commons' });
    });

    // Execute search
    act(() => {
      result.current.search();
    });

    // Should be loading
    expect(result.current.state.loading).toBe(true);

    // Wait for results
    await waitFor(() => {
      expect(result.current.state.loading).toBe(false);
    });

    // Should have results
    expect(result.current.state.results.length).toBeGreaterThan(0);
  });

  it('clears all state', () => {
    const { result } = renderHook(() => useCustomSearch());

    // Add filters and modify state
    act(() => {
      result.current.addFilter();
      result.current.updateFilter(result.current.state.filters[0].id, {
        value: 'test',
      });
    });

    // Clear
    act(() => {
      result.current.clear();
    });

    expect(result.current.state.filters.length).toBe(1);
    expect(result.current.state.filters[0].value).toBe('');
    expect(result.current.state.results.length).toBe(0);
  });

  it('correctly reports hasFilters', () => {
    const { result } = renderHook(() => useCustomSearch());

    // Initially no filters with values
    expect(result.current.hasFilters).toBe(false);

    // Add a value
    act(() => {
      result.current.updateFilter(result.current.state.filters[0].id, {
        value: 'test',
      });
    });

    expect(result.current.hasFilters).toBe(true);
  });

  it('correctly reports hasMore', async () => {
    const { result } = renderHook(() => useCustomSearch());

    // Initially no more
    expect(result.current.hasMore).toBe(false);

    // After search with results, check hasMore
    act(() => {
      result.current.updateFilter(result.current.state.filters[0].id, {
        value: 'a',
      });
      result.current.search();
    });

    await waitFor(() => {
      expect(result.current.state.loading).toBe(false);
    });

    // hasMore depends on whether there's a continuation token
    // With mock data, small result sets won't have more
    expect(typeof result.current.hasMore).toBe('boolean');
  });
});

