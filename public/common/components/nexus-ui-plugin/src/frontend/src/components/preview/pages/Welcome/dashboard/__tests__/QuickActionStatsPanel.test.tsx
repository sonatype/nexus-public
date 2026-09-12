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
    componentCount: null,
    componentLimit: 0,
    componentsLoading: false,
    componentsError: false,
    retry: jest.fn(),
  }),
}));

// ---------- helpers ----------------------------------------------------

import { ExtJS } from '../../../../../../interface/ExtJS';
import { useMetricHealthApi } from '../../../settings/support/metric-health/useMetricHealthApi';
import { useInstanceTotals } from '../useInstanceTotals';

function getComponentsCardValue(container: HTMLElement): HTMLElement | null {
  // Locate the card by its title rather than its grid position, so the helper
  // stays correct if the card order changes.
  const titles = container.querySelectorAll('.nxrm-quick-action-stat-card__title');
  const componentsTitle = Array.from(titles).find((el) => el.textContent?.trim() === 'Components');
  const content = componentsTitle?.closest('.nxrm-quick-action-stat-card__content');
  return (content?.querySelector('.nxrm-quick-action-stat-card__value') as HTMLElement) ?? null;
}

function mockInstanceTotals(overrides: Record<string, unknown> = {}) {
  (useInstanceTotals as jest.Mock).mockReturnValue({
    data: null,
    loading: false,
    componentCount: null,
    componentLimit: 0,
    componentsLoading: false,
    componentsError: false,
    retry: jest.fn(),
    ...overrides,
  });
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

  it('does NOT render the System Health card at all on cloud', async () => {
    setIsCloud(true);
    const { container } = renderPanel();
    await waitFor(() => {
      const titles = container.querySelectorAll('.nxrm-quick-action-stat-card__title');
      const systemHealthTitle = Array.from(titles).find((el) => el.textContent?.includes('System Health'));
      expect(systemHealthTitle).toBeUndefined();
    });
  });

  it('still renders other cards (Repositories, Components) on cloud', async () => {
    setIsCloud(true);
    renderPanel();
    await waitFor(() => {
      expect(screen.getByText(/repositories/i)).toBeInTheDocument();
      expect(screen.getByText(/components/i)).toBeInTheDocument();
    });
  });

  it('renders a progress bar with limit text when totalComponentsLimit is set', async () => {
    mockInstanceTotals({ componentCount: 8000, componentLimit: 10000 });
    const { container } = renderPanel();

    await waitFor(() => {
      const valueDiv = getComponentsCardValue(container);
      expect(valueDiv).not.toBeNull();
      expect(within(valueDiv!).getByText(/8,000 of 10,000/)).toBeInTheDocument();
    });
  });

  it('does not render a progress bar when totalComponentsLimit is 0', async () => {
    mockInstanceTotals({ componentCount: 5000, componentLimit: 0 });
    const { container } = renderPanel();

    await waitFor(() => {
      const valueDiv = getComponentsCardValue(container);
      expect(valueDiv).not.toBeNull();
      expect(within(valueDiv!).queryByText(/of.*limit/)).not.toBeInTheDocument();
    });
  });

  it('shows the loading spinner while the Components metric is loading', async () => {
    mockInstanceTotals({ componentsLoading: true });
    const { container } = renderPanel();

    await waitFor(() => {
      const valueDiv = getComponentsCardValue(container);
      expect(valueDiv).not.toBeNull();
      expect(within(valueDiv!).getByText(/Loading/i)).toBeInTheDocument();
    });
  });

  it('shows an error message with a Retry button when the metric times out', async () => {
    mockInstanceTotals({ componentsError: true });
    const { container } = renderPanel();

    await waitFor(() => {
      const valueDiv = getComponentsCardValue(container);
      expect(valueDiv).not.toBeNull();
      expect(within(valueDiv!).getByText(/Couldn't load/i)).toBeInTheDocument();
      expect(within(valueDiv!).getByRole('button', { name: /retry/i })).toBeInTheDocument();
    });
    // The spinner must NOT be present in the error state.
    const valueDiv = getComponentsCardValue(container);
    expect(within(valueDiv!).queryByText(/Loading/i)).not.toBeInTheDocument();
  });

  it('calls retry() when the Retry button is clicked', async () => {
    const retry = jest.fn();
    mockInstanceTotals({ componentsError: true, retry });
    const { container } = renderPanel();

    let retryButton: HTMLElement | undefined;
    await waitFor(() => {
      const valueDiv = getComponentsCardValue(container);
      retryButton = within(valueDiv!).getByRole('button', { name: /retry/i });
      expect(retryButton).toBeInTheDocument();
    });

    fireEvent.click(retryButton!);
    expect(retry).toHaveBeenCalledTimes(1);
  });
});

describe('QuickActionStatsPanel — System Health loading and null states', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    setIsCloud(false);
  });

  it('shows loading skeleton while healthChecks is null', async () => {
    // Keep healthChecks null by making the fetch never resolve
    const { useMetricHealthApi } = require('../../../settings/support/metric-health/useMetricHealthApi');
    useMetricHealthApi.mockReturnValue({
      fetchMetricHealth: jest.fn(() => new Promise(() => {})), // never resolves
    });

    const { container } = renderPanel();
    await waitFor(() => {
      const titles = container.querySelectorAll('.nxrm-quick-action-stat-card__title');
      const systemHealthTitle = Array.from(titles).find((el) => el.textContent?.includes('System Health'));
      const content = systemHealthTitle?.closest('.nxrm-quick-action-stat-card__content');
      expect(content).not.toBeNull();
      expect(within(content!).getByText(/Loading/i)).toBeInTheDocument();
    });
  });

  it('does NOT render the System Health card at all on cloud', async () => {
    setIsCloud(true);
    const { container } = renderPanel();

    await waitFor(() => {
      const titles = container.querySelectorAll('.nxrm-quick-action-stat-card__title');
      const systemHealthTitle = Array.from(titles).find((el) => el.textContent?.includes('System Health'));
      expect(systemHealthTitle).toBeUndefined();
    });
  });

  it('does NOT render the System Health card when fetchMetricHealth fails', async () => {
    const { useMetricHealthApi } = require('../../../settings/support/metric-health/useMetricHealthApi');
    useMetricHealthApi.mockReturnValue({
      fetchMetricHealth: jest.fn().mockRejectedValue(new Error('network')),
    });

    const { container } = renderPanel();
    await waitFor(() => {
      const titles = container.querySelectorAll('.nxrm-quick-action-stat-card__title');
      const systemHealthTitle = Array.from(titles).find((el) => el.textContent?.includes('System Health'));
      expect(systemHealthTitle).toBeUndefined();
    });
  });

  it('does NOT render an href on the System Health card while it is loading', async () => {
    const { useMetricHealthApi } = require('../../../settings/support/metric-health/useMetricHealthApi');
    useMetricHealthApi.mockReturnValue({
      fetchMetricHealth: jest.fn(() => new Promise(() => {})), // never resolves
    });

    renderPanel();
    await waitFor(() => {
      // While loading the card is present but MUST NOT be a link.
      expect(screen.queryByRole('link', { name: /system health/i })).not.toBeInTheDocument();
    });
  });

  it('renders progress bar when healthChecks is available on self-hosted', async () => {
    const { useMetricHealthApi } = require('../../../settings/support/metric-health/useMetricHealthApi');
    useMetricHealthApi.mockReturnValue({
      fetchMetricHealth: jest.fn().mockResolvedValue([
        { name: 'check1', result: { healthy: true } },
        { name: 'check2', result: { healthy: false } },
      ]),
    });

    const { container } = renderPanel();
    await waitFor(() => {
      const titles = container.querySelectorAll('.nxrm-quick-action-stat-card__title');
      const systemHealthTitle = Array.from(titles).find((el) => el.textContent?.includes('System Health'));
      expect(systemHealthTitle).toBeDefined();
      const content = systemHealthTitle?.closest('.nxrm-quick-action-stat-card__content');
      expect(content).not.toBeNull();
      const valueDiv = content?.querySelector('.nxrm-quick-action-stat-card__value');
      expect(valueDiv).not.toBeNull();
      const numbers = within(valueDiv!).getAllByText('1');
      expect(numbers.length).toBe(2); // both unhealthy and healthy counts are 1
    });
  });
});

describe('QuickActionStatsPanel — Repositories loading and error states', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    setIsCloud(false);
  });

  it('shows loading skeleton while reposByFormat.loading is true', async () => {
    const { useRepositoriesByFormat } = require('../useRepositoriesByFormat');
    useRepositoriesByFormat.mockReturnValue({
      loading: true,
      error: null,
      data: null,
    });

    const { container } = renderPanel();
    await waitFor(() => {
      const titles = container.querySelectorAll('.nxrm-quick-action-stat-card__title');
      const reposTitle = Array.from(titles).find((el) => el.textContent?.trim() === 'Repositories');
      const content = reposTitle?.closest('.nxrm-quick-action-stat-card__content');
      expect(content).not.toBeNull();
      expect(within(content!).getByText(/Loading/i)).toBeInTheDocument();
    });
  });

  it('does NOT render the Repositories card when reposByFormat.error is set', async () => {
    const { useRepositoriesByFormat } = require('../useRepositoriesByFormat');
    useRepositoriesByFormat.mockReturnValue({
      loading: false,
      error: { message: 'Fetch failed' },
      data: null,
    });

    const { container } = renderPanel();
    await waitFor(() => {
      const titles = container.querySelectorAll('.nxrm-quick-action-stat-card__title');
      const reposTitle = Array.from(titles).find((el) => el.textContent?.trim() === 'Repositories');
      expect(reposTitle).toBeUndefined();
    });
  });

  it('renders repository count and buttons when data is available', async () => {
    const { useRepositoriesByFormat } = require('../useRepositoriesByFormat');
    useRepositoriesByFormat.mockReturnValue({
      loading: false,
      error: null,
      data: [
        { format: 'maven2', totalCount: 10 },
        { format: 'npm', totalCount: 5 },
      ],
    });

    const { container } = renderPanel();
    await waitFor(() => {
      const titles = container.querySelectorAll('.nxrm-quick-action-stat-card__title');
      const reposTitle = Array.from(titles).find((el) => el.textContent?.trim() === 'Repositories');
      const content = reposTitle?.closest('.nxrm-quick-action-stat-card__content');
      expect(content).not.toBeNull();
      expect(within(content!).getByText('15')).toBeInTheDocument();
      expect(within(content!).getByRole('button', { name: /browse/i })).toBeInTheDocument();
      expect(within(content!).getByRole('button', { name: /connect/i })).toBeInTheDocument();
    });
  });
});

describe('QuickActionStatsPanel — Cleanup Policies loading state', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    setIsCloud(false);
  });

  it('shows loading skeleton while cleanupCount is null', async () => {
    const { useCleanupPoliciesApi } = require('../../../settings/repository/cleanup/useCleanupPoliciesApi');
    useCleanupPoliciesApi.mockReturnValue({
      fetchCleanupPolicies: jest.fn(() => new Promise(() => {})), // never resolves
    });

    const { container } = renderPanel();
    await waitFor(() => {
      const titles = container.querySelectorAll('.nxrm-quick-action-stat-card__title');
      const cleanupTitle = Array.from(titles).find((el) => el.textContent?.trim() === 'Cleanup Policies');
      const content = cleanupTitle?.closest('.nxrm-quick-action-stat-card__content');
      expect(content).not.toBeNull();
      expect(within(content!).getByText(/Loading/i)).toBeInTheDocument();
    });
  });

  it('renders zero-state card when cleanupCount is 0', async () => {
    const { useCleanupPoliciesApi } = require('../../../settings/repository/cleanup/useCleanupPoliciesApi');
    useCleanupPoliciesApi.mockReturnValue({
      fetchCleanupPolicies: jest.fn().mockResolvedValue([]),
    });

    const { container } = renderPanel();
    await waitFor(() => {
      const titles = container.querySelectorAll('.nxrm-quick-action-stat-card__title');
      const cleanupTitle = Array.from(titles).find((el) => el.textContent?.trim() === 'Cleanup Policies');
      const content = cleanupTitle?.closest('.nxrm-quick-action-stat-card__content');
      expect(content).not.toBeNull();
      expect(within(content!).getByText('0')).toBeInTheDocument();
      expect(within(content!).getByRole('button', { name: /add cleanup policy/i })).toBeInTheDocument();
    });
  });

  it('renders count when cleanupCount is greater than 0', async () => {
    const { useCleanupPoliciesApi } = require('../../../settings/repository/cleanup/useCleanupPoliciesApi');
    useCleanupPoliciesApi.mockReturnValue({
      fetchCleanupPolicies: jest.fn().mockResolvedValue([
        { name: 'policy1' },
        { name: 'policy2' },
        { name: 'policy3' },
      ]),
    });

    const { container } = renderPanel();
    await waitFor(() => {
      const titles = container.querySelectorAll('.nxrm-quick-action-stat-card__title');
      const cleanupTitle = Array.from(titles).find((el) => el.textContent?.trim() === 'Cleanup Policies');
      const content = cleanupTitle?.closest('.nxrm-quick-action-stat-card__content');
      expect(content).not.toBeNull();
      expect(within(content!).getByText('3')).toBeInTheDocument();
    });
  });

  it('does NOT render Cleanup Policies card when fetch fails', async () => {
    const { useCleanupPoliciesApi } = require('../../../settings/repository/cleanup/useCleanupPoliciesApi');
    useCleanupPoliciesApi.mockReturnValue({
      fetchCleanupPolicies: jest.fn().mockRejectedValue(new Error('network')),
    });

    const { container } = renderPanel();
    await waitFor(() => {
      const titles = container.querySelectorAll('.nxrm-quick-action-stat-card__title');
      const cleanupTitle = Array.from(titles).find((el) => el.textContent?.trim() === 'Cleanup Policies');
      expect(cleanupTitle).toBeUndefined();
    });
  });

  it('does NOT render Cleanup Policies card when user lacks admin permission', async () => {
    const { ExtJS } = require('../../../../../../interface/ExtJS');
    ExtJS.checkPermission.mockReturnValue(false);

    const { container } = renderPanel();
    await waitFor(() => {
      const titles = container.querySelectorAll('.nxrm-quick-action-stat-card__title');
      const cleanupTitle = Array.from(titles).find((el) => el.textContent?.trim() === 'Cleanup Policies');
      expect(cleanupTitle).toBeUndefined();
    });
  });
});
