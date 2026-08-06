/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
/* global require */
/**
 * Complete mock for @sonatype/nexus-ui-plugin
 */

import React from 'react';

export const mockPermissions = {
  ADMIN: 'nexus:*',
  REPOSITORY_ADMIN: { READ: 'nexus:repository-admin:*:*:read' },
  SSL_TRUSTSTORE: { 
    READ: 'nexus:ssl-truststore:read', 
    CREATE: 'nexus:ssl-truststore:create', 
    UPDATE: 'nexus:ssl-truststore:update', 
    DELETE: 'nexus:ssl-truststore:delete' 
  },
  BLOB_STORES: { 
    READ: 'nexus:blobstores:read', 
    CREATE: 'nexus:blobstores:create', 
    UPDATE: 'nexus:blobstores:update', 
    DELETE: 'nexus:blobstores:delete' 
  },
  PRIVILEGES: { 
    READ: 'nexus:privileges:read', 
    CREATE: 'nexus:privileges:create',
    UPDATE: 'nexus:privileges:update',
    DELETE: 'nexus:privileges:delete'
  },
  SELECTORS: { 
    READ: 'nexus:selectors:read', 
    CREATE: 'nexus:selectors:create',
    UPDATE: 'nexus:selectors:update',
    DELETE: 'nexus:selectors:delete'
  },
  SETTINGS: { 
    READ: 'nexus:settings:read', 
    UPDATE: 'nexus:settings:update' 
  },
  USERS: { 
    READ: 'nexus:users:read',
    CREATE: 'nexus:users:create',
    UPDATE: 'nexus:users:update',
    DELETE: 'nexus:users:delete'
  },
  ROLES: { 
    READ: 'nexus:roles:read', 
    CREATE: 'nexus:roles:create', 
    UPDATE: 'nexus:roles:update', 
    DELETE: 'nexus:roles:delete' 
  },
  TASKS: { 
    READ: 'nexus:tasks:read', 
    CREATE: 'nexus:tasks:create', 
    UPDATE: 'nexus:tasks:update', 
    DELETE: 'nexus:tasks:delete', 
    START: 'nexus:tasks:start', 
    STOP: 'nexus:tasks:stop' 
  },
  LOGGING: { 
    READ: 'nexus:logging:read', 
    UPDATE: 'nexus:logging:update' 
  },
  ATLAS: { 
    READ: 'nexus:atlas:read', 
    CREATE: 'nexus:atlas:create' 
  },
  METRICS: { 
    READ: 'nexus:metrics:read' 
  },
  BUNDLES: { 
    READ: 'nexus:bundles:read' 
  },
  LICENSING: { 
    READ: 'nexus:licensing:read', 
    CREATE: 'nexus:licensing:create', 
    UNINSTALL: 'nexus:licensing:uninstall' 
  },
  LDAP: { 
    READ: 'nexus:ldap:read', 
    CREATE: 'nexus:ldap:create', 
    UPDATE: 'nexus:ldap:update', 
    DELETE: 'nexus:ldap:delete' 
  },
  USER_TOKENS_SETTINGS: { 
    READ: 'nexus:usertoken-settings:read', 
    UPDATE: 'nexus:usertoken-settings:update' 
  },
  USER_TOKENS_USERS: { 
    DELETE: 'nexus:usertoken-users:delete' 
  },
  TAGS: { 
    READ: 'nexus:tags:read' 
  },
  COMPONENT: { 
    CREATE: 'nexus:component:create' 
  },
  SEARCH: { 
    READ: 'nexus:search:read' 
  },
  CAPABILITIES: { 
    READ: 'nexus:capabilities:read', 
    CREATE: 'nexus:capabilities:create', 
    UPDATE: 'nexus:capabilities:update', 
    DELETE: 'nexus:capabilities:delete' 
  },
  MIGRATION: { 
    READ: 'nexus:migration:read' 
  }
};

export const mockExtJS = {
  checkPermission: jest.fn().mockReturnValue(true),
  state: jest.fn().mockReturnValue({
    getValue: jest.fn().mockImplementation((key) => {
      if (key === 'user') return { id: 'admin', name: 'admin', administrator: true };
      return undefined;
    }),
    getUser: jest.fn().mockReturnValue({ id: 'admin', name: 'admin', administrator: true }),
    getEdition: jest.fn().mockReturnValue('PRO'),
  }),
  usePermission: jest.fn().mockReturnValue(true),
  useUser: jest.fn().mockReturnValue({ name: 'admin', administrator: true }),
  showSuccessMessage: jest.fn(),
  showErrorMessage: jest.fn(),
  requestConfirmation: jest.fn().mockResolvedValue(true),
  urlOf: jest.fn().mockImplementation((path) => `http://localhost:8081${path}`),
  setDirtyStatus: jest.fn(),
  waitForExtJs: jest.fn().mockResolvedValue(true),
  fetchAuthenticationToken: jest.fn().mockResolvedValue('mock-token'),
  requestExtConfirmation: jest.fn().mockResolvedValue(true),
  useState: jest.fn().mockImplementation((selector) => {
    // For ExtJS state subscription - return the result of calling the selector
    if (typeof selector === 'function') {
      return selector();
    }
    return undefined;
  }),
  useUserAuthenticated: jest.fn().mockReturnValue(true),
  // Authentication
  signOut: jest.fn(),
  // Edition helpers
  isProEdition: jest.fn().mockReturnValue(true),
  isCommunityEdition: jest.fn().mockReturnValue(false),
  isOssEdition: jest.fn().mockReturnValue(false),
  // Status hook
  useStatus: jest.fn().mockReturnValue({ edition: 'PRO', version: '3.0.0' }),
};

export const mockAPIConstants = {
  REST: {
    INTERNAL: {
      BASE_URL: 'service/rest/internal/ui/',
      REALMS_TYPES: 'service/rest/internal/ui/realms/types',
      ANONYMOUS_SETTINGS: 'service/rest/internal/ui/anonymous-settings',
      NODES: 'service/rest/internal/ui/nodes',
      REPOSITORIES: 'service/rest/internal/ui/repositories/',
      REPOSITORIES_DETAILS: 'service/rest/internal/ui/repositories/details/',
      BLOB_STORES_TYPES: 'service/rest/internal/ui/blobstores/types',
      BLOB_STORES_QUOTA_TYPES: 'service/rest/internal/ui/blobstores/quotaTypes',
      BLOB_STORES_USAGE: 'service/rest/internal/ui/blobstores/usage',
      PRIVILEGES_TYPES: 'service/rest/internal/ui/privileges/types',
      SAML: 'service/rest/internal/ui/saml',
      OAUTH2: 'service/rest/internal/ui/oauth2',
      UPLOAD: 'service/rest/internal/ui/upload/',
      CURRENT_USER: 'service/rest/internal/ui/user/',
    },
    PUBLIC: {
      BASE_URL: 'service/rest/v1/',
      BLOB_STORES: 'service/rest/v1/blobstores',
      REPOSITORIES: 'service/rest/v1/repositories/',
      TASKS: 'service/rest/v1/tasks',
      USERS: 'service/rest/v1/security/users',
      ROLES: 'service/rest/v1/security/roles',
      PRIVILEGES: 'service/rest/v1/security/privileges',
      LDAP_SERVERS: 'service/rest/v1/security/ldap',
      LDAP_CHANGE_ORDER: 'service/rest/v1/security/ldap/change-order',
      ACTIVE_REALMS: 'service/rest/v1/security/realms/active',
      AVAILABLE_REALMS: 'service/rest/v1/security/realms/available',
      SSL_CERTIFICATES: 'service/rest/v1/security/ssl/truststore',
      SSL_CERTIFICATE_DETAILS: 'service/rest/v1/security/ssl',
      EMAIL_SERVER: 'service/rest/v1/email',
      VERIFY_EMAIL_SERVER: 'service/rest/v1/email/verify',
      LICENSE: 'service/rest/v1/system/license',
      USER_TOKENS: 'service/rest/v1/security/user-tokens',
      TAGS: 'service/rest/v1/tags',
      SAML: '/service/rest/v1/security/saml',
      OAUTH2: '/service/rest/v1/security/oauth2',
    },
    SYSTEM_INFORMATION: 'service/rest/atlas/system-information',
  },
  EXT: {
    URL: 'service/extdirect',
    HTTP: { 
      ACTION: 'coreui_HttpSettings', 
      METHODS: { READ: 'read', UPDATE: 'update' } 
    },
    LICENSING: { 
      ACTION: 'licensing_Licensing', 
      METHODS: { READ: 'read', INSTALL: 'install', UNINSTALL: 'uninstall' } 
    },
    EMAIL: { 
      ACTION: 'coreui_Email', 
      METHODS: { READ: 'read', UPDATE: 'update', VERIFY: 'sendVerification' } 
    },
    EMAIL_SERVER: {
      ACTION: 'coreui_Email',
      METHODS: { VERIFY: 'sendVerification', READ: 'read', UPDATE: 'update' }
    },
    CROWD: {
      ACTION: 'crowd_Crowd',
      METHODS: { READ: 'read', UPDATE: 'update', VERIFY: 'verifyConnection', CLEAR_CACHE: 'clearCache' }
    },
    SAML: {
      ACTION: 'saml_Saml',
      METHODS: { READ: 'read', UPDATE: 'update' }
    },
    OAUTH2: {
      ACTION: 'oauth2_OAuth2',
      METHODS: { READ: 'read', UPDATE: 'update' }
    },
    IQ_SERVER: {
      ACTION: 'clm_CLM',
      METHODS: { READ: 'read', UPDATE: 'update', VERIFY: 'verifyConnection' }
    },
    REALMS: {
      ACTION: 'coreui_RealmSettings',
      METHODS: { READ: 'read', UPDATE: 'update' }
    },
    ANONYMOUS: {
      ACTION: 'coreui_AnonymousSettings',
      METHODS: { READ: 'read', UPDATE: 'update' }
    },
    USER_TOKENS: {
      ACTION: 'usertoken_UserTokenConfiguration',
      METHODS: { READ: 'read', UPDATE: 'update', RESET: 'resetAllTokens' }
    },
    UPLOAD: {
      ACTION: 'coreui_Upload',
      METHODS: { GET_DEFINITIONS: 'getUploadDefinitions', GET_UPLOAD_DEFINITIONS: 'getUploadDefinitions', UPLOAD: 'doUpload' }
    },
    REPOSITORY: {
      ACTION: 'coreui_Repository',
      METHODS: { READ: 'read', READ_REFERENCES: 'readReferences', READ_WITH_FOR_ALL: 'readReferencesAddingEntryForAll', BROWSE: 'browse' }
    },
    BROWSE: {
      ACTION: 'coreui_Browse',
      METHODS: { READ: 'read', GET_TREE: 'getTree' }
    },
    PROPRIETARY_REPOSITORIES: {
      ACTION: 'coreui_ProprietaryRepositories',
      METHODS: { READ: 'read', UPDATE: 'update', POSSIBLE_REPOS: 'readPossibleRepos' }
    },
    BLOBSTORE: {
      ACTION: 'coreui_Blobstore',
      METHODS: { READ_NAMES: 'readNames' }
    },
    HEALTH_CHECK: {
      ACTION: 'healthcheck_Status',
      METHODS: { READ: 'read', UPDATE: 'update', ENABLE_ALL: 'enableAll' }
    },
    FIREWALL_REPOSITORY_STATUS: {
      ACTION: 'firewall_RepositoryStatus',
      METHODS: { READ: 'read' }
    },
    PRIVILEGE: {
      ACTION: 'coreui_Privilege',
      METHODS: {
        READ: { NAME: 'read', DATA: [{ page: 1, limit: 300, start: 0, sort: [{ direction: 'ASC', property: 'name' }] }] },
        READ_TYPES: 'readTypes'
      }
    },
    USER: {
      ACTION: 'coreui_User',
      METHODS: { READ: 'read', READ_SOURCES: 'readSources', UPDATE_ROLE_MAPPINGS: 'updateRoleMappings' }
    },
    CAPABILITY: {
      ACTION: 'capability_Capability',
      METHODS: { READ: 'read', READ_TYPES: 'readTypes', CREATE: 'create', UPDATE: 'update', UPDATE_NOTES: 'updateNotes', REMOVE: 'remove', ENABLE: 'enable', DISABLE: 'disable' }
    },
    TASK: {
      ACTION: 'coreui_Task',
      METHODS: { READ: 'read', CREATE: 'create', UPDATE: 'update', DELETE: 'remove', RUN: 'run', STOP: 'stop', READ_TYPES: 'readTypes' }
    },
    SSL: {
      ACTION: 'ssl_Certificate',
      METHODS: { DETAILS: 'details', RETRIEVE_FROM_HOST: 'retrieveFromHost' }
    },
    LDAP: {
      ACTION: 'ldap_LdapServer',
      METHODS: { CLEAR_CACHE: 'clearCache', VERIFY_CONNECTION: 'verifyConnection', READ_TEMPLATES: 'readTemplates', VERIFY_USER_MAPPING: 'verifyUserMapping', VERIFY_LOGIN: 'verifyLogin' }
    },
    OUTREACH: {
      ACTION: 'outreach_Outreach',
      METHODS: { READ_STATUS: 'readStatus', GET_PROXY_DOWNLOAD_NUMBERS: 'getProxyDownloadNumbers' }
    },
    TAGS_LIST: {
      ACTION: 'proui_TagList',
      METHODS: { READ_TAGS: 'readTags' }
    },
    SMALL_PAGE_SIZE: 25,
    MIDDLE_PAGE_SIZE: 50,
    BIG_PAGE_SIZE: 300,
  },
  DEBOUNCE_DELAY: 500,
  SORT_DIRECTIONS: {
    DESC: 'desc',
    ASC: 'asc',
  },
};

export const mockExtAPIUtils = {
  extAPIRequest: jest.fn().mockResolvedValue({ result: { success: true, data: {} } }),
  checkForError: jest.fn(),
  extractResult: jest.fn().mockImplementation((response, defaultValue) => {
    if (response?.result?.data !== undefined) {
      return response.result.data;
    }
    return defaultValue;
  }),
  checkForErrorAndExtract: jest.fn().mockImplementation((response) => {
    if (response?.data?.result?.data !== undefined) {
      return response.data.result.data;
    }
    if (response?.result?.data !== undefined) {
      return response.result.data;
    }
    if (response?.data !== undefined) {
      return response.data;
    }
    return [];
  }),
  createRequestBody: jest.fn().mockImplementation((action, method, data = null) => ({
    action,
    method,
    data: data ? [data] : [{}],
    type: 'rpc',
    tid: 1,
  })),
  createData: jest.fn().mockImplementation((data) => [data]),
};

export const mockRouteNames = {
  ADMIN: {
    REPOSITORY: {
      CLEANUPPOLICIES: {
        ROOT: 'admin.repository.cleanuppolicies',
        LIST: 'admin.repository.cleanuppolicies.list',
        CREATE: 'admin.repository.cleanuppolicies.create',
        EDIT: 'admin.repository.cleanuppolicies.edit',
      },
      REPOSITORIES: {
        ROOT: 'admin.repository.repositories',
        LIST: 'admin.repository.repositories.list',
        CREATE: 'admin.repository.repositories.create',
        EDIT: 'admin.repository.repositories.edit',
      },
      BLOBSTORES: {
        ROOT: 'admin.repository.blobstores',
        LIST: 'admin.repository.blobstores.list',
        CREATE: 'admin.repository.blobstores.create',
        EDIT: 'admin.repository.blobstores.edit',
      },
      SELECTORS: {
        ROOT: 'admin.repository.selectors',
        LIST: 'admin.repository.selectors.list',
        CREATE: 'admin.repository.selectors.create',
        EDIT: 'admin.repository.selectors.edit',
      },
    },
    SECURITY: {
      PRIVILEGES: {
        ROOT: 'admin.security.privileges',
        LIST: 'admin.security.privileges.list',
        CREATE: 'admin.security.privileges.create',
        EDIT: 'admin.security.privileges.edit',
      },
      ROLES: {
        ROOT: 'admin.security.roles',
        LIST: 'admin.security.roles.list',
        CREATE: 'admin.security.roles.create',
        EDIT: 'admin.security.roles.edit',
      },
      USERS: {
        ROOT: 'admin.security.users',
        LIST: 'admin.security.users.list',
        CREATE: 'admin.security.users.create',
        EDIT: 'admin.security.users.edit',
      },
      LDAP: { ROOT: 'admin.security.ldap' },
      REALMS: { ROOT: 'admin.security.realms' },
      ANONYMOUS: { ROOT: 'admin.security.anonymous' },
      SSL: { ROOT: 'admin.security.ssl' },
    },
    SYSTEM: {
      CAPABILITIES: {
        ROOT: 'admin.system.capabilities',
        LIST: 'admin.system.capabilities.list',
        EDIT: 'admin.system.capabilities.edit',
        CREATE: 'admin.system.capabilities.create',
      },
      TASKS: {
        ROOT: 'admin.system.tasks',
        LIST: 'admin.system.tasks.list',
        CREATE: 'admin.system.tasks.create',
        EDIT: 'admin.system.tasks.edit',
      },
      EMAIL: { ROOT: 'admin.system.email' },
      HTTP: { ROOT: 'admin.system.http' },
      BUNDLES: { ROOT: 'admin.system.bundles' },
      LICENSING: { ROOT: 'admin.system.licensing' },
      NODES: { ROOT: 'admin.system.nodes' },
    },
    SUPPORT: {
      LOGGING: { ROOT: 'admin.support.logging' },
      LOGS: { ROOT: 'admin.support.logs' },
      SYSTEMINFO: { ROOT: 'admin.support.systeminfo' },
      SUPPORTZIP: { ROOT: 'admin.support.supportzip' },
    },
  },
  LOGIN: 'login',
  WELCOME: 'browse.welcome',
};

// Mock XState machine
const createMockMachine = () => {
  const machine = {
    id: 'mockMachine',
    initial: 'loading',
    context: {
      data: [],
      pristineData: [],
      sortField: 'name',
      sortDirection: 'asc',
      filter: '',
      error: '',
      isPristine: true,
      isTouched: {},
      loadError: null,
      saveError: null,
      validationErrors: {},
    },
    withConfig: jest.fn().mockImplementation(() => machine),
    withContext: jest.fn().mockImplementation(() => machine),
  };
  return machine;
};

export const mockListMachineUtils = {
  ASC: 'asc',
  DESC: 'desc',
  buildListMachine: jest.fn().mockImplementation(() => createMockMachine()),
  sortDataByFieldAndDirection: jest.fn().mockReturnValue(() => []),
  nextSortDirection: jest.fn().mockReturnValue(() => 'asc'),
  getSortDirection: jest.fn().mockReturnValue(null),
  hasAnyMatches: jest.fn().mockReturnValue(true),
};

export const mockFormUtils = {
  buildFormMachine: jest.fn().mockImplementation(() => createMockMachine()),
  isInvalid: jest.fn().mockReturnValue(false),
  isBlank: jest.fn().mockImplementation((value) => !value || value.trim() === ''),
  fieldProps: jest.fn().mockReturnValue({ name: '', value: '', isPristine: true, validationErrors: null }),
  selectProps: jest.fn().mockReturnValue({ name: '', value: '', isPristine: true }),
  checkboxProps: jest.fn().mockReturnValue({ name: '', isChecked: false }),
  formProps: jest.fn().mockReturnValue({
    loading: false,
    loadError: null,
    onSubmit: jest.fn(),
    submitBtnText: 'Save',
    submitError: null,
    submitMaskMessage: 'Saving...',
    submitMaskState: null,
    validationErrors: null,
  }),
  handleUpdate: jest.fn().mockReturnValue(jest.fn()),
  trimOnBlur: jest.fn().mockReturnValue(jest.fn()),
  saveTooltip: jest.fn().mockReturnValue(null),
  discardTooltip: jest.fn().mockReturnValue(null),
  submitMaskState: jest.fn().mockReturnValue(null),
  getValidationErrorsMessage: jest.fn().mockReturnValue(null),
  readOnlyCheckboxValueLabel: jest.fn().mockReturnValue('Enabled'),
  extractSaveErrorMessage: jest.fn().mockReturnValue(null),
  isInState: jest.fn().mockReturnValue(false),
};

export const mockTokenMachine = createMockMachine();

// Mock router
export const mockRouter = {
  stateService: {
    go: jest.fn().mockResolvedValue({}),
    current: { name: 'test' },
    params: {},
  },
  urlService: { rules: { initial: jest.fn() } },
  stateRegistry: { register: jest.fn() },
  transitionService: { onBefore: jest.fn(), onSuccess: jest.fn() },
  start: jest.fn(),
  plugin: jest.fn(),
};

export const mockCreateRouter = jest.fn().mockReturnValue(mockRouter);

// Mock UI Components
export const MockPage = (props) => React.createElement('div', { 'data-testid': 'page', ...props }, props.children);
export const MockPageHeader = (props) => React.createElement('header', { 'data-testid': 'page-header', ...props }, props.children);
export const MockPageTitle = (props) => React.createElement('h1', { 'data-testid': 'page-title', ...props }, props.text || props.children);
export const MockContentBody = (props) => React.createElement('div', { 'data-testid': 'content-body', ...props }, props.children);
export const MockSection = (props) => React.createElement('section', { 'data-testid': 'section', ...props }, props.children);
export const MockReadOnlyField = (props) => React.createElement('div', { 'data-testid': `readonly-field`, ...props }, props.value);

// Mock UIStrings
export const mockUIStrings = {
  SETTINGS: {
    CANCEL_BUTTON_LABEL: 'Cancel',
    DISCARD_BUTTON_LABEL: 'Discard',
    SAVE_BUTTON_LABEL: 'Save',
    DELETE_BUTTON_LABEL: 'Delete',
    READ_ONLY: {
      WARNING: 'You are viewing a read-only version of this page.',
      CHECKBOX: { ENABLED: 'Enabled', DISABLED: 'Disabled' }
    },
  },
  SAVING: 'Saving...',
  LOADING: 'Loading...',
  FILTER: 'Filter',
  CLOSE: 'Close',
  PRISTINE_TOOLTIP: 'There are no changes',
  INVALID_TOOLTIP: 'Validation errors are present',
  ERROR: {
    FIELD_REQUIRED: 'This field is required',
    LOAD_ERROR: 'An error occurred while loading the form',
    SAVE_ERROR: 'An error occurred while saving the form',
  },
  SAVE_SUCCESS: 'The form was saved successfully',
};

// Mock Utils
export const mockUtils = {
  getVersionMajorMinor: jest.fn().mockReturnValue('3.0'),
  getMajorVersion: jest.fn().mockReturnValue(3),
  canDeleteComponent: jest.fn().mockReturnValue(true),
  canBrowseComponent: jest.fn().mockReturnValue(true),
  timeoutPromise: jest.fn().mockResolvedValue(undefined),
  isBlank: jest.fn().mockImplementation((value) => !value || value.trim() === ''),
  notBlank: jest.fn().mockImplementation((value) => value && value.trim() !== ''),
};

/**
 * Create a complete mock of @sonatype/nexus-ui-plugin
 */
export const createNexusUiPluginMock = (overrides = {}) => ({
  ExtJS: { ...mockExtJS, ...(overrides.ExtJS || {}) },
  Permissions: { ...mockPermissions, ...(overrides.Permissions || {}) },
  APIConstants: { ...mockAPIConstants, ...(overrides.APIConstants || {}) },
  ExtAPIUtils: { ...mockExtAPIUtils, ...(overrides.ExtAPIUtils || {}) },
  RouteNames: { ...mockRouteNames, ...(overrides.RouteNames || {}) },
  ListMachineUtils: { ...mockListMachineUtils, ...(overrides.ListMachineUtils || {}) },
  FormUtils: { ...mockFormUtils, ...(overrides.FormUtils || {}) },
  UIStrings: { ...mockUIStrings, ...(overrides.UIStrings || {}) },
  TokenMachine: overrides.TokenMachine || mockTokenMachine,
  createRouter: overrides.createRouter || mockCreateRouter,
  Page: overrides.Page || MockPage,
  PageHeader: overrides.PageHeader || MockPageHeader,
  PageTitle: overrides.PageTitle || MockPageTitle,
  ContentBody: overrides.ContentBody || MockContentBody,
  Section: overrides.Section || MockSection,
  ReadOnlyField: overrides.ReadOnlyField || MockReadOnlyField,
  Utils: { ...mockUtils, ...(overrides.Utils || {}) },
  // Visibility utilities
  isVisible: overrides.isVisible || jest.fn().mockReturnValue(true),
  useIsVisible: overrides.useIsVisible || jest.fn().mockReturnValue(true),
  // Page hash utility
  isPageHashIncluding: overrides.isPageHashIncluding || jest.fn().mockReturnValue(false),
  // Login components
  LoginPage: overrides.LoginPage || jest.fn().mockImplementation(({ logoConfig: _logoConfig }) =>
    require('react').createElement('div', { 'data-testid': 'login-page' }, 'LoginPage')
  ),
  // XState form infrastructure (used by form machines)
  ENDPOINTS: overrides.ENDPOINTS || {
    PRIVILEGES: '/service/rest/v1/security/privileges',
    REPOSITORIES: '/service/rest/v1/repositories',
    CONTENT_SELECTORS: '/service/rest/v1/security/content-selectors',
    SCRIPTS: '/service/rest/v1/script',
    BLOB_STORES: '/service/rest/v1/blobstores',
    ROLES: '/service/rest/v1/security/roles',
    USERS: '/service/rest/v1/security/users',
    TASKS: '/service/rest/v1/tasks',
    CAPABILITIES: '/service/rest/v1/capabilities',
    ROUTING_RULES: '/service/rest/v1/routing-rules',
    LDAP: '/service/rest/v1/security/ldap',
    REALMS: '/service/rest/v1/security/realms',
    EMAIL: '/service/rest/v1/email',
    HTTP: '/service/rest/v1/http-settings',
    ANONYMOUS: '/service/rest/v1/security/anonymous',
    SSL: '/service/rest/v1/ssl',
    CLEANUP_POLICIES: '/service/rest/internal/cleanup-policies',
  },
  API_INTERNAL_UI: overrides.API_INTERNAL_UI || '/service/rest/internal/ui',
  restClient: overrides.restClient || {
    get: jest.fn().mockResolvedValue([]),
    post: jest.fn().mockResolvedValue({}),
    put: jest.fn().mockResolvedValue({}),
    delete: jest.fn().mockResolvedValue({}),
  },
  createFormMachine: overrides.createFormMachine || jest.fn((config) => {
    // Return a minimal mock machine for tests that import transitively
    const { createMachine } = require('xstate');
    return createMachine({ id: config.id || 'mock-form', initial: 'idle', states: { idle: {} } });
  }),
  useForm: overrides.useForm || jest.fn(() => ({
    data: {},
    isPristine: true,
    touched: {},
    isLoading: false,
    isSaving: false,
    isDeleting: false,
    hasLoadError: false,
    hasValidationErrors: false,
    isConfirmingDelete: false,
    isConfirmingCancel: false,
    isCancelled: false,
    isComplete: false,
    loadError: null,
    saveError: null,
    deleteError: null,
    validationErrors: {},
    field: jest.fn((name) => ({ name, value: '', error: undefined, onChange: jest.fn(), onBlur: jest.fn() })),
    checkbox: jest.fn((name) => ({ name, checked: false, error: undefined, onChange: jest.fn() })),
    select: jest.fn((name) => ({ name, value: '', error: undefined, onChange: jest.fn(), onBlur: jest.fn() })),
    submit: jest.fn(),
    reset: jest.fn(),
    retry: jest.fn(),
    requestDelete: jest.fn(),
    confirmDelete: jest.fn(),
    cancelDelete: jest.fn(),
    requestCancel: jest.fn(),
    confirmCancel: jest.fn(),
    stay: jest.fn(),
    state: { matches: jest.fn(() => false), context: {}, meta: {} },
    send: jest.fn(),
  })),
  ...(overrides.additional || {}),
});

export default createNexusUiPluginMock;
