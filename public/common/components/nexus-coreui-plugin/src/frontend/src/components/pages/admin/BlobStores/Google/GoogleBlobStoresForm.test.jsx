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
import {fireEvent, render, screen, waitFor, waitForElementToBeRemoved} from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import BlobStoresForm from '../BlobStoresForm';
import {ExtJS} from '@sonatype/nexus-ui-plugin';

import blobstoreTypes from '../testData/mockBlobStoreTypes.json';
import quotaTypes from '../testData/mockQuotaTypes.json';
import {URLs} from '../BlobStoresHelper';
import GoogleBlobStoreSettings from './GoogleBlobStoreSettings';
import GoogleBlobStoreActions from './GoogleBlobStoreActions';
import {blobStoreFormSelectors} from '../testUtils/blobStoreFormSelectors';
import {enableSoftQueryReadOnlyAndChangeLimit} from '../testUtils/enableSoftQueryReadOnlyAndChangeLimit';
import UIStrings from '../../../../../constants/UIStrings';
import { useCurrentStateAndParams } from '@uirouter/react';
import { ROUTE_NAMES } from '../../../../../routerConfig/routeNames/routeNames';

jest.mock('../BlobStoreTypes', () => ({
  __esModule: true,
  default: {
    google: {
      Settings: require('./GoogleBlobStoreSettings').default,
      Actions: require('./GoogleBlobStoreActions').default
    }
  }
}));

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

  // GCP Fields
  queryRegion: () => screen.getByLabelText(UIStrings.BLOB_STORES.GOOGLE.REGION.label),
  queryCredentialAuthentication: () => screen.getByLabelText(
      UIStrings.BLOB_STORES.GOOGLE.AUTHENTICATION.CREDENTIAL_JSON_FILE),
  queryFileInput: () => screen.getByLabelText(UIStrings.BLOB_STORES.GOOGLE.AUTHENTICATION.JSON_PATH.label),
  queryKmsEncryption: () => screen.getByLabelText(UIStrings.BLOB_STORES.GOOGLE.ENCRYPTION.KMS_MANAGED),
  queryKmsKeyResourceName: () => screen.getByLabelText(UIStrings.BLOB_STORES.GOOGLE.ENCRYPTION.KEY_NAME.label),

};

describe('BlobStoresForm-GCP', () => {
  beforeEach(() => {
    ExtJS.isProEdition.mockReturnValue(false);
    when(axios.get).calledWith(URLs.blobStoreTypesUrl).mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith(URLs.blobStoreQuotaTypesUrl).mockResolvedValue(quotaTypes);

    useCurrentStateAndParams.mockReset();
    useCurrentStateAndParams.mockReturnValue({state: { name: undefined }, params: {}});
    givenBlobStoresPermissions({ 'nexus:blobstores:update': true, 'nexus:blobstores:delete': true });
  });

  function renderCreateView() {
    useCurrentStateAndParams.mockReturnValue({state: { name: ADMIN.REPOSITORY.BLOBSTORES.CREATE }, params: {}});
    return renderComponent();
  }

  function renderComponent() {
    return render(<BlobStoresForm />);
  }

  it('creates a new GCP blob store. region is a read-only', async function() {
    renderCreateView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.selectOptions(selectors.queryTypeSelect(), 'Google Cloud Platform');
    const regionElement = selectors.queryRegion();
    expect(regionElement).toBeInTheDocument();
    expect(regionElement).toHaveTextContent(
        'The region is automatically set based on where Nexus Repository is running in GCP. Ensure the bucket is in the same region.');
  });

  it('creates a new GCP blob store with default application authentication', async function() {
    renderCreateView();

    const data = {
      name: 'gcp-blob-store',
      bucketConfiguration: {
        bucketSecurity: {
          authenticationMethod: 'applicationDefault'
        },
        encryption: {
          encryptionType: 'default'
        },
        bucket: {
          name: 'test-bucket',
          prefix: 'pre'
        }
      },
      softQuota: {
        limit: 1048576,
        type: 'spaceUsedQuota',
        enabled: true
      }
    };

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.selectOptions(selectors.queryTypeSelect(), 'Google Cloud Platform');
    fireEvent.change(selectors.queryName(), {target: {value: data.name}});
    fireEvent.change(selectors.queryBucket(), {target: {value: data.bucketConfiguration.bucket.name}});
    fireEvent.change(selectors.queryPrefix(), {target: {value: data.bucketConfiguration.bucket.prefix}});

    enableSoftQueryReadOnlyAndChangeLimit('1');

    userEvent.click(selectors.querySubmitButton());

    await waitFor(() => expect(axios.post).toHaveBeenCalledWith(
        'service/rest/v1/blobstores/google',
        data
    ));
  });

  it('creates a new GCP blob store with JSON credentials authentication', async function() {
    renderCreateView();

    const data = {
      name: 'gcp-blob-store',
      bucketConfiguration: {
        bucketSecurity: {
          authenticationMethod: 'accountKey',
          accountKey: "{\"private_key_id\":\"test\"}",
          file: {
            0: expect.any(File),
            item: expect.any(Function),
            length: 1,
          }
        },
        encryption: {
          encryptionType: 'default'
        },
        bucket: {
          name: 'test-bucket2',
        }
      },
    };

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.selectOptions(selectors.queryTypeSelect(), 'Google Cloud Platform');
    fireEvent.change(selectors.queryName(), {target: {value: data.name}});
    fireEvent.change(selectors.queryBucket(), {target: {value: data.bucketConfiguration.bucket.name}});
    userEvent.click(selectors.queryCredentialAuthentication());

    const file = new File([new ArrayBuffer(1)], 'credentials.json', {type: 'application/json'});
    file.text = jest.fn().mockResolvedValue(JSON.stringify({private_key_id: 'test'}));

    userEvent.upload(selectors.queryFileInput(), file);
    await file.text();

    userEvent.click(selectors.querySubmitButton());

    await waitFor(() => expect(axios.post).toHaveBeenCalledWith(
        'service/rest/v1/blobstores/google',
        data
    ));
  });

  it('creates a new GCP blob store with default encryption', async function() {
    renderCreateView();

    const data = {
      name: 'gcp-blob-store',
      bucketConfiguration: {
        bucketSecurity: {
          authenticationMethod: 'applicationDefault'
        },
        encryption: {
          encryptionType: 'default'
        },
        bucket: {
          name: 'test-bucket',
          prefix: 'pre'
        }
      },
      softQuota: {
        limit: 1048576,
        type: 'spaceUsedQuota',
        enabled: true
      }
    };

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.selectOptions(selectors.queryTypeSelect(), 'Google Cloud Platform');
    fireEvent.change(selectors.queryName(), {target: {value: data.name}});
    fireEvent.change(selectors.queryBucket(), {target: {value: data.bucketConfiguration.bucket.name}});
    fireEvent.change(selectors.queryPrefix(), {target: {value: data.bucketConfiguration.bucket.prefix}});

    enableSoftQueryReadOnlyAndChangeLimit('1');

    userEvent.click(selectors.querySubmitButton());

    await waitFor(() => expect(axios.post).toHaveBeenCalledWith(
        'service/rest/v1/blobstores/google',
        data
    ));
  });

  it('creates a new GCP blob store with KMS encryption', async function() {
    renderCreateView();

    const data = {
      name: 'gcp-blob-store',
      bucketConfiguration: {
        bucketSecurity: {
          authenticationMethod: 'applicationDefault'
        },
        encryption: {
          encryptionType: 'kmsManagedEncryption',
          encryptionKey: 'projects/test_project_id/locations/global/keyRings/test_key_ring/cryptoKeys/test_key'
        },
        bucket: {
          name: 'test-bucket',
          prefix: 'pre'
        }
      },
      softQuota: {
        limit: 1048576,
        type: 'spaceUsedQuota',
        enabled: true
      }
    };

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.selectOptions(selectors.queryTypeSelect(), 'Google Cloud Platform');
    fireEvent.change(selectors.queryName(), {target: {value: data.name}});
    fireEvent.change(selectors.queryBucket(), {target: {value: data.bucketConfiguration.bucket.name}});
    fireEvent.change(selectors.queryPrefix(), {target: {value: data.bucketConfiguration.bucket.prefix}});
    userEvent.click(selectors.queryKmsEncryption());
    fireEvent.change(selectors.queryKmsKeyResourceName(), {target: {value: data.bucketConfiguration.encryption.encryptionKey}});

    enableSoftQueryReadOnlyAndChangeLimit('1');

    userEvent.click(selectors.querySubmitButton());

    await waitFor(() => expect(axios.post).toHaveBeenCalledWith(
        'service/rest/v1/blobstores/google',
        data
    ));
  });
});
