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
import userEvent from '@testing-library/user-event';
import {when} from 'jest-when';
import Axios from 'axios';
import {ExtJS, APIConstants, ExtAPIUtils} from '@sonatype/nexus-ui-plugin';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import {
  render,
  screen,
  waitFor,
  waitForElementToBeRemoved,
  getByText,
  queryByText,
  act,
} from '@testing-library/react';

import UIStrings from '../../../../constants/UIStrings';
import UsersDetails from './UsersDetails';

import {ROLES} from './Users.testdata';

import {DEFAULT_SOURCE, URL, STATUSES} from './UsersHelper';

const {
  singleUserUrl,
  createUserUrl,
  defaultRolesUrl,
  findUsersUrl,
  changePasswordUrl,
  resetTokenUrl
} = URL;

const {
  USERS: {
    FORM: LABELS,
    MODAL,
    TOKEN
  },
  SETTINGS
} = UIStrings;

const {EXT: {USER: {ACTION, METHODS}, URL: EXT_URL}} = APIConstants;

jest.mock('axios', () => ({
  ...jest.requireActual('axios'),
  get: jest.fn(),
  put: jest.fn(),
  post: jest.fn(),
  delete: jest.fn(),
}));

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    requestConfirmation: jest.fn(),
    checkPermission: jest.fn(),
    showErrorMessage: jest.fn(),
    showSuccessMessage: jest.fn(),
    state: jest.fn().mockReturnValue({
      getValue: jest.fn(),
      getUser: jest.fn(),
    }),
    isProEdition: jest.fn().mockReturnValue(true),
  },
}));

const testId = 'UserId';
const testFirstName = 'User First Name';
const testLastName = 'User Last Name';
const testEmail = 'test@mail.com';
const testPassword = 'test.test';
const testStatus = 'active';
const testRoles = ['nx-admin'];
const testExternalRoles = ['External_Role_1', 'External_Role_2'];
const testSource = 'default';

const USER = {
  userId: testId,
  firstName: testFirstName,
  lastName: testLastName,
  emailAddress: testEmail,
  source: testSource,
  status: testStatus,
  roles: testRoles,
};

const selectors = {
  ...TestUtils.selectors,
  ...TestUtils.formSelectors,
  id: () => screen.queryByLabelText(LABELS.ID.LABEL),
  firstName: () => screen.queryByLabelText(LABELS.FIRST_NAME.LABEL),
  lastName: () => screen.queryByLabelText(LABELS.LAST_NAME.LABEL),
  password: () => screen.queryByLabelText(LABELS.PASSWORD.LABEL),
  confirmPassword: () => screen.queryByLabelText(LABELS.CONFIRM_PASSWORD.LABEL),
  email: () => screen.queryByLabelText(LABELS.EMAIL.LABEL),
  status: () => screen.queryByLabelText(LABELS.STATUS.OPTIONS.ACTIVE),
  roles: () => screen.queryByRole('group', {name: LABELS.ROLES.GRANTED}),
  externalRoles: () => screen.queryByLabelText(LABELS.EXTERNAL_ROLES.LABEL),
  requiredValidation: () => screen.queryByText(UIStrings.ERROR.FIELD_REQUIRED),
  passwordNoMatchValidation: () => screen.queryByText(UIStrings.ERROR.PASSWORD_NO_MATCH_ERROR),
  invalidEmailValidation: () => screen.queryByText(UIStrings.ERROR.INVALID_EMAIL),
  readOnly: {
    id: () => screen.getByText(LABELS.ID.LABEL).nextSibling,
    firstName: () => screen.getByText(LABELS.FIRST_NAME.LABEL).nextSibling,
    lastName: () => screen.getByText(LABELS.LAST_NAME.LABEL).nextSibling,
    email: () => screen.getByText(LABELS.EMAIL.LABEL).nextSibling,
    status: () => screen.getByText(LABELS.STATUS.LABEL).nextSibling,
    roles: () => screen.queryAllByRole('list')[0],
    warning: () => screen.getByText(SETTINGS.READ_ONLY.WARNING),
    externalRoles: () => screen.getByText(LABELS.EXTERNAL_ROLES.LABEL).nextSibling,
  },
  cancelButton: () => screen.getByText(SETTINGS.CANCEL_BUTTON_LABEL),
  deleteButton: () => screen.queryByText(SETTINGS.DELETE_BUTTON_LABEL),
  modal: {
    openButton: () => screen.getByText(MODAL.CHANGE_PASSWORD),
    container: () => screen.getByLabelText(MODAL.CHANGE_PASSWORD),
    queryModal: () => screen.queryByLabelText(MODAL.CHANGE_PASSWORD),
    inputAdminPassword: () => screen.getByTestId('adminPassword').querySelector('input'),
    inputNewPassword: () => screen.getByTestId('newPassword').querySelector('input'),
    inputConfirmPassword: () => screen.getByTestId('confirmPassword').querySelector('input'),
    text: () => screen.getByText(MODAL.TEXT),
    next: () => screen.getByText(MODAL.NEXT),
    title: () => screen.getByText(MODAL.ADMIN_PASSWORD),
    cancel: () => getByText(selectors.modal.container(), SETTINGS.CANCEL_BUTTON_LABEL),
    save: () => getByText(selectors.modal.container(), SETTINGS.SAVE_BUTTON_LABEL),
    querySave: () => queryByText(selectors.modal.container(), SETTINGS.SAVE_BUTTON_LABEL),
    retryButton: () => screen.queryByText('Retry'),
    queryChangePasswordMask: () => screen.queryByText('Confirming Admin Password')
  },
  token: {
    container: () => screen.getByLabelText(TOKEN.RESET_USER_TOKEN),
    queryModal: () => screen.queryByLabelText(TOKEN.RESET_USER_TOKEN),
    cancel: () => getByText(selectors.token.container(), SETTINGS.CANCEL_BUTTON_LABEL),
    title: () => screen.getByText(TOKEN.LABEL),
    queryTitle: () => screen.queryByText(TOKEN.LABEL),
    link: () => screen.getByText('capabilities page'),
    queryResetButton: () => screen.queryByText(TOKEN.RESET_USER_TOKEN),
    resetButton: () => screen.getByText(TOKEN.RESET_USER_TOKEN, {ignore: 'span'}),
    activeLabel: () => screen.getByText(TOKEN.TEXT),
    modalText: () => screen.getByText(TOKEN.ACTIVE_FEATURE),
    authenticate: () => screen.getByText(TOKEN.AUTHENTICATE),
    queryAuthenticate: () => screen.queryByText(TOKEN.AUTHENTICATE),
  }
};

const shouldSeeDetailsInReadOnlyMode = ({statusValue = testStatus} = {}) => {
  const {readOnly: {id, firstName, lastName, email, status, roles}} = selectors;

  expect(id()).toHaveTextContent(testId);
  expect(firstName()).toHaveTextContent(testFirstName);
  expect(lastName()).toHaveTextContent(testLastName);
  expect(email()).toHaveTextContent(testEmail);
  expect(status()).toHaveTextContent(STATUSES[statusValue].label);

  testRoles.forEach(it => {
    expect(roles()).toHaveTextContent(it);
  });
};

const expectToHaveStatus = (value) => {
  const {status} = selectors;

  if (value === STATUSES.active.id) {
    expect(status()).toBeChecked();
  } else {
    expect(status()).not.toBeChecked();
  }
};

describe('UsersDetails', function() {
  const onDone = jest.fn();
  const CONFIRM = Promise.resolve();

  const renderAndWaitForLoad = async (userId, source = DEFAULT_SOURCE) => {
    const itemId = userId ? `${encodeURIComponent(source)}/${encodeURIComponent(userId)}` : '';
    render(<UsersDetails itemId={itemId} onDone={onDone}/>);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
  }

  const clickOnRoles = roles => roles.forEach(it => userEvent.click(screen.getByText(ROLES[it].name)));

  beforeEach(() => {
    when(Axios.get).calledWith(defaultRolesUrl).mockResolvedValue({data: Object.values(ROLES)});
    when(Axios.get).calledWith(findUsersUrl(testId, testSource)).mockResolvedValue({data: [{
      ...USER,
      externalRoles: [],
      readOnly: false,
    }]});
    ExtJS.checkPermission.mockReturnValue(true);
    ExtJS.state().getValue.mockReturnValue('test');
    ExtJS.state().getUser.mockReturnValue({id: 'id'});
    ExtJS.isProEdition.mockReturnValue(true);
  });

  describe('Local User Form', function() {
    it('renders local user resolved data', async function() {
      const {id, firstName, lastName, email, roles, querySubmitButton, queryFormError} = selectors;

      await renderAndWaitForLoad(testId);

      expect(id()).toHaveValue(testId);
      expect(id()).toBeDisabled();
      expect(firstName()).toHaveValue(testFirstName);
      expect(lastName()).toHaveValue(testLastName);
      expect(email()).toHaveValue(testEmail);
      expectToHaveStatus(testStatus);

      testRoles.forEach(it => {
        expect(roles()).toHaveTextContent(ROLES[it].name);
      });
      userEvent.click(querySubmitButton());
      expect(queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();
    });

    it('renders local user validation messages', async function() {
      const {
        id,
        firstName,
        lastName,
        email,
        status,
        password,
        confirmPassword,
        roles,
        querySubmitButton,
        queryFormError
      } = selectors;
      const {requiredValidation, invalidEmailValidation, passwordNoMatchValidation} = selectors;

      const expectRequiredValidation = async (field) => {
        const value = 'test';
        await TestUtils.changeField(field, value);
        userEvent.clear(field());
        expect(requiredValidation()).toBeInTheDocument();
        await TestUtils.changeField(field, value);
        expect(requiredValidation()).not.toBeInTheDocument();
      }

      await renderAndWaitForLoad();

      expect(id()).toBeInTheDocument();
      expect(firstName()).toBeInTheDocument();
      expect(lastName()).toBeInTheDocument();
      expect(email()).toBeInTheDocument();
      expect(status()).toBeInTheDocument();
      expect(password()).toBeInTheDocument();
      expect(confirmPassword()).toBeInTheDocument();
      expect(status()).toBeInTheDocument();
      expect(roles()).toBeInTheDocument();

      await expectRequiredValidation(id);
      await expectRequiredValidation(firstName);
      await expectRequiredValidation(lastName);
      await expectRequiredValidation(email);
      await expectRequiredValidation(password);
      await expectRequiredValidation(confirmPassword);

      expect(invalidEmailValidation()).toBeInTheDocument();
      await TestUtils.changeField(email, testEmail);
      expect(invalidEmailValidation()).not.toBeInTheDocument();

      userEvent.clear(password());
      expect(requiredValidation()).toBeInTheDocument();
      expect(passwordNoMatchValidation()).toBeInTheDocument();
      await TestUtils.changeField(password, testPassword);
      await TestUtils.changeField(confirmPassword, testPassword);
      expect(requiredValidation()).not.toBeInTheDocument();
      expect(passwordNoMatchValidation()).not.toBeInTheDocument();

      userEvent.click(querySubmitButton());
      expect(queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

      clickOnRoles(testRoles);
      userEvent.click(status());

      expect(queryFormError()).not.toBeInTheDocument();
    });

    it('creates local user', async function() {
      const {id, firstName, lastName, email, password, confirmPassword, querySubmitButton, querySavingMask} = selectors;

      const REQUEST = {
        ...USER,
        password: testPassword,
        passwordConfirm: testPassword,
      };

      when(Axios.post).calledWith(createUserUrl, REQUEST).mockResolvedValue({data: {}});

      await renderAndWaitForLoad();

      await TestUtils.changeField(id, testId);
      await TestUtils.changeField(firstName, testFirstName);
      await TestUtils.changeField(lastName, testLastName);
      await TestUtils.changeField(email, testEmail);
      await TestUtils.changeField(password, testPassword);
      await TestUtils.changeField(confirmPassword, testPassword);
      clickOnRoles(testRoles);

      userEvent.click(querySubmitButton());
      await waitForElementToBeRemoved(querySavingMask());

      expect(Axios.post).toHaveBeenCalledWith(createUserUrl, REQUEST);
      expect(NX.Messages.success).toHaveBeenCalledWith(UIStrings.SAVE_SUCCESS);
    });

    it('updates local user', async function() {
      const {
        firstName,
        lastName,
        email,
        status,
        password,
        confirmPassword,
        querySubmitButton,
        querySavingMask
      } = selectors;

      const data = {
        userId: testId,
        firstName: 'test2',
        lastName: 'test3',
        emailAddress: 'test1@test2.com',
        source: DEFAULT_SOURCE,
        status: 'disabled',
        roles: ['replication-role'],
        externalRoles: [],
        readOnly: false,
      };

      Axios.put.mockReturnValue(Promise.resolve());

      await renderAndWaitForLoad(testId);

      await TestUtils.changeField(firstName, data.firstName);
      await TestUtils.changeField(lastName, data.lastName);
      await TestUtils.changeField(email, data.emailAddress);
      userEvent.click(status());
      clickOnRoles(testRoles);
      clickOnRoles(data.roles);

      expect(password()).not.toBeInTheDocument();
      expect(confirmPassword()).not.toBeInTheDocument();

      userEvent.click(querySubmitButton());
      await waitForElementToBeRemoved(querySavingMask());

      expect(Axios.put).toHaveBeenCalledWith(singleUserUrl(testId), data);
      expect(NX.Messages.success).toHaveBeenCalledWith(UIStrings.SAVE_SUCCESS);
    });

    it('does not show delete button for the anonymous user', async function() {
      const {deleteButton} = selectors;
      const anonymousUserId = 'anonymous';
      const anonymousUsernameProp = 'anonymousUsername';

      when(ExtJS.state().getValue).calledWith(anonymousUsernameProp).mockReturnValue(anonymousUserId);

      when(Axios.get).calledWith(findUsersUrl(anonymousUserId, testSource)).mockResolvedValue({data: [{
        ...USER,
        userId: anonymousUserId,
        externalRoles: [],
        readOnly: false,
      }]});

      await renderAndWaitForLoad(anonymousUserId);

      expect(ExtJS.state().getValue).toHaveBeenCalledWith(anonymousUsernameProp);

      expect(deleteButton()).not.toBeInTheDocument();
    });

    it('does not show delete button for the current user', async function() {
      const {deleteButton} = selectors;

      ExtJS.state().getUser.mockReturnValue({id: testId});

      await renderAndWaitForLoad(testId);

      expect(deleteButton()).not.toBeInTheDocument();
    });

    it('requests confirmation when delete is requested', async function() {
      const {deleteButton} = selectors;
      Axios.delete.mockReturnValue(Promise.resolve(null));

      await renderAndWaitForLoad(testId);

      ExtJS.requestConfirmation.mockReturnValue(CONFIRM);
      userEvent.click(deleteButton());

      await waitFor(() => expect(Axios.delete).toBeCalledWith(singleUserUrl(testId)));
      expect(onDone).toBeCalled();
      expect(ExtJS.showSuccessMessage).toBeCalled();
    });

    describe('Read-Only Mode', function() {
      it('renders user details without edit permissions', async () => {
        const {readOnly: {warning}, cancelButton} = selectors;
        when(ExtJS.checkPermission).calledWith('nexus:users:update').mockReturnValue(false);

        await renderAndWaitForLoad(testId);

        expect(warning()).toBeInTheDocument();
        shouldSeeDetailsInReadOnlyMode();

        userEvent.click(cancelButton());
        await waitFor(() => expect(onDone).toBeCalled());
      });
    });
  });

  describe('External User Form', function() {
    const crowdSource = 'Crowd';
    const statusValue = 'disabled';
    const PARAMS = {
      externalRoles: testExternalRoles,
      source: crowdSource,
      status: statusValue,
      readOnly: false,
    };
    const EXTERNAL = {...USER, ...PARAMS};

    beforeEach(() => {
      when(Axios.get).calledWith(findUsersUrl(testId, crowdSource)).mockResolvedValue({data: [EXTERNAL]});
    });

    it('renders external user resolved data', async function() {
      const {id, firstName, lastName, email, status, roles, externalRoles, querySubmitButton, queryFormError, deleteButton} = selectors;

      await renderAndWaitForLoad(testId, crowdSource);

      expect(id()).toHaveValue(testId);
      expect(id()).toBeDisabled();
      expect(firstName()).toHaveValue(testFirstName);
      expect(firstName()).toBeDisabled();
      expect(lastName()).toHaveValue(testLastName);
      expect(lastName()).toBeDisabled();
      expect(email()).toHaveValue(testEmail);
      expect(email()).toBeDisabled();
      expectToHaveStatus(statusValue);
      expect(status()).toBeDisabled();

      testRoles.forEach(it => {
        expect(roles()).toHaveTextContent(ROLES[it].name);
      });
      expect(externalRoles()).toBeDisabled();
      expect(externalRoles()).toHaveValue(testExternalRoles.join('\n'));

      expect(deleteButton()).not.toBeInTheDocument();
      userEvent.click(querySubmitButton());
      expect(queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();
    });

    it('updates external user', async function() {
      const {password, confirmPassword, querySubmitButton, querySavingMask} = selectors;

      const data = {
        ...EXTERNAL,
        roles: ['replication-role'],
      };

      Axios.post.mockReturnValue({data: TestUtils.makeExtResult({})});

      await renderAndWaitForLoad(testId, crowdSource);

      clickOnRoles(testRoles);
      clickOnRoles(data.roles);

      expect(password()).not.toBeInTheDocument();
      expect(confirmPassword()).not.toBeInTheDocument();

      userEvent.click(querySubmitButton());
      await waitForElementToBeRemoved(querySavingMask());

      await waitFor(() => expect(Axios.post).toHaveBeenCalledWith(
          EXT_URL,
          ExtAPIUtils.createRequestBody(ACTION, METHODS.UPDATE_ROLE_MAPPINGS, {
            data: [{realm: EXTERNAL.source, roles: data.roles, userId: EXTERNAL.userId}]
          })
      ));

      expect(NX.Messages.success).toHaveBeenCalledWith(UIStrings.SAVE_SUCCESS);
    });

    it('removes all roles', async function() {
      const {querySubmitButton, querySavingMask} = selectors;

      Axios.post.mockReturnValue({data: TestUtils.makeExtResult({})});

      await renderAndWaitForLoad(testId, crowdSource);

      clickOnRoles(testRoles);

      userEvent.click(querySubmitButton());
      await waitForElementToBeRemoved(querySavingMask());

      await waitFor(() => expect(Axios.post).toHaveBeenCalledWith(
          EXT_URL,
          ExtAPIUtils.createRequestBody(ACTION, METHODS.UPDATE_ROLE_MAPPINGS, {
            data: [{realm: EXTERNAL.source, roles: [], userId: EXTERNAL.userId}]
          })
      ));

      expect(NX.Messages.success).toHaveBeenCalledWith(UIStrings.SAVE_SUCCESS);
    });

    describe('Read-Only Mode', function() {
      it('renders external user details without edit permissions', async () => {
        const {readOnly: {warning, externalRoles}, cancelButton} = selectors;
        when(ExtJS.checkPermission).calledWith('nexus:users:update').mockReturnValue(false);

        await renderAndWaitForLoad(testId, crowdSource);

        expect(warning()).toBeInTheDocument();
        shouldSeeDetailsInReadOnlyMode({statusValue});

        expect(externalRoles()).toHaveTextContent(testExternalRoles.join(''))

        userEvent.click(cancelButton());
        await waitFor(() => expect(onDone).toBeCalled());
      });
    });
  });

  it('renders load error message', async function() {
    const message = 'Load error message!';

    Axios.get.mockReturnValue(Promise.reject({message}));

    await renderAndWaitForLoad(testId);

    expect(screen.getByRole('alert')).toHaveTextContent(message);
  });

  it('shows save API errors', async function() {
    const message = 'Use a unique userId';
    const {id, firstName, lastName, email, status, password, confirmPassword, querySubmitButton, querySavingMask} = selectors;

    when(Axios.post).calledWith(createUserUrl, expect.objectContaining({userId: testId}))
        .mockRejectedValue({response: {data: message}});

    await renderAndWaitForLoad();

    await TestUtils.changeField(id, testId);
    await TestUtils.changeField(firstName, testFirstName);
    await TestUtils.changeField(lastName, testLastName);
    await TestUtils.changeField(email, testEmail);
    await TestUtils.changeField(password, testPassword);
    await TestUtils.changeField(confirmPassword, testPassword);
    userEvent.click(status());
    clickOnRoles(testRoles);

    userEvent.click(querySubmitButton());
    await waitForElementToBeRemoved(querySavingMask());

    expect(Axios.post).toHaveBeenCalledWith(createUserUrl, expect.anything());

    await waitFor(() => expect(selectors.querySaveErrorAlert()).toHaveTextContent(message));
  });

  it('fires onDone when cancelled', async function() {
    const {cancelButton} = selectors;

    await renderAndWaitForLoad();

    userEvent.click(cancelButton());

    await waitFor(() => expect(onDone).toBeCalled());
  });

  it('uses proper urls', function() {
    expect(singleUserUrl('testId')).toBe('service/rest/v1/security/users/testId');
    expect(singleUserUrl('test>?* %$@&Id ')).toBe('service/rest/v1/security/users/test%3E%3F*%20%25%24%40%26Id%20');

    expect(findUsersUrl('test?')).toBe('service/rest/v1/security/users?source=default&userId=test%3F');
    expect(findUsersUrl('test>', 'Crowd')).toBe('service/rest/v1/security/users?source=Crowd&userId=test%3E');

    expect(defaultRolesUrl).toBe('service/rest/v1/security/roles?source=default');
    expect(createUserUrl).toBe('service/rest/v1/security/users');
  });

  describe('Change Password', () => {
    beforeEach(() => {
      when(ExtJS.checkPermission)
        .calledWith('nexus:users:update')
        .mockReturnValue(true);

      when(ExtJS.checkPermission)
        .calledWith('nexus:*')
        .mockReturnValue(true);

      when(Axios.get)
        .calledWith(changePasswordUrl(testId))
        .mockResolvedValue({
          data: {
            userId: 'admin',
          },
        });

      ExtJS.fetchAuthenticationToken = jest.fn(() =>
        Promise.resolve({ data: 'fakeToken', success: true })
      );

      ExtJS.state = jest.fn().mockReturnValue({
        getUser: jest.fn().mockReturnValue({id: 'admin'}),
        getValue: jest.fn()
      });
    });

    it('renders correctly', async () => {
      const { title, next, text, cancel, openButton} = selectors.modal;

      await renderAndWaitForLoad(testId);

      expect(openButton()).toBeInTheDocument();

      await userEvent.click(openButton());

      expect(Axios.get).toHaveBeenCalled();
      expect(title()).toBeInTheDocument();
      expect(cancel()).toBeInTheDocument();
      expect(next()).toBeInTheDocument();
      expect(text()).toBeInTheDocument();
    });

    it('shows fields to change password if the admin password is correct', async () => {
      const {
        next,
        cancel,
        openButton,
        save,
        inputAdminPassword,
        inputNewPassword,
        inputConfirmPassword
      } = selectors.modal;

      await renderAndWaitForLoad(testId);

      await userEvent.click(openButton());
      await TestUtils.changeField(inputAdminPassword, 'admin123');

      expect(next()).toBeInTheDocument();

      await userEvent.click(next());

      await waitFor(() => {
        expect(cancel()).toBeInTheDocument();
        expect(save()).toBeInTheDocument();
        expect(inputNewPassword()).toBeInTheDocument();
        expect(inputConfirmPassword()).toBeInTheDocument();
      });
    });

    it('does not show fields to change password if the admin password is incorrect', async () => {
      const {next, cancel, openButton, querySave, inputAdminPassword} = selectors.modal;

      ExtJS.fetchAuthenticationToken = jest.fn(() =>
        Promise.resolve({ data: "Invalid", success: false, message: 'Authentication failed' })
      );

      await renderAndWaitForLoad(testId);

      await userEvent.click(openButton());
      await TestUtils.changeField(inputAdminPassword, 'incorrect');

      expect(next()).toBeInTheDocument();

      await userEvent.click(next());

      await waitFor(() => {
        expect(next()).toBeInTheDocument();
        expect(cancel()).toBeInTheDocument();
        expect(querySave()).not.toBeInTheDocument();
        expect(inputAdminPassword()).toHaveErrorMessage('Authentication failed');
      });
    });

    it('prevents password change when new does not match confirm', async () => {
      const {
        openButton,
        inputAdminPassword,
        inputNewPassword,
        inputConfirmPassword,
        next,
        save
      } = selectors.modal;
      const {
        queryFormError
      } = selectors;

      await renderAndWaitForLoad(testId);

      await waitFor(() => {
        expect(openButton()).toBeInTheDocument();
      });

      await userEvent.click(openButton());
      await TestUtils.changeField(inputAdminPassword, 'admin123');
      await userEvent.click(next());

      await waitFor(() => {
        expect(inputNewPassword()).toBeInTheDocument();
        expect(inputConfirmPassword()).toBeInTheDocument();
      });

      await TestUtils.changeField(inputNewPassword, '123456');
      await TestUtils.changeField(inputConfirmPassword, '1234');

      userEvent.click(save());
      expect(queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();
    });

    it('sends the correct password change request', async () => {
      const expectedPassword = '123456';
      const {
        openButton,
        inputAdminPassword,
        inputNewPassword,
        inputConfirmPassword,
        next,
        save,
        queryChangePasswordMask
      } = selectors.modal;

      await renderAndWaitForLoad(testId);

      await userEvent.click(openButton());
      await TestUtils.changeField(inputAdminPassword, 'admin123');
      await userEvent.click(next());

      await waitFor(() => {
        expect(inputNewPassword()).toBeInTheDocument();
        expect(inputConfirmPassword()).toBeInTheDocument();
      });

      await TestUtils.changeField(inputNewPassword, expectedPassword);
      await TestUtils.changeField(inputConfirmPassword, expectedPassword);

      userEvent.click(save());
      await waitForElementToBeRemoved(queryChangePasswordMask());

      expect(Axios.put).toHaveBeenCalledTimes(1);
      expect(Axios.put).toHaveBeenCalledWith(
          changePasswordUrl(testId),
          expectedPassword,
          {headers: {'Content-Type': 'text/plain'}}
      );
    });

    it('closes modal when pressing cancel button', async () => {
      ExtJS.fetchAuthenticationToken = jest.fn(() =>
        Promise.resolve({ data: 'fakeToken', success: true })
      );

      const {
        openButton,
        queryModal,
        container,
        cancel,
        next,
        inputAdminPassword,
        save
      } = selectors.modal;

      await renderAndWaitForLoad(testId);

      expect(openButton()).toBeInTheDocument();

      await userEvent.click(openButton());

      expect(container()).toBeInTheDocument();
      expect(cancel()).toBeInTheDocument();

      await userEvent.click(cancel());

      expect(queryModal()).not.toBeInTheDocument();

      await userEvent.click(openButton());

      await TestUtils.changeField(inputAdminPassword, 'admin123');

      await userEvent.click(next());

      await waitFor(() => {
        expect(cancel()).toBeInTheDocument();
        expect(save()).toBeInTheDocument();
      });

      await userEvent.click(cancel());

      expect(queryModal()).not.toBeInTheDocument();
    });


    it('can not change password for anonymous user', async () => {
      const userId = 'anonymous';

      when(Axios.get).calledWith(findUsersUrl(userId, testSource)).mockResolvedValue({data: [{
        ...USER,
        userId,
        externalRoles: [],
        readOnly: false,
      }]});

      ExtJS.state().getValue.mockReturnValue(userId);

      const {queryModal} = selectors.modal;

      await renderAndWaitForLoad(userId);

      expect(queryModal()).not.toBeInTheDocument();
    });

    it('can not change password if user does not have enough permissions', async () => {
      const {openButton} = selectors.modal;

      when(ExtJS.checkPermission)
        .calledWith('nexus:*').mockReturnValue(false);

      await renderAndWaitForLoad(testId);

      expect(openButton()).toHaveClass('disabled')
    });

    it('show error message in case there is something wrong when changing the password', async () => {
      const mockPassword = '123456';
      const {
        openButton,
        next,
        save,
        inputNewPassword,
        inputConfirmPassword,
        inputAdminPassword,
        retryButton,
        queryChangePasswordMask
      } = selectors.modal;

      await renderAndWaitForLoad(testId);

      await userEvent.click(openButton());
      await TestUtils.changeField(inputAdminPassword, 'admin123');

      await userEvent.click(next());

      await waitFor(() => {
        expect(inputNewPassword()).toBeInTheDocument();
        expect(inputConfirmPassword()).toBeInTheDocument();
      });

      await TestUtils.changeField(inputNewPassword, mockPassword);
      await TestUtils.changeField(inputConfirmPassword, mockPassword);

      Axios.put.mockRejectedValue({response:{status: 400}});

      expect(save()).toBeInTheDocument();

      userEvent.click(save());
      await waitForElementToBeRemoved(queryChangePasswordMask());

      expect(Axios.put).toHaveBeenCalledTimes(1);
      expect(Axios.put).toHaveBeenCalledWith(
          changePasswordUrl(testId),
          mockPassword,
          {headers: {'Content-Type': 'text/plain'}}
      );
      expect(retryButton()).toBeInTheDocument();
    });
  });

  describe('Reset Token', () => {
    beforeEach(() => {
      ExtJS.state = jest.fn().mockReturnValue({
        getUser: jest.fn().mockReturnValue({id: 'admin'}),
        getValue: jest.fn().mockReturnValue(['usertoken'])
      });
    });

    it('renders correctly', async () => {
      ExtJS.state = jest.fn().mockReturnValue({
        getUser: jest.fn().mockReturnValue({id: 'admin'}),
        getValue: jest.fn(),
      });

      const {
        title,
        link,
        queryResetButton,
      } = selectors.token;

      await renderAndWaitForLoad(testId);

      expect(title()).toBeInTheDocument();
      expect(link()).toBeInTheDocument();
      expect(queryResetButton()).not.toBeInTheDocument();
    });

    it('does not show reset token user section if it is not nexus pro', async () => {
      const {queryTitle, queryResetButton} = selectors.token;

      ExtJS.isProEdition = jest.fn().mockReturnValue(false);

      await renderAndWaitForLoad(testId);

      expect(queryTitle()).not.toBeInTheDocument();
      expect(queryResetButton()).not.toBeInTheDocument();
    });

    it('show reset button if the capability is enabled', async () => {
      const {resetButton} = selectors.token;

      await renderAndWaitForLoad(testId);

      expect(resetButton()).toBeInTheDocument();
    });

    it('requires authentication to be able to reset the token', async () => {
      const {
        token: {
          resetButton,
          activeLabel,
          modalText,
          authenticate
        },
        modal: {
          inputAdminPassword
        }
      } = selectors;

      ExtJS.fetchAuthenticationToken = jest.fn(() =>
        Promise.resolve({ data: "Invalid", success: false, message: 'Authentication failed' })
      );

      await renderAndWaitForLoad(testId);

      await userEvent.click(resetButton());

      expect(activeLabel()).toBeInTheDocument();
      expect(modalText()).toBeInTheDocument();
      expect(authenticate()).toBeInTheDocument();

      await TestUtils.changeField(inputAdminPassword, 'incorrect');
      await userEvent.click(authenticate());

      await waitFor(() => {
        expect(inputAdminPassword()).toHaveErrorMessage('Authentication failed');
      });
    });

    it('reset button is disabled if the user does not have enough permissions', async () => {
      const {resetButton} = selectors.token;

      when(ExtJS.checkPermission)
        .calledWith('nexus:usertoken-settings:update').mockReturnValue(false);

      await renderAndWaitForLoad(testId);

      expect(resetButton()).toHaveClass('disabled');
    });

    it('reset token properly', async () => {
      when(Axios.delete)
        .calledWith(resetTokenUrl(testId, 'default'))
        .mockResolvedValue();

      ExtJS.fetchAuthenticationToken = jest.fn(() =>
        Promise.resolve({ data: 'fakeToken', success: true })
      );

      const {
        token: {
          resetButton,
          authenticate,
          queryAuthenticate
        },
        modal: {
          inputAdminPassword
        }
      } = selectors;

      await renderAndWaitForLoad(testId);
      expect(resetButton()).not.toHaveClass('disabled');

      await userEvent.click(resetButton());

      await TestUtils.changeField(inputAdminPassword, 'admin123');

      await act(async () => userEvent.click(authenticate()));

      await waitFor(() => {
        expect(Axios.delete).toHaveBeenCalledTimes(1);
        expect(Axios.delete).toHaveBeenCalledWith(resetTokenUrl(testId, 'default'));
        expect(queryAuthenticate()).not.toBeInTheDocument();
      });
    });

    it('closes modal when pressing cancel button', async () => {
      const {resetButton, cancel, queryModal} = selectors.token;

      await renderAndWaitForLoad(testId);

      expect(resetButton()).not.toBeDisabled();

      await userEvent.click(resetButton());

      expect(cancel()).toBeInTheDocument();

      await userEvent.click(cancel());

      expect(queryModal()).not.toBeInTheDocument();
    });
  });
});
