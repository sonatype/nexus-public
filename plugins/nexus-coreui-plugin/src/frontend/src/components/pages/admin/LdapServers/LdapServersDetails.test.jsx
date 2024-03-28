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
import {omit} from 'ramda';
import {
  render,
  screen,
  waitForElementToBeRemoved,
  within,
  waitFor,
} from '@testing-library/react';
import {
  ExtJS,
  Permissions,
  APIConstants,
  FormUtils,
} from '@sonatype/nexus-ui-plugin';
import userEvent from '@testing-library/user-event';
import LdapServersDetails from './LdapServersDetails';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import UIStrings from '../../../../constants/UIStrings';
import {USER_AND_GROUP_TEMPLATE, LDAP_SERVERS} from './LdapServers.testdata';

const {EXT} = APIConstants;

import {
  generateUrl,
  URL,
  findAuthMethod,
  findGroupType,
} from './LdapServersHelper';

const {singleLdapServersUrl} = URL;

const {
  LDAP_SERVERS: {
    FORM: LABELS,
    FORM: {
      VERIFY_USER_MAPPING_BUTTON,
      VERIFY_LOGIN_BUTTON,
      MODAL_PASSWORD,
      MODAL_VERIFY_LOGIN,
      MODAL_VERIFY_USER_MAPPING
  }
  },
  SETTINGS,
  USE_TRUST_STORE,
  CLOSE
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
  get: jest.fn(),
  delete: jest.fn(),
  put: jest.fn(),
}));

const selectors = {
  ...TestUtils.selectors,
  ...TestUtils.formSelectors,
  title: () => screen.queryByText(LABELS.CONFIGURATION),
  cancelButton: () => screen.queryByText(SETTINGS.CANCEL_BUTTON_LABEL),
  panel: () => screen.getByRole('tabpanel'),
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
    deleteButton: () =>
      screen.queryByText(LABELS.DELETE_BUTTON).closest('button'),
    changePasswordButton: () => {
      const element = screen.queryByText(LABELS.CHANGE_PASSWORD);
      return element?.closest('button') || element;
    },
    confirmDeleteLabel: () => screen.queryByText(LABELS.MODAL_DELETE.LABEL),
    modalDelete: {
      container: () => screen.queryByRole('dialog'),
      confirmButton: () =>
        within(selectors.createConnection.modalDelete.container()).queryByText(
          LABELS.MODAL_DELETE.CONFIRM
        ),
      cancelButton: () =>
        within(selectors.createConnection.modalDelete.container()).queryByText(
          SETTINGS.CANCEL_BUTTON_LABEL
        ),
    },
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
    saveButton: () => screen.queryByText(UIStrings.SETTINGS.SAVE_BUTTON_LABEL),
  },
  tabs: {
    tablist: () => screen.getByRole('tablist'),
    tabConnection: () => screen.getAllByRole('tab')[0],
    tabUserAndGroup: () => screen.getAllByRole('tab')[1],
  },
  modalPassword: {
    title: () => screen.queryByText(LABELS.MODAL_PASSWORD.TITLE),
    password: () => screen.queryByLabelText(LABELS.MODAL_PASSWORD.LABEL),
    container: () => screen.queryByRole('dialog', {name: MODAL_PASSWORD.TITLE}),
    submit: () =>
      within(selectors.modalPassword.container()).queryByText('Submit'),
    cancel: () =>
      within(selectors.modalPassword.container()).queryByText(
        SETTINGS.CANCEL_BUTTON_LABEL
      ),
    errorMessage: () =>
      within(selectors.modalPassword.container()).queryByText(
        'An error occurred saving data. This field is required'
      ),
  },
  modalVerifyLogin: {
    modal: () => screen.queryByRole('dialog', {name: MODAL_VERIFY_LOGIN.TITLE}),
    usernameInput: () => within(selectors.modalVerifyLogin.modal()).getByLabelText(MODAL_VERIFY_LOGIN.USERNAME.LABEL),
    passwordInput: () => within(selectors.modalVerifyLogin.modal()).getByLabelText(MODAL_VERIFY_LOGIN.PASSWORD.LABEL),
    testConnectionButton: () => within(selectors.modalVerifyLogin.modal()).getByText(MODAL_VERIFY_LOGIN.TEST_CONNECTION_BUTTON),
    cancelButton: () => within(selectors.modalVerifyLogin.modal()).getByText(SETTINGS.CANCEL_BUTTON_LABEL),
    errorMessage: () => within(selectors.modalVerifyLogin.modal())
      .getByText((str) => str.startsWith(MODAL_VERIFY_LOGIN.ERROR_MESSAGE)),
    successMessage: () => within(selectors.modalVerifyLogin.modal())
      .getByText((str) => str.startsWith(MODAL_VERIFY_LOGIN.SUCCESS_MESSAGE))
  },
  modalVerifyUserMapping: {
    modal: () => screen.queryByRole('dialog', {name: MODAL_VERIFY_USER_MAPPING.TITLE}),
    closeButton: () => within(selectors.modalVerifyUserMapping.modal()).getByText(CLOSE),
    rows: () => within(selectors.modalVerifyUserMapping.modal()).getAllByRole('row'),
    errorMessage: () => within(selectors.modalVerifyUserMapping.modal())
      .getByText((str) => str.startsWith(TestUtils.LOADING_ERROR)),
  },
  modalVerifyUserMappingItem: {
    modal: (name) => screen.queryByRole('dialog', {name}),
    backButton: (name) => within(selectors.modalVerifyUserMappingItem.modal(name))
      .getByText(MODAL_VERIFY_USER_MAPPING.ITEM.BACK_BUTTON),
  },
  verifyLoginButton: () => screen.queryByText(VERIFY_LOGIN_BUTTON),
  verifyUserMappingButton: () => screen.queryByText(VERIFY_USER_MAPPING_BUTTON)
};

describe('LdapServersDetails', () => {
  const onDoneMock = jest.fn();

  const renderView = async (itemId) => {
    const result = render(
      <LdapServersDetails onDone={onDoneMock} itemId={itemId} />
    );
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
    return result;
  };

  const renderViewUserAndGroup = async () => {
    const {nextButton} = selectors.createConnection;

    const response = {data: TestUtils.makeExtResult(USER_AND_GROUP_TEMPLATE)};

    when(Axios.post)
      .calledWith(EXT.URL, expect.objectContaining({
        action: EXT.LDAP.ACTION,
        method: EXT.LDAP.METHODS.READ_TEMPLATES
      }))
      .mockResolvedValue(response)

    await renderView();
    await fillConnectionForm();
    userEvent.click(nextButton());
  };

  beforeEach(() => {
    when(ExtJS.checkPermission)
      .calledWith(Permissions.LDAP.UPDATE)
      .mockReturnValue(true);

    onDoneMock.mockReset();
  });

  const connectionData = {
    name: 'test-connection',
    protocol: 'LDAP',
    host: 'host.example.com',
    port: '610',
    searchBase: 'dc=win,dc=blackforest,dc=local',
    authScheme: 'SIMPLE',
    authUsername: 'this is my test username',
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

  const fillConnectionForm = async (data = connectionData, isEdit = false) => {
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

    await TestUtils.changeField(name, data.name);
    userEvent.selectOptions(protocol(), data.protocol);
    await TestUtils.changeField(host, data.host);
    await TestUtils.changeField(port, data.port);
    await TestUtils.changeField(search, data.searchBase);
    userEvent.selectOptions(authenticationMethod(), data.authScheme);
    await TestUtils.changeField(username, data.authUsername);

    if (!isEdit) {
      await TestUtils.changeField(password, data.password);
    }
  };

  const VERIFY_CONNECTION_REQUEST = expect.objectContaining({
    action: EXT.LDAP.ACTION,
    method: EXT.LDAP.METHODS.VERIFY_CONNECTION,
  });

  const itemId = 'test-item-id';

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

      userEvent.click(nextButton());

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
        verifyConnectionButton,
      } = selectors.createConnection;

      await renderView();

      await fillConnectionForm();

      await TestUtils.changeField(name, '');

      expect(verifyConnectionButton()).toHaveClass('disabled');

      await fillConnectionForm();

      await TestUtils.changeField(host, '');

      expect(verifyConnectionButton()).toHaveClass('disabled');

      await fillConnectionForm();

      await TestUtils.changeField(port, '');

      expect(verifyConnectionButton()).toHaveClass('disabled');

      await fillConnectionForm();

      await TestUtils.changeField(search, '');

      expect(verifyConnectionButton()).toHaveClass('disabled');

      await fillConnectionForm();

      await TestUtils.changeField(username, '');

      expect(verifyConnectionButton()).toHaveClass('disabled');

      await fillConnectionForm();

      await TestUtils.changeField(password, '');

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

        userEvent.click(verifyConnectionButton());

        await waitFor(() => expect(Axios.post).toHaveBeenCalledWith(
          EXT.URL,
          VERIFY_CONNECTION_REQUEST
        ));
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

        userEvent.click(verifyConnectionButton());

        await waitFor(() => expect(ExtJS.showErrorMessage).toHaveBeenCalledWith(response));
      });
    });
  });

  describe('User and Groups', () => {
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

      userEvent.click(cancelButton());

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

      await TestUtils.changeField(userRelativeDN, userAndGroupData.userBaseDn);
      userEvent.click(saveButton());
      expect(
        selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)
      ).toBeInTheDocument();

      userEvent.click(userSubtree());
      expect(
        selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)
      ).toBeInTheDocument();

      await TestUtils.changeField(
        objectClass,
        userAndGroupData.userObjectClass
      );
      expect(
        selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)
      ).toBeInTheDocument();

      await TestUtils.changeField(userFilter, userAndGroupData.userLdapFilter);
      expect(
        selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)
      ).toBeInTheDocument();

      await TestUtils.changeField(
        userIdAttribute,
        userAndGroupData.userIdAttribute
      );
      expect(
        selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)
      ).toBeInTheDocument();

      await TestUtils.changeField(
        realNameAttribute,
        userAndGroupData.userRealNameAttribute
      );
      expect(
        selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)
      ).toBeInTheDocument();

      await TestUtils.changeField(
        emailAttribute,
        userAndGroupData.userEmailAddressAttribute
      );
      expect(
        selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)
      ).toBeInTheDocument();

      await TestUtils.changeField(
        passwordAttribute,
        userAndGroupData.userPasswordAttribute
      );
      expect(
        selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)
      ).toBeInTheDocument();

      userEvent.click(mapLdap());

      expect(saveButton()).not.toHaveClass('disabled');
    });

    it('Users can save the form and see a success message', async () => {
      const {template, saveButton} = selectors.userAndGroup;

      await renderViewUserAndGroup();

      userEvent.selectOptions(template(), templateData.name);

      expect(saveButton()).not.toHaveClass('disabled');

      Axios.post.mockResolvedValueOnce({});

      userEvent.click(saveButton());

      await waitFor(() => expect(onDoneMock).toHaveBeenCalled());

      expect(ExtJS.showSuccessMessage).toHaveBeenCalledWith(
        LABELS.SAVE_SUCCESS_MESSAGE(connectionData.name)
      );
    });

    it('Users cannot save the form if there is an error and see an error message', async () => {
      const {template, saveButton} = selectors.userAndGroup;
      const errorMessage = 'Failed to create the LDAP';

      await renderViewUserAndGroup();

      userEvent.selectOptions(template(), templateData.name);

      expect(saveButton()).not.toHaveClass('disabled');

      Axios.post.mockRejectedValueOnce({response: {data: errorMessage}});

      userEvent.click(saveButton());

      await waitFor(() => expect(onDoneMock).not.toHaveBeenCalled());
      expect(ExtJS.showErrorMessage).toHaveBeenCalledWith(errorMessage);
    });
  });

  describe('Edit form', () => {
    const data = LDAP_SERVERS[0];
    const anonymousData = {
      ...data,
      authScheme: 'NONE',
    };

    beforeEach(() => {
      when(ExtJS.checkPermission)
        .calledWith(Permissions.LDAP.DELETE)
        .mockReturnValue(true);

      when(ExtJS.checkPermission)
        .calledWith(Permissions.LDAP.UPDATE)
        .mockReturnValue(true);

      when(Axios.get)
        .calledWith(singleLdapServersUrl(itemId))
        .mockReturnValue({data});

      const response = {
        data: TestUtils.makeExtResult(USER_AND_GROUP_TEMPLATE),
      };

      Axios.post.mockResolvedValue(response);
    });

    it('Users can see and use tabs to switch between the connection form and user and group form', async () => {
      const {tabConnection, tabUserAndGroup, tablist} = selectors.tabs;

      await renderView(itemId);

      expect(tablist()).toBeInTheDocument();
      expect(tabConnection().textContent).toContain(LABELS.TABS.CONNECTION);
      expect(tabUserAndGroup().textContent).toContain(
        LABELS.TABS.USER_AND_GROUP
      );
    });

    it('Users move between the forms using the tabs', async () => {
      const {
        tabs: {tabConnection, tabUserAndGroup, tablist},
        createConnection: {nextButton},
        userAndGroup: {saveButton},
      } = selectors;

      await renderView(itemId);

      expect(tablist()).toBeInTheDocument();
      expect(nextButton()).toBeInTheDocument();
      expect(saveButton()).not.toBeInTheDocument();

      userEvent.click(tabUserAndGroup());

      expect(nextButton()).not.toBeInTheDocument();
      expect(saveButton()).toBeInTheDocument();

      userEvent.click(tabConnection());

      expect(nextButton()).toBeInTheDocument();
      expect(saveButton()).not.toBeInTheDocument();
    });

    it('Users cannot move to the User and Group tab if there is invalid value', async () => {
      const {
        tabs: {tabUserAndGroup},
        createConnection: {nextButton, name},
        userAndGroup: {saveButton},
      } = selectors;

      await renderView(itemId);

      expect(nextButton()).toBeInTheDocument();
      expect(saveButton()).not.toBeInTheDocument();

      await TestUtils.changeField(name, '');

      userEvent.click(tabUserAndGroup());

      await waitFor(() => expect(nextButton()).toBeInTheDocument());
      expect(saveButton()).not.toBeInTheDocument();
      expect(
        selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)
      ).toBeInTheDocument();
    });

    it('The LDAP server configuration is loaded properly', async () => {
      const {
        createConnection: {
          name,
          protocol,
          host,
          port,
          search,
          authenticationMethod,
          username,
          waitTimeout,
          retryTimeout,
          maxRetries,
          nextButton,
        },
        userAndGroup: {
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
        },
      } = selectors;

      await renderView(itemId);

      expect(name()).toHaveValue(data.name);
      expect(protocol()).toHaveValue(data.protocol);
      expect(host()).toHaveValue(data.host);
      expect(port()).toHaveValue(String(data.port));
      expect(search()).toHaveValue(data.searchBase);
      expect(authenticationMethod()).toHaveValue(data.authScheme);
      expect(username()).toHaveValue(data.authUsername);
      expect(waitTimeout()).toHaveValue(String(data.connectionTimeoutSeconds));
      expect(retryTimeout()).toHaveValue(
        String(data.connectionRetryDelaySeconds)
      );
      expect(maxRetries()).toHaveValue(String(data.maxIncidentsCount));

      userEvent.click(nextButton());

      expect(userRelativeDN()).toHaveValue(data.userBaseDn);
      expect(userSubtree()).not.toBeChecked();
      expect(objectClass()).toHaveValue(data.userObjectClass);
      expect(userFilter()).toHaveValue(data.userLdapFilter);
      expect(userIdAttribute()).toHaveValue(data.userIdAttribute);
      expect(realNameAttribute()).toHaveValue(data.userRealNameAttribute);
      expect(emailAttribute()).toHaveValue(data.userEmailAddressAttribute);
      expect(passwordAttribute()).toHaveValue(data.userPasswordAttribute);
      expect(mapLdap()).toBeChecked();
      expect(groupType()).toHaveValue(data.groupType);
      expect(userMemberOfAttribute()).toHaveValue(data.userMemberOfAttribute);
    });

    it('Users can delete the LDAP Server configuration', async () => {
      const {
        deleteButton,
        confirmDeleteLabel,
        modalDelete: {confirmButton, cancelButton},
      } = selectors.createConnection;

      await renderView(itemId);

      expect(deleteButton()).toBeInTheDocument();

      userEvent.click(deleteButton());

      expect(confirmDeleteLabel()).toBeInTheDocument();
      expect(confirmButton()).toBeInTheDocument();
      expect(cancelButton()).toBeInTheDocument();

      userEvent.click(cancelButton());

      expect(confirmDeleteLabel()).not.toBeInTheDocument();

      userEvent.click(deleteButton());

      userEvent.click(confirmButton());

      await waitFor(() => expect(Axios.delete).toHaveBeenCalledWith(
        singleLdapServersUrl(data.name)
      ));
    });

    it('Users cannot delete the LDAP Server configuration if they do not have enough permissions', async () => {
      when(ExtJS.checkPermission)
        .calledWith(Permissions.LDAP.DELETE)
        .mockReturnValue(false);

      const {deleteButton} = selectors.createConnection;

      await renderView(itemId);

      expect(deleteButton()).toBeInTheDocument();
      expect(deleteButton()).toHaveClass('disabled');
    });

    it('Users see a modal when trying to change the password, then the form is saved', async () => {
      const {
        createConnection: {changePasswordButton},
        modalPassword: {title, password, submit, cancel, errorMessage},
      } = selectors;

      await renderView(itemId);

      expect(changePasswordButton()).toBeInTheDocument();
      expect(changePasswordButton()).not.toHaveClass('disabled');

      userEvent.click(changePasswordButton());

      expect(title()).toBeInTheDocument();
      expect(password()).toBeInTheDocument();

      userEvent.click(cancel());

      expect(title()).not.toBeInTheDocument();
      expect(password()).not.toBeInTheDocument();

      userEvent.click(changePasswordButton());

      expect(title()).toBeInTheDocument();
      expect(password()).toBeInTheDocument();

      userEvent.click(submit());

      expect(errorMessage()).toBeInTheDocument();

      await TestUtils.changeField(password, connectionData.password);

      userEvent.click(submit());

      await waitFor(() => expect(title()).not.toBeInTheDocument());
      expect(password()).not.toBeInTheDocument();

      const expected = {
        ...omit(['connectionRetryDelay', 'connectionTimeout'], data),
        authPassword: connectionData.password,
      };

      expect(Axios.post).toHaveBeenCalledWith(
        EXT.URL,
        VERIFY_CONNECTION_REQUEST
      );
      expect(ExtJS.showSuccessMessage).toHaveBeenCalledWith(
        LABELS.UPDATE_SUCCESS_MESSAGE(data.name)
      );
      expect(Axios.put).toHaveBeenCalledWith(
        singleLdapServersUrl(data.name),
        expected
      );
    });

    it('if the auth scheme is anonymous should not show the change password button', async () => {
      when(Axios.get)
        .calledWith(singleLdapServersUrl(itemId))
        .mockReturnValue({data: anonymousData});

      const {
        createConnection: {changePasswordButton},
      } = selectors;

      await renderView(itemId);

      expect(changePasswordButton()).not.toBeInTheDocument();
    });

    it('if the auth scheme is anonymous should not ask for the password when verifying the connection', async () => {
      when(Axios.get)
        .calledWith(singleLdapServersUrl(itemId))
        .mockReturnValue({data: anonymousData});

      const {
        createConnection: {verifyConnectionButton},
        modalPassword: {title, password},
      } = selectors;

      await renderView(itemId);

      userEvent.click(verifyConnectionButton());

      await waitFor(() => expect(title()).not.toBeInTheDocument());
      expect(password()).not.toBeInTheDocument();

      const response = {data: TestUtils.makeExtResult({})};

      Axios.post.mockResolvedValue(response);

      expect(Axios.post).toHaveBeenCalledWith(
        EXT.URL,
        VERIFY_CONNECTION_REQUEST
      );
      expect(ExtJS.showSuccessMessage).toHaveBeenCalledWith(
        LABELS.VERIFY_SUCCESS_MESSAGE(
          generateUrl(data.protocol, data.host, data.port)
        )
      );
    });

    it('Users can verify connection with the new values', async () => {
      const {
        createConnection: {verifyConnectionButton},
        modalPassword: {title, password, submit},
      } = selectors;

      await renderView(itemId);

      expect(verifyConnectionButton()).not.toHaveClass('disabled');

      userEvent.click(verifyConnectionButton());

      expect(title()).toBeInTheDocument();
      expect(password()).toBeInTheDocument();

      await TestUtils.changeField(password, connectionData.password);

      const response = {data: TestUtils.makeExtResult({})};
      Axios.post.mockResolvedValue(response);

      userEvent.click(submit());

      await waitFor(() => expect(Axios.post).toHaveBeenCalledWith(
        EXT.URL,
        VERIFY_CONNECTION_REQUEST
      ));
      expect(ExtJS.showSuccessMessage).toHaveBeenCalledWith(
        LABELS.VERIFY_SUCCESS_MESSAGE(
          generateUrl(data.protocol, data.host, data.port)
        )
      );
    });

    it('Users should be able to change and save the configuration', async () => {
      let newData = LDAP_SERVERS[1];
      newData.port = String(newData.port);

      const templateData = USER_AND_GROUP_TEMPLATE[1];

      const {
        createConnection: {nextButton},
        userAndGroup: {template, saveButton},
        modalPassword: {title, password, submit},
      } = selectors;

      await renderView(itemId);

      await fillConnectionForm(newData, true);

      userEvent.click(nextButton());

      userEvent.selectOptions(template(), templateData.name);

      userEvent.click(saveButton());

      await waitFor(() => expect(title()).toBeInTheDocument());
      expect(password()).toBeInTheDocument();

      await TestUtils.changeField(password, connectionData.password);

      userEvent.click(submit());

      await waitFor(() => expect(ExtJS.showSuccessMessage).toHaveBeenCalledWith(
        LABELS.UPDATE_SUCCESS_MESSAGE(newData.name)
      ));

      const expected = {
        ...omit(['connectionRetryDelay', 'connectionTimeout'], data), // Omits not needed values.
        ...omit(['id', 'order'], newData), // We cannot change the id and order, so keeps the original values.
        ...omit(['name'], templateData), // Omits the template name, not needed
        authPassword: connectionData.password,
      };

      expect(Axios.post).toHaveBeenCalledWith(
        EXT.URL,
        VERIFY_CONNECTION_REQUEST
      );
      expect(ExtJS.showSuccessMessage).toHaveBeenCalledWith(
        LABELS.UPDATE_SUCCESS_MESSAGE(expected.name)
      );
      expect(Axios.put).toHaveBeenCalledWith(
        singleLdapServersUrl(expected.name),
        expected
      );
    });

    it('if the auth scheme is anonymous should not ask for the password when saving the form', async () => {
      const authScheme = 'NONE';
      let newData = LDAP_SERVERS[1];
      newData.port = String(newData.port);

      const templateData = USER_AND_GROUP_TEMPLATE[1];

      const {
        createConnection: {nextButton, authenticationMethod},
        userAndGroup: {template, saveButton},
        modalPassword: {title, password},
      } = selectors;

      await renderView(itemId);

      await fillConnectionForm(newData, true);

      userEvent.selectOptions(authenticationMethod(), authScheme);

      userEvent.click(nextButton());

      userEvent.selectOptions(template(), templateData.name);

      userEvent.click(saveButton());

      await waitFor(() => expect(title()).not.toBeInTheDocument());
      expect(password()).not.toBeInTheDocument();

      expect(ExtJS.showSuccessMessage).toHaveBeenCalledWith(
        LABELS.UPDATE_SUCCESS_MESSAGE(newData.name)
      );

      const expected = {
        ...omit(['connectionRetryDelay', 'connectionTimeout'], data), // Omits not needed values.
        ...omit(['id', 'order'], newData), // We cannot change the id and order, so keeps the original values.
        ...omit(['name'], templateData), // Omits the template name, not needed
        authPassword: '',
        authScheme,
      };

      expect(Axios.post).toHaveBeenCalledWith(
        EXT.URL,
        VERIFY_CONNECTION_REQUEST
      );
      expect(ExtJS.showSuccessMessage).toHaveBeenCalledWith(
        LABELS.UPDATE_SUCCESS_MESSAGE(expected.name)
      );
      expect(Axios.put).toHaveBeenCalledWith(
        singleLdapServersUrl(expected.name),
        expected
      );
    });
  });

  describe('Read only', () => {
    const data = LDAP_SERVERS[0];

    beforeEach(() => {
      when(ExtJS.checkPermission)
        .calledWith(Permissions.LDAP.UPDATE)
        .mockReturnValue(false);

      when(Axios.get)
        .calledWith(singleLdapServersUrl(itemId))
        .mockReturnValue({data});
    });

    it('should render properly', async () => {
      const {
        panel,
        tabs: {tabConnection, tabUserAndGroup, tablist},
      } = selectors;

      await renderView(itemId);
      const authMethod = findAuthMethod(data.authScheme);
      const useTrustStore = FormUtils.readOnlyCheckboxValueLabel(
        data.useTrustStore
      );
      const userSubtree = FormUtils.readOnlyCheckboxValueLabel(
        data.userSubtree
      );
      const mapLdap = FormUtils.readOnlyCheckboxValueLabel(
        data.ldapGroupsAsRoles
      );
      const groupType = findGroupType(data.groupType);

      expect(tablist()).toBeInTheDocument();
      expect(tabConnection().textContent).toContain(LABELS.TABS.CONNECTION);
      expect(tabUserAndGroup().textContent).toContain(
        LABELS.TABS.USER_AND_GROUP
      );

      expect(within(panel()).queryByText(data.name)).toBeInTheDocument();
      expect(within(panel()).queryByText(data.protocol)).toBeInTheDocument();
      expect(within(panel()).queryByText(data.host)).toBeInTheDocument();
      expect(within(panel()).queryByText(data.port)).toBeInTheDocument();
      expect(within(panel()).queryByText(useTrustStore)).toBeInTheDocument();
      expect(within(panel()).queryByText(authMethod.label)).toBeInTheDocument();
      expect(
        within(panel()).queryByText(data.authUsername)
      ).toBeInTheDocument();
      expect(
        within(panel()).queryByText(data.connectionTimeoutSeconds)
      ).toBeInTheDocument();
      expect(
        within(panel()).queryByText(data.connectionRetryDelaySeconds)
      ).toBeInTheDocument();
      expect(
        within(panel()).queryByText(data.maxIncidentsCount)
      ).toBeInTheDocument();

      userEvent.click(tabUserAndGroup());

      expect(within(panel()).queryByText(data.userBaseDn)).toBeInTheDocument();
      expect(within(panel()).queryByText(userSubtree)).toBeInTheDocument();
      expect(
        within(panel()).queryByText(data.userObjectClass)
      ).toBeInTheDocument();
      expect(
        within(panel()).queryByText(data.userIdAttribute)
      ).toBeInTheDocument();
      expect(
        within(panel()).queryByText(data.userRealNameAttribute)
      ).toBeInTheDocument();
      expect(
        within(panel()).queryByText(data.userEmailAddressAttribute)
      ).toBeInTheDocument();
      expect(within(panel()).queryByText(mapLdap)).toBeInTheDocument();
      expect(within(panel()).queryByText(groupType.label)).toBeInTheDocument();
      expect(
        within(panel()).queryByText(data.userMemberOfAttribute)
      ).toBeInTheDocument();
    });
  });

  describe('verify login', () => {
    const itemId = 'test-item-id';
    const ldapConfig = LDAP_SERVERS[0];
    const authPassword = 'LdapSystemPassword';
    const username = 'ldap-user-1';
    const password = 'ldap-user-1-password';

    beforeEach(() => {
      when(ExtJS.checkPermission)
        .calledWith(Permissions.LDAP.DELETE)
        .mockReturnValue(true);

      when(Axios.get)
        .calledWith(singleLdapServersUrl(itemId))
        .mockReturnValue({ldapConfig});

      when(Axios.post)
        .calledWith(EXT.URL, expect.objectContaining({
          action: EXT.LDAP.ACTION,
          method: EXT.LDAP.METHODS.VERIFY_LOGIN
        }))
        .mockResolvedValue({data: TestUtils.makeExtResult({})});
    });

    const {
      verifyLoginButton, 
      modalVerifyLogin: {
        modal,
        usernameInput,
        passwordInput,
        testConnectionButton,
        cancelButton,
        successMessage,
        errorMessage
      }, 
      modalPassword,
      tabs: {tabUserAndGroup}
    } = selectors;

    it('verifies login in create mode; handles cancel; shows success message', async () => {
      await renderViewUserAndGroup();

      userEvent.click(verifyLoginButton());
      expect(modal()).toBeInTheDocument();

      userEvent.click(cancelButton());
      expect(modal()).not.toBeInTheDocument();

      userEvent.click(verifyLoginButton());

      await TestUtils.changeField(usernameInput, username);
      await TestUtils.changeField(passwordInput, password);

      userEvent.click(testConnectionButton());

      await waitFor(() => expect(successMessage()).toBeInTheDocument());
    });

    it('verifies login in edit mode; asks system password; shows error message', async () => {
      when(Axios.post)
        .calledWith(EXT.URL, expect.objectContaining({
          action: EXT.LDAP.ACTION,
          method: EXT.LDAP.METHODS.VERIFY_LOGIN
        }))
        .mockRejectedValueOnce({});

      await renderView(itemId);
      userEvent.click(tabUserAndGroup());

      userEvent.click(verifyLoginButton());

      await TestUtils.changeField(modalPassword.password, authPassword);
      userEvent.click(modalPassword.submit());
      expect(modal()).toBeInTheDocument();

      await TestUtils.changeField(usernameInput, username);
      await TestUtils.changeField(passwordInput, password);

      userEvent.click(testConnectionButton());

      await waitFor(() => expect(errorMessage()).toBeInTheDocument());
    });
  });

  describe('verify user mapping', () => {
    const itemId = 'test-item-id';
    const ldapConfig = LDAP_SERVERS[0];
    const authPassword = 'LdapSystemPassword';

    const userMapping = [
      {
        username: 'administrator',
        realName: 'Admin Administrator',
        email: 'admin@example.com',
        membership: [
          'Domain Admins',
          'Enterprise Admins',
          'Administrators',
          'Schema Admins'
        ]
      },
      {
        username: 'jdoe',
        realName: 'John Doe',
        email: 'jdoe@example.com',
        membership: [
          'Users'
        ]
      }
    ];

    beforeEach(() => {
      when(ExtJS.checkPermission)
        .calledWith(Permissions.LDAP.DELETE)
        .mockReturnValue(true);

      when(Axios.get)
        .calledWith(singleLdapServersUrl(itemId))
        .mockReturnValue({ldapConfig});

      when(Axios.post)
        .calledWith(EXT.URL, expect.objectContaining({
          action: EXT.LDAP.ACTION,
          method: EXT.LDAP.METHODS.VERIFY_USER_MAPPING
        }))
        .mockResolvedValue({data: TestUtils.makeExtResult(userMapping)});
    });

    const {
      verifyUserMappingButton, 
      modalVerifyUserMapping,
      modalVerifyUserMappingItem,
      modalPassword,
      tabs: {tabUserAndGroup}
    } = selectors;

    it('verifies user mapping in create mode', async () => {
      await renderViewUserAndGroup();

      userEvent.click(verifyUserMappingButton());
      expect(modalVerifyUserMapping.modal()).toBeInTheDocument();
      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      userEvent.click(modalVerifyUserMapping.closeButton());
      expect(modalVerifyUserMapping.modal()).not.toBeInTheDocument();

      userEvent.click(verifyUserMappingButton());

      await waitFor(() => expect(modalVerifyUserMapping.rows()).toHaveLength(3)); 

      userEvent.click(modalVerifyUserMapping.rows()[1]);

      const item = userMapping[0];

      const itemModal = modalVerifyUserMappingItem.modal(item.realName);

      expect(itemModal).toBeInTheDocument();

      const content = itemModal.textContent;

      expect(content).toContain(item.username);
      expect(content).toContain(item.realName);
      expect(content).toContain(item.email);
      expect(content).toContain(item.membership.join(', '));
    });

    it('verifies user mapping in edit mode', async () => {
      when(Axios.post)
        .calledWith(EXT.URL, expect.objectContaining({
          action: EXT.LDAP.ACTION,
          method: EXT.LDAP.METHODS.VERIFY_USER_MAPPING
        }))
        .mockRejectedValueOnce({});

      await renderView(itemId);
      userEvent.click(tabUserAndGroup());

      userEvent.click(verifyUserMappingButton());

      await TestUtils.changeField(modalPassword.password, authPassword);
      userEvent.click(modalPassword.submit());
      expect(modalVerifyUserMapping.modal()).toBeInTheDocument();

      await waitFor(() => expect(modalVerifyUserMapping.errorMessage()).toBeInTheDocument());
    });
  });
});
