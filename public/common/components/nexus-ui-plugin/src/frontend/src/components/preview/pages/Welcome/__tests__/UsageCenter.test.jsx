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
import UsageCenter from '../UsageCenter';

jest.mock('../../../../../interface/ExtJS', () => ({
  ExtJS: {
    isProEdition: jest.fn(),
    state: jest.fn(),
  },
}));

// Source imports '../../shared/Tooltip' (2 up from Welcome/). From __tests__/ we
// need 3 ups to reach the same preview/shared/Tooltip path. Mocked (rather than
// exercising real Radix hover/focus behavior) following the established pattern
// in preview/shared/Navigation/__tests__/NavItem.test.tsx.
jest.mock('../../../shared/Tooltip', () => ({
  Tooltip: ({children, content}) => (
    <>
      {children}
      <span data-testid="tooltip-content">{content}</span>
    </>
  ),
}));

jest.mock('../../../../widgets/SystemStatusAlerts/CELimits/UsageHelper', () => ({
  helperFunctions: {
    getMetricData: jest.fn(),
  },
}));

import {ExtJS} from '../../../../../interface/ExtJS';
import {helperFunctions} from '../../../../widgets/SystemStatusAlerts/CELimits/UsageHelper';

const mockGetMetricData = helperFunctions.getMetricData;
const mockIsProEdition = ExtJS.isProEdition;
const mockState = ExtJS.state;

const METRIC_DATA = {
  component_total_count:          {metricValue: 1234,  thresholdValue: 120000, highestRecordedCount: 5000,  aggregates: []},
  peak_requests_per_day:          {metricValue: 2500,  thresholdValue: 200000, highestRecordedCount: 3000,  aggregates: []},
  peak_requests_per_day_30d:      {metricValue: 3100,  thresholdValue: 200000, highestRecordedCount: 3200,  aggregates: []},
  totalMonthlyRequestMetrics:     {metricValue: 10000, thresholdValue: 0,      highestRecordedCount: 0,     aggregates: []},
  averageMonthlyRequestMetrics:   {metricValue: 8000,  thresholdValue: 0,      highestRecordedCount: 0,     aggregates: []},
  highestMonthlyRequestMetrics:   {metricValue: 12000, thresholdValue: 0,      highestRecordedCount: 0,     aggregates: []},
};

const DEFAULT_METRIC = {metricValue: 0, thresholdValue: 0, highestRecordedCount: 0, aggregates: []};

// Matches real EvaluatorResult shape (metricName/metricValue, not name/value)
const mockUsage = [{metricName: 'component_total_count', metricValue: 1234, thresholds: [], aggregates: []}];

function makeStateValue(edition, extra = {}) {
  return {
    getEdition: jest.fn().mockReturnValue(edition),
    getValue: jest.fn().mockImplementation((key, def) => {
      if (key in extra) return extra[key];
      if (key === 'contentUsageEvaluationResult') return mockUsage;
      if (key === 'datastore.isPostgresql') return false;
      return def ?? null;
    }),
  };
}

describe('UsageCenter', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockGetMetricData.mockImplementation((_usage, metricName) => METRIC_DATA[metricName] ?? DEFAULT_METRIC);
  });

  it('renders nothing when usage data is empty', () => {
    mockIsProEdition.mockReturnValue(true);
    mockState.mockReturnValue(makeStateValue('PRO', {contentUsageEvaluationResult: []}));
    const {container} = render(<UsageCenter />);
    expect(container.firstChild).toBeNull();
  });

  it('renders nothing when contentUsageEvaluationResult is null', () => {
    mockIsProEdition.mockReturnValue(true);
    mockState.mockReturnValue(makeStateValue('PRO', {contentUsageEvaluationResult: null}));
    const {container} = render(<UsageCenter />);
    expect(container.firstChild).toBeNull();
  });

  describe('Pro Edition', () => {
    beforeEach(() => {
      mockIsProEdition.mockReturnValue(true);
      mockState.mockReturnValue(makeStateValue('PRO'));
    });

    // NEXUS-53863: StatCard must surface highestRecordedCount for Total Components
    it('shows "Highest Recorded Count (30 days)" label for Total Components', () => {
      render(<UsageCenter />);
      const labels = screen.getAllByText('Highest Recorded Count (30 days)');
      expect(labels.length).toBeGreaterThanOrEqual(1);
    });

    it('shows highestRecordedCount value (5,000) for Total Components', () => {
      render(<UsageCenter />);
      expect(screen.getByText('5,000')).toBeInTheDocument();
    });

    // NEXUS-53863: StatCard must surface highestRecordedCount for Requests Per Day
    it('shows highestRecordedCount value (3,000) for Requests Per Day', () => {
      render(<UsageCenter />);
      expect(screen.getByText('3,000')).toBeInTheDocument();
    });

    // NEXUS-53863: Monthly Requests replaces single StatCard with a 3-metric breakdown
    it('shows "Total requests in [month]" label for Monthly Requests', () => {
      render(<UsageCenter />);
      expect(screen.getByText(/Total requests in/)).toBeInTheDocument();
    });

    it('shows total monthly value (10,000)', () => {
      render(<UsageCenter />);
      expect(screen.getByText('10,000')).toBeInTheDocument();
    });

    it('shows average monthly label and value', () => {
      render(<UsageCenter />);
      expect(screen.getByText('Average requests (12 months)')).toBeInTheDocument();
      expect(screen.getByText('8,000')).toBeInTheDocument();
    });

    it('shows highest monthly label and value', () => {
      render(<UsageCenter />);
      expect(screen.getByText('Highest recorded count (12 months)')).toBeInTheDocument();
      expect(screen.getByText('12,000')).toBeInTheDocument();
    });

    // NEXUS-54200: Classic UI parity — only CE cards carry help tooltips, Pro has none
    it('renders no help-tooltip triggers', () => {
      render(<UsageCenter />);
      expect(screen.queryByTestId('tooltip-content')).not.toBeInTheDocument();
    });
  });

  describe('Community Edition', () => {
    beforeEach(() => {
      mockIsProEdition.mockReturnValue(false);
      mockState.mockReturnValue(makeStateValue('COMMUNITY'));
    });

    // NEXUS-53863: MonthlyMetricsCard CE was broken (used CARDS.MONTHLY_METRICS which doesn't exist);
    // must use MONTHLY_REQUESTS.TOTAL/AVERAGE/HIGHEST instead
    it('shows "Total requests in [month]" label in monthly card', () => {
      render(<UsageCenter />);
      expect(screen.getByText(/Total requests in/)).toBeInTheDocument();
    });

    it('shows total monthly value (10,000)', () => {
      render(<UsageCenter />);
      expect(screen.getByText('10,000')).toBeInTheDocument();
    });

    it('shows average monthly label and value', () => {
      render(<UsageCenter />);
      expect(screen.getByText('Average requests (12 months)')).toBeInTheDocument();
      expect(screen.getByText('8,000')).toBeInTheDocument();
    });

    it('shows highest monthly label and value', () => {
      render(<UsageCenter />);
      expect(screen.getByText('Highest recorded count (12 months)')).toBeInTheDocument();
      expect(screen.getByText('12,000')).toBeInTheDocument();
    });

    it('renders StatCardWithThreshold with threshold label and values for Total Components', () => {
      render(<UsageCenter />);
      // both CE cards render "Usage Limit" label — expect at least one
      const usageLimitLabels = screen.getAllByText('Usage Limit');
      expect(usageLimitLabels.length).toBeGreaterThanOrEqual(1);
      // component count metricValue (1,234) and thresholdValue (120,000) both visible
      expect(screen.getByText('1,234')).toBeInTheDocument();
      expect(screen.getByText('120,000')).toBeInTheDocument();
    });

    it('renders StatCardWithThreshold without crashing when thresholdValue is 0', () => {
      mockGetMetricData.mockImplementation((_usage, metricName) => {
        if (metricName === 'component_total_count' || metricName === 'peak_requests_per_day') {
          return {metricValue: 0, thresholdValue: 0, highestRecordedCount: 0, aggregates: []};
        }
        return METRIC_DATA[metricName] ?? DEFAULT_METRIC;
      });
      expect(() => render(<UsageCenter />)).not.toThrow();
    });

    // NEXUS-54200: CE cards carry Classic UI-parity help tooltips (Total Components,
    // Requests Per Day, Requests Per Month)
    it('renders a help-tooltip trigger for each of the three CE cards', () => {
      render(<UsageCenter />);
      expect(screen.getByLabelText('Total Components information')).toBeInTheDocument();
      expect(screen.getByLabelText('Requests Per Day information')).toBeInTheDocument();
      expect(screen.getByLabelText('Requests Per Month information')).toBeInTheDocument();
    });

    it('supplies tooltip content for each CE help-tooltip trigger', () => {
      render(<UsageCenter />);
      const tooltipContents = screen.getAllByTestId('tooltip-content');
      expect(tooltipContents.length).toBe(3);
      tooltipContents.forEach((node) => expect(node.textContent).not.toBe(''));
    });

    // NEXUS-54200: tooltip triggers are real <button>s, not bare <span>s, so they
    // resolve to an accessible "button" role instead of "generic" (WCAG 4.1.2)
    it('exposes each help-tooltip trigger with an accessible button role', () => {
      render(<UsageCenter />);
      expect(screen.getByRole('button', {name: 'Total Components information'})).toBeInTheDocument();
      expect(screen.getByRole('button', {name: 'Requests Per Day information'})).toBeInTheDocument();
      expect(screen.getByRole('button', {name: 'Requests Per Month information'})).toBeInTheDocument();
    });
  });

  describe('Status badge', () => {
    // NEXUS-53863: status badge appears next to the "Usage Center" heading

    it('shows "Usage below limits" badge for Pro edition', () => {
      mockIsProEdition.mockReturnValue(true);
      mockState.mockReturnValue(makeStateValue('PRO'));
      render(<UsageCenter />);
      expect(screen.getByText('Usage below limits')).toBeInTheDocument();
    });

    it('shows "Usage below limits" badge when CE metrics are well under thresholds', () => {
      mockIsProEdition.mockReturnValue(false);
      // metricValue 1234 vs thresholdValue 120000 — far below 75%
      mockState.mockReturnValue(makeStateValue('COMMUNITY'));
      render(<UsageCenter />);
      expect(screen.getByText('Usage below limits')).toBeInTheDocument();
    });

    it('shows "Usage nearing limits" badge when a CE metric approaches its threshold', () => {
      mockIsProEdition.mockReturnValue(false);
      mockState.mockReturnValue(makeStateValue('COMMUNITY'));
      // Override getMetricData: component at 80% of threshold (> 75% PERCENTAGE)
      mockGetMetricData.mockImplementation((_usage, metricName) => {
        if (metricName === 'component_total_count') {
          return {metricValue: 96000, thresholdValue: 120000, highestRecordedCount: 96000, aggregates: []};
        }
        return METRIC_DATA[metricName] ?? DEFAULT_METRIC;
      });
      render(<UsageCenter />);
      expect(screen.getByText('Usage nearing limits')).toBeInTheDocument();
    });

    it('shows "Usage over limits" badge when a CE metric exceeds its threshold', () => {
      mockIsProEdition.mockReturnValue(false);
      mockState.mockReturnValue(makeStateValue('COMMUNITY'));
      mockGetMetricData.mockImplementation((_usage, metricName) => {
        if (metricName === 'component_total_count') {
          return {metricValue: 130000, thresholdValue: 120000, highestRecordedCount: 130000, aggregates: []};
        }
        return METRIC_DATA[metricName] ?? DEFAULT_METRIC;
      });
      render(<UsageCenter />);
      expect(screen.getByText('Usage over limits')).toBeInTheDocument();
    });

    it('"over limits" takes priority over "nearing limits" when both conditions are present', () => {
      mockIsProEdition.mockReturnValue(false);
      mockState.mockReturnValue(makeStateValue('COMMUNITY'));
      mockGetMetricData.mockImplementation((_usage, metricName) => {
        if (metricName === 'component_total_count') {
          // over threshold
          return {metricValue: 130000, thresholdValue: 120000, highestRecordedCount: 130000, aggregates: []};
        }
        if (metricName === 'peak_requests_per_day') {
          // approaching threshold
          return {metricValue: 160000, thresholdValue: 200000, highestRecordedCount: 160000, aggregates: []};
        }
        return METRIC_DATA[metricName] ?? DEFAULT_METRIC;
      });
      render(<UsageCenter />);
      expect(screen.getByText('Usage over limits')).toBeInTheDocument();
      expect(screen.queryByText('Usage nearing limits')).not.toBeInTheDocument();
    });
  });
});
