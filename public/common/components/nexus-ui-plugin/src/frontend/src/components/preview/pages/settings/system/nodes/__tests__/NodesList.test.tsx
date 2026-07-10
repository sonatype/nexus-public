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
import { render, screen, waitFor } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { NodesList } from '../NodesList';
import { NodeInfo } from '../types';

// Mock useNodesApi
const mockFetchNodes = jest.fn();

jest.mock('../useNodesApi', () => ({
  useNodesApi: () => ({
    loading: false,
    error: null,
    fetchNodes: mockFetchNodes,
  }),
}));

function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

const mockNodes: NodeInfo[] = [
  {
    name: 'node-1',
    displayName: 'Primary Node',
    local: true,
  },
  {
    name: 'node-2',
    displayName: 'Secondary Node',
    local: false,
  },
  {
    name: 'node-3',
    displayName: '', // Empty to test fallback
    local: false,
  },
];

describe('NodesList', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockFetchNodes.mockResolvedValue(mockNodes);
  });

  it('renders the nodes list with display names', async () => {
    render(<NodesList refreshKey={0} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Primary Node')).toBeInTheDocument();
      expect(screen.getByText('Secondary Node')).toBeInTheDocument();
    });
  });

  it('displays node names in table', async () => {
    render(<NodesList refreshKey={0} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('node-1')).toBeInTheDocument();
      expect(screen.getByText('node-2')).toBeInTheDocument();
    });
  });

  it('uses node name as fallback when displayName is empty', async () => {
    render(<NodesList refreshKey={0} />, { wrapper: TestWrapper });

    await waitFor(() => {
      // Node 3 has empty displayName, so node name appears in both columns:
      // - Node Name column (as fallback)
      // - Node Identity column (as the identity)
      const node3Elements = screen.getAllByText('node-3');
      expect(node3Elements).toHaveLength(2);
    });
  });

  it('shows empty state when no nodes exist', async () => {
    mockFetchNodes.mockResolvedValue([]);

    render(<NodesList refreshKey={0} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText(/no nodes/i)).toBeInTheDocument();
    });
  });

  it('shows single node for non-clustered environment', async () => {
    mockFetchNodes.mockResolvedValue([mockNodes[0]]);

    render(<NodesList refreshKey={0} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Primary Node')).toBeInTheDocument();
    });
  });

  it('shows loading state while fetching nodes', async () => {
    mockFetchNodes.mockImplementation(
      () => new Promise((resolve) => setTimeout(() => resolve(mockNodes), 100))
    );

    render(<NodesList refreshKey={0} />, { wrapper: TestWrapper });

    expect(screen.getByText(/loading/i)).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('Primary Node')).toBeInTheDocument();
    });
  });

  it('shows error state when fetch fails', async () => {
    mockFetchNodes.mockRejectedValue(new Error('Failed to load'));

    render(<NodesList refreshKey={0} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText(/failed to load/i)).toBeInTheDocument();
    });
  });

  it('refreshes list when refreshKey changes', async () => {
    const { rerender } = render(<NodesList refreshKey={0} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(mockFetchNodes).toHaveBeenCalledTimes(1);
    });

    rerender(
      <TestWrapper>
        <NodesList refreshKey={1} />
      </TestWrapper>
    );

    await waitFor(() => {
      expect(mockFetchNodes).toHaveBeenCalledTimes(2);
    });
  });

  it('displays column headers for Node Name and Node Identity only', async () => {
    render(<NodesList refreshKey={0} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Primary Node')).toBeInTheDocument();
    });

    expect(screen.getByRole('columnheader', { name: 'Node Name' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Node Identity' })).toBeInTheDocument();
    expect(screen.queryByRole('columnheader', { name: 'Status' })).not.toBeInTheDocument();
  });

  it('shows Current Node badge only for the local node', async () => {
    render(<NodesList refreshKey={0} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Primary Node')).toBeInTheDocument();
    });

    // Only node-1 (local) gets the badge
    const currentNodeBadges = screen.getAllByText('Current Node');
    expect(currentNodeBadges).toHaveLength(1);
  });

  it('does not show Current Node badge for remote nodes', async () => {
    render(<NodesList refreshKey={0} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Secondary Node')).toBeInTheDocument();
    });

    // Remote nodes should not have a Current Node badge
    const currentNodeBadges = screen.queryAllByText('Current Node');
    expect(currentNodeBadges).toHaveLength(1); // only the local node
  });

  it('does not show Remote or Local (Current) badges', async () => {
    render(<NodesList refreshKey={0} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Primary Node')).toBeInTheDocument();
    });

    expect(screen.queryByText('Remote')).not.toBeInTheDocument();
    expect(screen.queryByText('Local (Current)')).not.toBeInTheDocument();
  });

  it('shows displayName as node name when it differs from name', async () => {
    const hostnameNodes: NodeInfo[] = [
      { name: 'uuid-1234', displayName: 'my-macbook.local', local: true },
      { name: 'uuid-5678', displayName: 'remote-host.local', local: false },
    ];
    mockFetchNodes.mockResolvedValue(hostnameNodes);

    render(<NodesList refreshKey={0} />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('my-macbook.local')).toBeInTheDocument();
      expect(screen.getByText('remote-host.local')).toBeInTheDocument();
    });

    // Node Identity column still shows UUID
    expect(screen.getByText('uuid-1234')).toBeInTheDocument();
    expect(screen.getByText('uuid-5678')).toBeInTheDocument();
  });
});
