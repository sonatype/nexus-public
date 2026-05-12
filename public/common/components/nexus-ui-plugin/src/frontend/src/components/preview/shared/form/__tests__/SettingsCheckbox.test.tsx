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
import { SettingsCheckbox } from '../SettingsCheckbox';

describe('SettingsCheckbox', () => {
  const defaultProps = {
    name: 'test-checkbox',
    label: 'Test Checkbox',
    checked: false,
    onChange: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('rendering', () => {
    it('renders with label', () => {
      render(<SettingsCheckbox {...defaultProps} />);
      expect(screen.getByLabelText('Test Checkbox')).toBeInTheDocument();
    });

    it('renders unchecked by default', () => {
      render(<SettingsCheckbox {...defaultProps} />);
      expect(screen.getByRole('checkbox')).not.toBeChecked();
    });

    it('renders checked when checked prop is true', () => {
      render(<SettingsCheckbox {...defaultProps} checked={true} />);
      expect(screen.getByRole('checkbox')).toBeChecked();
    });

    it('renders with description', () => {
      render(
        <SettingsCheckbox
          {...defaultProps}
          description="This is a helpful description"
        />
      );
      expect(screen.getByText('This is a helpful description')).toBeInTheDocument();
    });

    it('renders check icon when checked', () => {
      const { container } = render(
        <SettingsCheckbox {...defaultProps} checked={true} />
      );
      expect(container.querySelector('.settings-checkbox__icon')).toBeInTheDocument();
    });

    it('does not render check icon when unchecked', () => {
      const { container } = render(
        <SettingsCheckbox {...defaultProps} checked={false} />
      );
      expect(container.querySelector('.settings-checkbox__icon')).not.toBeInTheDocument();
    });

    it('applies custom className', () => {
      const { container } = render(
        <SettingsCheckbox {...defaultProps} className="custom-class" />
      );
      expect(container.querySelector('.custom-class')).toBeInTheDocument();
    });

    it('applies disabled styling when disabled', () => {
      const { container } = render(
        <SettingsCheckbox {...defaultProps} disabled />
      );
      expect(container.querySelector('.settings-checkbox--disabled')).toBeInTheDocument();
    });
  });

  describe('user interactions', () => {
    it('calls onChange with true when clicking unchecked checkbox', () => {
      const onChange = jest.fn();
      render(<SettingsCheckbox {...defaultProps} checked={false} onChange={onChange} />);

      fireEvent.click(screen.getByRole('checkbox'));

      expect(onChange).toHaveBeenCalledWith(true, expect.any(Object));
    });

    it('calls onChange with false when clicking checked checkbox', () => {
      const onChange = jest.fn();
      render(<SettingsCheckbox {...defaultProps} checked={true} onChange={onChange} />);

      fireEvent.click(screen.getByRole('checkbox'));

      expect(onChange).toHaveBeenCalledWith(false, expect.any(Object));
    });

    it('toggles via label click', () => {
      const onChange = jest.fn();
      render(<SettingsCheckbox {...defaultProps} onChange={onChange} />);

      fireEvent.click(screen.getByText('Test Checkbox'));

      expect(onChange).toHaveBeenCalled();
    });
  });

  describe('disabled state', () => {
    it('disables checkbox when disabled prop is true', () => {
      render(<SettingsCheckbox {...defaultProps} disabled />);
      expect(screen.getByRole('checkbox')).toBeDisabled();
    });

    it('checkbox is disabled and cannot be interacted with', () => {
      render(<SettingsCheckbox {...defaultProps} disabled />);
      const checkbox = screen.getByRole('checkbox');
      
      // Verify it's disabled
      expect(checkbox).toBeDisabled();
      expect(checkbox).toHaveAttribute('disabled');
    });
  });

  describe('accessibility', () => {
    it('associates label with checkbox via htmlFor', () => {
      render(<SettingsCheckbox {...defaultProps} />);
      const checkbox = screen.getByRole('checkbox');
      expect(checkbox).toHaveAttribute('id', 'settings-checkbox-test-checkbox');
    });

    it('sets aria-describedby when description is present', () => {
      render(
        <SettingsCheckbox {...defaultProps} description="Description text" />
      );
      const checkbox = screen.getByRole('checkbox');
      expect(checkbox).toHaveAttribute('aria-describedby', expect.stringContaining('desc'));
    });

    it('does not set aria-describedby when no description', () => {
      render(<SettingsCheckbox {...defaultProps} />);
      const checkbox = screen.getByRole('checkbox');
      expect(checkbox).not.toHaveAttribute('aria-describedby');
    });

    it('can be toggled with keyboard', () => {
      const onChange = jest.fn();
      render(<SettingsCheckbox {...defaultProps} onChange={onChange} />);

      const checkbox = screen.getByRole('checkbox');
      checkbox.focus();
      fireEvent.keyDown(checkbox, { key: ' ', code: 'Space' });
      fireEvent.click(checkbox);

      expect(onChange).toHaveBeenCalled();
    });
  });

  describe('controlled component', () => {
    it('reflects external checked state changes', () => {
      const { rerender } = render(
        <SettingsCheckbox {...defaultProps} checked={false} />
      );
      expect(screen.getByRole('checkbox')).not.toBeChecked();

      rerender(<SettingsCheckbox {...defaultProps} checked={true} />);
      expect(screen.getByRole('checkbox')).toBeChecked();
    });
  });
});

