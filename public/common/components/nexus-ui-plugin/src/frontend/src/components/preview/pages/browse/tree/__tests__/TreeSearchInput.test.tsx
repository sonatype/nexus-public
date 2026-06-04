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

import { TreeSearchInput } from '../TreeSearchInput';

describe('TreeSearchInput', () => {
  const defaultProps = {
    value: '',
    onChange: jest.fn(),
    onClear: jest.fn(),
    validation: { isValid: true },
    matchCount: 0,
    isSearchActive: false,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('rendering', () => {
    it('renders the search input', () => {
      render(<TreeSearchInput {...defaultProps} />);

      expect(screen.getByTestId('tree-search-input')).toBeInTheDocument();
      expect(screen.getByTestId('tree-search-field')).toBeInTheDocument();
    });

    it('renders with placeholder text', () => {
      render(<TreeSearchInput {...defaultProps} />);

      expect(screen.getByPlaceholderText(/filter tree/i)).toBeInTheDocument();
    });

    it('renders with the correct value', () => {
      render(<TreeSearchInput {...defaultProps} value="test" />);

      const input = screen.getByRole('textbox');
      expect(input).toHaveValue('test');
    });

    it('renders disabled state', () => {
      render(<TreeSearchInput {...defaultProps} disabled={true} />);

      const input = screen.getByRole('textbox');
      expect(input).toBeDisabled();
    });
  });

  describe('clear button', () => {
    it('shows clear button when value is present', () => {
      render(<TreeSearchInput {...defaultProps} value="test" />);

      expect(screen.getByTestId('tree-search-clear')).toBeInTheDocument();
    });

    it('hides clear button when value is empty', () => {
      render(<TreeSearchInput {...defaultProps} value="" />);

      expect(screen.queryByTestId('tree-search-clear')).not.toBeInTheDocument();
    });

    it('calls onClear when clear button is clicked', async () => {
      const onClear = jest.fn();
      render(<TreeSearchInput {...defaultProps} value="test" onClear={onClear} />);

      const clearButton = screen.getByTestId('tree-search-clear');
      await userEvent.click(clearButton);

      expect(onClear).toHaveBeenCalledTimes(1);
    });
  });

  describe('onChange', () => {
    it('calls onChange when input value changes', async () => {
      const onChange = jest.fn();
      render(<TreeSearchInput {...defaultProps} onChange={onChange} />);

      const input = screen.getByRole('textbox');
      await userEvent.type(input, 'test');

      expect(onChange).toHaveBeenCalled();
    });
  });

  describe('keyboard navigation', () => {
    it('clears input on Escape key', () => {
      const onClear = jest.fn();
      render(<TreeSearchInput {...defaultProps} value="test" onClear={onClear} />);

      const input = screen.getByRole('textbox');
      fireEvent.keyDown(input, { key: 'Escape' });

      expect(onClear).toHaveBeenCalledTimes(1);
    });

    it('does not clear on Escape when value is empty', () => {
      const onClear = jest.fn();
      render(<TreeSearchInput {...defaultProps} value="" onClear={onClear} />);

      const input = screen.getByRole('textbox');
      fireEvent.keyDown(input, { key: 'Escape' });

      expect(onClear).not.toHaveBeenCalled();
    });
  });

  describe('match count display', () => {
    it('shows match count when search is active', () => {
      render(
        <TreeSearchInput
          {...defaultProps}
          isSearchActive={true}
          matchCount={5}
        />
      );

      expect(screen.getByTestId('tree-search-match-count')).toBeInTheDocument();
      expect(screen.getByText('5 matches')).toBeInTheDocument();
    });

    it('shows singular match text for single result', () => {
      render(
        <TreeSearchInput
          {...defaultProps}
          isSearchActive={true}
          matchCount={1}
        />
      );

      expect(screen.getByText('1 match')).toBeInTheDocument();
    });

    it('shows no matches message when count is zero', () => {
      render(
        <TreeSearchInput
          {...defaultProps}
          isSearchActive={true}
          matchCount={0}
        />
      );

      expect(screen.getByText(/no matches/i)).toBeInTheDocument();
    });

    it('hides match count when search is not active', () => {
      render(
        <TreeSearchInput
          {...defaultProps}
          isSearchActive={false}
          matchCount={0}
        />
      );

      expect(screen.queryByTestId('tree-search-match-count')).not.toBeInTheDocument();
    });
  });

  describe('validation error display', () => {
    it('shows error message when validation fails', () => {
      render(
        <TreeSearchInput
          {...defaultProps}
          validation={{ isValid: false, error: 'Search term too long' }}
        />
      );

      expect(screen.getByTestId('tree-search-error')).toBeInTheDocument();
      expect(screen.getByText('Search term too long')).toBeInTheDocument();
    });

    it('applies red color to input when validation fails', () => {
      render(
        <TreeSearchInput
          {...defaultProps}
          validation={{ isValid: false, error: 'Error' }}
        />
      );

      const input = screen.getByRole('textbox');
      expect(input).toHaveAttribute('aria-invalid', 'true');
    });

    it('hides match count when validation fails', () => {
      render(
        <TreeSearchInput
          {...defaultProps}
          isSearchActive={true}
          matchCount={5}
          validation={{ isValid: false, error: 'Error' }}
        />
      );

      expect(screen.queryByTestId('tree-search-match-count')).not.toBeInTheDocument();
    });
  });

  describe('accessibility', () => {
    it('has aria-label on input', () => {
      render(<TreeSearchInput {...defaultProps} />);

      const input = screen.getByRole('textbox');
      expect(input).toHaveAttribute('aria-label', 'Filter repository tree');
    });

    it('has aria-invalid when validation fails', () => {
      render(
        <TreeSearchInput
          {...defaultProps}
          validation={{ isValid: false, error: 'Error' }}
        />
      );

      const input = screen.getByRole('textbox');
      expect(input).toHaveAttribute('aria-invalid', 'true');
    });

    it('has aria-describedby linking to error message', () => {
      render(
        <TreeSearchInput
          {...defaultProps}
          validation={{ isValid: false, error: 'Error' }}
        />
      );

      const input = screen.getByRole('textbox');
      expect(input).toHaveAttribute('aria-describedby', 'tree-search-error');
    });

    it('clear button has accessible label', () => {
      render(<TreeSearchInput {...defaultProps} value="test" />);

      const clearButton = screen.getByTestId('tree-search-clear');
      expect(clearButton).toHaveAttribute('aria-label', 'Clear filter');
    });
  });

  describe('autoFocus', () => {
    it('focuses input on mount when autoFocus is true', () => {
      render(<TreeSearchInput {...defaultProps} autoFocus={true} />);

      const input = screen.getByRole('textbox');
      expect(document.activeElement).toBe(input);
    });

    it('does not focus input on mount when autoFocus is false', () => {
      render(<TreeSearchInput {...defaultProps} autoFocus={false} />);

      const input = screen.getByRole('textbox');
      expect(document.activeElement).not.toBe(input);
    });
  });
});
