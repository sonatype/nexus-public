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
import { render, screen } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { UsageTable } from '../UsageTable';
import { USAGE_STRINGS } from '../usageStrings';

const renderWithTheme = (ui: React.ReactElement) => render(<Theme>{ui}</Theme>);

describe('UsageTable', () => {
  it('renders all 6 cloud column headers', () => {
    renderWithTheme(<UsageTable metrics={[]} />);
    Object.values(USAGE_STRINGS.columns).forEach((label) => {
      expect(screen.getByText(label)).toBeInTheDocument();
    });
  });

  it('shows the empty state spanning all 6 columns when there are no metrics', () => {
    renderWithTheme(<UsageTable metrics={[]} />);
    const emptyCell = screen.getByText(USAGE_STRINGS.EMPTY).closest('td');
    // Radix Themes Table.Cell forwards colSpan to the underlying <td>, so the
    // empty-state row spans the full 6-column width (not a single cell).
    expect(emptyCell).toHaveAttribute('colspan', '6');
  });

  it('renders a data row with month and egress/storage percentage changes', () => {
    renderWithTheme(
      <UsageTable
        metrics={[{
          metricDate: '2026-01-01',
          egress: 1024,
          storage: 2048,
          percentageChangeEgress: 71,
          percentageChangeStorage: -5,
        }]}
      />,
    );
    expect(screen.getByText('Jan 2026')).toBeInTheDocument();
    expect(screen.getByText('71%')).toBeInTheDocument(); // egress % change (abs)
    expect(screen.getByText('5%')).toBeInTheDocument(); // storage % change (abs of -5)
  });

  it('renders "0%" with no trend icon when the percentage change is exactly 0', () => {
    const { container } = renderWithTheme(
      <UsageTable
        metrics={[{
          metricDate: '2026-03-01',
          egress: 1024,
          storage: 2048,
          percentageChangeEgress: 0,
          percentageChangeStorage: null,
        }]}
      />,
    );
    expect(screen.getByText('0%')).toBeInTheDocument();
    expect(container.querySelector('.change-icon')).toBeNull();
  });

  it('shows "0.00 Bytes" for egress, storage, and total usage when there is no value', () => {
    renderWithTheme(
      <UsageTable
        metrics={[{
          metricDate: '2026-02-01',
          egress: null,
          storage: 0,
          percentageChangeEgress: null,
          percentageChangeStorage: null,
        }]}
      />,
    );
    // Egress (null), Peak Storage (0), and Total Usage (0) all render 0.00 Bytes
    expect(screen.getAllByText('0.00 Bytes')).toHaveLength(3);
  });
});
