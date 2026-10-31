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
import {act} from 'react-dom/test-utils';
import {
  fireEvent,
  waitFor,
  waitForElementToBeRemoved,
  render,
  screen
} from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import {ExtJS, APIConstants} from '@sonatype/nexus-ui-plugin';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import AnonymousSettings from './AnonymousSettings';
import UIStrings from '../../../../constants/UIStrings';
import axios from "axios";
import {when} from "jest-when";

const {
  ANONYMOUS_SETTINGS: ANONYMOUS_API
} = APIConstants.REST.INTERNAL;

const mockRealmTypes = [
  {id: 'r1', name: 'Realm One'},
  {id: 'r2', name: 'Realm Two'}
];
const mockAnonymousSettings = {
  enabled: true,
  userId: 'testUser',
  realmName: 'r2'
};

jest.mock('@sonatype/nexus-ui-plugin', () => {
  return {
    ...jest.requireActual('@sonatype/nexus-ui-plugin'),
    ExtJS: {
      showSuccessMessage: jest.fn(),
      showErrorMessage: jest.fn(),
      setDirtyStatus: jest.requireActual('@sonatype/nexus-ui-plugin').ExtJS.setDirtyStatus,
      checkPermission: jest.fn().mockReturnValue(true),
      useUser: jest.fn(() => ({ name: 'test-user' })),
      waitForPermissions: jest.fn().mockResolvedValue(undefined),
    }
  }
});

const selectors = {
  ...TestUtils.formSelectors,
  ...TestUtils.selectors,
  enabledField: () => screen.getByLabelText(UIStrings.ANONYMOUS_SETTINGS.ENABLED_CHECKBOX_DESCRIPTION),
  userIdField: () => screen.getByLabelText(UIStrings.ANONYMOUS_SETTINGS.USERNAME_TEXTFIELD_LABEL),
  realmField: () => screen.getByLabelText(UIStrings.ANONYMOUS_SETTINGS.REALM_SELECT_LABEL),
  saveButton: () => screen.getByText(UIStrings.SETTINGS.SAVE_BUTTON_LABEL),
  discardButton: () => screen.getByText(UIStrings.SETTINGS.DISCARD_BUTTON_LABEL)
};

describe('AnonymousSettings', () => {
  beforeEach(() => {
    window.dirty = [];
    when(axios.get).calledWith('service/rest/internal/ui/realms/types').mockResolvedValue({
      data: mockRealmTypes
    });
    when(axios.get).calledWith('service/rest/internal/ui/anonymous-settings').mockResolvedValue({
      data: mockAnonymousSettings
    });
    axios.put.mockResolvedValue();
  });

  afterEach(() => {
    window.dirty = [];
  });

  async function renderView() {
    const view = render(<AnonymousSettings/>);
    await waitForElementToBeRemoved(selectors.queryLoadingMask())
    return view;
  }

  it('fetches the values of fields from the API and updates them as expected', async () => {
    let {
      enabledField, userIdField, realmField, discardButton, saveButton
    } = selectors;

    await renderView();

    expect(Axios.get).toHaveBeenCalledTimes(2);
    expect(enabledField()).toBeChecked();
    expect(userIdField()).toHaveValue('testUser');
    expect(realmField()).toHaveValue('r2');

    userEvent.click(saveButton());
    expect(selectors.queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();
    expect(discardButton()).toHaveClass('disabled');
  });

  it('Sends changes to the API on save', async () => {
    let {
      enabledField, userIdField, realmField, saveButton, discardButton
    } = selectors;

    await renderView();

    userEvent.click(enabledField());
    await waitFor(() => expect(enabledField()).not.toBeChecked());

    fireEvent.change(userIdField(), {target: {value: 'changed-username'}});
    await waitFor(() => expect(userIdField()).toHaveValue());

    fireEvent.change(realmField(), {target: {value: 'r1'}});
    await waitFor(() => expect(realmField()).toHaveValue('r1'));

    expect(saveButton()).toBeEnabled();
    expect(discardButton()).toBeEnabled();
    expect(selectors.queryFormError(TestUtils.NO_CHANGES_MESSAGE)).not.toBeInTheDocument();

    expect(Axios.put).toHaveBeenCalledTimes(0);

    await act(async () => userEvent.click(saveButton()));

    expect(Axios.put).toHaveBeenCalledTimes(1);
    expect(Axios.put).toHaveBeenCalledWith(
        'service/rest/internal/ui/anonymous-settings',
        {
          enabled: false,
          userId: 'changed-username',
          realmName: 'r1'
        }
    );

    userEvent.click(saveButton());
    expect(selectors.queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();
    expect(discardButton()).toHaveClass('disabled');
  });

  it('Resets the form on discard', async () => {
    let {
      userIdField, discardButton
    } = selectors;

    await renderView();

    fireEvent.change(userIdField(), {target: {value: ''}})
    await waitFor(() => expect(userIdField()).toHaveValue(''));
    expect(userIdField()).toHaveAccessibleErrorMessage(TestUtils.REQUIRED_MESSAGE);
    expect(discardButton()).toBeEnabled();

    userEvent.click(discardButton());
    expect(userIdField()).toHaveValue('testUser');
    expect(discardButton()).toHaveClass('disabled');
  });

  it('Sets the dirty flag appropriately', async () => {
    let {
      userIdField, discardButton
    } = selectors;

    await renderView();

    expect(window.dirty).toEqual([]);

    fireEvent.change(userIdField(), {target: {value: 'anonymous'}})
    await waitFor(() => expect(userIdField()).toHaveValue('anonymous'));

    expect(window.dirty).toEqual(['AnonymousSettingsForm']);

    userEvent.click(discardButton());

    expect(window.dirty).toEqual([]);
  });

  it('Shows page in Read Only mode', async () => {
    ExtJS.checkPermission.mockReturnValueOnce(false);
    const dataClass = 'nx-read-only__data';
    const labelClass = 'nx-read-only__label';

    await renderView();

    expect(screen.getByText(UIStrings.SETTINGS.READ_ONLY.WARNING)).toBeInTheDocument();

    expect(screen.getByText(UIStrings.ANONYMOUS_SETTINGS.ENABLED_CHECKBOX_LABEL)).toHaveClass(labelClass);
    expect(screen.getByText('Enabled')).toHaveClass(dataClass);
    expect(screen.getByText(UIStrings.ANONYMOUS_SETTINGS.USERNAME_TEXTFIELD_LABEL)).toHaveClass(labelClass);
    expect(screen.getByText('testUser')).toHaveClass(dataClass);
    expect(screen.getByText(UIStrings.ANONYMOUS_SETTINGS.REALM_SELECT_LABEL)).toHaveClass(labelClass);
    expect(screen.getByText('Realm Two')).toHaveClass(dataClass);
  });

  it('Removes userId trailing spaces', async () => {
    await renderView();

    await TestUtils.changeField(selectors.userIdField, ' ' + mockAnonymousSettings.userId + ' ');

    userEvent.click(selectors.querySubmitButton());

    await waitFor(() => expect(Axios.put).toBeCalledWith(ANONYMOUS_API, mockAnonymousSettings));
  });
});
