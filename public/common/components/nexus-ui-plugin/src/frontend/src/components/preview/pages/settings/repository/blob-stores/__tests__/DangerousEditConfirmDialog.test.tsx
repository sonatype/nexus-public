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

/**
 * @jest-environment jsdom
 */

import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import { DangerousEditConfirmDialog } from '../DangerousEditConfirmDialog';

const BLOB_STORE_NAME = 'my-blob-store';
const CHANGED_FIELDS = [
  { field: 'path', label: 'Path' },
  { field: 'bucketConfiguration.bucket.region', label: 'Region' },
];

function renderDialog(props: Partial<React.ComponentProps<typeof DangerousEditConfirmDialog>> = {}) {
  const defaultProps = {
    open: true,
    onClose: jest.fn(),
    onConfirm: jest.fn(),
    blobStoreName: BLOB_STORE_NAME,
    changedFields: CHANGED_FIELDS,
  };
  return render(
    <Theme>
      <DangerousEditConfirmDialog {...defaultProps} {...props} />
    </Theme>
  );
}

describe('DangerousEditConfirmDialog', () => {
  it('should render the dialog title', () => {
    renderDialog();
    expect(screen.getByText('Update Blob Store?')).toBeInTheDocument();
  });

  it('should display the warning message', () => {
    renderDialog();
    expect(screen.getByText(/change configuration fields that may cause data loss/)).toBeInTheDocument();
  });

  it('should list the changed dangerous fields', () => {
    renderDialog();
    expect(screen.getByText('Path')).toBeInTheDocument();
    expect(screen.getByText('Region')).toBeInTheDocument();
  });

  it('should display the blob store name for confirmation', () => {
    renderDialog();
    expect(screen.getByText(BLOB_STORE_NAME)).toBeInTheDocument();
  });

  it('should disable confirm button when input is empty', () => {
    renderDialog();
    const confirmButton = screen.getByRole('button', { name: 'Confirm Update' });
    expect(confirmButton).toBeDisabled();
  });

  it('should disable confirm button when input does not match', async () => {
    renderDialog();
    const input = screen.getByLabelText('Acknowledgement');
    await userEvent.type(input, 'wrong-name');
    const confirmButton = screen.getByRole('button', { name: 'Confirm Update' });
    expect(confirmButton).toBeDisabled();
  });

  it('should enable confirm button when input matches blob store name', async () => {
    renderDialog();
    const input = screen.getByLabelText('Acknowledgement');
    await userEvent.type(input, BLOB_STORE_NAME);
    const confirmButton = screen.getByRole('button', { name: 'Confirm Update' });
    expect(confirmButton).not.toBeDisabled();
  });

  it('should call onConfirm when confirm button is clicked with valid input', async () => {
    const onConfirm = jest.fn();
    renderDialog({ onConfirm });
    const input = screen.getByLabelText('Acknowledgement');
    await userEvent.type(input, BLOB_STORE_NAME);
    const confirmButton = screen.getByRole('button', { name: 'Confirm Update' });
    fireEvent.click(confirmButton);
    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it('should call onConfirm when Enter is pressed with valid input', async () => {
    const onConfirm = jest.fn();
    renderDialog({ onConfirm });
    const input = screen.getByLabelText('Acknowledgement');
    await userEvent.type(input, BLOB_STORE_NAME);
    fireEvent.keyDown(input, { key: 'Enter' });
    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it('should not call onConfirm when Enter is pressed with invalid input', async () => {
    const onConfirm = jest.fn();
    renderDialog({ onConfirm });
    const input = screen.getByLabelText('Acknowledgement');
    await userEvent.type(input, 'wrong');
    fireEvent.keyDown(input, { key: 'Enter' });
    expect(onConfirm).not.toHaveBeenCalled();
  });

  it('should show error text when input is non-empty but incorrect', async () => {
    renderDialog();
    const input = screen.getByLabelText('Acknowledgement');
    await userEvent.type(input, 'wrong');
    expect(screen.getByRole('alert')).toHaveTextContent('The confirmation text provided is incorrect');
  });

  it('should not show error text when input is empty', () => {
    renderDialog();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('should call onClose when Cancel button is clicked', () => {
    const onClose = jest.fn();
    renderDialog({ onClose });
    const cancelButton = screen.getByRole('button', { name: 'Cancel' });
    fireEvent.click(cancelButton);
    expect(onClose).toHaveBeenCalled();
  });

  it('should not render when open is false', () => {
    renderDialog({ open: false });
    expect(screen.queryByText('Update Blob Store?')).not.toBeInTheDocument();
  });
});
