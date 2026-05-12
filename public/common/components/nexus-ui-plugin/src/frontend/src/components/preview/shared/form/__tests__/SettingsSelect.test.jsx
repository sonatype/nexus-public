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
import { SettingsSelect } from '../SettingsSelect';

// Mock Radix UI components
// The component uses compound Select pattern: Root > Trigger + Content > Items
// We mock it to render as a native select for easier testing
jest.mock('@radix-ui/themes', () => {
  // eslint-disable-next-line no-undef
  const React = require('react');
  
  // Shared context for compound component communication
  const SelectContext = React.createContext({});

  const MockSelectRoot = ({ children, value, onValueChange, disabled }) => {
    const [options, setOptions] = React.useState([]);
    
    return (
      <SelectContext.Provider value={{ value, onValueChange, disabled, options, setOptions }}>
        {children}
      </SelectContext.Provider>
    );
  };

  const MockSelectTrigger = ({ id, className, placeholder, ...props }) => {
    const { value, onValueChange, disabled, options } = React.useContext(SelectContext);
    return (
      <select 
        id={id} 
        className={className} 
        value={value || ''} 
        onChange={(e) => onValueChange?.(e.target.value)}
        disabled={disabled}
        aria-invalid={props['aria-invalid']}
        aria-describedby={props['aria-describedby']}
        data-testid={props['data-testid']}
      >
        <option value="">{placeholder}</option>
        {options.map(opt => (
          <option key={opt.value} value={opt.value} disabled={opt.disabled}>
            {opt.label}
          </option>
        ))}
      </select>
    );
  };

  const MockSelectContent = ({ children }) => {
    const { setOptions } = React.useContext(SelectContext);
    
    React.useEffect(() => {
      const opts = [];
      React.Children.forEach(children, child => {
        if (React.isValidElement(child) && child.props.value !== undefined) {
          opts.push({
            value: child.props.value,
            label: child.props.children,
            disabled: child.props.disabled || false,
          });
        }
      });
      setOptions(opts);
    }, [children, setOptions]);
    
    return null;
  };

  const MockSelectItem = () => null;

  return {
    Box: ({ children, className }) => <div className={className}>{children}</div>,
    Text: ({ children, className, as: Tag = 'span', id }) => {
      const Component = Tag || 'span';
      return <Component className={className} id={id}>{children}</Component>;
    },
    Select: {
      Root: MockSelectRoot,
      Trigger: MockSelectTrigger,
      Content: MockSelectContent,
      Item: MockSelectItem,
    },
  };
});

// Mock lucide-react
jest.mock('lucide-react', () => ({
  AlertCircle: () => <span data-testid="alert-icon">!</span>,
  ChevronDown: () => <span data-testid="chevron-icon">▾</span>,
}));

describe('SettingsSelect', () => {
  const defaultOptions = [
    { value: 'option1', label: 'Option 1' },
    { value: 'option2', label: 'Option 2' },
    { value: 'option3', label: 'Option 3' },
  ];

  const defaultProps = {
    name: 'test-select',
    value: '',
    onChange: jest.fn(),
    options: defaultOptions,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders select with options', async () => {
    render(<SettingsSelect {...defaultProps} />);
    
    expect(screen.getByRole('combobox')).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.getByText('Option 1')).toBeInTheDocument();
      expect(screen.getByText('Option 2')).toBeInTheDocument();
      expect(screen.getByText('Option 3')).toBeInTheDocument();
    });
  });

  it('renders label when provided', () => {
    render(<SettingsSelect {...defaultProps} label="Select Option" />);
    
    expect(screen.getByText('Select Option')).toBeInTheDocument();
    expect(screen.getByLabelText('Select Option')).toBeInTheDocument();
  });

  it('shows required indicator when required is true', () => {
    render(<SettingsSelect {...defaultProps} label="Select" required />);
    
    expect(screen.getByText('*')).toBeInTheDocument();
  });

  it('displays placeholder when value is empty', () => {
    render(<SettingsSelect {...defaultProps} placeholder="Choose..." />);
    
    expect(screen.getByText('Choose...')).toBeInTheDocument();
  });

  it('displays selected value', () => {
    render(<SettingsSelect {...defaultProps} value="option2" />);
    
    expect(screen.getByRole('combobox')).toHaveValue('option2');
  });

  it('calls onChange when selection changes', async () => {
    const onChange = jest.fn();
    render(<SettingsSelect {...defaultProps} onChange={onChange} />);
    
    // Wait for options to load
    await waitFor(() => {
      expect(screen.getByText('Option 1')).toBeInTheDocument();
    });
    
    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'option2' } });
    
    // Component's onChange only receives the value, not an event
    expect(onChange).toHaveBeenCalledWith('option2');
  });

  it('renders help text when provided', () => {
    render(<SettingsSelect {...defaultProps} helpText="This is help text" />);
    
    expect(screen.getByText('This is help text')).toBeInTheDocument();
  });

  it('renders error message and hides help text', () => {
    render(
      <SettingsSelect 
        {...defaultProps} 
        helpText="Help text"
        error="This is required" 
      />
    );
    
    expect(screen.getByText('This is required')).toBeInTheDocument();
    expect(screen.queryByText('Help text')).not.toBeInTheDocument();
  });

  it('sets aria-invalid when error is present', () => {
    render(<SettingsSelect {...defaultProps} error="Error" />);
    
    expect(screen.getByRole('combobox')).toHaveAttribute('aria-invalid', 'true');
  });

  it('disables select when disabled is true', () => {
    render(<SettingsSelect {...defaultProps} disabled />);
    
    expect(screen.getByRole('combobox')).toBeDisabled();
  });

  it('supports disabled options', async () => {
    const optionsWithDisabled = [
      { value: 'option1', label: 'Option 1' },
      { value: 'option2', label: 'Option 2', disabled: true },
    ];
    render(<SettingsSelect {...defaultProps} options={optionsWithDisabled} />);
    
    // Wait for options to load
    await waitFor(() => {
      expect(screen.getByText('Option 1')).toBeInTheDocument();
    });
    
    const options = screen.getAllByRole('option');
    // First option is placeholder, so disabled option is at index 2
    expect(options.find(o => o.textContent === 'Option 2')).toBeDisabled();
  });

  it('applies custom className', () => {
    const { container } = render(<SettingsSelect {...defaultProps} className="custom" />);
    
    expect(container.firstChild).toHaveClass('custom');
  });

  it('applies error class when error is present', () => {
    const { container } = render(<SettingsSelect {...defaultProps} error="Error" />);
    
    expect(container.firstChild).toHaveClass('settings-select--error');
  });

  it('has correct id on select', () => {
    render(<SettingsSelect {...defaultProps} />);
    
    expect(screen.getByRole('combobox')).toHaveAttribute('id', 'settings-select-test-select');
  });

  // Note: Radix Select.Trigger renders its own internal chevron,
  // so we don't test for a separate ChevronDown icon here
});

