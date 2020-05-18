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
import { act } from 'react-dom/test-utils';
import {fireEvent, render, wait} from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';
import TestUtils from 'nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import Axios from 'axios';
import AnonymousSettings from './AnonymousSettings';
import UIStrings from '../../../../constants/UIStrings';

const mockRealmTypes = [
  {id: 'r1', name: 'Realm One'},
  {id: 'r2', name: 'Realm Two'}
];
const mockAnonymousSettings = {
  enabled: true,
  userId: 'testUser',
  realmName: 'r2'
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

jest.mock('axios', () => {  // Mock out parts of axios, has to be done in same scope as import statements
  return {
    ...jest.requireActual('axios'), // Use most functions from actual axios
    get: jest.fn((url) => {
      switch (url) {
        case '/service/rest/internal/ui/realms/types':
          return Promise.resolve({data: mockRealmTypes});
        case '/service/rest/internal/ui/anonymous-settings':
          return Promise.resolve({data: mockAnonymousSettings});
      }
    }),
    put: jest.fn(() => Promise.resolve())
  };
});

describe('AnonymousSettings', () => {
  beforeEach(() => {
    window.dirty = [];
  });

  afterEach(() => {
    window.dirty = [];
  });

  function renderView(view) {
    return TestUtils.render(view, ({getByLabelText, getByText}) => ({
      enabledField: () => getByLabelText(UIStrings.ANONYMOUS_SETTINGS.ENABLED_CHECKBOX_DESCRIPTION),
      userIdField: () => getByLabelText(UIStrings.ANONYMOUS_SETTINGS.USERNAME_TEXTFIELD_LABEL),
      realmField: () => getByLabelText(UIStrings.ANONYMOUS_SETTINGS.REALM_SELECT_LABEL),
      saveButton: () => getByText(UIStrings.SETTINGS.SAVE_BUTTON_LABEL),
      discardButton: () => getByText(UIStrings.SETTINGS.DISCARD_BUTTON_LABEL)
    }));
  }

  it('renders correctly', async () => {
    let {container, loadingMask} = renderView(<AnonymousSettings/>);

    await wait(() => expect(loadingMask()).not.toBeInTheDocument());

    expect(container).toMatchSnapshot();
  });

  it('fetches the values of fields from the API and updates them as expected', async () => {
    let {
      loadingMask, enabledField, userIdField, realmField, saveButton, discardButton
    } = renderView(<AnonymousSettings/>);

    await wait(() => expect(loadingMask()).not.toBeInTheDocument());

    expect(Axios.get).toHaveBeenCalledTimes(2);
    expect(enabledField()).toBeChecked();
    expect(userIdField()).toHaveValue('testUser');
    expect(realmField()).toHaveValue('r2');
    expect(saveButton()).toBeDisabled();
    expect(discardButton()).toBeDisabled();
  });

  it('Sends changes to the API on save', async () => {
    let {
      loadingMask, enabledField, userIdField, realmField, saveButton, discardButton
    } = renderView(<AnonymousSettings/>);

    await wait(() => expect(loadingMask()).not.toBeInTheDocument());

    fireEvent.click(enabledField());
    await wait(() => expect(enabledField()).not.toBeChecked());

    fireEvent.change(userIdField(), {target: {value: 'changed-username'}});
    await wait(() => expect(userIdField()).toHaveValue());

    fireEvent.change(realmField(), {target: {value: 'r1'}});
    await wait(() => expect(realmField()).toHaveValue('r1'));

    expect(saveButton()).toBeEnabled();
    expect(discardButton()).toBeEnabled();

    expect(Axios.put).toHaveBeenCalledTimes(0);

    await act(async () => fireEvent.click(saveButton()));

    expect(Axios.put).toHaveBeenCalledTimes(1);
    expect(Axios.put).toHaveBeenCalledWith(
        '/service/rest/internal/ui/anonymous-settings',
        {
          enabled: false,
          userId: 'changed-username',
          realmName: 'r1'
        }
    );

    expect(saveButton()).not.toBeEnabled();
    expect(discardButton()).not.toBeEnabled();
  });

  it('Resets the form on discard', async () => {
    let {
      loadingMask, userIdField, saveButton, discardButton
    } = renderView(<AnonymousSettings/>);

    await wait(() => expect(loadingMask()).not.toBeInTheDocument());

    fireEvent.change(userIdField(), {target: {value: ''}})
    await wait(() => expect(userIdField()).toHaveValue(''));

    expect(saveButton()).not.toBeEnabled();
    expect(discardButton()).toBeEnabled();

    fireEvent.click(discardButton());

    expect(userIdField()).toHaveValue('testUser');
    expect(saveButton()).not.toBeEnabled();
    expect(discardButton()).not.toBeEnabled();
  });

  it('Sets the dirty flag appropriately', async () => {
    let {
      loadingMask, userIdField, discardButton
    } = renderView(<AnonymousSettings/>);

    await wait(() => expect(loadingMask()).not.toBeInTheDocument());

    expect(window.dirty).toEqual([]);

    fireEvent.change(userIdField(), {target: {value: 'anonymous'}})
    await wait(() => expect(userIdField()).toHaveValue('anonymous'));

    expect(window.dirty).toEqual(['AnonymousSettings']);

    fireEvent.click(discardButton());

    expect(window.dirty).toEqual([]);
  });
});
