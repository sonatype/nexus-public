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
import { render, screen, fireEvent, waitFor, act, within } from '@testing-library/react';
import BlobStoresPage from '../BlobStoresPage';
import * as useBlobStoresModule from '../useBlobStores';

// ---------------------------------------------------------------------------
// Module mocks
// ---------------------------------------------------------------------------

jest.mock('../useBlobStores', () => ({
  useBlobStoresList: jest.fn(),
  useBlobStoreTypes: jest.fn(),
  useBlobStore: jest.fn(),
  useBlobStorePromote: jest.fn(),
}));

jest.mock('../FileBlobStoreSettings', () => () => (
  <div data-testid="file-settings">File Settings</div>
));
jest.mock('../S3BlobStoreSettings', () => () => (
  <div data-testid="s3-settings">S3 Settings</div>
));
jest.mock('../AzureBlobStoreSettings', () => () => (
  <div data-testid="azure-settings">Azure Settings</div>
));
jest.mock('../GoogleBlobStoreSettings', () => () => (
  <div data-testid="google-settings">Google Settings</div>
));
jest.mock('../GroupBlobStoreSettings', () => () => (
  <div data-testid="group-settings">Group Settings</div>
));
jest.mock('../ConvertToGroupModal', () => () => (
  <div data-testid="convert-modal">Convert Modal</div>
));
jest.mock('../BlobStoreWizardCreate', () => ({
  BlobStoreWizardCreate: () => <div data-testid="create-wizard">Create Wizard</div>,
}));

jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    checkPermission: () => true,
    isProEdition: () => true,
    usePermission: (_fn: () => boolean) => true,
    useUser: () => true,
    state: () => ({ getValue: () => false }),
  },
}));

jest.mock('../../../../../../../interface/HumanReadableUtils', () => ({
  HumanReadableUtils: { bytesToString: (n: number) => `${n}B` },
}));

jest.mock('../../../../../../../constants/Permissions', () => ({
  Permissions: {
    BLOB_STORES: { CREATE: 'create', UPDATE: 'update', DELETE: 'delete' },
  },
}));

// Shared form components — minimal stubs that forward key props for testing
jest.mock('../../../../../shared/form', () => ({
  SettingsForm: ({ children, onSave, onCancel, dirty }: any) => (
    <div data-testid="settings-form" data-dirty={String(dirty)}>
      <button
        data-testid="form-submit"
        onClick={onSave}
        disabled={!dirty}
      >
        Save
      </button>
      <button data-testid="form-cancel" onClick={onCancel}>Cancel</button>
      {children}
    </div>
  ),
  SettingsFormSection: ({ children, title }: any) => (
    <div data-testid="settings-form-section">
      <h2>{title}</h2>
      {children}
    </div>
  ),
  SettingsTextInput: ({ label, value, onChange, error, name }: any) => (
    <div>
      <label htmlFor={name}>{label}</label>
      <input
        id={name}
        data-testid={`input-${name}`}
        value={value ?? ''}
        onChange={(e) => onChange(e.target.value)}
      />
      {error && <span data-testid={`error-${name}`}>{error}</span>}
    </div>
  ),
  SettingsSelect: ({ label, value, onChange, options, name, error }: any) => (
    <div>
      <label htmlFor={name}>{label}</label>
      <select
        id={name}
        data-testid={`select-${name}`}
        value={value ?? ''}
        onChange={(e) => onChange(e.target.value)}
      >
        {(options || []).map((opt: any) => (
          <option key={opt.value} value={opt.value}>{opt.label}</option>
        ))}
      </select>
      {error && <span data-testid={`error-${name}`}>{error}</span>}
    </div>
  ),
  SettingsCheckbox: ({ label, checked, onChange, name }: any) => (
    <label>
      <input
        type="checkbox"
        data-testid={`checkbox-${name}`}
        checked={!!checked}
        onChange={(e) => onChange(e.target.checked)}
      />
      {label}
    </label>
  ),
  SettingsAlert: ({ children }: any) => <div data-testid="alert">{children}</div>,
  SettingsButton: ({ children, onClick, disabled }: any) => (
    <button onClick={onClick} disabled={disabled}>{children}</button>
  ),
}));

jest.mock('../../../../../shared', () => ({
  PageHeader: ({ children }: any) => <div data-testid="page-header">{children}</div>,
  EntityTable: ({ data, onRowClick }: any) => (
    <div data-testid="entity-table">
      {(data || []).map((row: any) => (
        <div
          key={row.name}
          data-testid={`row-${row.name}`}
          onClick={() => onRowClick(row)}
          role="button"
        >
          {row.name}
        </div>
      ))}
    </div>
  ),
  EmptyState: () => <div data-testid="empty-state" />,
  StatusBadge: () => <span />,
  HelpSection: () => <div data-testid="help-section" />,
  useToast: () => ({ success: jest.fn(), error: jest.fn() }),
  clearDirtyState: jest.fn(),
}));

jest.mock('../../../../../shared', () => ({
  ...jest.requireActual('../../../../../shared'),
  PageHeader: ({ children }: any) => <div data-testid="page-header">{children}</div>,
  EntityTable: ({ data, onRowClick }: any) => (
    <div data-testid="entity-table">
      {(data || []).map((row: any) => (
        <div
          key={row.name}
          data-testid={`row-${row.name}`}
          onClick={() => onRowClick(row)}
          role="button"
        >
          {row.name}
        </div>
      ))}
    </div>
  ),
  EmptyState: () => <div data-testid="empty-state" />,
  StatusBadge: () => <span />,
  HelpSection: () => <div data-testid="help-section" />,
  useToast: () => ({ success: jest.fn(), error: jest.fn() }),
  clearDirtyState: jest.fn(),
}));

// ---------------------------------------------------------------------------
// Test helpers
// ---------------------------------------------------------------------------

const MOCK_QUOTA_TYPES = [
  { id: 'spaceUsedQuota', name: 'Space Used' },
  { id: 'spaceRemainingQuota', name: 'Space Remaining' },
];

const MOCK_TYPES = [
  { id: 'file', name: 'File' },
  { id: 's3', name: 'S3' },
  { id: 'azure', name: 'Azure Cloud Storage' },
  { id: 'google', name: 'Google Cloud Storage' },
];

/** Build the default return value for useBlobStore */
function makeBlobStoreHook(overrides: Partial<ReturnType<typeof useBlobStoresModule.useBlobStore>> = {}) {
  const save = jest.fn().mockResolvedValue(undefined);
  const remove = jest.fn().mockResolvedValue(undefined);
  return {
    blobStore: null as any,
    blobStoreUsage: 0,
    repositoryUsage: 0,
    loading: false,
    error: null,
    save,
    remove,
    ...overrides,
  };
}

/** Point the URL hash to the edit view for a given type/name. */
function setEditHash(type: string, name: string) {
  window.location.hash = `#preview/admin/repository/blobstores/${type}/${name}`;
}

/** Point the URL hash to the list view. */
function setListHash() {
  window.location.hash = '#preview/admin/repository/blobstores';
}

const mockUseBlobStore = useBlobStoresModule.useBlobStore as jest.Mock;
const mockUseBlobStoresList = useBlobStoresModule.useBlobStoresList as jest.Mock;
const mockUseBlobStoreTypes = useBlobStoresModule.useBlobStoreTypes as jest.Mock;
const mockUseBlobStorePromote = useBlobStoresModule.useBlobStorePromote as jest.Mock;

beforeEach(() => {
  jest.clearAllMocks();

  mockUseBlobStoresList.mockReturnValue({
    blobStores: [],
    loading: false,
    error: null,
    refetch: jest.fn(),
  });

  mockUseBlobStoreTypes.mockReturnValue({
    types: MOCK_TYPES,
    quotaTypes: MOCK_QUOTA_TYPES,
    loading: false,
    error: null,
  });

  mockUseBlobStorePromote.mockReturnValue({ promoting: false, promote: jest.fn() });

  // Default: list view
  setListHash();
});

// ---------------------------------------------------------------------------
// REGRESSION: soft quota checkbox checked when blob store has quota
// ---------------------------------------------------------------------------

describe('soft quota display on edit', () => {
  // -----------------------------------------------------------------------
  // Bug: useBlobStore returned raw API data (no "enabled" field, bytes limit).
  // The checkbox was bound to softQuota.enabled which was always undefined →
  // always unchecked, even when the backend had quota configured.
  // Fix: useBlobStore now transforms the response (enabled=true, limit in MB).
  // -----------------------------------------------------------------------

  it('shows the soft quota checkbox as checked when the loaded blob store has softQuota', async () => {
    setEditHash('file', 'my-store');

    // Simulate what useBlobStore returns AFTER our fix (already transformed)
    mockUseBlobStore.mockReturnValue(
      makeBlobStoreHook({
        blobStore: {
          name: 'my-store',
          type: 'file',
          path: '/data',
          softQuota: { enabled: true, type: 'spaceUsedQuota', limit: 100 },
        },
      })
    );

    render(<BlobStoresPage />);

    const checkbox = await screen.findByTestId('checkbox-blobstore-softquota-enabled');
    expect(checkbox).toBeChecked();
  });

  it('shows the soft quota checkbox as unchecked when the loaded blob store has no softQuota', async () => {
    setEditHash('file', 'plain-store');

    mockUseBlobStore.mockReturnValue(
      makeBlobStoreHook({
        blobStore: { name: 'plain-store', type: 'file', path: '/data' },
      })
    );

    render(<BlobStoresPage />);

    const checkbox = await screen.findByTestId('checkbox-blobstore-softquota-enabled');
    expect(checkbox).not.toBeChecked();
  });

  it('displays the soft quota limit in MB (not bytes) in the limit input', async () => {
    setEditHash('file', 'my-store');

    // useBlobStore returns 100 (MB) after the bytes→MB conversion fix
    mockUseBlobStore.mockReturnValue(
      makeBlobStoreHook({
        blobStore: {
          name: 'my-store',
          type: 'file',
          path: '/data',
          softQuota: { enabled: true, type: 'spaceUsedQuota', limit: 100 },
        },
      })
    );

    render(<BlobStoresPage />);

    const limitInput = await screen.findByTestId('input-blobstore-softquota-limit');
    // Must be "100" (MB), never "104857600" (raw bytes)
    expect(limitInput).toHaveValue('100');
  });

  it('shows the soft quota section for all blob store types', async () => {
    for (const type of ['file', 's3', 'azure', 'google']) {
      setEditHash(type, 'test-store');

      mockUseBlobStore.mockReturnValue(
        makeBlobStoreHook({
          blobStore: { name: 'test-store', type },
        })
      );

      const { unmount } = render(<BlobStoresPage />);

      // Soft quota section heading must be visible
      expect(await screen.findByText('Soft Quota')).toBeInTheDocument();

      unmount();
    }
  });
});

// ---------------------------------------------------------------------------
// REGRESSION: form is not dirty on initial load
// ---------------------------------------------------------------------------

describe('dirty state on edit load', () => {
  // -----------------------------------------------------------------------
  // Bug: baseData was set from raw API shape while formData got the
  // transformed shape (enabled + MB). The JSON comparison differed
  // immediately, making the form appear dirty before any user interaction.
  // Fix: baseData is now set from the same normalized object as formData.
  // -----------------------------------------------------------------------

  it('form is NOT dirty immediately after loading an existing blob store', async () => {
    setEditHash('file', 'my-store');

    mockUseBlobStore.mockReturnValue(
      makeBlobStoreHook({
        blobStore: {
          name: 'my-store',
          type: 'file',
          path: '/data',
          softQuota: { enabled: true, type: 'spaceUsedQuota', limit: 100 },
        },
      })
    );

    render(<BlobStoresPage />);

    const form = await screen.findByTestId('settings-form');
    // data-dirty is set from the isDirty computed value passed as dirty={isDirty}
    expect(form).toHaveAttribute('data-dirty', 'false');
  });

  it('form IS dirty after the user changes the soft quota limit', async () => {
    setEditHash('file', 'my-store');

    mockUseBlobStore.mockReturnValue(
      makeBlobStoreHook({
        blobStore: {
          name: 'my-store',
          type: 'file',
          path: '/data',
          softQuota: { enabled: true, type: 'spaceUsedQuota', limit: 100 },
        },
      })
    );

    render(<BlobStoresPage />);

    const limitInput = await screen.findByTestId('input-blobstore-softquota-limit');
    fireEvent.change(limitInput, { target: { value: '200' } });

    const form = screen.getByTestId('settings-form');
    expect(form).toHaveAttribute('data-dirty', 'true');
  });

  it('form IS dirty after the user enables soft quota on a store with no quota', async () => {
    setEditHash('file', 'plain-store');

    mockUseBlobStore.mockReturnValue(
      makeBlobStoreHook({
        blobStore: { name: 'plain-store', type: 'file', path: '/data' },
      })
    );

    render(<BlobStoresPage />);

    const checkbox = await screen.findByTestId('checkbox-blobstore-softquota-enabled');
    fireEvent.click(checkbox);

    const form = screen.getByTestId('settings-form');
    expect(form).toHaveAttribute('data-dirty', 'true');
  });

  // -----------------------------------------------------------------------
  // Bug: REST API does not return `type` in the response body (it is implied
  // by the URL path). If formData had no `type` but baseData did, the dirty
  // comparison would always differ → false "Unsaved changes" on every load.
  // Fix: normalize() injects type into formData from routeState.
  // -----------------------------------------------------------------------
  it('form is NOT dirty when the API body omits the type field (type comes from URL)', async () => {
    setEditHash('file', 'my-store');

    mockUseBlobStore.mockReturnValue(
      makeBlobStoreHook({
        // Simulate real API response: no `type` field
        blobStore: { name: 'my-store', path: '/data' } as any,
      })
    );

    render(<BlobStoresPage />);

    const form = await screen.findByTestId('settings-form');
    expect(form).toHaveAttribute('data-dirty', 'false');
  });

  // -----------------------------------------------------------------------
  // Bug: REST API does not return `name` in the response body (it is implied
  // by the URL path param). formData.name was undefined → validate() failed
  // with "Name is required" → save was silently blocked.
  // Fix: normalize() injects name into formData from routeState.blobStoreName.
  // -----------------------------------------------------------------------
  it('form is NOT dirty when the API body omits the name field (name comes from URL)', async () => {
    setEditHash('file', 'url-name-store');

    mockUseBlobStore.mockReturnValue(
      makeBlobStoreHook({
        // Simulate real API response: no `name` field
        blobStore: { path: '/data', type: 'file' } as any,
      })
    );

    render(<BlobStoresPage />);

    const form = await screen.findByTestId('settings-form');
    expect(form).toHaveAttribute('data-dirty', 'false');
  });
});

// ---------------------------------------------------------------------------
// REGRESSION: save sends correct PUT request with MB→bytes conversion
// ---------------------------------------------------------------------------

describe('save flow for soft quota', () => {
  // -----------------------------------------------------------------------
  // Bug: the SettingsForm only fires onSave when dirty=true.  Before the
  // fix the form was always pristine because useBlobStore returned raw data
  // and the enabled flag was never set → checkbox unchecked → no changes
  // detected → Save button disabled → no HTTP request was made.
  // -----------------------------------------------------------------------

  it('calls save with softQuota when user modifies the limit and submits', async () => {
    setEditHash('file', 'my-store');

    const save = jest.fn().mockResolvedValue(undefined);
    mockUseBlobStore.mockReturnValue(
      makeBlobStoreHook({
        blobStore: {
          name: 'my-store',
          type: 'file',
          path: '/data',
          softQuota: { enabled: true, type: 'spaceUsedQuota', limit: 100 },
        },
        save,
      })
    );

    render(<BlobStoresPage />);

    // Change the limit
    const limitInput = await screen.findByTestId('input-blobstore-softquota-limit');
    fireEvent.change(limitInput, { target: { value: '200' } });

    // Submit
    const submitBtn = screen.getByTestId('form-submit');
    expect(submitBtn).not.toBeDisabled();
    await act(async () => { fireEvent.click(submitBtn); });

    expect(save).toHaveBeenCalledTimes(1);
    expect(save).toHaveBeenCalledWith(
      expect.objectContaining({
        softQuota: expect.objectContaining({
          enabled: true,
          type: 'spaceUsedQuota',
          limit: 200,
        }),
      })
    );
  });

  it('calls save when user enables soft quota on a store with no quota', async () => {
    setEditHash('file', 'plain-store');

    const save = jest.fn().mockResolvedValue(undefined);
    mockUseBlobStore.mockReturnValue(
      makeBlobStoreHook({
        blobStore: { name: 'plain-store', type: 'file', path: '/data' },
        save,
      })
    );

    render(<BlobStoresPage />);

    // Enable quota
    const checkbox = await screen.findByTestId('checkbox-blobstore-softquota-enabled');
    fireEvent.click(checkbox);

    // Fill in required fields (type pre-filled to spaceUsedQuota, need limit)
    const limitInput = screen.getByTestId('input-blobstore-softquota-limit');
    fireEvent.change(limitInput, { target: { value: '500' } });

    const submitBtn = screen.getByTestId('form-submit');
    expect(submitBtn).not.toBeDisabled();
    await act(async () => { fireEvent.click(submitBtn); });

    expect(save).toHaveBeenCalledTimes(1);
    expect(save).toHaveBeenCalledWith(
      expect.objectContaining({
        softQuota: expect.objectContaining({ enabled: true, limit: 500 }),
      })
    );
  });

  it('calls save for S3 blob store when quota is modified', async () => {
    setEditHash('s3', 's3-store');

    const save = jest.fn().mockResolvedValue(undefined);
    mockUseBlobStore.mockReturnValue(
      makeBlobStoreHook({
        blobStore: {
          name: 's3-store',
          type: 's3',
          bucketConfiguration: {
            bucket: { region: 'us-east-1', name: 'my-bucket', prefix: '' },
          },
          softQuota: { enabled: true, type: 'spaceUsedQuota', limit: 50 },
        },
        save,
      })
    );

    render(<BlobStoresPage />);

    const limitInput = await screen.findByTestId('input-blobstore-softquota-limit');
    fireEvent.change(limitInput, { target: { value: '75' } });

    await act(async () => {
      fireEvent.click(screen.getByTestId('form-submit'));
    });

    expect(save).toHaveBeenCalledTimes(1);
    expect(save).toHaveBeenCalledWith(
      expect.objectContaining({
        softQuota: expect.objectContaining({ limit: 75 }),
      })
    );
  });

  it('calls save for Azure blob store when quota is modified', async () => {
    setEditHash('azure', 'az-store');

    const save = jest.fn().mockResolvedValue(undefined);
    mockUseBlobStore.mockReturnValue(
      makeBlobStoreHook({
        blobStore: {
          name: 'az-store',
          type: 'azure',
          bucketConfiguration: {
            accountName: 'myaccount',
            containerName: 'mycontainer',
            authentication: { authenticationMethod: 'ENVIRONMENTVARIABLE' },
          },
          softQuota: { enabled: true, type: 'spaceRemainingQuota', limit: 200 },
        },
        save,
      })
    );

    render(<BlobStoresPage />);

    const limitInput = await screen.findByTestId('input-blobstore-softquota-limit');
    fireEvent.change(limitInput, { target: { value: '300' } });

    await act(async () => {
      fireEvent.click(screen.getByTestId('form-submit'));
    });

    expect(save).toHaveBeenCalledTimes(1);
    expect(save).toHaveBeenCalledWith(
      expect.objectContaining({
        softQuota: expect.objectContaining({ limit: 300 }),
      })
    );
  });

  it('calls save for Google Cloud blob store when quota is modified', async () => {
    setEditHash('google', 'gcs-store');

    const save = jest.fn().mockResolvedValue(undefined);
    mockUseBlobStore.mockReturnValue(
      makeBlobStoreHook({
        blobStore: {
          name: 'gcs-store',
          type: 'google',
          bucketConfiguration: {
            bucket: { name: 'my-gcs-bucket', prefix: '' },
          },
          softQuota: { enabled: true, type: 'spaceUsedQuota', limit: 128 },
        },
        save,
      })
    );

    render(<BlobStoresPage />);

    const limitInput = await screen.findByTestId('input-blobstore-softquota-limit');
    fireEvent.change(limitInput, { target: { value: '256' } });

    await act(async () => {
      fireEvent.click(screen.getByTestId('form-submit'));
    });

    expect(save).toHaveBeenCalledTimes(1);
    expect(save).toHaveBeenCalledWith(
      expect.objectContaining({
        softQuota: expect.objectContaining({ limit: 256 }),
      })
    );
  });

  it('does NOT call save when Save button is disabled (form is pristine)', async () => {
    setEditHash('file', 'pristine-store');

    const save = jest.fn();
    mockUseBlobStore.mockReturnValue(
      makeBlobStoreHook({
        blobStore: { name: 'pristine-store', type: 'file', path: '/data' },
        save,
      })
    );

    render(<BlobStoresPage />);

    await screen.findByTestId('settings-form');

    const submitBtn = screen.getByTestId('form-submit');
    // Form is pristine → button is disabled
    expect(submitBtn).toBeDisabled();

    fireEvent.click(submitBtn);
    expect(save).not.toHaveBeenCalled();
  });
});

// ---------------------------------------------------------------------------
// REGRESSION: soft quota validation
// ---------------------------------------------------------------------------

describe('soft quota validation', () => {
  // -----------------------------------------------------------------------
  // Bug: BlobStoresPage.validate() had no soft quota checks.
  // A user could enable quota without filling in type/limit; save would
  // silently strip softQuota from the payload (formatBlobStoreData condition
  // requires both type and limit > 0 to include it).
  // Fix: validate() now checks softQuota.type and softQuota.limit when enabled.
  // -----------------------------------------------------------------------

  it('blocks save and shows error when quota is enabled but limit is empty', async () => {
    setEditHash('file', 'my-store');

    const save = jest.fn();
    mockUseBlobStore.mockReturnValue(
      makeBlobStoreHook({
        blobStore: { name: 'my-store', type: 'file', path: '/data' },
        save,
      })
    );

    render(<BlobStoresPage />);

    // Enable quota (auto-sets type, but limit stays empty)
    const checkbox = await screen.findByTestId('checkbox-blobstore-softquota-enabled');
    fireEvent.click(checkbox);

    // Force save attempt via the handler (not the disabled button path)
    const form = screen.getByTestId('settings-form');
    // Simulate clicking the save button while dirty but with invalid quota
    const submitBtn = screen.getByTestId('form-submit');
    await act(async () => { fireEvent.click(submitBtn); });

    // Save must not have been called
    expect(save).not.toHaveBeenCalled();

    // Error message must be visible
    expect(screen.getByTestId('error-blobstore-softquota-limit')).toBeInTheDocument();
  });

  it('blocks save and shows error when quota is enabled but type is cleared', async () => {
    setEditHash('file', 'my-store');

    const save = jest.fn();
    mockUseBlobStore.mockReturnValue(
      makeBlobStoreHook({
        blobStore: {
          name: 'my-store',
          type: 'file',
          path: '/data',
          softQuota: { enabled: true, type: 'spaceUsedQuota', limit: 100 },
        },
        save,
      })
    );

    render(<BlobStoresPage />);

    // Clear the quota type
    const typeSelect = await screen.findByTestId('select-blobstore-softquota-type');
    fireEvent.change(typeSelect, { target: { value: '' } });

    const submitBtn = screen.getByTestId('form-submit');
    await act(async () => { fireEvent.click(submitBtn); });

    expect(save).not.toHaveBeenCalled();
    expect(screen.getByTestId('error-blobstore-softquota-type')).toBeInTheDocument();
  });

  it('allows save when quota is enabled with valid type and limit', async () => {
    setEditHash('file', 'my-store');

    const save = jest.fn().mockResolvedValue(undefined);
    mockUseBlobStore.mockReturnValue(
      makeBlobStoreHook({
        blobStore: { name: 'my-store', type: 'file', path: '/data' },
        save,
      })
    );

    render(<BlobStoresPage />);

    const checkbox = await screen.findByTestId('checkbox-blobstore-softquota-enabled');
    fireEvent.click(checkbox); // enables; type auto-set to spaceUsedQuota

    const limitInput = screen.getByTestId('input-blobstore-softquota-limit');
    fireEvent.change(limitInput, { target: { value: '100' } });

    const submitBtn = screen.getByTestId('form-submit');
    await act(async () => { fireEvent.click(submitBtn); });

    expect(save).toHaveBeenCalledTimes(1);
  });
});

// ---------------------------------------------------------------------------
// REGRESSION: selectedType always set on edit
// ---------------------------------------------------------------------------

describe('selectedType initialization on edit', () => {
  // -----------------------------------------------------------------------
  // Bug: if the blob store API body omitted the type field, selectedType
  // stayed as empty string.  The soft quota section gate is
  // `{selectedType && ...}` — so nothing rendered.
  // Fix: selectedType falls back to routeState.blobStoreType (from the URL).
  // -----------------------------------------------------------------------

  it('renders the soft quota section when type comes from the URL (not the API body)', async () => {
    setEditHash('file', 'no-type-in-body');

    // API body omits the type field — type must come from the URL
    mockUseBlobStore.mockReturnValue(
      makeBlobStoreHook({
        blobStore: { name: 'no-type-in-body', path: '/data' }, // no type field
      })
    );

    render(<BlobStoresPage />);

    // Soft quota section must render despite missing type in API body
    expect(await screen.findByText('Soft Quota')).toBeInTheDocument();
  });

  it('renders the correct type-specific settings component for each URL type', async () => {
    const cases = [
      { urlType: 'file', testId: 'file-settings' },
      { urlType: 's3', testId: 's3-settings' },
      { urlType: 'azure', testId: 'azure-settings' },
      { urlType: 'google', testId: 'google-settings' },
    ];

    for (const { urlType, testId } of cases) {
      setEditHash(urlType, 'store');

      mockUseBlobStore.mockReturnValue(
        makeBlobStoreHook({
          blobStore: { name: 'store', path: '/data' }, // no type in body
        })
      );

      const { unmount } = render(<BlobStoresPage />);

      expect(await screen.findByTestId(testId)).toBeInTheDocument();
      unmount();
    }
  });
});

// ---------------------------------------------------------------------------
// REGRESSION: save works when the REST API omits name/type from the response
// ---------------------------------------------------------------------------

describe('save when API omits name and type fields', () => {
  // -----------------------------------------------------------------------
  // Root cause: GET /service/rest/v1/blobstores/file/{name} does NOT include
  // `name` or `type` in the JSON body — both are implied by the URL.
  //
  // Before the fix:
  //   - formData.name was undefined → validate() failed silently ("Name is
  //     required") → handleSave() returned early → no HTTP request was made.
  //   - formData.type was undefined but baseData had type:"file" → isDirty was
  //     always true → "Unsaved changes" shown immediately on load.
  //
  // After the fix the useEffect normalizes formData: it injects name from
  // routeState.blobStoreName and type from routeState.blobStoreType so both
  // validate() and the dirty comparison work correctly.
  // -----------------------------------------------------------------------

  it('save is called when the API body omits the name field (name from URL)', async () => {
    setEditHash('file', 'url-name-store');

    const save = jest.fn().mockResolvedValue(undefined);
    mockUseBlobStore.mockReturnValue(
      makeBlobStoreHook({
        // Simulate real API: no `name` field in body
        blobStore: { path: '/data', type: 'file' } as any,
        save,
      })
    );

    render(<BlobStoresPage />);

    // Enable soft quota (makes form dirty) and fill in required fields
    const checkbox = await screen.findByTestId('checkbox-blobstore-softquota-enabled');
    fireEvent.click(checkbox);
    fireEvent.change(screen.getByTestId('input-blobstore-softquota-limit'), { target: { value: '100' } });

    const submitBtn = screen.getByTestId('form-submit');
    expect(submitBtn).not.toBeDisabled();
    await act(async () => { fireEvent.click(submitBtn); });

    // save must be called — previously it was silently blocked by validate()
    expect(save).toHaveBeenCalledTimes(1);
  });

  it('save payload contains the name sourced from the URL path param', async () => {
    setEditHash('file', 'url-name-store');

    const save = jest.fn().mockResolvedValue(undefined);
    mockUseBlobStore.mockReturnValue(
      makeBlobStoreHook({
        blobStore: { path: '/data', type: 'file' } as any,
        save,
      })
    );

    render(<BlobStoresPage />);

    const checkbox = await screen.findByTestId('checkbox-blobstore-softquota-enabled');
    fireEvent.click(checkbox);
    fireEvent.change(screen.getByTestId('input-blobstore-softquota-limit'), { target: { value: '200' } });

    await act(async () => { fireEvent.click(screen.getByTestId('form-submit')); });

    expect(save).toHaveBeenCalledWith(
      expect.objectContaining({ name: 'url-name-store' })
    );
  });

  it('save is called when the API body omits the type field (type from URL)', async () => {
    setEditHash('file', 'my-store');

    const save = jest.fn().mockResolvedValue(undefined);
    mockUseBlobStore.mockReturnValue(
      makeBlobStoreHook({
        // Simulate real API: no `type` field in body
        blobStore: { name: 'my-store', path: '/data' } as any,
        save,
      })
    );

    render(<BlobStoresPage />);

    const checkbox = await screen.findByTestId('checkbox-blobstore-softquota-enabled');
    fireEvent.click(checkbox);
    fireEvent.change(screen.getByTestId('input-blobstore-softquota-limit'), { target: { value: '50' } });

    await act(async () => { fireEvent.click(screen.getByTestId('form-submit')); });

    expect(save).toHaveBeenCalledTimes(1);
    expect(save).toHaveBeenCalledWith(expect.objectContaining({ name: 'my-store', type: 'file' }));
  });
});
