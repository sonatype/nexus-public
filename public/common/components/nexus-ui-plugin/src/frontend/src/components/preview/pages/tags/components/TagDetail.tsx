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

import React, { useCallback, useState } from 'react';
import {
  Box,
  Button,
  Card,
  Code,
  Flex,
  Heading,
  IconButton,
  ScrollArea,
  Separator,
  Text,
  Tooltip,
} from '@radix-ui/themes';
import {
  ArrowLeft,
  Calendar,
  Check,
  Clock,
  Copy,
  ExternalLink,
  RefreshCw,
  Search,
  Tag,
  Trash2,
} from 'lucide-react';
import { useRouter } from '@uirouter/react';

import { useTagDetail } from '../hooks/useTagDetail';
import { deleteTag } from '../tags.api';
import { useToast } from '../../../shared/Toast';
import { ConfirmDialog } from '../../../shared/form';

import './TagDetail.scss';

/**
 * Props for the TagDetail component.
 */
export interface TagDetailProps {
  /** Name of the tag to display */
  tagName: string;
  /** Callback when back button is clicked */
  onBack?: () => void;
}

/**
 * UI Strings for the Tag Detail component.
 */
const STRINGS = {
  backToTags: 'Back to Tags',
  tagDetails: 'Tag Details',
  firstCreated: 'First Created',
  lastUpdated: 'Last Updated',
  attributes: 'Attributes',
  copyJson: 'Copy JSON',
  copied: 'Copied!',
  findTaggedItems: 'Find tagged items',
  noAttributes: 'No attributes',
  deleteTag: 'Delete Tag',
  deleteTitle: 'Delete Tag',
  deleteDescription: 'Are you sure you want to delete the tag',
  deleteWarning: 'This action cannot be undone. All associations with this tag will be removed.',
  cancel: 'Cancel',
  delete: 'Delete',
  loading: 'Loading tag details...',
  errorTitle: 'Failed to load tag',
  retry: 'Retry',
};

/**
 * Route state names for navigation.
 */
const ROUTE_STATES = {
  list: 'preview.browse.tags',
  search: 'preview.browse.search',
};

/**
 * Format a date string for display.
 */
function formatDate(dateString: string): string {
  try {
    const date = new Date(dateString);
    return date.toLocaleString(undefined, {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  } catch {
    return dateString;
  }
}

/**
 * TagDetail displays detailed information about a single tag.
 *
 * Features:
 * - Tag name header
 * - First Created / Last Updated timestamps
 * - Attributes display (JSON key-value pairs)
 * - Copy JSON to clipboard
 * - Link to search for tagged components
 * - Delete tag functionality
 * - Back navigation
 * - Loading, error states
 * - Dark mode support
 *
 * Layout:
 * ┌─────────────────────────────────────────────────────────────────┐
 * │ ← Back to Tags                                                  │
 * │                                                                 │
 * │ Tag: my-release-1.0                                             │
 * ├─────────────────────────────────────────────────────────────────┤
 * │                                                                 │
 * │ Created:      2024-01-15 10:30:00                               │
 * │ Last Updated: 2024-01-20 14:45:00                               │
 * │                                                                 │
 * ├─────────────────────────────────────────────────────────────────┤
 * │ Attributes                                                      │
 * │ ┌─────────────────────────────────────────────────────────────┐ │
 * │ │ version: 1.0.0                                              │ │
 * │ │ environment: production                                     │ │
 * │ │ release-date: 2024-01-15                                    │ │
 * │ └─────────────────────────────────────────────────────────────┘ │
 * │                                                                 │
 * │                                    [🔍 Find]  [🗑️ Delete Tag]   │
 * └─────────────────────────────────────────────────────────────────┘
 */
export function TagDetail({ tagName, onBack }: TagDetailProps): JSX.Element {
  const router = useRouter();
  const toast = useToast();
  const { state, actions } = useTagDetail(tagName);
  const [copied, setCopied] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);

  const { tagDetail, loading, error } = state;

  /**
   * Navigate back to the tags list.
   */
  const handleBack = useCallback(() => {
    if (onBack) {
      onBack();
    } else {
      router.stateService.go(ROUTE_STATES.list);
    }
  }, [onBack, router]);

  /**
   * Navigate to search for tagged items.
   */
  const handleFindTaggedItems = useCallback(() => {
    // Navigate to search with tag filter
    const searchQuery = `tags="${tagName}"`;
    window.location.hash = `#browse/search/custom/=${encodeURIComponent(searchQuery)}`;
  }, [tagName]);

  /**
   * Copy attributes JSON to clipboard.
   */
  const handleCopyJson = useCallback(async () => {
    if (!tagDetail?.attributes) return;

    try {
      const json = JSON.stringify(tagDetail.attributes, null, 2);
      await navigator.clipboard.writeText(json);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('Failed to copy to clipboard:', err);
    }
  }, [tagDetail?.attributes]);

  /**
   * Handle delete confirmation.
   */
  const handleDeleteConfirm = useCallback(async () => {
    setIsDeleting(true);
    try {
      await deleteTag(tagName);
      toast.success(`Tag "${tagName}" deleted successfully`);
      setDeleteDialogOpen(false);
      handleBack();
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to delete tag';
      toast.error(message);
    } finally {
      setIsDeleting(false);
    }
  }, [tagName, handleBack, toast]);

  // Loading state
  if (loading) {
    return (
      <div className="tag-detail" data-testid="tag-detail">
        <Box p="4">
          <Button variant="ghost" onClick={handleBack} className="tag-detail__back-button">
            <ArrowLeft size={16} />
            {STRINGS.backToTags}
          </Button>
        </Box>
        <Flex align="center" justify="center" p="6" direction="column" gap="3">
          <RefreshCw size={32} className="tag-detail__loading-icon" />
          <Text color="gray">{STRINGS.loading}</Text>
        </Flex>
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <div className="tag-detail" data-testid="tag-detail">
        <Box p="4">
          <Button variant="ghost" onClick={handleBack} className="tag-detail__back-button">
            <ArrowLeft size={16} />
            {STRINGS.backToTags}
          </Button>
        </Box>
        <Flex align="center" justify="center" p="6" direction="column" gap="3">
          <Text color="red" size="3" weight="medium">
            {STRINGS.errorTitle}
          </Text>
          <Text color="gray" size="2">
            {error}
          </Text>
          <Button variant="soft" onClick={actions.retry}>
            <RefreshCw size={16} />
            {STRINGS.retry}
          </Button>
        </Flex>
      </div>
    );
  }

  // No data state (shouldn't happen if loading is false and no error)
  if (!tagDetail) {
    return (
      <div className="tag-detail" data-testid="tag-detail">
        <Box p="4">
          <Button variant="ghost" onClick={handleBack} className="tag-detail__back-button">
            <ArrowLeft size={16} />
            {STRINGS.backToTags}
          </Button>
        </Box>
      </div>
    );
  }

  const attributesJson = JSON.stringify(tagDetail.attributes, null, 2);
  const hasAttributes =
    tagDetail.attributes && Object.keys(tagDetail.attributes).length > 0;

  return (
    <div className="tag-detail" data-testid="tag-detail">
      {/* Back Navigation */}
      <Box p="4" className="tag-detail__header">
        <Button
          variant="ghost"
          onClick={handleBack}
          className="tag-detail__back-button"
          data-testid="back-button"
        >
          <ArrowLeft size={16} />
          {STRINGS.backToTags}
        </Button>
      </Box>

      {/* Main Content */}
      <Box px="4" pb="4">
        <Card>
          {/* Tag Header */}
          <Box p="4" className="tag-detail__title-section">
            <Flex align="center" gap="3">
              <Tag size={28} className="tag-detail__icon" />
              <Box>
                <Text size="2" color="gray">
                  {STRINGS.tagDetails}
                </Text>
                <Heading size="5" data-testid="tag-name">
                  {tagDetail.name}
                </Heading>
              </Box>
            </Flex>
          </Box>

          <Separator size="4" />

          {/* Timestamps Section */}
          <Box p="4" className="tag-detail__timestamps">
            <Flex gap="6" wrap="wrap">
              <Flex align="center" gap="2">
                <Calendar size={16} className="tag-detail__timestamp-icon" />
                <Box>
                  <Text size="1" color="gray">
                    {STRINGS.firstCreated}
                  </Text>
                  <Text size="2" data-testid="first-created">
                    {formatDate(tagDetail.firstCreated)}
                  </Text>
                </Box>
              </Flex>
              <Flex align="center" gap="2">
                <Clock size={16} className="tag-detail__timestamp-icon" />
                <Box>
                  <Text size="1" color="gray">
                    {STRINGS.lastUpdated}
                  </Text>
                  <Text size="2" data-testid="last-updated">
                    {formatDate(tagDetail.lastUpdated)}
                  </Text>
                </Box>
              </Flex>
            </Flex>
          </Box>

          <Separator size="4" />

          {/* Attributes Section */}
          <Box p="4" className="tag-detail__attributes">
            <Flex justify="between" align="center" mb="3">
              <Text size="3" weight="medium">
                {STRINGS.attributes}
              </Text>
              {hasAttributes && (
                <Tooltip content={copied ? STRINGS.copied : STRINGS.copyJson}>
                  <IconButton
                    variant="ghost"
                    size="1"
                    onClick={handleCopyJson}
                    data-testid="copy-json-button"
                    aria-label={STRINGS.copyJson}
                  >
                    {copied ? <Check size={16} /> : <Copy size={16} />}
                  </IconButton>
                </Tooltip>
              )}
            </Flex>
            <ScrollArea scrollbars="vertical" style={{ maxHeight: '300px' }}>
              {hasAttributes ? (
                <Code
                  size="2"
                  className="tag-detail__json-code"
                  data-testid="attributes-json"
                >
                  <pre>{attributesJson}</pre>
                </Code>
              ) : (
                <Text color="gray" size="2" data-testid="no-attributes">
                  {STRINGS.noAttributes}
                </Text>
              )}
            </ScrollArea>
          </Box>

          <Separator size="4" />

          {/* Actions Section */}
          <Box p="4" className="tag-detail__actions">
            <Flex justify="end" gap="3">
              <Button
                variant="soft"
                onClick={handleFindTaggedItems}
                data-testid="find-tagged-items-button"
              >
                <Search size={16} />
                {STRINGS.findTaggedItems}
              </Button>
              <Button
                variant="soft"
                color="red"
                onClick={() => setDeleteDialogOpen(true)}
                data-testid="delete-tag-button"
              >
                <Trash2 size={16} />
                {STRINGS.deleteTag}
              </Button>
            </Flex>
          </Box>
        </Card>
      </Box>

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        open={deleteDialogOpen}
        testId="delete-tag-dialog"
        onOpenChange={setDeleteDialogOpen}
        title={STRINGS.deleteTitle}
        message={<>{STRINGS.deleteDescription} <strong>{tagDetail.name}</strong>?</>}
        confirmLabel={isDeleting ? 'Deleting...' : STRINGS.delete}
        cancelLabel={STRINGS.cancel}
        variant="danger"
        onConfirm={handleDeleteConfirm}
        loading={isDeleting}
      >
        <Box mt="2">
          <Text size="2" color="red">
            {STRINGS.deleteWarning}
          </Text>
        </Box>
      </ConfirmDialog>
    </div>
  );
}

export default TagDetail;

