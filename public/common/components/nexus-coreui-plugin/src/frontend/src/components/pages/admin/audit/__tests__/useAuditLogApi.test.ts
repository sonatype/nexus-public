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
import { useAuditLogApi } from '../useAuditLogApi';
import { restClient } from '@/utils/api';
import type { AuditFilters, AuditLogResponse } from '../audit.types';

// Mock the restClient
jest.mock('@/utils/api', () => ({
  restClient: {
    get: jest.fn(),
  },
}));

const mockRestClient = restClient as jest.Mocked<typeof restClient>;

// Test data
const mockAuditLogResponse: AuditLogResponse = {
  items: [
    {
      id: 1,
      domain: 'security.user',
      type: 'created',
      context: 'testuser',
      timestamp: '2026-03-12T10:00:00.000Z',
      initiator: 'admin',
      nodeId: 'node-1',
      attributes: { name: 'testuser' },
    },
    {
      id: 2,
      domain: 'repository',
      type: 'updated',
      context: 'maven-central',
      timestamp: '2026-03-12T09:30:00.000Z',
      initiator: 'admin',
      nodeId: 'node-1',
      attributes: { repositoryName: 'maven-central' },
    },
  ],
  pagination: {
    totalItems: 50,
    totalPages: 3,
    currentPage: 1,
    itemsPerPage: 20,
  },
};

const defaultFilters: AuditFilters = {
  categories: [],
  eventTypes: [],
  dateRange: 'last-30-days',
  initiators: [],
  searchQuery: '',
};

describe('useAuditLogApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockRestClient.get.mockResolvedValue(mockAuditLogResponse);
  });

  describe('Initial Fetch', () => {
    it('should fetch data on mount', async () => {
      const { result } = renderHook(() =>
        useAuditLogApi({
          filters: defaultFilters,
          page: 1,
          limit: 20,
        })
      );

      // Initially loading
      expect(result.current.loading).toBe(true);
      expect(result.current.data).toBeNull();

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.data).toEqual(mockAuditLogResponse);
      expect(result.current.error).toBeNull();
    });

    it('should build correct URL with default parameters', async () => {
      renderHook(() =>
        useAuditLogApi({
          filters: defaultFilters,
          page: 1,
          limit: 20,
        })
      );

      await waitFor(() => {
        expect(mockRestClient.get).toHaveBeenCalled();
      });

      const calledUrl = mockRestClient.get.mock.calls[0][0];
      expect(calledUrl).toContain('/service/rest/internal/ui/audit-log');
      expect(calledUrl).toContain('page=1');
      expect(calledUrl).toContain('limit=20');
    });
  });

  describe('Pagination', () => {
    it('should include correct page in URL', async () => {
      renderHook(() =>
        useAuditLogApi({
          filters: defaultFilters,
          page: 3,
          limit: 20,
        })
      );

      await waitFor(() => {
        expect(mockRestClient.get).toHaveBeenCalled();
      });

      const calledUrl = mockRestClient.get.mock.calls[0][0];
      expect(calledUrl).toContain('page=3');
    });

    it('should include correct limit in URL', async () => {
      renderHook(() =>
        useAuditLogApi({
          filters: defaultFilters,
          page: 1,
          limit: 50,
        })
      );

      await waitFor(() => {
        expect(mockRestClient.get).toHaveBeenCalled();
      });

      const calledUrl = mockRestClient.get.mock.calls[0][0];
      expect(calledUrl).toContain('limit=50');
    });
  });

  describe('Category Filtering', () => {
    it('should include category filters in URL', async () => {
      const filters: AuditFilters = {
        ...defaultFilters,
        categories: ['security', 'repository'],
      };

      renderHook(() =>
        useAuditLogApi({
          filters,
          page: 1,
          limit: 20,
        })
      );

      await waitFor(() => {
        expect(mockRestClient.get).toHaveBeenCalled();
      });

      const calledUrl = mockRestClient.get.mock.calls[0][0];
      expect(calledUrl).toContain('categories=security');
      expect(calledUrl).toContain('categories=repository');
    });
  });

  describe('Event Type Filtering', () => {
    it('should include event type filters in URL', async () => {
      const filters: AuditFilters = {
        ...defaultFilters,
        eventTypes: ['created', 'deleted'],
      };

      renderHook(() =>
        useAuditLogApi({
          filters,
          page: 1,
          limit: 20,
        })
      );

      await waitFor(() => {
        expect(mockRestClient.get).toHaveBeenCalled();
      });

      const calledUrl = mockRestClient.get.mock.calls[0][0];
      expect(calledUrl).toContain('types=created');
      expect(calledUrl).toContain('types=deleted');
    });
  });

  describe('Initiator Filtering', () => {
    it('should include initiator filters in URL', async () => {
      const filters: AuditFilters = {
        ...defaultFilters,
        initiators: ['admin', 'system'],
      };

      renderHook(() =>
        useAuditLogApi({
          filters,
          page: 1,
          limit: 20,
        })
      );

      await waitFor(() => {
        expect(mockRestClient.get).toHaveBeenCalled();
      });

      const calledUrl = mockRestClient.get.mock.calls[0][0];
      expect(calledUrl).toContain('initiators=admin');
      expect(calledUrl).toContain('initiators=system');
    });
  });

  describe('Date Range Filtering', () => {
    it('should calculate correct date range for last-24-hours', async () => {
      const filters: AuditFilters = {
        ...defaultFilters,
        dateRange: 'last-24-hours',
      };

      renderHook(() =>
        useAuditLogApi({
          filters,
          page: 1,
          limit: 20,
        })
      );

      await waitFor(() => {
        expect(mockRestClient.get).toHaveBeenCalled();
      });

      const calledUrl = mockRestClient.get.mock.calls[0][0];
      expect(calledUrl).toContain('startDate=');
      expect(calledUrl).toContain('endDate=');
    });

    it('should calculate correct date range for last-7-days', async () => {
      const filters: AuditFilters = {
        ...defaultFilters,
        dateRange: 'last-7-days',
      };

      renderHook(() =>
        useAuditLogApi({
          filters,
          page: 1,
          limit: 20,
        })
      );

      await waitFor(() => {
        expect(mockRestClient.get).toHaveBeenCalled();
      });

      const calledUrl = mockRestClient.get.mock.calls[0][0];
      expect(calledUrl).toContain('startDate=');
      expect(calledUrl).toContain('endDate=');
    });

    it('should use custom dates when dateRange is custom', async () => {
      const filters: AuditFilters = {
        ...defaultFilters,
        dateRange: 'custom',
        customStartDate: '2026-03-01T00:00:00.000Z',
        customEndDate: '2026-03-10T23:59:59.999Z',
      };

      renderHook(() =>
        useAuditLogApi({
          filters,
          page: 1,
          limit: 20,
        })
      );

      await waitFor(() => {
        expect(mockRestClient.get).toHaveBeenCalled();
      });

      const calledUrl = mockRestClient.get.mock.calls[0][0];
      expect(calledUrl).toContain('startDate=2026-03-01T00%3A00%3A00.000Z');
      expect(calledUrl).toContain('endDate=2026-03-10T23%3A59%3A59.999Z');
    });
  });

  describe('Search Query', () => {
    it('should include search query in URL when provided', async () => {
      const filters: AuditFilters = {
        ...defaultFilters,
        searchQuery: 'test search',
      };

      renderHook(() =>
        useAuditLogApi({
          filters,
          page: 1,
          limit: 20,
        })
      );

      await waitFor(() => {
        expect(mockRestClient.get).toHaveBeenCalled();
      });

      const calledUrl = mockRestClient.get.mock.calls[0][0];
      expect(calledUrl).toContain('q=test+search');
    });
  });

  describe('Error Handling', () => {
    it('should handle API errors', async () => {
      const errorMessage = 'Network error';
      mockRestClient.get.mockRejectedValueOnce(new Error(errorMessage));

      const { result } = renderHook(() =>
        useAuditLogApi({
          filters: defaultFilters,
          page: 1,
          limit: 20,
        })
      );

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.error).toBe(errorMessage);
      expect(result.current.data).toBeNull();
    });

    it('should handle non-Error exceptions', async () => {
      mockRestClient.get.mockRejectedValueOnce('Unknown error');

      const { result } = renderHook(() =>
        useAuditLogApi({
          filters: defaultFilters,
          page: 1,
          limit: 20,
        })
      );

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.error).toBe('Failed to fetch audit log');
    });
  });

  describe('Refetch', () => {
    it('should refetch data when refetch is called', async () => {
      const { result } = renderHook(() =>
        useAuditLogApi({
          filters: defaultFilters,
          page: 1,
          limit: 20,
        })
      );

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(mockRestClient.get).toHaveBeenCalledTimes(1);

      act(() => {
        result.current.refetch();
      });

      await waitFor(() => {
        expect(mockRestClient.get).toHaveBeenCalledTimes(2);
      });
    });
  });

  describe('Re-fetch on Filter Change', () => {
    it('should fetch new data when filters change', async () => {
      const { result, rerender } = renderHook(
        ({ filters }) =>
          useAuditLogApi({
            filters,
            page: 1,
            limit: 20,
          }),
        {
          initialProps: { filters: defaultFilters },
        }
      );

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(mockRestClient.get).toHaveBeenCalledTimes(1);

      const newFilters: AuditFilters = {
        ...defaultFilters,
        categories: ['security'],
      };

      rerender({ filters: newFilters });

      await waitFor(() => {
        expect(mockRestClient.get).toHaveBeenCalledTimes(2);
      });
    });
  });
});
