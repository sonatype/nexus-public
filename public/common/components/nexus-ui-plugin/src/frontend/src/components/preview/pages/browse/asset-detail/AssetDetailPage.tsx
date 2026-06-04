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

import React, { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Button,
  Card,
  Code,
  Flex,
  Heading,
  ScrollArea,
  Separator,
  Spinner,
  Tabs,
  Text,
  Badge,
  Callout,
  TextField,
} from '@radix-ui/themes';
import {
  ArrowLeft,
  Check,
  Copy,
  Download,
  ExternalLink,
  File,
  Package,
  Plus,
  Shield,
  Tag,
  Trash2,
  X,
} from 'lucide-react';
import { useRouter } from '@uirouter/react';
import { ExtJS } from '../../../../../interface/ExtJS';
import { ExtAPIUtils } from '../../../../../interface/ExtAPIUtils';

import { useAssetDetail } from './useAssetDetail';
import type { AssetDetailData, ComponentTag } from './asset-detail.types';

import './AssetDetailPage.scss';

/**
 * UI Strings for the asset detail page.
 */
const STRINGS = {
  backButton: 'Back to Search',
  loading: 'Loading asset details...',
  error: 'Failed to load asset details',
  retry: 'Retry',
  
  tabs: {
    summary: 'Summary',
    usage: 'Usage',
    attributes: 'Attributes',
    tags: 'Component Tags',
    lifecycle: 'Sonatype Lifecycle',
  },

  summary: {
    path: 'Path',
    contentType: 'Content Type',
    fileSize: 'File Size',
    blobCreated: 'Blob Created',
    blobUpdated: 'Blob Updated',
    lastDownloaded: 'Last Downloaded',
    locallyCached: 'Locally Cached',
    blobRef: 'Blob Reference',
    containingRepo: 'Containing Repo',
    uploader: 'Uploader',
    uploaderIp: "Uploader's IP Address",
  },

  usage: {
    empty: 'No usage snippets available for this format.',
  },

  tags: {
    empty: 'No tags associated with this component.',
    addTag: 'Add Tag',
    tagPlaceholder: 'Enter tag name...',
    addButton: 'Add',
    removeConfirm: 'Remove tag?',
  },

  lifecycle: {
    notConfigured: 'Sonatype Lifecycle integration not configured.',
    learnMore: 'Learn more about IQ Server integration',
  },

  actions: {
    download: 'Download',
    delete: 'Delete Asset',
  },
};

/**
 * Format file size in human-readable form.
 */
function formatFileSize(bytes?: number | null): string {
  if (bytes == null) return '-';
  if (bytes === 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return `${(bytes / Math.pow(1024, i)).toFixed(1)} ${units[i]}`;
}

/**
 * Format date in readable form.
 */
function formatDate(dateString?: string | null): string {
  if (!dateString) return '-';
  try {
    return new Date(dateString).toLocaleString();
  } catch {
    return dateString;
  }
}

/**
 * Simple data row component.
 */
function DetailRow({ 
  label, 
  value, 
  isPath = false 
}: { 
  label: string; 
  value: string | null | undefined;
  isPath?: boolean;
}): JSX.Element {
  return (
    <Flex py="2" className="asset-detail__row">
      <Text size="2" color="gray" className="asset-detail__row-label">
        {label}
      </Text>
      <Text 
        size="2" 
        className={`asset-detail__row-value ${isPath ? 'asset-detail__row-value--path' : ''}`}
      >
        {value || '-'}
      </Text>
    </Flex>
  );
}

/**
 * Generate usage snippets based on format.
 */
function getUsageSnippets(
  format: string | undefined,
  group: string | null | undefined,
  name: string | undefined,
  version: string | null | undefined
): { title: string; code: string }[] {
  if (!format || !name) return [];
  const snippets: { title: string; code: string }[] = [];
  const g = group || '';
  const v = version || 'LATEST';

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
      snippets.push({ title: '.NET CLI', code: `dotnet add package ${name} --version ${v}` });
      break;
    case 'docker':
      snippets.push({ title: 'Docker', code: `docker pull ${name}:${v}` });
      break;
    case 'helm':
      snippets.push({ title: 'Helm', code: `helm install ${name} --version ${v}` });
      break;
    case 'yum':
    case 'rpm':
      snippets.push({ title: 'yum', code: `yum install ${name}-${v}` });
      snippets.push({ title: 'dnf', code: `dnf install ${name}-${v}` });
      break;
    case 'apt':
    case 'deb':
      snippets.push({ title: 'apt', code: `apt-get install ${name}=${v}` });
      break;
    case 'rubygems':
      snippets.push({ title: 'gem', code: `gem install ${name} -v ${v}` });
      break;
    case 'go':
    case 'golang':
      snippets.push({ title: 'Go', code: `go get ${g}/${name}@${v}` });
      break;
  }
  return snippets;
}

/**
 * Copyable code snippet component.
 */
function UsageSnippet({ title, code }: { title: string; code: string }): JSX.Element {
  const [copied, setCopied] = useState(false);
  
  const handleCopy = useCallback(() => {
    navigator.clipboard.writeText(code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }, [code]);

  return (
    <Box mb="3" className="asset-detail__snippet">
      <Flex justify="between" align="center" mb="1">
        <Text size="2" weight="medium">{title}</Text>
        <Button variant="ghost" size="1" onClick={handleCopy}>
          {copied ? <Check size={12} /> : <Copy size={12} />}
          {copied ? 'Copied!' : 'Copy'}
        </Button>
      </Flex>
      <Code size="1" className="asset-detail__snippet-code">
        {code}
      </Code>
    </Box>
  );
}

/**
 * Component Tags panel with add/remove functionality.
 */
function TagsPanel({ 
  tags, 
  onAddTag, 
  onRemoveTag,
  canEdit,
  loading,
}: {
  tags: ComponentTag[];
  onAddTag: (tagName: string) => Promise<void>;
  onRemoveTag: (tagName: string) => Promise<void>;
  canEdit: boolean;
  loading: boolean;
}): JSX.Element {
  const [newTagName, setNewTagName] = useState('');
  const [addingTag, setAddingTag] = useState(false);

  const handleAddTag = async () => {
    if (!newTagName.trim()) return;
    setAddingTag(true);
    try {
      await onAddTag(newTagName.trim());
      setNewTagName('');
    } finally {
      setAddingTag(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleAddTag();
    }
  };

  if (loading) {
    return (
      <Flex align="center" justify="center" p="6" gap="2">
        <Spinner size="2" />
        <Text size="2" color="gray">Loading tags...</Text>
      </Flex>
    );
  }

  return (
    <Box p="3">
      {/* Add tag input */}
      {canEdit && (
        <Flex gap="2" mb="4" className="asset-detail__add-tag">
          <TextField.Root
            placeholder={STRINGS.tags.tagPlaceholder}
            value={newTagName}
            onChange={(e) => setNewTagName(e.target.value)}
            onKeyDown={handleKeyDown}
            disabled={addingTag}
            style={{ flex: 1 }}
          />
          <Button 
            variant="soft" 
            onClick={handleAddTag} 
            disabled={!newTagName.trim() || addingTag}
          >
            <Plus size={14} />
            {STRINGS.tags.addButton}
          </Button>
        </Flex>
      )}

      {/* Tags list */}
      {tags.length > 0 ? (
        <Flex gap="2" wrap="wrap" className="asset-detail__tags-list">
          {tags.map((tag) => (
            <Badge 
              key={tag.name} 
              size="2" 
              variant="soft" 
              color="violet"
              className="asset-detail__tag-badge"
            >
              <Tag size={12} />
              {tag.name}
              {canEdit && (
                <button
                  type="button"
                  className="asset-detail__tag-remove"
                  onClick={() => onRemoveTag(tag.name)}
                  aria-label={`Remove tag ${tag.name}`}
                >
                  <X size={12} />
                </button>
              )}
            </Badge>
          ))}
        </Flex>
      ) : (
        <Flex direction="column" align="center" justify="center" p="6" gap="2">
          <Tag size={32} color="var(--gray-9)" />
          <Text color="gray" size="2">{STRINGS.tags.empty}</Text>
        </Flex>
      )}
    </Box>
  );
}

/**
 * Sonatype Lifecycle panel.
 */
function LifecyclePanel({ componentId }: { componentId?: string }): JSX.Element {
  // TODO: Integrate with IQ Server API when available
  return (
    <Flex direction="column" align="center" justify="center" p="6" gap="3">
      <Shield size={32} color="var(--gray-9)" />
      <Text color="gray" size="2">{STRINGS.lifecycle.notConfigured}</Text>
      <Button variant="ghost" size="1" asChild>
        <a
          href="http://links.sonatype.com/products/nxrm3/docs/iq-server"
          target="_blank"
          rel="noopener noreferrer"
        >
          {STRINGS.lifecycle.learnMore}
          <ExternalLink size={12} />
        </a>
      </Button>
    </Flex>
  );
}

/**
 * Props for AssetDetailPage.
 */
interface AssetDetailPageProps {
  /** Repository name */
  repositoryName: string;
  /** Asset ID (base64 encoded) */
  assetId: string;
  /** Component ID (optional, for component-level operations) */
  componentId?: string;
}

/**
 * AssetDetailPage - Unified asset detail view with all tabs.
 * 
 * Accessible from both Search results and Browse navigation.
 * Provides comprehensive asset information including:
 * - Summary (metadata)
 * - Usage (install snippets)
 * - Attributes (extended properties)
 * - Component Tags (tagging functionality)
 * - Sonatype Lifecycle (IQ integration)
 */
export function AssetDetailPage({ 
  repositoryName, 
  assetId,
  componentId,
}: AssetDetailPageProps): JSX.Element {
  const router = useRouter();
  const [activeTab, setActiveTab] = useState('summary');

  const {
    asset,
    component,
    tags,
    loading,
    tagsLoading,
    error,
    refetch,
    addTag,
    removeTag,
  } = useAssetDetail({ repositoryName, assetId, componentId });

  const canEdit = ExtJS.checkPermission('nexus:tags:*');
  const canDelete = ExtJS.checkPermission('nexus:repository-admin:*:*:delete');

  // Generate usage snippets
  const usageSnippets = getUsageSnippets(
    asset?.format,
    component?.group,
    component?.name || asset?.name,
    component?.version
  );

  const handleBack = useCallback(() => {
    router.stateService.go('preview.browse.search');
  }, [router]);

  const handleDownload = useCallback(() => {
    if (asset?.downloadUrl) {
      window.open(asset.downloadUrl, '_blank');
    }
  }, [asset]);

  const handleDelete = useCallback(async () => {
    if (!asset) return;
    // TODO: Implement delete with confirmation dialog
    ExtJS.showInfoMessage('Delete functionality coming soon');
  }, [asset]);

  // Loading state
  if (loading && !asset) {
    return (
      <Box className="asset-detail-page">
        <Flex align="center" justify="center" p="9" gap="3">
          <Spinner size="3" />
          <Text size="3">{STRINGS.loading}</Text>
        </Flex>
      </Box>
    );
  }

  // Error state
  if (error && !asset) {
    return (
      <Box className="asset-detail-page">
        <Callout.Root color="red" className="asset-detail-page__error">
          <Callout.Icon>
            <X size={16} />
          </Callout.Icon>
          <Callout.Text>{error}</Callout.Text>
        </Callout.Root>
        <Flex justify="center" mt="4">
          <Button variant="soft" onClick={refetch}>
            {STRINGS.retry}
          </Button>
        </Flex>
      </Box>
    );
  }

  return (
    <Box className="asset-detail-page">
      {/* Header */}
      <Flex align="center" gap="3" className="asset-detail-page__header">
        <Button variant="ghost" size="2" onClick={handleBack}>
          <ArrowLeft size={18} />
          {STRINGS.backButton}
        </Button>
      </Flex>

      <ScrollArea className="asset-detail-page__content">
        <Card className="asset-detail-page__card">
          {/* Asset Title */}
          <Flex align="center" gap="3" p="4" className="asset-detail-page__title-row">
            <File size={24} className="asset-detail-page__icon" />
            <Box>
              <Heading size="5">{asset?.name || asset?.path?.split('/').pop()}</Heading>
              <Text size="2" color="gray">
                {repositoryName} • {asset?.format}
              </Text>
            </Box>
          </Flex>

          <Separator size="4" />

          {/* Tabs */}
          <Tabs.Root value={activeTab} onValueChange={setActiveTab}>
            <Tabs.List className="asset-detail-page__tabs">
              <Tabs.Trigger value="summary">{STRINGS.tabs.summary}</Tabs.Trigger>
              <Tabs.Trigger value="usage">{STRINGS.tabs.usage}</Tabs.Trigger>
              <Tabs.Trigger value="attributes">{STRINGS.tabs.attributes}</Tabs.Trigger>
              <Tabs.Trigger value="tags">{STRINGS.tabs.tags}</Tabs.Trigger>
              <Tabs.Trigger value="lifecycle">{STRINGS.tabs.lifecycle}</Tabs.Trigger>
            </Tabs.List>

            {/* Summary Tab */}
            <Tabs.Content value="summary">
              <Box p="4">
                <DetailRow label={STRINGS.summary.path} value={asset?.path} isPath />
                <DetailRow label={STRINGS.summary.contentType} value={asset?.contentType} />
                <DetailRow label={STRINGS.summary.fileSize} value={formatFileSize(asset?.size)} />
                <DetailRow label={STRINGS.summary.blobCreated} value={formatDate(asset?.blobCreated)} />
                <DetailRow label={STRINGS.summary.blobUpdated} value={formatDate(asset?.blobUpdated)} />
                <DetailRow label={STRINGS.summary.lastDownloaded} value={formatDate(asset?.lastDownloaded)} />
                <DetailRow label={STRINGS.summary.locallyCached} value={asset?.locallyCached ? 'true' : 'false'} />
                <DetailRow label={STRINGS.summary.blobRef} value={asset?.blobRef} />
                <DetailRow label={STRINGS.summary.containingRepo} value={repositoryName} />
                {asset?.uploader && <DetailRow label={STRINGS.summary.uploader} value={asset.uploader} />}
                {asset?.uploaderIp && <DetailRow label={STRINGS.summary.uploaderIp} value={asset.uploaderIp} />}
              </Box>
            </Tabs.Content>

            {/* Usage Tab */}
            <Tabs.Content value="usage">
              <Box p="4">
                {usageSnippets.length > 0 ? (
                  usageSnippets.map((s) => (
                    <UsageSnippet key={s.title} title={s.title} code={s.code} />
                  ))
                ) : (
                  <Text color="gray" size="2">{STRINGS.usage.empty}</Text>
                )}
              </Box>
            </Tabs.Content>

            {/* Attributes Tab */}
            <Tabs.Content value="attributes">
              <Box p="4">
                <DetailRow label="Repository" value={repositoryName} />
                <DetailRow label="Format" value={asset?.format} />
                {component && (
                  <>
                    <DetailRow label="Group" value={component.group} />
                    <DetailRow label="Name" value={component.name} />
                    <DetailRow label="Version" value={component.version} />
                  </>
                )}
                {asset?.checksum && (
                  <>
                    {asset.checksum.sha1 && <DetailRow label="SHA-1" value={asset.checksum.sha1} />}
                    {asset.checksum.sha256 && <DetailRow label="SHA-256" value={asset.checksum.sha256} />}
                    {asset.checksum.sha512 && <DetailRow label="SHA-512" value={asset.checksum.sha512} />}
                    {asset.checksum.md5 && <DetailRow label="MD5" value={asset.checksum.md5} />}
                  </>
                )}
              </Box>
            </Tabs.Content>

            {/* Component Tags Tab */}
            <Tabs.Content value="tags">
              <TagsPanel
                tags={tags}
                onAddTag={addTag}
                onRemoveTag={removeTag}
                canEdit={canEdit}
                loading={tagsLoading}
              />
            </Tabs.Content>

            {/* Sonatype Lifecycle Tab */}
            <Tabs.Content value="lifecycle">
              <LifecyclePanel componentId={componentId} />
            </Tabs.Content>
          </Tabs.Root>

          {/* Actions Footer - Appears for all tabs */}
          <Separator size="4" />
          <Flex p="4" gap="2" justify="end" className="asset-detail-page__actions">
            {/* Research in Guide link - Component level */}
            {component?.name && component?.version && (
              <Button
                variant="soft"
                size="2"
                asChild
                data-testid="research-in-guide-link"
              >
                <a
                  href={(() => {
                    // Build Guide URL: https://guide.sonatype.com/component/{format}/{namespace:name or name}/{version}
                    const formatName = asset?.format === 'maven2' ? 'maven' : asset?.format || 'unknown';
                    const componentPath =
                      asset?.format === 'maven2' && component?.group
                        ? `${encodeURIComponent(component.group)}%3A${encodeURIComponent(component.name)}`
                        : encodeURIComponent(component.name);
                    return `https://guide.sonatype.com/component/${formatName}/${componentPath}/${encodeURIComponent(component.version)}?referrer=repo-assetdetail`;
                  })()}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  <ExternalLink size={14} />
                  Research in Guide
                </a>
              </Button>
            )}
            {asset?.downloadUrl && (
              <Button variant="soft" size="2" onClick={handleDownload}>
                <Download size={14} />
                {STRINGS.actions.download}
              </Button>
            )}
            {canDelete && (
              <Button variant="outline" color="red" size="2" onClick={handleDelete}>
                <Trash2 size={14} />
                {STRINGS.actions.delete}
              </Button>
            )}
          </Flex>
        </Card>
      </ScrollArea>
    </Box>
  );
}

export default AssetDetailPage;
