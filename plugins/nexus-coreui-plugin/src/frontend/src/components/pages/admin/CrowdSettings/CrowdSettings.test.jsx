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
import {interpret} from 'xstate';
import {fireEvent, render, waitFor, waitForElementToBeRemoved} from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import {ExtJS} from '@sonatype/nexus-ui-plugin';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import {CrowdSettingsForm} from './CrowdSettings';
import CrowdSettingsMachine from './CrowdSettingsMachine';
import UIStrings from '../../../../constants/UIStrings';

jest.mock('@sonatype/nexus-ui-plugin', () => {
  return {
    ...jest.requireActual('@sonatype/nexus-ui-plugin'),
    ExtJS: {
      showSuccessMessage: jest.fn(),
      showErrorMessage: jest.fn(),
      setDirtyStatus: jest.requireActual('@sonatype/nexus-ui-plugin').ExtJS.setDirtyStatus
    }
  }
});

const DEFAULT_CROWD_SETTINGS = {
  enabled: false,
  realmActive: false,
  applicationName: '',
  applicationPassword: '',
  url: '',
  useTrustStoreForUrl: false,
  timeout: ''
};

const PENDING_REQUEST = () => new Promise(jest.fn());
const DEFAULT_RESPONSE = () => Promise.resolve({data: DEFAULT_CROWD_SETTINGS});
const ERROR_RESPONSE = () => Promise.reject({
  response: {
    data: [{"id": "FIELD url", "message": "must not be empty"}]
  }
});

const selectors = {
  ...TestUtils.selectors,
  ...TestUtils.formSelectors
};

describe('CrowdSettings', () => {
  beforeEach(() => {
    window.dirty = [];
  });

  afterEach(() => {
    window.dirty = [];
  });

  function renderView(view) {
    const FIELD_LABELS = UIStrings.CROWD_SETTINGS.FIELDS;

    return TestUtils.render(view, ({getByLabelText, getByRole, getByText}) => ({
      errorMessage: () => getByRole('alert'),

      enabledField: () => getByLabelText(FIELD_LABELS.enabledDescription),
      realmActiveField: () => getByLabelText('To control ordering, go to Realms page.'),
      urlField: () => getByLabelText(FIELD_LABELS.urlLabel),
      useTrustStoreField: () => getByLabelText(FIELD_LABELS.useTrustStoreText),
      applicationNameField: () => getByLabelText(FIELD_LABELS.applicationNameLabel),
      applicationPasswordField: () => getByLabelText(FIELD_LABELS.applicationPasswordLabel),
      timeoutField: () => getByLabelText(FIELD_LABELS.timeoutLabel),
      nanError: () => getByText(UIStrings.ERROR.NAN),
      minError: (min) => getByText(UIStrings.ERROR.MIN(min)),

      discardButton: () => getByText(UIStrings.SETTINGS.DISCARD_BUTTON_LABEL),
      verifyButton: () => getByText(UIStrings.CROWD_SETTINGS.BUTTONS.VERIFY_BUTTON_LABEL),
      clearButton: () => getByText(UIStrings.CROWD_SETTINGS.BUTTONS.CLEAR_BUTTON_LABEL)
    }))
  }

  const changeFieldAndAssertValue = async (fieldSelector, value) => {
    if (fieldSelector().type === 'checkbox') {
      userEvent.click(fieldSelector());
      return await waitFor(() => value ? expect(fieldSelector()).toBeChecked() : expect(fieldSelector()).not.toBeChecked());
    }
    else {
      fireEvent.change(fieldSelector(), {target: {value: value}});
      return await waitFor(() => expect(fieldSelector()).toHaveValue(value));
    }
  };

  it('renders a loading form as expected', () => {
    const service = interpret(CrowdSettingsMachine.withConfig({
      services: {
        fetchData: PENDING_REQUEST
      }
    })).start();
    const {loadingMask} = renderView(<CrowdSettingsForm service={service}/>);

    expect(loadingMask()).toBeInTheDocument();
  });

  it('renders the form with default settings', async () => {
    const service = interpret(CrowdSettingsMachine.withConfig({
      services: {
        fetchData: DEFAULT_RESPONSE
      }
    })).start();

    const {
      container,
      loadingMask,
      discardButton,
      verifyButton,
      clearButton
    } = renderView(<CrowdSettingsForm service={service}/>);

    await waitFor(() => expect(service.state.value).toBe('loaded'));

    expect(container).not.toContainElement(loadingMask());
    expect(discardButton()).toHaveClass('disabled');
    expect(verifyButton()).toBeDisabled();
    expect(clearButton()).not.toHaveClass('disabled');
  });

  it('shows load error if the load failed', async () => {
    const service = interpret(CrowdSettingsMachine.withConfig({
      services: {
        fetchData: ERROR_RESPONSE
      }
    })).start();

    renderView(<CrowdSettingsForm service={service}/>);

    await waitFor(() => expect(service.state.value).toBe('loadError'));
    expect(ExtJS.showErrorMessage).toHaveBeenCalledWith(UIStrings.CROWD_SETTINGS.MESSAGES.LOAD_ERROR);
  });

  it('enables the discard button when there are changes', async () => {
    const service = interpret(CrowdSettingsMachine.withConfig({
      services: {
        fetchData: DEFAULT_RESPONSE
      }
    })).start();

    const {
      enabledField,
      realmActiveField,
      applicationNameField,
      applicationPasswordField,
      urlField,
      timeoutField,
      discardButton
    } = renderView(<CrowdSettingsForm service={service}/>);

    const discardChanges = async () => {
      await waitFor(() => expect(discardButton()).not.toHaveClass('disabled'));
      userEvent.click(discardButton());
      await waitFor(() => expect(discardButton()).toHaveClass('disabled'));
    };

    await waitFor(() => expect(service.state.value).toBe('loaded'));

    await changeFieldAndAssertValue(enabledField, true);
    await discardChanges();
    await waitFor(() => expect(enabledField()).not.toBeChecked());

    await changeFieldAndAssertValue(realmActiveField, true);
    await discardChanges();
    await waitFor(() => expect(realmActiveField()).not.toBeChecked());

    await changeFieldAndAssertValue(urlField, 'http://example.com');
    await discardChanges();
    await waitFor(() => expect(urlField()).toHaveValue(''));

    await changeFieldAndAssertValue(applicationNameField, 'nxrm');
    await discardChanges();
    await waitFor(() => expect(applicationNameField()).toHaveValue(''));

    await changeFieldAndAssertValue(applicationPasswordField, 'test');
    await discardChanges();
    await waitFor(() => expect(applicationPasswordField()).toHaveValue(''));

    await changeFieldAndAssertValue(timeoutField, '2');
    await discardChanges();
    await waitFor(() => expect(timeoutField()).toBeEmpty);
  });

  it('indicates a URL is invalid', async () => {
    const service = interpret(CrowdSettingsMachine.withConfig({
      services: {
        fetchData: DEFAULT_RESPONSE
      }
    })).start();

    const {getByText, getByLabelText} = render(<CrowdSettingsForm service={service}/>);

    await waitFor(() => expect(service.state.value).toBe('loaded'));

    fireEvent.change(getByLabelText(UIStrings.CROWD_SETTINGS.FIELDS.urlLabel), {target: {value: 'test'}});

    await waitFor(() => expect(getByText(UIStrings.CROWD_SETTINGS.FIELDS.urlValidationError)).toBeVisible());
  });

  it('enables the save button when there are valid changes', async () => {
    const service = interpret(CrowdSettingsMachine.withConfig({
      services: {
        fetchData: DEFAULT_RESPONSE
      }
    })).start();

    const {
      applicationNameField,
      applicationPasswordField,
      urlField
    } = renderView(<CrowdSettingsForm service={service}/>);

    await waitFor(() => expect(service.state.value).toBe('loaded'));

    await changeFieldAndAssertValue(urlField, 'https://example.com');
    await changeFieldAndAssertValue(applicationNameField, 'NXRM');
    await changeFieldAndAssertValue(applicationPasswordField, 'secret');

    await changeFieldAndAssertValue(urlField, 'example.com');

    userEvent.click(selectors.querySubmitButton());
    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

    await changeFieldAndAssertValue(urlField, 'https://example.com');
    expect(selectors.queryFormError()).not.toBeInTheDocument();

    await changeFieldAndAssertValue(applicationNameField, '');
    userEvent.click(selectors.querySubmitButton());
    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

    await changeFieldAndAssertValue(applicationNameField, 'NXRM');
    expect(selectors.queryFormError()).not.toBeInTheDocument();

    await changeFieldAndAssertValue(applicationPasswordField, '');
    userEvent.click(selectors.querySubmitButton());
    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();
    await changeFieldAndAssertValue(applicationPasswordField, 'secret');
    expect(selectors.queryFormError()).not.toBeInTheDocument();
  });

  it('saves changes successfully', async () => {
    var storedConfiguration = DEFAULT_CROWD_SETTINGS;
    const fetchMock = jest.fn();
    fetchMock.mockReturnValueOnce(Promise.resolve({data: storedConfiguration}));
    const service = interpret(CrowdSettingsMachine.withConfig({
      services: {
        fetchData: () => fetchMock,
        saveData: ({data}) => {
          expect(data).toStrictEqual(updatedConfiguration);
          storedConfiguration = data;
          return Promise.resolve();
        }
      }
    })).start();

    const {
      enabledField,
      realmActiveField,
      urlField,
      useTrustStoreField,
      applicationNameField,
      applicationPasswordField,
      timeoutField,
      discardButton
    } = renderView(<CrowdSettingsForm service={service}/>);

    await waitFor(() => expect(service.state.value).toBe('loaded'));

    await changeFieldAndAssertValue(enabledField, true);
    await changeFieldAndAssertValue(realmActiveField, true);
    await changeFieldAndAssertValue(urlField, 'https://example.net');
    await changeFieldAndAssertValue(useTrustStoreField, true);
    await changeFieldAndAssertValue(applicationNameField, 'nxrm');
    await changeFieldAndAssertValue(applicationPasswordField, 'secret');
    await changeFieldAndAssertValue(timeoutField, '4');

    const updatedConfiguration = {
      enabled: true,
      realmActive: true,
      applicationName: 'nxrm',
      applicationPassword: 'secret',
      url: 'https://example.net',
      useTrustStoreForUrl: true,
      timeout: '4'
    };

    const fetchedConfiguration = {
      ...updatedConfiguration,
      applicationPassword: '#~NXRM~PLACEHOLDER~PASSWORD~#'
    };

    fetchMock.mockReturnValueOnce(Promise.resolve({data: fetchedConfiguration}));

    userEvent.click(selectors.querySubmitButton());
    await waitForElementToBeRemoved(selectors.querySavingMask());

    expect(ExtJS.showSuccessMessage).toHaveBeenCalledWith(UIStrings.CROWD_SETTINGS.MESSAGES.SAVE_SUCCESS);
    expect(discardButton()).toHaveClass('disabled');
    expect(enabledField()).toBeChecked();
    expect(realmActiveField()).toBeChecked();
    expect(urlField()).toHaveValue('https://example.net');
    expect(useTrustStoreField()).toBeChecked();
    expect(applicationNameField()).toHaveValue('nxrm');
    expect(applicationPasswordField()).toHaveValue('#~NXRM~PLACEHOLDER~PASSWORD~#');
    expect(timeoutField()).toHaveValue('4');
  });

  it('shows a save error if the save failed', async () => {
    const service = interpret(CrowdSettingsMachine.withConfig({
      services: {
        fetchData: DEFAULT_RESPONSE,
        saveData: ERROR_RESPONSE
      }
    })).start();

    const {
      urlField,
      applicationNameField,
      applicationPasswordField,
    } = renderView(<CrowdSettingsForm service={service}/>);

    await waitFor(() => expect(service.state.value).toBe('loaded'));

    await changeFieldAndAssertValue(urlField, 'http://example.net');
    await changeFieldAndAssertValue(applicationNameField, 'nxrm');
    await changeFieldAndAssertValue(applicationPasswordField, 'secret');

    userEvent.click(selectors.querySubmitButton());

    await waitFor(() => expect(ExtJS.showErrorMessage).toHaveBeenCalledWith(UIStrings.CROWD_SETTINGS.MESSAGES.SAVE_ERROR));
  });

  it('sets the dirty status appropriately', async () => {
    const service = interpret(CrowdSettingsMachine.withConfig({
      services: {
        fetchData: DEFAULT_RESPONSE,
        saveData: ERROR_RESPONSE
      }
    })).start();

    const {urlField} = renderView(<CrowdSettingsForm service={service}/>);

    await waitFor(() => expect(service.state.value).toBe('loaded'));

    expect(window.dirty).toEqual([]);

    await changeFieldAndAssertValue(urlField, 'http://example.net');

    expect(window.dirty).toEqual(['CrowdSettingsForm']);
  });

  it('prevents invalid timeout values', async () => {
    const service = interpret(CrowdSettingsMachine.withConfig({
      services: {
        fetchData: DEFAULT_RESPONSE,
        saveData: ERROR_RESPONSE
      }
    })).start();

    const {timeoutField, nanError, minError} = renderView(<CrowdSettingsForm service={service}/>);

    await waitFor(() => expect(service.state.value).toBe('loaded'));

    expect(timeoutField()).toHaveValue('');

    await changeFieldAndAssertValue(timeoutField, '1');

    await changeFieldAndAssertValue(timeoutField, '');

    await changeFieldAndAssertValue(timeoutField, '-1');
    expect(minError(1)).toBeInTheDocument();

    await changeFieldAndAssertValue(timeoutField, 'a');
    expect(nanError()).toBeInTheDocument();
  });
});
