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
import { getRouter } from '../../../../routerConfig/routerConfig';
import { UIRouter, useCurrentStateAndParams } from '@uirouter/react';

const {privilegesUrl, singlePrivilegeUrl, updatePrivilegeUrl, createPrivilegeUrl} = URL;

const XSS_STRING = TestUtils.XSS_STRING;
const {PRIVILEGES: {FORM: LABELS, MESSAGES: {NO_ACTION_ERROR}}, SETTINGS} = UIStrings;
const {EXT: {URL: EXT_URL}, REST: {INTERNAL: {PRIVILEGES_TYPES}}} = APIConstants;

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    requestConfirmation: jest.fn(),
    checkPermission: jest.fn(),
    showErrorMessage: jest.fn(),
    showSuccessMessage: jest.fn(),
    state: jest.fn().mockReturnValue({
      getValue: jest.fn()
    })
  },
}));

jest.mock('@uirouter/react', () => ({
  ...jest.requireActual('@uirouter/react'),
  useCurrentStateAndParams: jest.fn(),
}));

const testName = 'PrivilegeName';
const testDescription = 'Test Privilege Description';
const testScriptName = 'TestScriptName';
const testScriptActions = 'run,add';
const testContentSelectorActions = 'browse,read';
const testContentSelector = 'Test_Selector_1';
const testRepository = 'TestRepository';

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
  format: '*',
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
    repository: () => screen.getByText(FIELDS.REPOSITORY.LABEL).nextSibling,
  },
  cancelButton: () => screen.queryByRole('button', {name: SETTINGS.CANCEL_BUTTON_LABEL}),
  deleteButton: () => screen.queryByRole('button', {name: SETTINGS.DELETE_BUTTON_LABEL}),
};

describe('PrivilegesDetails', function() {
  const CONFIRM = Promise.resolve();

  const renderAndWaitForLoad = async (itemId) => {
    useCurrentStateAndParams.mockReturnValue({params: {itemId}});
    const router = getRouter();
    const view = (
      <UIRouter router={router}>
        <PrivilegesDetails />
      </UIRouter>
    );
    render(view);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
  }

  beforeEach(() => {
    when(Axios.get).calledWith(PRIVILEGES_TYPES)
        .mockResolvedValue({data: clone(TYPES)});
    when(Axios.get).calledWith(singlePrivilegeUrl(testName)).mockResolvedValue({
      data: {...SCRIPT_PRIVILEGE, readOnly: false}
    });
    ExtJS.checkPermission.mockReturnValue(true);
    useCurrentStateAndParams.mockReset();
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
    expect(name()).toHaveAccessibleErrorMessage(TestUtils.REQUIRED_MESSAGE);
    await TestUtils.changeField(name, testName);
    expect(name()).not.toHaveAccessibleErrorMessage(TestUtils.REQUIRED_MESSAGE);

    await TestUtils.changeField(description, testDescription);

    await TestUtils.changeField(scriptName, testScriptName);
    userEvent.clear(scriptName());
    expect(scriptName()).toHaveAccessibleErrorMessage(TestUtils.REQUIRED_MESSAGE);
    await TestUtils.changeField(scriptName, testScriptName);
    expect(scriptName()).not.toHaveAccessibleErrorMessage(TestUtils.REQUIRED_MESSAGE);

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

  it('renders validation messages for the Repository Content Selector privilege', async function () {
    const { type, name, description, contentSelector, getActionsGroup, repository, querySubmitButton, queryFormError } =
      selectors;

    when(Axios.post).calledWith(EXT_URL, expect.objectContaining({action: 'coreui_Selector'}))
        .mockResolvedValue({data: TestUtils.makeExtResult(clone(SELECTORS))});

    await renderAndWaitForLoad();
    expect(Axios.get).toHaveBeenCalledWith(PRIVILEGES_TYPES);

    userEvent.selectOptions(type(), TYPE_IDS.REPOSITORY_CONTENT_SELECTOR);
    await waitFor(() => {
      expect(Axios.post).toHaveBeenCalledWith(EXT_URL, expect.objectContaining({action: 'coreui_Selector'}));
      expect(Axios.post).not.toHaveBeenCalledWith(EXT_URL, expect.objectContaining({method: 'readReferencesAddingEntryForAll'}));
    });

    expect(name()).not.toHaveAccessibleErrorMessage(TestUtils.REQUIRED_MESSAGE);
    expect(description()).not.toHaveAccessibleErrorMessage(TestUtils.REQUIRED_MESSAGE);
    expect(contentSelector()).not.toHaveAccessibleErrorMessage(TestUtils.REQUIRED_MESSAGE);
    expect(repository()).not.toHaveAccessibleErrorMessage(TestUtils.REQUIRED_MESSAGE);
    expect(getActionsGroup()).not.toHaveAccessibleDescription(NO_ACTION_ERROR);

    userEvent.click(querySubmitButton());
    expect(queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();
    expect(name()).toHaveAccessibleErrorMessage(TestUtils.REQUIRED_MESSAGE);
    expect(contentSelector()).toHaveAccessibleErrorMessage(TestUtils.REQUIRED_MESSAGE);
    expect(repository()).toHaveAccessibleErrorMessage(TestUtils.REQUIRED_MESSAGE);
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
    } = selectors;

    when(Axios.post).calledWith(EXT_URL, expect.objectContaining({method: 'readReferencesAddingEntriesForAllFormats'}))
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
    expect(ExtJS.showSuccessMessage).toBeCalled();
  });

  describe('Read Only Mode', function() {
    const shouldSeeDetailsInReadOnlyMode = typeLabel => {
      const {readOnly: {type, name, description}} = selectors;
      expect(type()).toHaveTextContent(typeLabel);
      expect(name()).toHaveTextContent(testName);
      expect(description()).toHaveTextContent(testDescription);
    };

    it('renders Script privilege in Read Only Mode', async () => {
      const {readOnly: {scriptName, actions}} = selectors;
      const warning = () => screen.getByText(LABELS.DEFAULT_PRIVILEGE_WARNING);

      when(Axios.get).calledWith(singlePrivilegeUrl(testName)).mockResolvedValue({
        data: {...SCRIPT_PRIVILEGE, readOnly: true}
      });

      await renderAndWaitForLoad(testName);

      expect(warning()).toBeInTheDocument();
      shouldSeeDetailsInReadOnlyMode(TYPES_MAP[TYPE_IDS.SCRIPT].name);

      expect(scriptName()).toHaveTextContent(testScriptName);
      expect(actions()).toHaveTextContent("Run, Add");
    });

    it('renders Repository Content Selector privilege in Read Only Mode', async () => {
      const {
        readOnly: { actions, contentSelector, repository },
      } = selectors;

      const warning = () => screen.getByText(LABELS.DEFAULT_PRIVILEGE_WARNING);

      when(Axios.get).calledWith(singlePrivilegeUrl(testName)).mockResolvedValue({
        data: {...REPO_SELECTOR_PRIVILEGE, readOnly: true}
      });

      await renderAndWaitForLoad(testName);

      expect(warning()).toBeInTheDocument();
      shouldSeeDetailsInReadOnlyMode(TYPES_MAP[TYPE_IDS.REPOSITORY_CONTENT_SELECTOR].name);

      expect(contentSelector()).toHaveTextContent(testContentSelector);
      expect(repository()).toHaveTextContent(testRepository);
      expect(actions()).toHaveTextContent("Browse, Read");
    });

    it('does not transform repository field when loading Repository Content Selector privilege with specific repository', async () => {
      const {repository} = selectors;
      const testFormat = 'maven2';
      const PRIVILEGE_WITH_FORMAT = {
        type: TYPE_IDS.REPOSITORY_CONTENT_SELECTOR,
        name: testName,
        description: testDescription,
        contentSelector: testContentSelector,
        format: testFormat,
        repository: testRepository,
        actions: testContentSelectorActions.split(','),
        readOnly: false,
      };

      when(Axios.get).calledWith(singlePrivilegeUrl(testName)).mockResolvedValue({
        data: PRIVILEGE_WITH_FORMAT
      });

      await renderAndWaitForLoad(testName);

      // Repository field should remain unchanged when it's a specific repository
      expect(repository()).toHaveValue(testRepository);
    });

    it('transforms repository field when loading Repository Content Selector privilege with wildcard repository and non-wildcard format', async () => {
      const {repository} = selectors;
      const testFormat = 'maven2';
      const PRIVILEGE_WITH_WILDCARD_REPO = {
        type: TYPE_IDS.REPOSITORY_CONTENT_SELECTOR,
        name: testName,
        description: testDescription,
        contentSelector: testContentSelector,
        format: testFormat,
        repository: '*',
        actions: testContentSelectorActions.split(','),
        readOnly: false,
      };

      when(Axios.get).calledWith(singlePrivilegeUrl(testName)).mockResolvedValue({
        data: PRIVILEGE_WITH_WILDCARD_REPO
      });

      await renderAndWaitForLoad(testName);

      // Repository field should be transformed to *-{format} when repository is '*' and format is specific
      expect(repository()).toHaveValue(`*-${testFormat}`);
    });

    it('does not transform repository field when loading Repository Content Selector privilege with wildcard format', async () => {
      const {repository} = selectors;
      const PRIVILEGE_WITH_WILDCARD_FORMAT = {
        type: TYPE_IDS.REPOSITORY_CONTENT_SELECTOR,
        name: testName,
        description: testDescription,
        contentSelector: testContentSelector,
        format: '*',
        repository: testRepository,
        actions: testContentSelectorActions.split(','),
        readOnly: false,
      };

      when(Axios.get).calledWith(singlePrivilegeUrl(testName)).mockResolvedValue({
        data: PRIVILEGE_WITH_WILDCARD_FORMAT
      });

      await renderAndWaitForLoad(testName);

      // Repository field should remain unchanged when format is '*'
      expect(repository()).toHaveValue(testRepository);
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

  describe('ALL Action Support', function() {
    it('expands ALL action to all checkboxes when loading a privilege', async function() {
      const {getActionsGroup, getActionCheckbox} = selectors;
      const PRIVILEGE_WITH_ALL = {
        type: TYPE_IDS.REPOSITORY_CONTENT_SELECTOR,
        name: testName,
        description: testDescription,
        contentSelector: testContentSelector,
        format: '*',
        repository: testRepository,
        actions: ['ALL'],
        readOnly: false,
      };

      when(Axios.get).calledWith(singlePrivilegeUrl(testName)).mockResolvedValue({
        data: PRIVILEGE_WITH_ALL
      });

      await renderAndWaitForLoad(testName);

      const actionsGroup = getActionsGroup();
      expect(actionsGroup).toBeInTheDocument();

      // All checkboxes should be checked
      expect(getActionCheckbox(actionsGroup, 'Browse')).toBeChecked();
      expect(getActionCheckbox(actionsGroup, 'Read')).toBeChecked();
      expect(getActionCheckbox(actionsGroup, 'Edit')).toBeChecked();
      expect(getActionCheckbox(actionsGroup, 'Add')).toBeChecked();
      expect(getActionCheckbox(actionsGroup, 'Delete')).toBeChecked();
    });

    it('expands * action to all checkboxes when loading a privilege', async function() {
      const {getActionsGroup, getActionCheckbox} = selectors;
      const PRIVILEGE_WITH_STAR = {
        type: TYPE_IDS.REPOSITORY_CONTENT_SELECTOR,
        name: testName,
        description: testDescription,
        contentSelector: testContentSelector,
        format: '*',
        repository: testRepository,
        actions: ['*'],
        readOnly: false,
      };

      when(Axios.get).calledWith(singlePrivilegeUrl(testName)).mockResolvedValue({
        data: PRIVILEGE_WITH_STAR
      });

      await renderAndWaitForLoad(testName);

      const actionsGroup = getActionsGroup();
      expect(actionsGroup).toBeInTheDocument();

      // All checkboxes should be checked
      expect(getActionCheckbox(actionsGroup, 'Browse')).toBeChecked();
      expect(getActionCheckbox(actionsGroup, 'Read')).toBeChecked();
      expect(getActionCheckbox(actionsGroup, 'Edit')).toBeChecked();
      expect(getActionCheckbox(actionsGroup, 'Add')).toBeChecked();
      expect(getActionCheckbox(actionsGroup, 'Delete')).toBeChecked();
    });

    it('sends ALL when all checkboxes are selected and saved', async function() {
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
      } = selectors;

      const PRIVILEGE_WITH_ALL_ACTIONS = {
        type: TYPE_IDS.REPOSITORY_CONTENT_SELECTOR,
        name: testName,
        description: testDescription,
        contentSelector: testContentSelector,
        format: '*',
        repository: testRepository,
        actions: ['ALL'],
      };

      when(Axios.post).calledWith(EXT_URL, expect.objectContaining({method: 'readReferencesAddingEntriesForAllFormats'}))
          .mockResolvedValue({data: TestUtils.makeExtResult(clone(REPOSITORIES))});
      when(Axios.post).calledWith(EXT_URL, expect.objectContaining({action: 'coreui_Selector'}))
          .mockResolvedValue({data: TestUtils.makeExtResult(clone(SELECTORS))});
      when(Axios.post).calledWith(createPrivilegeUrl(TYPE_IDS.REPOSITORY_CONTENT_SELECTOR), PRIVILEGE_WITH_ALL_ACTIONS)
          .mockResolvedValue({data: {}});

      await renderAndWaitForLoad();

      userEvent.selectOptions(type(), TYPE_IDS.REPOSITORY_CONTENT_SELECTOR);
      await TestUtils.changeField(name, testName);
      await TestUtils.changeField(description, testDescription);
      userEvent.selectOptions(contentSelector(), testContentSelector);

      await TestUtils.changeField(repository, 'm');
      await waitFor(() => expect(screen.getByText(testRepository)).toBeInTheDocument());
      userEvent.click(screen.getByText(testRepository));

      const actionsGroup = getActionsGroup();
      // Select all actions
      userEvent.click(getActionCheckbox(actionsGroup, 'Browse'));
      userEvent.click(getActionCheckbox(actionsGroup, 'Read'));
      userEvent.click(getActionCheckbox(actionsGroup, 'Edit'));
      userEvent.click(getActionCheckbox(actionsGroup, 'Add'));
      userEvent.click(getActionCheckbox(actionsGroup, 'Delete'));

      userEvent.click(querySubmitButton());
      await waitForElementToBeRemoved(querySavingMask());

      // Verify that ALL was sent instead of individual actions
      expect(Axios.post).toHaveBeenCalledWith(
          createPrivilegeUrl(TYPE_IDS.REPOSITORY_CONTENT_SELECTOR),
          PRIVILEGE_WITH_ALL_ACTIONS
      );
      expect(NX.Messages.success).toHaveBeenCalledWith(UIStrings.SAVE_SUCCESS);
    });

    it('updates privilege with ALL when all checkboxes are selected', async function() {
      const {getActionsGroup, getActionCheckbox, querySubmitButton, querySavingMask} = selectors;

      const INITIAL_PRIVILEGE = {
        type: TYPE_IDS.REPOSITORY_CONTENT_SELECTOR,
        name: testName,
        description: testDescription,
        contentSelector: testContentSelector,
        format: '*',
        repository: testRepository,
        actions: ['browse', 'read'],
        readOnly: false,
      };

      const UPDATED_PRIVILEGE = {
        ...INITIAL_PRIVILEGE,
        actions: ['ALL'],
      };

      when(Axios.get).calledWith(singlePrivilegeUrl(testName)).mockResolvedValue({
        data: INITIAL_PRIVILEGE
      });
      Axios.put.mockResolvedValue({data: {}});

      await renderAndWaitForLoad(testName);

      const actionsGroup = getActionsGroup();

      // Initially only browse and read should be checked
      expect(getActionCheckbox(actionsGroup, 'Browse')).toBeChecked();
      expect(getActionCheckbox(actionsGroup, 'Read')).toBeChecked();
      expect(getActionCheckbox(actionsGroup, 'Edit')).not.toBeChecked();

      // Select all remaining actions
      userEvent.click(getActionCheckbox(actionsGroup, 'Edit'));
      userEvent.click(getActionCheckbox(actionsGroup, 'Add'));
      userEvent.click(getActionCheckbox(actionsGroup, 'Delete'));

      userEvent.click(querySubmitButton());
      await waitForElementToBeRemoved(querySavingMask());

      // Verify that ALL was sent
      expect(Axios.put).toHaveBeenCalledWith(
          updatePrivilegeUrl(TYPE_IDS.REPOSITORY_CONTENT_SELECTOR, testName),
          UPDATED_PRIVILEGE
      );
      expect(NX.Messages.success).toHaveBeenCalledWith(UIStrings.SAVE_SUCCESS);
    });

    it('renders ALL action correctly in Read Only Mode', async () => {
      const {readOnly: {actions}} = selectors;
      const PRIVILEGE_WITH_ALL = {
        type: TYPE_IDS.REPOSITORY_CONTENT_SELECTOR,
        name: testName,
        description: testDescription,
        contentSelector: testContentSelector,
        format: '*',
        repository: testRepository,
        actions: ['ALL'],
        readOnly: true,
      };

      when(Axios.get).calledWith(singlePrivilegeUrl(testName)).mockResolvedValue({
        data: PRIVILEGE_WITH_ALL
      });

      await renderAndWaitForLoad(testName);

      // Should display all actions as text
      expect(actions()).toHaveTextContent("Browse, Read, Edit, Add, Delete");
    });

    it('handles Script privilege with ALL action including run', async function() {
      const {getActionsGroup, getActionCheckbox} = selectors;
      const SCRIPT_PRIVILEGE_WITH_ALL = {
        type: TYPE_IDS.SCRIPT,
        name: testName,
        description: testDescription,
        scriptName: testScriptName,
        actions: ['ALL'],
        readOnly: false,
      };

      when(Axios.get).calledWith(singlePrivilegeUrl(testName)).mockResolvedValue({
        data: SCRIPT_PRIVILEGE_WITH_ALL
      });

      await renderAndWaitForLoad(testName);

      const actionsGroup = getActionsGroup();
      expect(actionsGroup).toBeInTheDocument();

      // All checkboxes including 'run' should be checked
      expect(getActionCheckbox(actionsGroup, 'Browse')).toBeChecked();
      expect(getActionCheckbox(actionsGroup, 'Read')).toBeChecked();
      expect(getActionCheckbox(actionsGroup, 'Edit')).toBeChecked();
      expect(getActionCheckbox(actionsGroup, 'Add')).toBeChecked();
      expect(getActionCheckbox(actionsGroup, 'Delete')).toBeChecked();
      expect(getActionCheckbox(actionsGroup, 'Run')).toBeChecked();
    });

    it('sends ALL for Script privilege when all checkboxes including run are selected', async function() {
      const {type, name, description, scriptName, getActionsGroup, getActionCheckbox, querySubmitButton, querySavingMask} = selectors;

      const SCRIPT_WITH_ALL = {
        type: TYPE_IDS.SCRIPT,
        name: testName,
        description: testDescription,
        scriptName: testScriptName,
        actions: ['ALL'],
      };

      when(Axios.post).calledWith(createPrivilegeUrl(TYPE_IDS.SCRIPT), SCRIPT_WITH_ALL).mockResolvedValue({data: {}});

      await renderAndWaitForLoad();

      userEvent.selectOptions(type(), TYPE_IDS.SCRIPT);
      await TestUtils.changeField(name, testName);
      await TestUtils.changeField(description, testDescription);
      await TestUtils.changeField(scriptName, testScriptName);

      const actionsGroup = getActionsGroup();
      // Select all actions including run
      userEvent.click(getActionCheckbox(actionsGroup, 'Browse'));
      userEvent.click(getActionCheckbox(actionsGroup, 'Read'));
      userEvent.click(getActionCheckbox(actionsGroup, 'Edit'));
      userEvent.click(getActionCheckbox(actionsGroup, 'Add'));
      userEvent.click(getActionCheckbox(actionsGroup, 'Delete'));
      userEvent.click(getActionCheckbox(actionsGroup, 'Run'));

      userEvent.click(querySubmitButton());
      await waitForElementToBeRemoved(querySavingMask());

      expect(Axios.post).toHaveBeenCalledWith(createPrivilegeUrl(TYPE_IDS.SCRIPT), SCRIPT_WITH_ALL);
      expect(NX.Messages.success).toHaveBeenCalledWith(UIStrings.SAVE_SUCCESS);
    });

    it('does not send ALL if only some actions are selected', async function() {
      const {type, name, description, contentSelector, getActionsGroup, getActionCheckbox, repository, querySubmitButton, querySavingMask} = selectors;

      const PRIVILEGE_WITH_SOME_ACTIONS = {
        type: TYPE_IDS.REPOSITORY_CONTENT_SELECTOR,
        name: testName,
        description: testDescription,
        contentSelector: testContentSelector,
        format: '*',
        repository: testRepository,
        actions: ['browse', 'read', 'edit'], // Only 3 out of 5
      };

      when(Axios.post).calledWith(EXT_URL, expect.objectContaining({method: 'readReferencesAddingEntriesForAllFormats'}))
          .mockResolvedValue({data: TestUtils.makeExtResult(clone(REPOSITORIES))});
      when(Axios.post).calledWith(EXT_URL, expect.objectContaining({action: 'coreui_Selector'}))
          .mockResolvedValue({data: TestUtils.makeExtResult(clone(SELECTORS))});
      when(Axios.post).calledWith(createPrivilegeUrl(TYPE_IDS.REPOSITORY_CONTENT_SELECTOR), PRIVILEGE_WITH_SOME_ACTIONS)
          .mockResolvedValue({data: {}});

      await renderAndWaitForLoad();

      userEvent.selectOptions(type(), TYPE_IDS.REPOSITORY_CONTENT_SELECTOR);
      await TestUtils.changeField(name, testName);
      await TestUtils.changeField(description, testDescription);
      userEvent.selectOptions(contentSelector(), testContentSelector);

      await TestUtils.changeField(repository, 'm');
      await waitFor(() => expect(screen.getByText(testRepository)).toBeInTheDocument());
      userEvent.click(screen.getByText(testRepository));

      const actionsGroup = getActionsGroup();
      // Select only 3 actions
      userEvent.click(getActionCheckbox(actionsGroup, 'Browse'));
      userEvent.click(getActionCheckbox(actionsGroup, 'Read'));
      userEvent.click(getActionCheckbox(actionsGroup, 'Edit'));

      userEvent.click(querySubmitButton());
      await waitForElementToBeRemoved(querySavingMask());

      // Verify individual actions were sent, not ALL
      expect(Axios.post).toHaveBeenCalledWith(
          createPrivilegeUrl(TYPE_IDS.REPOSITORY_CONTENT_SELECTOR),
          PRIVILEGE_WITH_SOME_ACTIONS
      );
      expect(NX.Messages.success).toHaveBeenCalledWith(UIStrings.SAVE_SUCCESS);
    });
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
