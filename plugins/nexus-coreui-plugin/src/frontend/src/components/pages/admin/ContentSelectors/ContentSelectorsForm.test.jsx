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
import {screen, waitFor, render, waitForElementToBeRemoved} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {when} from 'jest-when';
import {act} from 'react-dom/test-utils';
import axios from 'axios';
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import ContentSelectorsDetails from './ContentSelectorsDetails';

import UIStrings from '../../../../constants/UIStrings';

jest.mock('axios', () => ({
  ...jest.requireActual('axios'), // Use most functions from actual axios
  get: jest.fn(),
  put: jest.fn(),
  post: jest.fn(),
  delete: jest.fn()
}));

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    requestConfirmation: jest.fn(),
    showErrorMessage: jest.fn(),
    checkPermission: jest.fn(),
  },
  Utils: {
    buildFormMachine: function(args) {
      const machine = jest.requireActual('@sonatype/nexus-ui-plugin').Utils.buildFormMachine(args);
      return machine.withConfig({
        actions: {
          logSaveSuccess: jest.fn(),
          logSaveError: jest.fn(),
          logLoadError: jest.fn()
        }
      })
    },
    isInvalid: jest.requireActual('@sonatype/nexus-ui-plugin').Utils.isInvalid,
    isBlank: jest.requireActual('@sonatype/nexus-ui-plugin').Utils.isBlank,
    isName: jest.requireActual('@sonatype/nexus-ui-plugin').Utils.isName,
    notBlank: jest.requireActual('@sonatype/nexus-ui-plugin').Utils.notBlank,
    fieldProps: jest.requireActual('@sonatype/nexus-ui-plugin').Utils.fieldProps,
    saveTooltip: jest.requireActual('@sonatype/nexus-ui-plugin').Utils.saveTooltip
  }
}));

const selectors = {
  ...TestUtils.selectors,
  ...TestUtils.formSelectors,

  getType: () => screen.getByText(UIStrings.CONTENT_SELECTORS.TYPE_LABEL).nextSibling,
  name: () => screen.queryByLabelText(UIStrings.CONTENT_SELECTORS.NAME_LABEL),
  description: () => screen.queryByLabelText(UIStrings.CONTENT_SELECTORS.DESCRIPTION_LABEL),
  expression: () => screen.queryByLabelText(UIStrings.CONTENT_SELECTORS.EXPRESSION_LABEL),
  cancelButton: () => screen.queryByText(UIStrings.SETTINGS.CANCEL_BUTTON_LABEL),
  deleteButton: () => screen.queryByText(UIStrings.SETTINGS.DELETE_BUTTON_LABEL),
  previewButton: () => screen.queryByText(UIStrings.CONTENT_SELECTORS.PREVIEW.BUTTON),
  readOnly: {
    name: () => screen.getByText(UIStrings.CONTENT_SELECTORS.NAME_LABEL).nextSibling,
    type: () => screen.getByText(UIStrings.CONTENT_SELECTORS.TYPE_LABEL).nextSibling,
    description: () => screen.getByText(UIStrings.CONTENT_SELECTORS.DESCRIPTION_LABEL).nextSibling,
    expression: () => screen.getByText(UIStrings.CONTENT_SELECTORS.EXPRESSION_LABEL).nextSibling,
    warning: () => screen.getByText(UIStrings.SETTINGS.READ_ONLY.WARNING)
  },
};

describe('ContentSelectorsForm', function() {
  const CONFIRM = Promise.resolve();
  const onDone = jest.fn();

  function renderEditView(itemId) {
    return render(<ContentSelectorsDetails itemId={itemId} onDone={onDone}/>);
  }

  const renderCreateView = async () => {
    return render(<ContentSelectorsDetails onDone={onDone}/>);
  }

  beforeEach(() => {
    axios.post.mockReset();

    when(ExtJS.checkPermission)
      .calledWith('nexus:selectors:update')
      .mockReturnValue(true);

    when(ExtJS.checkPermission)
    .calledWith('nexus:selectors:create')
    .mockReturnValue(true);

    when(ExtJS.checkPermission)
    .calledWith('nexus:selectors:delete')
    .mockReturnValue(true);
  });

  it('renders the resolved data', async function() {
    const itemId = 'test';

    axios.get.mockImplementation((url) => {
      if (url === `service/rest/v1/security/content-selectors/${itemId}`) {
        return Promise.resolve({
          data: {
            'name' : 'content-selector-name',
            'type' : 'csel',
            'description' : 'description',
            'expression' : 'format == "raw"'
          }
        });
      }
      else if (url === 'service/rest/internal/ui/repositories?withAll=true&withFormats=true') {
        return Promise.resolve({data: []});
      }
    });

    const {description, expression} = selectors;
    renderEditView(itemId);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(selectors.queryTitle()).toHaveTextContent('Edit content-selector-name');
    expect(selectors.getType()).toHaveTextContent('CSEL');
    expect(description()).toHaveValue('description');
    expect(expression()).toHaveValue('format == "raw"');

    userEvent.click(selectors.querySubmitButton());

    expect(selectors.queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();
  });

  it('renders an error message', async function() {
    axios.get.mockReturnValue(Promise.reject({message: 'Error'}));

    const {container} = renderEditView('itemId');

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(container.querySelector('.nx-alert--error')).toHaveTextContent('Error');
  });

  it('requires the name and expression fields when creating a new content selector', async function() {
    axios.get.mockReturnValue(Promise.resolve({data: []}));

    const {name, expression} = selectors;
    renderCreateView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.click(selectors.querySubmitButton());
    expect(selectors.queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();

    await TestUtils.changeField(name, '');
    await TestUtils.changeField(expression, 'format == "raw"');

    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

    await TestUtils.changeField(name, 'name');
    await TestUtils.changeField(expression, '');
    userEvent.click(selectors.querySubmitButton());

    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

    await TestUtils.changeField(name, 'name');
    await TestUtils.changeField(expression, 'format == "raw"');

    expect(selectors.queryFormError()).not.toBeInTheDocument();
  });

  it('fires onDone when cancelled', async function() {
    const {cancelButton} = selectors;
    renderCreateView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.click(cancelButton());

    await waitFor(() => expect(onDone).toBeCalled());
  });

  it('shows errors from the backend on the correct field', async function() {
    when(axios.post).calledWith('service/rest/v1/security/content-selectors', expect.anything()).mockRejectedValue({
      response: {
        data: [
          {
            "id": "PARAMETER name",
            "message": "An error occurred with the name field"
          },
          {
            "id": "HelperBean.expression",
            "message": "Invalid CSEL: tokenization error in '\"maven2' at line 1 column 18"
          }
        ]
      }
    });

    const {expression, name} = selectors;
    renderCreateView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    await TestUtils.changeField(name, 'test');
    await TestUtils.changeField(expression, 'format == "maven2');

    userEvent.click(selectors.querySubmitButton());

    await waitForElementToBeRemoved(selectors.querySavingMask());
    expect(screen.queryByText('An error occurred with the name field')).toBeInTheDocument();
    expect(screen.queryByText('Invalid CSEL: tokenization error in \'"maven2\' at line 1 column 18')).toBeInTheDocument();
  });

  it('allows new line for expression textarea', async function() {
    const {expression} = selectors;
    renderCreateView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.type(expression(), 'format == "raw"{enter}and format == "maven2"')

    expect(expression()).toHaveValue('format == "raw"\nand format == "maven2"');
  });

  it('requests confirmation when delete is requested', async function() {
    const itemId = 'test';
    axios.get.mockImplementation((url) => {
      if (url === `service/rest/v1/security/content-selectors/${itemId}`) {
        return Promise.resolve({
          data: {
            'name' : itemId,
            'type' : 'csel',
            'description' : 'description',
            'expression' : 'format == "raw"'
          }
        });
      }
      else if (url === 'service/rest/internal/ui/repositories?withAll=true&withFormats=true') {
        return Promise.resolve({data: []});
      }
    });

    axios.delete.mockReturnValue(Promise.resolve());

    const {deleteButton} = selectors;
    renderEditView(itemId);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    axios.put.mockReturnValue(Promise.resolve());

    ExtJS.requestConfirmation.mockReturnValue(CONFIRM);
    userEvent.click(deleteButton());

    expect(selectors.queryFormError()).not.toBeInTheDocument();
    await waitFor(() => expect(axios.delete).toBeCalledWith(`service/rest/v1/security/content-selectors/${itemId}`));
    expect(onDone).toBeCalled();
  });

  it('loads xss and escapes the values', () => {
    axios.get.mockImplementation((url) => {
      if (url === `service/rest/v1/security/content-selectors/${itemId}`) {
        return Promise.resolve({
          data: {
            'name' : itemId,
            'type' : 'csel',
            'description' : 'description',
            'expression' : 'format == "raw"'
          }
        });
      }
      else if (url === 'service/rest/internal/ui/repositories?withAll=true&withFormats=true') {
        return Promise.resolve({data: []});
      }
    });
  });

  it('saves', async function() {
    axios.get.mockReturnValue(Promise.resolve({data: []}));
    axios.post.mockReturnValue(Promise.resolve());

    const {name, description, expression} = selectors;
    renderCreateView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    await waitFor(() => expect(window.dirty).toEqual([]));

    await TestUtils.changeField(name, 'test');
    await TestUtils.changeField(description, 'description');
    await TestUtils.changeField(expression, 'format == "raw"');

    await waitFor(() => expect(window.dirty).toEqual(['ContentSelectorsFormMachine']));

    expect(selectors.querySubmitButton()).not.toBeDisabled();
    userEvent.click(selectors.querySubmitButton());

    await waitForElementToBeRemoved(selectors.querySavingMask());

    expect(axios.post).toHaveBeenCalledWith(
        'service/rest/v1/security/content-selectors',
        {name: 'test', description: 'description', expression: 'format == "raw"'}
    );
    expect(window.dirty).toEqual([]);
  });

  describe('ContentSelectorsPreview', function() {
    it('shows previews', async function () {
      const emptyPreviewMessage = UIStrings.CONTENT_SELECTORS.PREVIEW.EMPTY;

      const itemId = 'test';
      axios.get.mockImplementation((url) => {
        if (url === `service/rest/v1/security/content-selectors/${itemId}`) {
          return Promise.resolve({
            data: {
              'name' : itemId,
              'type' : 'csel',
              'description' : 'description',
              'expression' : 'format == "raw"'
            }
          });
        }
        else if (url === 'service/rest/internal/ui/repositories?withAll=true&withFormats=true') {
          return Promise.resolve({data: [
            {id: "*", name: "(All Repositories)"}
          ]});
        }
      });

      axios.post.mockResolvedValue({data: {
        total: 1,
        results: [{
          id: null,
          repository: 'maven-central',
          format: 'maven2',
          group: 'org.apache.maven',
          name: 'maven-aether-provider',
          version: '3.0',
          assets: null
        }]
      }});

      const {previewButton} = selectors;
      renderEditView(itemId);

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      expect(screen.queryByText(emptyPreviewMessage)).toBeInTheDocument();

      await act(async () => userEvent.click(previewButton()));

      expect(screen.queryByText('maven-aether-provider')).toBeInTheDocument();
    });

    it('shows preview error API message', async function () {
      const itemId = 'test';
      axios.get.mockImplementation((url) => {
        if (url === `service/rest/v1/security/content-selectors/${itemId}`) {
          return Promise.resolve({
            data: {
              'name' : itemId,
              'type' : 'csel',
              'description' : 'description',
              'expression' : 'format == "raw"'
            }
          });
        }
        else if (url === 'service/rest/internal/ui/repositories?withAll=true&withFormats=true') {
          return Promise.resolve({data: [
            {id: "*", name: "(All Repositories)"}
          ]});
        }
      });

      axios.post.mockRejectedValue({
        response: {
          data: [
            {
              id: 'HelperBean.expression',
              message: 'Invalid CSEL'
            }
          ]
        }
      });

      const {previewButton} = selectors;
      renderEditView(itemId);

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      await act(async () => userEvent.click(previewButton()));

      expect(screen.queryByText('An error occurred loading data. Invalid CSEL')).toBeInTheDocument();
    });
  });

  describe('Read Only Mode', function() {
    it('renders content selector without edit permissions', async function() {

      when(ExtJS.checkPermission)
      .calledWith('nexus:selectors:update')
      .mockReturnValue(false);

      axios.get.mockImplementation((url) => {
        if (url === 'service/rest/v1/security/content-selectors/itemId') {
          return Promise.resolve({
            data: {
              'name' : 'test',
              'type' : 'csel',
              'description' : 'test content selector with format raw',
              'expression' : 'format == "raw"'
            }
          });
        }
      });

      const {readOnly: {name, type, description, expression, warning}} = selectors;

      await act(async () => {
        renderEditView('itemId');
      })

      expect(warning()).toBeInTheDocument();

      expect(name()).toHaveTextContent('test');
      expect(type()).toHaveTextContent('CSEL');
      expect(description()).toHaveTextContent('test content selector with format raw');
      expect(expression()).toHaveTextContent('format == "raw"');
    });
  });
});
