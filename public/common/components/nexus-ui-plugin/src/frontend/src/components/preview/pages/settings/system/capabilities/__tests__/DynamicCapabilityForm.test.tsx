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
import { Theme } from '@radix-ui/themes';
import { DynamicCapabilityForm } from '../DynamicCapabilityForm';
import { useCapabilitiesApi } from '../useCapabilitiesApi';
import { useCapabilitiesForm } from '../useCapabilitiesForm';
import { CapabilityType, FormField, Capability } from '../types';

// Mock hooks
jest.mock('../useCapabilitiesApi');
jest.mock('../useCapabilitiesForm');
jest.mock('../../../../../../../interface/api', () => ({ restClient: { get: jest.fn().mockResolvedValue([]) } }));

const mockUseCapabilitiesApi = useCapabilitiesApi as jest.MockedFunction<typeof useCapabilitiesApi>;
const mockUseCapabilitiesForm = useCapabilitiesForm as jest.MockedFunction<typeof useCapabilitiesForm>;

function createMockCapForm(data: any = {}) {
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
    state: { matches: jest.fn(() => false), context: { data, capability: null, capabilityTypes: [], selectedCapabilityType: null } },
    send: jest.fn(),
  } as any;
}

const renderWithTheme = (component: React.ReactElement) => {
  return render(<Theme>{component}</Theme>);
};

// Comprehensive test data for all field types
const allFieldTypes: FormField[] = [
  {
    id: 'stringField',
    type: 'string',
    label: 'String Field',
    helpText: 'A string input',
    required: true,
  },
  {
    id: 'numberField',
    type: 'number',
    label: 'Number Field',
    helpText: 'A number input',
    required: true,
    initialValue: '100',
    minValue: 0,
    maxValue: 1000,
  },
  {
    id: 'passwordField',
    type: 'password',
    label: 'Password Field',
    helpText: 'A password input',
    required: false,
  },
  {
    id: 'booleanField',
    type: 'boolean',
    label: 'Boolean Field',
    helpText: 'A checkbox',
    required: false,
    initialValue: 'true',
  },
  {
    id: 'textField',
    type: 'text',
    label: 'Text Area Field',
    helpText: 'A textarea input',
    required: false,
  },
  {
    id: 'urlField',
    type: 'url',
    label: 'URL Field',
    helpText: 'A URL input',
    required: false,
  },
  {
    id: 'comboboxField',
    type: 'combobox',
    label: 'Combobox Field',
    helpText: 'A combobox input',
    required: false,
  },
  {
    id: 'itemselectField',
    type: 'itemselect',
    label: 'Item Select Field',
    helpText: 'An itemselect input',
    required: false,
  },
  {
    id: 'repoTargetField',
    type: 'repo-target',
    label: 'Repo Target Field',
    helpText: 'A repo target input',
    required: false,
  },
  {
    id: 'repoGroupTargetField',
    type: 'repo-or-group-target',
    label: 'Repo Or Group Target Field',
    helpText: 'A repo or group target input',
    required: false,
  },
];

const mockCapabilityTypeWithAllFields: CapabilityType = {
  id: 'test-all-fields',
  name: 'Test All Field Types',
  about: 'This capability tests all field types',
  formFields: allFieldTypes,
};

const simpleCapabilityType: CapabilityType = {
  id: 'simple-capability',
  name: 'Simple Capability',
  about: 'A simple test capability',
  formFields: [
    {
      id: 'repository',
      type: 'string',
      label: 'Repository',
      helpText: 'Select a repository',
      required: true,
    },
    {
      id: 'interval',
      type: 'number',
      label: 'Interval',
      helpText: 'Check interval in seconds',
      required: true,
      initialValue: '60',
    },
  ],
};

const existingCapability: Capability = {
  id: 'cap-1',
  typeId: 'simple-capability',
  typeName: 'Simple Capability',
  enabled: true,
  active: true,
  error: false,
  state: 'active',
  stateDescription: 'Active',
  description: 'Test capability',
  notes: 'Existing notes',
  properties: {
    repository: 'maven-central',
    interval: '120',
  },
};

describe('DynamicCapabilityForm', () => {
  const mockOnSave = jest.fn();
  const mockOnCancel = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockOnSave.mockResolvedValue(undefined);
    mockUseCapabilitiesApi.mockReturnValue({
      loading: false, error: null, setError: jest.fn(),
      fetchCapabilities: jest.fn(), fetchCapabilityTypes: jest.fn(),
      createCapability: jest.fn(), updateCapability: jest.fn(), deleteCapability: jest.fn(),
      enableCapability: jest.fn(), disableCapability: jest.fn(),
    } as any);
    mockUseCapabilitiesForm.mockImplementation(({ capability }: any) => {
      const formData = capability ? {
        typeId: capability.typeId, enabled: capability.enabled, notes: capability.notes || '',
        properties: capability.properties || {},
      } : { typeId: '', enabled: true, notes: '', properties: {} };
      return {
        form: createMockCapForm(formData),
        capability: capability || null,
        capabilityTypes: [],
        selectedCapabilityType: null,
        isCreate: !capability,
      } as any;
    });
  });

  describe('rendering', () => {
    it('renders the form with capability type info', () => {
      renderWithTheme(
        <DynamicCapabilityForm
          capabilityType={simpleCapabilityType}
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByText('Simple Capability')).toBeInTheDocument();
      expect(screen.getByText('A simple test capability')).toBeInTheDocument();
    });

    it('renders Create button in create mode', () => {
      renderWithTheme(
        <DynamicCapabilityForm
          capabilityType={simpleCapabilityType}
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByRole('button', { name: 'Create' })).toBeInTheDocument();
    });

    it('hides Save button in edit mode when form is pristine', () => {
      renderWithTheme(
        <DynamicCapabilityForm
          capabilityType={simpleCapabilityType}
          capability={existingCapability}
          isCreate={false}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />
      );

      // Form is pristine by default, so Save button is hidden (showActionsOnlyWhenDirty behavior)
      expect(screen.queryByRole('button', { name: 'Save' })).not.toBeInTheDocument();
    });

    it('renders Cancel button', () => {
      renderWithTheme(
        <DynamicCapabilityForm
          capabilityType={simpleCapabilityType}
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
    });

    it('renders without crashing with minimal props', () => {
      const minimalType: CapabilityType = {
        id: 'minimal',
        name: 'Minimal',
        about: '',
        formFields: [],
      };

      const { container } = renderWithTheme(
        <DynamicCapabilityForm
          capabilityType={minimalType}
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />
      );

      expect(container).toBeInTheDocument();
    });

    it('displays error message when error prop is provided', () => {
      renderWithTheme(
        <DynamicCapabilityForm
          capabilityType={simpleCapabilityType}
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          error="Something went wrong"
        />
      );

      expect(screen.getByText('Something went wrong')).toBeInTheDocument();
    });

    it('disables buttons when loading', () => {
      renderWithTheme(
        <DynamicCapabilityForm
          capabilityType={simpleCapabilityType}
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          loading={true}
        />
      );

      expect(screen.getByRole('button', { name: 'Create' })).toBeDisabled();
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
    });
  });

  describe('field types', () => {
    it('renders all field types correctly', () => {
      renderWithTheme(
        <DynamicCapabilityForm
          capabilityType={mockCapabilityTypeWithAllFields}
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />
      );

      // Check that each field type is rendered
      expect(screen.getByLabelText(/String Field/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Number Field/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Password Field/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Boolean Field/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Text Area Field/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/URL Field/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Combobox Field/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Item Select Field/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Repo Target Field/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Repo Or Group Target Field/i)).toBeInTheDocument();
    });

    it('initializes number field with initial value', () => {
      // Mock with initial values from formFields
      mockUseCapabilitiesForm.mockReturnValue({
        form: createMockCapForm({ typeId: 'test-all-fields', enabled: true, notes: '', properties: { numberField: '100', booleanField: 'true' } }),
        capability: null, capabilityTypes: [], selectedCapabilityType: null, isCreate: true,
      } as any);

      renderWithTheme(
        <DynamicCapabilityForm capabilityType={mockCapabilityTypeWithAllFields} isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} />
      );

      const numberInput = screen.getByLabelText(/Number Field/i);
      expect(numberInput).toHaveValue(100);
    });

    it('initializes boolean field with initial value', () => {
      mockUseCapabilitiesForm.mockReturnValue({
        form: createMockCapForm({ typeId: 'test-all-fields', enabled: true, notes: '', properties: { numberField: '100', booleanField: 'true' } }),
        capability: null, capabilityTypes: [], selectedCapabilityType: null, isCreate: true,
      } as any);

      renderWithTheme(
        <DynamicCapabilityForm capabilityType={mockCapabilityTypeWithAllFields} isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} />
      );

      const booleanInput = screen.getByLabelText(/Boolean Field/i);
      expect(booleanInput).toBeChecked();
    });
  });

  describe('form initialization', () => {
    it('initializes form with existing capability values', () => {
      renderWithTheme(
        <DynamicCapabilityForm
          capabilityType={simpleCapabilityType}
          capability={existingCapability}
          isCreate={false}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByLabelText(/Repository/i)).toHaveValue('maven-central');
      expect(screen.getByLabelText(/Interval/i)).toHaveValue(120);
    });

    it('initializes enabled checkbox from existing capability', () => {
      renderWithTheme(
        <DynamicCapabilityForm
          capabilityType={simpleCapabilityType}
          capability={{ ...existingCapability, enabled: false }}
          isCreate={false}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByLabelText(/Enable this capability/i)).not.toBeChecked();
    });

    it('initializes notes from existing capability', () => {
      renderWithTheme(
        <DynamicCapabilityForm
          capabilityType={simpleCapabilityType}
          capability={existingCapability}
          isCreate={false}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByLabelText(/Notes/i)).toHaveValue('Existing notes');
    });
  });

  describe('user interactions', () => {
    it('calls onCancel when Cancel button is clicked', async () => {
      renderWithTheme(
        <DynamicCapabilityForm
          capabilityType={simpleCapabilityType}
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />
      );

      await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));

      expect(mockOnCancel).toHaveBeenCalled();
    });

    it('calls form.send on field change', async () => {
      const mockSend = jest.fn();
      mockUseCapabilitiesForm.mockReturnValue({
        form: { ...createMockCapForm({ typeId: 'simple-capability', enabled: true, notes: '', properties: {} }), send: mockSend },
        capability: null, capabilityTypes: [], selectedCapabilityType: null, isCreate: true,
      } as any);

      renderWithTheme(
        <DynamicCapabilityForm capabilityType={simpleCapabilityType} isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} />
      );

      const repositoryInput = screen.getByLabelText(/Repository/i);
      fireEvent.change(repositoryInput, { target: { value: 'new-repo' } });

      expect(mockSend).toHaveBeenCalledWith(expect.objectContaining({ type: 'UPDATE', name: 'properties' }));
    });

    it('calls form.send on enabled toggle', async () => {
      const mockSend = jest.fn();
      mockUseCapabilitiesForm.mockReturnValue({
        form: { ...createMockCapForm({ typeId: 'simple-capability', enabled: true, notes: '', properties: {} }), send: mockSend },
        capability: null, capabilityTypes: [], selectedCapabilityType: null, isCreate: true,
      } as any);

      renderWithTheme(
        <DynamicCapabilityForm capabilityType={simpleCapabilityType} isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} />
      );

      const enabledCheckbox = screen.getByLabelText(/Enable this capability/i);
      expect(enabledCheckbox).toBeChecked();

      fireEvent.click(enabledCheckbox);
      expect(mockSend).toHaveBeenCalledWith(expect.objectContaining({ type: 'UPDATE', name: 'enabled', value: false }));
    });

    it('renders notes field from hook', async () => {
      mockUseCapabilitiesForm.mockReturnValue({
        form: createMockCapForm({ typeId: 'simple-capability', enabled: true, notes: 'Test notes', properties: {} }),
        capability: null, capabilityTypes: [], selectedCapabilityType: null, isCreate: true,
      } as any);

      renderWithTheme(
        <DynamicCapabilityForm capabilityType={simpleCapabilityType} isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} />
      );

      expect(screen.getByDisplayValue('Test notes')).toBeInTheDocument();
    });

    it('calls form.send on boolean field toggle', async () => {
      const mockSend = jest.fn();
      mockUseCapabilitiesForm.mockReturnValue({
        form: { ...createMockCapForm({ typeId: 'test-all-fields', enabled: true, notes: '', properties: { booleanField: 'true', numberField: '100' } }), send: mockSend },
        capability: null, capabilityTypes: [], selectedCapabilityType: null, isCreate: true,
      } as any);

      renderWithTheme(
        <DynamicCapabilityForm capabilityType={mockCapabilityTypeWithAllFields} isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} />
      );

      const booleanField = screen.getByLabelText(/Boolean Field/i);
      expect(booleanField).toBeChecked();

      fireEvent.click(booleanField);
      expect(mockSend).toHaveBeenCalledWith(expect.objectContaining({ type: 'UPDATE', name: 'properties' }));
    });
  });

  describe('form validation', () => {
    it('shows validation error for empty required field', async () => {
      mockUseCapabilitiesForm.mockReturnValue({
        form: { ...createMockCapForm({ typeId: 'simple-capability', enabled: true, notes: '', properties: {} }),
          validationErrors: { 'properties.repository': 'Repository is required' },
          touched: { properties: true } },
        capability: null, capabilityTypes: [], selectedCapabilityType: null, isCreate: true,
      } as any);

      renderWithTheme(
        <DynamicCapabilityForm capabilityType={simpleCapabilityType} isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} />
      );

      expect(screen.getByText('Repository is required')).toBeInTheDocument();
    });

    it('shows no validation error when field has value', async () => {
      mockUseCapabilitiesForm.mockReturnValue({
        form: createMockCapForm({ typeId: 'simple-capability', enabled: true, notes: '', properties: { repository: 'test-repo', interval: '60' } }),
        capability: null, capabilityTypes: [], selectedCapabilityType: null, isCreate: true,
      } as any);

      renderWithTheme(
        <DynamicCapabilityForm capabilityType={simpleCapabilityType} isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} />
      );

      expect(screen.queryByText('Repository is required')).not.toBeInTheDocument();
    });

    it('shows validation error for empty required number field', async () => {
      const numberOnlyType: CapabilityType = { id: 'number-only', name: 'Number Only', about: '', formFields: [{ id: 'count', type: 'number', label: 'Count', required: true }] };
      mockUseCapabilitiesForm.mockReturnValue({
        form: { ...createMockCapForm({ typeId: 'number-only', enabled: true, notes: '', properties: {} }),
          validationErrors: { 'properties.count': 'Count is required' },
          touched: { properties: true } },
        capability: null, capabilityTypes: [], selectedCapabilityType: null, isCreate: true,
      } as any);

      renderWithTheme(<DynamicCapabilityForm capabilityType={numberOnlyType} isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} />);
      expect(screen.getByText('Count is required')).toBeInTheDocument();
    });

    it('shows validation error for number field minimum value', async () => {
      const minValueType: CapabilityType = { id: 'min-value', name: 'Min Value', about: '', formFields: [{ id: 'count', type: 'number', label: 'Count', required: true, minValue: 10 }] };
      mockUseCapabilitiesForm.mockReturnValue({
        form: { ...createMockCapForm({ typeId: 'min-value', enabled: true, notes: '', properties: { count: '5' } }),
          validationErrors: { 'properties.count': 'Count must be at least 10' },
          touched: { properties: true } },
        capability: null, capabilityTypes: [], selectedCapabilityType: null, isCreate: true,
      } as any);

      renderWithTheme(<DynamicCapabilityForm capabilityType={minValueType} isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} />);
      expect(screen.getByText('Count must be at least 10')).toBeInTheDocument();
    });

    it('shows validation error for number field maximum value', async () => {
      const maxValueType: CapabilityType = { id: 'max-value', name: 'Max Value', about: '', formFields: [{ id: 'count', type: 'number', label: 'Count', required: true, maxValue: 100 }] };
      mockUseCapabilitiesForm.mockReturnValue({
        form: { ...createMockCapForm({ typeId: 'max-value', enabled: true, notes: '', properties: { count: '150' } }),
          validationErrors: { 'properties.count': 'Count must be at most 100' },
          touched: { properties: true } },
        capability: null, capabilityTypes: [], selectedCapabilityType: null, isCreate: true,
      } as any);

      renderWithTheme(<DynamicCapabilityForm capabilityType={maxValueType} isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} />);
      expect(screen.getByText('Count must be at most 100')).toBeInTheDocument();
    });

    it('shows validation error for regex pattern', async () => {
      const regexType: CapabilityType = { id: 'regex-type', name: 'Regex Type', about: '', formFields: [{ id: 'email', type: 'string', label: 'Email', required: false, regexValidation: '^[a-z]+@[a-z]+\\.[a-z]+$' }] };
      mockUseCapabilitiesForm.mockReturnValue({
        form: { ...createMockCapForm({ typeId: 'regex-type', enabled: true, notes: '', properties: { email: 'invalid-email' } }),
          validationErrors: { 'properties.email': 'Email is invalid' },
          touched: { properties: true } },
        capability: null, capabilityTypes: [], selectedCapabilityType: null, isCreate: true,
      } as any);

      renderWithTheme(<DynamicCapabilityForm capabilityType={regexType} isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} />);
      expect(screen.getByText('Email is invalid')).toBeInTheDocument();
    });

    it('renders without validation error when regex pattern is invalid', async () => {
      const invalidRegexType: CapabilityType = { id: 'invalid-regex', name: 'Invalid Regex', about: '', formFields: [{ id: 'field', type: 'string', label: 'Field', required: false, regexValidation: '[invalid(regex' }] };
      mockUseCapabilitiesForm.mockReturnValue({
        form: createMockCapForm({ typeId: 'invalid-regex', enabled: true, notes: '', properties: { field: 'some value' } }),
        capability: null, capabilityTypes: [], selectedCapabilityType: null, isCreate: true,
      } as any);

      renderWithTheme(<DynamicCapabilityForm capabilityType={invalidRegexType} isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} />);
      expect(screen.queryByText(/is invalid/i)).not.toBeInTheDocument();
    });
  });

  describe('form submission', () => {
    it('calls form.send SUBMIT on valid submission', async () => {
      const mockSend = jest.fn();
      mockUseCapabilitiesForm.mockReturnValue({
        form: { ...createMockCapForm({ typeId: 'simple-capability', enabled: true, notes: '', properties: { repository: 'test-repo', interval: '60' } }),
          isPristine: false, send: mockSend },
        capability: null, capabilityTypes: [], selectedCapabilityType: null, isCreate: true,
      } as any);

      renderWithTheme(
        <DynamicCapabilityForm capabilityType={simpleCapabilityType} isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} />
      );

      fireEvent.click(screen.getByRole('button', { name: 'Create' }));
      expect(mockSend).toHaveBeenCalledWith('SUBMIT');
    });

    it('renders edit form with existing capability data', async () => {
      renderWithTheme(
        <DynamicCapabilityForm capabilityType={simpleCapabilityType} capability={existingCapability} isCreate={false} onSave={mockOnSave} onCancel={mockOnCancel} />
      );

      // Form content should be visible
      expect(screen.getByDisplayValue('maven-central')).toBeInTheDocument();
      // Save button is hidden when form is pristine (showActionsOnlyWhenDirty behavior)
      expect(screen.queryByRole('button', { name: 'Save' })).not.toBeInTheDocument();
    });

    it('shows Save button when form is dirty in edit mode', async () => {
      mockUseCapabilitiesForm.mockReturnValue({
        form: { ...createMockCapForm({ typeId: 'simple-capability', enabled: true, notes: '', properties: { repository: 'maven-central', interval: '60' } }),
          isPristine: false },
        capability: existingCapability, capabilityTypes: [], selectedCapabilityType: null, isCreate: false,
      } as any);

      renderWithTheme(
        <DynamicCapabilityForm capabilityType={simpleCapabilityType} capability={existingCapability} isCreate={false} onSave={mockOnSave} onCancel={mockOnCancel} />
      );

      // When showActionsOnlyWhenDirty is true and form is dirty, Save appears in both top and bottom action bars
      const saveButtons = screen.getAllByRole('button', { name: 'Save' });
      expect(saveButtons.length).toBeGreaterThanOrEqual(1);
    });

    it('renders notes from form data', async () => {
      mockUseCapabilitiesForm.mockReturnValue({
        form: createMockCapForm({ typeId: 'simple-capability', enabled: true, notes: 'Test notes', properties: { repository: 'test-repo', interval: '60' } }),
        capability: null, capabilityTypes: [], selectedCapabilityType: null, isCreate: true,
      } as any);

      renderWithTheme(
        <DynamicCapabilityForm capabilityType={simpleCapabilityType} isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} />
      );

      expect(screen.getByDisplayValue('Test notes')).toBeInTheDocument();
    });

    it('renders enabled state from form data', async () => {
      mockUseCapabilitiesForm.mockReturnValue({
        form: createMockCapForm({ typeId: 'simple-capability', enabled: false, notes: '', properties: { repository: 'test-repo', interval: '60' } }),
        capability: null, capabilityTypes: [], selectedCapabilityType: null, isCreate: true,
      } as any);

      renderWithTheme(
        <DynamicCapabilityForm capabilityType={simpleCapabilityType} isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} />
      );

      const enabledCheckbox = screen.getByLabelText(/Enable this capability/i);
      expect(enabledCheckbox).not.toBeChecked();
    });
  });

  describe('disabled and read-only fields', () => {
    it('disables fields marked as disabled', () => {
      const disabledFieldType: CapabilityType = {
        id: 'disabled-field',
        name: 'Disabled Field',
        about: '',
        formFields: [
          {
            id: 'disabledField',
            type: 'string',
            label: 'Disabled Field',
            required: false,
            disabled: true,
          },
        ],
      };

      renderWithTheme(
        <DynamicCapabilityForm
          capabilityType={disabledFieldType}
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByLabelText(/Disabled Field/i)).toBeDisabled();
    });

    it('disables fields marked as read-only', () => {
      const readOnlyFieldType: CapabilityType = {
        id: 'readonly-field',
        name: 'Read Only Field',
        about: '',
        formFields: [
          {
            id: 'readOnlyField',
            type: 'string',
            label: 'Read Only Field',
            required: false,
            readOnly: true,
          },
        ],
      };

      renderWithTheme(
        <DynamicCapabilityForm
          capabilityType={readOnlyFieldType}
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByLabelText(/Read Only Field/i)).toBeDisabled();
    });
  });

  describe('capability type without about', () => {
    it('renders without about text', () => {
      const noAboutType: CapabilityType = {
        id: 'no-about',
        name: 'No About',
        formFields: [],
      };

      renderWithTheme(
        <DynamicCapabilityForm
          capabilityType={noAboutType}
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByText('No About')).toBeInTheDocument();
    });
  });

  describe('capability type sync (bug 7dcn)', () => {
    it('sends CAPABILITY_TYPE_CHANGE to sync typeId with machine on create', () => {
      const mockSend = jest.fn();
      mockUseCapabilitiesForm.mockReturnValue({
        form: { ...createMockCapForm({ typeId: '', enabled: true, notes: '', properties: {} }), send: mockSend, isLoading: false },
        capability: null, capabilityTypes: [], selectedCapabilityType: null, isCreate: true,
      } as any);

      renderWithTheme(
        <DynamicCapabilityForm
          capabilityType={simpleCapabilityType}
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />
      );

      expect(mockSend).toHaveBeenCalledWith({
        type: 'CAPABILITY_TYPE_CHANGE',
        value: 'simple-capability',
      });
    });

    it('does not send CAPABILITY_TYPE_CHANGE when typeId already matches', () => {
      const mockSend = jest.fn();
      mockUseCapabilitiesForm.mockReturnValue({
        form: { ...createMockCapForm({ typeId: 'simple-capability', enabled: true, notes: '', properties: {} }), send: mockSend, isLoading: false },
        capability: null, capabilityTypes: [], selectedCapabilityType: null, isCreate: true,
      } as any);

      renderWithTheme(
        <DynamicCapabilityForm
          capabilityType={simpleCapabilityType}
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />
      );

      expect(mockSend).not.toHaveBeenCalledWith(
        expect.objectContaining({ type: 'CAPABILITY_TYPE_CHANGE' })
      );
    });
  });

  describe('capability type changes', () => {
    it('resets form when capability type changes', () => {
      const { rerender } = renderWithTheme(
        <DynamicCapabilityForm
          capabilityType={simpleCapabilityType}
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />
      );

      const differentType: CapabilityType = {
        id: 'different-type',
        name: 'Different Type',
        about: '',
        formFields: [
          {
            id: 'newField',
            type: 'string',
            label: 'New Field',
            required: true,
          },
        ],
      };

      rerender(
        <Theme>
          <DynamicCapabilityForm
            capabilityType={differentType}
            isCreate={true}
            onSave={mockOnSave}
            onCancel={mockOnCancel}
          />
        </Theme>
      );

      expect(screen.getByText('Different Type')).toBeInTheDocument();
      expect(screen.getByLabelText(/New Field/i)).toBeInTheDocument();
    });
  });

  describe('field without initialValue uses empty default', () => {
    it('uses empty string for string field without initialValue', () => {
      const noInitialType: CapabilityType = {
        id: 'no-initial',
        name: 'No Initial',
        about: '',
        formFields: [
          {
            id: 'emptyField',
            type: 'string',
            label: 'Empty Field',
            required: false,
          },
        ],
      };

      renderWithTheme(
        <DynamicCapabilityForm
          capabilityType={noInitialType}
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByLabelText(/Empty Field/i)).toHaveValue('');
    });

    it('uses false for boolean field without initialValue', async () => {
      const booleanNoInitialType: CapabilityType = {
        id: 'boolean-no-initial',
        name: 'Boolean No Initial',
        about: '',
        formFields: [
          {
            id: 'boolField',
            type: 'boolean',
            label: 'Bool Field',
            required: false,
          },
        ],
      };

      renderWithTheme(
        <DynamicCapabilityForm
          capabilityType={booleanNoInitialType}
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByLabelText(/Bool Field/i)).not.toBeChecked();
    });
  });
});
