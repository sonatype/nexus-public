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
import { screen, within, cleanup, render, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as ExtAPIUtils from '../../../interface/ExtAPIUtils';
import Axios from 'axios';
import APIConstants from '../../../constants/APIConstants';
import { AdminRouteNames } from '../../../constants/admin/AdminRouteNames';

import CapabilitiesEdit from './CapabilitiesEdit';
import ExtJS from '../../../interface/ExtJS';
import UIStrings from '../../../constants/UIStrings';

// Mock Axios
jest.mock('axios');

// Mock ExtJS
jest.mock('../../../interface/ExtJS');

// Mock route params for @uirouter/react
const mockGo = jest.fn();
const mockRouter = {
  stateService: {
    go: mockGo,
  },
};

jest.mock('@uirouter/react', () => ({
  useCurrentStateAndParams: () => ({ params: { id: '123' } }),
  useRouter: () => mockRouter,
}));

// Mock NxModal to avoid portal/focus-trap/animation scheduling issues in tests
jest.mock('@sonatype/react-shared-components', () => {
  const actual = jest.requireActual('@sonatype/react-shared-components');

  const TestModal = ({ onCancel, 'aria-labelledby': ariaLabelledby, children }) => (
    <div role='dialog' aria-labelledby={ariaLabelledby} data-testid='nx-modal'>
      {children}
      {/* Optional close control to mimic ESC/backdrop if you need it */}
      <button type='button' onClick={onCancel} aria-label='__modal-close__' style={{ display: 'none' }} />
    </div>
  );
  TestModal.displayName = 'TestModal';
  TestModal.Header = ({ children }) => <div>{children}</div>;
  TestModal.Header.displayName = 'TestModal.Header';
  TestModal.Content = ({ children }) => <div>{children}</div>;
  TestModal.Content.displayName = 'TestModal.Content';

  return {
    ...actual,
    NxModal: TestModal,
  };
});

// Mock ExtAPIUtils for DynamicFormField
jest.mock('../../../interface/ExtAPIUtils', () => ({
  extAPIRequest: jest.fn(),
  checkForErrorAndExtract: jest.fn(response => response.data),
  checkForError: jest.fn(),
  extractResult: jest.fn((response, defaultValue) => response.data || defaultValue),
}));

const { ACTION, METHODS } = APIConstants.EXT.CAPABILITY;

const CAPABILITY_TYPES = [
  {
    id: "audit",
    name: "Audit",
    about:
      "<p>Audit capability for tracking repository access and changes.</p>",
    formFields: [],
  },
  {
    id: "baseurl",
    name: "Base URL",
    about: "<p>Base URL configuration for the repository.</p>",
    formFields: [
      {
        id: "baseUrl",
        type: "url",
        label: "Base URL",
        helpText: "The base URL for the repository.",
        required: true,
        disabled: false,
        readOnly: false,
        initialValue: null,
        attributes: {},
      },
    ],
  },
  {
    id: "scheduler",
    name: "Scheduler",
    about: "<p>Scheduler capability for managing tasks and jobs.</p>",
    formFields: [
      {
        id: "taskName",
        type: "string",
        label: "Task Name",
        helpText: "Name of the scheduled task",
        required: true,
        disabled: false,
        readOnly: false,
        initialValue: null,
        attributes: {},
      },
    ],
  },
  {
    id: "rapture.settings",
    name: "UI: Settings",
    about: "Customize user-interface settings.",
    formFields: [
      {
        id: "title",
        type: "string",
        label: "Title",
        helpText: "Browser page title",
        required: true,
        disabled: false,
        readOnly: false,
        regexValidation: null,
        initialValue: "Sonatype Nexus Repository",
        attributes: {},
        minValue: null,
        maxValue: null,
        storeApi: null,
        storeFilters: null,
        idMapping: null,
        nameMapping: null,
        allowAutocomplete: false,
      },
      {
        id: "debugAllowed",
        type: "checkbox",
        label: "Debug allowed",
        helpText: "Allow developer debugging",
        required: false,
        disabled: false,
        readOnly: false,
        regexValidation: null,
        initialValue: "true",
        attributes: {},
        minValue: null,
        maxValue: null,
        storeApi: null,
        storeFilters: null,
        idMapping: null,
        nameMapping: null,
        allowAutocomplete: false,
      },
      {
        id: "statusIntervalAuthenticated",
        type: "number",
        label: "Authenticated user status interval",
        helpText:
          "Interval between status requests for authenticated users (seconds)",
        required: true,
        disabled: false,
        readOnly: false,
        regexValidation: null,
        initialValue: "5",
        attributes: {},
        minValue: null,
        maxValue: null,
        storeApi: null,
        storeFilters: null,
        idMapping: null,
        nameMapping: null,
        allowAutocomplete: false,
      },
      {
        id: "statusIntervalAnonymous",
        type: "number",
        label: "Anonymous user status interval",
        helpText:
          "Interval between status requests for anonymous user (seconds)",
        required: true,
        disabled: false,
        readOnly: false,
        regexValidation: null,
        initialValue: "60",
        attributes: {},
        minValue: null,
        maxValue: null,
        storeApi: null,
        storeFilters: null,
        idMapping: null,
        nameMapping: null,
        allowAutocomplete: false,
      },
      {
        id: "sessionTimeout",
        type: "number",
        label: "Session timeout",
        helpText:
          "Period of inactivity before session times out (minutes). A value of 0 will mean that a session never expires.",
        required: true,
        disabled: false,
        readOnly: false,
        regexValidation: null,
        initialValue: "30",
        attributes: {},
        minValue: null,
        maxValue: null,
        storeApi: null,
        storeFilters: null,
        idMapping: null,
        nameMapping: null,
        allowAutocomplete: false,
      },
      {
        id: "requestTimeout",
        type: "number",
        label: "Standard request timeout",
        helpText:
          "Period of time to keep the connection alive for requests expected to take a normal period of time (seconds)",
        required: true,
        disabled: false,
        readOnly: false,
        regexValidation: null,
        initialValue: "60",
        attributes: {},
        minValue: "30",
        maxValue: null,
        storeApi: null,
        storeFilters: null,
        idMapping: null,
        nameMapping: null,
        allowAutocomplete: false,
      },
      {
        id: "longRequestTimeout",
        type: "number",
        label: "Extended request timeout",
        helpText:
          "Period of time to keep the connection alive for requests expected to take an extended period of time (seconds)",
        required: true,
        disabled: false,
        readOnly: false,
        regexValidation: null,
        initialValue: "180",
        attributes: {},
        minValue: "180",
        maxValue: null,
        storeApi: null,
        storeFilters: null,
        idMapping: null,
        nameMapping: null,
        allowAutocomplete: false,
      },
    ],
  },
  {
    id: "webhook.repository",
    name: "Webhook: Repository",
    about: "\nSend HTTP POST requests for <strong>repository</strong> events.",
    formFields: [
      {
        id: "repository",
        type: "combobox",
        label: "Repository",
        helpText: "Repository to discriminate events from",
        required: true,
        disabled: false,
        readOnly: false,
        regexValidation: null,
        initialValue: null,
        attributes: {},
        minValue: null,
        maxValue: null,
        storeApi: "coreui_Repository.readReferences",
        storeFilters: {
          type: "!group",
        },
        idMapping: null,
        nameMapping: null,
        allowAutocomplete: true,
      },
      {
        id: "names",
        type: "itemselect",
        label: "Event Types",
        helpText: "Event types which trigger this Webhook",
        required: true,
        disabled: false,
        readOnly: false,
        regexValidation: null,
        initialValue: null,
        attributes: {
          toTitle: "Selected",
          fromTitle: "Available",
          buttons: ["add", "remove"],
        },
        minValue: null,
        maxValue: null,
        storeApi: "coreui_Webhook.listWithTypeRepository",
        storeFilters: null,
        idMapping: null,
        nameMapping: null,
        allowAutocomplete: false,
      },
      {
        id: "url",
        type: "url",
        label: "URL",
        helpText: "Send a HTTP POST request to this URL",
        required: true,
        disabled: false,
        readOnly: false,
        regexValidation: null,
        initialValue: null,
        attributes: {},
        minValue: null,
        maxValue: null,
        storeApi: null,
        storeFilters: null,
        idMapping: null,
        nameMapping: null,
        allowAutocomplete: false,
      },
      {
        id: "secret",
        type: "password",
        label: "Secret Key",
        helpText: "Key to use for HMAC payload digest",
        required: false,
        disabled: false,
        readOnly: false,
        regexValidation: null,
        initialValue: null,
        attributes: {},
        minValue: null,
        maxValue: null,
        storeApi: null,
        storeFilters: null,
        idMapping: null,
        nameMapping: null,
        allowAutocomplete: false,
      },
    ],
  },
];

describe('CapabilitiesEdit', () => {
  // Stable geometry in jsdom (elements + ranges)
  const RECT = {
    x: 0,
    y: 0,
    top: 0,
    left: 0,
    bottom: 10,
    right: 100,
    width: 100,
    height: 10,
    toJSON: () => {},
  };

  const RECT_LIST = {
    length: 1,
    0: RECT,
    item: () => RECT,
    [Symbol.iterator]: function* () {
      yield RECT;
    },
  };

  beforeAll(() => {
    // Use fake timers to control async work from tooltip's updateBatcher
    jest.useFakeTimers();

    // Geometry shims for jsdom - ensure getBoundingClientRect always returns valid rect
    Element.prototype.getBoundingClientRect = jest.fn(() => RECT);
    HTMLElement.prototype.getBoundingClientRect = jest.fn(() => RECT);
    Element.prototype.getClientRects = jest.fn(() => RECT_LIST);

    // NxOverflowTooltip sometimes measures text via Range APIs
    if (global.Range) {
      Range.prototype.getBoundingClientRect = jest.fn(() => RECT);
      Range.prototype.getClientRects = jest.fn(() => RECT_LIST);
    }

    if (!global.ResizeObserver) {
      global.ResizeObserver = class {
        observe() {}
        unobserve() {}
        disconnect() {}
      };
    }
  });

  afterAll(() => {
    jest.useRealTimers();
  });
  const defaultMockImplementation = (action, method) => {
    if (action === ACTION && method === METHODS.READ) {
      return Promise.resolve({
        data: [
          {
            id: '123',
            typeId: 'audit',
            typeName: 'Audit',
            description: 'Enabled',
            enabled: true,
            properties: {},
          },
        ],
      });
    }
    if (action === ACTION && method === METHODS.READ_TYPES) {
      return Promise.resolve({ data: CAPABILITY_TYPES });
    }
    if (action === ACTION && method === METHODS.UPDATE) {
      return Promise.resolve({ data: { success: true } });
    }
    // For other methods, return success by default
    return Promise.resolve({ data: { success: true } });
  };

  beforeEach(() => {
    jest.clearAllMocks();
    // Reset router mock
    mockGo.mockClear();

    // Rewire default implementations for every test
    ExtAPIUtils.extAPIRequest.mockImplementation(defaultMockImplementation);
    ExtAPIUtils.checkForErrorAndExtract.mockImplementation(response => response.data);
    ExtAPIUtils.checkForError.mockImplementation(() => {});
    ExtAPIUtils.extractResult.mockImplementation((response, defaultValue) => response?.data ?? defaultValue);

    // Default permission check to true
    ExtJS.checkPermission.mockReturnValue(true);
  });

  afterEach(() => {
    cleanup();
  });

  const mockSuccessfulLoad = (capability, types = CAPABILITY_TYPES) => {
    // Mock READ call - returns array of capabilities
    ExtAPIUtils.extAPIRequest.mockImplementation((action, method) => {
      if (action === ACTION && method === METHODS.READ) {
        return Promise.resolve({ data: [capability] });
      }
      if (action === ACTION && method === METHODS.READ_TYPES) {
        // Handle both single object and array - wrap single object in array
        const typesArray = Array.isArray(types) ? types : [types];
        return Promise.resolve({ data: typesArray });
      }
      if (action === ACTION && method === METHODS.UPDATE) {
        return Promise.resolve({ data: { success: true } });
      }
      return Promise.resolve({ data: { success: true } });
    });
  };

  // Helper to create a basic capability object
  const createCapability = (overrides = {}) => ({
    id: '123',
    typeId: 'audit',
    typeName: 'Audit',
    description: 'Enabled',
    enabled: true,
    properties: {},
    ...overrides,
  });

  // Helper to create a baseurl capability
  const createBaseUrlCapability = (overrides = {}) =>
    createCapability({
      typeId: 'baseurl',
      typeName: 'Base URL',
      description: 'Test',
      properties: {
        baseUrl: 'https://example.com',
      },
      ...overrides,
    });

  // Helper to create a rapture.settings capability
  const createUISettingsCapability = (overrides = {}) =>
    createCapability({
      typeId: 'rapture.settings',
      typeName: 'UI: Settings',
      description: 'Test',
      properties: {
        title: 'test',
        debugAllowed: 'true',
        statusIntervalAuthenticated: '5',
        statusIntervalAnonymous: '100',
        sessionTimeout: '10',
        requestTimeout: '5',
        longRequestTimeout: '20',
      },
      ...overrides,
    });

  // Helper to create a webhook.repository capability
  const createWebhookRepositoryCapability = (overrides = {}) =>
    createCapability({
      typeId: 'webhook.repository',
      typeName: 'Webhook: Repository',
      description: 'Test',
      properties: {
        repository: 'npm-hosted',
        names: ['asset', 'component'],
        url: 'https://example.com/webhook',
        secret: 's3cr3t',
      },
      ...overrides,
    });

  // Helper to wait for capability to load and return the heading
  const waitForCapabilityToLoad = async expectedHeading => {
    const heading = await screen.findByRole('heading', { name: expectedHeading });
    return heading;
  };

  // Helper to interact with delete modal
  const openDeleteModal = async () => {
    const deleteButton = screen.getByRole('button', { name: 'Delete' });
    userEvent.click(deleteButton);
    const dialog = await screen.findByRole('dialog'); // waits until it exists
    expect(dialog).toBeVisible();
    expect(within(dialog).getByRole('heading', { name: 'Delete Capability?' })).toBeVisible();
    return dialog;
  };

  it('shows loading initially', async () => {
    ExtAPIUtils.extAPIRequest.mockImplementation(
      () => new Promise(resolve => setTimeout(() => resolve({ data: CAPABILITY_TYPES }), 100))
    );

    render(<CapabilitiesEdit />);

    // Should show loading state
    const loadingText = await screen.findByText(/loading/i);
    expect(loadingText).toBeVisible();
  });

  it('renders the summary tile based on the capability and capability type', async () => {
    const capabilityTypeAbout = 'This is a <strong>test</strong> capability type with HTML content';
    const capabilityStatus = '<span class="enabled">Enabled</span>';

    const capability = {
      id: '123',
      typeId: '932',
      typeName: 'Audit',
      description: 'Test Audit Capability',
      stateDescription: 'Active and running',
      status: capabilityStatus,
      state: 'test-state',
      tags: {
        Category: 'Security',
      },
    };

    const capabilityType = buildCapabilityType({
      id: '932',
      about: capabilityTypeAbout,
    });

    mockSuccessfulLoad(capability, capabilityType);

    render(<CapabilitiesEdit />);

    // Wait for title to load
    const heading = await screen.findByRole('heading', { name: 'Capability Summary' });
    expect(heading).toBeVisible();

    assertDefinitionRenderedCorrectly('Type', 'Audit');
    assertDefinitionRenderedCorrectly('Description', 'Test Audit Capability');
    assertDefinitionRenderedCorrectly('State', 'test-state');
    assertDefinitionRenderedCorrectly('Category', 'Security');

    // Verify Status field (rendered as HTML)
    const status = screen.getByRole('definition', { name: 'Status' });
    expect(status).toBeVisible();
    const statusElement = within(status).getByText('Enabled');
    expect(statusElement).toBeVisible();
    expect(statusElement.tagName).toBe('SPAN');
    expect(statusElement.className).toBe('enabled');

    // Verify About field (rendered as HTML from capability type)
    const about = screen.getByRole('definition', { name: 'About' });
    expect(about).toBeVisible();
    const aboutElement = within(about).getByText('test');
    expect(aboutElement).toBeVisible();
    expect(aboutElement.tagName).toBe('STRONG');
  });

  it('renders NA for missing data in the summary tile', async () => {
    const capability = {
      id: '123',
      typeId: '932',
      typeName: null,
      description: null,
      stateDescription: null,
      status: null,
      tags: {},
    };

    const capabilityType = buildCapabilityType({
      id: '932',
      about: null,
    });

    mockSuccessfulLoad(capability, capabilityType);

    render(<CapabilitiesEdit />);

    // Wait for title to load
    const heading = await screen.findByRole('heading', { name: 'Capability Summary' });
    expect(heading).toBeVisible();

    const tileLabels = screen.getAllByRole('term');
    const tileDefinitions = screen.getAllByRole('definition');

    expect(tileLabels.length).toEqual(6);
    expect(tileDefinitions.length).toEqual(tileLabels.length);

    tileDefinitions.forEach(tileDef => expect(tileDef.textContent).toBe('N/A'));
  });

  it('shows error message when the request fails', async () => {
    ExtAPIUtils.extAPIRequest.mockRejectedValue(new Error('Failed to fetch'));

    render(<CapabilitiesEdit />);

    // Wait for error alert to appear with propagated message
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('An error occurred loading data. Failed to fetch');
  });

  it('renders a composed title when capability is found', async () => {
    mockSuccessfulLoad(createCapability());
    render(<CapabilitiesEdit />);
    const heading = await waitForCapabilityToLoad('Audit - Enabled');
    expect(heading).toBeVisible();
  });

  it('renders a title from typeName only when capability is found but has no description', async () => {
    mockSuccessfulLoad(createCapability({ typeName: 'Some Type', description: undefined }));
    render(<CapabilitiesEdit />);
    const heading = await waitForCapabilityToLoad('Some Type');
    expect(heading).toBeVisible();
  });

  it('renders the form title', async () => {
    mockSuccessfulLoad(createCapability());
    render(<CapabilitiesEdit />);
    await waitForCapabilityToLoad('Audit - Enabled');
    const formTitle = screen.getByText('Capability Settings');
    expect(formTitle).toBeVisible();
  });

  it('renders enabled checkbox', async () => {
    mockSuccessfulLoad(createCapability());
    render(<CapabilitiesEdit />);
    await waitForCapabilityToLoad('Audit - Enabled');
    const checkbox = screen.getByRole('checkbox', { name: /Enable/i });
    expect(checkbox).toBeVisible();
    expect(checkbox).toBeChecked();
  });

  it('renders form fields when capability type has formFields', async () => {
    mockSuccessfulLoad(createBaseUrlCapability(), CAPABILITY_TYPES);
    render(<CapabilitiesEdit />);
    await waitForCapabilityToLoad('Base URL - Test');
    const baseUrlField = screen.getByLabelText('Base URL');
    expect(baseUrlField).toBeVisible();
    expect(baseUrlField).toHaveValue('https://example.com');
  });

  it('shows retry button when error occurs', async () => {
    ExtAPIUtils.extAPIRequest.mockRejectedValue(new Error('Failed to load'));

    render(<CapabilitiesEdit />);

    await screen.findByText(/An error occurred loading data/i);
    const retryButton = screen.getByRole('button', { name: /retry/i });
    expect(retryButton).toBeVisible();
  });

  it('retries loading on retry button click', async () => {
    ExtAPIUtils.extAPIRequest.mockRejectedValueOnce(new Error('Failed to load'));
    mockSuccessfulLoad(createCapability());
    render(<CapabilitiesEdit />);
    await screen.findByText(/An error occurred loading data/i);
    const retryButton = screen.getByRole('button', { name: /retry/i });
    userEvent.click(retryButton);
    await waitForCapabilityToLoad('Audit - Enabled');
    expect(screen.queryByText(/An error occurred loading data/i)).not.toBeInTheDocument();
  });

  it('should delete the capability and navigate to the capabilities list when confirmed', async () => {
    mockSuccessfulLoad(createCapability({ typeName: 'Test Capability', description: 'Test Description' }));
    Axios.delete.mockResolvedValue({ data: { success: true } });
    render(<CapabilitiesEdit />);
    await waitForCapabilityToLoad('Test Capability - Test Description');
    const deleteButton = screen.getByRole('button', { name: 'Delete' });
    expect(deleteButton).toBeVisible();
    expect(deleteButton).not.toBeDisabled();
    const dialog = await openDeleteModal();
    const confirmDeleteButton = within(dialog).getByRole('button', { name: 'Delete' });
    userEvent.click(confirmDeleteButton);
    await waitFor(() => {
      expect(mockGo).toHaveBeenCalledWith(AdminRouteNames.SYSTEM.CAPABILITIES.LIST);
    });
  });

  it('should show error alert when delete fails', async () => {
    mockSuccessfulLoad(createCapability({ typeName: 'Test Capability', description: 'Test Description' }));
    const deleteErrorMessage = 'Failed to delete capability due to network error';
    Axios.delete.mockRejectedValue(new Error(deleteErrorMessage));
    render(<CapabilitiesEdit />);
    await waitForCapabilityToLoad('Test Capability - Test Description');
    const dialog = await openDeleteModal();
    const confirmDeleteButton = within(dialog).getByRole('button', { name: 'Delete' });
    userEvent.click(confirmDeleteButton);
    const errorAlerts = await screen.findAllByRole('alert');
    const deleteErrorAlert = errorAlerts.find(alert => alert.textContent.includes(deleteErrorMessage));
    expect(deleteErrorAlert).toBeVisible();
    expect(deleteErrorAlert).toHaveTextContent(deleteErrorMessage);
    expect(mockGo).not.toHaveBeenCalled();
    const deleteButtonAfterError = screen.getByRole('button', { name: 'Delete' });
    expect(deleteButtonAfterError).not.toBeDisabled();
  });

  it('should close modal and not invoke delete endpoint when cancel is clicked', async () => {
    mockSuccessfulLoad(createCapability({ typeName: 'Test Capability', description: 'Test Description' }));
    render(<CapabilitiesEdit />);
    await waitForCapabilityToLoad('Test Capability - Test Description');
    const dialog = await openDeleteModal();
    const cancelButton = within(dialog).getByRole('button', { name: 'Cancel' });
    userEvent.click(cancelButton);
    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
    expect(Axios.delete).not.toHaveBeenCalled();
    expect(mockGo).not.toHaveBeenCalled();
    const deleteButtonAfterCancel = screen.getByRole('button', { name: 'Delete' });
    expect(deleteButtonAfterCancel).toBeVisible();
    expect(deleteButtonAfterCancel).not.toBeDisabled();
  });

  it('should display default warning message in delete modal when deleteWarningMessage is not set', async () => {
    mockSuccessfulLoad(createCapability({ typeName: 'Test Capability', description: 'Test Description' }));
    render(<CapabilitiesEdit />);
    await waitForCapabilityToLoad('Test Capability - Test Description');
    const dialog = await openDeleteModal();
    expect(
      within(dialog).getByText(/This will permanently remove this capability and its configuration/)
    ).toBeVisible();
  });

  it('should display custom warning message in delete modal when deleteWarningMessage is set', async () => {
    const customWarningMessage = 'Some other message';
    mockSuccessfulLoad(
      createCapability({
        typeName: 'Test Capability',
        description: 'Test Description',
        deleteWarningMessage: customWarningMessage,
      })
    );
    render(<CapabilitiesEdit />);
    await waitForCapabilityToLoad('Test Capability - Test Description');
    const dialog = await openDeleteModal();
    expect(within(dialog).getByText(customWarningMessage)).toBeVisible();
  });

  describe('form validation and submission', () => {
    it('does not show validation errors before submit is attempted', async () => {
      mockSuccessfulLoad(createBaseUrlCapability(), CAPABILITY_TYPES);
      render(<CapabilitiesEdit />);
      await waitForCapabilityToLoad('Base URL - Test');
      const submitButton = screen.getByRole('button', { name: /save/i });
      expect(submitButton).toBeVisible();
      expect(screen.queryByText(/This field is required/i)).not.toBeInTheDocument();
    });

    it('shows validation errors after submit is attempted with empty required field', async () => {
      mockSuccessfulLoad(createBaseUrlCapability(), CAPABILITY_TYPES);
      render(<CapabilitiesEdit />);
      await waitForCapabilityToLoad('Base URL - Test');
      const baseUrlField = screen.getByLabelText('Base URL');
      userEvent.clear(baseUrlField);
      const submitButton = screen.getByRole('button', { name: /save/i });
      userEvent.click(submitButton);
      expect(screen.getByText(/This field is required/i)).toBeVisible();
    });

    it('hides validation errors when form becomes valid after submit attempt', async () => {
      mockSuccessfulLoad(createBaseUrlCapability(), CAPABILITY_TYPES);
      render(<CapabilitiesEdit />);
      await waitForCapabilityToLoad('Base URL - Test');
      const baseUrlField = screen.getByLabelText('Base URL');
      userEvent.clear(baseUrlField);
      const submitButton = screen.getByRole('button', { name: /save/i });
      userEvent.click(submitButton);
      expect(screen.getByText(/This field is required/i)).toBeVisible();
      userEvent.type(baseUrlField, 'https://example.com');
      expect(screen.queryByText(/This field is required/i)).not.toBeInTheDocument();
    });

    it('shows URL validation error for invalid URL format', async () => {
      mockSuccessfulLoad(createBaseUrlCapability(), CAPABILITY_TYPES);
      render(<CapabilitiesEdit />);
      await waitForCapabilityToLoad('Base URL - Test');
      const baseUrlField = screen.getByLabelText('Base URL');
      userEvent.clear(baseUrlField);
      userEvent.type(baseUrlField, 'invalid-url');
      const submitButton = screen.getByRole('button', { name: /save/i });
      userEvent.click(submitButton);
      expect(screen.queryByText(/URL should be in the format/i)).toBeVisible();
      userEvent.clear(baseUrlField);
      userEvent.type(baseUrlField, 'https://example.com');
      expect(screen.queryByText(/URL should be in the format/i)).not.toBeInTheDocument();
    });

    it('successfully saves and navigates to list when form is valid', async () => {
      mockSuccessfulLoad(createBaseUrlCapability(), CAPABILITY_TYPES);
      render(<CapabilitiesEdit />);
      await waitForCapabilityToLoad('Base URL - Test');
      const baseUrlField = screen.getByLabelText('Base URL');
      userEvent.clear(baseUrlField);
      userEvent.type(baseUrlField, 'https://newurl.com');
      const submitButton = screen.getByRole('button', { name: /save/i });
      userEvent.click(submitButton);
      await waitFor(() => {
        expect(mockGo).toHaveBeenCalledWith(AdminRouteNames.SYSTEM.CAPABILITIES.LIST);
      });
    });
  });

  describe('pristine state handling', () => {
    it('allows saving when only enabled checkbox is changed', async () => {
      mockSuccessfulLoad(createCapability());
      render(<CapabilitiesEdit />);
      await waitForCapabilityToLoad('Audit - Enabled');
      const checkbox = screen.getByRole('checkbox', { name: /Enable/i });
      expect(checkbox).toBeChecked();
      userEvent.click(checkbox);
      expect(checkbox).not.toBeChecked();
      const submitButton = screen.getByRole('button', { name: /save/i });
      userEvent.click(submitButton);
      await waitFor(() => {
        expect(mockGo).toHaveBeenCalledWith(AdminRouteNames.SYSTEM.CAPABILITIES.LIST);
      });
    });
  });

  describe('notes field', () => {
    it('renders notes field for capability with existing notes', async () => {
      mockSuccessfulLoad(createCapability({ notes: 'Existing notes' }));
      render(<CapabilitiesEdit />);
      await waitForCapabilityToLoad('Audit - Enabled');
      const notesField = screen.getByLabelText('Notes');
      expect(notesField).toBeVisible();
      expect(notesField).toHaveValue('Existing notes');
    });

    it('renders notes field as empty when capability has no notes', async () => {
      mockSuccessfulLoad(createCapability({ notes: undefined }));
      render(<CapabilitiesEdit />);
      await waitForCapabilityToLoad('Audit - Enabled');
      const notesField = screen.getByLabelText('Notes');
      expect(notesField).toBeVisible();
      expect(notesField).toHaveValue('');
    });

    it('renders notes field as empty when capability has empty notes string', async () => {
      mockSuccessfulLoad(createCapability({ notes: '' }));
      render(<CapabilitiesEdit />);
      await waitForCapabilityToLoad('Audit - Enabled');
      const notesField = screen.getByLabelText('Notes');
      expect(notesField).toBeVisible();
      expect(notesField).toHaveValue('');
    });

    it('allows editing notes field', async () => {
      mockSuccessfulLoad(createCapability({ notes: 'Original notes' }));
      render(<CapabilitiesEdit />);
      await waitForCapabilityToLoad('Audit - Enabled');
      const notesField = screen.getByLabelText('Notes');
      userEvent.clear(notesField);
      userEvent.type(notesField, 'Updated notes');
      expect(notesField).toHaveValue('Updated notes');
    });

    it('saves notes field correctly', async () => {
      let savedPayload;
      ExtAPIUtils.extAPIRequest.mockImplementation((action, method, data) => {
        if (action === ACTION && method === METHODS.READ) {
          return Promise.resolve({ data: [createCapability({ notes: 'Original notes' })] });
        }
        if (action === ACTION && method === METHODS.READ_TYPES) {
          return Promise.resolve({ data: CAPABILITY_TYPES });
        }
        if (action === ACTION && method === METHODS.UPDATE) {
          savedPayload = data;
          return Promise.resolve({ data: { success: true } });
        }
        return Promise.resolve({ data: { success: true } });
      });

      render(<CapabilitiesEdit />);
      await waitForCapabilityToLoad('Audit - Enabled');
      const notesField = screen.getByLabelText('Notes');
      userEvent.clear(notesField);
      userEvent.type(notesField, 'New notes');
      const submitButton = screen.getByRole('button', { name: /save/i });
      userEvent.click(submitButton);
      await waitFor(() => {
        expect(mockGo).toHaveBeenCalledWith(AdminRouteNames.SYSTEM.CAPABILITIES.LIST);
      });
      expect(savedPayload.data[0].notes).toBe('New notes');
    });

    it('saves empty notes field', async () => {
      let savedPayload;
      ExtAPIUtils.extAPIRequest.mockImplementation((action, method, data) => {
        if (action === ACTION && method === METHODS.READ) {
          return Promise.resolve({ data: [createCapability({ notes: 'Original notes' })] });
        }
        if (action === ACTION && method === METHODS.READ_TYPES) {
          return Promise.resolve({ data: CAPABILITY_TYPES });
        }
        if (action === ACTION && method === METHODS.UPDATE) {
          savedPayload = data;
          return Promise.resolve({ data: { success: true } });
        }
        return Promise.resolve({ data: { success: true } });
      });

      render(<CapabilitiesEdit />);
      await waitForCapabilityToLoad('Audit - Enabled');
      const notesField = screen.getByLabelText('Notes');
      userEvent.clear(notesField);
      const submitButton = screen.getByRole('button', { name: /save/i });
      userEvent.click(submitButton);
      await waitFor(() => {
        expect(mockGo).toHaveBeenCalledWith(AdminRouteNames.SYSTEM.CAPABILITIES.LIST);
      });
      expect(savedPayload.data[0].notes).toBe('');
    });

    it('allows saving when only notes field is changed', async () => {
      mockSuccessfulLoad(createCapability({ notes: 'Original notes' }));
      render(<CapabilitiesEdit />);
      await waitForCapabilityToLoad('Audit - Enabled');
      const notesField = screen.getByLabelText('Notes');
      userEvent.clear(notesField);
      userEvent.type(notesField, 'Updated notes');
      const submitButton = screen.getByRole('button', { name: /save/i });
      userEvent.click(submitButton);
      await waitFor(() => {
        expect(mockGo).toHaveBeenCalledWith(AdminRouteNames.SYSTEM.CAPABILITIES.LIST);
      });
    });

    it('renders notes field as read-only when user lacks edit permission', async () => {
      ExtJS.checkPermission.mockImplementation(permission => {
        if (permission === 'nexus:capabilities:edit') {
          return false;
        }
        return true;
      });

      mockSuccessfulLoad(createCapability({ notes: 'Read-only notes' }));
      render(<CapabilitiesEdit />);
      await waitForCapabilityToLoad('Audit - Enabled');
      const formSection = screen.getByTestId('capabilities-form-section');
      const terms = within(formSection).getAllByRole('term');
      const definitions = within(formSection).getAllByRole('definition');

      // Find Notes field
      const notesTermIndex = terms.findIndex(term => term.textContent === 'Notes');
      expect(notesTermIndex).toBeGreaterThan(-1);
      expect(definitions[notesTermIndex]).toHaveTextContent('Read-only notes');

      // Verify notes field is not editable (should be read-only)
      const notesField = screen.queryByLabelText('Notes');
      expect(notesField).not.toBeInTheDocument();
    });

    it('renders notes field as NA in read-only mode when notes is empty', async () => {
      ExtJS.checkPermission.mockImplementation(permission => {
        if (permission === 'nexus:capabilities:edit') {
          return false;
        }
        return true;
      });

      mockSuccessfulLoad(createCapability({ notes: '' }));
      render(<CapabilitiesEdit />);
      await waitForCapabilityToLoad('Audit - Enabled');
      const formSection = screen.getByTestId('capabilities-form-section');
      const terms = within(formSection).getAllByRole('term');
      const definitions = within(formSection).getAllByRole('definition');

      // Find Notes field
      const notesTermIndex = terms.findIndex(term => term.textContent === 'Notes');
      expect(notesTermIndex).toBeGreaterThan(-1);
      expect(definitions[notesTermIndex]).toHaveTextContent('NA');
    });
  });

  describe('submit error handling', () => {
    it('shows error alert when save fails', async () => {
      const saveErrorMessage = 'Failed to save capability due to server error';
      ExtAPIUtils.extAPIRequest.mockImplementation((action, method) => {
        if (action === ACTION && method === METHODS.READ) {
          return Promise.resolve({ data: [createBaseUrlCapability()] });
        }
        if (action === ACTION && method === METHODS.READ_TYPES) {
          return Promise.resolve({ data: CAPABILITY_TYPES });
        }
        if (action === ACTION && method === METHODS.UPDATE) {
          return Promise.reject(new Error(saveErrorMessage));
        }
        return Promise.resolve({ data: { success: true } });
      });
      render(<CapabilitiesEdit />);
      await waitForCapabilityToLoad('Base URL - Test');
      const baseUrlField = screen.getByLabelText('Base URL');
      userEvent.clear(baseUrlField);
      userEvent.type(baseUrlField, 'https://newurl.com');
      const submitButton = screen.getByRole('button', { name: /save/i });
      userEvent.click(submitButton);
      const errorAlert = await screen.findByRole('alert');
      expect(errorAlert).toBeVisible();
      expect(errorAlert).toHaveTextContent(saveErrorMessage);
      expect(mockGo).not.toHaveBeenCalled();
    });
  });

  describe('read only', () => {
    it('renders alert when user lacks edit permission', async () => {
      // Mock permission check to deny edit permission
      ExtJS.checkPermission.mockImplementation(permission => {
        if (permission === 'nexus:capabilities:edit') {
          return false;
        }
        return true;
      });

      mockSuccessfulLoad(createBaseUrlCapability(), CAPABILITY_TYPES);
      render(<CapabilitiesEdit />);
      await waitForCapabilityToLoad('Base URL - Test');
      const alert = screen.getByText(UIStrings.SETTINGS.READ_ONLY.WARNING);
      expect(alert).toBeVisible();
    });

    it('renders show NA when form is read-only and field has no value', async () => {
      // Mock permission check to deny edit permission
      ExtJS.checkPermission.mockImplementation(permission => {
        if (permission === 'nexus:capabilities:edit') {
          return false;
        }
        return true;
      });

      mockSuccessfulLoad(createBaseUrlCapability({properties: { baseurl: '' }}), CAPABILITY_TYPES);
      render(<CapabilitiesEdit />);
      await waitForCapabilityToLoad('Base URL - Test');
      const formSection = screen.getByTestId('capabilities-form-section');
      const terms = within(formSection).getAllByRole('term');
      const definitions = within(formSection).getAllByRole('definition');
      expect(terms.length).toBeGreaterThan(0);
      expect(definitions.length).toBeGreaterThan(0);

      // Verify Base URL field is read-only
      expect(terms[0]).toHaveTextContent('Capability State');
      expect(terms[1]).toHaveTextContent('Base URL');

      expect(definitions[0]).toHaveTextContent('Enabled');
      expect(definitions[1]).toHaveTextContent('NA');

      // Verify delete button is enabled

      const deleteButton = screen.queryByRole('button', { name: 'Delete' });
      expect(deleteButton).not.toBeInTheDocument();

      // Verify submit button is not visible

      const submitButton = screen.queryByRole('button', { name: /save/i, hidden: true });
      expect(submitButton).not.toBeNull();
    });

    it('renders form fields as read-only when user lacks edit permission (text, checkbox, numeric)', async () => {
      // Mock permission check to deny edit permission
      ExtJS.checkPermission.mockImplementation(permission => {
        if (permission === 'nexus:capabilities:edit') {
          return false;
        }
        return true;
      });

      mockSuccessfulLoad(createUISettingsCapability(), CAPABILITY_TYPES);
      render(<CapabilitiesEdit />);
      await waitForCapabilityToLoad('UI: Settings - Test');
      const formSection = screen.getByTestId('capabilities-form-section');
      const terms = within(formSection).getAllByRole('term');
      const definitions = within(formSection).getAllByRole('definition');
      expect(terms.length).toBeGreaterThan(0);
      expect(definitions.length).toBeGreaterThan(0);

      // Verify Base URL field is read-only
      expect(terms[0]).toHaveTextContent('Capability State');
      expect(terms[1]).toHaveTextContent('Title');
      expect(terms[2]).toHaveTextContent('Debug allowed');
      expect(terms[3]).toHaveTextContent('Authenticated user status interval');
      expect(terms[4]).toHaveTextContent('Anonymous user status interval');
      expect(terms[5]).toHaveTextContent('Session timeout');
      expect(terms[6]).toHaveTextContent('Standard request timeout');
      expect(terms[7]).toHaveTextContent('Extended request timeout');

      expect(definitions[0]).toHaveTextContent('Enabled');
      expect(definitions[1]).toHaveTextContent('test');
      expect(definitions[2]).toHaveTextContent('Enabled');
      expect(definitions[3]).toHaveTextContent('5');
      expect(definitions[4]).toHaveTextContent('100');
      expect(definitions[5]).toHaveTextContent('10');
      expect(definitions[6]).toHaveTextContent('5');
      expect(definitions[7]).toHaveTextContent('20');

      // Verify delete button is enabled

      const deleteButton = screen.queryByRole('button', { name: 'Delete' });
      expect(deleteButton).not.toBeInTheDocument();

      // Verify submit button is not visible

      const submitButton = screen.queryByRole('button', { name: /save/i, hidden: true });
      expect(submitButton).not.toBeNull();
    });

    it('renders form fields as read-only when user lacks edit permission (itemselect, combobox, url, password)', async () => {
      // Mock permission check to deny edit permission
      ExtJS.checkPermission.mockImplementation(permission => {
        if (permission === 'nexus:capabilities:edit') {
          return false;
        }
        return true;
      });

      mockSuccessfulLoad(createWebhookRepositoryCapability(), CAPABILITY_TYPES);
      render(<CapabilitiesEdit />);
      await waitForCapabilityToLoad('Webhook: Repository - Test');
      const formSection = screen.getByTestId('capabilities-form-section');
      const terms = within(formSection).getAllByRole('term');
      const definitions = within(formSection).getAllByRole('definition');
      expect(terms.length).toBeGreaterThan(0);
      expect(definitions.length).toBeGreaterThan(0);

      // Verify Base URL field is read-only
      expect(terms[0]).toHaveTextContent('Capability State');
      expect(terms[1]).toHaveTextContent('Repository');
      expect(terms[2]).toHaveTextContent('Event Types');
      expect(terms[3]).toHaveTextContent('URL');
      expect(terms[4]).toHaveTextContent('Secret Key');

      expect(definitions[0]).toHaveTextContent('Enabled');
      expect(definitions[1]).toHaveTextContent('npm-hosted');
      expect(definitions[2]).toHaveTextContent('asset');
      expect(definitions[3]).toHaveTextContent('component');
      expect(definitions[4]).toHaveTextContent('https://example.com/webhook');
      expect(definitions[5]).toHaveTextContent("********");

      // Verify delete button is enabled

      const deleteButton = screen.queryByRole('button', { name: 'Delete' });
      expect(deleteButton).not.toBeInTheDocument();

      // Verify submit button is not visible

      const submitButton = screen.queryByRole('button', { name: /save/i, hidden: true });
      expect(submitButton).not.toBeNull();
    });
  });

  it('should display inactive warning alert when capability is enabled but not active', async () => {
    const stateDescription = 'Capability is disabled due to missing configuration';

    // Mock capability load with enabled=true, active=false, and stateDescription
    mockSuccessfulLoad(
      createCapability({
        typeName: 'Test Capability',
        description: 'Test Description',
        enabled: true,
        active: false,
        stateDescription: stateDescription,
      })
    );

    render(<CapabilitiesEdit />);

    // Wait for the capability to load
    await screen.findByRole('heading', { name: 'Test Capability - Test Description' });

    // Verify warning alert is displayed with the state description
    // Use getAllByRole since there might be multiple alerts (validation + warning)
    const alerts = screen.getAllByRole('alert');
    const warningAlert = alerts.find(alert => alert.textContent === stateDescription);
    expect(warningAlert).toBeVisible();
    expect(warningAlert.textContent).toBe(stateDescription);
  });

  it('should not display inactive warning alert when capability is not enabled', async () => {
    const stateDescription = 'Some description';

    // Mock capability load with enabled=false
    mockSuccessfulLoad(
      createCapability({
        typeName: 'Test Capability',
        description: 'Test Description',
        enabled: false,
        active: false,
        stateDescription: stateDescription,
      })
    );

    render(<CapabilitiesEdit />);

    // Wait for the capability to load
    await screen.findByRole('heading', { name: 'Test Capability - Test Description' });

    // Verify warning alert is NOT displayed
    // Check for warning alert specifically by looking for alert with warning class
    // The stateDescription appears in the summary tile, so we need to check for the alert element specifically
    const alerts = screen.queryAllByRole('alert');
    const warningAlerts = alerts.filter(
      alert => alert.classList.contains('nx-alert--warning') || alert.className.includes('warning')
    );
    expect(warningAlerts.length).toBe(0);
  });

  it('should not display inactive warning alert when capability is active', async () => {
    const stateDescription = 'Some description';

    // Mock capability load with enabled=true, active=true
    mockSuccessfulLoad(
      createCapability({
        typeName: 'Test Capability',
        description: 'Test Description',
        enabled: true,
        active: true,
        stateDescription: stateDescription,
      })
    );

    render(<CapabilitiesEdit />);

    // Wait for the capability to load
    await screen.findByRole('heading', { name: 'Test Capability - Test Description' });

    // Verify warning alert is NOT displayed
    // Check for warning alert specifically by looking for alert with warning class
    // The stateDescription appears in the summary tile, so we need to check for the alert element specifically
    const alerts = screen.queryAllByRole('alert');
    const warningAlerts = alerts.filter(
      alert => alert.classList.contains('nx-alert--warning') || alert.className.includes('warning')
    );
    expect(warningAlerts.length).toBe(0);
  });

  it('should not display inactive warning alert when stateDescription is missing', async () => {
    // Mock capability load with enabled=true, active=false, but no stateDescription
    mockSuccessfulLoad(
      createCapability({
        typeName: 'Test Capability',
        description: 'Test Description',
        enabled: true,
        active: false,
      })
    );

    render(<CapabilitiesEdit />);

    // Wait for the capability to load
    await screen.findByRole('heading', { name: 'Test Capability - Test Description' });

    // Verify "Capability Summary" heading is present (tile exists)
    expect(screen.getByRole('heading', { name: 'Capability Summary' })).toBeInTheDocument();

    // Verify no inactive warning alert is displayed (narrow assertion to check for specific text, not any alert)
    expect(screen.queryByText(/Capability is disabled|Some description/i)).not.toBeInTheDocument();
  });

  it('should disable delete button when user does not have delete permissions', async () => {
    // Mock permission check to deny delete permission
    ExtJS.checkPermission.mockReturnValue(false);

    // Mock successful capability load
    mockSuccessfulLoad(
      createCapability({
        typeName: 'Test Capability',
        description: 'Test Description',
      })
    );

    render(<CapabilitiesEdit />);

    // Wait for the capability to load
    await screen.findByRole('heading', { name: 'Test Capability - Test Description' });

    // Verify ExtJS.checkPermission was called with the correct permission
    expect(ExtJS.checkPermission).toHaveBeenCalledWith('nexus:capabilities:delete');

    // Verify the delete button is disabled
    const deleteButton = screen.queryByRole('button', { name: 'Delete' });
    expect(deleteButton).not.toBeInTheDocument();
  });

  describe('delete modal does not trigger form validation banner', () => {
    it('does not show form validation error banner when delete modal opens on a capability with required fields', async () => {
      // clicking Delete shows the confirmation modal but the form behind it must NOT show a validation error banner
      mockSuccessfulLoad(createBaseUrlCapability(), CAPABILITY_TYPES);
      render(<CapabilitiesEdit />);
      await waitForCapabilityToLoad('Base URL - Test');

      await openDeleteModal();

      // The form validation error banner must not appear while the delete modal is open
      expect(screen.queryByText(/There were validation errors/i)).not.toBeInTheDocument();
    });

    it('does not show form validation error banner when delete modal opens on a capability with no form fields', async () => {
      mockSuccessfulLoad(createCapability(), CAPABILITY_TYPES);
      render(<CapabilitiesEdit />);
      await waitForCapabilityToLoad('Audit - Enabled');

      await openDeleteModal();

      expect(screen.queryByText(/There were validation errors/i)).not.toBeInTheDocument();
    });

    it('restores normal validation behaviour after delete modal is cancelled', async () => {
      mockSuccessfulLoad(createBaseUrlCapability(), CAPABILITY_TYPES);
      render(<CapabilitiesEdit />);
      await waitForCapabilityToLoad('Base URL - Test');

      const dialog = await openDeleteModal();
      userEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }));

      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());

      // Clear the required field and attempt save — validation banner must reappear normally
      const baseUrlField = screen.getByLabelText('Base URL');
      userEvent.clear(baseUrlField);
      userEvent.click(screen.getByRole('button', { name: /save/i }));

      expect(screen.getByText(/This field is required/i)).toBeVisible();
    });

    it('hides already-visible validation banner when delete modal opens, restores it on cancel', async () => {
      // Scenario: user clears a required field, clicks Save (banner appears), then clicks
      // Delete without fixing the error. The banner must disappear while the modal is open
      // and come back after cancelling — without requiring another save attempt.
      mockSuccessfulLoad(createBaseUrlCapability(), CAPABILITY_TYPES);
      render(<CapabilitiesEdit />);
      await waitForCapabilityToLoad('Base URL - Test');

      // Trigger the validation banner by clearing a required field and submitting
      const baseUrlField = screen.getByLabelText('Base URL');
      userEvent.clear(baseUrlField);
      userEvent.click(screen.getByRole('button', { name: /save/i }));
      expect(screen.getByText(/There were validation errors/i)).toBeVisible();

      const dialog = await openDeleteModal();
      expect(screen.queryByText(/There were validation errors/i)).not.toBeInTheDocument();

      userEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }));
      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
      expect(screen.getByText(/There were validation errors/i)).toBeVisible();
    });
  });

  it('should not show the delete button when capability is a system capability', async () => {
    // Mock successful capability load with isSystem flag
    mockSuccessfulLoad(
      createCapability({
        typeName: 'System Capability',
        description: 'Test Description',
        isSystem: true,
      })
    );

    render(<CapabilitiesEdit />);

    // Wait for the capability to load
    await screen.findByRole('heading', { name: 'System Capability - Test Description' });

    // Verify the delete button is not added to the document for system capabilities
    expect(screen.queryByRole('button', { name: 'Delete' })).not.toBeInTheDocument();
  });

  function buildCapabilityType(capabilityOverrides) {
    return {
      id: 'test-type-id',
      name: 'Test Type',
      about: 'Test capability type description',
      formFields: [],
      ...capabilityOverrides,
    };
  }

  function assertDefinitionRenderedCorrectly(expectedTermValue, expectedDefinitionValue) {
    const definition = screen.getByRole('definition', { name: expectedTermValue });
    expect(definition).toBeVisible();
    expect(definition.textContent).toEqual(expectedDefinitionValue);
  }
});
