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

import React, { useState, useCallback } from 'react';
import {
  Box,
  Card,
  Flex,
  Heading,
  Text,
  DataList,
  Button,
  Badge,
  Tabs,
  Code,
  ScrollArea,
  Separator,
} from '@radix-ui/themes';
import { Trash2, Download, FileArchive, ChevronDown, Copy, Check, Tag, Shield, ExternalLink } from 'lucide-react';

import { ExtJS } from '../../../../../interface/ExtJS';
import { ConfirmDialog } from '../../../shared/form';
import { DeepResearchLink } from '../../../shared/DeepResearchLink';
import { isIqServerEnabled } from '../../browse/repository-list/useRepositoryList';
import type { AssetDetailPanelProps, AssetAttributes } from './detail.types';
import {
  formatFileSize,
  formatDate,
  getLastDownloadedDisplay,
  isAssetCached,
  getAssetDownloadUrl,
  getFilenameFromPath,
} from './detail.utils';

import './DetailPanel.scss';

/**
 * UI strings for the asset detail panel.
 */
const STRINGS = {
  tabs: {
    summary: 'Summary',
    usage: 'Usage',
    attributes: 'Attributes',
    tags: 'Component Tags',
    lifecycle: 'Sonatype Lifecycle Component',
  },
  summaryTitle: 'Summary',
  attributesTitle: 'Attributes',
  repository: 'Repository',
  format: 'Format',
  group: 'Group',
  name: 'Name',
  version: 'Version',
  path: 'Path',
  contentType: 'Content Type',
  fileSize: 'File Size',
  blobCreated: 'Blob Created',
  blobUpdated: 'Blob Updated',
  downloadCount: 'Download Count',
  downloadUnit: 'times',
  lastDownloaded: 'Last Downloaded',
  locallyCached: 'Locally Cached',
  blobRef: 'Blob Reference',
  containingRepository: 'Containing Repository',
  uploadedBy: 'Uploaded By',
  uploadedFromIp: 'Uploaded From IP',
  checksums: 'Checksums',
  deleteButton: 'Delete asset',
  downloadButton: 'Download',
  deleteConfirmTitle: 'Confirm deletion?',
  deleteConfirmButton: 'Delete',
  cancelButton: 'Cancel',
  notAvailable: '-',
  yes: 'Yes',
  no: 'No',
  copySnippet: 'Copy',
  copied: 'Copied!',
  usageDescription: 'Add this dependency to your project:',
  noUsageAvailable: 'Usage snippets not available for this format.',
  noTagsAvailable: 'No tags associated with this component.',
  lifecycleNotConfigured: 'Sonatype Lifecycle integration is not configured.',
  lifecycleDescription: 'View security and license information from Sonatype Lifecycle.',
};

/**
 * AttributeSection - Collapsible section for displaying asset attributes.
 */
interface AttributeSectionProps {
  title: string;
  attributes: Record<string, unknown>;
  defaultOpen?: boolean;
}

function AttributeSection({ title, attributes, defaultOpen = false }: AttributeSectionProps): JSX.Element | null {
  const [isOpen, setIsOpen] = useState(defaultOpen);

  const displayableAttributes = Object.entries(attributes).filter(
    ([, value]) => value !== null && value !== undefined && typeof value !== 'object'
  );

  if (displayableAttributes.length === 0) {
    return null;
  }

  return (
    <Box className="collapsible-section">
      <Flex
        className={`collapsible-section__header ${isOpen ? 'collapsible-section__header--open' : ''}`}
        onClick={() => setIsOpen(!isOpen)}
      >
        <Text className="collapsible-section__title">{title}</Text>
        <ChevronDown
          size={16}
          className={`collapsible-section__toggle ${isOpen ? 'collapsible-section__toggle--open' : ''}`}
        />
      </Flex>
      {isOpen && (
        <Box className="collapsible-section__content">
          {displayableAttributes.map(([key, value]) => (
            <Flex key={key} className="attributes-section__item">
              <Text className="attributes-section__key">{key}</Text>
              <Text className="attributes-section__value">{String(value)}</Text>
            </Flex>
          ))}
        </Box>
      )}
    </Box>
  );
}

/**
 * ChecksumSection - Displays checksum information for an asset.
 */
interface ChecksumSectionProps {
  checksums?: AssetAttributes['checksum'];
}

function ChecksumSection({ checksums }: ChecksumSectionProps): JSX.Element | null {
  if (!checksums) {
    return null;
  }

  const checksumEntries = Object.entries(checksums).filter(([, value]) => value);

  if (checksumEntries.length === 0) {
    return null;
  }

  return (
    <Box mt="4">
      <Heading size="3" mb="3">
        {STRINGS.checksums}
      </Heading>
      <DataList.Root>
        {checksumEntries.map(([algorithm, value]) => (
          <DataList.Item key={algorithm}>
            <DataList.Label minWidth="80px">{algorithm.toUpperCase()}</DataList.Label>
            <DataList.Value>
              <Text className="detail-panel__checksum">{value}</Text>
            </DataList.Value>
          </DataList.Item>
        ))}
      </DataList.Root>
    </Box>
  );
}

/**
 * UsageSnippet - Copyable code snippet for dependency usage.
 */
interface UsageSnippetProps {
  title: string;
  code: string;
}

function UsageSnippet({ title, code }: UsageSnippetProps): JSX.Element {
  const [copied, setCopied] = useState(false);

  const handleCopy = useCallback(() => {
    navigator.clipboard.writeText(code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }, [code]);

  return (
    <Box className="usage-snippet" mb="4">
      <Flex justify="between" align="center" mb="2">
        <Text weight="medium" size="2">{title}</Text>
        <Button variant="ghost" size="1" onClick={handleCopy}>
          {copied ? <Check size={14} /> : <Copy size={14} />}
          {copied ? STRINGS.copied : STRINGS.copySnippet}
        </Button>
      </Flex>
      <Box className="usage-snippet__code">
        <Code size="2" style={{ whiteSpace: 'pre-wrap', display: 'block', padding: 'var(--space-3)' }}>
          {code}
        </Code>
      </Box>
    </Box>
  );
}

/**
 * Generate usage snippets based on format.
 */
function getUsageSnippets(
  format: string,
  group: string | null | undefined,
  name: string,
  version: string | null | undefined
): { title: string; code: string }[] {
  const snippets: { title: string; code: string }[] = [];
  const g = group || '';
  const v = version || 'LATEST';

  switch (format?.toLowerCase()) {
    case 'maven2':
      snippets.push({
        title: 'Maven',
        code: `<dependency>
  <groupId>${g}</groupId>
  <artifactId>${name}</artifactId>
  <version>${v}</version>
</dependency>`,
      });
      snippets.push({
        title: 'Gradle (Groovy)',
        code: `implementation '${g}:${name}:${v}'`,
      });
      snippets.push({
        title: 'Gradle (Kotlin)',
        code: `implementation("${g}:${name}:${v}")`,
      });
      break;
    case 'npm':
      snippets.push({
        title: 'npm',
        code: `npm install ${name}@${v}`,
      });
      snippets.push({
        title: 'yarn',
        code: `yarn add ${name}@${v}`,
      });
      break;
    case 'pypi':
      snippets.push({
        title: 'pip',
        code: `pip install ${name}==${v}`,
      });
      break;
    case 'nuget':
      snippets.push({
        title: 'NuGet CLI',
        code: `Install-Package ${name} -Version ${v}`,
      });
      snippets.push({
        title: '.NET CLI',
        code: `dotnet add package ${name} --version ${v}`,
      });
      break;
    case 'docker':
      snippets.push({
        title: 'Docker Pull',
        code: `docker pull ${name}:${v}`,
      });
      break;
    case 'go':
      snippets.push({
        title: 'Go',
        code: `go get ${g}/${name}@${v}`,
      });
      break;
    case 'rubygems':
      snippets.push({
        title: 'Bundler',
        code: `gem '${name}', '~> ${v}'`,
      });
      snippets.push({
        title: 'RubyGems',
        code: `gem install ${name} -v ${v}`,
      });
      break;
    default:
      break;
  }

  return snippets;
}

/**
 * AssetDetailPanel - Displays detailed information about a selected asset.
 * Now with tabbed navigation matching the Default UI.
 */
export function AssetDetailPanel({
  asset,
  component,
  onDelete,
  canDelete = false,
  onDownload,
}: AssetDetailPanelProps): JSX.Element {
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const iqEnabled = isIqServerEnabled();

  const {
    name,
    repositoryName,
    format,
    contentType,
    size,
    blobCreated,
    blobUpdated,
    lastDownloaded,
    downloadCount,
    blobRef,
    containingRepositoryName,
    createdBy,
    createdByIp,
    attributes,
  } = asset;

  const displayName = getFilenameFromPath(name);
  const isCached = isAssetCached(contentType, size);
  const downloadUrl = getAssetDownloadUrl(repositoryName, name);

  const handleDeleteClick = useCallback(() => {
    setShowDeleteDialog(true);
  }, []);

  const handleDeleteConfirm = useCallback(() => {
    setShowDeleteDialog(false);
    onDelete?.();
  }, [onDelete]);

  const handleDownloadClick = useCallback(() => {
    if (onDownload) {
      onDownload();
    } else {
      window.open(downloadUrl, '_blank');
    }
  }, [onDownload, downloadUrl]);

  const displayValue = (value: string | null | undefined): string => value || STRINGS.notAvailable;

  // Extract format-specific attributes (excluding checksum)
  const formatAttributes = attributes
    ? Object.entries(attributes).filter(
        ([key]) => key !== 'checksum' && key !== 'content' && typeof attributes[key] === 'object'
      )
    : [];

  // Get usage snippets
  const usageSnippets = component
    ? getUsageSnippets(format, component.group, component.name, component.version)
    : [];

  return (
    <Box className="detail-panel asset-detail-panel">
      {/* Header with title and actions */}
      <Flex justify="between" align="center" mb="4" wrap="wrap" gap="2">
        <Flex align="center" gap="2">
          <FileArchive size={20} />
          <Heading size="4">{displayName}</Heading>
        </Flex>
        <Flex className="detail-panel__actions">
          {component && component.version && (
            <DeepResearchLink
              ecosystem={format}
              packageName={format === 'maven2' && component.group ? `${component.group}:${component.name}` : component.name}
              version={component.version}
              variant="soft"
              size="2"
              referrer="browse-asset-detail"
            />
          )}
          <Button variant="soft" onClick={handleDownloadClick}>
            <Download size={16} />
            {STRINGS.downloadButton}
          </Button>
          {canDelete && onDelete && (
            <Button
              variant="soft"
              color="red"
              onClick={handleDeleteClick}
              className="detail-panel__delete-btn"
            >
              <Trash2 size={16} />
              {STRINGS.deleteButton}
            </Button>
          )}
        </Flex>
      </Flex>

      {/* Tabbed Content */}
      <Tabs.Root defaultValue="summary" className="detail-panel__tabs">
        <Tabs.List>
          <Tabs.Trigger value="summary">{STRINGS.tabs.summary}</Tabs.Trigger>
          <Tabs.Trigger value="usage">{STRINGS.tabs.usage}</Tabs.Trigger>
          <Tabs.Trigger value="attributes">{STRINGS.tabs.attributes}</Tabs.Trigger>
          <Tabs.Trigger value="tags">{STRINGS.tabs.tags}</Tabs.Trigger>
          <Tabs.Trigger value="lifecycle">{STRINGS.tabs.lifecycle}</Tabs.Trigger>
        </Tabs.List>

        {/* Summary Tab */}
        <Tabs.Content value="summary">
          <ScrollArea className="detail-panel__tab-content">
            <Box py="4">
              <DataList.Root>
                <DataList.Item>
                  <DataList.Label minWidth="140px">{STRINGS.path}</DataList.Label>
                  <DataList.Value>
                    <Text className="asset-detail-panel__path">{name}</Text>
                  </DataList.Value>
                </DataList.Item>
                <DataList.Item>
                  <DataList.Label minWidth="140px">{STRINGS.contentType}</DataList.Label>
                  <DataList.Value>{displayValue(contentType)}</DataList.Value>
                </DataList.Item>
                <DataList.Item>
                  <DataList.Label minWidth="140px">{STRINGS.fileSize}</DataList.Label>
                  <DataList.Value>{formatFileSize(size)}</DataList.Value>
                </DataList.Item>
                <DataList.Item>
                  <DataList.Label minWidth="140px">{STRINGS.blobCreated}</DataList.Label>
                  <DataList.Value>{formatDate(blobCreated)}</DataList.Value>
                </DataList.Item>
                <DataList.Item>
                  <DataList.Label minWidth="140px">{STRINGS.blobUpdated}</DataList.Label>
                  <DataList.Value>{formatDate(blobUpdated)}</DataList.Value>
                </DataList.Item>
                <DataList.Item>
                  <DataList.Label minWidth="140px">{STRINGS.lastDownloaded}</DataList.Label>
                  <DataList.Value>{getLastDownloadedDisplay(lastDownloaded)}</DataList.Value>
                </DataList.Item>
                {downloadCount !== undefined && downloadCount !== null && (
                  <DataList.Item>
                    <DataList.Label minWidth="140px">{STRINGS.downloadCount}</DataList.Label>
                    <DataList.Value>{downloadCount} {STRINGS.downloadUnit}</DataList.Value>
                  </DataList.Item>
                )}
                <DataList.Item>
                  <DataList.Label minWidth="140px">{STRINGS.locallyCached}</DataList.Label>
                  <DataList.Value>
                    <Badge color={isCached ? 'green' : 'gray'}>
                      {isCached ? STRINGS.yes : STRINGS.no}
                    </Badge>
                  </DataList.Value>
                </DataList.Item>
                {blobRef && (
                  <DataList.Item>
                    <DataList.Label minWidth="140px">{STRINGS.blobRef}</DataList.Label>
                    <DataList.Value>
                      <Text className="asset-detail-panel__blob-ref">{blobRef}</Text>
                    </DataList.Value>
                  </DataList.Item>
                )}
                {containingRepositoryName && containingRepositoryName !== repositoryName && (
                  <DataList.Item>
                    <DataList.Label minWidth="140px">{STRINGS.containingRepository}</DataList.Label>
                    <DataList.Value>{containingRepositoryName}</DataList.Value>
                  </DataList.Item>
                )}
                {createdBy && (
                  <DataList.Item>
                    <DataList.Label minWidth="140px">{STRINGS.uploadedBy}</DataList.Label>
                    <DataList.Value>{createdBy}</DataList.Value>
                  </DataList.Item>
                )}
                {createdByIp && (
                  <DataList.Item>
                    <DataList.Label minWidth="140px">{STRINGS.uploadedFromIp}</DataList.Label>
                    <DataList.Value>{createdByIp}</DataList.Value>
                  </DataList.Item>
                )}
              </DataList.Root>

              {/* Checksums */}
              {attributes?.checksum && <ChecksumSection checksums={attributes.checksum} />}
            </Box>
          </ScrollArea>
        </Tabs.Content>

        {/* Usage Tab */}
        <Tabs.Content value="usage">
          <ScrollArea className="detail-panel__tab-content">
            <Box py="4">
              {usageSnippets.length > 0 ? (
                <>
                  <Text color="gray" size="2" mb="4">
                    {STRINGS.usageDescription}
                  </Text>
                  {usageSnippets.map((snippet) => (
                    <UsageSnippet key={snippet.title} title={snippet.title} code={snippet.code} />
                  ))}
                </>
              ) : (
                <Flex align="center" justify="center" py="6">
                  <Text color="gray">{STRINGS.noUsageAvailable}</Text>
                </Flex>
              )}
            </Box>
          </ScrollArea>
        </Tabs.Content>

        {/* Attributes Tab */}
        <Tabs.Content value="attributes">
          <ScrollArea className="detail-panel__tab-content">
            <Box py="4">
              {formatAttributes.length > 0 ? (
                formatAttributes.map(([key, value]) => (
                  <AttributeSection
                    key={key}
                    title={key}
                    attributes={value as Record<string, unknown>}
                    defaultOpen={true}
                  />
                ))
              ) : (
                <DataList.Root>
                  <DataList.Item>
                    <DataList.Label minWidth="140px">{STRINGS.repository}</DataList.Label>
                    <DataList.Value>{repositoryName}</DataList.Value>
                  </DataList.Item>
                  <DataList.Item>
                    <DataList.Label minWidth="140px">{STRINGS.format}</DataList.Label>
                    <DataList.Value>{format}</DataList.Value>
                  </DataList.Item>
                  {component && (
                    <>
                      <DataList.Item>
                        <DataList.Label minWidth="140px">{STRINGS.group}</DataList.Label>
                        <DataList.Value>{displayValue(component.group)}</DataList.Value>
                      </DataList.Item>
                      <DataList.Item>
                        <DataList.Label minWidth="140px">{STRINGS.name}</DataList.Label>
                        <DataList.Value>{component.name}</DataList.Value>
                      </DataList.Item>
                      <DataList.Item>
                        <DataList.Label minWidth="140px">{STRINGS.version}</DataList.Label>
                        <DataList.Value>{displayValue(component.version)}</DataList.Value>
                      </DataList.Item>
                    </>
                  )}
                </DataList.Root>
              )}
            </Box>
          </ScrollArea>
        </Tabs.Content>

        {/* Component Tags Tab */}
        <Tabs.Content value="tags">
          <ScrollArea className="detail-panel__tab-content">
            <Box py="4">
              <Flex direction="column" align="center" justify="center" py="6" gap="3">
                <Tag size={32} color="var(--gray-9)" />
                <Text color="gray">{STRINGS.noTagsAvailable}</Text>
                <Text color="gray" size="1">
                  Tags can be managed via the REST API or during upload.
                </Text>
              </Flex>
            </Box>
          </ScrollArea>
        </Tabs.Content>

        {/* Sonatype Lifecycle Tab */}
        <Tabs.Content value="lifecycle">
          <ScrollArea className="detail-panel__tab-content">
            <Box py="4">
              <Flex direction="column" align="center" justify="center" py="6" gap="3">
                <Shield size={32} color="var(--gray-9)" />
                {iqEnabled ? (
                  <>
                    <Text color="gray">Component security analysis available in IQ Server.</Text>
                    {component && component.version && (
                      <Button
                        variant="soft"
                        size="2"
                        onClick={() => {
                          const clm = ExtJS.state()?.getValue?.('clm');
                          const iqUrl = clm?.url;
                          if (iqUrl && component.name) {
                            window.open(
                              `${iqUrl}/assets/index.html#/componentDetails/${encodeURIComponent(format)}/${encodeURIComponent(component.name)}/${encodeURIComponent(component.version || '')}`,
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
                  <>
                    <Text color="gray">{STRINGS.lifecycleNotConfigured}</Text>
                    <Text color="gray" size="1">
                      {STRINGS.lifecycleDescription}
                    </Text>
                  </>
                )}
              </Flex>
            </Box>
          </ScrollArea>
        </Tabs.Content>
      </Tabs.Root>

      {/* Footer Actions */}
      <Separator size="4" />
      <Flex p="3" gap="2" justify="end">
        {component && component.version && (
          <DeepResearchLink
            ecosystem={format}
            packageName={format === 'maven2' && component.group ? `${component.group}:${component.name}` : component.name}
            version={component.version}
            variant="soft"
            size="2"
            referrer="browse-asset-detail"
          />
        )}
        <Button variant="soft" size="2" onClick={handleDownloadClick}>
          <Download size={14} />
          {STRINGS.downloadButton}
        </Button>
        {canDelete && onDelete && (
          <Button
            variant="soft"
            color="red"
            size="2"
            onClick={handleDeleteClick}
          >
            <Trash2 size={14} />
            {STRINGS.deleteButton}
          </Button>
        )}
      </Flex>

      <ConfirmDialog
        open={showDeleteDialog}
        testId="delete-asset-dialog"
        onOpenChange={setShowDeleteDialog}
        title={STRINGS.deleteConfirmTitle}
        message={<Text as="p" weight="bold">{displayName}</Text>}
        confirmLabel={STRINGS.deleteConfirmButton}
        cancelLabel={STRINGS.cancelButton}
        variant="danger"
        onConfirm={handleDeleteConfirm}
      />
    </Box>
  );
}

export default AssetDetailPanel;
