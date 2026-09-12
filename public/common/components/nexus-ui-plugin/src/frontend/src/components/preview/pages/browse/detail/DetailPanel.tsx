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

import React, { useState, useCallback, useEffect } from 'react';
import {
  Box,
  Button,
  Card,
  Callout,
  Code,
  Flex,
  Heading,
  Separator,
  Spinner,
  Tabs,
  Text,
  Tooltip,
} from '@radix-ui/themes';
import {
  AlertCircle,
  Check,
  ChevronDown,
  Copy,
  Download,
  ExternalLink,
  Shield,
  Tag,
  Trash2,
} from 'lucide-react';

import { ExtJS } from '../../../../../interface/ExtJS';
import { DeleteDialog } from '../actions/DeleteDialog';
import { deleteComponent, deleteAsset, deleteFolder, fetchBrowseNodes } from '../browse.api';
import type { DeleteItemInfo } from '../actions/actions.types';
import type { BrowseNode } from '../tree/browse-tree.types';
import { formatFileSize, formatDate, getAssetDownloadUrl, sanitizeRegistryUrl } from './detail.utils';
import { DeepResearchLink, useToast } from '../../../shared';
import { isIqServerEnabled } from '../repository-list/useRepositoryList';
import {
  formatAttributeValue,
  shouldDisplayAttributeFacet,
} from '../browse.constants';

import './DetailPanel.scss';

/**
 * UI Strings for the detail panel.
 */
const STRINGS = {
  emptyTitle: 'No Item Selected',
  emptyDescription: 'Select An Item In The Tree To View Its Details.',
  loading: 'Loading details...',
  error: 'Error loading details',

  component: {
    title: 'Component',
    repository: 'Repository',
    format: 'Format',
    group: 'Group',
    name: 'Name',
    version: 'Version',
    deleteButton: 'Delete Component',
  },

  asset: {
    title: 'Asset',
    repository: 'Repository',
    format: 'Format',
    path: 'Path',
    contentType: 'Content Type',
    size: 'Size',
    blobCreated: 'Created',
    blobUpdated: 'Updated',
    lastDownloaded: 'Last Downloaded',
    downloadButton: 'Download',
    deleteButton: 'Delete Asset',
    viewButton: 'View',
  },

  folder: {
    title: 'Folder',
    path: 'Path',
    repository: 'Repository',
    deleteButton: 'Delete Folder',
    deleteWarning: 'This will delete all contents within this folder.',
  },
};

/**
 * Simple data row for the detail panel.
 */
interface DetailRowProps {
  label: string;
  value: string | null | undefined;
  className?: string;
  /** Whether this is a path value (gets special wrapping treatment) */
  isPath?: boolean;
}

function DetailRow({ label, value, className = '', isPath = false }: DetailRowProps): JSX.Element {
  const valueClass = isPath ? 'detail-panel__value detail-panel__value--path' : 'detail-panel__value';
  return (
    <Flex className={`detail-panel__row ${className}`} justify="between" py="2">
      <Text size="2" color="gray" className="detail-panel__label">
        {label}
      </Text>
      <Text size="2" className={valueClass}>
        {value || '-'}
      </Text>
    </Flex>
  );
}

/**
 * Path row with copy button — same affordance as GA Dependencies card.
 */
function PathRowWithCopy({ label, value }: { label: string; value: string | null | undefined }): JSX.Element {
  const [copied, setCopied] = useState(false);
  const pathValue = value || '';

  const handleCopy = useCallback(() => {
    if (pathValue) {
      navigator.clipboard.writeText(pathValue);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  }, [pathValue]);

  return (
    <Box className="detail-panel__row detail-panel__row--path" py="2">
      <Text size="2" color="gray" weight="medium" mb="2" className="detail-panel__label">
        {label}
      </Text>
      <Box
        className="detail-panel__path-box"
        p="4"
        style={{
          backgroundColor: 'var(--gray-2)',
          borderRadius: '6px',
        }}
      >
        <Flex align="center" justify="between" gap="2">
          <Code
            size="2"
            color="gray"
            variant="ghost"
            style={{
              whiteSpace: 'pre-wrap',
              wordBreak: 'break-all',
              flex: 1,
              overflowX: 'auto',
            }}
          >
            {pathValue || '-'}
          </Code>
          {pathValue && (
            <Tooltip content={copied ? 'Copied!' : 'Copy path'}>
              <Button variant="soft" color="blue" size="2" onClick={handleCopy}>
                {copied ? <Check size={14} /> : <Copy size={14} />}
                {copied ? 'Copied!' : 'Path'}
              </Button>
            </Tooltip>
          )}
        </Flex>
      </Box>
    </Box>
  );
}

/**
 * Collapsible facet section for attribute groups (Firewall, Npm, Docker, etc.).
 * Adds visual hierarchy with collapsible sections, dividers, and bold headers.
 * Default is closed; call sites explicitly pass defaultOpen for important facets.
 */
function FacetSection({
  title,
  children,
  defaultOpen = false,
  testId,
}: {
  title: string;
  children: React.ReactNode;
  defaultOpen?: boolean;
  testId?: string;
}): JSX.Element {
  const [isOpen, setIsOpen] = useState(defaultOpen);

  const handleToggle = () => setIsOpen(!isOpen);
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      handleToggle();
    }
  };

  return (
    <Box mt="4" data-testid={testId}>
      {/* Section header with bold title and expand/collapse toggle */}
      <Flex
        align="center"
        justify="between"
        py="2"
        px="2"
        style={{
          cursor: 'pointer',
          backgroundColor: 'var(--gray-a2)',
          borderRadius: 'var(--radius-2)',
          marginBottom: isOpen ? 'var(--space-2)' : 0,
        }}
        onClick={handleToggle}
        onKeyDown={handleKeyDown}
        role="button"
        tabIndex={0}
        aria-expanded={isOpen}
      >
        <Text size="2" weight="bold" style={{ textTransform: 'capitalize' }}>
          {title}
        </Text>
        <ChevronDown
          size={16}
          style={{
            transition: 'transform 0.2s ease',
            transform: isOpen ? 'rotate(180deg)' : 'rotate(0deg)',
            color: 'var(--gray-9)',
          }}
        />
      </Flex>
      {/* Collapsible content */}
      {isOpen && (
        <Box pt="1" px="2">
          {children}
        </Box>
      )}
    </Box>
  );
}

/**
 * Props for DetailPanel component.
 */
export interface DetailPanelProps {
  /** Currently selected node (null if none) */
  node: BrowseNode | null;
  /** Repository name for context */
  repositoryName: string;
  /** Component details if available */
  componentData?: ComponentData | null;
  /** Asset details if available */
  assetData?: AssetData | null;
  /** Whether details are loading */
  loading?: boolean;
  /** Error message if loading failed */
  error?: string | null;
  /** Callback when item is deleted */
  onDeleted?: () => void;
  /** Whether user has delete permission */
  canDelete?: boolean;
  /** Active tab from URL (for URL-based navigation) */
  activeTab?: string;
  /** Callback when tab changes (for URL updates) */
  onTabChange?: (tab: string) => void;
}

/**
 * Component data from the API.
 */
export interface ComponentData {
  id: string;
  repositoryName: string;
  format: string;
  group: string | null;
  name: string;
  version: string | null;
}

/**
 * Asset data from the API.
 */
export interface AssetData {
  id: string;
  name: string;
  format: string;
  contentType?: string;
  size?: number;
  repositoryName: string;
  blobCreated: string | null;
  blobUpdated: string | null;
  lastDownloaded: string | null;
  path?: string;
  downloadUrl?: string;
  blobRef?: string;
  createdBy?: string;
  createdByIp?: string; // mapped from API but intentionally not rendered — privacy concern
  checksum?: Record<string, string>;
  /**
   * Format-specific attributes bag from the backend (e.g. `docker.registryUrl`).
   * Intentionally loose (`Record<string, unknown>`) since shape varies per format.
   */
  attributes?: Record<string, unknown>;
  registryUrl?: string;
}

/**
 * DetailPanel displays the details of a selected node in the browse tree.
 *
 * Features:
 * - Shows component, asset, or folder details based on node type
 * - Download button for assets
 * - Delete button with confirmation dialog
 * - Loading and error states
 */
export function DetailPanel({
  node,
  repositoryName,
  componentData,
  assetData,
  loading = false,
  error = null,
  onDeleted,
  canDelete = false,
  activeTab = 'summary',
  onTabChange,
}: DetailPanelProps): JSX.Element {
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  /** When true, the pending delete targets the entire package folder; when false, just the metadata asset. */
  const [deleteAsFolder, setDeleteAsFolder] = useState(false);
  const toast = useToast();

  /**
   * Get the item info for delete dialog.
   * For package root nodes (asset + non-leaf), the target depends on `deleteAsFolder`:
   * - false → delete the metadata asset only
   * - true  → delete the entire package folder
   */
  const getDeleteItemInfo = useCallback((): DeleteItemInfo | null => {
    if (!node) return null;

    // Package root: asset node that is also a folder (has children)
    if (node.type === 'asset' && !node.leaf) {
      return deleteAsFolder
        ? { type: 'folder', id: node.id, name: node.text, repositoryName }
        : { type: 'asset', id: node.assetId || node.id, name: node.text, repositoryName };
    }

    switch (node.type) {
      case 'component':
        return {
          type: 'component',
          id: node.componentId || node.id,
          name: node.text,
          repositoryName,
        };
      case 'asset':
        return {
          type: 'asset',
          id: node.assetId || node.id,
          name: node.text,
          repositoryName,
        };
      case 'folder':
        return {
          type: 'folder',
          id: node.id,
          name: node.text,
          repositoryName,
        };
      default:
        return null;
    }
  }, [node, repositoryName, deleteAsFolder]);

  /**
   * Handle delete confirmation.
   * Uses Promise.race with a timeout to prevent indefinite hanging when the
   * backend returns a 500 (e.g., missing AptHostedMetadataFacet on cloud).
   * On error the tree is still refreshed because the delete may have partially
   * succeeded (asset removed but metadata update failed).
   */
  const handleDeleteConfirm = useCallback(async () => {
    setIsDeleting(true);
    setDeleteError(null);

    const DELETE_TIMEOUT_MS = 30_000;

    try {
      // Determine delete target. Order matters and MUST follow the selected node's type so the
      // action matches what the panel renders:
      // 1. Package root "delete entire package" (asset node that has children, deleteAsFolder=true)
      // 2. Standard folder delete (folder nodes were previously a no-op — this is the fix)
      // 3. Asset node -> delete ONLY that asset. The asset detail panel also loads componentData
      //    (to show component info), so we must key off node.type here; otherwise deleting a single
      //    asset would fall through to the componentData branch and cascade-delete the whole
      //    component. For multi-asset components (e.g. a Terraform provider version, which holds one
      //    zip per OS/arch) that wrongly removed every sibling platform.
      // 4. Component node -> delete the component.
      let deletePromise: Promise<unknown>;
      if (node && node.type === 'asset' && !node.leaf && deleteAsFolder) {
        deletePromise = deleteFolder(node.id, repositoryName);
      } else if (node && node.type === 'folder') {
        deletePromise = deleteFolder(node.id, repositoryName);
      } else if (node && node.type === 'asset' && assetData) {
        // assetData.id is the RAW internal id from ExtDirect readAsset (not the
        // base64 RepositoryItemIDXO the REST API expects); deleteAsset encodes it.
        deletePromise = deleteAsset(assetData.id, repositoryName);
      } else if (componentData) {
        deletePromise = deleteComponent({
          id: componentData.id,
          repositoryName: componentData.repositoryName,
          format: componentData.format,
          group: componentData.group,
          name: componentData.name,
          version: componentData.version,
        });
      } else if (assetData) {
        deletePromise = deleteAsset(assetData.id, repositoryName);
      } else {
        deletePromise = Promise.resolve();
      }

      const timeoutPromise = new Promise<never>((_, reject) => {
        setTimeout(
          () => reject(new Error('Delete operation timed out. The item may have been deleted.')),
          DELETE_TIMEOUT_MS,
        );
      });

      await Promise.race([deletePromise, timeoutPromise]);

      setDeleteDialogOpen(false);
      toast.success('Item deleted successfully');
      if (onDeleted) {
        onDeleted();
      }
    } catch (err) {
      const errorMessage =
        err instanceof Error ? err.message : typeof err === 'string' ? err : 'Delete failed.';
      toast.error(errorMessage);
      setDeleteError(`Cannot delete: ${errorMessage}`);
      setDeleteDialogOpen(false);

      // Refresh tree - delete may have partially succeeded on the server
      if (onDeleted) {
        onDeleted();
      }
    } finally {
      setIsDeleting(false);
    }
  }, [onDeleted, componentData, assetData, repositoryName, toast, node, deleteAsFolder]);

  /**
   * Open delete dialog and clear any previous delete error.
   */
  const handleDeleteClick = useCallback(() => {
    setDeleteError(null);
    setDeleteDialogOpen(true);
  }, []);

  /**
   * Handle download click.
   */
  const handleDownload = useCallback(() => {
    if (assetData) {
      const downloadUrl = assetData.downloadUrl ?? getAssetDownloadUrl(repositoryName, assetData.name);
      window.open(downloadUrl, '_blank');
    }
  }, [assetData, repositoryName]);

  // Empty state — Card (detail panel wrapper) + dashed Box (ui-state skill Category 1: no icons, Title Case, data-driven)
  if (!node) {
    return (
      <Card className="detail-panel detail-panel--empty">
        <Box
          p="4"
          style={{
            border: '1px dashed var(--gray-7)',
            borderRadius: 'var(--radius-3)',
            minHeight: '160px',
          }}
        >
          <Flex
            direction="column"
            align="center"
            justify="center"
            gap="2"
            style={{ height: '100%', minHeight: '132px' }}
          >
            <Text size="3" weight="medium">
              {STRINGS.emptyTitle}
            </Text>
            <Text size="1" color="gray" align="center">
              {STRINGS.emptyDescription}
            </Text>
          </Flex>
        </Box>
      </Card>
    );
  }

  // Loading state
  if (loading) {
    return (
      <Flex
        align="center"
        justify="center"
        direction="column"
        className="detail-panel detail-panel--loading"
        p="6"
      >
        <Spinner size="3" />
        <Text color="gray" size="2" mt="3">
          {STRINGS.loading}
        </Text>
      </Flex>
    );
  }

  // Error state
  if (error) {
    return (
      <Flex
        align="center"
        justify="center"
        direction="column"
        className="detail-panel detail-panel--error"
        p="6"
      >
        <Text color="red" size="2">
          {STRINGS.error}: {error}
        </Text>
      </Flex>
    );
  }

  // Render based on node type
  return (
    <Box className="detail-panel" p="4" pt="0">
      {deleteError && (
        <Callout.Root color="red" mb="3" size="2">
          <Callout.Icon>
            <AlertCircle size={16} />
          </Callout.Icon>
          <Callout.Text>{deleteError}</Callout.Text>
        </Callout.Root>
      )}

      {node.type === 'component' && (
        <ComponentDetails
          node={node}
          repositoryName={repositoryName}
          componentData={componentData}
          canDelete={canDelete}
          onDeleteClick={handleDeleteClick}
        />
      )}

      {node.type === 'asset' && node.leaf && (
        <AssetDetails
          node={node}
          repositoryName={repositoryName}
          assetData={assetData}
          componentData={componentData}
          canDelete={canDelete}
          onDeleteClick={handleDeleteClick}
          onDownloadClick={handleDownload}
          activeTab={activeTab}
          onTabChange={onTabChange}
        />
      )}

      {node.type === 'asset' && !node.leaf && (
        <PackageRootDetails
          node={node}
          repositoryName={repositoryName}
          assetData={assetData}
          canDelete={canDelete}
          onDeleteMetadataClick={() => { setDeleteAsFolder(false); handleDeleteClick(); }}
          onDeleteFolderClick={() => { setDeleteAsFolder(true); handleDeleteClick(); }}
        />
      )}

      {node.type === 'folder' && (
        <FolderDetails
          node={node}
          repositoryName={repositoryName}
          canDelete={canDelete}
          onDeleteClick={handleDeleteClick}
        />
      )}

      {/* Delete Dialog */}
      <DeleteDialog
        open={deleteDialogOpen}
        onOpenChange={setDeleteDialogOpen}
        item={getDeleteItemInfo()}
        onConfirm={handleDeleteConfirm}
        isDeleting={isDeleting}
      />
    </Box>
  );
}

/**
 * Package root details view.
 *
 * Shown for asset nodes that also act as folder containers (non-leaf assets).
 * This occurs in npm repositories where the package root (e.g. `lodash`) is both
 * a browsable folder containing version subfolders AND the aggregated metadata asset
 * (package.json). Users can delete just the metadata asset or the entire package folder.
 */
interface PackageRootDetailsProps {
  node: BrowseNode;
  repositoryName: string;
  assetData?: AssetData | null;
  canDelete: boolean;
  onDeleteMetadataClick: () => void;
  onDeleteFolderClick: () => void;
}

function PackageRootDetails({
  node,
  repositoryName,
  assetData,
  canDelete,
  onDeleteMetadataClick,
  onDeleteFolderClick,
}: PackageRootDetailsProps): JSX.Element {
  return (
    <Card>
      <Box p="3" className="detail-panel__header">
        <Heading size="5">{node.text}</Heading>
      </Box>

      <Separator size="4" />

      <Box p="3">
        <DetailRow label="Repository" value={repositoryName} />
        <DetailRow label="Format" value={assetData?.format} />
        <DetailRow label="Content Type" value={assetData?.contentType} />
        <DetailRow label="Size" value={formatFileSize(assetData?.size)} />
        <DetailRow label="Last Updated" value={formatDate(assetData?.blobUpdated)} />
        <PathRowWithCopy label="Path" value={assetData?.path || assetData?.name} />
      </Box>

      <Separator size="4" />

      <Box p="3">
        <Callout.Root color="blue" size="1">
          <Callout.Icon>
            <AlertCircle size={14} />
          </Callout.Icon>
          <Callout.Text>
            This node represents both a package folder (containing version subfolders) and
            a metadata asset. Choose which to delete below.
          </Callout.Text>
        </Callout.Root>
      </Box>

      {canDelete && (
        <>
          <Separator size="4" />
          <Flex p="3" gap="2" justify="end">
            <Button
              variant="outline"
              color="orange"
              size="2"
              onClick={onDeleteMetadataClick}
            >
              <Trash2 size={14} />
              Delete Metadata Only
            </Button>
            <Button
              variant="outline"
              color="red"
              size="2"
              onClick={onDeleteFolderClick}
            >
              <Trash2 size={14} />
              Delete Entire Package
            </Button>
          </Flex>
        </>
      )}
    </Card>
  );
}

/**
 * Component details view.
 */
interface ComponentDetailsProps {
  node: BrowseNode;
  repositoryName: string;
  componentData?: ComponentData | null;
  canDelete: boolean;
  onDeleteClick: () => void;
}

function ComponentDetails({
  node,
  repositoryName,
  componentData,
  canDelete,
  onDeleteClick,
}: ComponentDetailsProps): JSX.Element {
  return (
    <Card>
      {/* Header — size="5" (one level down from page size="6") */}
      <Box p="3" className="detail-panel__header">
        <Heading size="5">{node.text}</Heading>
      </Box>

      <Separator size="4" />

      <Box p="3">
        <DetailRow label={STRINGS.component.repository} value={repositoryName} />
        <DetailRow label={STRINGS.component.format} value={componentData?.format} />
        <DetailRow label={STRINGS.component.group} value={componentData?.group} />
        <DetailRow label={STRINGS.component.name} value={componentData?.name || node.text} />
        <DetailRow label={STRINGS.component.version} value={componentData?.version} />
      </Box>

      {/* Actions */}
      {canDelete && (
        <>
          <Separator size="4" />
          <Flex p="3" gap="2" justify="end">
            {componentData?.version && (
              <Button
                variant="soft"
                size="2"
                onClick={() => {
                  const format = componentData.format;
                  const name = componentData.name;
                  const guideUrl = `https://guide.sonatype.com/component/${format}/${encodeURIComponent(name)}/${encodeURIComponent(componentData.version)}?referrer=browse-tree-component`;
                  window.open(guideUrl, '_blank', 'noopener,noreferrer');
                }}
              >
                <ExternalLink size={14} />
                Research in Guide
              </Button>
            )}
            <Button
              variant="outline"
              color="red"
              size="2"
              onClick={onDeleteClick}
            >
              <Trash2 size={14} />
              {STRINGS.component.deleteButton}
            </Button>
          </Flex>
        </>
      )}
    </Card>
  );
}

/**
 * Asset details view with tabbed navigation.
 */
interface AssetDetailsProps {
  node: BrowseNode;
  repositoryName: string;
  assetData?: AssetData | null;
  componentData?: ComponentData | null;
  canDelete: boolean;
  onDeleteClick: () => void;
  onDownloadClick: () => void;
  /** Active tab from URL (for URL-based navigation) */
  activeTab?: string;
  /** Callback when tab changes (for URL updates) */
  onTabChange?: (tab: string) => void;
}

/**
 * Generate usage snippets based on format.
 */
function getUsageSnippets(
  format: string | undefined,
  group: string | null | undefined,
  name: string | undefined,
  version: string | null | undefined,
  registryUrl?: string | null
): { title: string; code: string }[] {
  if (!format || !name) return [];
  const snippets: { title: string; code: string }[] = [];
  const g = group || '';
  const v = version || 'LATEST';
  const dockerPullTarget = sanitizeRegistryUrl(registryUrl) ? `${sanitizeRegistryUrl(registryUrl)}/${name}` : name;

  switch (format.toLowerCase()) {
    case 'maven2':
      snippets.push({
        title: 'Maven',
        code: `<dependency>\n  <groupId>${g}</groupId>\n  <artifactId>${name}</artifactId>\n  <version>${v}</version>\n</dependency>`,
      });
      snippets.push({
        title: 'Gradle',
        code: `implementation '${g}:${name}:${v}'`,
      });
      break;
    case 'npm':
      snippets.push({ title: 'npm', code: `npm install ${name}@${v}` });
      snippets.push({ title: 'yarn', code: `yarn add ${name}@${v}` });
      break;
    case 'pypi':
      snippets.push({ title: 'pip', code: `pip install ${name}==${v}` });
      break;
    case 'nuget':
      snippets.push({ title: 'NuGet', code: `Install-Package ${name} -Version ${v}` });
      break;
    case 'docker':
      snippets.push({ title: 'Docker', code: `docker pull ${dockerPullTarget}:${v}` });
      break;
  }
  return snippets;
}

/**
 * Copyable code snippet — Usage tab layout with GA copy affordance (icon-only, Tooltip, soft blue).
 */
function UsageSnippet({ title, code }: { title: string; code: string }): JSX.Element {
  const [copied, setCopied] = useState(false);
  const handleCopy = useCallback(() => {
    navigator.clipboard.writeText(code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }, [code]);

  return (
    <Box className="usage-snippet" mb="4">
      <Flex justify="between" align="center" mb="2">
        <Text weight="medium" size="2">
          {title}
        </Text>
        <Tooltip content={copied ? 'Copied!' : 'Copy'}>
          <Button
            variant="soft"
            color="blue"
            size="2"
            onClick={handleCopy}
            aria-label={`Copy ${title} snippet`}
          >
            <Copy size={14} />
          </Button>
        </Tooltip>
      </Flex>
      <Box className="usage-snippet__code">
        <Code size="2" style={{ whiteSpace: 'pre-wrap', display: 'block', padding: 'var(--space-3)' }}>
          {code}
        </Code>
      </Box>
    </Box>
  );
}

function AssetDetails({
  node,
  repositoryName,
  assetData,
  componentData,
  canDelete,
  onDeleteClick,
  onDownloadClick,
  activeTab = 'summary',
  onTabChange,
}: AssetDetailsProps): JSX.Element {
  const iqEnabled = isIqServerEnabled();
  const isProEdition = ExtJS.isProEdition?.() ?? false;
  // Top-level registryUrl computed by the backend at read time so the docker pull
  // snippet becomes `docker pull host:port/image:tag` when present (NEXUS-51972).
  const dockerRegistryUrl = assetData?.registryUrl;

  const DOCKER_ATTR_LABELS: Record<string, string> = {
    os: 'OS',
    arch: 'Architecture',
    architecture: 'Architecture',
    created: 'Created',
    author: 'Author',
    workingDir: 'Working Directory',
    totalSize: 'Total Size',
    exposedPorts: 'Exposed Ports',
    entrypoint: 'Entrypoint',
    cmd: 'Cmd',
    env: 'Environment',
    labels: 'Labels',
    history: 'History',
    content_digest: 'Digest',
    image_name: 'Image Name',
    image_tag: 'Image Tag',
  };
  const usageSnippets = getUsageSnippets(
    assetData?.format,
    componentData?.group,
    componentData?.name || node.text,
    componentData?.version,
    dockerRegistryUrl
  );

  return (
    <Card>
      {/* Header — size="5" (one level down from page size="6") */}
      <Box p="3" className="detail-panel__header">
        <Heading size="5" className="detail-panel__header-title">{node.text}</Heading>
      </Box>

      {/* Tabs - controlled by URL when onTabChange is provided */}
      <Tabs.Root value={activeTab} onValueChange={onTabChange}>
        <Tabs.List>
          <Tabs.Trigger value="summary">Summary</Tabs.Trigger>
          <Tabs.Trigger value="usage">Usage</Tabs.Trigger>
          <Tabs.Trigger value="attributes">Attributes</Tabs.Trigger>
          {isProEdition && <Tabs.Trigger value="tags">Component Tags</Tabs.Trigger>}
          <Tabs.Trigger value="lifecycle">Sonatype Lifecycle</Tabs.Trigger>
        </Tabs.List>

        {/* Summary Tab */}
        <Tabs.Content value="summary">
          <Box p="3">
            <DetailRow label={STRINGS.asset.repository} value={repositoryName} />
            <DetailRow label={STRINGS.asset.format} value={assetData?.format} />
            {componentData && (
              <>
                <DetailRow label="Group" value={componentData.group} />
                <DetailRow label="Component" value={componentData.name} />
                <DetailRow label="Version" value={componentData.version} />
              </>
            )}
            <PathRowWithCopy label={STRINGS.asset.path} value={assetData?.path || assetData?.name} />
            <DetailRow label={STRINGS.asset.contentType} value={assetData?.contentType} />
            <DetailRow label={STRINGS.asset.size} value={formatFileSize(assetData?.size)} />
            <DetailRow label={STRINGS.asset.blobCreated} value={formatDate(assetData?.blobCreated)} />
            <DetailRow label={STRINGS.asset.blobUpdated} value={formatDate(assetData?.blobUpdated)} />
            <DetailRow label={STRINGS.asset.lastDownloaded} value={formatDate(assetData?.lastDownloaded)} />
            {assetData?.blobRef && <DetailRow label="Blob Reference" value={assetData.blobRef} />}
            {assetData?.createdBy && <DetailRow label="Uploaded By" value={assetData.createdBy} />}
          </Box>
        </Tabs.Content>

        {/* Usage Tab */}
        <Tabs.Content value="usage">
          <Box p="3">
            {usageSnippets.length > 0 ? (
              usageSnippets.map((s) => <UsageSnippet key={s.title} title={s.title} code={s.code} />)
            ) : (
              <Text color="gray" size="2">No usage snippets available for this format.</Text>
            )}
          </Box>
        </Tabs.Content>

        {/* Attributes Tab */}
        <Tabs.Content value="attributes">
          <Box p="3">
            <DetailRow label="Repository" value={repositoryName} />
            <DetailRow label="Format" value={assetData?.format} />
            {componentData && (
              <>
                <DetailRow label="Group" value={componentData.group} />
                <Flex className="detail-panel__row" justify="between" py="2">
                  <Text size="2" color="gray" className="detail-panel__label">
                    Name
                  </Text>
                  <Flex align="center" gap="2" className="detail-panel__value">
                    <Text size="2">{componentData.name || '-'}</Text>
                    {componentData.version && (
                      <Box onClick={(e) => e.stopPropagation()}>
                        <DeepResearchLink
                          ecosystem={assetData?.format || componentData.format}
                          packageName={
                            assetData?.format === 'maven2' && componentData.group
                              ? `${componentData.group}:${componentData.name}`
                              : componentData.name
                          }
                          version={componentData.version}
                          iconOnly
                          size="1"
                          referrer="browse-tree"
                        />
                      </Box>
                    )}
                  </Flex>
                </Flex>
                <DetailRow label="Version" value={componentData.version} />
              </>
            )}


            {/* Checksums — rendered dynamically from REST Map<String,String> */}
            {assetData?.checksum && Object.keys(assetData.checksum).length > 0 && (
              <FacetSection title="Checksums" testId="attr-section-checksum" defaultOpen={true}>
                {Object.entries(assetData.checksum).map(([algo, value]) => (
                  <DetailRow key={algo} label={algo.toUpperCase()} value={value} />
                ))}
              </FacetSection>
            )}

            {/*
              Dynamic attribute facets surfaced from `attributes.*`.
              Matches Classic UI's AssetAttributes.js behavior - iterates ALL facets
              (firewall, format-specific, etc.) and renders each as a labeled section.
              Uses shouldDisplayAttributeFacet to hide internal facets (NEXUS-52920).
            */}
            {assetData?.attributes &&
              Object.entries(assetData.attributes)
                .filter(([key, value]) => shouldDisplayAttributeFacet(key, value))
                .map(([sectionKey, sectionValue]) => {
                  const entries = Object.entries(sectionValue as Record<string, unknown>)
                    .filter(([, val]) => val !== null && val !== undefined && val !== '');
                  if (entries.length === 0) return null;
                  return (
                    <FacetSection
                      key={sectionKey}
                      title={sectionKey}
                      testId={`attr-section-${sectionKey}`}
                      defaultOpen={sectionKey === 'firewall' || sectionKey === 'npm' || sectionKey === 'docker'}
                    >
                      {entries.map(([k, v]) => (
                        <Flex
                          key={k}
                          className="detail-panel__row"
                          justify="between"
                          py="2"
                          data-testid={`attr-${sectionKey}-${k}`}
                        >
                          <Text size="2" color="gray" className="detail-panel__label">
                            {sectionKey === 'docker' ? (DOCKER_ATTR_LABELS[k] ?? k) : k}
                          </Text>
                          <Text
                            size="2"
                            className="detail-panel__value"
                            style={{ whiteSpace: 'pre-wrap', textAlign: 'right' }}
                          >
                            {formatAttributeValue(v, k, formatFileSize)}
                          </Text>
                        </Flex>
                      ))}
                    </FacetSection>
                  );
                })}
          </Box>
        </Tabs.Content>

        {/* Component Tags Tab — Pro feature: hidden in CE mode */}
        {isProEdition && (
          <Tabs.Content value="tags">
            <Box p="3">
              <Flex direction="column" align="center" justify="center" p="4" gap="2">
                <Tag size={24} color="var(--gray-9)" />
                <Text color="gray" size="2">No tags associated with this component.</Text>
              </Flex>
            </Box>
          </Tabs.Content>
        )}

        {/* Sonatype Lifecycle Tab */}
        <Tabs.Content value="lifecycle">
          <Flex direction="column" align="center" justify="center" p="6" gap="2">
            <Shield size={32} color="var(--gray-9)" />
            {iqEnabled ? (
              <>
                <Text color="gray" size="2">Component security analysis available in IQ Server.</Text>
                {componentData && (
                  <Button
                    variant="soft"
                    size="2"
                    onClick={() => {
                      const clm = ExtJS.state()?.getValue?.('clm');
                      const iqUrl = clm?.url;
                      if (iqUrl && componentData.name) {
                        window.open(
                          `${iqUrl}/assets/index.html#/componentDetails/${encodeURIComponent(componentData.format)}/${encodeURIComponent(componentData.name)}/${encodeURIComponent(componentData.version || '')}`,
                          '_blank',
                          'noopener,noreferrer'
                        );
                      }
                    }}
                    disabled={!ExtJS.state()?.getValue?.('clm')?.url}
                  >
                    <ExternalLink size={14} />
                    View in IQ Server
                  </Button>
                )}
              </>
            ) : (
              <Text color="gray" size="2">Sonatype Lifecycle integration not configured.</Text>
            )}
          </Flex>
        </Tabs.Content>
      </Tabs.Root>

      {/* Actions */}
      <Separator size="4" />
      <Flex p="3" gap="2" justify="end">
        {componentData?.version && (
          <Button
            variant="soft"
            size="2"
            asChild
          >
            <a
              href={(() => {
                const format = assetData?.format === 'maven2' ? 'maven' : assetData?.format || componentData.format;
                const componentPath =
                  assetData?.format === 'maven2' && componentData?.group
                    ? `${encodeURIComponent(componentData.group)}%3A${encodeURIComponent(componentData.name)}`
                    : encodeURIComponent(componentData.name);
                return `https://guide.sonatype.com/component/${format}/${componentPath}/${encodeURIComponent(componentData.version)}?referrer=repo-browse-asset`;
              })()}
              target="_blank"
              rel="noopener noreferrer"
            >
              <ExternalLink size={14} />
              Research in Guide
            </a>
          </Button>
        )}
        <Button variant="soft" size="2" onClick={onDownloadClick}>
          <Download size={14} />
          {STRINGS.asset.downloadButton}
        </Button>
        {canDelete && (
          <Button variant="outline" color="red" size="2" onClick={onDeleteClick}>
            <Trash2 size={14} />
            {STRINGS.asset.deleteButton}
          </Button>
        )}
      </Flex>
    </Card>
  );
}

/**
 * Folder details view with metadata (children count, type breakdown).
 */
interface FolderDetailsProps {
  node: BrowseNode;
  repositoryName: string;
  canDelete: boolean;
  onDeleteClick: () => void;
}

interface FolderChildInfo {
  total: number;
  folders: number;
  components: number;
  assets: number;
}

function FolderDetails({
  node,
  repositoryName,
  canDelete,
  onDeleteClick,
}: FolderDetailsProps): JSX.Element {
  const [childInfo, setChildInfo] = useState<FolderChildInfo | null>(null);

  useEffect(() => {
    let cancelled = false;
    fetchBrowseNodes({ repositoryName, node: node.id })
      .then((children) => {
        if (cancelled) return;
        setChildInfo({
          total: children.length,
          folders: children.filter((c) => c.type === 'folder').length,
          components: children.filter((c) => c.type === 'component').length,
          assets: children.filter((c) => c.type === 'asset').length,
        });
      })
      .catch(() => {
        if (!cancelled) setChildInfo(null);
      });
    return () => { cancelled = true; };
  }, [repositoryName, node.id]);

  const depthSegments = node.id.split('/').filter(Boolean);

  return (
    <Card>
      {/* Header — size="5" (one level down from page size="6") */}
      <Box p="3" className="detail-panel__header">
        <Heading size="5">{node.text}</Heading>
      </Box>

      <Separator size="4" />

      <Box p="3">
        <DetailRow label={STRINGS.folder.repository} value={repositoryName} />
        <PathRowWithCopy label={STRINGS.folder.path} value={node.id} />
        <DetailRow label="Depth" value={String(depthSegments.length)} />
        {childInfo && (
          <>
            <DetailRow label="Children" value={String(childInfo.total)} />
            {childInfo.folders > 0 && (
              <DetailRow label="  Folders" value={String(childInfo.folders)} />
            )}
            {childInfo.components > 0 && (
              <DetailRow label="  Components" value={String(childInfo.components)} />
            )}
            {childInfo.assets > 0 && (
              <DetailRow label="  Assets" value={String(childInfo.assets)} />
            )}
          </>
        )}
      </Box>

      {canDelete && (
        <>
          <Separator size="4" />
          <Box p="3">
            <Text size="1" color="gray" mb="2">
              {STRINGS.folder.deleteWarning}
            </Text>
            <Flex gap="2" justify="end">
              <Button
                variant="outline"
                color="red"
                size="2"
                onClick={onDeleteClick}
              >
                <Trash2 size={14} />
                {STRINGS.folder.deleteButton}
              </Button>
            </Flex>
          </Box>
        </>
      )}
    </Card>
  );
}

export default DetailPanel;
