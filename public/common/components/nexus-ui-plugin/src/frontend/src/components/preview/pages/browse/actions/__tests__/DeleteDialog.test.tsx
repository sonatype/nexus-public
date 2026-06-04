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
import { render, screen, fireEvent } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import '@testing-library/jest-dom';

import { DeleteDialog } from '../DeleteDialog';
import { ACTION_STRINGS, type DeleteItemInfo } from '../actions.types';

// Helper to wrap components with Radix Theme
function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

// Test data
const mockComponentItem: DeleteItemInfo = {
  type: 'component',
  id: 'comp-123',
  name: 'my-component',
  repositoryName: 'maven-releases',
};

const mockAssetItem: DeleteItemInfo = {
  type: 'asset',
  id: 'asset-456',
  name: 'artifact.jar',
  repositoryName: 'npm-hosted',
};

const mockFolderItem: DeleteItemInfo = {
  type: 'folder',
  id: '/path/to/folder',
  name: 'folder',
  repositoryName: 'raw-hosted',
};

const mockItemWithoutRepo: DeleteItemInfo = {
  type: 'component',
  id: 'comp-789',
  name: 'simple-component',
};

describe('DeleteDialog', () => {
  const mockOnOpenChange = jest.fn();
  const mockOnConfirm = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('rendering', () => {
    it('renders nothing when item is null', () => {
      renderWithTheme(
        <DeleteDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          item={null}
          onConfirm={mockOnConfirm}
        />
      );
      // Dialog should not render when item is null
      expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    });

    it('renders dialog when open with item', () => {
      renderWithTheme(
        <DeleteDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          item={mockComponentItem}
          onConfirm={mockOnConfirm}
        />
      );
      expect(screen.getByRole('alertdialog')).toBeInTheDocument();
    });

    it('does not render dialog when closed', () => {
      renderWithTheme(
        <DeleteDialog
          open={false}
          onOpenChange={mockOnOpenChange}
          item={mockComponentItem}
          onConfirm={mockOnConfirm}
        />
      );
      expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    });

    it('renders component delete title correctly', () => {
      renderWithTheme(
        <DeleteDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          item={mockComponentItem}
          onConfirm={mockOnConfirm}
        />
      );
      expect(screen.getByText(ACTION_STRINGS.deleteDialog.componentTitle)).toBeInTheDocument();
    });

    it('renders asset delete title correctly', () => {
      renderWithTheme(
        <DeleteDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          item={mockAssetItem}
          onConfirm={mockOnConfirm}
        />
      );
      expect(screen.getByText(ACTION_STRINGS.deleteDialog.assetTitle)).toBeInTheDocument();
    });

    it('renders folder delete title correctly', () => {
      renderWithTheme(
        <DeleteDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          item={mockFolderItem}
          onConfirm={mockOnConfirm}
        />
      );
      expect(screen.getByText(ACTION_STRINGS.deleteDialog.folderTitle)).toBeInTheDocument();
    });

    it('renders confirmation message with item name', () => {
      renderWithTheme(
        <DeleteDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          item={mockComponentItem}
          onConfirm={mockOnConfirm}
        />
      );
      expect(screen.getByText(/my-component/)).toBeInTheDocument();
      expect(screen.getByText(/This action cannot be undone/)).toBeInTheDocument();
    });

    it('renders repository name when provided', () => {
      renderWithTheme(
        <DeleteDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          item={mockComponentItem}
          onConfirm={mockOnConfirm}
        />
      );
      expect(screen.getByText(/Repository:/)).toBeInTheDocument();
      expect(screen.getByText(/maven-releases/)).toBeInTheDocument();
    });

    it('does not render repository info when not provided', () => {
      renderWithTheme(
        <DeleteDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          item={mockItemWithoutRepo}
          onConfirm={mockOnConfirm}
        />
      );
      expect(screen.queryByText(/Repository:/)).not.toBeInTheDocument();
    });

    it('renders confirm and cancel buttons', () => {
      renderWithTheme(
        <DeleteDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          item={mockComponentItem}
          onConfirm={mockOnConfirm}
        />
      );
      expect(screen.getByText(ACTION_STRINGS.deleteDialog.confirmButton)).toBeInTheDocument();
      expect(screen.getByText(ACTION_STRINGS.deleteDialog.cancelButton)).toBeInTheDocument();
    });

    it('renders without warning icon per modal design standards', () => {
      renderWithTheme(
        <DeleteDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          item={mockComponentItem}
          onConfirm={mockOnConfirm}
        />
      );
      const dialog = screen.getByRole('alertdialog');
      // Per modal.md standards, warning boxes should NOT have icons
      // Only query for AlertCircle icon specifically (not buttons with icons)
      const alertIcon = dialog.querySelector('svg[data-lucide="alert-circle"]');
      expect(alertIcon).not.toBeInTheDocument();
    });
  });

  describe('interactions', () => {
    it('calls onConfirm when confirm button is clicked', () => {
      renderWithTheme(
        <DeleteDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          item={mockComponentItem}
          onConfirm={mockOnConfirm}
        />
      );
      fireEvent.click(screen.getByText(ACTION_STRINGS.deleteDialog.confirmButton));
      expect(mockOnConfirm).toHaveBeenCalledTimes(1);
    });

    it('calls onOpenChange(false) when cancel button is clicked', () => {
      renderWithTheme(
        <DeleteDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          item={mockComponentItem}
          onConfirm={mockOnConfirm}
        />
      );
      fireEvent.click(screen.getByText(ACTION_STRINGS.deleteDialog.cancelButton));
      expect(mockOnOpenChange).toHaveBeenCalledWith(false);
    });

    it('prevents onOpenChange when isDeleting is true', () => {
      renderWithTheme(
        <DeleteDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          item={mockComponentItem}
          onConfirm={mockOnConfirm}
          isDeleting={true}
        />
      );
      // Cancel button should be disabled
      const cancelButton = screen.getByText(ACTION_STRINGS.deleteDialog.cancelButton);
      expect(cancelButton).toBeDisabled();
    });
  });

  describe('loading state', () => {
    it('shows loading spinner when isDeleting is true', () => {
      renderWithTheme(
        <DeleteDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          item={mockComponentItem}
          onConfirm={mockOnConfirm}
          isDeleting={true}
        />
      );
      expect(screen.getByText(ACTION_STRINGS.deleteDialog.deletingButton)).toBeInTheDocument();
    });

    it('disables buttons when isDeleting is true', () => {
      renderWithTheme(
        <DeleteDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          item={mockComponentItem}
          onConfirm={mockOnConfirm}
          isDeleting={true}
        />
      );
      expect(screen.getByText(ACTION_STRINGS.deleteDialog.cancelButton)).toBeDisabled();
      expect(screen.getByText(ACTION_STRINGS.deleteDialog.deletingButton).closest('button')).toBeDisabled();
    });

    it('disables delete button when deleting', () => {
      renderWithTheme(
        <DeleteDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          item={mockComponentItem}
          onConfirm={mockOnConfirm}
          isDeleting={true}
        />
      );
      const deleteButton = screen.getByText(ACTION_STRINGS.deleteDialog.deletingButton).closest('button');
      expect(deleteButton).toBeDisabled();
    });
  });

  describe('different item types', () => {
    it('shows correct message for component deletion', () => {
      renderWithTheme(
        <DeleteDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          item={mockComponentItem}
          onConfirm={mockOnConfirm}
        />
      );
      // Check title specifically
      expect(screen.getByRole('heading', { name: /delete component/i })).toBeInTheDocument();
      // Check description contains the item name
      expect(screen.getByText(/my-component/)).toBeInTheDocument();
    });

    it('shows correct message for asset deletion', () => {
      renderWithTheme(
        <DeleteDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          item={mockAssetItem}
          onConfirm={mockOnConfirm}
        />
      );
      // Check title specifically
      expect(screen.getByRole('heading', { name: /delete asset/i })).toBeInTheDocument();
      // Check description contains the item name
      expect(screen.getByText(/artifact\.jar/)).toBeInTheDocument();
    });

    it('shows correct message for folder deletion', () => {
      renderWithTheme(
        <DeleteDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          item={mockFolderItem}
          onConfirm={mockOnConfirm}
        />
      );
      // Check title specifically
      expect(screen.getByRole('heading', { name: /delete folder/i })).toBeInTheDocument();
      // Check description mentions "all its contents"
      expect(screen.getByText(/all its contents/i)).toBeInTheDocument();
    });
  });

  describe('accessibility', () => {
    it('has correct role', () => {
      renderWithTheme(
        <DeleteDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          item={mockComponentItem}
          onConfirm={mockOnConfirm}
        />
      );
      expect(screen.getByRole('alertdialog')).toBeInTheDocument();
    });

    it('has cancel button that can be focused', () => {
      renderWithTheme(
        <DeleteDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          item={mockComponentItem}
          onConfirm={mockOnConfirm}
        />
      );
      const cancelButton = screen.getByText(ACTION_STRINGS.deleteDialog.cancelButton);
      cancelButton.focus();
      expect(document.activeElement).toBe(cancelButton);
    });
  });
});

