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
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import AzureBlobStoreSettings from '../AzureBlobStoreSettings';
import * as useBlobStoresModule from '../useBlobStores';

jest.mock('../useBlobStores', () => ({
  useAzureConnectionTest: jest.fn(),
}));

// Mock shared form components
jest.mock('../../../../shared/form', () => ({
  SettingsFormSection: ({ children, title }) => (
    <div data-testid="settings-form-section">
      <h2>{title}</h2>
      {children}
    </div>
  ),
  SettingsTextInput: ({ label, value, onChange }) => (
    <div>
      <label>{label}</label>
      <input 
        value={value || ''} 
        onChange={(e) => onChange(e.target.value)}
        data-testid={`input-${label}`}
      />
    </div>
  ),
  SettingsPasswordInput: ({ label, value, onChange }) => (
    <div>
      <label>{label}</label>
      <input 
        type="password"
        value={value || ''} 
        onChange={(e) => onChange(e.target.value)}
        data-testid={`password-${label}`}
      />
    </div>
  ),
  SettingsSelect: ({ label, value, onChange, options }) => (
    <div>
      <label>{label}</label>
      <select value={value} onChange={(e) => onChange(e.target.value)} data-testid={`select-${label}`}>
        {options.map((opt) => (
          <option key={opt.value} value={opt.value}>{opt.label}</option>
        ))}
      </select>
    </div>
  ),
  SettingsButton: ({ children, onClick, disabled }) => (
    <button onClick={onClick} disabled={disabled} data-testid="button">{children}</button>
  ),
  SettingsAlert: ({ children, variant }) => (
    <div data-testid="alert" data-variant={variant}>{children}</div>
  )
}));

describe('AzureBlobStoreSettings', () => {
  const mockUseAzureConnectionTest = useBlobStoresModule.useAzureConnectionTest as jest.Mock;
  const defaultProps = {
    data: {
      name: 'test-azure',
      bucketConfiguration: {
        accountName: 'myaccount',
        containerName: 'mycontainer',
        authentication: {
          authenticationMethod: 'ENVIRONMENTVARIABLE'
        }
      }
    },
    onChange: jest.fn(),
    disabled: false,
    isEdit: false
  };

  const mockTestConnection = jest.fn();
  const mockReset = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    
    mockUseAzureConnectionTest.mockReturnValue({
      testing: false,
      result: null,
      testConnection: mockTestConnection,
      reset: mockReset
    });
  });

  it('renders the component', () => {
    render(<AzureBlobStoreSettings {...defaultProps} />);
    expect(screen.getByText('Azure Blob Storage Configuration')).toBeInTheDocument();
  });

  it('displays account name input', () => {
    render(<AzureBlobStoreSettings {...defaultProps} />);
    const input = screen.getByTestId('input-Account Name');
    expect(input).toHaveValue('myaccount');
  });

  it('displays container name input', () => {
    render(<AzureBlobStoreSettings {...defaultProps} />);
    const input = screen.getByTestId('input-Container Name');
    expect(input).toHaveValue('mycontainer');
  });

  it('calls onChange when account name is updated', () => {
    render(<AzureBlobStoreSettings {...defaultProps} />);
    const input = screen.getByTestId('input-Account Name');
    
    fireEvent.change(input, { target: { value: 'newaccount' } });
    
    expect(defaultProps.onChange).toHaveBeenCalledWith('bucketConfiguration.accountName', 'newaccount');
    expect(mockReset).toHaveBeenCalled();
  });

  it('displays authentication method selector', () => {
    render(<AzureBlobStoreSettings {...defaultProps} />);
    expect(screen.getByTestId('select-Authentication Method')).toBeInTheDocument();
  });

  it('shows account key field when ACCOUNTKEY method is selected', () => {
    const propsWithAccountKey = {
      ...defaultProps,
      data: {
        ...defaultProps.data,
        bucketConfiguration: {
          ...defaultProps.data.bucketConfiguration,
          authentication: {
            authenticationMethod: 'ACCOUNTKEY'
          }
        }
      }
    };

    render(<AzureBlobStoreSettings {...propsWithAccountKey} />);
    expect(screen.getByTestId('password-Account Key')).toBeInTheDocument();
  });

  it('hides account key field for other authentication methods', () => {
    render(<AzureBlobStoreSettings {...defaultProps} />);
    expect(screen.queryByTestId('password-Account Key')).not.toBeInTheDocument();
  });

  it('renders test connection button', () => {
    render(<AzureBlobStoreSettings {...defaultProps} />);
    expect(screen.getByText('Test Connection')).toBeInTheDocument();
  });

  it('calls testConnection when test button is clicked', () => {
    render(<AzureBlobStoreSettings {...defaultProps} />);
    const button = screen.getByText('Test Connection');
    
    fireEvent.click(button);
    
    expect(mockTestConnection).toHaveBeenCalledWith({
      blobStoreName: undefined,
      accountName: 'myaccount',
      containerName: 'mycontainer',
      authenticationMethod: 'ENVIRONMENTVARIABLE',
      accountKey: undefined
    });
  });

  it('shows success alert when connection test succeeds', () => {
    mockUseAzureConnectionTest.mockReturnValue({
      testing: false,
      result: 'success',
      testConnection: mockTestConnection,
      reset: mockReset
    });

    render(<AzureBlobStoreSettings {...defaultProps} />);
    expect(screen.getByText('Connection succeeded')).toBeInTheDocument();
  });

  it('shows error alert when connection test fails', () => {
    mockUseAzureConnectionTest.mockReturnValue({
      testing: false,
      result: 'error',
      testConnection: mockTestConnection,
      reset: mockReset
    });

    render(<AzureBlobStoreSettings {...defaultProps} />);
    expect(screen.getByText(/Connection failed/)).toBeInTheDocument();
  });

  it('shows loading state when testing', () => {
    mockUseAzureConnectionTest.mockReturnValue({
      testing: true,
      result: null,
      testConnection: mockTestConnection,
      reset: mockReset
    });

    render(<AzureBlobStoreSettings {...defaultProps} />);
    expect(screen.getByText('Testing connection...')).toBeInTheDocument();
  });
});

