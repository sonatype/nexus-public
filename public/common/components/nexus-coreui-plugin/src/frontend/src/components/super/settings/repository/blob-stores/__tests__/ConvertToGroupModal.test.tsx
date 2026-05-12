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
import ConvertToGroupModal from '../ConvertToGroupModal';

// Mock shared form components
jest.mock('../../../../shared/form', () => ({
  SettingsTextInput: ({ label, value, onChange, error }) => (
    <div>
      <label>{label}</label>
      <input 
        value={value} 
        onChange={(e) => onChange(e.target.value)}
        data-testid="name-input"
      />
      {error && <span data-testid="error">{error}</span>}
    </div>
  ),
  SettingsAlert: ({ children }) => <div data-testid="alert">{children}</div>,
  SettingsButton: ({ children, onClick, disabled, variant }) => (
    <button 
      onClick={onClick} 
      disabled={disabled}
      data-testid={`button-${variant || 'default'}`}
    >
      {children}
    </button>
  )
}));

describe('ConvertToGroupModal', () => {
  const defaultProps = {
    blobStoreName: 'my-blob-store',
    onConfirm: jest.fn(),
    onCancel: jest.fn(),
    promoting: false
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders the modal', () => {
    render(<ConvertToGroupModal {...defaultProps} />);
    expect(screen.getByText('Convert to Group Blob Store')).toBeInTheDocument();
  });

  it('displays warning alert', () => {
    render(<ConvertToGroupModal {...defaultProps} />);
    expect(screen.getByTestId('alert')).toBeInTheDocument();
    expect(screen.getByText(/This action cannot be undone/)).toBeInTheDocument();
  });

  it('pre-fills name with original-suffix', () => {
    render(<ConvertToGroupModal {...defaultProps} />);
    const input = screen.getByTestId('name-input');
    expect(input).toHaveValue('my-blob-store-original');
  });

  it('calls onCancel when cancel button is clicked', () => {
    render(<ConvertToGroupModal {...defaultProps} />);
    const cancelButton = screen.getByTestId('button-ghost');
    
    fireEvent.click(cancelButton);
    
    expect(defaultProps.onCancel).toHaveBeenCalled();
  });

  it('calls onConfirm with new name when convert button is clicked', () => {
    render(<ConvertToGroupModal {...defaultProps} />);
    const convertButton = screen.getByTestId('button-primary');
    
    fireEvent.click(convertButton);
    
    expect(defaultProps.onConfirm).toHaveBeenCalledWith('my-blob-store-original');
  });

  it('shows error when name is empty', () => {
    render(<ConvertToGroupModal {...defaultProps} />);
    const input = screen.getByTestId('name-input');
    const convertButton = screen.getByTestId('button-primary');
    
    fireEvent.change(input, { target: { value: '' } });
    fireEvent.click(convertButton);
    
    expect(convertButton).toBeDisabled();
    expect(defaultProps.onConfirm).not.toHaveBeenCalled();
  });

  it('shows error when name contains invalid characters', () => {
    render(<ConvertToGroupModal {...defaultProps} />);
    const input = screen.getByTestId('name-input');
    const convertButton = screen.getByTestId('button-primary');
    
    fireEvent.change(input, { target: { value: 'invalid name!' } });
    fireEvent.click(convertButton);
    
    expect(screen.getByTestId('error')).toHaveTextContent(/can only contain/);
    expect(defaultProps.onConfirm).not.toHaveBeenCalled();
  });

  it('disables buttons when promoting', () => {
    render(<ConvertToGroupModal {...defaultProps} promoting={true} />);
    
    expect(screen.getByTestId('button-ghost')).toBeDisabled();
    expect(screen.getByTestId('button-primary')).toBeDisabled();
  });

  it('shows converting text when promoting', () => {
    render(<ConvertToGroupModal {...defaultProps} promoting={true} />);
    expect(screen.getByText('Converting...')).toBeInTheDocument();
  });

  it('clears error when name is updated', () => {
    render(<ConvertToGroupModal {...defaultProps} />);
    const input = screen.getByTestId('name-input');
    const convertButton = screen.getByTestId('button-primary');
    
    // Trigger error with invalid characters
    fireEvent.change(input, { target: { value: 'invalid name!' } });
    fireEvent.click(convertButton);
    expect(screen.getByTestId('error')).toBeInTheDocument();
    
    // Fix the name
    fireEvent.change(input, { target: { value: 'valid-name' } });
    expect(screen.queryByTestId('error')).not.toBeInTheDocument();
  });
});

