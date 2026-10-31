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
import { ToastProvider } from '../../../../../shared';

import { CapabilitiesPage } from '../CapabilitiesPage';
import * as useCapabilitiesApiModule from '../useCapabilitiesApi';

// Mock the API hook
jest.mock('../useCapabilitiesApi', () => ({
  useCapabilitiesApi: jest.fn(),
}));

// Mock router hooks
const mockGo = jest.fn();
let mockRouteName = 'preview.admin.system.capabilities.list';
let mockRouteParams: Record<string, string> = {};

jest.mock('@uirouter/react', () => ({
  useRouter: () => ({
    stateService: { go: mockGo },
  }),
  useCurrentStateAndParams: () => ({
    state: { name: mockRouteName },
    params: mockRouteParams,
  }),
}));

const mockedUseCapabilitiesApi = useCapabilitiesApiModule.useCapabilitiesApi as jest.MockedFunction<
  typeof useCapabilitiesApiModule.useCapabilitiesApi
>;

// Mock child components
jest.mock('../CapabilitiesList', () => ({
  CapabilitiesList: function MockCapabilitiesList({
    onSelect,
  }: {
    onSelect: (capability: any) => void;
  }) {
    return (
      <div data-testid="capabilities-list">
        <button
          onClick={() =>
            onSelect({
              id: 'cap-1',
              typeId: 'outreach',
              typeName: 'Outreach',
              enabled: true,
              active: true,
              error: false,
              state: 'active',
              stateDescription: 'Active',
              description: 'Outreach capability',
              notes: '',
              properties: {},
            })
          }
        >
          Select Capability
        </button>
      </div>
    );
  },
}));

jest.mock('../CapabilityTypeSelector', () => ({
  CapabilityTypeSelector: function MockCapabilityTypeSelector({
    onSelect,
  }: {
    onSelect: (type: any) => void;
  }) {
    return (
      <div data-testid="capability-type-selector">
        <button
          onClick={() =>
            onSelect({
              id: 'outreach',
              name: 'Outreach',
              about: 'Outreach',
              formFields: [],
            })
          }
        >
          Select Type
        </button>
      </div>
    );
  },
}));

jest.mock('../DynamicCapabilityForm', () => ({
  DynamicCapabilityForm: function MockDynamicCapabilityForm({
    onSave,
    onCancel,
    onSaveComplete,
  }: {
    onSave: (data: any) => void;
    onCancel: () => void;
    onSaveComplete?: () => void;
  }) {
    return (
      <div data-testid="capability-form">
        <button
          onClick={() =>
            onSave({
              typeId: 'outreach',
              enabled: true,
              notes: 'Test',
              properties: {},
            })
          }
        >
          Save
        </button>
        <button data-testid="mock-save-complete" onClick={() => onSaveComplete?.()}>
          Save Complete
        </button>
        <button onClick={onCancel}>Cancel</button>
      </div>
    );
  },
}));

jest.mock('../CapabilityDetail', () => ({
  CapabilityDetail: function MockCapabilityDetail({
    onEdit,
    onDelete,
    onBack,
    onEnable,
    onDisable,
  }: {
    onEdit: () => void;
    onDelete: () => void;
    onBack: () => void;
    onEnable: () => void;
    onDisable: () => void;
  }) {
    return (
      <div data-testid="capability-detail">
        <button onClick={onEdit}>Edit</button>
        <button onClick={onDelete}>Delete</button>
        <button onClick={onBack}>Back</button>
        <button onClick={onEnable}>Enable</button>
        <button onClick={onDisable}>Disable</button>
      </div>
    );
  },
}));

jest.mock('../../../../../shared/form', () => {
  const React = require('react');
  const actual = jest.requireActual('../../../../../shared/form');
  return {
    ...actual,
    WizardForm: function MockWizardForm({
      children,
      onComplete,
      onCancel,
      onStepChange,
      currentStep,
      testId,
    }: {
      children: React.ReactNode;
      onComplete: () => void;
      onCancel: () => void;
      onStepChange?: (step: number) => void;
      currentStep: number;
      testId?: string;
    }) {
      return React.createElement('div', { 'data-testid': testId || 'wizard-form', 'data-step': currentStep },
        children,
        onStepChange && React.createElement('button', { onClick: () => onStepChange(currentStep + 1), 'data-testid': 'wizard-next' }, 'Next'),
        React.createElement('button', { onClick: onComplete, 'data-testid': 'wizard-complete' }, 'Complete'),
        React.createElement('button', { onClick: onCancel, 'data-testid': 'wizard-cancel' }, 'Cancel')
      );
    },
  };
});

// Mock ExtJS
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    checkPermission: jest.fn().mockReturnValue(true),
  },
  APIConstants: {
    EXT: {
      CAPABILITY: {
        ACTION: 'capability_Capability',
        METHODS: {
          READ: 'read',
          READ_TYPES: 'readTypes',
          CREATE: 'create',
          UPDATE: 'update',
          REMOVE: 'remove',
          ENABLE: 'enable',
          DISABLE: 'disable',
        },
      },
    },
    REST: { INTERNAL: {}, PUBLIC: {} },
  },
}));

function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme><ToastProvider>{children}</ToastProvider></Theme>;
}

describe('CapabilitiesPage', () => {
  const mockFetchCapability = jest.fn();
  const mockFetchCapabilityTypes = jest.fn();
  const mockCreateCapability = jest.fn();
  const mockUpdateCapability = jest.fn();
  const mockDeleteCapability = jest.fn();
  const mockEnableCapability = jest.fn();
  const mockDisableCapability = jest.fn();
  const mockSetError = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    // clearAllMocks() resets call history but not custom implementations; reset the
    // router mock so a per-test mockImplementation cannot leak into other tests.
    mockGo.mockReset();
    mockRouteName = 'preview.admin.system.capabilities.list';
    mockRouteParams = {};
    mockedUseCapabilitiesApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchCapabilities: jest.fn().mockResolvedValue([]),
      fetchCapabilityTypes: mockFetchCapabilityTypes.mockResolvedValue([
        {
          id: 'outreach',
          name: 'Outreach: Management',
          about: 'Enables outreach features',
          formFields: [],
        },
      ]),
      fetchCapability: mockFetchCapability.mockResolvedValue({
        id: 'cap-1',
        typeId: 'outreach',
        typeName: 'Outreach: Management',
        enabled: true,
        active: true,
        notes: 'Test capability',
        properties: {},
        tags: {},
        state: 'ACTIVE',
        stateDescription: 'Active',
      }),
      createCapability: mockCreateCapability.mockResolvedValue({}),
      updateCapability: mockUpdateCapability.mockResolvedValue({}),
      deleteCapability: mockDeleteCapability.mockResolvedValue({}),
      enableCapability: mockEnableCapability.mockResolvedValue({}),
      disableCapability: mockDisableCapability.mockResolvedValue({}),
    });
  });

  it('renders the capabilities list by default', () => {
    render(<CapabilitiesPage />, { wrapper: TestWrapper });

    expect(screen.getByTestId('capabilities-list')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Capabilities' })).toBeInTheDocument();
    expect(screen.getByText('Manage repository manager capabilities and plugins')).toBeInTheDocument();
  });

  it('navigates to create route when Create Capability is clicked', () => {
    render(<CapabilitiesPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByRole('button', { name: 'Create Capability' }));

    expect(mockGo).toHaveBeenCalledWith('preview.admin.system.capabilities.create');
  });

  it('shows type selector when route is .create', () => {
    mockRouteName = 'preview.admin.system.capabilities.create';

    render(<CapabilitiesPage />, { wrapper: TestWrapper });

    expect(screen.getByTestId('capability-wizard')).toBeInTheDocument();
    expect(screen.getByTestId('capability-type-selector')).toBeInTheDocument();
  });

  it('navigates to createWithType route when a type is selected and wizard advances', () => {
    mockRouteName = 'preview.admin.system.capabilities.create';

    render(<CapabilitiesPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Select Type'));

    fireEvent.click(screen.getByTestId('wizard-next'));

    expect(mockGo).toHaveBeenCalledWith(
      'preview.admin.system.capabilities.createWithType',
      { typeId: 'outreach' }
    );
  });

  it('navigates to detail route when a capability is selected', () => {
    render(<CapabilitiesPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Select Capability'));

    expect(mockGo).toHaveBeenCalledWith(
      'preview.admin.system.capabilities.detail',
      { capabilityId: 'cap-1' }
    );
  });

  it('navigates to list route when Cancel is clicked in wizard', () => {
    mockRouteName = 'preview.admin.system.capabilities.create';

    render(<CapabilitiesPage />, { wrapper: TestWrapper });

    const cancelButton = screen.getByTestId('wizard-cancel');
    fireEvent.click(cancelButton);

    expect(mockGo).toHaveBeenCalledWith('preview.admin.system.capabilities.list');
  });

  it('navigates to list (not type selection) after a capability is created (NEXUS-53839)', async () => {
    mockRouteName = 'preview.admin.system.capabilities.createWithType';
    mockRouteParams = { typeId: 'outreach' };

    const { rerender } = render(<CapabilitiesPage />, { wrapper: TestWrapper });

    // Emulate the real ui-router: go() applies the route change asynchronously.
    // Until it settles, the component keeps rendering the previous route. This is
    // what allows the deep-link effect to re-enter the create flow when handleBack
    // clears selectedType while the route is still .createWithType.
    mockGo.mockImplementation((name: string, params?: Record<string, string>) => {
      Promise.resolve().then(() => {
        mockRouteName = name;
        mockRouteParams = params ?? {};
        rerender(<Theme><ToastProvider><CapabilitiesPage /></ToastProvider></Theme>);
      });
    });

    // Wait for the type to load and the form to render
    const saveComplete = await screen.findByTestId('mock-save-complete');

    // Simulate the form machine reporting a completed save (post-create navigation)
    fireEvent.click(saveComplete);

    // After creation we must land on the capabilities list, never bounce back
    // to the type-selection (.create) or the create wizard (.createWithType).
    await waitFor(() => {
      expect(mockRouteName).toBe('preview.admin.system.capabilities.list');
    });
    expect(mockGo).not.toHaveBeenCalledWith('preview.admin.system.capabilities.create');
    expect(mockGo).not.toHaveBeenCalledWith(
      'preview.admin.system.capabilities.createWithType',
      expect.anything()
    );
    // The list must be shown, not the type selector / wizard.
    expect(screen.getByTestId('capabilities-list')).toBeInTheDocument();
    expect(screen.queryByTestId('capability-type-selector')).not.toBeInTheDocument();
  });

  it('does not re-enter the create flow when navigation is still settling after create (NEXUS-53839)', async () => {
    // Reproduces the deep-link effect re-entry: the route is still .createWithType
    // (navigation not yet applied) but the save has completed and selectedType has
    // been cleared. The effect must NOT re-fetch the type and re-enter the wizard.
    mockRouteName = 'preview.admin.system.capabilities.createWithType';
    mockRouteParams = { typeId: 'outreach' };

    // handleBack() will call go('.list'); keep the route unchanged to emulate the
    // in-flight window where useCurrentStateAndParams has not updated yet.
    mockGo.mockImplementation(() => {});

    render(<CapabilitiesPage />, { wrapper: TestWrapper });

    // Effect loads the type (deep-link entry), advancing to the config step.
    await screen.findByTestId('mock-save-complete');
    // Type is fetched exactly once for the genuine deep-link entry.
    expect(mockFetchCapabilityTypes).toHaveBeenCalledTimes(1);

    // Simulate save completion: handleBack clears selectedType while route is still
    // .createWithType. Without the createStep guard, the effect re-fires and calls
    // fetchCapabilityTypes again to rebuild the wizard.
    fireEvent.click(screen.getByTestId('mock-save-complete'));

    await waitFor(() => {
      expect(mockGo).toHaveBeenCalledWith('preview.admin.system.capabilities.list');
    });
    // The effect must not have re-fetched types to re-enter the create flow.
    expect(mockFetchCapabilityTypes).toHaveBeenCalledTimes(1);
  });

  it('displays page header with icon and description', () => {
    render(<CapabilitiesPage />, { wrapper: TestWrapper });

    expect(screen.getByRole('heading', { name: 'Capabilities' })).toBeInTheDocument();
    expect(screen.getByText('Manage repository manager capabilities and plugins')).toBeInTheDocument();
  });

  it('handles loading state', () => {
    mockedUseCapabilitiesApi.mockReturnValue({
      loading: true,
      error: null,
      setError: mockSetError,
      fetchCapabilities: jest.fn(),
      fetchCapabilityTypes: mockFetchCapabilityTypes,
      fetchCapability: mockFetchCapability,
      createCapability: mockCreateCapability,
      updateCapability: mockUpdateCapability,
      deleteCapability: mockDeleteCapability,
      enableCapability: mockEnableCapability,
      disableCapability: mockDisableCapability,
    });

    render(<CapabilitiesPage />, { wrapper: TestWrapper });

    // Page should still render
    expect(screen.getByRole('heading', { name: 'Capabilities' })).toBeInTheDocument();
  });

  it('handles error state', () => {
    mockedUseCapabilitiesApi.mockReturnValue({
      loading: false,
      error: 'Failed to load capabilities',
      setError: mockSetError,
      fetchCapabilities: jest.fn(),
      fetchCapabilityTypes: mockFetchCapabilityTypes,
      fetchCapability: mockFetchCapability,
      createCapability: mockCreateCapability,
      updateCapability: mockUpdateCapability,
      deleteCapability: mockDeleteCapability,
      enableCapability: mockEnableCapability,
      disableCapability: mockDisableCapability,
    });

    render(<CapabilitiesPage />, { wrapper: TestWrapper });

    // Error is now handled via Toast notifications (Sprint 15)
    // Page should still render even when API has error
    expect(screen.getByRole('heading', { name: 'Capabilities' })).toBeInTheDocument();
  });

  it('derives create viewMode from .createWithType route', () => {
    mockRouteName = 'preview.admin.system.capabilities.createWithType';
    mockRouteParams = { typeId: 'outreach' };

    render(<CapabilitiesPage />, { wrapper: TestWrapper });

    expect(screen.getByTestId('capability-wizard')).toBeInTheDocument();
    expect(screen.queryByTestId('capabilities-list')).not.toBeInTheDocument();
  });

  it('derives detail viewMode from .detail route', () => {
    mockRouteName = 'preview.admin.system.capabilities.detail';
    mockRouteParams = { capabilityId: 'cap-1' };

    render(<CapabilitiesPage />, { wrapper: TestWrapper });

    expect(screen.queryByTestId('capabilities-list')).not.toBeInTheDocument();
    expect(screen.queryByTestId('capability-type-selector')).not.toBeInTheDocument();
  });

  it('navigates to list after selecting then back', () => {
    render(<CapabilitiesPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Select Capability'));

    expect(mockGo).toHaveBeenCalledWith(
      'preview.admin.system.capabilities.detail',
      { capabilityId: 'cap-1' }
    );
  });

  describe('breadcrumb navigation', () => {
    it('renders breadcrumbs for list view', () => {
      render(<CapabilitiesPage />, { wrapper: TestWrapper });

      // Breadcrumbs: Settings (clickable) > Capabilities (current page)
      expect(screen.getByRole('button', { name: 'Settings' })).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: 'Capabilities' })).toBeInTheDocument();
    });

    it('clicking Settings breadcrumb navigates to settings page', () => {
      render(<CapabilitiesPage />, { wrapper: TestWrapper });

      screen.getByRole('button', { name: 'Settings' }).click();
      expect(window.location.hash).toBe('#preview/admin/settings');
    });

    it('renders breadcrumbs for create/selectType view', () => {
      mockRouteName = 'preview.admin.system.capabilities.create';

      render(<CapabilitiesPage />, { wrapper: TestWrapper });

      // Breadcrumbs: Settings (clickable) > Capabilities (clickable) > Create (current page)
      expect(screen.getByRole('button', { name: 'Settings' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Capabilities' })).toBeInTheDocument();
      expect(screen.getByText('Create')).toBeInTheDocument();
    });

    it('renders breadcrumbs for createWithType view', () => {
      mockRouteName = 'preview.admin.system.capabilities.createWithType';
      mockRouteParams = { typeId: 'outreach' };

      render(<CapabilitiesPage />, { wrapper: TestWrapper });

      // Breadcrumbs: Settings (clickable) > Capabilities (clickable) > Create (current page)
      expect(screen.getByRole('button', { name: 'Settings' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Capabilities' })).toBeInTheDocument();
      expect(screen.getByText('Create')).toBeInTheDocument();
    });

    it('renders breadcrumbs for detail view with capability type name', async () => {
      mockRouteName = 'preview.admin.system.capabilities.detail';
      mockRouteParams = { capabilityId: 'cap-1' };

      mockedUseCapabilitiesApi.mockReturnValue({
        loading: false,
        error: null,
        setError: mockSetError,
        fetchCapabilities: jest.fn().mockResolvedValue([
          {
            id: 'cap-1',
            typeId: 'outreach',
            typeName: 'Outreach: Management',
            enabled: true,
            active: true,
            error: false,
            state: 'ACTIVE',
            stateDescription: 'Active',
            description: 'Outreach capability',
            notes: '',
            properties: {},
          },
        ]),
        fetchCapabilityTypes: mockFetchCapabilityTypes,
        fetchCapability: mockFetchCapability,
        createCapability: mockCreateCapability,
        updateCapability: mockUpdateCapability,
        deleteCapability: mockDeleteCapability,
        enableCapability: mockEnableCapability,
        disableCapability: mockDisableCapability,
      });

      render(<CapabilitiesPage />, { wrapper: TestWrapper });

      // Breadcrumbs: Settings (clickable) > Capabilities (clickable) > {typeName} (current page)
      expect(screen.getByRole('button', { name: 'Settings' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Capabilities' })).toBeInTheDocument();
      // The last breadcrumb should be the capability type name (after async fetch resolves)
      await waitFor(() => {
        expect(document.querySelector('[aria-current="page"]')?.textContent).toBe('Outreach: Management');
      });
    });
  });
});

