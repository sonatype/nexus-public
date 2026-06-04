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
import { render, screen, waitFor, within, fireEvent } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { CapabilitiesList } from '../CapabilitiesList';
import { Capability } from '../types';

const mockFetchCapabilities = jest.fn();
const mockFetchCapabilityTypes = jest.fn();

jest.mock('../useCapabilitiesApi', () => ({
  useCapabilitiesApi: () => ({
    loading: false,
    error: null,
    fetchCapabilities: mockFetchCapabilities,
    fetchCapabilityTypes: mockFetchCapabilityTypes,
    enableCapability: jest.fn(),
    disableCapability: jest.fn(),
  }),
}));

jest.mock('../../../../../shared/icons/action-icons', () => ({
  ActionIcons: {
    Cancel: () => <span data-testid="close-icon">X</span>,
    Search: () => <span data-testid="search-icon">S</span>,
    Puzzle: () => <span data-testid="puzzle-icon">P</span>,
  },
}));

const renderWithTheme = (component: React.ReactElement) => {
  return render(<Theme>{component}</Theme>);
};

const mockCapabilityTypes = [
  { id: 'outreach', name: 'Outreach: Management', about: '', formFields: [] },
  { id: 'healthcheck', name: 'Health Check: Configuration', about: '', formFields: [] },
  { id: 'audit', name: 'Audit', about: '', formFields: [] },
];

const mockCapabilities: Capability[] = [
  {
    id: 'cap-1',
    typeId: 'outreach',
    typeName: 'Outreach: Management', // REST API returns human-readable name
    enabled: true,
    active: true,
    error: false,
    state: 'active',
    description: 'Enabled for all repos',
    stateDescription: 'Active',
    notes: 'Automatically added on Jan 02 2026',
    properties: {},
  },
  {
    id: 'cap-2',
    typeId: 'healthcheck',
    typeName: 'Health Check: Configuration', // REST API returns human-readable name
    enabled: false,
    active: false,
    error: false,
    state: 'disabled',
    stateDescription: 'Disabled by user',
    notes: '',
    properties: {},
  },
  {
    id: 'cap-3',
    typeId: 'audit',
    typeName: 'Audit', // REST API returns human-readable name
    enabled: true,
    active: true,
    error: false,
    state: 'active',
    stateDescription: 'Active',
    properties: {},
  },
];

describe('CapabilitiesList', () => {
  const mockOnSelect = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockFetchCapabilities.mockResolvedValue(mockCapabilities);
    mockFetchCapabilityTypes.mockResolvedValue(mockCapabilityTypes);
  });

  it('renders the capabilities list', async () => {
    renderWithTheme(<CapabilitiesList onSelect={mockOnSelect} refreshKey={0} />);

    await waitFor(() => {
      expect(screen.getByText('Outreach: Management')).toBeInTheDocument();
    });
  });

  it('shows empty state when no capabilities exist', async () => {
    mockFetchCapabilities.mockResolvedValue([]);

    renderWithTheme(<CapabilitiesList onSelect={mockOnSelect} refreshKey={0} />);

    await waitFor(() => {
      expect(screen.getByText(/no capabilities/i)).toBeInTheDocument();
    });
  }, 10000);

  it('calls fetchCapabilities on mount', async () => {
    renderWithTheme(<CapabilitiesList onSelect={mockOnSelect} refreshKey={0} />);

    await waitFor(() => {
      expect(mockFetchCapabilities).toHaveBeenCalled();
    });
  });

  describe('enriched columns (bug o7gp)', () => {
    it('shows type name from types API instead of raw typeId', async () => {
      renderWithTheme(<CapabilitiesList onSelect={mockOnSelect} refreshKey={0} />);

      await waitFor(() => {
        expect(screen.getByText('Outreach: Management')).toBeInTheDocument();
        expect(screen.getByText('Health Check: Configuration')).toBeInTheDocument();
      });
    });

    it('renders Category column with correct category badges', async () => {
      renderWithTheme(<CapabilitiesList onSelect={mockOnSelect} refreshKey={0} />);

      await waitFor(() => {
        expect(screen.getByText('Outreach: Management')).toBeInTheDocument();
      });

      // Category badges should be present (appears in both filter sidebar and table)
      const coreBadges = screen.getAllByText('Core');
      expect(coreBadges.length).toBeGreaterThan(0);

      // Health Check is a unique category
      const healthCheckBadges = screen.getAllByText('Health Check');
      expect(healthCheckBadges.length).toBeGreaterThan(0);
    });

    it('shows Description column with capability.description from REST API', async () => {
      renderWithTheme(<CapabilitiesList onSelect={mockOnSelect} refreshKey={0} />);

      await waitFor(() => {
        expect(screen.getByText('Outreach: Management')).toBeInTheDocument();
      });

      // Description column shows capability.description (or stateDescription fallback)
      expect(screen.getByRole('columnheader', { name: /description/i })).toBeInTheDocument();
      // cap-1 has explicit description
      expect(screen.getByText('Enabled for all repos')).toBeInTheDocument();
      // cap-2 has stateDescription fallback
      expect(screen.getByText('Disabled by user')).toBeInTheDocument();
    });

    it('shows Notes column with capability.notes', async () => {
      renderWithTheme(<CapabilitiesList onSelect={mockOnSelect} refreshKey={0} />);

      await waitFor(() => {
        expect(screen.getByText('Outreach: Management')).toBeInTheDocument();
      });

      expect(screen.getByRole('columnheader', { name: /notes/i })).toBeInTheDocument();
      expect(screen.getByText('Automatically added on Jan 02 2026')).toBeInTheDocument();
    });

    it('renders all capabilities', async () => {
      renderWithTheme(<CapabilitiesList onSelect={mockOnSelect} refreshKey={0} />);

      await waitFor(() => {
        // Check for each capability type name to be displayed
        // Some appear multiple times (in filter sidebar and in table)
        expect(screen.getByText('Outreach: Management')).toBeInTheDocument();
        expect(screen.getByText('Health Check: Configuration')).toBeInTheDocument();
        const auditElements = screen.getAllByText('Audit');
        expect(auditElements.length).toBeGreaterThan(0);
      });
    });

    it('searches across type name, description, notes, and category', async () => {
      renderWithTheme(<CapabilitiesList onSelect={mockOnSelect} refreshKey={0} />);

      await waitFor(() => {
        expect(screen.getByText('Outreach: Management')).toBeInTheDocument();
      });

      const searchInput = screen.getByPlaceholderText('Search capabilities...');
      fireEvent.change(searchInput, { target: { value: 'Jan 02' } });

      await waitFor(() => {
        expect(screen.getByText('Outreach: Management')).toBeInTheDocument();
        expect(screen.queryByText('Health Check: Configuration')).not.toBeInTheDocument();
      });
    });
  });
});
