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

import { Pagination } from '../components/Pagination';

// Wrapper component for Radix Theme
const wrapper = ({ children }: { children: React.ReactNode }) => (
  <Theme>{children}</Theme>
);

describe('Pagination', () => {
  const defaultProps = {
    currentPage: 0,
    pageCount: 5,
    onChange: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders pagination with correct page info', () => {
    render(<Pagination {...defaultProps} />, { wrapper });

    expect(screen.getByTestId('pagination')).toBeInTheDocument();
    expect(screen.getByText('Page 1 of 5')).toBeInTheDocument();
  });

  it('does not render when pageCount is 1', () => {
    render(<Pagination {...defaultProps} pageCount={1} />, { wrapper });

    expect(screen.queryByTestId('pagination')).not.toBeInTheDocument();
  });

  it('does not render when pageCount is 0', () => {
    render(<Pagination {...defaultProps} pageCount={0} />, { wrapper });

    expect(screen.queryByTestId('pagination')).not.toBeInTheDocument();
  });

  it('disables first and previous buttons on first page', () => {
    render(<Pagination {...defaultProps} currentPage={0} />, { wrapper });

    const firstButton = screen.getByLabelText('First page');
    const previousButton = screen.getByLabelText('Previous page');

    expect(firstButton).toBeDisabled();
    expect(previousButton).toBeDisabled();
  });

  it('disables next and last buttons on last page', () => {
    render(<Pagination {...defaultProps} currentPage={4} />, { wrapper });

    const nextButton = screen.getByLabelText('Next page');
    const lastButton = screen.getByLabelText('Last page');

    expect(nextButton).toBeDisabled();
    expect(lastButton).toBeDisabled();
  });

  it('enables all buttons when on middle page', () => {
    render(<Pagination {...defaultProps} currentPage={2} />, { wrapper });

    expect(screen.getByLabelText('First page')).not.toBeDisabled();
    expect(screen.getByLabelText('Previous page')).not.toBeDisabled();
    expect(screen.getByLabelText('Next page')).not.toBeDisabled();
    expect(screen.getByLabelText('Last page')).not.toBeDisabled();
  });

  it('calls onChange with 0 when clicking first', () => {
    const onChange = jest.fn();
    render(<Pagination {...defaultProps} currentPage={2} onChange={onChange} />, { wrapper });

    fireEvent.click(screen.getByLabelText('First page'));
    expect(onChange).toHaveBeenCalledWith(0);
  });

  it('calls onChange with previous page when clicking previous', () => {
    const onChange = jest.fn();
    render(<Pagination {...defaultProps} currentPage={2} onChange={onChange} />, { wrapper });

    fireEvent.click(screen.getByLabelText('Previous page'));
    expect(onChange).toHaveBeenCalledWith(1);
  });

  it('calls onChange with next page when clicking next', () => {
    const onChange = jest.fn();
    render(<Pagination {...defaultProps} currentPage={2} onChange={onChange} />, { wrapper });

    fireEvent.click(screen.getByLabelText('Next page'));
    expect(onChange).toHaveBeenCalledWith(3);
  });

  it('calls onChange with last page when clicking last', () => {
    const onChange = jest.fn();
    render(<Pagination {...defaultProps} currentPage={2} onChange={onChange} />, { wrapper });

    fireEvent.click(screen.getByLabelText('Last page'));
    expect(onChange).toHaveBeenCalledWith(4);
  });

  it('does not go below 0 when clicking previous on first page', () => {
    const onChange = jest.fn();
    render(<Pagination {...defaultProps} currentPage={0} onChange={onChange} />, { wrapper });

    // Button should be disabled, but let's verify the logic
    fireEvent.click(screen.getByLabelText('Previous page'));
    expect(onChange).not.toHaveBeenCalled();
  });

  it('does not go above pageCount - 1 when clicking next on last page', () => {
    const onChange = jest.fn();
    render(<Pagination {...defaultProps} currentPage={4} onChange={onChange} />, { wrapper });

    // Button should be disabled, but let's verify the logic
    fireEvent.click(screen.getByLabelText('Next page'));
    expect(onChange).not.toHaveBeenCalled();
  });

  it('updates page info when currentPage changes', () => {
    const { rerender } = render(<Pagination {...defaultProps} currentPage={0} />, { wrapper });

    expect(screen.getByText('Page 1 of 5')).toBeInTheDocument();

    rerender(
      <Theme>
        <Pagination {...defaultProps} currentPage={2} />
      </Theme>
    );

    expect(screen.getByText('Page 3 of 5')).toBeInTheDocument();
  });
});

