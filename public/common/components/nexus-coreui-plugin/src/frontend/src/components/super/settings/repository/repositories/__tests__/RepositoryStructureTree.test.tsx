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
import { TooltipProvider } from '@radix-ui/themes';
import '@testing-library/jest-dom';
import { RepositoryStructureTree } from '../RepositoryStructureTree';
import { useRepositoryTree } from '../useRepositoryTree';
import { useStructureComponentSearch } from '../useStructureComponentSearch';

// Mock the hooks
jest.mock('../useRepositoryTree');
jest.mock('../useStructureComponentSearch');

// Mock Radix UI components that might cause issues in Jest
jest.mock('@radix-ui/themes', () => ({
  ...jest.requireActual('@radix-ui/themes'),
  Tooltip: ({ children, content }: any) => <div title={content}>{children}</div>,
  TooltipProvider: ({ children }: any) => <div>{children}</div>,
  ScrollArea: ({ children }: any) => <div>{children}</div>,
  Callout: {
    Root: ({ children }: any) => <div>{children}</div>,
    Icon: ({ children }: any) => <div>{children}</div>,
    Text: ({ children }: any) => <div>{children}</div>,
  },
}));

const wrap = (children: React.ReactNode) => (
  <div>
    {children}
  </div>
);

const mockNodes = [
  {
    id: 'group1',
    name: 'group1',
    type: 'group',
    format: 'maven2',
    status: 'online',
    online: true,
    children: [
      {
        id: 'group1::hosted1',
        name: 'hosted1',
        type: 'hosted',
        format: 'maven2',
        status: 'online',
        online: true,
        blobStore: 'default',
      },
      {
        id: 'group1::proxy1',
        name: 'proxy1',
        type: 'proxy',
        format: 'maven2',
        status: 'blocked',
        online: true,
        remoteUrl: 'https://example.com',
      }
    ]
  }
];

describe('RepositoryStructureTree', () => {
  const mockToggleExpand = jest.fn();
  const mockRefresh = jest.fn();

  beforeEach(() => {
    (useRepositoryTree as jest.Mock).mockReturnValue({
      tree: mockNodes,
      loading: false,
      expanding: false,
      error: undefined,
      expandedIds: new Set(['group1']),
      toggleExpand: mockToggleExpand,
      expandAll: jest.fn(),
      collapseAll: jest.fn(),
      revealIssues: jest.fn(),
      setExpandedIds: jest.fn(),
      refresh: mockRefresh,
      usages: [],
    });
    (useStructureComponentSearch as jest.Mock).mockReturnValue({
      reposWithMatches: new Set(),
      loading: false,
      error: null,
    });
  });

  it('renders the root node and its children', () => {
    render(wrap(<RepositoryStructureTree repositoryName="group1" />));

    expect(screen.getAllByText('group1').length).toBeGreaterThan(0);
    expect(screen.getByText('hosted1')).toBeInTheDocument();
    expect(screen.getByText('proxy1')).toBeInTheDocument();
  });

  it('displays blob store label for hosted repo', () => {
    render(wrap(<RepositoryStructureTree repositoryName="group1" />));
    expect(screen.getByText('[default]')).toBeInTheDocument();
  });

  it('displays remote icon for proxy repo', () => {
    render(wrap(<RepositoryStructureTree repositoryName="group1" />));
    // Globe icon from Lucide should be present
    expect(document.querySelector('.repo-type-icon--proxy')).toBeInTheDocument();
  });

  it('proxy remote URL shows truncated link text and opens in new tab', () => {
    render(wrap(<RepositoryStructureTree repositoryName="group1" />));
    const proxyRemoteLink = screen.getByRole('link', { name: 'Open remote URL in new tab' });
    expect(proxyRemoteLink).toHaveAttribute('href', 'https://example.com');
    expect(proxyRemoteLink).toHaveAttribute('target', '_blank');
    expect(proxyRemoteLink).toHaveAttribute('rel', 'noopener noreferrer');
    expect(proxyRemoteLink).toHaveTextContent('https://example.com');
  });

  it('truncates long remote URLs with ellipsis', () => {
    const longUrlNodes = [
      {
        id: 'group1',
        name: 'group1',
        type: 'group',
        format: 'maven2',
        status: 'online',
        online: true,
        children: [
          {
            id: 'group1::proxy1',
            name: 'proxy1',
            type: 'proxy',
            format: 'maven2',
            status: 'online',
            online: true,
            remoteUrl: 'https://repo.example.com/very/long/path/to/maven2/repository',
          },
        ],
      },
    ];
    (useRepositoryTree as jest.Mock).mockReturnValue({
      tree: longUrlNodes,
      loading: false,
      expanding: false,
      error: undefined,
      expandedIds: new Set(['group1']),
      toggleExpand: jest.fn(),
      expandAll: jest.fn(),
      collapseAll: jest.fn(),
      revealIssues: jest.fn(),
      setExpandedIds: jest.fn(),
      refresh: jest.fn(),
      usages: [],
    });
    render(wrap(<RepositoryStructureTree repositoryName="group1" />));
    const proxyRemoteLink = screen.getByRole('link', { name: 'Open remote URL in new tab' });
    expect(proxyRemoteLink).toHaveTextContent(/…$/);
    expect(proxyRemoteLink).toHaveAttribute('title', 'https://repo.example.com/very/long/path/to/maven2/repository');
  });

  it('shows loading state', () => {
    (useRepositoryTree as jest.Mock).mockReturnValue({
      tree: [],
      loading: true,
      expanding: false,
      error: undefined,
      expandedIds: new Set(),
      toggleExpand: jest.fn(),
      expandAll: jest.fn(),
      collapseAll: jest.fn(),
      revealIssues: jest.fn(),
      setExpandedIds: jest.fn(),
      refresh: jest.fn(),
      usages: [],
    });

    render(wrap(<RepositoryStructureTree repositoryName="group1" />));
    expect(screen.getAllByText(/loading structure/i)[0]).toBeInTheDocument();
  });

  it('shows error state', () => {
    (useRepositoryTree as jest.Mock).mockReturnValue({
      tree: [],
      loading: false,
      expanding: false,
      error: 'Failed to load',
      expandedIds: new Set(),
      toggleExpand: jest.fn(),
      expandAll: jest.fn(),
      collapseAll: jest.fn(),
      revealIssues: jest.fn(),
      setExpandedIds: jest.fn(),
      refresh: mockRefresh,
      usages: [],
    });

    render(wrap(<RepositoryStructureTree repositoryName="group1" />));
    expect(screen.getByText('Failed to load')).toBeInTheDocument();
    
    const retryButton = screen.getByRole('button', { name: /try again/i });
    fireEvent.click(retryButton);
    expect(mockRefresh).toHaveBeenCalled();
  });

  it('handles keyboard navigation - ArrowDown', () => {
    render(wrap(<RepositoryStructureTree repositoryName="group1" />));
    const tree = screen.getByRole('tree');
    
    // Initial focus on root
    fireEvent.keyDown(tree, { key: 'ArrowDown' });
    // Focus should move to hosted1 (based on my implementation)
    // We can check focused class
    const nodes = document.querySelectorAll('.repository-tree-node');
    expect(nodes[1]).toHaveClass('repository-tree-node--focused');
  });

  it('shows empty state when filter has no matches', () => {
    (useStructureComponentSearch as jest.Mock).mockReturnValue({
      reposWithMatches: new Set(),
      loading: false,
      error: null,
    });
    render(wrap(<RepositoryStructureTree repositoryName="group1" />));
    const input = screen.getByPlaceholderText(
      /search structure \(repo, blob store, or component\)/i
    );
    fireEvent.change(input, { target: { value: 'nonexistent-xyz' } });
    expect(
      screen.getByText(
        /no matches for "nonexistent-xyz" in repo names, blob stores, or components/i
      )
    ).toBeInTheDocument();
  });

  it('shows loading spinner when component search is in flight', () => {
    (useStructureComponentSearch as jest.Mock).mockReturnValue({
      reposWithMatches: new Set(),
      loading: true,
      error: null,
    });
    render(wrap(<RepositoryStructureTree repositoryName="group1" />));
    expect(document.querySelector('.repository-tree-node__spinner')).toBeInTheDocument();
  });

  it('handles circular dependency warning', () => {
    const circularNodes = [
      {
        id: 'group1',
        name: 'group1',
        type: 'group',
        format: 'maven2',
        status: 'online',
        online: true,
        isCircular: true,
        children: []
      }
    ];

    (useRepositoryTree as jest.Mock).mockReturnValue({
      tree: circularNodes,
      loading: false,
      expanding: false,
      error: undefined,
      expandedIds: new Set(['group1']),
      toggleExpand: jest.fn(),
      expandAll: jest.fn(),
      collapseAll: jest.fn(),
      revealIssues: jest.fn(),
      setExpandedIds: jest.fn(),
      refresh: jest.fn(),
      usages: [],
    });

    render(wrap(<RepositoryStructureTree repositoryName="group1" />));
    // AlertTriangle should be present
    expect(screen.getByTestId('circular-warning')).toBeInTheDocument();
  });
});
