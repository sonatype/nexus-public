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
import FileBlobStoreSettings from '../FileBlobStoreSettings';

// Mock shared form components
jest.mock('../../../../shared/form', () => ({
  SettingsFormSection: ({ children, title }) => (
    <div data-testid="settings-form-section">
      <h2>{title}</h2>
      {children}
    </div>
  ),
  SettingsTextInput: ({ label, value, onChange, helpText, placeholder, required, disabled }) => (
    <div>
      <label>{label}{required && ' *'}</label>
      <input 
        value={value || ''} 
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        disabled={disabled}
        data-testid={`input-${label}`}
      />
      {helpText && <span className="help">{helpText}</span>}
    </div>
  ),
  SettingsAlert: ({ children, variant }) => (
    <div data-testid="alert" data-variant={variant}>{children}</div>
  )
}));

describe('FileBlobStoreSettings', () => {
  const defaultProps = {
    data: { name: 'test-store', path: '/data/blobs/test' },
    onChange: jest.fn(),
    disabled: false,
    isEdit: false
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders the component', () => {
    render(<FileBlobStoreSettings {...defaultProps} />);
    expect(screen.getByText('File Storage Configuration')).toBeInTheDocument();
  });

  it('displays path input with current value', () => {
    render(<FileBlobStoreSettings {...defaultProps} />);
    const input = screen.getByTestId('input-Path');
    expect(input).toHaveValue('/data/blobs/test');
  });

  it('calls onChange when path is updated', () => {
    render(<FileBlobStoreSettings {...defaultProps} />);
    const input = screen.getByTestId('input-Path');
    
    fireEvent.change(input, { target: { value: '/new/path' } });
    
    expect(defaultProps.onChange).toHaveBeenCalledWith('path', '/new/path');
  });

  it('shows warning alert in edit mode', () => {
    render(<FileBlobStoreSettings {...defaultProps} isEdit={true} />);
    expect(screen.getByTestId('alert')).toBeInTheDocument();
    expect(screen.getByTestId('alert')).toHaveAttribute('data-variant', 'warning');
  });

  it('does not show warning alert in create mode', () => {
    render(<FileBlobStoreSettings {...defaultProps} isEdit={false} />);
    expect(screen.queryByTestId('alert')).not.toBeInTheDocument();
  });

  it('disables input when disabled prop is true', () => {
    render(<FileBlobStoreSettings {...defaultProps} disabled={true} />);
    const input = screen.getByTestId('input-Path');
    expect(input).toBeDisabled();
  });

  it('displays help text', () => {
    render(<FileBlobStoreSettings {...defaultProps} />);
    expect(screen.getByText(/An absolute path or a path relative/)).toBeInTheDocument();
  });
});

