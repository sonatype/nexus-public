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
import {render, screen, waitFor, waitForElementToBeRemoved, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {when} from 'jest-when';
import Axios from 'axios';
import {clone} from 'ramda';
import {ExtJS, APIConstants} from '@sonatype/nexus-ui-plugin';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import UIStrings from '../../../../constants/UIStrings';
import PrivilegesDetails from './PrivilegesDetails';
import {TYPES as TYPE_IDS, FIELDS, URL} from './PrivilegesHelper';
import {BREADR_ACTIONS, TYPES, TYPES_MAP, SELECTORS, REPOSITORIES} from './Privileges.testdata';

const {privilegesUrl, singlePrivilegeUrl, updatePrivilegeUrl, createPrivilegeUrl} = URL;

const XSS_STRING = TestUtils.XSS_STRING;
const {PRIVILEGES: {FORM: LABELS, MESSAGES: {NO_ACTION_ERROR}}, SETTINGS} = UIStrings;
const {EXT: {URL: EXT_URL}, REST: {INTERNAL: {PRIVILEGES_TYPES}}} = APIConstants;

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
  },
}));

const testName = 'PrivilegeName';
const testDescription = 'Test Privilege Description';
const testScriptName = 'TestScriptName';
const testScriptActions = 'run,add';
const testContentSelectorActions = 'browse,read';
const testContentSelector = 'Test_Selector_1';
const testRepository = 'TestRepository';
const testFormat = 'TestFormat';

const SCRIPT_PRIVILEGE = {
  type: TYPE_IDS.SCRIPT,
  name: testName,
  description: testDescription,
  scriptName: testScriptName,
  actions: testScriptActions.split(','),
};

const REPO_SELECTOR_PRIVILEGE = {
  type: TYPE_IDS.REPOSITORY_CONTENT_SELECTOR,
  name: testName,
  description: testDescription,
  contentSelector: testContentSelector,
  format: testFormat,
  repository: testRepository,
  actions: testContentSelectorActions.split(','),
};

const selectors = {
  ...TestUtils.selectors,
  ...TestUtils.formSelectors,
  type: () => screen.queryByLabelText(LABELS.TYPE.LABEL),
  name: () => screen.queryByLabelText(LABELS.NAME.LABEL),
  description: () => screen.queryByLabelText(LABELS.DESCRIPTION.LABEL),
  scriptName: () => screen.queryByLabelText(FIELDS.SCRIPT_NAME.LABEL),
  actions: () => screen.queryByLabelText(FIELDS.ACTIONS.LABEL),
  contentSelector: () => screen.queryByLabelText(FIELDS.CONTENT_SELECTOR.LABEL),
  format: () => screen.queryByLabelText(FIELDS.FORMAT.LABEL),
  repository: () => screen.queryByLabelText(FIELDS.REPOSITORY.LABEL),
  getActionsGroup: () => screen.queryByRole('group', {name: FIELDS.ACTIONS.LABEL}),
  getActionCheckbox: (c, n) => within(c).getByRole('checkbox', {name: n}),
  readOnly: {
    type: () => screen.getByText(LABELS.TYPE.LABEL).nextSibling,
    name: () => screen.getByText(LABELS.NAME.LABEL).nextSibling,
    description: () => screen.getByText(LABELS.DESCRIPTION.LABEL).nextSibling,
    scriptName: () => screen.getByText(FIELDS.SCRIPT_NAME.LABEL).nextSibling,
    actions: () => screen.getByText(FIELDS.ACTIONS.LABEL).nextSibling,
    contentSelector: () => screen.getByText(FIELDS.CONTENT_SELECTOR.LABEL).nextSibling,
    format: () => screen.getByText(FIELDS.FORMAT.LABEL).nextSibling,
    repository: () => screen.getByText(FIELDS.REPOSITORY.LABEL).nextSibling,
  },
  cancelButton: () => screen.queryByRole('button', {name: SETTINGS.CANCEL_BUTTON_LABEL}),
  deleteButton: () => screen.queryByRole('button', {name: SETTINGS.DELETE_BUTTON_LABEL}),
};

describe('PrivilegesDetails', function() {
  const onDone = jest.fn();
  const CONFIRM = Promise.resolve();

  const renderAndWaitForLoad = async (itemId) => {
    render(<PrivilegesDetails itemId={itemId || ''} onDone={onDone}/>);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
  }

  beforeEach(() => {
    when(Axios.get).calledWith(PRIVILEGES_TYPES)
        .mockResolvedValue({data: clone(TYPES)});
    when(Axios.get).calledWith(singlePrivilegeUrl(testName)).mockResolvedValue({
      data: {...SCRIPT_PRIVILEGE, readOnly: false}
    });
    ExtJS.checkPermission.mockReturnValue(true);
  });

  it('renders the resolved data', async function() {
    const {type, name, description, scriptName, getActionsGroup, querySubmitButton, queryFormError} = selectors;

    await renderAndWaitForLoad(testName);

    expect(type()).toHaveValue(TYPE_IDS.SCRIPT);
    expect(type()).toBeDisabled();
    expect(name()).toHaveValue(testName);
    expect(name()).toBeDisabled();
    expect(description()).toHaveValue(testDescription);
    expect(scriptName()).toHaveValue(testScriptName);
    expectActionsToRender(getActionsGroup(), BREADR_ACTIONS, ['Run', 'Add']);

    userEvent.click(querySubmitButton());
    expect(queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();
  });

  it('renders the resolved data with XSS', async function() {
    const {name, description, scriptName, getActionsGroup} = selectors;
    const XSS_PRIVILEGE = {
      ...SCRIPT_PRIVILEGE,
      name: XSS_STRING,
      description: XSS_STRING,
      scriptName: XSS_STRING,
      actions: testScriptActions.split(','),
    };

    when(Axios.get).calledWith(singlePrivilegeUrl(testName)).mockResolvedValue({
      data: {...XSS_PRIVILEGE, readOnly: false}
    });

    await renderAndWaitForLoad(testName);

    expect(name()).toHaveValue(XSS_STRING);
    expect(description()).toHaveValue(XSS_STRING);
    expect(scriptName()).toHaveValue(XSS_STRING);
    expectActionsToRender(getActionsGroup(), BREADR_ACTIONS, ['Run', 'Add']);
  });

  it('renders load error message', async function() {
    const message = 'Load error message!';

    Axios.get.mockReturnValue(Promise.reject({message}));

    await renderAndWaitForLoad();

    expect(screen.getByRole('alert')).toHaveTextContent(message);
  });

  it('renders all required fields for for types', async function() {
    const {type, name, description} = selectors;

    await renderAndWaitForLoad();

    expect(type()).toBeInTheDocument();
    expect(name()).toBeInTheDocument();
    expect(description()).toBeInTheDocument();

    TYPES.forEach(({id, formFields}) => {
      userEvent.selectOptions(type(), id);
      formFields.forEach(field => {
        if (field.type === 'string') {
          expect(screen.getByLabelText(field.label)).toBeInTheDocument();
        }
      });
    });
  });

  it('renders validation messages for the Script privilege', async function() {
    const {type, name, description, scriptName, getActionsGroup, getActionCheckbox, querySubmitButton, queryFormError} = selectors;

    await renderAndWaitForLoad();

    userEvent.click(querySubmitButton());
    expect(queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();

    expect(type()).toBeInTheDocument();
    expect(name()).toBeInTheDocument();
    expect(description()).toBeInTheDocument();
    expect(scriptName()).not.toBeInTheDocument();
    expect(getActionsGroup()).not.toBeInTheDocument();

    userEvent.selectOptions(type(), TYPE_IDS.SCRIPT);
    expect(scriptName()).toBeInTheDocument();
    expect(getActionsGroup()).toBeInTheDocument();
    expect(queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

    await TestUtils.changeField(name, testName);
    userEvent.clear(name());
    expect(name()).toHaveErrorMessage(TestUtils.REQUIRED_MESSAGE);
    await TestUtils.changeField(name, testName);
    expect(name()).not.toHaveErrorMessage(TestUtils.REQUIRED_MESSAGE);

    await TestUtils.changeField(description, testDescription);

    await TestUtils.changeField(scriptName, testScriptName);
    userEvent.clear(scriptName());
    expect(scriptName()).toHaveErrorMessage(TestUtils.REQUIRED_MESSAGE);
    await TestUtils.changeField(scriptName, testScriptName);
    expect(scriptName()).not.toHaveErrorMessage(TestUtils.REQUIRED_MESSAGE);

    const browseCheckbox = getActionCheckbox(getActionsGroup(), 'Browse');
    userEvent.click(browseCheckbox);
    expect(getActionsGroup()).not.toHaveAccessibleDescription(NO_ACTION_ERROR);
    userEvent.click(browseCheckbox);
    expect(getActionsGroup()).toHaveAccessibleDescription(NO_ACTION_ERROR);
    userEvent.click(browseCheckbox);
    expect(getActionsGroup()).not.toHaveAccessibleDescription(NO_ACTION_ERROR);

    expect(queryFormError()).not.toBeInTheDocument();
  });

  it('creates Script privilege', async function() {
    const {type, name, description, scriptName, getActionsGroup, getActionCheckbox, querySubmitButton} = selectors;

    when(Axios.post).calledWith(createPrivilegeUrl(TYPE_IDS.SCRIPT), SCRIPT_PRIVILEGE).mockResolvedValue({data: {}});

    await renderAndWaitForLoad();

    userEvent.selectOptions(type(), TYPE_IDS.SCRIPT);
    await TestUtils.changeField(name, testName);
    await TestUtils.changeField(description, testDescription);
    await TestUtils.changeField(scriptName, testScriptName);
    const runCheckbox = getActionCheckbox(getActionsGroup(), 'Run');
    const addCheckbox = getActionCheckbox(getActionsGroup(), 'Add');
    userEvent.click(runCheckbox);
    userEvent.click(addCheckbox);

    userEvent.click(querySubmitButton());
    await waitForElementToBeRemoved(selectors.querySavingMask());

    expect(Axios.post).toHaveBeenCalledWith(createPrivilegeUrl(TYPE_IDS.SCRIPT), SCRIPT_PRIVILEGE);
    expect(NX.Messages.success).toHaveBeenCalledWith(UIStrings.SAVE_SUCCESS);
  });

  it('renders validation messages for the Repository Content Selector privilege', async function() {
    const {type, name, description, contentSelector, getActionsGroup, repository, querySubmitButton,
      queryFormError, format} = selectors;

    when(Axios.post).calledWith(EXT_URL, expect.objectContaining({action: 'coreui_Selector'}))
        .mockResolvedValue({data: TestUtils.makeExtResult(clone(SELECTORS))});

    await renderAndWaitForLoad();
    expect(Axios.get).toHaveBeenCalledWith(PRIVILEGES_TYPES);

    userEvent.selectOptions(type(), TYPE_IDS.REPOSITORY_CONTENT_SELECTOR);
    await waitFor(() => {
      expect(Axios.post).toHaveBeenCalledWith(EXT_URL, expect.objectContaining({action: 'coreui_Selector'}));
      expect(Axios.post).not.toHaveBeenCalledWith(EXT_URL, expect.objectContaining({method: 'readReferencesAddingEntryForAll'}));
    });

    expect(name()).not.toHaveErrorMessage(TestUtils.REQUIRED_MESSAGE);
    expect(description()).not.toHaveErrorMessage(TestUtils.REQUIRED_MESSAGE);
    expect(contentSelector()).not.toHaveErrorMessage(TestUtils.REQUIRED_MESSAGE);
    expect(format()).not.toHaveErrorMessage(TestUtils.REQUIRED_MESSAGE);
    expect(repository()).not.toHaveErrorMessage(TestUtils.REQUIRED_MESSAGE);
    expect(getActionsGroup()).not.toHaveAccessibleDescription(NO_ACTION_ERROR);

    userEvent.click(querySubmitButton());
    expect(queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();
    expect(name()).toHaveErrorMessage(TestUtils.REQUIRED_MESSAGE);
    expect(contentSelector()).toHaveErrorMessage(TestUtils.REQUIRED_MESSAGE);
    expect(format()).toHaveErrorMessage(TestUtils.REQUIRED_MESSAGE);
    expect(repository()).toHaveErrorMessage(TestUtils.REQUIRED_MESSAGE);
    expect(getActionsGroup()).toHaveAccessibleDescription(NO_ACTION_ERROR);
  });

  it('creates Repository Content Selector privilege', async function() {
    const {
      type,
      name,
      description,
      contentSelector,
      getActionsGroup,
      getActionCheckbox,
      repository,
      querySubmitButton,
      querySavingMask,
      format
    } = selectors;

    when(Axios.post).calledWith(EXT_URL, expect.objectContaining({method: 'readReferencesAddingEntryForAll'}))
        .mockResolvedValue({data: TestUtils.makeExtResult(clone(REPOSITORIES))});
    when(Axios.post).calledWith(EXT_URL, expect.objectContaining({action: 'coreui_Selector'}))
        .mockResolvedValue({data: TestUtils.makeExtResult(clone(SELECTORS))});
    when(Axios.post).calledWith(createPrivilegeUrl(TYPE_IDS.REPOSITORY_CONTENT_SELECTOR), REPO_SELECTOR_PRIVILEGE)
        .mockResolvedValue({data: {}});

    await renderAndWaitForLoad();

    userEvent.selectOptions(type(), TYPE_IDS.REPOSITORY_CONTENT_SELECTOR);
    await TestUtils.changeField(name, testName);
    await TestUtils.changeField(description, testDescription);
    userEvent.selectOptions(contentSelector(), testContentSelector);
    await TestUtils.changeField(format, testFormat);

    await TestUtils.changeField(repository, 'm');
    await waitFor(() => expect(screen.getByText(testRepository)).toBeInTheDocument());
    userEvent.click(screen.getByText(testRepository));

    const browseCheckbox = getActionCheckbox(getActionsGroup(), 'Browse');
    const readCheckbox = getActionCheckbox(getActionsGroup(), 'Read');
    userEvent.click(browseCheckbox);
    userEvent.click(readCheckbox);

    userEvent.click(querySubmitButton());
    await waitForElementToBeRemoved(querySavingMask());

    expect(Axios.post).toHaveBeenCalledWith(
        createPrivilegeUrl(TYPE_IDS.REPOSITORY_CONTENT_SELECTOR),
        REPO_SELECTOR_PRIVILEGE
    );
    expect(NX.Messages.success).toHaveBeenCalledWith(UIStrings.SAVE_SUCCESS);
  });

  it('updates Script privilege', async function() {
    const {description, scriptName, getActionsGroup, getActionCheckbox, querySubmitButton, querySavingMask} = selectors;

    const data = {
      type: TYPE_IDS.SCRIPT,
      name: testName,
      description: 'Updated description',
      scriptName: 'NewScriptName',
      actions: ['delete', 'edit'],
      readOnly: false,
    };

    Axios.put.mockReturnValue(Promise.resolve());

    await renderAndWaitForLoad(testName);

    await TestUtils.changeField(description, data.description);
    await TestUtils.changeField(scriptName, data.scriptName);
    const runCheckbox = getActionCheckbox(getActionsGroup(), 'Run');
    const addCheckbox = getActionCheckbox(getActionsGroup(), 'Add');
    userEvent.click(runCheckbox);
    userEvent.click(addCheckbox);
    const deleteCheckbox = getActionCheckbox(getActionsGroup(), 'Delete');
    const editCheckbox = getActionCheckbox(getActionsGroup(), 'Edit');
    userEvent.click(deleteCheckbox);
    userEvent.click(editCheckbox);

    userEvent.click(querySubmitButton());
    await waitForElementToBeRemoved(querySavingMask());

    expect(Axios.put).toHaveBeenCalledWith(updatePrivilegeUrl(TYPE_IDS.SCRIPT, testName), data);
    expect(NX.Messages.success).toHaveBeenCalledWith(UIStrings.SAVE_SUCCESS);
  });

  it('shows save API errors', async function() {
    const message = "Use a unique privilegeId";
    const {type, name, description, scriptName, getActionsGroup, getActionCheckbox, querySubmitButton, querySavingMask} = selectors;

    when(Axios.post).calledWith(createPrivilegeUrl(TYPE_IDS.SCRIPT), expect.objectContaining({name: testName}))
        .mockRejectedValue({response: {data: message}});

    await renderAndWaitForLoad();

    userEvent.selectOptions(type(), TYPE_IDS.SCRIPT);

    await TestUtils.changeField(name, testName);
    await TestUtils.changeField(description, testDescription);
    await TestUtils.changeField(scriptName, testScriptName);
    const addCheckbox = getActionCheckbox(getActionsGroup(), 'Add');
    userEvent.click(addCheckbox);

    userEvent.click(querySubmitButton());
    await waitForElementToBeRemoved(querySavingMask());

    expect(screen.getByText(new RegExp(message))).toBeInTheDocument();
  });

  it('requests confirmation when delete is requested', async function() {
    const {deleteButton} = selectors;
    Axios.delete.mockReturnValue(Promise.resolve(null));

    await renderAndWaitForLoad(testName);

    ExtJS.requestConfirmation.mockReturnValue(CONFIRM);
    userEvent.click(deleteButton());

    await waitFor(() => expect(Axios.delete).toBeCalledWith(singlePrivilegeUrl(testName)));
    expect(onDone).toBeCalled();
    expect(ExtJS.showSuccessMessage).toBeCalled();
  });

  it('fires onDone when cancelled', async function() {
    const {cancelButton} = selectors;
    await renderAndWaitForLoad();

    userEvent.click(cancelButton());

    await waitFor(() => expect(onDone).toBeCalled());
  });

  describe('Read Only Mode', function() {
    const shouldSeeDetailsInReadOnlyMode = typeLabel => {
      const {readOnly: {type, name, description}} = selectors;
      expect(type()).toHaveTextContent(typeLabel);
      expect(name()).toHaveTextContent(testName);
      expect(description()).toHaveTextContent(testDescription);
    };

    it('renders Script privilege in Read Only Mode', async () => {
      const {cancelButton, readOnly: {scriptName, actions}} = selectors;
      const warning = () => screen.getByText(LABELS.DEFAULT_PRIVILEGE_WARNING);

      when(Axios.get).calledWith(singlePrivilegeUrl(testName)).mockResolvedValue({
        data: {...SCRIPT_PRIVILEGE, readOnly: true}
      });

      await renderAndWaitForLoad(testName);

      expect(warning()).toBeInTheDocument();
      shouldSeeDetailsInReadOnlyMode(TYPES_MAP[TYPE_IDS.SCRIPT].name);

      expect(scriptName()).toHaveTextContent(testScriptName);
      expect(actions()).toHaveTextContent("Run, Add");

      userEvent.click(cancelButton());
      await waitFor(() => expect(onDone).toBeCalled());
    });

    it('renders Repository Content Selector privilege in Read Only Mode', async () => {
      const {cancelButton, readOnly: {actions, contentSelector, format, repository}} = selectors;

      const warning = () => screen.getByText(LABELS.DEFAULT_PRIVILEGE_WARNING);

      when(Axios.get).calledWith(singlePrivilegeUrl(testName)).mockResolvedValue({
        data: {...REPO_SELECTOR_PRIVILEGE, readOnly: true}
      });

      await renderAndWaitForLoad(testName);

      expect(warning()).toBeInTheDocument();
      shouldSeeDetailsInReadOnlyMode(TYPES_MAP[TYPE_IDS.REPOSITORY_CONTENT_SELECTOR].name);

      expect(contentSelector()).toHaveTextContent(testContentSelector);
      expect(format()).toHaveTextContent(testFormat);
      expect(repository()).toHaveTextContent(testRepository);
      expect(actions()).toHaveTextContent("Browse, Read");

      userEvent.click(cancelButton());
      await waitFor(() => expect(onDone).toBeCalled());
    });

    it('renders Script privilege without edit permissions', async () => {
      const {readOnly: {scriptName, actions}} = selectors;
      const warning = () => screen.getByText(SETTINGS.READ_ONLY.WARNING);

      when(ExtJS.checkPermission).calledWith('nexus:privileges:update').mockReturnValue(false);

      await renderAndWaitForLoad(testName);

      expect(warning()).toBeInTheDocument();
      shouldSeeDetailsInReadOnlyMode(TYPES_MAP[TYPE_IDS.SCRIPT].name);

      expect(scriptName()).toHaveTextContent(testScriptName);
      expect(actions()).toHaveTextContent("Run, Add");
    });
  });

  it('uses proper urls', function() {
    expect(privilegesUrl).toBe('service/rest/v1/security/privileges');

    expect(singlePrivilegeUrl('testId')).toBe('service/rest/v1/security/privileges/testId');
    expect(singlePrivilegeUrl('a.b_c-d')).toBe('service/rest/v1/security/privileges/a.b_c-d');

    expect(updatePrivilegeUrl('repository-admin','test')).toBe('service/rest/v1/security/privileges/repository-admin/test');
    expect(createPrivilegeUrl('repository-admin')).toBe('service/rest/v1/security/privileges/repository-admin');
  });
});

function expectActionsToRender(actionsGroup, actions, selectedActions) {
  const {getActionCheckbox} = selectors;

  expect(actionsGroup).toBeInTheDocument();
  for (let a of actions) {
    const checkbox = getActionCheckbox(actionsGroup, a);
    expect(checkbox).toBeInTheDocument();
    selectedActions.includes(a) ? expect(checkbox).toBeChecked() : expect(checkbox).not.toBeChecked();
  }
}
