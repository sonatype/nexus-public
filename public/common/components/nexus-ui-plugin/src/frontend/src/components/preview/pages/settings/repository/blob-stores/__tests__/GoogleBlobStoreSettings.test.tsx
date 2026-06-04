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
import { render, screen, fireEvent } from '@testing-library/react';
import GoogleBlobStoreSettings from '../GoogleBlobStoreSettings';

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
  SettingsSelect: ({ label, value, onChange, options }) => (
    <div>
      <label>{label}</label>
      <select value={value || ''} onChange={(e) => onChange(e.target.value)} data-testid={`select-${label}`}>
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
  SettingsAlert: ({ children }) => <div data-testid="alert">{children}</div>
}));

describe('GoogleBlobStoreSettings', () => {
  const defaultProps = {
    data: {
      name: 'test-google',
      bucketConfiguration: {
        bucket: {
          projectId: 'my-project',
          name: 'my-bucket',
          prefix: 'nexus/'
        },
        bucketSecurity: {
          authenticationMethod: 'applicationDefault'
        },
        encryption: {
          encryptionType: 'default'
        }
      }
    },
    onChange: jest.fn(),
    disabled: false,
    isEdit: false
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders the component', () => {
    render(<GoogleBlobStoreSettings {...defaultProps} />);
    expect(screen.getByText('Google Cloud Storage Configuration')).toBeInTheDocument();
  });

  it('displays project ID input', () => {
    render(<GoogleBlobStoreSettings {...defaultProps} />);
    const input = screen.getByTestId('input-Project ID');
    expect(input).toHaveValue('my-project');
  });

  it('displays bucket name input', () => {
    render(<GoogleBlobStoreSettings {...defaultProps} />);
    const input = screen.getByTestId('input-Bucket');
    expect(input).toHaveValue('my-bucket');
  });

  it('displays prefix input', () => {
    render(<GoogleBlobStoreSettings {...defaultProps} />);
    const input = screen.getByTestId('input-Prefix');
    expect(input).toHaveValue('nexus/');
  });

  it('calls onChange when bucket name is updated', () => {
    render(<GoogleBlobStoreSettings {...defaultProps} />);
    const input = screen.getByTestId('input-Bucket');
    
    fireEvent.change(input, { target: { value: 'new-bucket' } });
    
    expect(defaultProps.onChange).toHaveBeenCalledWith('bucketConfiguration.bucket.name', 'new-bucket');
  });

  it('renders authentication section', () => {
    render(<GoogleBlobStoreSettings {...defaultProps} />);
    expect(screen.getByText('Authentication')).toBeInTheDocument();
  });

  it('displays authentication method selector', () => {
    render(<GoogleBlobStoreSettings {...defaultProps} />);
    expect(screen.getByTestId('select-Authentication Method')).toBeInTheDocument();
  });

  it('shows file upload when accountKey method is selected', () => {
    const propsWithAccountKey = {
      ...defaultProps,
      data: {
        ...defaultProps.data,
        bucketConfiguration: {
          ...defaultProps.data.bucketConfiguration,
          bucketSecurity: {
            authenticationMethod: 'accountKey'
          }
        }
      }
    };

    render(<GoogleBlobStoreSettings {...propsWithAccountKey} />);
    expect(screen.getByText('Choose File')).toBeInTheDocument();
  });

  it('renders encryption section', () => {
    render(<GoogleBlobStoreSettings {...defaultProps} />);
    expect(screen.getByText('Encryption')).toBeInTheDocument();
  });

  it('displays KMS checkbox', () => {
    render(<GoogleBlobStoreSettings {...defaultProps} />);
    expect(screen.getByTestId('checkbox-Enable KMS managed encryption')).toBeInTheDocument();
  });

  it('shows KMS key input when KMS is enabled', () => {
    const propsWithKms = {
      ...defaultProps,
      data: {
        ...defaultProps.data,
        bucketConfiguration: {
          ...defaultProps.data.bucketConfiguration,
          encryption: {
            encryptionType: 'kmsManagedEncryption',
            encryptionKey: 'my-key'
          }
        }
      }
    };

    render(<GoogleBlobStoreSettings {...propsWithKms} />);
    expect(screen.getByTestId('input-KMS Key ID')).toBeInTheDocument();
  });

  it('hides KMS key input when KMS is disabled', () => {
    render(<GoogleBlobStoreSettings {...defaultProps} />);
    expect(screen.queryByTestId('input-KMS Key ID')).not.toBeInTheDocument();
  });

  it('toggles KMS encryption', () => {
    render(<GoogleBlobStoreSettings {...defaultProps} />);
    const checkbox = screen.getByTestId('checkbox-Enable KMS managed encryption');
    
    fireEvent.click(checkbox);
    
    expect(defaultProps.onChange).toHaveBeenCalledWith(
      'bucketConfiguration.encryption.encryptionType',
      'kmsManagedEncryption'
    );
  });

  it('shows region info alert', () => {
    render(<GoogleBlobStoreSettings {...defaultProps} />);
    expect(screen.getByText(/The region is automatically set/)).toBeInTheDocument();
  });
});

