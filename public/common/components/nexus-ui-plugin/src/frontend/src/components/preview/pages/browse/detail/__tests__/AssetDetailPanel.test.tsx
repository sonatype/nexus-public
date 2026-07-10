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

import { AssetDetailPanel } from '../AssetDetailPanel';
import type { AssetXO, ComponentXO } from '../detail.types';
import ExtJS from '../../../../../../interface/ExtJS';

// Test wrapper with Radix Theme
const renderWithTheme = (ui: React.ReactElement) => {
  return render(<Theme>{ui}</Theme>);
};

// Mock asset data
const mockAsset: AssetXO = {
  id: 'asset-123',
  name: '/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar',
  format: 'maven2',
  contentType: 'application/java-archive',
  size: 1572864, // 1.5 MB
  repositoryName: 'maven-releases',
  containingRepositoryName: 'maven-releases',
  downloadUrl: 'https://server.example.com/custom-context/repository/maven-releases/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar',
  blobCreated: '2024-01-15T10:30:00Z',
  blobUpdated: '2024-01-15T10:30:00Z',
  lastDownloaded: '2024-01-20T14:25:00Z',
  downloadCount: 42,
  blobRef: 'default@12345',
  componentId: 'component-123',
  createdBy: 'admin',
  createdByIp: '192.168.1.100',
  attributes: {
    checksum: {
      sha1: 'abc123def456',
      sha256: 'xyz789abc123def456ghi789jkl012mno345',
      md5: 'aabbccdd',
    },
    maven2: {
      groupId: 'org.apache.commons',
      artifactId: 'commons-lang3',
      version: '3.14.0',
      extension: 'jar',
    },
  },
};

const mockComponent: ComponentXO = {
  id: 'component-123',
  repositoryName: 'maven-releases',
  group: 'org.apache.commons',
  name: 'commons-lang3',
  version: '3.14.0',
  format: 'maven2',
};

const mockAssetMinimal: AssetXO = {
  id: 'asset-456',
  name: '/file.txt',
  format: 'raw',
  contentType: 'text/plain',
  size: 100,
  repositoryName: 'raw-hosted',
  blobCreated: null,
  blobUpdated: null,
  lastDownloaded: null,
  blobRef: null,
  componentId: null,
  createdBy: null,
  createdByIp: null,
};

const mockAssetUncached: AssetXO = {
  ...mockAssetMinimal,
  contentType: 'unknown',
  size: 0,
};

describe('AssetDetailPanel', () => {
  // Mock window.open
  const mockWindowOpen = jest.fn();
  const originalWindowOpen = window.open;

  beforeEach(() => {
    window.open = mockWindowOpen;
    mockWindowOpen.mockClear();
  });

  afterEach(() => {
    window.open = originalWindowOpen;
  });

  describe('rendering', () => {
    it('renders asset filename as heading', () => {
      renderWithTheme(<AssetDetailPanel asset={mockAsset} />);

      expect(screen.getByRole('heading', { name: 'commons-lang3-3.14.0.jar' })).toBeInTheDocument();
    });

    // Note: Repository name test removed - shown in parent DetailPanel context

    it('renders format in Attributes tab', async () => {
      renderWithTheme(<AssetDetailPanel asset={mockAsset} />);

      // Click on Attributes tab to see format-specific attributes
      await userEvent.click(screen.getByRole('tab', { name: /attributes/i }));

      // Format-specific attributes contain maven2 values
      // The maven2 section header should be visible
      expect(screen.getByText('maven2')).toBeInTheDocument();
    });

    it('renders content type', () => {
      renderWithTheme(<AssetDetailPanel asset={mockAsset} />);

      expect(screen.getByText('application/java-archive')).toBeInTheDocument();
    });

    it('renders formatted file size', () => {
      renderWithTheme(<AssetDetailPanel asset={mockAsset} />);

      expect(screen.getByText('1.50 MB')).toBeInTheDocument();
    });

    it('renders full asset path', () => {
      renderWithTheme(<AssetDetailPanel asset={mockAsset} />);

      expect(
        screen.getByText('/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar')
      ).toBeInTheDocument();
    });

    it('renders download count with unit', () => {
      renderWithTheme(<AssetDetailPanel asset={mockAsset} />);

      expect(screen.getByText('42 times')).toBeInTheDocument();
    });

    it('renders blob reference', () => {
      renderWithTheme(<AssetDetailPanel asset={mockAsset} />);

      expect(screen.getByText('default@12345')).toBeInTheDocument();
    });

    it('renders uploaded by', () => {
      renderWithTheme(<AssetDetailPanel asset={mockAsset} />);

      expect(screen.getByText('admin')).toBeInTheDocument();
    });

    it('renders uploaded from IP', () => {
      renderWithTheme(<AssetDetailPanel asset={mockAsset} />);

      expect(screen.getByText('192.168.1.100')).toBeInTheDocument();
    });
  });

  describe('component information', () => {
    it('renders format-specific attributes in Attributes tab when available', async () => {
      renderWithTheme(<AssetDetailPanel asset={mockAsset} component={mockComponent} />);

      // Click on Attributes tab
      await userEvent.click(screen.getByRole('tab', { name: /attributes/i }));

      // Should show maven2 attributes (format-specific)
      // The AttributeSection displays key-value pairs from attributes.maven2
      expect(screen.getByText('org.apache.commons')).toBeInTheDocument();
      expect(screen.getByText('commons-lang3')).toBeInTheDocument();
      expect(screen.getByText('3.14.0')).toBeInTheDocument();
    });

    it('renders component group/name/version in Attributes tab when no format-specific attributes', async () => {
      renderWithTheme(<AssetDetailPanel asset={mockAssetMinimal} component={mockComponent} />);

      // Click on Attributes tab
      await userEvent.click(screen.getByRole('tab', { name: /attributes/i }));

      // Should show component info since there are no format-specific attributes
      expect(screen.getByText('Group')).toBeInTheDocument();
      expect(screen.getByText('org.apache.commons')).toBeInTheDocument();
    });

    it('does not render component info when component is not provided', async () => {
      renderWithTheme(<AssetDetailPanel asset={mockAssetMinimal} />);

      // Click on Attributes tab
      await userEvent.click(screen.getByRole('tab', { name: /attributes/i }));

      // Group label should not be present (only shown when component is provided)
      expect(screen.queryByText('Group')).not.toBeInTheDocument();
    });
  });

  describe('locally cached badge', () => {
    it('shows "Yes" badge when asset is cached', () => {
      renderWithTheme(<AssetDetailPanel asset={mockAsset} />);

      expect(screen.getByText('Yes')).toBeInTheDocument();
    });

    it('shows "No" badge when asset is not cached', () => {
      renderWithTheme(<AssetDetailPanel asset={mockAssetUncached} />);

      expect(screen.getByText('No')).toBeInTheDocument();
    });
  });

  describe('checksums', () => {
    it('renders checksum section when checksums are present', () => {
      renderWithTheme(<AssetDetailPanel asset={mockAsset} />);

      expect(screen.getByText('Checksums')).toBeInTheDocument();
    });

    it('renders SHA1 checksum', () => {
      renderWithTheme(<AssetDetailPanel asset={mockAsset} />);

      expect(screen.getByText('SHA1')).toBeInTheDocument();
      expect(screen.getByText('abc123def456')).toBeInTheDocument();
    });

    it('renders SHA256 checksum', () => {
      renderWithTheme(<AssetDetailPanel asset={mockAsset} />);

      expect(screen.getByText('SHA256')).toBeInTheDocument();
    });

    it('renders MD5 checksum', () => {
      renderWithTheme(<AssetDetailPanel asset={mockAsset} />);

      expect(screen.getByText('MD5')).toBeInTheDocument();
      expect(screen.getByText('aabbccdd')).toBeInTheDocument();
    });

    it('does not render checksum section when no checksums', () => {
      renderWithTheme(<AssetDetailPanel asset={mockAssetMinimal} />);

      expect(screen.queryByText('Checksums')).not.toBeInTheDocument();
    });
  });

  describe('download button', () => {
    it('renders download button', () => {
      renderWithTheme(<AssetDetailPanel asset={mockAsset} />);

      const downloadButtons = screen.getAllByRole('button', { name: /download/i });
      expect(downloadButtons.length).toBeGreaterThanOrEqual(1);
    });

    it.each([
      ['root context path (/)',    'https://nexus.example.com/repository/maven-releases/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar'],
      ['custom context path (/nexus)', 'https://nexus.example.com/nexus/repository/maven-releases/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar'],
      ['custom context path (/nxrm)',  'https://nexus.example.com/nxrm/repository/maven-releases/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar'],
    ])('uses server-provided downloadUrl unchanged for %s', async (_label, serverDownloadUrl) => {
      const asset = { ...mockAsset, downloadUrl: serverDownloadUrl };
      renderWithTheme(<AssetDetailPanel asset={asset} />);

      const downloadButtons = screen.getAllByRole('button', { name: /download/i });
      await userEvent.click(downloadButtons[0]);

      expect(mockWindowOpen).toHaveBeenCalledWith(serverDownloadUrl, '_blank');
    });

    it('falls back to client-generated URL when server downloadUrl is absent', async () => {
      const { downloadUrl: _omitted, ...assetWithoutDownloadUrl } = mockAsset;
      renderWithTheme(<AssetDetailPanel asset={assetWithoutDownloadUrl as typeof mockAsset} />);

      const downloadButtons = screen.getAllByRole('button', { name: /download/i });
      await userEvent.click(downloadButtons[0]);

      expect(mockWindowOpen).toHaveBeenCalledWith(
        '/repository/maven-releases/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar',
        '_blank'
      );
    });

    it('calls onDownload when provided', async () => {
      const onDownload = jest.fn();
      renderWithTheme(<AssetDetailPanel asset={mockAsset} onDownload={onDownload} />);

      const downloadButtons = screen.getAllByRole('button', { name: /download/i });
      await userEvent.click(downloadButtons[0]);

      expect(onDownload).toHaveBeenCalledTimes(1);
      expect(mockWindowOpen).not.toHaveBeenCalled();
    });
  });

  describe('delete button', () => {
    it('does not show delete button when canDelete is false', () => {
      renderWithTheme(
        <AssetDetailPanel asset={mockAsset} canDelete={false} onDelete={jest.fn()} />
      );

      expect(screen.queryByRole('button', { name: /delete asset/i })).not.toBeInTheDocument();
    });

    it('does not show delete button when onDelete is not provided', () => {
      renderWithTheme(<AssetDetailPanel asset={mockAsset} canDelete={true} />);

      expect(screen.queryByRole('button', { name: /delete asset/i })).not.toBeInTheDocument();
    });

    it('shows delete button when canDelete is true and onDelete is provided', () => {
      renderWithTheme(
        <AssetDetailPanel asset={mockAsset} canDelete={true} onDelete={jest.fn()} />
      );

      const deleteButtons = screen.getAllByRole('button', { name: /delete asset/i });
      expect(deleteButtons.length).toBeGreaterThanOrEqual(1);
    });

    it('opens confirmation dialog when delete button is clicked', async () => {
      renderWithTheme(
        <AssetDetailPanel asset={mockAsset} canDelete={true} onDelete={jest.fn()} />
      );

      const deleteButtons = screen.getAllByRole('button', { name: /delete asset/i });
      await userEvent.click(deleteButtons[0]);

      expect(screen.getByText('Confirm deletion?')).toBeInTheDocument();
    });

    it('calls onDelete when delete is confirmed', async () => {
      const onDelete = jest.fn();
      renderWithTheme(
        <AssetDetailPanel asset={mockAsset} canDelete={true} onDelete={onDelete} />
      );

      const deleteButtons = screen.getAllByRole('button', { name: /delete asset/i });
      await userEvent.click(deleteButtons[0]);

      const confirmButton = screen.getByRole('button', { name: /^Delete$/i });
      await userEvent.click(confirmButton);

      expect(onDelete).toHaveBeenCalledTimes(1);
    });

    it('closes dialog without calling onDelete when cancelled', async () => {
      const onDelete = jest.fn();
      renderWithTheme(
        <AssetDetailPanel asset={mockAsset} canDelete={true} onDelete={onDelete} />
      );

      const deleteButtons = screen.getAllByRole('button', { name: /delete asset/i });
      await userEvent.click(deleteButtons[0]);

      const cancelButton = screen.getByRole('button', { name: /cancel/i });
      await userEvent.click(cancelButton);

      expect(onDelete).not.toHaveBeenCalled();
    });
  });

  describe('Component Tags tab visibility', () => {
    afterEach(() => {
      jest.restoreAllMocks();
    });

    it('hides Component Tags tab in CE mode', () => {
      jest.spyOn(ExtJS, 'isProEdition').mockReturnValue(false);

      renderWithTheme(<AssetDetailPanel asset={mockAsset} />);

      expect(screen.queryByRole('tab', { name: /component tags/i })).not.toBeInTheDocument();
    });

    it('shows Component Tags tab in Pro Edition', () => {
      jest.spyOn(ExtJS, 'isProEdition').mockReturnValue(true);

      renderWithTheme(<AssetDetailPanel asset={mockAsset} />);

      expect(screen.getByRole('tab', { name: /component tags/i })).toBeInTheDocument();
    });
  });

  describe('null/missing values', () => {
    it('handles null blobCreated', () => {
      renderWithTheme(<AssetDetailPanel asset={mockAssetMinimal} />);

      const blobCreatedLabel = screen.getByText('Blob Created');
      const row = blobCreatedLabel.closest('[class*="DataListItem"]');
      expect(row).toHaveTextContent('-');
    });

    it('handles null lastDownloaded', () => {
      renderWithTheme(<AssetDetailPanel asset={mockAssetMinimal} />);

      // Should show "Never" for last downloaded
      expect(screen.getByText('Never')).toBeInTheDocument();
    });

    it('does not render blob ref when null', () => {
      renderWithTheme(<AssetDetailPanel asset={mockAssetMinimal} />);

      expect(screen.queryByText('Blob Reference')).not.toBeInTheDocument();
    });

    it('does not render uploaded by when null', () => {
      renderWithTheme(<AssetDetailPanel asset={mockAssetMinimal} />);

      expect(screen.queryByText('Uploaded By')).not.toBeInTheDocument();
    });

    it('does not render uploaded from IP when null', () => {
      renderWithTheme(<AssetDetailPanel asset={mockAssetMinimal} />);

      expect(screen.queryByText('Uploaded From IP')).not.toBeInTheDocument();
    });
  });
});

