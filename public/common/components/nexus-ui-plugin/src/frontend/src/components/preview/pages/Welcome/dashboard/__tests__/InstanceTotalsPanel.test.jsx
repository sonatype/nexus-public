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
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import { InstanceTotalsPanel } from '../InstanceTotalsPanel';

// Helper to wrap component with Radix Theme
function renderWithTheme(ui) {
  return render(<Theme>{ui}</Theme>);
}

// Sample data matching the InstanceTotals type
const sampleData = {
  totalComponents: 12345678,
  peakRequestsPerDay: 1234,
  peakRequestsPerMonth: 45678,
};

describe('InstanceTotalsPanel', () => {
  describe('rendering', () => {
    it('renders the panel with heading when data is provided', () => {
      renderWithTheme(<InstanceTotalsPanel data={sampleData} loading={false} />);

      expect(screen.getByText('Usage Metrics')).toBeInTheDocument();
    });

    it('renders all three metric cards', () => {
      renderWithTheme(<InstanceTotalsPanel data={sampleData} loading={false} />);

      expect(screen.getByText('Total Components')).toBeInTheDocument();
      expect(screen.getByText('Peak Requests/Day')).toBeInTheDocument();
      expect(screen.getByText('Peak Requests/Month')).toBeInTheDocument();
    });

    it('displays the correct values', () => {
      renderWithTheme(<InstanceTotalsPanel data={sampleData} loading={false} />);

      expect(screen.getByText('12,345,678')).toBeInTheDocument();
      expect(screen.getByText('1,234')).toBeInTheDocument();
      expect(screen.getByText('45,678')).toBeInTheDocument();
    });
  });

  describe('number formatting', () => {
    it('formats numbers with commas', () => {
      const data = {
        totalComponents: 1000000,
        peakRequestsPerDay: 50000,
        peakRequestsPerMonth: 999999,
      };
      renderWithTheme(<InstanceTotalsPanel data={data} loading={false} />);

      expect(screen.getByText('1,000,000')).toBeInTheDocument();
      expect(screen.getByText('50,000')).toBeInTheDocument();
      expect(screen.getByText('999,999')).toBeInTheDocument();
    });

    it('handles zero values by showing "No activity"', () => {
      const data = {
        totalComponents: 0,
        peakRequestsPerDay: 0,
        peakRequestsPerMonth: 0,
      };
      renderWithTheme(<InstanceTotalsPanel data={data} loading={false} />);

      // Zero values show "No activity" instead of "0"
      const noActivityTexts = screen.getAllByText('No activity');
      expect(noActivityTexts).toHaveLength(3);
    });

    it('handles small numbers without commas', () => {
      const data = {
        totalComponents: 123,
        peakRequestsPerDay: 45,
        peakRequestsPerMonth: 678,
      };
      renderWithTheme(<InstanceTotalsPanel data={data} loading={false} />);

      expect(screen.getByText('123')).toBeInTheDocument();
      expect(screen.getByText('45')).toBeInTheDocument();
      expect(screen.getByText('678')).toBeInTheDocument();
    });
  });

  describe('conditional rendering', () => {
    it('returns null when data is null', () => {
      const { container } = renderWithTheme(
        <InstanceTotalsPanel data={null} loading={false} />
      );

      expect(container.querySelector('.nxrm-instance-totals')).not.toBeInTheDocument();
    });

    it('returns null when loading is true', () => {
      const { container } = renderWithTheme(
        <InstanceTotalsPanel data={sampleData} loading={true} />
      );

      expect(container.querySelector('.nxrm-instance-totals')).not.toBeInTheDocument();
    });

    it('returns null when both loading and no data', () => {
      const { container } = renderWithTheme(
        <InstanceTotalsPanel data={null} loading={true} />
      );

      expect(container.querySelector('.nxrm-instance-totals')).not.toBeInTheDocument();
    });
  });

  describe('metric help', () => {
    it('opens help modal when ? button is clicked', async () => {
      renderWithTheme(<InstanceTotalsPanel data={sampleData} loading={false} />);

      const helpButtons = screen.getAllByRole('button', {
        name: /how is.*calculated/i,
      });
      expect(helpButtons.length).toBeGreaterThanOrEqual(1);

      await userEvent.click(helpButtons[0]);

      expect(screen.getByRole('dialog')).toBeInTheDocument();
      expect(screen.getByText(/how is .* calculated\?/i)).toBeInTheDocument();
    });
  });
});
