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
import { render, screen, fireEvent } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import UsagePage from '../UsagePage';
import { USAGE_STRINGS } from '../usageStrings';
import * as useUsageModule from '../useUsage';

jest.mock('../UsageChart', () => ({ UsageChart: () => <div data-testid="usage-chart" /> }));
jest.mock('../UsageTable', () => ({ UsageTable: () => <div data-testid="usage-table" /> }));

const baseHook = {
  loading: false, error: null, isPermissionError: false,
  metrics: [], retry: jest.fn(),
  storageNoteVisible: true, dismissStorageNote: jest.fn(),
  chartLoading: false, chartError: null, chartData: [], monthOptions: [],
  selectedMonth: null, selectMonth: jest.fn(), retryChart: jest.fn(),
};
const spy = jest.spyOn(useUsageModule, 'useUsage');
const renderWithTheme = (ui: React.ReactElement) => render(<Theme>{ui}</Theme>);

describe('UsagePage', () => {
  beforeEach(() => jest.clearAllMocks());

  it('renders the docs link, chart, table, and update-frequency alert', () => {
    spy.mockReturnValue({ ...baseHook });
    renderWithTheme(<UsagePage />);
    expect(screen.getByText(USAGE_STRINGS.LEARN_MORE)).toHaveAttribute('href', USAGE_STRINGS.LEARN_MORE_URL);
    expect(screen.getByTestId('usage-chart')).toBeInTheDocument();
    expect(screen.getByTestId('usage-table')).toBeInTheDocument();
    expect(screen.getByText(USAGE_STRINGS.UPDATE_FREQUENCY)).toBeInTheDocument();
  });

  it('renders the dismissible storage note by default', () => {
    spy.mockReturnValue({ ...baseHook, storageNoteVisible: true });
    renderWithTheme(<UsagePage />);
    expect(screen.getByText(/version history retained for 45 days/)).toBeInTheDocument();
  });

  it('hides the storage note when dismissed', () => {
    spy.mockReturnValue({ ...baseHook, storageNoteVisible: false });
    renderWithTheme(<UsagePage />);
    expect(screen.queryByText(/version history retained for 45 days/)).not.toBeInTheDocument();
  });

  it('shows a permission error and hides the content', () => {
    spy.mockReturnValue({ ...baseHook, isPermissionError: true, error: USAGE_STRINGS.PERMISSION_ERROR });
    renderWithTheme(<UsagePage />);
    expect(screen.getByText(USAGE_STRINGS.PERMISSION_ERROR)).toBeInTheDocument();
    expect(screen.queryByTestId('usage-chart')).not.toBeInTheDocument();
  });

  it('offers an explicit Retry action on a load error (no auto-refetch dismiss)', () => {
    const retry = jest.fn();
    spy.mockReturnValue({ ...baseHook, error: 'Something failed', retry });
    renderWithTheme(<UsagePage />);
    fireEvent.click(screen.getByRole('button', { name: /retry/i }));
    expect(retry).toHaveBeenCalled();
  });

  it('renders a Settings breadcrumb that navigates back to the settings hub', () => {
    spy.mockReturnValue({ ...baseHook });
    renderWithTheme(<UsagePage />);
    fireEvent.click(screen.getByRole('button', { name: 'Settings' }));
    expect(window.location.hash).toBe('#preview/admin/settings');
  });
});
