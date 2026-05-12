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
import '@testing-library/jest-dom';
import { SettingsTextInput } from '../SettingsTextInput';

// Mock Radix UI components
jest.mock('@radix-ui/themes', () => ({
  Box: ({ children, className }) => <div className={className}>{children}</div>,
  Text: ({ children, className, as: Tag = 'span', id }) => <Tag className={className} id={id}>{children}</Tag>,
}));

// Mock lucide-react
jest.mock('lucide-react', () => ({
  AlertCircle: () => <span data-testid="alert-icon">!</span>,
}));

describe('SettingsTextInput', () => {
  const defaultProps = {
    name: 'test-input',
    value: '',
    onChange: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders input with correct name and id', () => {
    render(<SettingsTextInput {...defaultProps} />);
    
    const input = screen.getByRole('textbox');
    expect(input).toHaveAttribute('name', 'test-input');
    expect(input).toHaveAttribute('id', 'settings-input-test-input');
  });

  it('renders label when provided', () => {
    render(<SettingsTextInput {...defaultProps} label="Username" />);
    
    expect(screen.getByText('Username')).toBeInTheDocument();
    expect(screen.getByLabelText('Username')).toBeInTheDocument();
  });

  it('shows required indicator when required is true', () => {
    render(<SettingsTextInput {...defaultProps} label="Username" required />);
    
    expect(screen.getByText('*')).toBeInTheDocument();
  });

  it('displays value correctly', () => {
    render(<SettingsTextInput {...defaultProps} value="test value" />);
    
    expect(screen.getByRole('textbox')).toHaveValue('test value');
  });

  it('calls onChange with value when input changes', () => {
    const onChange = jest.fn();
    render(<SettingsTextInput {...defaultProps} onChange={onChange} />);
    
    fireEvent.change(screen.getByRole('textbox'), { target: { value: 'new value' } });
    
    expect(onChange).toHaveBeenCalledWith('new value', expect.any(Object));
  });

  it('renders placeholder text', () => {
    render(<SettingsTextInput {...defaultProps} placeholder="Enter text..." />);
    
    expect(screen.getByPlaceholderText('Enter text...')).toBeInTheDocument();
  });

  it('renders help text when provided', () => {
    render(<SettingsTextInput {...defaultProps} helpText="This is help text" />);
    
    expect(screen.getByText('This is help text')).toBeInTheDocument();
  });

  it('renders error message and hides help text when error is provided', () => {
    render(
      <SettingsTextInput 
        {...defaultProps} 
        helpText="This is help text"
        error="This is an error" 
      />
    );
    
    expect(screen.getByText('This is an error')).toBeInTheDocument();
    expect(screen.queryByText('This is help text')).not.toBeInTheDocument();
  });

  it('sets aria-invalid when error is present', () => {
    render(<SettingsTextInput {...defaultProps} error="Error message" />);
    
    expect(screen.getByRole('textbox')).toHaveAttribute('aria-invalid', 'true');
  });

  it('disables input when disabled is true', () => {
    render(<SettingsTextInput {...defaultProps} disabled />);
    
    expect(screen.getByRole('textbox')).toBeDisabled();
  });

  it('makes input read-only when readOnly is true', () => {
    render(<SettingsTextInput {...defaultProps} readOnly />);
    
    expect(screen.getByRole('textbox')).toHaveAttribute('readOnly');
  });

  it('supports different input types', () => {
    render(<SettingsTextInput {...defaultProps} type="email" />);
    
    expect(screen.getByRole('textbox')).toHaveAttribute('type', 'email');
  });

  it('supports number input with min, max, step', () => {
    render(
      <SettingsTextInput 
        {...defaultProps} 
        type="number" 
        min={0} 
        max={100} 
        step={5} 
      />
    );
    
    const input = screen.getByRole('spinbutton');
    expect(input).toHaveAttribute('min', '0');
    expect(input).toHaveAttribute('max', '100');
    expect(input).toHaveAttribute('step', '5');
  });

  it('supports maxLength', () => {
    render(<SettingsTextInput {...defaultProps} maxLength={50} />);
    
    expect(screen.getByRole('textbox')).toHaveAttribute('maxLength', '50');
  });

  it('supports autoComplete', () => {
    render(<SettingsTextInput {...defaultProps} autoComplete="email" />);
    
    expect(screen.getByRole('textbox')).toHaveAttribute('autocomplete', 'email');
  });

  it('calls onBlur when input loses focus', () => {
    const onBlur = jest.fn();
    render(<SettingsTextInput {...defaultProps} onBlur={onBlur} />);
    
    fireEvent.blur(screen.getByRole('textbox'));
    
    expect(onBlur).toHaveBeenCalledTimes(1);
  });

  it('calls onFocus when input gains focus', () => {
    const onFocus = jest.fn();
    render(<SettingsTextInput {...defaultProps} onFocus={onFocus} />);
    
    fireEvent.focus(screen.getByRole('textbox'));
    
    expect(onFocus).toHaveBeenCalledTimes(1);
  });

  it('applies custom className', () => {
    const { container } = render(<SettingsTextInput {...defaultProps} className="custom" />);
    
    expect(container.firstChild).toHaveClass('custom');
  });

  it('applies error class when error is present', () => {
    const { container } = render(<SettingsTextInput {...defaultProps} error="Error" />);
    
    expect(container.firstChild).toHaveClass('settings-text-input--error');
  });

  it('forwards ref to input element', () => {
    const ref = React.createRef();
    render(<SettingsTextInput {...defaultProps} inputRef={ref} />);
    
    expect(ref.current).toBeInstanceOf(HTMLInputElement);
  });

  it('sets aria-describedby when help text is provided', () => {
    render(<SettingsTextInput {...defaultProps} helpText="Help" />);
    
    expect(screen.getByRole('textbox')).toHaveAttribute(
      'aria-describedby', 
      'settings-help-test-input'
    );
  });

  it('sets aria-describedby when error is provided', () => {
    render(<SettingsTextInput {...defaultProps} error="Error" />);
    
    expect(screen.getByRole('textbox')).toHaveAttribute(
      'aria-describedby', 
      'settings-error-test-input'
    );
  });

  it('required asterisk has aria-hidden', () => {
    const { container } = render(<SettingsTextInput {...defaultProps} label="Name" required />);
    const asterisk = container.querySelector('.settings-text-input__required');
    expect(asterisk).toHaveAttribute('aria-hidden', 'true');
    expect(asterisk).toHaveTextContent('*');
  });

  it('accepts password type', () => {
    render(<SettingsTextInput {...defaultProps} type="password" />);
    const input = document.querySelector('input[type="password"]');
    expect(input).toBeInTheDocument();
  });
});

