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
import { SettingsToggle } from '../SettingsToggle';

describe('SettingsToggle', () => {
  const defaultProps = {
    name: 'test-toggle',
    label: 'Test Toggle',
    checked: false,
    onChange: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('rendering', () => {
    it('renders with label', () => {
      render(<SettingsToggle {...defaultProps} />);
      expect(screen.getByText('Test Toggle')).toBeInTheDocument();
    });

    it('renders with role="switch"', () => {
      render(<SettingsToggle {...defaultProps} />);
      expect(screen.getByRole('switch')).toBeInTheDocument();
    });

    it('renders unchecked by default', () => {
      render(<SettingsToggle {...defaultProps} />);
      expect(screen.getByRole('switch')).toHaveAttribute('aria-checked', 'false');
    });

    it('renders checked when checked prop is true', () => {
      render(<SettingsToggle {...defaultProps} checked={true} />);
      expect(screen.getByRole('switch')).toHaveAttribute('aria-checked', 'true');
    });

    it('renders with description', () => {
      render(
        <SettingsToggle
          {...defaultProps}
          description="This is a helpful description"
        />
      );
      expect(screen.getByText('This is a helpful description')).toBeInTheDocument();
    });

    it('applies checked styling when checked', () => {
      const { container } = render(
        <SettingsToggle {...defaultProps} checked={true} />
      );
      expect(container.querySelector('.settings-toggle__switch--checked')).toBeInTheDocument();
    });

    it('applies custom className', () => {
      const { container } = render(
        <SettingsToggle {...defaultProps} className="custom-class" />
      );
      expect(container.querySelector('.custom-class')).toBeInTheDocument();
    });

    it('applies disabled styling when disabled', () => {
      const { container } = render(
        <SettingsToggle {...defaultProps} disabled />
      );
      expect(container.querySelector('.settings-toggle--disabled')).toBeInTheDocument();
    });
  });

  describe('user interactions', () => {
    it('calls onChange with true when clicking unchecked toggle', () => {
      const onChange = jest.fn();
      render(<SettingsToggle {...defaultProps} checked={false} onChange={onChange} />);

      fireEvent.click(screen.getByRole('switch'));

      expect(onChange).toHaveBeenCalledWith(true);
    });

    it('calls onChange with false when clicking checked toggle', () => {
      const onChange = jest.fn();
      render(<SettingsToggle {...defaultProps} checked={true} onChange={onChange} />);

      fireEvent.click(screen.getByRole('switch'));

      expect(onChange).toHaveBeenCalledWith(false);
    });

    it('toggles via label click', () => {
      const onChange = jest.fn();
      render(<SettingsToggle {...defaultProps} onChange={onChange} />);

      fireEvent.click(screen.getByText('Test Toggle'));

      expect(onChange).toHaveBeenCalledWith(true);
    });
  });

  describe('keyboard interactions', () => {
    it('toggles with Enter key', () => {
      const onChange = jest.fn();
      render(<SettingsToggle {...defaultProps} onChange={onChange} />);

      const toggle = screen.getByRole('switch');
      fireEvent.keyDown(toggle, { key: 'Enter' });

      expect(onChange).toHaveBeenCalledWith(true);
    });

    it('toggles with Space key', () => {
      const onChange = jest.fn();
      render(<SettingsToggle {...defaultProps} onChange={onChange} />);

      const toggle = screen.getByRole('switch');
      fireEvent.keyDown(toggle, { key: ' ' });

      expect(onChange).toHaveBeenCalledWith(true);
    });

    it('does not toggle with other keys', () => {
      const onChange = jest.fn();
      render(<SettingsToggle {...defaultProps} onChange={onChange} />);

      const toggle = screen.getByRole('switch');
      fireEvent.keyDown(toggle, { key: 'Tab' });

      expect(onChange).not.toHaveBeenCalled();
    });
  });

  describe('disabled state', () => {
    it('disables toggle when disabled prop is true', () => {
      render(<SettingsToggle {...defaultProps} disabled />);
      expect(screen.getByRole('switch')).toBeDisabled();
    });

    it('does not call onChange when disabled', () => {
      const onChange = jest.fn();
      render(<SettingsToggle {...defaultProps} disabled onChange={onChange} />);

      fireEvent.click(screen.getByRole('switch'));

      expect(onChange).not.toHaveBeenCalled();
    });

    it('does not toggle on keyboard when disabled', () => {
      const onChange = jest.fn();
      render(<SettingsToggle {...defaultProps} disabled onChange={onChange} />);

      const toggle = screen.getByRole('switch');
      fireEvent.keyDown(toggle, { key: 'Enter' });

      expect(onChange).not.toHaveBeenCalled();
    });
  });

  describe('accessibility', () => {
    it('has role="switch"', () => {
      render(<SettingsToggle {...defaultProps} />);
      expect(screen.getByRole('switch')).toBeInTheDocument();
    });

    it('has aria-checked attribute', () => {
      render(<SettingsToggle {...defaultProps} checked={true} />);
      expect(screen.getByRole('switch')).toHaveAttribute('aria-checked', 'true');
    });

    it('sets aria-describedby when description is present', () => {
      render(
        <SettingsToggle {...defaultProps} description="Description text" />
      );
      const toggle = screen.getByRole('switch');
      expect(toggle).toHaveAttribute('aria-describedby', expect.stringContaining('desc'));
    });

    it('does not set aria-describedby when no description', () => {
      render(<SettingsToggle {...defaultProps} />);
      const toggle = screen.getByRole('switch');
      expect(toggle).not.toHaveAttribute('aria-describedby');
    });

    it('associates label with toggle via htmlFor', () => {
      render(<SettingsToggle {...defaultProps} />);
      const toggle = screen.getByRole('switch');
      expect(toggle).toHaveAttribute('id', 'settings-toggle-test-toggle');
    });
  });

  describe('controlled component', () => {
    it('reflects external checked state changes', () => {
      const { rerender } = render(
        <SettingsToggle {...defaultProps} checked={false} />
      );
      expect(screen.getByRole('switch')).toHaveAttribute('aria-checked', 'false');

      rerender(<SettingsToggle {...defaultProps} checked={true} />);
      expect(screen.getByRole('switch')).toHaveAttribute('aria-checked', 'true');
    });
  });
});


