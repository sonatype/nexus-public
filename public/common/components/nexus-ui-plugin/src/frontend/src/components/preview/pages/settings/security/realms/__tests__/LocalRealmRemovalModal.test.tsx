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
import { Theme } from '@radix-ui/themes';

import { LocalRealmRemovalModal } from '../LocalRealmRemovalModal';

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

describe('LocalRealmRemovalModal', () => {
  const mockOnClose = jest.fn();
  const mockOnConfirm = jest.fn();

  const defaultProps = {
    open: true,
    onClose: mockOnClose,
    onConfirm: mockOnConfirm,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders when open is true', () => {
    render(<LocalRealmRemovalModal {...defaultProps} />, { wrapper: TestWrapper });

    expect(screen.getByText('Confirm Removal of Local Realms')).toBeInTheDocument();
    expect(screen.getByTestId('local-realm-removal-modal')).toBeInTheDocument();
  });

  it('does not render when open is false', () => {
    render(<LocalRealmRemovalModal {...defaultProps} open={false} />, { wrapper: TestWrapper });

    expect(screen.queryByText('Confirm Removal of Local Realms')).not.toBeInTheDocument();
  });

  it('displays warning message', () => {
    render(<LocalRealmRemovalModal {...defaultProps} />, { wrapper: TestWrapper });

    expect(screen.getByText(/Warning! Removing local realms will prevent local admin access/)).toBeInTheDocument();
  });

  it('displays acknowledgement instructions', () => {
    render(<LocalRealmRemovalModal {...defaultProps} />, { wrapper: TestWrapper });

    expect(screen.getByText(/Type "I acknowledge" in order to proceed with this action/)).toBeInTheDocument();
  });

  it('has documentation link', () => {
    render(<LocalRealmRemovalModal {...defaultProps} />, { wrapper: TestWrapper });

    const link = screen.getByText('Realms help documentation');
    expect(link).toHaveAttribute('href', 'http://links.sonatype.com/products/nxrm3/docs/realms');
  });

  it('disables confirm button initially', () => {
    render(<LocalRealmRemovalModal {...defaultProps} />, { wrapper: TestWrapper });

    const confirmButton = screen.getByTestId('confirm-button');
    expect(confirmButton).toBeDisabled();
  });

  it('enables confirm button when "I acknowledge" is typed', () => {
    render(<LocalRealmRemovalModal {...defaultProps} />, { wrapper: TestWrapper });

    const input = screen.getByTestId('acknowledgement-input');
    fireEvent.change(input, { target: { value: 'I acknowledge' } });

    const confirmButton = screen.getByTestId('confirm-button');
    expect(confirmButton).not.toBeDisabled();
  });

  it('keeps confirm button disabled with partial text', () => {
    render(<LocalRealmRemovalModal {...defaultProps} />, { wrapper: TestWrapper });

    const input = screen.getByTestId('acknowledgement-input');
    fireEvent.change(input, { target: { value: 'I acknow' } });

    const confirmButton = screen.getByTestId('confirm-button');
    expect(confirmButton).toBeDisabled();
  });

  it('shows validation error with incorrect text', () => {
    render(<LocalRealmRemovalModal {...defaultProps} />, { wrapper: TestWrapper });

    const input = screen.getByTestId('acknowledgement-input');
    fireEvent.change(input, { target: { value: 'wrong text' } });

    expect(screen.getByText('Invalid acknowledgement')).toBeInTheDocument();
  });

  it('calls onConfirm when confirm button is clicked', () => {
    render(<LocalRealmRemovalModal {...defaultProps} />, { wrapper: TestWrapper });

    const input = screen.getByTestId('acknowledgement-input');
    fireEvent.change(input, { target: { value: 'I acknowledge' } });

    const confirmButton = screen.getByTestId('confirm-button');
    fireEvent.click(confirmButton);

    expect(mockOnConfirm).toHaveBeenCalledTimes(1);
  });

  it('calls onClose when cancel button is clicked', () => {
    render(<LocalRealmRemovalModal {...defaultProps} />, { wrapper: TestWrapper });

    const cancelButton = screen.getByTestId('cancel-button');
    fireEvent.click(cancelButton);

    expect(mockOnClose).toHaveBeenCalledTimes(1);
  });

  it('calls onClose when X button is clicked', () => {
    render(<LocalRealmRemovalModal {...defaultProps} />, { wrapper: TestWrapper });

    const closeButton = screen.getByLabelText('Close');
    fireEvent.click(closeButton);

    expect(mockOnClose).toHaveBeenCalledTimes(1);
  });

  it('calls onConfirm when Enter is pressed with valid acknowledgement', () => {
    render(<LocalRealmRemovalModal {...defaultProps} />, { wrapper: TestWrapper });

    const input = screen.getByTestId('acknowledgement-input');
    fireEvent.change(input, { target: { value: 'I acknowledge' } });
    fireEvent.keyDown(input, { key: 'Enter' });

    expect(mockOnConfirm).toHaveBeenCalledTimes(1);
  });

  it('does not call onConfirm when Enter is pressed with invalid acknowledgement', () => {
    render(<LocalRealmRemovalModal {...defaultProps} />, { wrapper: TestWrapper });

    const input = screen.getByTestId('acknowledgement-input');
    fireEvent.change(input, { target: { value: 'wrong' } });
    fireEvent.keyDown(input, { key: 'Enter' });

    expect(mockOnConfirm).not.toHaveBeenCalled();
  });

  it('resets acknowledgement when modal is closed and reopened', async () => {
    const { rerender } = render(<LocalRealmRemovalModal {...defaultProps} />, { wrapper: TestWrapper });

    const input = screen.getByTestId('acknowledgement-input');
    fireEvent.change(input, { target: { value: 'I acknowledge' } });
    expect(input).toHaveValue('I acknowledge');

    // Close modal
    rerender(
      <Theme>
        <LocalRealmRemovalModal {...defaultProps} open={false} />
      </Theme>
    );

    // Reopen modal
    rerender(
      <Theme>
        <LocalRealmRemovalModal {...defaultProps} open={true} />
      </Theme>
    );

    await waitFor(() => {
      const newInput = screen.getByTestId('acknowledgement-input');
      expect(newInput).toHaveValue('');
    });
  });
});
