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
import '@testing-library/jest-dom';

// Replace the Radix-based form components with plain DOM equivalents so the
// modal's behavior (validation, never-expires warning, submit gating) can be
// driven via fireEvent. The form components have their own tests; this suite
// focuses on CreateTokenModal logic.
jest.mock('../../../../../shared/form', () => ({
  SettingsTextInput: ({ name, label, value, onChange, error, helpText }: any) => (
    <label>
      {label}
      <input
        data-testid={`input-${name}`}
        value={value}
        onChange={(e) => onChange(e.target.value)}
      />
      {helpText && <span>{helpText}</span>}
      {error && <span>{error}</span>}
    </label>
  ),
  SettingsSelect: ({ name, label, value, onChange, options }: any) => (
    <label>
      {label}
      <select
        data-testid={`select-${name}`}
        value={value}
        onChange={(e) => onChange(e.target.value)}
      >
        {options.map((o: any) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
    </label>
  ),
  SettingsTextArea: ({ name, label, value, onChange, maxLength }: any) => (
    <label>
      {label}
      <textarea
        data-testid={`textarea-${name}`}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        maxLength={maxLength}
      />
    </label>
  ),
  SettingsButton: ({ children, onClick, disabled, ...rest }: any) => (
    <button onClick={onClick} disabled={disabled} {...rest}>
      {children}
    </button>
  ),
}));

import { CreateTokenModal } from '../CreateTokenModal';
import { SERVICE_ACCOUNT_TOKENS_STRINGS } from '../strings';

const LABELS = SERVICE_ACCOUNT_TOKENS_STRINGS.CREATE_MODAL;

describe('CreateTokenModal', () => {
  const defaultProps = {
    open: true,
    onClose: jest.fn(),
    onCreate: jest.fn(),
    roles: [
      { id: 'nx-admin', name: 'Administrator' },
      { id: 'nx-deploy', name: 'Deployer' },
    ],
    loading: false,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('rendering', () => {
    it('renders when open', () => {
      render(<CreateTokenModal {...defaultProps} />);
      expect(screen.getByTestId('sat-create-modal')).toBeInTheDocument();
    });

    it('does not render when closed', () => {
      render(<CreateTokenModal {...defaultProps} open={false} />);
      expect(screen.queryByTestId('sat-create-modal')).not.toBeInTheDocument();
    });

    it('renders the expiration select', () => {
      render(<CreateTokenModal {...defaultProps} />);

      const expirationSelect = screen.getByTestId('select-expirationDays');
      expect(expirationSelect).toBeInTheDocument();
    });
  });

  describe('never-expires warning', () => {
    it('does not show warning when expiration is not "never"', () => {
      render(<CreateTokenModal {...defaultProps} />);

      // Default should be "30 days", not "never"
      expect(screen.queryByText(LABELS.NEVER_EXPIRES_WARNING)).not.toBeInTheDocument();
    });

    it('moves focus to the warning when expiration is set to "Never" so screen readers announce it', async () => {
      render(<CreateTokenModal {...defaultProps} />);

      fireEvent.change(screen.getByTestId('select-expirationDays'), {
        target: { value: '-1' },
      });

      const warning = screen.getByText(LABELS.NEVER_EXPIRES_WARNING);
      const wrapper = warning.closest('[tabindex="-1"]');
      expect(wrapper).toBeInTheDocument();
      // Focus is queued via setTimeout to land after Radix Select's own
      // focus-return-to-trigger, so wait for it.
      await waitFor(() => expect(wrapper).toHaveFocus());
    });

    it('hides the warning again when a finite expiration is reselected', () => {
      render(<CreateTokenModal {...defaultProps} />);

      const select = screen.getByTestId('select-expirationDays');
      fireEvent.change(select, { target: { value: '-1' } });
      expect(screen.getByText(LABELS.NEVER_EXPIRES_WARNING)).toBeInTheDocument();

      fireEvent.change(select, { target: { value: '30' } });
      expect(screen.queryByText(LABELS.NEVER_EXPIRES_WARNING)).not.toBeInTheDocument();
    });
  });

  describe('existing-name validation', () => {
    it('shows duplicate-name error when name matches an existing token', () => {
      render(<CreateTokenModal {...defaultProps} existingNames={['jenkins-prod']} />);

      const nameInput = screen.getByTestId('input-name');
      fireEvent.change(nameInput, { target: { value: 'jenkins-prod' } });

      expect(screen.getByText(LABELS.NAME_DUPLICATE_ERROR('jenkins-prod'))).toBeInTheDocument();
      expect(screen.getByTestId('sat-create-submit')).toBeDisabled();
    });

    it('keeps the name help text visible alongside the duplicate-name error', () => {
      render(<CreateTokenModal {...defaultProps} existingNames={['jenkins-prod']} />);

      const nameInput = screen.getByTestId('input-name');
      fireEvent.change(nameInput, { target: { value: 'jenkins-prod' } });

      expect(screen.getByText(LABELS.NAME_HELP)).toBeInTheDocument();
      expect(screen.getByText(LABELS.NAME_DUPLICATE_ERROR('jenkins-prod'))).toBeInTheDocument();
    });

    it('does not flag the name when no existing names match', () => {
      render(<CreateTokenModal {...defaultProps} existingNames={['other-token']} />);

      const nameInput = screen.getByTestId('input-name');
      fireEvent.change(nameInput, { target: { value: 'jenkins-prod' } });

      expect(
        screen.queryByText(LABELS.NAME_DUPLICATE_ERROR('jenkins-prod'))
      ).not.toBeInTheDocument();
    });
  });

  describe('name pattern validation', () => {
    it('shows pattern error when name contains spaces or special chars', () => {
      render(<CreateTokenModal {...defaultProps} />);

      fireEvent.change(screen.getByTestId('input-name'), { target: { value: 'a cool token' } });

      expect(screen.getByText(LABELS.NAME_INVALID_CHARS_ERROR)).toBeInTheDocument();
      expect(screen.getByTestId('sat-create-submit')).toBeDisabled();
    });

    it('accepts names with letters, numbers, hyphens, and underscores', () => {
      render(<CreateTokenModal {...defaultProps} />);

      fireEvent.change(screen.getByTestId('input-name'), { target: { value: 'my-token_v2' } });

      expect(screen.queryByText(LABELS.NAME_INVALID_CHARS_ERROR)).not.toBeInTheDocument();
    });
  });

  describe('description', () => {
    it('passes maxLength=256 to the textarea', () => {
      render(<CreateTokenModal {...defaultProps} />);

      const textarea = screen.getByTestId('textarea-description');
      expect(textarea).toHaveAttribute('maxLength', '256');
    });

    it('renders the live character counter', () => {
      render(<CreateTokenModal {...defaultProps} />);

      expect(screen.getByTestId('sat-description-counter')).toHaveTextContent('0 / 256');

      fireEvent.change(screen.getByTestId('textarea-description'), { target: { value: 'hello' } });
      expect(screen.getByTestId('sat-description-counter')).toHaveTextContent('5 / 256');
    });
  });

  describe('accessibility', () => {
    it('renders as a dialog element', () => {
      render(<CreateTokenModal {...defaultProps} />);

      // Radix Dialog assigns role=dialog and aria-modal to its own internal
      // element; we just assert the dialog role is present.
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });
  });

  describe('rolesError', () => {
    it('renders the forbidden error and disables the submit button when rolesError is set', () => {
      render(
        <CreateTokenModal
          {...defaultProps}
          roles={[]}
          rolesError={LABELS.ROLES_LOAD_ERROR_FORBIDDEN}
        />
      );

      const errorBox = screen.getByTestId('sat-create-roles-error');
      expect(errorBox).toHaveTextContent(
        'Missing privilege: nexus:roles:read. Contact your administrator.'
      );

      fireEvent.change(screen.getByTestId('input-name'), { target: { value: 'svc' } });
      expect(screen.getByTestId('sat-create-submit')).toBeDisabled();
    });
  });
});
