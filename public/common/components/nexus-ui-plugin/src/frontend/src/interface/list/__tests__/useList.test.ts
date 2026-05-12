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
import { createListMachine } from '../createListMachine';
import { useList } from '../useList';

interface TestItem {
  id: string;
  name: string;
  category: string;
}

const mockData: TestItem[] = [
  { id: '1', name: 'Alpha', category: 'A' },
  { id: '2', name: 'Beta', category: 'B' },
  { id: '3', name: 'Gamma', category: 'A' },
];

describe('useList', () => {
  describe('data loading', () => {
    it('should load data successfully', async () => {
      const machine = createListMachine({
        id: 'test-list',
      }).withConfig({
        services: {
          fetchData: async () => mockData,
        },
      });

      const { result } = renderHook(() => useList(machine));

      await waitFor(() => {
        expect(result.current.loaded).toBe(true);
      });

      expect(result.current.data).toEqual(mockData);
      expect(result.current.error).toBeNull();
    });

    it('should handle fetch errors', async () => {
      const errorMessage = 'Failed to load';
      const machine = createListMachine({
        id: 'test-list',
      }).withConfig({
        services: {
          fetchData: async () => {
            throw new Error(errorMessage);
          },
        },
      });

      const { result } = renderHook(() => useList(machine));

      await waitFor(() => {
        expect(result.current.error).toBe(errorMessage);
      });

      expect(result.current.loading).toBe(false);
    });

    it('should reload data', async () => {
      let fetchCount = 0;
      const machine = createListMachine({
        id: 'test-list',
      }).withConfig({
        services: {
          fetchData: async () => {
            fetchCount++;
            return mockData;
          },
        },
      });

      const { result } = renderHook(() => useList(machine));

      await waitFor(() => {
        expect(result.current.loaded).toBe(true);
      });

      expect(fetchCount).toBe(1);

      act(() => {
        result.current.reload();
      });

      await waitFor(() => {
        expect(fetchCount).toBe(2);
      });
    });
  });

  describe('sorting', () => {
    it('should sort data', async () => {
      const machine = createListMachine<TestItem>({
        id: 'test-list',
        context: {
          sortField: 'name',
          sortDirection: 'asc',
        },
      }).withConfig({
        services: {
          fetchData: async () => mockData,
        },
      });

      const { result } = renderHook(() => useList(machine));

      await waitFor(() => {
        expect(result.current.loaded).toBe(true);
      });

      expect(result.current.data[0].name).toBe('Alpha');

      act(() => {
        result.current.sort('name');
      });

      await waitFor(() => {
        expect(result.current.sortDirection).toBe('desc');
      });

      expect(result.current.data[0].name).toBe('Gamma');
    });
  });

  describe('hook stability', () => {
    it('should memoize callback functions', async () => {
      const machine = createListMachine({
        id: 'test-list',
      }).withConfig({
        services: {
          fetchData: async () => mockData,
        },
      });

      const { result, rerender } = renderHook(() => useList(machine));

      await waitFor(() => {
        expect(result.current.loaded).toBe(true);
      });

      const { setFilters, clearFilters, sort, reload } = result.current;

      rerender();

      // Callbacks should be the same reference
      expect(result.current.setFilters).toBe(setFilters);
      expect(result.current.clearFilters).toBe(clearFilters);
      expect(result.current.sort).toBe(sort);
      expect(result.current.reload).toBe(reload);
    });
  });
});
