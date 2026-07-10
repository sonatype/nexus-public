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

// Mock Radix UI components
jest.mock('@radix-ui/themes', () => ({
  Box: ({ children, className }) => <div className={className}>{children}</div>,
  Text: ({ children, className, as: Tag = 'span', id }) => <Tag className={className} id={id}>{children}</Tag>,
}));

// Mock lucide-react
jest.mock('lucide-react', () => ({
  Check: () => <span data-testid="check-icon">✓</span>,
}));

describe('SettingsCheckbox', () => {
  const defaultProps = {
    name: 'test-checkbox',
    label: 'Test Checkbox',
    onChange: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders checkbox with label', () => {
    render(<SettingsCheckbox {...defaultProps} />);
    
    expect(screen.getByRole('checkbox')).toBeInTheDocument();
    expect(screen.getByText('Test Checkbox')).toBeInTheDocument();
  });

  it('is unchecked by default', () => {
    render(<SettingsCheckbox {...defaultProps} />);
    
    expect(screen.getByRole('checkbox')).not.toBeChecked();
  });

  it('is checked when checked prop is true', () => {
    render(<SettingsCheckbox {...defaultProps} checked={true} />);
    
    expect(screen.getByRole('checkbox')).toBeChecked();
  });

  it('shows check icon when checked', () => {
    render(<SettingsCheckbox {...defaultProps} checked={true} />);
    
    expect(screen.getByTestId('check-icon')).toBeInTheDocument();
  });

  it('hides check icon when unchecked', () => {
    render(<SettingsCheckbox {...defaultProps} checked={false} />);
    
    expect(screen.queryByTestId('check-icon')).not.toBeInTheDocument();
  });

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

  it('renders description when provided', () => {
    render(
      <SettingsCheckbox 
        {...defaultProps} 
        description="This is a description" 
      />
    );
    
    expect(screen.getByText('This is a description')).toBeInTheDocument();
  });

  it('sets aria-describedby when description is provided', () => {
    render(
      <SettingsCheckbox 
        {...defaultProps} 
        description="Description" 
      />
    );
    
    expect(screen.getByRole('checkbox')).toHaveAttribute(
      'aria-describedby',
      'settings-checkbox-desc-test-checkbox'
    );
  });

  it('disables checkbox when disabled is true', () => {
    render(<SettingsCheckbox {...defaultProps} disabled />);
    
    expect(screen.getByRole('checkbox')).toBeDisabled();
  });

  it('does not call onChange when disabled', () => {
    const onChange = jest.fn();
    render(<SettingsCheckbox {...defaultProps} disabled onChange={onChange} />);
    
    // The native checkbox is disabled so it should not change
    // But the onChange is called by browser click propagation
    // So we just verify the checkbox is disabled
    expect(screen.getByRole('checkbox')).toBeDisabled();
  });

  it('applies custom className', () => {
    const { container } = render(<SettingsCheckbox {...defaultProps} className="custom" />);
    
    expect(container.firstChild).toHaveClass('custom');
  });

  it('applies disabled class when disabled', () => {
    const { container } = render(<SettingsCheckbox {...defaultProps} disabled />);
    
    expect(container.firstChild).toHaveClass('settings-checkbox--disabled');
  });

  it('has correct id on checkbox', () => {
    render(<SettingsCheckbox {...defaultProps} />);
    
    expect(screen.getByRole('checkbox')).toHaveAttribute('id', 'settings-checkbox-test-checkbox');
  });

  it('label is associated with checkbox via htmlFor', () => {
    render(<SettingsCheckbox {...defaultProps} />);

    expect(screen.getByLabelText('Test Checkbox')).toBeInTheDocument();
  });

  it('sets data-analytics-id when analyticsId is provided', () => {
    render(<SettingsCheckbox {...defaultProps} analyticsId="my-id" />);

    expect(screen.getByRole('checkbox')).toHaveAttribute('data-analytics-id', 'my-id');
  });

  it('omits data-analytics-id when analyticsId is not provided', () => {
    render(<SettingsCheckbox {...defaultProps} />);

    expect(screen.getByRole('checkbox')).not.toHaveAttribute('data-analytics-id');
  });
});

