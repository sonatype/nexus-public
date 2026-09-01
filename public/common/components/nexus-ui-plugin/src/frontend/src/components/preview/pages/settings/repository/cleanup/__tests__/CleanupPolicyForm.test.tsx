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
import '@testing-library/jest-dom';
import { Theme } from '@radix-ui/themes';

// Polyfill ResizeObserver for Radix UI components in jsdom
if (typeof globalThis.ResizeObserver === 'undefined') {
  (globalThis as any).ResizeObserver = class ResizeObserver {
    observe() {}
    unobserve() {}
    disconnect() {}
  };
}

import { CleanupPolicyForm } from '../CleanupPolicyForm';
import { CleanupPolicy, FormatCriteria } from '../types';
import * as useCleanupPoliciesApiModule from '../useCleanupPoliciesApi';
import { useCleanupPolicyForm } from '../useCleanupPolicyForm';

// Mock SCSS imports
jest.mock('../CleanupPolicyForm.scss', () => ({}));

// Mock hooks
jest.mock('../useCleanupPoliciesApi');
jest.mock('../useCleanupPolicyForm');

const mockUseCleanupPolicyForm = useCleanupPolicyForm as jest.MockedFunction<typeof useCleanupPolicyForm>;

function createMockCleanupForm(data: any = {}) {
  return {
    field: jest.fn((name: string) => {
      const value = data[name];
      return { name, value: value != null ? String(value) : '', onChange: jest.fn(), onBlur: jest.fn(), error: undefined };
    }),
    data,
    isPristine: true,
    isSaving: false,
    isLoading: false,
    isDeleting: false,
    saveError: null,
    validationErrors: {},
    state: { matches: jest.fn(() => false), context: { data, policy: null, criteriaEnabled: {} } },
    send: jest.fn(),
  } as any;
}

// Mock child components
jest.mock('../CleanupPolicyPreview', () => ({
  CleanupPolicyPreview: () => <div data-testid="cleanup-policy-preview">Preview</div>,
}));

jest.mock('../CleanupPolicyDryRun', () => ({
  CleanupPolicyDryRun: () => <div data-testid="cleanup-policy-dryrun">Dry Run</div>,
}));

// Mock unsaved changes warning
jest.mock('../../../../../shared', () => ({
  useUnsavedChangesWarning: jest.fn(),
  clearDirtyState: jest.fn(),
}));

// Mock shared form components to avoid SCSS import issues
jest.mock('../../../../../shared/form', () => ({
  SettingsForm: ({ children, onSubmit, onCancel, submitLabel, cancelLabel, testId, error, footerExtra }: any) => (
    <form data-testid={testId} onSubmit={(e: any) => { e.preventDefault(); onSubmit?.(); }}>
      {error && <div role="alert">{error}</div>}
      {children}
      <button type="submit">{submitLabel || 'Save'}</button>
      <button type="button" onClick={onCancel}>{cancelLabel || 'Cancel'}</button>
      {footerExtra}
    </form>
  ),
  SettingsFormSection: ({ title, description, children }: any) => (
    <fieldset>
      <legend>{title}</legend>
      {description && <p>{description}</p>}
      {children}
    </fieldset>
  ),
  SettingsTextInput: ({ label, name, value, onChange, onBlur, disabled, required, placeholder, error }: any) => (
    <div>
      {label && <label htmlFor={name}>{label}</label>}
      <input id={name} name={name} value={value || ''} onChange={onChange} onBlur={onBlur}
        disabled={disabled} required={required} placeholder={placeholder} aria-label={label} />
      {error && <span>{error}</span>}
    </div>
  ),
  SettingsTextArea: ({ label, name, value, onChange, onBlur, className }: any) => (
    <div className={className}>
      {label && <label htmlFor={name}>{label}</label>}
      <textarea id={name} name={name} value={value || ''} onChange={onChange} onBlur={onBlur} />
    </div>
  ),
  SettingsSelect: ({ label, name, value, onChange, options, placeholder }: any) => (
    <div>
      {label && <label htmlFor={name}>{label}</label>}
      <select id={name} name={name} value={value || ''} onChange={(e: any) => onChange?.(e.target.value)}>
        {placeholder && <option value="">{placeholder}</option>}
        {options?.map((opt: any) => <option key={opt.value} value={opt.value}>{opt.label}</option>)}
      </select>
    </div>
  ),
  SettingsButton: ({ children, onClick, icon, disabled, testId, variant }: any) => (
    <button data-testid={testId} onClick={onClick} disabled={disabled}>{children}</button>
  ),
  SettingsAlert: ({ children, type }: any) => (
    <div role="alert" data-type={type}>{children}</div>
  ),
  SettingsTransferList: ({
    availableItems = [],
    selectedItems = [],
    onChange,
    availableLabel,
    selectedLabel,
    helpText,
    getItemId = (i: any) => i.id ?? i,
    getItemLabel = (i: any) => i.name ?? i,
    testId,
  }: any) => {
    const selectedIds = new Set(selectedItems.map((i: any) => getItemId(i)));
    const remaining = availableItems.filter((i: any) => !selectedIds.has(getItemId(i)));
    return (
      <div data-testid={testId}>
        <div data-testid="transfer-list-available-label">{availableLabel}</div>
        <div data-testid="transfer-list-selected-label">{selectedLabel}</div>
        {helpText && <div data-testid="transfer-list-help-text">{helpText}</div>}
        <ul data-testid="transfer-list-available">
          {remaining.map((item: any) => (
            <li key={getItemId(item)}>
              <button
                type="button"
                data-testid={`transfer-list-add-${getItemId(item)}`}
                onClick={() => onChange?.([...selectedItems, item])}
              >
                {getItemLabel(item)}
              </button>
            </li>
          ))}
        </ul>
        <ul data-testid="transfer-list-selected">
          {selectedItems.map((item: any) => (
            <li key={getItemId(item)}>
              <span>{getItemLabel(item)}</span>
              <button
                type="button"
                data-testid={`transfer-list-remove-${getItemId(item)}`}
                onClick={() =>
                  onChange?.(selectedItems.filter((i: any) => getItemId(i) !== getItemId(item)))
                }
              >
                remove
              </button>
            </li>
          ))}
        </ul>
      </div>
    );
  },
}));

const mockedUseCleanupPoliciesApi = useCleanupPoliciesApiModule.useCleanupPoliciesApi as jest.MockedFunction<
  typeof useCleanupPoliciesApiModule.useCleanupPoliciesApi
>;

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

describe('CleanupPolicyForm', () => {
  const mockOnSave = jest.fn().mockResolvedValue(undefined);
  const mockOnCancel = jest.fn();
  const mockOnDelete = jest.fn();

  const mockFormatCriteria: FormatCriteria[] = [
    {
      id: 'maven2',
      name: 'Maven2',
      availableCriteria: ['lastBlobUpdated', 'lastDownloaded', 'isPrerelease', 'regex'],
    },
    {
      id: 'npm',
      name: 'npm',
      availableCriteria: ['lastBlobUpdated', 'lastDownloaded'],
    },
    {
      id: 'docker',
      name: 'Docker',
      availableCriteria: ['lastBlobUpdated', 'lastDownloaded'],
    },
  ];

  const mockPolicy: CleanupPolicy = {
    name: 'test-policy',
    format: 'maven2',
    notes: 'Test description',
    criteriaLastBlobUpdated: 30,
    criteriaLastDownloaded: null,
    criteriaReleaseType: null,
    criteriaAssetRegex: null,
    retain: null,
    sortBy: null,
    inUseCount: 0,
  };

  beforeEach(() => {
    jest.clearAllMocks();
    mockUseCleanupPolicyForm.mockImplementation(({ policy }: any) => {
      const formData = policy ? {
        name: policy.name, format: policy.format, notes: policy.notes || '',
        criteriaLastBlobUpdated: policy.criteriaLastBlobUpdated, criteriaLastDownloaded: policy.criteriaLastDownloaded,
        criteriaReleaseType: policy.criteriaReleaseType, criteriaAssetRegex: policy.criteriaAssetRegex,
        retain: policy.retain, sortBy: policy.sortBy,
      } : { name: '', format: '', notes: '', criteriaLastBlobUpdated: null, criteriaLastDownloaded: null,
        criteriaReleaseType: null, criteriaAssetRegex: null, retain: null, sortBy: null };
      return {
        form: createMockCleanupForm(formData),
        policy: policy || null,
        isCreate: !policy,
        criteriaEnabled: { lastBlobUpdated: !!policy?.criteriaLastBlobUpdated, lastDownloaded: !!policy?.criteriaLastDownloaded,
          assetRegex: !!policy?.criteriaAssetRegex, retain: !!policy?.retain },
        changeFormat: jest.fn(),
        toggleCriteria: jest.fn(),
        changeReleaseType: jest.fn(),
      } as any;
    });
    mockedUseCleanupPoliciesApi.mockReturnValue({
      loading: false,
      error: null,
      setError: jest.fn(),
      fetchCleanupPolicies: jest.fn().mockResolvedValue([]),
      fetchCleanupPolicy: jest.fn().mockResolvedValue(null),
      fetchFormatCriteria: jest.fn().mockResolvedValue(mockFormatCriteria),
      fetchRepositories: jest.fn().mockResolvedValue([]),
      createCleanupPolicy: jest.fn().mockResolvedValue({}),
      updateCleanupPolicy: jest.fn().mockResolvedValue({}),
      deleteCleanupPolicy: jest.fn().mockResolvedValue({}),
      previewCleanupPolicy: jest.fn().mockResolvedValue({ components: [], total: 0 }),
      getDryRunCsvUrl: jest.fn().mockReturnValue(''),
      isPreviewEnabled: jest.fn().mockReturnValue(false),
      isRetainEnabled: jest.fn().mockReturnValue(false),
      isRetainAllFormatsEnabled: jest.fn().mockReturnValue(false),
    } as any);
  });

  describe('create mode', () => {
    it('renders empty form in create mode', () => {
      render(
        <CleanupPolicyForm
          isCreate={true}
          formatCriteria={mockFormatCriteria}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByText('Policy Settings')).toBeInTheDocument();
      expect(screen.getByLabelText(/Name/i)).toBeInTheDocument();
    });

    it('shows Create button in create mode', () => {
      render(
        <CleanupPolicyForm
          isCreate={true}
          formatCriteria={mockFormatCriteria}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByRole('button', { name: /Create/i })).toBeInTheDocument();
    });

    it('allows editing name in create mode', async () => {
      render(
        <CleanupPolicyForm
          isCreate={true}
          formatCriteria={mockFormatCriteria}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      const nameInput = screen.getByLabelText(/Name/i);
      // Name field should be enabled in create mode (not readonly/disabled)
      expect(nameInput).not.toBeDisabled();
      // The hook's field() was called for the name field
      const formMock = mockUseCleanupPolicyForm.mock.results[0]?.value?.form;
      expect(formMock?.field).toHaveBeenCalledWith('name');
    });
  });

  describe('edit mode', () => {
    it('renders form with policy data in edit mode', () => {
      render(
        <CleanupPolicyForm
          policy={mockPolicy}
          isCreate={false}
          formatCriteria={mockFormatCriteria}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByDisplayValue('test-policy')).toBeInTheDocument();
    });

    it('shows Save button in edit mode', () => {
      render(
        <CleanupPolicyForm
          policy={mockPolicy}
          isCreate={false}
          formatCriteria={mockFormatCriteria}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByRole('button', { name: /Save/i })).toBeInTheDocument();
    });

    it('disables name input in edit mode', () => {
      render(
        <CleanupPolicyForm
          policy={mockPolicy}
          isCreate={false}
          formatCriteria={mockFormatCriteria}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      const nameInput = screen.getByDisplayValue('test-policy');
      expect(nameInput).toBeDisabled();
    });
  });

  describe('form submission', () => {
    it('validates name is required', async () => {
      

      render(
        <CleanupPolicyForm
          isCreate={true}
          formatCriteria={mockFormatCriteria}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      // Submit without name
      const submitButton = screen.getByRole('button', { name: /Create/i });
      await userEvent.click(submitButton);

      expect(mockOnSave).not.toHaveBeenCalled();
    });

    it('validates format is required', async () => {
      

      render(
        <CleanupPolicyForm
          isCreate={true}
          formatCriteria={mockFormatCriteria}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      // Fill name but not format
      const nameInput = screen.getByLabelText(/Name/i);
      await userEvent.type(nameInput, 'new-policy');

      const submitButton = screen.getByRole('button', { name: /Create/i });
      await userEvent.click(submitButton);

      expect(mockOnSave).not.toHaveBeenCalled();
    });

    it('does not call onSave when form is incomplete', async () => {
      // Start with a completely empty form (no policy)
      render(
        <CleanupPolicyForm
          isCreate={true}
          formatCriteria={mockFormatCriteria}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      // Try to submit the empty form
      const submitButton = screen.getByRole('button', { name: /Create/i });
      await userEvent.click(submitButton);

      // Should not call onSave since form is incomplete
      expect(mockOnSave).not.toHaveBeenCalled();
    });
  });

  // Note: Criteria section tests removed - they depend on complex Radix UI Select interactions
  // and internal React state timing that doesn't work reliably in jsdom.
  // The criteria functionality is covered by E2E tests in e2e/tests/

  describe('cancel functionality', () => {
    it('calls onCancel when Cancel button is clicked', async () => {
      render(
        <CleanupPolicyForm
          isCreate={true}
          formatCriteria={mockFormatCriteria}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      const cancelButton = screen.getByRole('button', { name: /Cancel/i });
      fireEvent.click(cancelButton);

      expect(mockOnCancel).toHaveBeenCalled();
    });
  });

  describe('delete functionality', () => {
    it('shows delete button when canDelete is true', () => {
      render(
        <CleanupPolicyForm
          policy={mockPolicy}
          isCreate={false}
          formatCriteria={mockFormatCriteria}
          canDelete={true}
          onSave={mockOnSave}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByRole('button', { name: /Delete/i })).toBeInTheDocument();
    });

    it('hides delete button when canDelete is false', () => {
      render(
        <CleanupPolicyForm
          policy={mockPolicy}
          isCreate={false}
          formatCriteria={mockFormatCriteria}
          canDelete={false}
          onSave={mockOnSave}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.queryByRole('button', { name: /Delete/i })).not.toBeInTheDocument();
    });

    it('calls onDelete when delete button is clicked', () => {
      render(
        <CleanupPolicyForm
          policy={mockPolicy}
          isCreate={false}
          formatCriteria={mockFormatCriteria}
          canDelete={true}
          onSave={mockOnSave}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      const deleteButton = screen.getByRole('button', { name: /Delete/i });
      fireEvent.click(deleteButton);

      expect(mockOnDelete).toHaveBeenCalled();
    });
  });

  describe('loading state', () => {
    it('shows loading message when loading is true', () => {
      render(
        <CleanupPolicyForm
          isCreate={true}
          formatCriteria={mockFormatCriteria}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          loading={true}
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByText('Loading...')).toBeInTheDocument();
    });
  });

  describe('error display', () => {
    it('displays error message when error prop is provided', () => {
      render(
        <CleanupPolicyForm
          isCreate={true}
          formatCriteria={mockFormatCriteria}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          error="Failed to save cleanup policy"
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByText('Failed to save cleanup policy')).toBeInTheDocument();
    });
  });

  describe('data-testid attributes', () => {
    it('has correct data-testid on form', () => {
      render(
        <CleanupPolicyForm
          isCreate={true}
          formatCriteria={mockFormatCriteria}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByTestId('cleanup-policy-form')).toBeInTheDocument();
    });

    it('has correct mode in create mode', () => {
      render(
        <CleanupPolicyForm
          isCreate={true}
          formatCriteria={mockFormatCriteria}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      // In create mode, form renders with Create button
      expect(screen.getByTestId('cleanup-policy-form')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Create/i })).toBeInTheDocument();
    });

    it('has correct mode in edit mode', () => {
      render(
        <CleanupPolicyForm
          policy={mockPolicy}
          isCreate={false}
          formatCriteria={mockFormatCriteria}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      // In edit mode, form renders with Save button
      expect(screen.getByTestId('cleanup-policy-form')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Save/i })).toBeInTheDocument();
    });
  });

  describe('description section', () => {
    it('renders description about cleanup policies', () => {
      render(
        <CleanupPolicyForm
          isCreate={true}
          formatCriteria={mockFormatCriteria}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      expect(
        screen.getByText(/allow you to automatically delete unused components/)
      ).toBeInTheDocument();
    });
  });

  describe('notes field', () => {
    it('renders description/notes textarea', () => {
      render(
        <CleanupPolicyForm
          isCreate={true}
          formatCriteria={mockFormatCriteria}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByText('Description')).toBeInTheDocument();
    });

    it('populates notes field from policy in edit mode', () => {
      render(
        <CleanupPolicyForm
          policy={mockPolicy}
          isCreate={false}
          formatCriteria={mockFormatCriteria}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByDisplayValue('Test description')).toBeInTheDocument();
    });

    it('applies cleanup-policy-form__notes-textarea class to Description field', () => {
      const { container } = render(
        <CleanupPolicyForm
          isCreate={true}
          formatCriteria={mockFormatCriteria}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      expect(container.querySelector('.cleanup-policy-form__notes-textarea')).toBeInTheDocument();
    });
  });

  describe('preview section visibility', () => {
    it('shows dry run preview section when format is selected even without criteria (PostgreSQL)', () => {
      mockedUseCleanupPoliciesApi.mockReturnValue({
        loading: false,
        error: null,
        setError: jest.fn(),
        fetchCleanupPolicies: jest.fn().mockResolvedValue([]),
        fetchCleanupPolicy: jest.fn().mockResolvedValue(null),
        fetchFormatCriteria: jest.fn().mockResolvedValue(mockFormatCriteria),
        fetchRepositories: jest.fn().mockResolvedValue([]),
        createCleanupPolicy: jest.fn().mockResolvedValue({}),
        updateCleanupPolicy: jest.fn().mockResolvedValue({}),
        deleteCleanupPolicy: jest.fn().mockResolvedValue({}),
        previewCleanupPolicy: jest.fn().mockResolvedValue({ components: [], total: 0 }),
        getDryRunCsvUrl: jest.fn().mockReturnValue(''),
        isPreviewEnabled: jest.fn().mockReturnValue(true),
        isRetainEnabled: jest.fn().mockReturnValue(false),
        isRetainAllFormatsEnabled: jest.fn().mockReturnValue(false),
      } as any);

      const policyWithFormat: CleanupPolicy = {
        ...mockPolicy,
        criteriaLastBlobUpdated: null,
        criteriaAssetRegex: null,
      };

      render(
        <CleanupPolicyForm
          policy={policyWithFormat}
          isCreate={false}
          formatCriteria={mockFormatCriteria}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByTestId('cleanup-policy-dryrun')).toBeInTheDocument();
    });

    it('shows legacy preview when format is selected and preview is disabled (non-PostgreSQL)', () => {
      render(
        <CleanupPolicyForm
          policy={mockPolicy}
          isCreate={false}
          formatCriteria={mockFormatCriteria}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByTestId('cleanup-policy-preview')).toBeInTheDocument();
    });
  });

  describe('retain/Number of Versions section', () => {
    const mavenPolicyWithReleases: CleanupPolicy = {
      name: 'maven-releases-policy',
      format: 'maven2',
      notes: '',
      criteriaLastBlobUpdated: 30,
      criteriaLastDownloaded: null,
      criteriaReleaseType: 'RELEASES',
      criteriaAssetRegex: null,
      retain: null,
      sortBy: null,
      inUseCount: 0,
    };

    const mavenPolicyNoReleases: CleanupPolicy = {
      name: 'maven-all-policy',
      format: 'maven2',
      notes: '',
      criteriaLastBlobUpdated: 30,
      criteriaLastDownloaded: null,
      criteriaReleaseType: '',
      criteriaAssetRegex: null,
      retain: null,
      sortBy: null,
      inUseCount: 0,
    };

    const mavenPolicyPrereleases: CleanupPolicy = {
      name: 'maven-prereleases-policy',
      format: 'maven2',
      notes: '',
      criteriaLastBlobUpdated: 30,
      criteriaLastDownloaded: null,
      criteriaReleaseType: 'PRERELEASES',
      criteriaAssetRegex: null,
      retain: null,
      sortBy: null,
      inUseCount: 0,
    };

    const mavenPolicyReleasesNoCriteria: CleanupPolicy = {
      name: 'maven-releases-no-criteria',
      format: 'maven2',
      notes: '',
      criteriaLastBlobUpdated: null,
      criteriaLastDownloaded: null,
      criteriaReleaseType: 'RELEASES',
      criteriaAssetRegex: null,
      retain: null,
      sortBy: null,
      inUseCount: 0,
    };

    const mavenPolicyEmpty: CleanupPolicy = {
      name: 'maven-empty-policy',
      format: 'maven2',
      notes: '',
      criteriaLastBlobUpdated: null,
      criteriaLastDownloaded: null,
      criteriaReleaseType: '',
      criteriaAssetRegex: null,
      retain: null,
      sortBy: null,
      inUseCount: 0,
    };

    const dockerPolicyWithCriteria: CleanupPolicy = {
      name: 'docker-policy',
      format: 'docker',
      notes: '',
      criteriaLastBlobUpdated: 30,
      criteriaLastDownloaded: null,
      criteriaReleaseType: null,
      criteriaAssetRegex: null,
      retain: null,
      sortBy: null,
      inUseCount: 0,
    };

    const dockerPolicyNoCriteria: CleanupPolicy = {
      name: 'docker-policy',
      format: 'docker',
      notes: '',
      criteriaLastBlobUpdated: null,
      criteriaLastDownloaded: null,
      criteriaReleaseType: null,
      criteriaAssetRegex: null,
      retain: null,
      sortBy: null,
      inUseCount: 0,
    };

    function setupRetainEnabled() {
      mockedUseCleanupPoliciesApi.mockReturnValue({
        loading: false,
        error: null,
        setError: jest.fn(),
        fetchCleanupPolicies: jest.fn().mockResolvedValue([]),
        fetchCleanupPolicy: jest.fn().mockResolvedValue(null),
        fetchFormatCriteria: jest.fn().mockResolvedValue(mockFormatCriteria),
        fetchRepositories: jest.fn().mockResolvedValue([]),
        createCleanupPolicy: jest.fn().mockResolvedValue({}),
        updateCleanupPolicy: jest.fn().mockResolvedValue({}),
        deleteCleanupPolicy: jest.fn().mockResolvedValue({}),
        previewCleanupPolicy: jest.fn().mockResolvedValue({ components: [], total: 0 }),
        getDryRunCsvUrl: jest.fn().mockReturnValue(''),
        isPreviewEnabled: jest.fn().mockReturnValue(false),
        isRetainEnabled: jest.fn().mockReturnValue(true),
        isRetainAllFormatsEnabled: jest.fn().mockReturnValue(false),
      } as any);
    }

    describe('maven2 format', () => {
      it('shows retain section with maven-specific warning when release type is empty (default combined)', () => {
        setupRetainEnabled();

        render(
          <CleanupPolicyForm
            policy={mavenPolicyNoReleases}
            isCreate={false}
            formatCriteria={mockFormatCriteria}
            onSave={mockOnSave}
            onCancel={mockOnCancel}
          />,
          { wrapper: TestWrapper }
        );

        expect(screen.getByText('Except, do not remove any component that meets the following criterion:')).toBeInTheDocument();
        expect(screen.getByText('This option is only applicable to releases. Select "Releases" from the Release Type dropdown to enable this option.')).toBeInTheDocument();
        expect(screen.getByTestId('checkbox-criteria-retain')).toBeDisabled();
      });

      it('disables retain with maven-specific warning when release type is PRERELEASES', () => {
        setupRetainEnabled();

        render(
          <CleanupPolicyForm
            policy={mavenPolicyPrereleases}
            isCreate={false}
            formatCriteria={mockFormatCriteria}
            onSave={mockOnSave}
            onCancel={mockOnCancel}
          />,
          { wrapper: TestWrapper }
        );

        expect(screen.getByText('This option is only applicable to releases. Select "Releases" from the Release Type dropdown to enable this option.')).toBeInTheDocument();
        expect(screen.getByTestId('checkbox-criteria-retain')).toBeDisabled();
      });

      it('shows combined maven-specific warning when neither release type nor criteria are selected', () => {
        setupRetainEnabled();

        render(
          <CleanupPolicyForm
            policy={mavenPolicyEmpty}
            isCreate={true}
            formatCriteria={mockFormatCriteria}
            onSave={mockOnSave}
            onCancel={mockOnCancel}
          />,
          { wrapper: TestWrapper }
        );

        expect(screen.getByText('This option is only applicable to releases. Select "Releases" and at least one cleanup criterion to enable this option.')).toBeInTheDocument();
        expect(screen.getByTestId('checkbox-criteria-retain')).toBeDisabled();
      });

      it('shows criteria-required warning when RELEASES selected but no other criteria chosen', () => {
        setupRetainEnabled();

        render(
          <CleanupPolicyForm
            policy={mavenPolicyReleasesNoCriteria}
            isCreate={false}
            formatCriteria={mockFormatCriteria}
            onSave={mockOnSave}
            onCancel={mockOnCancel}
          />,
          { wrapper: TestWrapper }
        );

        expect(screen.getByText('Only after selecting the cleanup criteria, retain can be enabled.')).toBeInTheDocument();
        expect(screen.getByTestId('checkbox-criteria-retain')).toBeDisabled();
      });

      it('enables retain section when release type is RELEASES and a criterion is selected', () => {
        setupRetainEnabled();

        render(
          <CleanupPolicyForm
            policy={mavenPolicyWithReleases}
            isCreate={false}
            formatCriteria={mockFormatCriteria}
            onSave={mockOnSave}
            onCancel={mockOnCancel}
          />,
          { wrapper: TestWrapper }
        );

        expect(screen.getByText('Except, do not remove any component that meets the following criterion:')).toBeInTheDocument();
        expect(screen.queryByText(/This option is only applicable to releases/)).not.toBeInTheDocument();
        expect(screen.queryByText('Only after selecting the cleanup criteria, retain can be enabled.')).not.toBeInTheDocument();
        expect(screen.getByTestId('checkbox-criteria-retain')).not.toBeDisabled();
      });

      it('does not show retain section when retain feature flag is disabled', () => {
        render(
          <CleanupPolicyForm
            policy={mavenPolicyWithReleases}
            isCreate={false}
            formatCriteria={mockFormatCriteria}
            onSave={mockOnSave}
            onCancel={mockOnCancel}
          />,
          { wrapper: TestWrapper }
        );

        expect(screen.queryByText('Except, do not remove any component that meets the following criterion:')).not.toBeInTheDocument();
      });
    });

    describe('docker format', () => {
      it('shows retain section with criteria-only warning when no cleanup criterion is selected (no release dropdown for docker)', () => {
        setupRetainEnabled();

        render(
          <CleanupPolicyForm
            policy={dockerPolicyNoCriteria}
            isCreate={false}
            formatCriteria={mockFormatCriteria}
            onSave={mockOnSave}
            onCancel={mockOnCancel}
          />,
          { wrapper: TestWrapper }
        );

        expect(screen.getByText('Except, do not remove any component that meets the following criterion:')).toBeInTheDocument();
        // Docker has no Release Type dropdown, so the release-type requirement does not gate Retain.
        expect(screen.getByText('Only after selecting the cleanup criteria, retain can be enabled.')).toBeInTheDocument();
        expect(screen.queryByText(/Select a release type/)).not.toBeInTheDocument();
        expect(screen.getByTestId('checkbox-criteria-retain')).toBeDisabled();
      });

      it('enables retain section when a cleanup criterion is selected (no release type needed for docker)', () => {
        setupRetainEnabled();

        render(
          <CleanupPolicyForm
            policy={dockerPolicyWithCriteria}
            isCreate={false}
            formatCriteria={mockFormatCriteria}
            onSave={mockOnSave}
            onCancel={mockOnCancel}
          />,
          { wrapper: TestWrapper }
        );

        expect(screen.getByText('Except, do not remove any component that meets the following criterion:')).toBeInTheDocument();
        expect(screen.queryByText(/Select a release type/)).not.toBeInTheDocument();
        expect(screen.queryByText('Only after selecting the cleanup criteria, retain can be enabled.')).not.toBeInTheDocument();
        expect(screen.getByTestId('checkbox-criteria-retain')).not.toBeDisabled();
      });

      it('shows sort by component age for docker format', () => {
        setupRetainEnabled();

        render(
          <CleanupPolicyForm
            policy={dockerPolicyWithCriteria}
            isCreate={false}
            formatCriteria={mockFormatCriteria}
            onSave={mockOnSave}
            onCancel={mockOnCancel}
          />,
          { wrapper: TestWrapper }
        );

        expect(screen.getByText(/component age/)).toBeInTheDocument();
      });
    });

    describe('unsupported format', () => {
      it('does not show retain section for npm format', () => {
        // npm is in RETAIN_SUPPORTED_FORMATS, so simulate it being unsupported by
        // having checkRetainEnabled return false for npm.
        mockedUseCleanupPoliciesApi.mockReturnValue({
          loading: false,
          error: null,
          setError: jest.fn(),
          fetchCleanupPolicies: jest.fn().mockResolvedValue([]),
          fetchCleanupPolicy: jest.fn().mockResolvedValue(null),
          fetchFormatCriteria: jest.fn().mockResolvedValue(mockFormatCriteria),
          fetchRepositories: jest.fn().mockResolvedValue([]),
          createCleanupPolicy: jest.fn().mockResolvedValue({}),
          updateCleanupPolicy: jest.fn().mockResolvedValue({}),
          deleteCleanupPolicy: jest.fn().mockResolvedValue({}),
          previewCleanupPolicy: jest.fn().mockResolvedValue({ components: [], total: 0 }),
          getDryRunCsvUrl: jest.fn().mockReturnValue(''),
          isPreviewEnabled: jest.fn().mockReturnValue(false),
          isRetainEnabled: jest.fn((format: string) => format !== 'npm'),
          isRetainAllFormatsEnabled: jest.fn().mockReturnValue(false),
        } as any);

        const npmPolicy: CleanupPolicy = {
          name: 'npm-policy',
          format: 'npm',
          notes: '',
          criteriaLastBlobUpdated: 30,
          criteriaLastDownloaded: null,
          criteriaReleaseType: null,
          criteriaAssetRegex: null,
          retain: null,
          sortBy: null,
          inUseCount: 0,
        };

        render(
          <CleanupPolicyForm
            policy={npmPolicy}
            isCreate={false}
            formatCriteria={mockFormatCriteria}
            onSave={mockOnSave}
            onCancel={mockOnCancel}
          />,
          { wrapper: TestWrapper }
        );

        expect(screen.queryByText('Except, do not remove any component that meets the following criterion:')).not.toBeInTheDocument();
      });
    });
  });

  describe('repositories transfer list', () => {
    const npmPolicy: CleanupPolicy = {
      name: 'npm-policy',
      format: 'npm',
      notes: '',
      criteriaLastBlobUpdated: 30,
      criteriaLastDownloaded: null,
      criteriaReleaseType: null,
      criteriaAssetRegex: null,
      retain: null,
      sortBy: null,
      inUseCount: 0,
    };

    function mockApi(overrides: any = {}) {
      mockedUseCleanupPoliciesApi.mockReturnValue({
        loading: false,
        error: null,
        setError: jest.fn(),
        fetchCleanupPolicies: jest.fn().mockResolvedValue([]),
        fetchCleanupPolicy: jest.fn().mockResolvedValue(null),
        fetchFormatCriteria: jest.fn().mockResolvedValue(mockFormatCriteria),
        fetchRepositories: jest.fn().mockResolvedValue([]),
        createCleanupPolicy: jest.fn().mockResolvedValue({}),
        updateCleanupPolicy: jest.fn().mockResolvedValue({}),
        deleteCleanupPolicy: jest.fn().mockResolvedValue({}),
        previewCleanupPolicy: jest.fn().mockResolvedValue({ components: [], total: 0 }),
        getDryRunCsvUrl: jest.fn().mockReturnValue(''),
        isPreviewEnabled: jest.fn().mockReturnValue(false),
        isRetainEnabled: jest.fn().mockReturnValue(false),
        isRetainAllFormatsEnabled: jest.fn().mockReturnValue(true),
        ...overrides,
      } as any);
    }

    it('hides Repositories section when isRetainAllFormatsEnabled flag is off', () => {
      mockApi({
        isRetainAllFormatsEnabled: jest.fn().mockReturnValue(false),
        fetchRepositories: jest.fn().mockResolvedValue([
          { id: 'repo-a', name: 'repo-a' },
        ]),
      });

      render(
        <CleanupPolicyForm
          policy={npmPolicy}
          isCreate={false}
          formatCriteria={mockFormatCriteria}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.queryByText('Repositories')).not.toBeInTheDocument();
      expect(screen.queryByTestId('cleanup-policy-repositories-transfer-list')).not.toBeInTheDocument();
    });

    it('hides Repositories section for unsupported format (maven2) even when flag is on', () => {
      mockApi();

      render(
        <CleanupPolicyForm
          policy={mockPolicy}
          isCreate={false}
          formatCriteria={mockFormatCriteria}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.queryByTestId('cleanup-policy-repositories-transfer-list')).not.toBeInTheDocument();
    });

    it('shows empty-state message when format has no available repositories', async () => {
      mockApi({ fetchRepositories: jest.fn().mockResolvedValue([]) });

      render(
        <CleanupPolicyForm
          policy={npmPolicy}
          isCreate={false}
          formatCriteria={mockFormatCriteria}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      await waitFor(() =>
        expect(screen.getByText('No repositories available for this format.')).toBeInTheDocument()
      );
      expect(screen.queryByTestId('cleanup-policy-repositories-transfer-list')).not.toBeInTheDocument();
    });

    it('renders the transfer list with correct labels and help text when repos are available', async () => {
      mockApi({
        fetchRepositories: jest.fn().mockResolvedValue([
          { id: 'repo-a', name: 'repo-a' },
          { id: 'repo-b', name: 'repo-b' },
        ]),
      });

      render(
        <CleanupPolicyForm
          policy={npmPolicy}
          isCreate={false}
          formatCriteria={mockFormatCriteria}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      await waitFor(() =>
        expect(screen.getByTestId('cleanup-policy-repositories-transfer-list')).toBeInTheDocument()
      );

      expect(screen.getByTestId('transfer-list-available-label')).toHaveTextContent('Available Repositories');
      expect(screen.getByTestId('transfer-list-selected-label')).toHaveTextContent('Applied Repositories');
      expect(screen.getByTestId('transfer-list-help-text')).toHaveTextContent(
        'Select repositories to apply this policy to'
      );
    });

    it('pre-populates Applied Repositories from policy.repositories on edit', async () => {
      mockApi({
        fetchRepositories: jest.fn().mockResolvedValue([
          { id: 'repo-a', name: 'repo-a' },
          { id: 'repo-b', name: 'repo-b' },
          { id: 'repo-c', name: 'repo-c' },
        ]),
      });

      render(
        <CleanupPolicyForm
          policy={{ ...npmPolicy, repositories: ['repo-b'] } as any}
          isCreate={false}
          formatCriteria={mockFormatCriteria}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      await waitFor(() =>
        expect(screen.getByTestId('cleanup-policy-repositories-transfer-list')).toBeInTheDocument()
      );

      const selectedList = screen.getByTestId('transfer-list-selected');
      const availableList = screen.getByTestId('transfer-list-available');
      expect(selectedList).toHaveTextContent('repo-b');
      expect(availableList).toHaveTextContent('repo-a');
      expect(availableList).toHaveTextContent('repo-c');
      expect(availableList).not.toHaveTextContent('repo-b');
    });

    it('moves an item from Available to Applied when its add button is clicked', async () => {
      mockApi({
        fetchRepositories: jest.fn().mockResolvedValue([
          { id: 'repo-a', name: 'repo-a' },
          { id: 'repo-b', name: 'repo-b' },
        ]),
      });

      render(
        <CleanupPolicyForm
          policy={npmPolicy}
          isCreate={false}
          formatCriteria={mockFormatCriteria}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      await waitFor(() =>
        expect(screen.getByTestId('cleanup-policy-repositories-transfer-list')).toBeInTheDocument()
      );

      fireEvent.click(screen.getByTestId('transfer-list-add-repo-a'));

      await waitFor(() =>
        expect(screen.getByTestId('transfer-list-selected')).toHaveTextContent('repo-a')
      );
      expect(screen.getByTestId('transfer-list-available')).not.toHaveTextContent('repo-a');
    });

    it('moves an item from Applied back to Available when its remove button is clicked', async () => {
      mockApi({
        fetchRepositories: jest.fn().mockResolvedValue([
          { id: 'repo-a', name: 'repo-a' },
          { id: 'repo-b', name: 'repo-b' },
        ]),
      });

      render(
        <CleanupPolicyForm
          policy={{ ...npmPolicy, repositories: ['repo-a'] } as any}
          isCreate={false}
          formatCriteria={mockFormatCriteria}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      await waitFor(() =>
        expect(screen.getByTestId('cleanup-policy-repositories-transfer-list')).toBeInTheDocument()
      );

      fireEvent.click(screen.getByTestId('transfer-list-remove-repo-a'));

      await waitFor(() =>
        expect(screen.getByTestId('transfer-list-available')).toHaveTextContent('repo-a')
      );
      expect(screen.getByTestId('transfer-list-selected')).not.toHaveTextContent('repo-a');
    });
  });

  describe('form field width classes', () => {
    it('applies --wide modifier to Asset Name Matcher criteria input', () => {
      mockUseCleanupPolicyForm.mockImplementation(({ policy }: any) => {
        const formData = {
          name: policy?.name || '',
          format: policy?.format || 'maven2',
          notes: policy?.notes || '',
          criteriaLastBlobUpdated: null,
          criteriaLastDownloaded: null,
          criteriaReleaseType: null,
          criteriaAssetRegex: '.*-SNAPSHOT.*',
          retain: null,
          sortBy: null,
        };
        return {
          form: createMockCleanupForm(formData),
          policy: policy || null,
          isCreate: !policy,
          criteriaEnabled: {
            lastBlobUpdated: false,
            lastDownloaded: false,
            assetRegex: true,
            retain: false,
          },
          changeFormat: jest.fn(),
          toggleCriteria: jest.fn(),
          changeReleaseType: jest.fn(),
        } as any;
      });

      const { container } = render(
        <CleanupPolicyForm
          isCreate={true}
          formatCriteria={mockFormatCriteria}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      expect(container.querySelector('.cleanup-policy-form__criteria-input--wide')).toBeInTheDocument();
    });
  });
});

