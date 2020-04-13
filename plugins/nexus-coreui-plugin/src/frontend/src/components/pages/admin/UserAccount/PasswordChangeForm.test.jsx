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
import {fireEvent, render, wait} from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';
import PasswordChangeForm from './PasswordChangeForm';
import UIStrings from '../../../../constants/UIStrings';
import Axios from 'axios';

jest.mock('nexus-ui-plugin', () => {
  return {
    ...jest.requireActual('nexus-ui-plugin'),
    ExtJS: {
      showSuccessMessage: jest.fn(),
      fetchAuthenticationToken: jest.fn(() => Promise.resolve({data: 'fakeToken'})),
    },
  };
});

jest.mock('axios', () => {
  return {
    put: jest.fn(() => Promise.resolve()),
  };
});

describe('PasswordChangeForm', () => {
  const renderView = async (view) => {
    let selectors = {};
    await act(async () => {
      let {container, getByText, getByLabelText} = render(view);
      selectors = {
        container,
        passwordCurrent: () => getByLabelText(UIStrings.USER_ACCOUNT.PASSWORD_CURRENT_FIELD_LABEL),
        passwordNew: () => getByLabelText(UIStrings.USER_ACCOUNT.PASSWORD_NEW_FIELD_LABEL),
        passwordNewConfirm: () => getByLabelText(UIStrings.USER_ACCOUNT.PASSWORD_NEW_CONFIRM_FIELD_LABEL),
        changePasswordButton: () => getByText(UIStrings.USER_ACCOUNT.ACTIONS.changePassword),
        discardButton: () => getByText(UIStrings.USER_ACCOUNT.ACTIONS.discardChangePassword),
      };
    });
    return selectors;
  };

  it('renders correctly', async () => {
    let {container, passwordCurrent, passwordNew, passwordNewConfirm, changePasswordButton, discardButton} =
        await renderView(<PasswordChangeForm userId='admin'/>);

    expect(container).toMatchSnapshot();
    expect(passwordCurrent()).toHaveValue('');
    expect(passwordNew()).toHaveValue('');
    expect(passwordNewConfirm()).toHaveValue('');
    expect(changePasswordButton()).not.toBeEnabled();
    expect(discardButton()).not.toBeEnabled();
  });

  it('prevents password change when new matches current', async () => {
    let {passwordCurrent, passwordNew, passwordNewConfirm, changePasswordButton, discardButton} =
        await renderView(<PasswordChangeForm userId='admin'/>);

    fireEvent.change(passwordCurrent(), {target: {value: 'foobar'}});
    await wait(() => expect(passwordCurrent()).toHaveValue('foobar'));

    fireEvent.change(passwordNew(), {target: {value: 'foobar'}});
    await wait(() => expect(passwordNew()).toHaveValue('foobar'));

    fireEvent.change(passwordNewConfirm(), {target: {value: 'foobar'}});
    await wait(() => expect(passwordNewConfirm()).toHaveValue('foobar'));

    expect(changePasswordButton()).not.toBeEnabled();
    expect(discardButton()).toBeEnabled();
  });

  it('prevents password change when new does not match confirm', async () => {
    let {passwordCurrent, passwordNew, passwordNewConfirm, changePasswordButton, discardButton} =
        await renderView(<PasswordChangeForm userId='admin'/>);

    fireEvent.change(passwordCurrent(), {target: {value: 'foobar'}});
    await wait(() => expect(passwordCurrent()).toHaveValue('foobar'));

    fireEvent.change(passwordNew(), {target: {value: 'bazzle'}});
    await wait(() => expect(passwordNew()).toHaveValue('bazzle'));

    fireEvent.change(passwordNewConfirm(), {target: {value: 'bazzl'}});
    await wait(() => expect(passwordNewConfirm()).toHaveValue('bazzl'));

    expect(changePasswordButton()).not.toBeEnabled();
    expect(discardButton()).toBeEnabled();
  });

  it('sends the correct password change request', async () => {
    let {passwordCurrent, passwordNew, passwordNewConfirm, changePasswordButton, discardButton} =
        await renderView(<PasswordChangeForm userId='admin'/>);

    fireEvent.change(passwordCurrent(), {target: {value: 'foobar'}});
    await wait(() => expect(passwordCurrent()).toHaveValue('foobar'));

    fireEvent.change(passwordNew(), {target: {value: 'bazzle'}});
    await wait(() => expect(passwordNew()).toHaveValue('bazzle'));

    fireEvent.change(passwordNewConfirm(), {target: {value: 'bazzle'}});
    await wait(() => expect(passwordNewConfirm()).toHaveValue('bazzle'));

    expect(changePasswordButton()).toBeEnabled();
    expect(discardButton()).toBeEnabled();

    await act(async () => fireEvent.click(changePasswordButton()));

    expect(Axios.put).toHaveBeenCalledTimes(1);
    expect(Axios.put).toHaveBeenCalledWith(
        `/service/rest/internal/ui/user/admin/password`,
        {
          authToken: 'fakeToken',
          password: 'bazzle',
        }
    );
  });

  it('resets the form on discard', async () => {
    let {passwordCurrent, passwordNew, passwordNewConfirm, changePasswordButton, discardButton} =
        await renderView(<PasswordChangeForm userId='admin'/>);

    fireEvent.change(passwordCurrent(), {target: {value: 'foobar'}});
    await wait(() => expect(passwordCurrent()).toHaveValue('foobar'));

    fireEvent.change(passwordNew(), {target: {value: 'bazzle'}});
    await wait(() => expect(passwordNew()).toHaveValue('bazzle'));

    fireEvent.change(passwordNewConfirm(), {target: {value: 'bazzle'}});
    await wait(() => expect(passwordNewConfirm()).toHaveValue('bazzle'));

    expect(changePasswordButton()).toBeEnabled();
    expect(discardButton()).toBeEnabled();

    await act(async () => fireEvent.click(discardButton()));

    expect(passwordCurrent()).toHaveValue('');
    expect(passwordNew()).toHaveValue('');
    expect(passwordNewConfirm()).toHaveValue('');
    expect(changePasswordButton()).not.toBeEnabled();
    expect(discardButton()).not.toBeEnabled();
  });
});
