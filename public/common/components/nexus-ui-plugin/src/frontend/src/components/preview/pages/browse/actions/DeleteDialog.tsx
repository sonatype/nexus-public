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
import { Text } from '@radix-ui/themes';

import { ConfirmDialog } from '../../../shared/form';
import { ACTION_STRINGS, type DeleteDialogProps, type DeleteItemType } from './actions.types';

function getTitle(type: DeleteItemType): string {
  switch (type) {
    case 'component':
      return ACTION_STRINGS.deleteDialog.componentTitle;
    case 'asset':
      return ACTION_STRINGS.deleteDialog.assetTitle;
    case 'folder':
      return ACTION_STRINGS.deleteDialog.folderTitle;
    default:
      return 'Delete Item';
  }
}

function getMessage(type: DeleteItemType, name: string): string {
  switch (type) {
    case 'component':
      return ACTION_STRINGS.deleteDialog.componentMessage(name);
    case 'asset':
      return ACTION_STRINGS.deleteDialog.assetMessage(name);
    case 'folder':
      return ACTION_STRINGS.deleteDialog.folderMessage(name);
    default:
      return `Are you sure you want to delete "${name}"?`;
  }
}

/**
 * DeleteDialog - Domain-specific delete confirmation for browse module.
 *
 * Wraps the shared ConfirmDialog with item-type-aware messaging
 * and optional repository context.
 */
export function DeleteDialog({
  open,
  onOpenChange,
  item,
  onConfirm,
  isDeleting = false,
}: DeleteDialogProps): JSX.Element | null {
  if (!item) {
    return null;
  }

  return (
    <ConfirmDialog
      open={open}
      testId="delete-browse-item-dialog"
      onOpenChange={onOpenChange}
      title={getTitle(item.type)}
      message={getMessage(item.type, item.name)}
      confirmLabel={isDeleting ? ACTION_STRINGS.deleteDialog.deletingButton : ACTION_STRINGS.deleteDialog.confirmButton}
      cancelLabel={ACTION_STRINGS.deleteDialog.cancelButton}
      variant="danger"
      onConfirm={onConfirm}
      loading={isDeleting}
    >
      {item.repositoryName && (
        <Text size="1" color="gray" mt="2">
          Repository: {item.repositoryName}
        </Text>
      )}
    </ConfirmDialog>
  );
}

export default DeleteDialog;

