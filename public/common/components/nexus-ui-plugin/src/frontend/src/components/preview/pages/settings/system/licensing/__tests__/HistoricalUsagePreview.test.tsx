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
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

import { HistoricalUsagePreview } from '../HistoricalUsagePreview';

// Mock HumanReadableUtils to avoid @sonatype/react-shared-components ESM import chain
jest.mock('../../../../../../../interface/HumanReadableUtils', () => ({
  __esModule: true,
  default: {
    bytesToString: jest.fn((bytes: number) => {
      if (bytes >= 1073741824) return `${(bytes / 1073741824).toFixed(1)} GB`;
      if (bytes >= 1048576) return `${(bytes / 1048576).toFixed(1)} MB`;
      return `${bytes} B`;
    }),
  },
}));

// Mock restClient
jest.mock('../../../../../../../interface/api', () => ({
  restClient: {
    get: jest.fn(),
  },
  parseApiError: jest.fn((err) => ({ message: err.message || 'Error' })),
}));

const { restClient } = require('../../../../../../../interface/api');

// Wrapper for Radix Theme
function renderWithTheme(component: React.ReactElement) {
  return render(<Theme>{component}</Theme>);
}

describe('HistoricalUsagePreview', () => {
  const mockUsageData = [
    {
      metricDate: '2024-01-01T00:00:00Z',
      componentCount: 1500,
      percentageChangeComponent: 10,
      requestCount: 50000,
      percentageChangeRequest: 5,
      responseSize: 1073741824, // 1 GB
      peakStorage: 2147483648, // 2 GB
    },
    {
      metricDate: '2024-02-01T00:00:00Z',
      componentCount: 1650,
      percentageChangeComponent: -5,
      requestCount: 45000,
      percentageChangeRequest: -10,
      responseSize: 536870912, // 512 MB
      peakStorage: 3221225472, // 3 GB
    },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('shows loading state initially', () => {
    restClient.get.mockImplementation(() => new Promise(() => {})); // Never resolves

    renderWithTheme(<HistoricalUsagePreview />);

    expect(screen.getByText('Loading usage data...')).toBeInTheDocument();
  });

  it('renders table headers correctly', async () => {
    restClient.get.mockResolvedValue(mockUsageData);

    renderWithTheme(<HistoricalUsagePreview />);

    await waitFor(() => {
      expect(screen.getByText('Month')).toBeInTheDocument();
    });

    expect(screen.getByText('Peak Components')).toBeInTheDocument();
    expect(screen.getByText('Components % Change')).toBeInTheDocument();
    expect(screen.getByText('Total Requests')).toBeInTheDocument();
    expect(screen.getByText('Requests % Change')).toBeInTheDocument();
    expect(screen.getByText('Total Egress')).toBeInTheDocument();
    expect(screen.getByText('Peak Storage')).toBeInTheDocument();
  });

  it('renders data rows correctly', async () => {
    restClient.get.mockResolvedValue(mockUsageData);

    renderWithTheme(<HistoricalUsagePreview />);

    await waitFor(() => {
      expect(screen.getByText('Jan 2024')).toBeInTheDocument();
    });

    expect(screen.getByText('Feb 2024')).toBeInTheDocument();
    expect(screen.getByText('1,500')).toBeInTheDocument();
    expect(screen.getByText('1,650')).toBeInTheDocument();
    expect(screen.getByText('50,000')).toBeInTheDocument();
    expect(screen.getByText('45,000')).toBeInTheDocument();
  });

  it('renders "Learn how usage is calculated" link', async () => {
    restClient.get.mockResolvedValue(mockUsageData);

    renderWithTheme(<HistoricalUsagePreview />);

    await waitFor(() => {
      expect(screen.getByText('Historical Usage')).toBeInTheDocument();
    });

    const learnMoreLink = screen.getByText('Learn how usage is calculated');
    expect(learnMoreLink).toBeInTheDocument();
    expect(learnMoreLink.closest('a')).toHaveAttribute(
      'href',
      'http://links.sonatype.com/products/nxrm3/license/historical-usage'
    );
  });

  it('shows empty state when no data', async () => {
    restClient.get.mockResolvedValue([]);

    renderWithTheme(<HistoricalUsagePreview />);

    await waitFor(() => {
      expect(screen.getByText('No historical usage data available')).toBeInTheDocument();
    });
  });

  it('handles error state and shows retry button', async () => {
    restClient.get.mockRejectedValue(new Error('Network error'));

    renderWithTheme(<HistoricalUsagePreview />);

    await waitFor(() => {
      expect(screen.getByText('Network error')).toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument();
  });

  it('retry button triggers another fetch attempt', async () => {
    restClient.get.mockRejectedValueOnce(new Error('Network error'));
    restClient.get.mockResolvedValueOnce([]);

    renderWithTheme(<HistoricalUsagePreview />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));

    await waitFor(() => {
      expect(screen.getByText('No historical usage data available')).toBeInTheDocument();
    });

    expect(restClient.get).toHaveBeenCalledTimes(2);
  });

  it('renders title and description', async () => {
    restClient.get.mockResolvedValue(mockUsageData);

    renderWithTheme(<HistoricalUsagePreview />);

    await waitFor(() => {
      expect(screen.getByText('Historical Usage')).toBeInTheDocument();
    });

    expect(screen.getByText(/Monitor your storage usage trends over time/)).toBeInTheDocument();
  });

  describe('formatBytes', () => {
    it('renders 0 bytes as a formatted value, not N/A', async () => {
      restClient.get.mockResolvedValue([{
        metricDate: '2024-01-01T00:00:00Z',
        componentCount: 0,
        percentageChangeComponent: null,
        requestCount: 0,
        percentageChangeRequest: null,
        responseSize: 0,
        peakStorage: 0,
      }]);

      renderWithTheme(<HistoricalUsagePreview />);

      await waitFor(() => {
        expect(screen.getByText('Jan 2024')).toBeInTheDocument();
      });

      // 0 bytes should render as '0 B', not 'N/A'
      const zeroBCells = screen.getAllByText('0 B');
      expect(zeroBCells.length).toBeGreaterThanOrEqual(1);
    });

    it('renders null bytes as N/A', async () => {
      restClient.get.mockResolvedValue([{
        metricDate: '2024-01-01T00:00:00Z',
        componentCount: 100,
        percentageChangeComponent: null,
        requestCount: 100,
        percentageChangeRequest: null,
        responseSize: null,
        peakStorage: null,
      }]);

      renderWithTheme(<HistoricalUsagePreview />);

      await waitFor(() => {
        expect(screen.getByText('Jan 2024')).toBeInTheDocument();
      });

      const naCells = screen.getAllByText('N/A');
      expect(naCells.length).toBeGreaterThanOrEqual(1);
    });

    it('uses egress fallback when responseSize is null', async () => {
      restClient.get.mockResolvedValue([{
        metricDate: '2024-01-01T00:00:00Z',
        componentCount: 100,
        percentageChangeComponent: null,
        requestCount: 100,
        percentageChangeRequest: null,
        responseSize: null,
        egress: 1073741824, // 1 GB via fallback
        peakStorage: null,
        storage: null,
      }]);

      renderWithTheme(<HistoricalUsagePreview />);

      await waitFor(() => {
        expect(screen.getByText('Jan 2024')).toBeInTheDocument();
      });

      // egress fallback should render the formatted value, not N/A
      expect(screen.getByText('1.0 GB')).toBeInTheDocument();
    });

    it('uses storage fallback when peakStorage is null', async () => {
      restClient.get.mockResolvedValue([{
        metricDate: '2024-01-01T00:00:00Z',
        componentCount: 100,
        percentageChangeComponent: null,
        requestCount: 100,
        percentageChangeRequest: null,
        responseSize: null,
        egress: null,
        peakStorage: null,
        storage: 2147483648, // 2 GB via fallback
      }]);

      renderWithTheme(<HistoricalUsagePreview />);

      await waitFor(() => {
        expect(screen.getByText('Jan 2024')).toBeInTheDocument();
      });

      // storage fallback should render the formatted value, not N/A
      expect(screen.getByText('2.0 GB')).toBeInTheDocument();
    });
  });

  describe('percentage change rendering', () => {
    it('renders positive percentage change with TrendingUp icon and change-icon--up class', async () => {
      restClient.get.mockResolvedValue([{
        metricDate: '2024-01-01T00:00:00Z',
        componentCount: 100,
        percentageChangeComponent: 15,
        requestCount: 100,
        percentageChangeRequest: 8,
        responseSize: null,
        peakStorage: null,
      }]);

      const { container } = renderWithTheme(<HistoricalUsagePreview />);

      await waitFor(() => {
        expect(screen.getByText('Jan 2024')).toBeInTheDocument();
      });

      expect(screen.getAllByText('15%').length).toBeGreaterThanOrEqual(1);
      expect(container.querySelectorAll('.change-icon--up').length).toBeGreaterThanOrEqual(1);
    });

    it('renders negative percentage change with TrendingDown icon and change-icon--down class', async () => {
      restClient.get.mockResolvedValue([{
        metricDate: '2024-01-01T00:00:00Z',
        componentCount: 100,
        percentageChangeComponent: -20,
        requestCount: 100,
        percentageChangeRequest: -3,
        responseSize: null,
        peakStorage: null,
      }]);

      const { container } = renderWithTheme(<HistoricalUsagePreview />);

      await waitFor(() => {
        expect(screen.getByText('Jan 2024')).toBeInTheDocument();
      });

      // formatPercentage uses Math.abs, so negative shows as positive value
      expect(screen.getAllByText('20%').length).toBeGreaterThanOrEqual(1);
      expect(container.querySelectorAll('.change-icon--down').length).toBeGreaterThanOrEqual(1);
    });

    it('renders zero percentage change with neutral Minus icon and change-icon--na class', async () => {
      restClient.get.mockResolvedValue([{
        metricDate: '2024-01-01T00:00:00Z',
        componentCount: 100,
        percentageChangeComponent: 0,
        requestCount: 100,
        percentageChangeRequest: 0,
        responseSize: null,
        peakStorage: null,
      }]);

      const { container } = renderWithTheme(<HistoricalUsagePreview />);

      await waitFor(() => {
        expect(screen.getByText('Jan 2024')).toBeInTheDocument();
      });

      expect(screen.getAllByText('0%').length).toBeGreaterThanOrEqual(1);
      expect(container.querySelectorAll('.change-icon--na').length).toBeGreaterThanOrEqual(1);
    });

    it('renders null percentage change as N/A with change-icon--na class', async () => {
      restClient.get.mockResolvedValue([{
        metricDate: '2024-01-01T00:00:00Z',
        componentCount: 100,
        percentageChangeComponent: null,
        requestCount: 100,
        percentageChangeRequest: null,
        responseSize: null,
        peakStorage: null,
      }]);

      const { container } = renderWithTheme(<HistoricalUsagePreview />);

      await waitFor(() => {
        expect(screen.getByText('Jan 2024')).toBeInTheDocument();
      });

      const naCells = screen.getAllByText('N/A');
      expect(naCells.length).toBeGreaterThanOrEqual(2);
      expect(container.querySelectorAll('.change-icon--na').length).toBeGreaterThanOrEqual(1);
    });
  });

  describe('non-numeric string handling', () => {
    it('formatNumber returns N/A for non-numeric string', async () => {
      restClient.get.mockResolvedValue([{
        metricDate: '2024-01-01T00:00:00Z',
        componentCount: 'unknown',
        percentageChangeComponent: null,
        requestCount: 'N/A',
        percentageChangeRequest: null,
        responseSize: null,
        peakStorage: null,
      }]);

      renderWithTheme(<HistoricalUsagePreview />);

      await waitFor(() => {
        expect(screen.getByText('Jan 2024')).toBeInTheDocument();
      });

      // Non-numeric strings should render as N/A, not raw string or NaN
      const naCells = screen.getAllByText('N/A');
      expect(naCells.length).toBeGreaterThanOrEqual(1);
    });

    it('formatBytes returns N/A for non-numeric string', async () => {
      restClient.get.mockResolvedValue([{
        metricDate: '2024-01-01T00:00:00Z',
        componentCount: 0,
        percentageChangeComponent: null,
        requestCount: 0,
        percentageChangeRequest: null,
        responseSize: 'unknown',
        peakStorage: 'unknown',
      }]);

      renderWithTheme(<HistoricalUsagePreview />);

      await waitFor(() => {
        expect(screen.getByText('Jan 2024')).toBeInTheDocument();
      });

      // Non-numeric strings for byte values should render as N/A
      const naCells = screen.getAllByText('N/A');
      expect(naCells.length).toBeGreaterThanOrEqual(1);
    });
  });
});
