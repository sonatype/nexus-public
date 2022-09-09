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
import {ExtJS, TestUtils} from '@sonatype/nexus-ui-plugin';
import {
  render,
  fireEvent,
  screen,
  waitFor,
  waitForElementToBeRemoved,
  getByText,
  queryByText,
  act
} from '@testing-library/react';

import UIStrings from '../../../../constants/UIStrings';
import UsersDetails from './UsersDetails';

import {ROLES} from './Users.testdata';

import {DEFAULT_SOURCE, URL, STATUSES} from './UsersHelper';

const {singleUserUrl, createUserUrl, defaultRolesUrl, findUsersUrl, changePasswordUrl} = URL;

const {USERS: {FORM: LABELS, MODAL}, SETTINGS} = UIStrings;

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
  id: () => screen.queryByLabelText(LABELS.ID.LABEL),
  firstName: () => screen.queryByLabelText(LABELS.FIRST_NAME.LABEL),
  lastName: () => screen.queryByLabelText(LABELS.LAST_NAME.LABEL),
  password: () => screen.queryByLabelText(LABELS.PASSWORD.LABEL),
  confirmPassword: () => screen.queryByLabelText(LABELS.CONFIRM_PASSWORD.LABEL),
  email: () => screen.queryByLabelText(LABELS.EMAIL.LABEL),
  status: () => screen.queryByLabelText(LABELS.STATUS.LABEL),
  roles: () => screen.queryByRole('group', {name: LABELS.ROLES.GRANTED}),
  externalRolesLabel: () => screen.getByText(LABELS.EXTERNAL_ROLES.LABEL),
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
    defaultUserWarning: () => screen.getByText(LABELS.DEFAULT_USER_WARNING),
  },
  cancelButton: () => screen.getByText(SETTINGS.CANCEL_BUTTON_LABEL),
  saveButton: () => screen.getByText(SETTINGS.SAVE_BUTTON_LABEL),
  deleteButton: () => screen.queryByText(SETTINGS.DELETE_BUTTON_LABEL),
  modal: {
    openButton: () => screen.getByText(MODAL.CHANGE_PASSWORD),
    container: () => screen.getByLabelText(MODAL.CHANGE_PASSWORD),
    queryModal: () => screen.queryByLabelText(MODAL.CHANGE_PASSWORD),
    inputAdminPassword: () => screen.getByTestId('adminPassword'),
    inputNewPassword: () => screen.getByTestId('newPassword'),
    inputConfirmPassword: () => screen.getByTestId('confirmPassword'),
    text: () => screen.getByText(MODAL.TEXT),
    next: () => screen.getByText(MODAL.NEXT),
    title: () => screen.getByText(MODAL.ADMIN_PASSWORD),
    cancel: () => getByText(selectors.modal.container(), SETTINGS.CANCEL_BUTTON_LABEL),
    save: () => getByText(selectors.modal.container(), SETTINGS.SAVE_BUTTON_LABEL),
    querySave: () => queryByText(selectors.modal.container(), SETTINGS.SAVE_BUTTON_LABEL),
    retryButton: () => screen.queryByText('Retry'),
  },
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

const shouldSeeExternalRoles = () => {
  const {externalRolesLabel} = selectors;

  let externalRole = externalRolesLabel();
  testExternalRoles.forEach(it => {
    externalRole = externalRole.nextSibling;
    expect(externalRole).toHaveTextContent(it);
  });
};

describe('UsersDetails', function() {
  const onDone = jest.fn();
  const CONFIRM = Promise.resolve();

  const renderAndWaitForLoad = async (userId, source = DEFAULT_SOURCE) => {
    const itemId = userId ? `${encodeURIComponent(source)}/${encodeURIComponent(userId)}` : '';
    render(<UsersDetails itemId={itemId} onDone={onDone}/>);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
  }

  const clickOnRoles = roles => roles.forEach(it => fireEvent.click(screen.getByText(ROLES[it].name)));

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
  });

  describe('Local User Form', function() {
    it('renders local user resolved data', async function() {
      const {id, firstName, lastName, email, status, roles, saveButton} = selectors;

      await renderAndWaitForLoad(testId);

      expect(id()).toHaveValue(testId);
      expect(id()).toBeDisabled();
      expect(firstName()).toHaveValue(testFirstName);
      expect(lastName()).toHaveValue(testLastName);
      expect(email()).toHaveValue(testEmail);
      expect(status()).toHaveValue(testStatus);

      testRoles.forEach(it => {
        expect(roles()).toHaveTextContent(ROLES[it].name);
      });
      expect(saveButton()).toHaveClass('disabled');
    });

    it('renders local user validation messages', async function() {
      const {id, firstName, lastName, email, status, password, confirmPassword, roles, saveButton} = selectors;
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

      expect(saveButton()).toHaveClass('disabled');

      clickOnRoles(testRoles);
      userEvent.selectOptions(status(), testStatus);

      expect(saveButton()).not.toHaveClass('disabled');
    });

    it('creates local user', async function() {
      const {id, firstName, lastName, email, status, password, confirmPassword, roles, saveButton} = selectors;

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
      userEvent.selectOptions(status(), testStatus);
      clickOnRoles(testRoles);

      expect(saveButton()).not.toHaveClass('disabled');
      userEvent.click(saveButton());

      await waitFor(() => expect(Axios.post).toHaveBeenCalledWith(createUserUrl, REQUEST));
      expect(NX.Messages.success).toHaveBeenCalledWith(UIStrings.SAVE_SUCCESS);
    });

    it('updates local user', async function() {
      const {firstName, lastName, email, status, password, confirmPassword, saveButton} = selectors;

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
      userEvent.selectOptions(status(), data.status);
      clickOnRoles(testRoles);
      clickOnRoles(data.roles);

      expect(password()).not.toBeInTheDocument();
      expect(confirmPassword()).not.toBeInTheDocument();

      expect(saveButton()).not.toHaveClass('disabled');

      userEvent.click(saveButton());

      await waitFor(() => expect(Axios.put).toHaveBeenCalledWith(singleUserUrl(testId), data));
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

        fireEvent.click(cancelButton());
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
      const {id, firstName, lastName, email, status, roles, saveButton, deleteButton} = selectors;

      await renderAndWaitForLoad(testId, crowdSource);

      expect(id()).toHaveValue(testId);
      expect(id()).toBeDisabled();
      expect(firstName()).toHaveValue(testFirstName);
      expect(firstName()).toBeDisabled();
      expect(lastName()).toHaveValue(testLastName);
      expect(lastName()).toBeDisabled();
      expect(email()).toHaveValue(testEmail);
      expect(email()).toBeDisabled();
      expect(status()).toHaveValue(statusValue);
      expect(status()).toBeDisabled();

      testRoles.forEach(it => {
        expect(roles()).toHaveTextContent(ROLES[it].name);
      });

      shouldSeeExternalRoles();

      expect(deleteButton()).not.toBeInTheDocument();
      expect(saveButton()).toHaveClass('disabled');
    });

    it('updates external user', async function() {
      const {password, confirmPassword, saveButton} = selectors;

      const data = {
        ...EXTERNAL,
        roles: ['replication-role'],
      };

      Axios.put.mockReturnValue(Promise.resolve());

      await renderAndWaitForLoad(testId, crowdSource);

      clickOnRoles(testRoles);
      clickOnRoles(data.roles);

      expect(password()).not.toBeInTheDocument();
      expect(confirmPassword()).not.toBeInTheDocument();

      expect(saveButton()).not.toHaveClass('disabled');

      userEvent.click(saveButton());

      await waitFor(() => expect(Axios.put).toHaveBeenCalledWith(singleUserUrl(testId), data));
      expect(NX.Messages.success).toHaveBeenCalledWith(UIStrings.SAVE_SUCCESS);
    });

    describe('Read-Only Mode', function() {
      it('renders external user details without edit permissions', async () => {
        const {readOnly: {warning}, cancelButton} = selectors;
        when(ExtJS.checkPermission).calledWith('nexus:users:update').mockReturnValue(false);

        await renderAndWaitForLoad(testId, crowdSource);

        expect(warning()).toBeInTheDocument();
        shouldSeeDetailsInReadOnlyMode({statusValue});
        shouldSeeExternalRoles();

        fireEvent.click(cancelButton());
        await waitFor(() => expect(onDone).toBeCalled());
      });

      it('renders default external user details', async () => {
        const {readOnly: {defaultUserWarning}, cancelButton} = selectors;

        when(Axios.get).calledWith(findUsersUrl(testId, crowdSource)).mockResolvedValue({
          data: [{...EXTERNAL, readOnly: true}]
        });

        await renderAndWaitForLoad(testId, crowdSource);

        expect(defaultUserWarning()).toBeInTheDocument();
        shouldSeeDetailsInReadOnlyMode({statusValue});
        shouldSeeExternalRoles();

        fireEvent.click(cancelButton());
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
    const {id, firstName, lastName, email, status, password, confirmPassword, saveButton} = selectors;

    when(Axios.post).calledWith(createUserUrl, expect.objectContaining({userId: testId}))
        .mockRejectedValue({response: {data: message}});

    await renderAndWaitForLoad();

    await TestUtils.changeField(id, testId);
    await TestUtils.changeField(firstName, testFirstName);
    await TestUtils.changeField(lastName, testLastName);
    await TestUtils.changeField(email, testEmail);
    await TestUtils.changeField(password, testPassword);
    await TestUtils.changeField(confirmPassword, testPassword);
    userEvent.selectOptions(status(), testStatus);
    clickOnRoles(testRoles);

    expect(saveButton()).not.toHaveClass('disabled');
    userEvent.click(saveButton());

    await waitFor(() => expect(Axios.post).toHaveBeenCalledWith(createUserUrl, expect.anything()));

    expect(NX.Messages.error).toHaveBeenCalledWith(UIStrings.ERROR.SAVE_ERROR);
    expect(screen.getByText(new RegExp(message))).toBeInTheDocument();
  });

  it('fires onDone when cancelled', async function() {
    const {cancelButton} = selectors;

    await renderAndWaitForLoad();

    userEvent.click(cancelButton());

    await waitFor(() => expect(onDone).toBeCalled());
  });

  it('uses proper urls', function() {
    expect(singleUserUrl('testId')).toBe('/service/rest/v1/security/users/testId');
    expect(singleUserUrl('test>?* %$@&Id ')).toBe('/service/rest/v1/security/users/test%3E%3F*%20%25%24%40%26Id%20');

    expect(findUsersUrl('test?')).toBe('/service/rest/v1/security/users?source=default&userId=test%3F');
    expect(findUsersUrl('test>', 'Crowd')).toBe('/service/rest/v1/security/users?source=Crowd&userId=test%3E');

    expect(defaultRolesUrl).toBe('/service/rest/v1/security/roles?source=default');
    expect(createUserUrl).toBe('/service/rest/v1/security/users');
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

      ExtJS.state().getUser.mockReturnValue({id: 'admin'});
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
        save,
      } = selectors.modal;

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

      expect(save()).toHaveAttribute('aria-disabled', 'true');
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

      await act(async () => userEvent.click(save()));

      await waitFor(() => {
        expect(Axios.put).toHaveBeenCalledTimes(1);
        expect(Axios.put).toHaveBeenCalledWith(
            changePasswordUrl(testId),
            expectedPassword,
            { headers: { 'Content-Type': 'text/plain' } }
          );
      });
    });

    it('closes modal when pressing cancel button', async () => {
      const {
        openButton,
        queryModal,
        container,
        cancel
      } = selectors.modal;

      await renderAndWaitForLoad(testId);

      expect(openButton()).toBeInTheDocument();

      await userEvent.click(openButton());

      expect(container()).toBeInTheDocument();
      expect(cancel()).toBeInTheDocument();

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

      expect(openButton()).toBeDisabled();
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
        querySave
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

      await act(async () => userEvent.click(save()));

      await waitFor(() => {
        expect(Axios.put).toHaveBeenCalledTimes(1);
        expect(Axios.put).toHaveBeenCalledWith(
          changePasswordUrl(testId),
          mockPassword,
          { headers: { 'Content-Type': 'text/plain' } }
          );
          expect(retryButton()).toBeInTheDocument();
          expect(querySave()).not.toBeInTheDocument();
      });
    });
  });
});
