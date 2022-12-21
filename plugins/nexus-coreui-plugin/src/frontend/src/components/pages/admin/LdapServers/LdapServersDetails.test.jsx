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
import {USER_AND_GROUP_TEMPLATE} from './LdapServers.testdata';

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
  cancelButton: () => screen.queryByText(SETTINGS.CANCEL_BUTTON_LABEL),
  createConnection: {
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
    useTruststore: () => screen.queryByLabelText(USE_TRUST_STORE.DESCRIPTION),
  },
  userAndGroup: {
    template: () => screen.getByLabelText(LABELS.TEMPLATE.LABEL),
    userRelativeDN: () => screen.getByLabelText(LABELS.USER_RELATIVE_DN.LABEL),
    userSubtree: () => screen.getByLabelText(LABELS.USER_SUBTREE.SUB_LABEL),
    objectClass: () => screen.getByLabelText(LABELS.OBJECT_CLASS.LABEL),
    userFilter: () => screen.getByLabelText(LABELS.USER_FILTER.LABEL),
    userIdAttribute: () => screen.getByLabelText(LABELS.USER_ID_ATTRIBUTE),
    realNameAttribute: () => screen.getByLabelText(LABELS.REAL_NAME_ATTRIBUTE),
    emailAttribute: () => screen.getByLabelText(LABELS.EMAIL_ATTRIBUTE),
    passwordAttribute: () =>
      screen.getByLabelText(LABELS.PASSWORD_ATTRIBUTE.LABEL),
    mapLdap: () => screen.getByLabelText(LABELS.MAP_LDAP.SUB_LABEL),
    groupType: () => screen.getByLabelText(LABELS.GROUP_TYPE.LABEL),
    userMemberOfAttribute: () =>
      screen.getByLabelText(LABELS.GROUP_MEMBER_OF_ATTRIBUTE.LABEL),
    groupDn: () => screen.getByLabelText(LABELS.GROUP_DN.LABEL),
    groupSubtree: () => screen.getByLabelText(LABELS.GROUP_SUBTREE.SUB_LABEL),
    groupObjectClass: () =>
      screen.getByLabelText(LABELS.GROUP_OBJECT_CLASS.LABEL),
    groupIdAttribute: () =>
      screen.getByLabelText(LABELS.GROUP_ID_ATTRIBUTE.LABEL),
    groupMemberAttribute: () =>
      screen.getByLabelText(LABELS.GROUP_MEMBER_ATTRIBUTE.LABEL),
    groupMemberFormat: () =>
      screen.getByLabelText(LABELS.GROUP_MEMBER_FORMAT.LABEL),
    saveButton: () => screen.getByText(UIStrings.SETTINGS.SAVE_BUTTON_LABEL),
  },
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
    onDoneMock.mockReset();
  });

  const connectionData = {
    name: 'test-connection',
    protocol: 'ldap',
    host: 'host.example.com',
    port: '610',
    search: 'dc=win,dc=blackforest,dc=local',
    method: 'Simple Authentication',
    username: 'this is my test username',
    password: 'this is the test password',
  };

  const userAndGroupData = {
    userBaseDn: 'cn=users',
    userSubtree: false,
    ldapGroupsAsRoles: false,
    userObjectClass: 'user',
    userLdapFilter: '',
    userIdAttribute: 'sAMAccountName',
    userRealNameAttribute: 'cn',
    userEmailAddressAttribute: 'mail',
    userPasswordAttribute: '',
  };

  const fillConnectionForm = async () => {
    const {
      name,
      protocol,
      host,
      port,
      search,
      authenticationMethod,
      username,
      password,
    } = selectors.createConnection;

    await TestUtils.changeField(name, connectionData.name);
    userEvent.selectOptions(protocol(), connectionData.protocol);
    await TestUtils.changeField(host, connectionData.host);
    await TestUtils.changeField(port, connectionData.port);
    await TestUtils.changeField(search, connectionData.search);
    userEvent.selectOptions(authenticationMethod(), connectionData.method);
    await TestUtils.changeField(username, connectionData.username);
    await TestUtils.changeField(password, connectionData.password);
  };

  describe('Create Connection', () => {
    it('renders the form correctly', async () => {
      const {
        title,
        cancelButton,
        createConnection: {
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
        },
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
      const {nextButton} = selectors.createConnection;

      await renderView();

      await fillConnectionForm();

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
      } = selectors.createConnection;

      await renderView();

      await fillConnectionForm();

      await TestUtils.changeField(name, '');

      expect(nextButton()).toHaveClass('disabled');
      expect(verifyConnectionButton()).toHaveClass('disabled');

      await fillConnectionForm();

      await TestUtils.changeField(host, '');

      expect(nextButton()).toHaveClass('disabled');
      expect(verifyConnectionButton()).toHaveClass('disabled');

      await fillConnectionForm();

      await TestUtils.changeField(port, '');

      expect(nextButton()).toHaveClass('disabled');
      expect(verifyConnectionButton()).toHaveClass('disabled');

      await fillConnectionForm();

      await TestUtils.changeField(search, '');

      expect(nextButton()).toHaveClass('disabled');
      expect(verifyConnectionButton()).toHaveClass('disabled');

      await fillConnectionForm();

      await TestUtils.changeField(username, '');

      expect(nextButton()).toHaveClass('disabled');
      expect(verifyConnectionButton()).toHaveClass('disabled');

      await fillConnectionForm();

      await TestUtils.changeField(password, '');

      expect(nextButton()).toHaveClass('disabled');
      expect(verifyConnectionButton()).toHaveClass('disabled');
    });

    it('users can add the truststore', async () => {
      const {useTruststore, protocol} = selectors.createConnection;

      await renderView();

      await fillConnectionForm();

      userEvent.selectOptions(protocol(), LABELS.PROTOCOL.OPTIONS.ldaps);

      expect(useTruststore()).toBeInTheDocument();
    });

    describe('Verify Connection', () => {
      it('users can verify connection and see a success message', async () => {
        const {verifyConnectionButton} = selectors.createConnection;

        await renderView();

        expect(verifyConnectionButton()).toHaveClass('disabled');

        await fillConnectionForm();

        expect(verifyConnectionButton()).not.toHaveClass('disabled');

        const response = {data: TestUtils.makeExtResult({})};

        Axios.post.mockResolvedValueOnce(response);

        await act(async () => userEvent.click(verifyConnectionButton()));

        expect(ExtJS.showSuccessMessage).toHaveBeenCalledWith(
          LABELS.VERIFY_SUCCESS_MESSAGE(
            generateUrl(
              connectionData.protocol,
              connectionData.host,
              connectionData.port
            )
          )
        );
      });

      it('shows a error message', async () => {
        const {verifyConnectionButton} = selectors.createConnection;

        await renderView();

        expect(verifyConnectionButton()).toHaveClass('disabled');

        await fillConnectionForm();

        expect(verifyConnectionButton()).not.toHaveClass('disabled');

        const response = 'error message';

        Axios.post.mockRejectedValueOnce(response);

        await act(async () => userEvent.click(verifyConnectionButton()));

        expect(ExtJS.showErrorMessage).toHaveBeenCalledWith(response);
      });
    });
  });

  describe('User and Groups', () => {
    const renderViewUserAndGroup = async () => {
      const {nextButton} = selectors.createConnection;
      const response = {data: TestUtils.makeExtResult(USER_AND_GROUP_TEMPLATE)};

      Axios.post.mockResolvedValueOnce(response);

      await renderView();
      await fillConnectionForm();
      await userEvent.click(nextButton());
      await waitForElementToBeRemoved(selectors.queryLoadingMask());
    };

    const templateData = USER_AND_GROUP_TEMPLATE[0];

    it('renders the form correctly', async () => {
      const {
        title,
        userAndGroup: {
          template,
          userRelativeDN,
          userSubtree,
          objectClass,
          userFilter,
          userIdAttribute,
          realNameAttribute,
          emailAttribute,
          passwordAttribute,
          mapLdap,
          groupType,
          userMemberOfAttribute: memberOfAttribute,
          groupDn,
          groupSubtree,
          groupObjectClass,
          groupIdAttribute,
          groupMemberAttribute,
          groupMemberFormat,
        },
      } = selectors;

      await renderViewUserAndGroup();

      expect(title()).toBeInTheDocument();
      expect(template()).toBeInTheDocument();
      expect(userRelativeDN()).toBeInTheDocument();
      expect(userSubtree()).toBeInTheDocument();
      expect(objectClass()).toBeInTheDocument();
      expect(userFilter()).toBeInTheDocument();
      expect(userIdAttribute()).toBeInTheDocument();
      expect(realNameAttribute()).toBeInTheDocument();
      expect(emailAttribute()).toBeInTheDocument();
      expect(passwordAttribute()).toBeInTheDocument();
      expect(mapLdap()).toBeInTheDocument();
      expect(groupType()).toBeInTheDocument();
      expect(memberOfAttribute()).toBeInTheDocument();

      userEvent.selectOptions(groupType(), LABELS.GROUP_TYPE.OPTIONS.static.id);

      expect(groupDn()).toBeInTheDocument();
      expect(groupSubtree()).toBeInTheDocument();
      expect(groupObjectClass()).toBeInTheDocument();
      expect(groupIdAttribute()).toBeInTheDocument();
      expect(groupMemberAttribute()).toBeInTheDocument();
      expect(groupMemberFormat()).toBeInTheDocument();
    });

    it('Users can cancel', async () => {
      const {cancelButton} = selectors;

      await renderViewUserAndGroup();

      expect(cancelButton()).toBeInTheDocument();

      await userEvent.click(cancelButton());

      expect(onDoneMock).toHaveBeenCalled();
    });

    it('Users can select a template and preload default values', async () => {
      const {
        template,
        saveButton,
        userRelativeDN,
        userSubtree,
        objectClass,
        userFilter,
        userIdAttribute,
        realNameAttribute,
        emailAttribute,
        passwordAttribute,
        mapLdap,
        groupType,
        userMemberOfAttribute,
      } = selectors.userAndGroup;

      await renderViewUserAndGroup();

      expect(saveButton()).toHaveClass('disabled');

      userEvent.selectOptions(template(), templateData.name);

      expect(userRelativeDN()).toHaveValue(templateData.userBaseDn);
      expect(userSubtree()).not.toBeChecked();
      expect(objectClass()).toHaveValue(templateData.userObjectClass);
      expect(userFilter()).toHaveValue('');
      expect(userIdAttribute()).toHaveValue(templateData.userIdAttribute);
      expect(realNameAttribute()).toHaveValue(
        templateData.userRealNameAttribute
      );
      expect(emailAttribute()).toHaveValue(
        templateData.userEmailAddressAttribute
      );
      expect(passwordAttribute()).toHaveValue('');
      expect(mapLdap()).toBeChecked();
      expect(groupType()).toHaveValue(templateData.groupType);
      expect(userMemberOfAttribute()).toHaveValue(
        templateData.userMemberOfAttribute
      );
      expect(saveButton()).not.toHaveClass('disabled');
    });

    it('Users cannot save if there is an invalid value', async () => {
      const {
        saveButton,
        userRelativeDN,
        userSubtree,
        objectClass,
        userFilter,
        userIdAttribute,
        realNameAttribute,
        emailAttribute,
        passwordAttribute,
        mapLdap,
      } = selectors.userAndGroup;
      await renderViewUserAndGroup();

      expect(saveButton()).toHaveClass('disabled');

      await TestUtils.changeField(userRelativeDN, userAndGroupData.userBaseDn);
      expect(saveButton()).toHaveClass('disabled');

      userEvent.click(userSubtree());
      expect(saveButton()).toHaveClass('disabled');

      await TestUtils.changeField(
        objectClass,
        userAndGroupData.userObjectClass
      );
      expect(saveButton()).toHaveClass('disabled');

      await TestUtils.changeField(userFilter, userAndGroupData.userLdapFilter);
      expect(saveButton()).toHaveClass('disabled');

      await TestUtils.changeField(
        userIdAttribute,
        userAndGroupData.userIdAttribute
      );
      expect(saveButton()).toHaveClass('disabled');

      await TestUtils.changeField(
        realNameAttribute,
        userAndGroupData.userRealNameAttribute
      );
      expect(saveButton()).toHaveClass('disabled');

      await TestUtils.changeField(
        emailAttribute,
        userAndGroupData.userEmailAddressAttribute
      );
      expect(saveButton()).toHaveClass('disabled');

      await TestUtils.changeField(
        passwordAttribute,
        userAndGroupData.userPasswordAttribute
      );
      expect(saveButton()).toHaveClass('disabled');

      userEvent.click(mapLdap());

      expect(saveButton()).not.toHaveClass('disabled');
    });

    it('Users can save the form and see a success message', async () => {
      const {template, saveButton} = selectors.userAndGroup;

      await renderViewUserAndGroup();

      userEvent.selectOptions(template(), templateData.name);

      expect(saveButton()).not.toHaveClass('disabled');

      Axios.post.mockResolvedValueOnce({});

      await act(async () => userEvent.click(saveButton()));

      expect(onDoneMock).toHaveBeenCalled();

      expect(ExtJS.showSuccessMessage).toHaveBeenCalledWith(
        LABELS.SAVE_SUCCESS_MESSAGE(connectionData.name)
      );
    });

    it('Users cannot save the form if there is an error and see an error message', async () => {
      const {template, saveButton} = selectors.userAndGroup;
      const errorMessage = 'Failed to create the LDAP';

      await renderViewUserAndGroup();

      expect(saveButton()).toHaveClass('disabled');

      userEvent.selectOptions(template(), templateData.name);

      expect(saveButton()).not.toHaveClass('disabled');

      Axios.post.mockRejectedValueOnce({response: {data: errorMessage}});

      await act(async () => userEvent.click(saveButton()));

      expect(onDoneMock).not.toHaveBeenCalled();
      expect(ExtJS.showErrorMessage).toHaveBeenCalledWith(errorMessage);
    });
  });
});
