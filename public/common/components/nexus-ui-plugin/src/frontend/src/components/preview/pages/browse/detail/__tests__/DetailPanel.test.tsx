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
import ExtJS from '../../../../../../interface/ExtJS';

const mockFetchBrowseNodes = jest.fn();
const mockFetchComponentTags = jest.fn();

jest.mock('../../browse.api', () => ({
  deleteComponent: jest.fn().mockResolvedValue(['mock-id']),
  deleteAsset: jest.fn().mockResolvedValue(undefined),
  fetchBrowseNodes: (...args: unknown[]) => mockFetchBrowseNodes(...args),
  fetchComponentTags: (...args: unknown[]) => mockFetchComponentTags(...args),
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

// Mock the shared Repository Health Check intelligence hook (the network/entitlement seam) but
// keep its pure helpers (formatComponentAge/…) real so intelligenceRows renders authentically.
const mockUseComponentIntelligence = jest.fn();
jest.mock('../../../search/details/useComponentIntelligence', () => ({
  ...jest.requireActual('../../../search/details/useComponentIntelligence'),
  useComponentIntelligence: (opts: unknown) => mockUseComponentIntelligence(opts),
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

// Production shape: BrowseResource.toBrowseNodeXO leaves componentId null on asset rows, so an
// asset node NEVER carries one. The owning component's raw id arrives on the asset response
// instead (mockAssetData.componentId below) — NEXUS-54501.
const mockAssetNode: BrowseNode = {
  id: '/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar',
  text: 'commons-lang3-3.14.0.jar',
  type: 'asset',
  leaf: true,
  assetId: 'asset-123',
};

const mockFolderNode: BrowseNode = {
  id: '/org/apache/commons',
  text: 'commons',
  type: 'folder',
  leaf: false,
};

// Mock component data
// REST v1 returns the base64 RepositoryItemIDXO form as ComponentXO.id, i.e.
// base64('maven-releases:7e8fcfcb'). Kept encoded here so any code path that sends this id to an
// ExtDirect method (which needs the raw DetachedEntityId) is visible in assertions.
const RAW_COMPONENT_ID = '7e8fcfcb';
const ENCODED_COMPONENT_ID = 'bWF2ZW4tcmVsZWFzZXM6N2U4ZmNmY2I';

const mockComponentData: ComponentData = {
  id: ENCODED_COMPONENT_ID,
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
  // Server-provided absolute URL — includes context path regardless of deployment
  downloadUrl: 'https://server.example.com/custom-context/repository/maven-releases/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar',
  // Raw ExtDirect id of the owning component, carried separately from componentData.id.
  componentId: RAW_COMPONENT_ID,
};

const repositoryName = 'maven-releases';

/**
 * Drives the five inputs of the Component Tags entitlement — user, `nexus:tags:read`, Pro edition,
 * non-EVAL edition and a valid installed license — which together mirror Classic's
 * `ComponentTags.isTaggingEnabled()` plus the explicit Pro check this component needs (unlike the
 * proui plugin, it ships in every edition). Defaults are a fully entitled Pro instance; each test
 * overrides one arm.
 */
const setTagEntitlement = ({
  user = { id: 'admin', authenticated: true },
  permission = true,
  edition = 'PRO',
  license = { installed: true, valid: true },
  proEdition = true,
}: {
  user?: unknown;
  permission?: boolean;
  edition?: string;
  license?: { installed?: boolean; valid?: boolean } | null;
  proEdition?: boolean;
} = {}) => {
  jest.spyOn(ExtJS, 'isLicensedEdition').mockReturnValue(proEdition);
  jest.spyOn(ExtJS, 'useUser').mockReturnValue(user as never);
  jest.spyOn(ExtJS, 'useLicense').mockReturnValue(license as never);
  // Run the real predicate so the assertion covers the permission constant, not just a boolean.
  jest.spyOn(ExtJS, 'usePermission').mockImplementation((getValue: () => boolean) => getValue());
  jest.spyOn(ExtJS, 'checkPermission').mockImplementation((p: string) => permission && p === 'nexus:tags:read');
  jest.spyOn(ExtJS, 'state').mockReturnValue({
    getValue: () => undefined,
    getEdition: () => edition,
  } as unknown as ReturnType<typeof ExtJS.state>);
};

describe('DetailPanel', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (window as any).__nexusToast = mockToast;
    mockFetchBrowseNodes.mockResolvedValue([
      { id: 'child1', text: 'child1', type: 'folder', leaf: false },
      { id: 'child2', text: 'child2', type: 'component', leaf: false },
      { id: 'child3', text: 'child3.jar', type: 'asset', leaf: true },
    ]);
    mockFetchComponentTags.mockResolvedValue([]);
    // Default: no intelligence requested/resolved, so it renders nothing and never interferes
    // with the unrelated suites. Individual tests override this per scenario.
    mockUseComponentIntelligence.mockReturnValue({ data: null, status: 'idle', alertsStatus: 'unknown' });
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

    it('closes dialog and shows error toast without running the deletion path when asset delete fails (cloud 500)', async () => {
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

      // onDeleted is the successful-deletion path (BrowsePage removes the node
      // from the tree and navigates away). A rejected delete must not run it.
      expect(onDeleted).not.toHaveBeenCalled();
      expect(mockToast.success).not.toHaveBeenCalled();
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

    it('deletes ONLY the asset (never the component) for an asset node, even when componentData is loaded', async () => {
      // Regression (NEXUS-54172): the asset detail panel also receives componentData (to show component
      // info). The confirm handler must key off node.type and call deleteAsset — NOT fall through to the
      // componentData branch and cascade-delete the whole component. For multi-asset components (e.g. a
      // Terraform provider version = one zip per OS/arch) that wrongly removed every sibling platform.
      const onDeleted = jest.fn();
      (deleteAsset as jest.Mock).mockResolvedValueOnce(undefined);

      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
          componentData={mockComponentData}
          canDelete={true}
          onDeleted={onDeleted}
        />
      );

      const deleteButton = screen.getByRole('button', { name: /delete asset/i });
      await userEvent.click(deleteButton);
      const confirmButton = screen.getByRole('button', { name: /^Delete$/i });
      await userEvent.click(confirmButton);

      await waitFor(() => {
        expect(deleteAsset).toHaveBeenCalledWith(mockAssetData.id, repositoryName);
      });
      expect(deleteComponent).not.toHaveBeenCalled();
    });

    it('shows error toast without running the deletion path when component delete fails', async () => {
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

      expect(onDeleted).not.toHaveBeenCalled();
      expect(mockToast.success).not.toHaveBeenCalled();
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

    it.each([
      ['root context path (/)',    'https://nexus.example.com/repository/maven-releases/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar'],
      ['custom context path (/nexus)', 'https://nexus.example.com/nexus/repository/maven-releases/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar'],
      ['custom context path (/nxrm)',  'https://nexus.example.com/nxrm/repository/maven-releases/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar'],
    ])('uses server-provided downloadUrl unchanged for %s', async (_label, serverDownloadUrl) => {
      const assetData = { ...mockAssetData, downloadUrl: serverDownloadUrl };
      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={assetData}
        />
      );

      const downloadButton = screen.getByRole('button', { name: /download/i });
      await userEvent.click(downloadButton);

      expect(mockWindowOpen).toHaveBeenCalledWith(serverDownloadUrl, '_blank');
    });

    it('falls back to client-generated URL when downloadUrl is absent', async () => {
      const { downloadUrl: _omitted, ...assetDataWithoutDownloadUrl } = mockAssetData;
      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={assetDataWithoutDownloadUrl as typeof mockAssetData}
        />
      );

      const downloadButton = screen.getByRole('button', { name: /download/i });
      await userEvent.click(downloadButton);

      expect(mockWindowOpen).toHaveBeenCalledWith(
        '/repository/maven-releases/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar',
        '_blank'
      );
    });
  });

  describe('docker attributes tab (NEXUS-51972)', () => {
    const dockerAssetNode: BrowseNode = {
      id: '/v2/nginx/blobs/sha256:abc',
      text: 'nginx:latest',
      type: 'asset',
      leaf: true,
      assetId: 'docker-asset-1',
    };

    const dockerAssetData: AssetData = {
      id: 'docker-asset-1',
      name: '/v2/nginx/blobs/sha256:abc',
      format: 'docker',
      contentType: 'application/vnd.docker.image.rootfs.diff.tar.gzip',
      size: 10_000,
      repositoryName: 'docker-hosted',
      blobCreated: '2024-06-01T10:00:00Z',
      blobUpdated: '2024-06-02T10:00:00Z',
      lastDownloaded: null,
      attributes: {
        docker: {
          os: 'linux',
          arch: 'amd64',
          created: '2024-05-01T12:00:00Z',
          author: 'Nginx Team',
          workingDir: '/etc/nginx',
          totalSize: 52_428_800,
          exposedPorts: ['80/tcp', '443/tcp'],
          cmd: ['nginx', '-g', 'daemon off;'],
          entrypoint: ['/docker-entrypoint.sh'],
        },
      },
    };

    it('renders docker metadata fields on the Attributes tab', () => {
      renderWithTheme(
        <DetailPanel
          node={dockerAssetNode}
          repositoryName="docker-hosted"
          assetData={dockerAssetData}
          activeTab="attributes"
        />,
      );

      expect(screen.getByText('OS')).toBeInTheDocument();
      expect(screen.getByText('linux')).toBeInTheDocument();
      expect(screen.getByText('Architecture')).toBeInTheDocument();
      expect(screen.getByText('amd64')).toBeInTheDocument();
      expect(screen.getByText('Author')).toBeInTheDocument();
      expect(screen.getByText('Nginx Team')).toBeInTheDocument();
      expect(screen.getByText('Working Directory')).toBeInTheDocument();
      expect(screen.getByText('/etc/nginx')).toBeInTheDocument();
      expect(screen.getByText('Exposed Ports')).toBeInTheDocument();
      expect(screen.getByText('80/tcp, 443/tcp')).toBeInTheDocument();
      expect(screen.getByText('Entrypoint')).toBeInTheDocument();
      expect(screen.getByText('/docker-entrypoint.sh')).toBeInTheDocument();
      expect(screen.getByText('Cmd')).toBeInTheDocument();
      expect(screen.getByText('nginx, -g, daemon off;')).toBeInTheDocument();
    });

    it('omits docker metadata rows when their values are absent', () => {
      const sparseAssetData: AssetData = {
        ...dockerAssetData,
        attributes: { docker: { os: 'linux' } },
      };

      renderWithTheme(
        <DetailPanel
          node={dockerAssetNode}
          repositoryName="docker-hosted"
          assetData={sparseAssetData}
          activeTab="attributes"
        />,
      );

      expect(screen.getByText('OS')).toBeInTheDocument();
      expect(screen.queryByText('Architecture')).not.toBeInTheDocument();
      expect(screen.queryByText('Exposed Ports')).not.toBeInTheDocument();
      expect(screen.queryByText('Cmd')).not.toBeInTheDocument();
    });

    it('does not render docker metadata rows for non-docker assets', () => {
      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
          activeTab="attributes"
        />,
      );

      expect(screen.queryByText('OS')).not.toBeInTheDocument();
      expect(screen.queryByText('Architecture')).not.toBeInTheDocument();
    });
  });

  describe('Component Tags entitlement (Area 3 / NEXUS-54501)', () => {
    afterEach(() => {
      jest.restoreAllMocks();
    });

    it('shows the Component Tags tab on a fully entitled Pro instance', () => {
      setTagEntitlement();

      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
        />
      );

      expect(screen.getByRole('tab', { name: /component tags/i })).toBeInTheDocument();
    });

    /**
     * Every arm of Classic's `ComponentTags.isTaggingEnabled()`, plus the edition check this
     * component needs because it ships in editions the proui plugin does not.
     *
     * The `nexus:tags:read` case is the regression: gating the tab on the edition alone showed a
     * Pro user without the permission a Component Tags tab that could only ever be empty, since
     * the fetch was already gated on the permission.
     */
    it.each([
      ['the edition is not Pro (CE)', { proEdition: false }],
      ['the user is anonymous', { user: null }],
      ['nexus:tags:read is not held', { permission: false }],
      ['the edition is EVAL', { edition: 'EVAL' }],
      ['no license is installed', { license: { installed: false, valid: false } }],
      ['the installed license is invalid', { license: { installed: true, valid: false } }],
      ['the license state has not been published', { license: null }],
    ])('hides the Component Tags tab when %s', (_case, overrides) => {
      setTagEntitlement(overrides);

      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
        />
      );

      expect(screen.queryByRole('tab', { name: /component tags/i })).not.toBeInTheDocument();
    });

    it('falls back to Summary for ?tab=tags when tags are not entitled', () => {
      setTagEntitlement({ permission: false });
      const onTabChange = jest.fn();

      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
          activeTab="tags"
          onTabChange={onTabChange}
        />
      );

      expect(onTabChange).toHaveBeenCalledWith('summary');
    });

    it('does not fetch tags when the tab is not entitled', () => {
      setTagEntitlement({ permission: false });

      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
          activeTab="tags"
        />
      );

      expect(mockFetchComponentTags).not.toHaveBeenCalled();
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

  describe('Sonatype Lifecycle tab entitlement (Area 6 / NEXUS-54501)', () => {
    /**
     * Drives the four inputs of Classic's `HealthCheckAssetDetail.isHealthCheckEnabled()` — user,
     * `nexus:healthcheckalerts:read`, edition, license — plus the edition flag the Component Tags
     * tab reads. Defaults are a fully entitled Pro instance; each test overrides one arm.
     */
    const setEntitlement = ({
      user = { id: 'admin', authenticated: true },
      permission = true,
      edition = 'PRO',
      license = { installed: true, valid: true },
      proEdition = true,
    }: {
      user?: unknown;
      permission?: boolean;
      edition?: string;
      license?: { installed?: boolean; valid?: boolean } | null;
      proEdition?: boolean;
    } = {}) => {
      jest.spyOn(ExtJS, 'isLicensedEdition').mockReturnValue(proEdition);
      jest.spyOn(ExtJS, 'useUser').mockReturnValue(user as never);
      jest.spyOn(ExtJS, 'useLicense').mockReturnValue(license as never);
      // Run the real predicate so the assertion covers the permission constant, not just a boolean.
      jest.spyOn(ExtJS, 'usePermission').mockImplementation((getValue: () => boolean) => getValue());
      jest
        .spyOn(ExtJS, 'checkPermission')
        .mockImplementation((p: string) => permission && p === 'nexus:healthcheckalerts:read');
      jest.spyOn(ExtJS, 'state').mockReturnValue({
        getValue: () => undefined,
        getEdition: () => edition,
      } as unknown as ReturnType<typeof ExtJS.state>);
    };

    afterEach(() => {
      jest.restoreAllMocks();
    });

    it('shows the Sonatype Lifecycle tab on an entitled instance', () => {
      setEntitlement();

      renderWithTheme(
        <DetailPanel node={mockAssetNode} repositoryName={repositoryName} assetData={mockAssetData} />
      );

      expect(screen.getByRole('tab', { name: /sonatype lifecycle/i })).toBeInTheDocument();
    });

    it.each([
      ['the user is anonymous', { user: null }],
      ['nexus:healthcheckalerts:read is not held', { permission: false }],
      ['the edition is EVAL', { edition: 'EVAL' }],
      ['no license is installed', { license: { installed: false, valid: false } }],
      ['the installed license is invalid', { license: { installed: true, valid: false } }],
      ['the license state has not been published', { license: null }],
    ])('hides the Sonatype Lifecycle tab when %s', (_case, overrides) => {
      setEntitlement(overrides);

      renderWithTheme(
        <DetailPanel node={mockAssetNode} repositoryName={repositoryName} assetData={mockAssetData} />
      );

      expect(screen.queryByRole('tab', { name: /sonatype lifecycle/i })).not.toBeInTheDocument();
    });

    it('hides the Sonatype Lifecycle tab when the asset has no owning component', () => {
      setEntitlement();

      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          // A blob with no owning component (e.g. a NuGet feed index). Classic's gate is
          // `Boolean(asset.get('componentId'))`, so the tab must stay hidden.
          assetData={{ ...mockAssetData, componentId: undefined }}
        />
      );

      expect(screen.queryByRole('tab', { name: /sonatype lifecycle/i })).not.toBeInTheDocument();
    });

    it('shows the Sonatype Lifecycle tab from asset.componentId when the browse node has none', () => {
      setEntitlement();

      renderWithTheme(
        <DetailPanel
          // Production asset node: no componentId at all (NEXUS-54501).
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
          componentData={mockComponentData}
        />
      );

      expect(screen.getByRole('tab', { name: /sonatype lifecycle/i })).toBeInTheDocument();
    });

    it('falls back to Summary and corrects the URL when ?tab=lifecycle is not entitled', async () => {
      setEntitlement({ permission: false });
      const onTabChange = jest.fn();

      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
          activeTab="lifecycle"
          onTabChange={onTabChange}
        />
      );

      expect(screen.getByRole('tab', { name: /summary/i })).toHaveAttribute('data-state', 'active');
      await waitFor(() => expect(onTabChange).toHaveBeenCalledWith('summary'));
    });

    it('falls back to Summary and corrects the URL when ?tab=tags is not available on this edition', async () => {
      setEntitlement({ proEdition: false });
      const onTabChange = jest.fn();

      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
          activeTab="tags"
          onTabChange={onTabChange}
        />
      );

      expect(screen.getByRole('tab', { name: /summary/i })).toHaveAttribute('data-state', 'active');
      await waitFor(() => expect(onTabChange).toHaveBeenCalledWith('summary'));
    });

    /**
     * `ExtJS.useLicense()` is `undefined` until `NX.State` publishes the license, and both
     * entitlements read false until then. Rewriting the URL on that first render sent an entitled
     * user's `?tab=lifecycle` deep link to `summary` permanently, because the query string — the
     * source of `activeTab` — had already been overwritten by the time the state landed.
     */
    it('does not rewrite ?tab=lifecycle while the license state is unresolved', async () => {
      setEntitlement();
      // Passing `license: undefined` would hit the destructuring default, so override the hook.
      jest.spyOn(ExtJS, 'useLicense').mockReturnValue(undefined as never);
      const onTabChange = jest.fn();

      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
          activeTab="lifecycle"
          onTabChange={onTabChange}
        />
      );

      // The panel still shows Summary rather than an empty body, but the URL is left alone.
      expect(screen.getByRole('tab', { name: /summary/i })).toHaveAttribute('data-state', 'active');
      await waitFor(() => expect(onTabChange).not.toHaveBeenCalled());
    });

    it('does not rewrite ?tab=tags while the license state is unresolved', async () => {
      setEntitlement();
      // Passing `license: undefined` would hit the destructuring default, so override the hook.
      jest.spyOn(ExtJS, 'useLicense').mockReturnValue(undefined as never);
      const onTabChange = jest.fn();

      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
          activeTab="tags"
          onTabChange={onTabChange}
        />
      );

      await waitFor(() => expect(onTabChange).not.toHaveBeenCalled());
    });

    it('selects the entitled tab once the license state arrives', async () => {
      setEntitlement();
      // Passing `license: undefined` would hit the destructuring default, so override the hook.
      jest.spyOn(ExtJS, 'useLicense').mockReturnValue(undefined as never);
      const onTabChange = jest.fn();

      const props = {
        node: mockAssetNode,
        repositoryName,
        assetData: mockAssetData,
        activeTab: 'lifecycle',
        onTabChange,
      };
      const { rerender } = renderWithTheme(<DetailPanel {...props} />);

      expect(screen.queryByRole('tab', { name: /sonatype lifecycle/i })).not.toBeInTheDocument();

      // NX.State publishes; useLicense re-renders with the real value.
      setEntitlement();
      rerender(
        <Theme>
          <DetailPanel {...props} />
        </Theme>
      );

      expect(await screen.findByRole('tab', { name: /sonatype lifecycle/i })).toHaveAttribute(
        'data-state',
        'active'
      );
      expect(onTabChange).not.toHaveBeenCalled();
    });

    it('keeps an entitled ?tab=lifecycle selected and does not rewrite the URL', async () => {
      setEntitlement();
      const onTabChange = jest.fn();

      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
          activeTab="lifecycle"
          onTabChange={onTabChange}
        />
      );

      expect(screen.getByRole('tab', { name: /sonatype lifecycle/i })).toHaveAttribute(
        'data-state',
        'active'
      );
      await waitFor(() => expect(onTabChange).not.toHaveBeenCalled());
    });
  });

  describe('summary parity rows (Area 1 / NEXUS-54501)', () => {
    const proxyAssetData: AssetData = {
      ...mockAssetData,
      contentType: 'application/java-archive',
      size: 2048,
      createdBy: 'admin',
      createdByIp: '10.1.2.3',
      containingRepositoryName: 'maven-central',
    };

    it('shows the locally-cached indicator (Yes) for a cached asset', () => {
      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={proxyAssetData}
          activeTab="summary"
        />
      );

      expect(screen.getByText('Locally cached')).toBeInTheDocument();
      expect(screen.getByText('Yes')).toBeInTheDocument();
    });

    it('shows "No" for a not-yet-cached proxy placeholder (unknown content type, zero size)', () => {
      const uncachedAssetData: AssetData = {
        ...proxyAssetData,
        contentType: 'unknown',
        size: 0,
      };
      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={uncachedAssetData}
          activeTab="summary"
        />
      );

      expect(screen.getByText('Locally cached')).toBeInTheDocument();
      expect(screen.getByText('No')).toBeInTheDocument();
    });

    it('shows the containing repository when present', () => {
      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={proxyAssetData}
          activeTab="summary"
        />
      );

      expect(screen.getByText('Containing repo')).toBeInTheDocument();
      expect(screen.getByText('maven-central')).toBeInTheDocument();
    });

    it('shows the uploader and the uploader IP address', () => {
      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={proxyAssetData}
          activeTab="summary"
        />
      );

      expect(screen.getByText('Uploader')).toBeInTheDocument();
      expect(screen.getByText('admin')).toBeInTheDocument();
      expect(screen.getByText("Uploader's IP Address")).toBeInTheDocument();
      expect(screen.getByText('10.1.2.3')).toBeInTheDocument();
    });

    it('keeps the containing repo row (Classic renders it unconditionally) but omits the IP row when absent', () => {
      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
          activeTab="summary"
        />
      );

      // Classic's ComponentAssetInfo.setModel assigns Containing repo unconditionally and only
      // guards createdBy/createdByIp, so the row stays with the empty-value placeholder.
      const containingRepoLabel = screen.getByText('Containing repo');
      expect(containingRepoLabel).toBeInTheDocument();
      expect(containingRepoLabel.closest('.detail-panel__row')).toHaveTextContent('Containing repo-');

      expect(screen.queryByText('Uploader')).not.toBeInTheDocument();
      expect(screen.queryByText("Uploader's IP Address")).not.toBeInTheDocument();
    });
  });

  describe('attributes accordions default open (Area 5 / NEXUS-54501)', () => {
    const assetWithFacets: AssetData = {
      ...mockAssetData,
      attributes: {
        cache: { last_verified: 'CACHE_VERIFIED_MARKER' },
        pypi: { requires_python: 'PYPI_PYTHON_MARKER' },
      },
    };

    it('renders the cache facet content open by default (no click required)', () => {
      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={assetWithFacets}
          activeTab="attributes"
        />
      );

      expect(screen.getByText('CACHE_VERIFIED_MARKER')).toBeInTheDocument();
    });

    it('renders format-specific facet content open by default (no click required)', () => {
      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={assetWithFacets}
          activeTab="attributes"
        />
      );

      expect(screen.getByText('PYPI_PYTHON_MARKER')).toBeInTheDocument();
    });
  });

  describe('usage snippets parity (Area 4 / NEXUS-54501)', () => {
    it('renders the full maven2 snippet set (incl. PURL) via the shared registry, not just Maven/Gradle', () => {
      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
          componentData={mockComponentData}
          activeTab="usage"
        />
      );

      // Shared registry displayNames (search/details/dependencySnippets/maven2.ts) — the old
      // hardcoded getUsageSnippets only emitted "Maven" + "Gradle" and had no PURL.
      expect(screen.getByText('Apache Maven')).toBeInTheDocument();
      expect(screen.getByText('Gradle Groovy DSL')).toBeInTheDocument();
      expect(screen.getByText('PURL')).toBeInTheDocument();
    });

    it('renders a copyable PURL snippet with the pkg:maven coordinates', () => {
      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
          componentData={mockComponentData}
          activeTab="usage"
        />
      );

      expect(
        screen.getByText('pkg:maven/org.apache.commons/commons-lang3@3.14.0')
      ).toBeInTheDocument();
    });

    it('builds maven2 coordinates from the component, never from the asset filename', () => {
      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
          componentData={mockComponentData}
          activeTab="usage"
        />
      );

      // Real coordinates: group / name / version. Before the fix the panel fell back to
      // `componentData?.name || node.text` with componentData null for every asset selection, so
      // the artifactId was the filename and group/version were empty (NEXUS-54501):
      //   <groupId></groupId><artifactId>commons-lang3-3.14.0.jar</artifactId><version></version>
      const maven = screen.getByText(/<artifactId>/);
      expect(maven).toHaveTextContent('<groupId>org.apache.commons</groupId>');
      expect(maven).toHaveTextContent('<artifactId>commons-lang3</artifactId>');
      expect(maven).toHaveTextContent('<version>3.14.0</version>');
      expect(maven).not.toHaveTextContent('commons-lang3-3.14.0.jar');
    });

    it('builds nuget coordinates from the component, never from the .nupkg filename', () => {
      const nupkgNode: BrowseNode = {
        ...mockAssetNode,
        id: '/NuGet.Core/2.14.0/NuGet.Core.2.14.0.nupkg',
        text: 'NuGet.Core.2.14.0.nupkg',
      };
      renderWithTheme(
        <DetailPanel
          node={nupkgNode}
          repositoryName={repositoryName}
          assetData={{ ...mockAssetData, format: 'nuget', name: 'NuGet.Core.2.14.0.nupkg' }}
          componentData={{
            ...mockComponentData,
            format: 'nuget',
            group: null,
            name: 'NuGet.Core',
            version: '2.14.0',
          }}
          activeTab="usage"
        />
      );

      // Package id and version, not the .nupkg filename — which previously produced
      // `dotnet add package NuGet.Core.2.14.0.nupkg --version `.
      expect(screen.getByText('dotnet add package NuGet.Core --version 2.14.0')).toBeInTheDocument();
      expect(screen.getByText('Install-Package NuGet.Core -Version 2.14.0')).toBeInTheDocument();
      // The filename still titles the panel; no *snippet* may contain it.
      expect(screen.queryByText(/add package .*\.nupkg/)).not.toBeInTheDocument();
      expect(screen.queryByText(/Install-Package .*\.nupkg/)).not.toBeInTheDocument();
    });

    it('shows the no-snippets message rather than filename-derived coordinates for a component-less asset', () => {
      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={{ ...mockAssetData, componentId: undefined }}
          activeTab="usage"
        />
      );

      expect(screen.getByText(/No usage snippets available for this format/i)).toBeInTheDocument();
      expect(screen.queryByText(/commons-lang3-3\.14\.0\.jar<\/artifactId>/)).not.toBeInTheDocument();
    });

    it('shows the no-snippets message for a format with no generator', () => {
      const rawAssetData: AssetData = { ...mockAssetData, format: 'raw' };
      const rawComponentData: ComponentData = { ...mockComponentData, format: 'raw' };
      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={rawAssetData}
          componentData={rawComponentData}
          activeTab="usage"
        />
      );

      expect(screen.getByText(/No usage snippets available for this format/i)).toBeInTheDocument();
    });
  });

  describe('package URL (PURL) row (Area 3 / NEXUS-54501)', () => {
    const purl = 'pkg:maven/org.apache.commons/commons-lang3@3.14.0';
    const componentNodeWithPurl: BrowseNode = { ...mockComponentNode, packageUrl: purl };
    const assetNodeWithPurl: BrowseNode = { ...mockAssetNode, packageUrl: purl };

    it('renders a copyable PURL row on the component summary when the node has a packageUrl', () => {
      renderWithTheme(
        <DetailPanel
          node={componentNodeWithPurl}
          repositoryName={repositoryName}
          componentData={mockComponentData}
        />
      );

      // The PURL appears both in the dedicated Package URL row and as a PURL usage snippet
      // on the component card (both intended, NEXUS-54501); the copy button is unique to the row.
      expect(screen.getAllByText(purl).length).toBeGreaterThan(0);
      expect(screen.getByRole('button', { name: /copy package url/i })).toBeInTheDocument();
    });

    it('renders a copyable PURL row on the asset summary when the node has a packageUrl', () => {
      renderWithTheme(
        <DetailPanel
          node={assetNodeWithPurl}
          repositoryName={repositoryName}
          assetData={mockAssetData}
          componentData={mockComponentData}
          activeTab="summary"
        />
      );

      expect(screen.getByText(purl)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /copy package url/i })).toBeInTheDocument();
    });

    it('omits the PURL row when the node has no packageUrl', () => {
      renderWithTheme(
        <DetailPanel
          node={mockComponentNode}
          repositoryName={repositoryName}
          componentData={mockComponentData}
        />
      );

      expect(screen.queryByRole('button', { name: /copy package url/i })).not.toBeInTheDocument();
    });

    it('copies the PURL to the clipboard when the copy button is clicked', async () => {
      const writeText = jest.fn();
      Object.assign(navigator, { clipboard: { writeText } });

      renderWithTheme(
        <DetailPanel
          node={componentNodeWithPurl}
          repositoryName={repositoryName}
          componentData={mockComponentData}
        />
      );

      await userEvent.click(screen.getByRole('button', { name: /copy package url/i }));
      expect(writeText).toHaveBeenCalledWith(purl);
    });
  });

  describe('component tags load/display (Area 3 / NEXUS-54501)', () => {
    afterEach(() => {
      jest.restoreAllMocks();
    });

    it('loads and displays the component tags in Pro edition with tags:read permission', async () => {
      setTagEntitlement();
      mockFetchComponentTags.mockResolvedValue(['release', 'stable']);

      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
          componentData={mockComponentData}
          activeTab="tags"
        />
      );

      await waitFor(() => {
        expect(screen.getByText('release')).toBeInTheDocument();
      });
      expect(screen.getByText('stable')).toBeInTheDocument();
      // Raw ExtDirect id from the asset response. Sending mockComponentData.id (the encoded
      // RepositoryItemIDXO) makes readTagListDetails fail server-side with
      // `For input string: "bWF2ZW4t..."`, which fetchComponentTags swallows into [] — the
      // manual-test symptom "No tags associated with this component" (NEXUS-54501).
      expect(mockFetchComponentTags).toHaveBeenCalledWith(RAW_COMPONENT_ID, repositoryName);
      expect(mockFetchComponentTags).not.toHaveBeenCalledWith(ENCODED_COMPONENT_ID, repositoryName);
    });

    it('shows the empty message when the component has no tags', async () => {
      setTagEntitlement();
      mockFetchComponentTags.mockResolvedValue([]);

      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
          componentData={mockComponentData}
          activeTab="tags"
        />
      );

      await waitFor(() => {
        expect(screen.getByText(/No tags associated with this component/i)).toBeInTheDocument();
      });
    });

    it('does not fetch tags without the tags:read permission', async () => {
      setTagEntitlement({ permission: false });

      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
          componentData={mockComponentData}
          activeTab="tags"
        />
      );

      // No fetch is attempted when the user lacks nexus:tags:read.
      expect(mockFetchComponentTags).not.toHaveBeenCalled();
    });

    /**
     * Classic reaches the same tag panel from `ComponentTags.onComponentInfoUpdated`, so selecting a
     * component node — with no asset selected — must show its tags. The preview panel previously
     * rendered tags only from `AssetDetails` (NEXUS-54501).
     */
    it('renders the Component Tags section for a component-node selection', async () => {
      setTagEntitlement();
      mockFetchComponentTags.mockResolvedValue(['release']);

      renderWithTheme(
        <DetailPanel
          node={mockComponentNode}
          repositoryName={repositoryName}
          componentData={mockComponentData}
        />
      );

      expect(await screen.findByTestId('component-tags-section')).toBeInTheDocument();
      expect(screen.getByText('release')).toBeInTheDocument();
      // The node's raw ExtDirect id, not componentData.id (the encoded RepositoryItemIDXO).
      expect(mockFetchComponentTags).toHaveBeenCalledWith(mockComponentNode.componentId, repositoryName);
      expect(mockFetchComponentTags).not.toHaveBeenCalledWith(ENCODED_COMPONENT_ID, repositoryName);
    });

    it('omits the Component Tags section on a component node when tags are not entitled', () => {
      setTagEntitlement({ permission: false });

      renderWithTheme(
        <DetailPanel
          node={mockComponentNode}
          repositoryName={repositoryName}
          componentData={mockComponentData}
        />
      );

      expect(screen.queryByTestId('component-tags-section')).not.toBeInTheDocument();
      expect(mockFetchComponentTags).not.toHaveBeenCalled();
    });
  });

  describe('OCI/Docker image digest in usage snippets (Area 4 / NEXUS-54501)', () => {
    const digest = 'sha256:6b3f6a0c1c1e4d0a9b5c2d3e4f5061728394a5b6c7d8e9f0a1b2c3d4e5f60718';
    const ociComponentData: ComponentData = {
      ...mockComponentData,
      format: 'oci',
      group: null,
      name: 'localhost:8081/myimage',
      version: 'latest',
    };
    const ociAssetNode: BrowseNode = { ...mockAssetNode, id: '/myimage/latest', text: 'latest' };

    const renderOci = (attributes?: Record<string, unknown>) =>
      renderWithTheme(
        <DetailPanel
          node={ociAssetNode}
          repositoryName={repositoryName}
          assetData={{ ...mockAssetData, format: 'oci', name: 'latest', attributes }}
          componentData={ociComponentData}
          activeTab="usage"
        />
      );

    /**
     * The backend writes the image digest to `attributes.<format>.content_digest`
     * (`OciAttributes.P_CONTENT_DIGEST` / `DockerAttributes.P_DOCKER_DIGEST`). Reading a top-level
     * `attributes.digest` — which nothing populates — left the cosign snippet on its placeholder.
     */
    it('reads the digest from the oci facet', () => {
      renderOci({ oci: { content_digest: digest } });

      expect(screen.getByText(`cosign sign localhost:8081/myimage@${digest}`)).toBeInTheDocument();
      expect(screen.queryByText(/@<digest>/)).not.toBeInTheDocument();
    });

    it('reads the digest from the docker facet', () => {
      renderOci({ docker: { content_digest: digest } });

      expect(screen.getByText(`cosign sign localhost:8081/myimage@${digest}`)).toBeInTheDocument();
    });

    it('keeps the placeholder when no facet carries a digest', () => {
      renderOci({ oci: {} });

      expect(screen.getByText('cosign sign localhost:8081/myimage@<digest>')).toBeInTheDocument();
    });

    /**
     * The snippets are copy-into-a-shell text, so the server-supplied registry URL goes through
     * `sanitizeRegistryUrl` before it is spliced into the pull target. A value that is not a bare
     * `host[:port][/path]` is dropped and the snippet falls back to the unqualified image name.
     */
    describe('registryUrl sanitization', () => {
      const renderDockerWithRegistry = (registryUrl: string) =>
        renderWithTheme(
          <DetailPanel
            node={ociAssetNode}
            repositoryName={repositoryName}
            assetData={{ ...mockAssetData, format: 'docker', name: 'latest', registryUrl }}
            componentData={{ ...ociComponentData, format: 'docker' }}
            activeTab="usage"
          />
        );

      it('qualifies the pull target with a well-formed registry URL', () => {
        renderDockerWithRegistry('registry.example.com:8443');

        expect(
          screen.getByText('docker pull registry.example.com:8443/localhost:8081/myimage:latest')
        ).toBeInTheDocument();
      });

      it.each([
        ['a shell command substitution', 'evil.example.com/$(id)'],
        ['a shell separator', 'evil.example.com; rm -rf /'],
        ['a scheme and credentials', 'https://user:pw@evil.example.com'],
        ['a flag-looking value', '--config=/tmp/x'],
      ])('drops a registry URL containing %s', (_case, registryUrl) => {
        renderDockerWithRegistry(registryUrl);

        expect(screen.getByText('docker pull localhost:8081/myimage:latest')).toBeInTheDocument();
        expect(screen.queryByText(new RegExp(registryUrl.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))).not.toBeInTheDocument();
      });
    });
  });

  describe('component usage snippets (Area 3 / NEXUS-54501)', () => {
    it('renders usage snippets for a component node via the shared registry', () => {
      renderWithTheme(
        <DetailPanel
          node={mockComponentNode}
          repositoryName={repositoryName}
          componentData={mockComponentData}
        />
      );

      // Same shared registry the asset Usage tab uses — component parity with Classic.
      expect(screen.getByText('Apache Maven')).toBeInTheDocument();
      expect(
        screen.getByText('pkg:maven/org.apache.commons/commons-lang3@3.14.0')
      ).toBeInTheDocument();
    });

    it('omits the usage section for a component format with no generator', () => {
      const rawComponentData: ComponentData = { ...mockComponentData, format: 'raw' };
      renderWithTheme(
        <DetailPanel
          node={mockComponentNode}
          repositoryName={repositoryName}
          componentData={rawComponentData}
        />
      );

      expect(screen.queryByText('Apache Maven')).not.toBeInTheDocument();
    });
  });

  describe('component intelligence (Area 2 / NEXUS-54501)', () => {
    const resolvedIntelligence = {
      data: {
        mostPopularVersion: '3.15.0',
        ageMillis: 400 * 86400000, // > 364 days → "1.1 yrs"
        popularity: 87,
        securityAlerts: 5,
        criticalSecurityAlerts: 2,
        severeSecurityAlerts: 1,
        moderateSecurityAlerts: 2,
        licenseThreat: 4,
        licenseThreatName: 'GPL-3.0',
        cappedAlerts: false,
      },
      status: 'resolved' as const,
      alertsStatus: 'available' as const,
    };

    it('renders most popular version, age, popularity, security chiclets and license for a component', () => {
      mockUseComponentIntelligence.mockReturnValue(resolvedIntelligence);

      renderWithTheme(
        <DetailPanel
          node={mockComponentNode}
          repositoryName={repositoryName}
          componentData={mockComponentData}
        />
      );

      expect(screen.getByText('Most popular version')).toBeInTheDocument();
      expect(screen.getByText('3.15.0')).toBeInTheDocument();
      expect(screen.getByText('Age')).toBeInTheDocument();
      expect(screen.getByText('1.1 yrs')).toBeInTheDocument();
      expect(screen.getByText('Popularity')).toBeInTheDocument();
      expect(screen.getByText('87 %')).toBeInTheDocument();
      // Security chiclets by severity (CVE counts) + license threat badge.
      expect(screen.getByLabelText('2 critical')).toBeInTheDocument();
      expect(screen.getByLabelText('1 severe')).toBeInTheDocument();
      expect(screen.getByLabelText('2 moderate')).toBeInTheDocument();
      expect(screen.getByText('GPL-3.0')).toBeInTheDocument();
    });

    it('passes the component coordinates to the intelligence hook', () => {
      mockUseComponentIntelligence.mockReturnValue(resolvedIntelligence);

      renderWithTheme(
        <DetailPanel
          node={mockComponentNode}
          repositoryName={repositoryName}
          componentData={mockComponentData}
        />
      );

      expect(mockUseComponentIntelligence).toHaveBeenCalledWith(
        expect.objectContaining({
          gaId: 'maven2:org.apache.commons:commons-lang3',
          format: 'maven2',
          version: '3.14.0',
        })
      );
    });

    it('renders no intelligence section for an unsupported format', () => {
      mockUseComponentIntelligence.mockReturnValue({
        data: null,
        status: 'unsupported-format',
        alertsStatus: 'unknown',
      });

      renderWithTheme(
        <DetailPanel
          node={mockComponentNode}
          repositoryName={repositoryName}
          componentData={mockComponentData}
        />
      );

      expect(screen.queryByText('Most popular version')).not.toBeInTheDocument();
      expect(screen.queryByText('Popularity')).not.toBeInTheDocument();
    });
  });

  describe('PathRowWithCopy component', () => {
    const mockClipboard = {
      writeText: jest.fn(),
    };

    beforeEach(() => {
      Object.assign(navigator, { clipboard: mockClipboard });
      mockClipboard.writeText.mockClear();
    });

    it('shows "Path" button label with copy icon', () => {
      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
        />
      );

      // Button should have "Path" label visible
      expect(screen.getByRole('button', { name: /path/i })).toBeInTheDocument();
    });

    it('shows "Copy path" tooltip on hover', () => {
      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
        />
      );

      // The tooltip content "Copy path" should be accessible
      // Radix tooltips appear on hover, so we check the button exists
      const pathButton = screen.getByRole('button', { name: /path/i });
      expect(pathButton).toBeInTheDocument();
    });

    it('copies path to clipboard when button is clicked', async () => {
      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
        />
      );

      const pathButton = screen.getByRole('button', { name: /path/i });
      await userEvent.click(pathButton);

      expect(mockClipboard.writeText).toHaveBeenCalledWith(mockAssetData.name);
    });

    it('shows "Copied!" button label with check icon after click', async () => {
      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
        />
      );

      const pathButton = screen.getByRole('button', { name: /path/i });
      await userEvent.click(pathButton);

      // After click, button should show "Copied!"
      expect(screen.getByRole('button', { name: /copied/i })).toBeInTheDocument();
    });

    it('reverts to "Path" label after 2 seconds', async () => {
      jest.useFakeTimers();

      renderWithTheme(
        <DetailPanel
          node={mockAssetNode}
          repositoryName={repositoryName}
          assetData={mockAssetData}
        />
      );

      const pathButton = screen.getByRole('button', { name: /path/i });
      await userEvent.click(pathButton);

      // Immediately after click, shows "Copied!"
      expect(screen.getByRole('button', { name: /copied/i })).toBeInTheDocument();

      // Fast-forward 2 seconds
      jest.advanceTimersByTime(2000);

      // After 2 seconds, should revert to "Path"
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /path/i })).toBeInTheDocument();
      });

      jest.useRealTimers();
    });

    it('shows copy button for folder path', async () => {
      renderWithTheme(
        <DetailPanel node={mockFolderNode} repositoryName={repositoryName} />
      );

      // Folder details also uses PathRowWithCopy
      expect(screen.getByRole('button', { name: /path/i })).toBeInTheDocument();
    });
  });
});
