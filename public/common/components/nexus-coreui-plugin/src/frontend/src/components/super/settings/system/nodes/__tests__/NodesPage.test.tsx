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

import { NodesPage } from '../NodesPage';

// Mock child components
jest.mock('../NodesList', () => ({
  NodesList: function MockNodesList() {
    return <div data-testid="nodes-list">Nodes List</div>;
  },
}));

function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

describe('NodesPage', () => {
  it('renders the page header', () => {
    render(<NodesPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Nodes')).toBeInTheDocument();
    expect(screen.getByText('View cluster nodes in this Nexus Repository instance')).toBeInTheDocument();
  });

  it('renders the nodes list component', () => {
    render(<NodesPage />, { wrapper: TestWrapper });

    expect(screen.getByTestId('nodes-list')).toBeInTheDocument();
  });

  it('displays page icon', () => {
    const { container } = render(<NodesPage />, { wrapper: TestWrapper });

    // Should have an icon in the header
    expect(container.querySelector('.nodes-page__icon')).toBeInTheDocument();
  });

  it('has proper page layout structure', () => {
    render(<NodesPage />, { wrapper: TestWrapper });

    // Nodes list should be visible
    expect(screen.getByTestId('nodes-list')).toBeInTheDocument();
  });
});
