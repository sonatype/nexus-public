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
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';

jest.mock('../../../../../shared/form', () => ({
  WizardForm: ({ children, steps, currentStep, onStepChange, onComplete, onCancel, canAdvance, title, description, testId }) => (
    <div data-testid={testId}>
      <h1>{title}</h1>
      <p>{description}</p>
      <div data-testid={`${testId}-steps`}>
        {steps.map((s, i) => <span key={s.id}>{s.label}</span>)}
      </div>
      {children}
      <button
        onClick={() => onStepChange(currentStep + 1)}
        disabled={!canAdvance}
      >
        Continue
      </button>
      <button onClick={onCancel}>Cancel</button>
      <button onClick={onComplete} disabled={!canAdvance}>Create Blob Store</button>
    </div>
  ),
  SettingsFormSection: ({ children, title, description }) => (
    <div>
      <h2>{title}</h2>
      {description && <p>{description}</p>}
      {children}
    </div>
  ),
  SettingsTextInput: ({ name, label, value, onChange, placeholder, helpText, required, error, disabled, monospace }) => (
    <div>
      <label htmlFor={name}>{label}</label>
      <input
        id={name}
        value={value || ''}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        disabled={disabled}
        aria-label={label}
      />
      {error && <span role="alert">{error}</span>}
    </div>
  ),
  SettingsSelect: ({ name, label, value, onChange, options, helpText, required, error, disabled }) => (
    <div>
      <label htmlFor={name}>{label}</label>
      <select id={name} value={value} onChange={(e) => onChange(e.target.value)} disabled={disabled}>
        {options.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
      </select>
      {error && <span role="alert">{error}</span>}
    </div>
  ),
  SettingsCheckbox: ({ name, label, checked, onChange, disabled, badge }) => (
    <div>
      <label htmlFor={name}>{label}</label>
      <input id={name} type="checkbox" checked={checked} onChange={(e) => onChange(e.target.checked)} disabled={disabled} />
      {badge}
    </div>
  ),
  SettingsTransferList: () => <div data-testid="transfer-list" />,
  SettingsAlert: ({ children, type }) => <div data-testid="settings-alert" data-type={type}>{children}</div>,
}));

jest.mock('../../../../../shared', () => ({
  clearDirtyState: jest.fn(),
  useToast: () => ({ success: jest.fn(), error: jest.fn() }),
  PageHeader: ({ breadcrumbs, children }) => (
    <div data-testid="page-header">
      {breadcrumbs?.map((b) => (
        b.onClick
          ? <button key={b.label} onClick={b.onClick}>{b.label}</button>
          : <span key={b.label}>{b.label}</span>
      ))}
      {children}
    </div>
  ),
}));

jest.mock('../../../../../shared/hooks/useLicenseLevel', () => ({
  useLicenseLevel: jest.fn(() => false), // Default to non-enterprise
  LicenseLevel: {
    PRO: 'nexus-professional-edition',
    PRO_PLUS: 'nexus-professional-plus-edition',
    ENTERPRISE: 'nexus-enterprise-edition',
  },
}));

jest.mock('../useBlobStores', () => ({
  useBlobStore: () => ({
    save: jest.fn(),
  }),
  useBlobStoreTypes: () => ({
    types: [
      {
        id: 'file',
        name: 'File',
        fields: [
          {
            id: 'path',
            type: 'string',
            label: 'Path',
            required: true,
            attributes: { tokenReplacement: '/sonatype-work/blobs/${name}' },
          },
        ],
      },
      { id: 's3', name: 'S3' },
      { id: 'azure', name: 'Azure Cloud Storage' },
      { id: 'group', name: 'Group' },
    ],
    quotaTypes: [{ id: 'spaceUsedQuota', name: 'Space Used' }],
    loading: false,
  }),
  useS3DropdownValues: () => ({
    values: { regions: [] },
    loading: false,
  }),
  useGroupableBlobStores: () => ({
    blobStores: [],
    loading: false,
  }),
}));

jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    isLicensedEdition: () => false,
    useState: (fn) => fn(),
    state: () => ({
      getValue: (key) => {
        if (key === 'nexus.datastore.clustered.enabled') return false;
        if (key === 'nexus.application.workDirectory') return '/nexus-data';
        return undefined;
      },
    }),
  },
}));

jest.mock('../BlobStoreTypeSelector', () => ({
  BlobStoreTypeSelector: ({ selectedType, onSelect }) => (
    <div data-testid="type-selector">
      <p>Choose storage type</p>
      <button onClick={() => onSelect('file')}>File</button>
      <button onClick={() => onSelect('s3')}>S3</button>
      <button onClick={() => onSelect('azure')}>Azure</button>
      <button onClick={() => onSelect('group')}>Group</button>
    </div>
  ),
}));

jest.mock('../BlobStoreWizardStepCredentials', () => ({
  BlobStoreWizardStepCredentials: () => <div data-testid="step-credentials" />,
}));

jest.mock('../BlobStoreWizardStepAdvanced', () => ({
  BlobStoreWizardStepAdvanced: () => <div data-testid="step-advanced" />,
}));

import { BlobStoreWizardCreate } from '../BlobStoreWizardCreate';

async function goToBasicConfigStep() {
  render(<BlobStoreWizardCreate onBack={jest.fn()} />);
  await userEvent.click(screen.getByText('File'));
  await userEvent.click(screen.getByRole('button', { name: /Continue/i }));
}

describe('BlobStoreWizardCreate', () => {
  it('renders the wizard with step 0 (type selector)', () => {
    render(<BlobStoreWizardCreate onBack={jest.fn()} />);

    expect(screen.getAllByText('Create Blob Store').length).toBeGreaterThan(0);
    expect(screen.getByText('Choose storage type')).toBeInTheDocument();
    expect(screen.getByText('File')).toBeInTheDocument();
  });

  it('has step indicator for the wizard', () => {
    render(<BlobStoreWizardCreate onBack={jest.fn()} />);

    expect(screen.getByTestId('blob-store-wizard-steps')).toBeInTheDocument();
  });

  it('shows Continue button on step 0 (type selection)', () => {
    render(<BlobStoreWizardCreate onBack={jest.fn()} />);

    expect(screen.getByRole('button', { name: /Continue/i })).toBeInTheDocument();
  });

  it('auto-fills path when name is typed for file blob store', async () => {
    await goToBasicConfigStep();

    const nameInput = screen.getByLabelText(/name/i);
    const pathInput = screen.getByLabelText(/path/i);

    await userEvent.type(nameInput, 'myblob');

    expect(pathInput).toHaveValue('/sonatype-work/blobs/myblob');
  });

  it('stops auto-filling path after user manually edits it', async () => {
    await goToBasicConfigStep();

    const nameInput = screen.getByLabelText(/name/i);
    const pathInput = screen.getByLabelText(/path/i);

    await userEvent.type(nameInput, 'first');
    expect(pathInput).toHaveValue('/sonatype-work/blobs/first');

    await userEvent.clear(pathInput);
    await userEvent.type(pathInput, '/custom/path');

    await userEvent.clear(nameInput);
    await userEvent.type(nameInput, 'second');
    expect(pathInput).toHaveValue('/custom/path');
  });

  it('disables Next on Basic Config when path is manually cleared', async () => {
    await goToBasicConfigStep();

    const nameInput = screen.getByLabelText(/name/i);
    const pathInput = screen.getByLabelText(/path/i);

    await userEvent.type(nameInput, 'my-blob-store');
    await userEvent.clear(pathInput);

    const nextButton = screen.getByRole('button', { name: /Continue/i });
    expect(nextButton).toBeDisabled();
  });

  it('disables Next on Basic Config when path is root slash', async () => {
    await goToBasicConfigStep();

    const nameInput = screen.getByLabelText(/name/i);
    const pathInput = screen.getByLabelText(/path/i);

    await userEvent.type(nameInput, 'my-blob-store');
    await userEvent.clear(pathInput);
    await userEvent.type(pathInput, '/');

    const nextButton = screen.getByRole('button', { name: /Continue/i });
    expect(nextButton).toBeDisabled();
  });

  it('enables Next on Basic Config when name auto-fills path', async () => {
    await goToBasicConfigStep();

    const nameInput = screen.getByLabelText(/name/i);
    await userEvent.type(nameInput, 'my-blob-store');

    const nextButton = screen.getByRole('button', { name: /Continue/i });
    expect(nextButton).toBeEnabled();
  });

  describe('S3 bucket name validation in wizard step 1', () => {
    async function goToS3BasicConfigStep() {
      render(<BlobStoreWizardCreate onBack={jest.fn()} />);
      await userEvent.click(screen.getByText('S3'));
      await userEvent.click(screen.getByRole('button', { name: /Continue/i }));
    }

    it('shouldBlockAdvanceWhenS3BucketNameIsTooShort', async () => {
      await goToS3BasicConfigStep();

      const nameInput = screen.getByLabelText(/^name$/i);
      await userEvent.type(nameInput, 'my-s3-store');

      const bucketInput = screen.getByLabelText(/bucket/i);
      await userEvent.type(bucketInput, 'ab');

      expect(screen.getByRole('button', { name: /Continue/i })).toBeDisabled();
    });

    it('shouldAllowAdvanceWhenS3BucketNameMeetsMinimumLength', async () => {
      await goToS3BasicConfigStep();

      const nameInput = screen.getByLabelText(/^name$/i);
      await userEvent.type(nameInput, 'my-s3-store');

      const bucketInput = screen.getByLabelText(/bucket/i);
      await userEvent.type(bucketInput, 'abc');

      expect(screen.getByRole('button', { name: /Continue/i })).toBeEnabled();
    });
  });

  describe('S3 Pre-Signed URL in wizard step 1', () => {
    async function goToS3BasicConfigStep() {
      render(<BlobStoreWizardCreate onBack={jest.fn()} />);
      await userEvent.click(screen.getByText('S3'));
      await userEvent.click(screen.getByRole('button', { name: /Continue/i }));
    }

    afterEach(() => {
      const { ExtJS } = require('../../../../../../../interface/ExtJS');
      ExtJS.isLicensedEdition = () => false;
    });

    it('shows Pre-Signed URL disabled with a Pro pill on Community Edition', async () => {
      await goToS3BasicConfigStep();

      expect(screen.getByLabelText('Pre-Signed URL')).toBeDisabled();
      expect(screen.getByTestId('edition-pill-pro')).toHaveTextContent('Pro');
    });

    it('enables Pre-Signed URL without a pill on a licensed edition', async () => {
      const { ExtJS } = require('../../../../../../../interface/ExtJS');
      ExtJS.isLicensedEdition = () => true;

      await goToS3BasicConfigStep();

      expect(screen.getByLabelText('Pre-Signed URL')).toBeEnabled();
      expect(screen.queryByTestId('edition-pill-pro')).not.toBeInTheDocument();
    });

    it('shows the Pro license callout between Prefix and Pre-Signed URL on Community Edition', async () => {
      await goToS3BasicConfigStep();

      const callout = screen.getByTestId('license-callout-presigned-url');
      expect(callout).toHaveTextContent('Pre-Signed URL requires a Pro license.');
      expect(
        screen.getByLabelText('Prefix').compareDocumentPosition(callout) & Node.DOCUMENT_POSITION_FOLLOWING
      ).toBeTruthy();
      expect(
        callout.compareDocumentPosition(screen.getByLabelText('Pre-Signed URL')) & Node.DOCUMENT_POSITION_FOLLOWING
      ).toBeTruthy();
    });

    it('shows no license callout on a licensed edition', async () => {
      const { ExtJS } = require('../../../../../../../interface/ExtJS');
      ExtJS.isLicensedEdition = () => true;

      await goToS3BasicConfigStep();

      expect(screen.queryByTestId('license-callout-presigned-url')).not.toBeInTheDocument();
    });
  });

  describe('Azure account/container name validation in wizard step 1', () => {
    async function goToAzureBasicConfigStep() {
      render(<BlobStoreWizardCreate onBack={jest.fn()} />);
      await userEvent.click(screen.getByText('Azure'));
      await userEvent.click(screen.getByRole('button', { name: /Continue/i }));
    }

    it('shouldBlockAdvanceWhenAzureAccountNameIsTooShort', async () => {
      await goToAzureBasicConfigStep();

      await userEvent.type(screen.getByLabelText(/^name$/i), 'my-azure-store');
      await userEvent.type(screen.getByLabelText(/account name/i), 'ab');
      await userEvent.type(screen.getByLabelText(/container name/i), 'valid-container');

      expect(screen.getByRole('button', { name: /Continue/i })).toBeDisabled();
    });

    it('shouldBlockAdvanceWhenAzureContainerNameIsTooShort', async () => {
      await goToAzureBasicConfigStep();

      await userEvent.type(screen.getByLabelText(/^name$/i), 'my-azure-store');
      await userEvent.type(screen.getByLabelText(/account name/i), 'validaccount');
      await userEvent.type(screen.getByLabelText(/container name/i), 'ab');

      expect(screen.getByRole('button', { name: /Continue/i })).toBeDisabled();
    });

    it('shouldAllowAdvanceWhenAzureAccountAndContainerNamesMeetLengthRules', async () => {
      await goToAzureBasicConfigStep();

      await userEvent.type(screen.getByLabelText(/^name$/i), 'my-azure-store');
      await userEvent.type(screen.getByLabelText(/account name/i), 'validaccount');
      await userEvent.type(screen.getByLabelText(/container name/i), 'valid-container');

      expect(screen.getByRole('button', { name: /Continue/i })).toBeEnabled();
    });
  });

  describe('breadcrumb navigation', () => {
    it('renders breadcrumbs for create view', () => {
      render(<BlobStoreWizardCreate onBack={jest.fn()} />);

      expect(screen.getByRole('button', { name: 'Settings' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Blob Stores' })).toBeInTheDocument();
      expect(screen.getByText('Create')).toBeInTheDocument();
    });

    it('clicking Settings breadcrumb navigates to settings page', () => {
      render(<BlobStoreWizardCreate onBack={jest.fn()} />);

      screen.getByRole('button', { name: 'Settings' }).click();
      expect(window.location.hash).toBe('#preview/admin/settings');
    });

    it('clicking Blob Stores breadcrumb calls onBack', async () => {
      const mockOnBack = jest.fn();
      render(<BlobStoreWizardCreate onBack={mockOnBack} />);

      screen.getByRole('button', { name: 'Blob Stores' }).click();
      expect(mockOnBack).toHaveBeenCalled();
    });
  });

  describe('Global Distribution step visibility', () => {
    it('excludes Global Distribution step before a type is selected', () => {
      render(<BlobStoreWizardCreate onBack={jest.fn()} />);

      const steps = screen.getByTestId('blob-store-wizard-steps');
      expect(steps).toHaveTextContent('Select Type');
      expect(steps).toHaveTextContent('Basic Config');
      expect(steps).toHaveTextContent('Credentials');
      expect(steps).toHaveTextContent('Advanced');
      expect(steps).not.toHaveTextContent('Global Distribution');
    });

    it('includes Global Distribution step when the S3 type is selected', async () => {
      render(<BlobStoreWizardCreate onBack={jest.fn()} />);

      await userEvent.click(screen.getByText('S3'));

      await waitFor(() => {
        expect(screen.getByTestId('blob-store-wizard-steps')).toHaveTextContent('Global Distribution');
      });

      const steps = screen.getByTestId('blob-store-wizard-steps');
      expect(steps).toHaveTextContent('Select Type');
      expect(steps).toHaveTextContent('Basic Config');
      expect(steps).toHaveTextContent('Credentials');
      expect(steps).toHaveTextContent('Advanced');
    });

    it('excludes Global Distribution step for non-S3 types', async () => {
      render(<BlobStoreWizardCreate onBack={jest.fn()} />);

      // Select File blob store type - should NOT show Global Distribution step
      await userEvent.click(screen.getByText('File'));

      const steps = screen.getByTestId('blob-store-wizard-steps');
      expect(steps).toHaveTextContent('Select Type');
      expect(steps).toHaveTextContent('Basic Config');
      expect(steps).not.toHaveTextContent('Global Distribution');
      expect(steps).toHaveTextContent('Credentials');
      expect(steps).toHaveTextContent('Advanced');
    });
  });
});
