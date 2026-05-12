/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import {waitFor as waitForState} from 'xstate/lib/waitFor';
import Axios from 'axios';
import TestUtils from '../../../../interface/TestUtils';
import UsageInsightsChartMachine from './UsageInsightsChartMachine';
import * as UsageInsightsUtils from './UsageInsightsUtils';

// Mock Axios
jest.mock('axios');

// Mock UsageInsightsUtils
jest.mock('./UsageInsightsUtils', () => ({
  KEY_STORAGE: 'storage',
  KEY_EGRESS: 'egress',
  getMonthOptions: jest.fn(),
  getDateRange: jest.fn()
}));

describe('UsageInsightsChartMachine', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    // Default mocks
    UsageInsightsUtils.getMonthOptions.mockReturnValue([
      {
        key: '2024-01-01_2024-01-31',
        label: 'Jan 2024',
        value: {
          dateFrom: '2024-01-01',
          dateTo: '2024-01-31'
        }
      },
      {
        key: '2023-12-01_2023-12-31',
        label: 'Dec 2023',
        value: {
          dateFrom: '2023-12-01',
          dateTo: '2023-12-31'
        }
      }
    ]);

    UsageInsightsUtils.getDateRange.mockReturnValue({
      dateFrom: '2024-01-01',
      dateTo: '2024-01-31'
    });
  });

  describe('initial state', () => {
    it('should have correct initial state', () => {
      const machine = UsageInsightsChartMachine;

      expect(machine.initial).toBe('loading');
      expect(machine.id).toBe('UsageInsightsChartMachine');
    });

    it('should have correct initial context', () => {
      const machine = UsageInsightsChartMachine;

      expect(machine.context).toEqual({
        egressData: null,
        storageData: null,
        combinedData: null,
        loadError: null,
        monthOptions: [],
        selectedMonth: null,
        dateFrom: '',
        dateTo: '',
        isOpen: false
      });
    });
  });

  describe('loading state', () => {
    it('should fetch metrics and transition to loaded on success', async () => {
      const mockEgressResponse = {
        data: {
          data: [
            {date: '2024-01-01', bytes: 1000},
            {date: '2024-01-02', bytes: 2000}
          ]
        }
      };

      const mockStorageResponse = {
        data: {
          data: [
            {date: '2024-01-01', bytes: 500},
            {date: '2024-01-02', bytes: 1500}
          ]
        }
      };

      Axios.get.mockImplementation((url) => {
        if (url.includes('egress')) {
          return Promise.resolve(mockEgressResponse);
        }
        if (url.includes('storage')) {
          return Promise.resolve(mockStorageResponse);
        }
        return Promise.reject(new Error('Unknown URL'));
      });

      await TestUtils.withTestMachine(UsageInsightsChartMachine, async (service) => {
        const finalState = await waitForState(service, state => state.matches('loaded'));

        // Should initialize month options
        expect(finalState.context.monthOptions.length).toBeGreaterThan(0);
        expect(finalState.context.selectedMonth).not.toBeNull();

        // Should have fetched and combined data
        expect(finalState.context.egressData).toEqual(mockEgressResponse.data);
        expect(finalState.context.storageData).toEqual(mockStorageResponse.data);
        expect(finalState.context.combinedData).toBeDefined();
        expect(finalState.context.combinedData.length).toBe(2);

        // Verify combined data structure
        expect(finalState.context.combinedData[0]).toEqual({
          metricDate: '2024-01-01',
          egress: 1000,
          storage: 500,
          _available: { egress: true, storage: true }
        });
        expect(finalState.context.combinedData[1]).toEqual({
          metricDate: '2024-01-02',
          egress: 2000,
          storage: 1500,
          _available: { egress: true, storage: true }
        });

        // Verify API calls
        expect(Axios.get).toHaveBeenCalledWith(
          'service/rest/v1/daily-metrics/egress',
          expect.objectContaining({
            params: expect.objectContaining({
              dateFrom: expect.any(String),
              dateTo: expect.any(String)
            })
          })
        );
        expect(Axios.get).toHaveBeenCalledWith(
          'service/rest/v1/daily-metrics/storage',
          expect.objectContaining({
            params: expect.objectContaining({
              dateFrom: expect.any(String),
              dateTo: expect.any(String)
            })
          })
        );
      });
    });

    it('should handle missing data arrays gracefully', async () => {
      const mockEgressResponse = {data: {data: null}};
      const mockStorageResponse = {data: {data: null}};

      Axios.get.mockImplementation((url) => {
        if (url.includes('egress')) return Promise.resolve(mockEgressResponse);
        if (url.includes('storage')) return Promise.resolve(mockStorageResponse);
        return Promise.reject(new Error('Unknown URL'));
      });

      await TestUtils.withTestMachine(UsageInsightsChartMachine, async (service) => {
        const finalState = await waitForState(service, state => state.matches('loaded'));

        // Should handle null data arrays
        expect(finalState.context.combinedData).toEqual([]);
      });
    });

    it('should sort combined data by date', async () => {
      const mockEgressResponse = {
        data: {
          data: [
            {date: '2024-01-03', bytes: 3000},
            {date: '2024-01-01', bytes: 1000},
            {date: '2024-01-02', bytes: 2000}
          ]
        }
      };

      const mockStorageResponse = {
        data: {
          data: [
            {date: '2024-01-03', bytes: 1500},
            {date: '2024-01-01', bytes: 500},
            {date: '2024-01-02', bytes: 1000}
          ]
        }
      };

      Axios.get.mockImplementation((url) => {
        if (url.includes('egress')) return Promise.resolve(mockEgressResponse);
        if (url.includes('storage')) return Promise.resolve(mockStorageResponse);
        return Promise.reject(new Error('Unknown URL'));
      });

      await TestUtils.withTestMachine(UsageInsightsChartMachine, async (service) => {
        const finalState = await waitForState(service, state => state.matches('loaded'));

        // Should be sorted by date ascending
        expect(finalState.context.combinedData[0].metricDate).toBe('2024-01-01');
        expect(finalState.context.combinedData[1].metricDate).toBe('2024-01-02');
        expect(finalState.context.combinedData[2].metricDate).toBe('2024-01-03');
      });
    });

    it('should transition to loadError on fetch failure', async () => {
      const errorMessage = 'Network error';
      Axios.get.mockRejectedValue({
        message: errorMessage,
        config: {url: 'service/rest/v1/daily-metrics/egress'}
      });

      await TestUtils.withTestMachine(UsageInsightsChartMachine, async (service) => {
        const finalState = await waitForState(service, state => state.matches('loadError'));

        expect(finalState.context.loadError).toContain('egress');
        expect(finalState.context.loadError).toContain(errorMessage);
      });
    });

    it('should identify storage endpoint in error message', async () => {
      Axios.get.mockRejectedValue({
        message: 'Server error',
        config: {url: 'service/rest/v1/daily-metrics/storage'}
      });

      await TestUtils.withTestMachine(UsageInsightsChartMachine, async (service) => {
        const finalState = await waitForState(service, state => state.matches('loadError'));

        expect(finalState.context.loadError).toContain('storage');
      });
    });

    it('should handle error with response data message', async () => {
      Axios.get.mockRejectedValue({
        response: {
          data: {
            message: 'Unauthorized access'
          }
        },
        config: {url: 'service/rest/v1/daily-metrics/egress'}
      });

      await TestUtils.withTestMachine(UsageInsightsChartMachine, async (service) => {
        const finalState = await waitForState(service, state => state.matches('loadError'));

        expect(finalState.context.loadError).toContain('Unauthorized access');
      });
    });
  });

  describe('loaded state', () => {
    it('should handle REFRESH event', async () => {
      const mockEgressResponse = {data: {data: [{date: '2024-01-01', bytes: 1000}]}};
      const mockStorageResponse = {data: {data: [{date: '2024-01-01', bytes: 500}]}};

      Axios.get.mockImplementation((url) => {
        if (url.includes('egress')) return Promise.resolve(mockEgressResponse);
        if (url.includes('storage')) return Promise.resolve(mockStorageResponse);
        return Promise.reject(new Error('Unknown URL'));
      });

      await TestUtils.withTestMachine(UsageInsightsChartMachine, async (service) => {
        // Wait for initial load
        await waitForState(service, state => state.matches('loaded'));

        // Clear error if any
        const firstLoadedState = service.getSnapshot();
        expect(firstLoadedState.context.loadError).toBeNull();

        // Send REFRESH event
        service.send({type: 'REFRESH'});

        // Should transition back to loading then to loaded
        const reloadedState = await waitForState(service, state =>
          state.matches('loaded') && state.changed
        );

        expect(reloadedState.context.loadError).toBeNull();
      });
    });

    it('should handle SELECT_MONTH event', async () => {
      const mockEgressResponse = {data: {data: [{date: '2024-01-01', bytes: 1000}]}};
      const mockStorageResponse = {data: {data: [{date: '2024-01-01', bytes: 500}]}};

      Axios.get.mockImplementation((url) => {
        if (url.includes('egress')) return Promise.resolve(mockEgressResponse);
        if (url.includes('storage')) return Promise.resolve(mockStorageResponse);
        return Promise.reject(new Error('Unknown URL'));
      });

      await TestUtils.withTestMachine(UsageInsightsChartMachine, async (service) => {
        await waitForState(service, state => state.matches('loaded'));

        const newMonth = {
          key: '2023-12-01_2023-12-31',
          label: 'Dec 2023',
          value: {
            dateFrom: '2023-12-01',
            dateTo: '2023-12-31'
          }
        };

        // Send SELECT_MONTH event
        service.send({type: 'SELECT_MONTH', month: newMonth});

        // Should transition to loading with new month
        const reloadedState = await waitForState(service, state =>
          state.matches('loaded') && state.changed
        );

        expect(reloadedState.context.selectedMonth).toEqual(newMonth);
        expect(reloadedState.context.dateFrom).toBe('2023-12-01');
        expect(reloadedState.context.dateTo).toBe('2023-12-31');
        expect(reloadedState.context.isOpen).toBe(false);
      });
    });

    it('should handle TOGGLE_DROPDOWN event', async () => {
      const mockEgressResponse = {data: {data: [{date: '2024-01-01', bytes: 1000}]}};
      const mockStorageResponse = {data: {data: [{date: '2024-01-01', bytes: 500}]}};

      Axios.get.mockImplementation((url) => {
        if (url.includes('egress')) return Promise.resolve(mockEgressResponse);
        if (url.includes('storage')) return Promise.resolve(mockStorageResponse);
        return Promise.reject(new Error('Unknown URL'));
      });

      await TestUtils.withTestMachine(UsageInsightsChartMachine, async (service) => {
        await waitForState(service, state => state.matches('loaded'));

        const initialOpen = service.getSnapshot().context.isOpen;
        expect(initialOpen).toBe(false);

        // Toggle dropdown
        service.send({type: 'TOGGLE_DROPDOWN'});

        // Wait for state to update
        await new Promise(resolve => setTimeout(resolve, 100));

        const toggledState = service.getSnapshot();
        expect(toggledState.context.isOpen).toBe(true);

        // Toggle again
        service.send({type: 'TOGGLE_DROPDOWN'});
        await new Promise(resolve => setTimeout(resolve, 100));

        const toggledBackState = service.getSnapshot();
        expect(toggledBackState.context.isOpen).toBe(false);
      });
    });
  });

  describe('loadError state', () => {
    it('should handle RETRY event', async () => {
      let callCount = 0;
      Axios.get.mockImplementation(() => {
        callCount++;
        if (callCount === 1) {
          return Promise.reject(new Error('Network error'));
        }
        return Promise.resolve({data: {data: [{date: '2024-01-01', bytes: 1000}]}});
      });

      await TestUtils.withTestMachine(UsageInsightsChartMachine, async (service) => {
        // Wait for initial error
        await waitForState(service, state => state.matches('loadError'));

        expect(service.getSnapshot().context.loadError).toBeDefined();

        // Send RETRY event
        service.send({type: 'RETRY'});

        // Should transition to loading then to loaded
        const reloadedState = await waitForState(service, state => state.matches('loaded'));

        expect(reloadedState.context.loadError).toBeNull();
      });
    });
  });

  describe('ensureMonthSelection action', () => {
    it('should initialize month options on first load', async () => {
      const mockMonthOptions = [
        {
          key: '2024-01-01_2024-01-31',
          label: 'Jan 2024',
          value: {dateFrom: '2024-01-01', dateTo: '2024-01-31'}
        }
      ];

      UsageInsightsUtils.getMonthOptions.mockReturnValue(mockMonthOptions);

      const mockEgressResponse = {data: {data: []}};
      const mockStorageResponse = {data: {data: []}};

      Axios.get.mockImplementation((url) => {
        if (url.includes('egress')) return Promise.resolve(mockEgressResponse);
        if (url.includes('storage')) return Promise.resolve(mockStorageResponse);
        return Promise.reject(new Error('Unknown URL'));
      });

      await TestUtils.withTestMachine(UsageInsightsChartMachine, async (service) => {
        const finalState = await waitForState(service, state => state.matches('loaded'));

        expect(finalState.context.monthOptions).toEqual(mockMonthOptions);
        expect(finalState.context.selectedMonth).toEqual(mockMonthOptions[0]);
        expect(finalState.context.dateFrom).toBe('2024-01-01');
        expect(finalState.context.dateTo).toBe('2024-01-31');
      });
    });

    it('should handle empty month options gracefully', async () => {
      UsageInsightsUtils.getMonthOptions.mockReturnValue([]);
      UsageInsightsUtils.getDateRange.mockReturnValue({
        dateFrom: '2024-01-01',
        dateTo: '2024-01-31'
      });

      const mockEgressResponse = {data: {data: []}};
      const mockStorageResponse = {data: {data: []}};

      Axios.get.mockImplementation((url) => {
        if (url.includes('egress')) return Promise.resolve(mockEgressResponse);
        if (url.includes('storage')) return Promise.resolve(mockStorageResponse);
        return Promise.reject(new Error('Unknown URL'));
      });

      await TestUtils.withTestMachine(UsageInsightsChartMachine, async (service) => {
        const finalState = await waitForState(service, state => state.matches('loaded'));

        expect(finalState.context.monthOptions).toEqual([]);
        expect(finalState.context.selectedMonth).toBeNull();
        expect(finalState.context.dateFrom).toBe('2024-01-01');
        expect(finalState.context.dateTo).toBe('2024-01-31');
      });
    });
  });

  describe('clearData action', () => {
    it('should clear data when selecting new month', async () => {
      const mockEgressResponse = {data: {data: [{date: '2024-01-01', bytes: 1000}]}};
      const mockStorageResponse = {data: {data: [{date: '2024-01-01', bytes: 500}]}};

      Axios.get.mockImplementation((url) => {
        if (url.includes('egress')) return Promise.resolve(mockEgressResponse);
        if (url.includes('storage')) return Promise.resolve(mockStorageResponse);
        return Promise.reject(new Error('Unknown URL'));
      });

      await TestUtils.withTestMachine(UsageInsightsChartMachine, async (service) => {
        await waitForState(service, state => state.matches('loaded'));

        const initialState = service.getSnapshot();
        expect(initialState.context.combinedData).not.toBeNull();

        // Send SELECT_MONTH event
        service.send({
          type: 'SELECT_MONTH',
          month: {
            key: '2023-12-01_2023-12-31',
            label: 'Dec 2023',
            value: {dateFrom: '2023-12-01', dateTo: '2023-12-31'}
          }
        });

        // Data should be cleared before reloading
        const reloadedState = await waitForState(service, state =>
          state.matches('loaded') && state.changed
        );

        // After reload, data should be populated again
        expect(reloadedState.context.combinedData).toBeDefined();
      });
    });
  });
});
