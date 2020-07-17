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
import {fireEvent, wait, waitForElement, waitForElementToBeRemoved} from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';
import TestUtils from 'nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import axios from 'axios';
import {ExtJS} from 'nexus-ui-plugin';

import LoggingConfigurationForm from './LoggingConfigurationForm';

import UIStrings from "../../../../constants/UIStrings";

jest.mock('axios', () => ({
  ...jest.requireActual('axios'), // Use most functions from actual axios
  get: jest.fn(),
  put: jest.fn(),
  post: jest.fn()
}));

jest.mock('nexus-ui-plugin', () => ({
  ...jest.requireActual('nexus-ui-plugin'),
  ExtJS: {
    requestConfirmation: jest.fn()
  },
  Utils: {
    buildFormMachine: function(args) {
      const machine = jest.requireActual('nexus-ui-plugin').Utils.buildFormMachine(args);
      return machine.withConfig({
        actions: {
          logSaveSuccess: jest.fn(),
          logLoadError: jest.fn()
        }
      })
    },
    isInvalid: jest.requireActual('nexus-ui-plugin').Utils.isInvalid,
    isBlank: jest.requireActual('nexus-ui-plugin').Utils.isBlank,
    notBlank: jest.requireActual('nexus-ui-plugin').Utils.notBlank
  }
}));

describe('LoggingConfigurationForm', function() {
  const CONFIRM = Promise.resolve();
  const OVERRIDDEN_LOGGER = Promise.resolve({data: {override: true}});
  const onDone = jest.fn();

  function renderEditView() {
    return renderView(<LoggingConfigurationForm itemId="ROOT" onDone={onDone}/>);
  }

  function renderCreateView() {
    return renderView(<LoggingConfigurationForm onDone={onDone} />);
  }

  function renderView(view) {
    return TestUtils.render(view, ({queryByLabelText, queryByText}) => ({
      name: () => queryByLabelText(UIStrings.LOGGING.NAME_LABEL),
      level: () => queryByLabelText(UIStrings.LOGGING.LEVEL_LABEL),
      saveButton: () => queryByText(UIStrings.SETTINGS.SAVE_BUTTON_LABEL),
      cancelButton: () => queryByText(UIStrings.SETTINGS.CANCEL_BUTTON_LABEL),
      resetMask: () => queryByText(UIStrings.LOGGING.MESSAGES.RESETTING),
      resetButton: () => queryByText(UIStrings.LOGGING.RESET_BUTTON)
    }));
  }

  const changeFieldAndAssertValue = async (fieldSelector, value) => {
    fireEvent.change(fieldSelector(), {target: {value: value}});
    await wait(() => expect(fieldSelector()).toHaveValue(value));
  };

  it('renders a loading spinner', async function() {
    axios.get.mockReturnValue(new Promise(() => {}));

    const {container, loadingMask} = renderEditView();

    await waitForElement(loadingMask);

    expect(container).toMatchSnapshot();
  });

  it('renders the resolved data', async function() {
    axios.get.mockResolvedValue({
      data: {name: 'ROOT', level: 'INFO'}
    });

    const {container, loadingMask, name, level, saveButton} = renderEditView();

    await waitForElementToBeRemoved(loadingMask);

    expect(name()).toHaveValue('ROOT');
    expect(level()).toHaveValue('INFO');
    expect(saveButton()).toBeDisabled();
    expect(container).toMatchSnapshot();
  });

  it('renders an error message', async function() {
    axios.get.mockImplementation(() => new Promise((resolve, reject) => {
      setTimeout(() => reject({message: 'Error'}), 100);
    }));

    const {container, loadingMask} = renderEditView();

    await waitForElementToBeRemoved(loadingMask);

    expect(container.querySelector('.nx-alert--error')).toHaveTextContent('Error');
  });

  it('requires the name field when creating a new logging configuration', async function() {
    const {container, loadingMask, name, level, saveButton} = renderCreateView();

    await waitForElementToBeRemoved(loadingMask);

    expect(container).toMatchSnapshot();

    expect(saveButton()).toBeDisabled();
    expect(level()).toHaveValue('INFO');

    await changeFieldAndAssertValue(name, 'name');

    expect(saveButton()).toBeEnabled();
  });

  it('fires onDone when cancelled', async function() {
    const {loadingMask, cancelButton} = renderCreateView();

    await waitForElementToBeRemoved(loadingMask);

    fireEvent.click(cancelButton());

    await wait(() => expect(onDone).toBeCalled());
  });

  it('requests confirmation when the logger is overridden and saves when requested', async function() {
    const {loadingMask, name, level, saveButton} = renderCreateView();

    await waitForElementToBeRemoved(loadingMask);

    axios.put.mockReturnValue(Promise.resolve());

    await changeFieldAndAssertValue(name, 'name');
    await changeFieldAndAssertValue(level, 'DEBUG');
    expect(saveButton()).toBeEnabled();

    axios.get.mockReturnValue(OVERRIDDEN_LOGGER);
    ExtJS.requestConfirmation.mockReturnValue(CONFIRM);
    fireEvent.click(saveButton());

    await wait(() => expect(axios.put).toBeCalledWith(
        '/service/rest/internal/ui/loggingConfiguration/name',
        {name: 'name', level: 'DEBUG'}
    ));
    expect(onDone).toBeCalled();
  });

  it('saves', async function() {
    axios.get.mockReturnValue(Promise.resolve({
      data: {name: 'ROOT', level: 'INFO'}
    }));

    const {loadingMask, level, saveButton} = renderEditView();

    await waitForElementToBeRemoved(loadingMask);

    await wait(() => expect(window.dirty).toEqual([]));

    await changeFieldAndAssertValue(level, 'DEBUG');

    await wait(() => expect(window.dirty).toEqual(['LoggingConfigurationFormMachine']));

    fireEvent.click(saveButton());

    await wait(() => expect(axios.put).toHaveBeenLastCalledWith(
        '/service/rest/internal/ui/loggingConfiguration/ROOT',
        {name: 'ROOT', level: 'DEBUG'}
    ));
    expect(window.dirty).toEqual([]);
  });

  it('resets an edited logger', async function() {
    axios.get.mockReturnValue(Promise.resolve({
      data: {name: 'test', level: 'ERROR'}
    }));

    const {loadingMask, resetButton, resetMask} = renderEditView();

    await waitForElementToBeRemoved(loadingMask);

    ExtJS.requestConfirmation.mockReturnValue(CONFIRM);
    axios.post.mockReturnValue(CONFIRM);
    fireEvent.click(resetButton());

    await wait(() => expect(resetMask()).toBeInTheDocument());

    expect(axios.post).toBeCalledWith('/service/rest/internal/ui/loggingConfiguration/test/reset');
    expect(onDone).toBeCalled();
  });
});
