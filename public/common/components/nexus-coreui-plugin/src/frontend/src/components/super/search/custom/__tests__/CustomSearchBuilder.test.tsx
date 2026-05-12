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
import { CustomSearchBuilder } from '../CustomSearchBuilder';
import { createEmptyFilter } from '../custom.types';

describe('CustomSearchBuilder', () => {
  const defaultProps = {
    filters: [createEmptyFilter()],
    onUpdateFilter: jest.fn(),
    onRemoveFilter: jest.fn(),
    onAddFilter: jest.fn(),
    onSearch: jest.fn(),
    onClear: jest.fn(),
    loading: false,
    hasFilters: false,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  const renderWithTheme = (ui: React.ReactElement) => {
    return render(<Theme>{ui}</Theme>);
  };

  it('renders header and description', () => {
    renderWithTheme(<CustomSearchBuilder {...defaultProps} />);

    expect(screen.getByText('Search Criteria')).toBeInTheDocument();
    expect(
      screen.getByText(/Build your search query by adding filter criteria/)
    ).toBeInTheDocument();
  });

  it('renders filter rows', () => {
    const filters = [createEmptyFilter(), createEmptyFilter()];
    renderWithTheme(<CustomSearchBuilder {...defaultProps} filters={filters} />);

    const removeButtons = screen.getAllByLabelText('Remove filter');
    expect(removeButtons.length).toBe(2);
  });

  it('calls onAddFilter when Add Filter is clicked', () => {
    renderWithTheme(<CustomSearchBuilder {...defaultProps} />);

    const addButton = screen.getByText('Add Filter');
    fireEvent.click(addButton);

    expect(defaultProps.onAddFilter).toHaveBeenCalledTimes(1);
  });

  it('calls onSearch when Search button is clicked', () => {
    renderWithTheme(<CustomSearchBuilder {...defaultProps} />);

    const searchButton = screen.getByText('Search');
    fireEvent.click(searchButton);

    expect(defaultProps.onSearch).toHaveBeenCalledTimes(1);
  });

  it('calls onSearch when Enter key is pressed', () => {
    renderWithTheme(<CustomSearchBuilder {...defaultProps} />);

    // Find the TextField input (placeholder is "e.g., spring-boot" for Keyword field)
    const input = screen.getByPlaceholderText(/e\.g\., spring-boot/i);
    fireEvent.keyDown(input, { key: 'Enter' });

    expect(defaultProps.onSearch).toHaveBeenCalledTimes(1);
  });

  it('shows Clear All button when hasFilters is true', () => {
    renderWithTheme(<CustomSearchBuilder {...defaultProps} hasFilters={true} />);

    expect(screen.getByText('Clear All')).toBeInTheDocument();
  });

  it('hides Clear All button when hasFilters is false', () => {
    renderWithTheme(<CustomSearchBuilder {...defaultProps} hasFilters={false} />);

    expect(screen.queryByText('Clear All')).not.toBeInTheDocument();
  });

  it('calls onClear when Clear All is clicked', () => {
    renderWithTheme(<CustomSearchBuilder {...defaultProps} hasFilters={true} />);

    const clearButton = screen.getByText('Clear All');
    fireEvent.click(clearButton);

    expect(defaultProps.onClear).toHaveBeenCalledTimes(1);
  });

  it('disables buttons when loading', () => {
    renderWithTheme(<CustomSearchBuilder {...defaultProps} loading={true} />);

    expect(screen.getByText('Add Filter')).toBeDisabled();
    expect(screen.getByText('Searching...')).toBeDisabled();
  });

  it('shows loading state on search button', () => {
    renderWithTheme(<CustomSearchBuilder {...defaultProps} loading={true} />);

    expect(screen.getByText('Searching...')).toBeInTheDocument();
    expect(screen.queryByText('Search')).not.toBeInTheDocument();
  });
});


