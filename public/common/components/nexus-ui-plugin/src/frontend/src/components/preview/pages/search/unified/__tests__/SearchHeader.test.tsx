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
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import SearchHeader from '../SearchHeader';
import type { SearchFormat } from '../unified.types';

// Helper to wrap components with Radix Theme
function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

describe('SearchHeader', () => {
  const defaultProps = {
    format: 'all' as SearchFormat,
    onFormatChange: jest.fn(),
    query: '',
    onSearch: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders with format dropdown and search input', () => {
    renderWithTheme(<SearchHeader {...defaultProps} />);
    
    // Format dropdown should be visible
    expect(screen.getByRole('combobox')).toBeInTheDocument();
    
    // Search input should be visible
    expect(screen.getByPlaceholderText(/search by/i)).toBeInTheDocument();
  });

  it('displays correct placeholder for selected format', () => {
    renderWithTheme(<SearchHeader {...defaultProps} format="npm" />);
    
    expect(screen.getByPlaceholderText(/search by name or scope/i)).toBeInTheDocument();
  });

  it('displays custom placeholder when provided', () => {
    renderWithTheme(
      <SearchHeader {...defaultProps} placeholder="Custom placeholder text" />
    );
    
    expect(screen.getByPlaceholderText('Custom placeholder text')).toBeInTheDocument();
  });

  it('fires onSearch on every keystroke with the cumulative input value', async () => {
    const onSearch = jest.fn();
    renderWithTheme(<SearchHeader {...defaultProps} onSearch={onSearch} />);

    const input = screen.getByPlaceholderText(/search by/i);
    await userEvent.type(input, 'mav');

    expect(onSearch).toHaveBeenCalledTimes(3);
    expect(onSearch).toHaveBeenNthCalledWith(1, 'm');
    expect(onSearch).toHaveBeenNthCalledWith(2, 'ma');
    expect(onSearch).toHaveBeenNthCalledWith(3, 'mav');
  });

  it('does not require Enter to fire onSearch', async () => {
    const onSearch = jest.fn();
    renderWithTheme(<SearchHeader {...defaultProps} onSearch={onSearch} />);

    const input = screen.getByPlaceholderText(/search by/i);
    await userEvent.type(input, 'docker');
    const callsBeforeEnter = onSearch.mock.calls.length;

    fireEvent.keyDown(input, { key: 'Enter', code: 'Enter' });

    expect(onSearch).toHaveBeenCalledTimes(callsBeforeEnter);
    expect(onSearch).toHaveBeenLastCalledWith('docker');
  });

  it('fires onSearch with the empty string when the input is cleared', async () => {
    const onSearch = jest.fn();
    renderWithTheme(<SearchHeader {...defaultProps} onSearch={onSearch} query="maven" />);

    const input = screen.getByPlaceholderText(/search by/i) as HTMLInputElement;
    expect(input.value).toBe('maven');

    await userEvent.clear(input);

    expect(onSearch).toHaveBeenLastCalledWith('');
  });

  it('syncs input value with query prop', () => {
    const { rerender } = renderWithTheme(
      <SearchHeader {...defaultProps} query="initial" />
    );
    
    const input = screen.getByPlaceholderText(/search by/i) as HTMLInputElement;
    expect(input.value).toBe('initial');
    
    // Update the query prop
    rerender(
      <Theme>
        <SearchHeader {...defaultProps} query="updated" />
      </Theme>
    );
    
    expect(input.value).toBe('updated');
  });

  it('renders format dropdown with default value', () => {
    renderWithTheme(<SearchHeader {...defaultProps} />);
    
    // Format dropdown shows "All Formats" by default
    expect(screen.getByText('All Formats')).toBeInTheDocument();
  });

  it('updates local input state on typing', async () => {
    renderWithTheme(<SearchHeader {...defaultProps} />);
    
    const input = screen.getByPlaceholderText(/search by/i) as HTMLInputElement;
    await userEvent.type(input, 'hello world');
    
    expect(input.value).toBe('hello world');
  });
});


