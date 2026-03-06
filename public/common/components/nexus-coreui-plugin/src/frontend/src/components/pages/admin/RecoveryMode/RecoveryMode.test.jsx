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
import {act} from 'react-dom/test-utils';
import {
  render,
  screen,
  waitForElementToBeRemoved
} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {when} from 'jest-when';
import axios from 'axios';

import {ExtJS, DateUtils, APIConstants} from '@sonatype/nexus-ui-plugin';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import RecoveryMode from './RecoveryMode';
import UIStrings from '../../../../constants/UIStrings';

const {
  RECOVERY_MODE: RECOVERY_MODE_PUBLIC_API
} = APIConstants.REST.PUBLIC;

const {
  RECOVERY_MODE: RECOVERY_MODE_UI_API
} = APIConstants.REST.INTERNAL;

const {LABELS, MENU, TABLE, CONFIRMATION_MODAL} = UIStrings.RECOVERY_MODE;

const mockRecoveryModeDisabled = {
  enabled: false,
  unexecutedPlans: false,
  blockedTaskNames: ['Task A', 'Task B'],
  reconcileTasks: [
    {
      name: 'Reconcile Task 1',
      currentState: 'WAITING',
      lastRun: 1609459200000,
      lastRunResult: 'SUCCESS'
    },
    {
      name: 'Reconcile Task 2',
      currentState: 'OK',
      lastRun: 1609545600000,
      lastRunResult: 'OK'
    }
  ]
};

const mockRecoveryModeEnabled = {
  ...mockRecoveryModeDisabled,
  enabled: true
};

const mockRecoveryModeEnabledWithUnexecutedPlans = {
  ...mockRecoveryModeEnabled,
  unexecutedPlans: true
};

const mockRecoveryModeEnabledWithRunningTasks = {
  ...mockRecoveryModeEnabled,
  reconcileTasks: [
    {
      name: 'Reconcile Task 1',
      currentState: 'WAITING',
      lastRun: 1609459200000,
      lastRunResult: 'SUCCESS'
    },
    {
      name: 'Reconcile Task 2',
      currentState: 'RUNNING',
      lastRun: 1609545600000,
      lastRunResult: 'FAILED'
    }
  ]
};

const mockRecoveryModeNoTasks = {
  enabled: false,
  unexecutedPlans: false,
  blockedTaskNames: [],
  reconcileTasks: []
};

jest.mock('@sonatype/nexus-ui-plugin', () => {
  return {
    ...jest.requireActual('@sonatype/nexus-ui-plugin'),
    ExtJS: {
      showSuccessMessage: jest.fn(),
      showErrorMessage: jest.fn(),
      setDirtyStatus: jest.requireActual('@sonatype/nexus-ui-plugin').ExtJS.setDirtyStatus,
      useUser: jest.fn(() => ({name: 'test-user'})),
      state: jest.fn(() => ({
        getValue: jest.fn(),
        setValue: jest.fn()
      }))
    },
    DateUtils: {
      prettyDateTime: jest.fn()
    }
  };
});

const selectors = {
  ...TestUtils.selectors,
  enableButton: () => screen.getByText(LABELS.ENABLE_BUTTON),
  disableButton: () => screen.getByText(LABELS.DISABLE_BUTTON),
  statusLabel: () => screen.getByText(LABELS.STATUS_LABEL, { selector: 'h3' }),
  howItWorks: () => screen.getByText(LABELS.HOW_IT_WORKS),
  dataRepairTasks: () => screen.getByText(LABELS.DATA_REPAIR_TASKS),
  queryEnableButton: () => screen.queryByText(LABELS.ENABLE_BUTTON),
  queryDisableButton: () => screen.queryByText(LABELS.DISABLE_BUTTON),
  confirmationModal: () => screen.getByLabelText('disable-recovery-mode-confirmation'),
  confirmationModalTitle: () => screen.getByText(CONFIRMATION_MODAL.TITLE),
  confirmationModalMessage: () => screen.getByText(CONFIRMATION_MODAL.MESSAGE),
  confirmButton: () => screen.getByText(CONFIRMATION_MODAL.CONFIRM_BUTTON),
  cancelButton: () => screen.getByText(CONFIRMATION_MODAL.CANCEL_BUTTON),
  queryConfirmationModal: () => screen.queryByLabelText('disable-recovery-mode-confirmation')
};

describe('RecoveryMode', () => {
  beforeEach(() => {
    when(axios.get).calledWith(RECOVERY_MODE_UI_API).mockResolvedValue({
      data: mockRecoveryModeDisabled
    });
    axios.post.mockResolvedValue();
    axios.delete.mockResolvedValue();
    DateUtils.prettyDateTime.mockImplementation((date) => date.toISOString());
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  async function renderView() {
    const view = render(<RecoveryMode/>);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
    return view;
  }

  it('renders null when user is not logged in', () => {
    ExtJS.useUser.mockImplementation(() => null);
    const {container} = render(<RecoveryMode/>);
    expect(container.firstChild).toBeNull();
    ExtJS.useUser.mockImplementation(() => ({name: 'test-user'}));
  });

  it('fetches recovery mode data from the API and displays it', async () => {
    when(axios.get).calledWith(RECOVERY_MODE_UI_API).mockResolvedValue({
      data: mockRecoveryModeEnabled
    });
    await renderView();

    expect(axios.get).toHaveBeenCalledWith(RECOVERY_MODE_UI_API);
    expect(screen.getByText(MENU.text)).toBeInTheDocument();
    expect(selectors.howItWorks()).toBeInTheDocument();
    expect(selectors.statusLabel()).toBeInTheDocument();
    expect(selectors.dataRepairTasks()).toBeInTheDocument();
  });

  it('displays Enable button when recovery mode is disabled', async () => {
    when(axios.get).calledWith(RECOVERY_MODE_UI_API).mockResolvedValue({
      data: mockRecoveryModeDisabled
    });
    await renderView();

    expect(selectors.enableButton()).toBeInTheDocument();
    expect(selectors.queryDisableButton()).not.toBeInTheDocument();
    expect(screen.getByText(LABELS.STATE_DISABLED)).toBeInTheDocument();
  });

  it('displays Disable button when recovery mode is enabled', async () => {
    when(axios.get).calledWith(RECOVERY_MODE_UI_API).mockResolvedValue({
      data: mockRecoveryModeEnabled
    });

    await renderView();

    expect(selectors.disableButton()).toBeInTheDocument();
    expect(selectors.disableButton().closest('button')).not.toBeDisabled();
    expect(selectors.queryEnableButton()).not.toBeInTheDocument();
    expect(screen.getByText(LABELS.STATE_ENABLED)).toBeInTheDocument();
    expect(screen.queryByText(/Recovery mode can't be disabled/)).not.toBeInTheDocument();
  });

  it('disables the Disable button and shows warning when reconcile tasks are running', async () => {
    when(axios.get).calledWith(RECOVERY_MODE_UI_API).mockResolvedValue({
      data: mockRecoveryModeEnabledWithRunningTasks
    });

    await renderView();

    expect(selectors.disableButton().closest('button')).toBeDisabled();
    expect(screen.getByText(/Recovery mode can't be disabled/)).toBeInTheDocument();
  });

  it('displays blocked task names in a list', async () => {
    when(axios.get).calledWith(RECOVERY_MODE_UI_API).mockResolvedValue({
      data: mockRecoveryModeEnabled
    });
    await renderView();

    expect(screen.getByText('Task A')).toBeInTheDocument();
    expect(screen.getByText('Task B')).toBeInTheDocument();
  });

  it('displays reconcile tasks in table', async () => {
    when(axios.get).calledWith(RECOVERY_MODE_UI_API).mockResolvedValue({
      data: mockRecoveryModeEnabled
    });
    await renderView();

    expect(screen.getByText(TABLE.NAME_LABEL)).toBeInTheDocument();
    expect(screen.getByText(TABLE.STATUS_LABEL, {selector : 'th'})).toBeInTheDocument();
    expect(screen.getByText(TABLE.LAST_RUN_LABEL)).toBeInTheDocument();
    expect(screen.getByText(TABLE.LAST_RESULT_LABEL)).toBeInTheDocument();

    expect(screen.getByText('Reconcile Task 1')).toBeInTheDocument();
    expect(screen.getByText('WAITING')).toBeInTheDocument();
    expect(screen.getByText('SUCCESS')).toBeInTheDocument();

    expect(screen.getByText('Reconcile Task 2')).toBeInTheDocument();
    expect(screen.getAllByText('OK').length).toBeGreaterThanOrEqual(1);

    expect(DateUtils.prettyDateTime).toHaveBeenCalledTimes(2);
  });

  it('displays empty message when no reconcile tasks exist', async () => {
    when(axios.get).calledWith(RECOVERY_MODE_UI_API).mockResolvedValue({
      data: mockRecoveryModeNoTasks
    });

    await renderView();

    expect(screen.getByText(TABLE.EMPTY_MESSAGE)).toBeInTheDocument();
  });

  it('enables recovery mode when Enable button is clicked', async () => {
    when(axios.get).calledWith(RECOVERY_MODE_UI_API).mockResolvedValue({
      data: mockRecoveryModeDisabled
    });
    await renderView();

    expect(axios.delete).not.toHaveBeenCalled();
    expect(axios.post).not.toHaveBeenCalled();

    await act(async () => {
      userEvent.click(selectors.enableButton());
    });

    expect(axios.post).toHaveBeenCalledWith(RECOVERY_MODE_PUBLIC_API);
    expect(ExtJS.showSuccessMessage).toHaveBeenCalledWith(
      UIStrings.RECOVERY_MODE.MESSAGES.SAVE_SUCCESS
    );
  });

  it('disables recovery mode when Disable button is clicked', async () => {
    when(axios.get).calledWith(RECOVERY_MODE_UI_API).mockResolvedValue({
      data: mockRecoveryModeEnabled
    });

    await renderView();

    expect(axios.delete).not.toHaveBeenCalled();
    expect(axios.post).not.toHaveBeenCalled();

    await act(async () => {
      userEvent.click(selectors.disableButton());
    });

    expect(axios.delete).toHaveBeenCalledWith(RECOVERY_MODE_PUBLIC_API);
    expect(ExtJS.showSuccessMessage).toHaveBeenCalledWith(
      UIStrings.RECOVERY_MODE.MESSAGES.SAVE_SUCCESS
    );
  });

  it('shows error message when API call fails on load', async () => {
    when(axios.get).calledWith(RECOVERY_MODE_UI_API).mockRejectedValue({
      message: 'Network error'
    });

    await renderView();

    expect(ExtJS.showErrorMessage).toHaveBeenCalledWith(
      UIStrings.RECOVERY_MODE.MESSAGES.LOAD_ERROR
    );
  });

  it('shows error message when save fails', async () => {
    axios.post.mockRejectedValueOnce({
      message: 'Save error'
    });

    await renderView();

    await act(async () => {
      userEvent.click(selectors.enableButton());
    });

    expect(ExtJS.showErrorMessage).toHaveBeenCalledWith(
      UIStrings.RECOVERY_MODE.MESSAGES.SAVE_ERROR
    );
  });

  it('updates ExtJS state after successful save', async () => {
    when(axios.get).calledWith(RECOVERY_MODE_UI_API).mockResolvedValue({
      data: mockRecoveryModeDisabled
    });

    const mockSetValue = jest.fn();
    const mockGetValue = jest.fn().mockReturnValue(false);
    ExtJS.state.mockReturnValue({
      getValue: mockGetValue,
      setValue: mockSetValue
    });

    await renderView();

    await act(async () => {
      userEvent.click(selectors.enableButton());
    });

    expect(mockSetValue).toHaveBeenCalledWith('recovery.mode.enabled', true);
  });

  it('disables recovery mode directly when no unexecuted plans exist', async () => {
    when(axios.get).calledWith(RECOVERY_MODE_UI_API).mockResolvedValue({
      data: mockRecoveryModeEnabled
    });

    await renderView();

    expect(selectors.queryConfirmationModal()).not.toBeInTheDocument();

    await act(async () => {
      userEvent.click(selectors.disableButton());
    });

    expect(axios.delete).toHaveBeenCalledWith(RECOVERY_MODE_PUBLIC_API);
    expect(selectors.queryConfirmationModal()).not.toBeInTheDocument();
  });

  it('shows confirmation modal when disabling with unexecuted plans', async () => {
    when(axios.get).calledWith(RECOVERY_MODE_UI_API).mockResolvedValue({
      data: mockRecoveryModeEnabledWithUnexecutedPlans
    });

    await renderView();

    expect(selectors.queryConfirmationModal()).not.toBeInTheDocument();

    await act(async () => {
      userEvent.click(selectors.disableButton());
    });

    expect(selectors.confirmationModal()).toBeInTheDocument();
    expect(selectors.confirmationModalTitle()).toBeInTheDocument();
    expect(selectors.confirmationModalMessage()).toBeInTheDocument();
    expect(selectors.confirmButton()).toBeInTheDocument();
    expect(selectors.cancelButton()).toBeInTheDocument();
    expect(axios.delete).not.toHaveBeenCalled();
  });

  it('disables recovery mode when confirm button is clicked in modal', async () => {
    when(axios.get).calledWith(RECOVERY_MODE_UI_API).mockResolvedValue({
      data: mockRecoveryModeEnabledWithUnexecutedPlans
    });

    await renderView();

    await act(async () => {
      userEvent.click(selectors.disableButton());
    });

    expect(selectors.confirmationModal()).toBeInTheDocument();

    await act(async () => {
      userEvent.click(selectors.confirmButton());
    });

    expect(axios.delete).toHaveBeenCalledWith(RECOVERY_MODE_PUBLIC_API);
    expect(ExtJS.showSuccessMessage).toHaveBeenCalledWith(
      UIStrings.RECOVERY_MODE.MESSAGES.SAVE_SUCCESS
    );
  });

  it('closes modal without disabling when cancel button is clicked', async () => {
    when(axios.get).calledWith(RECOVERY_MODE_UI_API).mockResolvedValue({
      data: mockRecoveryModeEnabledWithUnexecutedPlans
    });

    await renderView();

    await act(async () => {
      userEvent.click(selectors.disableButton());
    });

    expect(selectors.confirmationModal()).toBeInTheDocument();

    await act(async () => {
      userEvent.click(selectors.cancelButton());
    });

    expect(selectors.queryConfirmationModal()).not.toBeInTheDocument();
    expect(axios.delete).not.toHaveBeenCalled();
  });
});
