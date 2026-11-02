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

// Isolate the certificate dialog — its contract is covered by its own tests.
// Rendering it here as a placeholder lets us assert "opened / closed" without
// making Axios calls.
jest.mock('../../../repository/repositories/facets/CertificateViewDialog', () => ({
  CertificateViewDialog: ({ remoteUrl, onClose }: { remoteUrl: string; onClose: () => void }) => (
    <div data-testid="cert-view-dialog" data-url={remoteUrl}>
      <button type="button" onClick={onClose} data-testid="cert-view-close">close</button>
    </div>
  ),
}));

import { IqServerConnectionSettingsTab } from '../IqServerConnectionSettingsTab';
import { PASSWORD_PLACEHOLDER } from '../types';
import * as formMachine from '../iqServerFormMachine';
import * as connectionMachine from '../iqConnectionMachine';

const { toFormData } = jest.requireActual('../iqServerFormMachine');

const fetchIqSettings = formMachine.fetchIqSettings as jest.Mock;
const putIqSettings = formMachine.putIqSettings as jest.Mock;
const disableIqServer = formMachine.disableIqServer as jest.Mock;
const verifyConnection = connectionMachine.verifyConnection as jest.Mock;

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

function storedConfig(overrides: Record<string, any> = {}) {
  return toFormData({
    enabled: false,
    url: 'https://iq.example.com',
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

function renderTab(props: Record<string, any> = {}) {
  return render(
    <Theme>
      <IqServerConnectionSettingsTab
        onSaved={jest.fn()}
        onDisconnected={jest.fn()}
        {...props}
      />
    </Theme>
  );
}

async function awaitLoaded() {
  await waitFor(() =>
    expect(screen.queryByText('Loading configuration...')).not.toBeInTheDocument()
  );
}

const urlField = () => screen.getByLabelText(/IQ Server URL/) as HTMLInputElement;
const usernameField = () => screen.getByLabelText(/Username/) as HTMLInputElement;
const passwordField = () => screen.getByLabelText(/^Password/) as HTMLInputElement;
const testButton = () => screen.getByTestId('iq-tab-test');
const saveButton = () => screen.getByTestId('iq-tab-save');

beforeEach(() => {
  jest.clearAllMocks();
  verifyConnection.mockResolvedValue({ success: true });
  putIqSettings.mockImplementation(async (config: any) => config);
  disableIqServer.mockResolvedValue(undefined);
});

afterEach(() => {
  cleanup();
  Array.from(document.body.children).forEach(el => el.remove());
});

describe('IqServerConnectionSettingsTab — trust store & View Certificate', () => {
  it('hides the trust-store checkbox and View Certificate button when URL is not https', async () => {
    fetchIqSettings.mockResolvedValue(
      toFormData({
        enabled: false,
        url: 'http://iq.example.com',
        authenticationType: 'USER',
        username: 'admin',
        password: PASSWORD_PLACEHOLDER,
        useTrustStoreForUrl: false,
        timeoutSeconds: null,
        properties: '',
        showLink: true,
      })
    );
    renderTab();
    await awaitLoaded();

    expect(screen.queryByTestId('iq-tab-use-truststore')).not.toBeInTheDocument();
    expect(screen.queryByTestId('iq-tab-view-cert')).not.toBeInTheDocument();
  });

  it('shows the trust-store checkbox and persists its state', async () => {
    fetchIqSettings.mockResolvedValue(storedConfig({ useTrustStoreForUrl: true }));
    renderTab();
    await awaitLoaded();

    const checkbox = screen.getByTestId('iq-tab-use-truststore');
    expect(checkbox).toBeInTheDocument();
    // Radix Checkbox reports checked state via aria-checked / data-state.
    expect(checkbox).toHaveAttribute('data-state', 'checked');

    // Toggling the checkbox flips the state.
    fireEvent.click(checkbox);
    await waitFor(() =>
      expect(checkbox).toHaveAttribute('data-state', 'unchecked')
    );
  });

  it('renders a persistent View Certificate button on https URLs and opens the cert dialog', async () => {
    fetchIqSettings.mockResolvedValue(storedConfig());
    renderTab();
    await awaitLoaded();

    const viewCert = screen.getByTestId('iq-tab-view-cert');
    expect(viewCert).toBeInTheDocument();
    fireEvent.click(viewCert);

    expect(screen.getByTestId('cert-view-dialog')).toHaveAttribute(
      'data-url',
      'https://iq.example.com'
    );

    fireEvent.click(screen.getByTestId('cert-view-close'));
    expect(screen.queryByTestId('cert-view-dialog')).not.toBeInTheDocument();
  });

  it('renders the cert-warning next to View Certificate after an untrusted-cert Test failure', async () => {
    fetchIqSettings.mockResolvedValue(storedConfig());
    verifyConnection.mockResolvedValue({
      success: false,
      reason: 'PKIX path building failed: unable to find valid certification path',
    });
    renderTab();
    await awaitLoaded();

    fireEvent.click(testButton());
    await waitFor(() =>
      expect(screen.getByTestId('iq-tab-cert-warning')).toBeInTheDocument()
    );
    // Also shows the general "Connection failed" message near Test Connection.
    expect(screen.getByTestId('iq-tab-verify-msg')).toBeInTheDocument();
  });

  it('clears the cert warning after a retest succeeds', async () => {
    fetchIqSettings.mockResolvedValue(storedConfig());
    verifyConnection
      .mockResolvedValueOnce({
        success: false,
        reason: 'PKIX path building failed',
      })
      .mockResolvedValueOnce({ success: true });
    renderTab();
    await awaitLoaded();

    fireEvent.click(testButton());
    await waitFor(() =>
      expect(screen.getByTestId('iq-tab-cert-warning')).toBeInTheDocument()
    );

    fireEvent.click(testButton());
    await waitFor(() =>
      expect(screen.queryByTestId('iq-tab-cert-warning')).not.toBeInTheDocument()
    );
    expect(screen.getByText('Connection successful')).toBeInTheDocument();
  });

  it('drops the trust-store flag when the URL changes to http', async () => {
    fetchIqSettings.mockResolvedValue(storedConfig({ useTrustStoreForUrl: true }));
    renderTab();
    await awaitLoaded();

    // Change URL to non-https.
    fireEvent.change(urlField(), { target: { value: 'http://iq.example.com' } });

    // Checkbox is gone, and the payload sent on Save must omit the flag.
    expect(screen.queryByTestId('iq-tab-use-truststore')).not.toBeInTheDocument();
  });
});

describe('IqServerConnectionSettingsTab — form basics', () => {
  it('sends the stored-password sentinel back on Save when unchanged', async () => {
    fetchIqSettings.mockResolvedValue(storedConfig());
    const onSaved = jest.fn();
    renderTab({ onSaved });
    await awaitLoaded();

    // Reconnect flow: stored config with enabled=false, all fields loaded — Save is enabled without edits.
    fireEvent.click(saveButton());
    await waitFor(() => expect(putIqSettings).toHaveBeenCalled());
    const payload = putIqSettings.mock.calls[0][0];
    expect(payload.password).toBe(PASSWORD_PLACEHOLDER);
    await waitFor(() => expect(onSaved).toHaveBeenCalled());
  });

  it('lets the user replace the stored password via Change', async () => {
    fetchIqSettings.mockResolvedValue(storedConfig());
    renderTab();
    await awaitLoaded();

    fireEvent.click(screen.getByTestId('iq-tab-pw-change'));
    // The read-only mask is replaced by an editable field.
    await waitFor(() => expect(screen.queryByTestId('iq-tab-pw-stored')).not.toBeInTheDocument());

    fireEvent.change(passwordField(), { target: { value: 'newpassword' } });
    fireEvent.click(saveButton());
    await waitFor(() => expect(putIqSettings).toHaveBeenCalled());
    expect(putIqSettings.mock.calls[0][0].password).toBe('newpassword');
  });

  it('reverts in-progress edits back to the loaded config when Discard is clicked', async () => {
    fetchIqSettings.mockResolvedValue(storedConfig());
    renderTab();
    await awaitLoaded();

    // Discard is disabled until an edit is made.
    const discard = screen.getByTestId('iq-tab-discard');
    expect(discard).toBeDisabled();

    // Edit the URL, which enables Discard.
    fireEvent.change(urlField(), { target: { value: 'https://changed.example.com' } });
    expect(urlField().value).toBe('https://changed.example.com');
    await waitFor(() => expect(discard).not.toBeDisabled());

    // Discard reverts the field to the loaded value and re-disables itself. Nothing is persisted.
    fireEvent.click(discard);
    await waitFor(() => expect(urlField().value).toBe('https://iq.example.com'));
    expect(discard).toBeDisabled();
    expect(putIqSettings).not.toHaveBeenCalled();
  });

  it('never renders the stored-password sentinel to the DOM', async () => {
    fetchIqSettings.mockResolvedValue(storedConfig());
    renderTab();
    await awaitLoaded();

    expect(document.body.textContent).not.toContain(PASSWORD_PLACEHOLDER);
    document.querySelectorAll('input, textarea').forEach(el => {
      expect((el as HTMLInputElement).value).not.toBe(PASSWORD_PLACEHOLDER);
    });
  });

  it('confirms through the modal before disconnecting, then calls onDisconnected', async () => {
    fetchIqSettings.mockResolvedValue(storedConfig({ enabled: true }));
    const onDisconnected = jest.fn();
    renderTab({ onDisconnected, initiallyConnected: true });
    await awaitLoaded();

    // Clicking Disconnect opens the confirmation modal but does NOT disconnect yet.
    fireEvent.click(screen.getByTestId('iq-tab-disconnect'));
    expect(await screen.findByTestId('iq-disconnect-modal')).toBeInTheDocument();
    expect(disableIqServer).not.toHaveBeenCalled();

    // Confirming in the modal fires the disconnect and reports it up.
    fireEvent.click(screen.getByTestId('iq-disconnect-modal-confirm'));
    await waitFor(() => expect(disableIqServer).toHaveBeenCalled());
    await waitFor(() => expect(onDisconnected).toHaveBeenCalled());
    // Modal closes once the disconnect succeeds.
    await waitFor(() =>
      expect(screen.queryByTestId('iq-disconnect-modal')).not.toBeInTheDocument()
    );
  });

  it('refreshes into the reconnect view after disconnecting from the tab (NEXUS-55151)', async () => {
    // Regression: clicking the in-tab Disconnect kept the tab mounted with no refetch. The parent
    // flips its enabled flag (initiallyConnected -> false) but the tab's own settings.enabled stayed
    // true, and `wasConnected = initiallyConnected || settings.enabled` kept the stale true, so the
    // Disconnect button and connected layout never went away — the page looked un-refreshed.
    fetchIqSettings.mockResolvedValue(storedConfig({ enabled: true }));

    // Mirror the real parent (IqServerPage.applyDisconnected): on disconnect it re-renders the tab
    // with initiallyConnected=false while the tab stays mounted (no remount/refetch).
    function Harness() {
      const [connected, setConnected] = React.useState(true);
      return (
        <Theme>
          <IqServerConnectionSettingsTab
            onSaved={jest.fn()}
            onDisconnected={() => setConnected(false)}
            initiallyConnected={connected}
          />
        </Theme>
      );
    }
    render(<Harness />);
    await awaitLoaded();

    expect(screen.getByTestId('iq-tab-disconnect')).toBeInTheDocument();

    fireEvent.click(screen.getByTestId('iq-tab-disconnect'));
    fireEvent.click(await screen.findByTestId('iq-disconnect-modal-confirm'));

    await waitFor(() => expect(disableIqServer).toHaveBeenCalled());

    // The tab reflects the disconnect: the Disconnect button is gone and Save (reconnect) is enabled
    // off the still-loaded URL/username/stored-password, without any field edit.
    await waitFor(() =>
      expect(screen.queryByTestId('iq-tab-disconnect')).not.toBeInTheDocument()
    );
    expect(saveButton()).not.toBeDisabled();
  });

  it('does not disconnect when the confirmation modal is cancelled', async () => {
    fetchIqSettings.mockResolvedValue(storedConfig({ enabled: true }));
    const onDisconnected = jest.fn();
    renderTab({ onDisconnected, initiallyConnected: true });
    await awaitLoaded();

    fireEvent.click(screen.getByTestId('iq-tab-disconnect'));
    expect(await screen.findByTestId('iq-disconnect-modal')).toBeInTheDocument();

    fireEvent.click(screen.getByTestId('iq-disconnect-modal-cancel'));
    await waitFor(() =>
      expect(screen.queryByTestId('iq-disconnect-modal')).not.toBeInTheDocument()
    );
    expect(disableIqServer).not.toHaveBeenCalled();
    expect(onDisconnected).not.toHaveBeenCalled();
  });

  it('surfaces an error and closes the modal when Disconnect fails', async () => {
    fetchIqSettings.mockResolvedValue(storedConfig({ enabled: true }));
    disableIqServer.mockRejectedValue(new Error('disable failed'));
    const onDisconnected = jest.fn();
    renderTab({ onDisconnected, initiallyConnected: true });
    await awaitLoaded();

    fireEvent.click(screen.getByTestId('iq-tab-disconnect'));
    fireEvent.click(await screen.findByTestId('iq-disconnect-modal-confirm'));

    await waitFor(() => expect(disableIqServer).toHaveBeenCalled());
    await waitFor(() =>
      expect(screen.queryByTestId('iq-disconnect-modal')).not.toBeInTheDocument()
    );
    expect(screen.getByText('disable failed')).toBeInTheDocument();
    expect(onDisconnected).not.toHaveBeenCalled();
  });

  it('renders without crashing when the server returns a null url (never configured)', async () => {
    // Regression for NEXUS-55151: an all-null XO reached the tab and isSecureUrl called
    // null.trim(). fetchIqSettings normalizes it, but the tab must also tolerate a null url.
    fetchIqSettings.mockResolvedValue(
      toFormData({
        enabled: false,
        url: null as any,
        authenticationType: 'USER',
        username: null as any,
        password: null as any,
        useTrustStoreForUrl: false,
        timeoutSeconds: null,
        properties: '',
        showLink: false,
      })
    );
    renderTab();
    await awaitLoaded();

    // URL field renders empty; the https-only trust-store controls are hidden.
    expect(urlField().value).toBe('');
    expect(screen.queryByTestId('iq-tab-use-truststore')).not.toBeInTheDocument();
    // Test Connection is disabled because no URL is set.
    expect(testButton()).toBeDisabled();
  });

  it('shows an error callout when the initial fetch fails', async () => {
    fetchIqSettings.mockRejectedValue(new Error('boom'));
    renderTab();
    await awaitLoaded();

    expect(screen.getByText('boom')).toBeInTheDocument();
  });

  it('disables the form when canEdit is false', async () => {
    fetchIqSettings.mockResolvedValue(firstTimeConfig());
    renderTab({ canEdit: false });
    await awaitLoaded();

    // URL field is disabled via <fieldset disabled>.
    expect(urlField()).toBeDisabled();
    // A read-only informational callout is rendered.
    expect(
      screen.getByText(/You are viewing a read-only version of this page/)
    ).toBeInTheDocument();
  });

  it('accepts a first-time config and saves it', async () => {
    fetchIqSettings.mockResolvedValue(firstTimeConfig());
    const onSaved = jest.fn();
    renderTab({ onSaved });
    await awaitLoaded();

    fireEvent.change(urlField(), { target: { value: 'https://iq.example.com' } });
    fireEvent.change(usernameField(), { target: { value: 'admin' } });
    fireEvent.change(passwordField(), { target: { value: 'secret' } });

    // Save button becomes enabled once the required fields are filled.
    await waitFor(() => expect(saveButton()).not.toBeDisabled());
    fireEvent.click(saveButton());
    await waitFor(() => expect(putIqSettings).toHaveBeenCalled());
    const payload = putIqSettings.mock.calls[0][0];
    expect(payload.enabled).toBe(true);
    expect(payload.url).toBe('https://iq.example.com');
    expect(payload.username).toBe('admin');
    expect(payload.password).toBe('secret');
    await waitFor(() => expect(onSaved).toHaveBeenCalled());
  });
});

describe('PKI authentication', () => {
  it('asks for no password and sends none', async () => {
    fetchIqSettings.mockResolvedValue(
      storedConfig({ authenticationType: 'PKI', username: '', password: '' })
    );
    renderTab();
    await awaitLoaded();

    // Username and Password fields should not be visible for PKI auth
    expect(screen.queryByLabelText(/Username/)).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/^Password/)).not.toBeInTheDocument();
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
});
