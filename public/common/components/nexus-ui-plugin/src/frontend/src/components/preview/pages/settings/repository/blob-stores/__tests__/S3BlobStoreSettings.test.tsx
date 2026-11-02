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
import { render, screen, fireEvent, within } from '@testing-library/react';
import S3BlobStoreSettings from '../S3BlobStoreSettings';
import * as useBlobStoresModule from '../useBlobStores';

jest.mock('../useBlobStores', () => ({
  useS3DropdownValues: jest.fn(),
}));

// Mock ExtJS at the path the source uses
jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    isLicensedEdition: () => true,
    state: () => ({
      getValue: () => false
    }),
    useState: (getValue) => getValue(),
  }
}));

// Mock useLicenseLevel hook
jest.mock('../../../../../shared/hooks/useLicenseLevel', () => ({
  useLicenseLevel: jest.fn((level) => {
    // Mock PRO and ENTERPRISE availability based on edition
    const mockExtJS = require('../../../../../../../interface/ExtJS');
    const edition = mockExtJS.ExtJS.state().getValue('editionId', '');
    if (level === 'nexus-professional-edition') {
      return edition === 'nexus-professional-edition' ||
             edition === 'nexus-professional-plus-edition' ||
             edition === 'nexus-enterprise-edition';
    }
    if (level === 'nexus-enterprise-edition') {
      return edition === 'nexus-enterprise-edition';
    }
    return false;
  }),
  LicenseLevel: {
    PRO: 'nexus-professional-edition',
    PRO_PLUS: 'nexus-professional-plus-edition',
    ENTERPRISE: 'nexus-enterprise-edition'
  }
}));

// Mock shared form components
jest.mock('../../../../../shared/form', () => ({
  SettingsFormSection: ({ children, title }) => (
    <div data-testid="settings-form-section">
      <h2>{title}</h2>
      {children}
    </div>
  ),
  SettingsTextInput: ({ label, value, onChange, error, disabled }) => (
    <div>
      <label>{label}</label>
      <input
        value={value || ''}
        onChange={(e) => onChange(e.target.value)}
        disabled={disabled}
        data-testid={`input-${label}`}
      />
      {error && <p data-testid={`error-${label}`}>{error}</p>}
    </div>
  ),
  SettingsPasswordInput: ({ label, value, onChange }) => (
    <div>
      <label>{label}</label>
      <input
        type="password"
        value={value || ''}
        onChange={(e) => onChange(e.target.value)}
        data-testid={`password-${label}`}
      />
    </div>
  ),
  SettingsSelect: ({ label, value, onChange, options }) => (
    <div>
      <label>{label}</label>
      <select value={value} onChange={(e) => onChange(e.target.value)} data-testid={`select-${label}`}>
        {options.map((opt) => (
          <option key={opt.value} value={opt.value} disabled={opt.disabled}>{opt.label}</option>
        ))}
      </select>
      {options.filter((opt) => opt.badge).map((opt) => (
        <span key={opt.value} data-testid={`option-badge-${opt.value}`}>{opt.badge}</span>
      ))}
    </div>
  ),
  SettingsCheckbox: ({ label, checked, onChange, disabled, badge }) => (
    <div>
      <label>
        <input
          type="checkbox"
          checked={checked}
          onChange={(e) => onChange(e.target.checked)}
          disabled={disabled}
          data-testid={`checkbox-${label}`}
        />
        {label}
      </label>
      {badge}
    </div>
  ),
  SettingsAlert: ({ children }) => <div data-testid="alert">{children}</div>,
  SettingsButton: ({ children, onClick, disabled }) => (
    <button onClick={onClick} disabled={disabled}>{children}</button>
  ),
  SettingsTextArea: ({ label, value, onChange, helpText }) => (
    <div>
      <label>{label}</label>
      {helpText && <p data-testid={`help-${label}`}>{helpText}</p>}
      <textarea
        value={value || ''}
        onChange={(e) => onChange(e.target.value)}
        data-testid={`textarea-${label}`}
      />
    </div>
  )
}));

const COMMUNITY = 'nexus-community-edition';
const PRO = 'nexus-professional-edition';
const ENTERPRISE = 'nexus-enterprise-edition';
const PRO_FAMILY = [PRO, 'nexus-professional-plus-edition', ENTERPRISE];

function setEdition(editionId: string) {
  const { ExtJS } = require('../../../../../../../interface/ExtJS');
  ExtJS.isLicensedEdition = () => PRO_FAMILY.includes(editionId);
  ExtJS.state = () => ({
    getValue: (key: string, defaultValue: unknown = false) => (key === 'editionId' ? editionId : defaultValue),
  });
}

describe('S3BlobStoreSettings', () => {
  const mockUseS3DropdownValues = useBlobStoresModule.useS3DropdownValues as jest.Mock;

  const defaultProps = {
    data: {
      name: 'test-s3',
      bucketConfiguration: {
        bucket: {
          region: 'us-east-1',
          name: 'my-bucket',
          prefix: 'nexus/'
        },
        bucketSecurity: {},
        encryption: {},
        advancedBucketConnection: {},
        failoverBuckets: []
      }
    },
    onChange: jest.fn(),
    disabled: false,
    isEdit: false
  };

  beforeEach(() => {
    jest.clearAllMocks();
    setEdition(PRO);

    mockUseS3DropdownValues.mockReturnValue({
      values: {
        regions: [
          { id: 'us-east-1', name: 'US East (N. Virginia)' },
          { id: 'us-west-2', name: 'US West (Oregon)' }
        ],
        encryptionTypes: [
          { id: 's3', name: 'S3 Managed' },
          { id: 'kms', name: 'KMS Managed' }
        ]
      },
      loading: false
    });
  });

  it('renders basic configuration section', () => {
    render(<S3BlobStoreSettings {...defaultProps} />);
    expect(screen.getByText('S3 Bucket Configuration')).toBeInTheDocument();
  });

  it('displays bucket name input', () => {
    render(<S3BlobStoreSettings {...defaultProps} />);
    const input = screen.getByTestId('input-Bucket');
    expect(input).toHaveValue('my-bucket');
  });

  it('displays region selector', () => {
    render(<S3BlobStoreSettings {...defaultProps} />);
    expect(screen.getByTestId('select-Region')).toBeInTheDocument();
  });

  it('calls onChange when bucket name is updated', () => {
    render(<S3BlobStoreSettings {...defaultProps} />);
    const input = screen.getByTestId('input-Bucket');
    
    fireEvent.change(input, { target: { value: 'new-bucket' } });
    
    expect(defaultProps.onChange).toHaveBeenCalledWith('bucketConfiguration.bucket.name', 'new-bucket');
  });

  it('renders authentication section', () => {
    render(<S3BlobStoreSettings {...defaultProps} />);
    expect(screen.getByText('Authentication (Optional)')).toBeInTheDocument();
  });

  it('renders encryption section', () => {
    render(<S3BlobStoreSettings {...defaultProps} />);
    expect(screen.getByText('Encryption (Optional)')).toBeInTheDocument();
  });

  it('renders advanced settings section', () => {
    render(<S3BlobStoreSettings {...defaultProps} />);
    expect(screen.getByText('Advanced Connection Settings (Optional)')).toBeInTheDocument();
  });

  it('displays pre-signed URL checkbox for Pro edition', () => {
    render(<S3BlobStoreSettings {...defaultProps} />);
    expect(screen.getByTestId('checkbox-Pre-Signed URL')).toBeInTheDocument();
  });

  it('shows documentation link', () => {
    render(<S3BlobStoreSettings {...defaultProps} />);
    expect(screen.getByText('View Documentation →')).toBeInTheDocument();
  });

  describe('field-level validation messages', () => {
    it('shouldDisplayBucketNameErrorWhenProvided', () => {
      render(
        <S3BlobStoreSettings
          {...defaultProps}
          errors={{ 'bucketConfiguration.bucket.name': 'Bucket name must be between 3 and 63 characters' }}
        />
      );

      expect(screen.getByTestId('error-Bucket')).toHaveTextContent(
        'Bucket name must be between 3 and 63 characters'
      );
    });

    it('shouldNotDisplayBucketNameErrorWhenAbsent', () => {
      render(<S3BlobStoreSettings {...defaultProps} />);

      expect(screen.queryByTestId('error-Bucket')).not.toBeInTheDocument();
    });
  });

  describe('edition pills', () => {
    const distributionOption = (name: string) =>
      within(screen.getByTestId('select-Distribution Type')).getByRole('option', { name });

    it('disables Pre-Signed URL with a Pro pill on Community Edition', () => {
      setEdition(COMMUNITY);
      render(<S3BlobStoreSettings {...defaultProps} />);

      expect(screen.getByTestId('checkbox-Pre-Signed URL')).toBeDisabled();
      expect(screen.getByTestId('edition-pill-pro')).toHaveTextContent('Pro');
    });

    it('enables Pre-Signed URL without a pill on a Pro license', () => {
      setEdition(PRO);
      render(<S3BlobStoreSettings {...defaultProps} />);

      expect(screen.getByTestId('checkbox-Pre-Signed URL')).toBeEnabled();
      expect(screen.queryByTestId('edition-pill-pro')).not.toBeInTheDocument();
    });

    it.each([COMMUNITY, PRO])('disables CloudFront and S3 Transfer Acceleration with Enterprise pills on %s', (editionId) => {
      setEdition(editionId);
      render(<S3BlobStoreSettings {...defaultProps} />);

      expect(distributionOption('None')).toBeEnabled();
      expect(distributionOption('S3 Transfer Acceleration')).toBeDisabled();
      expect(distributionOption('CloudFront')).toBeDisabled();
      expect(within(screen.getByTestId('option-badge-TRANSFER_ACCELERATION')).getByTestId('edition-pill-enterprise'))
        .toHaveTextContent('Enterprise');
      expect(within(screen.getByTestId('option-badge-CLOUDFRONT')).getByTestId('edition-pill-enterprise'))
        .toHaveTextContent('Enterprise');
    });

    it('shows no pills and enables every option on Enterprise', () => {
      setEdition(ENTERPRISE);
      render(<S3BlobStoreSettings {...defaultProps} />);

      expect(screen.getByTestId('checkbox-Pre-Signed URL')).toBeEnabled();
      expect(distributionOption('S3 Transfer Acceleration')).toBeEnabled();
      expect(distributionOption('CloudFront')).toBeEnabled();
      expect(screen.queryAllByTestId(/^edition-pill-/)).toHaveLength(0);
    });

    it('keeps the Enterprise pill on a saved CloudFront configuration after a downgrade to Pro', () => {
      setEdition(PRO);
      render(
        <S3BlobStoreSettings
          {...defaultProps}
          isEdit
          data={{
            ...defaultProps.data,
            globalDistribution: { type: 'CLOUDFRONT', distributionUrl: 'https://d1.cloudfront.net/', keyId: 'k1' },
          }}
        />
      );

      expect(screen.getByTestId('select-Distribution Type')).toHaveValue('CLOUDFRONT');
      expect(distributionOption('CloudFront')).toBeDisabled();
      expect(within(screen.getByTestId('option-badge-CLOUDFRONT')).getByTestId('edition-pill-enterprise'))
        .toBeInTheDocument();
      expect(screen.getByTestId('input-Distribution URL')).toBeDisabled();
      expect(screen.getByTestId('input-Key ID')).toBeDisabled();
      expect(screen.getByRole('button', { name: 'Edit Private Key' })).toBeDisabled();
    });

    it('keeps CloudFront fields editable on Enterprise', () => {
      setEdition(ENTERPRISE);
      render(
        <S3BlobStoreSettings
          {...defaultProps}
          data={{ ...defaultProps.data, globalDistribution: { type: 'CLOUDFRONT', distributionUrl: '', keyId: '' } }}
        />
      );

      expect(screen.getByTestId('input-Distribution URL')).toBeEnabled();
      expect(screen.getByTestId('input-Key ID')).toBeEnabled();
    });

    it('keeps the Pro pill on an enabled Pre-Signed URL after a downgrade to Community Edition', () => {
      setEdition(COMMUNITY);
      render(
        <S3BlobStoreSettings
          {...defaultProps}
          data={{
            ...defaultProps.data,
            bucketConfiguration: { ...defaultProps.data.bucketConfiguration, preSignedUrlEnabled: true },
          }}
        />
      );

      expect(screen.getByTestId('checkbox-Pre-Signed URL')).toBeChecked();
      expect(screen.getByTestId('checkbox-Pre-Signed URL')).toBeDisabled();
      expect(screen.getByTestId('edition-pill-pro')).toBeInTheDocument();
    });
  });

  describe('license callouts', () => {
    const PRESIGNED_CALLOUT = 'license-callout-presigned-url';
    const GLOBAL_DISTRIBUTION_CALLOUT = 'license-callout-global-distribution';
    const precedes = (first: HTMLElement, second: HTMLElement) =>
      Boolean(first.compareDocumentPosition(second) & Node.DOCUMENT_POSITION_FOLLOWING);

    it('shows both callouts on Community Edition', () => {
      setEdition(COMMUNITY);
      render(<S3BlobStoreSettings {...defaultProps} />);

      expect(screen.getByTestId(PRESIGNED_CALLOUT)).toHaveTextContent('Pre-Signed URL requires a Pro license.');
      expect(screen.getByTestId(GLOBAL_DISTRIBUTION_CALLOUT)).toHaveTextContent(
        'CloudFront and S3 Transfer Acceleration require an Enterprise license.'
      );
    });

    it.each([PRO, 'nexus-professional-plus-edition'])(
      'shows only the Global Distribution callout on %s',
      (editionId) => {
        setEdition(editionId);
        render(<S3BlobStoreSettings {...defaultProps} />);

        expect(screen.queryByTestId(PRESIGNED_CALLOUT)).not.toBeInTheDocument();
        expect(screen.getByTestId(GLOBAL_DISTRIBUTION_CALLOUT)).toBeInTheDocument();
      }
    );

    it('shows no callouts on Enterprise', () => {
      setEdition(ENTERPRISE);
      render(<S3BlobStoreSettings {...defaultProps} />);

      expect(screen.queryByTestId(PRESIGNED_CALLOUT)).not.toBeInTheDocument();
      expect(screen.queryByTestId(GLOBAL_DISTRIBUTION_CALLOUT)).not.toBeInTheDocument();
    });

    it('places each callout directly above its feature', () => {
      setEdition(COMMUNITY);
      render(<S3BlobStoreSettings {...defaultProps} />);

      const presigned = screen.getByTestId(PRESIGNED_CALLOUT);
      expect(precedes(screen.getByTestId('input-Prefix'), presigned)).toBe(true);
      expect(precedes(presigned, screen.getByTestId('checkbox-Pre-Signed URL'))).toBe(true);

      const globalDistribution = screen.getByTestId(GLOBAL_DISTRIBUTION_CALLOUT);
      expect(precedes(screen.getByTestId('checkbox-Pre-Signed URL'), globalDistribution)).toBe(true);
      expect(precedes(globalDistribution, screen.getByText('Global Distribution (Optional)'))).toBe(true);
    });

    it('keeps both callouts on a read-only form', () => {
      setEdition(COMMUNITY);
      render(<S3BlobStoreSettings {...defaultProps} disabled />);

      expect(screen.getByTestId(PRESIGNED_CALLOUT)).toBeInTheDocument();
      expect(screen.getByTestId(GLOBAL_DISTRIBUTION_CALLOUT)).toBeInTheDocument();
    });
  });

  describe('global distribution', () => {
    it.each([COMMUNITY, PRO, ENTERPRISE])('renders global distribution section on %s', (editionId) => {
      setEdition(editionId);
      render(<S3BlobStoreSettings {...defaultProps} />);

      expect(screen.getByText('Global Distribution (Optional)')).toBeInTheDocument();
      expect(screen.getByTestId('select-Distribution Type')).toBeInTheDocument();
    });

    it('shows CloudFront fields when CloudFront is selected', () => {
      const mockExtJS = require('../../../../../../../interface/ExtJS');
      mockExtJS.ExtJS.state = () => ({
        getValue: (key: string, defaultValue: any) => {
          if (key === 'editionId') return 'nexus-enterprise-edition';
          return defaultValue;
        }
      });

      const props = {
        ...defaultProps,
        data: {
          ...defaultProps.data,
          globalDistribution: {
            type: 'CLOUDFRONT',
            distributionUrl: 'https://d111111abcdef8.cloudfront.net/',
            keyId: 'test-key-id',
            privateKey: 'test-private-key'
          }
        }
      };

      render(<S3BlobStoreSettings {...props} />);
      expect(screen.getByTestId('input-Distribution URL')).toBeInTheDocument();
      expect(screen.getByTestId('input-Key ID')).toBeInTheDocument();
      expect(screen.getByTestId('textarea-Private Key')).toBeInTheDocument();
    });

    it('shows private key fields immediately when switching to CloudFront in edit mode', () => {
      const mockExtJS = require('../../../../../../../interface/ExtJS');
      mockExtJS.ExtJS.state = () => ({
        getValue: (key: string, defaultValue: any) => {
          if (key === 'editionId') return 'nexus-enterprise-edition';
          return defaultValue;
        }
      });

      const props = {
        ...defaultProps,
        isEdit: true,
        data: {
          ...defaultProps.data,
          name: 'test-blobstore',
          globalDistribution: {
            type: 'CLOUDFRONT',
            distributionUrl: '', // Empty - user just switched to CloudFront
            keyId: '', // Empty - user just switched to CloudFront
            privateKey: '',
            privateKeyPassword: ''
          }
        }
      };

      render(<S3BlobStoreSettings {...props} />);

      // Should show fields immediately (not the edit button) because there's no existing config
      expect(screen.getByTestId('textarea-Private Key')).toBeInTheDocument();
      expect(screen.getByTestId('password-Private Key Password')).toBeInTheDocument();

      // Should NOT show the edit button
      expect(screen.queryByText('Edit Private Key')).not.toBeInTheDocument();
    });

    it('keeps private key fields visible when user fills in distributionUrl and keyId after switching to CloudFront', () => {
      const mockExtJS = require('../../../../../../../interface/ExtJS');
      mockExtJS.ExtJS.state = () => ({
        getValue: (key: string, defaultValue: any) => {
          if (key === 'editionId') return 'nexus-enterprise-edition';
          return defaultValue;
        }
      });

      const props = {
        ...defaultProps,
        isEdit: true,
        data: {
          ...defaultProps.data,
          name: 'test-blobstore',
          globalDistribution: {
            type: 'CLOUDFRONT',
            distributionUrl: '', // Will be filled by user
            keyId: '', // Will be filled by user
            privateKey: '',
            privateKeyPassword: ''
          }
        }
      };

      render(<S3BlobStoreSettings {...props} />);

      // Initially fields are shown
      expect(screen.getByTestId('textarea-Private Key')).toBeInTheDocument();

      // User fills in distributionUrl
      const distributionUrlInput = screen.getByTestId('input-Distribution URL');
      fireEvent.change(distributionUrlInput, { target: { value: 'https://d111111abcdef8.cloudfront.net/' } });

      // User fills in keyId
      const keyIdInput = screen.getByTestId('input-Key ID');
      fireEvent.change(keyIdInput, { target: { value: 'test-key-id' } });

      // Fields should STILL be shown (not replaced with edit button)
      expect(screen.getByTestId('textarea-Private Key')).toBeInTheDocument();
      expect(screen.getByTestId('password-Private Key Password')).toBeInTheDocument();
      expect(screen.queryByText('Edit Private Key')).not.toBeInTheDocument();
    });

    it('shows Transfer Acceleration as an option', () => {
      const mockExtJS = require('../../../../../../../interface/ExtJS');
      mockExtJS.ExtJS.state = () => ({
        getValue: (key: string, defaultValue: any) => {
          if (key === 'editionId') return 'nexus-enterprise-edition';
          return defaultValue;
        }
      });

      const props = {
        ...defaultProps,
        data: {
          ...defaultProps.data,
          globalDistribution: { type: 'TRANSFER_ACCELERATION' }
        }
      };

      render(<S3BlobStoreSettings {...props} />);

      // Transfer Acceleration has no additional fields
      expect(screen.queryByTestId('input-Distribution URL')).not.toBeInTheDocument();
    });

    it('clears global distribution when None is selected', () => {
      const mockExtJS = require('../../../../../../../interface/ExtJS');
      mockExtJS.ExtJS.state = () => ({
        getValue: (key: string, defaultValue: any) => {
          if (key === 'editionId') return 'nexus-enterprise-edition';
          return defaultValue;
        }
      });

      const props = {
        ...defaultProps,
        data: {
          ...defaultProps.data,
          globalDistribution: { type: 'TRANSFER_ACCELERATION' }
        }
      };

      render(<S3BlobStoreSettings {...props} />);
      const select = screen.getByTestId('select-Distribution Type');

      fireEvent.change(select, { target: { value: '' } });

      expect(props.onChange).toHaveBeenCalledWith('globalDistribution', undefined);
    });

    describe('CloudFront sensitive fields in edit mode', () => {
      it('shows edit button instead of private key fields when editing existing CloudFront config', () => {
        const mockExtJS = require('../../../../../../../interface/ExtJS');
        mockExtJS.ExtJS.state = () => ({
          getValue: (key: string, defaultValue: any) => {
            if (key === 'editionId') return 'nexus-enterprise-edition';
            return defaultValue;
          }
        });

        const props = {
          ...defaultProps,
          isEdit: true,
          data: {
            ...defaultProps.data,
            name: 'test-blobstore', // Name indicates edit mode
            globalDistribution: {
              type: 'CLOUDFRONT',
              distributionUrl: 'https://d111111abcdef8.cloudfront.net/',
              keyId: 'test-key-id',
              privateKey: '', // Empty - API doesn't return WRITE_ONLY fields
              privateKeyPassword: '' // Empty - API doesn't return WRITE_ONLY fields
            }
          }
        };

        render(<S3BlobStoreSettings {...props} />);

        // Should show the notice and edit button, not the fields
        expect(screen.getByText('A private key has been configured for this CloudFront distribution.')).toBeInTheDocument();
        expect(screen.getByText('Edit Private Key')).toBeInTheDocument();

        // Should NOT show the private key fields initially
        expect(screen.queryByTestId('textarea-Private Key')).not.toBeInTheDocument();
        expect(screen.queryByTestId('password-Private Key Password')).not.toBeInTheDocument();
      });

      it('shows private key fields when edit button is clicked', () => {
        const mockExtJS = require('../../../../../../../interface/ExtJS');
        mockExtJS.ExtJS.state = () => ({
          getValue: (key: string, defaultValue: any) => {
            if (key === 'editionId') return 'nexus-enterprise-edition';
            return defaultValue;
          }
        });

        const props = {
          ...defaultProps,
          isEdit: true,
          data: {
            ...defaultProps.data,
            name: 'test-blobstore',
            globalDistribution: {
              type: 'CLOUDFRONT',
              distributionUrl: 'https://d111111abcdef8.cloudfront.net/',
              keyId: 'test-key-id',
              privateKey: '',
              privateKeyPassword: ''
            }
          }
        };

        render(<S3BlobStoreSettings {...props} />);

        // Click the edit button
        const editButton = screen.getByText('Edit Private Key');
        fireEvent.click(editButton);

        // Now the fields should be visible
        expect(screen.getByTestId('textarea-Private Key')).toBeInTheDocument();
        expect(screen.getByTestId('password-Private Key Password')).toBeInTheDocument();

        // And the cancel button should be visible
        expect(screen.getByText('Cancel')).toBeInTheDocument();
      });

      it('hides private key fields when cancel button is clicked', () => {
        const mockExtJS = require('../../../../../../../interface/ExtJS');
        mockExtJS.ExtJS.state = () => ({
          getValue: (key: string, defaultValue: any) => {
            if (key === 'editionId') return 'nexus-enterprise-edition';
            return defaultValue;
          }
        });

        const props = {
          ...defaultProps,
          isEdit: true,
          data: {
            ...defaultProps.data,
            name: 'test-blobstore',
            globalDistribution: {
              type: 'CLOUDFRONT',
              distributionUrl: 'https://d111111abcdef8.cloudfront.net/',
              keyId: 'test-key-id',
              privateKey: '',
              privateKeyPassword: ''
            }
          }
        };

        render(<S3BlobStoreSettings {...props} />);

        // Click edit to show fields
        fireEvent.click(screen.getByText('Edit Private Key'));
        expect(screen.getByTestId('textarea-Private Key')).toBeInTheDocument();

        // Click cancel to hide them
        fireEvent.click(screen.getByText('Cancel'));
        expect(screen.queryByTestId('textarea-Private Key')).not.toBeInTheDocument();
        expect(screen.queryByTestId('password-Private Key Password')).not.toBeInTheDocument();
      });

      it('does not modify form state with placeholder on mount', () => {
        const mockExtJS = require('../../../../../../../interface/ExtJS');
        mockExtJS.ExtJS.state = () => ({
          getValue: (key: string, defaultValue: any) => {
            if (key === 'editionId') return 'nexus-enterprise-edition';
            return defaultValue;
          }
        });

        const props = {
          ...defaultProps,
          isEdit: true,
          data: {
            ...defaultProps.data,
            name: 'test-blobstore',
            globalDistribution: {
              type: 'CLOUDFRONT',
              distributionUrl: 'https://d111111abcdef8.cloudfront.net/',
              keyId: 'test-key-id',
              privateKey: '',
              privateKeyPassword: ''
            }
          }
        };

        render(<S3BlobStoreSettings {...props} />);

        // onChange should NOT be called to inject placeholder
        expect(props.onChange).not.toHaveBeenCalled();
      });

      it('allows user to enter new private key after clicking edit', () => {
        const mockExtJS = require('../../../../../../../interface/ExtJS');
        mockExtJS.ExtJS.state = () => ({
          getValue: (key: string, defaultValue: any) => {
            if (key === 'editionId') return 'nexus-enterprise-edition';
            return defaultValue;
          }
        });

        const props = {
          ...defaultProps,
          isEdit: true,
          data: {
            ...defaultProps.data,
            name: 'test-blobstore',
            globalDistribution: {
              type: 'CLOUDFRONT',
              distributionUrl: 'https://d111111abcdef8.cloudfront.net/',
              keyId: 'test-key-id',
              privateKey: '', // Empty - as returned by API
              privateKeyPassword: ''
            }
          }
        };

        render(<S3BlobStoreSettings {...props} />);

        // Click edit button first
        fireEvent.click(screen.getByText('Edit Private Key'));

        const privateKeyField = screen.getByTestId('textarea-Private Key');

        // User types a new key
        fireEvent.change(privateKeyField, { target: { value: 'new-private-key' } });

        // Should send the new value
        expect(props.onChange).toHaveBeenCalledWith(
          'globalDistribution.privateKey',
          'new-private-key'
        );
      });

      it('sends actual password value when user types after clicking edit', () => {
        const mockExtJS = require('../../../../../../../interface/ExtJS');
        mockExtJS.ExtJS.state = () => ({
          getValue: (key: string, defaultValue: any) => {
            if (key === 'editionId') return 'nexus-enterprise-edition';
            return defaultValue;
          }
        });

        const props = {
          ...defaultProps,
          isEdit: true,
          data: {
            ...defaultProps.data,
            name: 'test-blobstore',
            globalDistribution: {
              type: 'CLOUDFRONT',
              distributionUrl: 'https://d111111abcdef8.cloudfront.net/',
              keyId: 'test-key-id',
              privateKey: '',
              privateKeyPassword: ''
            }
          }
        };

        render(<S3BlobStoreSettings {...props} />);

        // Click edit button first
        fireEvent.click(screen.getByText('Edit Private Key'));

        const passwordField = screen.getByTestId('password-Private Key Password');

        // User types a new password
        fireEvent.change(passwordField, { target: { value: 'new-password' } });

        // Should send the actual value
        expect(props.onChange).toHaveBeenCalledWith(
          'globalDistribution.privateKeyPassword',
          'new-password'
        );
      });
    });
  });
});

