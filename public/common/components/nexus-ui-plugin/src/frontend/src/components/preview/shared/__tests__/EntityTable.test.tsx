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
import { EntityTable, TableColumn } from '../EntityTable';

const renderWithTheme = (ui: React.ReactElement) => {
  return render(<Theme>{ui}</Theme>);
};

interface TestItem {
  id: string;
  name: string;
  type: string;
  status: string;
}

describe('EntityTable', () => {
  const testData: TestItem[] = [
    { id: '1', name: 'maven-central', type: 'proxy', status: 'online' },
    { id: '2', name: 'npm-hosted', type: 'hosted', status: 'online' },
    { id: '3', name: 'docker-hub', type: 'proxy', status: 'offline' },
  ];

  const columns: TableColumn<TestItem>[] = [
    { id: 'name', header: 'Name', accessor: 'name', sortable: true },
    { id: 'type', header: 'Type', accessor: 'type' },
    { id: 'status', header: 'Status', accessor: (item) => item.status.toUpperCase() },
  ];

  it('renders table with data', () => {
    renderWithTheme(
      <EntityTable
        data={testData}
        columns={columns}
        getRowKey={(item) => item.id}
      />
    );

    expect(screen.getByText('Name')).toBeInTheDocument();
    expect(screen.getByText('Type')).toBeInTheDocument();
    expect(screen.getByText('maven-central')).toBeInTheDocument();
    expect(screen.getByText('npm-hosted')).toBeInTheDocument();
  });

  it('renders loading state', () => {
    renderWithTheme(
      <EntityTable
        data={[]}
        columns={columns}
        getRowKey={(item) => item.id}
        loading
        loadingMessage="Loading repositories..."
      />
    );

    expect(screen.getByText('Loading repositories...')).toBeInTheDocument();
  });

  it('renders error state', () => {
    const mockOnRetry = jest.fn();

    renderWithTheme(
      <EntityTable
        data={[]}
        columns={columns}
        getRowKey={(item) => item.id}
        error="Failed to load data"
        onRetry={mockOnRetry}
      />
    );

    expect(screen.getByText('Failed to load data')).toBeInTheDocument();
    
    const retryButton = screen.getByRole('button', { name: /try again/i });
    fireEvent.click(retryButton);
    expect(mockOnRetry).toHaveBeenCalled();
  });

  it('renders empty state', () => {
    renderWithTheme(
      <EntityTable
        data={[]}
        columns={columns}
        getRowKey={(item) => item.id}
      />
    );

    expect(screen.getByText('No data available')).toBeInTheDocument();
  });

  it('renders custom empty state', () => {
    renderWithTheme(
      <EntityTable
        data={[]}
        columns={columns}
        getRowKey={(item) => item.id}
        emptyState={<div>Custom empty message</div>}
      />
    );

    expect(screen.getByText('Custom empty message')).toBeInTheDocument();
  });

  it('calls onRowClick when row is clicked', () => {
    const mockOnRowClick = jest.fn();

    renderWithTheme(
      <EntityTable
        data={testData}
        columns={columns}
        getRowKey={(item) => item.id}
        onRowClick={mockOnRowClick}
      />
    );

    fireEvent.click(screen.getByText('maven-central'));
    expect(mockOnRowClick).toHaveBeenCalledWith(testData[0]);
  });

  it('calls onRowClick on Enter key press', () => {
    const mockOnRowClick = jest.fn();

    renderWithTheme(
      <EntityTable
        data={testData}
        columns={columns}
        getRowKey={(item) => item.id}
        onRowClick={mockOnRowClick}
      />
    );

    const row = screen.getByText('maven-central').closest('tr');
    if (row) {
      fireEvent.keyDown(row, { key: 'Enter' });
      expect(mockOnRowClick).toHaveBeenCalledWith(testData[0]);
    }
  });

  it('calls onSort when sortable header is clicked', () => {
    const mockOnSort = jest.fn();

    renderWithTheme(
      <EntityTable
        data={testData}
        columns={columns}
        getRowKey={(item) => item.id}
        onSort={mockOnSort}
      />
    );

    fireEvent.click(screen.getByText('Name'));
    expect(mockOnSort).toHaveBeenCalledWith('name');
  });

  it('does not call onSort for non-sortable columns', () => {
    const mockOnSort = jest.fn();

    renderWithTheme(
      <EntityTable
        data={testData}
        columns={columns}
        getRowKey={(item) => item.id}
        onSort={mockOnSort}
      />
    );

    fireEvent.click(screen.getByText('Type'));
    expect(mockOnSort).not.toHaveBeenCalled();
  });

  it('shows sort indicator for sorted column', () => {
    renderWithTheme(
      <EntityTable
        data={testData}
        columns={columns}
        getRowKey={(item) => item.id}
        sortBy="name"
        sortDirection="asc"
      />
    );

    // Should have ascending sort indicator
    const sortedHeader = screen.getByText('Name').closest('th');
    expect(sortedHeader).toHaveAttribute('aria-sort', 'ascending');
  });

  it('hides row arrow when showRowArrow is false', () => {
    const { container } = renderWithTheme(
      <EntityTable
        data={testData}
        columns={columns}
        getRowKey={(item) => item.id}
        showRowArrow={false}
      />
    );

    expect(container.querySelector('.entity-table__arrow')).not.toBeInTheDocument();
  });

  it('renders custom accessor function', () => {
    renderWithTheme(
      <EntityTable
        data={testData}
        columns={columns}
        getRowKey={(item) => item.id}
      />
    );

    // Status column uses accessor function to uppercase (multiple items have 'ONLINE' status)
    const onlineElements = screen.getAllByText('ONLINE');
    expect(onlineElements.length).toBeGreaterThanOrEqual(1);
  });

  it('applies custom className', () => {
    const { container } = renderWithTheme(
      <EntityTable
        data={testData}
        columns={columns}
        getRowKey={(item) => item.id}
        className="custom-table"
      />
    );

    expect(container.querySelector('.custom-table')).toBeInTheDocument();
  });

  it('has accessible aria-label', () => {
    const { container } = renderWithTheme(
      <EntityTable
        data={testData}
        columns={columns}
        getRowKey={(item) => item.id}
        ariaLabel="Repository list"
      />
    );

    // Radix UI Table wraps content in divs, so we query by aria-label directly
    const table = container.querySelector('[aria-label="Repository list"]');
    expect(table).toBeInTheDocument();
  });

  it('makes rows non-clickable when clickable is false', () => {
    const mockOnRowClick = jest.fn();

    renderWithTheme(
      <EntityTable
        data={testData}
        columns={columns}
        getRowKey={(item) => item.id}
        onRowClick={mockOnRowClick}
        clickable={false}
      />
    );

    fireEvent.click(screen.getByText('maven-central'));
    expect(mockOnRowClick).not.toHaveBeenCalled();
  });
});


