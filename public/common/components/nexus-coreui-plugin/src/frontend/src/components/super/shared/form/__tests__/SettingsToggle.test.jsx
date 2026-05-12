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

// Mock Radix UI components
jest.mock('@radix-ui/themes', () => ({
  Box: ({ children, className }) => <div className={className}>{children}</div>,
  Text: ({ children, className, as: Tag = 'span', id }) => <Tag className={className} id={id}>{children}</Tag>,
}));

describe('SettingsToggle', () => {
  const defaultProps = {
    name: 'test-toggle',
    label: 'Enable Feature',
    onChange: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders toggle with label', () => {
    render(<SettingsToggle {...defaultProps} />);
    
    expect(screen.getByRole('switch')).toBeInTheDocument();
    expect(screen.getByText('Enable Feature')).toBeInTheDocument();
  });

  it('is unchecked by default', () => {
    render(<SettingsToggle {...defaultProps} />);
    
    expect(screen.getByRole('switch')).toHaveAttribute('aria-checked', 'false');
  });

  it('is checked when checked prop is true', () => {
    render(<SettingsToggle {...defaultProps} checked={true} />);
    
    expect(screen.getByRole('switch')).toHaveAttribute('aria-checked', 'true');
  });

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

  it('renders description when provided', () => {
    render(
      <SettingsToggle 
        {...defaultProps} 
        description="Toggle this to enable the feature" 
      />
    );
    
    expect(screen.getByText('Toggle this to enable the feature')).toBeInTheDocument();
  });

  it('sets aria-describedby when description is provided', () => {
    render(
      <SettingsToggle 
        {...defaultProps} 
        description="Description" 
      />
    );
    
    expect(screen.getByRole('switch')).toHaveAttribute(
      'aria-describedby',
      'settings-toggle-desc-test-toggle'
    );
  });

  it('disables toggle when disabled is true', () => {
    render(<SettingsToggle {...defaultProps} disabled />);
    
    expect(screen.getByRole('switch')).toBeDisabled();
  });

  it('does not call onChange when disabled', () => {
    const onChange = jest.fn();
    render(<SettingsToggle {...defaultProps} disabled onChange={onChange} />);
    
    fireEvent.click(screen.getByRole('switch'));
    
    expect(onChange).not.toHaveBeenCalled();
  });

  it('toggles on Enter key press', () => {
    const onChange = jest.fn();
    render(<SettingsToggle {...defaultProps} checked={false} onChange={onChange} />);
    
    fireEvent.keyDown(screen.getByRole('switch'), { key: 'Enter' });
    
    expect(onChange).toHaveBeenCalledWith(true);
  });

  it('toggles on Space key press', () => {
    const onChange = jest.fn();
    render(<SettingsToggle {...defaultProps} checked={false} onChange={onChange} />);
    
    fireEvent.keyDown(screen.getByRole('switch'), { key: ' ' });
    
    expect(onChange).toHaveBeenCalledWith(true);
  });

  it('applies custom className', () => {
    const { container } = render(<SettingsToggle {...defaultProps} className="custom" />);
    
    expect(container.firstChild).toHaveClass('custom');
  });

  it('applies disabled class when disabled', () => {
    const { container } = render(<SettingsToggle {...defaultProps} disabled />);
    
    expect(container.firstChild).toHaveClass('settings-toggle--disabled');
  });

  it('applies checked class to switch when checked', () => {
    render(<SettingsToggle {...defaultProps} checked={true} />);
    
    expect(screen.getByRole('switch')).toHaveClass('settings-toggle__switch--checked');
  });

  it('has correct id on switch', () => {
    render(<SettingsToggle {...defaultProps} />);
    
    expect(screen.getByRole('switch')).toHaveAttribute('id', 'settings-toggle-test-toggle');
  });

  it('toggles when clicking on label', () => {
    const onChange = jest.fn();
    render(<SettingsToggle {...defaultProps} checked={false} onChange={onChange} />);
    
    fireEvent.click(screen.getByText('Enable Feature'));
    
    expect(onChange).toHaveBeenCalledWith(true);
  });
});

