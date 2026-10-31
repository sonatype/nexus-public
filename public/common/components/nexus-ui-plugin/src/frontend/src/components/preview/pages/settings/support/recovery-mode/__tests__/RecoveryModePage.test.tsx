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
import { Theme } from '@radix-ui/themes';

import { RecoveryModePage } from '../RecoveryModePage';
import { RecoveryModeData } from '../types';
import { useRecoveryModeApi } from '../useRecoveryModeApi';
import { useToast } from '../../../../../shared';

// --- Mocks ---------------------------------------------------------------
jest.mock('@uirouter/react', () => ({
  useRouter: () => ({ stateService: { go: jest.fn() } }),
}));

jest.mock('../useRecoveryModeApi', () => ({
  useRecoveryModeApi: jest.fn(),
}));

jest.mock('../../../../../shared', () => {
  const actual = jest.requireActual('../../../../../shared');
  return { ...actual, useToast: jest.fn() };
});

const api = {
  fetchRecoveryMode: jest.fn(),
  enableRecoveryMode: jest.fn().mockResolvedValue(undefined),
  disableRecoveryMode: jest.fn().mockResolvedValue(undefined),
  error: null,
  setError: jest.fn(),
};
const toast = { success: jest.fn(), error: jest.fn() };
(useRecoveryModeApi as jest.Mock).mockReturnValue(api);
(useToast as jest.Mock).mockReturnValue(toast);

const renderPage = () => render(<Theme><RecoveryModePage /></Theme>);

const baseData = (overrides: Partial<RecoveryModeData> = {}): RecoveryModeData => ({
  enabled: false,
  unexecutedPlans: false,
  blockedTaskNames: [],
  reconcileTasks: [],
  ...overrides,
});

beforeEach(() => {
  jest.clearAllMocks();
  api.enableRecoveryMode.mockResolvedValue(undefined);
  api.disableRecoveryMode.mockResolvedValue(undefined);
  (useRecoveryModeApi as jest.Mock).mockReturnValue(api);
  (useToast as jest.Mock).mockReturnValue(toast);
});

describe('RecoveryModePage', () => {
  it('shows a loading state then the page', async () => {
    api.fetchRecoveryMode.mockResolvedValueOnce(baseData());
    renderPage();
    expect(screen.getByText(/Loading recovery mode/i)).toBeInTheDocument();
    await screen.findByTestId('recovery-mode-status');
  });

  it('renders Disabled state with an Enable button', async () => {
    api.fetchRecoveryMode.mockResolvedValueOnce(baseData({ enabled: false }));
    renderPage();
    expect(await screen.findByText('Disabled')).toBeInTheDocument();
    expect(screen.getByTestId('recovery-mode-enable')).toBeInTheDocument();
  });

  it('renders Enabled state with a Disable button', async () => {
    api.fetchRecoveryMode.mockResolvedValueOnce(baseData({ enabled: true }));
    renderPage();
    expect(await screen.findByText('Enabled')).toBeInTheDocument();
    expect(screen.getByTestId('recovery-mode-disable')).toBeInTheDocument();
  });

  it('disables the Disable button while a task is RUNNING and wraps it with a tooltip', async () => {
    api.fetchRecoveryMode.mockResolvedValueOnce(
      baseData({
        enabled: true,
        reconcileTasks: [{ id: '1', name: 'x', type: 'blobstore.planReconciliation', currentState: 'RUNNING' }],
      })
    );
    renderPage();
    const btn = await screen.findByTestId('recovery-mode-disable');
    expect(btn).toBeDisabled();
    // Tooltip wrapper present only while running
    expect(screen.getByTestId('recovery-mode-disable-tooltip')).toBeInTheDocument();
  });

  it('does not wrap the Disable button with a tooltip when no task is running', async () => {
    api.fetchRecoveryMode.mockResolvedValueOnce(baseData({ enabled: true }));
    renderPage();
    await screen.findByTestId('recovery-mode-disable');
    expect(screen.queryByTestId('recovery-mode-disable-tooltip')).not.toBeInTheDocument();
  });

  it('enables recovery mode via the Enable button', async () => {
    api.fetchRecoveryMode.mockResolvedValue(baseData({ enabled: false }));
    renderPage();
    fireEvent.click(await screen.findByTestId('recovery-mode-enable'));
    await waitFor(() => expect(api.enableRecoveryMode).toHaveBeenCalled());
  });

  it('disables directly when there are no unexecuted plans', async () => {
    api.fetchRecoveryMode.mockResolvedValue(baseData({ enabled: true, unexecutedPlans: false }));
    renderPage();
    fireEvent.click(await screen.findByTestId('recovery-mode-disable'));
    await waitFor(() => expect(api.disableRecoveryMode).toHaveBeenCalled());
  });

  it('shows a confirm modal before disabling when there are unexecuted plans', async () => {
    api.fetchRecoveryMode.mockResolvedValue(baseData({ enabled: true, unexecutedPlans: true }));
    renderPage();
    fireEvent.click(await screen.findByTestId('recovery-mode-disable'));
    // Modal appears; DELETE not called yet
    expect(await screen.findByText('Disable recovery mode with unexecuted plans?')).toBeInTheDocument();
    expect(api.disableRecoveryMode).not.toHaveBeenCalled();
    // Confirm
    fireEvent.click(screen.getByRole('button', { name: 'Disable Mode' }));
    await waitFor(() => expect(api.disableRecoveryMode).toHaveBeenCalled());
  });

  it('renders the hook error message when the initial fetch fails', async () => {
    api.fetchRecoveryMode.mockRejectedValueOnce(new Error('boom'));
    (useRecoveryModeApi as jest.Mock).mockReturnValue({ ...api, error: 'Failed to load recovery mode settings' });
    renderPage();
    // Loading resolves, then the error banner is shown
    expect(await screen.findByRole('alert')).toHaveTextContent('Failed to load recovery mode settings');
  });

  it('disables the Enable action when the initial fetch fails (no state loaded)', async () => {
    api.fetchRecoveryMode.mockRejectedValue(new Error('boom'));
    renderPage();
    // With no data loaded, the (disabled-state) Enable button renders but is disabled.
    const enableBtn = await screen.findByTestId('recovery-mode-enable');
    expect(enableBtn).toBeDisabled();
  });
});
