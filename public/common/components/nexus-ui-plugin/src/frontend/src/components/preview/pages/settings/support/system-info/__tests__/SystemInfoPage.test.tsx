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

import { SystemInfoPage } from '../SystemInfoPage';
import * as useSystemInfoModule from '../useSystemInfo';
import { ExtJS } from '@sonatype/nexus-ui-plugin';

// Mock the integration hook
jest.mock('../useSystemInfo');

const mockedUseSystemInfo = useSystemInfoModule.useSystemInfo as jest.MockedFunction<
  typeof useSystemInfoModule.useSystemInfo
>;

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

const mockSystemInfo = {
  'nexus-status': {
    version: '3.88.0-01',
    edition: 'PRO',
    status: 'Running',
  },
  'nexus-node': {
    nodeId: 'node-1',
    clustered: false,
  },
  'nexus-license': {
    licenseType: 'Professional',
    validTo: '2025-12-31',
  },
  'system-runtime': {
    javaVersion: '17.0.1',
    availableProcessors: 8,
  },
};

const mockSections: [string, any][] = Object.entries(mockSystemInfo);

function makeHook(
  overrides: Partial<ReturnType<typeof useSystemInfoModule.useSystemInfo>> = {}
): ReturnType<typeof useSystemInfoModule.useSystemInfo> {
  return {
    systemInfo: mockSystemInfo,
    nodes: [],
    selectedNode: null,
    isHAMode: false,
    isLoading: false,
    isRefreshing: false,
    error: null,
    expandedSections: new Set(['nexus-status', 'nexus-node', 'nexus-license']),
    sections: mockSections,
    sectionRefs: { current: {} } as React.MutableRefObject<Record<string, HTMLDivElement | null>>,
    handleNodeSelect: jest.fn(),
    handleRefresh: jest.fn(),
    handleDownload: jest.fn(),
    handleCopy: jest.fn().mockResolvedValue(undefined),
    handleExpandAll: jest.fn(),
    handleCollapseAll: jest.fn(),
    handleSectionToggle: jest.fn(),
    handleJumpToSection: jest.fn(),
    clearError: jest.fn(),
    ...overrides,
  };
}

describe('SystemInfoPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jest.spyOn(ExtJS, 'checkPermission').mockReturnValue(true);
    mockedUseSystemInfo.mockReturnValue(makeHook());
  });

  it('renders loading state', () => {
    mockedUseSystemInfo.mockReturnValue(makeHook({ isLoading: true }));

    render(<SystemInfoPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Loading system information...')).toBeInTheDocument();
  });

  it('renders the page header', () => {
    render(<SystemInfoPage />, { wrapper: TestWrapper });

    expect(screen.getByRole('heading', { name: 'System Information' })).toBeInTheDocument();
    expect(screen.getByText('View detailed system and server information')).toBeInTheDocument();
  });

  it('displays system info sections', () => {
    render(<SystemInfoPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Nexus Status')).toBeInTheDocument();
    expect(screen.getByText('Nexus Node')).toBeInTheDocument();
    expect(screen.getByText('Nexus License')).toBeInTheDocument();
    expect(screen.getByText('System Runtime')).toBeInTheDocument();
  });

  it('expands sections listed in expandedSections', () => {
    render(<SystemInfoPage />, { wrapper: TestWrapper });

    // nexus-status is in expandedSections — content visible
    expect(screen.getByText('version')).toBeInTheDocument();
    expect(screen.getByText('3.88.0-01')).toBeInTheDocument();
  });

  it('does not show content for collapsed sections', () => {
    mockedUseSystemInfo.mockReturnValue(
      makeHook({ expandedSections: new Set(['nexus-status']) })
    );

    render(<SystemInfoPage />, { wrapper: TestWrapper });

    expect(screen.getByText('version')).toBeInTheDocument();
    expect(screen.queryByText('nodeId')).not.toBeInTheDocument();
  });

  it('calls handleRefresh when refresh button is clicked', () => {
    const mockHandleRefresh = jest.fn();
    mockedUseSystemInfo.mockReturnValue(makeHook({ handleRefresh: mockHandleRefresh }));

    render(<SystemInfoPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByRole('button', { name: /refresh/i }));

    expect(mockHandleRefresh).toHaveBeenCalled();
  });

  it('calls handleDownload when download button is clicked', () => {
    const mockHandleDownload = jest.fn();
    mockedUseSystemInfo.mockReturnValue(makeHook({ handleDownload: mockHandleDownload }));

    render(<SystemInfoPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByRole('button', { name: /download/i }));

    expect(mockHandleDownload).toHaveBeenCalled();
  });

  it('calls handleCopy when copy button is clicked', () => {
    const mockHandleCopy = jest.fn().mockResolvedValue(undefined);
    mockedUseSystemInfo.mockReturnValue(makeHook({ handleCopy: mockHandleCopy }));

    render(<SystemInfoPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByRole('button', { name: /copy/i }));

    expect(mockHandleCopy).toHaveBeenCalled();
  });

  it('displays error alert when hook returns an error', () => {
    mockedUseSystemInfo.mockReturnValue(makeHook({ error: 'Failed to load system information' }));

    render(<SystemInfoPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Failed to load system information')).toBeInTheDocument();
  });

  it('calls clearError when error alert is dismissed', () => {
    const mockClearError = jest.fn();
    mockedUseSystemInfo.mockReturnValue(
      makeHook({ error: 'Some error', clearError: mockClearError })
    );

    render(<SystemInfoPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByRole('button', { name: /dismiss/i }));

    expect(mockClearError).toHaveBeenCalled();
  });

  it('shows no permission message when user lacks permission', () => {
    jest.spyOn(ExtJS, 'checkPermission').mockReturnValue(false);
    mockedUseSystemInfo.mockReturnValue(makeHook({ isLoading: false }));

    render(<SystemInfoPage />, { wrapper: TestWrapper });

    expect(
      screen.getByText('You do not have permission to view system information.')
    ).toBeInTheDocument();
  });

  it('displays help section with documentation link', () => {
    render(<SystemInfoPage />, { wrapper: TestWrapper });

    expect(screen.getByText('About System Information')).toBeInTheDocument();
    expect(screen.getByText('documentation')).toHaveAttribute(
      'href',
      'https://help.sonatype.com/en/system-information.html'
    );
  });

  describe('HA mode', () => {
    const mockHANodes = [
      { nodeId: 'node-1', friendlyName: 'Node 1', local: true },
      { nodeId: 'node-2', friendlyName: 'Node 2', local: false },
    ];

    it('shows node selector in HA mode', () => {
      mockedUseSystemInfo.mockReturnValue(
        makeHook({ isHAMode: true, nodes: mockHANodes, selectedNode: 'node-1' })
      );

      render(<SystemInfoPage />, { wrapper: TestWrapper });

      expect(screen.getByText('Select Node')).toBeInTheDocument();
      expect(screen.getByText('Node 1')).toBeInTheDocument();
      expect(screen.getByText('Node 2')).toBeInTheDocument();
    });

    it('does not show node selector when not in HA mode', () => {
      render(<SystemInfoPage />, { wrapper: TestWrapper });

      expect(screen.queryByText('Select Node')).not.toBeInTheDocument();
    });

    it('calls handleNodeSelect when a node button is clicked', () => {
      const mockHandleNodeSelect = jest.fn();
      mockedUseSystemInfo.mockReturnValue(
        makeHook({
          isHAMode: true,
          nodes: mockHANodes,
          selectedNode: 'node-1',
          handleNodeSelect: mockHandleNodeSelect,
        })
      );

      render(<SystemInfoPage />, { wrapper: TestWrapper });

      fireEvent.click(screen.getByRole('button', { name: /node 2/i }));

      expect(mockHandleNodeSelect).toHaveBeenCalledWith('node-2');
    });

    it('marks selected node with aria-pressed true', () => {
      mockedUseSystemInfo.mockReturnValue(
        makeHook({ isHAMode: true, nodes: mockHANodes, selectedNode: 'node-1' })
      );

      render(<SystemInfoPage />, { wrapper: TestWrapper });

      expect(screen.getByRole('button', { name: /node 1/i })).toHaveAttribute(
        'aria-pressed',
        'true'
      );
    });
  });

  describe('Navigation features', () => {
    it('renders navigation bar with Jump to Section dropdown', () => {
      render(<SystemInfoPage />, { wrapper: TestWrapper });

      expect(screen.getByText('Jump to:')).toBeInTheDocument();
      expect(screen.getByTestId('system-info-jump-to')).toBeInTheDocument();
    });

    it('renders Expand All button', () => {
      render(<SystemInfoPage />, { wrapper: TestWrapper });

      expect(screen.getByTestId('system-info-expand-all')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /expand all/i })).toBeInTheDocument();
    });

    it('renders Collapse All button', () => {
      render(<SystemInfoPage />, { wrapper: TestWrapper });

      expect(screen.getByTestId('system-info-collapse-all')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /collapse all/i })).toBeInTheDocument();
    });

    it('calls handleExpandAll when Expand All is clicked', () => {
      const mockHandleExpandAll = jest.fn();
      mockedUseSystemInfo.mockReturnValue(makeHook({ handleExpandAll: mockHandleExpandAll }));

      render(<SystemInfoPage />, { wrapper: TestWrapper });

      fireEvent.click(screen.getByTestId('system-info-expand-all'));

      expect(mockHandleExpandAll).toHaveBeenCalled();
    });

    it('calls handleCollapseAll when Collapse All is clicked', () => {
      const mockHandleCollapseAll = jest.fn();
      mockedUseSystemInfo.mockReturnValue(makeHook({ handleCollapseAll: mockHandleCollapseAll }));

      render(<SystemInfoPage />, { wrapper: TestWrapper });

      fireEvent.click(screen.getByTestId('system-info-collapse-all'));

      expect(mockHandleCollapseAll).toHaveBeenCalled();
    });

    it('shows no section content when all sections are collapsed', () => {
      mockedUseSystemInfo.mockReturnValue(makeHook({ expandedSections: new Set() }));

      render(<SystemInfoPage />, { wrapper: TestWrapper });

      expect(screen.getByText('Nexus Status')).toBeInTheDocument();
      expect(screen.queryByText('version')).not.toBeInTheDocument();
    });

    it('calls handleSectionToggle when section header is clicked', () => {
      const mockHandleSectionToggle = jest.fn();
      mockedUseSystemInfo.mockReturnValue(
        makeHook({ handleSectionToggle: mockHandleSectionToggle })
      );

      render(<SystemInfoPage />, { wrapper: TestWrapper });

      const sectionHeader = screen.getByText('Nexus Status').closest('[role="button"]');
      if (sectionHeader) {
        fireEvent.click(sectionHeader);
      }

      expect(mockHandleSectionToggle).toHaveBeenCalledWith('nexus-status', expect.any(Boolean));
    });

    it('hides navigation bar when no sections are available', () => {
      mockedUseSystemInfo.mockReturnValue(makeHook({ sections: [], systemInfo: null }));

      render(<SystemInfoPage />, { wrapper: TestWrapper });

      expect(screen.queryByTestId('system-info-nav')).not.toBeInTheDocument();
    });
  });

  describe('Breadcrumb navigation', () => {
    it('renders breadcrumbs with Settings link', () => {
      render(<SystemInfoPage />, { wrapper: TestWrapper });

      expect(screen.getByRole('button', { name: 'Settings' })).toBeInTheDocument();
    });

    it('renders System Information as current page in breadcrumbs', () => {
      const { container } = render(<SystemInfoPage />, { wrapper: TestWrapper });

      const currentBreadcrumb = container.querySelector('[aria-current="page"]');
      expect(currentBreadcrumb).toBeInTheDocument();
      expect(currentBreadcrumb?.textContent).toBe('System Information');
    });

    it('navigates to Settings when Settings breadcrumb is clicked', () => {
      render(<SystemInfoPage />, { wrapper: TestWrapper });

      const originalHash = window.location.hash;
      screen.getByRole('button', { name: 'Settings' }).click();
      expect(window.location.hash).toBe('#preview/admin/settings');
      window.location.hash = originalHash;
    });
  });
});
