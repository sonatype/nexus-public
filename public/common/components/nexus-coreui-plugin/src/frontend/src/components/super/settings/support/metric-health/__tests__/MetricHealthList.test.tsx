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

import { MetricHealthList } from '../MetricHealthList';
import { HealthCheck } from '../types';

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

describe('MetricHealthList', () => {
  const mockChecks: HealthCheck[] = [
    { name: 'threadDeadlockHealthCheck', result: { healthy: true, message: 'No deadlocks' } },
    { name: 'databaseHealthCheck', result: { healthy: true, message: 'Database connected' } },
    { name: 'memoryHealthCheck', result: { healthy: false, message: 'Memory usage high' } },
    { name: 'diskSpaceHealthCheck', result: { healthy: true } },
  ];

  const mockOnSelectCheck = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders health check list', () => {
    render(
      <MetricHealthList
        checks={mockChecks}
        selectedCheck={null}
        onSelectCheck={mockOnSelectCheck}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText('Thread Deadlock')).toBeInTheDocument();
    expect(screen.getByText('Database')).toBeInTheDocument();
    expect(screen.getByText('Memory')).toBeInTheDocument();
    expect(screen.getByText('Disk Space')).toBeInTheDocument();
  });

  it('displays healthy/unhealthy counts', () => {
    render(
      <MetricHealthList
        checks={mockChecks}
        selectedCheck={null}
        onSelectCheck={mockOnSelectCheck}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText('3 healthy')).toBeInTheDocument();
    expect(screen.getByText('1 unhealthy')).toBeInTheDocument();
  });

  it('sorts unhealthy checks first', () => {
    render(
      <MetricHealthList
        checks={mockChecks}
        selectedCheck={null}
        onSelectCheck={mockOnSelectCheck}
      />,
      { wrapper: TestWrapper }
    );

    const buttons = screen.getAllByRole('button');
    // First button (after filter) should be Memory (unhealthy)
    expect(buttons[0]).toHaveTextContent('Memory');
  });

  it('highlights selected check', () => {
    render(
      <MetricHealthList
        checks={mockChecks}
        selectedCheck="databaseHealthCheck"
        onSelectCheck={mockOnSelectCheck}
      />,
      { wrapper: TestWrapper }
    );

    const databaseButton = screen.getByRole('button', { name: /database/i });
    expect(databaseButton).toHaveAttribute('aria-selected', 'true');
  });

  it('calls onSelectCheck when check is clicked', () => {
    render(
      <MetricHealthList
        checks={mockChecks}
        selectedCheck={null}
        onSelectCheck={mockOnSelectCheck}
      />,
      { wrapper: TestWrapper }
    );

    const databaseButton = screen.getByRole('button', { name: /database/i });
    fireEvent.click(databaseButton);

    expect(mockOnSelectCheck).toHaveBeenCalledWith('databaseHealthCheck');
  });

  it('filters checks by search input', () => {
    render(
      <MetricHealthList
        checks={mockChecks}
        selectedCheck={null}
        onSelectCheck={mockOnSelectCheck}
      />,
      { wrapper: TestWrapper }
    );

    const filterInput = screen.getByPlaceholderText('Filter health checks...');
    fireEvent.change(filterInput, { target: { value: 'memory' } });

    expect(screen.getByText('Memory')).toBeInTheDocument();
    expect(screen.queryByText('Database')).not.toBeInTheDocument();
    expect(screen.queryByText('Thread Deadlock')).not.toBeInTheDocument();
  });

  it('filter is case-insensitive', () => {
    render(
      <MetricHealthList
        checks={mockChecks}
        selectedCheck={null}
        onSelectCheck={mockOnSelectCheck}
      />,
      { wrapper: TestWrapper }
    );

    const filterInput = screen.getByPlaceholderText('Filter health checks...');
    fireEvent.change(filterInput, { target: { value: 'DATABASE' } });

    expect(screen.getByText('Database')).toBeInTheDocument();
  });

  it('shows empty message when no checks match filter', () => {
    render(
      <MetricHealthList
        checks={mockChecks}
        selectedCheck={null}
        onSelectCheck={mockOnSelectCheck}
      />,
      { wrapper: TestWrapper }
    );

    const filterInput = screen.getByPlaceholderText('Filter health checks...');
    fireEvent.change(filterInput, { target: { value: 'nonexistent' } });

    expect(screen.getByText('No matching health checks found')).toBeInTheDocument();
  });

  it('shows empty message when checks array is empty', () => {
    render(
      <MetricHealthList
        checks={[]}
        selectedCheck={null}
        onSelectCheck={mockOnSelectCheck}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText('No health checks available')).toBeInTheDocument();
  });

  it('displays appropriate icons for healthy checks', () => {
    render(
      <MetricHealthList
        checks={[{ name: 'testCheck', result: { healthy: true } }]}
        selectedCheck={null}
        onSelectCheck={mockOnSelectCheck}
      />,
      { wrapper: TestWrapper }
    );

    const healthyIcon = document.querySelector('.metric-health-list__icon--healthy');
    expect(healthyIcon).toBeInTheDocument();
  });

  it('displays appropriate icons for unhealthy checks', () => {
    render(
      <MetricHealthList
        checks={[{ name: 'testCheck', result: { healthy: false } }]}
        selectedCheck={null}
        onSelectCheck={mockOnSelectCheck}
      />,
      { wrapper: TestWrapper }
    );

    const unhealthyIcon = document.querySelector('.metric-health-list__icon--unhealthy');
    expect(unhealthyIcon).toBeInTheDocument();
  });

  it('does not show unknown count when none exist', () => {
    render(
      <MetricHealthList
        checks={mockChecks}
        selectedCheck={null}
        onSelectCheck={mockOnSelectCheck}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.queryByText(/unknown/i)).not.toBeInTheDocument();
  });

  it('formats check names correctly', () => {
    const checksWithCamelCase: HealthCheck[] = [
      { name: 'someComplexHealthCheck', result: { healthy: true } },
    ];

    render(
      <MetricHealthList
        checks={checksWithCamelCase}
        selectedCheck={null}
        onSelectCheck={mockOnSelectCheck}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText('Some Complex')).toBeInTheDocument();
  });

  it('applies custom className', () => {
    const { container } = render(
      <MetricHealthList
        checks={mockChecks}
        selectedCheck={null}
        onSelectCheck={mockOnSelectCheck}
        className="custom-class"
      />,
      { wrapper: TestWrapper }
    );

    expect(container.querySelector('.metric-health-list.custom-class')).toBeInTheDocument();
  });
});
