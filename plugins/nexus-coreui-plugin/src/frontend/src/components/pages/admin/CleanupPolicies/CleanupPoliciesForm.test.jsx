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
import {when} from 'jest-when';

import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import axios from 'axios';
import {ExtJS, Utils} from '@sonatype/nexus-ui-plugin';

import CleanupPoliciesForm from './CleanupPoliciesForm';

import UIStrings from '../../../../constants/UIStrings';

jest.mock('axios', () => ({
  ...jest.requireActual('axios'), // Use most functions from actual axios
  get: jest.fn(),
  put: jest.fn(),
  post: jest.fn(),
  delete: jest.fn()
}));

jest.spyOn(ExtJS, 'requestConfirmation').mockImplementation(jest.fn());
jest.spyOn(ExtJS, 'showErrorMessage').mockImplementation(jest.fn());
jest.spyOn(ExtJS, 'showSuccessMessage').mockImplementation(jest.fn());

jest.spyOn(Utils, 'buildFormMachine').mockImplementation((args) => {
  const machine = jest.requireActual('@sonatype/nexus-ui-plugin').Utils.buildFormMachine(args);
  // Disable the default logSaveSuccess etc methods to avoid extjs calls
  return machine.withConfig({
    actions: {
      logSaveSuccess: jest.fn(),
      logSaveError: jest.fn(),
      logLoadError: jest.fn()
    }
  })
});

const EDIT_URL = (itemId) => `/service/rest/internal/cleanup-policies/${itemId}`;
const FORMATS_URL = '/service/rest/internal/cleanup-policies/criteria/formats';
const REPOSITORIES_URL = '/service/rest/internal/ui/repositories';
const PREVIEW_URL = '/service/rest/internal/cleanup-policies/preview/components';

describe('CleanupPoliciesForm', function() {
  const CONFIRM = Promise.resolve();
  const onDone = jest.fn();

  const EDITABLE_ITEM = {
    'name' : 'test',
    'format' : 'testformat',
    'notes' : 'test notes',
    'criteriaLastBlobUpdated' : 7,
    'criteriaLastDownloaded' : 8,
    'criteriaReleaseType' : 'RELEASES',
    'criteriaAssetRegex' : '.*'
  };

  function renderEditView(itemId) {
    return renderView(<CleanupPoliciesForm itemId={itemId} onDone={onDone}/>);
  }

  function renderCreateView() {
    return renderView(<CleanupPoliciesForm onDone={onDone} />);
  }

  function renderView(view) {
    return TestUtils.render(view, ({queryByLabelText, queryByText, queryByPlaceholderText, loadingMask}) => ({
      name: () => queryByLabelText(UIStrings.CLEANUP_POLICIES.NAME_LABEL),
      format: () => queryByLabelText(UIStrings.CLEANUP_POLICIES.FORMAT_LABEL),
      notes: () => queryByLabelText(UIStrings.CLEANUP_POLICIES.NOTES_LABEL),
      criteriaLastBlobUpdated: () => queryByLabelText(UIStrings.CLEANUP_POLICIES.LAST_UPDATED_LABEL),
      criteriaLastDownloaded: () => queryByLabelText(UIStrings.CLEANUP_POLICIES.LAST_DOWNLOADED_LABEL),
      criteriaReleaseType: () => queryByLabelText(UIStrings.CLEANUP_POLICIES.RELEASE_TYPE_LABEL),
      criteriaAssetRegex: () => queryByLabelText(UIStrings.CLEANUP_POLICIES.ASSET_NAME_LABEL),
      saveButton: () => queryByText(UIStrings.SETTINGS.SAVE_BUTTON_LABEL),
      cancelButton: () => queryByText(UIStrings.SETTINGS.CANCEL_BUTTON_LABEL),
      deleteButton: () => queryByText(UIStrings.SETTINGS.DELETE_BUTTON_LABEL),
      previewButton: () => queryByText(UIStrings.CLEANUP_POLICIES.PREVIEW.BUTTON),
      previewRepositories: () => queryByLabelText(UIStrings.CLEANUP_POLICIES.PREVIEW.REPOSITORY_LABEL),
      previewFilterText: () => queryByPlaceholderText(UIStrings.CLEANUP_POLICIES.FILTER_PLACEHOLDER)
    }));
  }

  beforeEach(() => {
    when(axios.get).calledWith(REPOSITORIES_URL).mockResolvedValue({
      data:  [{
        'id': 'maven-central',
        'name': 'maven-central'
      }]
    });

    when(axios.get).calledWith(EDIT_URL(EDITABLE_ITEM.name)).mockResolvedValue({
      data: EDITABLE_ITEM
    });

    when(axios.get).calledWith(FORMATS_URL).mockResolvedValue({
      data: [{
        'id' : 'testformat',
        'name' : 'Test Format',
        'availableCriteria' : ['lastBlobUpdated', 'lastDownloaded', 'isPrerelease', 'regex']
      }]
    });
  });

  it('renders a loading spinner', async function() {
    axios.get.mockImplementation(() => new Promise(() => {}));

    const {container, loadingMask} = renderEditView('test');

    await waitForElement(loadingMask);

    expect(container).toMatchSnapshot();
  });

  it('renders the resolved data', async function() {
    const {container,
      loadingMask,
      name,
      format,
      notes,
      criteriaLastBlobUpdated,
      criteriaLastDownloaded,
      criteriaReleaseType,
      criteriaAssetRegex,
      saveButton} = renderEditView(EDITABLE_ITEM.name);

    await waitForElementToBeRemoved(loadingMask);

    expect(name()).toHaveValue('test');
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
    const {loadingMask, name, format, saveButton} = renderCreateView();

    await waitForElementToBeRemoved(loadingMask);
    expect(saveButton()).toHaveClass('disabled');

    await TestUtils.changeField(name, 'name');
    expect(saveButton()).toHaveClass('disabled');

    await TestUtils.changeField(format, 'testformat')
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
    when(axios.get).calledWith(EDIT_URL(itemId)).mockResolvedValue({
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

    axios.delete.mockReturnValue(Promise.resolve());

    const {loadingMask, deleteButton} = renderEditView(itemId);

    await waitForElementToBeRemoved(loadingMask);

    axios.put.mockReturnValue(Promise.resolve());

    ExtJS.requestConfirmation.mockReturnValue(CONFIRM);
    fireEvent.click(deleteButton());

    await wait(() => expect(axios.delete).toBeCalledWith(EDIT_URL(itemId)));
    expect(onDone).toBeCalled();
  });

  it('saves', async function() {
    axios.post.mockReturnValue(Promise.resolve());

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

  describe('preview', function() {
    it('submits the filter text to the backend', async function() {
      const {loadingMask, previewButton, previewFilterText, previewRepositories, queryByText} = renderEditView(EDITABLE_ITEM.name);

      await waitForElementToBeRemoved(loadingMask);

      await TestUtils.changeField(previewRepositories, 'maven-central');

      when(axios.post).calledWith(PREVIEW_URL, {
        criteriaLastBlobUpdated: EDITABLE_ITEM.criteriaLastBlobUpdated,
        criteriaLastDownloaded: EDITABLE_ITEM.criteriaLastDownloaded,
        criteriaReleaseType: EDITABLE_ITEM.criteriaReleaseType,
        criteriaAssetRegex: EDITABLE_ITEM.criteriaAssetRegex,
        filter: '',
        repository: 'maven-central'
      }).mockResolvedValueOnce({
        data: {
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
        }
      });

      fireEvent.click(previewButton());

      await waitForElementToBeRemoved(loadingMask);

      expect(queryByText('maven-aether-provider')).toBeInTheDocument();

      when(axios.post).calledWith(PREVIEW_URL, {
        criteriaLastBlobUpdated: EDITABLE_ITEM.criteriaLastBlobUpdated,
        criteriaLastDownloaded: EDITABLE_ITEM.criteriaLastDownloaded,
        criteriaReleaseType: EDITABLE_ITEM.criteriaReleaseType,
        criteriaAssetRegex: EDITABLE_ITEM.criteriaAssetRegex,
        filter: 'test',
        repository: 'maven-central'
      }).mockResolvedValueOnce({
        data: {
          total: 0,
          results: []
        }
      });

      await TestUtils.changeField(previewFilterText, 'test');

      await waitForElementToBeRemoved(() => queryByText('maven-aether-provider'));

      expect(queryByText('No assets in repository matched the criteria')).toBeInTheDocument();
    });
  });
});
