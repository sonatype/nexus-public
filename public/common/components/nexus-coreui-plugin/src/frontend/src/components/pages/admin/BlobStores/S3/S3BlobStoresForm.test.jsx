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
import {fireEvent, render, screen, waitFor, waitForElementToBeRemoved, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import BlobStoresForm from '../BlobStoresForm';
import UIStrings from '../../../../../constants/UIStrings';
import {ExtJS} from '@sonatype/nexus-ui-plugin';

import blobstoreTypes from '../testData/mockBlobStoreTypes.json';
import quotaTypes from '../testData/mockQuotaTypes.json';
import {URLs} from '../BlobStoresHelper';
import S3BlobStoreSettings from './S3BlobStoreSettings';
import S3BlobStoreWarning from './S3BlobStoreWarning';
import S3BlobStoreActions from './S3BlobStoreActions';
import {blobStoreFormSelectors} from '../testUtils/blobStoreFormSelectors';
import { ROUTE_NAMES } from '../../../../../routerConfig/routeNames/routeNames';
import { useCurrentStateAndParams } from '@uirouter/react';

jest.mock('../BlobStoreTypes', () => ({
  __esModule: true,
  default: {
    s3: {
      Settings: require('./S3BlobStoreSettings').default,
      Warning: require('./S3BlobStoreWarning').default,
      Actions: require('./S3BlobStoreActions').default
    }
  }
}));

const S3_STRINGS = UIStrings.S3_BLOBSTORE_CONFIGURATION;
const MAX_REPLICATION_BUCKETS = 5;

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    requestConfirmation: jest.fn(),
    showErrorMessage: jest.fn(),
    state: jest.fn().mockReturnValue({
      getValue: jest.fn().mockReturnValue(true)
    }),
    isProEdition: jest.fn(),
    checkPermission: jest.fn(key => {
      return BlobStoresFormTestPermissions[key] ?? false;
    }),
  }
}));

let BlobStoresFormTestPermissions = {};

function givenBlobStoresPermissions(permissionLookup) {
  BlobStoresFormTestPermissions = permissionLookup;
}

const stateServiceGoMock = jest.fn();

jest.mock('@uirouter/react', () => ({
  ...jest.requireActual('@uirouter/react'),
    useCurrentStateAndParams: jest.fn(),
    useRouter: () =>({
      stateService: {
        go: stateServiceGoMock,
      }
    })
}));

const ADMIN = ROUTE_NAMES.ADMIN;

const selectors = {
  ...TestUtils.selectors,
  ...TestUtils.formSelectors,

  ...blobStoreFormSelectors,

  // S3 Fields
  queryPreSigned: () => screen.queryByLabelText(S3_STRINGS.S3BlobStore_Presigned_HelpText),
  queryRegion: () => screen.queryByTestId('s3-primary-region'),
  queryAssumeRole: () => screen.queryByLabelText('Assume Role ARN'),
  querySessionToken: () => screen.queryByLabelText('Session Token'),
  queryEncryptionType: () => screen.queryByLabelText('Encryption Type'),
  queryKmsKeyId: () => screen.queryByLabelText('KMS Key ID (Optional)'),
  queryReplicationBucketRegionSelects: () => screen.queryAllByLabelText('Region').slice(1),
  queryReplicationBucketBucketNames: () => screen.queryAllByLabelText('Bucket Name'),
  queryMaxConnectionPoolSize: () => screen.queryByLabelText('Max Connection Pool Size'),
  queryAccessKeyId: () => screen.getByLabelText('Access Key ID'),
  querySecretAccessKey: () => screen.getByLabelText('Secret Access Key'),
  queryEndpointURL: () => screen.getByLabelText('Endpoint URL'),
  queryReplicationBuckets: () => screen.queryByText('AWS S3 Replication Buckets (Optional)'),
  addReplicationBucketButton: () => screen.queryByRole('button', {name: 'Add Replication Bucket'}),
  removeReplicationBucketButtons: () => screen.queryAllByRole('button', {name: 'Remove Bucket'}),
  configureReplicationBucketsInfo: () => screen.getAllByText(S3_STRINGS.S3BlobStore_ReplicationBucketsSettings_ConfigureBucketReplicationMessage),
  maxReplicationBucketsWarning: () => screen.getByText(S3_STRINGS.S3BlobStore_ReplicationBucketsSettings_MaxFailoverBucketsWarning),
  getUsePathStyle: () => within(screen.getByRole('group', {name: 'Use path-style access'})).getByLabelText(
      'Setting this flag will result in path-style access being used for all requests'),
};

describe('BlobStoresForm-S3', () => {
  beforeEach(() => {
    ExtJS.isProEdition.mockReturnValue(false);
    when(ExtJS.state().getValue).calledWith('S3FailoverEnabled', expect.any(Boolean)).mockReturnValue(false);
    when(axios.get).calledWith(URLs.blobStoreTypesUrl).mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith(URLs.blobStoreQuotaTypesUrl).mockResolvedValue(quotaTypes);

    useCurrentStateAndParams.mockReset();
    useCurrentStateAndParams.mockReturnValue({state: { name: undefined }, params: {}});
    givenBlobStoresPermissions({ 'nexus:blobstores:update': true, 'nexus:blobstores:delete': true });
  });

  function renderEditView(itemId) {
    const [ type, name ] = itemId.split('/');
    useCurrentStateAndParams.mockReturnValue({state: { name: ADMIN.REPOSITORY.BLOBSTORES.EDIT }, params: {type, name}});
    return renderComponent();
  }

  function renderCreateView() {
    useCurrentStateAndParams.mockReturnValue({state: { name: ADMIN.REPOSITORY.BLOBSTORES.CREATE }, params: {}});
    return renderComponent();
  }

  function renderComponent() {
    return render(<BlobStoresForm />);
  }

  it('renders the form and buttons when the S3 type is selected', async function() {
    renderCreateView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.selectOptions(selectors.queryTypeSelect(), 'S3');
    expect(selectors.queryTypeSelect()).toHaveValue('s3');
  });

  it('renders S3 specific form fields', async function() {
    renderCreateView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    chooseBlobStoreType('s3');

    expect(selectors.queryRegion()).toBeInTheDocument();
    expect(selectors.queryBucket()).toBeInTheDocument();
    expect(selectors.queryPrefix()).toBeInTheDocument();
    expect(selectors.queryAccessKeyId()).toBeInTheDocument();
    expect(selectors.querySecretAccessKey()).toBeInTheDocument();
    expect(selectors.queryAssumeRole()).toBeInTheDocument();
    expect(selectors.querySessionToken()).toBeInTheDocument();
    expect(selectors.queryEndpointURL()).toBeInTheDocument();
    expect(selectors.queryEncryptionType()).toBeInTheDocument();
    expect(selectors.queryKmsKeyId()).toBeInTheDocument();
    expect(selectors.getUsePathStyle()).toBeInTheDocument();
  });

  describe('create s3 blob store', () => {
    it('handles form field validation and changes correctly', async () => {
      renderCreateView();

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      chooseBlobStoreType('s3');

      // Check for required fields - This relies on the submit button already being clicked so errors are already displaying
      fireEvent.change(selectors.queryName(), {target: {value: 'test'}});
      expect(selectors.queryName()).toHaveValue('test');

      fireEvent.change(selectors.queryBucket(), {target: {value: 'bucket'}});
      expect(selectors.queryBucket()).toHaveValue('bucket');
      expect(selectors.queryFormError()).not.toBeInTheDocument();

      // Check that the access key id field requires a secret access key
      fireEvent.change(selectors.queryAccessKeyId(), {target: {value: 'someAccessKey'}});
      expect(selectors.queryAccessKeyId()).toHaveValue('someAccessKey');
      userEvent.click(selectors.querySubmitButton());
      expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

      fireEvent.change(selectors.querySecretAccessKey(), {target: {value: 'SomeSecretAccessKey'}});
      expect(selectors.querySecretAccessKey()).toHaveValue('SomeSecretAccessKey');
      expect(selectors.queryFormError()).not.toBeInTheDocument();

      // Check that the endpoint URL is required and must be a valid URL
      fireEvent.change(selectors.queryEndpointURL(), {target: {value: 'invalidUrl'}});
      expect(selectors.queryEndpointURL()).toHaveValue('invalidUrl');
      userEvent.click(selectors.querySubmitButton());
      expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

      fireEvent.change(selectors.queryEndpointURL(), {target: {value: ''}});
      expect(selectors.queryEndpointURL()).toHaveValue('');
      fireEvent.change(selectors.queryEndpointURL(), {target: {value: 'http://www.fakeurl.com'}});
      expect(selectors.queryEndpointURL()).toHaveValue('http://www.fakeurl.com');
      expect(selectors.queryFormError()).not.toBeInTheDocument();

      // Check maximum connection pool size validation errors
      fireEvent.change(selectors.queryMaxConnectionPoolSize(), {target: {value: '0'}});
      expect(selectors.queryMaxConnectionPoolSize()).toHaveAccessibleErrorMessage('The minimum value for this field is 1');
      fireEvent.change(selectors.queryMaxConnectionPoolSize(), {target: {value: '2000000000'}});
      expect(selectors.queryMaxConnectionPoolSize()).toHaveAccessibleErrorMessage('The maximum value for this field is 1000000000');
      fireEvent.change(selectors.queryMaxConnectionPoolSize(), {target: {value: ''}});
      expect(selectors.queryMaxConnectionPoolSize()).not.toHaveAccessibleErrorMessage(expect.anything());

      // Enable the soft quota section
      userEvent.click(selectors.softQuota.queryEnabled());
      expect(selectors.softQuota.queryType()).not.toBeInTheDocument();
      expect(selectors.softQuota.querySpaceUsedQuotaReadOnly()).toBeInTheDocument();
    });

    it('allows manipulation of replication buckets', async () => {
      when(ExtJS.state().getValue).calledWith('S3FailoverEnabled', expect.any(Boolean)).mockReturnValue(true);
      renderCreateView();

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      chooseBlobStoreType('s3');

      expect(selectors.queryReplicationBuckets()).toBeInTheDocument();
      expect(selectors.addReplicationBucketButton()).not.toBeVisible();
      expect(selectors.removeReplicationBucketButtons()).toHaveLength(0);

      // Expand the replication buckets
      userEvent.click(selectors.queryReplicationBuckets());
      expect(selectors.addReplicationBucketButton()).toBeVisible();

      // Add a replication bucket with configuration
      userEvent.click(selectors.addReplicationBucketButton());
      expect(selectors.removeReplicationBucketButtons()[0]).toBeInTheDocument();
      userEvent.selectOptions(selectors.queryReplicationBucketRegionSelects()[0], 'us-west-2');
      expect(selectors.queryReplicationBucketRegionSelects()[0]).toHaveValue('us-west-2');
      fireEvent.change(selectors.queryReplicationBucketBucketNames()[0], {target: {value: 'test-replication-bucket'}});
      expect(selectors.queryReplicationBucketBucketNames()[0]).toHaveValue('test-replication-bucket');
      expect(selectors.removeReplicationBucketButtons()).toHaveLength(1);

      // Check that the replication bucket can be removed
      userEvent.click(selectors.removeReplicationBucketButtons()[0]);
      expect(selectors.removeReplicationBucketButtons()).toHaveLength(0);
    });

    it ('creates a new S3 blob store failover buckets not visible', async () => {
      renderCreateView();

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      userEvent.selectOptions(selectors.queryTypeSelect(), 'S3');
      expect(selectors.queryTypeSelect()).toHaveValue('s3');

      // Replication buckets not available
      expect(selectors.queryReplicationBuckets()).not.toBeInTheDocument();
    });

    it('creates a new S3 blob store with the expected request', async () => {
      when(ExtJS.state().getValue).calledWith('S3FailoverEnabled', expect.any(Boolean)).mockReturnValue(true);

      renderCreateView();

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      chooseBlobStoreType('s3');

      // Set the form values
      fireEvent.change(selectors.queryName(), {target: {value: 'test'}});
      fireEvent.change(selectors.queryBucket(), {target: {value: 'bucket'}});
      fireEvent.change(selectors.queryEndpointURL(), {target: {value: 'http://www.fakeurl.com'}});
      fireEvent.change(selectors.queryMaxConnectionPoolSize(), {target: {value: '1'}});
      fireEvent.change(selectors.queryAccessKeyId(), {target: {value: 'someAccessKey'}});
      fireEvent.change(selectors.querySecretAccessKey(), {target: {value: 'SomeSecretAccessKey'}});
      userEvent.click(selectors.queryReplicationBuckets());
      userEvent.click(selectors.addReplicationBucketButton());
      userEvent.selectOptions(selectors.queryReplicationBucketRegionSelects()[0], 'us-west-2');
      fireEvent.change(selectors.queryReplicationBucketBucketNames()[0], {target: {value: 'test-replication-bucket'}});
      userEvent.click(selectors.softQuota.queryEnabled());
      fireEvent.change(selectors.softQuota.queryLimit(), {target: {value: '1'}});

      userEvent.click(selectors.querySubmitButton());

      const request = {
        name: 'test',
        bucketConfiguration: {
          bucket: { region: 'DEFAULT', name: 'bucket', prefix: '' },
          bucketSecurity: {
            accessKeyId: 'someAccessKey',
            secretAccessKey: 'SomeSecretAccessKey'
          },
          encryption: null,
          advancedBucketConnection: {
            endpoint: 'http://www.fakeurl.com',
            maxConnectionPoolSize: '1',
            forcePathStyle: false
          },
          failoverBuckets: [{region: 'us-west-2', bucketName: 'test-replication-bucket'}],
          activeRegion: null
        },
        softQuota: {
          enabled: true,
          limit: 1048576,
          type: 'spaceUsedQuota'
        }
      };

      await waitFor(() => expect(axios.post).toHaveBeenCalledWith('service/rest/v1/blobstores/s3', request));
    });

    it('PRO - creates a new S3 blob store', async () => {
      ExtJS.isProEdition.mockReturnValue(true);

      renderCreateView();

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      chooseBlobStoreType('s3');

      fireEvent.change(selectors.queryName(), {target: {value: 'test'}});
      fireEvent.change(selectors.queryBucket(), {target: {value: 'bucket'}});

      expect(selectors.queryPreSigned()).not.toBeChecked();
      userEvent.click(selectors.queryPreSigned());
      expect(selectors.queryPreSigned()).toBeChecked();

      userEvent.click(selectors.querySubmitButton());

      await waitFor(() => expect(axios.post).toHaveBeenCalledWith('service/rest/v1/blobstores/s3', expect.objectContaining({
        bucketConfiguration: expect.objectContaining({
          preSignedUrlEnabled: true
        })
      })));
    });
  });

  describe('edit s3 blob store', () => {
    it('loads an existing S3 blob store configuration for editing', async () => {
      when(ExtJS.state().getValue).calledWith('S3FailoverEnabled', expect.any(Boolean)).mockReturnValue(true);
      const blobStoreConfiguration = mockBlobStoreForEdit();

      renderEditView('s3/test');

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      // Check that the type selection and blob store name cannot be changed
      expect(selectors.queryTypeSelect()).not.toBeInTheDocument();

      // Check the bucket configuration
      expect(selectors.queryBucket()).toHaveValue(blobStoreConfiguration.bucketConfiguration.bucket.name);
      expect(selectors.queryPrefix()).toHaveValue(blobStoreConfiguration.bucketConfiguration.bucket.prefix);
      expect(selectors.queryAccessKeyId()).toHaveValue(blobStoreConfiguration.bucketConfiguration.bucketSecurity.accessKeyId);
      expect(selectors.querySecretAccessKey()).toHaveValue(blobStoreConfiguration.bucketConfiguration.bucketSecurity.secretAccessKey);
      expect(selectors.queryAssumeRole()).toHaveValue(blobStoreConfiguration.bucketConfiguration.bucketSecurity.role);
      expect(selectors.querySessionToken()).toHaveValue(blobStoreConfiguration.bucketConfiguration.bucketSecurity.sessionToken);

      // Check advanced bucket connection settings
      expect(selectors.queryEndpointURL()).toHaveValue(blobStoreConfiguration.bucketConfiguration.advancedBucketConnection.endpoint);

      // Check max replication buckets
      expect(selectors.configureReplicationBucketsInfo()).toHaveLength(MAX_REPLICATION_BUCKETS);
      expect(selectors.removeReplicationBucketButtons()).toHaveLength(MAX_REPLICATION_BUCKETS);
      expect(selectors.addReplicationBucketButton()).not.toBeInTheDocument();
      expect(selectors.addReplicationBucketButton()).not.toBeInTheDocument();
      expect(selectors.maxReplicationBucketsWarning()).toBeInTheDocument();

      // Check removing replication buckets
      userEvent.click(selectors.removeReplicationBucketButtons()[0]);
      userEvent.click(selectors.removeReplicationBucketButtons()[0]);
      expect(selectors.configureReplicationBucketsInfo()).toHaveLength(3);
      expect(selectors.removeReplicationBucketButtons()).toHaveLength(3);
      expect(selectors.addReplicationBucketButton()).toBeInTheDocument();

      // Check adding replication buckets
      userEvent.click(selectors.addReplicationBucketButton());
      expect(selectors.configureReplicationBucketsInfo()).toHaveLength(4);
      expect(selectors.removeReplicationBucketButtons()).toHaveLength(4);
      expect(selectors.addReplicationBucketButton()).toBeInTheDocument();

      // Check convertToGroup button
      expect(selectors.queryConvertToGroupButton()).toBeInTheDocument();

      // Check that Pre-Signed urls are unavailable
      expect(selectors.queryPreSigned()).not.toBeInTheDocument();
    });

    it('displays Pre-Signed urls for edit in PRO edition', async () => {
      ExtJS.isProEdition.mockReturnValue(true);

      mockBlobStoreForEdit();

      renderEditView('s3/test');

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      expect(selectors.queryPreSigned()).toBeInTheDocument();
      expect(selectors.queryPreSigned()).toBeChecked();
    });

    it('edits S3 blob store with failover buckets not visible', async () => {
      when(ExtJS.state().getValue).calledWith('S3FailoverEnabled', expect.any(Boolean)).mockReturnValue(false);

      mockBlobStoreForEdit();

      renderEditView('s3/test');

      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      expect(selectors.queryReplicationBuckets()).not.toBeInTheDocument();
    });
  });
});

function chooseBlobStoreType(typeId) {
  const blobStoreType = blobstoreTypes.data.find(type => type.id === typeId);
  userEvent.selectOptions(selectors.queryTypeSelect(), blobStoreType.name);
  expect(selectors.queryTypeSelect()).toHaveValue(blobStoreType.id);
}

function mockBlobStoreForEdit() {
  const blobStoreConfiguration = {
    name: 'test',
    bucketConfiguration: {
      bucket: { region: 'DEFAULT', name: 'bucket', prefix: 'prefix' },
      bucketSecurity: {
        accessKeyId: 'someAccessKey',
        secretAccessKey: 'SomeSecretAccessKey',
        role: 'assume-role-arn',
        sessionToken: 'sesson-token'
      },
      encryption: { encryptionType: 'none', encryptionKey: '' },
      advancedBucketConnection: {
        endpoint: 'http://www.fakeurl.com',
        signerType: 'DEFAULT',
        forcePathStyle: ''
      },
      failoverBuckets: [
        {
          region: "us-east-1",
          bucketName: "replication-bucket-1"
        },
        {
          region: "us-east-2",
          bucketName: "replication-bucket-2"
        },
        {
          region: "us-west-1",
          bucketName: "replication-bucket-3"
        },
        {
          region: "us-west-2",
          bucketName: "replication-bucket-4"
        },
        {
          region: "eu-west-1",
          bucketName: "replication-bucket-5"
        }
      ],
      preSignedUrlEnabled: true
    }
  };
  when(axios.get).calledWith('service/rest/v1/blobstores/s3/test').mockResolvedValue({
    data: blobStoreConfiguration
  });
  return blobStoreConfiguration;
}
