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

import CleanupPoliciesForm from './CleanupPoliciesForm';

import UIStrings from '../../../../constants/UIStrings';

jest.mock('axios', () => ({
  ...jest.requireActual('axios'), // Use most functions from actual axios
  get: jest.fn(),
  put: jest.fn(),
  post: jest.fn(),
  delete: jest.fn()
}));

jest.mock('nexus-ui-plugin', () => ({
  ...jest.requireActual('nexus-ui-plugin'),
  ExtJS: {
    requestConfirmation: jest.fn(),
    showErrorMessage: jest.fn()
  },
  Utils: {
    buildFormMachine: function(args) {
      const machine = jest.requireActual('nexus-ui-plugin').Utils.buildFormMachine(args);
      return machine.withConfig({
        actions: {
          logSaveSuccess: jest.fn(),
          logSaveError: jest.fn(),
          logLoadError: jest.fn()
        }
      })
    },
    buildListMachine: function(args) {
      return jest.requireActual('nexus-ui-plugin').Utils.buildListMachine(args);
    },
    isInvalid: jest.requireActual('nexus-ui-plugin').Utils.isInvalid,
    isBlank: jest.requireActual('nexus-ui-plugin').Utils.isBlank,
    notBlank: jest.requireActual('nexus-ui-plugin').Utils.notBlank,
    fieldProps: jest.requireActual('nexus-ui-plugin').Utils.fieldProps,
    saveTooltip: jest.requireActual('nexus-ui-plugin').Utils.saveTooltip,
    nextSortDirection: jest.requireActual('nexus-ui-plugin').Utils.nextSortDirection,
    getSortDirection: jest.requireActual('nexus-ui-plugin').Utils.getSortDirection
  }
}));

describe('CleanupPoliciesForm', function() {
  const CONFIRM = Promise.resolve();
  const onDone = jest.fn();

  function renderEditView(itemId) {
    return renderView(<CleanupPoliciesForm itemId={itemId} onDone={onDone}/>);
  }

  function renderCreateView() {
    return renderView(<CleanupPoliciesForm onDone={onDone} />);
  }

  function renderView(view) {
    return TestUtils.render(view, ({queryByLabelText, queryByText}) => ({
      name: () => queryByLabelText(UIStrings.CLEANUP_POLICIES.NAME_LABEL),
      format: () => queryByLabelText(UIStrings.CLEANUP_POLICIES.FORMAT_LABEL),
      notes: () => queryByLabelText(UIStrings.CLEANUP_POLICIES.NOTES_LABEL),
      criteriaLastBlobUpdated: () => queryByLabelText(UIStrings.CLEANUP_POLICIES.LAST_UPDATED_LABEL),
      criteriaLastDownloaded: () => queryByLabelText(UIStrings.CLEANUP_POLICIES.LAST_DOWNLOADED_LABEL),
      criteriaReleaseType: () => queryByLabelText(UIStrings.CLEANUP_POLICIES.RELEASE_TYPE_LABEL),
      criteriaAssetRegex: () => queryByLabelText(UIStrings.CLEANUP_POLICIES.ASSET_NAME_LABEL),
      saveButton: () => queryByText(UIStrings.SETTINGS.SAVE_BUTTON_LABEL),
      cancelButton: () => queryByText(UIStrings.SETTINGS.CANCEL_BUTTON_LABEL),
      deleteButton: () => queryByText(UIStrings.SETTINGS.DELETE_BUTTON_LABEL)
    }));
  }

  it('renders a loading spinner', async function() {
    axios.get.mockImplementation(() => new Promise(() => {}));

    const {container, loadingMask} = renderEditView('test');

    await waitForElement(loadingMask);

    expect(container).toMatchSnapshot();
  });

  it('renders the resolved data', async function() {
    const itemId = 'test';

    axios.get.mockImplementation((url) => {
      if (url === `/service/rest/internal/cleanup-policies/${itemId}`) {
        return Promise.resolve({
          data: {
            'name' : 'test name',
            'format' : 'testformat',
            'notes' : 'test notes',
            'criteriaLastBlobUpdated' : 7,
            'criteriaLastDownloaded' : 8,
            'criteriaReleaseType' : 'RELEASES',
            'criteriaAssetRegex' : '.*'
          }
        });
      }
      else if (url === '/service/rest/internal/cleanup-policies/criteria/formats') {
        return Promise.resolve({
          data: [{
            'id' : 'testformat',
            'name' : 'Test Format',
            'availableCriteria' : ['lastBlobUpdated', 'lastDownloaded', 'isPrerelease', 'regex']
          }]
        });
      }
    });

    const {container,
      loadingMask,
      name,
      format,
      notes,
      criteriaLastBlobUpdated,
      criteriaLastDownloaded,
      criteriaReleaseType,
      criteriaAssetRegex,
      saveButton} = renderEditView(itemId);

    await waitForElementToBeRemoved(loadingMask);

    expect(name()).toHaveValue('test name');
    expect(format()).toHaveValue('testformat');
    expect(notes()).toHaveValue('test notes');
    expect(criteriaLastBlobUpdated()).toHaveValue('7');
    expect(criteriaLastDownloaded()).toHaveValue('8');
    expect(criteriaReleaseType()).toHaveValue('RELEASES')
    expect(criteriaAssetRegex()).toHaveValue('.*')
    expect(saveButton()).toHaveClass('disabled');
    expect(container).toMatchSnapshot();
  });

  it('renders an error message', async function() {
    axios.get.mockReturnValue(Promise.reject({message: 'Error'}));

    const {container, loadingMask} = renderEditView('itemId');

    await waitForElementToBeRemoved(loadingMask);

    expect(container.querySelector('.nx-alert--error')).toHaveTextContent('Error');
  });

  it('requires the name and format fields when creating a new cleanup policy', async function() {
    axios.get.mockImplementation((url) => {
      if (url === '/service/rest/internal/cleanup-policies') {
        return Promise.resolve({data: []});
      }
      else if (url === '/service/rest/internal/cleanup-policies/criteria/formats') {
        return Promise.resolve({
          data: [{
            'id' : '*',
            'name' : 'All Formats',
            'availableCriteria' : ['lastBlobUpdated', 'lastDownloaded']
          }]
        });
      }
    });

    const {loadingMask, name, format, saveButton} = renderCreateView();

    await waitForElementToBeRemoved(loadingMask);
    expect(saveButton()).toHaveClass('disabled');

    await TestUtils.changeField(name, 'name');
    expect(saveButton()).toHaveClass('disabled');

    await TestUtils.changeField(format, '*')
    expect(saveButton()).not.toHaveClass('disabled');
  });

  it('fires onDone when cancelled', async function() {
    const {loadingMask, cancelButton} = renderCreateView();

    await waitForElementToBeRemoved(loadingMask);

    fireEvent.click(cancelButton());

    await wait(() => expect(onDone).toBeCalled());
  });

  it('requests confirmation when delete is requested', async function() {
    const itemId = 'test';
    axios.get.mockImplementation((url) => {
      if (url === '/service/rest/internal/cleanup-policies') {
        return Promise.resolve({data: []});
      }
      else if (url === `/service/rest/internal/cleanup-policies/${itemId}`) {
        return Promise.resolve({
          data: {
            'name' : 'test',
            'format' : 'testformat',
            'notes' : 'test notes',
            'criteriaLastBlobUpdated' : '7',
            'criteriaLastDownloaded' : '8',
            'criteriaReleaseType' : 'RELEASES',
            'criteriaAssetRegex' : '.*'
          }
        });
      }
      else if (url === '/service/rest/internal/cleanup-policies/criteria/formats') {
        return Promise.resolve({
          data: [{
            'id' : 'testformat',
            'name' : 'Test Format',
            'availableCriteria' : ['lastBlobUpdated', 'lastDownloaded', 'isPrerelease', 'regex']
          }]
        });
      }
    });

    axios.delete.mockReturnValue(Promise.resolve());

    const {loadingMask, deleteButton} = renderEditView(itemId);

    await waitForElementToBeRemoved(loadingMask);

    axios.put.mockReturnValue(Promise.resolve());

    ExtJS.requestConfirmation.mockReturnValue(CONFIRM);
    fireEvent.click(deleteButton());

    await wait(() => expect(axios.delete).toBeCalledWith(`/service/rest/internal/cleanup-policies/${itemId}`));
    expect(onDone).toBeCalled();
  });

  it('saves', async function() {
    axios.post.mockReturnValue(Promise.resolve());

    axios.get.mockImplementation((url) => {
      if (url === '/service/rest/internal/cleanup-policies/criteria/formats') {
        return Promise.resolve({
          data: [{
            'id' : 'testformat',
            'name' : 'Test Format',
            'availableCriteria' : ['lastBlobUpdated', 'lastDownloaded', 'isPrerelease', 'regex']
          }]
        });
      }
    });

    const {loadingMask, name, format, notes, saveButton} = renderCreateView();

    await waitForElementToBeRemoved(loadingMask);

    await wait(() => expect(window.dirty).toEqual([]));

    await TestUtils.changeField(name, 'test');
    await TestUtils.changeField(format, 'testformat');
    await TestUtils.changeField(notes, 'notes');

    await wait(() => expect(window.dirty).toEqual(['CleanupPoliciesFormMachine']));

    expect(saveButton()).not.toBeDisabled();
    fireEvent.click(saveButton());

    await wait(() => expect(axios.post).toHaveBeenCalledWith(
        '/service/rest/internal/cleanup-policies',
        {name: 'test', format: 'testformat', notes: 'notes'}
    ));
    expect(window.dirty).toEqual([]);
  });
});
