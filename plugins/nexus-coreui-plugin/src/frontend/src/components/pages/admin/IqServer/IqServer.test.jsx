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
import {render, screen, waitForElementToBeRemoved} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import {when} from 'jest-when';

import Axios from 'axios';
import IqServer from './IqServer';
import UIStrings from '../../../../constants/UIStrings';

const {IQ_SERVER, SETTINGS} = UIStrings;

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

const selectors = {
  ...TestUtils.selectors,
  ...TestUtils.formSelectors,
  getEnabledCheckbox: () => screen.getByLabelText(IQ_SERVER.ENABLED.sublabel),
  getUrlInput: () => screen.getByLabelText(IQ_SERVER.IQ_SERVER_URL.label),
  getAuthenticationMethodSelect: () => screen.getByLabelText(IQ_SERVER.AUTHENTICATION_TYPE.label),
  getUsernameInput: () => screen.queryByLabelText(IQ_SERVER.USERNAME.label),
  getPasswordInput: () => screen.queryByLabelText(IQ_SERVER.PASSWORD.label),
  getConnectionTimeoutInput: () => screen.getByLabelText(IQ_SERVER.CONNECTION_TIMEOUT.label),
  getPropertiesInput: () => screen.getByLabelText(IQ_SERVER.PROPERTIES.label),
  getShowIqServerLinkCheckbox: () => screen.getByLabelText(IQ_SERVER.SHOW_LINK.sublabel),
  getVerifyConnectionButton: () => screen.getByText(IQ_SERVER.VERIFY_CONNECTION_BUTTON_LABEL),
  getCertificateCheckbox: () => screen.getByLabelText(IQ_SERVER.TRUST_STORE.sublabel),
  getCertificateButton: () => screen.getByText(IQ_SERVER.CERTIFICATE),
  getDiscardButton: () => screen.getByText(SETTINGS.DISCARD_BUTTON_LABEL),
  getCloseButton: () => screen.queryByText('Close'),
  getReenterPasswordError: () => screen.queryByText(UIStrings.IQ_SERVER.PASSWORD_ERROR),
  getFieldRequiredError: () => screen.queryByText(UIStrings.ERROR.FIELD_REQUIRED)
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
    when(Axios.get).calledWith('service/rest/v1/iq').mockResolvedValue({
      data: DEFAULT_RESPONSE
    });
  });

  afterEach(() => {
    window.dirty = [];
  });

  it('fetches the empty settings from the backend and displays the empty form', async () => {
    render(<IqServer/>);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(selectors.getEnabledCheckbox()).not.toBeChecked();
    expect(selectors.getCertificateCheckbox()).not.toBeChecked();
    expect(selectors.getUrlInput()).toHaveValue("");
    expect(selectors.getAuthenticationMethodSelect()).toHaveValue("");
    expect(selectors.getConnectionTimeoutInput()).toHaveValue("");
    expect(selectors.getPropertiesInput()).toHaveValue("");
    expect(selectors.getShowIqServerLinkCheckbox()).not.toBeChecked();
  });

  it('fetches existing settings from the backend and displays them in the form', async () => {
    when(Axios.get).calledWith('service/rest/v1/iq').mockResolvedValue({
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

  it('shows contextual error message for empty password', async () => {
    when(Axios.get).calledWith('service/rest/v1/iq').mockResolvedValue({
      data: {
        "enabled": true,
        "showLink": true,
        "url": "http://example.com",
        "authenticationType": "USER",
        "username": "user",
        "password": "#~NXRM~PLACEHOLDER~PASSWORD~#",
        "useTrustStoreForUrl": false,
        "timeoutSeconds": 100,
        "properties": "some\ntext"
      }
    });

    render(<IqServer/>);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.click(selectors.querySubmitButton());
    expect(selectors.queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();

    await TestUtils.changeField(selectors.getUrlInput, 'http://example.comxx');
    expect(selectors.getPasswordInput()).toHaveValue("");
    expect(selectors.getReenterPasswordError()).toBeInTheDocument();
    expect(selectors.getFieldRequiredError()).not.toBeInTheDocument();
    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

    await TestUtils.changeField(selectors.getUrlInput, 'http://example.com');
    expect(selectors.getReenterPasswordError()).not.toBeInTheDocument();
    expect(selectors.getFieldRequiredError()).toBeInTheDocument();
    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

    await TestUtils.changeField(selectors.getPasswordInput, 'some');
    expect(selectors.getReenterPasswordError()).not.toBeInTheDocument();
    expect(selectors.getFieldRequiredError()).not.toBeInTheDocument();
    expect(selectors.queryFormError()).not.toBeInTheDocument();
  });

  it('enables the verify connection button when form is valid', async () => {
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

  it('enables the certificate view button when iq server url is valid', async () => {
    when(global.NX.Permissions.check)
      .calledWith('nexus:ssl-truststore:read')
      .mockReturnValue(true);
    when(global.NX.Permissions.check)
      .calledWith('nexus:ssl-truststore:create')
      .mockReturnValue(true);
    when(global.NX.Permissions.check)
      .calledWith('nexus:ssl-truststore:update')
      .mockReturnValue(true);

    render(<IqServer/>);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.click(selectors.getCertificateButton());

    expect(selectors.getCloseButton()).not.toBeInTheDocument();

    await TestUtils.changeField(selectors.getUrlInput, 'https://example.com');

    userEvent.click(selectors.getCertificateButton());

    expect(selectors.getCloseButton()).toBeInTheDocument();

    userEvent.click(selectors.getCloseButton());

    expect(selectors.getCloseButton()).not.toBeInTheDocument();
  });

  it('disables the save button when the form is invalid', async () => {
    render(<IqServer/>);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.click(selectors.querySubmitButton());
    expect(selectors.queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();

    userEvent.click(selectors.getEnabledCheckbox());

    expect(selectors.getEnabledCheckbox()).toBeChecked();
    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();
  });

  it('enables the save button when the form is valid', async () => {
    render(<IqServer/>);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.click(selectors.querySubmitButton());
    expect(selectors.queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();

    await TestUtils.changeField(selectors.getUrlInput, 'http://example.com');
    userEvent.selectOptions(selectors.getAuthenticationMethodSelect(), 'PKI');

    expect(selectors.queryFormError()).not.toBeInTheDocument();
  });

  it('requires the username and password for the USER authentication method', async () => {
    render(<IqServer/>);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.click(selectors.querySubmitButton());
    expect(selectors.queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();

    await TestUtils.changeField(selectors.getUrlInput, 'http://example.com');
    userEvent.selectOptions(selectors.getAuthenticationMethodSelect(), 'USER');

    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

    await TestUtils.changeField(selectors.getUsernameInput, 'user');
    await TestUtils.changeField(selectors.getPasswordInput, 'pass');

    expect(selectors.queryFormError()).not.toBeInTheDocument();
  });

  it('requires a connection timeout greater than 0, less than or equal to 3600', async () => {
    render(<IqServer/>);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.click(selectors.querySubmitButton());
    expect(selectors.queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();

    userEvent.click(selectors.getEnabledCheckbox());
    expect(selectors.getEnabledCheckbox()).toBeChecked();

    await TestUtils.changeField(selectors.getUrlInput, 'http://example.com');

    userEvent.selectOptions(selectors.getAuthenticationMethodSelect(), 'PKI');

    await TestUtils.changeField(selectors.getConnectionTimeoutInput, '0');
    expect(selectors.getConnectionTimeoutInput()).toHaveErrorMessage('The minimum value for this field is 1');

    userEvent.clear(selectors.getConnectionTimeoutInput());
    await TestUtils.changeField(selectors.getConnectionTimeoutInput, '3601');
    expect(selectors.getConnectionTimeoutInput()).toHaveErrorMessage('The maximum value for this field is 3600');

    userEvent.clear(selectors.getConnectionTimeoutInput());
    await TestUtils.changeField(selectors.getConnectionTimeoutInput, '1');
    expect(selectors.queryFormError()).not.toBeInTheDocument();

    userEvent.clear(selectors.getConnectionTimeoutInput());
    await TestUtils.changeField(selectors.getConnectionTimeoutInput, '3600');
    expect(selectors.queryFormError()).not.toBeInTheDocument();
  });

  it('discards changes', async () => {
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

    when(Axios.post).calledWith('service/rest/internal/ui/iq/verify-connection', simpleData).mockResolvedValue({
      data: {reason: 'Test App', success: true}
    });
    when(Axios.put).calledWith('service/rest/v1/iq', simpleData).mockResolvedValue({data: {}});

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
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
    expect(screen.queryByText(/Test App/i)).toBeInTheDocument();
    expect(Axios.post).toHaveBeenCalledTimes(1);
    expect(Axios.post).toHaveBeenCalledWith('service/rest/internal/ui/iq/verify-connection', simpleData);

    userEvent.click(selectors.querySubmitButton());
    await waitForElementToBeRemoved(selectors.querySavingMask());
    expect(Axios.put).toHaveBeenCalledTimes(1);
    expect(Axios.put).toHaveBeenCalledWith('service/rest/v1/iq', simpleData);
  });

  it('allows for the url to be updated when there is an alert', async () => {
    render(<IqServer/>);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    await TestUtils.changeField(selectors.getUrlInput, 'http://example.com');
    userEvent.selectOptions(selectors.getAuthenticationMethodSelect(), 'PKI');
    expect(selectors.getVerifyConnectionButton()).not.toHaveAttribute('disabled');

    userEvent.click(selectors.getVerifyConnectionButton());
    await TestUtils.changeField(selectors.getUrlInput, 'http://newexample.com');
    expect(selectors.getUrlInput()).toHaveValue('http://newexample.com');
  });

  describe('Read Only Mode', () => {
    const dataClass = 'nx-read-only__data';
    const labelClass = 'nx-read-only__label';

    it('Shows Iq Server configuration in Read Only mode', async () => {
      ExtJS.checkPermission.mockReturnValueOnce(false);

      render(<IqServer/>);

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      expect(screen.getByText(SETTINGS.READ_ONLY.WARNING)).toBeInTheDocument();

      expect(screen.getByText(IQ_SERVER.ENABLED.label)).toHaveClass(labelClass);
      expect(screen.getByText('Disabled')).toHaveClass(dataClass);
      expect(screen.queryByText(IQ_SERVER.IQ_SERVER_URL.label)).not.toBeInTheDocument();
    });

    it('Shows empty Iq Server page in Read Only mode', async () => {
      ExtJS.checkPermission.mockReturnValueOnce(false);

      when(Axios.get).calledWith('service/rest/v1/iq').mockResolvedValue({
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

      expect(screen.getByText(SETTINGS.READ_ONLY.WARNING)).toBeInTheDocument();

      expect(screen.getByText(IQ_SERVER.ENABLED.label)).toHaveClass(labelClass);
      expect(screen.getByText(IQ_SERVER.SHOW_LINK.label)).toHaveClass(labelClass);
      expect(screen.getAllByText('Enabled').length).toBe(2);
      expect(screen.getByText(IQ_SERVER.IQ_SERVER_URL.label)).toHaveClass(labelClass);
      expect(screen.getByText('http://example.com')).toHaveClass(dataClass);
      expect(screen.getByText(IQ_SERVER.AUTHENTICATION_TYPE.label)).toHaveClass(labelClass);
      expect(screen.getByText('User Authentication')).toHaveClass(dataClass);
      expect(screen.getByText(IQ_SERVER.USERNAME.label)).toHaveClass(labelClass);
      expect(screen.getByText('user')).toHaveClass(dataClass);
      expect(screen.getByText(IQ_SERVER.CONNECTION_TIMEOUT.label)).toHaveClass(labelClass);
      expect(screen.getByText(IQ_SERVER.CONNECTION_TIMEOUT_DEFAULT_VALUE_LABEL)).toHaveClass(dataClass);
      expect(screen.getByText(IQ_SERVER.PROPERTIES.label)).toHaveClass(labelClass);
      expect(screen.getByText('some=text')).toHaveClass(dataClass);
    });
  })
});
