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
import {render, screen, waitForElementToBeRemoved, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom/extend-expect';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import {when} from 'jest-when';

import Axios from 'axios';
import IqServer from './IqServer';
import UIStrings from "../../../../constants/UIStrings";

jest.mock('@sonatype/nexus-ui-plugin', () => {
  return {
    ...jest.requireActual('@sonatype/nexus-ui-plugin'),
    ExtJS: {
      showSuccessMessage: jest.fn(),
      showErrorMessage: jest.fn(),
      setDirtyStatus: jest.requireActual('@sonatype/nexus-ui-plugin').ExtJS.setDirtyStatus,
      checkPermission: jest.fn().mockReturnValue(true),
    }
  }
});

jest.mock('axios', () => {  // Mock out parts of axios, has to be done in same scope as import statements
  return {
    ...jest.requireActual('axios'), // Use most functions from actual axios
    get: jest.fn(),
    put: jest.fn(),
    post: jest.fn()
  };
});

global.NX = {
  Permissions: {
    check: jest.fn(() => true)
  }
}

const selectors = {
  ...TestUtils.selectors,
  getEnabledCheckbox: () => screen.getByLabelText("Enable the use of IQ Server"),
  getUrlInput: () => screen.getByLabelText("IQ Server URL"),
  getAuthenticationMethodSelect: () => screen.getByLabelText("Authentication Method"),
  getUsernameInput: () => screen.queryByLabelText('Username'),
  getPasswordInput: () => screen.queryByLabelText('Password'),
  getConnectionTimeoutInput: () => screen.getByLabelText("Connection Timeout"),
  getPropertiesInput: () => screen.getByLabelText("Properties"),
  getShowIqServerLinkCheckbox: () => screen.getByLabelText(
      "Show IQ Server link in the Browse menu when the server is enabled"),
  getVerifyConnectionButton: () => screen.getByText('Verify Connection'),
  getDiscardButton: () => screen.getByText('Discard'),
  getSaveButton: () => screen.getByText('Save')
};

const DEFAULT_RESPONSE = {
  "enabled": false,
  "showLink": false,
  "url": null,
  "authenticationType": null,
  "username": null,
  "password": null,
  "useTrustStoreForUrl": false,
  "timeoutSeconds": null,
  "properties": null
};

describe('IqServer', () => {
  beforeEach(() => {
    window.dirty = [];
  });

  afterEach(() => {
    window.dirty = [];
  });

  it('fetches the empty settings from the backend and displays the empty form', async () => {
    when(Axios.get).calledWith('/service/rest/v1/iq').mockResolvedValue({
      data: DEFAULT_RESPONSE
    });

    render(<IqServer/>);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(selectors.getEnabledCheckbox()).not.toBeChecked();
    expect(selectors.getUrlInput()).toHaveValue("");
    expect(selectors.getAuthenticationMethodSelect()).toHaveValue("");
    expect(selectors.getConnectionTimeoutInput()).toHaveValue("");
    expect(selectors.getPropertiesInput()).toHaveValue("");
    expect(selectors.getShowIqServerLinkCheckbox()).not.toBeChecked();
  });

  it('fetches existing settings from the backend and displays them in the form', async () => {
    when(Axios.get).calledWith('/service/rest/v1/iq').mockResolvedValue({
      data: {
        "enabled": true,
        "showLink": true,
        "url": "http://example.com",
        "authenticationType": "USER",
        "username": "user",
        "password": "pass",
        "useTrustStoreForUrl": true,
        "timeoutSeconds": 100,
        "properties": "some\ntext"
      }
    });

    render(<IqServer/>);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(selectors.getEnabledCheckbox()).toBeChecked();
    expect(selectors.getUrlInput()).toHaveValue("http://example.com");
    expect(selectors.getAuthenticationMethodSelect()).toHaveValue("USER");
    expect(selectors.getConnectionTimeoutInput()).toHaveValue("100");
    expect(selectors.getPropertiesInput()).toHaveValue("some\ntext");
    expect(selectors.getShowIqServerLinkCheckbox()).toBeChecked();
  });

  it('enables the verify connection button when form is valid', async () => {
    when(Axios.get).calledWith('/service/rest/v1/iq').mockResolvedValue({
      data: DEFAULT_RESPONSE
    });

    render(<IqServer/>);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(selectors.getVerifyConnectionButton()).toHaveAttribute('disabled');

    await TestUtils.changeField(selectors.getUrlInput, 'http://example.com');
    userEvent.selectOptions(selectors.getAuthenticationMethodSelect(), 'PKI');
    expect(selectors.getVerifyConnectionButton()).not.toHaveAttribute('disabled');

    userEvent.selectOptions(selectors.getAuthenticationMethodSelect(), 'USER');
    expect(selectors.getVerifyConnectionButton()).toHaveAttribute('disabled');

    await TestUtils.changeField(selectors.getUsernameInput, 'admin');
    await TestUtils.changeField(selectors.getPasswordInput, 'password');
    expect(selectors.getVerifyConnectionButton()).not.toHaveAttribute('disabled');
  });

  it('disables the save button when the form is invalid', async () => {
    when(Axios.get).calledWith('/service/rest/v1/iq').mockResolvedValue({
      data: DEFAULT_RESPONSE
    });

    render(<IqServer/>);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(selectors.getSaveButton()).toHaveAttribute('aria-disabled', 'true');
    expect(selectors.getSaveButton()).toHaveAttribute('aria-label', 'Submit disabled: There are no changes');

    userEvent.click(selectors.getEnabledCheckbox());

    expect(selectors.getEnabledCheckbox()).toBeChecked();
    expect(selectors.getSaveButton()).toHaveAttribute('aria-disabled', 'true');
    expect(selectors.getSaveButton()).toHaveAttribute('aria-label', 'Submit disabled: Validation errors are present');
  });

  it('enables the save button when the form is valid', async () => {
    when(Axios.get).calledWith('/service/rest/v1/iq').mockResolvedValue({
      data: DEFAULT_RESPONSE
    });

    render(<IqServer/>);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    await TestUtils.changeField(selectors.getUrlInput, 'http://example.com');
    userEvent.selectOptions(selectors.getAuthenticationMethodSelect(), 'PKI');

    expect(selectors.getSaveButton()).not.toHaveAttribute('aria-disabled', 'true');
  });

  it('requires the usename and password for the USER authentication method', async () => {
    when(Axios.get).calledWith('/service/rest/v1/iq').mockResolvedValue({
      data: DEFAULT_RESPONSE
    });

    render(<IqServer/>);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    await TestUtils.changeField(selectors.getUrlInput, 'http://example.com');
    userEvent.selectOptions(selectors.getAuthenticationMethodSelect(), 'USER');

    expect(selectors.getSaveButton()).toHaveAttribute('aria-disabled', 'true');
    expect(selectors.getSaveButton()).toHaveAttribute('aria-label', 'Submit disabled: Validation errors are present');

    await TestUtils.changeField(selectors.getUsernameInput, 'user');
    await TestUtils.changeField(selectors.getPasswordInput, 'pass');

    expect(selectors.getSaveButton()).not.toHaveAttribute('aria-disabled', 'true');
  });

  it('requires a connection timeout greater than 0, less than or equal to 3600', async () => {
    when(Axios.get).calledWith('/service/rest/v1/iq').mockResolvedValue({
      data: DEFAULT_RESPONSE
    });

    render(<IqServer/>);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.click(selectors.getEnabledCheckbox());
    expect(selectors.getEnabledCheckbox()).toBeChecked();

    await TestUtils.changeField(selectors.getUrlInput, 'http://example.com');

    userEvent.selectOptions(selectors.getAuthenticationMethodSelect(), 'PKI');

    await TestUtils.changeField(selectors.getConnectionTimeoutInput, '0');
    expect(selectors.getSaveButton()).toHaveAttribute('aria-disabled', 'true');
    expect(selectors.getSaveButton()).toHaveAttribute('aria-label', 'Submit disabled: Validation errors are present');

    userEvent.clear(selectors.getConnectionTimeoutInput());
    await TestUtils.changeField(selectors.getConnectionTimeoutInput, '3601');
    expect(selectors.getSaveButton()).toHaveAttribute('aria-disabled', 'true');
    expect(selectors.getSaveButton()).toHaveAttribute('aria-label', 'Submit disabled: Validation errors are present');

    userEvent.clear(selectors.getConnectionTimeoutInput());
    await TestUtils.changeField(selectors.getConnectionTimeoutInput, '1');
    expect(selectors.getSaveButton()).not.toHaveAttribute('aria-disabled', 'true');
    expect(selectors.getSaveButton()).not.toHaveAttribute('aria-label', 'Submit disabled: Validation errors are present');

    userEvent.clear(selectors.getConnectionTimeoutInput());
    await TestUtils.changeField(selectors.getConnectionTimeoutInput, '3600');
    expect(selectors.getSaveButton()).not.toHaveAttribute('aria-disabled', 'true');
    expect(selectors.getSaveButton()).not.toHaveAttribute('aria-label', 'Submit disabled: Validation errors are present');
  });

  it('discards changes', async () => {
    when(Axios.get).calledWith('/service/rest/v1/iq').mockResolvedValue({
      data: DEFAULT_RESPONSE
    });

    render(<IqServer/>);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.click(selectors.getEnabledCheckbox());
    expect(selectors.getEnabledCheckbox()).toBeChecked();

    await TestUtils.changeField(selectors.getUrlInput, 'http://example.com');

    userEvent.selectOptions(selectors.getAuthenticationMethodSelect(), 'USER');
    expect(selectors.getAuthenticationMethodSelect()).toHaveValue('USER');

    await TestUtils.changeField(selectors.getUsernameInput, 'user');
    await TestUtils.changeField(selectors.getPasswordInput, 'pass');
    await TestUtils.changeField(selectors.getConnectionTimeoutInput, '0');
    await TestUtils.changeField(selectors.getPropertiesInput, 'properties');

    userEvent.click(selectors.getShowIqServerLinkCheckbox());
    expect(selectors.getShowIqServerLinkCheckbox()).toBeChecked();

    userEvent.click(selectors.getDiscardButton());

    expect(selectors.getEnabledCheckbox()).not.toBeChecked();
    expect(selectors.getUrlInput()).toHaveValue('');
    expect(selectors.getAuthenticationMethodSelect()).toHaveValue('');
    expect(selectors.getUsernameInput()).not.toBeInTheDocument();
    expect(selectors.getPasswordInput()).not.toBeInTheDocument();
    expect(selectors.getConnectionTimeoutInput()).toHaveValue('');
    expect(selectors.getPropertiesInput()).toHaveValue('');

    userEvent.selectOptions(selectors.getAuthenticationMethodSelect(), 'USER');
    expect(selectors.getUsernameInput()).toHaveValue('');
    expect(selectors.getPasswordInput()).toHaveValue('');
  });

  it('runs api calls when click verify connection and save buttons', async () => {
    when(Axios.get).calledWith('/service/rest/v1/iq').mockResolvedValue({
      data: DEFAULT_RESPONSE
    });

    const simpleData = {
      authenticationType: 'USER',
      enabled: true,
      password: 'pass',
      properties: 'properties',
      showLink: false,
      timeoutSeconds: '1',
      url: 'https://example.com',
      useTrustStoreForUrl: false,
      username: 'user',
    };

    render(<IqServer/>);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.click(selectors.getEnabledCheckbox());
    expect(selectors.getEnabledCheckbox()).toBeChecked();

    await TestUtils.changeField(selectors.getUrlInput, simpleData.url);

    userEvent.selectOptions(selectors.getAuthenticationMethodSelect(), simpleData.authenticationType);
    expect(selectors.getAuthenticationMethodSelect()).toHaveValue(simpleData.authenticationType);

    await TestUtils.changeField(selectors.getUsernameInput, simpleData.username);
    await TestUtils.changeField(selectors.getPasswordInput, simpleData.password);
    await TestUtils.changeField(selectors.getConnectionTimeoutInput, simpleData.timeoutSeconds);
    await TestUtils.changeField(selectors.getPropertiesInput, simpleData.properties);

    expect(selectors.getVerifyConnectionButton()).not.toHaveAttribute('disabled');
    userEvent.click(selectors.getVerifyConnectionButton());

    await waitFor(() => expect(Axios.post).toHaveBeenCalledTimes(1));

    await waitFor(() => expect(Axios.post).toHaveBeenCalledWith(
      '/service/rest/internal/ui/iq/verify-connection', simpleData
    ));

    expect(selectors.getSaveButton()).not.toHaveAttribute('aria-disabled', 'true');
    userEvent.click(selectors.getSaveButton());

    await waitFor(() => expect(Axios.put).toHaveBeenCalledTimes(1));

    await waitFor(() => expect(Axios.put).toHaveBeenCalledWith(
      '/service/rest/v1/iq', simpleData
    ));
  });

  describe('Read Only Mode', () => {
    const dataClass = 'nx-read-only__data';
    const labelClass = 'nx-read-only__label';

    it('Shows Iq Server configuration in Read Only mode', async () => {
      ExtJS.checkPermission.mockReturnValueOnce(false);

      when(Axios.get).calledWith('/service/rest/v1/iq').mockResolvedValue({
        data: DEFAULT_RESPONSE
      });

      render(<IqServer/>);

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      expect(screen.getByText(UIStrings.SETTINGS.READ_ONLY.WARNING)).toBeInTheDocument();

      expect(screen.getByText(UIStrings.IQ_SERVER.ENABLED.label)).toHaveClass(labelClass);
      expect(screen.getByText('Disabled')).toHaveClass(dataClass);
      expect(screen.queryByText(UIStrings.IQ_SERVER.IQ_SERVER_URL.label)).not.toBeInTheDocument();
    });

    it('Shows empty Iq Server page in Read Only mode', async () => {
      ExtJS.checkPermission.mockReturnValueOnce(false);

      when(Axios.get).calledWith('/service/rest/v1/iq').mockResolvedValue({
        data: {
          "enabled": true,
          "showLink": true,
          "url": "http://example.com",
          "authenticationType": "USER",
          "username": "user",
          "password": "pass",
          "useTrustStoreForUrl": true,
          "timeoutSeconds": null,
          "properties": "some=text"
        }
      });

      render(<IqServer/>);

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      expect(screen.getByText(UIStrings.SETTINGS.READ_ONLY.WARNING)).toBeInTheDocument();

      expect(screen.getByText(UIStrings.IQ_SERVER.ENABLED.label)).toHaveClass(labelClass);
      expect(screen.getByText(UIStrings.IQ_SERVER.SHOW_LINK.label)).toHaveClass(labelClass);
      expect(screen.getAllByText('Enabled').length).toBe(2);
      expect(screen.getByText(UIStrings.IQ_SERVER.IQ_SERVER_URL.label)).toHaveClass(labelClass);
      expect(screen.getByText('http://example.com')).toHaveClass(dataClass);
      expect(screen.getByText(UIStrings.IQ_SERVER.AUTHENTICATION_TYPE.label)).toHaveClass(labelClass);
      expect(screen.getByText('User Authentication')).toHaveClass(dataClass);
      expect(screen.getByText(UIStrings.IQ_SERVER.USERNAME.label)).toHaveClass(labelClass);
      expect(screen.getByText('user')).toHaveClass(dataClass);
      expect(screen.getByText(UIStrings.IQ_SERVER.CONNECTION_TIMEOUT.label)).toHaveClass(labelClass);
      expect(screen.getByText(UIStrings.IQ_SERVER.CONNECTION_TIMEOUT_DEFAULT_VALUE_LABEL)).toHaveClass(dataClass);
      expect(screen.getByText(UIStrings.IQ_SERVER.PROPERTIES.label)).toHaveClass(labelClass);
      expect(screen.getByText('some=text')).toHaveClass(dataClass);
    });
  })
});
