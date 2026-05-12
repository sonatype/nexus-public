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
  Separator,
  Code,
} from '@radix-ui/themes';
import { Trash2, Package, Copy, ExternalLink } from 'lucide-react';

import { ConfirmDialog } from '../../../../components/super/shared/form';
import type { ComponentDetailPanelProps } from './detail.types';

import './DetailPanel.scss';

/**
 * UI strings for the component detail panel.
 */
const STRINGS = {
  title: 'Summary',
  repository: 'Repository',
  format: 'Format',
  group: 'Group',
  name: 'Name',
  version: 'Version',
  deleteButton: 'Delete component',
  deleteConfirmTitle: 'Confirm deletion?',
  deleteConfirmMessage: 'This will delete all asset(s) associated with the component:',
  deleteConfirmButton: 'Delete',
  cancelButton: 'Cancel',
  dependencySnippetTitle: 'Dependency Information',
  mavenSnippetTitle: 'Maven',
  gradleSnippetTitle: 'Gradle',
  copyButton: 'Copy',
  copiedMessage: 'Copied!',
  notAvailable: '-',
};

/**
 * Generates Maven dependency snippet for a component.
 */
function getMavenSnippet(group: string | null, name: string, version: string | null): string {
  const groupId = group || 'GROUP_ID';
  const artifactId = name || 'ARTIFACT_ID';
  const ver = version || 'VERSION';

  return `<dependency>
    <groupId>${groupId}</groupId>
    <artifactId>${artifactId}</artifactId>
    <version>${ver}</version>
</dependency>`;
}

/**
 * Generates Gradle dependency snippet for a component.
 */
function getGradleSnippet(group: string | null, name: string, version: string | null): string {
  const groupId = group || 'GROUP_ID';
  const artifactId = name || 'ARTIFACT_ID';
  const ver = version || 'VERSION';

  return `implementation '${groupId}:${artifactId}:${ver}'`;
}

/**
 * ComponentDetailPanel - Displays detailed information about a selected component.
 *
 * Shows component metadata (repository, format, group, name, version) and
 * provides actions for deleting the component. For Maven components, also
 * displays dependency snippets.
 */
export function ComponentDetailPanel({
  component,
  onDelete,
  canDelete = false,
}: ComponentDetailPanelProps): JSX.Element {
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [copiedSnippet, setCopiedSnippet] = useState<string | null>(null);

  const { repositoryName, format, group, name, version } = component;

  const isMavenFormat = format === 'maven2';

  const handleDeleteClick = useCallback(() => {
    setShowDeleteDialog(true);
  }, []);

  const handleDeleteConfirm = useCallback(() => {
    setShowDeleteDialog(false);
    onDelete?.();
  }, [onDelete]);

  const handleCopySnippet = useCallback(async (snippet: string, type: string) => {
    try {
      await navigator.clipboard.writeText(snippet);
      setCopiedSnippet(type);
      setTimeout(() => setCopiedSnippet(null), 2000);
    } catch (err) {
      console.error('Failed to copy snippet:', err);
    }
  }, []);

  const displayValue = (value: string | null): string => value || STRINGS.notAvailable;

  return (
    <Box className="detail-panel component-detail-panel">
      {/* Header with title and actions */}
      <Flex justify="between" align="center" mb="4">
        <Flex align="center" gap="2">
          <Package size={20} />
          <Heading size="4">{name}</Heading>
        </Flex>
        <Flex gap="2">
          {version && (
            <Button
              variant="soft"
              asChild
              data-testid="deep-research-link"
            >
              <a
                href={(() => {
                  // Build Guide URL: https://guide.sonatype.com/component/{format}/{namespace:name or name}/{version}
                  const formatName = format === 'maven2' ? 'maven' : format;
                  const componentPath =
                    format === 'maven2' && group
                      ? `${encodeURIComponent(group)}%3A${encodeURIComponent(name)}`
                      : encodeURIComponent(name);
                  return `https://guide.sonatype.com/component/${formatName}/${componentPath}/${encodeURIComponent(version)}?referrer=repo-componentdetail`;
                })()}
                target="_blank"
                rel="noopener noreferrer"
              >
                <ExternalLink size={16} />
                Research in Guide
              </a>
            </Button>
          )}
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

      {/* Summary Card */}
      <Card mb="4">
        <Heading size="3" mb="3">
          {STRINGS.title}
        </Heading>
        <DataList.Root>
          <DataList.Item>
            <DataList.Label minWidth="120px">{STRINGS.repository}</DataList.Label>
            <DataList.Value>{repositoryName}</DataList.Value>
          </DataList.Item>
          <DataList.Item>
            <DataList.Label minWidth="120px">{STRINGS.format}</DataList.Label>
            <DataList.Value>{format}</DataList.Value>
          </DataList.Item>
          <DataList.Item>
            <DataList.Label minWidth="120px">{STRINGS.group}</DataList.Label>
            <DataList.Value>{displayValue(group)}</DataList.Value>
          </DataList.Item>
          <DataList.Item>
            <DataList.Label minWidth="120px">{STRINGS.name}</DataList.Label>
            <DataList.Value>{name}</DataList.Value>
          </DataList.Item>
          <DataList.Item>
            <DataList.Label minWidth="120px">{STRINGS.version}</DataList.Label>
            <DataList.Value>{displayValue(version)}</DataList.Value>
          </DataList.Item>
        </DataList.Root>
      </Card>

      {/* Dependency Snippets for Maven */}
      {isMavenFormat && (
        <Card>
          <Heading size="3" mb="3">
            {STRINGS.dependencySnippetTitle}
          </Heading>

          {/* Maven Snippet */}
          <Box mb="4">
            <Flex justify="between" align="center" mb="2">
              <Text weight="medium" size="2">
                {STRINGS.mavenSnippetTitle}
              </Text>
              <Button
                variant="ghost"
                size="1"
                onClick={() => handleCopySnippet(getMavenSnippet(group, name, version), 'maven')}
              >
                <Copy size={14} />
                {copiedSnippet === 'maven' ? STRINGS.copiedMessage : STRINGS.copyButton}
              </Button>
            </Flex>
            <Code
              size="2"
              className="detail-panel__code-block"
              style={{ display: 'block', whiteSpace: 'pre', overflowX: 'auto' }}
            >
              {getMavenSnippet(group, name, version)}
            </Code>
          </Box>

          <Separator size="4" mb="4" />

          {/* Gradle Snippet */}
          <Box>
            <Flex justify="between" align="center" mb="2">
              <Text weight="medium" size="2">
                {STRINGS.gradleSnippetTitle}
              </Text>
              <Button
                variant="ghost"
                size="1"
                onClick={() => handleCopySnippet(getGradleSnippet(group, name, version), 'gradle')}
              >
                <Copy size={14} />
                {copiedSnippet === 'gradle' ? STRINGS.copiedMessage : STRINGS.copyButton}
              </Button>
            </Flex>
            <Code
              size="2"
              className="detail-panel__code-block"
              style={{ display: 'block', whiteSpace: 'pre', overflowX: 'auto' }}
            >
              {getGradleSnippet(group, name, version)}
            </Code>
          </Box>
        </Card>
      )}

      <ConfirmDialog
        open={showDeleteDialog}
        testId="delete-component-dialog"
        onOpenChange={setShowDeleteDialog}
        title={STRINGS.deleteConfirmTitle}
        message={<>{STRINGS.deleteConfirmMessage}<Text as="p" weight="bold" mt="2">{name}</Text></>}
        confirmLabel={STRINGS.deleteConfirmButton}
        cancelLabel={STRINGS.cancelButton}
        variant="danger"
        onConfirm={handleDeleteConfirm}
      />
    </Box>
  );
}

export default ComponentDetailPanel;

