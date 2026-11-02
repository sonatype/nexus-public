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
import { Theme } from '@radix-ui/themes';
import { BlobStoreWizardStepGlobalDistribution } from '../BlobStoreWizardStepGlobalDistribution';
import type { BlobStoreFormData } from '../types';
import * as useLicenseLevelModule from '../../../../../shared/hooks/useLicenseLevel';

// Mock the useLicenseLevel hook
jest.mock('../../../../../shared/hooks/useLicenseLevel', () => ({
  useLicenseLevel: jest.fn(),
  LicenseLevel: {
    PRO: 'nexus-professional-edition',
    PRO_PLUS: 'nexus-professional-plus-edition',
    ENTERPRISE: 'nexus-enterprise-edition'
  }
}));

// Mock @radix-ui/themes to render select as native HTML select for testing
// Keep Theme component from actual package
jest.mock('@radix-ui/themes', () => {
  const actual = jest.requireActual('@radix-ui/themes');
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
      <>
        <select
          id={id}
          className={className}
          value={value || ''}
          onChange={(e) => onValueChange?.(e.target.value)}
          disabled={disabled}
          aria-invalid={props['aria-invalid']}
          aria-describedby={props['aria-describedby']}
          data-testid={props['data-testid']}
          aria-label={props['aria-label']}
        >
          <option value="">{placeholder || 'None'}</option>
          {options.map(opt => (
            <option key={opt.value} value={opt.value} disabled={opt.disabled}>
              {opt.label}
            </option>
          ))}
        </select>
        {options.filter(opt => opt.badge).map(opt => (
          <span key={opt.value} data-testid={`option-badge-${opt.value}`}>{opt.badge}</span>
        ))}
      </>
    );
  };

  const MockSelectContent = ({ children }) => {
    const { setOptions } = React.useContext(SelectContext);

    React.useEffect(() => {
      const opts = [];
      React.Children.forEach(children, child => {
        if (React.isValidElement(child) && child.props.value !== undefined) {
          const content = child.props.children;
          const [label, badge] = React.isValidElement(content)
            ? React.Children.toArray(content.props.children)
            : [content, undefined];
          opts.push({
            value: child.props.value,
            label,
            badge,
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
    ...actual, // Keep all original exports (including Theme)
    Box: ({ children, className, ...props }) => <div className={className} {...props}>{children}</div>,
    Text: ({ children, className, as: Tag = 'span', id, ...props }) => {
      const Component = Tag || 'span';
      return <Component className={className} id={id} {...props}>{children}</Component>;
    },
    Flex: ({ children, className, ...props }) => (
      <div className={className} style={{ display: 'flex' }} {...props}>{children}</div>
    ),
    IconButton: ({ children, className, ...props }) => (
      <button className={className} type="button" {...props}>{children}</button>
    ),
    Select: {
      Root: MockSelectRoot,
      Trigger: MockSelectTrigger,
      Content: MockSelectContent,
      Item: MockSelectItem,
    },
    TextField: {
      Root: ({ children, className, onChange, value, ...props }) => {
        // If TextField.Root has onChange/value, render as input (for SettingsPasswordInput/SettingsTextInput)
        if (onChange) {
          return <input className={className} onChange={onChange} value={value} {...props} />;
        }
        // Otherwise render as div container (for other uses)
        return <div className={className} {...props}>{children}</div>;
      },
      Input: ({ className, ...props }) => <input className={className} {...props} />,
      Slot: ({ children, className, ...props }) => <div className={className} {...props}>{children}</div>,
    },
    TextArea: ({ className, ...props }) => <textarea className={className} {...props} />,
  };
});

// Mock lucide-react icons
jest.mock('lucide-react', () => ({
  Globe: () => <span data-testid="globe-icon">🌐</span>,
  Eye: () => <span data-testid="eye-icon">👁</span>,
  EyeOff: () => <span data-testid="eye-off-icon">👁‍🗨</span>,
  AlertCircle: () => <span data-testid="alert-circle-icon">⚠</span>,
  Lock: () => <span data-testid="lock-icon" />,
  ExternalLink: () => <span data-testid="external-link-icon" />,
}));

const mockUseLicenseLevel = useLicenseLevelModule.useLicenseLevel as jest.Mock;

function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

describe('BlobStoreWizardStepGlobalDistribution', () => {
  const mockOnChange = jest.fn();
  const defaultFormData: BlobStoreFormData = {
    name: 'test-blob-store',
    type: 'S3',
  };

  beforeEach(() => {
    mockOnChange.mockClear();
    // Default to enterprise license being available
    mockUseLicenseLevel.mockReturnValue(true);
  });

  it('renders the form with CloudFront and S3 Transfer Acceleration disabled and pilled below Enterprise', async () => {
    mockUseLicenseLevel.mockReturnValue(false);

    renderWithTheme(
      <BlobStoreWizardStepGlobalDistribution
        data={defaultFormData}
        onChange={mockOnChange}
      />
    );

    expect(screen.getByText('Global Distribution (Optional)')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByRole('option', { name: 'CloudFront' })).toBeDisabled();
    });
    expect(screen.getByRole('option', { name: 'S3 Transfer Acceleration' })).toBeDisabled();
    expect(screen.getByRole('option', { name: 'None' })).toBeEnabled();
    expect(screen.getByTestId('option-badge-CLOUDFRONT')).toHaveTextContent('Enterprise');
    expect(screen.getByTestId('option-badge-TRANSFER_ACCELERATION')).toHaveTextContent('Enterprise');
  });

  it('enables every distribution option without pills on Enterprise', async () => {
    renderWithTheme(
      <BlobStoreWizardStepGlobalDistribution
        data={defaultFormData}
        onChange={mockOnChange}
      />
    );

    await waitFor(() => {
      expect(screen.getByRole('option', { name: 'CloudFront' })).toBeEnabled();
    });
    expect(screen.getByRole('option', { name: 'S3 Transfer Acceleration' })).toBeEnabled();
    expect(screen.queryByTestId('edition-pill-enterprise')).not.toBeInTheDocument();
  });

  it('shows the Enterprise license callout above the section below Enterprise', () => {
    mockUseLicenseLevel.mockReturnValue(false);

    renderWithTheme(
      <BlobStoreWizardStepGlobalDistribution
        data={defaultFormData}
        onChange={mockOnChange}
      />
    );

    const callout = screen.getByTestId('license-callout-global-distribution');
    expect(callout).toHaveTextContent('CloudFront and S3 Transfer Acceleration require an Enterprise license.');
    expect(
      callout.compareDocumentPosition(screen.getByText('Global Distribution (Optional)')) &
        Node.DOCUMENT_POSITION_FOLLOWING
    ).toBeTruthy();
  });

  it('shows no license callout on Enterprise', () => {
    renderWithTheme(
      <BlobStoreWizardStepGlobalDistribution
        data={defaultFormData}
        onChange={mockOnChange}
      />
    );

    expect(screen.queryByTestId('license-callout-global-distribution')).not.toBeInTheDocument();
  });

  it('renders the form for S3 type with enterprise license', () => {
    renderWithTheme(
      <BlobStoreWizardStepGlobalDistribution
        data={defaultFormData}
        onChange={mockOnChange}
      />
    );

    expect(screen.getByText('Global Distribution (Optional)')).toBeInTheDocument();
    expect(screen.getByLabelText('Distribution Type')).toBeInTheDocument();
  });

  it('shows CloudFront fields when CloudFront is selected', async () => {
    renderWithTheme(
      <BlobStoreWizardStepGlobalDistribution
        data={defaultFormData}
        onChange={mockOnChange}
      />
    );

    // Wait for options to be loaded
    await waitFor(() => {
      expect(screen.getByRole('combobox')).toBeInTheDocument();
    });

    const distributionTypeSelect = screen.getByRole('combobox');
    fireEvent.change(distributionTypeSelect, { target: { value: 'CLOUDFRONT' } });

    expect(mockOnChange).toHaveBeenCalledWith('globalDistribution', {
      type: 'CLOUDFRONT',
      distributionUrl: '',
      keyId: '',
      privateKey: '',
      privateKeyPassword: '',
    });
  });

  it('shows Transfer Acceleration when selected', async () => {
    renderWithTheme(
      <BlobStoreWizardStepGlobalDistribution
        data={defaultFormData}
        onChange={mockOnChange}
      />
    );

    // Wait for options to be loaded
    await waitFor(() => {
      expect(screen.getByRole('combobox')).toBeInTheDocument();
    });

    const distributionTypeSelect = screen.getByRole('combobox');
    fireEvent.change(distributionTypeSelect, { target: { value: 'TRANSFER_ACCELERATION' } });

    expect(mockOnChange).toHaveBeenCalledWith('globalDistribution', {
      type: 'TRANSFER_ACCELERATION',
    });
  });

  it('clears globalDistribution when None is selected', async () => {
    const formDataWithCloudFront: BlobStoreFormData = {
      ...defaultFormData,
      globalDistribution: {
        type: 'CLOUDFRONT',
        distributionUrl: 'https://example.cloudfront.net',
        keyId: 'test-key-id',
        privateKey: 'test-private-key',
        privateKeyPassword: 'test-password',
      },
    };

    renderWithTheme(
      <BlobStoreWizardStepGlobalDistribution
        data={formDataWithCloudFront}
        onChange={mockOnChange}
      />
    );

    // Wait for options to be loaded
    await waitFor(() => {
      expect(screen.getByRole('combobox')).toBeInTheDocument();
    });

    const distributionTypeSelect = screen.getByRole('combobox');
    fireEvent.change(distributionTypeSelect, { target: { value: '' } });

    expect(mockOnChange).toHaveBeenCalledWith('globalDistribution', undefined);
  });

  it('updates CloudFront fields individually', async () => {
    const formDataWithCloudFront: BlobStoreFormData = {
      ...defaultFormData,
      globalDistribution: {
        type: 'CLOUDFRONT',
        distributionUrl: '',
        keyId: '',
        privateKey: '',
        privateKeyPassword: '',
      },
    };

    renderWithTheme(
      <BlobStoreWizardStepGlobalDistribution
        data={formDataWithCloudFront}
        onChange={mockOnChange}
      />
    );

    // Find the label for Distribution URL
    expect(screen.getByText('Distribution URL')).toBeInTheDocument();

    // Find the input by its placeholder
    const distributionUrlInput = screen.getByPlaceholderText('https://d111111abcdef8.cloudfront.net/');

    // Use fireEvent.change to trigger the change event
    fireEvent.change(distributionUrlInput, { target: { value: 'https://d111111abcdef8.cloudfront.net/' } });

    expect(mockOnChange).toHaveBeenCalledWith('globalDistribution.distributionUrl', 'https://d111111abcdef8.cloudfront.net/');
  });
});
