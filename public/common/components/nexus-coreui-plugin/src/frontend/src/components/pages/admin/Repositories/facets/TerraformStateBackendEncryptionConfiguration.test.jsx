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
import { render, screen, fireEvent, act, within } from '@testing-library/react';
import TerraformStateBackendEncryptionConfiguration from './TerraformStateBackendEncryptionConfiguration';
import UIStrings from '../../../../../constants/UIStrings';

jest.mock('@sonatype/react-shared-components', () => ({
  // fieldset/legend gives the group ARIA role with an accessible name from legend,
  // allowing getByRole('group', { name: label }) without coupling to DOM structure.
  NxFormGroup: ({ label, children }) => (
    <fieldset>
      <legend>{label}</legend>
      {children}
    </fieldset>
  ),
  // validatable gate mirrors the real NxTextInput — errors only render when the field is validatable.
  NxTextInput: ({ value, onChange, validationErrors, validatable }) => (
    <div>
      <input
        role="textbox"
        value={value || ''}
        onChange={(e) => onChange && onChange(e.target.value)}
      />
      {validatable && validationErrors && validationErrors.map((err) => (
        <span key={err} role="alert">{err}</span>
      ))}
    </div>
  ),
  NxStatefulInfoAlert: ({ children }) => <div role="status">{children}</div>,
}));

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  FormUtils: {
    fieldProps: jest.fn((name, state) => ({
      value: name.split('.').reduce((obj, key) => obj?.[key], state.context.data) ?? '',
      onChange: jest.fn(),
    })),
  },
}));

const { ENCRYPTION } = UIStrings.REPOSITORIES.EDITOR.TERRAFORM_STATE_BACKEND;

function makeParentMachine(overrides = {}) {
  const defaultData = {
    terraformStateBackend: {
      encryption: { encryptionKey: '' },
      lockTimeoutMinutes: 30,
      maxStateSizeMB: 256,
    },
    ...overrides,
  };

  const state = { context: { data: defaultData } };
  const sendParent = jest.fn();
  return [[state, sendParent], sendParent];
}

function getInputByLabel(label) {
  return within(screen.getByRole('group', { name: label })).getByRole('textbox');
}

describe('TerraformStateBackendEncryptionConfiguration', () => {
  it('renders the encryption caption', () => {
    const [parentMachine] = makeParentMachine();
    render(<TerraformStateBackendEncryptionConfiguration parentMachine={parentMachine} />);
    expect(screen.getByText(ENCRYPTION.CAPTION)).toBeInTheDocument();
  });

  it('renders the mandatory encryption info alert', () => {
    const [parentMachine] = makeParentMachine();
    render(<TerraformStateBackendEncryptionConfiguration parentMachine={parentMachine} />);
    expect(screen.getByText(ENCRYPTION.ENCRYPTION_REQUIRED_INFO)).toBeInTheDocument();
  });

  it('always shows encryption key and confirm key fields', () => {
    const [parentMachine] = makeParentMachine();
    render(<TerraformStateBackendEncryptionConfiguration parentMachine={parentMachine} />);
    expect(screen.getByRole('group', { name: ENCRYPTION.ENCRYPTION_KEY.LABEL })).toBeInTheDocument();
    expect(screen.getByRole('group', { name: ENCRYPTION.ENCRYPTION_KEY_CONFIRM.LABEL })).toBeInTheDocument();
  });

  it('renders lock timeout and max state size fields regardless of encryption', () => {
    const [parentMachine] = makeParentMachine();
    render(<TerraformStateBackendEncryptionConfiguration parentMachine={parentMachine} />);
    expect(screen.getByRole('group', { name: ENCRYPTION.LOCK_TIMEOUT_MINUTES.LABEL })).toBeInTheDocument();
    expect(screen.getByRole('group', { name: ENCRYPTION.MAX_STATE_SIZE_MB.LABEL })).toBeInTheDocument();
  });

  it('dispatches UPDATE event when encryption key is changed', () => {
    const [parentMachine, sendParent] = makeParentMachine();
    render(<TerraformStateBackendEncryptionConfiguration parentMachine={parentMachine} />);

    fireEvent.change(getInputByLabel(ENCRYPTION.ENCRYPTION_KEY.LABEL), { target: { value: 'newkey' } });

    expect(sendParent).toHaveBeenCalledWith({
      type: 'UPDATE',
      name: 'terraformStateBackend.encryption.encryptionKey',
      value: 'newkey',
    });
  });

  it('clears confirm key when primary encryption key changes', async () => {
    const [parentMachine] = makeParentMachine();
    render(<TerraformStateBackendEncryptionConfiguration parentMachine={parentMachine} />);

    await act(async () => {
      fireEvent.change(getInputByLabel(ENCRYPTION.ENCRYPTION_KEY_CONFIRM.LABEL), { target: { value: 'somevalue' } });
    });
    expect(getInputByLabel(ENCRYPTION.ENCRYPTION_KEY_CONFIRM.LABEL)).toHaveValue('somevalue');

    await act(async () => {
      fireEvent.change(getInputByLabel(ENCRYPTION.ENCRYPTION_KEY.LABEL), { target: { value: 'newkey' } });
    });
    expect(getInputByLabel(ENCRYPTION.ENCRYPTION_KEY_CONFIRM.LABEL)).toHaveValue('');
  });

  it('dispatches UPDATE event with parsed integer when lock timeout is changed', () => {
    const [parentMachine, sendParent] = makeParentMachine();
    render(<TerraformStateBackendEncryptionConfiguration parentMachine={parentMachine} />);

    // The handler coerces the string input value to a number before dispatching
    fireEvent.change(getInputByLabel(ENCRYPTION.LOCK_TIMEOUT_MINUTES.LABEL), { target: { value: '60' } });

    expect(sendParent).toHaveBeenCalledWith({
      type: 'UPDATE',
      name: 'terraformStateBackend.lockTimeoutMinutes',
      value: 60,
    });
  });

  it('dispatches UPDATE event with parsed integer when max state size is changed', () => {
    const [parentMachine, sendParent] = makeParentMachine();
    render(<TerraformStateBackendEncryptionConfiguration parentMachine={parentMachine} />);

    // The handler coerces the string input value to a number before dispatching
    fireEvent.change(getInputByLabel(ENCRYPTION.MAX_STATE_SIZE_MB.LABEL), { target: { value: '100' } });

    expect(sendParent).toHaveBeenCalledWith({
      type: 'UPDATE',
      name: 'terraformStateBackend.maxStateSizeMB',
      value: 100,
    });
  });

  it('shows error when lock timeout is empty', () => {
    const [parentMachine] = makeParentMachine();
    render(<TerraformStateBackendEncryptionConfiguration parentMachine={parentMachine} />);

    fireEvent.change(getInputByLabel(ENCRYPTION.LOCK_TIMEOUT_MINUTES.LABEL), { target: { value: '' } });

    expect(screen.getByText('Please enter a valid number')).toBeInTheDocument();
  });

  it('shows error when lock timeout is out of range', () => {
    const [parentMachine] = makeParentMachine();
    render(<TerraformStateBackendEncryptionConfiguration parentMachine={parentMachine} />);

    fireEvent.change(getInputByLabel(ENCRYPTION.LOCK_TIMEOUT_MINUTES.LABEL), { target: { value: '0' } });

    expect(screen.getByText('Value must be between 1 and 1440')).toBeInTheDocument();
  });

  it('shows error when max state size is empty', () => {
    const [parentMachine] = makeParentMachine();
    render(<TerraformStateBackendEncryptionConfiguration parentMachine={parentMachine} />);

    fireEvent.change(getInputByLabel(ENCRYPTION.MAX_STATE_SIZE_MB.LABEL), { target: { value: '' } });

    expect(screen.getByText('Please enter a valid number')).toBeInTheDocument();
  });

  it('shows error when max state size is out of range', () => {
    const [parentMachine] = makeParentMachine();
    render(<TerraformStateBackendEncryptionConfiguration parentMachine={parentMachine} />);

    fireEvent.change(getInputByLabel(ENCRYPTION.MAX_STATE_SIZE_MB.LABEL), { target: { value: '513' } });

    expect(screen.getByText('Value must be between 1 and 512')).toBeInTheDocument();
  });

  // These two tests use await act(async () => {...}) because the mismatch check runs inside a
  // useEffect — effects are scheduled after render and need async act to flush before asserting.
  it('shows key mismatch error when keys differ', async () => {
    const [parentMachine] = makeParentMachine({
      terraformStateBackend: {
        encryption: { encryptionKey: 'key1' },
        lockTimeoutMinutes: 30,
        maxStateSizeMB: 256,
      },
    });
    render(<TerraformStateBackendEncryptionConfiguration parentMachine={parentMachine} />);

    await act(async () => {
      fireEvent.change(getInputByLabel(ENCRYPTION.ENCRYPTION_KEY_CONFIRM.LABEL), { target: { value: 'key2' } });
    });

    expect(screen.getByText(ENCRYPTION.ENCRYPTION_KEY_CONFIRM.MISMATCH_ERROR)).toBeInTheDocument();
  });

  it('does not dispatch UPDATE event when confirm key field changes', () => {
    const [parentMachine, sendParent] = makeParentMachine();
    render(<TerraformStateBackendEncryptionConfiguration parentMachine={parentMachine} />);

    fireEvent.change(getInputByLabel(ENCRYPTION.ENCRYPTION_KEY_CONFIRM.LABEL), { target: { value: 'anyvalue' } });

    expect(sendParent).not.toHaveBeenCalled();
  });

  it('shows error when lock timeout is non-numeric', () => {
    const [parentMachine] = makeParentMachine();
    render(<TerraformStateBackendEncryptionConfiguration parentMachine={parentMachine} />);

    fireEvent.change(getInputByLabel(ENCRYPTION.LOCK_TIMEOUT_MINUTES.LABEL), { target: { value: 'abc' } });

    expect(screen.getByText('Please enter a valid number')).toBeInTheDocument();
  });

  it('shows error when max state size is non-numeric', () => {
    const [parentMachine] = makeParentMachine();
    render(<TerraformStateBackendEncryptionConfiguration parentMachine={parentMachine} />);

    fireEvent.change(getInputByLabel(ENCRYPTION.MAX_STATE_SIZE_MB.LABEL), { target: { value: 'abc' } });

    expect(screen.getByText('Please enter a valid number')).toBeInTheDocument();
  });

  it('accepts lock timeout boundary values 1 and 1440 without error', () => {
    const [parentMachine, sendParent] = makeParentMachine();
    render(<TerraformStateBackendEncryptionConfiguration parentMachine={parentMachine} />);

    fireEvent.change(getInputByLabel(ENCRYPTION.LOCK_TIMEOUT_MINUTES.LABEL), { target: { value: '1' } });
    expect(sendParent).toHaveBeenCalledWith({
      type: 'UPDATE',
      name: 'terraformStateBackend.lockTimeoutMinutes',
      value: 1,
    });

    fireEvent.change(getInputByLabel(ENCRYPTION.LOCK_TIMEOUT_MINUTES.LABEL), { target: { value: '1440' } });
    expect(sendParent).toHaveBeenCalledWith({
      type: 'UPDATE',
      name: 'terraformStateBackend.lockTimeoutMinutes',
      value: 1440,
    });

    expect(screen.queryByText('Value must be between 1 and 1440')).not.toBeInTheDocument();
  });

  it('accepts max state size boundary values 1 and 512 without error', () => {
    const [parentMachine, sendParent] = makeParentMachine();
    render(<TerraformStateBackendEncryptionConfiguration parentMachine={parentMachine} />);

    fireEvent.change(getInputByLabel(ENCRYPTION.MAX_STATE_SIZE_MB.LABEL), { target: { value: '1' } });
    expect(sendParent).toHaveBeenCalledWith({
      type: 'UPDATE',
      name: 'terraformStateBackend.maxStateSizeMB',
      value: 1,
    });

    fireEvent.change(getInputByLabel(ENCRYPTION.MAX_STATE_SIZE_MB.LABEL), { target: { value: '512' } });
    expect(sendParent).toHaveBeenCalledWith({
      type: 'UPDATE',
      name: 'terraformStateBackend.maxStateSizeMB',
      value: 512,
    });

    expect(screen.queryByText('Value must be between 1 and 512')).not.toBeInTheDocument();
  });

  // These two tests use await act(async () => {...}) because clearing an error calls setState(null),
  // which schedules a re-render that needs async act to flush before asserting absence.
  it('clears lock timeout error once a valid value is entered', async () => {
    const [parentMachine] = makeParentMachine();
    render(<TerraformStateBackendEncryptionConfiguration parentMachine={parentMachine} />);

    fireEvent.change(getInputByLabel(ENCRYPTION.LOCK_TIMEOUT_MINUTES.LABEL), { target: { value: '' } });
    expect(screen.getByText('Please enter a valid number')).toBeInTheDocument();

    await act(async () => {
      fireEvent.change(getInputByLabel(ENCRYPTION.LOCK_TIMEOUT_MINUTES.LABEL), { target: { value: '60' } });
    });
    expect(screen.queryByText('Please enter a valid number')).not.toBeInTheDocument();
  });

  it('clears max state size error once a valid value is entered', async () => {
    const [parentMachine] = makeParentMachine();
    render(<TerraformStateBackendEncryptionConfiguration parentMachine={parentMachine} />);

    fireEvent.change(getInputByLabel(ENCRYPTION.MAX_STATE_SIZE_MB.LABEL), { target: { value: '' } });
    expect(screen.getByText('Please enter a valid number')).toBeInTheDocument();

    await act(async () => {
      fireEvent.change(getInputByLabel(ENCRYPTION.MAX_STATE_SIZE_MB.LABEL), { target: { value: '100' } });
    });
    expect(screen.queryByText('Please enter a valid number')).not.toBeInTheDocument();
  });

  it('does not show mismatch error when confirm key is empty even if primary key is set', () => {
    const [parentMachine] = makeParentMachine({
      terraformStateBackend: {
        encryption: { encryptionKey: 'somekey' },
        lockTimeoutMinutes: 30,
        maxStateSizeMB: 256,
      },
    });
    render(<TerraformStateBackendEncryptionConfiguration parentMachine={parentMachine} />);

    // confirm field starts empty — the useEffect guard (both must be non-empty) should suppress the error
    expect(screen.queryByText(ENCRYPTION.ENCRYPTION_KEY_CONFIRM.MISMATCH_ERROR)).not.toBeInTheDocument();
  });

  it('does not show key mismatch error when keys match', async () => {
    const [parentMachine] = makeParentMachine({
      terraformStateBackend: {
        encryption: { encryptionKey: 'same-key' },
        lockTimeoutMinutes: 30,
        maxStateSizeMB: 256,
      },
    });
    render(<TerraformStateBackendEncryptionConfiguration parentMachine={parentMachine} />);

    await act(async () => {
      fireEvent.change(getInputByLabel(ENCRYPTION.ENCRYPTION_KEY_CONFIRM.LABEL), { target: { value: 'same-key' } });
    });

    expect(screen.queryByText(ENCRYPTION.ENCRYPTION_KEY_CONFIRM.MISMATCH_ERROR)).not.toBeInTheDocument();
  });
});
