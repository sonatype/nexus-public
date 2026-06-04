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

import { RepositoriesByFormatPanel } from '../RepositoriesByFormatPanel';

// Test wrapper with Radix Theme
const TestWrapper = ({ children }) => <Theme>{children}</Theme>;

// Sample test data - REAL DATA structure only
const mockData = [
  {
    format: 'Maven',
    formatCode: 'maven2',
    proxyCount: 3,
    hostedCount: 2,
    groupCount: 1,
    totalCount: 6,
    onlineCount: 6,
    offlineCount: 0,
    malwareCountsAvailable: true,
    malwareCount: 0,
    hcEnabledProxyCount: 2,
  },
  {
    format: 'npm',
    formatCode: 'npm',
    proxyCount: 1,
    hostedCount: 1,
    groupCount: 1,
    totalCount: 3,
    onlineCount: 2,
    offlineCount: 1,
    malwareCountsAvailable: true,
    malwareCount: 3,
    hcEnabledProxyCount: 1,
  },
  {
    format: 'Docker',
    formatCode: 'docker',
    proxyCount: 2,
    hostedCount: 0,
    groupCount: 0,
    totalCount: 2,
    onlineCount: 2,
    offlineCount: 0,
    malwareCountsAvailable: true,
    malwareCount: 0,
    hcEnabledProxyCount: 0,
  },
];

describe('RepositoriesByFormatPanel', () => {
  it('renders with data', () => {
    render(
      <TestWrapper>
        <RepositoriesByFormatPanel data={mockData} />
      </TestWrapper>
    );

    expect(screen.getByText('Repositories by Format')).toBeInTheDocument();
    expect(screen.getByText('Maven')).toBeInTheDocument();
    expect(screen.getByText('npm')).toBeInTheDocument();
    expect(screen.getByText('Docker')).toBeInTheDocument();
  });

  it('renders loading state with skeleton table', () => {
    const { container } = render(
      <TestWrapper>
        <RepositoriesByFormatPanel data={[]} loading={true} />
      </TestWrapper>
    );

    // Loading shows skeleton table with headers, not text message
    expect(screen.getByText('Repositories by Format')).toBeInTheDocument();
    expect(container.querySelector('.repos-by-format-panel--loading')).toBeInTheDocument();
  });

  it('renders error state', () => {
    const onRetry = jest.fn();
    render(
      <TestWrapper>
        <RepositoriesByFormatPanel
          data={[]}
          error="Failed to fetch repositories"
          onRetry={onRetry}
        />
      </TestWrapper>
    );

    expect(screen.getByText('Failed to fetch repositories')).toBeInTheDocument();
    expect(screen.getByText('Retry')).toBeInTheDocument();
    
    fireEvent.click(screen.getByText('Retry'));
    expect(onRetry).toHaveBeenCalledTimes(1);
  });

  it('renders empty state', () => {
    render(
      <TestWrapper>
        <RepositoriesByFormatPanel data={[]} />
      </TestWrapper>
    );

    expect(screen.getByText('No repositories found')).toBeInTheDocument();
  });

  it('displays correct column headers', () => {
    render(
      <TestWrapper>
        <RepositoriesByFormatPanel data={mockData} />
      </TestWrapper>
    );

    expect(screen.getByText('Format')).toBeInTheDocument();
    expect(screen.getByText('Proxy')).toBeInTheDocument();
    expect(screen.getByText('Hosted')).toBeInTheDocument();
    expect(screen.getByText('Group')).toBeInTheDocument();
    expect(screen.getByText('Total')).toBeInTheDocument();
    expect(screen.getByText('Threats')).toBeInTheDocument();
    expect(screen.getByText('Online / Offline')).toBeInTheDocument();
  });

  it('shows online/offline counts in Online / Offline column (not separate Online/Offline labels)', () => {
    render(
      <TestWrapper>
        <RepositoriesByFormatPanel data={mockData} />
      </TestWrapper>
    );

    expect(screen.queryByText('Online')).not.toBeInTheDocument();
    expect(screen.queryByText('Offline')).not.toBeInTheDocument();
    expect(screen.getAllByText('/').length).toBeGreaterThanOrEqual(3);
  });

  it('calls onViewRepos with format code when row is clicked', () => {
    const onViewRepos = jest.fn();
    render(
      <TestWrapper>
        <RepositoriesByFormatPanel data={mockData} onViewRepos={onViewRepos} />
      </TestWrapper>
    );

    // Component uses row click - find the Maven row by its format badge and click it
    const mavenRow = screen.getByText('Maven').closest('tr');
    fireEvent.click(mavenRow);

    expect(onViewRepos).toHaveBeenCalledWith('maven2');
  });

  it('displays repository counts correctly', () => {
    render(
      <TestWrapper>
        <RepositoriesByFormatPanel data={mockData} />
      </TestWrapper>
    );

    // Maven: proxy=3, hosted=2, group=1, total=6
    // These numbers should appear in the table
    const threes = screen.getAllByText('3');
    expect(threes.length).toBeGreaterThan(0);

    // Total column may duplicate counts with Status column (e.g. online/offline)
    expect(screen.getAllByText('6').length).toBeGreaterThanOrEqual(1);
  });
});

