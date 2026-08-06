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
import { render, screen, fireEvent, } from '@testing-library/react';
import AzureBlobStoreSettings from '../AzureBlobStoreSettings';
import * as useBlobStoresModule from '../useBlobStores';

jest.mock('../useBlobStores', () => ({
  useAzureConnectionTest: jest.fn(),
}));

// Mock ExtJS at the path the source uses. Individual tests can override via
// jest.requireMock so we can flip isProEdition per case.
jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    isProEdition: jest.fn(() => true),
    state: jest.fn(() => ({
      getValue: (key: string) => key === 'azureSasUrlEnabled' ? true : null
    })),
  }
}));

// Mock shared form components
jest.mock('../../../../../shared/form', () => ({
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
  SettingsCheckbox: ({ label, checked, onChange }) => (
    <div>
      <label>
        <input
          type="checkbox"
          checked={checked}
          onChange={(e) => onChange(e.target.checked)}
          data-testid={`checkbox-${label}`}
        />
        {label}
      </label>
    </div>
  ),
  SettingsButton: ({ children, onClick, disabled }) => (
    <button onClick={onClick} disabled={disabled} data-testid="button">{children}</button>
  ),
  SettingsAlert: ({ children, variant, type }) => (
    <div data-testid="alert" data-variant={variant} data-type={type}>{children}</div>
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

  describe('Direct Download (SAS URLs) checkbox', () => {
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const extjs = require('../../../../../../../interface/ExtJS').ExtJS;

    beforeEach(() => {
      extjs.isProEdition.mockReturnValue(true);
    });

    it('renders the checkbox on Pro edition', () => {
      render(<AzureBlobStoreSettings {...defaultProps} />);
      expect(screen.getByTestId('checkbox-Direct Download (SAS URLs)')).toBeInTheDocument();
    });

    it('does not render the checkbox on non-Pro editions', () => {
      extjs.isProEdition.mockReturnValue(false);
      render(<AzureBlobStoreSettings {...defaultProps} />);
      expect(screen.queryByTestId('checkbox-Direct Download (SAS URLs)')).not.toBeInTheDocument();
    });

    it('reflects preSignedUrlEnabled=true from config', () => {
      const props = {
        ...defaultProps,
        data: {
          ...defaultProps.data,
          bucketConfiguration: {
            ...defaultProps.data.bucketConfiguration,
            preSignedUrlEnabled: true,
          }
        }
      };
      render(<AzureBlobStoreSettings {...props} />);
      expect(screen.getByTestId('checkbox-Direct Download (SAS URLs)')).toBeChecked();
    });

    it('reflects preSignedUrlEnabled=false when unset', () => {
      render(<AzureBlobStoreSettings {...defaultProps} />);
      expect(screen.getByTestId('checkbox-Direct Download (SAS URLs)')).not.toBeChecked();
    });

    it('calls onChange with the preSignedUrlEnabled path when toggled', () => {
      render(<AzureBlobStoreSettings {...defaultProps} />);
      const checkbox = screen.getByTestId('checkbox-Direct Download (SAS URLs)');
      fireEvent.click(checkbox);
      expect(defaultProps.onChange).toHaveBeenCalledWith(
        'bucketConfiguration.preSignedUrlEnabled',
        true
      );
    });

    describe('RBAC info note', () => {
      const noteText = /Storage Blob Delegator/;

      it('shows the note when enabled with MANAGEDIDENTITY auth', () => {
        const props = {
          ...defaultProps,
          data: {
            ...defaultProps.data,
            bucketConfiguration: {
              ...defaultProps.data.bucketConfiguration,
              preSignedUrlEnabled: true,
              authentication: { authenticationMethod: 'MANAGEDIDENTITY' },
            }
          }
        };
        render(<AzureBlobStoreSettings {...props} />);
        expect(screen.getByText(noteText)).toBeInTheDocument();
      });

      it('shows the note when enabled with ENVIRONMENTVARIABLE auth', () => {
        const props = {
          ...defaultProps,
          data: {
            ...defaultProps.data,
            bucketConfiguration: {
              ...defaultProps.data.bucketConfiguration,
              preSignedUrlEnabled: true,
              authentication: { authenticationMethod: 'ENVIRONMENTVARIABLE' },
            }
          }
        };
        render(<AzureBlobStoreSettings {...props} />);
        expect(screen.getByText(noteText)).toBeInTheDocument();
      });

      it('hides the note when enabled with ACCOUNTKEY auth', () => {
        const props = {
          ...defaultProps,
          data: {
            ...defaultProps.data,
            bucketConfiguration: {
              ...defaultProps.data.bucketConfiguration,
              preSignedUrlEnabled: true,
              authentication: { authenticationMethod: 'ACCOUNTKEY' },
            }
          }
        };
        render(<AzureBlobStoreSettings {...props} />);
        expect(screen.queryByText(noteText)).not.toBeInTheDocument();
      });

      it('hides the note when checkbox is off, even with MI auth', () => {
        const props = {
          ...defaultProps,
          data: {
            ...defaultProps.data,
            bucketConfiguration: {
              ...defaultProps.data.bucketConfiguration,
              preSignedUrlEnabled: false,
              authentication: { authenticationMethod: 'MANAGEDIDENTITY' },
            }
          }
        };
        render(<AzureBlobStoreSettings {...props} />);
        expect(screen.queryByText(noteText)).not.toBeInTheDocument();
      });

      it('hides the note on non-Pro editions even when enabled with MI', () => {
        extjs.isProEdition.mockReturnValue(false);
        const props = {
          ...defaultProps,
          data: {
            ...defaultProps.data,
            bucketConfiguration: {
              ...defaultProps.data.bucketConfiguration,
              preSignedUrlEnabled: true,
              authentication: { authenticationMethod: 'MANAGEDIDENTITY' },
            }
          }
        };
        render(<AzureBlobStoreSettings {...props} />);
        expect(screen.queryByText(noteText)).not.toBeInTheDocument();
      });
    });
  });
});

