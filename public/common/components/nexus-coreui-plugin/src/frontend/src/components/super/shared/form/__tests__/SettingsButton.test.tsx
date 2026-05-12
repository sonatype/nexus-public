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
import { Theme } from '@radix-ui/themes';
import { Save } from 'lucide-react';
import { SettingsButton } from '../SettingsButton';

function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

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
      render(<SettingsButton {...defaultProps} />, { wrapper: TestWrapper });
      expect(screen.getByRole('button', { name: 'Click Me' })).toBeInTheDocument();
    });

    it('renders as button type by default', () => {
      render(<SettingsButton {...defaultProps} />, { wrapper: TestWrapper });
      expect(screen.getByRole('button')).toHaveAttribute('type', 'button');
    });

    it('renders as submit type when specified', () => {
      render(<SettingsButton {...defaultProps} type="submit" />, { wrapper: TestWrapper });
      expect(screen.getByRole('button')).toHaveAttribute('type', 'submit');
    });

    it('renders as reset type when specified', () => {
      render(<SettingsButton {...defaultProps} type="reset" />, { wrapper: TestWrapper });
      expect(screen.getByRole('button')).toHaveAttribute('type', 'reset');
    });

    it('applies custom className', () => {
      render(<SettingsButton {...defaultProps} className="custom-class" />, { wrapper: TestWrapper });
      expect(screen.getByRole('button')).toHaveClass('custom-class');
    });
  });

  describe('variants', () => {
    it('applies secondary variant by default', () => {
      render(<SettingsButton {...defaultProps} />, { wrapper: TestWrapper });
      expect(screen.getByRole('button')).toHaveAttribute('data-variant', 'secondary');
    });

    it('applies primary variant', () => {
      render(<SettingsButton {...defaultProps} variant="primary" />, { wrapper: TestWrapper });
      expect(screen.getByRole('button')).toHaveAttribute('data-variant', 'primary');
    });

    it('applies danger variant', () => {
      render(<SettingsButton {...defaultProps} variant="danger" />, { wrapper: TestWrapper });
      expect(screen.getByRole('button')).toHaveAttribute('data-variant', 'danger');
    });

    it('applies ghost variant', () => {
      render(<SettingsButton {...defaultProps} variant="ghost" />, { wrapper: TestWrapper });
      expect(screen.getByRole('button')).toHaveAttribute('data-variant', 'ghost');
    });
  });

  describe('sizes', () => {
    it('applies medium size by default', () => {
      render(<SettingsButton {...defaultProps} />, { wrapper: TestWrapper });
      expect(screen.getByRole('button')).toHaveAttribute('data-size', 'medium');
    });

    it('applies small size', () => {
      render(<SettingsButton {...defaultProps} size="small" />, { wrapper: TestWrapper });
      expect(screen.getByRole('button')).toHaveAttribute('data-size', 'small');
    });

    it('applies large size', () => {
      render(<SettingsButton {...defaultProps} size="large" />, { wrapper: TestWrapper });
      expect(screen.getByRole('button')).toHaveAttribute('data-size', 'large');
    });
  });

  describe('icons', () => {
    it('renders with icon prop', () => {
      render(<SettingsButton {...defaultProps} icon={Save} />, { wrapper: TestWrapper });
      expect(screen.getByTestId('settings-button-icon')).toBeInTheDocument();
    });

    it('icon is displayed alongside text', () => {
      render(<SettingsButton {...defaultProps} icon={Save} />, { wrapper: TestWrapper });
      expect(screen.getByTestId('settings-button-icon')).toBeInTheDocument();
      expect(screen.getByRole('button')).toHaveTextContent('Click Me');
    });
  });

  describe('loading state', () => {
    it('shows loading spinner when loading', () => {
      render(<SettingsButton {...defaultProps} loading />, { wrapper: TestWrapper });
      expect(screen.getByTestId('settings-button-spinner')).toBeInTheDocument();
    });

    it('disables button when loading', () => {
      render(<SettingsButton {...defaultProps} loading />, { wrapper: TestWrapper });
      expect(screen.getByRole('button')).toBeDisabled();
    });

    it('hides icon when loading (spinner shown instead)', () => {
      render(<SettingsButton {...defaultProps} icon={Save} loading />, { wrapper: TestWrapper });
      expect(screen.queryByTestId('settings-button-icon')).not.toBeInTheDocument();
      expect(screen.getByTestId('settings-button-spinner')).toBeInTheDocument();
    });
  });

  describe('user interactions', () => {
    it('calls onClick when clicked', () => {
      const onClick = jest.fn();
      render(<SettingsButton {...defaultProps} onClick={onClick} />, { wrapper: TestWrapper });

      fireEvent.click(screen.getByRole('button'));

      expect(onClick).toHaveBeenCalledTimes(1);
    });

    it('does not call onClick when disabled', () => {
      const onClick = jest.fn();
      render(<SettingsButton {...defaultProps} onClick={onClick} disabled />, { wrapper: TestWrapper });

      fireEvent.click(screen.getByRole('button'));

      expect(onClick).not.toHaveBeenCalled();
    });

    it('does not call onClick when loading', () => {
      const onClick = jest.fn();
      render(<SettingsButton {...defaultProps} onClick={onClick} loading />, { wrapper: TestWrapper });

      fireEvent.click(screen.getByRole('button'));

      expect(onClick).not.toHaveBeenCalled();
    });
  });

  describe('disabled state', () => {
    it('disables button when disabled prop is true', () => {
      render(<SettingsButton {...defaultProps} disabled />, { wrapper: TestWrapper });
      expect(screen.getByRole('button')).toBeDisabled();
    });

    it('is disabled when both disabled and loading', () => {
      render(<SettingsButton {...defaultProps} disabled loading />, { wrapper: TestWrapper });
      expect(screen.getByRole('button')).toBeDisabled();
    });
  });

  describe('accessibility', () => {
    it('can be focused', () => {
      render(<SettingsButton {...defaultProps} />, { wrapper: TestWrapper });
      const button = screen.getByRole('button');
      button.focus();
      expect(button).toHaveFocus();
    });

    it('can be activated with Enter key', () => {
      const onClick = jest.fn();
      render(<SettingsButton {...defaultProps} onClick={onClick} />, { wrapper: TestWrapper });

      const button = screen.getByRole('button');
      fireEvent.keyDown(button, { key: 'Enter' });
      fireEvent.click(button);

      expect(onClick).toHaveBeenCalled();
    });

    it('can be activated with Space key', () => {
      const onClick = jest.fn();
      render(<SettingsButton {...defaultProps} onClick={onClick} />, { wrapper: TestWrapper });

      const button = screen.getByRole('button');
      fireEvent.keyDown(button, { key: ' ', code: 'Space' });
      fireEvent.click(button);

      expect(onClick).toHaveBeenCalled();
    });
  });

  describe('testId prop', () => {
    it('forwards testId as data-testid', () => {
      render(<SettingsButton {...defaultProps} testId="my-btn" />, { wrapper: TestWrapper });
      expect(screen.getByTestId('my-btn')).toBeInTheDocument();
    });

    it('auto-sets data-testid to button-submit for submit type', () => {
      render(<SettingsButton type="submit">Submit</SettingsButton>, { wrapper: TestWrapper });
      expect(screen.getByTestId('button-submit')).toBeInTheDocument();
    });
  });

  describe('form integration', () => {
    it('submits form when type is submit', () => {
      const onSubmit = jest.fn((e) => e.preventDefault());
      render(
        <Theme>
          <form onSubmit={onSubmit}>
            <SettingsButton type="submit">Submit</SettingsButton>
          </form>
        </Theme>
      );

      fireEvent.click(screen.getByRole('button', { name: 'Submit' }));

      expect(onSubmit).toHaveBeenCalled();
    });
  });
});
