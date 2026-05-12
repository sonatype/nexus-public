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
import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import { CapabilityTypeSelector } from '../CapabilityTypeSelector';
import { CapabilityType } from '../types';

const mockFetchCapabilityTypes = jest.fn();
const mockFetchCapabilities = jest.fn();

jest.mock('../useCapabilitiesApi', () => ({
  useCapabilitiesApi: () => ({
    fetchCapabilityTypes: mockFetchCapabilityTypes,
    fetchCapabilities: mockFetchCapabilities,
  }),
}));

const renderWithTheme = (component: React.ReactElement) => {
  return render(<Theme>{component}</Theme>);
};

const mockCapabilityTypes: CapabilityType[] = [
  {
    id: 'outreach',
    name: 'Outreach: Management',
    about: 'Enables outreach features for repositories',
    formFields: [],
  },
  {
    id: 'healthcheck',
    name: 'Health Check',
    about: 'Performs health checks on components',
    formFields: [],
  },
  {
    id: 'logging',
    name: 'Log4j Logging',
    about: undefined,
    formFields: [],
  },
];

describe('CapabilityTypeSelector', () => {
  const mockOnSelect = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockFetchCapabilityTypes.mockResolvedValue(mockCapabilityTypes);
    mockFetchCapabilities.mockResolvedValue([]);
  });

  describe('rendering', () => {
    it('renders without crashing', async () => {
      const { container } = renderWithTheme(<CapabilityTypeSelector onSelect={mockOnSelect} />);

      await waitFor(() => {
        expect(container).toBeInTheDocument();
      });
    });

    it('fetches capability types on mount', async () => {
      renderWithTheme(<CapabilityTypeSelector onSelect={mockOnSelect} />);

      await waitFor(() => {
        expect(mockFetchCapabilityTypes).toHaveBeenCalled();
      });
    });

    it('displays capability type names', async () => {
      renderWithTheme(<CapabilityTypeSelector onSelect={mockOnSelect} />);

      await waitFor(() => {
        expect(screen.getByText('Health Check')).toBeInTheDocument();
      });

      expect(screen.getByText('Outreach: Management')).toBeInTheDocument();
    });

    it('displays types sorted alphabetically', async () => {
      renderWithTheme(<CapabilityTypeSelector onSelect={mockOnSelect} />);

      await waitFor(() => {
        const cards = screen.getAllByText(/Health Check|Log4j Logging|Outreach/);
        expect(cards[0]).toHaveTextContent('Health Check');
        expect(cards[1]).toHaveTextContent('Log4j Logging');
        expect(cards[2]).toHaveTextContent('Outreach: Management');
      });
    });

    it('displays about text when available', async () => {
      renderWithTheme(<CapabilityTypeSelector onSelect={mockOnSelect} />);

      await waitFor(() => {
        expect(screen.getByText('Enables outreach features for repositories')).toBeInTheDocument();
      });
    });

    it('handles types without about text', async () => {
      renderWithTheme(<CapabilityTypeSelector onSelect={mockOnSelect} />);

      await waitFor(() => {
        expect(screen.getByText('Log4j Logging')).toBeInTheDocument();
      });
    });

    it('shows type count with available ratio', async () => {
      renderWithTheme(<CapabilityTypeSelector onSelect={mockOnSelect} />);

      await waitFor(() => {
        expect(screen.getByTestId('type-selector-count')).toHaveTextContent('3 of 3 types available');
      });
    });
  });

  describe('loading state', () => {
    it('shows loading indicator while fetching', async () => {
      // Create a promise that doesn't resolve immediately
      let resolvePromise: (value: CapabilityType[]) => void;
      const pendingPromise = new Promise<CapabilityType[]>((resolve) => {
        resolvePromise = resolve;
      });
      mockFetchCapabilityTypes.mockReturnValue(pendingPromise);

      renderWithTheme(<CapabilityTypeSelector onSelect={mockOnSelect} />);

      // Should show loading state
      expect(screen.getByText('Loading capability types...')).toBeInTheDocument();

      // Resolve the promise
      await act(async () => {
        resolvePromise!(mockCapabilityTypes);
      });

      // Loading should be gone
      await waitFor(() => {
        expect(screen.queryByText('Loading capability types...')).not.toBeInTheDocument();
      });
    });
  });

  describe('error state', () => {
    it('shows error message when fetch fails', async () => {
      mockFetchCapabilityTypes.mockRejectedValue(new Error('Network error'));

      renderWithTheme(<CapabilityTypeSelector onSelect={mockOnSelect} />);

      await waitFor(() => {
        expect(screen.getByText('Network error')).toBeInTheDocument();
      });
    });

    it('shows generic error for non-Error objects', async () => {
      mockFetchCapabilityTypes.mockRejectedValue('Unknown error');

      renderWithTheme(<CapabilityTypeSelector onSelect={mockOnSelect} />);

      await waitFor(() => {
        expect(screen.getByText('Failed to load capability types')).toBeInTheDocument();
      });
    });
  });

  describe('empty state', () => {
    it('shows empty state when no types available', async () => {
      mockFetchCapabilityTypes.mockResolvedValue([]);

      renderWithTheme(<CapabilityTypeSelector onSelect={mockOnSelect} />);

      await waitFor(() => {
        expect(screen.getByText(/no capability types match/i)).toBeInTheDocument();
      });
    });

    it('shows 0 types available when empty', async () => {
      mockFetchCapabilityTypes.mockResolvedValue([]);

      renderWithTheme(<CapabilityTypeSelector onSelect={mockOnSelect} />);

      await waitFor(() => {
        expect(screen.getByTestId('type-selector-count')).toHaveTextContent('0 of 0 types available');
      });
    });
  });

  describe('search/filter', () => {
    it('renders search input', async () => {
      renderWithTheme(<CapabilityTypeSelector onSelect={mockOnSelect} />);

      await waitFor(() => {
        expect(screen.getByPlaceholderText('Filter capability types...')).toBeInTheDocument();
      });
    });

    it('filters types by name', async () => {
      renderWithTheme(<CapabilityTypeSelector onSelect={mockOnSelect} />);

      await waitFor(() => {
        expect(screen.getByText('Health Check')).toBeInTheDocument();
      });

      const searchInput = screen.getByPlaceholderText('Filter capability types...');
      await userEvent.type(searchInput, 'health');

      await waitFor(() => {
        expect(screen.getByText('Health Check')).toBeInTheDocument();
        expect(screen.queryByText('Outreach: Management')).not.toBeInTheDocument();
      });
    });

    it('filters types by about text', async () => {
      renderWithTheme(<CapabilityTypeSelector onSelect={mockOnSelect} />);

      await waitFor(() => {
        expect(screen.getByText('Health Check')).toBeInTheDocument();
      });

      const searchInput = screen.getByPlaceholderText('Filter capability types...');
      await userEvent.type(searchInput, 'repositories');

      await waitFor(() => {
        expect(screen.getByText('Outreach: Management')).toBeInTheDocument();
        expect(screen.queryByText('Health Check')).not.toBeInTheDocument();
      });
    });

    it('is case insensitive', async () => {
      renderWithTheme(<CapabilityTypeSelector onSelect={mockOnSelect} />);

      await waitFor(() => {
        expect(screen.getByText('Health Check')).toBeInTheDocument();
      });

      const searchInput = screen.getByPlaceholderText('Filter capability types...');
      await userEvent.type(searchInput, 'HEALTH');

      await waitFor(() => {
        expect(screen.getByText('Health Check')).toBeInTheDocument();
      });
    });

    it('shows empty state when filter matches nothing', async () => {
      renderWithTheme(<CapabilityTypeSelector onSelect={mockOnSelect} />);

      await waitFor(() => {
        expect(screen.getByText('Health Check')).toBeInTheDocument();
      });

      const searchInput = screen.getByPlaceholderText('Filter capability types...');
      await userEvent.type(searchInput, 'nonexistent');

      await waitFor(() => {
        expect(screen.getByText(/no capability types match/i)).toBeInTheDocument();
      });
    });

    it('updates count when filtering', async () => {
      renderWithTheme(<CapabilityTypeSelector onSelect={mockOnSelect} />);

      await waitFor(() => {
        expect(screen.getByTestId('type-selector-count')).toHaveTextContent('3 of 3 types available');
      });

      const searchInput = screen.getByPlaceholderText('Filter capability types...');
      await userEvent.type(searchInput, 'health');

      await waitFor(() => {
        expect(screen.getByTestId('type-selector-count')).toHaveTextContent('1 of 1 types available');
      });
    });

    it('shows all types when search is cleared', async () => {
      renderWithTheme(<CapabilityTypeSelector onSelect={mockOnSelect} />);

      await waitFor(() => {
        expect(screen.getByText('Health Check')).toBeInTheDocument();
      });

      const searchInput = screen.getByPlaceholderText('Filter capability types...');
      await userEvent.type(searchInput, 'health');

      await waitFor(() => {
        expect(screen.queryByText('Outreach: Management')).not.toBeInTheDocument();
      });

      await userEvent.clear(searchInput);

      await waitFor(() => {
        expect(screen.getByText('Outreach: Management')).toBeInTheDocument();
        expect(screen.getByText('Health Check')).toBeInTheDocument();
      });
    });
  });

  describe('selection', () => {
    it('calls onSelect when a type card is clicked', async () => {
      renderWithTheme(<CapabilityTypeSelector onSelect={mockOnSelect} />);

      await waitFor(() => {
        expect(screen.getByText('Health Check')).toBeInTheDocument();
      });

      const healthCheckCard = screen.getByText('Health Check').closest('.capability-type-selector__card');
      await userEvent.click(healthCheckCard!);

      expect(mockOnSelect).toHaveBeenCalledWith(
        expect.objectContaining({
          id: 'healthcheck',
          name: 'Health Check',
        })
      );
    });

    it('passes full type object to onSelect', async () => {
      renderWithTheme(<CapabilityTypeSelector onSelect={mockOnSelect} />);

      await waitFor(() => {
        expect(screen.getByText('Health Check')).toBeInTheDocument();
      });

      const healthCheckCard = screen.getByText('Health Check').closest('.capability-type-selector__card');
      await userEvent.click(healthCheckCard!);

      expect(mockOnSelect).toHaveBeenCalledWith({
        id: 'healthcheck',
        name: 'Health Check',
        about: 'Performs health checks on components',
        formFields: [],
      });
    });
  });

  describe('singleton detection (bug a9vq)', () => {
    const typesWithWebhook: CapabilityType[] = [
      ...mockCapabilityTypes,
      { id: 'webhook.global', name: 'Webhook: Global', about: 'Global webhook', formFields: [] },
    ];

    const existingCapabilities = [
      { id: 'cap-1', typeId: 'outreach', typeName: 'Outreach', enabled: true, active: true, error: false, state: 'active' as const, properties: {} },
      { id: 'cap-2', typeId: 'webhook.global', typeName: 'Webhook', enabled: true, active: true, error: false, state: 'active' as const, properties: {} },
    ];

    it('shows "Already configured" badge on existing singleton types', async () => {
      mockFetchCapabilityTypes.mockResolvedValue(typesWithWebhook);
      mockFetchCapabilities.mockResolvedValue(existingCapabilities);

      renderWithTheme(<CapabilityTypeSelector onSelect={mockOnSelect} />);

      await waitFor(() => {
        expect(screen.getByText('Health Check')).toBeInTheDocument();
      });

      const outreachCard = screen.getByTestId('type-card-outreach');
      expect(outreachCard).toHaveClass('capability-type-selector__card--disabled');
      expect(outreachCard).toHaveTextContent('Already configured');
    });

    it('does not call onSelect for disabled singleton types', async () => {
      mockFetchCapabilityTypes.mockResolvedValue(typesWithWebhook);
      mockFetchCapabilities.mockResolvedValue(existingCapabilities);

      renderWithTheme(<CapabilityTypeSelector onSelect={mockOnSelect} />);

      await waitFor(() => {
        expect(screen.getByTestId('type-card-outreach')).toBeInTheDocument();
      });

      await userEvent.click(screen.getByTestId('type-card-outreach'));
      expect(mockOnSelect).not.toHaveBeenCalled();
    });

    it('keeps webhook types clickable even when instances exist (multi-instance)', async () => {
      mockFetchCapabilityTypes.mockResolvedValue(typesWithWebhook);
      mockFetchCapabilities.mockResolvedValue(existingCapabilities);

      renderWithTheme(<CapabilityTypeSelector onSelect={mockOnSelect} />);

      await waitFor(() => {
        expect(screen.getByTestId('type-card-webhook.global')).toBeInTheDocument();
      });

      const webhookCard = screen.getByTestId('type-card-webhook.global');
      expect(webhookCard).not.toHaveClass('capability-type-selector__card--disabled');

      await userEvent.click(webhookCard);
      expect(mockOnSelect).toHaveBeenCalledWith(
        expect.objectContaining({ id: 'webhook.global' })
      );
    });

    it('shows correct available count excluding singletons', async () => {
      mockFetchCapabilityTypes.mockResolvedValue(typesWithWebhook);
      mockFetchCapabilities.mockResolvedValue(existingCapabilities);

      renderWithTheme(<CapabilityTypeSelector onSelect={mockOnSelect} />);

      await waitFor(() => {
        expect(screen.getByTestId('type-selector-count')).toHaveTextContent('3 of 4 types available');
      });
    });

    it('sorts available types before disabled types', async () => {
      mockFetchCapabilityTypes.mockResolvedValue(typesWithWebhook);
      mockFetchCapabilities.mockResolvedValue(existingCapabilities);

      renderWithTheme(<CapabilityTypeSelector onSelect={mockOnSelect} />);

      await waitFor(() => {
        const cards = screen.getAllByTestId(/^type-card-/);
        expect(cards[cards.length - 1]).toHaveAttribute('data-testid', 'type-card-outreach');
      });
    });
  });
});
