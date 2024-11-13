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
import {ExtJS, APIConstants, Permissions} from '@sonatype/nexus-ui-plugin';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import UIStrings from '../../../../constants/UIStrings';
import TasksDetails from './TasksDetails';

import {TASKS, TASKS_TYPES} from './Tasks.testdata';
import {URLs} from './TasksHelper';

const {runTaskUrl, stopTaskUrl} = URLs;
const XSS_STRING = TestUtils.XSS_STRING;
const {TASKS: {FORM: LABELS, SUMMARY}, SETTINGS} = UIStrings;
const {EXT: {TASK: {ACTION, METHODS}, URL: EXT_URL}} = APIConstants;

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
    formatDate: jest.fn().mockReturnValue('+01:00'),
  },
}));

const testId = 'd3275dbe-c784-47f1-8ea2-338088f7ceab';
const TASK = TASKS.find(task => task.id === testId);

const selectors = {
  ...TestUtils.selectors,
  ...TestUtils.formSelectors,
  type: () => screen.queryByLabelText(LABELS.TYPE.LABEL),
  enabled: () => screen.queryByLabelText(LABELS.ENABLED.SUB_LABEL),
  name: () => screen.queryByLabelText(LABELS.NAME.LABEL),
  alertEmail: () => screen.queryByLabelText(LABELS.EMAIL.LABEL),
  notificationCondition: () => screen.queryByLabelText(LABELS.SEND_NOTIFICATION_ON.LABEL),
  summary: {
    tile: () => screen.queryByRole('heading', {name: LABELS.SECTIONS.SUMMARY}).closest('section'),
    id: () => within(selectors.summary.tile()).getByText(SUMMARY.ID).nextSibling,
    name: () => within(selectors.summary.tile()).getByText(SUMMARY.NAME).nextSibling,
    type: () => within(selectors.summary.tile()).getByText(SUMMARY.TYPE).nextSibling,
    status: () => within(selectors.summary.tile()).getByText(SUMMARY.STATUS).nextSibling,
    lastResult: () => within(selectors.summary.tile()).getByText(SUMMARY.LAST_RESULT).nextSibling,
    nextRun: () => within(selectors.summary.tile()).getByText(SUMMARY.NEXT_RUN).nextSibling,
    lastRun: () => within(selectors.summary.tile()).getByText(SUMMARY.LAST_RUN).nextSibling,
    runButton: () => within(selectors.summary.tile()).queryByRole('button', {name: SUMMARY.BUTTONS.RUN}),
    stopButton: () => within(selectors.summary.tile()).queryByRole('button', {name: SUMMARY.BUTTONS.STOP}),
    deleteButton: () => within(selectors.summary.tile()).queryByRole('button', {name: SETTINGS.DELETE_BUTTON_LABEL}),
  },
  cancelButton: () => screen.getByText(SETTINGS.CANCEL_BUTTON_LABEL),
};

describe('TasksDetails', function() {
  const onDone = jest.fn();
  const CONFIRM = Promise.resolve();

  const renderAndWaitForLoad = async (itemId) => {
    render(<TasksDetails itemId={itemId || ''} onDone={onDone}/>);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
  }

  const mockTasksResponse = (data) => {
    when(Axios.post).calledWith(EXT_URL, expect.objectContaining({action: ACTION, method: METHODS.READ}))
        .mockResolvedValue({data: TestUtils.makeExtResult(clone(data))});
  };

  beforeEach(() => {
    mockTasksResponse(TASKS);
    when(Axios.post).calledWith(EXT_URL, expect.objectContaining({action: ACTION, method: METHODS.READ_TYPES}))
        .mockResolvedValue({data: TestUtils.makeExtResult(clone(TASKS_TYPES))});
    ExtJS.checkPermission.mockReturnValue(true);
  });

  it('renders the resolved data', async function() {
    const {enabled, name, alertEmail, notificationCondition, querySubmitButton, queryFormError, summary: {
      id, name: summaryName, type: summaryType, status, lastResult, nextRun, lastRun, runButton, stopButton, deleteButton
    }} = selectors;

    await renderAndWaitForLoad(testId);

    expect(id()).toHaveTextContent(testId);
    expect(summaryName()).toHaveTextContent(TASK.name);
    expect(summaryType()).toHaveTextContent(TASK.typeName);
    expect(status()).toHaveTextContent(TASK.statusDescription);
    expect(nextRun()).toHaveTextContent(TASK.nextRun);
    expect(lastRun()).toHaveTextContent(TASK.lastRun);
    expect(lastResult()).toHaveTextContent(TASK.lastRunResult);
    expect(runButton()).toBeInTheDocument();
    expect(stopButton()).not.toBeInTheDocument();
    expect(deleteButton()).toBeInTheDocument();

    expect(name()).toHaveValue(TASK.name);
    expect(alertEmail()).toHaveValue(TASK.alertEmail);
    expect(notificationCondition()).toHaveValue(TASK.notificationCondition);
    expect(enabled()).toBeChecked();

    userEvent.click(querySubmitButton());
    expect(queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();
  });

  it('renders the resolved data with XSS', async function() {
    const {name, alertEmail, summary: {
      name: summaryName, type: summaryType, status, lastResult, nextRun, lastRun
    }} = selectors;

    const XSS_TASK = {
      ...TASK,
      name: XSS_STRING,
      typeName: XSS_STRING,
      statusDescription: XSS_STRING,
      nextRun: XSS_STRING,
      lastRun: XSS_STRING,
      lastRunResult: XSS_STRING,
      alertEmail: XSS_STRING,
    };

    mockTasksResponse([XSS_TASK]);

    await renderAndWaitForLoad(testId);

    expect(name()).toHaveValue(XSS_STRING);
    expect(summaryName()).toHaveTextContent(XSS_STRING);
    expect(summaryType()).toHaveTextContent(XSS_STRING);
    expect(status()).toHaveTextContent(XSS_STRING);
    expect(nextRun()).toHaveTextContent(XSS_STRING);
    expect(lastRun()).toHaveTextContent(XSS_STRING);
    expect(lastResult()).toHaveTextContent(XSS_STRING);

    expect(name()).toHaveValue(XSS_STRING);
    expect(alertEmail()).toHaveValue(XSS_STRING);
  });

  it('renders load error message', async function() {
    const message = 'Load error message!';

    Axios.post.mockRejectedValue({message});

    await renderAndWaitForLoad();

    expect(screen.getByRole('alert')).toHaveTextContent(message);
  });

  it('validates type, name and notification email fields', async function() {
    const {type, enabled, name, alertEmail, notificationCondition, querySubmitButton, queryFormError} = selectors;

    await renderAndWaitForLoad();

    expect(type()).toBeInTheDocument();
    expect(name()).not.toBeInTheDocument();

    userEvent.click(querySubmitButton());
    expect(queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();
    expect(type()).toHaveErrorMessage(TestUtils.REQUIRED_MESSAGE);

    userEvent.selectOptions(type(), TASK.typeId);

    expect(enabled()).toBeChecked();
    expect(notificationCondition()).toHaveValue('FAILURE');

    expect(name()).toBeInTheDocument();
    expect(type()).not.toHaveErrorMessage();

    userEvent.type(name(), 'test');
    userEvent.clear(name());
    expect(name()).toHaveErrorMessage(TestUtils.REQUIRED_MESSAGE);

    userEvent.type(name(), 'test');
    expect(name()).not.toHaveErrorMessage();

    userEvent.type(alertEmail(), 'test');
    expect(alertEmail()).toHaveErrorMessage();

    userEvent.type(alertEmail(), 'test@test.com');
    expect(alertEmail()).not.toHaveErrorMessage();
  });

  it('creates task', async function() {
    const {type, name, alertEmail, querySubmitButton} = selectors;

    when(Axios.post).calledWith(EXT_URL, expect.objectContaining({action: ACTION, method: METHODS.CREATE}))
        .mockResolvedValue({data: TestUtils.makeExtResult({})});

    await renderAndWaitForLoad();

    userEvent.selectOptions(type(), TASK.typeId);
    userEvent.type(name(), 'test');
    userEvent.type(alertEmail(), 'test@test.com');

    userEvent.click(querySubmitButton());
    await waitForElementToBeRemoved(selectors.querySavingMask());

    expect(Axios.post).toHaveBeenLastCalledWith(EXT_URL, {
      action: ACTION,
      method: METHODS.CREATE,
      tid: 1,
      type: 'rpc',
      data: [{
        alertEmail: 'test@test.com',
        enabled: true,
        name: 'test',
        notificationCondition: 'FAILURE',
        properties: {},
        schedule: 'manual',
        timeZoneOffset: '+01:00',
        typeId: TASK.typeId,
      }],
    });
    expect(NX.Messages.success).toHaveBeenCalledWith(UIStrings.SAVE_SUCCESS);
  });

  it('updates task', async function() {
    const {name, alertEmail, notificationCondition, querySubmitButton} = selectors;

    const data = {
      name: 'new name',
      alertEmail: 'foo@bar.baz',
      notificationCondition: 'FAILURE',
    };

    when(Axios.post).calledWith(EXT_URL, expect.objectContaining({action: ACTION, method: METHODS.UPDATE}))
        .mockResolvedValue({data: TestUtils.makeExtResult({})});

    await renderAndWaitForLoad(testId);

    userEvent.clear(name());
    userEvent.clear(alertEmail());
    userEvent.type(name(), data.name);
    userEvent.type(alertEmail(), data.alertEmail);
    userEvent.selectOptions(notificationCondition(), data.notificationCondition);

    userEvent.click(querySubmitButton());
    await waitForElementToBeRemoved(selectors.querySavingMask());

    expect(Axios.post).toHaveBeenLastCalledWith(EXT_URL, {
      action: ACTION,
      method: METHODS.UPDATE,
      tid: 1,
      type: 'rpc',
      data: [{
        ...TASK,
        ...data,
        timeZoneOffset: "+01:00",
        properties: {},
      }],
    });

    expect(NX.Messages.success).toHaveBeenCalledWith(UIStrings.SAVE_SUCCESS);
  });

  it('shows save API errors', async function() {
    const message = 'Api error!';
    const {type, name, alertEmail, querySubmitButton} = selectors;

    when(Axios.post).calledWith(EXT_URL, expect.objectContaining({action: ACTION, method: METHODS.CREATE}))
        .mockRejectedValue({message});

    await renderAndWaitForLoad();

    userEvent.selectOptions(type(), TASK.typeId);
    userEvent.type(name(), 'test');
    userEvent.type(alertEmail(), 'test@test.com');

    userEvent.click(querySubmitButton());
    await waitForElementToBeRemoved(selectors.querySavingMask());

    expect(screen.getByText(new RegExp(message))).toBeInTheDocument();
  });

  it('requests confirmation when delete is requested', async function() {
    const {summary: {deleteButton}} = selectors;

    when(Axios.post).calledWith(EXT_URL, expect.objectContaining({action: ACTION, method: METHODS.DELETE}))
        .mockResolvedValue({data: TestUtils.makeExtResult({})});

    await renderAndWaitForLoad(testId);

    ExtJS.requestConfirmation.mockReturnValue(CONFIRM);
    userEvent.click(deleteButton());

    await waitFor(() => {
      expect(Axios.post).toHaveBeenLastCalledWith(EXT_URL, expect.objectContaining({
        action: ACTION,
        method: METHODS.DELETE,
        data: [testId],
      }));
    });

    expect(onDone).toBeCalled();
    expect(ExtJS.showSuccessMessage).toHaveBeenCalledWith(expect.stringContaining('Task deleted'));
  });

  it('requests confirmation when run task is requested', async function() {
    const {summary: {runButton}} = selectors;

    when(Axios.post).calledWith(runTaskUrl(testId)).mockResolvedValue({});

    await renderAndWaitForLoad(testId);

    ExtJS.requestConfirmation.mockReturnValue(CONFIRM);
    userEvent.click(runButton());

    await waitFor(() => {
      expect(Axios.post).toHaveBeenLastCalledWith('service/rest/v1/tasks/d3275dbe-c784-47f1-8ea2-338088f7ceab/run');
    });

    expect(ExtJS.showSuccessMessage).toHaveBeenCalledWith(expect.stringContaining('Task started'));
  });

  it('requests confirmation when stop task is requested', async function() {
    const {summary: {stopButton}} = selectors;

    mockTasksResponse([{...TASK, runnable: false, stoppable: true}]);
    when(Axios.post).calledWith(stopTaskUrl(testId)).mockResolvedValue({});

    await renderAndWaitForLoad(testId);

    ExtJS.requestConfirmation.mockReturnValue(CONFIRM);
    expect(stopButton()).toBeInTheDocument();

    userEvent.click(stopButton());
    await waitFor(() => {
      expect(Axios.post).toHaveBeenLastCalledWith('service/rest/v1/tasks/d3275dbe-c784-47f1-8ea2-338088f7ceab/stop');
    });

    expect(ExtJS.showSuccessMessage).toHaveBeenCalledWith(expect.stringContaining('Task stopped'));
  });

  it('shows the run button when a task is enabled and runnable', async function() {
    const {summary: {runButton, stopButton}} = selectors;

    mockTasksResponse([{...TASK, enabled: true, runnable: true, stoppable: false}]);

    await renderAndWaitForLoad(testId);

    expect(runButton()).toBeInTheDocument();
    expect(stopButton()).not.toBeInTheDocument();
  });

  it('shows the stop button when a task is stoppable', async function() {
    const {summary: {runButton, stopButton}} = selectors;

    mockTasksResponse([{...TASK, enabled: false, runnable: false, stoppable: true}]);

    await renderAndWaitForLoad(testId);

    expect(runButton()).not.toBeInTheDocument();
    expect(stopButton()).toBeInTheDocument();
  });

  it('hides the run button when a task in not enabled', async function() {
    const {summary: {runButton, stopButton}} = selectors;

    mockTasksResponse([{...TASK, enabled: false, runnable: true, stoppable: false}]);

    await renderAndWaitForLoad(testId);

    expect(runButton()).not.toBeInTheDocument();
    expect(stopButton()).not.toBeInTheDocument();
  });

  it('disables buttons when not enough permission', async function() {
    const {summary: {runButton, stopButton, deleteButton}} = selectors;

    mockTasksResponse([{...TASK, enabled: true, runnable: true, stoppable: true}]);
    when(ExtJS.checkPermission).calledWith(Permissions.TASKS.READ).mockReturnValue(true);
    when(ExtJS.checkPermission).calledWith(Permissions.TASKS.UPDATE).mockReturnValue(true);
    when(ExtJS.checkPermission).calledWith(Permissions.TASKS.DELETE).mockReturnValue(false);
    when(ExtJS.checkPermission).calledWith(Permissions.TASKS.START).mockReturnValue(false);
    when(ExtJS.checkPermission).calledWith(Permissions.TASKS.STOP).mockReturnValue(false);

    await renderAndWaitForLoad(testId);

    expect(deleteButton()).toHaveClass('disabled');
    expect(runButton()).toHaveClass('disabled');
    expect(stopButton()).toHaveClass('disabled');

    await TestUtils.expectToSeeTooltipOnHover(deleteButton(), UIStrings.PERMISSION_ERROR);
    await TestUtils.expectToSeeTooltipOnHover(runButton(), UIStrings.PERMISSION_ERROR);
    await TestUtils.expectToSeeTooltipOnHover(stopButton(), UIStrings.PERMISSION_ERROR);
  });

  it('fires onDone when cancelled', async function() {
    const {cancelButton} = selectors;

    await renderAndWaitForLoad(testId);

    userEvent.click(cancelButton());

    await waitFor(() => expect(onDone).toBeCalled());
  });

  it('uses proper urls', function() {
    expect(runTaskUrl(testId)).toBe('service/rest/v1/tasks/d3275dbe-c784-47f1-8ea2-338088f7ceab/run');
    expect(stopTaskUrl(testId)).toBe('service/rest/v1/tasks/d3275dbe-c784-47f1-8ea2-338088f7ceab/stop');
  });
});
