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
import axios from 'axios';
import {fireEvent, waitFor, waitForElementToBeRemoved} from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import {ExtJS, APIConstants} from '@sonatype/nexus-ui-plugin';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import SamlConfiguration from './SamlConfiguration';
import UIStrings from '../../../../constants/UIStrings';

const SAML_API_URL = APIConstants.REST.INTERNAL.SAML;

jest.mock('axios', () => ({
  get: jest.fn(),
  put: jest.fn()
}));

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    showSuccessMessage: jest.fn(),
    showErrorMessage: jest.fn()
  }
}));

const DEFAULT_SAML_CONFIGURATION = {
  idpMetadata: "",
  entityIdUri: "",
  validateResponseSignature: 'default',
  validateAssertionSignature: 'default',
  usernameAttr: "username",
  firstNameAttr: "",
  lastNameAttr: "",
  emailAttr: "",
  roleAttr: ""
};

const PENDING_REQUEST = () => new Promise(jest.fn());
const DEFAULT_RESPONSE = {data: DEFAULT_SAML_CONFIGURATION};
const ERROR_RESPONSE = {response: {data: [{"id": "FIELD idpMetadata", "message": "must be valid XML"}]}};

const selectors = {
  ...TestUtils.selectors,
  ...TestUtils.formSelectors
};

describe('SamlConfiguration', () => {
  beforeEach(() => {
    window.dirty = [];
  });

  afterEach(() => {
    window.dirty = [];
  });

  function render() {
    const FIELD_LABELS = UIStrings.SAML_CONFIGURATION.FIELDS;

    return TestUtils.render(<SamlConfiguration/>, ({getByLabelText, getByRole, getByText}) => ({
      errorMessage: () => getByRole('alert'),
      idpMetadataField: () => getByLabelText(FIELD_LABELS.idpMetadataLabel),
      entityIdUriField: () => getByLabelText(FIELD_LABELS.entityIdUriLabel),
      entityIdUriValidationError: () => getByText(UIStrings.SAML_CONFIGURATION.FIELDS.entityIdUriValidationError),
      validateResponseSignatureField: () => getByLabelText(FIELD_LABELS.validateResponseSignatureLabel),
      validateAssertionSignatureField: () => getByLabelText(FIELD_LABELS.validateAssertionSignatureLabel),
      usernameField: () => getByLabelText(FIELD_LABELS.usernameAttrLabel),
      firstnameField: () => getByLabelText(FIELD_LABELS.firstNameAttrLabel),
      lastnameField: () => getByLabelText(FIELD_LABELS.lastNameAttrLabel),
      emailField: () => getByLabelText(FIELD_LABELS.emailAttrLabel),
      rolesField: () => getByLabelText(FIELD_LABELS.roleAttrLabel),
      discardButton: () => getByText(UIStrings.SETTINGS.DISCARD_BUTTON_LABEL),
      validationError: (error) => getByText(error)
    }));
  }

  it('renders the form with default settings', async () => {
    axios.get.mockResolvedValue(DEFAULT_RESPONSE);

    const {loadingMask, discardButton} = render();

    await waitForElementToBeRemoved(loadingMask);

    expect(discardButton()).toHaveClass('disabled');
  });

  it('shows load error if the load failed', async () => {
    const message = 'Server Error';
    axios.get.mockRejectedValue({message});

    render();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(selectors.queryLoadError()).toBeInTheDocument();
  });

  it('does not save when the save button is disabled', async () => {
    axios.get.mockResolvedValue(DEFAULT_RESPONSE);

    render();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.click(selectors.querySubmitButton());
    expect(selectors.queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();
  });

  it('does not save when the form is pristine', async () => {
    axios.get.mockResolvedValue({
      data: {
        ...DEFAULT_SAML_CONFIGURATION,
        idpMetadata: '<xml></xml>',
        entityIdUri: 'http://localhost'
      }
    });

    render();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.click(selectors.querySubmitButton());
    expect(selectors.queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();
  });

  it('enables the discard button when there are changes', async () => {
    axios.get.mockResolvedValue(DEFAULT_RESPONSE);

    const {
      loadingMask,
      idpMetadataField,
      entityIdUriField,
      validateAssertionSignatureField,
      validateResponseSignatureField,
      usernameField,
      firstnameField,
      lastnameField,
      emailField,
      rolesField,
      discardButton
    } = render();

    const discardChanges = async () => {
      userEvent.click(discardButton());
      await waitFor(() => expect(discardButton()).toHaveClass('disabled'));
    };

    await waitForElementToBeRemoved(loadingMask);

    await TestUtils.changeField(idpMetadataField, '<xml></xml>');
    await discardChanges();
    await waitFor(() => expect(idpMetadataField()).toHaveValue(''));

    await TestUtils.changeField(entityIdUriField, 'http://example.com');
    await discardChanges();
    await waitFor(() => expect(entityIdUriField()).toHaveValue(''));

    await TestUtils.changeField(validateAssertionSignatureField, 'true');
    await discardChanges();
    await waitFor(() => expect(validateAssertionSignatureField()).toHaveValue('default'));

    await TestUtils.changeField(validateResponseSignatureField, 'false');
    await discardChanges();
    await waitFor(() => expect(validateResponseSignatureField()).toHaveValue('default'));

    await TestUtils.changeField(usernameField, 'test');
    await discardChanges();
    await waitFor(() => expect(usernameField()).toHaveValue('username'));

    await TestUtils.changeField(firstnameField, 'test');
    await discardChanges();
    await waitFor(() => expect(firstnameField()).toHaveValue(''));

    await TestUtils.changeField(lastnameField, 'test');
    await discardChanges();
    await waitFor(() => expect(lastnameField()).toHaveValue(''));

    await TestUtils.changeField(emailField, 'test');
    await discardChanges();
    await waitFor(() => expect(emailField()).toHaveValue(''));

    await TestUtils.changeField(rolesField, 'test');
    await discardChanges();
    await waitFor(() => expect(rolesField()).toHaveValue(''));
  });

  it('tells the user to use a URI if the entity id field appears not to be', async () => {
    axios.get.mockResolvedValue(DEFAULT_RESPONSE);

    const {
      loadingMask,
      entityIdUriField,
      entityIdUriValidationError
    } = render();

    await waitForElementToBeRemoved(loadingMask);

    fireEvent.change(entityIdUriField(), {target: {value: 'test'}});

    await waitFor(() => expect(entityIdUriValidationError()).toBeInTheDocument());
  });

  it('tells the user the IDP Metadata is required', async () => {
    axios.get.mockResolvedValue(DEFAULT_RESPONSE);

    const {loadingMask, idpMetadataField, container} = render();

    await waitForElementToBeRemoved(loadingMask);

    await TestUtils.changeField(idpMetadataField, '');
    userEvent.click(selectors.querySubmitButton());
    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE));
  });

  it('enables the save button when there are valid changes', async () => {
    axios.get.mockResolvedValue(DEFAULT_RESPONSE);

    const {
      loadingMask,
      idpMetadataField,
      entityIdUriField,
      usernameField
    } = render();

    await waitForElementToBeRemoved(loadingMask);

    userEvent.click(selectors.querySubmitButton());

    await TestUtils.changeField(idpMetadataField, '<xml></xml>');
    await TestUtils.changeField(entityIdUriField, 'http://example.com');
    expect(selectors.queryFormError()).not.toBeInTheDocument();

    await TestUtils.changeField(idpMetadataField, '');
    userEvent.click(selectors.querySubmitButton());
    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();
    await TestUtils.changeField(idpMetadataField, '<xml></xml>');
    expect(selectors.queryFormError()).not.toBeInTheDocument();

    await TestUtils.changeField(entityIdUriField, '');
    userEvent.click(selectors.querySubmitButton());
    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();
    await TestUtils.changeField(entityIdUriField, 'http://example.com');
    expect(selectors.queryFormError()).not.toBeInTheDocument();

    await TestUtils.changeField(usernameField, '');
    userEvent.click(selectors.querySubmitButton());
    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();
  });

  it('saves changes successfully', async () => {
    axios.get.mockResolvedValueOnce(DEFAULT_RESPONSE)

    const {
      loadingMask,
      idpMetadataField,
      entityIdUriField,
      validateResponseSignatureField,
      validateAssertionSignatureField,
      usernameField,
      firstnameField,
      lastnameField,
      emailField,
      rolesField,
    } = render();

    await waitForElementToBeRemoved(loadingMask);

    await TestUtils.changeField(idpMetadataField, '<xml></xml>');
    await TestUtils.changeField(entityIdUriField, 'http://example.com');
    await TestUtils.changeField(validateResponseSignatureField, 'false');
    await TestUtils.changeField(validateAssertionSignatureField, 'true');
    await TestUtils.changeField(usernameField, 'userId')
    await TestUtils.changeField(firstnameField, 'firstName');
    await TestUtils.changeField(lastnameField, 'lastName');
    await TestUtils.changeField(emailField, 'email');
    await TestUtils.changeField(rolesField, 'groups');

    const updatedConfiguration = {
      idpMetadata: '<xml></xml>',
      entityIdUri: 'http://example.com',
      validateResponseSignature: 'false',
      validateAssertionSignature: 'true',
      usernameAttr: 'userId',
      firstNameAttr: 'firstName',
      lastNameAttr: 'lastName',
      emailAttr: 'email',
      roleAttr: 'groups'
    };

    axios.get.mockResolvedValue({data: updatedConfiguration});
    axios.put.mockResolvedValue({data: updatedConfiguration});

    userEvent.click(selectors.querySubmitButton());
    await waitForElementToBeRemoved(selectors.querySavingMask());

    expect(axios.put).toBeCalledWith(SAML_API_URL, updatedConfiguration);
    expect(ExtJS.showSuccessMessage).toHaveBeenCalledWith(UIStrings.SAML_CONFIGURATION.MESSAGES.SAVE_SUCCESS);
    expect(idpMetadataField()).toHaveValue(updatedConfiguration.idpMetadata);
    expect(entityIdUriField()).toHaveValue(updatedConfiguration.entityIdUri);
    expect(validateResponseSignatureField()).toHaveValue(updatedConfiguration.validateResponseSignature);
    expect(validateAssertionSignatureField()).toHaveValue(updatedConfiguration.validateAssertionSignature);
    expect(usernameField()).toHaveValue(updatedConfiguration.usernameAttr);
    expect(firstnameField()).toHaveValue(updatedConfiguration.firstNameAttr);
    expect(lastnameField()).toHaveValue(updatedConfiguration.lastNameAttr);
    expect(emailField()).toHaveValue(updatedConfiguration.emailAttr);
    expect(rolesField()).toHaveValue(updatedConfiguration.roleAttr);
  });

  it('shows a save error if the save failed', async () => {
    axios.get.mockResolvedValue(DEFAULT_RESPONSE);
    axios.put.mockRejectedValue(ERROR_RESPONSE);

    const {
      loadingMask,
      idpMetadataField,
      entityIdUriField,
      usernameField,
      validationError
    } = render();

    await waitForElementToBeRemoved(loadingMask);

    await TestUtils.changeField(idpMetadataField, '<xml>');
    await TestUtils.changeField(entityIdUriField, 'http://example.com');
    await TestUtils.changeField(usernameField, 'username');

    userEvent.click(selectors.querySubmitButton());
    await waitForElementToBeRemoved(selectors.querySavingMask());

    expect(validationError(ERROR_RESPONSE.response.data[0].message)).toBeInTheDocument();
  });

  it('sets the dirty status appropriately', async () => {
    axios.get.mockResolvedValue(DEFAULT_RESPONSE);

    const {
      loadingMask,
      idpMetadataField
    } = render();

    await waitForElementToBeRemoved(loadingMask);

    expect(window.dirty).toEqual([]);

    await TestUtils.changeField(idpMetadataField, '<xml>');

    expect(window.dirty).toEqual(['SamlConfigurationForm']);
  });

  it('trims LdP fields values before saving', async () => {
    axios.get.mockResolvedValueOnce(DEFAULT_RESPONSE)

    const {
      loadingMask,
      idpMetadataField,
      entityIdUriField,
      validateResponseSignatureField,
      validateAssertionSignatureField,
      usernameField,
      firstnameField,
      lastnameField,
      emailField,
      rolesField,
    } = render();

    const conf = {
      idpMetadata: '<xml></xml>',
      entityIdUri: 'http://example.com',
      validateResponseSignature: 'false',
      validateAssertionSignature: 'true',
      usernameAttr: 'userId',
      firstNameAttr: 'firstName',
      lastNameAttr: 'lastName',
      emailAttr: 'email',
      roleAttr: 'groups'
    };

    await waitForElementToBeRemoved(loadingMask);

    await TestUtils.changeField(idpMetadataField, conf.idpMetadata);
    await TestUtils.changeField(entityIdUriField, conf.entityIdUri);
    await TestUtils.changeField(validateResponseSignatureField, conf.validateResponseSignature);
    await TestUtils.changeField(validateAssertionSignatureField, conf.validateAssertionSignature);
    await TestUtils.changeField(usernameField, '  ' + conf.usernameAttr + '  ');
    await TestUtils.changeField(firstnameField, '  ' + conf.firstNameAttr + '  ');
    await TestUtils.changeField(lastnameField, '  ' + conf.lastNameAttr + '  ');
    await TestUtils.changeField(emailField, '  ' + conf.emailAttr + '  ');
    await TestUtils.changeField(rolesField, '  ' + conf.roleAttr + '  ');

    userEvent.click(selectors.querySubmitButton());
    await waitForElementToBeRemoved(selectors.querySavingMask());

    expect(axios.put).toBeCalledWith(SAML_API_URL, conf);
  });
});
