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
import { createListMachine } from '../createListMachine';

interface TestItem {
  id: string;
  name: string;
  category: string;
  count: number;
}

interface TestFilters {
  filter?: string;
  categoryFilter: string[];
}

const mockData: TestItem[] = [
  { id: '1', name: 'Alpha', category: 'A', count: 10 },
  { id: '2', name: 'Beta', category: 'B', count: 20 },
  { id: '3', name: 'Gamma', category: 'A', count: 15 },
  { id: '4', name: 'Delta', category: 'C', count: 5 },
];

describe('createListMachine', () => {
  describe('initial state', () => {
    it('should start in loading state', () => {
      const machine = createListMachine({
        id: 'test-list',
      }).withConfig({
        services: {
          fetchData: async () => [],
        },
      });

      const service = interpret(machine);
      service.start();

      expect(service.state.matches('loading')).toBe(true);

      service.stop();
    });

    it('should initialize context with default values', () => {
      const machine = createListMachine({
        id: 'test-list',
        context: {
          sortField: 'name',
          sortDirection: 'asc',
          filters: { categoryFilter: [] },
        },
      }).withConfig({
        services: {
          fetchData: async () => [],
        },
      });

      const service = interpret(machine);
      service.start();

      expect(service.state.context.sortField).toBe('name');
      expect(service.state.context.sortDirection).toBe('asc');
      expect(service.state.context.filters).toEqual({ categoryFilter: [] });
      expect(service.state.context.pristineData).toEqual([]);
      expect(service.state.context.data).toEqual([]);

      service.stop();
    });
  });

  describe('data loading', () => {
    it('should load data successfully', (done) => {
      const machine = createListMachine({
        id: 'test-list',
      }).withConfig({
        services: {
          fetchData: async () => mockData,
        },
      });

      const service = interpret(machine);

      service.onTransition((state) => {
        if (state.matches('loaded')) {
          expect(state.context.pristineData).toEqual(mockData);
          // Data is sorted by name ascending by default
          expect(state.context.data).toHaveLength(mockData.length);
          expect(state.context.error).toBeNull();
          service.stop();
          done();
        }
      });

      service.start();
    });

    it('should handle fetch errors', (done) => {
      const errorMessage = 'Failed to fetch data';
      const machine = createListMachine({
        id: 'test-list',
      }).withConfig({
        services: {
          fetchData: async () => {
            throw new Error(errorMessage);
          },
        },
      });

      const service = interpret(machine);

      service.onTransition((state) => {
        if (state.matches('error')) {
          expect(state.context.error).toBe(errorMessage);
          service.stop();
          done();
        }
      });

      service.start();
    });

    it('should reload data when LOAD event is sent', (done) => {
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

      const service = interpret(machine);

      service.onTransition((state) => {
        if (state.matches('loaded') && fetchCount === 1) {
          // First load complete, trigger reload
          service.send({ type: 'LOAD' });
        } else if (state.matches('loaded') && fetchCount === 2) {
          // Second load complete
          expect(fetchCount).toBe(2);
          service.stop();
          done();
        }
      });

      service.start();
    });
  });

  describe('filtering', () => {
    it('should filter data by text search', (done) => {
      const machine = createListMachine<TestItem, TestFilters>({
        id: 'test-list',
        context: {
          filters: { filter: '', categoryFilter: [] },
        },
      }).withConfig({
        services: {
          fetchData: async () => mockData,
        },
      });

      const service = interpret(machine);

      service.onTransition((state) => {
        if (state.matches('loaded') && state.context.pristineData.length > 0) {
          if (!state.context.filters.filter) {
            // Initial load - send filter event via SET_FILTERS
            service.send({ type: 'SET_FILTERS', filters: { filter: 'alpha' } });
          } else {
            // After filter applied
            expect(state.context.data).toHaveLength(1);
            expect(state.context.data[0].name).toBe('Alpha');
            service.stop();
            done();
          }
        }
      });

      service.start();
    });

    it('should use custom filterData function', (done) => {
      const machine = createListMachine<TestItem, TestFilters>({
        id: 'test-list',
        context: {
          filters: { categoryFilter: [] },
        },
      }).withConfig({
        services: {
          fetchData: async () => mockData,
        },
        actions: {
          filterData: (context) => {
            const { pristineData, filters } = context;
            if (filters.categoryFilter.length === 0) return pristineData;
            return pristineData.filter((item) => filters.categoryFilter.includes(item.category));
          },
        },
      });

      const service = interpret(machine);

      service.onTransition((state) => {
        if (state.matches('loaded') && state.context.pristineData.length > 0) {
          if (state.context.filters.categoryFilter.length === 0) {
            // Initial load - send filter event
            service.send({ type: 'SET_FILTERS', filters: { categoryFilter: ['A'] } });
          } else {
            // After filter applied
            expect(state.context.data).toHaveLength(2);
            expect(state.context.data.every((item) => item.category === 'A')).toBe(true);
            service.stop();
            done();
          }
        }
      });

      service.start();
    });

    it('should clear all filters', (done) => {
      const machine = createListMachine<TestItem, TestFilters>({
        id: 'test-list',
        context: {
          filters: { filter: '', categoryFilter: [] },
        },
      }).withConfig({
        services: {
          fetchData: async () => mockData,
        },
      });

      const service = interpret(machine);

      let step = 0;

      service.onTransition((state) => {
        if (state.matches('loaded') && state.context.pristineData.length > 0) {
          if (step === 0) {
            // Initial load - apply text filter via SET_FILTERS
            step++;
            service.send({ type: 'SET_FILTERS', filters: { filter: 'alpha' } });
          } else if (step === 1 && state.context.filters.filter === 'alpha') {
            // Text filter applied - now add category filter
            step++;
            service.send({ type: 'SET_FILTERS', filters: { categoryFilter: ['A'] } });
          } else if (step === 2 && state.context.filters.categoryFilter.length > 0) {
            // Category filter applied - now clear all filters
            step++;
            service.send({ type: 'CLEAR_FILTERS' });
          } else if (step === 3 && state.context.filters.filter === '') {
            // Verify filters cleared
            expect(state.context.filters.filter).toBe('');
            expect(state.context.filters.categoryFilter).toEqual([]);
            // Data should be sorted but contain all items
            expect(state.context.data).toHaveLength(mockData.length);
            service.stop();
            done();
          }
        }
      });

      service.start();
    });
  });

  describe('sorting', () => {
    it('should sort data ascending by default', (done) => {
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

      const service = interpret(machine);

      service.onTransition((state) => {
        if (state.matches('loaded')) {
          expect(state.context.data[0].name).toBe('Alpha');
          expect(state.context.data[1].name).toBe('Beta');
          expect(state.context.data[2].name).toBe('Delta');
          expect(state.context.data[3].name).toBe('Gamma');
          service.stop();
          done();
        }
      });

      service.start();
    });

    it('should sort data descending', (done) => {
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

      const service = interpret(machine);

      service.onTransition((state) => {
        if (state.matches('loaded') && state.context.pristineData.length > 0) {
          if (state.context.sortDirection === 'asc') {
            // Initial load - change to desc
            service.send({ type: 'SORT', field: 'name' });
          } else if (state.context.sortDirection === 'desc') {
            // After sort applied
            expect(state.context.data[0].name).toBe('Gamma');
            expect(state.context.data[1].name).toBe('Delta');
            expect(state.context.data[2].name).toBe('Beta');
            expect(state.context.data[3].name).toBe('Alpha');
            service.stop();
            done();
          }
        }
      });

      service.start();
    });

    it('should cycle through sort directions: asc -> desc -> null', (done) => {
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

      const service = interpret(machine);

      let step = 0;

      service.onTransition((state) => {
        if (state.matches('loaded') && state.context.pristineData.length > 0) {
          if (step === 0) {
            expect(state.context.sortDirection).toBe('asc');
            service.send({ type: 'SORT', field: 'name' });
            step++;
          } else if (step === 1) {
            expect(state.context.sortDirection).toBe('desc');
            service.send({ type: 'SORT', field: 'name' });
            step++;
          } else if (step === 2) {
            expect(state.context.sortDirection).toBeNull();
            service.stop();
            done();
          }
        }
      });

      service.start();
    });

    it('should reset to asc when sorting by different field', (done) => {
      const machine = createListMachine<TestItem>({
        id: 'test-list',
        context: {
          sortField: 'name',
          sortDirection: 'desc',
        },
      }).withConfig({
        services: {
          fetchData: async () => mockData,
        },
      });

      const service = interpret(machine);

      service.onTransition((state) => {
        if (state.matches('loaded') && state.context.pristineData.length > 0) {
          if (state.context.sortField === 'name') {
            // Initial load - sort by different field
            service.send({ type: 'SORT', field: 'count' });
          } else {
            // After sorting by count
            expect(state.context.sortField).toBe('count');
            expect(state.context.sortDirection).toBe('asc');
            expect(state.context.data[0].count).toBe(5);
            expect(state.context.data[3].count).toBe(20);
            service.stop();
            done();
          }
        }
      });

      service.start();
    });

    it('should use custom sortData function', (done) => {
      const machine = createListMachine<TestItem>({
        id: 'test-list',
      }).withConfig({
        services: {
          fetchData: async () => mockData,
        },
        actions: {
          sortData: (context) => {
            // Custom sort: reverse order
            return [...context.data].reverse();
          },
        },
      });

      const service = interpret(machine);

      service.onTransition((state) => {
        if (state.matches('loaded')) {
          expect(state.context.data[0].id).toBe('4');
          expect(state.context.data[3].id).toBe('1');
          service.stop();
          done();
        }
      });

      service.start();
    });
  });

  describe('combined filtering and sorting', () => {
    it('should filter and sort data together', (done) => {
      const machine = createListMachine<TestItem, TestFilters>({
        id: 'test-list',
        context: {
          sortField: 'name',
          sortDirection: 'asc',
          filters: { filter: '', categoryFilter: [] },
        },
      }).withConfig({
        services: {
          fetchData: async () => mockData,
        },
      });

      const service = interpret(machine);

      service.onTransition((state) => {
        if (state.matches('loaded') && state.context.pristineData.length > 0) {
          if (!state.context.filters.filter) {
            // Initial load - apply filter for items containing 'a' via SET_FILTERS
            service.send({ type: 'SET_FILTERS', filters: { filter: 'a' } });
          } else {
            // After filter applied (Alpha, Beta, Gamma, Delta all contain 'a')
            expect(state.context.data).toHaveLength(4);
            expect(state.context.data[0].name).toBe('Alpha');
            expect(state.context.data[3].name).toBe('Gamma');
            service.stop();
            done();
          }
        }
      });

      service.start();
    });
  });
});
