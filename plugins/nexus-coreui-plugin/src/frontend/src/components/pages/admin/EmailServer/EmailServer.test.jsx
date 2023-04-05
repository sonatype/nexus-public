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
  act,
} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ExtJS, APIConstants, ExtAPIUtils} from '@sonatype/nexus-ui-plugin';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import {when} from 'jest-when';

import Axios from 'axios';
import EmailServer from './EmailServer';
import UIStrings from '../../../../constants/UIStrings';

const {
  EMAIL_SERVER: {FORM: LABELS, VERIFY, READ_ONLY},
  USE_TRUST_STORE,
  SETTINGS,
  ERROR,
} = UIStrings;

const {
  EXT: {
    URL,
    EMAIL_SERVER: {ACTION, METHODS},
  },
} = APIConstants;

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
  ...TestUtils.formSelectors,
  enabled: () => screen.getByLabelText(LABELS.ENABLED.SUB_LABEL),
  host: () => screen.getByLabelText(LABELS.HOST.LABEL),
  port: () => screen.getByLabelText(LABELS.PORT.LABEL),
  username: () => screen.queryByLabelText(LABELS.USERNAME.LABEL),
  password: () => screen.queryByLabelText(LABELS.PASSWORD.LABEL),
  useTruststore: () => screen.queryByLabelText(USE_TRUST_STORE.DESCRIPTION),
  fromAddress: () => screen.queryByLabelText(LABELS.FROM_ADDRESS.LABEL),
  subjectPrefix: () => screen.queryByLabelText(LABELS.SUBJECT_PREFIX.LABEL),
  enableStarttls: () =>
    screen.queryByLabelText(LABELS.SSL_TLS_OPTIONS.OPTIONS.ENABLE_STARTTLS),
  requireStarttls: () =>
    screen.queryByLabelText(LABELS.SSL_TLS_OPTIONS.OPTIONS.REQUIRE_STARTTLS),
  enableSslTls: () =>
    screen.queryByLabelText(LABELS.SSL_TLS_OPTIONS.OPTIONS.ENABLE_SSL_TLS),
  identityCheck: () =>
    screen.queryByLabelText(LABELS.SSL_TLS_OPTIONS.OPTIONS.IDENTITY_CHECK),
  discardButton: () => screen.getByText(SETTINGS.DISCARD_BUTTON_LABEL),
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
    useTruststore: () => screen.getByText(USE_TRUST_STORE.LABEL),
    useTruststoreValue: () =>
      screen.getByText(USE_TRUST_STORE.LABEL).nextSibling,
    username: () => screen.getByText(LABELS.USERNAME.LABEL),
    usernameValue: () => screen.getByText(LABELS.USERNAME.LABEL).nextSibling,
    fromAddress: () => screen.getByText(LABELS.FROM_ADDRESS.LABEL),
    fromAddressValue: () =>
      screen.getByText(LABELS.FROM_ADDRESS.LABEL).nextSibling,
    subjectPrefix: () => screen.getByText(LABELS.SUBJECT_PREFIX.LABEL),
    subjectPrefixValue: () =>
      screen.getByText(LABELS.SUBJECT_PREFIX.LABEL).nextSibling,
    options: () => screen.getByText(LABELS.SSL_TLS_OPTIONS.LABEL),
    optionsValues: {
      enable: [
        () => screen.getByText(READ_ONLY.ENABLE.ENABLE_STARTTLS),
        () => screen.getByText(READ_ONLY.ENABLE.REQUIRE_STARTTLS),
        () => screen.getByText(READ_ONLY.ENABLE.ENABLE_SSL_TLS),
        () => screen.getByText(READ_ONLY.ENABLE.IDENTITY_CHECK),
      ],
      notEnable: [
        () => screen.getByText(READ_ONLY.NOT_ENABLE.ENABLE_STARTTLS),
        () => screen.getByText(READ_ONLY.NOT_ENABLE.REQUIRE_STARTTLS),
        () => screen.getByText(READ_ONLY.NOT_ENABLE.ENABLE_SSL_TLS),
        () => screen.getByText(READ_ONLY.NOT_ENABLE.IDENTITY_CHECK),
      ],
    },
  },
};

const DATA = {
  enabled: true,
  fromAddress: 'test@test.com',
  host: 'example.com',
  password: '#~NXRM~PLACEHOLDER~PASSWORD~#',
  port: 1234,
  sslOnConnectEnabled: true,
  sslCheckServerIdentityEnabled: true,
  nexusTrustStoreEnabled: true,
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
  sslCheckServerIdentityEnabled: false,
};

const populateForm = () => {
  const {
    enabled,
    host,
    port,
    username,
    password,
    fromAddress,
    subjectPrefix,
    enableStarttls,
    requireStarttls,
    enableSslTls,
    identityCheck,
    useTruststore,
  } = selectors;

  userEvent.click(enabled());
  userEvent.type(host(), DATA.host);
  userEvent.clear(port());
  userEvent.type(port(), DATA.port.toString());
  userEvent.click(useTruststore());
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
  const {
    enabled,
    host,
    port,
    username,
    password,
    fromAddress,
    subjectPrefix,
    enableStarttls,
    requireStarttls,
    enableSslTls,
    identityCheck,
  } = selectors;

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
  const REQUEST = ExtAPIUtils.createRequestBody(ACTION, METHODS.READ, null);
  const renderAndWaitForLoad = async () => {
    render(<EmailServer />);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
  };

  beforeEach(() => {
    Axios.post.mockReset();

    when(ExtJS.checkPermission)
      .calledWith('nexus:settings:update')
      .mockReturnValue(true);
  });

  it('renders the empty form', async () => {
    when(Axios.post)
      .calledWith(URL, REQUEST)
      .mockResolvedValueOnce({
        data: TestUtils.makeExtResult(EMPTY_DATA),
      });

    const {discardButton, queryFormError} = selectors;

    await renderAndWaitForLoad();

    formShouldBeEmpty();

    expect(queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();
    expect(discardButton()).toHaveClass('disabled');
  });

  it('renders the resolved data', async () => {
    const {
      enabled,
      host,
      port,
      username,
      password,
      fromAddress,
      subjectPrefix,
      enableStarttls,
      requireStarttls,
      enableSslTls,
      identityCheck,
      discardButton,
      querySubmitButton,
      queryFormError,
    } = selectors;

    when(Axios.post)
      .calledWith(URL, REQUEST)
      .mockResolvedValue({data: TestUtils.makeExtResult(DATA)});

    await renderAndWaitForLoad();

    expect(enabled()).toBeChecked();
    expect(host()).toHaveValue(DATA.host);
    expect(port()).toHaveValue(DATA.port.toString());
    expect(username()).toHaveValue(DATA.username);
    expect(password()).toHaveValue(DATA.password);
    expect(fromAddress()).toHaveValue(DATA.fromAddress);
    expect(subjectPrefix()).toHaveValue(DATA.subjectPrefix);
    expect(enableStarttls()).toBeChecked();
    expect(requireStarttls()).toBeChecked();
    expect(enableSslTls()).toBeChecked();
    expect(identityCheck()).toBeChecked();

    expect(discardButton()).toHaveClass('disabled');
    userEvent.click(querySubmitButton());
    expect(queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();
  });

  it('renders the resolved data with XSS', async () => {
    const {host, username, password, fromAddress, subjectPrefix} = selectors;

    when(Axios.post)
      .calledWith(URL, REQUEST)
      .mockResolvedValue({
        data: TestUtils.makeExtResult({
          ...DATA,
          fromAddress: XSS_STRING,
          host: XSS_STRING,
          password: XSS_STRING,
          subjectPrefix: XSS_STRING,
          username: XSS_STRING,
        }),
      });

    await renderAndWaitForLoad();

    expect(host()).toHaveValue(XSS_STRING);
    expect(username()).toHaveValue(XSS_STRING);
    expect(password()).toHaveValue(XSS_STRING);
    expect(fromAddress()).toHaveValue(XSS_STRING);
    expect(subjectPrefix()).toHaveValue(XSS_STRING);
  });

  it('enables the save button when the form is valid', async () => {
    when(Axios.post)
      .calledWith(URL, REQUEST)
      .mockResolvedValueOnce({
        data: TestUtils.makeExtResult(EMPTY_DATA),
      });

    const {host, port, fromAddress, querySubmitButton, queryFormError} =
      selectors;

    await renderAndWaitForLoad();

    userEvent.click(querySubmitButton());
    expect(queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();

    userEvent.type(host(), DATA.host);
    userEvent.type(port(), DATA.port.toString());
    userEvent.type(fromAddress(), DATA.fromAddress);

    expect(queryFormError()).not.toBeInTheDocument();
  });

  it('shows validation errors', async () => {
    when(Axios.post)
      .calledWith(URL, REQUEST)
      .mockResolvedValueOnce({
        data: TestUtils.makeExtResult(EMPTY_DATA),
      });

    const {host, port, fromAddress, querySubmitButton, queryFormError} =
      selectors;

    await renderAndWaitForLoad();

    userEvent.click(querySubmitButton());
    expect(queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();

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

    expect(queryFormError()).not.toBeInTheDocument();
  });

  it('creates email server configuration', async () => {
    when(global.NX.Permissions.check)
      .calledWith('nexus:ssl-truststore:read')
      .mockReturnValue(true);
    when(global.NX.Permissions.check)
      .calledWith('nexus:ssl-truststore:create')
      .mockReturnValue(true);
    when(global.NX.Permissions.check)
      .calledWith('nexus:ssl-truststore:update')
      .mockReturnValue(true);

    when(Axios.post)
      .calledWith(URL, REQUEST)
      .mockResolvedValue({
        data: TestUtils.makeExtResult(EMPTY_DATA),
      });

    const {querySubmitButton, queryFormError, querySavingMask} = selectors;

    await renderAndWaitForLoad();

    userEvent.click(querySubmitButton());
    expect(queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();

    populateForm();

    expect(queryFormError()).not.toBeInTheDocument();

    const NEW_UPDATE = ExtAPIUtils.createRequestBody(ACTION, METHODS.UPDATE, {
      data: [{...DATA, port: DATA.port.toString()}],
    });

    when(Axios.post)
      .calledWith(URL, NEW_UPDATE)
      .mockResolvedValueOnce({
        data: TestUtils.makeExtResult(DATA),
      });

    userEvent.click(querySubmitButton());
    await waitForElementToBeRemoved(querySavingMask);

    expect(Axios.post).toHaveBeenCalledWith(URL, NEW_UPDATE);

    expect(NX.Messages.success).toHaveBeenCalledWith(UIStrings.SAVE_SUCCESS);
  });

  it('discards changes', async () => {
    when(Axios.post)
      .calledWith(URL, REQUEST)
      .mockResolvedValue({
        data: TestUtils.makeExtResult(EMPTY_DATA),
      });

    const {discardButton} = selectors;

    await renderAndWaitForLoad();

    expect(discardButton()).toHaveClass('disabled');
    populateForm();
    expect(discardButton()).not.toHaveClass('disabled');

    userEvent.click(discardButton());
    formShouldBeEmpty();
  });

  it('if the user changes the host the password is cleared', async () => {
    when(Axios.post)
      .calledWith(URL, REQUEST)
      .mockResolvedValue({
        data: TestUtils.makeExtResult(DATA),
      });
    const {host, password} = selectors;

    await renderAndWaitForLoad();

    expect(host()).toHaveValue(DATA.host);
    expect(password()).toHaveValue(DATA.password);

    await TestUtils.changeField(host, 'new@email.com');

    expect(password()).toHaveValue('');
  });

  it('if the user changes the port the password is cleared', async () => {
    when(Axios.post)
      .calledWith(URL, REQUEST)
      .mockResolvedValue({
        data: TestUtils.makeExtResult(DATA),
      });
    const {port, password} = selectors;

    await renderAndWaitForLoad();

    expect(port()).toHaveValue(DATA.port.toString());
    expect(password()).toHaveValue(DATA.password);

    await TestUtils.changeField(port, '123');

    expect(password()).toHaveValue('');
  });

  describe('Verify configuration', () => {
    const email = 'example@testcom';
    const VERIFY = ExtAPIUtils.createRequestBody(ACTION, METHODS.VERIFY, {
      data: [EMPTY_DATA, email],
    });

    it('renders the empty input field', async () => {
      when(Axios.post)
        .calledWith(URL, REQUEST)
        .mockResolvedValue({
          data: TestUtils.makeExtResult(EMPTY_DATA),
        });
      const {input, button} = selectors.test;

      await renderAndWaitForLoad();

      expect(input()).toBeInTheDocument();
      expect(button()).toBeInTheDocument();
    });

    it('validates the email configuration', async () => {
      when(Axios.post)
        .calledWith(URL, REQUEST)
        .mockResolvedValue({
          data: TestUtils.makeExtResult(EMPTY_DATA),
        });

      const {input, button, success} = selectors.test;

      await renderAndWaitForLoad();

      await TestUtils.changeField(input, email);

      when(Axios.post)
        .calledWith(URL, VERIFY)
        .mockResolvedValue({
          data: TestUtils.makeExtResult(EMPTY_DATA),
        });

      await act(async () => userEvent.click(button()));

      expect(Axios.post).toHaveBeenCalledWith(URL, REQUEST);
      expect(success()).toBeInTheDocument();
    });

    it('show error message if the validation fails', async () => {
      when(Axios.post)
        .calledWith(URL, REQUEST)
        .mockResolvedValue({
          data: TestUtils.makeExtResult(DATA),
        });

      const {input, button, failed} = selectors.test;

      await renderAndWaitForLoad();

      await TestUtils.changeField(input, email);

      await act(async () => userEvent.click(button()));

      when(Axios.post)
        .calledWith(URL, VERIFY)
        .mockRejectedValueOnce({
          data: TestUtils.makeExtResult(DATA),
        });

      expect(Axios.post).toHaveBeenCalledWith(URL, REQUEST);

      expect(failed()).toBeInTheDocument();
    });

    it('validate email', async () => {
      when(Axios.post)
        .calledWith(URL, REQUEST)
        .mockResolvedValue({
          data: TestUtils.makeExtResult(DATA),
        });

      const {input, button} = selectors.test;

      await renderAndWaitForLoad();

      expect(button()).toBeDisabled();

      await TestUtils.changeField(input, 'wrong_email');

      expect(button()).toBeDisabled();
    });

    it('removes success message if the input value changes', async () => {
      const {input, button, success, querySuccess} = selectors.test;

      when(Axios.post)
        .calledWith(URL, REQUEST)
        .mockResolvedValue({
          data: TestUtils.makeExtResult(DATA),
        });

      await renderAndWaitForLoad();

      await TestUtils.changeField(input, email);

      when(Axios.post)
        .calledWith(
          URL,
          ExtAPIUtils.createRequestBody(ACTION, METHODS.VERIFY, {
            data: [DATA, email],
          })
        )
        .mockResolvedValue({
          data: TestUtils.makeExtResult(DATA),
        });

      await act(async () => userEvent.click(button()));

      expect(Axios.post).toHaveBeenCalledWith(URL, REQUEST);
      expect(success()).toBeInTheDocument();

      await TestUtils.changeField(input, 'changes');

      expect(querySuccess()).not.toBeInTheDocument();
    });

    it('removes error message if the input value changes', async () => {
      const {input, button, failed, queryFailed} = selectors.test;

      when(Axios.post)
        .calledWith(URL, REQUEST)
        .mockResolvedValue({
          data: TestUtils.makeExtResult(DATA),
        });

      await renderAndWaitForLoad();

      await TestUtils.changeField(input, email);

      when(Axios.post)
        .calledWith(
          URL,
          ExtAPIUtils.createRequestBody(ACTION, METHODS.VERIFY, {
            data: [DATA, email],
          })
        )
        .mockRejectedValueOnce({
          data: TestUtils.makeExtResult({}),
        });

      await act(async () => userEvent.click(button()));

      expect(Axios.post).toHaveBeenCalledWith(URL, REQUEST);
      expect(failed()).toBeInTheDocument();

      await TestUtils.changeField(input, 'changes');

      expect(queryFailed()).not.toBeInTheDocument();
    });
  });

  describe('Read only', () => {
    const data = {
      enabled: true,
      host: 'smtp.gmail.com',
      port: 465,
      username: 'my_user@sonatype.com',
      password: null,
      fromAddress: 'test@sonatype.com',
      subjectPrefix: 'subject',
      startTlsEnabled: true,
      startTlsRequired: true,
      sslOnConnectEnabled: true,
      sslServerIdentityCheckEnabled: true,
      nexusTrustStoreEnabled: true,
    };

    beforeEach(() => {
      when(ExtJS.checkPermission)
        .calledWith('nexus:settings:update')
        .mockReturnValue(false);

      Axios.post.mockReset();
    });

    it('shows default information if email server is not enabled', async () => {
      when(Axios.post)
        .calledWith(URL, REQUEST)
        .mockResolvedValue({
          data: TestUtils.makeExtResult(EMPTY_DATA),
        });

      const {title, enabled, warning, enabledValue} = selectors.readOnly;

      await renderAndWaitForLoad();

      expect(title()).toBeInTheDocument();
      expect(warning()).toBeInTheDocument();
      expect(enabled()).toBeInTheDocument();
      expect(enabledValue()).toHaveTextContent('Disabled');
    });

    it('shows the configuration correctly', async () => {
      when(Axios.post)
        .calledWith(URL, REQUEST)
        .mockResolvedValue({
          data: TestUtils.makeExtResult(data),
        });

      const {
        title,
        warning,
        enabled,
        enabledValue,
        host,
        hostValue,
        port,
        portValue,
        useTruststore,
        useTruststoreValue,
        username,
        usernameValue,
        fromAddress,
        fromAddressValue,
        subjectPrefix,
        subjectPrefixValue,
        options,
        optionsValues,
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
      expect(useTruststore()).toBeInTheDocument();
      expect(useTruststoreValue()).toHaveTextContent('Enabled');
      expect(username()).toBeInTheDocument();
      expect(usernameValue()).toHaveTextContent(data.username);
      expect(fromAddress()).toBeInTheDocument();
      expect(fromAddressValue()).toHaveTextContent(data.fromAddress);
      expect(subjectPrefix()).toBeInTheDocument();
      expect(subjectPrefixValue()).toHaveTextContent(data.subjectPrefix);
      expect(options()).toBeInTheDocument();
      optionsValues.enable.forEach((value) =>
        expect(value()).toBeInTheDocument()
      );
    });

    it('shows the corresponding message when SSL/TLS options are not enabled', async () => {
      const {optionsValues} = selectors.readOnly;
      const newData = {
        ...data,
        startTlsEnabled: false,
        startTlsRequired: false,
        sslOnConnectEnabled: false,
        sslServerIdentityCheckEnabled: false,
      };

      when(Axios.post)
        .calledWith(URL, REQUEST)
        .mockResolvedValue({
          data: TestUtils.makeExtResult(newData),
        });

      await renderAndWaitForLoad();

      optionsValues.notEnable.forEach((value) =>
        expect(value()).toBeInTheDocument()
      );
    });
  });
});
