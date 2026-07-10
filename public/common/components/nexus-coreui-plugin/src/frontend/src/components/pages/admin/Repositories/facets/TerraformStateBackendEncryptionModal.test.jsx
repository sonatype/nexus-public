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
import { render, screen, fireEvent, act, waitFor } from '@testing-library/react';
import axios from 'axios';
import TerraformStateBackendEncryptionModal from './TerraformStateBackendEncryptionModal';
import UIStrings from '../../../../../constants/UIStrings';

jest.mock('axios');

jest.mock('@sonatype/react-shared-components', () => {
  const ModalMock = ({ onCancel, children }) => (
    <div role="dialog" data-testid="nx-modal">
      {children}
      <button type="button" onClick={onCancel} aria-label="__modal-close__" style={{ display: 'none' }} />
    </div>
  );
  ModalMock.Header = ({ children }) => <div data-testid="nx-modal-header">{children}</div>;
  ModalMock.Body   = ({ children }) => <div data-testid="nx-modal-body">{children}</div>;
  ModalMock.Footer = ({ children }) => <div data-testid="nx-modal-footer">{children}</div>;

  return {
    NxModal: ModalMock,
    NxWarningAlert: ({ children }) => <div role="alert">{children}</div>,
    NxFormGroup: ({ label, sublabel, children }) => (
      <div>
        <label>{label}</label>
        {sublabel && <span>{sublabel}</span>}
        {children}
      </div>
    ),
    NxFieldset: ({ label, children }) => (
      <fieldset><legend>{label}</legend>{children}</fieldset>
    ),
    NxTextInput: ({ value, onChange, validationErrors }) => (
      <div>
        <input
          role="textbox"
          value={value || ''}
          onChange={(e) => onChange && onChange(e.target.value)}
        />
        {validationErrors && validationErrors.map((err) => (
          <span key={err} role="alert">{err}</span>
        ))}
      </div>
    ),
    NxCheckbox: ({ children, isChecked, onChange }) => (
      <label>
        <input type="checkbox" checked={!!isChecked} onChange={(e) => onChange && onChange(e.target.checked)} />
        {children}
      </label>
    ),
    NxButton: ({ children, onClick, disabled }) => (
      <button onClick={onClick} disabled={disabled}>{children}</button>
    ),
  };
});

const { ENCRYPTION_MODAL } = UIStrings.REPOSITORIES.EDITOR.TERRAFORM_STATE_BACKEND;

const DEFAULT_PROPS = {
  isOpen: true,
  onClose: jest.fn(),
  repositoryName: 'test-repo',
};

function renderModal(props = {}) {
  return render(<TerraformStateBackendEncryptionModal {...DEFAULT_PROPS} {...props} />);
}

describe('TerraformStateBackendEncryptionModal', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders null when isOpen is false', () => {
    const { container } = renderModal({ isOpen: false });
    expect(container.firstChild).toBeNull();
  });

  it('renders the modal title when isOpen is true', () => {
    renderModal();
    expect(screen.getByText(ENCRYPTION_MODAL.MODAL_TITLE)).toBeInTheDocument();
  });

  it('renders the existing key warning message', () => {
    renderModal();
    expect(screen.getByText(ENCRYPTION_MODAL.EXISTING_KEY_MESSAGE)).toBeInTheDocument();
  });

  it('renders encryption key and confirm key fields', () => {
    renderModal();
    expect(screen.getByText(ENCRYPTION_MODAL.ENCRYPTION_KEY_LABEL)).toBeInTheDocument();
    expect(screen.getByText(ENCRYPTION_MODAL.ENCRYPTION_KEY_CONFIRM_LABEL)).toBeInTheDocument();
  });

  it('renders the Update Key and Cancel buttons', () => {
    renderModal();
    expect(screen.getByText(ENCRYPTION_MODAL.UPDATE_BUTTON)).toBeInTheDocument();
    expect(screen.getByText(ENCRYPTION_MODAL.CANCEL_BUTTON)).toBeInTheDocument();
  });

  it('disables the Update Key button when fields are empty', () => {
    renderModal();
    const updateButton = screen.getByText(ENCRYPTION_MODAL.UPDATE_BUTTON).closest('button');
    expect(updateButton).toBeDisabled();
  });

  it('shows key mismatch error when keys differ', async () => {
    renderModal();
    const [keyInput, confirmInput] = screen.getAllByRole('textbox');

    await act(async () => {
      fireEvent.change(keyInput, { target: { value: 'key-a' } });
      fireEvent.change(confirmInput, { target: { value: 'key-b' } });
    });

    expect(screen.getByText(ENCRYPTION_MODAL.KEY_MISMATCH_ERROR)).toBeInTheDocument();
  });

  it('clears key mismatch error when keys match', async () => {
    renderModal();
    const [keyInput, confirmInput] = screen.getAllByRole('textbox');

    await act(async () => {
      fireEvent.change(keyInput, { target: { value: 'same' } });
      fireEvent.change(confirmInput, { target: { value: 'same' } });
    });

    expect(screen.queryByText(ENCRYPTION_MODAL.KEY_MISMATCH_ERROR)).not.toBeInTheDocument();
  });

  it('enables Update Key button when keys match and are non-empty', async () => {
    renderModal();
    const [keyInput, confirmInput] = screen.getAllByRole('textbox');

    await act(async () => {
      fireEvent.change(keyInput, { target: { value: 'my-secret' } });
      fireEvent.change(confirmInput, { target: { value: 'my-secret' } });
    });

    const updateButton = screen.getByText(ENCRYPTION_MODAL.UPDATE_BUTTON).closest('button');
    expect(updateButton).not.toBeDisabled();
  });

  it('calls axios.post with correct URL and payload on submit', async () => {
    axios.post.mockResolvedValue({ data: {} });
    const onClose = jest.fn();
    renderModal({ onClose });

    const [keyInput, confirmInput] = screen.getAllByRole('textbox');
    await act(async () => {
      fireEvent.change(keyInput, { target: { value: 'my-secret' } });
      fireEvent.change(confirmInput, { target: { value: 'my-secret' } });
    });

    await act(async () => {
      fireEvent.click(screen.getByText(ENCRYPTION_MODAL.UPDATE_BUTTON).closest('button'));
    });

    await waitFor(() => {
      expect(axios.post).toHaveBeenCalledWith(
        '/service/rest/v1/repositories/test-repo/terraform-backend/encryption',
        { encryptionKey: 'my-secret', reencryptAll: false }
      );
    });
    expect(onClose).toHaveBeenCalledWith(true);
  });

  it('sends reencryptAll: true when checkbox is checked', async () => {
    axios.post.mockResolvedValue({ data: {} });
    const onClose = jest.fn();
    renderModal({ onClose });

    const [keyInput, confirmInput] = screen.getAllByRole('textbox');
    await act(async () => {
      fireEvent.change(keyInput, { target: { value: 'my-secret' } });
      fireEvent.change(confirmInput, { target: { value: 'my-secret' } });
    });

    await act(async () => {
      fireEvent.click(screen.getByRole('checkbox'));
    });

    await act(async () => {
      fireEvent.click(screen.getByText(ENCRYPTION_MODAL.UPDATE_BUTTON).closest('button'));
    });

    await waitFor(() => {
      expect(axios.post).toHaveBeenCalledWith(
        '/service/rest/v1/repositories/test-repo/terraform-backend/encryption',
        { encryptionKey: 'my-secret', reencryptAll: true }
      );
    });
    expect(onClose).toHaveBeenCalledWith(true);
  });

  it('shows an error message when the API call fails', async () => {
    axios.post.mockRejectedValue({
      response: { data: { message: 'Server error' } },
    });
    renderModal();

    const [keyInput, confirmInput] = screen.getAllByRole('textbox');
    await act(async () => {
      fireEvent.change(keyInput, { target: { value: 'my-secret' } });
      fireEvent.change(confirmInput, { target: { value: 'my-secret' } });
    });

    await act(async () => {
      fireEvent.click(screen.getByText(ENCRYPTION_MODAL.UPDATE_BUTTON).closest('button'));
    });

    await waitFor(() => {
      expect(screen.getByText('Server error')).toBeInTheDocument();
    });
  });

  it('falls back to UPDATE_ERROR string when error has no message', async () => {
    axios.post.mockRejectedValue({});
    renderModal();

    const [keyInput, confirmInput] = screen.getAllByRole('textbox');
    await act(async () => {
      fireEvent.change(keyInput, { target: { value: 'my-secret' } });
      fireEvent.change(confirmInput, { target: { value: 'my-secret' } });
    });

    await act(async () => {
      fireEvent.click(screen.getByText(ENCRYPTION_MODAL.UPDATE_BUTTON).closest('button'));
    });

    await waitFor(() => {
      expect(screen.getByText(ENCRYPTION_MODAL.UPDATE_ERROR)).toBeInTheDocument();
    });
  });

  it('calls onClose(false) when Cancel is clicked', () => {
    const onClose = jest.fn();
    renderModal({ onClose });
    fireEvent.click(screen.getByText(ENCRYPTION_MODAL.CANCEL_BUTTON).closest('button'));
    expect(onClose).toHaveBeenCalledWith(false);
  });

  it('resets form fields when modal is reopened', async () => {
    const { rerender } = renderModal();

    const [keyInput] = screen.getAllByRole('textbox');
    await act(async () => {
      fireEvent.change(keyInput, { target: { value: 'filled-key' } });
    });

    // Close then reopen the modal
    rerender(
      <TerraformStateBackendEncryptionModal {...DEFAULT_PROPS} isOpen={false} />
    );
    rerender(
      <TerraformStateBackendEncryptionModal {...DEFAULT_PROPS} isOpen={true} />
    );

    const [freshKeyInput] = screen.getAllByRole('textbox');
    expect(freshKeyInput.value).toBe('');
  });
});
