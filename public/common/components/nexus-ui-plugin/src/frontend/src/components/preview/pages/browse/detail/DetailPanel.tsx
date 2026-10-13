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
  Badge,
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
  Tag,
  Trash2,
} from 'lucide-react';

import { ExtJS } from '../../../../../interface/ExtJS';
import Permissions from '../../../../../constants/Permissions';
import { DeleteDialog } from '../actions/DeleteDialog';
import { deleteComponent, deleteAsset, deleteFolder, fetchBrowseNodes, fetchComponentTags } from '../browse.api';
import type { DeleteItemInfo } from '../actions/actions.types';
import type { BrowseNode } from '../tree/browse-tree.types';
import {
  formatFileSize,
  formatDate,
  getAssetDownloadUrl,
  isAssetCached,
  getLastDownloadedDisplay,
  sanitizeRegistryUrl,
} from './detail.utils';
import { getDependencySnippets } from '../../search/details/dependencySnippets/registry';
import type {
  DependencySnippet,
  SnippetAssetModel,
  SnippetComponentModel,
} from '../../search/details/dependencySnippets/types';
import { useComponentIntelligence } from '../../search/details/useComponentIntelligence';
import { buildIntelligenceRows, renderIntelligenceValue } from '../../search/details/intelligenceRows';
import { LifecycleIframe, useLifecycleEntitlement } from './LifecycleIframe';
import { DeepResearchLink, useToast } from '../../../shared';
import { useIsLicensedEdition } from '../../../shared/hooks';
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
    locallyCached: 'Locally cached',
    containingRepository: 'Containing repo',
    uploadedBy: 'Uploader',
    uploadedIp: "Uploader's IP Address",
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
 * Copyable Package URL (PURL) row. Renders the component's `pkg:` coordinate with a
 * copy affordance, matching Classic's "component PURL to copy" (NEXUS-54501). Rendered
 * only when the browse node supplies a `packageUrl`.
 */
function PurlRow({ value }: { value: string }): JSX.Element {
  const [copied, setCopied] = useState(false);

  const handleCopy = useCallback(() => {
    navigator.clipboard.writeText(value);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }, [value]);

  return (
    <Box className="detail-panel__row detail-panel__row--purl" py="2">
      <Text size="2" color="gray" weight="medium" mb="2" className="detail-panel__label">
        Package URL
      </Text>
      <Box
        className="detail-panel__purl-box"
        p="4"
        style={{ backgroundColor: 'var(--gray-2)', borderRadius: '6px' }}
      >
        <Flex align="center" justify="between" gap="2">
          <Code
            size="2"
            color="gray"
            variant="ghost"
            style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-all', flex: 1, overflowX: 'auto' }}
          >
            {value}
          </Code>
          <Tooltip content={copied ? 'Copied!' : 'Copy package URL'}>
            <Button
              variant="soft"
              color="blue"
              size="2"
              onClick={handleCopy}
              aria-label="Copy package URL"
            >
              {copied ? <Check size={14} /> : <Copy size={14} />}
            </Button>
          </Tooltip>
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
  /**
   * Callback when a delete request was rejected.
   *
   * A rejected delete may still have partially succeeded server-side, so the
   * host re-reads the affected level; unlike {@link onDeleted} it must not remove
   * the node or navigate away.
   */
  onDeleteFailed?: () => void;
  /** Whether user has delete permission */
  canDelete?: boolean;
  /** Active tab from URL (for URL-based navigation) */
  activeTab?: string;
  /** Callback when tab changes (for URL updates) */
  onTabChange?: (tab: string) => void;
  /** When true, deletes are recoverable soft-deletes (moved to Trash); drives the confirm copy and
   *  success toast. Set by the parent only for hosted repositories with soft-delete enabled. */
  softDelete?: boolean;
  /** Optional retention window (days) surfaced in the soft-delete confirm copy. */
  retentionDays?: number;
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
  /** IP address the asset was uploaded from. Rendered for Classic parity (NEXUS-54501). */
  createdByIp?: string;
  /** For group repositories: the member repository that actually holds the asset. */
  containingRepositoryName?: string;
  checksum?: Record<string, string>;
  /**
   * Format-specific attributes bag from the backend (e.g. `docker.registryUrl`).
   * Intentionally loose (`Record<string, unknown>`) since shape varies per format.
   */
  attributes?: Record<string, unknown>;
  registryUrl?: string;
  /**
   * RAW internal id of the component that owns this asset, as returned by ExtDirect
   * `coreui_Component.readAsset`. Absent for a blob with no owning component.
   *
   * This is the only place an asset selection can learn it — asset browse nodes carry
   * `componentId: null` — and it is deliberately not `componentData.id`, which is the base64
   * `RepositoryItemIDXO` the REST API returns and the ExtDirect methods reject (NEXUS-54501,
   * same distinction as `useCanDelete` / NEXUS-54171).
   */
  componentId?: string;
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
  onDeleteFailed,
  canDelete = false,
  activeTab = 'summary',
  onTabChange,
  softDelete = false,
  retentionDays,
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
   *
   * `onDeleted` is the *successful*-deletion path: BrowsePage removes the node
   * from the tree and navigates away from it. A rejected delete must therefore
   * never invoke it, or the UI drops a node the server still holds.
   * The failure is surfaced as a toast plus an inline reason, and
   * `onDeleteFailed` re-reads the affected level from the server without removing
   * anything — a rejected delete can still have partially succeeded (a cloud APT
   * delete removes the asset and then fails updating format metadata), so the
   * tree converges on what the server actually holds while the user stays on the
   * node they tried to delete.
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
      toast.success(softDelete ? 'Item moved to Trash' : 'Item deleted successfully');
      if (onDeleted) {
        onDeleted();
      }
    } catch (err) {
      const errorMessage =
        err instanceof Error ? err.message : typeof err === 'string' ? err : 'Delete failed.';
      toast.error(errorMessage);
      setDeleteError(`Cannot delete: ${errorMessage}`);
      setDeleteDialogOpen(false);
      // Deliberately not onDeleted() — that path removes the node and navigates
      // away. Reconcile the level with the server instead; see the note above.
      if (onDeleteFailed) {
        onDeleteFailed();
      }
    } finally {
      setIsDeleting(false);
    }
  }, [
    onDeleted,
    onDeleteFailed,
    componentData,
    assetData,
    repositoryName,
    toast,
    node,
    deleteAsFolder,
    softDelete,
  ]);

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
        softDelete={softDelete}
        retentionDays={retentionDays}
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
 * Repository Health Check component intelligence section: most popular version, age, popularity,
 * security alert chiclets (CVE counts by severity), and license threat — the same signals and
 * source the Classic component-details panel uses (NEXUS-54501). Reuses the shared hook and the
 * shared row builder so the browse panel and the search Overview never diverge.
 *
 * Renders nothing (returns null) whenever the shared builder yields no rows — an unsupported
 * format, a user without `nexus:healthcheck:read`, or nothing requested yet — so it degrades
 * gracefully with no empty section left behind.
 */
function ComponentIntelligenceSection({
  format,
  group,
  name,
  version,
}: {
  format: string | undefined;
  group: string | null | undefined;
  name: string | undefined;
  version: string | null | undefined;
}): JSX.Element | null {
  // gaId is `format:group:name` (or `format:name` for group-less formats), the shape
  // parseGaCoordinates in the hook expects.
  const gaId = [format, group, name].filter(Boolean).join(':');
  const { data, status, alertsStatus } = useComponentIntelligence({
    gaId,
    format: format ?? '',
    version: version ?? null,
    enabled: Boolean(format && name),
  });

  const rows = buildIntelligenceRows(status, data, alertsStatus);
  if (rows.length === 0) {
    return null;
  }

  return (
    <>
      <Separator size="4" />
      <Box p="3">
        <Heading size="3" mb="3">Component Intelligence</Heading>
        {rows.map((row) => (
          <Flex
            key={row.label}
            className="detail-panel__row"
            justify="between"
            py="2"
            align="center"
          >
            <Text size="2" color="gray" className="detail-panel__label">
              {row.label}
            </Text>
            <Box className="detail-panel__value">{renderIntelligenceValue(row)}</Box>
          </Flex>
        ))}
      </Box>
    </>
  );
}

/**
 * Whether Component Tags may be shown at all, mirroring Classic's
 * `NX.proui.controller.ComponentTags.isTaggingEnabled()`: a signed-in user, `nexus:tags:read`,
 * a non-EVAL edition and a valid license. Every condition is read reactively, so a login,
 * permission change or license activation re-gates the section instead of leaving a stale one
 * on screen.
 *
 * The Pro-edition check is additional to Classic's ladder, not a substitute for part of it.
 * Classic can omit it because the whole proui plugin only loads in Pro; this component ships in
 * every edition, so the edition has to be asserted here.
 *
 * This is a single value on purpose — it gates tab availability, the trigger, the content and the
 * fetch together. Gating the fetch more tightly than the trigger is what produces a visible but
 * permanently empty tab.
 */
function useComponentTagEntitlement(): boolean {
  const user = ExtJS.useUser();
  const license = ExtJS.useLicense() as { installed?: boolean; valid?: boolean } | null | undefined;
  const canReadTags = ExtJS.usePermission(() => ExtJS.checkPermission(Permissions.TAGS.READ), []);
  const isLicensedEdition = useIsLicensedEdition();
  const edition = ExtJS.state().getEdition();
  const licenseValid = license?.installed === true && license?.valid === true;

  return Boolean(user) && canReadTags && isLicensedEdition && edition !== 'EVAL' && licenseValid;
}

/**
 * True once `NX.State` has published the license, i.e. the entitlement hooks are answering from real
 * state rather than from "not loaded yet".
 *
 * `ExtJS.useLicense()` is backed by `ExtJS.useState`, which returns `undefined` until the state
 * publishes and then re-renders. Every edition publishes an object — CE and Core publish
 * `{installed: false, valid: false}` — so `undefined` means unresolved, never "not entitled". The
 * user cannot serve as the signal here: an anonymous user legitimately has none.
 */
function useEntitlementResolved(): boolean {
  return ExtJS.useLicense() !== undefined;
}

/**
 * Loads the tags of one component when entitled, degrading to an empty list on any failure so a
 * missing tag service never blanks the surrounding panel.
 *
 * `componentId` must be the raw ExtDirect id: `readTagListDetails` resolves the component with
 * `find(new DetachedEntityId(id))` and rejects the encoded REST `RepositoryItemIDXO` form.
 */
function useComponentTags(
  componentId: string | null | undefined,
  repositoryName: string,
  entitled: boolean,
): string[] {
  const [tags, setTags] = useState<string[]>([]);

  useEffect(() => {
    if (!entitled || !componentId) {
      setTags([]);
      return;
    }
    let cancelled = false;
    fetchComponentTags(componentId, repositoryName)
      .then((loaded) => {
        if (!cancelled) setTags(loaded);
      })
      .catch(() => {
        if (!cancelled) setTags([]);
      });
    return () => {
      cancelled = true;
    };
  }, [entitled, componentId, repositoryName]);

  return tags;
}

/**
 * The tag badges themselves, shared by the component view (an inline section, as Classic adds a
 * collapsible tag panel to the component info container) and the asset view (a tab, as Classic
 * adds a tab to the asset container). Both reach it through the same entitlement.
 */
function ComponentTagList({ tags }: { tags: string[] }): JSX.Element {
  if (tags.length === 0) {
    return (
      <Flex direction="column" align="center" justify="center" p="4" gap="2">
        <Tag size={24} color="var(--gray-9)" />
        <Text color="gray" size="2">No tags associated with this component.</Text>
      </Flex>
    );
  }

  return (
    <Flex gap="2" wrap="wrap" data-testid="component-tags">
      {tags.map((tagName) => (
        <Badge key={tagName} color="blue" variant="soft" size="2">
          <Tag size={12} />
          {tagName}
        </Badge>
      ))}
    </Flex>
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
  // Component Tags for a component-node selection. Classic reaches the same panel from
  // `ComponentTags.onComponentInfoUpdated`, so a selected component shows its tags without
  // needing an asset to be selected first (NEXUS-54501).
  const tagsEntitled = useComponentTagEntitlement();
  const tags = useComponentTags(node.componentId, repositoryName, tagsEntitled);

  // Component-level usage snippets via the shared per-format registry (Classic parity,
  // NEXUS-54501). No asset context at the component level, so classifier/extension/registry
  // are absent — the generators degrade to the base coordinates. Empty for formats with no
  // generator, in which case the Usage section is omitted entirely.
  const usageSnippets = buildUsageSnippets(
    componentData?.format,
    componentData?.group,
    componentData?.name || node.text,
    componentData?.version,
  );

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
        {node.packageUrl && <PurlRow value={node.packageUrl} />}
      </Box>

      {/* Repository Health Check intelligence (Classic parity, NEXUS-54501). Renders nothing on
          unsupported formats or without the healthcheck permission. */}
      <ComponentIntelligenceSection
        format={componentData?.format}
        group={componentData?.group}
        name={componentData?.name || node.text}
        version={componentData?.version}
      />

      {/* Component Tags (Pro, entitled) — Classic's collapsible tag panel on the component info
          container. Absent entirely when unentitled, so CE and users without nexus:tags:read
          never see an empty section. */}
      {tagsEntitled && (
        <>
          <Separator size="4" />
          <Box p="3" data-testid="component-tags-section">
            <Heading size="3" mb="3">Component Tags</Heading>
            <ComponentTagList tags={tags} />
          </Box>
        </>
      )}

      {/* Usage snippets — rendered only for formats the registry knows how to build. */}
      {usageSnippets.length > 0 && (
        <>
          <Separator size="4" />
          <Box p="3">
            <Heading size="3" mb="3">Usage</Heading>
            {usageSnippets.map((s) => (
              <UsageSnippet
                key={s.displayName}
                title={s.displayName}
                code={s.snippetText}
                description={s.description}
              />
            ))}
          </Box>
        </>
      )}

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
 * Build the usage snippets for an asset/component using the shared per-format registry
 * (search/details/dependencySnippets). This gives the Browse detail panel full Classic
 * parity — all applicable tool snippets for the format, including PURL where the format
 * provides one — instead of the old hardcoded Maven/Gradle-only subset.
 *
 * Returns an empty array for a format with no generator, matching Classic's behavior of
 * rendering no snippet section.
 */
function buildUsageSnippets(
  format: string | undefined,
  group: string | null | undefined,
  name: string | undefined,
  version: string | null | undefined,
  asset?: AssetData | null,
): DependencySnippet[] {
  if (!format || !name) return [];

  const component: SnippetComponentModel = {
    format,
    group: group || '',
    name,
    version: version || '',
    repositoryName: asset?.repositoryName,
  };

  // The maven2 generator reads classifier/extension from the asset's maven2 attributes;
  // docker/oci read registryUrl/digest. Adapt the loose attributes bag to the typed model.
  const maven2Attrs = (asset?.attributes?.maven2 ?? undefined) as
    | { classifier?: string; extension?: string }
    | undefined;
  const assetModel: SnippetAssetModel = {
    attributes: maven2Attrs ? { maven2: maven2Attrs } : undefined,
    // Sanitized: the docker/oci generators splice this straight into a `docker pull <target>` line
    // the user is invited to copy into a shell, so anything but a bare host[:port][/path] is dropped.
    registryUrl: sanitizeRegistryUrl(asset?.registryUrl),
    digest: readContentDigest(asset),
  };

  return getDependencySnippets(format, component, assetModel);
}

/**
 * The image digest of an OCI/Docker asset, read from the format facet where the backend actually
 * writes it: `attributes.<format>.content_digest` (`OciAttributes.P_CONTENT_DIGEST` and
 * `DockerAttributes.P_DOCKER_DIGEST`, both the literal `content_digest`, exposed on the asset by
 * `OciAssetXODescriptor`). There is no top-level `attributes.digest` — reading that made the OCI
 * cosign snippet fall back to its `@<digest>` placeholder even for an asset that has a real digest.
 */
function readContentDigest(asset?: AssetData | null): string | undefined {
  const attributes = asset?.attributes;
  if (!attributes) return undefined;

  for (const facet of ['oci', 'docker'] as const) {
    const digest = (attributes[facet] as { content_digest?: unknown } | undefined)?.content_digest;
    if (typeof digest === 'string' && digest) {
      return digest;
    }
  }
  return undefined;
}

/**
 * Copyable code snippet — Usage tab layout with GA copy affordance (icon-only, Tooltip, soft blue).
 * `title` is the tool/snippet name; `description` is the optional Classic instruction line.
 */
function UsageSnippet({
  title,
  code,
  description,
}: {
  title: string;
  code: string;
  description?: string;
}): JSX.Element {
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
      {description && (
        <Text size="1" color="gray" mb="1" style={{ display: 'block' }}>
          {description}
        </Text>
      )}
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
  // Raw ExtDirect id, never componentData.id (the encoded REST RepositoryItemIDXO):
  // readTagListDetails resolves the component with `find(new DetachedEntityId(id))` and rejects
  // the encoded form. The node fallback covers a component-node selection (NEXUS-54501).
  const componentId = assetData?.componentId ?? node.componentId;

  // Component Tags entitlement — the full Classic ladder, not just the edition. Gating the tab on
  // Pro alone showed a permanently empty tab to a Pro user without nexus:tags:read.
  const tagsEntitled = useComponentTagEntitlement();
  const tags = useComponentTags(componentId, repositoryName, tagsEntitled);

  // Sonatype Lifecycle is entitled per user/edition/license/component, exactly as Classic gates
  // adding the panel at all (NEXUS-54501). Both the tab and its content honour it.
  // Classic reads `asset.get('componentId')` — the id carried by the selected node — not the
  // separately fetched component. The two normally agree, but `loadNodeDetail` swallows a failed
  // component fetch, and an unavailable component record is no reason to withhold the report.
  // Classic's `Boolean(asset.get('componentId'))` gate reads the same value.
  const lifecycleComponentId = componentId;
  const lifecycleEntitled = useLifecycleEntitlement(lifecycleComponentId);

  // A tab reached from the URL may not exist for this user — `?tab=lifecycle` or `?tab=tags`
  // without the matching entitlement. Radix would then render a tab list with nothing selected
  // and an empty body, so fall back to Summary and correct the URL rather than showing a blank panel.
  const availableTabs = new Set(['summary', 'usage', 'attributes']);
  if (tagsEntitled) availableTabs.add('tags');
  if (lifecycleEntitled) availableTabs.add('lifecycle');
  const selectedTab = availableTabs.has(activeTab) ? activeTab : 'summary';
  // Only correct the URL once the license has actually published. Before that both entitlements read
  // false, so an entitled user deep-linking to `?tab=lifecycle` would have the link rewritten to
  // `summary` on the first render and never get the tab back. Rendering still falls back to Summary
  // in the meantime; keeping `activeTab` intact lets the effect settle on it when the state lands.
  const entitlementResolved = useEntitlementResolved();
  useEffect(() => {
    if (!entitlementResolved) return;
    if (selectedTab !== activeTab) {
      onTabChange?.(selectedTab);
    }
  }, [entitlementResolved, selectedTab, activeTab, onTabChange]);

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
  // Semantic coordinates only. `node.text` is the asset filename ('maven-3.9.6.pom',
  // 'NuGet.Core.2.14.0.nupkg'), so falling back to it produced dependency snippets whose
  // artifactId/package id was a filename and whose group/version were empty. Without a resolved
  // component there are no coordinates, and buildUsageSnippets correctly returns none (NEXUS-54501).
  const usageSnippets = buildUsageSnippets(
    assetData?.format,
    componentData?.group,
    componentData?.name,
    componentData?.version,
    assetData,
  );

  return (
    <Card>
      {/* Header — size="5" (one level down from page size="6") */}
      <Box p="3" className="detail-panel__header">
        <Heading size="5" className="detail-panel__header-title">{node.text}</Heading>
      </Box>

      {/* Tabs - controlled by URL when onTabChange is provided */}
      <Tabs.Root value={selectedTab} onValueChange={onTabChange}>
        <Tabs.List>
          <Tabs.Trigger value="summary">Summary</Tabs.Trigger>
          <Tabs.Trigger value="usage">Usage</Tabs.Trigger>
          <Tabs.Trigger value="attributes">Attributes</Tabs.Trigger>
          {tagsEntitled && <Tabs.Trigger value="tags">Component Tags</Tabs.Trigger>}
          {lifecycleEntitled && <Tabs.Trigger value="lifecycle">Sonatype Lifecycle</Tabs.Trigger>}
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
            {/* Three separate populated dates (Classic parity, NEXUS-54501) */}
            <DetailRow label={STRINGS.asset.blobCreated} value={formatDate(assetData?.blobCreated)} />
            <DetailRow label={STRINGS.asset.blobUpdated} value={formatDate(assetData?.blobUpdated)} />
            <DetailRow
              label={STRINGS.asset.lastDownloaded}
              value={getLastDownloadedDisplay(assetData?.lastDownloaded)}
            />
            {/* Locally-cached indicator — for proxy assets, whether content is actually cached */}
            <DetailRow
              label={STRINGS.asset.locallyCached}
              value={isAssetCached(assetData?.contentType, assetData?.size) ? 'Yes' : 'No'}
            />
            {assetData?.blobRef && <DetailRow label="Blob Reference" value={assetData.blobRef} />}
            {/* Unconditional, matching Classic: the row is always present, empty when the asset is
                not served through a group repository. */}
            <DetailRow
              label={STRINGS.asset.containingRepository}
              value={assetData?.containingRepositoryName}
            />
            {assetData?.createdBy && (
              <DetailRow label={STRINGS.asset.uploadedBy} value={assetData.createdBy} />
            )}
            {assetData?.createdByIp && (
              <DetailRow label={STRINGS.asset.uploadedIp} value={assetData.createdByIp} />
            )}
            {node.packageUrl && <PurlRow value={node.packageUrl} />}
          </Box>
          {/* Repository Health Check intelligence (Classic parity, NEXUS-54501). Renders nothing on
              unsupported formats or without the healthcheck permission. */}
          <ComponentIntelligenceSection
            format={assetData?.format}
            group={componentData?.group}
            name={componentData?.name || node.text}
            version={componentData?.version}
          />
        </Tabs.Content>

        {/* Usage Tab */}
        <Tabs.Content value="usage">
          <Box p="3">
            {usageSnippets.length > 0 ? (
              usageSnippets.map((s) => (
                <UsageSnippet
                  key={s.displayName}
                  title={s.displayName}
                  code={s.snippetText}
                  description={s.description}
                />
              ))
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
                      // Classic parity (NEXUS-54501): cache and format-specific attribute
                      // accordions render expanded by default so their data is visible without
                      // an extra click. Checksums (above) already defaults open.
                      defaultOpen={true}
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

        {/* Component Tags Tab — mounted only when entitled (Pro, signed in, nexus:tags:read,
            non-EVAL, valid license), so an unentitled user never sees an empty tab. */}
        {tagsEntitled && (
          <Tabs.Content value="tags">
            <Box p="3">
              <ComponentTagList tags={tags} />
            </Box>
          </Tabs.Content>
        )}

        {/* Sonatype Lifecycle Tab — embedded IQ Server Health Check report (Classic parity,
            NEXUS-54501). Mounted only when entitled, so an unentitled user never issues the
            healthCheckFileDetail request the backend would answer with a 403/licence error page.
            Uses the containing repository (proxy-cached content lives there) and the asset path,
            matching Classic HealthCheckAssetDetail. */}
        {lifecycleEntitled && (
          <Tabs.Content value="lifecycle">
            <LifecycleIframe
              repositoryName={assetData?.containingRepositoryName || assetData?.repositoryName || repositoryName}
              path={assetData?.path || assetData?.name || node.text}
              format={assetData?.format || componentData?.format || ''}
              componentId={lifecycleComponentId}
            />
          </Tabs.Content>
        )}
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
