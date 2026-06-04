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
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import { Theme } from '@radix-ui/themes';
import { GenericSearchFilters } from '../GenericSearchFilters';
import type { GenericSearchFilters as FilterValues } from '../generic.types';

const wrap = (ui: React.ReactElement) => render(<Theme>{ui}</Theme>);

const defaultProps = {
  values: {} as FilterValues,
  onChange: jest.fn(),
  onSearch: jest.fn(),
  onClear: jest.fn(),
  loading: false,
};

describe('GenericSearchFilters', () => {
  beforeEach(() => jest.clearAllMocks());

  it('renders Format dropdown', () => {
    wrap(<GenericSearchFilters {...defaultProps} />);

    expect(screen.getByText('All Formats')).toBeInTheDocument();
  });

  it('shows Show Advanced Filters button initially', () => {
    wrap(<GenericSearchFilters {...defaultProps} />);

    expect(screen.getByText(/show advanced filters/i)).toBeInTheDocument();
  });

  it('toggles advanced filters panel on button click', async () => {
    wrap(<GenericSearchFilters {...defaultProps} />);

    const toggleBtn = screen.getByText(/show advanced filters/i);
    await userEvent.click(toggleBtn);

    expect(screen.getByText(/hide advanced filters/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/e.g., maven-central/i)).toBeInTheDocument();
  });

  it('does not show Clear Filters when no filters set', () => {
    wrap(<GenericSearchFilters {...defaultProps} />);

    expect(screen.queryByText(/clear filters/i)).not.toBeInTheDocument();
  });

  it('shows Clear Filters button when a filter has a value', () => {
    wrap(
      <GenericSearchFilters
        {...defaultProps}
        values={{ q: 'commons', format: undefined }}
      />
    );

    expect(screen.getByText(/clear filters/i)).toBeInTheDocument();
  });

  it('calls onClear when Clear Filters clicked', async () => {
    const onClear = jest.fn();
    wrap(
      <GenericSearchFilters
        {...defaultProps}
        values={{ q: 'commons' }}
        onClear={onClear}
      />
    );

    await userEvent.click(screen.getByText(/clear filters/i));

    expect(onClear).toHaveBeenCalled();
  });

  it('calls onSearch when Enter key pressed in advanced filter input', async () => {
    const onSearch = jest.fn();
    wrap(
      <GenericSearchFilters {...defaultProps} onSearch={onSearch} />
    );

    // Open advanced filters first
    await userEvent.click(screen.getByText(/show advanced filters/i));

    const repoInput = screen.getByPlaceholderText(/e.g., maven-central/i);
    fireEvent.keyDown(repoInput, { key: 'Enter' });

    expect(onSearch).toHaveBeenCalled();
  });

  it('shows active indicator when advanced filters have a value but panel is hidden', () => {
    wrap(
      <GenericSearchFilters
        {...defaultProps}
        values={{ repository: 'maven-central' }}
      />
    );

    // Should show "(Active)" indicator in the show button
    expect(screen.getByText(/active/i)).toBeInTheDocument();
  });
});
