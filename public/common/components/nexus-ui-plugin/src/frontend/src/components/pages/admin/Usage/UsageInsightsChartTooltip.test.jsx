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
  it('renders tooltip with date and values when both metrics available', () => {
    const data = {
      metricDate: '2024-01-15',
      [UIStrings.HISTORICAL_USAGE.CHART.LEGEND_EGRESS]: 1000,
      [UIStrings.HISTORICAL_USAGE.CHART.LEGEND_STORAGE]: 2000,
      _available: {egress: true, storage: true}
    };

    render(<UsageInsightsChartTooltip data={data} />);

    expect(screen.getByText(/Jan 15/)).toBeInTheDocument();
    expect(screen.getByText('Total Egress')).toBeInTheDocument();
    expect(screen.getByText('Peak Storage')).toBeInTheDocument();
    expect(screen.getByText(/1\.00 kB/)).toBeInTheDocument();
    expect(screen.getByText(/2\.00 kB/)).toBeInTheDocument();
  });

  it('renders storage before egress to match chart stacking order', () => {
    const data = {
      metricDate: '2024-01-15',
      [UIStrings.HISTORICAL_USAGE.CHART.LEGEND_EGRESS]: 1000,
      [UIStrings.HISTORICAL_USAGE.CHART.LEGEND_STORAGE]: 2000,
      _available: {egress: true, storage: true}
    };

    const {container} = render(<UsageInsightsChartTooltip data={data} />);

    const items = container.querySelectorAll('.tooltip-content-item');
    expect(items).toHaveLength(2);

    // Storage should be first (top of tooltip matches top of stacked bar)
    expect(items[0].querySelector('.tooltip-peak-storage')).toBeInTheDocument();
    expect(items[1].querySelector('.tooltip-total-egress')).toBeInTheDocument();
  });

  it('handles missing data gracefully', () => {
    render(<UsageInsightsChartTooltip data={null} />);

    expect(screen.getByText('Total Egress')).toBeInTheDocument();
    expect(screen.getByText('Peak Storage')).toBeInTheDocument();

    const notAvailable = screen.getAllByText(UIStrings.HISTORICAL_USAGE.CHART.DATA_NOT_AVAILABLE);
    expect(notAvailable).toHaveLength(2);
  });

  it('renders correct CSS classes', () => {
    const data = {
      metricDate: '2024-01-15',
      [UIStrings.HISTORICAL_USAGE.CHART.LEGEND_EGRESS]: 1000,
      [UIStrings.HISTORICAL_USAGE.CHART.LEGEND_STORAGE]: 2000,
      _available: {egress: true, storage: true}
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
      [UIStrings.HISTORICAL_USAGE.CHART.LEGEND_STORAGE]: 2000,
      _available: {egress: true, storage: true}
    };

    const {container} = render(<UsageInsightsChartTooltip data={data} />);

    expect(container.querySelector('.tooltip-item-symbol.tooltip-total-egress')).toBeInTheDocument();
    expect(container.querySelector('.tooltip-item-symbol.tooltip-peak-storage')).toBeInTheDocument();
  });

  it('handles zero values as real data', () => {
    const data = {
      metricDate: '2024-01-15',
      [UIStrings.HISTORICAL_USAGE.CHART.LEGEND_EGRESS]: 0,
      [UIStrings.HISTORICAL_USAGE.CHART.LEGEND_STORAGE]: 0,
      _available: {egress: true, storage: true}
    };

    render(<UsageInsightsChartTooltip data={data} />);

    const zeroValues = screen.getAllByText('0.00 Bytes');
    expect(zeroValues).toHaveLength(2);
  });

  it('shows "Data not available" when metric is missing from API', () => {
    const data = {
      metricDate: '2024-01-15',
      [UIStrings.HISTORICAL_USAGE.CHART.LEGEND_EGRESS]: 1000,
      [UIStrings.HISTORICAL_USAGE.CHART.LEGEND_STORAGE]: 0,
      _available: {egress: true}
    };

    render(<UsageInsightsChartTooltip data={data} />);

    expect(screen.getByText(/1\.00 kB/)).toBeInTheDocument();
    expect(screen.getByText(UIStrings.HISTORICAL_USAGE.CHART.DATA_NOT_AVAILABLE)).toBeInTheDocument();
  });

  it('shows "Data not available" for egress when only storage available', () => {
    const data = {
      metricDate: '2024-01-15',
      [UIStrings.HISTORICAL_USAGE.CHART.LEGEND_EGRESS]: 0,
      [UIStrings.HISTORICAL_USAGE.CHART.LEGEND_STORAGE]: 5000,
      _available: {storage: true}
    };

    render(<UsageInsightsChartTooltip data={data} />);

    expect(screen.getByText(/5\.00 kB/)).toBeInTheDocument();
    expect(screen.getByText(UIStrings.HISTORICAL_USAGE.CHART.DATA_NOT_AVAILABLE)).toBeInTheDocument();
  });

  it('shows "Data not available" for both when no _available info', () => {
    const data = {
      metricDate: '2024-01-15'
    };

    render(<UsageInsightsChartTooltip data={data} />);

    expect(screen.getByText('Total Egress')).toBeInTheDocument();
    expect(screen.getByText('Peak Storage')).toBeInTheDocument();

    const notAvailable = screen.getAllByText(UIStrings.HISTORICAL_USAGE.CHART.DATA_NOT_AVAILABLE);
    expect(notAvailable).toHaveLength(2);
  });

  it('formats large byte values correctly', () => {
    const data = {
      metricDate: '2024-01-15',
      [UIStrings.HISTORICAL_USAGE.CHART.LEGEND_EGRESS]: 1000000000,
      [UIStrings.HISTORICAL_USAGE.CHART.LEGEND_STORAGE]: 2000000000,
      _available: {egress: true, storage: true}
    };

    render(<UsageInsightsChartTooltip data={data} />);

    const gbValues = screen.getAllByText(/GB/);
    expect(gbValues.length).toBeGreaterThanOrEqual(2);
  });

  it('displays N/A when date is missing', () => {
    const data = {
      [UIStrings.HISTORICAL_USAGE.CHART.LEGEND_EGRESS]: 1000,
      _available: {egress: true}
    };

    render(<UsageInsightsChartTooltip data={data} />);

    expect(screen.getByText('N/A')).toBeInTheDocument();
  });
});
