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

import { TrashBulkActionBar } from '../TrashBulkActionBar';
import { DeletedItem } from '../types';

function makeItem(overrides: Partial<DeletedItem> = {}): DeletedItem {
  return {
    id: `item-${Math.random()}`,
    name: 'test-item',
    entityType: 'repository',
    deletedOn: '2026-03-15T10:00:00Z',
    deletedBy: 'admin',
    retentionExpiresOn: '2026-04-14T10:00:00Z',
    ...overrides,
  };
}

function Wrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

describe('TrashBulkActionBar', () => {
  const onRestoreClick = jest.fn();
  const onPurgeClick = jest.fn();
  const onClearSelection = jest.fn();

  beforeEach(() => jest.clearAllMocks());

  it('renders nothing when no items are selected', () => {
    render(
      <TrashBulkActionBar
        selectedItems={[]}
        onRestoreClick={onRestoreClick}
        onPurgeClick={onPurgeClick}
        onClearSelection={onClearSelection}
      />,
      { wrapper: Wrapper }
    );

    expect(screen.queryByTestId('bulk-action-bar')).not.toBeInTheDocument();
  });

  it('renders when items are selected', () => {
    render(
      <TrashBulkActionBar
        selectedItems={[makeItem()]}
        onRestoreClick={onRestoreClick}
        onPurgeClick={onPurgeClick}
        onClearSelection={onClearSelection}
      />,
      { wrapper: Wrapper }
    );

    expect(screen.getByTestId('bulk-action-bar')).toBeInTheDocument();
    expect(screen.getByText('1 item selected:')).toBeInTheDocument();
  });

  it('displays correct count for multiple items', () => {
    render(
      <TrashBulkActionBar
        selectedItems={[makeItem(), makeItem(), makeItem()]}
        onRestoreClick={onRestoreClick}
        onPurgeClick={onPurgeClick}
        onClearSelection={onClearSelection}
      />,
      { wrapper: Wrapper }
    );

    expect(screen.getByText('3 items selected:')).toBeInTheDocument();
  });

  it('shows correct selected count for mixed entity types', () => {
    render(
      <TrashBulkActionBar
        selectedItems={[
          makeItem({ entityType: 'repository' }),
          makeItem({ entityType: 'component' }),
          makeItem({ entityType: 'component' }),
        ]}
        onRestoreClick={onRestoreClick}
        onPurgeClick={onPurgeClick}
        onClearSelection={onClearSelection}
      />,
      { wrapper: Wrapper }
    );

    expect(screen.getByText('3 items selected:')).toBeInTheDocument();
  });

  it('calls onRestoreClick when Restore button clicked', () => {
    render(
      <TrashBulkActionBar
        selectedItems={[makeItem()]}
        onRestoreClick={onRestoreClick}
        onPurgeClick={onPurgeClick}
        onClearSelection={onClearSelection}
      />,
      { wrapper: Wrapper }
    );

    fireEvent.click(screen.getByTestId('bulk-restore-button'));
    expect(onRestoreClick).toHaveBeenCalledTimes(1);
  });

  it('calls onPurgeClick when Purge button clicked', () => {
    render(
      <TrashBulkActionBar
        selectedItems={[makeItem()]}
        onRestoreClick={onRestoreClick}
        onPurgeClick={onPurgeClick}
        onClearSelection={onClearSelection}
      />,
      { wrapper: Wrapper }
    );

    fireEvent.click(screen.getByTestId('bulk-purge-button'));
    expect(onPurgeClick).toHaveBeenCalledTimes(1);
  });

  it('calls onClearSelection when Clear button clicked', () => {
    render(
      <TrashBulkActionBar
        selectedItems={[makeItem()]}
        onRestoreClick={onRestoreClick}
        onPurgeClick={onPurgeClick}
        onClearSelection={onClearSelection}
      />,
      { wrapper: Wrapper }
    );

    fireEvent.click(screen.getByTestId('clear-selection-button'));
    expect(onClearSelection).toHaveBeenCalledTimes(1);
  });

  it('disables action buttons during loading', () => {
    render(
      <TrashBulkActionBar
        selectedItems={[makeItem()]}
        restoreLoading
        onRestoreClick={onRestoreClick}
        onPurgeClick={onPurgeClick}
        onClearSelection={onClearSelection}
      />,
      { wrapper: Wrapper }
    );

    expect(screen.getByTestId('bulk-restore-button')).toBeDisabled();
    expect(screen.getByTestId('bulk-purge-button')).toBeDisabled();
    expect(screen.getByTestId('clear-selection-button')).toBeDisabled();
  });

  it('has accessible region with aria-live', () => {
    render(
      <TrashBulkActionBar
        selectedItems={[makeItem()]}
        onRestoreClick={onRestoreClick}
        onPurgeClick={onPurgeClick}
        onClearSelection={onClearSelection}
      />,
      { wrapper: Wrapper }
    );

    const bar = screen.getByTestId('bulk-action-bar');
    expect(bar).toHaveAttribute('role', 'region');
    expect(bar).toHaveAttribute('aria-live', 'polite');
  });
});
