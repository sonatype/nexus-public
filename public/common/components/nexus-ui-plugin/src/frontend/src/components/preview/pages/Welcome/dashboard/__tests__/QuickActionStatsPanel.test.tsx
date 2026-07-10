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
import { render, screen, waitFor, within } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { QuickActionStatsPanel } from '../QuickActionStatsPanel';

// ---------- module mocks -----------------------------------------------

jest.mock('../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    state: jest.fn().mockReturnValue({
      getValue: jest.fn().mockReturnValue(false), // default: not cloud
      getUser: jest.fn().mockReturnValue({ id: 'admin' }),
    }),
    checkPermission: jest.fn().mockReturnValue(true),
  },
}));

jest.mock('../../../settings/support/metric-health/useMetricHealthApi', () => ({
  useMetricHealthApi: jest.fn().mockReturnValue({
    fetchMetricHealth: jest.fn().mockResolvedValue([]),
  }),
}));

jest.mock('../../../settings/repository/cleanup/useCleanupPoliciesApi', () => ({
  useCleanupPoliciesApi: jest.fn().mockReturnValue({
    fetchCleanupPolicies: jest.fn().mockResolvedValue([]),
  }),
}));

jest.mock('../useRepositoriesByFormat', () => ({
  useRepositoriesByFormat: jest.fn().mockReturnValue({
    loading: false,
    error: null,
    data: [],
  }),
}));

jest.mock('../useInstanceTotals', () => ({
  useInstanceTotals: jest.fn().mockReturnValue({
    data: null,
    loading: false,
    error: null,
  }),
}));

// ---------- helpers ----------------------------------------------------

import { ExtJS } from '../../../../../../interface/ExtJS';
import { useMetricHealthApi } from '../../../settings/support/metric-health/useMetricHealthApi';
import { useInstanceTotals } from '../useInstanceTotals';

function getComponentsCardValue(container: HTMLElement): HTMLElement | null {
  const cards = container.querySelectorAll('.nxrm-quick-action-stat-card__value');
  // The "Components" card is rendered third in the grid
  return (cards[2] as HTMLElement) ?? null;
}

function setIsCloud(value: boolean) {
  (ExtJS.state as jest.Mock).mockReturnValue({
    getValue: jest.fn().mockReturnValue(value),
    getUser: jest.fn().mockReturnValue({ id: 'admin' }),
  });
}

const renderPanel = () =>
  render(
    <Theme>
      <QuickActionStatsPanel />
    </Theme>
  );

// ---------- tests -------------------------------------------------------

describe('QuickActionStatsPanel — System Health cloud behaviour', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    setIsCloud(false);
  });

  it('renders a clickable link to metrichealth on self-hosted', async () => {
    renderPanel();
    await waitFor(() => {
      const link = screen.getByRole('link', { name: /system health/i });
      expect(link).toHaveAttribute('href', '#preview/admin/support/metrichealth');
    });
  });

  it('calls fetchMetricHealth on self-hosted', async () => {
    // jest.clearAllMocks() in beforeEach clears call counts but not implementations,
    // so this mock instance is the same one the component will invoke internally.
    const { fetchMetricHealth } = useMetricHealthApi();
    renderPanel();
    await waitFor(() => {
      expect(fetchMetricHealth).toHaveBeenCalled();
    });
  });

  it('does NOT render a link to metrichealth on cloud', async () => {
    setIsCloud(true);
    renderPanel();
    await waitFor(() => expect(screen.getByText(/system health/i)).toBeInTheDocument());
    expect(screen.queryByRole('link', { name: /system health/i })).toBeNull();
  });

  it('still renders the System Health card title on cloud', async () => {
    setIsCloud(true);
    renderPanel();
    await waitFor(() => {
      expect(screen.getByText(/system health/i)).toBeInTheDocument();
    });
  });

  it('renders a progress bar with limit text when totalComponentsLimit is set', async () => {
    useInstanceTotals.mockReturnValue({
      data: {
        totalComponents: 8000,
        peakRequestsPerDay: 0,
        peakRequestsPerMonth: 0,
        totalComponentsLimit: 10000,
        peakRequestsPerDayLimit: 0,
      },
      loading: false,
    });
    const { container } = renderPanel();

    await waitFor(() => {
      const valueDiv = getComponentsCardValue(container);
      expect(valueDiv).not.toBeNull();
      expect(within(valueDiv!).getByText(/8,000 of 10,000/)).toBeInTheDocument();
    });
  });

  it('does not render a progress bar when totalComponentsLimit is 0', async () => {
    useInstanceTotals.mockReturnValue({
      data: {
        totalComponents: 5000,
        peakRequestsPerDay: 0,
        peakRequestsPerMonth: 0,
        totalComponentsLimit: 0,
        peakRequestsPerDayLimit: 0,
      },
      loading: false,
    });
    const { container } = renderPanel();

    await waitFor(() => {
      const valueDiv = getComponentsCardValue(container);
      expect(valueDiv).not.toBeNull();
      expect(within(valueDiv!).queryByText(/of.*limit/)).not.toBeInTheDocument();
    });
  });
});
