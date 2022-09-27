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
import {
  render,
  screen,
  waitForElementToBeRemoved,
  waitFor,
  act
} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ExtJS, TestUtils, APIConstants} from '@sonatype/nexus-ui-plugin';
import {when} from 'jest-when';

import Axios from 'axios';
import EmailServer from './EmailServer';
import UIStrings from '../../../../constants/UIStrings';

const {
  EMAIL_SERVER: {
    FORM: LABELS,
    VERIFY,
    READ_ONLY
  },
  SETTINGS,
  ERROR
} = UIStrings;
const {REST: {PUBLIC: {EMAIL_SERVER: emailServerUrl}}} = APIConstants;
const XSS_STRING = TestUtils.XSS_STRING;

jest.mock('axios', () => ({
  ...jest.requireActual('axios'),
  get: jest.fn(),
  put: jest.fn(),
  post: jest.fn(),
}));

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    checkPermission: jest.fn(),
    showErrorMessage: jest.fn(),
    showSuccessMessage: jest.fn(),
  },
}));

const selectors = {
  ...TestUtils.selectors,
  enabled: () => screen.getByLabelText(LABELS.ENABLED.SUB_LABEL),
  host: () => screen.getByLabelText(LABELS.HOST.LABEL),
  port: () => screen.getByLabelText(LABELS.PORT.LABEL),
  username: () => screen.queryByLabelText(LABELS.USERNAME.LABEL),
  password: () => screen.queryByLabelText(LABELS.PASSWORD.LABEL),
  fromAddress: () => screen.queryByLabelText(LABELS.FROM_ADDRESS.LABEL),
  subjectPrefix: () => screen.queryByLabelText(LABELS.SUBJECT_PREFIX.LABEL),
  enableStarttls: () => screen.queryByLabelText(LABELS.SSL_TLS_OPTIONS.OPTIONS.ENABLE_STARTTLS),
  requireStarttls: () => screen.queryByLabelText(LABELS.SSL_TLS_OPTIONS.OPTIONS.REQUIRE_STARTTLS),
  enableSslTls: () => screen.queryByLabelText(LABELS.SSL_TLS_OPTIONS.OPTIONS.ENABLE_SSL_TLS),
  identityCheck: () => screen.queryByLabelText(LABELS.SSL_TLS_OPTIONS.OPTIONS.IDENTITY_CHECK),
  discardButton: () => screen.getByText(SETTINGS.DISCARD_BUTTON_LABEL),
  saveButton: () => screen.getByText(SETTINGS.SAVE_BUTTON_LABEL),
  test: {
    input: () => screen.getByLabelText(VERIFY.LABEL),
    button: () => screen.getByText(VERIFY.TEST),
    success: () => screen.getByText(VERIFY.SUCCESS),
    querySuccess: () => screen.queryByText(VERIFY.SUCCESS),
    failed: () => screen.getByText(VERIFY.ERROR),
    queryFailed: () => screen.queryByText(VERIFY.ERROR),
  },
  readOnly: {
    title: () => screen.getByText(LABELS.SECTIONS.SETUP),
    enabled: () => screen.getByText(LABELS.ENABLED.LABEL),
    enabledValue: () => screen.getByText(LABELS.ENABLED.LABEL).nextSibling,
    warning: () => screen.getByText(UIStrings.SETTINGS.READ_ONLY.WARNING),
    host: () => screen.getByText(LABELS.HOST.LABEL),
    hostValue: () => screen.getByText(LABELS.HOST.LABEL).nextSibling,
    port: () => screen.getByText(LABELS.PORT.LABEL),
    portValue: () => screen.getByText(LABELS.PORT.LABEL).nextSibling,
    username: () => screen.getByText(LABELS.USERNAME.LABEL),
    usernameValue: () => screen.getByText(LABELS.USERNAME.LABEL).nextSibling,
    fromAddress: () => screen.getByText(LABELS.FROM_ADDRESS.LABEL),
    fromAddressValue: () => screen.getByText(LABELS.FROM_ADDRESS.LABEL).nextSibling,
    subjectPrefix: () => screen.getByText(LABELS.SUBJECT_PREFIX.LABEL),
    subjectPrefixValue: () => screen.getByText(LABELS.SUBJECT_PREFIX.LABEL).nextSibling,
    options: () => screen.getByText(LABELS.SSL_TLS_OPTIONS.LABEL),
    optionsValues: {
      enable: [
        () => screen.getByText(READ_ONLY.ENABLE.ENABLE_STARTTLS),
        () => screen.getByText(READ_ONLY.ENABLE.REQUIRE_STARTTLS),
        () => screen.getByText(READ_ONLY.ENABLE.ENABLE_SSL_TLS),
        () => screen.getByText(READ_ONLY.ENABLE.IDENTITY_CHECK)
      ],
      notEnable: [
        () => screen.getByText(READ_ONLY.NOT_ENABLE.ENABLE_STARTTLS),
        () => screen.getByText(READ_ONLY.NOT_ENABLE.REQUIRE_STARTTLS),
        () => screen.getByText(READ_ONLY.NOT_ENABLE.ENABLE_SSL_TLS),
        () => screen.getByText(READ_ONLY.NOT_ENABLE.IDENTITY_CHECK)
      ]
    }
  }
};

const DATA = {
  enabled: true,
  fromAddress: 'test@test.com',
  host: 'example.com',
  password: null,
  port: 1234,
  sslOnConnectEnabled: true,
  sslServerIdentityCheckEnabled: true,
  startTlsEnabled: true,
  startTlsRequired: true,
  subjectPrefix: 'prefix',
  username: 'test',
};

const EMPTY_DATA = {
  enabled: false,
  host: null,
  port: 0,
  username: null,
  password: null,
  fromAddress: null,
  subjectPrefix: null,
  startTlsEnabled: false,
  startTlsRequired: false,
  sslOnConnectEnabled: false,
  sslServerIdentityCheckEnabled: false,
};

const populateForm = () => {
  const {enabled, host, port, username, password, fromAddress, subjectPrefix,
    enableStarttls, requireStarttls, enableSslTls, identityCheck} = selectors;

  userEvent.click(enabled());
  userEvent.type(host(), DATA.host);
  userEvent.clear(port());
  userEvent.type(port(), DATA.port.toString());
  userEvent.type(username(), DATA.username);
  userEvent.type(password(), DATA.password);
  userEvent.type(fromAddress(), DATA.fromAddress);
  userEvent.type(subjectPrefix(), DATA.subjectPrefix);
  userEvent.click(enableStarttls());
  userEvent.click(requireStarttls());
  userEvent.click(enableSslTls());
  userEvent.click(identityCheck());
};

const formShouldBeEmpty = () => {
  const {enabled, host, port, username, password, fromAddress, subjectPrefix,
    enableStarttls, requireStarttls, enableSslTls, identityCheck} = selectors;

  expect(enabled()).not.toBeChecked();
  expect(host()).toHaveValue('');
  expect(port()).toHaveValue('0');
  expect(username()).toHaveValue('');
  expect(password()).toHaveValue('');
  expect(fromAddress()).toHaveValue('');
  expect(subjectPrefix()).toHaveValue('');
  expect(enableStarttls()).not.toBeChecked();
  expect(requireStarttls()).not.toBeChecked();
  expect(enableSslTls()).not.toBeChecked();
  expect(identityCheck()).not.toBeChecked();
};

describe('EmailServer', () => {
  const renderAndWaitForLoad = async () => {
    render(<EmailServer/>);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
  }

  beforeEach(() => {
    when(Axios.get).calledWith(emailServerUrl).mockResolvedValue({
      data: EMPTY_DATA
    });
    ExtJS.checkPermission.mockReturnValue(true);
  });

  it('renders the empty form', async () => {
    const {discardButton, saveButton} = selectors;

    await renderAndWaitForLoad();

    formShouldBeEmpty();

    expect(saveButton()).toHaveClass('disabled');
    expect(discardButton()).toHaveClass('disabled');
  });

  it('renders the resolved data', async () => {
    const {enabled, host, port, username, password, fromAddress, subjectPrefix,
      enableStarttls, requireStarttls, enableSslTls, identityCheck, discardButton, saveButton} = selectors;

    when(Axios.get).calledWith(emailServerUrl).mockResolvedValue({
      data: DATA
    });

    await renderAndWaitForLoad();

    expect(enabled()).toBeChecked();
    expect(host()).toHaveValue('example.com');
    expect(port()).toHaveValue('1234');
    expect(username()).toHaveValue('test');
    expect(password()).toHaveValue('');
    expect(fromAddress()).toHaveValue('test@test.com');
    expect(subjectPrefix()).toHaveValue('prefix');
    expect(enableStarttls()).toBeChecked();
    expect(requireStarttls()).toBeChecked();
    expect(enableSslTls()).toBeChecked();
    expect(identityCheck()).toBeChecked();

    expect(saveButton()).toHaveClass('disabled');
    expect(discardButton()).toHaveClass('disabled');
  });

  it('renders the resolved data with XSS', async () => {
    const {host,username, password, fromAddress, subjectPrefix} = selectors;

    when(Axios.get).calledWith(emailServerUrl).mockResolvedValue({
      data: {
        ...DATA,
        fromAddress: XSS_STRING,
        host: XSS_STRING,
        password: XSS_STRING,
        subjectPrefix: XSS_STRING,
        username: XSS_STRING,
      }
    });

    await renderAndWaitForLoad();

    expect(host()).toHaveValue(XSS_STRING);
    expect(username()).toHaveValue(XSS_STRING);
    expect(password()).toHaveValue(XSS_STRING);
    expect(fromAddress()).toHaveValue(XSS_STRING);
    expect(subjectPrefix()).toHaveValue(XSS_STRING);
  });

  it('enables the save button when the form is valid', async () => {
    const {host, port, fromAddress, saveButton} = selectors;

    await renderAndWaitForLoad();

    expect(saveButton()).toHaveClass('disabled');
    userEvent.type(host(), DATA.host);
    userEvent.type(port(), DATA.port.toString());
    userEvent.type(fromAddress(), DATA.fromAddress);

    expect(saveButton()).not.toHaveClass('disabled');
  });

  it('shows validation errors', async () => {
    const {host, port, fromAddress, saveButton} = selectors;

    await renderAndWaitForLoad();

    userEvent.type(host(), 'host+');
    expect(host()).toHaveErrorMessage(ERROR.HOSTNAME);
    userEvent.clear(host());
    expect(host()).toHaveErrorMessage(ERROR.FIELD_REQUIRED);
    userEvent.type(host(), DATA.host);
    expect(host()).not.toHaveErrorMessage();

    userEvent.type(port(), '99999');
    expect(port()).toHaveErrorMessage(ERROR.MAX(65535));
    userEvent.clear(port());
    expect(port()).toHaveErrorMessage(ERROR.FIELD_REQUIRED);
    userEvent.type(port(), '0');
    expect(port()).toHaveErrorMessage(ERROR.MIN(1));
    userEvent.clear(port());
    userEvent.type(port(), 'test');
    expect(port()).toHaveErrorMessage(ERROR.NAN);
    userEvent.clear(port());
    userEvent.type(port(), DATA.port.toString());
    expect(port()).not.toHaveErrorMessage();

    userEvent.type(fromAddress(), 'test');
    expect(fromAddress()).toHaveErrorMessage(ERROR.INVALID_EMAIL);
    userEvent.clear(fromAddress());
    expect(fromAddress()).toHaveErrorMessage(ERROR.FIELD_REQUIRED);
    userEvent.type(fromAddress(), DATA.fromAddress);
    expect(fromAddress()).not.toHaveErrorMessage();

    expect(saveButton()).not.toHaveClass('disabled');
  });

  it('creates email server configuration', async () => {
    const {saveButton} = selectors;

    when(Axios.put).calledWith(emailServerUrl, expect.anything()).mockResolvedValue({data: {}});

    await renderAndWaitForLoad();

    expect(saveButton()).toHaveClass('disabled');
    populateForm();
    expect(saveButton()).not.toHaveClass('disabled');

    userEvent.click(saveButton());

    await waitFor(() => expect(Axios.put).toHaveBeenCalledWith(emailServerUrl, {...DATA, port: DATA.port.toString()}));
    expect(NX.Messages.success).toHaveBeenCalledWith(UIStrings.SAVE_SUCCESS);
  });


  it('discards changes', async () => {
    const {discardButton} = selectors;

    await renderAndWaitForLoad();

    expect(discardButton()).toHaveClass('disabled');
    populateForm();
    expect(discardButton()).not.toHaveClass('disabled');

    userEvent.click(discardButton());
    formShouldBeEmpty();
  });

  describe('Verify configuration', () => {
    const url = `${emailServerUrl}/verify`;
    const email = 'example@test.com';

    it('renders the empty input field', async () => {
      const {input, button} = selectors.test;

      await renderAndWaitForLoad();

      expect(input()).toBeInTheDocument();
      expect(button()).toBeInTheDocument();
    });

    it('validates the email configuration', async () => {
      const {input, button, success} = selectors.test;

      when(Axios.post).calledWith(url, email).mockResolvedValue({data: {success: true}});

      await renderAndWaitForLoad();

      await TestUtils.changeField(input, email);

      await act(async () => userEvent.click(button()));

      expect(Axios.post).toHaveBeenCalledWith(url, email);
      expect(success()).toBeInTheDocument();
    });

    it('show error message if the validation fails', async () => {
      const {input, button, failed} = selectors.test;

      when(Axios.post).calledWith(url, email).mockRejectedValue({data: {success: false}});

      await renderAndWaitForLoad();

      await TestUtils.changeField(input, email);

      await act(async () => userEvent.click(button()));

      expect(Axios.post).toHaveBeenCalledWith(url, email);
      expect(failed()).toBeInTheDocument();
    });

    it('validate email', async () => {
      const {input, button} = selectors.test;

      await renderAndWaitForLoad();

      expect(button()).toBeDisabled();

      await TestUtils.changeField(input, 'wrong_email');

      expect(button()).toBeDisabled();
    });

    it('removes success message if the input value changes', async () => {
      const {input, button, success, querySuccess} = selectors.test;

      when(Axios.post).calledWith(url, email).mockResolvedValue({data: {success: true}});

      await renderAndWaitForLoad();

      await TestUtils.changeField(input, email);

      await act(async () => userEvent.click(button()));

      expect(Axios.post).toHaveBeenCalledWith(url, email);
      expect(success()).toBeInTheDocument();

      await TestUtils.changeField(input, 'changes');

      expect(querySuccess()).not.toBeInTheDocument();
    });

    it('removes error message if the input value changes', async () => {
      const {input, button, failed, queryFailed} = selectors.test;

      when(Axios.post).calledWith(url, email).mockRejectedValue({data: {success: false}});

      await renderAndWaitForLoad();

      await TestUtils.changeField(input, email);

      await act(async () => userEvent.click(button()));

      expect(Axios.post).toHaveBeenCalledWith(url, email);
      expect(failed()).toBeInTheDocument();

      await TestUtils.changeField(input, 'changes');

      expect(queryFailed()).not.toBeInTheDocument();
    });
  });

  describe('Read only', () => {
    const data = {
      enabled: true,
      host: "smtp.gmail.com",
      port: 465,
      username: "my_user@sonatype.com",
      password: null,
      fromAddress: "test@sonatype.com",
      subjectPrefix: "subject",
      startTlsEnabled: true,
      startTlsRequired: true,
      sslOnConnectEnabled: true,
      sslServerIdentityCheckEnabled: true,
      nexusTrustStoreEnabled: true
    }

    beforeEach(() => {
      when(ExtJS.checkPermission)
        .calledWith('nexus:settings:update')
        .mockReturnValue(false);

      when(Axios.get)
        .calledWith(emailServerUrl)
        .mockResolvedValue({data});
    });

    it('shows default information if email server is not enabled', async () => {
      when(Axios.get).calledWith(emailServerUrl).mockResolvedValue({data: {}});

      const {title, enabled, warning, enabledValue} = selectors.readOnly;

      await renderAndWaitForLoad();

      expect(title()).toBeInTheDocument();
      expect(warning()).toBeInTheDocument();
      expect(enabled()).toBeInTheDocument();
      expect(enabledValue()).toHaveTextContent('Disabled');
    });

    it('shows the configuration correctly', async () => {
      const {
        title,
        warning,
        enabled,
        enabledValue,
        host,
        hostValue,
        port,
        portValue,
        username,
        usernameValue,
        fromAddress,
        fromAddressValue,
        subjectPrefix,
        subjectPrefixValue,
        options,
        optionsValues
      } = selectors.readOnly;

      await renderAndWaitForLoad();

      expect(title()).toBeInTheDocument();
      expect(warning()).toBeInTheDocument();
      expect(enabled()).toBeInTheDocument();
      expect(enabledValue()).toHaveTextContent('Enabled');
      expect(host()).toBeInTheDocument();
      expect(hostValue()).toHaveTextContent(data.host);
      expect(port()).toBeInTheDocument();
      expect(portValue()).toHaveTextContent(data.port);
      expect(username()).toBeInTheDocument();
      expect(usernameValue()).toHaveTextContent(data.username);
      expect(fromAddress()).toBeInTheDocument();
      expect(fromAddressValue()).toHaveTextContent(data.fromAddress);
      expect(subjectPrefix()).toBeInTheDocument();
      expect(subjectPrefixValue()).toHaveTextContent(data.subjectPrefix);
      expect(options()).toBeInTheDocument();
      optionsValues.enable.forEach((value) => expect(value()).toBeInTheDocument());
    });

    it('shows the corresponding message when SSL/TLS options are not enabled', async () => {
      const {optionsValues} = selectors.readOnly;
      const newData = {
        ...data,
        startTlsEnabled: false,
        startTlsRequired: false,
        sslOnConnectEnabled: false,
        sslServerIdentityCheckEnabled: false,
      }

      when(Axios.get).calledWith(emailServerUrl).mockResolvedValue({data:newData});

      await renderAndWaitForLoad();

      optionsValues.notEnable.forEach((value) => expect(value()).toBeInTheDocument());
    });
  })
});
