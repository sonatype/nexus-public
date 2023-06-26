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
import {screen, waitFor, waitForElementToBeRemoved} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {when} from 'jest-when';

import {ExtJS, Utils} from '@sonatype/nexus-ui-plugin';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

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

const selectors = {
  ...TestUtils.selectors,
  ...TestUtils.formSelectors,
  getCriteriaLastBlobUpdatedCheckbox: () => screen.getByTitle(/(Enable|Disable) Component Age Criteria/),
  getCriteriaLastDownloadedCheckbox: () => screen.getByTitle(/(Enable|Disable) Component Usage Criteria/),
  getCriteriaReleaseTypeCheckbox: () => screen.getByTitle(/(Enable|Disable) Release Type Criteria/),
  getCriteriaAssetRegexCheckbox: () => screen.getByTitle(/(Enable|Disable) Asset Name Matcher Criteria/)
}

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
    return TestUtils.render(view, ({queryByLabelText, queryByText, queryByPlaceholderText}) => ({
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
      previewFilterText: () => queryByPlaceholderText(UIStrings.CLEANUP_POLICIES.FILTER_PLACEHOLDER),
      previewSampleWarning: () => queryByText(UIStrings.CLEANUP_POLICIES.PREVIEW.SAMPLE_WARNING, {exact: false}),
      previewCmpCount: (a, t) => queryByText(UIStrings.CLEANUP_POLICIES.PREVIEW.COMPONENT_COUNT(a, t), {exact: false})
    }));
  }

  beforeEach(() => {
    ExtJS.state = jest.fn().mockReturnValue({
      getValue: jest.fn()
    });
    when(ExtJS.state().getValue).calledWith('nexus.cleanup.preview.enabled').mockReturnValue(false);

    when(axios.get)
      .calledWith(REPOSITORIES_URL, {params: {format: EDITABLE_ITEM.format}})
      .mockResolvedValue({
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

  it('renders the resolved data', async function() {
    const {
      name,
      format,
      notes,
      criteriaLastBlobUpdated,
      criteriaLastDownloaded,
      criteriaReleaseType,
      criteriaAssetRegex,
    } = renderEditView(EDITABLE_ITEM.name);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(name()).toHaveValue('test');
    expect(format()).toHaveValue('testformat');
    expect(notes()).toHaveValue('test notes');
    expect(selectors.getCriteriaLastBlobUpdatedCheckbox()).toHaveClass('tm-checked');
    expect(criteriaLastBlobUpdated()).toHaveValue('7');
    expect(selectors.getCriteriaLastDownloadedCheckbox()).toHaveClass('tm-checked');
    expect(criteriaLastDownloaded()).toHaveValue('8');
    expect(selectors.getCriteriaReleaseTypeCheckbox()).toHaveClass('tm-checked');
    expect(criteriaReleaseType()).toHaveValue('RELEASES');
    expect(selectors.getCriteriaAssetRegexCheckbox()).toHaveClass('tm-checked');
    expect(criteriaAssetRegex()).toHaveValue('.*');
    expect(selectors.queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();
  });

  it('renders an error message', async function() {
    axios.get.mockReturnValue(Promise.reject({message: 'Error'}));

    const {container} = renderEditView('itemId');

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(container.querySelector('.nx-alert--error')).toHaveTextContent('Error');
  });

  it('requires the name and format fields when creating a new cleanup policy', async function() {
    const {name, format} = renderCreateView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(selectors.queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();

    await TestUtils.changeField(name, 'name');
    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

    await TestUtils.changeField(format, 'testformat')
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('does not allow decimal values in lastBlobUpdated fields' , async function() {
    const {name, format, criteriaLastBlobUpdated, container} = renderCreateView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    await TestUtils.changeField(name, 'name');
    await TestUtils.changeField(format, 'testformat')
    const lastBlobCheckbox = container.querySelector('#criteria-last-blob-updated-group .nx-radio-checkbox');

    userEvent.click(lastBlobCheckbox);
    await TestUtils.changeField(criteriaLastBlobUpdated, '4');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();

    await TestUtils.changeField(criteriaLastBlobUpdated, '4.7');
    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();
  });

  it('does not allow decimal values in lastDownloaded fields' , async function() {
    const {name, format, criteriaLastDownloaded, container} = renderCreateView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    await TestUtils.changeField(name, 'name');
    await TestUtils.changeField(format, 'testformat')
    const lastDownloadedCheckbox = container.querySelector('#criteria-last-downloaded-group .nx-radio-checkbox');

    userEvent.click(lastDownloadedCheckbox);
    await TestUtils.changeField(criteriaLastDownloaded, '5');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();

    await TestUtils.changeField(criteriaLastDownloaded, '5.3');
    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();
  });

  it('fires onDone when cancelled', async function() {
    const {cancelButton} = renderCreateView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.click(cancelButton());

    await waitFor(() => expect(onDone).toBeCalled());
  });

  it('requests confirmation when delete is requested', async function() {
    const itemId = 'test';
    when(axios.get).calledWith(EDIT_URL(itemId)).mockResolvedValue({
      data: {...EDITABLE_ITEM}
    });

    axios.delete.mockReturnValue(Promise.resolve());

    const {deleteButton} = renderEditView(itemId);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    axios.put.mockReturnValue(Promise.resolve());

    ExtJS.requestConfirmation.mockReturnValue(CONFIRM);
    userEvent.click(deleteButton());

    await waitFor(() => expect(axios.delete).toBeCalledWith(EDIT_URL(itemId)));
    expect(onDone).toBeCalled();
  });

  it('saves', async function() {
    axios.post.mockReturnValue(Promise.resolve());

    const {name, format, notes, saveButton} = renderCreateView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    await waitFor(() => expect(window.dirty).toEqual([]));

    await TestUtils.changeField(name, 'test');
    await TestUtils.changeField(format, 'testformat');
    await TestUtils.changeField(notes, 'notes');

    await waitFor(() => expect(window.dirty).toEqual(['CleanupPoliciesFormMachine']));

    expect(saveButton()).not.toBeDisabled();
    userEvent.click(saveButton());

    await waitFor(() => expect(axios.post).toHaveBeenCalledWith(
        '/service/rest/internal/cleanup-policies',
        {
          name: 'test',
          format: 'testformat',
          notes: 'notes',
          criteriaAssetRegex: null,
          criteriaLastBlobUpdated: null,
          criteriaLastDownloaded: null,
          criteriaReleaseType: null
        }
    ));
    expect(window.dirty).toEqual([]);
  });

  it('resets data fields when disable checkboxes', async function() {
    const {container,
      criteriaLastBlobUpdated,
      criteriaLastDownloaded,
      criteriaReleaseType,
      criteriaAssetRegex,
      saveButton} = renderEditView(EDITABLE_ITEM.name);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(criteriaLastBlobUpdated()).not.toBeDisabled();
    expect(criteriaLastDownloaded()).not.toBeDisabled();
    expect(criteriaReleaseType()).not.toBeDisabled();
    expect(criteriaAssetRegex()).not.toBeDisabled();

    userEvent.selectOptions(criteriaReleaseType(), '');
    expect(criteriaReleaseType()).toHaveValue('');

    const lastBlobCheckbox = container.querySelector('#criteria-last-blob-updated-group .nx-radio-checkbox');
    const lastDownloadedCheckbox = container.querySelector('#criteria-last-downloaded-group .nx-radio-checkbox');
    const releaseTypeCheckbox = container.querySelector('#criteria-release-type-group .nx-radio-checkbox');
    const assetNameCheckbox = container.querySelector('#criteria-asset-name-group .nx-radio-checkbox');

    userEvent.click(lastBlobCheckbox);
    userEvent.click(lastDownloadedCheckbox);
    userEvent.click(releaseTypeCheckbox);
    userEvent.click(assetNameCheckbox);

    expect(criteriaLastBlobUpdated()).toBeDisabled();
    expect(criteriaLastDownloaded()).toBeDisabled();
    expect(criteriaReleaseType()).toBeDisabled();
    expect(criteriaAssetRegex()).toBeDisabled();

    expect(saveButton()).not.toHaveClass('disabled');

    userEvent.click(saveButton());

    await waitFor(() => expect(axios.put).toHaveBeenCalledWith(
      '/service/rest/internal/cleanup-policies/' + EDITABLE_ITEM.name,
      {
        criteriaAssetRegex: null,
        criteriaLastBlobUpdated: null,
        criteriaLastDownloaded: null,
        criteriaReleaseType: null,
        format: EDITABLE_ITEM.format,
        name: EDITABLE_ITEM.name,
        notes: EDITABLE_ITEM.notes
      }
    ));
  });

  describe('preview', function() {
    it('submits the filter text to the backend', async function() {
      const {
        previewButton,
        previewFilterText, 
        previewRepositories, 
        previewSampleWarning,
        previewCmpCount,
        queryByText
      } = renderEditView(EDITABLE_ITEM.name);

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      userEvent.selectOptions(previewRepositories(), 'maven-central');
      expect(previewRepositories()).toHaveValue('maven-central');

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

      userEvent.click(previewButton());

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      expect(queryByText('maven-aether-provider')).toBeInTheDocument();

      expect(previewSampleWarning()).toBeInTheDocument();
      expect(previewCmpCount(1, 1)).toBeInTheDocument();

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

      expect(previewSampleWarning()).not.toBeInTheDocument();
    });

    it('clears preview results when the cleanup policies form is changed', async function() {
      const {
        format,
        previewButton,
        previewRepositories,
        previewSampleWarning,
        previewCmpCount,
        queryByText
      } = renderEditView(EDITABLE_ITEM.name);

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      userEvent.selectOptions(previewRepositories(), 'maven-central');
      expect(previewRepositories()).toHaveValue('maven-central');

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

      userEvent.click(previewButton());

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      expect(queryByText('maven-aether-provider')).toBeInTheDocument();
      expect(selectors.getCriteriaLastBlobUpdatedCheckbox()).toHaveClass('tm-checked');
      expect(selectors.getCriteriaLastDownloadedCheckbox()).toHaveClass('tm-checked');
      expect(selectors.getCriteriaReleaseTypeCheckbox()).toHaveClass('tm-checked');
      expect(selectors.getCriteriaAssetRegexCheckbox()).toHaveClass('tm-checked');

      expect(previewSampleWarning()).toBeInTheDocument();
      expect(previewCmpCount(1, 1)).toBeInTheDocument();

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

      await TestUtils.changeField(format, 'testformat');

      expect(selectors.getCriteriaLastBlobUpdatedCheckbox()).toHaveClass('tm-unchecked');
      expect(selectors.getCriteriaLastDownloadedCheckbox()).toHaveClass('tm-unchecked');
      expect(selectors.getCriteriaReleaseTypeCheckbox()).toHaveClass('tm-unchecked');
      expect(selectors.getCriteriaAssetRegexCheckbox()).toHaveClass('tm-unchecked');
      expect(queryByText('No assets in repository matched the criteria')).toBeInTheDocument();
      expect(previewSampleWarning()).not.toBeInTheDocument();
    });
  });
});
