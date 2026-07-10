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
import {ExtJS} from '@sonatype/nexus-ui-plugin';

import blobstoreTypes from '../testData/mockBlobStoreTypes.json';
import quotaTypes from '../testData/mockQuotaTypes.json';
import {URLs} from '../BlobStoresHelper';
import AzureBlobStoreSettings from './AzureBlobStoreSettings';
import AzureBlobStoreActions from './AzureBlobStoreActions';
import {blobStoreFormSelectors} from '../testUtils/blobStoreFormSelectors';
import {enableSoftQueryReadOnlyAndChangeLimit} from '../testUtils/enableSoftQueryReadOnlyAndChangeLimit';
import BlobStoresForm from '../BlobStoresForm';

jest.mock('../BlobStoreTypes', () => ({
  __esModule: true,
  default: {
    azure: {
      Settings: require('./AzureBlobStoreSettings').default,
      Actions: require('./AzureBlobStoreActions').default
    }
  }
}));
import { UIRouter, useCurrentStateAndParams, useRouter } from '@uirouter/react';
import { ROUTE_NAMES } from '../../../../../routerConfig/routeNames/routeNames';

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

  queryAccountName: () => screen.queryByLabelText('Account Name'),
  queryContainerName: () => screen.queryByLabelText('Container Name')

};

describe('BlobStoresForm-Azure', () => {
  beforeEach(() => {
    ExtJS.isProEdition.mockReturnValue(false);
    when(axios.get).calledWith(URLs.blobStoreTypesUrl).mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith(URLs.blobStoreQuotaTypesUrl).mockResolvedValue(quotaTypes);

    useCurrentStateAndParams.mockReset();
    useCurrentStateAndParams.mockReturnValue({state: { name: undefined }, params: {}});
    givenBlobStoresPermissions({ 'nexus:blobstores:update': true, 'nexus:blobstores:delete': true });
  });

  it('creates a new Azure blob store', async function() {
    useCurrentStateAndParams.mockReturnValue({state: { name: ADMIN.REPOSITORY.BLOBSTORES.CREATE }, params: {}});

    render(<BlobStoresForm />);

    const name = 'azure-blob-store';
    const accountName = 'azure-account';
    const containerName = 'azure-container';

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    userEvent.selectOptions(selectors.queryTypeSelect(), 'Azure Cloud Storage');
    fireEvent.change(selectors.queryName(), {target: {value: name}});
    fireEvent.change(selectors.queryAccountName(), {target: {value: accountName}});
    fireEvent.change(selectors.queryContainerName(), {target: {value: containerName}});

    enableSoftQueryReadOnlyAndChangeLimit('1')

    userEvent.click(selectors.querySubmitButton());

    await waitFor(() => expect(axios.post).toHaveBeenCalledWith(
        'service/rest/v1/blobstores/azure',
        {
          name,
          bucketConfiguration: {
            authentication: {
              authenticationMethod: 'MANAGEDIDENTITY'
            },
            accountName,
            containerName
          },
          softQuota: {
            limit: 1048576,
            type: 'spaceUsedQuota',
            enabled: true
          }
        }
    ));
  });
});
