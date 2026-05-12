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
import { SettingsPasswordInput } from '../SettingsPasswordInput';

describe('SettingsPasswordInput', () => {
  const defaultProps = {
    name: 'test-password',
    label: 'Password',
    value: '',
    onChange: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('rendering', () => {
    it('renders with label', () => {
      render(<SettingsPasswordInput {...defaultProps} />);
      expect(screen.getByLabelText('Password')).toBeInTheDocument();
    });

    it('renders as password type by default', () => {
      render(<SettingsPasswordInput {...defaultProps} />);
      expect(screen.getByLabelText('Password')).toHaveAttribute('type', 'password');
    });

    it('renders with placeholder', () => {
      render(<SettingsPasswordInput {...defaultProps} placeholder="Enter password" />);
      expect(screen.getByPlaceholderText('Enter password')).toBeInTheDocument();
    });

    it('renders with help text', () => {
      render(<SettingsPasswordInput {...defaultProps} helpText="Minimum 8 characters" />);
      expect(screen.getByText('Minimum 8 characters')).toBeInTheDocument();
    });

    it('renders with error message', () => {
      render(<SettingsPasswordInput {...defaultProps} error="Password is required" />);
      expect(screen.getByText('Password is required')).toBeInTheDocument();
    });

    it('hides help text when error is present', () => {
      render(
        <SettingsPasswordInput
          {...defaultProps}
          helpText="Help text"
          error="Error message"
        />
      );
      expect(screen.queryByText('Help text')).not.toBeInTheDocument();
      expect(screen.getByText('Error message')).toBeInTheDocument();
    });

    it('shows required indicator when required', () => {
      render(<SettingsPasswordInput {...defaultProps} required />);
      expect(screen.getByText('*')).toBeInTheDocument();
    });

    it('applies error styling when error is present', () => {
      const { container } = render(
        <SettingsPasswordInput {...defaultProps} error="Error" />
      );
      expect(container.querySelector('.settings-password-input--error')).toBeInTheDocument();
    });

    it('applies custom className', () => {
      const { container } = render(
        <SettingsPasswordInput {...defaultProps} className="custom-class" />
      );
      expect(container.querySelector('.custom-class')).toBeInTheDocument();
    });
  });

  describe('visibility toggle', () => {
    it('renders show password button', () => {
      render(<SettingsPasswordInput {...defaultProps} />);
      expect(screen.getByRole('button', { name: 'Show password' })).toBeInTheDocument();
    });

    it('toggles to text type when show password is clicked', () => {
      render(<SettingsPasswordInput {...defaultProps} />);

      fireEvent.click(screen.getByRole('button', { name: 'Show password' }));

      expect(screen.getByLabelText('Password')).toHaveAttribute('type', 'text');
    });

    it('changes button label after showing password', () => {
      render(<SettingsPasswordInput {...defaultProps} />);

      fireEvent.click(screen.getByRole('button', { name: 'Show password' }));

      expect(screen.getByRole('button', { name: 'Hide password' })).toBeInTheDocument();
    });

    it('toggles back to password type when hide is clicked', () => {
      render(<SettingsPasswordInput {...defaultProps} />);

      // Show
      fireEvent.click(screen.getByRole('button', { name: 'Show password' }));
      expect(screen.getByLabelText('Password')).toHaveAttribute('type', 'text');

      // Hide
      fireEvent.click(screen.getByRole('button', { name: 'Hide password' }));
      expect(screen.getByLabelText('Password')).toHaveAttribute('type', 'password');
    });
  });

  describe('user interactions', () => {
    it('calls onChange when value changes', () => {
      const onChange = jest.fn();
      render(<SettingsPasswordInput {...defaultProps} onChange={onChange} />);

      const input = screen.getByLabelText('Password');
      fireEvent.change(input, { target: { value: 'secret123' } });

      expect(onChange).toHaveBeenCalledWith('secret123', expect.any(Object));
    });

    it('calls onBlur when input loses focus', () => {
      const onBlur = jest.fn();
      render(<SettingsPasswordInput {...defaultProps} onBlur={onBlur} />);

      const input = screen.getByLabelText('Password');
      fireEvent.blur(input);

      expect(onBlur).toHaveBeenCalled();
    });

    it('calls onFocus when input gains focus', () => {
      const onFocus = jest.fn();
      render(<SettingsPasswordInput {...defaultProps} onFocus={onFocus} />);

      const input = screen.getByLabelText('Password');
      fireEvent.focus(input);

      expect(onFocus).toHaveBeenCalled();
    });
  });

  describe('disabled state', () => {
    it('disables input when disabled prop is true', () => {
      render(<SettingsPasswordInput {...defaultProps} disabled />);
      expect(screen.getByLabelText('Password')).toBeDisabled();
    });
  });

  describe('autoComplete', () => {
    it('sets default autoComplete to current-password', () => {
      render(<SettingsPasswordInput {...defaultProps} />);
      expect(screen.getByLabelText('Password')).toHaveAttribute('autocomplete', 'current-password');
    });

    it('allows custom autoComplete value', () => {
      render(<SettingsPasswordInput {...defaultProps} autoComplete="new-password" />);
      expect(screen.getByLabelText('Password')).toHaveAttribute('autocomplete', 'new-password');
    });
  });

  describe('accessibility', () => {
    it('associates label with input via htmlFor', () => {
      render(<SettingsPasswordInput {...defaultProps} />);
      const input = screen.getByLabelText(/Password/);
      expect(input).toHaveAttribute('id', 'settings-input-test-password');
    });

    it('sets aria-invalid when error is present', () => {
      render(<SettingsPasswordInput {...defaultProps} error="Error" />);
      expect(screen.getByLabelText(/Password/)).toHaveAttribute('aria-invalid', 'true');
    });

    it('sets aria-describedby for help text', () => {
      render(<SettingsPasswordInput {...defaultProps} helpText="Help" />);
      const input = screen.getByLabelText(/Password/);
      expect(input).toHaveAttribute('aria-describedby', expect.stringContaining('help'));
    });

    it('sets aria-describedby for error text', () => {
      render(<SettingsPasswordInput {...defaultProps} error="Error" />);
      const input = screen.getByLabelText(/Password/);
      expect(input).toHaveAttribute('aria-describedby', expect.stringContaining('error'));
    });

    it('sets required attribute when required', () => {
      render(<SettingsPasswordInput {...defaultProps} required />);
      expect(screen.getByLabelText(/Password/)).toHaveAttribute('required');
    });

    it('visibility toggle has appropriate aria-label', () => {
      render(<SettingsPasswordInput {...defaultProps} />);
      expect(screen.getByRole('button', { name: 'Show password' })).toBeInTheDocument();
    });
  });

  describe('ref forwarding', () => {
    it('forwards ref to input element', () => {
      const ref = React.createRef<HTMLInputElement>();
      render(<SettingsPasswordInput {...defaultProps} inputRef={ref} />);
      expect(ref.current).toBeInstanceOf(HTMLInputElement);
    });
  });
});

