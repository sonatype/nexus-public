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

import { TrashFilterSidebar } from '../TrashFilterSidebar';
import { DEFAULT_FILTER_STATE } from '../types';

function Wrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

const FACETS = {
  entityTypes: { asset: 12, component: 3, user: 1 },
  formats: { maven2: 9, npm: 6 },
  deletedBy: [
    { name: 'admin', count: 8 },
    { name: 'alice', count: 5 },
  ],
};

describe('TrashFilterSidebar', () => {
  it('renders facet counts as section badges', () => {
    render(
      <TrashFilterSidebar
        facets={FACETS}
        facetsLoading={false}
        filterState={DEFAULT_FILTER_STATE}
        onFilterChange={jest.fn()}
        onReset={jest.fn()}
        disabled={false}
      />, { wrapper: Wrapper });

    expect(screen.getByText('Asset')).toBeInTheDocument();
    expect(screen.getByText('12')).toBeInTheDocument();
    expect(screen.getByText('maven2')).toBeInTheDocument();
    expect(screen.getByText('admin')).toBeInTheDocument();
  });

  it('checking entity type calls onFilterChange with array', () => {
    const onChange = jest.fn();
    render(
      <TrashFilterSidebar
        facets={FACETS}
        facetsLoading={false}
        filterState={DEFAULT_FILTER_STATE}
        onFilterChange={onChange}
        onReset={jest.fn()}
        disabled={false}
      />, { wrapper: Wrapper });

    fireEvent.click(screen.getByLabelText(/Asset/i));

    expect(onChange).toHaveBeenCalledWith('entityTypes', expect.arrayContaining(['asset']));
  });

  it('labels deletedBy options with the resolved display name but filters by the raw principal', () => {
    const onChange = jest.fn();
    const facetsWithNames = {
      ...FACETS,
      deletedBy: [
        { name: 'jsmith@example.com', count: 8, displayName: 'John Smith' },
        { name: 'svc-account', count: 2 },
      ],
    };
    render(
      <TrashFilterSidebar
        facets={facetsWithNames}
        facetsLoading={false}
        filterState={DEFAULT_FILTER_STATE}
        onFilterChange={onChange}
        onReset={jest.fn()}
        disabled={false}
      />, { wrapper: Wrapper });

    // Label shows the friendly name; raw email is not shown as a label.
    expect(screen.getByText('John Smith')).toBeInTheDocument();
    expect(screen.queryByText('jsmith@example.com')).not.toBeInTheDocument();
    // Entry without a display name falls back to the principal.
    expect(screen.getByText('svc-account')).toBeInTheDocument();

    // Selecting the friendly label filters by the underlying principal value.
    fireEvent.click(screen.getByLabelText(/John Smith/i));
    expect(onChange).toHaveBeenCalledWith('deletedBy', expect.arrayContaining(['jsmith@example.com']));
  });

  it('renders empty sections when facets is null and loading is true', () => {
    render(
      <TrashFilterSidebar
        facets={null}
        facetsLoading={true}
        filterState={DEFAULT_FILTER_STATE}
        onFilterChange={jest.fn()}
        onReset={jest.fn()}
        disabled={false}
      />, { wrapper: Wrapper });

    // The section headings should still render even while loading.
    expect(screen.getByText(/Entity Type/i)).toBeInTheDocument();
    expect(screen.getByText(/Format/i)).toBeInTheDocument();
    expect(screen.getByText(/Deleted By/i)).toBeInTheDocument();
  });
});
