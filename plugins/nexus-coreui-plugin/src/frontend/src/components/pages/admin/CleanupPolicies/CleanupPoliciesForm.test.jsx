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
import {
  screen,
  waitFor,
  waitForElementToBeRemoved,
  render,
  act,
  within,
} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {when} from 'jest-when';
import {URL} from './CleanupPoliciesHelper';
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import CleanupPoliciesForm from './CleanupPoliciesForm';

import UIStrings from '../../../../constants/UIStrings';

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
    urlOf: jest.fn().mockImplementation((path) => 'https://testurl' + path),
  },
}));

const FORMATS_URL = 'service/rest/internal/cleanup-policies/criteria/formats';
const REPOSITORIES_URL = 'service/rest/internal/ui/repositories';
const PREVIEW_URL =
  'service/rest/internal/cleanup-policies/preview/components';

const {CLEANUP_POLICIES: LABELS} = UIStrings;

const selectors = {
  ...TestUtils.selectors,
  ...TestUtils.formSelectors,
  name: () => screen.getByLabelText(LABELS.NAME_LABEL),
  format: () => screen.getByLabelText(LABELS.FORMAT_LABEL),
  notes: () => screen.getByLabelText(LABELS.DESCRIPTION_LABEL),
  releaseType: () => screen.getByLabelText(LABELS.RELEASE_TYPE_LABEL),
  getCriteriaLastBlobUpdatedCheckbox: () =>
    screen.getByTitle(/(Enable|Disable) Component Age Criterion/),
  criteriaLastBlobUpdated: () =>
    screen.queryByLabelText(LABELS.LAST_UPDATED_LABEL),
  getCriteriaLastDownloadedCheckbox: () =>
    screen.getByTitle(/(Enable|Disable) Component Usage Criterion/),
  criteriaLastDownloaded: () =>
    screen.queryByLabelText(LABELS.LAST_DOWNLOADED_LABEL),
  getCriteriaAssetRegexCheckbox: () =>
    screen.getByTitle(/(Enable|Disable) Asset Name Matcher Criterion/),
  criteriaAssetRegex: () => screen.getByLabelText(LABELS.ASSET_NAME_LABEL),
  criteriaVersion: () =>
    screen.queryByLabelText(LABELS.EXCLUSION_CRITERIA.VERSION_LABEL),
  getCriteriaVersionCheckbox: () =>
    screen.getByLabelText(LABELS.EXCLUSION_CRITERIA.LABEL),
  versionAlertMessage: () =>
    screen.queryByText(LABELS.EXCLUSION_CRITERIA.ALERT),
  additionalCriteriaMessage: () =>
    screen.queryByText(LABELS.EXCLUSION_CRITERIA.ADDITIONAL_CRITERIA_ALERT),
  normalizedVersionAlertMessage: () =>
    screen.queryByText(LABELS.EXCLUSION_CRITERIA.NORMALIZED_VERSION_ALERT),
  cancelButton: () => screen.getByText(UIStrings.SETTINGS.CANCEL_BUTTON_LABEL),
  deleteButton: () => screen.getByText(UIStrings.SETTINGS.DELETE_BUTTON_LABEL),
  saveButton: () => screen.getByText(UIStrings.SETTINGS.SAVE_BUTTON_LABEL),
  previewRepositories: () =>
    screen.getByLabelText(LABELS.PREVIEW.REPOSITORY_LABEL),
  previewButton: () => screen.getByText(LABELS.PREVIEW.BUTTON),
  previewSampleWarning: () =>
    screen.queryByText(LABELS.PREVIEW.SAMPLE_WARNING, {exact: false}),
  previewCmpCount: (a, t) =>
    screen.getByText(LABELS.PREVIEW.COMPONENT_COUNT(a, t), {
      exact: false,
    }),
  previewFilterText: () =>
    screen.getByPlaceholderText(LABELS.FILTER_PLACEHOLDER),
  dryRunRepositories: () =>
    screen.queryByRole('combobox', {
      description: UIStrings.CLEANUP_POLICIES.DRY_RUN.REPOSITORY_DESCRIPTION,
    }),
  dryRunCreateCSVButton: () =>
    screen.getByRole('link', {name: LABELS.DRY_RUN.BUTTON}),
};

describe('CleanupPoliciesForm', function () {
  const CONFIRM = Promise.resolve();
  const onDone = jest.fn();

  const EDITABLE_ITEM = {
    name: 'test',
    format: 'maven2',
    notes: 'test notes',
    criteriaLastBlobUpdated: 7,
    criteriaLastDownloaded: 8,
    criteriaReleaseType: 'RELEASES',
    criteriaAssetRegex: '.*',
    retain: 2,
    sortBy: 'version',
  };

  async function renderView(itemId) {
    const view = render(
      <CleanupPoliciesForm itemId={itemId} onDone={onDone} />
    );
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
    return view;
  }

  beforeEach(() => {
    ExtJS.state = jest.fn().mockReturnValue({
      getValue: jest.fn(),
    });
    when(ExtJS.state().getValue)
      .calledWith('datastore.isPostgresql')
      .mockReturnValue(false);
    when(ExtJS.state().getValue)
      .calledWith('nexus.cleanup.preview.enabled')
      .mockReturnValue(false);

    when(axios.get)
      .calledWith(REPOSITORIES_URL, {params: {format: EDITABLE_ITEM.format}})
      .mockResolvedValue({
        data: [
          {
            id: 'maven-central',
            name: 'maven-central',
          },
        ],
      });

    when(axios.get)
      .calledWith(URL.singleCleanupPolicyUrl(EDITABLE_ITEM.name))
      .mockResolvedValue({
        data: EDITABLE_ITEM,
      });

    when(axios.get)
      .calledWith(FORMATS_URL)
      .mockResolvedValue({
        data: [
          {
            id: 'maven2',
            name: 'maven2',
            availableCriteria: [
              'regex',
              'isPrerelease',
              'retain',
              'sortBy',
              'lastDownloaded',
              'lastBlobUpdated',
            ],
          },
          {
            id: 'docker',
            name: 'docker',
            availableCriteria: [
              'regex',
              'retain',
              'sortBy',
              'lastDownloaded',
              'lastBlobUpdated',
            ],
          },
          {
            id: 'testformat',
            name: 'Test Format',
            availableCriteria: [
              'lastBlobUpdated',
              'lastDownloaded',
              'isPrerelease',
              'regex',
            ],
          },
        ],
      });
  });

  it('renders the resolved data', async function () {
    const {
      name,
      format,
      notes,
      releaseType,
      getCriteriaLastBlobUpdatedCheckbox,
      criteriaLastBlobUpdated,
      getCriteriaLastDownloadedCheckbox,
      criteriaLastDownloaded,
      getCriteriaAssetRegexCheckbox,
      criteriaAssetRegex,
    } = selectors;

    await renderView(EDITABLE_ITEM.name);

    expect(name()).toHaveValue(EDITABLE_ITEM.name);
    expect(format()).toHaveValue(EDITABLE_ITEM.format);
    expect(notes()).toHaveValue(EDITABLE_ITEM.notes);
    expect(releaseType()).toHaveValue(EDITABLE_ITEM.criteriaReleaseType);
    expect(getCriteriaLastBlobUpdatedCheckbox()).toHaveClass('tm-checked');
    expect(criteriaLastBlobUpdated()).toHaveValue(
      EDITABLE_ITEM.criteriaLastBlobUpdated.toString()
    );
    expect(getCriteriaLastDownloadedCheckbox()).toHaveClass('tm-checked');
    expect(criteriaLastDownloaded()).toHaveValue(
      EDITABLE_ITEM.criteriaLastDownloaded.toString()
    );
    expect(getCriteriaAssetRegexCheckbox()).toHaveClass('tm-checked');
    expect(criteriaAssetRegex()).toHaveValue(EDITABLE_ITEM.criteriaAssetRegex);
  });

  it('renders an error message', async function () {
    axios.get.mockRejectedValue({message: 'Error'});

    const {container} = await renderView('itemId');

    expect(container.querySelector('.nx-alert--error')).toHaveTextContent(
      'Error'
    );
  });

  it('requires the name, format and some criteria when creating a new cleanup policy', async function () {
    const {name, format, criteriaLastBlobUpdated, getCriteriaLastBlobUpdatedCheckbox} = selectors;
    await renderView();

    await TestUtils.changeField(name, EDITABLE_ITEM.name);
    userEvent.click(selectors.querySubmitButton());
    expect(
      selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)
    ).toBeInTheDocument();

    await TestUtils.changeField(format, EDITABLE_ITEM.format);

    userEvent.click(getCriteriaLastBlobUpdatedCheckbox());
    await TestUtils.changeField(criteriaLastBlobUpdated, `${EDITABLE_ITEM.criteriaLastBlobUpdated}`);
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('does not allow to save if some cleanup criteria is not set' ,  async function (){
    const {name, format, saveButton} = selectors;

    await renderView();

    await TestUtils.changeField(name, EDITABLE_ITEM.name);
    await TestUtils.changeField(format, EDITABLE_ITEM.format);

    expect(screen.queryByText(LABELS.MESSAGES.NO_CRITERIA_ERROR)).not.toBeInTheDocument();

    userEvent.click(saveButton());

    expect(
        selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)
    ).toBeInTheDocument();

    expect(screen.queryByText(LABELS.MESSAGES.NO_CRITERIA_ERROR)).toBeInTheDocument();
  });

  it('does not allow decimal values in lastBlobUpdated fields', async function () {
    const {name, format, criteriaLastBlobUpdated} = selectors;
    const {container} = await renderView();

    await TestUtils.changeField(name, EDITABLE_ITEM.name);
    await TestUtils.changeField(format, EDITABLE_ITEM.format);
    const lastBlobCheckbox = container.querySelector(
      '#criteria-last-blob-updated-group .nx-radio-checkbox'
    );

    userEvent.click(lastBlobCheckbox);
    await TestUtils.changeField(criteriaLastBlobUpdated, '4');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();

    await TestUtils.changeField(criteriaLastBlobUpdated, '4.7');
    userEvent.click(selectors.querySubmitButton());
    expect(
      selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)
    ).toBeInTheDocument();
  });

  it('does not allow decimal values in lastDownloaded fields', async function () {
    const {name, format, criteriaLastDownloaded} = selectors;
    const {container} = await renderView();

    await TestUtils.changeField(name, EDITABLE_ITEM.name);
    await TestUtils.changeField(format, EDITABLE_ITEM.format);
    const lastDownloadedCheckbox = container.querySelector(
      '#criteria-last-downloaded-group .nx-radio-checkbox'
    );

    userEvent.click(lastDownloadedCheckbox);
    await TestUtils.changeField(criteriaLastDownloaded, '5');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();

    await TestUtils.changeField(criteriaLastDownloaded, '5.3');
    userEvent.click(selectors.querySubmitButton());
    expect(
      selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)
    ).toBeInTheDocument();
  });

  it('fires onDone when cancelled', async function () {
    await renderView();

    userEvent.click(selectors.cancelButton());

    await waitFor(() => expect(onDone).toBeCalled());
  });

  it('requests confirmation when delete is requested', async function () {
    const itemId = 'test';
    when(axios.get)
      .calledWith(URL.singleCleanupPolicyUrl(itemId))
      .mockResolvedValue({
        data: EDITABLE_ITEM,
      });

    axios.delete.mockReturnValue(Promise.resolve());

    const {deleteButton} = selectors;

    await renderView(itemId);

    axios.put.mockReturnValue(Promise.resolve());

    ExtJS.requestConfirmation.mockReturnValue(CONFIRM);

    await act(async () => userEvent.click(deleteButton()));

    await waitFor(() =>
      expect(axios.delete).toBeCalledWith(URL.singleCleanupPolicyUrl(itemId))
    );
    expect(onDone).toBeCalled();
  });

  it('saves', async function () {
    axios.post.mockReturnValue(Promise.resolve());

    const {name, format, notes, saveButton , criteriaLastBlobUpdated, getCriteriaLastBlobUpdatedCheckbox} = selectors;
    await renderView();

    expect(window.dirty).toEqual([]);

    await TestUtils.changeField(name, EDITABLE_ITEM.name);
    await TestUtils.changeField(format, EDITABLE_ITEM.format);
    await TestUtils.changeField(notes, EDITABLE_ITEM.notes);

    userEvent.click(getCriteriaLastBlobUpdatedCheckbox());
    await TestUtils.changeField(criteriaLastBlobUpdated, `${EDITABLE_ITEM.criteriaLastBlobUpdated}`);

    expect(window.dirty).toEqual(['CleanupPoliciesFormMachine']);
    expect(saveButton()).not.toBeDisabled();

    await act(async () => userEvent.click(saveButton()));

    await waitFor(() =>
      expect(axios.post).toHaveBeenCalledWith(
        'service/rest/internal/cleanup-policies',
        {
          name: EDITABLE_ITEM.name,
          format: EDITABLE_ITEM.format,
          notes: EDITABLE_ITEM.notes,
          criteriaAssetRegex: null,
          criteriaLastBlobUpdated: `${EDITABLE_ITEM.criteriaLastBlobUpdated}`,
          criteriaLastDownloaded: null,
          criteriaReleaseType: null,
          retain: null,
          sortBy: null,
        }
      )
    );
    expect(window.dirty).toEqual([]);
  });

  it('resets data fields when disable checkboxes', async function () {
    const {
      criteriaLastBlobUpdated,
      criteriaLastDownloaded,
      releaseType,
      criteriaAssetRegex,
      saveButton,
    } = selectors;
    const {container} = await renderView(EDITABLE_ITEM.name);

    expect(criteriaLastBlobUpdated()).not.toBeDisabled();
    expect(criteriaLastDownloaded()).not.toBeDisabled();
    expect(criteriaAssetRegex()).not.toBeDisabled();

    await act(async () => userEvent.selectOptions(releaseType(), ''));
    expect(releaseType()).toHaveValue('');
    const lastBlobCheckbox = container.querySelector(
      '#criteria-last-blob-updated-group .nx-radio-checkbox'
    );
    const lastDownloadedCheckbox = container.querySelector(
      '#criteria-last-downloaded-group .nx-radio-checkbox'
    );
    const assetNameCheckbox = container.querySelector(
      '#criteria-asset-name-group .nx-radio-checkbox'
    );

    await act(async () => userEvent.click(lastBlobCheckbox));
    await act(async () => userEvent.click(lastDownloadedCheckbox));
    await act(async () => userEvent.click(assetNameCheckbox));

    expect(criteriaLastBlobUpdated()).toBeDisabled();
    expect(criteriaLastDownloaded()).toBeDisabled();
    expect(criteriaAssetRegex()).toBeDisabled();

    expect(saveButton()).not.toHaveClass('disabled');

    expect(criteriaLastBlobUpdated()).toHaveValue('');
    expect(criteriaLastDownloaded()).toHaveValue('');
    expect(criteriaAssetRegex()).toHaveValue('');
  });

  describe('preview', function () {
    it('submits the filter text to the backend', async function () {
      const {
        previewButton,
        previewFilterText,
        previewRepositories,
        previewSampleWarning,
        previewCmpCount,
      } = selectors;

      await renderView(EDITABLE_ITEM.name);

      userEvent.selectOptions(previewRepositories(), 'maven-central');
      expect(previewRepositories()).toHaveValue('maven-central');

      when(axios.post)
        .calledWith(PREVIEW_URL, {
          criteriaLastBlobUpdated: EDITABLE_ITEM.criteriaLastBlobUpdated,
          criteriaLastDownloaded: EDITABLE_ITEM.criteriaLastDownloaded,
          criteriaReleaseType: EDITABLE_ITEM.criteriaReleaseType,
          criteriaAssetRegex: EDITABLE_ITEM.criteriaAssetRegex,
          filter: '',
          repository: 'maven-central',
        })
        .mockResolvedValueOnce({
          data: {
            total: 1,
            results: [
              {
                id: null,
                repository: 'maven-central',
                format: 'maven2',
                group: 'org.apache.maven',
                name: 'maven-aether-provider',
                version: '3.0',
                assets: null,
              },
            ],
          },
        });

      userEvent.click(previewButton());

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      expect(screen.getByText('maven-aether-provider')).toBeInTheDocument();

      expect(previewSampleWarning()).toBeInTheDocument();
      expect(previewCmpCount(1, 1)).toBeInTheDocument();

      when(axios.post)
        .calledWith(PREVIEW_URL, {
          criteriaLastBlobUpdated: EDITABLE_ITEM.criteriaLastBlobUpdated,
          criteriaLastDownloaded: EDITABLE_ITEM.criteriaLastDownloaded,
          criteriaReleaseType: EDITABLE_ITEM.criteriaReleaseType,
          criteriaAssetRegex: EDITABLE_ITEM.criteriaAssetRegex,
          filter: 'test',
          repository: 'maven-central',
        })
        .mockResolvedValueOnce({
          data: {
            total: 0,
            results: [],
          },
        });

      await TestUtils.changeField(previewFilterText, 'test');

      await waitForElementToBeRemoved(() =>
        screen.queryByText('maven-aether-provider')
      );

      expect(
        screen.getByText('No assets in repository matched the criteria')
      ).toBeInTheDocument();

      expect(previewSampleWarning()).not.toBeInTheDocument();
    });

    it('clears preview results when the cleanup policies form is changed', async function () {
      const {
        format,
        previewButton,
        previewRepositories,
        previewSampleWarning,
        previewCmpCount,
        getCriteriaLastBlobUpdatedCheckbox,
        getCriteriaLastDownloadedCheckbox,
        getCriteriaAssetRegexCheckbox,
      } = selectors;

      await renderView(EDITABLE_ITEM.name);

      userEvent.selectOptions(previewRepositories(), 'maven-central');
      expect(previewRepositories()).toHaveValue('maven-central');

      when(axios.post)
        .calledWith(PREVIEW_URL, {
          criteriaLastBlobUpdated: EDITABLE_ITEM.criteriaLastBlobUpdated,
          criteriaLastDownloaded: EDITABLE_ITEM.criteriaLastDownloaded,
          criteriaReleaseType: EDITABLE_ITEM.criteriaReleaseType,
          criteriaAssetRegex: EDITABLE_ITEM.criteriaAssetRegex,
          filter: '',
          repository: 'maven-central',
        })
        .mockResolvedValueOnce({
          data: {
            total: 1,
            results: [
              {
                id: null,
                repository: 'maven-central',
                format: 'maven2',
                group: 'org.apache.maven',
                name: 'maven-aether-provider',
                version: '3.0',
                assets: null,
              },
            ],
          },
        });

      userEvent.click(previewButton());

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      expect(screen.queryByText('maven-aether-provider')).toBeInTheDocument();
      expect(getCriteriaLastBlobUpdatedCheckbox()).toHaveClass('tm-checked');
      expect(getCriteriaLastDownloadedCheckbox()).toHaveClass('tm-checked');
      expect(getCriteriaAssetRegexCheckbox()).toHaveClass('tm-checked');

      expect(previewSampleWarning()).toBeInTheDocument();
      expect(previewCmpCount(1, 1)).toBeInTheDocument();

      await TestUtils.changeField(format, 'maven2');

      expect(getCriteriaLastBlobUpdatedCheckbox()).toHaveClass('tm-unchecked');
      expect(getCriteriaLastDownloadedCheckbox()).toHaveClass('tm-unchecked');
      expect(getCriteriaAssetRegexCheckbox()).toHaveClass('tm-unchecked');
      expect(
        screen.queryByText('No assets in repository matched the criteria')
      ).toBeInTheDocument();
      expect(previewSampleWarning()).not.toBeInTheDocument();
    });
  });

  describe('dry run', function () {
    beforeEach(() => {
      when(ExtJS.state().getValue)
        .calledWith('datastore.isPostgresql')
        .mockReturnValue(true);
      when(ExtJS.state().getValue)
        .calledWith('nexus.cleanup.preview.enabled')
        .mockReturnValue(true);
    });

    it('renders the resolved data', async function () {
      const {dryRunRepositories, dryRunCreateCSVButton} = selectors;

      await renderView(EDITABLE_ITEM.name);

      const selectDropdown = dryRunRepositories(),
        options = within(selectDropdown).queryAllByRole('option'),
        createButton = dryRunCreateCSVButton();

      expect(selectDropdown).toBeInTheDocument();
      expect(options).toHaveLength(2);
      expect(options[0]).toHaveTextContent('Select a repository');
      expect(options[1]).toHaveTextContent('maven-central');
      expect(createButton).toBeInTheDocument();
    });

    it('sets disabled on the select dropdown when no format is selected', async function () {
      const {format, dryRunRepositories} = selectors;

      await renderView(EDITABLE_ITEM.name);

      const selectDropdown = dryRunRepositories(),
        formatSelectDropdown = format();

      expect(selectDropdown).not.toBeDisabled();

      await act(async () => userEvent.selectOptions(formatSelectDropdown, ''));

      expect(formatSelectDropdown).toHaveValue('');
      expect(selectDropdown).not.toBeInTheDocument();

      await act(async () =>
        userEvent.selectOptions(formatSelectDropdown, 'testformat')
      );

      expect(formatSelectDropdown).toHaveValue('testformat');
      expect(selectDropdown).not.toBeDisabled();
    });

    it('sets disabled on the button when no repository or criteria is selected', async function () {
      const {dryRunRepositories, dryRunCreateCSVButton, name, format,
        criteriaLastBlobUpdated, getCriteriaLastBlobUpdatedCheckbox, querySubmitButton} = selectors;

      await renderView();

      await TestUtils.changeField(name, EDITABLE_ITEM.name);
      await TestUtils.changeField(format, EDITABLE_ITEM.format);

      const selectDropdown = dryRunRepositories(),
        createButton = dryRunCreateCSVButton();

      expect(selectDropdown).toHaveValue('');
      expect(createButton).toHaveAttribute('aria-disabled', 'true');

      userEvent.selectOptions(selectDropdown, 'maven-central');

      expect(selectDropdown).toHaveValue('maven-central');
      expect(createButton).toHaveAttribute('aria-disabled', 'true');

      userEvent.click(querySubmitButton());

      expect(
        selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)
      ).toBeInTheDocument();

      userEvent.click(getCriteriaLastBlobUpdatedCheckbox());
      await TestUtils.changeField(criteriaLastBlobUpdated, `${EDITABLE_ITEM.criteriaLastBlobUpdated}`);

      expect(createButton).toHaveAttribute('aria-disabled', 'false');

      userEvent.selectOptions(selectDropdown, '');

      expect(selectDropdown).toHaveValue('');
      expect(createButton).toHaveAttribute('aria-disabled', 'true');
    });

    it('generates the csv url with the right params', async function() {
      const {dryRunRepositories, dryRunCreateCSVButton} = selectors;
      const expectedUrl = `https://testurl/service/rest/internal/cleanup-policies/preview/components/csv?
      repository=maven-central
      &name=${EDITABLE_ITEM.name}
      &criteriaLastBlobUpdated=${EDITABLE_ITEM.criteriaLastBlobUpdated}
      &criteriaLastDownloaded=${EDITABLE_ITEM.criteriaLastDownloaded}
      &criteriaReleaseType=${EDITABLE_ITEM.criteriaReleaseType}
      &criteriaAssetRegex=${EDITABLE_ITEM.criteriaAssetRegex}
      &criteriaRetain=${EDITABLE_ITEM.retain}
      &criteriaSortBy=${EDITABLE_ITEM.sortBy}`;

      await renderView(EDITABLE_ITEM.name);

      const selectDropdown = dryRunRepositories(),
          createButton = dryRunCreateCSVButton();

      userEvent.selectOptions(selectDropdown, 'maven-central');

      expect(selectDropdown).toHaveValue('maven-central');
      expect(createButton).toHaveAttribute('aria-disabled', 'false');
      //we add a replacement to put the expected url in a single line so attributes can match
      expect(createButton).toHaveAttribute('href', expectedUrl.replace(/\s+/g, ''));
    });
  });

  describe('Exclusion Criteria - retain-n', function() {
    const preReleaseFormats = ['maven2'];

    const MAVEN_ITEM = {
      ...EDITABLE_ITEM,
      name: 'maven-test',
      format: 'maven2',
      retain: 1,
      sortBy: 'version',
    };

    const DOCKER_ITEM = {
      ...EDITABLE_ITEM,
      name: 'docker-test',
      format: 'docker',
      retain: 10,
      criteriaReleaseType: null,
      sortBy: 'date',
    };

    beforeEach(() => {
      when(axios.get)
          .calledWith(URL.singleCleanupPolicyUrl(MAVEN_ITEM.name))
          .mockResolvedValue({
            data: MAVEN_ITEM,
          });

      when(axios.get)
          .calledWith(URL.singleCleanupPolicyUrl(DOCKER_ITEM.name))
          .mockResolvedValue({
            data: DOCKER_ITEM,
          });

      when(ExtJS.state().getValue)
          .calledWith('nexus.cleanup.maven2Retain')
          .mockReturnValue(true);

      when(ExtJS.state().getValue)
          .calledWith('nexus.cleanup.dockerRetain')
          .mockReturnValue(true);

      when(ExtJS.state().getValue)
          .calledWith(`${MAVEN_ITEM.format}.normalized.version.available`)
          .mockReturnValue(true);

      when(ExtJS.state().getValue)
          .calledWith(`${DOCKER_ITEM.format}.normalized.version.available`)
          .mockReturnValue(true);
    });

    it('Version criteria should be enabled only when Release type is Releases', async function() {
      const {
        format,
        criteriaVersion,
        getCriteriaVersionCheckbox,
        releaseType,
        versionAlertMessage,
        additionalCriteriaMessage, 
        getCriteriaLastBlobUpdatedCheckbox,
        criteriaLastBlobUpdated
      } = selectors;

      await renderView();

      expect(criteriaVersion()).not.toBeInTheDocument();

      await TestUtils.changeField(format, MAVEN_ITEM.format);

      expect(criteriaVersion()).toBeVisible();
      expect(criteriaVersion()).toBeDisabled();
      expect(getCriteriaVersionCheckbox()).toBeVisible();
      expect(getCriteriaVersionCheckbox()).toBeDisabled();
      expect(versionAlertMessage()).toBeInTheDocument();

      await TestUtils.changeField(releaseType, 'RELEASES');

      expect(additionalCriteriaMessage()).toBeInTheDocument();

      expect(versionAlertMessage()).not.toBeInTheDocument();
      expect(getCriteriaVersionCheckbox()).toBeDisabled();

      userEvent.click(getCriteriaLastBlobUpdatedCheckbox());
      await TestUtils.changeField(criteriaLastBlobUpdated, `${EDITABLE_ITEM.criteriaLastBlobUpdated}`);
      
      userEvent.click(getCriteriaVersionCheckbox());
      expect(criteriaVersion()).toBeVisible();
      expect(criteriaVersion()).toBeEnabled();

      await TestUtils.changeField(releaseType, 'PRERELEASES');

      expect(getCriteriaVersionCheckbox()).toBeDisabled();
      expect(versionAlertMessage()).toBeInTheDocument();
      expect(additionalCriteriaMessage()).not.toBeInTheDocument();

      await TestUtils.changeField(releaseType, '');

      expect(getCriteriaVersionCheckbox()).toBeDisabled();
      expect(versionAlertMessage()).toBeInTheDocument();

      await TestUtils.changeField(releaseType, 'RELEASES');

      expect(getCriteriaVersionCheckbox()).toBeEnabled();
      expect(versionAlertMessage()).not.toBeInTheDocument();
    });

    it('remembers the crteria version even if the release type is changed', async function () {
      const {releaseType, criteriaVersion, getCriteriaVersionCheckbox} = selectors;

      await renderView(EDITABLE_ITEM.name);

      await act(async () => userEvent.selectOptions(releaseType(), 'RELEASES'));
      expect(releaseType()).toHaveValue('RELEASES');

      expect(criteriaVersion()).toBeVisible();
      expect(getCriteriaVersionCheckbox()).toBeVisible();
      userEvent.click(getCriteriaVersionCheckbox());

      await TestUtils.changeField(criteriaVersion, '5');

      await act(async () => userEvent.selectOptions(releaseType(), 'PRERELEASES'));
      expect(releaseType()).toHaveValue('PRERELEASES');

      expect(criteriaVersion()).not.toHaveValue('5');

      expect(criteriaVersion()).toBeDisabled();

      await act(async () => userEvent.selectOptions(releaseType(), 'RELEASES'));
      expect(releaseType()).toHaveValue('RELEASES');

      userEvent.click(getCriteriaVersionCheckbox());

      expect(criteriaVersion()).toHaveValue('5');
    });

    it.each([DOCKER_ITEM, MAVEN_ITEM])
    ('renders the resolved data including the retain-n configuration', async function(item) {
      const {
        name,
        format,
        notes,
        releaseType,
        getCriteriaLastBlobUpdatedCheckbox,
        criteriaLastBlobUpdated,
        getCriteriaLastDownloadedCheckbox,
        criteriaLastDownloaded,
        getCriteriaAssetRegexCheckbox,
        criteriaAssetRegex,
        criteriaVersion,
        querySubmitButton
      } = selectors;

      await renderView(item.name);

      expect(name()).toHaveValue(item.name);
      expect(format()).toHaveValue(item.format);
      expect(notes()).toHaveValue(item.notes);
      expect(getCriteriaLastBlobUpdatedCheckbox()).toHaveClass('tm-checked');
      expect(criteriaLastBlobUpdated()).toHaveValue(
          item.criteriaLastBlobUpdated.toString()
      );
      expect(getCriteriaLastDownloadedCheckbox()).toHaveClass('tm-checked');
      expect(criteriaLastDownloaded()).toHaveValue(
          item.criteriaLastDownloaded.toString()
      );
      expect(getCriteriaAssetRegexCheckbox()).toHaveClass('tm-checked');
      expect(criteriaAssetRegex()).toHaveValue(item.criteriaAssetRegex);
      userEvent.click(querySubmitButton());
      expect(
          selectors.queryFormError(TestUtils.NO_CHANGES_MESSAGE)
      ).toBeInTheDocument();

      if (preReleaseFormats.includes(item.format)) {
        expect(releaseType()).toHaveValue(item.criteriaReleaseType);

        if (item.criteriaReleaseType === "RELEASES") {
          expect(
              screen.queryByText(LABELS.EXCLUSION_CRITERIA.ALERT)
          ).not.toBeInTheDocument();
        }
      }

      expect(criteriaVersion()).toHaveValue(item.retain.toString());
    })

    it.each([MAVEN_ITEM, DOCKER_ITEM])
    ('Version criteria is visible for the format', async function(item) {
      const {name, format, notes, versionAlertMessage} = selectors;

      await renderView();

      expect(versionAlertMessage()).not.toBeInTheDocument();

      await TestUtils.changeField(name, item.name);
      await TestUtils.changeField(format, item.format);
      await TestUtils.changeField(notes, item.notes);

      if (preReleaseFormats.includes(item.format)) {
        expect(versionAlertMessage()).toBeInTheDocument();
      }

    });

    it.each([MAVEN_ITEM, DOCKER_ITEM])
    ('Version criteria should be disabled when the normalized version task is running', async (item) => {
      when(ExtJS.state().getValue)
          .calledWith(`${item.format}.normalized.version.available`)
          .mockReturnValue(false);

      const {
        name,
        format,
        versionAlertMessage,
        normalizedVersionAlertMessage,
        getCriteriaVersionCheckbox,
        criteriaVersion,
      } = selectors;

      await renderView();

      expect(normalizedVersionAlertMessage()).not.toBeInTheDocument();

      await TestUtils.changeField(name, item.name);
      await TestUtils.changeField(format, item.format);

      expect(versionAlertMessage()).not.toBeInTheDocument();
      expect(normalizedVersionAlertMessage()).toBeInTheDocument();
      expect(getCriteriaVersionCheckbox()).toBeDisabled();
      expect(criteriaVersion()).toBeDisabled();
    });

    it.each([MAVEN_ITEM, DOCKER_ITEM])
    ('saves the retain-n values', async function(item) {
      const {criteriaVersion, saveButton} = selectors;

      await renderView(item.name);

      await TestUtils.changeField(criteriaVersion, '5');

      expect(saveButton()).not.toBeDisabled();

      await act(async () => userEvent.click(saveButton()));

      await waitFor(() =>
          expect(axios.put).toHaveBeenCalledWith(
              URL.singleCleanupPolicyUrl(item.name),
              {
                ...item,
                retain: '5'
              }
          )
      );
    });
  });
});
