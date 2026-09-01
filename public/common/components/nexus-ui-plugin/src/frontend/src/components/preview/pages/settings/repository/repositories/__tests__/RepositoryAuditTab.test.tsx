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

import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { Theme } from '@radix-ui/themes';
import { RepositoryAuditTab } from '../RepositoryAuditTab';
import type { AuditLogResponse } from '../../../../../../../utils/audit/audit.types';

// Mock the useAuditLogApi hook
jest.mock('../../../../../../../utils/audit/useAuditLogApi', () => ({
  useAuditLogApi: jest.fn(),
}));

// Mock formatAuditEvent and formatTimestamp
jest.mock('../../../../../../../utils/audit/auditEventFormatter', () => ({
  formatAuditEvent: jest.fn((event) => ({
    ...event,
    category: 'repository',
    eventLabel: 'Created',
    summary: `Repository '${event.context}' created`,
    entityType: 'Repository',
    entityName: event.context,
  })),
  formatTimestamp: jest.fn((timestamp) => {
    const date = new Date(timestamp);
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
      hour12: true,
    });
  }),
}));

const mockUseAuditLogApi = require('../../../../../../../utils/audit/useAuditLogApi').useAuditLogApi;

const mockAuditEvent = {
  id: 1,
  domain: 'repository',
  type: 'created',
  context: 'test-repo',
  timestamp: '2024-01-15T10:30:00Z',
  initiator: 'admin',
  nodeId: 'node-1',
  attributes: {},
};

const mockAuditLogResponse: AuditLogResponse = {
  items: [mockAuditEvent],
  pagination: {
    totalItems: 1,
    totalPages: 1,
    currentPage: 1,
    itemsPerPage: 5,
  },
};

function renderRepositoryAuditTab(repositoryName: string = 'test-repo') {
  return render(
    <Theme>
      <RepositoryAuditTab repositoryName={repositoryName} />
    </Theme>
  );
}

describe('RepositoryAuditTab', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('loading state', () => {
    it('shouldRenderSpinnerWhenLoading', () => {
      mockUseAuditLogApi.mockReturnValue({
        data: null,
        loading: true,
        error: null,
      });

      renderRepositoryAuditTab();

      // Radix Spinner has class rt-Spinner
      const spinner = document.querySelector('.rt-Spinner');
      expect(spinner).toBeInTheDocument();
    });
  });

  describe('error state', () => {
    it('shouldRenderErrorMessageWhenErrorOccurs', () => {
      mockUseAuditLogApi.mockReturnValue({
        data: null,
        loading: false,
        error: 'Failed to fetch audit log',
      });

      renderRepositoryAuditTab();

      expect(screen.getByText('Failed to load audit events.')).toBeInTheDocument();
    });

    it('shouldRenderLinkToFullAuditLogOnError', () => {
      mockUseAuditLogApi.mockReturnValue({
        data: null,
        loading: false,
        error: 'Failed to fetch audit log',
      });

      renderRepositoryAuditTab('my-repo');

      const link = screen.getByRole('link', { name: /Open Full Audit Log/ });
      expect(link).toHaveAttribute('href', '#preview/browse/audit?repositoryName=my-repo');
    });
  });

  describe('empty state', () => {
    it('shouldRenderEmptyStateWhenNoEvents', () => {
      mockUseAuditLogApi.mockReturnValue({
        data: {
          items: [],
          pagination: {
            totalItems: 0,
            totalPages: 0,
            currentPage: 1,
            itemsPerPage: 5,
          },
        },
        loading: false,
        error: null,
      });

      renderRepositoryAuditTab();

      expect(screen.getByText('No Audit Events')).toBeInTheDocument();
      expect(screen.getByText('No audit activity recorded for this repository in the last 30 days.')).toBeInTheDocument();
    });

    it('shouldRenderLinkToFullAuditLogWhenEmpty', () => {
      mockUseAuditLogApi.mockReturnValue({
        data: {
          items: [],
          pagination: {
            totalItems: 0,
            totalPages: 0,
            currentPage: 1,
            itemsPerPage: 5,
          },
        },
        loading: false,
        error: null,
      });

      renderRepositoryAuditTab('my-repo');

      const link = screen.getByRole('link', { name: /Open Full Audit Log/ });
      expect(link).toHaveAttribute('href', '#preview/browse/audit?repositoryName=my-repo');
    });
  });

  describe('successful data rendering', () => {
    it('shouldRenderRecentActivityHeader', () => {
      mockUseAuditLogApi.mockReturnValue({
        data: mockAuditLogResponse,
        loading: false,
        error: null,
      });

      renderRepositoryAuditTab();

      expect(screen.getByText('Recent Activity')).toBeInTheDocument();
    });

    it('shouldRenderEventCount', () => {
      mockUseAuditLogApi.mockReturnValue({
        data: mockAuditLogResponse,
        loading: false,
        error: null,
      });

      renderRepositoryAuditTab();

      expect(screen.getByText('1 event (30 days)')).toBeInTheDocument();
    });

    it('shouldRenderEventCountWithPluralization', () => {
      mockUseAuditLogApi.mockReturnValue({
        data: {
          items: [mockAuditEvent],
          pagination: {
            totalItems: 5,
            totalPages: 1,
            currentPage: 1,
            itemsPerPage: 5,
          },
        },
        loading: false,
        error: null,
      });

      renderRepositoryAuditTab();

      expect(screen.getByText('5 events (30 days)')).toBeInTheDocument();
    });

    it('shouldRenderTableHeaders', () => {
      mockUseAuditLogApi.mockReturnValue({
        data: mockAuditLogResponse,
        loading: false,
        error: null,
      });

      renderRepositoryAuditTab();

      expect(screen.getByText('Time')).toBeInTheDocument();
      expect(screen.getByText('Category')).toBeInTheDocument();
      expect(screen.getByText('Event')).toBeInTheDocument();
      expect(screen.getByText('Summary')).toBeInTheDocument();
      expect(screen.getByText('Initiator')).toBeInTheDocument();
    });

    it('shouldRenderAuditEventInTable', () => {
      mockUseAuditLogApi.mockReturnValue({
        data: mockAuditLogResponse,
        loading: false,
        error: null,
      });

      renderRepositoryAuditTab();

      // Check initiator is rendered
      expect(screen.getByText('admin')).toBeInTheDocument();
    });

    it('shouldRenderSystemInitiatorWhenNull', () => {
      mockUseAuditLogApi.mockReturnValue({
        data: {
          items: [
            {
              ...mockAuditEvent,
              initiator: null,
            },
          ],
          pagination: {
            totalItems: 1,
            totalPages: 1,
            currentPage: 1,
            itemsPerPage: 5,
          },
        },
        loading: false,
        error: null,
      });

      renderRepositoryAuditTab();

      expect(screen.getByText('system')).toBeInTheDocument();
    });

    it('shouldRenderViewFullAuditLogButton', () => {
      mockUseAuditLogApi.mockReturnValue({
        data: mockAuditLogResponse,
        loading: false,
        error: null,
      });

      renderRepositoryAuditTab('my-repo');

      const link = screen.getByRole('link', { name: /View Full Audit Log/ });
      expect(link).toHaveAttribute('href', '#preview/browse/audit?repositoryName=my-repo');
    });
  });

  describe('pagination indicator', () => {
    it('shouldNotRenderPaginationIndicatorWhenFiveOrFewerEvents', () => {
      mockUseAuditLogApi.mockReturnValue({
        data: {
          items: [mockAuditEvent],
          pagination: {
            totalItems: 5,
            totalPages: 1,
            currentPage: 1,
            itemsPerPage: 5,
          },
        },
        loading: false,
        error: null,
      });

      renderRepositoryAuditTab();

      expect(screen.queryByText(/Showing 5 of/)).not.toBeInTheDocument();
    });

    it('shouldRenderPaginationIndicatorWhenMoreThanFiveEvents', () => {
      mockUseAuditLogApi.mockReturnValue({
        data: {
          items: [mockAuditEvent],
          pagination: {
            totalItems: 10,
            totalPages: 2,
            currentPage: 1,
            itemsPerPage: 5,
          },
        },
        loading: false,
        error: null,
      });

      renderRepositoryAuditTab();

      expect(screen.getByText('Showing 5 of 10 events.')).toBeInTheDocument();
    });

    it('shouldRenderViewAllLinkInPaginationIndicator', () => {
      mockUseAuditLogApi.mockReturnValue({
        data: {
          items: [mockAuditEvent],
          pagination: {
            totalItems: 10,
            totalPages: 2,
            currentPage: 1,
            itemsPerPage: 5,
          },
        },
        loading: false,
        error: null,
      });

      renderRepositoryAuditTab('my-repo');

      const viewAllLink = screen.getByText('View all in Audit Log');
      expect(viewAllLink).toHaveAttribute('href', '#preview/browse/audit?repositoryName=my-repo');
    });
  });

  describe('useAuditLogApi integration', () => {
    it('shouldCallUseAuditLogApiWithCorrectFilters', () => {
      mockUseAuditLogApi.mockReturnValue({
        data: mockAuditLogResponse,
        loading: false,
        error: null,
      });

      renderRepositoryAuditTab('my-repo');

      expect(mockUseAuditLogApi).toHaveBeenCalledWith({
        filters: {
          categories: [],
          domains: [],
          eventTypes: [],
          dateRange: 'last-30-days',
          initiator: '',
          initiators: [],
          searchQuery: '',
          repositoryName: 'my-repo',
        },
        page: 1,
        limit: 5,
      });
    });

    it('shouldEncodeRepositoryNameInUrl', () => {
      mockUseAuditLogApi.mockReturnValue({
        data: mockAuditLogResponse,
        loading: false,
        error: null,
      });

      renderRepositoryAuditTab('my repo with spaces');

      const link = screen.getByRole('link', { name: /View Full Audit Log/ });
      expect(link).toHaveAttribute('href', '#preview/browse/audit?repositoryName=my%20repo%20with%20spaces');
    });
  });
});
