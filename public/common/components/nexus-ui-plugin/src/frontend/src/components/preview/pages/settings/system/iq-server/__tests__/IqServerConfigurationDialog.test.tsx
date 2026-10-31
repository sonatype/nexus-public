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
import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

jest.mock('../../../../../shared/hooks/useIsCloud', () => ({
  useIsCloud: jest.fn(() => false),
}));

jest.mock('../iqServerFormMachine', () => {
  const actual = jest.requireActual('../iqServerFormMachine');
  return {
    ...actual,
    fetchIqSettings: jest.fn(),
    putIqSettings: jest.fn(),
    disableIqServer: jest.fn(),
  };
});
jest.mock('../iqConnectionMachine', () => {
  const actual = jest.requireActual('../iqConnectionMachine');
  return {
    ...actual,
    verifyConnection: jest.fn(),
  };
});

import { IqServerConfigurationDialog } from '../IqServerConfigurationDialog';
import { PASSWORD_PLACEHOLDER } from '../types';
import * as formMachine from '../iqServerFormMachine';
import * as connectionMachine from '../iqConnectionMachine';

const { toFormData } = jest.requireActual('../iqServerFormMachine');

const fetchIqSettings = formMachine.fetchIqSettings as jest.Mock;
const putIqSettings = formMachine.putIqSettings as jest.Mock;
const verifyConnection = connectionMachine.verifyConnection as jest.Mock;

/**
 * GET /v1/iq for an instance that has been configured: the stored password is never returned in
 * clear text, the backend substitutes PasswordPlaceholder.VALUE. `enabled` mirrors
 * ClmConnector.isActive(), so a disabled instance still reports url, username and the placeholder.
 */
function storedConfig(overrides: Record<string, any> = {}) {
  return toFormData({
    enabled: false,
    url: 'http://localhost:8070',
    authenticationType: 'USER',
    username: 'admin',
    password: PASSWORD_PLACEHOLDER,
    useTrustStoreForUrl: false,
    timeoutSeconds: null,
    properties: '',
    showLink: true,
    failOpenModeEnabled: false,
    ...overrides,
  });
}

/** GET /v1/iq before IQ Server has ever been configured. */
function firstTimeConfig() {
  return toFormData({
    enabled: false,
    url: '',
    authenticationType: 'USER',
    username: '',
    password: '',
    useTrustStoreForUrl: false,
    timeoutSeconds: null,
    properties: '',
    showLink: false,
  });
}

function renderDialog(props: Record<string, any> = {}) {
  return render(
    <Theme>
      <IqServerConfigurationDialog
        open
        onOpenChange={jest.fn()}
        onSaved={jest.fn()}
        onDisconnected={jest.fn()}
        {...props}
      />
    </Theme>
  );
}

const urlField = () => screen.getByLabelText(/IQ Server URL/) as HTMLInputElement;
const passwordField = () => screen.getByLabelText(/^Password/) as HTMLInputElement;
const testButton = () => screen.getByTestId('iq-dlg-test');
const saveButton = () => screen.getByTestId('iq-dlg-save');

/** Waits for the initial fetch to resolve and the form to paint. */
async function awaitLoaded() {
  await waitFor(() => expect(screen.queryByText('Loading configuration...')).not.toBeInTheDocument());
}

beforeEach(() => {
  jest.clearAllMocks();
  verifyConnection.mockResolvedValue({ success: true });
  putIqSettings.mockImplementation(async (config: any) => config);
});

afterEach(() => {
  cleanup();
  Array.from(document.body.children).forEach(el => el.remove());
});

const storedPasswordDisplay = () => screen.getByTestId('iq-dlg-pw-stored') as HTMLInputElement;
const changePasswordButton = () => screen.getByTestId('iq-dlg-pw-change');
/** Every control that could flip a password field to clear text. */
const visibilityToggles = () => screen.queryAllByRole('button', { name: /(show|hide) password/i });

/**
 * The sentinel is a wire value. It may sit in form state and in request payloads, but it must never
 * reach the DOM: not as text, not as an input value, not as an accessible name or description.
 */
function expectSentinelNotRendered() {
  expect(document.body.textContent).not.toContain(PASSWORD_PLACEHOLDER);
  document.querySelectorAll('input, textarea').forEach(el => {
    expect((el as HTMLInputElement).value).not.toBe(PASSWORD_PLACEHOLDER);
  });
  document.querySelectorAll('[aria-label], [placeholder], [title]').forEach(el => {
    expect(el.getAttribute('aria-label') ?? '').not.toContain(PASSWORD_PLACEHOLDER);
    expect(el.getAttribute('placeholder') ?? '').not.toContain(PASSWORD_PLACEHOLDER);
    expect(el.getAttribute('title') ?? '').not.toContain(PASSWORD_PLACEHOLDER);
  });
}

describe('IqServerConfigurationDialog password handling', () => {
  describe('stored password, untouched (connect -> disable -> reconnect)', () => {
    beforeEach(() => {
      fetchIqSettings.mockResolvedValue(storedConfig());
    });

    it('renders the stored credential as masked, read-only, and without the sentinel', async () => {
      renderDialog();
      await awaitLoaded();

      // User-observable state: a masked, non-empty field the user is told already holds a password.
      const field = storedPasswordDisplay();
      expect(field).toHaveAttribute('type', 'password');
      expect(field).toHaveAttribute('readonly');
      expect(field.value).not.toBe('');
      expect(
        screen.getByText(/A password is already saved for this IQ Server/)
      ).toBeInTheDocument();

      // Internal state is unaffected: nothing rendered exposes the protocol value.
      expectSentinelNotRendered();
    });

    it('offers no visibility control while the stored credential is retained', async () => {
      renderDialog();
      await awaitLoaded();

      // There is nothing to reveal — the browser never received a password — so the eye is withheld
      // and no interaction can put the sentinel on screen.
      expect(visibilityToggles()).toHaveLength(0);
      screen.getAllByRole('button').forEach(button => fireEvent.click(button));
      expectSentinelNotRendered();
    });

    it('lets Test Connection reuse the persisted credential and succeed', async () => {
      renderDialog();
      await awaitLoaded();

      expect(testButton()).not.toBeDisabled();
      fireEvent.click(testButton());

      await waitFor(() => expect(verifyConnection).toHaveBeenCalledTimes(1));
      // The request keeps the sentinel: IqUiResource.getPassword() resolves it to the stored secret.
      expect(verifyConnection).toHaveBeenCalledWith(
        expect.objectContaining({
          enabled: true,
          url: 'http://localhost:8070',
          authenticationType: 'USER',
          username: 'admin',
          password: PASSWORD_PLACEHOLDER,
        })
      );
      expect(await screen.findByText('Connection successful')).toBeInTheDocument();
      expectSentinelNotRendered();
    });

    it('lets Save reconnect without editing any field', async () => {
      renderDialog();
      await awaitLoaded();

      expect(saveButton()).not.toBeDisabled();
      fireEvent.click(saveButton());

      await waitFor(() => expect(putIqSettings).toHaveBeenCalledTimes(1));
      expect(putIqSettings).toHaveBeenCalledWith(
        expect.objectContaining({ enabled: true, password: PASSWORD_PLACEHOLDER })
      );
      expectSentinelNotRendered();
    });
  });

  describe('explicitly clearing the stored password', () => {
    beforeEach(() => {
      fetchIqSettings.mockResolvedValue(storedConfig());
    });

    it('empties the field and does not fall back to the stored credential', async () => {
      renderDialog();
      await awaitLoaded();

      fireEvent.click(changePasswordButton());

      // Distinct from "stored, untouched": the field is now editable and genuinely empty, and both
      // actions are unavailable rather than silently reusing what was persisted.
      expect(screen.queryByTestId('iq-dlg-pw-stored')).not.toBeInTheDocument();
      expect(passwordField().value).toBe('');
      expect(passwordField()).not.toHaveAttribute('readonly');
      expect(visibilityToggles()).toHaveLength(0);
      expect(testButton()).toBeDisabled();
      expect(saveButton()).toBeDisabled();

      fireEvent.click(testButton());
      fireEvent.click(saveButton());
      expect(verifyConnection).not.toHaveBeenCalled();
      expect(putIqSettings).not.toHaveBeenCalled();
      expectSentinelNotRendered();
    });

    it('sends only the replacement password once one is supplied', async () => {
      renderDialog();
      await awaitLoaded();

      fireEvent.click(changePasswordButton());
      fireEvent.change(passwordField(), { target: { value: 'replacement' } });

      fireEvent.click(testButton());
      await waitFor(() => expect(verifyConnection).toHaveBeenCalledTimes(1));
      expect(verifyConnection).toHaveBeenCalledWith(
        expect.objectContaining({ password: 'replacement' })
      );
      expect(verifyConnection).not.toHaveBeenCalledWith(
        expect.objectContaining({ password: PASSWORD_PLACEHOLDER })
      );

      fireEvent.click(saveButton());
      await waitFor(() => expect(putIqSettings).toHaveBeenCalledTimes(1));
      expect(putIqSettings).toHaveBeenCalledWith(
        expect.objectContaining({ password: 'replacement' })
      );
    });
  });

  describe('entering a new password', () => {
    it('behaves like a normal password field and is sent to both Test and Save', async () => {
      fetchIqSettings.mockResolvedValue(storedConfig({ enabled: true }));
      renderDialog({ initiallyConnected: true });
      await awaitLoaded();

      fireEvent.click(changePasswordButton());
      fireEvent.change(passwordField(), { target: { value: 'rotated-secret' } });
      expect(passwordField().value).toBe('rotated-secret');

      // Show/hide works normally on a value the user typed, and reveals exactly that value.
      expect(visibilityToggles()).toHaveLength(1);
      fireEvent.click(visibilityToggles()[0]);
      expect(passwordField()).toHaveAttribute('type', 'text');
      expect(passwordField().value).toBe('rotated-secret');
      expectSentinelNotRendered();

      // Consistent semantics: Test and Save carry the same password.
      fireEvent.click(testButton());
      await waitFor(() => expect(verifyConnection).toHaveBeenCalledTimes(1));
      fireEvent.click(saveButton());
      await waitFor(() => expect(putIqSettings).toHaveBeenCalledTimes(1));
      expect(verifyConnection.mock.calls[0][0].password).toBe('rotated-secret');
      expect(putIqSettings.mock.calls[0][0].password).toBe('rotated-secret');
    });
  });

  describe('editing the URL', () => {
    it('invalidates the stored-password state and requires a password for the new server', async () => {
      fetchIqSettings.mockResolvedValue(storedConfig());
      renderDialog();
      await awaitLoaded();

      fireEvent.change(urlField(), { target: { value: 'http://other-iq:8070' } });

      // The stored credential belongs to the old URL, so it is discarded rather than sent onward.
      expect(screen.queryByTestId('iq-dlg-pw-stored')).not.toBeInTheDocument();
      expect(passwordField().value).toBe('');
      expect(testButton()).toBeDisabled();
      expect(saveButton()).toBeDisabled();

      fireEvent.change(passwordField(), { target: { value: 'new-secret' } });
      expect(testButton()).not.toBeDisabled();
      fireEvent.click(testButton());

      await waitFor(() => expect(verifyConnection).toHaveBeenCalledTimes(1));
      expect(verifyConnection).toHaveBeenCalledWith(
        expect.objectContaining({ url: 'http://other-iq:8070', password: 'new-secret' })
      );
      expect(verifyConnection).not.toHaveBeenCalledWith(
        expect.objectContaining({ password: PASSWORD_PLACEHOLDER })
      );
    });

    it('leaves a typed password alone when the URL changes afterwards', async () => {
      fetchIqSettings.mockResolvedValue(storedConfig());
      renderDialog();
      await awaitLoaded();

      fireEvent.click(changePasswordButton());
      fireEvent.change(passwordField(), { target: { value: 'typed-secret' } });
      fireEvent.change(urlField(), { target: { value: 'http://other-iq:8070' } });

      expect(passwordField().value).toBe('typed-secret');
    });
  });

  describe('first-time setup', () => {
    beforeEach(() => {
      fetchIqSettings.mockResolvedValue(firstTimeConfig());
    });

    it('starts empty and never sends the placeholder', async () => {
      renderDialog();
      await awaitLoaded();

      expect(screen.queryByTestId('iq-dlg-pw-stored')).not.toBeInTheDocument();
      expect(passwordField().value).toBe('');
      expect(passwordField()).toBeRequired();
      expect(testButton()).toBeDisabled();
      // Nothing is stored, so there is nothing to reconnect to: Save stays gated on an edit.
      expect(saveButton()).toBeDisabled();
      expectSentinelNotRendered();

      fireEvent.change(urlField(), { target: { value: 'http://localhost:8070' } });
      fireEvent.change(screen.getByLabelText(/Username/), { target: { value: 'admin' } });
      fireEvent.change(passwordField(), { target: { value: 'first-secret' } });

      fireEvent.click(testButton());
      await waitFor(() => expect(verifyConnection).toHaveBeenCalledTimes(1));
      fireEvent.click(saveButton());
      await waitFor(() => expect(putIqSettings).toHaveBeenCalledTimes(1));

      expect(verifyConnection.mock.calls[0][0]).toEqual(
        expect.objectContaining({
          url: 'http://localhost:8070',
          authenticationType: 'USER',
          username: 'admin',
          password: 'first-secret',
        })
      );
      expect(putIqSettings.mock.calls[0][0].password).toBe('first-secret');
      expect(verifyConnection).not.toHaveBeenCalledWith(
        expect.objectContaining({ password: PASSWORD_PLACEHOLDER })
      );
      expect(putIqSettings).not.toHaveBeenCalledWith(
        expect.objectContaining({ password: PASSWORD_PLACEHOLDER })
      );
    });
  });

  describe('PKI authentication', () => {
    it('asks for no password and sends none', async () => {
      fetchIqSettings.mockResolvedValue(
        storedConfig({ authenticationType: 'PKI', username: '', password: '' })
      );
      renderDialog();
      await awaitLoaded();

      expect(screen.queryByLabelText(/^Password/)).not.toBeInTheDocument();
      expect(screen.queryByTestId('iq-dlg-pw-stored')).not.toBeInTheDocument();
      expect(testButton()).not.toBeDisabled();

      fireEvent.click(testButton());
      await waitFor(() => expect(verifyConnection).toHaveBeenCalledTimes(1));
      expect(verifyConnection).toHaveBeenCalledWith(
        expect.objectContaining({ authenticationType: 'PKI', username: '', password: '' })
      );

      fireEvent.click(saveButton());
      await waitFor(() => expect(putIqSettings).toHaveBeenCalledTimes(1));
      expect(putIqSettings).toHaveBeenCalledWith(
        expect.objectContaining({ authenticationType: 'PKI', password: '' })
      );
    });

    // Switching USER -> PKI in the dialog also clears username and password (the Select's
    // onValueChange), but the control is a Radix Select rendered in a portal and driving it in jsdom
    // is unreliable; the resulting PKI payload is what matters and is asserted above.
  });
});
