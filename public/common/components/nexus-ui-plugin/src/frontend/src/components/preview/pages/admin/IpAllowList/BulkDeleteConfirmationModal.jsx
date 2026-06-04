/*
 * Sonatype Nexus (TM) Professional Version.
 * Copyright (c) 2008-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/nexus/pro/attributions
 * "Sonatype" and "Sonatype Nexus" are trademarks of Sonatype, Inc.
 */

import React from 'react';
import { Flex, Button, Dialog, Spinner, IconButton } from '@radix-ui/themes';
import { X } from 'lucide-react';

/**
 * Confirmation modal for bulk delete operations.
 * Shows warning and requires user confirmation before deleting multiple IP addresses.
 */
export function BulkDeleteConfirmationModal({ isOpen, onClose, onConfirm, count, isLoading = false }) {
  return (
    <Dialog.Root open={isOpen} onOpenChange={onClose}>
      <Dialog.Content maxWidth="400px" data-testid="bulk-delete-modal">
        <Flex align="start" justify="between" mb="3">
          <Dialog.Title size="5">Delete {count} IP {count === 1 ? 'Address' : 'Addresses'}?</Dialog.Title>
          <Dialog.Close>
            <IconButton variant="ghost" size="1">
              <X size={18} />
            </IconButton>
          </Dialog.Close>
        </Flex>

        <Dialog.Description size="3" color="gray" style={{ marginBottom: '16px' }}>
          Are you sure you want to delete {count} selected IP {count === 1 ? 'address' : 'addresses'}?
          This action cannot be undone.
        </Dialog.Description>

        <Flex gap="3" justify="end">
          <Dialog.Close>
            <Button variant="surface" onClick={onClose} disabled={isLoading} data-testid="bulk-delete-cancel-button">
              Cancel
            </Button>
          </Dialog.Close>
          <Button variant="solid" color="red" onClick={onConfirm} disabled={isLoading} data-testid="bulk-delete-confirm-button">
            {isLoading && <Spinner size="1" style={{ marginRight: '8px' }} />}
            {isLoading ? 'Deleting...' : `Delete ${count} ${count === 1 ? 'Address' : 'Addresses'}`}
          </Button>
        </Flex>
      </Dialog.Content>
    </Dialog.Root>
  );
}
