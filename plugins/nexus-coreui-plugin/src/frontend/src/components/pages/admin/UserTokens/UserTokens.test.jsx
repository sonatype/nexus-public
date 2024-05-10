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
import {render, screen, waitForElementToBeRemoved, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ExtJS, APIConstants, Permissions} from '@sonatype/nexus-ui-plugin';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import {when} from 'jest-when';

import Axios from 'axios';
import UserTokens from './UserTokens';
import UIStrings from '../../../../constants/UIStrings';

const {
  USER_TOKEN_CONFIGURATION: {
    USER_TOKENS_CHECKBOX,
    REPOSITORY_AUTHENTICATION_CHECKBOX,
    EXPIRATION_CHECKBOX,
    USER_TOKEN_EXPIRY,
    USER_TOKEN_EXPIRY_CONFIRMATION,
    RESET_ALL_TOKENS_BUTTON,
    RESET_CONFIRMATION
  },
  SETTINGS: {
    CANCEL_BUTTON_LABEL,
    SAVE_BUTTON_LABEL,
    DISCARD_BUTTON_LABEL,
    READ_ONLY: {WARNING: READ_ONLY_WARNING}
  }
} = UIStrings;

const API_URL = APIConstants.REST.PUBLIC.USER_TOKENS;

jest.mock('@sonatype/nexus-ui-plugin', () => {
  return {
    ...jest.requireActual('@sonatype/nexus-ui-plugin'),
    ExtJS: {
      checkPermission: jest.fn(),
      showSuccessMessage: jest.fn(),
    }
  };
});

jest.mock('axios', () => {
  return {
    ...jest.requireActual('axios'),
    get: jest.fn(),
    put: jest.fn(),
    delete: jest.fn()
  };
});

const selectors = {
  ...TestUtils.selectors,
  userTokensCheckbox: () => screen.getAllByLabelText(USER_TOKENS_CHECKBOX.DESCRIPTION)[0],
  repositoryUserTokensCheckbox: () =>
    screen.getAllByLabelText(REPOSITORY_AUTHENTICATION_CHECKBOX.DESCRIPTION)[1],
  userTokenExpirationCheckbox: () =>
      screen.getAllByLabelText(EXPIRATION_CHECKBOX.DESCRIPTION)[2],
  userTokenExpiry: () => screen.getByLabelText(USER_TOKEN_EXPIRY.LABEL),
  saveButton: () => screen.queryByText(SAVE_BUTTON_LABEL),
  discardButton: () => screen.getByText(DISCARD_BUTTON_LABEL),
  resetButton: () => screen.queryByText(RESET_ALL_TOKENS_BUTTON),
  readOnlyWarning: () => screen.getByText(READ_ONLY_WARNING),
  confirmation: {
    modal: () => screen.queryByRole('dialog'),
    alert: () => within(selectors.confirmation.modal()).getByRole('alert'),
    cancelButton: () => within(selectors.confirmation.modal()).getByRole('button', {name: CANCEL_BUTTON_LABEL}),
    submitButton: (n) => within(selectors.confirmation.modal()).getByRole('button', {name: n}),
    input: () => within(selectors.confirmation.modal()).getByLabelText(RESET_CONFIRMATION.LABEL)
  }
};

const renderView = async () => {
  render(<UserTokens />);
  await waitForElementToBeRemoved(selectors.queryLoadingMask());
};

describe('user tokens', () => {
  beforeEach(() => {
    when(Axios.get)
      .calledWith(API_URL)
      .mockResolvedValue({
        data: {
          enabled: false,
          protectContent: false,
          expirationEnabled: false,
          expirationDays: 30
        }
      });
    ExtJS.checkPermission.mockReturnValue(true);
  });

  it('renders default form and enables/disables fields correctly', async () => {
    const {
      userTokensCheckbox,
      repositoryUserTokensCheckbox,
      userTokenExpirationCheckbox,
      userTokenExpiry,
      discardButton,
      resetButton
    } = selectors;

    await renderView();

    expect(userTokensCheckbox()).not.toBeChecked();
    expect(repositoryUserTokensCheckbox()).not.toBeChecked();
    expect(repositoryUserTokensCheckbox()).toBeDisabled();
    expect(userTokenExpirationCheckbox()).not.toBeChecked();
    expect(userTokenExpirationCheckbox()).toBeDisabled();
    expect(userTokenExpiry()).toBeDisabled();
    expect(userTokenExpiry()).toHaveValue('30');
    expect(discardButton()).toHaveClass('disabled');
    expect(resetButton()).not.toBeInTheDocument();

    userEvent.click(userTokensCheckbox());

    expect(repositoryUserTokensCheckbox()).not.toBeChecked();
    expect(repositoryUserTokensCheckbox()).toBeEnabled();
    expect(userTokenExpirationCheckbox()).not.toBeChecked();
    expect(userTokenExpirationCheckbox()).toBeEnabled();
    expect(userTokenExpiry()).toBeDisabled();
    expect(discardButton()).not.toHaveClass('disabled');
    expect(resetButton()).not.toBeInTheDocument();

    userEvent.click(userTokenExpirationCheckbox());

    expect(userTokenExpirationCheckbox()).toBeChecked();
    expect(userTokenExpiry()).toBeEnabled();

    userEvent.click(userTokenExpirationCheckbox());

    expect(userTokenExpirationCheckbox()).not.toBeChecked();
    expect(userTokenExpiry()).toBeDisabled();

    userEvent.click(userTokensCheckbox());

    expect(repositoryUserTokensCheckbox()).not.toBeChecked();
    expect(repositoryUserTokensCheckbox()).toBeDisabled();
    expect(userTokenExpirationCheckbox()).not.toBeChecked();
    expect(userTokenExpirationCheckbox()).toBeDisabled();
    expect(userTokenExpiry()).toBeDisabled();
    expect(discardButton()).toHaveClass('disabled');
    expect(resetButton()).not.toBeInTheDocument();
  });

  it('discards changes', async () => {
    const {userTokensCheckbox, repositoryUserTokensCheckbox, userTokenExpirationCheckbox, discardButton} = selectors;

    await renderView();

    userEvent.click(userTokensCheckbox());
    userEvent.click(repositoryUserTokensCheckbox());
    userEvent.click(userTokenExpirationCheckbox());

    expect(userTokensCheckbox()).toBeChecked();
    expect(repositoryUserTokensCheckbox()).toBeChecked();
    expect(userTokenExpirationCheckbox()).toBeChecked();

    userEvent.click(discardButton());

    expect(userTokensCheckbox()).not.toBeChecked();
    expect(repositoryUserTokensCheckbox()).not.toBeChecked();
    expect(userTokenExpirationCheckbox()).not.toBeChecked();
  });

  it('sends correct data to API: enable', async () => {
    const {userTokensCheckbox, repositoryUserTokensCheckbox, saveButton} = selectors;

    await renderView();

    userEvent.click(userTokensCheckbox());
    userEvent.click(repositoryUserTokensCheckbox());

    userEvent.click(saveButton());

    await waitFor(() =>
      expect(Axios.put).toBeCalledWith(API_URL, {
        enabled: true,
        protectContent: true,
        expirationEnabled: false,
        expirationDays: 30
      })
    );
  });

  it('validates the user token expiry field', async () => {
    const {userTokenExpiry} = selectors;

    when(Axios.get)
      .calledWith(API_URL)
      .mockResolvedValueOnce({
        data: {
          enabled: true,
          protectContent: true,
          expirationEnabled: true,
          expirationDays: 30
        }
      });

    await renderView();

    expect(userTokenExpiry()).not.toHaveErrorMessage();

    await TestUtils.changeField(userTokenExpiry, '');
    expect(userTokenExpiry()).toHaveErrorMessage(TestUtils.REQUIRED_MESSAGE);

    await TestUtils.changeField(userTokenExpiry, '-1');
    expect(userTokenExpiry()).toHaveErrorMessage('The minimum value for this field is 1');

    await TestUtils.changeField(userTokenExpiry, '1000');
    expect(userTokenExpiry()).toHaveErrorMessage('The maximum value for this field is 999');

    await TestUtils.changeField(userTokenExpiry, '3.5');
    expect(userTokenExpiry()).toHaveErrorMessage('This field must not contain decimal values');

    await TestUtils.changeField(userTokenExpiry, '2*');
    expect(userTokenExpiry()).toHaveErrorMessage('This field must contain a numeric value');
  });

  it('sends correct data to API: disable', async () => {
    const {userTokensCheckbox, repositoryUserTokensCheckbox, userTokenExpirationCheckbox, saveButton} = selectors;

    when(Axios.get)
      .calledWith(API_URL)
      .mockResolvedValueOnce({
        data: {
          enabled: true,
          protectContent: true,
          expirationEnabled: true,
          expirationDays: 30
        }
      });

    await renderView();

    expect(userTokensCheckbox()).toBeChecked();
    expect(repositoryUserTokensCheckbox()).toBeChecked();
    expect(userTokenExpirationCheckbox()).toBeChecked();

    userEvent.click(userTokensCheckbox());
    userEvent.click(saveButton());

    await waitFor(() =>
      expect(Axios.put).toBeCalledWith(API_URL, {
        enabled: false,
        protectContent: false,
        expirationEnabled: false,
        expirationDays: 30
      })
    );
  });

  it('renders read only view when user has no update permission', async () => {
    const {saveButton, readOnlyWarning} = selectors;

    when(ExtJS.checkPermission).calledWith(Permissions.USER_TOKENS_SETTINGS.UPDATE).mockReturnValue(false);

    await renderView();

    expect(saveButton()).not.toBeInTheDocument();
    expect(readOnlyWarning()).toBeInTheDocument();
  });
});

describe('reset user tokens', () => {
  beforeEach(() => {
    when(Axios.get)
      .calledWith(API_URL)
      .mockResolvedValue({
        data: {
          enabled: true,
          protectContent: false,
          expirationEnabled: false,
          expirationDays: 30
        }
      });
    ExtJS.checkPermission.mockReturnValue(true);
  });

  it('do not show reset button when user has no delete permission', async () => {
    const {resetButton, saveButton} = selectors;

    when(ExtJS.checkPermission).calledWith(Permissions.USER_TOKENS_SETTINGS.UPDATE).mockReturnValue(true);
    when(ExtJS.checkPermission).calledWith(Permissions.USER_TOKENS_USERS.DELETE).mockReturnValue(false);

    await renderView();

    expect(saveButton()).toBeInTheDocument();
    expect(resetButton()).not.toBeInTheDocument();
  });

  it('renders reset confirmation modal and submits reset', async () => {
    const {
      resetButton,
      confirmation: {modal, submitButton, cancelButton, input}
    } = selectors;

    await renderView();

    expect(resetButton()).toBeInTheDocument();

    userEvent.click(resetButton());

    await waitFor(() => expect(modal()).toBeInTheDocument());

    expect(cancelButton()).toBeInTheDocument();

    await TestUtils.changeField(input, 'abracadabra');

    await TestUtils.changeField(input, RESET_CONFIRMATION.CONFIRMATION_STRING);

    userEvent.click(submitButton(RESET_CONFIRMATION.CAPTION));

    await waitFor(() => expect(Axios.delete).toBeCalledWith(API_URL));

    expect(modal()).not.toBeInTheDocument();
  });

  it('allows the user tokens to be reset multiple times', async () => {
    const {
      resetButton,
      confirmation: {modal, submitButton, input}
    } = selectors;

    await renderView();

    when(Axios.delete).calledWith(API_URL).mockResolvedValueOnce(true);

    userEvent.click(resetButton());

    await waitFor(() => expect(modal()).toBeInTheDocument());

    await TestUtils.changeField(input, RESET_CONFIRMATION.CONFIRMATION_STRING);

    userEvent.click(submitButton(RESET_CONFIRMATION.CAPTION));

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.click(resetButton());

    await waitFor(() => expect(modal()).toBeInTheDocument());
  });

  it('closes the modal on cancel', async () => {
    const {
      resetButton,
      confirmation: {modal, cancelButton}
    } = selectors;

    await renderView();

    userEvent.click(resetButton());

    await waitFor(() => expect(modal()).toBeInTheDocument());

    userEvent.click(cancelButton());

    expect(modal()).not.toBeInTheDocument();
  });
});

describe('user token expiration confirmation', () => {
  beforeEach(() => {
    when(Axios.get)
      .calledWith(API_URL)
      .mockResolvedValue({
        data: {
          enabled: true,
          protectContent: false,
          expirationEnabled: false,
          expirationDays: 30
        }
      });
    ExtJS.checkPermission.mockReturnValue(true);
  });

  it('renders expiration confirmation modal with enabled alert and submits changes', async () => {
    const {
      userTokenExpirationCheckbox,
      saveButton,
      confirmation: {modal, submitButton, cancelButton, alert}
    } = selectors;

    const enabledAlertContent = 'Changes to user token expiration will apply to all existing user tokens. ' +
        'Any user tokens older than the specified age will now expire.';

    await renderView();

    userEvent.click(userTokenExpirationCheckbox());
    userEvent.click(saveButton());

    await waitFor(() => expect(modal()).toBeInTheDocument());
    expect(alert()).toBeInTheDocument();
    expect(alert()).toHaveTextContent(enabledAlertContent);
    expect(submitButton(USER_TOKEN_EXPIRY_CONFIRMATION.CONFIRM_BUTTON)).toBeInTheDocument();
    expect(cancelButton()).toBeInTheDocument();

    userEvent.click(submitButton(USER_TOKEN_EXPIRY_CONFIRMATION.CONFIRM_BUTTON));

    await waitFor(() =>
      expect(Axios.put).toBeCalledWith(API_URL, {
        enabled: true,
        protectContent: false,
        expirationEnabled: true,
        expirationDays: 30
      })
    );
  });

  it('renders expiration confirmation modal with disabled alert and submits changes', async () => {
    when(Axios.get)
      .calledWith(API_URL)
      .mockResolvedValue({
        data: {
          enabled: true,
          protectContent: false,
          expirationEnabled: true,
          expirationDays: 30
        }
      });

    const {
      userTokenExpirationCheckbox,
      saveButton,
      confirmation: {modal, submitButton, cancelButton, alert}
    } = selectors;

    const disabledAlertContent = 'Disabling user token expiration means that all active user tokens will ' +
        'remain active and never expire.'

    await renderView();

    userEvent.click(userTokenExpirationCheckbox());
    userEvent.click(saveButton());

    await waitFor(() => expect(modal()).toBeInTheDocument());
    expect(alert()).toBeInTheDocument();
    expect(alert()).toHaveTextContent(disabledAlertContent);
    expect(submitButton(USER_TOKEN_EXPIRY_CONFIRMATION.CONFIRM_BUTTON)).toBeInTheDocument();
    expect(cancelButton()).toBeInTheDocument();

    userEvent.click(submitButton(USER_TOKEN_EXPIRY_CONFIRMATION.CONFIRM_BUTTON));

    await waitFor(() =>
      expect(Axios.put).toBeCalledWith(API_URL, {
        enabled: true,
        protectContent: false,
        expirationEnabled: false,
        expirationDays: 30
      })
    );
  });

  it('closes the modal on cancel', async () => {
    const {
      userTokenExpirationCheckbox,
      saveButton,
      confirmation: {modal, cancelButton}
    } = selectors;

    await renderView();

    userEvent.click(userTokenExpirationCheckbox());
    userEvent.click(saveButton());

    await waitFor(() => expect(modal()).toBeInTheDocument());

    userEvent.click(cancelButton());

    expect(modal()).not.toBeInTheDocument();
  });
});
