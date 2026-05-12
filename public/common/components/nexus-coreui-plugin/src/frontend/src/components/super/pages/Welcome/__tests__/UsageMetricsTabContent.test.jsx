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
import UsageMetricsTabContent from '../UsageMetricsTabContent';
import {useUsageMetricsTabData} from '../useUsageMetricsTabData';

jest.mock('../useUsageMetricsTabData', () => ({
  useUsageMetricsTabData: jest.fn(),
}));

jest.mock('../dashboard', () => ({
  InstanceTotalsPanel: function MockInstanceTotalsPanel() {
    return <div data-testid="instance-totals-panel">InstanceTotalsPanel</div>;
  },
}));

jest.mock('../CloudUsageCenterPanel', () => ({
  CloudUsageCenterPanel: function MockCloudUsageCenterPanel({monthlyMetrics}) {
    if (monthlyMetrics.loading) return <div>Loading usage metrics...</div>;
    return <div data-testid="cloud-usage-panel">Usage Center</div>;
  },
}));

const baseHookResult = {
  isCloud: false,
  instanceTotals: {data: null, loading: false},
  monthlyMetrics: {loading: false, error: null, history: {egress: [], storage: []}},
  monthlyMetricsFormatted: undefined,
};

describe('UsageMetricsTabContent', () => {
  beforeEach(() => {
    useUsageMetricsTabData.mockReturnValue(baseHookResult);
  });

  it('renders InstanceTotalsPanel for non-cloud deployments', () => {
    render(<UsageMetricsTabContent />);
    expect(screen.getByTestId('instance-totals-panel')).toBeInTheDocument();
    expect(screen.queryByTestId('cloud-usage-panel')).not.toBeInTheDocument();
  });

  it('renders CloudUsageCenterPanel for cloud deployments', () => {
    useUsageMetricsTabData.mockReturnValue({...baseHookResult, isCloud: true});
    render(<UsageMetricsTabContent />);
    expect(screen.getByTestId('cloud-usage-panel')).toBeInTheDocument();
    expect(screen.queryByTestId('instance-totals-panel')).not.toBeInTheDocument();
  });

  it('passes monthlyMetrics to CloudUsageCenterPanel (loading state visible)', () => {
    useUsageMetricsTabData.mockReturnValue({
      ...baseHookResult,
      isCloud: true,
      monthlyMetrics: {loading: true, error: null, history: null},
    });
    render(<UsageMetricsTabContent />);
    expect(screen.getByText('Loading usage metrics...')).toBeInTheDocument();
  });
});
