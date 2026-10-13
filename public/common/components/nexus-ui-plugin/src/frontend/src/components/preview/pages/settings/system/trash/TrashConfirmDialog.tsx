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

import React, { useEffect, useState } from 'react';
import {
  AlertDialog,
  Badge,
  Box,
  Button,
  Callout,
  Flex,
  Text,
  TextField,
} from '@radix-ui/themes';
import { AlertTriangle, Loader2, OctagonX } from 'lucide-react';

import {
  BulkAction,
  DeletedItem,
  DELETED_ITEM_TYPE_LABELS,
  DeletedItemType,
} from './types';

// =============================================================================
// TYPES
// =============================================================================

export interface TrashConfirmDialogProps {
  /** Whether the dialog is open */
  open: boolean;
  /** The action being performed */
  action: BulkAction;
  /** The items being acted on */
  items: DeletedItem[];
  /** Whether the action is in progress */
  loading?: boolean;
  /** Called when the user confirms the action */
  onConfirm: () => void;
  /** Called when the user cancels */
  onCancel: () => void;
}

// =============================================================================
// HELPERS
// =============================================================================

/** Keyword the user must type to confirm each action */
const CONFIRM_KEYWORDS: Record<BulkAction, string> = {
  restore: 'Restore',
  purge: 'Delete',
};

const MAX_LIST_ITEMS = 5;

// =============================================================================
// SUB-COMPONENTS
// =============================================================================

interface ItemListProps {
  items: DeletedItem[];
}

/** Shows each item's name and entity type badge (soft, gray — Descriptor in card context). */
function ItemList({ items }: ItemListProps) {
  const visible = items.slice(0, MAX_LIST_ITEMS);
  const overflow = items.length - visible.length;

  return (
    <Box
      p="3"
      style={{
        background: 'var(--gray-a3)',
        borderRadius: 'var(--radius-2)',
        border: '1px solid var(--gray-a6)',
      }}
    >
      <Flex direction="column" gap="2">
        {visible.map((item) => (
          <Flex key={item.id} align="center" gap="2">
            <Badge variant="soft" color="gray" size="1" style={{ flexShrink: 0 }}>
              {DELETED_ITEM_TYPE_LABELS[item.entityType as DeletedItemType]}
            </Badge>
            <Text size="2" weight="medium" style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {item.name}
            </Text>
          </Flex>
        ))}
        {overflow > 0 && (
          <Text size="1" color="gray">
            …and {overflow} more item{overflow !== 1 ? 's' : ''}
          </Text>
        )}
      </Flex>
    </Box>
  );
}

// =============================================================================
// MAIN COMPONENT
// =============================================================================

/**
 * TrashConfirmDialog
 *
 * Radix AlertDialog with a typed-keyword confirmation gate.
 * - Restore: requires typing "Restore"
 * - Purge: requires typing "Delete"
 *
 * Uses AlertDialog primitives directly so we can disable the confirm button
 * until the keyword matches.
 */
export function TrashConfirmDialog({
  open,
  action,
  items,
  loading = false,
  onConfirm,
  onCancel,
}: TrashConfirmDialogProps) {
  const [inputValue, setInputValue] = useState('');

  useEffect(() => {
    if (open) setInputValue('');
  }, [open]);
  const keyword = CONFIRM_KEYWORDS[action];
  // Case-insensitive (and whitespace-tolerant) so "delete"/"DELETE"/"Delete" are all accepted.
  const isConfirmEnabled = inputValue.trim().toLowerCase() === keyword.toLowerCase() && !loading;

  const n = items.length;
  const isRestore = action === 'restore';

  // Repository-specific notes. Purging a repository cascades to its still-soft-deleted content in Trash.
  // On restore, a repository must be manually re-added to any groups it was a member of before deletion —
  // shown only for repositories that were actually grouped (captured at soft-delete time).
  const hasRepository = items.some((item) => item.entityType === 'repository');
  // Asset restore warnings, driven per-item by parentComponentInTrash from the browse payload:
  //  - parent in Trash  -> restoring the asset also restores its component; separately-deleted
  //    siblings may remain in Trash, so the component can come back incomplete (NEXUS-53985).
  //  - parent live       -> the asset is added back to an existing component that may have been
  //    re-uploaded/changed since deletion, so its contents may not match.
  //  - unknown (older server without the flag) -> fall back to the conservative combined note.
  const assets = items.filter((item) => item.entityType === 'asset');
  // Counts (not per-item lists) so the callout conveys scale without enumerating up to 250 rows.
  // "unknown parent" (older server without the flag) folds into the conservative parent-restored note.
  const parentInTrashCount = assets.filter(
    (item) => item.parentComponentInTrash === true || item.parentComponentInTrash === undefined,
  ).length;
  const parentLiveCount = assets.filter((item) => item.parentComponentInTrash === false).length;
  const assetWord = (count: number) => (count === 1 ? 'asset' : 'assets');
  const formerGroups = Array.from(
    new Set(
      items
        .filter((item) => item.entityType === 'repository')
        .flatMap((item) => item.containingGroups ?? []),
    ),
  );

  const title = isRestore
    ? `Restore ${n} Item${n !== 1 ? 's' : ''}?`
    : `Permanently Delete ${n} Item${n !== 1 ? 's' : ''}?`;

  const description = isRestore
    ? 'Selected items will be restored to their original locations and become visible in standard views.'
    : 'Selected items will be permanently removed. This action cannot be undone.';

  function handleOpenChange(nextOpen: boolean) {
    if (!nextOpen && loading) return;
    if (!nextOpen) {
      setInputValue('');
      onCancel();
    }
  }

  return (
    <AlertDialog.Root open={open} onOpenChange={handleOpenChange}>
      <AlertDialog.Content maxWidth="480px">
        <AlertDialog.Title>{title}</AlertDialog.Title>
        <AlertDialog.Description size="2">{description}</AlertDialog.Description>

        {/* Item list */}
        <Box mt="3">
          <ItemList items={items} />
        </Box>

        {/* Warning callout */}
        <Box mt="3">
          {isRestore ? (
            <Callout.Root color="amber" variant="soft" size="1">
              <Callout.Icon>
                <AlertTriangle size={14} />
              </Callout.Icon>
              <Callout.Text size="2">
                Deletion-related tasks will pause during restore and resume automatically when complete.
              </Callout.Text>
            </Callout.Root>
          ) : (
            <Callout.Root color="red" variant="soft" size="1">
              <Callout.Icon>
                <OctagonX size={14} />
              </Callout.Icon>
              <Callout.Text size="2">
                This is irreversible — the items are permanently removed and their storage is reclaimed by
                background cleanup.
              </Callout.Text>
            </Callout.Root>
          )}
        </Box>

        {/* Restore: group re-add note, only for repositories that were actually members of a group */}
        {isRestore && formerGroups.length > 0 && (
          <Box mt="3">
            <Callout.Root color="amber" variant="soft" size="1">
              <Callout.Icon>
                <AlertTriangle size={14} />
              </Callout.Icon>
              <Callout.Text size="2">
                Repositories are restored on their own — they are not re-added to groups automatically.
                {' '}After restore, re-add manually to the group{formerGroups.length !== 1 ? 's' : ''}:{' '}
                {formerGroups.join(', ')}.
              </Callout.Text>
            </Callout.Root>
          </Box>
        )}

        {/* Restore: asset whose parent component is ALSO in Trash — restore brings the component back too */}
        {isRestore && parentInTrashCount > 0 && (
          <Box mt="3">
            <Callout.Root color="amber" variant="soft" size="1">
              <Callout.Icon>
                <AlertTriangle size={14} />
              </Callout.Icon>
              <Callout.Text size="2">
                {parentInTrashCount} {assetWord(parentInTrashCount)} whose component was also deleted will bring
                the component back — but any sibling files deleted separately stay in Trash, so it may return
                incomplete. Restore those too to fully restore the artifact.
              </Callout.Text>
            </Callout.Root>
          </Box>
        )}

        {/* Restore: asset whose parent component is LIVE — added back to an existing (possibly changed) component */}
        {isRestore && parentLiveCount > 0 && (
          <Box mt="3">
            <Callout.Root color="amber" variant="soft" size="1">
              <Callout.Icon>
                <AlertTriangle size={14} />
              </Callout.Icon>
              <Callout.Text size="2">
                {parentLiveCount} {assetWord(parentLiveCount)} will be added back to an existing component. If that
                component was re-uploaded or changed since deletion, the restored {parentLiveCount === 1 ? 'file' : 'files'} may
                not match the component's current contents.
              </Callout.Text>
            </Callout.Root>
          </Box>
        )}

        {/* Purge: cascade note, shown when the selection includes a repository */}
        {!isRestore && hasRepository && (
          <Box mt="3">
            <Callout.Root color="amber" variant="soft" size="1">
              <Callout.Icon>
                <AlertTriangle size={14} />
              </Callout.Icon>
              <Callout.Text size="2">
                Purging a repository also permanently removes any of its components and assets that are
                currently in Trash.
              </Callout.Text>
            </Callout.Root>
          </Box>
        )}

        {/* Typed-keyword gate */}
        <Box mt="4">
          <TextField.Root
            id="confirm-input"
            aria-label={`Type ${keyword} to confirm`}
            aria-describedby="confirm-input-helper"
            placeholder={`Type "${keyword}" to confirm`}
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            autoComplete="off"
            data-testid="confirm-keyword-input"
          />
          <Text id="confirm-input-helper" size="1" color="gray" mt="1" style={{ display: 'block' }}>
            type &ldquo;{keyword}&rdquo; to confirm (case-insensitive)
          </Text>
        </Box>

        {/* Actions */}
        <Flex gap="3" mt="4" justify="end">
          <AlertDialog.Cancel>
            <Button variant="soft" color="gray" disabled={loading}>
              Cancel
            </Button>
          </AlertDialog.Cancel>
          <Button
            color={isRestore ? 'green' : 'red'}
            disabled={!isConfirmEnabled}
            onClick={onConfirm}
            data-testid="confirm-action-button"
          >
            {loading && <Loader2 size={16} style={{ animation: 'spin 1s linear infinite' }} />}
            {loading
              ? isRestore ? 'Restoring...' : 'Deleting...'
              : isRestore ? 'Restore Items' : 'Permanently Delete'}
          </Button>
        </Flex>
      </AlertDialog.Content>
    </AlertDialog.Root>
  );
}

export default TrashConfirmDialog;

