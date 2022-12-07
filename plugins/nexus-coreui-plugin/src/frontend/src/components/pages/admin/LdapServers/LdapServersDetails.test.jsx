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
import {when} from 'jest-when';
import {
  render,
  screen,
  waitForElementToBeRemoved,
  act,
} from '@testing-library/react';
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import userEvent from '@testing-library/user-event';
import LdapServersDetails from './LdapServersDetails';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import UIStrings from '../../../../constants/UIStrings';

import {generateUrl} from './LdapServersHelper';

const {
  LDAP_SERVERS: {FORM: LABELS},
  SETTINGS,
  USE_TRUST_STORE,
} = UIStrings;

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    checkPermission: jest.fn(),
    showErrorMessage: jest.fn(),
    showSuccessMessage: jest.fn(),
  },
}));

jest.mock('axios', () => ({
  ...jest.requireActual('axios'),
  post: jest.fn(),
}));

const selectors = {
  ...TestUtils.selectors,
  title: () => screen.queryByText(LABELS.CONFIGURATION),
  name: () => screen.getByLabelText(LABELS.NAME),
  setting: () => screen.getByText(LABELS.SETTINGS.LABEL),
  settingText: () => screen.getByText(LABELS.SETTINGS.SUB_LABEL),
  protocol: () => screen.getByLabelText(LABELS.PROTOCOL.LABEL),
  host: () => screen.getByLabelText(LABELS.HOSTNAME),
  port: () => screen.getByLabelText(LABELS.PORT),
  search: () => screen.getByLabelText(LABELS.SEARCH.LABEL),
  authenticationMethod: () =>
    screen.getByLabelText(LABELS.AUTHENTICATION.LABEL),
  username: () => screen.getByLabelText(LABELS.USERNAME.LABEL),
  password: () => screen.getByLabelText(LABELS.PASSWORD.LABEL),
  connection: () => screen.getByText(LABELS.CONNECTION_RULES.LABEL),
  connectionText: () => screen.getByText(LABELS.CONNECTION_RULES.SUB_LABEL),
  waitTimeout: () => screen.getByLabelText(LABELS.WAIT_TIMEOUT.LABEL),
  retryTimeout: () => screen.getByLabelText(LABELS.RETRY_TIMEOUT.LABEL),
  maxRetries: () => screen.getByLabelText(LABELS.MAX_RETRIES.LABEL),
  verifyConnectionButton: () => screen.getByText(LABELS.VERIFY_CONNECTION),
  nextButton: () => screen.queryByText(LABELS.NEXT),
  cancelButton: () => screen.getByText(SETTINGS.CANCEL_BUTTON_LABEL),
  useTruststore: () => screen.queryByLabelText(USE_TRUST_STORE.DESCRIPTION),
};

describe('LdapServersDetails', () => {
  const onDoneMock = jest.fn();

  const renderView = async () => {
    const result = render(<LdapServersDetails onDone={onDoneMock} />);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
    return result;
  };

  beforeEach(() => {
    when(ExtJS.checkPermission).mockReturnValue(true);
  });

  describe('Create Connection', () => {
    const data = {
      name: 'test-connection',
      protocol: 'ldap',
      host: 'host.example.com',
      port: '610',
      search: 'dc=win,dc=blackforest,dc=local',
      method: 'Simple Authentication',
      username: 'this is my test username',
      password: 'this is the test password',
    };

    const fillForm = async () => {
      const {
        name,
        protocol,
        host,
        port,
        search,
        authenticationMethod,
        username,
        password,
      } = selectors;

      await TestUtils.changeField(name, data.name);
      userEvent.selectOptions(protocol(), data.protocol);
      await TestUtils.changeField(host, data.host);
      await TestUtils.changeField(port, data.port);
      await TestUtils.changeField(search, data.search);
      userEvent.selectOptions(authenticationMethod(), data.method);
      await TestUtils.changeField(username, data.username);
      await TestUtils.changeField(password, data.password);
    };

    it('renders the form correctly', async () => {
      const {
        title,
        name,
        setting,
        settingText,
        protocol,
        host,
        port,
        search,
        authenticationMethod,
        connection,
        connectionText,
        waitTimeout,
        retryTimeout,
        maxRetries,
        verifyConnectionButton,
        nextButton,
        cancelButton,
      } = selectors;

      await renderView();

      expect(title()).toBeInTheDocument();
      expect(name()).toBeInTheDocument();
      expect(setting()).toBeInTheDocument();
      expect(settingText()).toBeInTheDocument();
      expect(protocol()).toBeInTheDocument();
      expect(host()).toBeInTheDocument();
      expect(port()).toBeInTheDocument();
      expect(search()).toBeInTheDocument();
      expect(authenticationMethod()).toBeInTheDocument();
      expect(connection()).toBeInTheDocument();
      expect(connectionText()).toBeInTheDocument();
      expect(waitTimeout()).toBeInTheDocument();
      expect(retryTimeout()).toBeInTheDocument();
      expect(maxRetries()).toBeInTheDocument();
      expect(verifyConnectionButton()).toBeInTheDocument();
      expect(nextButton()).toBeInTheDocument();
      expect(cancelButton()).toBeInTheDocument();
    });

    it('users can go to the Users and Group configuration', async () => {
      const {nextButton} = selectors;

      await renderView();

      await fillForm();

      expect(nextButton()).not.toHaveClass('disabled');

      await userEvent.click(nextButton());

      expect(nextButton()).not.toBeInTheDocument();
    });

    it('users can cancel', async () => {
      const {title, cancelButton} = selectors;

      await renderView();

      expect(title()).toBeInTheDocument();

      userEvent.click(cancelButton());

      expect(onDoneMock).toHaveBeenCalled();
    });

    it('users cannot go to the Users and Group configuration if there is an invalid value', async () => {
      const {
        name,
        host,
        port,
        search,
        username,
        password,
        nextButton,
        verifyConnectionButton,
      } = selectors;

      await renderView();

      await fillForm();

      await TestUtils.changeField(name, '');

      expect(nextButton()).toHaveClass('disabled');
      expect(verifyConnectionButton()).toHaveClass('disabled');

      await fillForm();

      await TestUtils.changeField(host, '');

      expect(nextButton()).toHaveClass('disabled');
      expect(verifyConnectionButton()).toHaveClass('disabled');

      await fillForm();

      await TestUtils.changeField(port, '');

      expect(nextButton()).toHaveClass('disabled');
      expect(verifyConnectionButton()).toHaveClass('disabled');

      await fillForm();

      await TestUtils.changeField(search, '');

      expect(nextButton()).toHaveClass('disabled');
      expect(verifyConnectionButton()).toHaveClass('disabled');

      await fillForm();

      await TestUtils.changeField(username, '');

      expect(nextButton()).toHaveClass('disabled');
      expect(verifyConnectionButton()).toHaveClass('disabled');

      await fillForm();

      await TestUtils.changeField(password, '');

      expect(nextButton()).toHaveClass('disabled');
      expect(verifyConnectionButton()).toHaveClass('disabled');
    });

    it('users can add the truststore', async () => {
      const {useTruststore, protocol} = selectors;

      await renderView();

      await fillForm();

      userEvent.selectOptions(protocol(), 'ldaps');

      expect(useTruststore()).toBeInTheDocument();
    });

    describe('Verify Connection', () => {
      it('users can verify connection and see a success message', async () => {
        const {verifyConnectionButton} = selectors;

        await renderView();

        expect(verifyConnectionButton()).toHaveClass('disabled');

        await fillForm();

        expect(verifyConnectionButton()).not.toHaveClass('disabled');

        const response = {data: TestUtils.makeExtResult({})};

        Axios.post.mockResolvedValueOnce(response);

        await act(async () => userEvent.click(verifyConnectionButton()));

        expect(ExtJS.showSuccessMessage).toHaveBeenCalledWith(
          LABELS.SUCCESS_MESSAGE(
            generateUrl(data.protocol, data.host, data.port)
          )
        );
      });

      it('shows a error message', async () => {
        const {verifyConnectionButton} = selectors;

        await renderView();

        expect(verifyConnectionButton()).toHaveClass('disabled');

        await fillForm();

        expect(verifyConnectionButton()).not.toHaveClass('disabled');

        const response = 'error message';

        Axios.post.mockRejectedValueOnce(response);

        await act(async () => userEvent.click(verifyConnectionButton()));

        expect(ExtJS.showErrorMessage).toHaveBeenCalledWith(response);
      });
    });
  });
});
