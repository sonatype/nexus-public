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
import { render, screen, waitFor, cleanup, fireEvent } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

// UIRouter — captured for assertion. Default state has no ?tab= so the page picks its default.
const mockGo = jest.fn();
const mockUseCurrentStateAndParams = jest.fn(() => ({
  state: { name: 'preview.admin.iq' },
  params: {},
}));
jest.mock('@uirouter/react', () => ({
  useRouter: () => ({ stateService: { go: mockGo } }),
  useCurrentStateAndParams: (...args: any[]) => mockUseCurrentStateAndParams(...args),
}));

// The IQ Server page reads the persisted enabled state from ExtJS bootstrap. Drive it per test.
import { ExtJS } from '../../../../../../../interface/ExtJS';
import Permissions from '../../../../../../../constants/Permissions';

const mockCheckPermission = jest.spyOn(ExtJS, 'checkPermission');
const mockStateGetValue = jest.fn();

jest.spyOn(ExtJS, 'state').mockReturnValue({
  getValue: (key: string) => mockStateGetValue(key),
} as any);

// Isolate the tabs from their data fetches — this suite only verifies the shell.
jest.mock('../IqServerOverviewTab', () => ({
  IqServerOverviewTab: () => <div data-testid="stub-overview-tab" />,
}));
jest.mock('../IqServerConnectionSettingsTab', () => ({
  IqServerConnectionSettingsTab: () => <div data-testid="stub-connection-tab" />,
}));

jest.mock('../iqServerFormMachine', () => {
  const actual = jest.requireActual('../iqServerFormMachine');
  return {
    ...actual,
    disableIqServer: jest.fn().mockResolvedValue(undefined),
  };
});

import { IqServerPage } from '../IqServerPage';
import * as formMachine from '../iqServerFormMachine';

const disableIqServer = formMachine.disableIqServer as jest.Mock;

function renderPage() {
  return render(
    <Theme>
      <IqServerPage />
    </Theme>
  );
}

function driveClm(state: { enabled?: boolean; url?: string } | null) {
  mockStateGetValue.mockImplementation((key: string) => (key === 'clm' ? state : undefined));
}

beforeEach(() => {
  jest.clearAllMocks();
  mockCheckPermission.mockReturnValue(true);
  mockUseCurrentStateAndParams.mockReturnValue({
    state: { name: 'preview.admin.iq' },
    params: {},
  });
  driveClm({ enabled: false });
});

afterEach(() => {
  cleanup();
  Array.from(document.body.children).forEach(el => el.remove());
});

describe('IqServerPage tab defaulting', () => {
  it('defaults to Overview and keeps it enabled even when IQ Server has never been configured', () => {
    driveClm(null);
    renderPage();

    // Overview is always reachable — its GET endpoints return an unconfigured payload (not an
    // error), so the tab renders the Lifecycle + Firewall summary in a "Not connected" state.
    const overview = screen.getByTestId('iq-tab-overview');
    expect(overview).not.toBeDisabled();

    // Overview is the default active tab; Connection Settings is mounted but not the visible pane.
    expect(screen.getByTestId('stub-overview-tab')).toBeInTheDocument();
    expect(screen.queryByTestId('stub-connection-tab')).not.toBeInTheDocument();
  });

  it('defaults to Overview when IQ Server is configured and enabled', () => {
    driveClm({ enabled: true, url: 'https://iq.example.com' });
    renderPage();

    const overview = screen.getByTestId('iq-tab-overview');
    expect(overview).not.toBeDisabled();
    // Both tabs are mounted (Radix forceMount) so we assert the connection-settings
    // tab is hidden from the visible pane. The overview stub is what renders.
    expect(screen.getByTestId('stub-overview-tab')).toBeInTheDocument();
  });

  it('honors the ?tab= URL param over the derived default', () => {
    driveClm({ enabled: true, url: 'https://iq.example.com' });
    mockUseCurrentStateAndParams.mockReturnValue({
      state: { name: 'preview.admin.iq' },
      params: { tab: 'connection-settings' },
    });
    renderPage();

    // The connection-settings stub is shown even though state is enabled.
    expect(screen.getByTestId('stub-connection-tab')).toBeInTheDocument();
    // The Overview content is not merely hidden — it is not rendered at all while
    // Connection Settings is active (NEXUS-55151: the inactive tabpanel stays empty).
    expect(screen.queryByTestId('stub-overview-tab')).not.toBeInTheDocument();
  });

  it('renders the URL badge in the header when configured', () => {
    driveClm({ enabled: true, url: 'https://iq.example.com' });
    renderPage();

    expect(screen.getByTestId('iq-server-url-chip')).toHaveTextContent('https://iq.example.com');
  });

  it('hides the enable/disable switch when never configured', () => {
    driveClm(null);
    renderPage();

    expect(screen.queryByTestId('iq-enable-toggle')).not.toBeInTheDocument();
  });

  it('shows the enable/disable switch when configured', () => {
    driveClm({ enabled: true, url: 'https://iq.example.com' });
    renderPage();

    expect(screen.getByTestId('iq-enable-toggle')).toBeInTheDocument();
  });

  it('disables the enable/disable switch when the user lacks settings:update', () => {
    driveClm({ enabled: true, url: 'https://iq.example.com' });
    mockCheckPermission.mockImplementation((p) => p !== Permissions.SETTINGS.UPDATE);
    renderPage();

    expect(screen.getByTestId('iq-enable-toggle')).toBeDisabled();
  });

  it('turning the switch off confirms through the modal before calling the disable endpoint', async () => {
    driveClm({ enabled: true, url: 'https://iq.example.com' });
    renderPage();

    // Flipping the switch off opens the confirmation modal but does NOT disable yet.
    fireEvent.click(screen.getByTestId('iq-enable-toggle'));
    expect(await screen.findByTestId('iq-disconnect-modal')).toBeInTheDocument();
    expect(disableIqServer).not.toHaveBeenCalled();

    // Confirming in the modal issues the disable request and closes the modal.
    fireEvent.click(screen.getByTestId('iq-disconnect-modal-confirm'));
    await waitFor(() => expect(disableIqServer).toHaveBeenCalled());
    await waitFor(() =>
      expect(screen.queryByTestId('iq-disconnect-modal')).not.toBeInTheDocument()
    );
  });

  it('does not disable when the confirmation modal is cancelled', async () => {
    driveClm({ enabled: true, url: 'https://iq.example.com' });
    renderPage();

    fireEvent.click(screen.getByTestId('iq-enable-toggle'));
    expect(await screen.findByTestId('iq-disconnect-modal')).toBeInTheDocument();

    fireEvent.click(screen.getByTestId('iq-disconnect-modal-cancel'));
    await waitFor(() =>
      expect(screen.queryByTestId('iq-disconnect-modal')).not.toBeInTheDocument()
    );
    expect(disableIqServer).not.toHaveBeenCalled();
    // The switch stays on since nothing was disabled.
    expect(screen.getByTestId('iq-enable-toggle')).toBeChecked();
  });

  it('turning the switch on when disabled routes into the Connection Settings tab', () => {
    driveClm({ enabled: false, url: 'https://iq.example.com' });
    renderPage();

    // Switch is unchecked when clm.enabled is false.
    const toggle = screen.getByTestId('iq-enable-toggle');
    fireEvent.click(toggle);

    // After clicking, the connection-settings tab is what's shown.
    expect(screen.getByTestId('stub-connection-tab')).toBeInTheDocument();
    // No disable call was made (it was an enable attempt).
    expect(disableIqServer).not.toHaveBeenCalled();
  });
});
