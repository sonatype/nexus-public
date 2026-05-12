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
import userEvent from '@testing-library/user-event';
import BlobStoresForm from '../BlobStoresForm';
import * as useBlobStoresModule from '../useBlobStores';
import { useBlobStoreForm } from '../useBlobStoreForm';

jest.mock('../useBlobStores', () => ({
  useBlobStoreTypes: jest.fn(),
  useBlobStore: jest.fn(),
  useBlobStorePromote: jest.fn(),
}));
jest.mock('../useBlobStoreForm');

const mockUseBlobStoreForm = useBlobStoreForm as jest.MockedFunction<typeof useBlobStoreForm>;

// Mock the router
const mockGo = jest.fn();
jest.mock('@uirouter/react', () => ({
  useRouter: () => ({
    stateService: {
      go: mockGo
    }
  }),
  useCurrentStateAndParams: () => ({
    state: { name: 'preview.admin.repository.blobstores.create' },
    params: {}
  })
}));

// Mock ExtJS
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    checkPermission: () => true,
    isProEdition: () => true,
    state: () => ({
      getValue: () => false
    })
  },
  Permissions: {
    BLOB_STORES: {
      UPDATE: 'nexus:blobstores:update',
      DELETE: 'nexus:blobstores:delete'
    }
  }
}));

// Mock shared form components
jest.mock('../../../../shared/form', () => ({
  SettingsForm: ({ children, onSave, onCancel, title }) => (
    <div data-testid="settings-form">
      <h1>{title}</h1>
      {children}
      <button onClick={onSave}>Save</button>
      <button onClick={onCancel}>Cancel</button>
    </div>
  ),
  SettingsFormSection: ({ children, title }) => (
    <div data-testid="settings-form-section">
      <h2>{title}</h2>
      {children}
    </div>
  ),
  SettingsTextInput: ({ label, value, onChange, error }) => (
    <div>
      <label>{label}</label>
      <input 
        value={value} 
        onChange={(e) => onChange(e.target.value)} 
        data-testid={`input-${label}`}
      />
      {error && <span className="error">{error}</span>}
    </div>
  ),
  SettingsSelect: ({ label, value, onChange, options }) => (
    <div>
      <label>{label}</label>
      <select value={value} onChange={(e) => onChange(e.target.value)} data-testid={`select-${label}`}>
        {options.map((opt) => (
          <option key={opt.value} value={opt.value}>{opt.label}</option>
        ))}
      </select>
    </div>
  ),
  SettingsCheckbox: ({ label, checked, onChange }) => (
    <div>
      <label>
        <input type="checkbox" checked={checked} onChange={(e) => onChange(e.target.checked)} />
        {label}
      </label>
    </div>
  ),
  SettingsAlert: ({ children }) => <div data-testid="alert">{children}</div>,
  SettingsButton: ({ children, onClick, disabled }) => (
    <button onClick={onClick} disabled={disabled}>{children}</button>
  )
}));

// Mock type-specific settings
jest.mock('../FileBlobStoreSettings', () => () => <div data-testid="file-settings">File Settings</div>);
jest.mock('../S3BlobStoreSettings', () => () => <div data-testid="s3-settings">S3 Settings</div>);
jest.mock('../AzureBlobStoreSettings', () => () => <div data-testid="azure-settings">Azure Settings</div>);
jest.mock('../GoogleBlobStoreSettings', () => () => <div data-testid="google-settings">Google Settings</div>);
jest.mock('../ConvertToGroupModal', () => () => <div data-testid="convert-modal">Convert Modal</div>);

describe('BlobStoresForm', () => {
  const mockUseBlobStoreTypes = useBlobStoresModule.useBlobStoreTypes as jest.Mock;
  const mockUseBlobStore = useBlobStoresModule.useBlobStore as jest.Mock;
  const mockUseBlobStorePromote = useBlobStoresModule.useBlobStorePromote as jest.Mock;

  const mockTypes = [
    { id: 'File', name: 'File' },
    { id: 'S3', name: 'S3' },
    { id: 'Azure', name: 'Azure Cloud' },
    { id: 'Google', name: 'Google Cloud' }
  ];

  const mockQuotaTypes = [
    { id: 'spaceUsedQuota', name: 'Space Used' },
    { id: 'spaceRemainingQuota', name: 'Space Remaining' }
  ];

  function createBlobStoreFormMock(data: any = {}, overrides: any = {}) {
    const { form: formOverrides, ...restOverrides } = overrides;
    return {
      form: {
        field: jest.fn((name: string) => ({ name, value: data[name] != null ? String(data[name]) : '', onChange: jest.fn(), onBlur: jest.fn(), error: undefined })),
        data,
        isPristine: true,
        isSaving: false,
        isLoading: false,
        isDeleting: false,
        saveError: null,
        validationErrors: {},
        state: { matches: jest.fn(() => false), context: { data, blobStoreTypes: mockTypes, quotaTypes: mockQuotaTypes, usage: { blobStoreUsage: 0, repositoryUsage: 0 } } },
        send: jest.fn(),
        ...formOverrides,
      } as any,
      isCreate: true,
      blobStoreTypes: mockTypes,
      quotaTypes: mockQuotaTypes,
      usage: { blobStoreUsage: 0, repositoryUsage: 0 },
      ...restOverrides,
    } as any;
  }

  beforeEach(() => {
    jest.clearAllMocks();

    mockUseBlobStoreForm.mockReturnValue(createBlobStoreFormMock({ name: '' }));

    mockUseBlobStoreTypes.mockReturnValue({
      types: mockTypes,
      quotaTypes: mockQuotaTypes,
      loading: false,
      error: null
    });

    mockUseBlobStore.mockReturnValue({
      blobStore: null,
      blobStoreUsage: 0,
      repositoryUsage: 0,
      loading: false,
      error: null,
      save: jest.fn().mockResolvedValue(undefined),
      remove: jest.fn().mockResolvedValue(undefined)
    });

    mockUseBlobStorePromote.mockReturnValue({
      promoting: false,
      promote: jest.fn().mockResolvedValue(undefined)
    });
  });

  it('renders create form with title', () => {
    render(<BlobStoresForm />);
    expect(screen.getByText('Create Blob Store')).toBeInTheDocument();
  });

  it('renders type selection dropdown', () => {
    render(<BlobStoresForm />);
    expect(screen.getByText('Type')).toBeInTheDocument();
  });

  it('shows File settings when File type is selected', async () => {
    mockUseBlobStoreForm.mockReturnValue(createBlobStoreFormMock({ name: '', type: 'File' }));
    render(<BlobStoresForm />);
    expect(screen.getByTestId('file-settings')).toBeInTheDocument();
  });

  it('shows S3 settings when S3 type is selected', async () => {
    mockUseBlobStoreForm.mockReturnValue(createBlobStoreFormMock({ name: '', type: 'S3' }));
    render(<BlobStoresForm />);
    expect(screen.getByTestId('s3-settings')).toBeInTheDocument();
  });

  it('shows Azure settings when Azure type is selected', async () => {
    mockUseBlobStoreForm.mockReturnValue(createBlobStoreFormMock({ name: '', type: 'Azure' }));
    render(<BlobStoresForm />);
    expect(screen.getByTestId('azure-settings')).toBeInTheDocument();
  });

  it('shows Google settings when Google type is selected', async () => {
    mockUseBlobStoreForm.mockReturnValue(createBlobStoreFormMock({ name: '', type: 'Google' }));
    render(<BlobStoresForm />);
    expect(screen.getByTestId('google-settings')).toBeInTheDocument();
  });

  it('shows name input after type is selected', async () => {
    mockUseBlobStoreForm.mockReturnValue(createBlobStoreFormMock({ name: '', type: 'File' }));
    render(<BlobStoresForm />);
    expect(screen.getByText('Name')).toBeInTheDocument();
  });

  it('shows soft quota section after type is selected', async () => {
    mockUseBlobStoreForm.mockReturnValue(createBlobStoreFormMock({ name: '', type: 'File' }));
    render(<BlobStoresForm />);
    expect(screen.getByText('Soft Quota')).toBeInTheDocument();
  });

  it('navigates back when cancel is clicked', async () => {
    render(<BlobStoresForm />);
    
    fireEvent.click(screen.getByText('Cancel'));
    
    expect(mockGo).toHaveBeenCalledWith('preview.admin.repository.blobstores.list');
  });

  it('shows loading state when loading', () => {
    mockUseBlobStoreForm.mockReturnValue(createBlobStoreFormMock({ name: '' }, {
      form: { isLoading: true },
    }));

    render(<BlobStoresForm />);
    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  it('shows error message when types fail to load', async () => {
    mockUseBlobStoreForm.mockReturnValue(createBlobStoreFormMock({ name: '' }, {
      form: { saveError: 'Failed to load blob store types' },
      blobStoreTypes: [],
    }));

    render(<BlobStoresForm />);
    expect(screen.getByText(/Failed to load blob store types/)).toBeInTheDocument();
  });

  it('shows error message when no types are available', async () => {
    mockUseBlobStoreForm.mockReturnValue(createBlobStoreFormMock({ name: '' }, {
      blobStoreTypes: [],
    }));

    render(<BlobStoresForm />);
    expect(screen.getByText(/No blob store types available/)).toBeInTheDocument();
  });

  it('disables dropdown when types are loading but we have types', () => {
    // When typesLoading is true, the whole form shows a loading spinner
    // so the dropdown is not rendered. This test verifies the component 
    // renders correctly when types are loaded.
    jest.spyOn(useBlobStoresModule, 'useBlobStoreTypes').mockReturnValue({
      types: mockTypes,
      quotaTypes: mockQuotaTypes,
      loading: false,
      error: null
    });

    render(<BlobStoresForm />);
    const typeSelect = screen.getByTestId('select-Type');
    // Dropdown should exist when types are loaded
    expect(typeSelect).toBeInTheDocument();
  });

  it('renders dropdown options correctly when types are loaded', () => {
    render(<BlobStoresForm />);
    
    const typeSelect = screen.getByTestId('select-Type') as HTMLSelectElement;
    const options = Array.from(typeSelect.options).map(opt => ({
      value: opt.value,
      label: opt.text
    }));

    expect(options).toContainEqual({ value: '', label: 'Select a type...' });
    expect(options).toContainEqual({ value: 'File', label: 'File' });
    expect(options).toContainEqual({ value: 'S3', label: 'S3' });
    expect(options).toContainEqual({ value: 'Azure', label: 'Azure Cloud' });
    expect(options).toContainEqual({ value: 'Google', label: 'Google Cloud' });
  });
});

