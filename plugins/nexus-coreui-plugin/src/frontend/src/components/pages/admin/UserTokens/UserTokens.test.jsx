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
      checkPermission: jest.fn()
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
  saveButton: () => screen.queryByText(SAVE_BUTTON_LABEL),
  discardButton: () => screen.getByText(DISCARD_BUTTON_LABEL),
  resetButton: () => screen.queryByText(RESET_ALL_TOKENS_BUTTON),
  readOnlyWarning: () => screen.getByText(READ_ONLY_WARNING),
  confirmation: {
    modal: () => screen.queryByRole('dialog'),
    cancelButton: () => within(selectors.confirmation.modal()).getByRole('button', {name: CANCEL_BUTTON_LABEL}),
    submitButton: () => within(selectors.confirmation.modal()).getByRole('button', {name: RESET_CONFIRMATION.CAPTION}),
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
          protectContent: false
        }
      });
    ExtJS.checkPermission.mockReturnValue(true);
  });

  it('renders default form and enables/disables fields correctly', async () => {
    const {
      userTokensCheckbox,
      repositoryUserTokensCheckbox,
      saveButton,
      discardButton,
      resetButton
    } = selectors;

    await renderView();

    expect(userTokensCheckbox()).not.toBeChecked();
    expect(repositoryUserTokensCheckbox()).not.toBeChecked();
    expect(repositoryUserTokensCheckbox()).toBeDisabled();
    expect(discardButton()).toHaveClass('disabled');
    expect(resetButton()).not.toBeInTheDocument();

    userEvent.click(userTokensCheckbox());

    expect(repositoryUserTokensCheckbox()).not.toBeChecked();
    expect(repositoryUserTokensCheckbox()).toBeEnabled();
    expect(discardButton()).not.toHaveClass('disabled');
    expect(resetButton()).not.toBeInTheDocument();

    userEvent.click(userTokensCheckbox());

    expect(repositoryUserTokensCheckbox()).not.toBeChecked();
    expect(repositoryUserTokensCheckbox()).toBeDisabled();
    expect(discardButton()).toHaveClass('disabled');
    expect(resetButton()).not.toBeInTheDocument();
  });

  it('discards changes', async () => {
    const {userTokensCheckbox, repositoryUserTokensCheckbox, discardButton} = selectors;

    await renderView();

    userEvent.click(userTokensCheckbox());
    userEvent.click(repositoryUserTokensCheckbox());

    expect(userTokensCheckbox()).toBeChecked();
    expect(repositoryUserTokensCheckbox()).toBeChecked();

    userEvent.click(discardButton());

    expect(userTokensCheckbox()).not.toBeChecked();
    expect(repositoryUserTokensCheckbox()).not.toBeChecked();
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
        protectContent: true
      })
    );
  });

  it('sends correct data to API: disable', async () => {
    const {userTokensCheckbox, repositoryUserTokensCheckbox, saveButton} = selectors;

    when(Axios.get)
      .calledWith(API_URL)
      .mockResolvedValueOnce({
        data: {
          enabled: true,
          protectContent: true
        }
      });

    await renderView();

    expect(userTokensCheckbox()).toBeChecked();
    expect(repositoryUserTokensCheckbox()).toBeChecked();

    userEvent.click(userTokensCheckbox());
    userEvent.click(saveButton());

    await waitFor(() =>
      expect(Axios.put).toBeCalledWith(API_URL, {
        enabled: false,
        protectContent: false
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
          protectContent: false
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

    userEvent.click(submitButton());

    await waitFor(() => expect(Axios.delete).toBeCalledWith(API_URL));

    expect(modal()).not.toBeInTheDocument();
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
