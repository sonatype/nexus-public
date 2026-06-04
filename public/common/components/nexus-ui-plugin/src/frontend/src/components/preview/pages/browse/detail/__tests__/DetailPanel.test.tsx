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
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import { Theme } from '@radix-ui/themes';

import { DetailPanel, type AssetData, type ComponentData } from '../DetailPanel';
import type { BrowseNode } from '../../tree/browse-tree.types';
import { deleteAsset, deleteComponent } from '../../browse.api';

const mockFetchBrowseNodes = jest.fn();

jest.mock('../../browse.api', () => ({
  deleteComponent: jest.fn().mockResolvedValue(['mock-id']),
  deleteAsset: jest.fn().mockResolvedValue(undefined),
  fetchBrowseNodes: (...args: unknown[]) => mockFetchBrowseNodes(...args),
}));

const mockToast = {
  showToast: jest.fn(),
  success: jest.fn(),
  error: jest.fn(),
  warning: jest.fn(),
  info: jest.fn(),
};

// Mock useToast from shared
jest.mock('../../../../shared', () => ({
  useToast: () => mockToast,
  DeepResearchLink: ({ href }: { href: string }) => <a href={href}>Deep Research</a>,
}));

// Test wrapper with Radix Theme
const renderWithTheme = (ui: React.ReactElement) => {
  return render(<Theme>{ui}</Theme>);
};

// Mock BrowseNode data
const mockComponentNode: BrowseNode = {
  id: '/org/apache/commons/commons-lang3/3.14.0',
  text: 'commons-lang3-3.14.0',
  type: 'component',
  leaf: false,
  componentId: 'component-123',
};

const mockAssetNode: BrowseNode = {
  id: '/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar',
  text: 'commons-lang3-3.14.0.jar',
  type: 'asset',
  leaf: true,
  componentId: 'component-123',
  assetId: 'asset-123',
};

const mockFolderNode: BrowseNode = {
  id: '/org/apache/commons',
  text: 'commons',
  type: 'folder',
  leaf: false,
};

// Mock component data
const mockComponentData: ComponentData = {
  id: 'component-123',
  repositoryName: 'maven-releases',
  format: 'maven2',
  group: 'org.apache.commons',
  name: 'commons-lang3',
  version: '3.14.0',
};

// Mock asset data
const mockAssetData: AssetData = {
  id: 'asset-123',
  name: '/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar',
  format: 'maven2',
  contentType: 'application/java-archive',
  size: 1572864,
  repositoryName: 'maven-releases',
  blobCreated: '2024-01-15T10:30:00Z',
  blobUpdated: '2024-01-15T10:30:00Z',
  lastDownloaded: '2024-01-20T14:25:00Z',
};

const repositoryName = 'maven-releases';

describe('DetailPanel', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (window as any).__nexusToast = mockToast;
    mockFetchBrowseNodes.mockResolvedValue([
      { id: 'child1', text: 'child1', type: 'folder', leaf: false },
      { id: 'child2', text: 'child2', type: 'component', leaf: false },
      { id: 'child3', text: 'child3.jar', type: 'asset', leaf: true },
    ]);
  });

  afterEach(() => {
    delete (window as any).__nexusToast;
  });

  describe('empty state', () => {
    it('shows empty state when node is null', () => {
      renderWithTheme(<DetailPanel node={null} repositoryName={repositoryName} />);

      expect(screen.getByText('No Item Selected')).toBeInTheDocument();
      expect(screen.getByText(/Select An Item In The Tree To View Its Details/)).toBeInTheDocument();
    });
  });

  describe('loading state', () => {
    it('shows loading spinner when loading is true', () => {
      renderWithTheme(
        <DetailPanel node={mockComponentNode} repositoryName={repositoryName} loading={true} />
      );

      expect(screen.getByText('Loading details...')).toBeInTheDocument();
    });
  });

  describe('error state', () => {
    it('shows error message when error is provided', () => {
      renderWithTheme(
        <DetailPanel
          node={mockComponentNode}
          repositoryName={repositoryName}
          error="Failed to load"
        />
      );

      expect(screen.getByText(/Error loading details/)).toBeInTheDocument();
      expect(screen.getByText(/Failed to load/)).toBeInTheDocument();
    });
  });

  describe('component details', () => {
    it('renders component details for component node', () => {
      renderWithTheme(
        <DetailPanel
          node={mockComponentNode}
          repositoryName={repositoryName}
          componentData={mockComponentData}
        />
      );

      expect(screen.getByRole('heading', { name: mockComponentNode.text })).toBeInTheDocument();
    });

    it('shows component repository', () => {
      renderWithTheme(
        <DetailPanel
          node={mockComponentNode}
          repositoryName={repositoryName}
          componentData={mockComponentData}
        />
      );

      expect(screen.getByText(repositoryName)).toBeInTheDocument();
    });

    it('shows component format', () => {
      renderWithTheme(
        <DetailPanel
          node={mockComponentNode}
          repositoryName={repositoryName}
          componentData={mockComponentData}
        />
      );

      expect(screen.getByText('maven2')).toBeInTheDocument();
    });

    it('shows delete button when canDelete is true', () => {
      renderWithTheme(
        <DetailPanel
          node={mockComponentNode}
          repositoryName={repositoryName}
          componentData={mockComponentData}
          canDelete={true}
        />
      );

      expect(screen.getByRole('button', { name: /delete component/i })).toBeInTheDocument();
    });

    it('hides delete button when canDelete is false', () => {
      renderWithTheme(
        <DetailPanel
          node={mockComponentNode}
          repositoryName={repositoryName}
          componentData={mockComponentData}
          canDelete={false}
        />
      );

      expect(screen.queryByRole('button', { name: /delete component/i })).not.toBeInTheDocument();
    });
  });

  describe('asset details', () => {
    it('renders asset details for asset node', () => {
      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
        />
      );

      expect(screen.getByRole('heading', { name: mockAssetNode.text })).toBeInTheDocument();
    });

    it('shows asset format', () => {
      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
        />
      );

      expect(screen.getByText('maven2')).toBeInTheDocument();
    });

    it('shows asset content type', () => {
      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
        />
      );

      expect(screen.getByText('application/java-archive')).toBeInTheDocument();
    });

    it('shows download button for assets', () => {
      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
        />
      );

      expect(screen.getByRole('button', { name: /download/i })).toBeInTheDocument();
    });

    it('shows delete button when canDelete is true', () => {
      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
          canDelete={true}
        />
      );

      expect(screen.getByRole('button', { name: /delete asset/i })).toBeInTheDocument();
    });
  });

  describe('folder details', () => {
    it('renders folder details for folder node', () => {
      renderWithTheme(
        <DetailPanel node={mockFolderNode} repositoryName={repositoryName} />
      );

      expect(screen.getByRole('heading', { name: mockFolderNode.text })).toBeInTheDocument();
    });

    it('shows folder path', () => {
      renderWithTheme(
        <DetailPanel node={mockFolderNode} repositoryName={repositoryName} />
      );

      expect(screen.getByText(mockFolderNode.id)).toBeInTheDocument();
    });

    it('shows repository name', () => {
      renderWithTheme(
        <DetailPanel node={mockFolderNode} repositoryName={repositoryName} />
      );

      expect(screen.getByText(repositoryName)).toBeInTheDocument();
    });

    it('shows delete button when canDelete is true', () => {
      renderWithTheme(
        <DetailPanel node={mockFolderNode} repositoryName={repositoryName} canDelete={true} />
      );

      expect(screen.getByRole('button', { name: /delete folder/i })).toBeInTheDocument();
    });

    it('shows delete warning when canDelete is true', () => {
      renderWithTheme(
        <DetailPanel node={mockFolderNode} repositoryName={repositoryName} canDelete={true} />
      );

      expect(screen.getByText(/delete all contents/i)).toBeInTheDocument();
    });

    it('fetches and displays children count (bug 7mv6)', async () => {
      renderWithTheme(
        <DetailPanel node={mockFolderNode} repositoryName={repositoryName} />
      );

      await waitFor(() => {
        expect(screen.getByText('3')).toBeInTheDocument();
      });

      expect(mockFetchBrowseNodes).toHaveBeenCalledWith({
        repositoryName,
        node: mockFolderNode.id,
      });
    });

    it('shows folder depth', () => {
      renderWithTheme(
        <DetailPanel node={mockFolderNode} repositoryName={repositoryName} />
      );

      expect(screen.getByText('Depth')).toBeInTheDocument();
    });

    it('shows children type breakdown', async () => {
      renderWithTheme(
        <DetailPanel node={mockFolderNode} repositoryName={repositoryName} />
      );

      await waitFor(() => {
        expect(screen.getByText('Children')).toBeInTheDocument();
        expect(screen.getByText('Folders')).toBeInTheDocument();
        expect(screen.getByText('Components')).toBeInTheDocument();
        expect(screen.getByText('Assets')).toBeInTheDocument();
      });
    });
  });

  describe('delete functionality', () => {
    it('opens delete dialog when delete button is clicked', async () => {
      renderWithTheme(
        <DetailPanel
          node={mockComponentNode}
          repositoryName={repositoryName}
          componentData={mockComponentData}
          canDelete={true}
        />
      );

      const deleteButton = screen.getByRole('button', { name: /delete component/i });
      await userEvent.click(deleteButton);

      // Dialog should be open
      expect(screen.getByRole('alertdialog')).toBeInTheDocument();
    });

    it('calls onDeleted when delete is confirmed', async () => {
      const onDeleted = jest.fn();
      renderWithTheme(
        <DetailPanel
          node={mockComponentNode}
          repositoryName={repositoryName}
          componentData={mockComponentData}
          canDelete={true}
          onDeleted={onDeleted}
        />
      );

      // Open dialog
      const deleteButton = screen.getByRole('button', { name: /delete component/i });
      await userEvent.click(deleteButton);

      // Confirm delete
      const confirmButton = screen.getByRole('button', { name: /^Delete$/i });
      await userEvent.click(confirmButton);

      // Wait for the simulated delete operation
      await waitFor(() => {
        expect(onDeleted).toHaveBeenCalled();
      });
    });

    it('closes dialog when cancel is clicked', async () => {
      renderWithTheme(
        <DetailPanel
          node={mockComponentNode}
          repositoryName={repositoryName}
          componentData={mockComponentData}
          canDelete={true}
        />
      );

      // Open dialog
      const deleteButton = screen.getByRole('button', { name: /delete component/i });
      await userEvent.click(deleteButton);

      // Cancel
      const cancelButton = screen.getByRole('button', { name: /cancel/i });
      await userEvent.click(cancelButton);

      // Dialog should be closed
      await waitFor(() => {
        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
      });
    });

    it('closes dialog, shows error toast, and refreshes tree when asset delete fails (cloud 500)', async () => {
      const onDeleted = jest.fn();
      (deleteAsset as jest.Mock).mockRejectedValueOnce(
        new Error('MissingFacetException: No facet of type AptHostedMetadataFacet attached to repository apt-proxy-debian')
      );

      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
          canDelete={true}
          onDeleted={onDeleted}
        />
      );

      const deleteButton = screen.getByRole('button', { name: /delete asset/i });
      await userEvent.click(deleteButton);
      const confirmButton = screen.getByRole('button', { name: /^Delete$/i });
      await userEvent.click(confirmButton);

      await waitFor(() => {
        expect(mockToast.error).toHaveBeenCalledWith(
          expect.stringContaining('MissingFacetException')
        );
      });

      expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();

      await waitFor(() => {
        expect(
          screen.getByText(/Cannot delete: MissingFacetException/)
        ).toBeInTheDocument();
      });

      expect(onDeleted).toHaveBeenCalled();
    });

    it('shows success toast when delete succeeds', async () => {
      const onDeleted = jest.fn();
      (deleteAsset as jest.Mock).mockResolvedValueOnce(undefined);

      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
          canDelete={true}
          onDeleted={onDeleted}
        />
      );

      const deleteButton = screen.getByRole('button', { name: /delete asset/i });
      await userEvent.click(deleteButton);
      const confirmButton = screen.getByRole('button', { name: /^Delete$/i });
      await userEvent.click(confirmButton);

      await waitFor(() => {
        expect(mockToast.success).toHaveBeenCalledWith('Item deleted successfully');
      });

      expect(onDeleted).toHaveBeenCalled();
    });

    it('shows error toast and refreshes tree when component delete fails', async () => {
      const onDeleted = jest.fn();
      (deleteComponent as jest.Mock).mockRejectedValueOnce(
        new Error('Server error (500). Please try again later.')
      );

      renderWithTheme(
        <DetailPanel
          node={mockComponentNode}
          repositoryName={repositoryName}
          componentData={mockComponentData}
          canDelete={true}
          onDeleted={onDeleted}
        />
      );

      const deleteButton = screen.getByRole('button', { name: /delete component/i });
      await userEvent.click(deleteButton);
      const confirmButton = screen.getByRole('button', { name: /^Delete$/i });
      await userEvent.click(confirmButton);

      await waitFor(() => {
        expect(mockToast.error).toHaveBeenCalledWith(
          expect.stringContaining('Server error')
        );
      });

      expect(onDeleted).toHaveBeenCalled();
    });

    it('handles delete timeout by racing with a timeout promise', async () => {
      const onDeleted = jest.fn();
      const neverResolves = new Promise<void>(() => {});
      (deleteAsset as jest.Mock).mockReturnValueOnce(neverResolves);

      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
          canDelete={true}
          onDeleted={onDeleted}
        />
      );

      const deleteButton = screen.getByRole('button', { name: /delete asset/i });
      await userEvent.click(deleteButton);

      expect(screen.getByRole('alertdialog')).toBeInTheDocument();
    });
  });

  describe('download functionality', () => {
    const mockWindowOpen = jest.fn();
    const originalWindowOpen = window.open;

    beforeEach(() => {
      window.open = mockWindowOpen;
      mockWindowOpen.mockClear();
    });

    afterEach(() => {
      window.open = originalWindowOpen;
    });

    it('opens download URL when download button is clicked', async () => {
      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
        />
      );

      const downloadButton = screen.getByRole('button', { name: /download/i });
      await userEvent.click(downloadButton);

      expect(mockWindowOpen).toHaveBeenCalledWith(
        expect.stringContaining('/repository/maven-releases/'),
        '_blank'
      );
    });
  });

  describe('URL-based tab navigation', () => {
    it('shows the active tab from props for asset details', () => {
      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
          activeTab="usage"
        />
      );

      // Usage tab should be active
      const usageTab = screen.getByRole('tab', { name: /usage/i });
      expect(usageTab).toHaveAttribute('data-state', 'active');
    });

    it('defaults to summary tab when activeTab is not provided', () => {
      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
        />
      );

      const summaryTab = screen.getByRole('tab', { name: /summary/i });
      expect(summaryTab).toHaveAttribute('data-state', 'active');
    });

    it('calls onTabChange when a tab is clicked', async () => {
      const onTabChange = jest.fn();
      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
          activeTab="summary"
          onTabChange={onTabChange}
        />
      );

      const attributesTab = screen.getByRole('tab', { name: /attributes/i });
      await userEvent.click(attributesTab);

      expect(onTabChange).toHaveBeenCalledWith('attributes');
    });

    it('shows attributes tab content when activeTab is attributes', () => {
      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
          activeTab="attributes"
        />
      );

      const attributesTab = screen.getByRole('tab', { name: /attributes/i });
      expect(attributesTab).toHaveAttribute('data-state', 'active');
    });
  });
});
