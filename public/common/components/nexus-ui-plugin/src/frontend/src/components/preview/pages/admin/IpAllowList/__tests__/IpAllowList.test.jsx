/*
 * Copyright (c) 2008-present Sonatype, Inc.
 *
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/pro/attributions
 * Sonatype and Sonatype Nexus are trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Foundation.
 * M2Eclipse is a trademark of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import IpAllowList from '../IpAllowList';
import { IpAllowListApi } from '../IpAllowListApi';

// Mock the API module — tests control return values via the imported mock functions.
jest.mock('../IpAllowListApi', () => ({
  IpAllowListApi: {
    getSettings: jest.fn(),
    getEntries: jest.fn(),
    getCurrentIp: jest.fn(),
    updateMode: jest.fn(),
    bulkUploadCsv: jest.fn(),
    bulkAdd: jest.fn(),
    updateEntry: jest.fn(),
    bulkDelete: jest.fn(),
    addEntry: jest.fn(),
  },
  // Lightweight identity transform — maps "entry" (backend) to "ipAddress" (UI).
  transformEntryToUI: jest.fn((item) => ({
    id: item.id,
    ipAddress: item.entry || item.ipAddress || '',
    entryType: item.entryType || 'IPV4',
    description: item.description || '',
    createdBy: item.createdBy || '',
    createdAt: item.createdAt || null,
    updatedAt: item.updatedAt || null,
    lastUpdated: item.updatedAt || item.createdAt || null,
  })),
}));

// Provide a minimal PageHeader so we don't pull in the full shared component graph.
jest.mock('../../../../shared', () => ({
  PageHeader: ({ title, actions }) => (
    <div>
      <h1>{title}</h1>
      <div>{actions}</div>
    </div>
  ),
}));

const EMPTY_SETTINGS = { mode: 'DISABLED', totalEntries: 0, maxEntries: 256, ipv4AddressesCovered: 0, ipv6AddressesCovered: 0 };
const EMPTY_ENTRIES = { entries: [], page: 0, pageSize: 20, totalEntries: 0, totalPages: 0 };
const SAMPLE_ENTRIES = {
  entries: [
    { id: '1', entry: '192.168.1.1', entryType: 'IPV4', createdBy: 'admin', createdAt: '2024-01-01T00:00:00Z' },
    { id: '2', entry: '10.0.0.0/24', entryType: 'CIDR_IPV4', createdBy: 'admin', createdAt: '2024-01-02T00:00:00Z' },
  ],
  page: 0,
  pageSize: 20,
  totalEntries: 2,
  totalPages: 1,
};

const renderPage = () =>
  render(
    <Theme>
      <IpAllowList />
    </Theme>
  );

const switchToIpAllowListTab = async () => {
  await waitFor(() => {
    expect(screen.getByRole('tab', { name: /IP Allow List/i })).toBeInTheDocument();
  });
  await userEvent.click(screen.getByRole('tab', { name: /IP Allow List/i }));
};

describe('IpAllowList', () => {
  beforeEach(() => {
    IpAllowListApi.getSettings.mockResolvedValue(EMPTY_SETTINGS);
    IpAllowListApi.getEntries.mockResolvedValue(EMPTY_ENTRIES);
    IpAllowListApi.getCurrentIp.mockResolvedValue({ ip: '1.2.3.4', allowed: false });
  });

  describe('page title', () => {
    it('renders the page heading', async () => {
      renderPage();
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: 'IP Allow List' })).toBeInTheDocument();
      });
    });
  });

  describe('loading state', () => {
    it('hides the search input while the initial load is in progress', () => {
      // Return a promise that never resolves so the component stays in loading state.
      IpAllowListApi.getSettings.mockReturnValue(new Promise(() => {}));
      IpAllowListApi.getEntries.mockReturnValue(new Promise(() => {}));

      renderPage();

      expect(screen.queryByTestId('search-input')).not.toBeInTheDocument();
      expect(screen.queryByTestId('add-ip-button')).not.toBeInTheDocument();
    });
  });

  describe('tabs', () => {
    it('renders Overview and IP Allow List tabs', async () => {
      renderPage();
      await waitFor(() => {
        expect(screen.getByRole('tab', { name: /Overview/i })).toBeInTheDocument();
        expect(screen.getByRole('tab', { name: /IP Allow List/i })).toBeInTheDocument();
      });
    });

    it('shows Overview tab as active by default', async () => {
      renderPage();
      await waitFor(() => {
        expect(screen.getByRole('tab', { name: /Overview/i })).toHaveAttribute('data-state', 'active');
      });
    });

    it('switches to IP Allow List tab on click', async () => {
      renderPage();
      await switchToIpAllowListTab();
      await waitFor(() => {
        expect(screen.getByRole('tab', { name: /IP Allow List/i })).toHaveAttribute('data-state', 'active');
      });
    });

    it('shows mode card in Overview tab', async () => {
      renderPage();
      await waitFor(() => {
        expect(screen.getByText('No IP filtering is applied.')).toBeInTheDocument();
        expect(screen.getByText('Log requests without blocking.')).toBeInTheDocument();
        expect(screen.getByText('Block requests from unlisted IPs.')).toBeInTheDocument();
      });
    });
  });

  describe('stat cards', () => {
    it('shows stat cards with zeroes when list is empty', async () => {
      renderPage();
      await switchToIpAllowListTab();
      await waitFor(() => {
        expect(screen.getByTestId('stat-card-total')).toHaveTextContent('0');
        expect(screen.getByTestId('stat-card-ipv4')).toHaveTextContent('0');
        expect(screen.getByTestId('stat-card-ipv6')).toHaveTextContent('0');
      });
    });

    it('shows correct counts from API settings', async () => {
      IpAllowListApi.getSettings.mockResolvedValue({
        mode: 'ENFORCE',
        totalEntries: 50032,
        maxEntries: 256,
        ipv4AddressesCovered: 12174,
        ipv6AddressesCovered: 18632,
      });
      IpAllowListApi.getEntries.mockResolvedValue(SAMPLE_ENTRIES);

      renderPage();
      await switchToIpAllowListTab();

      await waitFor(() => {
        expect(screen.getByTestId('stat-card-total')).toHaveTextContent('30,806'); // 12174 + 18632
        expect(screen.getByTestId('stat-card-ipv4')).toHaveTextContent('12,174');
        expect(screen.getByTestId('stat-card-ipv6')).toHaveTextContent('18,632');
      });
    });
  });

  describe('empty state', () => {
    it('shows the EmptyState when there are no entries after loading', async () => {
      renderPage();
      await switchToIpAllowListTab();
      await waitFor(() => {
        expect(screen.getByTestId('empty-state-add-entry')).toBeInTheDocument();
      });
    });

    it('shows an Import Entries action in the empty state', async () => {
      renderPage();
      await switchToIpAllowListTab();
      await waitFor(() => {
        expect(screen.getByTestId('empty-state-import')).toBeInTheDocument();
      });
    });

    it('does not show the search toolbar in the empty state', async () => {
      renderPage();
      await switchToIpAllowListTab();
      await waitFor(() => {
        expect(screen.queryByTestId('search-input')).not.toBeInTheDocument();
      });
    });
  });

  describe('table with entries', () => {
    beforeEach(() => {
      IpAllowListApi.getSettings.mockResolvedValue({
        mode: 'ENFORCE',
        totalEntries: 2,
        maxEntries: 256,
        ipv4AddressesCovered: 257,
        ipv6AddressesCovered: 0,
      });
      IpAllowListApi.getEntries.mockResolvedValue(SAMPLE_ENTRIES);
    });

    it('shows the search toolbar when entries are present', async () => {
      renderPage();
      await switchToIpAllowListTab();
      await waitFor(() => {
        expect(screen.getByTestId('search-input')).toBeInTheDocument();
      });
    });

    it('renders rows with IP addresses', async () => {
      renderPage();
      await switchToIpAllowListTab();
      await waitFor(() => {
        expect(screen.getByText('192.168.1.1')).toBeInTheDocument();
        expect(screen.getByText('10.0.0.0/24')).toBeInTheDocument();
      });
    });

    it('shows Add Entry and Import Entries toolbar buttons', async () => {
      renderPage();
      await switchToIpAllowListTab();
      await waitFor(() => {
        expect(screen.getByTestId('add-ip-button')).toBeInTheDocument();
        expect(screen.getByTestId('bulk-import-button')).toBeInTheDocument();
      });
    });
  });

  describe('error state', () => {
    it('shows an error message when the API call fails', async () => {
      IpAllowListApi.getSettings.mockRejectedValue(new Error('Network error'));

      renderPage();

      await waitFor(() => {
        expect(screen.getByText('Network error')).toBeInTheDocument();
      });
    });

    it('shows a Retry button in the error state', async () => {
      IpAllowListApi.getSettings.mockRejectedValue(new Error('Failed'));

      renderPage();

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument();
      });
    });

    it('retries the load when the Retry button is clicked', async () => {
      IpAllowListApi.getSettings
        .mockRejectedValueOnce(new Error('Temporary failure'))
        .mockResolvedValue(EMPTY_SETTINGS);

      renderPage();

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument();
      });

      fireEvent.click(screen.getByRole('button', { name: 'Retry' }));

      await waitFor(() => {
        expect(IpAllowListApi.getSettings).toHaveBeenCalledTimes(2);
      });
    });
  });

  describe('mode badge', () => {
    it('shows Disabled badge when mode is DISABLED', async () => {
      IpAllowListApi.getSettings.mockResolvedValue({ ...EMPTY_SETTINGS, mode: 'DISABLED' });
      renderPage();
      await waitFor(() => {
        expect(screen.getByTestId('mode-badge')).toHaveTextContent('Disabled');
      });
    });

    it('shows Monitor badge when mode is MONITOR', async () => {
      IpAllowListApi.getSettings.mockResolvedValue({ ...EMPTY_SETTINGS, mode: 'MONITOR' });
      renderPage();
      await waitFor(() => {
        expect(screen.getByTestId('mode-badge')).toHaveTextContent('Monitor');
      });
    });

    it('shows Enforce badge when mode is ENFORCE', async () => {
      IpAllowListApi.getSettings.mockResolvedValue({ ...EMPTY_SETTINGS, mode: 'ENFORCE' });
      renderPage();
      await waitFor(() => {
        expect(screen.getByTestId('mode-badge')).toHaveTextContent('Enforce');
      });
    });
  });

  describe('Add Entry modal', () => {
    it('opens AddIpModal when Add Entry button is clicked', async () => {
      IpAllowListApi.getSettings.mockResolvedValue({ mode: 'ENFORCE', totalEntries: 1, maxEntries: 256, ipv4AddressesCovered: 1, ipv6AddressesCovered: 0 });
      IpAllowListApi.getEntries.mockResolvedValue({
        entries: [{ id: '1', entry: '1.1.1.1', entryType: 'IPV4' }],
        page: 0,
        pageSize: 20,
        totalEntries: 1,
        totalPages: 1,
      });

      renderPage();
      await switchToIpAllowListTab();

      await waitFor(() => {
        expect(screen.getByTestId('add-ip-button')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByTestId('add-ip-button'));

      await waitFor(() => {
        expect(screen.getByTestId('add-ip-submit-button')).toBeInTheDocument();
      });
    });

    it('opens AddIpModal via empty-state Add Entry button', async () => {
      renderPage();
      await switchToIpAllowListTab();

      await waitFor(() => {
        expect(screen.getByTestId('empty-state-add-entry')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByTestId('empty-state-add-entry'));

      await waitFor(() => {
        expect(screen.getByTestId('add-ip-submit-button')).toBeInTheDocument();
      });
    });
  });

  describe('search', () => {
    beforeEach(() => {
      IpAllowListApi.getSettings.mockResolvedValue({ mode: 'ENFORCE', totalEntries: 3, maxEntries: 256, ipv4AddressesCovered: 3, ipv6AddressesCovered: 0 });
      IpAllowListApi.getEntries.mockResolvedValue({
        entries: [
          { id: '1', entry: '192.168.1.1', entryType: 'IPV4' },
          { id: '2', entry: '10.0.0.1', entryType: 'IPV4' },
          { id: '3', entry: '172.16.0.1', entryType: 'IPV4' },
        ],
        page: 0,
        pageSize: 20,
        totalEntries: 3,
        totalPages: 1,
      });
    });

    it('search input is present after loading with entries', async () => {
      renderPage();
      await switchToIpAllowListTab();
      await waitFor(() => {
        expect(screen.getByTestId('search-input')).toBeInTheDocument();
      });
    });

    it('calls getEntries with search param after debounce', async () => {
      jest.useFakeTimers();
      IpAllowListApi.getEntries.mockResolvedValue({ entries: [], totalEntries: 0, totalPages: 0 });

      renderPage();
      await switchToIpAllowListTab();

      await waitFor(() => {
        expect(screen.getByTestId('search-input')).toBeInTheDocument();
      });

      const searchInput = screen.getByTestId('search-input');
      fireEvent.change(searchInput, { target: { value: '192.168' } });

      // Advance past the 300ms debounce
      act(() => {
        jest.advanceTimersByTime(350);
      });

      await waitFor(() => {
        expect(IpAllowListApi.getEntries).toHaveBeenCalledWith(
          expect.any(Number),
          expect.any(Number),
          '192.168'
        );
      });

      jest.useRealTimers();
    });
  });
});
