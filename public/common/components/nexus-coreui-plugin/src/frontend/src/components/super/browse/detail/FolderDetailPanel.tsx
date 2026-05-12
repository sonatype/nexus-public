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
  Button,
  Callout,
} from '@radix-ui/themes';
import { Trash2, Folder, AlertTriangle } from 'lucide-react';

import { ConfirmDialog } from '../../../../components/super/shared/form';
import type { FolderDetailPanelProps } from './detail.types';

import './DetailPanel.scss';

/**
 * UI strings for the folder detail panel.
 */
const STRINGS = {
  title: 'Folder',
  path: 'Path',
  repository: 'Repository',
  deleteButton: 'Delete folder',
  deleteConfirmTitle: 'Delete the entire folder?',
  deleteConfirmMessage:
    "All assets you have permission to delete under folder '{folder}' will be removed. The view will not automatically refresh to show progress. This operation cannot be undone.",
  deleteConfirmButton: 'Delete',
  cancelButton: 'Cancel',
  warningMessage:
    'Deleting a folder will remove all assets within it that you have permission to delete. This action cannot be undone.',
};

/**
 * FolderDetailPanel - Displays information about a selected folder.
 *
 * Shows the folder path and provides an action to delete the entire folder
 * and its contents. Includes a warning about the destructive nature of the
 * delete operation.
 */
export function FolderDetailPanel({
  folder,
  onDelete,
  canDelete = false,
}: FolderDetailPanelProps): JSX.Element {
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);

  const { path, folderName, repositoryName } = folder;

  const handleDeleteClick = useCallback(() => {
    setShowDeleteDialog(true);
  }, []);

  const handleDeleteConfirm = useCallback(() => {
    setShowDeleteDialog(false);
    onDelete?.();
  }, [onDelete]);

  const deleteMessage = STRINGS.deleteConfirmMessage.replace('{folder}', folderName);

  return (
    <Box className="detail-panel folder-detail-panel">
      {/* Header with title and actions */}
      <Flex justify="between" align="center" mb="4">
        <Flex align="center" gap="2">
          <Folder size={20} />
          <Heading size="4">{folderName}</Heading>
        </Flex>
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

      {/* Folder Info Card */}
      <Card mb="4">
        <Heading size="3" mb="3">
          {STRINGS.title}
        </Heading>
        <Box mb="3">
          <Text size="2" color="gray" mb="1" as="p">
            {STRINGS.repository}
          </Text>
          <Text size="2">{repositoryName}</Text>
        </Box>
        <Box>
          <Text size="2" color="gray" mb="1" as="p">
            {STRINGS.path}
          </Text>
          <Text className="folder-detail-panel__path">{path}</Text>
        </Box>
      </Card>

      {/* Delete Warning */}
      {canDelete && onDelete && (
        <Callout.Root color="amber">
          <Callout.Icon>
            <AlertTriangle size={16} />
          </Callout.Icon>
          <Callout.Text>{STRINGS.warningMessage}</Callout.Text>
        </Callout.Root>
      )}

      <ConfirmDialog
        open={showDeleteDialog}
        testId="delete-folder-dialog"
        onOpenChange={setShowDeleteDialog}
        title={STRINGS.deleteConfirmTitle}
        message={deleteMessage}
        confirmLabel={STRINGS.deleteConfirmButton}
        cancelLabel={STRINGS.cancelButton}
        variant="danger"
        onConfirm={handleDeleteConfirm}
      />
    </Box>
  );
}

export default FolderDetailPanel;

