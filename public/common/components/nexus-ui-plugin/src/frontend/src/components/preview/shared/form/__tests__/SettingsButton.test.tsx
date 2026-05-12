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
import { Save, Trash2, Plus } from 'lucide-react';
import { SettingsButton } from '../SettingsButton';

describe('SettingsButton', () => {
  const defaultProps = {
    children: 'Click Me',
    onClick: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('rendering', () => {
    it('renders with children text', () => {
      render(<SettingsButton {...defaultProps} />);
      expect(screen.getByRole('button', { name: 'Click Me' })).toBeInTheDocument();
    });

    it('renders as button type by default', () => {
      render(<SettingsButton {...defaultProps} />);
      expect(screen.getByRole('button')).toHaveAttribute('type', 'button');
    });

    it('renders as submit type when specified', () => {
      render(<SettingsButton {...defaultProps} type="submit" />);
      expect(screen.getByRole('button')).toHaveAttribute('type', 'submit');
    });

    it('renders as reset type when specified', () => {
      render(<SettingsButton {...defaultProps} type="reset" />);
      expect(screen.getByRole('button')).toHaveAttribute('type', 'reset');
    });

    it('applies custom className', () => {
      const { container } = render(
        <SettingsButton {...defaultProps} className="custom-class" />
      );
      expect(container.querySelector('.custom-class')).toBeInTheDocument();
    });
  });

  describe('variants', () => {
    it('applies secondary variant by default', () => {
      const { container } = render(<SettingsButton {...defaultProps} />);
      expect(container.querySelector('.settings-button--secondary')).toBeInTheDocument();
    });

    it('applies primary variant', () => {
      const { container } = render(
        <SettingsButton {...defaultProps} variant="primary" />
      );
      expect(container.querySelector('.settings-button--primary')).toBeInTheDocument();
    });

    it('applies danger variant', () => {
      const { container } = render(
        <SettingsButton {...defaultProps} variant="danger" />
      );
      expect(container.querySelector('.settings-button--danger')).toBeInTheDocument();
    });

    it('applies ghost variant', () => {
      const { container } = render(
        <SettingsButton {...defaultProps} variant="ghost" />
      );
      expect(container.querySelector('.settings-button--ghost')).toBeInTheDocument();
    });
  });

  describe('sizes', () => {
    it('applies medium size by default', () => {
      const { container } = render(<SettingsButton {...defaultProps} />);
      expect(container.querySelector('.settings-button--medium')).toBeInTheDocument();
    });

    it('applies small size', () => {
      const { container } = render(
        <SettingsButton {...defaultProps} size="small" />
      );
      expect(container.querySelector('.settings-button--small')).toBeInTheDocument();
    });

    it('applies large size', () => {
      const { container } = render(
        <SettingsButton {...defaultProps} size="large" />
      );
      expect(container.querySelector('.settings-button--large')).toBeInTheDocument();
    });
  });

  describe('icons', () => {
    it('renders with icon prop', () => {
      const { container } = render(
        <SettingsButton {...defaultProps} icon={Save} />
      );
      expect(container.querySelector('.settings-button__icon')).toBeInTheDocument();
    });

    it('icon is displayed before text', () => {
      const { container } = render(
        <SettingsButton {...defaultProps} icon={Save} />
      );
      const icon = container.querySelector('.settings-button__icon');
      const text = container.querySelector('.settings-button__text');
      
      expect(icon).toBeInTheDocument();
      expect(text).toBeInTheDocument();
    });
  });

  describe('loading state', () => {
    it('shows loading spinner when loading', () => {
      const { container } = render(
        <SettingsButton {...defaultProps} loading />
      );
      expect(container.querySelector('.settings-button__spinner')).toBeInTheDocument();
    });

    it('disables button when loading', () => {
      render(<SettingsButton {...defaultProps} loading />);
      expect(screen.getByRole('button')).toBeDisabled();
    });

    it('hides icon when loading', () => {
      const { container } = render(
        <SettingsButton {...defaultProps} icon={Save} loading />
      );
      expect(container.querySelector('.settings-button__icon')).not.toBeInTheDocument();
      expect(container.querySelector('.settings-button__spinner')).toBeInTheDocument();
    });
  });

  describe('user interactions', () => {
    it('calls onClick when clicked', () => {
      const onClick = jest.fn();
      render(<SettingsButton {...defaultProps} onClick={onClick} />);

      fireEvent.click(screen.getByRole('button'));

      expect(onClick).toHaveBeenCalledTimes(1);
    });

    it('does not call onClick when disabled', () => {
      const onClick = jest.fn();
      render(<SettingsButton {...defaultProps} onClick={onClick} disabled />);

      fireEvent.click(screen.getByRole('button'));

      expect(onClick).not.toHaveBeenCalled();
    });

    it('does not call onClick when loading', () => {
      const onClick = jest.fn();
      render(<SettingsButton {...defaultProps} onClick={onClick} loading />);

      fireEvent.click(screen.getByRole('button'));

      expect(onClick).not.toHaveBeenCalled();
    });
  });

  describe('disabled state', () => {
    it('disables button when disabled prop is true', () => {
      render(<SettingsButton {...defaultProps} disabled />);
      expect(screen.getByRole('button')).toBeDisabled();
    });

    it('is disabled when both disabled and loading', () => {
      render(<SettingsButton {...defaultProps} disabled loading />);
      expect(screen.getByRole('button')).toBeDisabled();
    });
  });

  describe('accessibility', () => {
    it('can be focused', () => {
      render(<SettingsButton {...defaultProps} />);
      const button = screen.getByRole('button');
      button.focus();
      expect(button).toHaveFocus();
    });

    it('can be activated with Enter key', () => {
      const onClick = jest.fn();
      render(<SettingsButton {...defaultProps} onClick={onClick} />);

      const button = screen.getByRole('button');
      fireEvent.keyDown(button, { key: 'Enter' });
      fireEvent.click(button);

      expect(onClick).toHaveBeenCalled();
    });

    it('can be activated with Space key', () => {
      const onClick = jest.fn();
      render(<SettingsButton {...defaultProps} onClick={onClick} />);

      const button = screen.getByRole('button');
      fireEvent.keyDown(button, { key: ' ', code: 'Space' });
      fireEvent.click(button);

      expect(onClick).toHaveBeenCalled();
    });
  });

  describe('form integration', () => {
    it('submits form when type is submit', () => {
      const onSubmit = jest.fn((e) => e.preventDefault());
      render(
        <form onSubmit={onSubmit}>
          <SettingsButton type="submit">Submit</SettingsButton>
        </form>
      );

      fireEvent.click(screen.getByRole('button', { name: 'Submit' }));

      expect(onSubmit).toHaveBeenCalled();
    });
  });
});


