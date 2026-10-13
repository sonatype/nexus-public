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

import { TrashConfirmDialog } from '../TrashConfirmDialog';
import { DeletedItem } from '../types';

const mockItem: DeletedItem = {
  id: 'item-1',
  name: 'maven-central',
  entityType: 'repository',
  deletedOn: '2026-03-15T10:00:00Z',
  deletedBy: 'admin',
  retentionExpiresOn: '2026-04-14T10:00:00Z',
};

const mockItems: DeletedItem[] = [
  mockItem,
  {
    id: 'item-2',
    name: 'log4j:log4j:1.2.17',
    entityType: 'component',
    repository: 'maven-central',
    format: 'maven2',
    deletedOn: '2026-03-16T09:00:00Z',
    deletedBy: 'admin',
    retentionExpiresOn: '2026-04-15T09:00:00Z',
  },
];

const componentOnlyItems: DeletedItem[] = [
  {
    id: 'item-2',
    name: 'log4j:log4j:1.2.17',
    entityType: 'component',
    repository: 'maven-central',
    format: 'maven2',
    deletedOn: '2026-03-16T09:00:00Z',
    deletedBy: 'admin',
    retentionExpiresOn: '2026-04-15T09:00:00Z',
  },
];

function Wrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

describe('TrashConfirmDialog', () => {
  const onConfirm = jest.fn();
  const onCancel = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('restore dialog', () => {
    it('renders correct title and description for restore', () => {
      render(
        <TrashConfirmDialog
          open
          action="restore"
          items={mockItems}
          onConfirm={onConfirm}
          onCancel={onCancel}
        />,
        { wrapper: Wrapper }
      );

      expect(screen.getByText('Restore 2 Items?')).toBeInTheDocument();
      expect(screen.getByText(/restored to their original locations/)).toBeInTheDocument();
    });

    it('shows item names and entity type badges in the list', () => {
      render(
        <TrashConfirmDialog
          open
          action="restore"
          items={mockItems}
          onConfirm={onConfirm}
          onCancel={onCancel}
        />,
        { wrapper: Wrapper }
      );

      // Item names visible
      expect(screen.getByText('maven-central')).toBeInTheDocument();
      expect(screen.getByText('log4j:log4j:1.2.17')).toBeInTheDocument();
      // Entity type badges visible
      expect(screen.getAllByText('Repository').length).toBeGreaterThan(0);
      expect(screen.getByText('Component')).toBeInTheDocument();
    });

    it('displays amber warning callout for restore', () => {
      render(
        <TrashConfirmDialog
          open
          action="restore"
          items={mockItems}
          onConfirm={onConfirm}
          onCancel={onCancel}
        />,
        { wrapper: Wrapper }
      );

      expect(screen.getByText(/Deletion-related tasks will pause/)).toBeInTheDocument();
    });

    const assetItem = (parentComponentInTrash?: boolean): DeletedItem => ({
      id: 'asset-1',
      name: 'log4j-1.2.17.jar',
      entityType: 'asset',
      repository: 'maven-central',
      format: 'maven2',
      path: '/log4j/log4j/1.2.17/log4j-1.2.17.jar',
      deletedOn: '2026-03-16T09:00:00Z',
      deletedBy: 'admin',
      retentionExpiresOn: '2026-04-15T09:00:00Z',
      parentComponentInTrash,
    });

    it('shows the parent-restored note when the asset parent is also in Trash', () => {
      render(
        <TrashConfirmDialog open action="restore" items={[assetItem(true)]} onConfirm={onConfirm} onCancel={onCancel} />,
        { wrapper: Wrapper }
      );
      expect(screen.getByText(/bring the component back/)).toBeInTheDocument();
      expect(screen.queryByText(/added back to an existing component/)).not.toBeInTheDocument();
    });

    it('shows the content-mismatch note when the asset parent is live', () => {
      render(
        <TrashConfirmDialog open action="restore" items={[assetItem(false)]} onConfirm={onConfirm} onCancel={onCancel} />,
        { wrapper: Wrapper }
      );
      expect(screen.getByText(/added back to an existing component/)).toBeInTheDocument();
      expect(screen.queryByText(/bring the component back/)).not.toBeInTheDocument();
    });

    it('falls back to the conservative note when parentComponentInTrash is unknown', () => {
      render(
        <TrashConfirmDialog open action="restore" items={[assetItem(undefined)]} onConfirm={onConfirm} onCancel={onCancel} />,
        { wrapper: Wrapper }
      );
      expect(screen.getByText(/bring the component back/)).toBeInTheDocument();
    });

    it('aggregates both asset notes with counts for a mixed bulk selection', () => {
      const live = (id: string): DeletedItem => ({ ...assetItem(false), id });
      const inTrash = (id: string): DeletedItem => ({ ...assetItem(true), id });
      render(
        <TrashConfirmDialog
          open
          action="restore"
          items={[live('a'), live('b'), inTrash('c')]}
          onConfirm={onConfirm}
          onCancel={onCancel}
        />,
        { wrapper: Wrapper }
      );
      // 2 live-parent assets, 1 parent-in-trash asset -> one callout per type, with counts.
      expect(screen.getByText(/2 assets will be added back to an existing component/)).toBeInTheDocument();
      expect(screen.getByText(/1 asset whose component was also deleted will bring the component back/)).toBeInTheDocument();
    });

    it('hides both asset notes when no asset is selected', () => {
      render(
        <TrashConfirmDialog open action="restore" items={componentOnlyItems} onConfirm={onConfirm} onCancel={onCancel} />,
        { wrapper: Wrapper }
      );
      expect(screen.queryByText(/bring the component back/)).not.toBeInTheDocument();
      expect(screen.queryByText(/added back to an existing component/)).not.toBeInTheDocument();
    });

    it('shows the group re-add note with group names for a repository that was in groups', () => {
      const groupedRepo: DeletedItem = {
        ...mockItem,
        id: 'repo-grouped',
        containingGroups: ['maven-public', 'maven-all'],
      };
      render(
        <TrashConfirmDialog
          open
          action="restore"
          items={[groupedRepo]}
          onConfirm={onConfirm}
          onCancel={onCancel}
        />,
        { wrapper: Wrapper }
      );

      const note = screen.getByText(/not re-added to groups automatically/);
      expect(note).toBeInTheDocument();
      expect(note.textContent).toContain('maven-public');
      expect(note.textContent).toContain('maven-all');
    });

    it('hides the group re-add note for a repository that was not in any group', () => {
      render(
        <TrashConfirmDialog
          open
          action="restore"
          items={[mockItem]}
          onConfirm={onConfirm}
          onCancel={onCancel}
        />,
        { wrapper: Wrapper }
      );

      expect(screen.queryByText(/not re-added to groups automatically/)).not.toBeInTheDocument();
    });

    it('hides the group re-add note when no repository is selected', () => {
      render(
        <TrashConfirmDialog
          open
          action="restore"
          items={componentOnlyItems}
          onConfirm={onConfirm}
          onCancel={onCancel}
        />,
        { wrapper: Wrapper }
      );

      expect(screen.queryByText(/not re-added to groups automatically/)).not.toBeInTheDocument();
    });

    it('enables confirm for the keyword case-insensitively, rejects a wrong keyword', () => {
      render(
        <TrashConfirmDialog
          open
          action="restore"
          items={mockItems}
          onConfirm={onConfirm}
          onCancel={onCancel}
        />,
        { wrapper: Wrapper }
      );

      const confirmButton = screen.getByTestId('confirm-action-button');
      expect(confirmButton).toBeDisabled();

      const input = screen.getByTestId('confirm-keyword-input');
      fireEvent.change(input, { target: { value: 'delete' } }); // wrong keyword
      expect(confirmButton).toBeDisabled();

      fireEvent.change(input, { target: { value: 'restore' } }); // correct keyword, lower-case → accepted
      expect(confirmButton).not.toBeDisabled();
    });

    it('calls onConfirm when confirm button clicked after typing keyword', () => {
      render(
        <TrashConfirmDialog
          open
          action="restore"
          items={mockItems}
          onConfirm={onConfirm}
          onCancel={onCancel}
        />,
        { wrapper: Wrapper }
      );

      const input = screen.getByTestId('confirm-keyword-input');
      fireEvent.change(input, { target: { value: 'Restore' } });
      fireEvent.click(screen.getByTestId('confirm-action-button'));
      expect(onConfirm).toHaveBeenCalledTimes(1);
    });
  });

  describe('purge dialog', () => {
    it('renders correct title and description for purge', () => {
      render(
        <TrashConfirmDialog
          open
          action="purge"
          items={[mockItem]}
          onConfirm={onConfirm}
          onCancel={onCancel}
        />,
        { wrapper: Wrapper }
      );

      expect(screen.getByText('Permanently Delete 1 Item?')).toBeInTheDocument();
      expect(screen.getAllByText(/permanently removed/).length).toBeGreaterThan(0);
    });

    it('requires the keyword (case-insensitive) to enable confirm button', () => {
      render(
        <TrashConfirmDialog
          open
          action="purge"
          items={[mockItem]}
          onConfirm={onConfirm}
          onCancel={onCancel}
        />,
        { wrapper: Wrapper }
      );

      const confirmButton = screen.getByTestId('confirm-action-button');
      expect(confirmButton).toBeDisabled();

      const input = screen.getByTestId('confirm-keyword-input');
      fireEvent.change(input, { target: { value: 'restore' } }); // wrong keyword for purge
      expect(confirmButton).toBeDisabled();

      fireEvent.change(input, { target: { value: 'delete' } }); // correct keyword, lower-case → accepted
      expect(confirmButton).not.toBeDisabled();
    });

    it('shows red irreversible warning callout for purge', () => {
      render(
        <TrashConfirmDialog
          open
          action="purge"
          items={[mockItem]}
          onConfirm={onConfirm}
          onCancel={onCancel}
        />,
        { wrapper: Wrapper }
      );

      expect(screen.getByText(/irreversible/)).toBeInTheDocument();
    });

    it('shows the cascade-purge note when a repository is selected', () => {
      render(
        <TrashConfirmDialog
          open
          action="purge"
          items={[mockItem]}
          onConfirm={onConfirm}
          onCancel={onCancel}
        />,
        { wrapper: Wrapper }
      );

      expect(
        screen.getByText(/also permanently removes any of its components and assets/)
      ).toBeInTheDocument();
    });

    it('hides the cascade-purge note when no repository is selected', () => {
      render(
        <TrashConfirmDialog
          open
          action="purge"
          items={componentOnlyItems}
          onConfirm={onConfirm}
          onCancel={onCancel}
        />,
        { wrapper: Wrapper }
      );

      expect(
        screen.queryByText(/also permanently removes any of its components and assets/)
      ).not.toBeInTheDocument();
    });

    it('confirm button stays disabled during loading', () => {
      render(
        <TrashConfirmDialog
          open
          action="purge"
          items={[mockItem]}
          loading
          onConfirm={onConfirm}
          onCancel={onCancel}
        />,
        { wrapper: Wrapper }
      );

      const input = screen.getByTestId('confirm-keyword-input');
      fireEvent.change(input, { target: { value: 'Delete' } });

      const confirmButton = screen.getByTestId('confirm-action-button');
      expect(confirmButton).toBeDisabled();
    });
  });

  it('does not render when closed', () => {
    render(
      <TrashConfirmDialog
        open={false}
        action="restore"
        items={mockItems}
        onConfirm={onConfirm}
        onCancel={onCancel}
      />,
      { wrapper: Wrapper }
    );

    expect(screen.queryByText('Restore 2 Items?')).not.toBeInTheDocument();
  });
});
