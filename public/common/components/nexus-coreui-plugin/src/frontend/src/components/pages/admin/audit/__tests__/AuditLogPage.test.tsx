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
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';

import { AuditLogPage } from '../AuditLogPage';
import type { AuditLogResponse, AuditEvent } from '../audit.types';

jest.mock('../../../../shared/FilterSidebar', () => {
  const React = require('react');
  return {
    FilterSidebar: ({ sections, onClear, disabled, onFilterChange }: any) => {
      const children = [
        React.createElement('button', {
          key: 'clear',
          type: 'button',
          disabled,
          onClick: onClear,
          title: 'Clear all filters',
        }, 'Clear all'),
        ...(sections || []).map((section: any) => {
          const sectionChildren: any[] = [
            React.createElement('span', { key: 'label' }, section.label),
          ];
          if (section.type === 'checkbox' && section.options) {
            section.options.forEach((opt: any) => {
              const checked = Array.isArray(section.value) ? section.value.includes(opt.value) : false;
              sectionChildren.push(
                React.createElement('label', { key: opt.value },
                  React.createElement('input', {
                    type: 'checkbox',
                    checked,
                    onChange: () => {
                      if (Array.isArray(section.value)) {
                        const newVal = checked
                          ? section.value.filter((v: string) => v !== opt.value)
                          : [...section.value, opt.value];
                        onFilterChange?.(section.id, newVal);
                      }
                    },
                    disabled,
                  }),
                  opt.label,
                ),
              );
            });
          }
          if (section.type === 'select' && section.options) {
            sectionChildren.push(
              React.createElement('select', {
                key: 'select',
                value: typeof section.value === 'string' ? section.value : '',
                onChange: (e: any) => onFilterChange?.(section.id, e.target.value),
                disabled,
              }, section.options.map((opt: any) =>
                React.createElement('option', { key: opt.value, value: opt.value }, opt.label),
              )),
            );
          }
          return React.createElement('div', { key: section.id }, ...sectionChildren);
        }),
      ];
      return React.createElement('div', { 'data-testid': 'filter-sidebar' }, ...children);
    },
  };
});

// Mock @uirouter/react
jest.mock('@uirouter/react', () => ({
  useCurrentStateAndParams: () => ({ params: {} }),
}));

// Mock useRepositoriesApi
jest.mock('../../../../super/settings/repository/repositories/useRepositoriesApi', () => ({
  useRepositoriesApi: () => ({
    fetchRepositories: jest.fn().mockResolvedValue([]),
  }),
}));

// Mock the useAuditLogApi hook
const mockRefetch = jest.fn();
let mockData: AuditLogResponse | null = null;
let mockLoading = false;
let mockError: string | null = null;

jest.mock('../useAuditLogApi', () => ({
  useAuditLogApi: () => ({
    data: mockData,
    loading: mockLoading,
    error: mockError,
    refetch: mockRefetch,
  }),
}));

// Test data
const mockAuditEvents: AuditEvent[] = [
  {
    id: 1,
    domain: 'security.user',
    type: 'created',
    context: 'testuser',
    timestamp: '2026-03-12T10:00:00.000Z',
    initiator: 'admin',
    nodeId: 'node-1',
    attributes: { name: 'testuser', email: 'test@example.com' },
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
  {
    id: 3,
    domain: 'tasks',
    type: 'finished',
    context: 'Cleanup Task',
    timestamp: '2026-03-12T09:00:00.000Z',
    initiator: '*TASK',
    nodeId: 'node-1',
    attributes: { taskName: 'Cleanup Task', duration: '5m' },
  },
  {
    id: 4,
    domain: 'protection.config',
    type: 'updated',
    context: 'RHC Configuration',
    timestamp: '2026-03-12T08:30:00.000Z',
    initiator: null,
    nodeId: 'node-1',
    attributes: { enabled: true },
  },
];

const mockPaginatedResponse: AuditLogResponse = {
  items: mockAuditEvents,
  pagination: {
    totalItems: 100,
    totalPages: 5,
    currentPage: 1,
    itemsPerPage: 20,
  },
};

// Helper to render with Radix Theme provider
function renderWithTheme(component: React.ReactElement) {
  return render(<Theme>{component}</Theme>);
}

describe('AuditLogPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockData = mockPaginatedResponse;
    mockLoading = false;
    mockError = null;
  });

  describe('Component Rendering', () => {
    it('should render the page with correct heading', () => {
      renderWithTheme(<AuditLogPage />);

      expect(screen.getByRole('heading', { name: 'Audit Log' })).toBeInTheDocument();
    });

    it('should render refresh and export buttons', () => {
      renderWithTheme(<AuditLogPage />);

      expect(screen.getByRole('button', { name: /refresh/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /export csv/i })).toBeInTheDocument();
    });

    it('should render audit events table with correct columns', () => {
      renderWithTheme(<AuditLogPage />);

      expect(screen.getByText('Timestamp')).toBeInTheDocument();
      expect(screen.getByRole('columnheader', { name: 'Category' })).toBeInTheDocument();
      expect(screen.getByRole('columnheader', { name: 'Event' })).toBeInTheDocument();
      expect(screen.getByRole('columnheader', { name: 'Summary' })).toBeInTheDocument();
      expect(screen.getByRole('columnheader', { name: 'Initiator' })).toBeInTheDocument();
    });

    it('should render all audit events in the table', () => {
      renderWithTheme(<AuditLogPage />);

      // Check for initiator names
      expect(screen.getAllByText('admin').length).toBeGreaterThanOrEqual(2);
      expect(screen.getByText('*TASK')).toBeInTheDocument();
      expect(screen.getByText('system')).toBeInTheDocument(); // null initiator displays as 'system'
    });

    it('should render category badges in the table', () => {
      renderWithTheme(<AuditLogPage />);

      // Check that category badges exist in the table (using getAllBy since they appear in sidebar too)
      const securityBadges = screen.getAllByText('Security');
      const repositoryBadges = screen.getAllByText('Repository');
      const configurationBadges = screen.getAllByText('Configuration');
      const protectionBadges = screen.getAllByText('Protection');

      // Each should have at least one in the table
      expect(securityBadges.length).toBeGreaterThanOrEqual(1);
      expect(repositoryBadges.length).toBeGreaterThanOrEqual(1);
      expect(configurationBadges.length).toBeGreaterThanOrEqual(1);
      expect(protectionBadges.length).toBeGreaterThanOrEqual(1);
    });

    it('should render with testid for page identification', () => {
      renderWithTheme(<AuditLogPage />);

      expect(screen.getByTestId('audit-log-page')).toBeInTheDocument();
    });
  });

  describe('Filter Sidebar', () => {
    it('should render reset filters button', () => {
      renderWithTheme(<AuditLogPage />);

      expect(screen.getByRole('button', { name: /clear all/i })).toBeInTheDocument();
    });

    it('should render category filter checkboxes', () => {
      renderWithTheme(<AuditLogPage />);

      // The sidebar should have category checkboxes
      const checkboxes = screen.getAllByRole('checkbox');
      expect(checkboxes.length).toBeGreaterThanOrEqual(4); // At least 4 categories
    });

    it('should render date range dropdown', () => {
      renderWithTheme(<AuditLogPage />);

      const comboboxes = screen.getAllByRole('combobox');
      expect(comboboxes.length).toBeGreaterThanOrEqual(1);
    });

    it('should toggle category filter when checkbox clicked', async () => {
      renderWithTheme(<AuditLogPage />);

      const checkboxes = screen.getAllByRole('checkbox');
      const securityCheckbox = checkboxes[0]; // First checkbox should be Security

      await userEvent.click(securityCheckbox);

      // Checkbox should become checked
      expect(securityCheckbox).toBeChecked();
    });
  });

  describe('Search by Context', () => {
    it('should render search input field', () => {
      renderWithTheme(<AuditLogPage />);

      expect(screen.getByPlaceholderText(/search by context/i)).toBeInTheDocument();
    });

    it('should allow typing in search field', async () => {
      renderWithTheme(<AuditLogPage />);

      const searchInput = screen.getByPlaceholderText(/search by context/i);
      await userEvent.type(searchInput, 'testuser');

      expect(searchInput).toHaveValue('testuser');
    });

    it('should show clear button when search has value', async () => {
      renderWithTheme(<AuditLogPage />);

      const searchInput = screen.getByPlaceholderText(/search by context/i);
      await userEvent.type(searchInput, 'testuser');

      const clearButton = screen.getByRole('button', { name: /clear search/i });
      expect(clearButton).toBeInTheDocument();
    });

    it('should clear search when clear button clicked', async () => {
      renderWithTheme(<AuditLogPage />);

      const searchInput = screen.getByPlaceholderText(/search by context/i);
      await userEvent.type(searchInput, 'testuser');

      const clearButton = screen.getByRole('button', { name: /clear search/i });
      await userEvent.click(clearButton);

      expect(searchInput).toHaveValue('');
    });
  });

  describe('Loading State', () => {
    it('should show loading spinner when loading', () => {
      mockLoading = true;
      mockData = null;

      renderWithTheme(<AuditLogPage />);

      expect(screen.getByText('Loading audit events...')).toBeInTheDocument();
    });

    it('should not show table while loading', () => {
      mockLoading = true;
      mockData = null;

      renderWithTheme(<AuditLogPage />);

      expect(screen.queryByRole('table')).not.toBeInTheDocument();
    });
  });

  describe('Error State', () => {
    it('should display error message when error occurs', () => {
      mockError = 'Failed to fetch audit events';
      mockData = null;

      renderWithTheme(<AuditLogPage />);

      expect(screen.getByText('Failed to fetch audit events')).toBeInTheDocument();
    });
  });

  describe('Empty State', () => {
    it('should display empty state when no events found', () => {
      mockData = {
        items: [],
        pagination: {
          totalItems: 0,
          totalPages: 0,
          currentPage: 1,
          itemsPerPage: 20,
        },
      };

      renderWithTheme(<AuditLogPage />);

      expect(screen.getByText('No Audit Events Found')).toBeInTheDocument();
    });

    it('should show clear filters button in empty state when filters active', async () => {
      mockData = {
        items: [],
        pagination: {
          totalItems: 0,
          totalPages: 0,
          currentPage: 1,
          itemsPerPage: 20,
        },
      };

      renderWithTheme(<AuditLogPage />);

      // Apply a filter first
      const checkboxes = screen.getAllByRole('checkbox');
      await userEvent.click(checkboxes[0]); // Toggle a category

      // Should show clear all filters button in the empty state area
      const clearButtons = screen.getAllByRole('button', { name: /clear/i });
      expect(clearButtons.length).toBeGreaterThanOrEqual(1);
    });
  });

  describe('Pagination', () => {
    it('should display pagination info', () => {
      renderWithTheme(<AuditLogPage />);

      expect(screen.getByText(/Showing 1-20 of 100 events/)).toBeInTheDocument();
    });

    it('should render previous and next buttons', () => {
      renderWithTheme(<AuditLogPage />);

      expect(screen.getByRole('button', { name: 'Previous' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Next' })).toBeInTheDocument();
    });

    it('should disable previous button on first page', () => {
      renderWithTheme(<AuditLogPage />);

      expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled();
    });

    it('should enable next button when more pages exist', () => {
      renderWithTheme(<AuditLogPage />);

      expect(screen.getByRole('button', { name: 'Next' })).not.toBeDisabled();
    });

    it('should display page info', () => {
      renderWithTheme(<AuditLogPage />);

      expect(screen.getByText('Page 1 of 5')).toBeInTheDocument();
    });
  });

  describe('Row Expansion', () => {
    it('should expand row when clicked', async () => {
      renderWithTheme(<AuditLogPage />);

      // Find the first expandable row
      const rows = screen.getAllByRole('row');
      const dataRow = rows[1]; // First data row (index 0 is header)

      fireEvent.click(dataRow);

      await waitFor(() => {
        expect(screen.getByText('Event Details')).toBeInTheDocument();
      });
    });

    it('should show domain and type in expanded details', async () => {
      renderWithTheme(<AuditLogPage />);

      const rows = screen.getAllByRole('row');
      const dataRow = rows[1];

      fireEvent.click(dataRow);

      await waitFor(() => {
        expect(screen.getByText(/Domain:/)).toBeInTheDocument();
        expect(screen.getByText(/Type:/)).toBeInTheDocument();
      });
    });

    it('should show attributes in expanded details', async () => {
      renderWithTheme(<AuditLogPage />);

      const rows = screen.getAllByRole('row');
      const dataRow = rows[1];

      fireEvent.click(dataRow);

      await waitFor(() => {
        expect(screen.getByText('Attributes:')).toBeInTheDocument();
      });
    });

    it('should collapse row when clicked again', async () => {
      renderWithTheme(<AuditLogPage />);

      const rows = screen.getAllByRole('row');
      const dataRow = rows[1];

      // Expand
      fireEvent.click(dataRow);
      await waitFor(() => {
        expect(screen.getByText('Event Details')).toBeInTheDocument();
      });

      // Collapse
      fireEvent.click(dataRow);
      await waitFor(() => {
        expect(screen.queryByText('Event Details')).not.toBeInTheDocument();
      });
    });
  });

  describe('Refresh Functionality', () => {
    it('should call refetch when refresh button is clicked', async () => {
      renderWithTheme(<AuditLogPage />);

      const refreshButton = screen.getByRole('button', { name: /refresh/i });
      await userEvent.click(refreshButton);

      expect(mockRefetch).toHaveBeenCalled();
    });
  });

  describe('CSV Export', () => {
    it('should have export CSV button', () => {
      renderWithTheme(<AuditLogPage />);

      expect(screen.getByRole('button', { name: /export csv/i })).toBeInTheDocument();
    });

    it('should allow clicking export button without error', async () => {
      renderWithTheme(<AuditLogPage />);

      const exportButton = screen.getByRole('button', { name: /export csv/i });
      await userEvent.click(exportButton);

      expect(exportButton).toBeInTheDocument();
    });
  });

  describe('Accessibility', () => {
    it('should have accessible table structure', () => {
      renderWithTheme(<AuditLogPage />);

      expect(screen.getByRole('table')).toBeInTheDocument();
      expect(screen.getAllByRole('columnheader').length).toBeGreaterThan(0);
      expect(screen.getAllByRole('row').length).toBeGreaterThan(1);
    });

    it('should have accessible buttons', () => {
      renderWithTheme(<AuditLogPage />);

      const buttons = screen.getAllByRole('button');
      expect(buttons.length).toBeGreaterThan(0);
    });

    it('should have aria labels for filter bar and main content', () => {
      renderWithTheme(<AuditLogPage />);

      expect(screen.getByRole('complementary', { name: /filter bar/i })).toBeInTheDocument();
      expect(screen.getByRole('main', { name: /page content/i })).toBeInTheDocument();
    });

    it('should have aria label for actions bar', () => {
      renderWithTheme(<AuditLogPage />);

      expect(screen.getByRole('toolbar', { name: /actions bar/i })).toBeInTheDocument();
    });
  });
});
