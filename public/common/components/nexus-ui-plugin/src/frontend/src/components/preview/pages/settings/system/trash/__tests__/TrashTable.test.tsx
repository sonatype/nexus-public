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

import { TrashTable } from '../TrashTable';
import { fetchDeletedItemChildren } from '../useTrashApi';
import { DeletedItem } from '../types';

// The child-fetch is mocked so expansion tests don't hit the network.
jest.mock('../useTrashApi', () => ({
  fetchDeletedItemChildren: jest.fn(),
}));
const mockFetchChildren = fetchDeletedItemChildren as jest.MockedFunction<typeof fetchDeletedItemChildren>;

// Mock shared TablePagination to keep tests focused
jest.mock('../../../../../shared', () => ({
  ...(jest.requireActual('../../../../../shared') || {}),
  TablePagination: jest.fn(() => <div data-testid="pagination" />),
}));

// Mock Radix DropdownMenu to render content inline (avoids portal/JSDOM issues)
jest.mock('@radix-ui/themes', () => {
  const actual = jest.requireActual('@radix-ui/themes');
  return {
    ...actual,
    DropdownMenu: {
      Root: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
      Trigger: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
      Content: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
      Item: ({ children, onClick, ...props }: React.HTMLAttributes<HTMLButtonElement>) => (
        <button type="button" onClick={onClick} {...props}>{children}</button>
      ),
      Separator: () => <hr />,
    },
  };
});

function makeItem(overrides: Partial<DeletedItem> & { id: string; name: string }): DeletedItem {
  return {
    entityType: 'repository',
    deletedOn: '2026-03-15T10:00:00Z',
    deletedBy: 'admin',
    retentionExpiresOn: '2026-04-14T10:00:00Z',
    ...overrides,
  };
}

const ITEMS: DeletedItem[] = [
  makeItem({ id: 'r1', name: 'maven-central', entityType: 'repository' }),
  makeItem({
    id: 'c1',
    name: 'log4j:log4j:1.2.17',
    entityType: 'component',
    repository: 'maven-central',
    format: 'maven2',
    componentCoordinates: 'log4j:log4j:1.2.17',
  }),
  makeItem({
    id: 'a1',
    name: 'log4j-1.2.17.jar',
    entityType: 'asset',
    repository: 'maven-central',
    format: 'maven2',
    path: '/log4j/log4j/1.2.17/log4j-1.2.17.jar',
  }),
];

function Wrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

describe('TrashTable', () => {
  const onSelectionChange = jest.fn();
  const onRestoreClick = jest.fn();
  const onPurgeClick = jest.fn();
  const onSort = jest.fn();
  const onPageChange = jest.fn();

  const defaultProps = {
    selectedIds: new Set<string>(),
    onSelectionChange,
    onRestoreClick,
    onPurgeClick,
    onSort,
    onPageChange,
    sort: 'deletedOn:desc',
    offset: 0,
    limit: 20,
    total: ITEMS.length,
  };

  beforeEach(() => jest.clearAllMocks());

  it('renders table with correct item names', () => {
    render(
      <TrashTable items={ITEMS} {...defaultProps} />,
      { wrapper: Wrapper }
    );

    expect(screen.getAllByText('maven-central').length).toBeGreaterThan(0);
    expect(screen.getAllByText('log4j:log4j:1.2.17').length).toBeGreaterThan(0);
    expect(screen.getByText('log4j-1.2.17.jar')).toBeInTheDocument();
  });

  it('shows the resolved display name (with the raw principal on hover) when present, else the principal', () => {
    const items: DeletedItem[] = [
      makeItem({ id: 'n1', name: 'repo-named', deletedBy: 'jsmith@example.com', deletedByName: 'John Smith' }),
      makeItem({ id: 'n2', name: 'repo-raw', deletedBy: 'svc-account' }),
    ];
    render(<TrashTable items={items} {...defaultProps} total={items.length} />, { wrapper: Wrapper });

    const named = screen.getByText('John Smith');
    expect(named).toBeInTheDocument();
    // Raw principal is exposed via the title attribute for hover.
    expect(named).toHaveAttribute('title', 'jsmith@example.com');
    // No display name -> render the raw principal, with no title.
    const raw = screen.getByText('svc-account');
    expect(raw).toBeInTheDocument();
    expect(raw).not.toHaveAttribute('title');
  });

  it('renders entity type as plain text (Descriptor in table context)', () => {
    render(
      <TrashTable
        items={ITEMS} {...defaultProps}
      />,
      { wrapper: Wrapper }
    );

    expect(screen.getAllByText('Repository').length).toBeGreaterThan(0);
    expect(screen.getByText('Component')).toBeInTheDocument();
    expect(screen.getByText('Asset')).toBeInTheDocument();
  });

  it('renders row action trigger buttons with accessible labels', () => {
    render(
      <TrashTable
        items={ITEMS}
        {...defaultProps}
      />,
      { wrapper: Wrapper }
    );

    expect(screen.getByTestId('row-actions-r1')).toBeInTheDocument();
    expect(screen.getByLabelText('Actions for maven-central')).toBeInTheDocument();
  });

  it('calls onRestoreClick with correct item when Restore menu item clicked', () => {
    render(
      <TrashTable
        items={ITEMS}
        {...defaultProps}
      />,
      { wrapper: Wrapper }
    );

    // DropdownMenu is mocked to render inline; Restore item is directly accessible
    fireEvent.click(screen.getByTestId('restore-button-r1'));
    expect(onRestoreClick).toHaveBeenCalledWith(ITEMS[0]);
  });

  it('shows select-all checkbox', () => {
    render(
      <TrashTable
        items={ITEMS}
        {...defaultProps}
      />,
      { wrapper: Wrapper }
    );

    expect(screen.getByTestId('select-all-checkbox')).toBeInTheDocument();
  });

  it('select-all calls onSelectionChange with all ids', () => {
    render(
      <TrashTable
        items={ITEMS}
        {...defaultProps}
      />,
      { wrapper: Wrapper }
    );

    fireEvent.click(screen.getByTestId('select-all-checkbox'));
    expect(onSelectionChange).toHaveBeenCalledWith(
      new Set(['r1', 'c1', 'a1'])
    );
  });

  it('select-all unchecks all when all selected', () => {
    render(
      <TrashTable
        items={ITEMS}
        {...defaultProps}
        selectedIds={new Set(['r1', 'c1', 'a1'])}
      />,
      { wrapper: Wrapper }
    );

    fireEvent.click(screen.getByTestId('select-all-checkbox'));
    expect(onSelectionChange).toHaveBeenCalledWith(new Set());
  });

  it('individual row checkbox adds to selection', () => {
    render(
      <TrashTable
        items={ITEMS}
        {...defaultProps}
      />,
      { wrapper: Wrapper }
    );

    fireEvent.click(screen.getByLabelText('Select maven-central'));
    expect(onSelectionChange).toHaveBeenCalledWith(new Set(['r1']));
  });

  it('shows countdown for items nearing expiry', () => {
    const imminentItem = makeItem({
      id: 'x1',
      name: 'near-expiry',
      retentionExpiresOn: new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toISOString(),
    });

    render(
      <TrashTable
        items={[imminentItem]}
        {...defaultProps}
      />,
      { wrapper: Wrapper }
    );

    expect(screen.getByText(/d until purge|Purge imminent/)).toBeInTheDocument();
  });

  it('renders empty table without errors when items is empty', () => {
    render(
      <TrashTable
        items={[]}
        {...defaultProps}
        total={0}
      />,
      { wrapper: Wrapper }
    );

    expect(screen.getByTestId('trash-table')).toBeInTheDocument();
    expect(screen.queryByTestId('pagination')).not.toBeInTheDocument();
  });

  // ---------------------------------------------------------------------------
  // Expandable component -> asset hierarchy
  // ---------------------------------------------------------------------------

  it('shows an expand chevron only on component rows', () => {
    render(<TrashTable items={ITEMS} {...defaultProps} />, { wrapper: Wrapper });

    expect(screen.getByTestId('expand-toggle-c1')).toBeInTheDocument();
    // repository and asset rows are not expandable
    expect(screen.queryByTestId('expand-toggle-r1')).not.toBeInTheDocument();
    expect(screen.queryByTestId('expand-toggle-a1')).not.toBeInTheDocument();
  });

  it('lazy-loads and renders child asset rows when a component is expanded', async () => {
    mockFetchChildren.mockResolvedValue([
      makeItem({
        id: 'child-a1', name: 'log4j-1.2.17.jar', entityType: 'asset',
        repository: 'maven-central', format: 'maven2', path: '/log4j/log4j/1.2.17/log4j-1.2.17.jar',
      }),
      makeItem({
        id: 'child-a2', name: 'log4j-1.2.17.pom', entityType: 'asset',
        repository: 'maven-central', format: 'maven2', path: '/log4j/log4j/1.2.17/log4j-1.2.17.pom',
      }),
    ]);

    render(<TrashTable items={ITEMS} {...defaultProps} />, { wrapper: Wrapper });

    fireEvent.click(screen.getByTestId('expand-toggle-c1'));

    expect(mockFetchChildren).toHaveBeenCalledWith('c1');
    expect(await screen.findByTestId('child-row-child-a1')).toBeInTheDocument();
    expect(screen.getByTestId('child-row-child-a2')).toBeInTheDocument();
  });

  it('renders child asset rows as display-only, with no per-row action menu', async () => {
    const child = makeItem({
      id: 'child-a1', name: 'log4j-1.2.17.jar', entityType: 'asset',
      repository: 'maven-central', format: 'maven2', path: '/x.jar',
    });
    mockFetchChildren.mockResolvedValue([child]);

    render(<TrashTable items={ITEMS} {...defaultProps} />, { wrapper: Wrapper });

    fireEvent.click(screen.getByTestId('expand-toggle-c1'));
    await screen.findByTestId('child-row-child-a1');

    // Child assets carry no triple-dot actions menu; Restore/Purge is driven from the parent row.
    expect(screen.queryByTestId('row-actions-child-a1')).not.toBeInTheDocument();
    expect(screen.queryByTestId('restore-button-child-a1')).not.toBeInTheDocument();
    expect(screen.queryByTestId('purge-button-child-a1')).not.toBeInTheDocument();
  });

  it('shows an empty-state row when an expanded component has no soft-deleted assets', async () => {
    mockFetchChildren.mockResolvedValue([]);

    render(<TrashTable items={ITEMS} {...defaultProps} />, { wrapper: Wrapper });

    fireEvent.click(screen.getByTestId('expand-toggle-c1'));

    expect(await screen.findByText(/No soft-deleted assets/i)).toBeInTheDocument();
  });

  it('shows an error-state row when the child fetch fails', async () => {
    mockFetchChildren.mockRejectedValue(new Error('boom'));

    render(<TrashTable items={ITEMS} {...defaultProps} />, { wrapper: Wrapper });

    fireEvent.click(screen.getByTestId('expand-toggle-c1'));

    expect(await screen.findByText('boom')).toBeInTheDocument();
  });
});
