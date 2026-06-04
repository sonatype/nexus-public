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

import { NodeSelector } from '../NodeSelector';

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

describe('NodeSelector', () => {
  const mockNodes = [
    { nodeId: 'node-1', friendlyName: 'Node 1', hostname: 'host1.example.com', local: true },
    { nodeId: 'node-2', friendlyName: 'Node 2', hostname: 'host2.example.com', local: false },
    { nodeId: 'node-3', hostname: 'host3.example.com', local: false },
  ];

  const mockOnNodeSelect = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders nothing when there is only one node', () => {
    const { container } = render(
      <NodeSelector
        nodes={[mockNodes[0]]}
        selectedNode="node-1"
        onNodeSelect={mockOnNodeSelect}
      />,
      { wrapper: TestWrapper }
    );

    expect(container.querySelector('.node-selector')).toBeNull();
  });

  it('renders nothing when there are no nodes', () => {
    const { container } = render(
      <NodeSelector
        nodes={[]}
        selectedNode={null}
        onNodeSelect={mockOnNodeSelect}
      />,
      { wrapper: TestWrapper }
    );

    expect(container.querySelector('.node-selector')).toBeNull();
  });

  it('renders node selector with multiple nodes', () => {
    render(
      <NodeSelector
        nodes={mockNodes}
        selectedNode="node-1"
        onNodeSelect={mockOnNodeSelect}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText('Select Node')).toBeInTheDocument();
    expect(screen.getByText('Node 1')).toBeInTheDocument();
    expect(screen.getByText('Node 2')).toBeInTheDocument();
    expect(screen.getByText('host3.example.com')).toBeInTheDocument();
  });

  it('uses friendlyName when available', () => {
    render(
      <NodeSelector
        nodes={mockNodes}
        selectedNode="node-1"
        onNodeSelect={mockOnNodeSelect}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText('Node 1')).toBeInTheDocument();
    expect(screen.getByText('Node 2')).toBeInTheDocument();
  });

  it('falls back to hostname when friendlyName is not available', () => {
    render(
      <NodeSelector
        nodes={mockNodes}
        selectedNode="node-1"
        onNodeSelect={mockOnNodeSelect}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText('host3.example.com')).toBeInTheDocument();
  });

  it('falls back to nodeId when neither friendlyName nor hostname is available', () => {
    const nodesWithoutNames = [
      { nodeId: 'node-1' },
      { nodeId: 'node-2' },
    ];

    render(
      <NodeSelector
        nodes={nodesWithoutNames}
        selectedNode="node-1"
        onNodeSelect={mockOnNodeSelect}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByRole('button', { name: /node-1/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /node-2/i })).toBeInTheDocument();
  });

  it('highlights the selected node', () => {
    render(
      <NodeSelector
        nodes={mockNodes}
        selectedNode="node-1"
        onNodeSelect={mockOnNodeSelect}
      />,
      { wrapper: TestWrapper }
    );

    const selectedButton = screen.getByRole('button', { name: /node 1/i });
    expect(selectedButton).toHaveAttribute('aria-pressed', 'true');
  });

  it('calls onNodeSelect when a node is clicked', () => {
    render(
      <NodeSelector
        nodes={mockNodes}
        selectedNode="node-1"
        onNodeSelect={mockOnNodeSelect}
      />,
      { wrapper: TestWrapper }
    );

    const node2Button = screen.getByRole('button', { name: /node 2/i });
    fireEvent.click(node2Button);

    expect(mockOnNodeSelect).toHaveBeenCalledWith('node-2');
  });

  it('shows local badge for local node', () => {
    render(
      <NodeSelector
        nodes={mockNodes}
        selectedNode="node-1"
        onNodeSelect={mockOnNodeSelect}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText('local')).toBeInTheDocument();
  });

  it('does not show local badge for non-local nodes', () => {
    render(
      <NodeSelector
        nodes={mockNodes}
        selectedNode="node-2"
        onNodeSelect={mockOnNodeSelect}
      />,
      { wrapper: TestWrapper }
    );

    // Should only have one 'local' badge (for node-1)
    const localBadges = screen.getAllByText('local');
    expect(localBadges.length).toBe(1);
  });

  it('applies custom className', () => {
    const { container } = render(
      <NodeSelector
        nodes={mockNodes}
        selectedNode="node-1"
        onNodeSelect={mockOnNodeSelect}
        className="custom-class"
      />,
      { wrapper: TestWrapper }
    );

    const nodeSelector = container.querySelector('.node-selector');
    expect(nodeSelector).toHaveClass('node-selector', 'custom-class');
  });
});

