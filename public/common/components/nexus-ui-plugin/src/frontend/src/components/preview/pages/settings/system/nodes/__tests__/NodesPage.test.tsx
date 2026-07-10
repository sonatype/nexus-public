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

import { NodesPage } from '../NodesPage';

// Create mock function that can be configured
const mockGetValue = jest.fn();

// Mock ExtJS with mutable mock
jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    state: jest.fn(() => ({
      getValue: mockGetValue,
    })),
    useState: (fn: () => unknown) => fn(),
  },
}));

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
  beforeEach(() => {
    jest.clearAllMocks();
    window.location.hash = '';
    // Default: single-node mode
    mockGetValue.mockReturnValue(false);
  });

  it('renders the page header', () => {
    render(<NodesPage />, { wrapper: TestWrapper });

    expect(screen.getByRole('heading', { name: 'Nodes', level: 1 })).toBeInTheDocument();
    expect(screen.getByText('View cluster nodes in this Nexus Repository instance')).toBeInTheDocument();
  });

  it('renders the nodes list component', () => {
    render(<NodesPage />, { wrapper: TestWrapper });

    expect(screen.getByTestId('nodes-list')).toBeInTheDocument();
  });

  it('displays page icon', () => {
    const { container } = render(<NodesPage />, { wrapper: TestWrapper });

    // PageHeader renders the icon as an SVG
    expect(container.querySelector('svg')).toBeInTheDocument();
  });

  it('has proper page layout structure', () => {
    render(<NodesPage />, { wrapper: TestWrapper });

    // Nodes list should be visible
    expect(screen.getByTestId('nodes-list')).toBeInTheDocument();
  });

  describe('breadcrumbs', () => {
    it('renders Settings breadcrumb that navigates to settings page', async () => {
      render(<NodesPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Settings' })).toBeInTheDocument();
      });

      // Click Settings breadcrumb navigates to settings page
      screen.getByRole('button', { name: 'Settings' }).click();
      expect(window.location.hash).toBe('#preview/admin/settings');
    });

    it('renders Nodes as current page breadcrumb', async () => {
      render(<NodesPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        // The current page item is rendered as Text (not a button) with aria-current="page"
        const breadcrumb = screen.getByText('Nodes', { selector: '[aria-current="page"]' });
        expect(breadcrumb).toBeInTheDocument();
      });
    });
  });

  describe('single-node mode', () => {
    it('displays informational banner in single-node mode', () => {
      mockGetValue.mockReturnValue(false);
      render(<NodesPage />, { wrapper: TestWrapper });

      const banner = screen.getByTestId('single-node-banner');
      expect(banner).toBeInTheDocument();
      expect(screen.getByText(/This instance is running in single-node mode/)).toBeInTheDocument();
      expect(screen.getByText(/High Availability cluster nodes are not expected/)).toBeInTheDocument();
    });

    it('displays link to high availability documentation', () => {
      mockGetValue.mockReturnValue(false);
      render(<NodesPage />, { wrapper: TestWrapper });

      const link = screen.getByRole('link', { name: /high availability documentation/i });
      expect(link).toBeInTheDocument();
      expect(link).toHaveAttribute('href', 'https://links.sonatype.com/products/nxrm/high-availability');
      expect(link).toHaveAttribute('target', '_blank');
      expect(link).toHaveAttribute('rel', 'noopener noreferrer');
    });

    it('still renders the nodes list in single-node mode', () => {
      mockGetValue.mockReturnValue(false);
      render(<NodesPage />, { wrapper: TestWrapper });

      // The nodes list should still be visible (containing the local node)
      expect(screen.getByTestId('nodes-list')).toBeInTheDocument();
    });
  });

  describe('clustered mode', () => {
    it('does not display the informational banner in clustered mode', () => {
      mockGetValue.mockReturnValue(true);
      render(<NodesPage />, { wrapper: TestWrapper });

      const banner = screen.queryByTestId('single-node-banner');
      expect(banner).not.toBeInTheDocument();
    });

    it('renders the nodes list in clustered mode', () => {
      mockGetValue.mockReturnValue(true);
      render(<NodesPage />, { wrapper: TestWrapper });

      expect(screen.getByTestId('nodes-list')).toBeInTheDocument();
    });
  });
});
