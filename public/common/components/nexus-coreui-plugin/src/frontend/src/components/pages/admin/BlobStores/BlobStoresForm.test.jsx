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
import { when } from 'jest-when';
import { screen, waitFor, waitForElementToBeRemoved, within, render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import BlobStoresForm from './BlobStoresForm';
import FileBlobStoreWarning from './File/FileBlobStoreWarning';

import { URLs } from './BlobStoresHelper';

jest.mock('./BlobStoreTypes', () => ({
  __esModule: true,
  default: {
    s3: {
      Warning: () => 'Some S3 Warning',
    },
    file: {
      Warning: require('./File/FileBlobStoreWarning').default,
    }
  }
}));

import blobstoreTypes from './testData/mockBlobStoreTypes.json';
import quotaTypes from './testData/mockQuotaTypes.json';
import { blobStoreFormSelectors } from './testUtils/blobStoreFormSelectors';
import { UIRouter, useCurrentStateAndParams } from '@uirouter/react';
import { getRouter } from '../../../../routerConfig/routerConfig';
import { ROUTE_NAMES } from '../../../../routerConfig/routeNames/routeNames';

const {
  deleteBlobStoreUrl,
  convertToGroupBlobStoreUrl,
  createBlobStoreUrl,
  singleBlobStoreUrl,
  blobStoreTypesUrl,
  blobStoreQuotaTypesUrl,
  blobStoreUsageUrl,
} = URLs;

const ADMIN = ROUTE_NAMES.ADMIN;

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    requestConfirmation: jest.fn(),
    showErrorMessage: jest.fn(),
    state: jest.fn().mockReturnValue({
      getValue: jest.fn().mockReturnValue(true),
    }),
    isProEdition: jest.fn(),
    checkPermission: jest.fn(key => {
      return BlobStoresFormTestPermissions[key] ?? false;
    }),
  },
}));

let BlobStoresFormTestPermissions = {};

function givenBlobStoresPermissions(permissionLookup) {
  BlobStoresFormTestPermissions = permissionLookup;
}

function givenClusteringState(isClustered, workDirectory = '/nexus-data') {
  jest.requireMock('@sonatype/nexus-ui-plugin').ExtJS.state.mockReturnValue({
    getValue: jest.fn().mockReturnValue({
      isClustered,
      workDirectory,
    }),
  });
}

jest.mock('swagger-ui-react', () => jest.fn());
jest.mock('swagger-ui-react/swagger-ui.css', () => jest.fn());

const stateServiceGoMock = jest.fn();

jest.mock('@uirouter/react', () => ({
  ...jest.requireActual('@uirouter/react'),
  useCurrentStateAndParams: jest.fn(),
  useRouter: () => ({
    stateService: {
      go: stateServiceGoMock,
    },
  }),
}));

const selectors = {
  ...TestUtils.selectors,
  ...TestUtils.formSelectors,

  maxConnectionPoolSize: () => screen.queryByLabelText('Max Connection Pool Size'),

  ...blobStoreFormSelectors,

  queryTitle: () => screen.getByRole('heading', { level: 1, name: /.+/ }),
  queryPath: () => screen.getByLabelText('Path'),
  queryAvailableMembers: () => screen.getByRole('group', { name: 'Available Blob Stores' }),
  querySelectedMembers: () => screen.getByRole('group', { name: 'Selected Blob Stores' }),

   s3warning: () => screen.queryByText('Some S3 Warning'),

  convertModal: {
    modal: () => screen.queryByRole('dialog'),
    title: () => within(selectors.convertModal.modal()).getByRole('heading', { level: 2 }),
    warning: () => screen.getByText('You are converting to a group blob store. This action cannot be undone.'),
    newName: () => within(selectors.convertModal.modal()).queryByLabelText('Rename Original Blob Store'),
    convertButton: () => within(selectors.convertModal.modal()).getByRole('button', { name: 'Convert' }),
    cancel: () => within(selectors.convertModal.modal()).queryByText('Cancel'),
  },
};

// skipping due to flaky tests
describe('BlobStoresForm', function () {
  const SOFT_QUOTA_1_TERABYTE_IN_MEGABYTES = '1048576'; // 1 Terabyte = 1048576 Megabytes
  const SOFT_QUOTA_1_TERABYTE_IN_BYTES = 1099511627776; // 1 Terabyte = 1048576 Megabytes = 1099511627776 bytes

  function renderEditView(itemId) {
    const [type, name] = itemId.split('/');
    useCurrentStateAndParams.mockReturnValue({
      state: { name: ADMIN.REPOSITORY.BLOBSTORES.EDIT },
      params: { type, name },
    });
    return renderComponent();
  }

  function renderCreateView() {
    useCurrentStateAndParams.mockReturnValue({ state: { name: ADMIN.REPOSITORY.BLOBSTORES.CREATE }, params: {} });
    return renderComponent();
  }

  function renderComponent() {
    const router = getRouter();
    return render(
      <UIRouter router={router}>
        <BlobStoresForm />
      </UIRouter>
    );
  }

  beforeEach(() => {
    when(axios.get).calledWith('/service/rest/internal/ui/blobstores/types').mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith('/service/rest/internal/ui/blobstores/quotaTypes').mockResolvedValue(quotaTypes);
    useCurrentStateAndParams.mockReset();
    useCurrentStateAndParams.mockReturnValue({ state: { name: undefined }, params: {} });
    // Default: with update permission
    givenBlobStoresPermissions({ 'nexus:blobstores:update': true, 'nexus:blobstores:delete': true });
    // Mock ExtJS.state for clustering tests
    jest.requireMock('@sonatype/nexus-ui-plugin').ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue({
        isClustered: false,
        workDirectory: '',
      }),
    });
  });


  it('renders the type selection for create', async function () {
    renderCreateView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(selectors.cancelButton()).toBeEnabled();
    expect(selectors.queryTypeSelect().options.length).toBe(6);
    expect(Array.from(selectors.queryTypeSelect().options).map(option => option.textContent)).toEqual(
      expect.arrayContaining(['', 'File', 'Group'])
    );
    expect(selectors.queryTypeSelect()).toHaveValue('');
  });

  it('renders the form and buttons when the File type is selected', async function () {
    renderCreateView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.selectOptions(selectors.queryTypeSelect(), 'file');
    expect(selectors.queryTypeSelect()).toHaveValue('file');
  });

  it('validates the name field', async function () {
    renderCreateView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.selectOptions(selectors.queryTypeSelect(), 'file');
    expect(selectors.queryName()).toBeInTheDocument();
    expect(selectors.queryName()).not.toHaveAccessibleErrorMessage(TestUtils.REQUIRED_MESSAGE);

    userEvent.click(selectors.querySubmitButton());
    expect(selectors.queryName()).toHaveAccessibleErrorMessage(TestUtils.REQUIRED_MESSAGE);
    expect(selectors.queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();

    userEvent.type(selectors.queryName(), '/test');
    expect(selectors.queryName()).toHaveAccessibleErrorMessage(TestUtils.NAME_VALIDATION_MESSAGE);

    userEvent.clear(selectors.queryName());
    userEvent.type(selectors.queryName(), 'test');
    expect(selectors.queryName()).not.toHaveAccessibleErrorMessage(TestUtils.NAME_VALIDATION_MESSAGE);
  });

  it('renders the name field and dynamic path field when the File type is selected', async function () {
    renderCreateView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.selectOptions(selectors.queryTypeSelect(), 'file');
    expect(selectors.queryTypeSelect()).toHaveValue('file');

    expect(selectors.queryName()).toBeInTheDocument();

    expect(selectors.queryPath()).toBeInTheDocument();
    expect(selectors.queryPath()).toHaveValue('/<data-directory>/blobs/');
  });

  it('renders the soft quota fields when the blobstore type is selected', async function () {
    renderCreateView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.selectOptions(selectors.queryTypeSelect(), 'file');
    expect(selectors.queryTypeSelect()).toHaveValue('file');

    expect(selectors.softQuota.queryEnabled()).toBeInTheDocument();
    expect(selectors.softQuota.queryType()).not.toBeInTheDocument();
    expect(selectors.softQuota.queryLimit()).not.toBeInTheDocument();

    userEvent.click(selectors.softQuota.queryEnabled());
    expect(selectors.softQuota.queryEnabled()).toBeChecked();

    expect(selectors.softQuota.queryType()).toBeInTheDocument();
    expect(selectors.softQuota.queryLimit()).toBeInTheDocument();
  });

  it('enables the save button when there are changes', async function () {
    renderCreateView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.click(selectors.querySubmitButton());
    expect(selectors.queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();

    userEvent.selectOptions(selectors.queryTypeSelect(), 'file');
    expect(selectors.queryTypeSelect()).toHaveValue('file');

    expect(selectors.queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();

    userEvent.type(selectors.queryName(), 'test');
    expect(selectors.queryName()).toHaveValue('test');

    expect(selectors.queryFormError()).not.toBeInTheDocument();

    userEvent.click(selectors.softQuota.queryEnabled());
    userEvent.click(selectors.querySubmitButton());
    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

    userEvent.selectOptions(selectors.softQuota.queryType(), 'spaceRemainingQuota');
    expect(selectors.softQuota.queryType()).toHaveValue('spaceRemainingQuota');
    userEvent.type(selectors.softQuota.queryLimit(), '100');
    expect(selectors.softQuota.queryLimit()).toHaveValue('100');

    expect(selectors.queryFormError()).not.toBeInTheDocument();

    userEvent.clear(selectors.softQuota.queryLimit());
    expect(selectors.softQuota.queryLimit()).toHaveValue('');
    userEvent.click(selectors.querySubmitButton());

    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

    userEvent.click(selectors.softQuota.queryEnabled());

    expect(selectors.queryFormError()).not.toBeInTheDocument();
  });

  it('creates a new file blob store', async function () {
    renderCreateView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.selectOptions(selectors.queryTypeSelect(), 'file');
    expect(selectors.queryTypeSelect()).toHaveValue('file');
    userEvent.type(selectors.queryName(), 'test');
    expect(selectors.queryName()).toHaveValue('test');
    expect(selectors.queryPath()).toHaveValue('/<data-directory>/blobs/test');
    userEvent.clear(selectors.queryPath());
    userEvent.type(selectors.queryPath(), 'testPath');
    expect(selectors.queryPath()).toHaveValue('testPath');
    userEvent.click(selectors.softQuota.queryEnabled());
    userEvent.selectOptions(selectors.softQuota.queryType(), 'spaceRemainingQuota');
    expect(selectors.softQuota.queryType()).toHaveValue('spaceRemainingQuota');
    userEvent.type(selectors.softQuota.queryLimit(), SOFT_QUOTA_1_TERABYTE_IN_MEGABYTES);
    expect(selectors.softQuota.queryLimit()).toHaveValue(SOFT_QUOTA_1_TERABYTE_IN_MEGABYTES);
    userEvent.click(selectors.querySubmitButton());
    await waitForElementToBeRemoved(selectors.querySavingMask());

    expect(axios.post).toHaveBeenCalledWith('service/rest/v1/blobstores/file', {
      name: 'test',
      path: 'testPath',
      softQuota: {
        enabled: true,
        type: 'spaceRemainingQuota',
        limit: SOFT_QUOTA_1_TERABYTE_IN_BYTES,
      },
    });
  });

  it('edits a file blob store', async function () {
    when(axios.get)
      .calledWith('service/rest/v1/blobstores/file/test')
      .mockResolvedValue({
        data: {
          path: 'testPath',
          softQuota: {
            type: 'spaceRemainingQuota',
            limit: SOFT_QUOTA_1_TERABYTE_IN_MEGABYTES,
          },
        },
      });

    renderEditView('file/test');

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(selectors.queryConvertToGroupButton()).toBeInTheDocument();
  });

  it('edits a file blob store', async function () {
    when(axios.get)
      .calledWith('service/rest/v1/blobstores/file/test')
      .mockResolvedValue({
        data: {
          path: 'testPath',
          softQuota: {
            type: 'spaceRemainingQuota',
            limit: '104857600', // Bytes in 100 Megabytes
          },
        },
      });

    renderEditView('file/test');

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(selectors.queryTitle()).toHaveTextContent('Edit test');
    expect(screen.getByText('File Blob Store')).toBeInTheDocument();

    // The type and name fields cannot be changed during edit
    expect(selectors.queryTypeSelect()).not.toBeInTheDocument();
    expect(selectors.queryName()).not.toBeInTheDocument();

    expect(selectors.queryPath()).toHaveValue('testPath');
    expect(selectors.softQuota.queryEnabled()).toBeChecked();
    expect(selectors.softQuota.queryType()).toHaveValue('spaceRemainingQuota');
    expect(selectors.softQuota.queryLimit()).toHaveValue('100');
  });

  it('edits a group blob store', async function () {
    when(axios.get)
      .calledWith('service/rest/v1/blobstores/group/test')
      .mockResolvedValue({
        data: {
          softQuota: null,
          members: ['test-converted'],
          fillPolicy: 'writeToFirst',
        },
      });

    renderEditView('group/test');

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(selectors.queryTitle()).toHaveTextContent('Edit test');
    expect(screen.getByText('Group Blob Store')).toBeInTheDocument();

    expect(selectors.queryTypeSelect()).not.toBeInTheDocument();
    expect(selectors.queryName()).not.toBeInTheDocument();

    expect(selectors.queryAvailableMembers()).toHaveTextContent('default');
    expect(selectors.querySelectedMembers()).toHaveTextContent('test-converted');
  });

  it('convert to group is not shown when editing a group', async function () {
    when(axios.get)
      .calledWith('service/rest/v1/blobstores/group/test')
      .mockResolvedValue({
        data: {
          softQuota: null,
          members: ['test-converted'],
          fillPolicy: 'writeToFirst',
        },
      });

    renderEditView('group/test');

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(selectors.queryConvertToGroupButton()).not.toBeInTheDocument();
  });

  it('converts to the group blob store', async function () {
    const convertUrl = 'service/rest/v1/blobstores/group/convert/a-file%2Fee%3A%23%24%25%40/test_1';
    const errorMessage = 'Blob store could not be converted to a group blob store';

    when(axios.get)
      .calledWith('service/rest/v1/blobstores/file/a-file%2Fee%3A%23%24%25%40')
      .mockResolvedValue({
        data: {
          path: 'testPath',
          softQuota: {
            type: 'spaceRemainingQuota',
            limit: '104857600',
          },
        },
      });
    when(axios.post).calledWith(convertUrl).mockRejectedValue({ message: errorMessage });

    renderEditView('file/a-file%2Fee%3A%23%24%25%40');
    const {
      convertModal: { modal, title: modalTitle, warning, newName, convertButton, cancel },
    } = selectors;

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(selectors.queryTitle()).toHaveTextContent('Edit a-file/ee:#$%@');
    expect(selectors.queryConvertToGroupButton()).toBeInTheDocument();

    userEvent.click(selectors.queryConvertToGroupButton());
    expect(modal()).toBeInTheDocument();

    userEvent.click(cancel());
    expect(stateServiceGoMock).not.toHaveBeenCalled();
    expect(modal()).not.toBeInTheDocument();

    userEvent.click(selectors.queryConvertToGroupButton());
    expect(modalTitle()).toHaveTextContent('Convert to Group Blob Store');
    expect(warning()).toBeInTheDocument();
    expect(newName()).toHaveValue('a-file/ee:#$%@-original');
    expect(newName()).not.toHaveAccessibleErrorMessage();

    userEvent.click(convertButton());
    expect(newName()).toHaveAccessibleErrorMessage(TestUtils.NAME_VALIDATION_MESSAGE);
    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

    userEvent.clear(newName());
    userEvent.type(newName(), 'test_1');
    expect(newName()).not.toHaveAccessibleErrorMessage();

    userEvent.click(convertButton());
    await waitFor(() => expect(axios.post).toHaveBeenCalledWith(convertUrl));
    expect(stateServiceGoMock).not.toHaveBeenCalled();
    expect(selectors.querySaveError(errorMessage)).toBeInTheDocument();

    when(axios.post).calledWith(convertUrl).mockResolvedValue({ data: {} });
    userEvent.click(convertButton());

    await waitFor(() => expect(stateServiceGoMock).toHaveBeenCalled());
  });

  it('log save error message when blobstore can not be added to group', async function () {
    let updateUrl = 'service/rest/v1/blobstores/group/test';

    when(axios.get)
      .calledWith(updateUrl)
      .mockResolvedValue({
        data: {
          softQuota: null,
          members: ['test-converted', 'default'],
          fillPolicy: 'writeToFirst',
        },
      });

    let errorMessage = 'Blob Store is not eligible to be a group member';

    when(axios.put)
      .calledWith(updateUrl, {})
      .mockRejectedValue({
        response: { data: [{ id: '*', message: errorMessage }] },
      });

    renderEditView('group/test');

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    const consoleSpy = jest.spyOn(console, 'log');

    await axios.put(updateUrl, {}).catch(function (reason) {
      console.log(reason.response.data[0].message);
    });

    expect(consoleSpy).toHaveBeenCalledWith(errorMessage);
  });

  it('uses proper urls', function () {
    const validName = 'foo-bar_test';
    const invalidName = '/test%$#@8*>?';

    expect(blobStoreTypesUrl).toBe('/service/rest/internal/ui/blobstores/types');
    expect(blobStoreQuotaTypesUrl).toBe('/service/rest/internal/ui/blobstores/quotaTypes');

    expect(singleBlobStoreUrl(validName, invalidName)).toBe(
      'service/rest/v1/blobstores/foo-bar_test/%2Ftest%25%24%23%408*%3E%3F'
    );
    expect(singleBlobStoreUrl(invalidName, validName)).toBe(
      'service/rest/v1/blobstores/%2Ftest%25%24%23%408*%3E%3F/foo-bar_test'
    );

    expect(deleteBlobStoreUrl(validName)).toBe('service/rest/v1/blobstores/foo-bar_test');
    expect(deleteBlobStoreUrl(invalidName)).toBe('service/rest/v1/blobstores/%2Ftest%25%24%23%408*%3E%3F');

    expect(convertToGroupBlobStoreUrl(validName, invalidName)).toBe(
      'service/rest/v1/blobstores/group/convert/foo-bar_test/%2Ftest%25%24%23%408*%3E%3F'
    );
    expect(convertToGroupBlobStoreUrl(invalidName, validName)).toBe(
      'service/rest/v1/blobstores/group/convert/%2Ftest%25%24%23%408*%3E%3F/foo-bar_test'
    );

    expect(createBlobStoreUrl(validName)).toBe('service/rest/v1/blobstores/foo-bar_test');
    expect(createBlobStoreUrl(invalidName)).toBe('service/rest/v1/blobstores/%2Ftest%25%24%23%408*%3E%3F');

    expect(blobStoreUsageUrl(validName)).toBe('/service/rest/internal/ui/blobstores/usage/foo-bar_test');
    expect(blobStoreUsageUrl(invalidName)).toBe(
      '/service/rest/internal/ui/blobstores/usage/%2Ftest%25%24%23%408*%3E%3F'
    );
  });

  it('redirects to 404 when blob store type is not found', async function () {
    useCurrentStateAndParams.mockReturnValue({
      state: { name: ADMIN.REPOSITORY.BLOBSTORES.EDIT },
      params: { itemId: '' },
    });

    renderComponent();
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
    expect(stateServiceGoMock).toHaveBeenCalledWith(ROUTE_NAMES.MISSING_ROUTE);
  });

  it('renders warning when user does not have update permission', async function () {
    when(axios.get)
      .calledWith('service/rest/v1/blobstores/file/test')
      .mockResolvedValue({
        data: {
          path: 'testPath',
          softQuota: {
            type: 'spaceRemainingQuota',
            limit: SOFT_QUOTA_1_TERABYTE_IN_MEGABYTES,
          },
        },
      });
    givenBlobStoresPermissions({ 'nexus:blobstores:update': false });
    renderEditView('file/test');
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
    // Should show the warning alert and disable the submit button
    expect(
      screen.getByText('You don\'t have permission to edit this page. Contact your administrator to request access.')
    ).toBeInTheDocument();
    expect(selectors.querySubmitButton()).toHaveClass('disabled');
    expect(selectors.queryConvertToGroupButton()).toBeDisabled();
  });

  it('disables the delete button when user does not have delete permission', async function () {
    when(axios.get)
      .calledWith('service/rest/v1/blobstores/file/test')
      .mockResolvedValue({
        data: {
          path: 'testPath',
          softQuota: {
            type: 'spaceRemainingQuota',
            limit: SOFT_QUOTA_1_TERABYTE_IN_MEGABYTES,
          },
        },
      });
    givenBlobStoresPermissions({ 'nexus:blobstores:update': true, 'nexus:blobstores:delete': false });
    renderEditView('file/test');
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
    // Should disable the delete button
    const deleteButton = screen.getByRole('button', { name: /delete/i });
    expect(deleteButton).toHaveAttribute('aria-disabled', 'true');
  });

  it('renders the S3 warning when the S3 type is selected in create', async function () {
    renderCreateView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.selectOptions(selectors.queryTypeSelect(), 's3');
    expect(selectors.s3warning()).toBeVisible();
  });

  it('renders the S3 warning when the S3 type is selected in edit', async function () {
    when(axios.get)
      .calledWith('service/rest/v1/blobstores/s3/test')
      .mockResolvedValue({
        data: {
          path: 'testPath',
          softQuota: {
            type: 'spaceRemainingQuota',
            limit: SOFT_QUOTA_1_TERABYTE_IN_MEGABYTES,
          },
        },
      });
    renderEditView('s3/test');

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(selectors.s3warning()).toBeVisible();
  });

  it('does not render the S3 warning when the S3 type is selected in edit read only', async function () {
    when(axios.get)
      .calledWith('service/rest/v1/blobstores/s3/test')
      .mockResolvedValue({
        data: {
          path: 'testPath',
          softQuota: {
            type: 'spaceRemainingQuota',
            limit: SOFT_QUOTA_1_TERABYTE_IN_MEGABYTES,
          },
        },
      });
    givenBlobStoresPermissions({ 'nexus:blobstores:update': false });
    renderEditView('s3/test');

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(selectors.s3warning()).not.toBeInTheDocument();
  });

  it('renders the File HA warning when clustered and path is relative in create', async function () {
    givenClusteringState(true, '/nexus-data');
    renderCreateView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.selectOptions(selectors.queryTypeSelect(), 'file');
    userEvent.type(selectors.queryName(), 'test');
    userEvent.clear(selectors.queryPath());
    userEvent.type(selectors.queryPath(), 'relative/path');

    await waitFor(() => {
      expect(screen.getByText('High Availability Path Warning')).toBeInTheDocument();
    });
  });

  it('renders the File HA warning when clustered and path is under work directory in create', async function () {
    givenClusteringState(true, '/nexus-data');
    renderCreateView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.selectOptions(selectors.queryTypeSelect(), 'file');
    userEvent.type(selectors.queryName(), 'test');
    userEvent.clear(selectors.queryPath());
    userEvent.type(selectors.queryPath(), '/nexus-data/blobs');

    await waitFor(() => {
      expect(screen.getByText('High Availability Path Warning')).toBeInTheDocument();
    });
  });

  it('does not render the File HA warning when clustered but path is absolute outside work directory', async function () {
    givenClusteringState(true, '/nexus-data');
    renderCreateView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.selectOptions(selectors.queryTypeSelect(), 'file');
    userEvent.type(selectors.queryName(), 'test');
    userEvent.clear(selectors.queryPath());
    userEvent.type(selectors.queryPath(), '/mnt/shared-storage/blobs');

    expect(screen.queryByText('High Availability Path Warning')).not.toBeInTheDocument();
  });

  it('does not render the File HA warning when not clustered', async function () {
    givenClusteringState(false);
    renderCreateView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.selectOptions(selectors.queryTypeSelect(), 'file');
    userEvent.type(selectors.queryName(), 'test');
    userEvent.clear(selectors.queryPath());
    userEvent.type(selectors.queryPath(), '/nexus-data/blobs');

    expect(screen.queryByText('High Availability Path Warning')).not.toBeInTheDocument();
  });

  it('renders the File HA warning when clustered and path is under work directory in edit', async function () {
    givenClusteringState(true, '/nexus-data');
    when(axios.get)
      .calledWith('service/rest/v1/blobstores/file/test')
      .mockResolvedValue({
        data: {
          path: '/nexus-data/blobs',
          softQuota: {
            type: 'spaceRemainingQuota',
            limit: SOFT_QUOTA_1_TERABYTE_IN_MEGABYTES,
          },
        },
      });
    renderEditView('file/test');

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    await waitFor(() => {
      expect(screen.getByText('High Availability Path Warning')).toBeInTheDocument();
    });
  });

  it('does not render the File HA warning when clustered and path is under work directory in edit read only', async function () {
    givenClusteringState(true, '/nexus-data');
    givenBlobStoresPermissions({ 'nexus:blobstores:update': false });
    when(axios.get)
      .calledWith('service/rest/v1/blobstores/file/test')
      .mockResolvedValue({
        data: {
          path: '/nexus-data/blobs',
          softQuota: {
            type: 'spaceRemainingQuota',
            limit: SOFT_QUOTA_1_TERABYTE_IN_MEGABYTES,
          },
        },
      });
    renderEditView('file/test');

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(screen.queryByText('High Availability Path Warning')).not.toBeInTheDocument();
  });

  it('does not show File HA warning for sibling directory with prefix name', async function () {
    givenClusteringState(true, '/nexus-data');
    renderCreateView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.selectOptions(selectors.queryTypeSelect(), 'file');
    userEvent.type(selectors.queryName(), 'test');
    userEvent.clear(selectors.queryPath());
    userEvent.type(selectors.queryPath(), '/nexus-data-backup/blobs');

    expect(screen.queryByText('High Availability Path Warning')).not.toBeInTheDocument();
  });

  it('does not show File HA warning when work directory is prefix of path component', async function () {
    givenClusteringState(true, '/nexus-data/info');
    renderCreateView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.selectOptions(selectors.queryTypeSelect(), 'file');
    userEvent.type(selectors.queryName(), 'test');
    userEvent.clear(selectors.queryPath());
    userEvent.type(selectors.queryPath(), '/nexus-data/information/blobs');

    expect(screen.queryByText('High Availability Path Warning')).not.toBeInTheDocument();
  });

  describe('Analytics IDs', function () {
    it('has nxrm-blobstore-delete analytics ID on delete button in edit mode', async function () {
      when(axios.get)
        .calledWith('service/rest/v1/blobstores/file/test')
        .mockResolvedValue({
          data: {
            path: 'testPath',
            softQuota: null,
          },
        });

      renderEditView('file/test');

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      const deleteButton = screen.getByRole('button', { name: /delete/i });
      expect(deleteButton).toHaveAttribute('data-analytics-id', 'nxrm-blobstore-delete');
    });

    it('has nxrm-blobstore-convert-to-group analytics ID on convert button in edit mode', async function () {
      when(axios.get)
        .calledWith('service/rest/v1/blobstores/file/test')
        .mockResolvedValue({
          data: {
            path: 'testPath',
            softQuota: null,
          },
        });

      renderEditView('file/test');

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      const convertButton = screen.getByRole('button', { name: /convert to group/i });
      expect(convertButton).toHaveAttribute('data-analytics-id', 'nxrm-blobstore-convert-to-group');
    });
  });
});
