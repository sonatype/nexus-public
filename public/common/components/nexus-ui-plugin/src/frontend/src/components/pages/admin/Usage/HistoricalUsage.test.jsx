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
import { render, screen, waitFor } from '@testing-library/react';
import Axios from 'axios';
import HistoricalUsage from './HistoricalUsage';
import TestUtils from '../../../../interface/TestUtils';
import { historicalUsageColumns } from './HistoricalUsageColumns';
import ExtJS from '../../../../interface/ExtJS';

jest.mock('./UsageInsightsChart', () => ({
  UsageInsightsChart: () => <div data-testid="usage-insights-chart">Usage Insights Chart</div>
}));

describe('Licensing Historical Usage', () => {
  const requiredColumns = [
    historicalUsageColumns.metricDateMonth,
    historicalUsageColumns.peakComponents,
    historicalUsageColumns.percentageChangeComponent,
    historicalUsageColumns.totalRequests,
    historicalUsageColumns.percentageChangeRequests,
    historicalUsageColumns.totalEgress,
    historicalUsageColumns.peakStorage
  ];

  async function renderView() {
    return render(<HistoricalUsage columns={requiredColumns} />);
  }

  it('renders the alert about data update frequency', async () => {
    jest.spyOn(ExtJS, 'useState').mockReturnValue(true);

    await renderView();

    expect(screen.getByText('Storage usage metrics may take up to 72 hours to update. Recent repository activity, such as publishing, downloading, or deleting components, may not appear immediately.')).toBeInTheDocument();
  });

  it('does not render the alert about data update frequency when state is false', async () => {
    jest.spyOn(ExtJS, 'useState').mockReturnValue(false);

    await renderView();

    expect(screen.queryByText('Storage usage metrics may take up to 72 hours to update. Recent repository activity, such as publishing, downloading, or deleting components, may not appear immediately.')).not.toBeInTheDocument();
  });

  it('renders the title and description', async () => {
    await renderView();

    expect(screen.getByRole('heading', { name: 'Historical Usage' })).toBeInTheDocument();
    expect(screen.getByText('Monitor your repository usage trends over time.')).toBeInTheDocument();
  });

  it('renders the table headers correctly', async () => {
    await renderView();

    expect(screen.getByRole('columnheader', { name: 'Month' })).toBeInTheDocument();
    expect(screen.getByText('Peak Components')).toBeInTheDocument();
    expect(screen.getByText('Components % Change')).toBeInTheDocument();
    expect(screen.getByText('Total Requests')).toBeInTheDocument();
    expect(screen.getByText('Requests % Change')).toBeInTheDocument();
    expect(screen.getByText('Peak Storage')).toBeInTheDocument();
    expect(screen.getByText('Total Egress')).toBeInTheDocument();
  });

  // Note: Data rows test removed (NEXUS-48660) - XState machine mocking doesn't work correctly
  // The mock data doesn't flow through the state machine. Usage data is tested via E2E tests.

  it('renders change icons correctly', async () => {
    const mockData = [
      {
        metricDate: '2024-11-01T00:00:00.000',
        componentCount: 1000,
        percentageChangeComponent: 10,
        requestCount: 2000,
        percentageChangeRequest: -5,
        peakStorage: 1073741824,
        responseSize: 536870912
      }
    ];

    jest.spyOn(Axios, 'get').mockResolvedValue({ data: mockData });

    const { container } = await renderView();

    const icon = container.querySelector('[data-icon="info-circle"]');

    await TestUtils.expectToSeeTooltipOnHover(icon, 'Change rate of the peak component count from the previous month.');
  });

  it('renders N/A for unavailable data', async () => {
    const mockData = [
      {
        metricDate: '2024-11-01T00:00:00.000',
        componentCount: 'N/A',
        percentageChangeComponent: 'N/A',
        requestCount: 'N/A',
        percentageChangeRequest: 'N/A',
        peakStorage: 'N/A',
        responseSize: 'N/A'
      }
    ];

    jest.spyOn(Axios, 'get').mockResolvedValue({ data: mockData });

    await renderView();
    await waitFor(() => {
      expect(screen.getAllByText('N/A')[0]).toBeInTheDocument();
    });
  });

  it('renders the components change tooltip correctly', async () => {
    await renderView();

    const componentsChangeTooltipTrigger = screen
      .getByText('Components % Change')
      .closest('th')
      .querySelector('[data-icon="info-circle"]');
    expect(componentsChangeTooltipTrigger).toBeInTheDocument();

    await TestUtils.expectToSeeTooltipOnHover(
      componentsChangeTooltipTrigger,
      'Change rate of the peak component count from the previous month.'
    );
  });

  it('renders the requests change tooltip correctly', async () => {
    await renderView();

    const requestsChangeTooltipTrigger = screen
      .getByText('Requests % Change')
      .closest('th')
      .querySelector('[data-icon="info-circle"]');
    expect(requestsChangeTooltipTrigger).toBeInTheDocument();

    await TestUtils.expectToSeeTooltipOnHover(
      requestsChangeTooltipTrigger,
      'Change rate of the total monthly requests from the previous month.'
    );
  });

  it('renders the egress tooltip correctly', async () => {
    await renderView();

    const egressTooltipTrigger = screen
      .getByText('Total Egress')
      .closest('th')
      .querySelector('[data-icon="info-circle"]');
    expect(egressTooltipTrigger).toBeInTheDocument();

    await TestUtils.expectToSeeTooltipOnHover(
      egressTooltipTrigger,
      'Egress is based on application-level tracking and may differ from actual network transfer measured by your cloud provider.'
    );
  });

  describe('Usage Insights Chart', () => {
    it('does not render the chart when isCloud is false', async () => {
      jest.spyOn(ExtJS, 'useState').mockReturnValue(false);

      await renderView();

      expect(screen.queryByTestId('usage-insights-chart')).not.toBeInTheDocument();
    });

    it('does not render the chart when isCloud is not present', async () => {
      jest.spyOn(ExtJS, 'useState').mockReturnValue(undefined);

      await renderView();

      expect(screen.queryByTestId('usage-insights-chart')).not.toBeInTheDocument();
    });

    it('renders the chart when isCloud is true', async () => {
      jest.spyOn(ExtJS, 'useState').mockReturnValue(true);

      await renderView();

      expect(screen.getByTestId('usage-insights-chart')).toBeInTheDocument();
    });
  });
});
