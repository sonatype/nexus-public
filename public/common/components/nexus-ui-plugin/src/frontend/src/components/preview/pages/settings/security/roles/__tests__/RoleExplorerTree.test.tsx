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
import { RoleExplorerTree } from '../RoleExplorerTree';
import { SecurityTreeNode } from '../useRoleTree';

const mockTree: SecurityTreeNode[] = [
  {
    id: 'role1',
    entityId: 'role1',
    name: 'Parent Role',
    type: 'role',
    inherited: false,
    expanded: true,
    children: [
      {
        id: 'role1 > role2',
        entityId: 'role2',
        name: 'Child Role',
        type: 'role',
        inherited: true,
        parentRoleName: 'Parent Role',
        expanded: false,
        children: [
          {
            id: 'role1 > role2 : priv1',
            entityId: 'priv1',
            name: 'Child Privilege',
            type: 'privilege',
            inherited: true,
            parentRoleName: 'Child Role',
          }
        ]
      },
      {
        id: 'role1 : priv2',
        entityId: 'priv2',
        name: 'Direct Privilege',
        type: 'privilege',
        inherited: false,
        children: [
          {
            id: 'role1 : priv2 -> selector1',
            entityId: 'selector1',
            name: 'Content Selector',
            type: 'content-selector',
            inherited: false,
            csel: 'format == "maven2"',
          }
        ]
      }
    ]
  }
];

const renderWithTheme = (component: React.ReactNode) => {
  return render(<Theme>{component}</Theme>);
};

describe('RoleExplorerTree', () => {
  const mockOnToggleExpand = jest.fn();

  it('should render loading state', () => {
    renderWithTheme(
      <RoleExplorerTree tree={[]} loading={true} onToggleExpand={mockOnToggleExpand} />
    );
    expect(screen.getByText('Building security tree...')).toBeInTheDocument();
  });

  it('should render empty state', () => {
    renderWithTheme(
      <RoleExplorerTree tree={[]} loading={false} onToggleExpand={mockOnToggleExpand} />
    );
    expect(screen.getByText('No matching security nodes found.')).toBeInTheDocument();
  });

  it('should render the tree structure', () => {
    renderWithTheme(
      <RoleExplorerTree tree={mockTree} loading={false} onToggleExpand={mockOnToggleExpand} />
    );

    expect(screen.getByText('Parent Role')).toBeInTheDocument();
    expect(screen.getByText('Child Role')).toBeInTheDocument();
    expect(screen.getByText('(inherited)')).toBeInTheDocument();
    expect(screen.getByText('Direct Privilege')).toBeInTheDocument();
  });

  it('should show content selector expression', () => {
    // Expand the node to see children
    const expandedTree = [...mockTree];
    expandedTree[0].children![1].expanded = true;

    renderWithTheme(
      <RoleExplorerTree tree={expandedTree} loading={false} onToggleExpand={mockOnToggleExpand} />
    );

    expect(screen.getByText('format == "maven2"')).toBeInTheDocument();
  });

  it('should call onToggleExpand when expander is clicked', () => {
    renderWithTheme(
      <RoleExplorerTree tree={mockTree} loading={false} onToggleExpand={mockOnToggleExpand} />
    );

    const expandButtons = screen.getAllByLabelText(/Expand|Collapse/);
    fireEvent.click(expandButtons[0]);

    expect(mockOnToggleExpand).toHaveBeenCalledWith('role1');
  });

  it('should handle keyboard navigation (ArrowRight to expand)', () => {
    renderWithTheme(
      <RoleExplorerTree tree={mockTree} loading={false} onToggleExpand={mockOnToggleExpand} />
    );

    const childRoleNode = screen.getByText('Child Role').closest('[role="treeitem"]');
    if (childRoleNode) {
      fireEvent.keyDown(childRoleNode, { key: 'ArrowRight' });
      expect(mockOnToggleExpand).toHaveBeenCalledWith('role1 > role2');
    }
  });

  it('should show circular reference warning', () => {
    const circularTree: SecurityTreeNode[] = [
      {
        id: 'role1',
        entityId: 'role1',
        name: 'Circular Role',
        type: 'role',
        inherited: false,
        isCircular: true,
      }
    ];

    renderWithTheme(
      <RoleExplorerTree tree={circularTree} loading={false} onToggleExpand={mockOnToggleExpand} />
    );

    expect(document.querySelector('.role-explorer-tree__icon--warning')).toBeInTheDocument();
  });

  it('should not render a fullscreen toggle button', () => {
    renderWithTheme(
      <RoleExplorerTree tree={mockTree} loading={false} onToggleExpand={mockOnToggleExpand} />
    );
    expect(screen.queryByLabelText(/fullscreen/i)).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /fullscreen/i })).not.toBeInTheDocument();
  });
});
