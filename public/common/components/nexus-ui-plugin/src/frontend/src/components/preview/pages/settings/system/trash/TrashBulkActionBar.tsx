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
import { Box, Button, Flex, IconButton, Text } from '@radix-ui/themes';
import { RotateCcw, Trash2, X } from 'lucide-react';

import { DeletedItem } from './types';

// =============================================================================
// TYPES
// =============================================================================

export interface TrashBulkActionBarProps {
  /** Selected items */
  selectedItems: DeletedItem[];
  /** Whether a restore operation is in progress */
  restoreLoading?: boolean;
  /** Whether a purge operation is in progress */
  purgeLoading?: boolean;
  /** Called when the user clicks Restore */
  onRestoreClick: () => void;
  /** Called when the user clicks Purge */
  onPurgeClick: () => void;
  /** Called when the user clears the selection */
  onClearSelection: () => void;
}

// =============================================================================
// COMPONENT
// =============================================================================

/**
 * TrashBulkActionBar
 *
 * Fixed-bottom bar shown when one or more table rows are selected.
 * Displays selected count and Restore / Purge actions.
 *
 * Accessibility: region landmark with aria-live so screen readers announce changes.
 */
export function TrashBulkActionBar({
  selectedItems,
  restoreLoading = false,
  purgeLoading = false,
  onRestoreClick,
  onPurgeClick,
  onClearSelection,
}: TrashBulkActionBarProps) {
  if (selectedItems.length === 0) {
    return null;
  }

  const isLoading = restoreLoading || purgeLoading;
  const n = selectedItems.length;

  return (
    <Box
      role="region"
      aria-live="polite"
      aria-label="Bulk actions for selected deleted items"
      data-testid="bulk-action-bar"
      style={{
        position: 'fixed',
        left: '50%',
        transform: 'translateX(-50%)',
        bottom: '12px',
        backgroundColor: 'var(--gray-12)',
        color: 'var(--gray-1)',
        borderRadius: '12px',
        padding: '10px 12px',
        zIndex: 9000,
        boxShadow: 'var(--shadow-4)',
        maxWidth: 'calc(100vw - 24px)',
      }}
    >
      <Flex align="center" justify="center" gap="2" wrap="wrap">
        {/* Selection count */}
        <Text size="1" weight="medium" style={{ color: 'var(--gray-1)', whiteSpace: 'nowrap' }}>
          {n} item{n !== 1 ? 's' : ''} selected:
        </Text>

        {/* Actions */}
        <Button
          variant="soft"
          color="gray"
          highContrast
          size="1"
          disabled={isLoading}
          onClick={onRestoreClick}
          aria-label={`Restore ${n} selected item${n !== 1 ? 's' : ''}`}
          data-testid="bulk-restore-button"
          style={{ background: 'none', color: 'var(--gray-1)', border: '1px solid var(--gray-6)' }}
        >
          <RotateCcw size={12} />
          Restore
        </Button>

        <Button
          variant="soft"
          color="gray"
          highContrast
          size="1"
          disabled={isLoading}
          onClick={onPurgeClick}
          aria-label={`Permanently delete ${n} selected item${n !== 1 ? 's' : ''}`}
          data-testid="bulk-purge-button"
          style={{ background: 'none', color: 'var(--gray-1)', border: '1px solid var(--gray-6)' }}
        >
          <Trash2 size={12} />
          Purge
        </Button>

        <IconButton
          variant="ghost"
          color="gray"
          size="1"
          disabled={isLoading}
          onClick={onClearSelection}
          aria-label="Dismiss bulk actions"
          data-testid="clear-selection-button"
          style={{ color: 'var(--gray-1)' }}
        >
          <X size={14} aria-hidden="true" />
        </IconButton>
      </Flex>
    </Box>
  );
}

export default TrashBulkActionBar;

