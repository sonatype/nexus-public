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
import { GenericSearchResults } from '../GenericSearchResults';
import { mockGenericResults } from '../mockData';

const wrap = (ui: React.ReactElement) => render(<Theme>{ui}</Theme>);

describe('GenericSearchResults', () => {
  it('shows loading spinner when loading with no results', () => {
    wrap(
      <GenericSearchResults
        results={[]}
        loading={true}
        totalCount={0}
        onSelect={jest.fn()}
      />
    );

    expect(screen.getByText(/searching components/i)).toBeInTheDocument();
  });

  it('shows error callout when error is set', () => {
    wrap(
      <GenericSearchResults
        results={[]}
        loading={false}
        error="Search failed"
        totalCount={0}
        onSelect={jest.fn()}
      />
    );

    expect(screen.getByText(/search failed/i)).toBeInTheDocument();
  });

  it('shows empty state when no results and not loading', () => {
    wrap(
      <GenericSearchResults
        results={[]}
        loading={false}
        totalCount={0}
        onSelect={jest.fn()}
      />
    );

    expect(screen.getByText(/no components found/i)).toBeInTheDocument();
  });

  it('renders results table with component names', () => {
    const results = mockGenericResults.slice(0, 3);
    wrap(
      <GenericSearchResults
        results={results}
        loading={false}
        totalCount={results.length}
        onSelect={jest.fn()}
      />
    );

    expect(screen.getByText(/components found/i)).toBeInTheDocument();
    expect(screen.getByText('Name')).toBeInTheDocument();
    expect(screen.getByText('Format')).toBeInTheDocument();
    expect(screen.getByText('Version')).toBeInTheDocument();
    expect(screen.getByText('Repository')).toBeInTheDocument();
  });

  it('calls onSelect when row is clicked', async () => {
    const onSelect = jest.fn();
    const results = mockGenericResults.slice(0, 1);
    wrap(
      <GenericSearchResults
        results={results}
        loading={false}
        totalCount={1}
        onSelect={onSelect}
      />
    );

    const row = screen.getByText('commons-lang3').closest('tr');
    if (row) {
      await userEvent.click(row);
    }

    expect(onSelect).toHaveBeenCalledWith(results[0].id);
  });

  it('renders group when present', () => {
    const results = mockGenericResults.slice(0, 1);
    wrap(
      <GenericSearchResults
        results={results}
        loading={false}
        totalCount={1}
        onSelect={jest.fn()}
      />
    );

    expect(screen.getByText('org.apache.commons')).toBeInTheDocument();
  });
});
