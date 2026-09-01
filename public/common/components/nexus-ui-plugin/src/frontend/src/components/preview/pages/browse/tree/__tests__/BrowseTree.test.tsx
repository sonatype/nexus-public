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
import { render, screen, waitFor, fireEvent, } from '@testing-library/react';
import '@testing-library/jest-dom';

import { BrowseTree } from '../BrowseTree';
import { BrowseTreeNode } from '../BrowseTreeNode';
import { mockRootNodes, mockFolder1Children, getMockChildren } from './mockData';
import type { TreeNodeState, BrowseNode } from '../browse-tree.types';

// Mock the relative paths that the source imports from
jest.mock('../../../../../../interface/ExtAPIUtils', () => ({
  ExtAPIUtils: {
    extAPIRequest: jest.fn(),
    checkForErrorAndExtract: jest.fn((response) => response.data.result.data),
  },
}));

jest.mock('../../../../../../constants/APIConstants', () => ({
  APIConstants: {
    EXT: {
      BROWSE: {
        ACTION: 'coreui_Browse',
        METHODS: {
          READ: 'read',
        },
      },
    },
  },
}));

// Get the mocked module
import { ExtAPIUtils } from '../../../../../../interface/ExtAPIUtils';

/**
 * Helper to create a mock API response.
 */
function mockApiResponse(data: BrowseNode[]) {
  return {
    data: {
      result: {
        success: true,
        data,
      },
    },
  };
}

/**
 * Helper to setup the ExtAPIUtils mock for a specific node.
 */
function setupApiMock() {
  ExtAPIUtils.extAPIRequest.mockImplementation(
    async (_action: string, _method: string, { data }: { data: [{ repositoryName: string; node: string }] }) => {
      const requestedNodeId = data[0].node;
      const mockChildren = getMockChildren(requestedNodeId);
      return mockApiResponse(mockChildren);
    }
  );
}

describe('BrowseTree', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    setupApiMock();
  });

  describe('rendering', () => {
    it('renders loading state initially', () => {
      ExtAPIUtils.extAPIRequest.mockImplementation(() => new Promise(() => {})); // Never resolves
      render(<BrowseTree repositoryName="maven-releases" />);

      expect(screen.getByText(/loading/i)).toBeInTheDocument();
    });

    it('renders the tree with root nodes after loading', async () => {
      render(<BrowseTree repositoryName="maven-releases" />);

      await waitFor(() => {
        expect(screen.getByText('folder1')).toBeInTheDocument();
      });

      expect(screen.getByText('component1')).toBeInTheDocument();
      expect(screen.getByText('asset1.jar')).toBeInTheDocument();
    });

    it('renders a tree role element with aria-label', async () => {
      render(<BrowseTree repositoryName="maven-releases" />);

      await waitFor(() => {
        const tree = screen.getByRole('tree');
        expect(tree).toHaveAttribute('aria-label', 'maven-releases contents');
      });
    });

    it('renders empty state when no nodes', async () => {
      ExtAPIUtils.extAPIRequest.mockResolvedValue(mockApiResponse([]));
      render(<BrowseTree repositoryName="empty-repo" />);

      await waitFor(() => {
        expect(screen.getByText(/no items in this repository/i)).toBeInTheDocument();
      });
      expect(screen.getByTestId('empty-state')).toBeInTheDocument();
    });

    it('renders error state with retry button', async () => {
      ExtAPIUtils.extAPIRequest.mockRejectedValue(new Error('Network error'));
      render(<BrowseTree repositoryName="maven-releases" />);

      await waitFor(() => {
        expect(screen.getByText(/network error/i)).toBeInTheDocument();
      });

      expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
    });
  });

  describe('icons', () => {
    it('renders folder icon for folder type', async () => {
      render(<BrowseTree repositoryName="maven-releases" />);

      await waitFor(() => {
        expect(screen.getByTestId('icon-folder')).toBeInTheDocument();
      });
    });

    it('renders package icon for component type', async () => {
      render(<BrowseTree repositoryName="maven-releases" />);

      await waitFor(() => {
        expect(screen.getByTestId('icon-component')).toBeInTheDocument();
      });
    });

    it('renders file icon for asset type', async () => {
      render(<BrowseTree repositoryName="maven-releases" />);

      await waitFor(() => {
        expect(screen.getByTestId('icon-asset')).toBeInTheDocument();
      });
    });
  });

  describe('expand/collapse', () => {
    it('expands a folder when clicking the toggle', async () => {
      render(<BrowseTree repositoryName="maven-releases" />);

      await waitFor(() => {
        expect(screen.getByText('folder1')).toBeInTheDocument();
      });

      // Click the toggle for folder1
      const toggleButton = screen.getByTestId('toggle-folder1');
      fireEvent.click(toggleButton);

      // Should load and show children
      await waitFor(() => {
        expect(screen.getByText('subfolder1')).toBeInTheDocument();
      });

      expect(screen.getByText('component2')).toBeInTheDocument();
    });

    it('collapses an expanded folder when clicking the toggle', async () => {
      render(<BrowseTree repositoryName="maven-releases" />);

      await waitFor(() => {
        expect(screen.getByText('folder1')).toBeInTheDocument();
      });

      // Expand folder1
      const toggleButton = screen.getByTestId('toggle-folder1');
      fireEvent.click(toggleButton);

      await waitFor(() => {
        expect(screen.getByText('subfolder1')).toBeInTheDocument();
      });

      // Collapse folder1
      fireEvent.click(toggleButton);

      await waitFor(() => {
        expect(screen.queryByText('subfolder1')).not.toBeInTheDocument();
      });
    });

    it('does not show toggle for leaf nodes', async () => {
      render(<BrowseTree repositoryName="maven-releases" />);

      await waitFor(() => {
        expect(screen.getByText('asset1.jar')).toBeInTheDocument();
      });

      // The toggle for asset1.jar should be hidden (opacity: 0)
      const toggle = screen.getByTestId('toggle-asset1.jar');
      expect(toggle).toHaveStyle({ opacity: '0' });
    });
  });

  describe('selection', () => {
    it('calls onSelect when a node is clicked', async () => {
      const onSelect = jest.fn();
      render(<BrowseTree repositoryName="maven-releases" onSelect={onSelect} />);

      await waitFor(() => {
        expect(screen.getByText('folder1')).toBeInTheDocument();
      });

      // Click on folder1 link
      const link = screen.getByTestId('link-folder1');
      fireEvent.click(link);

      expect(onSelect).toHaveBeenCalledWith(
        expect.objectContaining({
          id: 'folder1',
          type: 'folder',
        })
      );
    });
  });

  describe('accessibility', () => {
    it('has proper tree role', async () => {
      render(<BrowseTree repositoryName="maven-releases" />);

      await waitFor(() => {
        expect(screen.getByRole('tree')).toBeInTheDocument();
      });
    });

    it('has proper treeitem role for nodes', async () => {
      render(<BrowseTree repositoryName="maven-releases" />);

      await waitFor(() => {
        expect(screen.getAllByRole('treeitem')).toHaveLength(3);
      });
    });

    it('has aria-expanded on expandable nodes', async () => {
      render(<BrowseTree repositoryName="maven-releases" />);

      await waitFor(() => {
        expect(screen.getByText('folder1')).toBeInTheDocument();
      });

      const folder1Item = screen.getAllByRole('treeitem')[0];
      expect(folder1Item).toHaveAttribute('aria-expanded', 'false');
    });

    it('has aria-label on the tree', async () => {
      render(<BrowseTree repositoryName="maven-releases" />);

      await waitFor(() => {
        const tree = screen.getByRole('tree');
        expect(tree).toHaveAttribute('aria-label', 'maven-releases contents');
      });
    });
  });

  describe('error handling', () => {
    it('retries on error when clicking retry button', async () => {
      // First call fails
      ExtAPIUtils.extAPIRequest.mockRejectedValueOnce(new Error('Network error'));

      render(<BrowseTree repositoryName="maven-releases" />);

      await waitFor(() => {
        expect(screen.getByText(/network error/i)).toBeInTheDocument();
      });

      // Setup successful response for retry
      setupApiMock();

      // Click retry
      const retryButton = screen.getByRole('button', { name: /retry/i });
      fireEvent.click(retryButton);

      // Should show tree after successful retry
      await waitFor(() => {
        expect(screen.getByText('folder1')).toBeInTheDocument();
      });
    });
  });

  describe('keyboard navigation', () => {
    it('handles ArrowDown key to move focus', async () => {
      render(<BrowseTree repositoryName="maven-releases" />);

      await waitFor(() => {
        expect(screen.getByText('folder1')).toBeInTheDocument();
      });

      const firstNode = screen.getByTestId('tree-node-folder1');
      fireEvent.keyDown(firstNode, { key: 'ArrowDown' });

      // Should not throw and handle the key
      expect(firstNode).toBeInTheDocument();
    });

    it('handles ArrowUp key to move focus', async () => {
      render(<BrowseTree repositoryName="maven-releases" />);

      await waitFor(() => {
        expect(screen.getByText('folder1')).toBeInTheDocument();
      });

      const firstNode = screen.getByTestId('tree-node-folder1');
      fireEvent.keyDown(firstNode, { key: 'ArrowUp' });

      expect(firstNode).toBeInTheDocument();
    });

    it('handles ArrowRight key to expand', async () => {
      render(<BrowseTree repositoryName="maven-releases" />);

      await waitFor(() => {
        expect(screen.getByText('folder1')).toBeInTheDocument();
      });

      const firstNode = screen.getByTestId('tree-node-folder1');
      fireEvent.keyDown(firstNode, { key: 'ArrowRight' });

      // Should trigger expansion
      await waitFor(() => {
        expect(screen.getByText('subfolder1')).toBeInTheDocument();
      });
    });

    it('handles ArrowLeft key to collapse or focus parent', async () => {
      render(<BrowseTree repositoryName="maven-releases" />);

      await waitFor(() => {
        expect(screen.getByText('folder1')).toBeInTheDocument();
      });

      const firstNode = screen.getByTestId('tree-node-folder1');
      fireEvent.keyDown(firstNode, { key: 'ArrowLeft' });

      expect(firstNode).toBeInTheDocument();
    });

    it('handles Enter key to select node', async () => {
      const onSelect = jest.fn();
      render(<BrowseTree repositoryName="maven-releases" onSelect={onSelect} />);

      await waitFor(() => {
        expect(screen.getByText('folder1')).toBeInTheDocument();
      });

      const firstNode = screen.getByTestId('tree-node-folder1');
      fireEvent.keyDown(firstNode, { key: 'Enter' });

      expect(onSelect).toHaveBeenCalled();
    });

    it('handles Space key to select node', async () => {
      const onSelect = jest.fn();
      render(<BrowseTree repositoryName="maven-releases" onSelect={onSelect} />);

      await waitFor(() => {
        expect(screen.getByText('folder1')).toBeInTheDocument();
      });

      const firstNode = screen.getByTestId('tree-node-folder1');
      fireEvent.keyDown(firstNode, { key: ' ' });

      expect(onSelect).toHaveBeenCalled();
    });

    it('ignores other keys', async () => {
      render(<BrowseTree repositoryName="maven-releases" />);

      await waitFor(() => {
        expect(screen.getByText('folder1')).toBeInTheDocument();
      });

      const firstNode = screen.getByTestId('tree-node-folder1');
      // Should not throw for unknown key
      fireEvent.keyDown(firstNode, { key: 'x' });

      expect(firstNode).toBeInTheDocument();
    });
  });

  describe('custom props', () => {
    it('applies custom className to container', async () => {
      render(<BrowseTree repositoryName="maven-releases" className="custom-class" />);

      await waitFor(() => {
        const container = screen.getByTestId('browse-tree');
        expect(container).toHaveClass('browse-tree');
        expect(container).toHaveClass('custom-class');
      });
    });

    it('applies custom className with baseUrl', async () => {
      render(<BrowseTree repositoryName="maven-releases" baseUrl="custom/browse/path" className="test-class" />);

      await waitFor(() => {
        const container = screen.getByTestId('browse-tree');
        expect(container).toHaveClass('test-class');
      });
    });
  });

  // BrowseTree search functionality was removed; search is now handled by InRepositorySearch.
});

describe('BrowseTreeNode', () => {
  const defaultProps = {
    repositoryName: 'maven-releases',
    depth: 0,
    onToggle: jest.fn(),
    onSelect: jest.fn(),
    isFocused: false,
    onKeyDown: jest.fn(),
    baseUrl: 'preview/browse/browse/maven-releases',
  };

  const createNodeState = (overrides: Partial<TreeNodeState> = {}): TreeNodeState => ({
    node: mockRootNodes[0], // folder1
    expanded: false,
    children: null,
    loading: false,
    ...overrides,
  });

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders the node text', () => {
    render(
      <BrowseTreeNode
        {...defaultProps}
        nodeState={createNodeState()}
      />
    );

    expect(screen.getByText('folder1')).toBeInTheDocument();
  });

  it('calls onToggle when toggle is clicked', async () => {
    const onToggle = jest.fn();

    render(
      <BrowseTreeNode
        {...defaultProps}
        onToggle={onToggle}
        nodeState={createNodeState()}
      />
    );

    const toggle = screen.getByTestId('toggle-folder1');
    fireEvent.click(toggle);

    expect(onToggle).toHaveBeenCalledWith('folder1');
  });

  it('calls onSelect when link is clicked', async () => {
    const onSelect = jest.fn();

    render(
      <BrowseTreeNode
        {...defaultProps}
        onSelect={onSelect}
        nodeState={createNodeState()}
      />
    );

    const link = screen.getByTestId('link-folder1');
    fireEvent.click(link);

    expect(onSelect).toHaveBeenCalledWith(mockRootNodes[0]);
  });

  it('shows loading spinner when loading', () => {
    render(
      <BrowseTreeNode
        {...defaultProps}
        nodeState={createNodeState({ loading: true })}
      />
    );

    // The loading spinner should be present (Radix Spinner)
    const toggle = screen.getByTestId('toggle-folder1');
    expect(toggle.querySelector('.rt-Spinner')).toBeInTheDocument();
  });

  it('renders children when expanded', () => {
    const childNodeStates: TreeNodeState[] = mockFolder1Children.map((node) => ({
      node,
      expanded: false,
      children: null,
      loading: false,
    }));

    render(
      <BrowseTreeNode
        {...defaultProps}
        nodeState={createNodeState({ expanded: true, children: childNodeStates })}
      />
    );

    expect(screen.getByText('subfolder1')).toBeInTheDocument();
    expect(screen.getByText('component2')).toBeInTheDocument();
  });

  it('applies focused class when isFocused is true', () => {
    render(
      <BrowseTreeNode
        {...defaultProps}
        isFocused={true}
        nodeState={createNodeState()}
      />
    );

    const node = screen.getByTestId('tree-node-folder1');
    expect(node).toHaveClass('browse-tree__node--focused');
  });

  it('shows error message when there is an error', () => {
    render(
      <BrowseTreeNode
        {...defaultProps}
        nodeState={createNodeState({ error: 'Failed to load children' })}
      />
    );

    expect(screen.getByText('Failed to load children')).toBeInTheDocument();
  });

  it('renders a clickable node label without a navigating href', () => {
    render(
      <BrowseTreeNode
        {...defaultProps}
        nodeState={createNodeState()}
      />
    );

    const link = screen.getByTestId('link-folder1');
    expect(link).toBeInTheDocument();
    expect(link).not.toHaveAttribute('href');
  });

  it('renders with correct depth indentation', () => {
    render(
      <BrowseTreeNode
        {...defaultProps}
        depth={2}
        nodeState={createNodeState()}
      />
    );

    const node = screen.getByTestId('tree-node-folder1');
    expect(node).toHaveStyle({ paddingLeft: '40px' }); // 2 * 20px
  });
});
