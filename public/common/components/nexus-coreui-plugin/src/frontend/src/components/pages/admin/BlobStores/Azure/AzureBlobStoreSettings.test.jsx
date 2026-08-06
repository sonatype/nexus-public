/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is the trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import React from 'react';
import {render, screen, fireEvent} from '@testing-library/react';
import {useActor} from '@xstate/react';

import AzureBlobStoreSettings from './AzureBlobStoreSettings';
import UIStrings from '../../../../../constants/UIStrings';

const AZURE = UIStrings.BLOB_STORES.AZURE;

jest.mock('@xstate/react', () => ({
  useActor: jest.fn(),
  useMachine: jest.fn()
}));

jest.mock('./AzureBlobStoreSettingsMachine', () => ({
  __esModule: true,
  default: {}
}));

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  FormUtils: {
    fieldProps: jest.fn((path, state) => ({
      value: path.reduce((obj, key) => obj?.[key], state.context.data) ?? '',
      isPristine: false,
      validationErrors: state.context.errors
    })),
    checkboxProps: jest.fn((fieldName, state) => ({
      isChecked: state.context.data.bucketConfiguration?.[fieldName] ?? false,
      isPristine: false,
      validationErrors: state.context.errors
    })),
    handleUpdate: jest.fn(() => jest.fn())
  },
  ExtJS: {
    isProEdition: jest.fn(() => true),
    state: () => ({getValue: (key) => key === 'azureSasUrlEnabled' ? true : null})
  }
}));

const ExtJS = require('@sonatype/nexus-ui-plugin').ExtJS;
const {useMachine} = require('@xstate/react');

function createMockState(data = {}, errors = {}) {
  return {
    context: {
      data: {
        name: 'test-blob-store',
        bucketConfiguration: {
          accountName: 'test-account',
          containerName: 'test-container',
          authentication: {
            authenticationMethod: 'ACCOUNTKEY'
          },
          ...data.bucketConfiguration
        },
        ...data
      },
      errors
    },
    matches: jest.fn(() => false)
  };
}

function createMockAzureState() {
  return {
    context: {},
    matches: jest.fn(() => false)
  };
}

function renderWith({isPro = true, bucketConfiguration = {}, errors = {}} = {}) {
  ExtJS.isProEdition.mockReturnValue(isPro);
  const mockState = createMockState({bucketConfiguration}, errors);
  const mockSend = jest.fn();
  const mockAzureState = createMockAzureState();
  const mockAzureSend = jest.fn();

  useActor.mockReturnValue([mockState, mockSend]);
  useMachine.mockReturnValue([mockAzureState, mockAzureSend]);

  return render(<AzureBlobStoreSettings service={{}} />);
}

describe('AzureBlobStoreSettings — preSignedUrl', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders the fieldset under Pro edition', () => {
    renderWith({isPro: true});
    expect(screen.getByText(AZURE.PRE_SIGNED_URL.LABEL)).toBeInTheDocument();
    expect(screen.getByLabelText(AZURE.PRE_SIGNED_URL.CHECKBOX_LABEL)).toBeInTheDocument();
  });

  it('hides the fieldset on non-Pro edition', () => {
    renderWith({isPro: false});
    expect(screen.queryByText(AZURE.PRE_SIGNED_URL.LABEL)).not.toBeInTheDocument();
  });

  it('shows the RBAC note when MI auth + checkbox checked', () => {
    renderWith({
      bucketConfiguration: {
        preSignedUrlEnabled: true,
        authentication: {authenticationMethod: 'MANAGEDIDENTITY'}
      }
    });
    expect(screen.getByText(AZURE.PRE_SIGNED_URL.RBAC_NOTE)).toBeInTheDocument();
  });

  it('shows the RBAC note when Env-credential auth + checkbox checked', () => {
    renderWith({
      bucketConfiguration: {
        preSignedUrlEnabled: true,
        authentication: {authenticationMethod: 'ENVIRONMENTVARIABLE'}
      }
    });
    expect(screen.getByText(AZURE.PRE_SIGNED_URL.RBAC_NOTE)).toBeInTheDocument();
  });

  it('hides the RBAC note when Account Key auth (even with checkbox checked)', () => {
    renderWith({
      bucketConfiguration: {
        preSignedUrlEnabled: true,
        authentication: {authenticationMethod: 'ACCOUNTKEY'}
      }
    });
    expect(screen.queryByText(AZURE.PRE_SIGNED_URL.RBAC_NOTE)).not.toBeInTheDocument();
  });

  it('renders the RBAC error alert when form-errors include preSignedUrlEnabled', () => {
    renderWith({
      bucketConfiguration: {
        preSignedUrlEnabled: true,
        authentication: {authenticationMethod: 'MANAGEDIDENTITY'}
      },
      errors: {bucketConfiguration: {preSignedUrlEnabled: 'rbac-denied'}}
    });
    expect(screen.getByText(AZURE.PRE_SIGNED_URL.RBAC_ERROR)).toBeInTheDocument();
  });
});
