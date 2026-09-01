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
import { UsageChart } from '../UsageChart';
import { USAGE_STRINGS } from '../usageStrings';

// jsdom lacks pointer-capture / scrollIntoView that Radix Select touches on mount.
beforeAll(() => {
  window.HTMLElement.prototype.hasPointerCapture = jest.fn(() => false);
  window.HTMLElement.prototype.releasePointerCapture = jest.fn();
  window.HTMLElement.prototype.scrollIntoView = jest.fn();
});

// recharts needs a sized container in jsdom; stub ResponsiveContainer.
jest.mock('recharts', () => {
  const Actual = jest.requireActual('recharts');
  return { ...Actual, ResponsiveContainer: ({ children }: any) => <div>{children}</div> };
});

const months = [
  { key: 'jan', label: 'Jan 2026', value: { dateFrom: '2026-01-01', dateTo: '2026-01-31' } },
  { key: 'dec', label: 'Dec 2025', value: { dateFrom: '2025-12-01', dateTo: '2025-12-31' } },
];
const base = {
  data: [{ metricDate: '2026-01-01', egress: 10, storage: 20 }],
  monthOptions: months, selectedMonth: months[0],
  onSelectMonth: jest.fn(), loading: false, error: null, onRetry: jest.fn(),
};
const renderWithTheme = (ui: React.ReactElement) => render(<Theme>{ui}</Theme>);

describe('UsageChart', () => {
  it('renders the chart title and the month selector showing the selected month', () => {
    renderWithTheme(<UsageChart {...base} />);
    expect(screen.getByText(/Usage Insights/i)).toBeInTheDocument();
    // Radix Select trigger exposes role="combobox" and displays the selected value.
    expect(screen.getByRole('combobox')).toBeInTheDocument();
    expect(screen.getByText('Jan 2026')).toBeInTheDocument();
  });

  it('renders an error state with a retry button', () => {
    const onRetry = jest.fn();
    renderWithTheme(<UsageChart {...base} error="boom" onRetry={onRetry} />);
    fireEvent.click(screen.getByRole('button', { name: /retry/i }));
    expect(onRetry).toHaveBeenCalled();
  });

  it('renders a loading indicator while loading', () => {
    renderWithTheme(<UsageChart {...base} loading={true} />);
    expect(screen.getByText(/loading/i)).toBeInTheDocument();
  });

  it('renders the empty state when there is no data', () => {
    renderWithTheme(<UsageChart {...base} data={[]} />);
    expect(screen.getByText(USAGE_STRINGS.EMPTY)).toBeInTheDocument();
  });
});
