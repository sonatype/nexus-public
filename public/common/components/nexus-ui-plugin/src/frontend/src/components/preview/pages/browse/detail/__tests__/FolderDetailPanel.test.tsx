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
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import { Theme } from '@radix-ui/themes';

import { FolderDetailPanel } from '../FolderDetailPanel';
import type { FolderInfo } from '../detail.types';

// Test wrapper with Radix Theme
const renderWithTheme = (ui: React.ReactElement) => {
  return render(<Theme>{ui}</Theme>);
};

// Mock folder data
const mockFolder: FolderInfo = {
  path: '/org/apache/commons',
  folderName: 'commons',
  repositoryName: 'maven-releases',
};

const mockRootFolder: FolderInfo = {
  path: '/',
  folderName: '',
  repositoryName: 'raw-hosted',
};

const mockDeepFolder: FolderInfo = {
  path: '/very/deep/nested/folder/structure',
  folderName: 'structure',
  repositoryName: 'npm-hosted',
};

describe('FolderDetailPanel', () => {
  describe('rendering', () => {
    it('renders folder name as heading', () => {
      renderWithTheme(<FolderDetailPanel folder={mockFolder} />);

      expect(screen.getByRole('heading', { name: 'commons' })).toBeInTheDocument();
    });

    it('renders repository name', () => {
      renderWithTheme(<FolderDetailPanel folder={mockFolder} />);

      expect(screen.getByText('maven-releases')).toBeInTheDocument();
    });

    it('renders full path', () => {
      renderWithTheme(<FolderDetailPanel folder={mockFolder} />);

      expect(screen.getByText('/org/apache/commons')).toBeInTheDocument();
    });

    it('renders deep folder path', () => {
      renderWithTheme(<FolderDetailPanel folder={mockDeepFolder} />);

      expect(screen.getByText('/very/deep/nested/folder/structure')).toBeInTheDocument();
    });

    it('renders root folder path', () => {
      renderWithTheme(<FolderDetailPanel folder={mockRootFolder} />);

      expect(screen.getByText('/')).toBeInTheDocument();
    });

    it('renders "Folder" section title', () => {
      renderWithTheme(<FolderDetailPanel folder={mockFolder} />);

      expect(screen.getByRole('heading', { name: 'Folder' })).toBeInTheDocument();
    });
  });

  describe('warning message', () => {
    it('shows warning when canDelete is true', () => {
      renderWithTheme(
        <FolderDetailPanel folder={mockFolder} canDelete={true} onDelete={jest.fn()} />
      );

      expect(
        screen.getByText(/Deleting a folder will remove all assets within it/i)
      ).toBeInTheDocument();
    });

    it('does not show warning when canDelete is false', () => {
      renderWithTheme(<FolderDetailPanel folder={mockFolder} canDelete={false} />);

      expect(
        screen.queryByText(/Deleting a folder will remove all assets within it/i)
      ).not.toBeInTheDocument();
    });

    it('does not show warning when onDelete is not provided', () => {
      renderWithTheme(<FolderDetailPanel folder={mockFolder} canDelete={true} />);

      expect(
        screen.queryByText(/Deleting a folder will remove all assets within it/i)
      ).not.toBeInTheDocument();
    });
  });

  describe('delete button', () => {
    it('does not show delete button when canDelete is false', () => {
      renderWithTheme(
        <FolderDetailPanel folder={mockFolder} canDelete={false} onDelete={jest.fn()} />
      );

      expect(screen.queryByRole('button', { name: /delete folder/i })).not.toBeInTheDocument();
    });

    it('does not show delete button when onDelete is not provided', () => {
      renderWithTheme(<FolderDetailPanel folder={mockFolder} canDelete={true} />);

      expect(screen.queryByRole('button', { name: /delete folder/i })).not.toBeInTheDocument();
    });

    it('shows delete button when canDelete is true and onDelete is provided', () => {
      renderWithTheme(
        <FolderDetailPanel folder={mockFolder} canDelete={true} onDelete={jest.fn()} />
      );

      expect(screen.getByRole('button', { name: /delete folder/i })).toBeInTheDocument();
    });

    it('opens confirmation dialog when delete button is clicked', async () => {
      renderWithTheme(
        <FolderDetailPanel folder={mockFolder} canDelete={true} onDelete={jest.fn()} />
      );

      const deleteButton = screen.getByRole('button', { name: /delete folder/i });
      await userEvent.click(deleteButton);

      expect(screen.getByText('Delete the entire folder?')).toBeInTheDocument();
    });

    it('shows folder name in confirmation dialog', async () => {
      renderWithTheme(
        <FolderDetailPanel folder={mockFolder} canDelete={true} onDelete={jest.fn()} />
      );

      const deleteButton = screen.getByRole('button', { name: /delete folder/i });
      await userEvent.click(deleteButton);

      expect(screen.getByText(/under folder 'commons' will be removed/i)).toBeInTheDocument();
    });

    it('calls onDelete when delete is confirmed', async () => {
      const onDelete = jest.fn();
      renderWithTheme(
        <FolderDetailPanel folder={mockFolder} canDelete={true} onDelete={onDelete} />
      );

      // Open dialog
      const deleteButton = screen.getByRole('button', { name: /delete folder/i });
      await userEvent.click(deleteButton);

      // Confirm delete
      const confirmButton = screen.getByRole('button', { name: /^Delete$/i });
      await userEvent.click(confirmButton);

      expect(onDelete).toHaveBeenCalledTimes(1);
    });

    it('closes dialog without calling onDelete when cancelled', async () => {
      const onDelete = jest.fn();
      renderWithTheme(
        <FolderDetailPanel folder={mockFolder} canDelete={true} onDelete={onDelete} />
      );

      // Open dialog
      const deleteButton = screen.getByRole('button', { name: /delete folder/i });
      await userEvent.click(deleteButton);

      // Cancel
      const cancelButton = screen.getByRole('button', { name: /cancel/i });
      await userEvent.click(cancelButton);

      expect(onDelete).not.toHaveBeenCalled();
    });
  });

  describe('accessibility', () => {
    it('has proper heading hierarchy', () => {
      renderWithTheme(<FolderDetailPanel folder={mockFolder} />);

      const headings = screen.getAllByRole('heading');
      expect(headings.length).toBeGreaterThanOrEqual(2);
    });

    it('delete button is focusable', () => {
      renderWithTheme(
        <FolderDetailPanel folder={mockFolder} canDelete={true} onDelete={jest.fn()} />
      );

      const deleteButton = screen.getByRole('button', { name: /delete folder/i });
      deleteButton.focus();
      expect(deleteButton).toHaveFocus();
    });
  });
});

