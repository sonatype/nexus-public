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
import '@testing-library/jest-dom';

import { RevokeTokenModal } from '../RevokeTokenModal';

describe('RevokeTokenModal', () => {
  const defaultProps = {
    open: true,
    tokenName: 'jenkins-prod',
    onConfirm: jest.fn(),
    onClose: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders when open and moves focus to the warning so screen readers announce it', () => {
    render(<RevokeTokenModal {...defaultProps} />);

    const modal = screen.getByTestId('sat-revoke-modal');
    expect(modal).toBeInTheDocument();
    expect(modal).toHaveTextContent('jenkins-prod');

    const warning = screen.getByText(/Revoking/);
    const wrapper = warning.closest('[tabindex="-1"]');
    expect(wrapper).toBeInTheDocument();
    expect(wrapper).toHaveFocus();
  });

  it('disables the confirm button until the user types the exact token name', () => {
    render(<RevokeTokenModal {...defaultProps} />);

    const confirm = screen.getByTestId('sat-revoke-confirm');
    expect(confirm).toBeDisabled();

    const input = screen.getByTestId('sat-revoke-input');
    fireEvent.change(input, { target: { value: 'wrong' } });
    expect(confirm).toBeDisabled();

    fireEvent.change(input, { target: { value: 'jenkins-prod' } });
    expect(confirm).not.toBeDisabled();
  });

  it('calls onConfirm when confirm is clicked with matching name', () => {
    const onConfirm = jest.fn();
    render(<RevokeTokenModal {...defaultProps} onConfirm={onConfirm} />);

    const input = screen.getByTestId('sat-revoke-input');
    fireEvent.change(input, { target: { value: 'jenkins-prod' } });

    fireEvent.click(screen.getByTestId('sat-revoke-confirm'));
    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it('calls onClose when cancel is clicked', () => {
    const onClose = jest.fn();
    render(<RevokeTokenModal {...defaultProps} onClose={onClose} />);

    fireEvent.click(screen.getByTestId('sat-revoke-cancel'));
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('disables both buttons while loading', () => {
    render(<RevokeTokenModal {...defaultProps} loading />);

    const input = screen.getByTestId('sat-revoke-input');
    fireEvent.change(input, { target: { value: 'jenkins-prod' } });

    expect(screen.getByTestId('sat-revoke-confirm')).toBeDisabled();
    expect(screen.getByTestId('sat-revoke-cancel')).toBeDisabled();
  });
});
