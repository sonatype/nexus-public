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
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { SettingsForm } from '../SettingsForm';

// Mock Radix UI components
jest.mock('@radix-ui/themes', () => {
  const AlertDialogRoot = ({ children, open }) => open ? <div data-testid="alert-dialog">{children}</div> : null;
  const AlertDialogContent = ({ children }) => <div>{children}</div>;
  const AlertDialogTitle = ({ children }) => <h2>{children}</h2>;
  const AlertDialogDescription = ({ children }) => <p>{children}</p>;
  const AlertDialogCancel = ({ children }) => children;
  const AlertDialogAction = ({ children }) => children;

  return {
    Box: ({ children, className }) => <div className={className}>{children}</div>,
    Flex: ({ children, className, gap, justify }) => (
      <div className={className} data-gap={gap} data-justify={justify}>{children}</div>
    ),
    ScrollArea: ({ children, className }) => <div className={className}>{children}</div>,
    Heading: ({ children, className, as: Tag = 'h1' }) => <Tag className={className}>{children}</Tag>,
    Text: ({ children, className, as: Tag = 'span' }) => <Tag className={className}>{children}</Tag>,
    Button: ({ children, onClick, disabled, variant, color }) => (
      <button onClick={onClick} disabled={disabled} data-variant={variant} data-color={color}>{children}</button>
    ),
    Spinner: () => <span data-testid="spinner" />,
    Theme: ({ children }) => <div>{children}</div>,
    AlertDialog: {
      Root: AlertDialogRoot,
      Content: AlertDialogContent,
      Title: AlertDialogTitle,
      Description: AlertDialogDescription,
      Cancel: AlertDialogCancel,
      Action: AlertDialogAction,
    },
  };
});

// Mock child components
jest.mock('../SettingsButton', () => ({
  SettingsButton: ({ children, onClick, disabled, type, variant, loading }) => (
    <button 
      type={type} 
      onClick={onClick} 
      disabled={disabled}
      data-variant={variant}
      data-loading={loading}
    >
      {children}
    </button>
  ),
}));

jest.mock('../SettingsAlert', () => ({
  SettingsAlert: ({ children, type }) => (
    <div data-testid={`alert-${type}`} role="alert">{children}</div>
  ),
}));

describe('SettingsForm', () => {
  const defaultProps = {
    title: 'Test Form',
    children: <div data-testid="form-content">Form Content</div>,
  };

  afterEach(() => {
    window.dirty = [];
  });

  it('renders title and children', () => {
    render(<SettingsForm {...defaultProps} />);
    
    expect(screen.getByText('Test Form')).toBeInTheDocument();
    expect(screen.getByTestId('form-content')).toBeInTheDocument();
  });

  it('renders description when provided', () => {
    render(<SettingsForm {...defaultProps} description="Test description" />);
    
    expect(screen.getByText('Test description')).toBeInTheDocument();
  });

  it('renders error alert when error prop is provided', () => {
    render(<SettingsForm {...defaultProps} error="Something went wrong" />);
    
    expect(screen.getByText('Something went wrong')).toBeInTheDocument();
  });

  it('renders success alert when success prop is provided', () => {
    render(<SettingsForm {...defaultProps} success="Saved successfully" />);
    
    expect(screen.getByTestId('alert-success')).toBeInTheDocument();
    expect(screen.getByText('Saved successfully')).toBeInTheDocument();
  });

  it('renders save and cancel buttons by default', () => {
    const onCancel = jest.fn();
    render(<SettingsForm {...defaultProps} onSubmit={jest.fn()} onCancel={onCancel} />);
    
    expect(screen.getByText('Save')).toBeInTheDocument();
    expect(screen.getByText('Discard')).toBeInTheDocument();
  });

  it('uses custom button labels when provided', () => {
    render(
      <SettingsForm 
        {...defaultProps} 
        submitLabel="Apply" 
        cancelLabel="Reset"
        onSubmit={jest.fn()}
        onCancel={jest.fn()}
      />
    );
    
    expect(screen.getByText('Apply')).toBeInTheDocument();
    expect(screen.getByText('Reset')).toBeInTheDocument();
  });

  it('disables submit button when pristine is true', () => {
    render(<SettingsForm {...defaultProps} pristine={true} onSubmit={jest.fn()} />);
    
    const submitButton = screen.getByText('Save');
    expect(submitButton).toBeDisabled();
  });

  it('enables submit button when pristine is false', () => {
    render(<SettingsForm {...defaultProps} pristine={false} onSubmit={jest.fn()} />);
    
    const submitButton = screen.getByText('Save');
    expect(submitButton).not.toBeDisabled();
  });

  it('disables submit button when loading is true', () => {
    render(<SettingsForm {...defaultProps} pristine={false} loading={true} onSubmit={jest.fn()} />);
    
    const submitButton = screen.getByText('Save');
    expect(submitButton).toBeDisabled();
  });

  it('calls onSubmit when form is submitted and not pristine', () => {
    const onSubmit = jest.fn();
    render(<SettingsForm {...defaultProps} pristine={false} onSubmit={onSubmit} />);
    
    const form = document.querySelector('form');
    fireEvent.submit(form);
    
    expect(onSubmit).toHaveBeenCalledTimes(1);
  });

  it('does not call onSubmit when pristine', () => {
    const onSubmit = jest.fn();
    render(<SettingsForm {...defaultProps} pristine={true} onSubmit={onSubmit} />);
    
    const form = document.querySelector('form');
    fireEvent.submit(form);
    
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('calls onCancel directly when form is pristine', () => {
    const onCancel = jest.fn();
    render(<SettingsForm {...defaultProps} onCancel={onCancel} pristine={true} />);
    
    fireEvent.click(screen.getByText('Discard'));
    
    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it('shows discard dialog by default when dirty (confirmDiscard defaults to true)', async () => {
    const onCancel = jest.fn();
    render(<SettingsForm {...defaultProps} onCancel={onCancel} pristine={false} />);
    
    fireEvent.click(screen.getByText('Discard'));
    
    await waitFor(() => {
      expect(screen.getByText(/your changes will be lost/i)).toBeInTheDocument();
    });
    expect(onCancel).not.toHaveBeenCalled();
  });

  it('calls onCancel directly when dirty but confirmDiscard is explicitly false', () => {
    const onCancel = jest.fn();
    render(<SettingsForm {...defaultProps} onCancel={onCancel} pristine={false} confirmDiscard={false} />);
    
    fireEvent.click(screen.getByText('Discard'));
    
    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it('shows discard dialog when dirty and confirmDiscard is true', async () => {
    const onCancel = jest.fn();
    render(
      <SettingsForm {...defaultProps} onCancel={onCancel} pristine={false} confirmDiscard />
    );
    
    fireEvent.click(screen.getByText('Discard'));
    
    await waitFor(() => {
      expect(screen.getByText(/your changes will be lost/i)).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: /stay/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /leave/i })).toBeInTheDocument();
    expect(onCancel).not.toHaveBeenCalled();
  });

  it('calls onCancel when Leave is clicked in discard dialog', async () => {
    const onCancel = jest.fn();
    render(
      <SettingsForm {...defaultProps} onCancel={onCancel} pristine={false} confirmDiscard />
    );
    
    fireEvent.click(screen.getByText('Discard'));
    
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /leave/i })).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByRole('button', { name: /leave/i }));
    
    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it('shows unsaved changes message when not pristine', () => {
    render(<SettingsForm {...defaultProps} pristine={false} />);

    // Indicator is always present in the DOM so the action bar reserves the same width pristine ↔ dirty; when dirty it's visible.
    const indicator = screen.getByText('Unsaved changes');
    expect(indicator).toBeInTheDocument();
    expect(indicator).toHaveAttribute('data-pristine', 'false');
  });

  it('hides unsaved changes message when noDirtyTracking is true', () => {
    render(<SettingsForm {...defaultProps} pristine={false} noDirtyTracking />);

    // The indicator is rendered (reserving its slot) but visually hidden via data-pristine="true".
    const indicator = screen.getByText('Unsaved changes');
    expect(indicator).toBeInTheDocument();
    expect(indicator).toHaveAttribute('data-pristine', 'true');
  });

  it('hides actions when showActions is false', () => {
    render(<SettingsForm {...defaultProps} showActions={false} />);
    
    expect(screen.queryByText('Save')).not.toBeInTheDocument();
  });

  it('applies custom className', () => {
    const { container } = render(<SettingsForm {...defaultProps} className="custom-class" />);
    
    expect(container.querySelector('form')).toHaveClass('custom-class');
  });

  it('has noValidate attribute on form', () => {
    const { container } = render(<SettingsForm {...defaultProps} />);

    expect(container.querySelector('form')).toHaveAttribute('noValidate');
  });

  it('clears window.dirty after discard confirm', async () => {
    const onCancel = jest.fn();
    window.dirty = [];

    // Use dirty={true} instead of pristine={false} so SettingsForm tracks dirty state itself
    // (when pristine prop is passed, parent is assumed to be managing state)
    render(
      <SettingsForm {...defaultProps} onCancel={onCancel} dirty confirmDiscard />
    );

    // Form should register as dirty
    expect(window.dirty.length).toBeGreaterThan(0);

    // Click cancel to trigger discard dialog
    fireEvent.click(screen.getByText('Discard'));

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /leave/i })).toBeInTheDocument();
    });

    // Confirm discard
    fireEvent.click(screen.getByRole('button', { name: /leave/i }));

    // After confirming discard, window.dirty must be empty so the router's
    // onBefore hook won't show a second "unsaved changes" modal
    expect(window.dirty).toEqual([]);
    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it('does not re-register dirty state after discard confirm on re-render', async () => {
    const onCancel = jest.fn();
    window.dirty = [];

    // Use dirty={true} instead of pristine={false} so SettingsForm tracks dirty state itself
    const { rerender } = render(
      <SettingsForm {...defaultProps} onCancel={onCancel} dirty confirmDiscard />
    );

    // Confirm the form is tracked as dirty
    expect(window.dirty.length).toBeGreaterThan(0);

    // Click cancel and confirm discard
    fireEvent.click(screen.getByText('Discard'));
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /leave/i })).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: /leave/i }));

    expect(window.dirty).toEqual([]);

    // Simulate a React re-render (e.g., parent state change from navigation)
    // The form should NOT re-register as dirty
    rerender(
      <SettingsForm {...defaultProps} onCancel={onCancel} dirty confirmDiscard />
    );

    expect(window.dirty).toEqual([]);
  });
});

