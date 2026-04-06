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
import {render, screen} from '@testing-library/react';
import {UsageInsightsChartTooltip} from './UsageInsightsChartTooltip';
import UIStrings from './../../../../constants/HistoricalUsageStrings';

describe('UsageInsightsChartTooltip', () => {
  it('renders tooltip with date and values', () => {
    const data = {
      metricDate: '2024-01-15',
      [UIStrings.HISTORICAL_USAGE.CHART.LEGEND_EGRESS]: 1000,
      [UIStrings.HISTORICAL_USAGE.CHART.LEGEND_STORAGE]: 2000
    };

    render(<UsageInsightsChartTooltip data={data} />);

    // Check date is formatted and displayed
    expect(screen.getByText(/Jan 15/)).toBeInTheDocument();

    // Check egress and storage labels
    expect(screen.getByText('Total Egress')).toBeInTheDocument();
    expect(screen.getByText('Peak Storage')).toBeInTheDocument();

    // Check formatted byte values
    expect(screen.getByText(/1\.00 kB/)).toBeInTheDocument();
    expect(screen.getByText(/2\.00 kB/)).toBeInTheDocument();
  });

  it('handles missing data gracefully', () => {
    render(<UsageInsightsChartTooltip data={null} />);

    // Should still render labels even with no data
    expect(screen.getByText('Total Egress')).toBeInTheDocument();
    expect(screen.getByText('Peak Storage')).toBeInTheDocument();

    // Should display 0 bytes for missing values (appears twice for egress and storage)
    const zeroBytes = screen.getAllByText('0.00 Bytes');
    expect(zeroBytes.length).toBeGreaterThanOrEqual(2);
  });

  it('renders correct CSS classes', () => {
    const data = {
      metricDate: '2024-01-15',
      [UIStrings.HISTORICAL_USAGE.CHART.LEGEND_EGRESS]: 1000,
      [UIStrings.HISTORICAL_USAGE.CHART.LEGEND_STORAGE]: 2000
    };

    const {container} = render(<UsageInsightsChartTooltip data={data} />);

    expect(container.querySelector('.usage-insights-chart-tooltip')).toBeInTheDocument();
    expect(container.querySelector('.tooltip-title')).toBeInTheDocument();
    expect(container.querySelector('.tooltip-content')).toBeInTheDocument();
    expect(container.querySelector('.tooltip-content-item')).toBeInTheDocument();
  });

  it('renders egress and storage symbols', () => {
    const data = {
      metricDate: '2024-01-15',
      [UIStrings.HISTORICAL_USAGE.CHART.LEGEND_EGRESS]: 1000,
      [UIStrings.HISTORICAL_USAGE.CHART.LEGEND_STORAGE]: 2000
    };

    const {container} = render(<UsageInsightsChartTooltip data={data} />);

    expect(container.querySelector('.tooltip-item-symbol.tooltip-total-egress')).toBeInTheDocument();
    expect(container.querySelector('.tooltip-item-symbol.tooltip-peak-storage')).toBeInTheDocument();
  });

  it('handles zero values', () => {
    const data = {
      metricDate: '2024-01-15',
      [UIStrings.HISTORICAL_USAGE.CHART.LEGEND_EGRESS]: 0,
      [UIStrings.HISTORICAL_USAGE.CHART.LEGEND_STORAGE]: 0
    };

    render(<UsageInsightsChartTooltip data={data} />);

    // Should display 0.00 Bytes for zero values
    const zeroValues = screen.getAllByText('0.00 Bytes');
    expect(zeroValues).toHaveLength(2);
  });

  it('handles partial data', () => {
    const data = {
      metricDate: '2024-01-15'
      // egress and storage missing
    };

    render(<UsageInsightsChartTooltip data={data} />);

    // Should display 0 bytes when egress/storage are undefined
    expect(screen.getByText('Total Egress')).toBeInTheDocument();
    expect(screen.getByText('Peak Storage')).toBeInTheDocument();
  });

  it('formats large byte values correctly', () => {
    const data = {
      metricDate: '2024-01-15',
      [UIStrings.HISTORICAL_USAGE.CHART.LEGEND_EGRESS]: 1000000000, // 1 GB (using decimal)
      [UIStrings.HISTORICAL_USAGE.CHART.LEGEND_STORAGE]: 2000000000 // 2 GB (using decimal)
    };

    render(<UsageInsightsChartTooltip data={data} />);

    // Check that values are formatted with GB unit (appears twice for egress and storage)
    const gbValues = screen.getAllByText(/GB/);
    expect(gbValues.length).toBeGreaterThanOrEqual(2);
  });
});
