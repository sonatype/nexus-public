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
import {when} from 'jest-when';
import {screen, waitForElementToBeRemoved, within, waitFor, act} from '@testing-library/react'
import userEvent from '@testing-library/user-event';

import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import S3BlobStoreSettings from './S3/S3BlobStoreSettings';
import S3BlobStoreWarning from './S3/S3BlobStoreWarning';
import BlobStoresForm from './BlobStoresForm';

import {URLs} from './BlobStoresHelper';

const {
  deleteBlobStoreUrl,
  convertToGroupBlobStoreUrl,
  createBlobStoreUrl,
  singleBlobStoreUrl,
  blobStoreTypesUrl,
  blobStoreQuotaTypesUrl,
  blobStoreUsageUrl,
} = URLs;

// Include the blob stores types on the window
import '../../../../index';

jest.mock('axios', () => ({
  ...jest.requireActual('axios'), // Use most functions from actual axios
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn()
}));

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    requestConfirmation: jest.fn(),
    showErrorMessage: jest.fn()
  }
}));

jest.mock("swagger-ui-react", () => jest.fn());
jest.mock("swagger-ui-react/swagger-ui.css", () => jest.fn());

const blobstoreTypes = {
  data: [{
    "id": "file",
    "name": "File",
    "fields": [
      {
        "helpText": "An absolute path or a path relative to <data-directory>/blobs",
        "id": "path",
        "regexValidation": null,
        "required": true,
        "disabled": false,
        "readOnly": false,
        "label": "Path",
        "attributes": {
          "tokenReplacement": "/<data-directory>/blobs/${name}",
          "long": true
        },
        "type": "string",
        "allowAutocomplete": false
      }
    ]
  }, {
    "id": "group",
    "name": "Group",
    "fields": [
      {
        "id": "members",
        "required": true,
        "label": "Members",
        "initialValue": null,
        "attributes": {
          "toTitle": "Selected Blob Stores",
          "fromTitle": "Available Blob Stores",
          "buttons": ["up", "add", "remove", "down"],
          "options": ["default", "test", "test-converted"]
        },
        "type": "itemselect",
        "allowAutocomplete": false
      }, {
        "id": "fillPolicy",
        "required": true,
        "label": "Fill Policy",
        "initialValue": null,
        "attributes": {
          "options": ["Round Robin", "Write to First"]
        },
        "type": "combobox"
      }
    ]
  },
    {
      "id": "s3",
      "name": "S3"
    },
    {
      "id": "azure",
      "name": "Azure Cloud Storage"
    },
    {
      "id": "google",
      "name": "Google Cloud Platform"
    }
  ]
};

const quotaTypes = {
  data: [
    {
      "id": "spaceRemainingQuota",
      "name": "Space Remaining"
    }, {
      "id": "spaceUsedQuota",
      "name": "Space Used"
    }
  ]
};

const selectors = {
  ...TestUtils.selectors,
  ...TestUtils.formSelectors,
  maxConnectionPoolSize: () => screen.queryByLabelText('Max Connection Pool Size'),
  cancelButton: () => screen.getByText('Cancel'),
  getSoftQuota: () => within(screen.getByRole('group', {name: 'Soft Quota'})).getByLabelText('Enabled'),
  getUsePathStyle: () => within(screen.getByRole('group', {name: 'Use path-style access'})).getByLabelText(
      'Setting this flag will result in path-style access being used for all requests'),
  convertModal: {
    modal: () => screen.queryByRole('dialog'),
    title: () => within(selectors.convertModal.modal()).getByRole('heading', {level: 2}),
    warning: () => screen.getByText('You are converting to a group blob store. This action cannot be undone.'),
    newName: () => within(selectors.convertModal.modal()).queryByLabelText('Rename Original Blob Store'),
    convertButton: () => within(selectors.convertModal.modal()).getByRole('button', {name: 'Convert'}),
    cancel: () => within(selectors.convertModal.modal()).queryByText('Cancel'),
  },
}

describe('BlobStoresForm', function() {
  const onDone = jest.fn();
  const confirm = Promise.resolve();
  const SOFT_QUOTA_1_TERABYTE_IN_MEGABYTES = '1048576'; // 1 Terabyte = 1048576 Megabytes
  const SOFT_QUOTA_1_TERABYTE_IN_BYTES = 1099511627776; // 1 Terabyte = 1048576 Megabytes = 1099511627776 bytes

  window.ReactComponents = {S3BlobStoreSettings, S3BlobStoreWarning};

  function render(itemId) {
    return TestUtils.render(<BlobStoresForm itemId={itemId || ''} onDone={onDone}/>,
        ({getByRole, getByLabelText, queryByLabelText, getByText, queryByText}) => ({
          title: () => getByRole('heading', {level: 1}),
          typeSelect: () => queryByLabelText('Type'),
          name: () => queryByLabelText('Name'),
          path: () => getByLabelText('Path'),
          region: () => getByLabelText('Region'),
          bucket: () => getByLabelText('Bucket'),
          credentialAuthentication: () => getByLabelText('Credential JSON File'),
          fileInput: () => getByLabelText('JSON Credential File Path'),
          prefix: () => getByLabelText('Prefix'),
          expiration: () => getByLabelText('Expiration Days'),
          accessKeyId: () => getByLabelText('Access Key ID'),
          secretAccessKey: () => getByLabelText('Secret Access Key'),
          assumeRole: () => getByLabelText('Assume Role ARN (Optional)'),
          sessionToken: () => getByLabelText('Session Token ARN (Optional)'),
          encryptionType: () => getByLabelText('Encryption Type'),
          kmsKeyId: () => getByLabelText('KMS Key ID (Optional)'),
          endpointURL: () => getByLabelText('Endpoint URL'),
          signatureVersion: () => getByLabelText('Signature Version'),
          availableMembers: () => getByRole('group', {name: 'Available Blob Stores'}),
          selectedMembers: () => getByRole('group', {name: 'Selected Blob Stores'}),
          softQuotaType: () => queryByLabelText('Constraint Type'),
          softQuotaLimit: () => queryByLabelText('Constraint Limit (in MB)'),
          spaceUsedQuotaLabel: () => getByText('Space Used'),
          convertToGroup: () => queryByText('Convert to Group'),
          azureAccountName: () => queryByLabelText('Account Name'),
          azureContainerName: () => queryByLabelText('Container Name')
        }));
  }

  it('renders the loading spinner', async function() {
    axios.get.mockReturnValue(new Promise(() => {}));

    const {loadingMask} = render();

    expect(loadingMask()).toBeInTheDocument();
  });

  it('renders the type selection for create', async function() {
    when(axios.get).calledWith('/service/rest/internal/ui/blobstores/types').mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith('/service/rest/internal/ui/blobstores/quotaTypes').mockResolvedValue(quotaTypes);

    const {loadingMask, typeSelect} = render();

    await waitForElementToBeRemoved(loadingMask);

    expect(selectors.cancelButton()).toBeEnabled();
    expect(typeSelect().options.length).toBe(6);
    expect(Array.from(typeSelect().options).map(option => option.textContent)).toEqual(expect.arrayContaining([
        '',
        'File',
        'Group'
    ]));
    expect(typeSelect()).toHaveValue('');
  });

  it('renders the form and buttons when the File type is selected', async function() {
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/types').mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/quotaTypes').mockResolvedValue(quotaTypes);

    const {loadingMask, typeSelect} = render();

    await waitForElementToBeRemoved(loadingMask);

    userEvent.selectOptions(typeSelect(), 'file');
    expect(typeSelect()).toHaveValue('file');
  });

  it('renders the form and buttons when the S3 type is selected', async function() {
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/types').mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/quotaTypes').mockResolvedValue(quotaTypes);

    const {loadingMask, typeSelect} = render();

    await waitForElementToBeRemoved(loadingMask);

    userEvent.selectOptions(typeSelect(), 's3');
    expect(typeSelect()).toHaveValue('s3');
  });

  it('validates the name field', async function() {
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/types').mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/quotaTypes').mockResolvedValue(quotaTypes);

    const {loadingMask, typeSelect, name} = render();

    await waitForElementToBeRemoved(loadingMask);

    userEvent.selectOptions(typeSelect(), 'file');
    expect(name()).toBeInTheDocument();
    expect(name()).not.toHaveErrorMessage(TestUtils.REQUIRED_MESSAGE);

    userEvent.click(selectors.querySubmitButton());
    expect(name()).toHaveErrorMessage(TestUtils.REQUIRED_MESSAGE);
    expect(selectors.queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();

    userEvent.type(name(), '/test');
    expect(name()).toHaveErrorMessage(TestUtils.NAME_VALIDATION_MESSAGE);

    userEvent.clear(name());
    userEvent.type(name(), 'test');
    expect(name()).not.toHaveErrorMessage(TestUtils.NAME_VALIDATION_MESSAGE);
  });

  it('renders S3 specific form fields', async function() {
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/types').mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/quotaTypes').mockResolvedValue(quotaTypes);

    const {
      loadingMask,
      typeSelect,
      region,
      bucket,
      prefix,
      expiration,
      accessKeyId,
      secretAccessKey,
      assumeRole,
      sessionToken,
      encryptionType,
      kmsKeyId,
      endpointURL,
      signatureVersion
    } = render();

    await waitForElementToBeRemoved(loadingMask);

    userEvent.selectOptions(typeSelect(), 's3');
    expect(typeSelect()).toHaveValue('s3');

    expect(region()).toBeInTheDocument();
    expect(bucket()).toBeInTheDocument();
    expect(prefix()).toBeInTheDocument();
    expect(expiration()).toBeInTheDocument();
    expect(accessKeyId()).toBeInTheDocument();
    expect(secretAccessKey()).toBeInTheDocument();
    expect(assumeRole()).toBeInTheDocument();
    expect(sessionToken()).toBeInTheDocument();
    expect(endpointURL()).toBeInTheDocument();
    expect(encryptionType()).toBeInTheDocument();
    expect(kmsKeyId()).toBeInTheDocument();
    expect(signatureVersion()).toBeInTheDocument();
    expect(selectors.getUsePathStyle()).toBeInTheDocument();
  });

  it('enables the save button when the minimum fields are filled in S3 blobstore', async function() {
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/types').mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/quotaTypes').mockResolvedValue(quotaTypes);

    const {
      name,
      loadingMask,
      typeSelect,
      expiration,
      bucket,
      accessKeyId,
      secretAccessKey,
      endpointURL,
    } = render();

    await waitForElementToBeRemoved(loadingMask);

    userEvent.click(selectors.querySubmitButton());
    expect(selectors.queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();

    userEvent.selectOptions(typeSelect(), 's3');
    expect(typeSelect()).toHaveValue('s3');
    expect(expiration()).toHaveValue('3');

    expect(selectors.queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();

    userEvent.type(name(), 'test');
    expect(name()).toHaveValue('test');

    userEvent.type(bucket(), 'bucket');
    expect(bucket()).toHaveValue('bucket');
    expect(selectors.queryFormError()).not.toBeInTheDocument();

    userEvent.type(accessKeyId(), 'someAccessKey');
    expect(accessKeyId()).toHaveValue('someAccessKey');
    userEvent.click(selectors.querySubmitButton());
    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

    userEvent.type(secretAccessKey(), 'SomeSecretAccessKey');
    expect(secretAccessKey()).toHaveValue('SomeSecretAccessKey');
    expect(selectors.queryFormError()).not.toBeInTheDocument();

    userEvent.type(endpointURL(), 'invalidURL');
    expect(endpointURL()).toHaveValue('invalidURL');
    userEvent.click(selectors.querySubmitButton());
    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

    userEvent.clear(endpointURL());
    expect(endpointURL()).toHaveValue('');
    userEvent.type(endpointURL(), 'http://www.fakeurl.com');
    expect(endpointURL()).toHaveValue('http://www.fakeurl.com');
    expect(selectors.queryFormError()).not.toBeInTheDocument();
  });

  it('renders the name field and dynamic path field when the File type is selected', async function() {
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/types').mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/quotaTypes').mockResolvedValue(quotaTypes);

    const {loadingMask, typeSelect, name, path} = render();

    await waitForElementToBeRemoved(loadingMask);

    userEvent.selectOptions(typeSelect(), 'file');
    expect(typeSelect()).toHaveValue('file');

    expect(name()).toBeInTheDocument();

    expect(path()).toBeInTheDocument();
    expect(path()).toHaveValue('/<data-directory>/blobs/');
  });

  it('renders the soft quota fields when the blobstore type is selected', async function() {
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/types').mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/quotaTypes').mockResolvedValue(quotaTypes);

    const {loadingMask, typeSelect, softQuotaType, softQuotaLimit} = render();

    await waitForElementToBeRemoved(loadingMask);

    userEvent.selectOptions(typeSelect(), 'file');
    expect(typeSelect()).toHaveValue('file');

    expect(selectors.getSoftQuota()).toBeInTheDocument();
    expect(softQuotaType()).not.toBeInTheDocument();
    expect(softQuotaLimit()).not.toBeInTheDocument();

    userEvent.click(selectors.getSoftQuota());

    expect(softQuotaType()).toBeInTheDocument();
    expect(softQuotaLimit()).toBeInTheDocument();
  });

  it('enables the save button when there are changes', async function() {
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/types').mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/quotaTypes').mockResolvedValue(quotaTypes);

    const {loadingMask, typeSelect, name, softQuotaType, softQuotaLimit} = render();

    await waitForElementToBeRemoved(loadingMask);

    userEvent.click(selectors.querySubmitButton());
    expect(selectors.queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();

    userEvent.selectOptions(typeSelect(), 'file');
    expect(typeSelect()).toHaveValue('file');

    expect(selectors.queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();

    userEvent.type(name(), 'test');
    expect(name()).toHaveValue('test');

    expect(selectors.queryFormError()).not.toBeInTheDocument();

    userEvent.click(selectors.getSoftQuota());
    userEvent.click(selectors.querySubmitButton());
    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

    userEvent.selectOptions(softQuotaType(), 'spaceRemainingQuota');
    expect(softQuotaType()).toHaveValue('spaceRemainingQuota');
    userEvent.type(softQuotaLimit(), '100');
    expect(softQuotaLimit()).toHaveValue('100');

    expect(selectors.queryFormError()).not.toBeInTheDocument();

    userEvent.clear(softQuotaLimit());
    expect(softQuotaLimit()).toHaveValue('');
    userEvent.click(selectors.querySubmitButton());

    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

    userEvent.click(selectors.getSoftQuota());

    expect(selectors.queryFormError()).not.toBeInTheDocument();
  });

  it('creates a new file blob store', async function() {
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/types').mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/quotaTypes').mockResolvedValue(quotaTypes);

    const {loadingMask, typeSelect, name, path, softQuotaType, softQuotaLimit} = render();

    await waitForElementToBeRemoved(loadingMask);

    userEvent.selectOptions(typeSelect(), 'file');
    expect(typeSelect()).toHaveValue('file');
    userEvent.type(name(), 'test');
    expect(name()).toHaveValue('test');
    expect(path()).toHaveValue('/<data-directory>/blobs/test');
    userEvent.clear(path());
    userEvent.type(path(), 'testPath');
    expect(path()).toHaveValue('testPath');
    userEvent.click(selectors.getSoftQuota());
    userEvent.selectOptions(softQuotaType(), 'spaceRemainingQuota');
    expect(softQuotaType()).toHaveValue('spaceRemainingQuota');
    userEvent.type(softQuotaLimit(), SOFT_QUOTA_1_TERABYTE_IN_MEGABYTES);
    expect(softQuotaLimit()).toHaveValue(SOFT_QUOTA_1_TERABYTE_IN_MEGABYTES);
    userEvent.click(selectors.querySubmitButton());

    expect(axios.post).toHaveBeenCalledWith(
        'service/rest/v1/blobstores/file',
        {
          name: 'test',
          path: 'testPath',
          softQuota: {
            enabled: true,
            type: 'spaceRemainingQuota',
            limit: SOFT_QUOTA_1_TERABYTE_IN_BYTES
          }
        }
    );
  });

  it('creates a new S3 blob store', async function() {
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/types').mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/quotaTypes').mockResolvedValue(quotaTypes);

    const {
      name,
      loadingMask,
      typeSelect,
      expiration,
      bucket,
      accessKeyId,
      secretAccessKey,
      endpointURL,
      softQuotaType,
      softQuotaLimit,
      spaceUsedQuotaLabel
    } = render();

    await waitForElementToBeRemoved(loadingMask);

    userEvent.selectOptions(typeSelect(), 'S3');
    expect(typeSelect()).toHaveValue('s3');
    expect(expiration()).toHaveValue('3');
    userEvent.click(selectors.querySubmitButton());

    expect(selectors.queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();

    userEvent.type(name(), 'test');
    expect(name()).toHaveValue('test');

    userEvent.type(bucket(), 'bucket');
    expect(bucket()).toHaveValue('bucket');
    expect(selectors.queryFormError()).not.toBeInTheDocument();

    userEvent.type(accessKeyId(), 'someAccessKey');
    expect(accessKeyId()).toHaveValue('someAccessKey');
    userEvent.click(selectors.querySubmitButton());
    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

    userEvent.type(secretAccessKey(), 'SomeSecretAccessKey');
    expect(secretAccessKey()).toHaveValue('SomeSecretAccessKey');
    expect(selectors.queryFormError()).not.toBeInTheDocument();

    userEvent.type(endpointURL(), 'invalidURL');
    expect(endpointURL()).toHaveValue('invalidURL');
    userEvent.click(selectors.querySubmitButton());
    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

    userEvent.clear(endpointURL());
    expect(endpointURL()).toHaveValue('');
    userEvent.type(endpointURL(), 'http://www.fakeurl.com');
    expect(endpointURL()).toHaveValue('http://www.fakeurl.com');
    expect(selectors.queryFormError()).not.toBeInTheDocument();

    userEvent.type(selectors.maxConnectionPoolSize(), '0');
    expect(selectors.maxConnectionPoolSize()).toHaveErrorMessage('The minimum value for this field is 1');
    userEvent.clear(selectors.maxConnectionPoolSize());
    userEvent.type(selectors.maxConnectionPoolSize(), '2000000000');
    expect(selectors.maxConnectionPoolSize()).toHaveErrorMessage('The maximum value for this field is 1000000000');
    userEvent.clear(selectors.maxConnectionPoolSize());
    userEvent.type(selectors.maxConnectionPoolSize(), '1');
    expect(selectors.maxConnectionPoolSize()).not.toHaveErrorMessage(expect.anything());

    userEvent.click(selectors.getSoftQuota());
    expect(softQuotaType()).not.toBeInTheDocument();
    expect(spaceUsedQuotaLabel()).toBeInTheDocument();
    userEvent.type(softQuotaLimit(), '1');

    userEvent.click(selectors.querySubmitButton());

    expect(axios.post).toHaveBeenCalledWith(
        'service/rest/v1/blobstores/s3',
        {
          name: 'test',
          bucketConfiguration: {
            bucket: { region: 'DEFAULT', name: 'bucket', prefix: '', expiration: '3' },
            bucketSecurity: {
              accessKeyId: 'someAccessKey',
              secretAccessKey: 'SomeSecretAccessKey'
            },
            encryption: null,
            advancedBucketConnection: {
              endpoint: 'http://www.fakeurl.com',
              maxConnectionPoolSize: '1',
              forcePathStyle: false
            }
          },
          softQuota: {
            enabled: true,
            limit: 1048576,
            type: 'spaceUsedQuota'
          }
        }
    );
  });

  it('creates a new Azure blob store', async function() {
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/types').mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/quotaTypes').mockResolvedValue(quotaTypes);

    const {
      name,
      loadingMask,
      typeSelect,
      azureAccountName,
      azureContainerName,
      softQuotaType,
      softQuotaLimit,
      spaceUsedQuotaLabel
    } = render();

    const data = {
      name: 'azure-blob-store',
      bucketConfiguration: {
        authentication: {
          authenticationMethod: 'MANAGEDIDENTITY'
        },
        accountName: 'azure-account',
        containerName: 'azure-container'
      },
      softQuota: {
        limit: 1048576,
        type: 'spaceUsedQuota',
        enabled: true
      }
    };

    await waitForElementToBeRemoved(loadingMask);

    userEvent.selectOptions(typeSelect(), 'Azure Cloud Storage');
    userEvent.type(name(), data.name);
    userEvent.type(azureAccountName(), data.bucketConfiguration.accountName);
    userEvent.type(azureContainerName(), data.bucketConfiguration.containerName);

    userEvent.click(selectors.getSoftQuota());
    expect(softQuotaType()).not.toBeInTheDocument();
    expect(spaceUsedQuotaLabel()).toBeInTheDocument();
    userEvent.type(softQuotaLimit(), '1');

    userEvent.click(selectors.querySubmitButton());

    expect(axios.post).toHaveBeenCalledWith(
      'service/rest/v1/blobstores/azure',
      data
    );
  });

  it('creates a new GCP blob store with default application authentication', async function() {
    const {
      name,
      loadingMask,
      typeSelect,
      softQuotaType,
      softQuotaLimit,
      spaceUsedQuotaLabel,
      bucket,
      region
    } = render();

    const data = {
      name: 'gcp-blob-store',
      bucketConfiguration: {
        bucketSecurity: {
          authenticationMethod: 'applicationDefault'
        },
        bucket: {
          name: 'test-bucket',
          region: 'us-central1'
        }
      },
      softQuota: {
        limit: 1048576,
        type: 'spaceUsedQuota',
        enabled: true
      }
    };

    await waitForElementToBeRemoved(loadingMask);

    userEvent.selectOptions(typeSelect(), 'Google Cloud Platform');
    userEvent.type(name(), data.name);
    userEvent.type(bucket(), data.bucketConfiguration.bucket.name);
    userEvent.type(region(), data.bucketConfiguration.bucket.region);

    userEvent.click(selectors.getSoftQuota());
    expect(softQuotaType()).not.toBeInTheDocument();
    expect(spaceUsedQuotaLabel()).toBeInTheDocument();
    userEvent.type(softQuotaLimit(), '1');

    userEvent.click(selectors.querySubmitButton());

    expect(axios.post).toHaveBeenCalledWith(
      'service/rest/v1/blobstores/google',
      data
    );
  });

  it('creates a new GCP blob store with JSON credentials authentication', async function() {
    const {
      name,
      loadingMask,
      typeSelect,
      bucket,
      region,
      credentialAuthentication,
      fileInput
    } = render();

    const data = {
      name: 'gcp-blob-store',
      bucketConfiguration: {
        bucketSecurity: {
          authenticationMethod: 'accountKey',
          accountKey: "{\"private_key_id\":\"test\"}"
        },
        bucket: {
          name: 'test-bucket2',
          region: 'us-central1'
        }
      },
      files: {
        0: expect.any(File),
        item: expect.any(Function),
        length: 1,
      },
    };

    await act(async () => {    
      await waitForElementToBeRemoved(loadingMask);

      userEvent.selectOptions(typeSelect(), 'Google Cloud Platform');
      userEvent.type(name(), data.name);
      userEvent.type(bucket(), data.bucketConfiguration.bucket.name);
      userEvent.type(region(), data.bucketConfiguration.bucket.region);
      userEvent.click(credentialAuthentication());

      const file = new File([new ArrayBuffer(1)], 'credentials.json', { type: 'application/json' });
      file.text = jest.fn().mockResolvedValue(JSON.stringify({ private_key_id: 'test' }));

      userEvent.upload(fileInput(), file);
      await file.text();

      userEvent.click(selectors.querySubmitButton());
    });

    expect(axios.post).toHaveBeenCalledWith(
      'service/rest/v1/blobstores/google',
      data
    );
  });

  it('edits a file blob store', async function() {
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/types').mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/quotaTypes').mockResolvedValue(quotaTypes);
    when(axios.get).calledWith('service/rest/v1/blobstores/file/test').mockResolvedValue({
      data: {
        path: 'testPath',
        softQuota: {
          type: 'spaceRemainingQuota',
          limit: SOFT_QUOTA_1_TERABYTE_IN_MEGABYTES
        }
      }
    });

    const {
      loadingMask,
      convertToGroup
    } = render('file/test');

    await waitForElementToBeRemoved(loadingMask);

    expect(convertToGroup()).toBeInTheDocument();
  });

  it('edits an s3 blob store', async function() {
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/types').mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/quotaTypes').mockResolvedValue(quotaTypes);
    when(axios.get).calledWith('service/rest/v1/blobstores/s3/test').mockResolvedValue({
      data: {
        name: 'test',
        bucketConfiguration: {
          bucket: { region: 'DEFAULT', name: 'bucket', prefix: '', expiration: '3' },
          bucketSecurity: {
            accessKeyId: 'someAccessKey',
            secretAccessKey: 'SomeSecretAccessKey',
            role: '',
            sessionToken: ''
          },
          encryption: { encryptionType: 'none', encryptionKey: '' },
          advancedBucketConnection: {
            endpoint: 'http://www.fakeurl.com',
            signerType: 'DEFAULT',
            forcePathStyle: ''
          }
        }
      }
    });

    const {
      loadingMask,
      convertToGroup,
      typeSelect,
      expiration,
      bucket,
      accessKeyId,
      secretAccessKey,
      endpointURL,
      name,
      title
    } = render('s3/test');

    await waitForElementToBeRemoved(loadingMask);

    expect(title()).toHaveTextContent('Edit test');

    // The type and name fields cannot be changed during edit
    expect(typeSelect()).not.toBeInTheDocument();
    expect(name()).not.toBeInTheDocument();

    expect(expiration()).toHaveValue('3');
    expect(bucket()).toHaveValue('bucket');
    expect(accessKeyId()).toHaveValue('someAccessKey');
    expect(secretAccessKey()).toHaveValue('SomeSecretAccessKey');
    expect(endpointURL()).toHaveValue('http://www.fakeurl.com');

    expect(convertToGroup()).toBeInTheDocument();
  });

  it('edits a file blob store', async function() {
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/types').mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/quotaTypes').mockResolvedValue(quotaTypes);
    when(axios.get).calledWith('service/rest/v1/blobstores/file/test').mockResolvedValue({
      data: {
        path: 'testPath',
        softQuota: {
          type: 'spaceRemainingQuota',
          limit: '104857600' // Bytes in 100 Megabytes
        }
      }
    });

    const {
      getByText,
      loadingMask,
      name,
      path,
      title,
      softQuotaType,
      softQuotaLimit,
      typeSelect
    } = render('file/test');

    await waitForElementToBeRemoved(loadingMask);

    expect(title()).toHaveTextContent('Edit test');
    expect(getByText('File Blob Store')).toBeInTheDocument();

    // The type and name fields cannot be changed during edit
    expect(typeSelect()).not.toBeInTheDocument();
    expect(name()).not.toBeInTheDocument();

    expect(path()).toHaveValue('testPath');
    expect(selectors.getSoftQuota()).toBeChecked();
    expect(softQuotaType()).toHaveValue('spaceRemainingQuota');
    expect(softQuotaLimit()).toHaveValue('100');
  });

  it('edits a group blob store', async function() {
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/types').mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/quotaTypes').mockResolvedValue(quotaTypes);
    when(axios.get).calledWith('service/rest/v1/blobstores/group/test').mockResolvedValue({
      data: {
        "softQuota" : null,
        "members" : [ "test-converted" ],
        "fillPolicy" : "writeToFirst"
      }
    });

    const {
      availableMembers,
      getByText,
      loadingMask,
      name,
      selectedMembers,
      title,
      typeSelect
    } = render('group/test');

    await waitForElementToBeRemoved(loadingMask);

    expect(title()).toHaveTextContent('Edit test');
    expect(getByText('Group Blob Store')).toBeInTheDocument();

    expect(typeSelect()).not.toBeInTheDocument();
    expect(name()).not.toBeInTheDocument();

    expect(availableMembers()).toHaveTextContent('default');
    expect(selectedMembers()).toHaveTextContent('test-converted');
  });

  it('convert to group is not shown when editing a group', async function() {
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/types').mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/quotaTypes').mockResolvedValue(quotaTypes);
    when(axios.get).calledWith('service/rest/v1/blobstores/group/test').mockResolvedValue({
      data: {
        "softQuota" : null,
        "members" : [ "test-converted" ],
        "fillPolicy" : "writeToFirst"
      }
    });

    const {
      loadingMask,
      convertToGroup
    } = render('group/test');

    await waitForElementToBeRemoved(loadingMask);

    expect(convertToGroup()).not.toBeInTheDocument();
  });

  it('converts to the group blob store', async function() {
    const convertUrl = 'service/rest/v1/blobstores/group/convert/a-file%2Fee%3A%23%24%25%40/test_1';
    const errorMessage = 'Blob store could not be converted to a group blob store';

    when(axios.get).calledWith('service/rest/internal/ui/blobstores/types').mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/quotaTypes').mockResolvedValue(quotaTypes);
    when(axios.get).calledWith('service/rest/v1/blobstores/file/a-file%2Fee%3A%23%24%25%40').mockResolvedValue({
      data: {
        path: 'testPath',
        softQuota: {
          type: 'spaceRemainingQuota',
          limit: '104857600'
        }
      }
    });
    when(axios.post).calledWith(convertUrl).mockRejectedValue({message: errorMessage});

    const {loadingMask, convertToGroup, title} = render('file/a-file%2Fee%3A%23%24%25%40');
    const {convertModal: {modal, title: modalTitle, warning, newName, convertButton, cancel}} = selectors;

    await waitForElementToBeRemoved(loadingMask);

    expect(title()).toHaveTextContent('Edit a-file/ee:#$%@');
    expect(convertToGroup()).toBeInTheDocument();

    userEvent.click(convertToGroup());
    expect(modal()).toBeInTheDocument();

    userEvent.click(cancel());
    expect(onDone).not.toBeCalled();
    expect(modal()).not.toBeInTheDocument();

    userEvent.click(convertToGroup());
    expect(modalTitle()).toHaveTextContent('Convert to Group Blob Store');
    expect(warning()).toBeInTheDocument();
    expect(newName()).toHaveValue('a-file/ee:#$%@-original');
    expect(newName()).not.toHaveErrorMessage();

    userEvent.click(convertButton());
    expect(newName()).toHaveErrorMessage(TestUtils.NAME_VALIDATION_MESSAGE);
    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

    userEvent.clear(newName());
    userEvent.type(newName(), 'test_1');
    expect(newName()).not.toHaveErrorMessage();

    userEvent.click(convertButton());
    await waitFor(() => expect(axios.post).toHaveBeenCalledWith(convertUrl));
    expect(onDone).not.toBeCalled();
    expect(selectors.querySaveError(errorMessage)).toBeInTheDocument();

    when(axios.post).calledWith(convertUrl).mockResolvedValue({data: {}});
    userEvent.click(convertButton());

    await waitFor(() => expect(onDone).toBeCalled());
  });

  it('log save error message when blobstore can not be added to group', async function() {
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/types').mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith('service/rest/internal/ui/blobstores/quotaTypes').mockResolvedValue(quotaTypes);

    let updateUrl = 'service/rest/v1/blobstores/group/test';

    when(axios.get).calledWith(updateUrl).mockResolvedValue({
      data: {
        "softQuota" : null,
        "members" : [ "test-converted", "default" ],
        "fillPolicy" : "writeToFirst"
      }
    });

    let errorMessage = 'Blob Store is not eligible to be a group member';

    when(axios.put).calledWith(updateUrl, {}).mockRejectedValue({
        response: {data: [{ "id": "*", "message": errorMessage}]}
    });

    const {loadingMask} = render('group/test');

    await waitForElementToBeRemoved(loadingMask);

    const consoleSpy = jest.spyOn(console, 'log');

    await axios.put(updateUrl, {}).catch(function(reason) {
      console.log(reason.response.data[0].message);
    });

    expect(consoleSpy).toHaveBeenCalledWith(errorMessage);
  });

  it('uses proper urls', function() {
    const validName = 'foo-bar_test';
    const invalidName = '/test%$#@8*>?';

    expect(blobStoreTypesUrl).toBe('/service/rest/internal/ui/blobstores/types');
    expect(blobStoreQuotaTypesUrl).toBe('/service/rest/internal/ui/blobstores/quotaTypes');

    expect(singleBlobStoreUrl(validName, invalidName)).toBe('service/rest/v1/blobstores/foo-bar_test/%2Ftest%25%24%23%408*%3E%3F');
    expect(singleBlobStoreUrl(invalidName, validName)).toBe('service/rest/v1/blobstores/%2Ftest%25%24%23%408*%3E%3F/foo-bar_test');

    expect(deleteBlobStoreUrl(validName)).toBe('service/rest/v1/blobstores/foo-bar_test');
    expect(deleteBlobStoreUrl(invalidName)).toBe('service/rest/v1/blobstores/%2Ftest%25%24%23%408*%3E%3F');

    expect(convertToGroupBlobStoreUrl(validName, invalidName)).toBe('service/rest/v1/blobstores/group/convert/foo-bar_test/%2Ftest%25%24%23%408*%3E%3F');
    expect(convertToGroupBlobStoreUrl(invalidName, validName)).toBe('service/rest/v1/blobstores/group/convert/%2Ftest%25%24%23%408*%3E%3F/foo-bar_test');

    expect(createBlobStoreUrl(validName)).toBe('service/rest/v1/blobstores/foo-bar_test');
    expect(createBlobStoreUrl(invalidName)).toBe('service/rest/v1/blobstores/%2Ftest%25%24%23%408*%3E%3F');

    expect(blobStoreUsageUrl(validName)).toBe('/service/rest/internal/ui/blobstores/usage/foo-bar_test');
    expect(blobStoreUsageUrl(invalidName)).toBe('/service/rest/internal/ui/blobstores/usage/%2Ftest%25%24%23%408*%3E%3F');
  });
});
