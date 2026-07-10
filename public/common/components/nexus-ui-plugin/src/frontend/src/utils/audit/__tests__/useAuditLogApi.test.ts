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
import type { AuditFilters, AuditLogResponse } from '../audit.types';
import { restClient } from '../../../interface/api';

const mockGet = jest.spyOn(restClient, 'get');

const mockResponse: AuditLogResponse = {
  items: [],
  pagination: { totalItems: 0, totalPages: 0, currentPage: 1, itemsPerPage: 20 },
};

const baseFilters: AuditFilters = {
  categories: [],
  domains: [],
  eventTypes: [],
  dateRange: 'last-30-days',
  initiator: '',
  initiators: [],
  searchQuery: '',
};

beforeEach(() => {
  jest.clearAllMocks();
  mockGet.mockResolvedValue(mockResponse);
});

describe('useAuditLogApi — branch coverage', () => {
  describe('Date Range: last-90-days', () => {
    it('appends startDate and endDate for last-90-days', async () => {
      const filters: AuditFilters = { ...baseFilters, dateRange: 'last-90-days' };

      renderHook(() => useAuditLogApi({ filters, page: 1, limit: 20 }));

      await waitFor(() => expect(mockGet).toHaveBeenCalled());

      const url = mockGet.mock.calls[0][0] as string;
      expect(url).toContain('startDate=');
      expect(url).toContain('endDate=');

      // Verify approximately 90 days back: startDate should be before endDate
      const params = new URLSearchParams(url.split('?')[1]);
      const start = new Date(params.get('startDate')!);
      const end = new Date(params.get('endDate')!);
      const diffDays = (end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24);
      expect(diffDays).toBeGreaterThan(88);
      expect(diffDays).toBeLessThan(92);
    });
  });

  describe('Date Range: unknown/default (falls back to 30 days)', () => {
    it('uses 30-day window for an unrecognised dateRange value', async () => {
      // Cast to bypass TypeScript so we can exercise the default switch branch
      const filters = { ...baseFilters, dateRange: 'last-60-days' } as unknown as AuditFilters;

      renderHook(() => useAuditLogApi({ filters, page: 1, limit: 20 }));

      await waitFor(() => expect(mockGet).toHaveBeenCalled());

      const url = mockGet.mock.calls[0][0] as string;
      expect(url).toContain('startDate=');
      expect(url).toContain('endDate=');

      const params = new URLSearchParams(url.split('?')[1]);
      const start = new Date(params.get('startDate')!);
      const end = new Date(params.get('endDate')!);
      const diffDays = (end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24);
      expect(diffDays).toBeGreaterThan(28);
      expect(diffDays).toBeLessThan(32);
    });
  });

  describe('Date Range: custom without both dates', () => {
    it('omits startDate/endDate when customStartDate is missing', async () => {
      const filters: AuditFilters = {
        ...baseFilters,
        dateRange: 'custom',
        customStartDate: undefined,
        customEndDate: '2026-03-10T23:59:59.999Z',
      };

      renderHook(() => useAuditLogApi({ filters, page: 1, limit: 20 }));

      await waitFor(() => expect(mockGet).toHaveBeenCalled());

      const url = mockGet.mock.calls[0][0] as string;
      expect(url).not.toContain('startDate=');
      expect(url).not.toContain('endDate=');
    });

    it('omits startDate/endDate when customEndDate is missing', async () => {
      const filters: AuditFilters = {
        ...baseFilters,
        dateRange: 'custom',
        customStartDate: '2026-03-01T00:00:00.000Z',
        customEndDate: undefined,
      };

      renderHook(() => useAuditLogApi({ filters, page: 1, limit: 20 }));

      await waitFor(() => expect(mockGet).toHaveBeenCalled());

      const url = mockGet.mock.calls[0][0] as string;
      expect(url).not.toContain('startDate=');
      expect(url).not.toContain('endDate=');
    });

    it('omits startDate/endDate when both custom dates are absent', async () => {
      const filters: AuditFilters = {
        ...baseFilters,
        dateRange: 'custom',
      };

      renderHook(() => useAuditLogApi({ filters, page: 1, limit: 20 }));

      await waitFor(() => expect(mockGet).toHaveBeenCalled());

      const url = mockGet.mock.calls[0][0] as string;
      expect(url).not.toContain('startDate=');
      expect(url).not.toContain('endDate=');
    });
  });

  describe('Repository filter', () => {
    it('appends repositoryName to the URL when provided', async () => {
      const filters: AuditFilters = { ...baseFilters, repositoryName: 'maven-central' };

      renderHook(() => useAuditLogApi({ filters, page: 1, limit: 20 }));

      await waitFor(() => expect(mockGet).toHaveBeenCalled());

      const url = mockGet.mock.calls[0][0] as string;
      expect(url).toContain('repositoryName=maven-central');
    });

    it('omits repositoryName from the URL when not provided', async () => {
      renderHook(() => useAuditLogApi({ filters: baseFilters, page: 1, limit: 20 }));

      await waitFor(() => expect(mockGet).toHaveBeenCalled());

      const url = mockGet.mock.calls[0][0] as string;
      expect(url).not.toContain('repositoryName=');
    });
  });

  describe('Effect cleanup (cancelled flag)', () => {
    it('does not update state when the hook unmounts before the fetch completes', async () => {
      let resolveFetch!: (value: AuditLogResponse) => void;
      mockGet.mockReturnValueOnce(
        new Promise<AuditLogResponse>((resolve) => {
          resolveFetch = resolve;
        }) as Promise<unknown> as Promise<AuditLogResponse>
      );

      const { result, unmount } = renderHook(() =>
        useAuditLogApi({ filters: baseFilters, page: 1, limit: 20 })
      );

      // Unmount before the fetch resolves
      unmount();

      // Resolve after unmount — state should not be updated
      act(() => {
        resolveFetch(mockResponse);
      });

      // data stays null because cancelled=true suppressed the state update
      expect(result.current.data).toBeNull();
    });
  });

  describe('Search query omitted when empty', () => {
    it('does not append q param when searchQuery is empty', async () => {
      renderHook(() => useAuditLogApi({ filters: baseFilters, page: 1, limit: 20 }));

      await waitFor(() => expect(mockGet).toHaveBeenCalled());

      const url = mockGet.mock.calls[0][0] as string;
      expect(url).not.toContain('q=');
    });
  });

  describe('Search query included when non-empty', () => {
    it('appends q param when searchQuery is provided', async () => {
      const filters: AuditFilters = { ...baseFilters, searchQuery: 'admin' };

      renderHook(() => useAuditLogApi({ filters, page: 1, limit: 20 }));

      await waitFor(() => expect(mockGet).toHaveBeenCalled());

      const url = mockGet.mock.calls[0][0] as string;
      expect(url).toContain('q=admin');
    });
  });

  describe('Date Range: custom with both dates provided', () => {
    it('uses the provided start and end dates in the URL', async () => {
      const filters: AuditFilters = {
        ...baseFilters,
        dateRange: 'custom',
        customStartDate: '2026-01-01T00:00:00.000Z',
        customEndDate: '2026-01-31T23:59:59.999Z',
      };

      renderHook(() => useAuditLogApi({ filters, page: 1, limit: 20 }));

      await waitFor(() => expect(mockGet).toHaveBeenCalled());

      const url = mockGet.mock.calls[0][0] as string;
      expect(url).toContain('startDate=2026-01-01T00%3A00%3A00.000Z');
      expect(url).toContain('endDate=2026-01-31T23%3A59%3A59.999Z');
    });
  });

  describe('Basic fetch success', () => {
    it('sets data and clears loading after a successful fetch', async () => {
      const { result } = renderHook(() =>
        useAuditLogApi({ filters: baseFilters, page: 1, limit: 20 })
      );

      await waitFor(() => expect(result.current.loading).toBe(false));

      expect(result.current.data).toEqual(mockResponse);
      expect(result.current.error).toBeNull();
    });
  });

  describe('Refetch', () => {
    it('re-issues the request when refetch is called', async () => {
      const { result } = renderHook(() =>
        useAuditLogApi({ filters: baseFilters, page: 1, limit: 20 })
      );

      await waitFor(() => expect(result.current.loading).toBe(false));
      expect(mockGet).toHaveBeenCalledTimes(1);

      act(() => {
        result.current.refetch();
      });

      await waitFor(() => expect(mockGet).toHaveBeenCalledTimes(2));
    });
  });

  describe('Error handling', () => {
    it('sets error message from Error instance when fetch fails', async () => {
      mockGet.mockRejectedValueOnce(new Error('Network failure'));

      const { result } = renderHook(() =>
        useAuditLogApi({ filters: baseFilters, page: 1, limit: 20 })
      );

      await waitFor(() => expect(result.current.loading).toBe(false));

      expect(result.current.error).toBe('Network failure');
      expect(result.current.data).toBeNull();
    });

    it('sets generic error message for non-Error rejections', async () => {
      mockGet.mockRejectedValueOnce('unexpected');

      const { result } = renderHook(() =>
        useAuditLogApi({ filters: baseFilters, page: 1, limit: 20 })
      );

      await waitFor(() => expect(result.current.loading).toBe(false));

      expect(result.current.error).toBe('Failed to fetch audit log');
    });
  });

  describe('Date Range: last-24-hours', () => {
    it('appends startDate approximately 1 day back', async () => {
      const filters: AuditFilters = { ...baseFilters, dateRange: 'last-24-hours' };

      renderHook(() => useAuditLogApi({ filters, page: 1, limit: 20 }));

      await waitFor(() => expect(mockGet).toHaveBeenCalled());

      const url = mockGet.mock.calls[0][0] as string;
      const params = new URLSearchParams(url.split('?')[1]);
      const start = new Date(params.get('startDate')!);
      const end = new Date(params.get('endDate')!);
      const diffDays = (end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24);
      expect(diffDays).toBeGreaterThan(0.9);
      expect(diffDays).toBeLessThan(1.1);
    });
  });

  describe('Date Range: last-7-days', () => {
    it('appends startDate approximately 7 days back', async () => {
      const filters: AuditFilters = { ...baseFilters, dateRange: 'last-7-days' };

      renderHook(() => useAuditLogApi({ filters, page: 1, limit: 20 }));

      await waitFor(() => expect(mockGet).toHaveBeenCalled());

      const url = mockGet.mock.calls[0][0] as string;
      const params = new URLSearchParams(url.split('?')[1]);
      const start = new Date(params.get('startDate')!);
      const end = new Date(params.get('endDate')!);
      const diffDays = (end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24);
      expect(diffDays).toBeGreaterThan(6.9);
      expect(diffDays).toBeLessThan(7.1);
    });
  });
});
