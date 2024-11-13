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
import {fireEvent, waitFor, waitForElementToBeRemoved, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import UIStrings from '../../../../constants/UIStrings';
import UserAccount from './UserAccount';

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

jest.mock('@sonatype/nexus-ui-plugin', () => {
  return {
    ...jest.requireActual('@sonatype/nexus-ui-plugin'),
    ExtJS: {
      showSuccessMessage: jest.fn(),
      showErrorMessage: jest.fn(),
      setDirtyStatus: jest.requireActual('@sonatype/nexus-ui-plugin').ExtJS.setDirtyStatus
    }
  }
});

jest.mock('axios', () => {
  return {
    ...jest.requireActual('axios'),
    get: jest.fn((url) => {
      if (url === 'service/rest/internal/ui/user') {
        return Promise.resolve({data: mockUserAccount});
      }
    }),
    put: jest.fn(() => Promise.resolve())
  };
});

const selectors = {
  ...TestUtils.formSelectors
};

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
      discardButton: () => getByText(UIStrings.SETTINGS.DISCARD_BUTTON_LABEL)
    }));
  }

  it('renders correctly for an external user', async () => {
    Axios.get.mockImplementationOnce(() => Promise.resolve({data: mockExternalUserAccount}));

    let {
      loadingMask,
      userIdField,
      firstNameField,
      lastNameField,
      emailField,
      discardButton
    } = renderView(<UserAccount/>);

    await waitForElementToBeRemoved(loadingMask);

    expect(userIdField()).toBeDisabled();
    expect(firstNameField()).toBeDisabled();
    expect(lastNameField()).toBeDisabled();
    expect(emailField()).toBeDisabled();

    expect(discardButton()).toHaveClass('disabled');
  });

  it('fetches the values of fields from the API and updates them as expected', async () => {
    let {loadingMask, userIdField, firstNameField, lastNameField, emailField, discardButton} = renderView(<UserAccount/>);

    await waitForElementToBeRemoved(loadingMask);

    expect(Axios.get).toHaveBeenCalledTimes(1);
    expect(userIdField()).toHaveValue('admin');
    expect(firstNameField()).toHaveValue('User');
    expect(lastNameField()).toHaveValue('Admin');
    expect(emailField()).toHaveValue('admin@example.com');

    expect(screen.queryAllByText(`${TestUtils.THERE_WERE_ERRORS} ${TestUtils.NO_CHANGES_MESSAGE}`)).toHaveLength(2);
    expect(discardButton()).toHaveClass('disabled');
  });

  it('Sends changes to the API on save', async () => {
    let {
      loadingMask, lastNameField, saveButton, discardButton
    } = renderView(<UserAccount/>);

    await waitForElementToBeRemoved(loadingMask);

    fireEvent.change(lastNameField(), {target: {value: 'FooBar'}});
    await waitFor(() => expect(lastNameField()).toHaveValue('FooBar'));

    expect(screen.queryAllByText(`${TestUtils.THERE_WERE_ERRORS} ${TestUtils.NO_CHANGES_MESSAGE}`)).toHaveLength(1);
    expect(discardButton()).not.toHaveClass('disabled');

    expect(Axios.put).toHaveBeenCalledTimes(0);

    userEvent.click(saveButton());
    await waitFor(() =>
        expect(screen.queryAllByText(`${TestUtils.THERE_WERE_ERRORS} ${TestUtils.NO_CHANGES_MESSAGE}`)).toHaveLength(1)
    );
    expect(discardButton()).toHaveClass('disabled');

    expect(Axios.put).toHaveBeenCalledTimes(1);
    expect(Axios.put).toHaveBeenCalledWith(
        'service/rest/internal/ui/user',
        {
          email: 'admin@example.com',
          firstName: 'User',
          lastName: 'FooBar',
          userId: 'admin',
          external: false
        }
    );
  });

  it('Resets the form on discard', async () => {
    let {
      loadingMask, lastNameField, saveButton, discardButton
    } = renderView(<UserAccount/>);

    await waitForElementToBeRemoved(loadingMask);

    fireEvent.change(lastNameField(), {target: {value: 'FooBar'}});
    await waitFor(() => expect(lastNameField()).toHaveValue('FooBar'));

    expect(saveButton()).not.toHaveClass('disabled');
    expect(discardButton()).not.toHaveClass('disabled');

    userEvent.click(discardButton());
    await waitFor(() => expect(discardButton()).toHaveClass('disabled'));

    expect(lastNameField()).toHaveValue('Admin');
    expect(screen.queryAllByText(`${TestUtils.THERE_WERE_ERRORS} ${TestUtils.NO_CHANGES_MESSAGE}`)).toHaveLength(2)
    expect(discardButton()).toHaveClass('disabled');
  });

  it('Sets the dirty flag appropriately', async () => {
    let {
      loadingMask, lastNameField, discardButton
    } = renderView(<UserAccount/>);

    await waitForElementToBeRemoved(loadingMask);

    expect(window.dirty).toEqual([]);

    fireEvent.change(lastNameField(), {target: {value: 'FooBar'}});
    await waitFor(() => expect(lastNameField()).toHaveValue('FooBar'));

    expect(window.dirty).toEqual(['UserAccount']);

    userEvent.click(discardButton());

    await waitFor(() => expect(window.dirty).toEqual([]));
  });
});
