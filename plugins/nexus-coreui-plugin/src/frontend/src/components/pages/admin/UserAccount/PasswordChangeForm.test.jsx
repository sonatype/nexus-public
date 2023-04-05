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
import Axios from 'axios';
import {waitFor} from '@testing-library/react';
import {act} from 'react-dom/test-utils';
import userEvent from '@testing-library/user-event';

import {ExtJS} from '@sonatype/nexus-ui-plugin';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import UIStrings from '../../../../constants/UIStrings';
import PasswordChangeForm from './PasswordChangeForm';

jest.mock('@sonatype/nexus-ui-plugin', () => {
  return {
    ...jest.requireActual('@sonatype/nexus-ui-plugin'),
    ExtJS: {
      showSuccessMessage: jest.fn(),
      showErrorMessage: jest.fn(),
      fetchAuthenticationToken: jest.fn(() => Promise.resolve({data: 'fakeToken'})),
    }
  };
});

jest.mock('axios', () => {
  return {
    put: jest.fn(() => Promise.resolve()),
  };
});

const selectors = {
  ...TestUtils.formSelectors,
};

describe('PasswordChangeForm', () => {
  const CHANGE_PASSWORD_URL = '/service/rest/internal/ui/user/admin/password';

  const render = () => TestUtils.render(<PasswordChangeForm userId="admin"/>, ({getByLabelText, getByText}) => ({
    passwordCurrent: () => getByLabelText(UIStrings.USER_ACCOUNT.PASSWORD_CURRENT_FIELD_LABEL),
    passwordNew: () => getByLabelText(UIStrings.USER_ACCOUNT.PASSWORD_NEW_FIELD_LABEL),
    passwordNewConfirm: () => getByLabelText(UIStrings.USER_ACCOUNT.PASSWORD_NEW_CONFIRM_FIELD_LABEL),
    changePasswordButton: () => getByText(UIStrings.USER_ACCOUNT.ACTIONS.changePassword),
    discardButton: () => getByText(UIStrings.USER_ACCOUNT.ACTIONS.discardChangePassword),
  }));

  it('renders correctly', async () => {
    let {passwordCurrent, passwordNew, passwordNewConfirm, discardButton} = render();

    expect(passwordCurrent()).toHaveValue('');
    expect(passwordNew()).toHaveValue('');
    expect(passwordNewConfirm()).toHaveValue('');
    expect(selectors.queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();
    expect(discardButton()).toHaveClass('disabled');
  });

  it('prevents password change when new matches current', async () => {
    let {passwordCurrent, passwordNew, passwordNewConfirm, discardButton} = render();

    await TestUtils.changeField(passwordCurrent, 'foobar');

    await TestUtils.changeField(passwordNew, 'foobar');

    await TestUtils.changeField(passwordNewConfirm, 'foobar');

    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();
    expect(discardButton()).not.toHaveClass('disabled');
  });

  it('prevents password change when new does not match confirm', async () => {
    let {passwordCurrent, passwordNew, passwordNewConfirm, discardButton} = render();

    await TestUtils.changeField(passwordCurrent, 'foobar');

    await TestUtils.changeField(passwordNew, 'bazzle');

    await TestUtils.changeField(passwordNewConfirm, 'bazzl');

    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();
    expect(discardButton()).not.toHaveClass('disabled');
  });

  it('sends the correct password change request', async () => {
    let {passwordCurrent, passwordNew, passwordNewConfirm, changePasswordButton, discardButton} = render();

    await TestUtils.changeField(passwordCurrent, 'foobar');

    await TestUtils.changeField(passwordNew, 'bazzle');

    await TestUtils.changeField(passwordNewConfirm, 'bazzle');

    expect(changePasswordButton()).not.toHaveClass('disabled');
    expect(discardButton()).not.toHaveClass('disabled');

    userEvent.click(changePasswordButton());

    await waitFor(() => expect(Axios.put).toHaveBeenCalledTimes(1));
    expect(Axios.put).toHaveBeenCalledWith(
        CHANGE_PASSWORD_URL,
        {
          authToken: 'fakeToken',
          password: 'bazzle',
        }
    );
    expect(passwordCurrent()).toHaveValue('');
    expect(passwordNew()).toHaveValue('');
    expect(passwordNewConfirm()).toHaveValue('');
    expect(selectors.queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();
    expect(discardButton()).toHaveClass('disabled');
  });

  it('shows password API validation error', async () => {
    const {passwordCurrent, passwordNew, passwordNewConfirm, changePasswordButton} = render();
    const apiValidationError = 'Passwords must contain an uppercase letter'

    await TestUtils.changeField(passwordCurrent, 'foobar');
    await TestUtils.changeField(passwordNew, 'bazzle');
    await TestUtils.changeField(passwordNewConfirm, 'bazzle');

    Axios.put.mockRejectedValue({
      response: {
        data: [
          {
            id: '*',
            message: apiValidationError
          }
        ]
      }
    });

    await act(async () => userEvent.click(changePasswordButton()));

    expect(ExtJS.showErrorMessage).toHaveBeenCalledWith(apiValidationError);
  });

  it('resets the form on discard', async () => {
    let {passwordCurrent, passwordNew, passwordNewConfirm, changePasswordButton, discardButton} = render();

    await TestUtils.changeField(passwordCurrent, 'foobar');

    await TestUtils.changeField(passwordNew, 'bazzle');

    await TestUtils.changeField(passwordNewConfirm, 'bazzle');

    expect(changePasswordButton()).not.toHaveClass('disabled');
    expect(discardButton()).not.toHaveClass('disabled');

    userEvent.click(discardButton());

    await waitFor(() => expect(passwordCurrent()).toHaveValue(''));
    expect(passwordNew()).toHaveValue('');
    expect(passwordNewConfirm()).toHaveValue('');

    expect(selectors.queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();
    expect(discardButton()).toHaveClass('disabled');
  });
});
