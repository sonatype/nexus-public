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
import { SettingsAlert } from '../SettingsAlert';

// Mock Radix UI components
jest.mock('@radix-ui/themes', () => ({
  Box: ({ children, className, ...props }) => <div className={className} {...props}>{children}</div>,
}));

// Mock lucide-react
jest.mock('lucide-react', () => ({
  AlertCircle: () => <span data-testid="error-icon">⚠</span>,
  CheckCircle: () => <span data-testid="success-icon">✓</span>,
  Info: () => <span data-testid="info-icon">ℹ</span>,
  AlertTriangle: () => <span data-testid="warning-icon">⚠</span>,
  X: () => <span data-testid="close-icon">×</span>,
}));

describe('SettingsAlert', () => {
  it('renders alert with message', () => {
    render(<SettingsAlert>This is an alert</SettingsAlert>);
    
    // Default type="info" uses role="status", not role="alert"
    expect(screen.getByRole('status')).toBeInTheDocument();
    expect(screen.getByText('This is an alert')).toBeInTheDocument();
  });

  it('renders with role="alert" for error type', () => {
    render(<SettingsAlert type="error">Error message</SettingsAlert>);
    
    expect(screen.getByRole('alert')).toBeInTheDocument();
  });

  it('renders info icon by default', () => {
    render(<SettingsAlert>Info message</SettingsAlert>);
    
    expect(screen.getByTestId('info-icon')).toBeInTheDocument();
  });

  it('renders error icon for error type', () => {
    render(<SettingsAlert type="error">Error message</SettingsAlert>);
    
    expect(screen.getByTestId('error-icon')).toBeInTheDocument();
  });

  it('renders success icon for success type', () => {
    render(<SettingsAlert type="success">Success message</SettingsAlert>);
    
    expect(screen.getByTestId('success-icon')).toBeInTheDocument();
  });

  it('renders warning icon for warning type', () => {
    render(<SettingsAlert type="warning">Warning message</SettingsAlert>);
    
    expect(screen.getByTestId('warning-icon')).toBeInTheDocument();
  });

  it('applies info class by default', () => {
    const { container } = render(<SettingsAlert>Info</SettingsAlert>);
    
    expect(container.firstChild).toHaveClass('settings-alert--info');
  });

  it('applies error class for error type', () => {
    const { container } = render(<SettingsAlert type="error">Error</SettingsAlert>);
    
    expect(container.firstChild).toHaveClass('settings-alert--error');
  });

  it('applies success class for success type', () => {
    const { container } = render(<SettingsAlert type="success">Success</SettingsAlert>);
    
    expect(container.firstChild).toHaveClass('settings-alert--success');
  });

  it('applies warning class for warning type', () => {
    const { container } = render(<SettingsAlert type="warning">Warning</SettingsAlert>);
    
    expect(container.firstChild).toHaveClass('settings-alert--warning');
  });

  it('shows close button when onClose is provided', () => {
    render(<SettingsAlert onClose={jest.fn()}>Closable</SettingsAlert>);
    
    expect(screen.getByRole('button', { name: 'Dismiss' })).toBeInTheDocument();
  });

  it('does not show close button when onClose is not provided', () => {
    render(<SettingsAlert>Not closable</SettingsAlert>);
    
    expect(screen.queryByRole('button', { name: 'Dismiss' })).not.toBeInTheDocument();
  });

  it('calls onClose when close button is clicked', () => {
    const onClose = jest.fn();
    render(<SettingsAlert onClose={onClose}>Closable</SettingsAlert>);
    
    fireEvent.click(screen.getByRole('button', { name: 'Dismiss' }));
    
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('applies custom className', () => {
    const { container } = render(<SettingsAlert className="custom">Alert</SettingsAlert>);
    
    expect(container.firstChild).toHaveClass('custom');
  });

  it('renders complex children', () => {
    render(
      <SettingsAlert>
        <strong>Important:</strong> Please check your settings.
      </SettingsAlert>
    );
    
    expect(screen.getByText('Important:')).toBeInTheDocument();
    expect(screen.getByText(/Please check/)).toBeInTheDocument();
  });
});

