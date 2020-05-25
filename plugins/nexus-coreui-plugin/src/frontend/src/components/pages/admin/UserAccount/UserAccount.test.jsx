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
import TestUtils from 'nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import Axios from 'axios';
import UserAccount from './UserAccount';
import UIStrings from '../../../../constants/UIStrings';

const mockUserAccount = {
  userId: 'admin',
  firstName: 'User',
  lastName: 'Admin',
  email: 'admin@example.com',
  external: false
};

const mockExternalUserAccount = {
  userId: 'externalUser',
  firstName: 'External',
  lastName: 'User',
  email: 'externalUser@saml.com',
  external: true
};

jest.mock('nexus-ui-plugin', () => {
  return {
    ...jest.requireActual('nexus-ui-plugin'),
    ExtJS: {
      showSuccessMessage: jest.fn(),
      showErrorMessage: jest.fn(),
      setDirtyStatus: jest.requireActual('nexus-ui-plugin').ExtJS.setDirtyStatus
    }
  }
});

jest.mock('axios', () => {
  return {
    ...jest.requireActual('axios'),
    get: jest.fn((url) => {
      if (url === '/service/rest/internal/ui/user') {
        return Promise.resolve({data: mockUserAccount});
      }
    }),
    put: jest.fn(() => Promise.resolve())
  };
});


describe('UserAccount', () => {
  beforeEach(() => {
    window.dirty = [];
  });

  afterEach(() => {
    window.dirty = [];
  });

  function renderView(view) {
    return TestUtils.render(view, ({getByLabelText, getByText}) => ({
      userIdField: () => getByLabelText(UIStrings.USER_ACCOUNT.ID_FIELD_LABEL),
      firstNameField: () => getByLabelText(UIStrings.USER_ACCOUNT.FIRST_FIELD_LABEL),
      lastNameField: () => getByLabelText(UIStrings.USER_ACCOUNT.LAST_FIELD_LABEL),
      emailField: () => getByLabelText(UIStrings.USER_ACCOUNT.EMAIL_FIELD_LABEL),
      saveButton: () => getByText(UIStrings.SETTINGS.SAVE_BUTTON_LABEL),
      discardButton: () => getByText(UIStrings.SETTINGS.SAVE_BUTTON_LABEL)
    }));
  }

  it('renders correctly', async () => {
    let {container, loadingMask} = renderView(<UserAccount/>);

    await wait(() => expect(loadingMask()).not.toBeInTheDocument());

    expect(container).toMatchSnapshot();
  });

  it('renders correctly for an external user', async () => {
    Axios.get.mockImplementationOnce(() => Promise.resolve({data: mockExternalUserAccount}));

    let {
      container, loadingMask, userIdField, firstNameField, lastNameField, emailField,
      saveButton, discardButton
    } = renderView(<UserAccount/>);

    await wait(() => expect(loadingMask()).not.toBeInTheDocument());

    expect(userIdField().hasAttribute('readonly','true')).toBe(true);
    expect(firstNameField().hasAttribute('readonly','true')).toBe(true);
    expect(lastNameField().hasAttribute('readonly','true')).toBe(true);
    expect(emailField().hasAttribute('readonly','true')).toBe(true);

    expect(saveButton()).toBeDisabled();
    expect(discardButton()).toBeDisabled();

    expect(container).toMatchSnapshot('externalUser');
  });

  it('fetches the values of fields from the API and updates them as expected', async () => {
    let {
      loadingMask, userIdField, firstNameField, lastNameField, emailField,
        saveButton, discardButton
    } = renderView(<UserAccount/>);

    await wait(() => expect(loadingMask()).not.toBeInTheDocument());

    expect(Axios.get).toHaveBeenCalledTimes(1);
    expect(userIdField()).toHaveValue('admin');
    expect(firstNameField()).toHaveValue('User');
    expect(lastNameField()).toHaveValue('Admin');
    expect(emailField()).toHaveValue('admin@example.com');
    expect(saveButton()).toBeDisabled();
    expect(discardButton()).toBeDisabled();
  });

  it('Sends changes to the API on save', async () => {
    let {
      loadingMask, lastNameField, saveButton, discardButton
    } = renderView(<UserAccount/>);

    await wait(() => expect(loadingMask()).not.toBeInTheDocument());

    fireEvent.change(lastNameField(), {target: {value: 'FooBar'}});
    await wait(() => expect(lastNameField()).toHaveValue('FooBar'));

    expect(saveButton()).toBeEnabled();
    expect(discardButton()).toBeEnabled();

    expect(Axios.put).toHaveBeenCalledTimes(0);

    await act(async () => fireEvent.click(saveButton()));

    expect(Axios.put).toHaveBeenCalledTimes(1);
    expect(Axios.put).toHaveBeenCalledWith(
        '/service/rest/internal/ui/user',
        {
          email: 'admin@example.com',
          firstName: 'User',
          lastName: 'FooBar',
          userId: 'admin',
          external: false
        }
    );

    expect(saveButton()).not.toBeEnabled();
    expect(discardButton()).not.toBeEnabled();
  });

  it('Resets the form on discard', async () => {
    let {
      loadingMask, lastNameField, saveButton, discardButton
    } = renderView(<UserAccount/>);

    await wait(() => expect(loadingMask()).not.toBeInTheDocument());

    fireEvent.change(lastNameField(), {target: {value: 'FooBar'}});
    await wait(() => expect(lastNameField()).toHaveValue('FooBar'));

    expect(saveButton()).toBeEnabled();
    expect(discardButton()).toBeEnabled();

    fireEvent.click(discardButton());

    await wait(() => expect(lastNameField()).toHaveValue('Admin'));
    expect(saveButton()).not.toBeEnabled();
    expect(discardButton()).not.toBeEnabled();
  });

  it('Sets the dirty flag appropriately', async () => {
    let {
      loadingMask, lastNameField, discardButton
    } = renderView(<UserAccount/>);

    await wait(() => expect(loadingMask()).not.toBeInTheDocument());

    expect(window.dirty).toEqual([]);

    fireEvent.change(lastNameField(), {target: {value: 'FooBar'}});
    await wait(() => expect(lastNameField()).toHaveValue('FooBar'));

    expect(window.dirty).toEqual(['UserAccount']);

    fireEvent.click(discardButton());

    await wait(() => expect(window.dirty).toEqual([]));
  });
});
