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
import { SettingsTextArea } from '../SettingsTextArea';

describe('SettingsTextArea', () => {
  const defaultProps = {
    name: 'test-textarea',
    label: 'Test Label',
    value: '',
    onChange: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('rendering', () => {
    it('renders with label', () => {
      render(<SettingsTextArea {...defaultProps} />);
      expect(screen.getByLabelText('Test Label')).toBeInTheDocument();
    });

    it('renders without label when not provided', () => {
      render(<SettingsTextArea name="test" value="" />);
      expect(screen.getByRole('textbox')).toBeInTheDocument();
    });

    it('renders with placeholder', () => {
      render(<SettingsTextArea {...defaultProps} placeholder="Enter text" />);
      expect(screen.getByPlaceholderText('Enter text')).toBeInTheDocument();
    });

    it('renders with help text', () => {
      render(<SettingsTextArea {...defaultProps} helpText="This is helpful" />);
      expect(screen.getByText('This is helpful')).toBeInTheDocument();
    });

    it('renders with error message', () => {
      render(<SettingsTextArea {...defaultProps} error="This field is required" />);
      expect(screen.getByText('This field is required')).toBeInTheDocument();
    });

    it('hides help text when error is present', () => {
      render(
        <SettingsTextArea
          {...defaultProps}
          helpText="Help text"
          error="Error message"
        />
      );
      expect(screen.queryByText('Help text')).not.toBeInTheDocument();
      expect(screen.getByText('Error message')).toBeInTheDocument();
    });

    it('shows required indicator when required', () => {
      render(<SettingsTextArea {...defaultProps} required />);
      expect(screen.getByText('*')).toBeInTheDocument();
    });

    it('applies error styling when error is present', () => {
      const { container } = render(
        <SettingsTextArea {...defaultProps} error="Error" />
      );
      expect(container.querySelector('.settings-textarea--error')).toBeInTheDocument();
    });

    it('applies custom className', () => {
      const { container } = render(
        <SettingsTextArea {...defaultProps} className="custom-class" />
      );
      expect(container.querySelector('.custom-class')).toBeInTheDocument();
    });

    it('renders with default rows', () => {
      render(<SettingsTextArea {...defaultProps} />);
      expect(screen.getByRole('textbox')).toHaveAttribute('rows', '4');
    });

    it('renders with custom rows', () => {
      render(<SettingsTextArea {...defaultProps} rows={10} />);
      expect(screen.getByRole('textbox')).toHaveAttribute('rows', '10');
    });

    it('applies monospace styling when monospace is true', () => {
      const { container } = render(
        <SettingsTextArea {...defaultProps} monospace />
      );
      expect(container.querySelector('.settings-textarea__input--mono')).toBeInTheDocument();
    });
  });

  describe('user interactions', () => {
    it('calls onChange when value changes', () => {
      const onChange = jest.fn();
      render(<SettingsTextArea {...defaultProps} onChange={onChange} />);

      const textarea = screen.getByRole('textbox');
      fireEvent.change(textarea, { target: { value: 'new value' } });

      expect(onChange).toHaveBeenCalledWith('new value', expect.any(Object));
    });

    it('calls onBlur when textarea loses focus', () => {
      const onBlur = jest.fn();
      render(<SettingsTextArea {...defaultProps} onBlur={onBlur} />);

      const textarea = screen.getByRole('textbox');
      fireEvent.blur(textarea);

      expect(onBlur).toHaveBeenCalled();
    });

    it('calls onFocus when textarea gains focus', () => {
      const onFocus = jest.fn();
      render(<SettingsTextArea {...defaultProps} onFocus={onFocus} />);

      const textarea = screen.getByRole('textbox');
      fireEvent.focus(textarea);

      expect(onFocus).toHaveBeenCalled();
    });

    it('handles multi-line input', () => {
      const onChange = jest.fn();
      render(<SettingsTextArea {...defaultProps} onChange={onChange} />);

      const textarea = screen.getByRole('textbox');
      const multiLineValue = 'line1\nline2\nline3';
      fireEvent.change(textarea, { target: { value: multiLineValue } });

      expect(onChange).toHaveBeenCalledWith(multiLineValue, expect.any(Object));
    });
  });

  describe('disabled state', () => {
    it('disables textarea when disabled prop is true', () => {
      render(<SettingsTextArea {...defaultProps} disabled />);
      expect(screen.getByRole('textbox')).toBeDisabled();
    });
  });

  describe('readOnly state', () => {
    it('makes textarea read-only when readOnly prop is true', () => {
      render(<SettingsTextArea {...defaultProps} readOnly />);
      expect(screen.getByRole('textbox')).toHaveAttribute('readonly');
    });
  });

  describe('maxLength', () => {
    it('applies maxLength constraint', () => {
      render(<SettingsTextArea {...defaultProps} maxLength={500} />);
      expect(screen.getByRole('textbox')).toHaveAttribute('maxLength', '500');
    });
  });

  describe('accessibility', () => {
    it('associates label with textarea via htmlFor', () => {
      render(<SettingsTextArea {...defaultProps} />);
      const textarea = screen.getByLabelText('Test Label');
      expect(textarea).toHaveAttribute('id', 'settings-textarea-test-textarea');
    });

    it('sets aria-invalid when error is present', () => {
      render(<SettingsTextArea {...defaultProps} error="Error" />);
      expect(screen.getByRole('textbox')).toHaveAttribute('aria-invalid', 'true');
    });

    it('sets aria-describedby for help text', () => {
      render(<SettingsTextArea {...defaultProps} helpText="Help" />);
      const textarea = screen.getByRole('textbox');
      expect(textarea).toHaveAttribute('aria-describedby', expect.stringContaining('help'));
    });

    it('sets aria-describedby for error text', () => {
      render(<SettingsTextArea {...defaultProps} error="Error" />);
      const textarea = screen.getByRole('textbox');
      expect(textarea).toHaveAttribute('aria-describedby', expect.stringContaining('error'));
    });

    it('sets required attribute when required', () => {
      render(<SettingsTextArea {...defaultProps} required />);
      expect(screen.getByRole('textbox')).toHaveAttribute('required');
    });
  });

  describe('ref forwarding', () => {
    it('forwards ref to textarea element', () => {
      const ref = React.createRef<HTMLTextAreaElement>();
      render(<SettingsTextArea {...defaultProps} inputRef={ref} />);
      expect(ref.current).toBeInstanceOf(HTMLTextAreaElement);
    });
  });
});


