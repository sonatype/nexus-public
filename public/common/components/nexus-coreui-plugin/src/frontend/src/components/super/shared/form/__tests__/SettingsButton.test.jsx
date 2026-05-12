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
import { SettingsButton } from '../SettingsButton';

function TestWrapper({ children }) {
  return <Theme>{children}</Theme>;
}

// Mock lucide-react icons used in these tests
jest.mock('lucide-react', () => ({
  Loader2: ({ 'data-testid': testId, className }) =>
    <span data-testid={testId || 'spinner'} className={className}>⟳</span>,
  Save: ({ 'data-testid': testId, className }) =>
    <span data-testid={testId || 'save-icon'} className={className}>💾</span>,
}));

describe('SettingsButton', () => {
  it('renders button with children', () => {
    render(<SettingsButton>Click Me</SettingsButton>, { wrapper: TestWrapper });

    expect(screen.getByRole('button')).toHaveTextContent('Click Me');
  });

  it('defaults to button type', () => {
    render(<SettingsButton>Test</SettingsButton>, { wrapper: TestWrapper });

    expect(screen.getByRole('button')).toHaveAttribute('type', 'button');
  });

  it('supports submit type', () => {
    render(<SettingsButton type="submit">Submit</SettingsButton>, { wrapper: TestWrapper });

    expect(screen.getByRole('button')).toHaveAttribute('type', 'submit');
  });

  it('calls onClick when clicked', () => {
    const onClick = jest.fn();
    render(<SettingsButton onClick={onClick}>Click</SettingsButton>, { wrapper: TestWrapper });

    fireEvent.click(screen.getByRole('button'));

    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it('is disabled when disabled prop is true', () => {
    render(<SettingsButton disabled>Disabled</SettingsButton>, { wrapper: TestWrapper });

    expect(screen.getByRole('button')).toBeDisabled();
  });

  it('is disabled when loading prop is true', () => {
    render(<SettingsButton loading>Loading</SettingsButton>, { wrapper: TestWrapper });

    expect(screen.getByRole('button')).toBeDisabled();
  });

  it('shows spinner when loading', () => {
    render(<SettingsButton loading>Loading</SettingsButton>, { wrapper: TestWrapper });

    expect(screen.getByTestId('settings-button-spinner')).toBeInTheDocument();
  });

  it('hides spinner when not loading', () => {
    render(<SettingsButton>Not Loading</SettingsButton>, { wrapper: TestWrapper });

    expect(screen.queryByTestId('settings-button-spinner')).not.toBeInTheDocument();
  });

  it('renders icon when provided', () => {
    const SaveIcon = () => <span data-testid="save-icon">💾</span>;
    render(<SettingsButton icon={SaveIcon}>Save</SettingsButton>, { wrapper: TestWrapper });

    expect(screen.getByTestId('save-icon')).toBeInTheDocument();
  });

  it('hides icon when loading (shows spinner instead)', () => {
    const SaveIcon = () => <span data-testid="save-icon">💾</span>;
    render(<SettingsButton icon={SaveIcon} loading>Save</SettingsButton>, { wrapper: TestWrapper });

    expect(screen.queryByTestId('save-icon')).not.toBeInTheDocument();
    expect(screen.getByTestId('settings-button-spinner')).toBeInTheDocument();
  });

  it('applies primary variant via data-variant attribute', () => {
    render(<SettingsButton variant="primary">Primary</SettingsButton>, { wrapper: TestWrapper });

    expect(screen.getByRole('button')).toHaveAttribute('data-variant', 'primary');
  });

  it('applies secondary variant by default via data-variant attribute', () => {
    render(<SettingsButton>Secondary</SettingsButton>, { wrapper: TestWrapper });

    expect(screen.getByRole('button')).toHaveAttribute('data-variant', 'secondary');
  });

  it('applies danger variant via data-variant attribute', () => {
    render(<SettingsButton variant="danger">Delete</SettingsButton>, { wrapper: TestWrapper });

    expect(screen.getByRole('button')).toHaveAttribute('data-variant', 'danger');
  });

  it('applies ghost variant via data-variant attribute', () => {
    render(<SettingsButton variant="ghost">Ghost</SettingsButton>, { wrapper: TestWrapper });

    expect(screen.getByRole('button')).toHaveAttribute('data-variant', 'ghost');
  });

  it('applies small size via data-size attribute', () => {
    render(<SettingsButton size="small">Small</SettingsButton>, { wrapper: TestWrapper });

    expect(screen.getByRole('button')).toHaveAttribute('data-size', 'small');
  });

  it('applies medium size by default via data-size attribute', () => {
    render(<SettingsButton>Medium</SettingsButton>, { wrapper: TestWrapper });

    expect(screen.getByRole('button')).toHaveAttribute('data-size', 'medium');
  });

  it('applies large size via data-size attribute', () => {
    render(<SettingsButton size="large">Large</SettingsButton>, { wrapper: TestWrapper });

    expect(screen.getByRole('button')).toHaveAttribute('data-size', 'large');
  });

  it('applies custom className', () => {
    render(<SettingsButton className="custom">Custom</SettingsButton>, { wrapper: TestWrapper });

    expect(screen.getByRole('button')).toHaveClass('custom');
  });

  it('does not call onClick when disabled', () => {
    const onClick = jest.fn();
    render(<SettingsButton onClick={onClick} disabled>Click</SettingsButton>, { wrapper: TestWrapper });

    fireEvent.click(screen.getByRole('button'));

    expect(onClick).not.toHaveBeenCalled();
  });
});
