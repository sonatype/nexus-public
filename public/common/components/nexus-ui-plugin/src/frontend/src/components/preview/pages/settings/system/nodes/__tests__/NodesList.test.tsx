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
import { NodesList } from '../NodesList';
import { NodeInfo } from '../types';

const mockUseNodes = jest.fn();
jest.mock('../useNodes', () => ({ useNodes: () => mockUseNodes() }));

function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

const mockNodes: NodeInfo[] = [
  { name: 'node-1', displayName: 'Primary Node', local: true },
  { name: 'node-2', displayName: 'Secondary Node', local: false },
  { name: 'node-3', displayName: '', local: false },
];

function setNodesState(overrides: Partial<ReturnType<typeof mockUseNodes>> = {}) {
  mockUseNodes.mockReturnValue({
    nodes: mockNodes,
    loading: false,
    error: null,
    refresh: jest.fn(),
    retry: jest.fn(),
    ...overrides,
  });
}

describe('NodesList', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    setNodesState();
  });

  it('renders the nodes list with display names', () => {
    render(<NodesList />, { wrapper: TestWrapper });
    expect(screen.getByText('Primary Node')).toBeInTheDocument();
    expect(screen.getByText('Secondary Node')).toBeInTheDocument();
  });

  it('displays node identities in the table', () => {
    render(<NodesList />, { wrapper: TestWrapper });
    expect(screen.getByText('node-1')).toBeInTheDocument();
    expect(screen.getByText('node-2')).toBeInTheDocument();
  });

  it('uses node name as fallback when displayName is empty', () => {
    render(<NodesList />, { wrapper: TestWrapper });
    // node-3 has empty displayName -> name appears in both the Name and Identity columns
    expect(screen.getAllByText('node-3')).toHaveLength(2);
  });

  it('shows empty state when no nodes exist', () => {
    setNodesState({ nodes: [] });
    render(<NodesList />, { wrapper: TestWrapper });
    expect(screen.getByText(/no nodes/i)).toBeInTheDocument();
  });

  it('shows loading state while fetching nodes', () => {
    setNodesState({ loading: true, nodes: [] });
    render(<NodesList />, { wrapper: TestWrapper });
    expect(screen.getByText(/loading/i)).toBeInTheDocument();
  });

  it('shows error state when fetch fails', () => {
    setNodesState({ error: 'Failed to load', nodes: [] });
    render(<NodesList />, { wrapper: TestWrapper });
    expect(screen.getByText(/failed to load/i)).toBeInTheDocument();
  });

  it('displays only Node Name and Node Identity column headers', () => {
    render(<NodesList />, { wrapper: TestWrapper });
    expect(screen.getByRole('columnheader', { name: 'Node Name' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Node Identity' })).toBeInTheDocument();
    expect(screen.queryByRole('columnheader', { name: 'Status' })).not.toBeInTheDocument();
  });

  it('shows Current Node badge only for the local node', () => {
    render(<NodesList />, { wrapper: TestWrapper });
    expect(screen.getAllByText('Current Node')).toHaveLength(1);
  });

  it('does not show Remote or Local (Current) badges', () => {
    render(<NodesList />, { wrapper: TestWrapper });
    expect(screen.queryByText('Remote')).not.toBeInTheDocument();
    expect(screen.queryByText('Local (Current)')).not.toBeInTheDocument();
  });

  it('shows displayName as node name when it differs from the identity', () => {
    setNodesState({
      nodes: [
        { name: 'uuid-1234', displayName: 'my-macbook.local', local: true },
        { name: 'uuid-5678', displayName: 'remote-host.local', local: false },
      ],
    });
    render(<NodesList />, { wrapper: TestWrapper });
    expect(screen.getByText('my-macbook.local')).toBeInTheDocument();
    expect(screen.getByText('remote-host.local')).toBeInTheDocument();
    expect(screen.getByText('uuid-1234')).toBeInTheDocument();
    expect(screen.getByText('uuid-5678')).toBeInTheDocument();
  });
});
