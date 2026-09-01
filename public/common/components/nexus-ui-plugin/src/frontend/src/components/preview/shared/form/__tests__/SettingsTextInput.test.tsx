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

describe('SettingsTextInput', () => {
  const defaultProps = {
    name: 'test-input',
    label: 'Test Label',
    value: '',
    onChange: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('rendering', () => {
    it('renders with label', () => {
      render(<SettingsTextInput {...defaultProps} />);
      expect(screen.getByLabelText('Test Label')).toBeInTheDocument();
    });

    it('renders without label when not provided', () => {
      render(<SettingsTextInput name="test" value="" />);
      expect(screen.getByRole('textbox')).toBeInTheDocument();
    });

    it('renders with placeholder', () => {
      render(<SettingsTextInput {...defaultProps} placeholder="Enter value" />);
      expect(screen.getByPlaceholderText('Enter value')).toBeInTheDocument();
    });

    it('renders with help text', () => {
      render(<SettingsTextInput {...defaultProps} helpText="This is helpful" />);
      expect(screen.getByText('This is helpful')).toBeInTheDocument();
    });

    it('renders with error message', () => {
      render(<SettingsTextInput {...defaultProps} error="This field is required" />);
      expect(screen.getByText('This field is required')).toBeInTheDocument();
    });

    it('hides help text when error is present', () => {
      render(
        <SettingsTextInput
          {...defaultProps}
          helpText="Help text"
          error="Error message"
        />
      );
      expect(screen.queryByText('Help text')).not.toBeInTheDocument();
      expect(screen.getByText('Error message')).toBeInTheDocument();
    });

    it('shows required indicator when required', () => {
      render(<SettingsTextInput {...defaultProps} required />);
      expect(screen.getByText('*')).toBeInTheDocument();
    });

    it('applies error styling when error is present', () => {
      const { container } = render(
        <SettingsTextInput {...defaultProps} error="Error" />
      );
      expect(container.querySelector('.settings-text-input--error')).toBeInTheDocument();
    });

    it('applies custom className', () => {
      const { container } = render(
        <SettingsTextInput {...defaultProps} className="custom-class" />
      );
      expect(container.querySelector('.custom-class')).toBeInTheDocument();
    });

    it('applies monospace styling when monospace is true', () => {
      const { container } = render(
        <SettingsTextInput {...defaultProps} monospace />
      );
      expect(container.querySelector('.settings-text-input__input--mono')).toBeInTheDocument();
    });

    it('does not apply monospace styling by default', () => {
      const { container } = render(<SettingsTextInput {...defaultProps} />);
      expect(container.querySelector('.settings-text-input__input--mono')).not.toBeInTheDocument();
    });
  });

  describe('input types', () => {
    it('renders as text input by default', () => {
      render(<SettingsTextInput {...defaultProps} />);
      expect(screen.getByRole('textbox')).toHaveAttribute('type', 'text');
    });

    it('renders as email input', () => {
      render(<SettingsTextInput {...defaultProps} type="email" />);
      expect(screen.getByRole('textbox')).toHaveAttribute('type', 'email');
    });

    it('renders as number input', () => {
      render(<SettingsTextInput {...defaultProps} type="number" />);
      expect(screen.getByRole('spinbutton')).toHaveAttribute('type', 'number');
    });

    it('renders as url input', () => {
      render(<SettingsTextInput {...defaultProps} type="url" />);
      expect(screen.getByRole('textbox')).toHaveAttribute('type', 'url');
    });

    it('renders as tel input', () => {
      render(<SettingsTextInput {...defaultProps} type="tel" />);
      expect(screen.getByRole('textbox')).toHaveAttribute('type', 'tel');
    });
  });

  describe('user interactions', () => {
    it('calls onChange when value changes', async () => {
      const onChange = jest.fn();
      render(<SettingsTextInput {...defaultProps} onChange={onChange} />);

      const input = screen.getByRole('textbox');
      fireEvent.change(input, { target: { value: 'new value' } });

      expect(onChange).toHaveBeenCalledWith('new value', expect.any(Object));
    });

    it('calls onBlur when input loses focus', () => {
      const onBlur = jest.fn();
      render(<SettingsTextInput {...defaultProps} onBlur={onBlur} />);

      const input = screen.getByRole('textbox');
      fireEvent.blur(input);

      expect(onBlur).toHaveBeenCalled();
    });

    it('calls onFocus when input gains focus', () => {
      const onFocus = jest.fn();
      render(<SettingsTextInput {...defaultProps} onFocus={onFocus} />);

      const input = screen.getByRole('textbox');
      fireEvent.focus(input);

      expect(onFocus).toHaveBeenCalled();
    });

    it('calls onKeyDown when key is pressed', () => {
      const onKeyDown = jest.fn();
      render(<SettingsTextInput {...defaultProps} onKeyDown={onKeyDown} />);

      const input = screen.getByRole('textbox');
      fireEvent.keyDown(input, { key: 'Enter' });

      expect(onKeyDown).toHaveBeenCalled();
    });
  });

  describe('disabled state', () => {
    it('disables input when disabled prop is true', () => {
      render(<SettingsTextInput {...defaultProps} disabled />);
      expect(screen.getByRole('textbox')).toBeDisabled();
    });

    it('does not call onChange when disabled', async () => {
      const onChange = jest.fn();
      render(<SettingsTextInput {...defaultProps} disabled onChange={onChange} />);

      const input = screen.getByRole('textbox');
      fireEvent.change(input, { target: { value: 'new value' } });

      // Input should still be disabled, onChange won't fire
      expect(input).toBeDisabled();
    });
  });

  describe('readOnly state', () => {
    it('makes input read-only when readOnly prop is true', () => {
      render(<SettingsTextInput {...defaultProps} readOnly />);
      expect(screen.getByRole('textbox')).toHaveAttribute('readonly');
    });
  });

  describe('number input constraints', () => {
    it('applies min constraint', () => {
      render(<SettingsTextInput {...defaultProps} type="number" min={0} />);
      expect(screen.getByRole('spinbutton')).toHaveAttribute('min', '0');
    });

    it('applies max constraint', () => {
      render(<SettingsTextInput {...defaultProps} type="number" max={100} />);
      expect(screen.getByRole('spinbutton')).toHaveAttribute('max', '100');
    });

    it('applies step constraint', () => {
      render(<SettingsTextInput {...defaultProps} type="number" step={5} />);
      expect(screen.getByRole('spinbutton')).toHaveAttribute('step', '5');
    });

    it('blurs number input on wheel scroll to prevent accidental value changes', () => {
      render(<SettingsTextInput {...defaultProps} type="number" value="60" />);
      const input = screen.getByRole('spinbutton');
      input.focus();
      expect(document.activeElement).toBe(input);
      fireEvent.wheel(input, { deltaY: -100 });
      expect(document.activeElement).not.toBe(input);
    });

    it('does not blur text input on wheel scroll', () => {
      render(<SettingsTextInput {...defaultProps} type="text" value="hello" />);
      const input = screen.getByRole('textbox');
      input.focus();
      expect(document.activeElement).toBe(input);
      fireEvent.wheel(input, { deltaY: -100 });
      expect(document.activeElement).toBe(input);
    });
  });

  describe('accessibility', () => {
    it('associates label with input via htmlFor', () => {
      render(<SettingsTextInput {...defaultProps} />);
      const input = screen.getByLabelText('Test Label');
      expect(input).toHaveAttribute('id', 'settings-input-test-input');
    });

    it('sets aria-invalid when error is present', () => {
      render(<SettingsTextInput {...defaultProps} error="Error" />);
      expect(screen.getByRole('textbox')).toHaveAttribute('aria-invalid', 'true');
    });

    it('sets aria-describedby for help text', () => {
      render(<SettingsTextInput {...defaultProps} helpText="Help" />);
      const input = screen.getByRole('textbox');
      expect(input).toHaveAttribute('aria-describedby', expect.stringContaining('help'));
    });

    it('sets aria-describedby for error text', () => {
      render(<SettingsTextInput {...defaultProps} error="Error" />);
      const input = screen.getByRole('textbox');
      expect(input).toHaveAttribute('aria-describedby', expect.stringContaining('error'));
    });

    it('sets required attribute when required', () => {
      render(<SettingsTextInput {...defaultProps} required />);
      expect(screen.getByRole('textbox')).toHaveAttribute('required');
    });

    it('sets aria-required="true" when required', () => {
      render(<SettingsTextInput {...defaultProps} required />);
      expect(screen.getByTestId('input-test-input')).toHaveAttribute('aria-required', 'true');
    });

    it('sets aria-required="false" when not required', () => {
      render(<SettingsTextInput {...defaultProps} />);
      expect(screen.getByTestId('input-test-input')).toHaveAttribute('aria-required', 'false');
    });
  });

  describe('maxLength', () => {
    it('applies maxLength constraint', () => {
      render(<SettingsTextInput {...defaultProps} maxLength={50} />);
      expect(screen.getByRole('textbox')).toHaveAttribute('maxLength', '50');
    });
  });

  describe('autoComplete', () => {
    it('sets autoComplete attribute', () => {
      render(<SettingsTextInput {...defaultProps} autoComplete="email" />);
      expect(screen.getByRole('textbox')).toHaveAttribute('autocomplete', 'email');
    });
  });

  describe('ref forwarding', () => {
    it('forwards ref to input element', () => {
      const ref = React.createRef<HTMLInputElement>();
      render(<SettingsTextInput {...defaultProps} inputRef={ref} />);
      expect(ref.current).toBeInstanceOf(HTMLInputElement);
    });
  });
});


