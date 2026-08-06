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
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';

import { AuditFilterSidebar } from '../AuditFilterSidebar';
import { AuditFilters, AuditCategory } from '../audit.types';

const defaultFilters: AuditFilters = {
  categories: [],
  domains: [],
  eventTypes: [],
  dateRange: 'last-30-days',
  initiator: '',
  initiators: [],
  searchQuery: '',
};

const mockOnCategoryToggle = jest.fn();
const mockOnEventTypeToggle = jest.fn();
const mockOnInitiatorChange = jest.fn();
const mockOnRepositoryNameChange = jest.fn();
const mockOnRepositoryTypeChange = jest.fn();
const mockOnDateRangeChange = jest.fn();
const mockOnClearAllFilters = jest.fn();

function renderWithTheme(component: React.ReactElement) {
  return render(<Theme>{component}</Theme>);
}

function renderSidebar(overrides: Partial<Parameters<typeof AuditFilterSidebar>[0]> = {}) {
  return renderWithTheme(
    <AuditFilterSidebar
      filters={defaultFilters}
      repositories={[]}
      onCategoryToggle={mockOnCategoryToggle}
      onEventTypeToggle={mockOnEventTypeToggle}
      onInitiatorChange={mockOnInitiatorChange}
      onRepositoryNameChange={mockOnRepositoryNameChange}
      onRepositoryTypeChange={mockOnRepositoryTypeChange}
      onDateRangeChange={mockOnDateRangeChange}
      onClearAllFilters={mockOnClearAllFilters}
      {...overrides}
    />
  );
}

describe('AuditFilterSidebar', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('Rendering', () => {
    it('should render reset filters button', () => {
      renderSidebar();
      expect(screen.getByRole('button', { name: /reset filters/i })).toBeInTheDocument();
    });

    it('should disable reset button when disabled prop is true', () => {
      renderSidebar({ disabled: true });
      expect(screen.getByRole('button', { name: /reset filters/i })).toBeDisabled();
    });

    it('should enable reset button by default', () => {
      renderSidebar();
      expect(screen.getByRole('button', { name: /reset filters/i })).not.toBeDisabled();
    });

    it('should render date range section', () => {
      renderSidebar();
      expect(screen.getByText('Date Range')).toBeInTheDocument();
    });

    it('should render category section', () => {
      renderSidebar();
      expect(screen.getByText('Categories')).toBeInTheDocument();
    });

    it('should render event type section', () => {
      renderSidebar();
      expect(screen.getByText('Event Types')).toBeInTheDocument();
    });

    it('should render initiator section', () => {
      renderSidebar();
      expect(screen.getByText('Initiator')).toBeInTheDocument();
    });

    it('should render all category options', () => {
      renderSidebar();
      expect(screen.getByText('Security')).toBeInTheDocument();
      expect(screen.getAllByText('Repository').length).toBeGreaterThanOrEqual(1);
      expect(screen.getByText('Configuration')).toBeInTheDocument();
      expect(screen.getByText('Protection')).toBeInTheDocument();
    });

    it('should render event type options', () => {
      renderSidebar();
      expect(screen.getByText('Created')).toBeInTheDocument();
      expect(screen.getByText('Updated')).toBeInTheDocument();
      expect(screen.getByText('Deleted')).toBeInTheDocument();
    });

    it('should render initiator text field', () => {
      renderSidebar();
      const inputs = screen.getAllByRole('textbox');
      expect(inputs.length).toBeGreaterThanOrEqual(1);
    });
  });

  describe('Category Filter', () => {
    it('should call onCategoryToggle when category checkbox clicked', async () => {
      renderSidebar();
      const checkboxes = screen.getAllByRole('checkbox');
      await userEvent.click(checkboxes[0]);
      expect(mockOnCategoryToggle).toHaveBeenCalled();
    });

    it('should show category as checked when in filters', () => {
      const filtersWithCategory: AuditFilters = {
        ...defaultFilters,
        categories: ['security' as AuditCategory],
      };
      renderSidebar({ filters: filtersWithCategory });
      const checkboxes = screen.getAllByRole('checkbox');
      expect(checkboxes[0]).toBeChecked();
    });
  });

  describe('Event Type Filter', () => {
    it('should call onEventTypeToggle when event type checkbox clicked', async () => {
      renderSidebar();
      const checkboxes = screen.getAllByRole('checkbox');
      // Event type checkboxes come after category checkboxes (4)
      await userEvent.click(checkboxes[4]);
      expect(mockOnEventTypeToggle).toHaveBeenCalled();
    });

    it('should show event type as checked when in filters', () => {
      const filtersWithEventType: AuditFilters = {
        ...defaultFilters,
        eventTypes: ['created'],
      };
      renderSidebar({ filters: filtersWithEventType });
      const checkboxes = screen.getAllByRole('checkbox');
      expect(checkboxes[4]).toBeChecked();
    });
  });

  describe('Clear Filters', () => {
    it('should call onClearAllFilters when reset button clicked', async () => {
      renderSidebar();
      const resetButton = screen.getByRole('button', { name: /reset filters/i });
      await userEvent.click(resetButton);
      expect(mockOnClearAllFilters).toHaveBeenCalled();
    });
  });

  describe('Disabled State', () => {
    it('should disable all checkboxes when disabled prop is true', () => {
      renderSidebar({ disabled: true });
      const checkboxes = screen.getAllByRole('checkbox');
      checkboxes.forEach((checkbox) => {
        expect(checkbox).toBeDisabled();
      });
    });

    it('should disable reset button when disabled', () => {
      renderSidebar({ disabled: true });
      expect(screen.getByRole('button', { name: /reset filters/i })).toBeDisabled();
    });

    it('should NOT disable initiator input when disabled prop is true (prevents focus loss)', () => {
      // the initiator input should never be disabled even during loading to prevent focus loss when typing
      renderSidebar({ disabled: true });
      const initiatorInput = screen.getByPlaceholderText(/filter by initiator/i);
      expect(initiatorInput).not.toBeDisabled();
    });
  });

  describe('Initiator Filter', () => {
    it('should call onInitiatorChange when typing in initiator input', async () => {
      renderSidebar();
      const input = screen.getByPlaceholderText(/filter by initiator/i);
      await userEvent.type(input, 'admin');
      expect(mockOnInitiatorChange).toHaveBeenCalled();
    });

    it('should allow typing multiple characters without losing focus', async () => {
      renderSidebar();
      const input = screen.getByPlaceholderText(/filter by initiator/i);
      await userEvent.type(input, 'admin-user');
      expect(mockOnInitiatorChange).toHaveBeenCalledTimes('admin-user'.length);
      expect(input).toHaveFocus(); // directly assert focus is retained
    });

    it('should display initiator value from filters', () => {
      const filtersWithInitiator: AuditFilters = {
        ...defaultFilters,
        initiator: 'admin',
      };
      renderSidebar({ filters: filtersWithInitiator });
      const input = screen.getByPlaceholderText(/filter by initiator/i) as HTMLInputElement;
      expect(input.value).toBe('admin');
    });
  });
});
