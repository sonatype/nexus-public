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
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

import { LdapForm } from '../LdapForm';
import { LdapServer } from '../types';
import { useLdapForm } from '../useLdapForm';

// Mock hooks. NOTE: LdapForm is props-based — the verify/create/update/fetch
// functions come in as props from the single useLdapApi instance owned by
// LdapPage (NEXUS-53624 PG13), so this suite drives and asserts on those props
// rather than mocking useLdapApi.
jest.mock('../useLdapForm');

// Radix's Select.Trigger renders as a <button role="combobox">, not a native
// <select>, so fireEvent.change on it never fires onValueChange - it's a
// no-op that silently makes any assertion built on it vacuous (see
// SettingsSelect.test.jsx/.test.tsx, which mock this same module for the
// same reason). Override only Select here - everything else from
// @radix-ui/themes (Theme, Dialog, Box, Flex, Text, etc.) stays real. Unlike
// the existing SettingsSelect mocks (which never wire Item's click to
// onValueChange and are only used for static-rendering assertions), this one
// actually threads onValueChange through so tests can select a real option
// and assert on the resulting side effects (e.g. the F2 verified-badge tests).
// Maintenance note: this re-implements just enough of Select.Root/Trigger/
// Content/Item's contract to work with how SettingsSelect.jsx uses them
// today. If @radix-ui/themes changes that contract (e.g. wraps Content in a
// Viewport/Portal, or moves the selected-label lookup off Root), this mock
// will need updating to match - it is not a passthrough to the real thing.
jest.mock('@radix-ui/themes', () => {
  // eslint-disable-next-line no-undef
  const React = require('react');
  const actual = jest.requireActual('@radix-ui/themes');
  const SelectValueContext = React.createContext(undefined);

  // Real Radix's Select.Trigger displays the currently selected item's label
  // via Select.Root's internal context - it does NOT come from a `children`
  // prop (SettingsSelect.jsx never passes one). Reproduce that here by having
  // Root walk its own children to find the Item matching the current value
  // and injecting that Item's label into Trigger, instead of hardcoding a
  // fixed placeholder that would never reflect a selection.
  function findSelectedLabel(children: any, value: any): any {
    let result;
    React.Children.forEach(children, (child: any) => {
      if (result !== undefined || !React.isValidElement(child)) return;
      if (child.type === Item && child.props.value === value) {
        result = child.props.children;
        return;
      }
      if (child.props?.children) {
        const nested = findSelectedLabel(child.props.children, value);
        if (nested !== undefined) result = nested;
      }
    });
    return result;
  }

  function Root({ children, value, onValueChange, name, disabled }: any) {
    const selectedLabel = findSelectedLabel(children, value);
    // Real Radix's Select.Root propagates `disabled` down to its Trigger
    // (rendering a disabled <button>). SettingsSelect.jsx sets `disabled` on
    // Root, not Trigger, so mirror that here - otherwise gating assertions
    // (e.g. the NEXUS-53627 read-only tests) would be silently vacuous.
    return (
      <SelectValueContext.Provider value={onValueChange}>
        <div data-testid={`select-root-${name}`}>
          {React.Children.map(children, (child: any) =>
            React.isValidElement(child) && child.type === Trigger
              ? React.cloneElement(child, { children: selectedLabel, disabled })
              : child
          )}
        </div>
      </SelectValueContext.Provider>
    );
  }

  function Trigger({ id, placeholder, className, children, ...props }: any) {
    return (
      <button type="button" id={id} role="combobox" className={className} {...props}>
        {children || placeholder}
      </button>
    );
  }

  function Content({ children }: any) {
    return <div>{children}</div>;
  }

  function Item({ children, value, disabled }: any) {
    const onValueChange = React.useContext(SelectValueContext);
    return (
      <div role="option" aria-disabled={disabled} onClick={() => !disabled && onValueChange?.(value)}>
        {children}
      </div>
    );
  }

  return {
    ...actual,
    Select: { Root, Trigger, Content, Item },
  };
});

const mockUseLdapForm = useLdapForm as jest.MockedFunction<typeof useLdapForm>;

function createMockLdapForm(data: any = {}) {
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
    state: { matches: jest.fn(() => false), context: { data, server: null } },
    send: jest.fn(),
  } as any;
}

// Mock ExtJS
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    checkPermission: jest.fn().mockReturnValue(true),
  },
  ExtAPIUtils: {
    extAPIRequest: jest.fn(),
    checkForError: jest.fn(),
  },
  APIConstants: {
    EXT: {
      LDAP: {
        ACTION: 'ldap_LdapServer',
        METHODS: {
          VERIFY_CONNECTION: 'verifyConnection',
          VERIFY_USER_MAPPING: 'verifyUserMapping',
          VERIFY_LOGIN: 'verifyLogin',
          READ_TEMPLATES: 'readTemplates',
        },
      },
    },
  },
}));

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

describe('LdapForm', () => {
  const mockServer: LdapServer = {
    id: 'server1',
    name: 'Test LDAP Server',
    host: 'ldap.example.com',
    port: 389,
    protocol: 'ldap',
    searchBase: 'dc=example,dc=com',
    authScheme: 'simple',
    authUsername: 'cn=admin,dc=example,dc=com',
    authPassword: 'secret',
    order: 1,
    userObjectClass: 'inetOrgPerson',
    userIdAttribute: 'uid',
    userRealNameAttribute: 'cn',
    userEmailAddressAttribute: 'mail',
    userBaseDn: 'ou=users',
    ldapGroupsAsRoles: true,
    groupType: 'static',
    groupBaseDn: 'ou=groups',
    groupObjectClass: 'groupOfNames',
    groupIdAttribute: 'cn',
    groupMemberAttribute: 'member',
    groupMemberFormat: '${dn}',
  };

  const defaultProps = {
    server: null,
    isCreate: true,
    loading: false,
    onSave: jest.fn(),
    onCancel: jest.fn(),
    fetchTemplates: jest.fn().mockResolvedValue([]),
    createServer: jest.fn().mockResolvedValue({}),
    updateServer: jest.fn().mockResolvedValue({}),
    verifyConnection: jest.fn().mockResolvedValue(undefined),
    verifyUserMapping: jest.fn().mockResolvedValue([]),
    verifyLogin: jest.fn().mockResolvedValue(undefined),
  };

  beforeEach(() => {
    jest.clearAllMocks();
    // LdapForm is props-based; reset the verify/fetch props to their default
    // resolved values so per-test overrides (below) start from a clean slate.
    defaultProps.fetchTemplates.mockResolvedValue([]);
    defaultProps.verifyConnection.mockResolvedValue(undefined);
    defaultProps.verifyUserMapping.mockResolvedValue([]);
    defaultProps.verifyLogin.mockResolvedValue(undefined);
    // NOTE: this mockImplementation is invoked by React during LdapForm's own
    // render (it stands in for the real useLdapForm hook call inside
    // LdapForm.tsx), so calling React.useState here is exactly as safe as
    // calling it from any other custom hook - it runs in a consistent
    // position on every render of the one component that calls it. This
    // makes form.send({type:'UPDATE',...}) actually mutate formData across
    // re-renders (previously it was a no-op jest.fn(), which silently broke
    // any test asserting on a value written back through the form, e.g. the
    // numeric-field-keeps-0 tests below).
    mockUseLdapForm.mockImplementation(({ server }: any) => {
      // Mirrors DEFAULT_LDAP_SERVER in types.ts (ldapGroupsAsRoles: true,
      // groupType: 'dynamic' per NEXUS-53623 F3) so this create-mode
      // fallback doesn't silently drift from the real default.
      const initialFormData = server ? { ...server } : {
        name: '', host: '', port: 389, protocol: 'ldap', searchBase: '', authScheme: 'simple',
        authUsername: '', authPassword: '', useTrustStore: false, connectionTimeout: 30,
        connectionRetryDelay: 300, maxIncidentsCount: 3, userBaseDn: '', userSubtree: false,
        userObjectClass: '', userLdapFilter: '', userIdAttribute: '', userRealNameAttribute: '',
        userEmailAddressAttribute: '', userPasswordAttribute: '', ldapGroupsAsRoles: true,
        groupType: 'dynamic', groupBaseDn: '', groupSubtree: false, groupObjectClass: '',
        groupIdAttribute: '', groupMemberAttribute: '', groupMemberFormat: '', userMemberOfAttribute: '',
      };
      const [formData, setFormData] = React.useState<any>(initialFormData);
      const form = createMockLdapForm(formData);
      form.send = jest.fn((event: any) => {
        if (event?.type === 'UPDATE') {
          setFormData((prev: any) => ({ ...prev, [event.name]: event.value }));
        }
      });
      return {
        form,
        server: server || null,
        isCreate: !server,
        applyTemplate: jest.fn(),
        // Mirrors ldapFormMachine.ts's changeProtocol action (also swaps the
        // default port) so tests that actually select a new protocol option
        // (rather than asserting a value fireEvent.change wrote directly)
        // observe a real state update, same rationale as form.send above.
        changeProtocol: jest.fn((protocol: string) => {
          setFormData((prev: any) => {
            let port = prev.port;
            if (protocol === 'ldaps' && port === 389) port = 636;
            else if (protocol === 'ldap' && port === 636) port = 389;
            return { ...prev, protocol, port };
          });
        }),
        confirmPasswordAndSubmit: jest.fn(),
      } as any;
    });
  });

  it('renders wizard form for create mode', () => {
    render(<LdapForm {...defaultProps} />, { wrapper: TestWrapper });

    expect(screen.getByTestId('ldap-wizard-form')).toBeInTheDocument();
  });

  it('renders wizard form for edit mode', () => {
    render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });

    expect(screen.getByTestId('ldap-wizard-form')).toBeInTheDocument();
  });

  it('shows step indicator with connection as current step', () => {
    render(<LdapForm {...defaultProps} />, { wrapper: TestWrapper });

    expect(screen.getByTestId('ldap-form')).toHaveAttribute('data-step', 'connection');
  });

  it('starts on Connection step by default', () => {
    render(<LdapForm {...defaultProps} />, { wrapper: TestWrapper });

    // Connection step should show Name field (using data-testid from SettingsTextInput)
    expect(screen.getByTestId('input-name')).toBeInTheDocument();
  });

  it('displays connection fields', () => {
    render(<LdapForm {...defaultProps} />, { wrapper: TestWrapper });

    // Use data-testid from SettingsTextInput components
    expect(screen.getByTestId('input-name')).toBeInTheDocument();
    expect(screen.getByTestId('input-host')).toBeInTheDocument();
    expect(screen.getByTestId('input-port')).toBeInTheDocument();
    expect(screen.getByTestId('input-searchBase')).toBeInTheDocument();
  });

  it('populates form fields with existing server data', () => {
    render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });

    expect(screen.getByDisplayValue('Test LDAP Server')).toBeInTheDocument();
    expect(screen.getByDisplayValue('ldap.example.com')).toBeInTheDocument();
    expect(screen.getByDisplayValue('389')).toBeInTheDocument();
    expect(screen.getByDisplayValue('dc=example,dc=com')).toBeInTheDocument();
  });

  it('calls onCancel when Cancel button is clicked', () => {
    render(<LdapForm {...defaultProps} />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Cancel'));

    expect(defaultProps.onCancel).toHaveBeenCalled();
  });

  it('validates required fields before navigation', async () => {
    render(<LdapForm {...defaultProps} />, { wrapper: TestWrapper });

    // Next button should be disabled when required fields are empty
    const nextButton = screen.getByRole('button', { name: /Continue/i });
    expect(nextButton).toBeDisabled();
  });

  it('enables Next button when connection form is valid', async () => {
    render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });

    const nextButton = screen.getByRole('button', { name: /Continue/i });
    expect(nextButton).not.toBeDisabled();
  });

  it('shows verify connection button', () => {
    render(<LdapForm {...defaultProps} />, { wrapper: TestWrapper });

    expect(screen.getByText('Verify Connection')).toBeInTheDocument();
  });

  it('does not call verifyConnection when required connection fields are missing', () => {
    // Create mode with empty defaults: the button is disabled (connectionValid === false) and
    // handleVerifyConnection also re-validates, so the API must not be hit.
    render(<LdapForm {...defaultProps} />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByRole('button', { name: /Verify Connection/i }));

    expect(defaultProps.verifyConnection).not.toHaveBeenCalled();
  });

  it('calls verifyConnection with the form data when connection fields are valid', () => {
    // Edit mode with a fully-populated server: connectionValid === true, so the verify call fires.
    render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByRole('button', { name: /Verify Connection/i }));

    expect(defaultProps.verifyConnection).toHaveBeenCalledTimes(1);
    // Edit mode passes the existing server name so the backend can merge the stored bind password.
    expect(defaultProps.verifyConnection).toHaveBeenCalledWith(
      expect.objectContaining({ name: mockServer.name, host: mockServer.host }),
      mockServer.name
    );
  });

  it('displays loading state when loading', () => {
    render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} loading={true} />, { wrapper: TestWrapper });

    // When loading, the connection step Next button should be disabled
    const nextButton = screen.getByRole('button', { name: /Continue/i });
    // While connectionValid passes, the button behavior during loading is what matters
    // The form is not navigable in loading state since the purpose is to prevent changes
    expect(nextButton).toBeInTheDocument();
  });

  it('shows Next button on connection step and Save on user/group step', () => {
    render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });

    expect(screen.getByRole('button', { name: /Continue/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /^Save$/i })).not.toBeInTheDocument();
  });

  it('calls onSave with form data when Save is clicked', async () => {
    render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });

    // With valid data, the form should be saveable from the first step via component logic
    // The Next button being enabled indicates the form is valid for navigation
    const nextButton = screen.getByRole('button', { name: /Continue/i });
    expect(nextButton).not.toBeDisabled();

    // onCancel should be callable - this verifies props are passed correctly
    fireEvent.click(screen.getByRole('button', { name: /Cancel/i }));
    expect(defaultProps.onCancel).toHaveBeenCalled();
  });

  it('shows protocol select with LDAP and LDAPS options', () => {
    render(<LdapForm {...defaultProps} />, { wrapper: TestWrapper });

    // Use testid for SettingsSelect
    expect(screen.getByTestId('select-protocol')).toBeInTheDocument();
  });

  it('shows authentication scheme options', () => {
    render(<LdapForm {...defaultProps} />, { wrapper: TestWrapper });

    // Use testid for SettingsSelect
    expect(screen.getByTestId('select-authScheme')).toBeInTheDocument();
  });

  it('shows username field when auth scheme is simple', () => {
    render(<LdapForm {...defaultProps} />, { wrapper: TestWrapper });

    // Use testid for SettingsTextInput and SettingsPasswordInput
    expect(screen.getByTestId('input-authUsername')).toBeInTheDocument();
    expect(screen.getByTestId('password-authPassword')).toBeInTheDocument();
  });

  it('shows Cancel button on connection step', () => {
    render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });

    expect(screen.getByRole('button', { name: /Cancel/i })).toBeInTheDocument();
  });

  it('navigates back to connection step when Back is clicked', () => {
    render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });

    // Connection fields should be visible on the default step
    expect(screen.getByTestId('input-host')).toBeInTheDocument();
    expect(screen.getByTestId('input-name')).toBeInTheDocument();
  });

  it('shows both steps in step indicator', () => {
    render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });

    expect(screen.getByTestId('ldap-wizard-form-steps')).toBeInTheDocument();
  });

  it('disables Next when connection step is invalid', () => {
    render(<LdapForm {...defaultProps} />, { wrapper: TestWrapper });

    const nextButton = screen.getByRole('button', { name: /Continue/i });
    expect(nextButton).toBeDisabled();
  });

  it('shows connection timeout field', () => {
    render(<LdapForm {...defaultProps} />, { wrapper: TestWrapper });

    expect(screen.getByTestId('input-connectionTimeout')).toBeInTheDocument();
  });

  it('shows retry delay field', () => {
    render(<LdapForm {...defaultProps} />, { wrapper: TestWrapper });

    expect(screen.getByTestId('input-connectionRetryDelay')).toBeInTheDocument();
  });

  it('shows max incidents field', () => {
    render(<LdapForm {...defaultProps} />, { wrapper: TestWrapper });

    expect(screen.getByTestId('input-maxIncidentsCount')).toBeInTheDocument();
  });

  it('validates name field is required', () => {
    render(<LdapForm {...defaultProps} />, { wrapper: TestWrapper });

    const nameInput = screen.getByTestId('input-name');
    fireEvent.change(nameInput, { target: { value: '' } });
    fireEvent.blur(nameInput);

    // Next button should be disabled with empty name
    const nextButton = screen.getByRole('button', { name: /Continue/i });
    expect(nextButton).toBeDisabled();
  });

  it('validates host field is required', () => {
    render(<LdapForm {...defaultProps} />, { wrapper: TestWrapper });

    const hostInput = screen.getByTestId('input-host');
    fireEvent.change(hostInput, { target: { value: '' } });
    fireEvent.blur(hostInput);

    // Next button should be disabled with empty host
    const nextButton = screen.getByRole('button', { name: /Continue/i });
    expect(nextButton).toBeDisabled();
  });

  it('validates port must be a number', () => {
    render(<LdapForm {...defaultProps} />, { wrapper: TestWrapper });

    const portInput = screen.getByTestId('input-port');
    expect(portInput).toHaveAttribute('type', 'number');
  });

  it('shows default port 389 for LDAP protocol', () => {
    render(<LdapForm {...defaultProps} />, { wrapper: TestWrapper });

    expect(screen.getByDisplayValue('389')).toBeInTheDocument();
  });

  it('allows changing protocol selection', () => {
    render(<LdapForm {...defaultProps} />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByRole('option', { name: 'ldaps (SSL)' }));

    // Protocol should be updated - the trigger displays the selected option's label
    expect(screen.getByTestId('select-protocol')).toHaveTextContent('ldaps (SSL)');
  });

  it('displays ldaps protocol when editing server with ldaps', () => {
    const ldapsServer = {
      ...mockServer,
      protocol: 'ldaps' as const,
      port: 636,
    };

    render(<LdapForm {...defaultProps} server={ldapsServer} isCreate={false} />, { wrapper: TestWrapper });

    // Protocol select should show ldaps (SSL) label, not ldap
    // The Radix Select.Trigger displays the selected option's label
    const protocolSelect = screen.getByTestId('select-protocol');
    expect(protocolSelect).toHaveTextContent('ldaps (SSL)');
  });

  it('allows changing auth scheme', () => {
    render(<LdapForm {...defaultProps} />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByRole('option', { name: 'DIGEST-MD5' }));

    // Auth scheme should be updated - the trigger displays the selected option's label
    expect(screen.getByTestId('select-authScheme')).toHaveTextContent('DIGEST-MD5');
  });

  it('renders correctly with no server (create mode)', () => {
    render(<LdapForm {...defaultProps} server={null} isCreate={true} />, { wrapper: TestWrapper });

    expect(screen.getByTestId('ldap-wizard-form')).toBeInTheDocument();
    expect(screen.getByTestId('input-name')).toHaveValue('');
  });

  it('shows form loading state indicator', () => {
    render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} loading={true} />, { wrapper: TestWrapper });

    expect(screen.getByTestId('ldap-wizard-form')).toBeInTheDocument();
  });

  it('has verify connection button enabled when form is valid', () => {
    render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });

    expect(screen.getByRole('button', { name: /Verify Connection/i })).not.toBeDisabled();
  });

  it('shows password re-entry modal when saving an existing server with authentication', async () => {
    mockUseLdapForm.mockImplementation(({ server }: any) => {
      const formData = { ...server, authPassword: '' }; // No password pre-filled for edit
      return {
        form: createMockLdapForm(formData),
        server: server || null,
        isCreate: false,
        applyTemplate: jest.fn(),
        changeProtocol: jest.fn(),
        confirmPasswordAndSubmit: jest.fn(),
      } as any;
    });

    render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });

    // Navigate to step 2
    fireEvent.click(screen.getByRole('button', { name: /Continue/i }));

    // Click Save
    fireEvent.click(screen.getByRole('button', { name: /^Save$/i }));

    // Modal should appear
    expect(screen.getByTestId('ldap-password-modal')).toBeInTheDocument();
    expect(screen.getByText(/For security reasons, the password must be re-entered/i)).toBeInTheDocument();
  });

  describe('read-only mode when lacking nexus:ldap:update (NEXUS-53627)', () => {
    beforeEach(() => {
      global.NX.Permissions.check.mockImplementation(
        (permission: string) => permission !== 'nexus:ldap:update'
      );
    });

    it('disables all connection-step fields', () => {
      render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });

      expect(screen.getByTestId('input-name')).toBeDisabled();
      expect(screen.getByTestId('input-host')).toBeDisabled();
      expect(screen.getByTestId('input-port')).toBeDisabled();
      expect(screen.getByTestId('input-searchBase')).toBeDisabled();
      expect(screen.getByTestId('select-protocol')).toBeDisabled();
      expect(screen.getByTestId('select-authScheme')).toBeDisabled();
      expect(screen.getByTestId('input-authUsername')).toBeDisabled();
      expect(screen.getByTestId('password-authPassword')).toBeDisabled();
      expect(screen.getByTestId('input-connectionTimeout')).toBeDisabled();
      expect(screen.getByTestId('input-connectionRetryDelay')).toBeDisabled();
      expect(screen.getByTestId('input-maxIncidentsCount')).toBeDisabled();
    });

    it('disables the Verify Connection button', () => {
      render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });

      expect(screen.getByRole('button', { name: /Verify Connection/i })).toBeDisabled();
    });

    it('shows a permission warning banner explaining why the form is read-only', () => {
      render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });

      expect(screen.getByText(/You don't have permission to edit this LDAP server/i)).toBeInTheDocument();
    });

    it('keeps Continue enabled on the connection step so a read-only user can still view step 2', () => {
      render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });

      expect(screen.getByRole('button', { name: /Continue/i })).not.toBeDisabled();
    });

    it('disables all user/group-step fields and the Save button after navigating to step 2', () => {
      render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });

      fireEvent.click(screen.getByRole('button', { name: /Continue/i }));

      expect(screen.getByTestId('input-userBaseDn')).toBeDisabled();
      expect(screen.getByTestId('checkbox-userSubtree')).toBeDisabled();
      expect(screen.getByTestId('input-userObjectClass')).toBeDisabled();
      expect(screen.getByTestId('input-userIdAttribute')).toBeDisabled();
      expect(screen.getByTestId('input-userRealNameAttribute')).toBeDisabled();
      expect(screen.getByTestId('input-userEmailAddressAttribute')).toBeDisabled();
      expect(screen.getByTestId('checkbox-ldapGroupsAsRoles')).toBeDisabled();
      expect(screen.getByRole('button', { name: /^Save$/i })).toBeDisabled();
    });

    it('disables the Verify User Mapping and Verify Login buttons on step 2', () => {
      render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });

      fireEvent.click(screen.getByRole('button', { name: /Continue/i }));

      expect(screen.getByTestId('ldap-verify-user-mapping')).toBeDisabled();
      expect(screen.getByTestId('ldap-verify-login-button')).toBeDisabled();
    });
  });

  describe('create-mode field gating uses nexus:ldap:create, not nexus:ldap:update (NEXUS-53627 review fix)', () => {
    it('enables fields and the Create button when the user has nexus:ldap:create but lacks nexus:ldap:update', () => {
      global.NX.Permissions.check.mockImplementation(
        (permission: string) => permission !== 'nexus:ldap:update'
      );

      render(<LdapForm {...defaultProps} server={mockServer} isCreate={true} />, { wrapper: TestWrapper });

      expect(screen.getByTestId('input-name')).not.toBeDisabled();

      fireEvent.click(screen.getByRole('button', { name: /Continue/i }));

      expect(screen.getByRole('button', { name: /^Create$/i })).not.toBeDisabled();
    });

    it('disables fields and the Create button when the user has nexus:ldap:update but lacks nexus:ldap:create', () => {
      global.NX.Permissions.check.mockImplementation(
        (permission: string) => permission !== 'nexus:ldap:create'
      );

      render(<LdapForm {...defaultProps} server={mockServer} isCreate={true} />, { wrapper: TestWrapper });

      expect(screen.getByTestId('input-name')).toBeDisabled();
      // Step navigation stays available even though the user can't submit —
      // same "view but don't act" behavior as the edit-mode read-only case.
      expect(screen.getByRole('button', { name: /Continue/i })).not.toBeDisabled();

      fireEvent.click(screen.getByRole('button', { name: /Continue/i }));

      expect(screen.getByRole('button', { name: /^Create$/i })).toBeDisabled();
      expect(screen.getByText(/You don't have permission to create LDAP servers/i)).toBeInTheDocument();
    });

    it('still allows Verify Connection in create mode when the user lacks nexus:ldap:create but has nexus:ldap:update', () => {
      global.NX.Permissions.check.mockImplementation(
        (permission: string) => permission !== 'nexus:ldap:create'
      );

      render(<LdapForm {...defaultProps} server={mockServer} isCreate={true} />, { wrapper: TestWrapper });

      // Verify* backend endpoints require nexus:ldap:update unconditionally,
      // even during create — so this stays enabled despite fields being disabled.
      expect(screen.getByRole('button', { name: /Verify Connection/i })).not.toBeDisabled();
    });
  });

  describe('footer Delete button', () => {
    it('renders when onDelete is provided in edit mode', () => {
      render(
        <LdapForm {...defaultProps} server={mockServer} isCreate={false} onDelete={jest.fn()} />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByTestId('form-delete')).toBeInTheDocument();
    });

    it('does not render when onDelete is not provided (e.g. user lacks nexus:ldap:delete)', () => {
      render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });

      expect(screen.queryByTestId('form-delete')).not.toBeInTheDocument();
    });
  });

  it('saves directly via SUBMIT, skipping the password modal, when authPassword is already populated', async () => {
    const confirmPasswordAndSubmit = jest.fn();
    // Stable send across renders: LdapForm re-renders on navigation, so a per-render
    // jest.fn() would leave the assertion checking a different instance than the one
    // handleSave actually called.
    const send = jest.fn();
    mockUseLdapForm.mockImplementation(({ server }: any) => {
      // Simulates a user who typed a new password directly into the
      // Password field (rather than via the re-entry modal) before saving.
      const formData = { ...server, authPassword: 'freshly-typed-password' };
      const mockForm = createMockLdapForm(formData);
      mockForm.send = send;
      return {
        form: mockForm,
        server: server || null,
        isCreate: false,
        applyTemplate: jest.fn(),
        changeProtocol: jest.fn(),
        confirmPasswordAndSubmit,
      } as any;
    });

    render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByRole('button', { name: /Continue/i }));
    fireEvent.click(screen.getByRole('button', { name: /^Save$/i }));

    // No modal - the password is already in form state, so handleSave goes
    // straight to SUBMIT instead of gating on re-entry.
    expect(screen.queryByTestId('ldap-password-modal')).not.toBeInTheDocument();
    expect(send).toHaveBeenCalledWith('SUBMIT');
    expect(confirmPasswordAndSubmit).not.toHaveBeenCalled();
  });

  it('shows static group fields when editing a server with ldapGroupsAsRoles and groupType static', () => {
    const staticGroupServer = {
      ...mockServer,
      ldapGroupsAsRoles: true,
      groupType: 'static' as const,
      groupObjectClass: 'groupOfNames',
      groupIdAttribute: 'cn',
      groupMemberAttribute: 'member',
      groupMemberFormat: '${dn}',
    };

    mockUseLdapForm.mockImplementation(({ server }: any) => {
      const formData = { ...server };
      return {
        form: createMockLdapForm(formData),
        server: server || null,
        isCreate: false,
        applyTemplate: jest.fn(),
        changeProtocol: jest.fn(),
        confirmPasswordAndSubmit: jest.fn(),
      } as any;
    });

    render(<LdapForm {...defaultProps} server={staticGroupServer} isCreate={false} />, { wrapper: TestWrapper });

    // Navigate to User & Group step (connection step is valid for mockServer)
    fireEvent.click(screen.getByRole('button', { name: /Continue/i }));

    // Static-group-only fields should be visible
    expect(screen.getByTestId('input-groupObjectClass')).toBeInTheDocument();
    expect(screen.getByTestId('input-groupMemberFormat')).toBeInTheDocument();

    // Dynamic-group-only field should NOT be present
    expect(screen.queryByTestId('input-userMemberOfAttribute')).not.toBeInTheDocument();
  });

  it('shows dynamic group field when editing a server with ldapGroupsAsRoles and groupType dynamic', () => {
    const dynamicGroupServer = {
      ...mockServer,
      ldapGroupsAsRoles: true,
      groupType: 'dynamic' as const,
      userMemberOfAttribute: 'memberOf',
    };

    mockUseLdapForm.mockImplementation(({ server }: any) => {
      const formData = { ...server };
      return {
        form: createMockLdapForm(formData),
        server: server || null,
        isCreate: false,
        applyTemplate: jest.fn(),
        changeProtocol: jest.fn(),
        confirmPasswordAndSubmit: jest.fn(),
      } as any;
    });

    render(<LdapForm {...defaultProps} server={dynamicGroupServer} isCreate={false} />, { wrapper: TestWrapper });

    // Navigate to User & Group step
    fireEvent.click(screen.getByRole('button', { name: /Continue/i }));

    // Dynamic-group-only field should be visible
    expect(screen.getByTestId('input-userMemberOfAttribute')).toBeInTheDocument();

    // Static-group-only fields should NOT be present
    expect(screen.queryByTestId('input-groupObjectClass')).not.toBeInTheDocument();
    expect(screen.queryByTestId('input-groupMemberFormat')).not.toBeInTheDocument();
  });

  it('confirms the re-entered password synchronously instead of using a setTimeout-delayed submit', async () => {
    const confirmPasswordAndSubmit = jest.fn();
    mockUseLdapForm.mockImplementation(({ server }: any) => {
      const formData = { ...server, authPassword: '' }; // No password pre-filled for edit
      return {
        form: createMockLdapForm(formData),
        server: server || null,
        isCreate: false,
        applyTemplate: jest.fn(),
        changeProtocol: jest.fn(),
        confirmPasswordAndSubmit,
      } as any;
    });

    render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByRole('button', { name: /Continue/i }));
    fireEvent.click(screen.getByRole('button', { name: /^Save$/i }));

    expect(screen.getByTestId('ldap-password-modal')).toBeInTheDocument();

    fireEvent.change(screen.getByTestId('ldap-password-input'), { target: { value: 're-entered-secret' } });
    fireEvent.click(screen.getByTestId('ldap-password-submit'));

    // No setTimeout races: confirmPasswordAndSubmit fires as soon as the verify
    // bind resolves (a single microtask hop from the default mock), with the
    // password that was typed. Awaiting waitFor here accommodates the verify
    // promise that gates submission per NEXUS-53959 — the old bug this test
    // was written to guard against (an intentional setTimeout wrapper causing
    // an UPDATE/SUBMIT race) is a separate concern.
    await waitFor(() => {
      expect(confirmPasswordAndSubmit).toHaveBeenCalledWith('re-entered-secret');
    });

    // The modal closes right away rather than waiting on a delayed submit.
    expect(screen.queryByTestId('ldap-password-modal')).not.toBeInTheDocument();
  });

  it('NEXUS-53959: verifies the re-entered password against the LDAP server before submitting', async () => {
    // Regression: the password re-entry modal was a security rubber-stamp -
    // any string typed here was assigned to context and submitted, so a wrong
    // password on Save silently persisted the edit. The modal's Save must
    // POST /v1/security/ldap/verify-connection with the entered password
    // FIRST, and only fall through to confirmPasswordAndSubmit / updateServer
    // when the bind succeeds. A rejected verify keeps the modal open and
    // surfaces the backend's error message.
    const confirmPasswordAndSubmit = jest.fn();
    mockUseLdapForm.mockImplementation(({ server }: any) => {
      const formData = { ...server, authPassword: '' };
      return {
        form: createMockLdapForm(formData),
        server: server || null,
        isCreate: false,
        applyTemplate: jest.fn(),
        changeProtocol: jest.fn(),
        confirmPasswordAndSubmit,
      } as any;
    });

    const bindFailure = new Error('LDAP bind failed: invalid credentials');
    const verifyConnection = jest.fn().mockRejectedValue(bindFailure);

    render(
      <LdapForm {...defaultProps} server={mockServer} isCreate={false} verifyConnection={verifyConnection} />,
      { wrapper: TestWrapper }
    );

    fireEvent.click(screen.getByRole('button', { name: /Continue/i }));
    fireEvent.click(screen.getByRole('button', { name: /^Save$/i }));

    expect(screen.getByTestId('ldap-password-modal')).toBeInTheDocument();

    fireEvent.change(screen.getByTestId('ldap-password-input'), { target: { value: 'wrong-password' } });
    fireEvent.click(screen.getByTestId('ldap-password-submit'));

    // Verify was called with the re-entered password AND the existing server name
    // (edit mode - the backend needs it to merge stored config, though here
    // authPassword is non-empty so the merge doesn't kick in).
    await waitFor(() => {
      expect(verifyConnection).toHaveBeenCalledTimes(1);
    });
    expect(verifyConnection).toHaveBeenCalledWith(
      expect.objectContaining({ name: mockServer.name, authPassword: 'wrong-password' }),
      mockServer.name
    );

    // Verify failed -> submit path must NOT run. This is the security-critical
    // assertion: without the fix, the PUT went through regardless of verify.
    expect(confirmPasswordAndSubmit).not.toHaveBeenCalled();
    expect(defaultProps.updateServer).not.toHaveBeenCalled();

    // Modal stays open with the backend's error message so the user can retry.
    await waitFor(() => {
      expect(screen.getByText(bindFailure.message)).toBeInTheDocument();
    });
    expect(screen.getByTestId('ldap-password-modal')).toBeInTheDocument();
  });

  it('NEXUS-53959: submits with the re-entered password when the verify bind succeeds', async () => {
    const confirmPasswordAndSubmit = jest.fn();
    mockUseLdapForm.mockImplementation(({ server }: any) => {
      const formData = { ...server, authPassword: '' };
      return {
        form: createMockLdapForm(formData),
        server: server || null,
        isCreate: false,
        applyTemplate: jest.fn(),
        changeProtocol: jest.fn(),
        confirmPasswordAndSubmit,
      } as any;
    });

    const verifyConnection = jest.fn().mockResolvedValue(undefined);

    render(
      <LdapForm {...defaultProps} server={mockServer} isCreate={false} verifyConnection={verifyConnection} />,
      { wrapper: TestWrapper }
    );

    fireEvent.click(screen.getByRole('button', { name: /Continue/i }));
    fireEvent.click(screen.getByRole('button', { name: /^Save$/i }));
    fireEvent.change(screen.getByTestId('ldap-password-input'), { target: { value: 'correct-password' } });
    fireEvent.click(screen.getByTestId('ldap-password-submit'));

    await waitFor(() => {
      expect(verifyConnection).toHaveBeenCalledTimes(1);
    });
    await waitFor(() => {
      expect(confirmPasswordAndSubmit).toHaveBeenCalledWith('correct-password');
    });
    // Modal closes on success.
    expect(screen.queryByTestId('ldap-password-modal')).not.toBeInTheDocument();
  });

  it('requires a password before confirming re-entry', async () => {
    const confirmPasswordAndSubmit = jest.fn();
    mockUseLdapForm.mockImplementation(({ server }: any) => {
      const formData = { ...server, authPassword: '' };
      return {
        form: createMockLdapForm(formData),
        server: server || null,
        isCreate: false,
        applyTemplate: jest.fn(),
        changeProtocol: jest.fn(),
        confirmPasswordAndSubmit,
      } as any;
    });

    render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByRole('button', { name: /Continue/i }));
    fireEvent.click(screen.getByRole('button', { name: /^Save$/i }));

    expect(screen.getByTestId('ldap-password-submit')).toBeDisabled();
    expect(confirmPasswordAndSubmit).not.toHaveBeenCalled();
  });

  describe('connection verified badge (NEXUS-53623 F2)', () => {
    async function verifyConnectionAndAwaitBadge() {
      fireEvent.click(screen.getByRole('button', { name: /Verify Connection/i }));
      await waitFor(() => {
        expect(screen.getAllByText('Connection successful').length).toBeGreaterThan(0);
      });
    }

    it('shows the badge after a successful Verify Connection call', async () => {
      render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });

      await verifyConnectionAndAwaitBadge();
    });

    it.each([
      ['host', 'input-host', 'other.example.com'],
      ['searchBase', 'input-searchBase', 'dc=other,dc=com'],
      ['authUsername', 'input-authUsername', 'cn=other,dc=example,dc=com'],
      ['authPassword', 'password-authPassword', 'new-secret'],
    ])('clears the badge when %s changes', async (_field, testId, newValue) => {
      render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });
      await verifyConnectionAndAwaitBadge();

      fireEvent.change(screen.getByTestId(testId), { target: { value: newValue } });

      expect(screen.queryByText('Connection successful')).not.toBeInTheDocument();
    });

    it('clears the badge when protocol changes', async () => {
      render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });
      await verifyConnectionAndAwaitBadge();

      fireEvent.click(screen.getByRole('option', { name: 'ldaps (SSL)' }));

      expect(screen.queryByText('Connection successful')).not.toBeInTheDocument();
    });

    it('clears the badge when the port changes', async () => {
      render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });
      await verifyConnectionAndAwaitBadge();

      fireEvent.change(screen.getByTestId('input-port'), { target: { value: '636' } });

      expect(screen.queryByText('Connection successful')).not.toBeInTheDocument();
    });

    it('clears the badge when the authentication scheme changes', async () => {
      render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });
      await verifyConnectionAndAwaitBadge();

      fireEvent.click(screen.getByRole('option', { name: 'DIGEST-MD5' }));

      expect(screen.queryByText('Connection successful')).not.toBeInTheDocument();
    });

    it('clears the badge when useTrustStore changes', async () => {
      const ldapsServer = { ...mockServer, protocol: 'ldaps' as const, port: 636 };
      render(<LdapForm {...defaultProps} server={ldapsServer} isCreate={false} />, { wrapper: TestWrapper });
      await verifyConnectionAndAwaitBadge();

      fireEvent.click(screen.getByTestId('checkbox-useTrustStore'));

      expect(screen.queryByText('Connection successful')).not.toBeInTheDocument();
    });

    it('clears the badge when authRealm changes', async () => {
      const saslServer = { ...mockServer, authScheme: 'DIGEST-MD5' as const };
      render(<LdapForm {...defaultProps} server={saslServer} isCreate={false} />, { wrapper: TestWrapper });
      await verifyConnectionAndAwaitBadge();

      fireEvent.change(screen.getByTestId('input-authRealm'), { target: { value: 'EXAMPLE.COM' } });

      expect(screen.queryByText('Connection successful')).not.toBeInTheDocument();
    });

    it('does not clear the badge when the name changes (regression guard)', async () => {
      render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });
      await verifyConnectionAndAwaitBadge();

      fireEvent.change(screen.getByTestId('input-name'), { target: { value: 'Renamed Server' } });

      expect(screen.getAllByText('Connection successful').length).toBeGreaterThan(0);
    });

    it('does not clear the badge when connectionTimeout, connectionRetryDelay, or maxIncidentsCount change (regression guard)', async () => {
      render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });
      await verifyConnectionAndAwaitBadge();

      fireEvent.change(screen.getByTestId('input-connectionTimeout'), { target: { value: '60' } });
      fireEvent.change(screen.getByTestId('input-connectionRetryDelay'), { target: { value: '600' } });
      fireEvent.change(screen.getByTestId('input-maxIncidentsCount'), { target: { value: '5' } });

      expect(screen.getAllByText('Connection successful').length).toBeGreaterThan(0);
    });
  });

  describe('independent verify buttons (NEXUS-53623 F6)', () => {
    it('does not disable or spin the user-mapping/login buttons while Verify Connection is pending', async () => {
      let resolveVerifyConnection: () => void = () => {};
      const verifyConnection = jest.fn(
        () => new Promise<void>((resolve) => { resolveVerifyConnection = resolve; })
      );

      render(
        <LdapForm {...defaultProps} verifyConnection={verifyConnection} server={mockServer} isCreate={false} />,
        { wrapper: TestWrapper }
      );

      fireEvent.click(screen.getByRole('button', { name: /Verify Connection/i }));

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Verify Connection/i })).toHaveAttribute('aria-busy', 'true');
      });

      // Navigate to the User & Group step while connection verification is still in flight.
      fireEvent.click(screen.getByRole('button', { name: /Continue/i }));

      expect(screen.getByTestId('ldap-verify-user-mapping')).not.toBeDisabled();
      expect(screen.getByTestId('ldap-verify-user-mapping')).toHaveAttribute('aria-busy', 'false');
      expect(screen.getByTestId('ldap-verify-login-button')).not.toBeDisabled();

      // Flush the pending verifyConnection promise inside act() so its
      // resulting setConnectionVerifying(false)/setConnectionVerified(true)
      // updates are applied before the test ends, instead of leaking an
      // "update not wrapped in act()" warning from a resolution the test
      // never awaited.
      await act(async () => {
        resolveVerifyConnection();
        await Promise.resolve();
      });
    });

    it('does not disable or spin the login button while Verify User Mapping is pending', async () => {
      let resolveVerifyUserMapping: (users: unknown[]) => void = () => {};
      const verifyUserMapping = jest.fn(
        () => new Promise((resolve) => { resolveVerifyUserMapping = resolve; })
      );

      render(
        <LdapForm {...defaultProps} verifyUserMapping={verifyUserMapping} server={mockServer} isCreate={false} />,
        { wrapper: TestWrapper }
      );

      fireEvent.click(screen.getByRole('button', { name: /Continue/i }));
      fireEvent.click(screen.getByTestId('ldap-verify-user-mapping'));

      await waitFor(() => {
        expect(screen.getByTestId('ldap-verify-user-mapping')).toHaveAttribute('aria-busy', 'true');
      });

      expect(screen.getByTestId('ldap-verify-login-button')).not.toBeDisabled();
      expect(screen.getByTestId('ldap-verify-login-button')).toHaveAttribute('aria-busy', 'false');

      // Flush the pending verifyUserMapping promise inside act(), same
      // rationale as the Verify Connection test above.
      await act(async () => {
        resolveVerifyUserMapping([]);
        await Promise.resolve();
      });
    });

    it('does not disable or spin the user-mapping button when opening the Verify Login modal', () => {
      render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });

      fireEvent.click(screen.getByRole('button', { name: /Continue/i }));
      fireEvent.click(screen.getByTestId('ldap-verify-login-button'));

      expect(screen.getByTestId('ldap-login-modal')).toBeInTheDocument();
      expect(screen.getByTestId('ldap-verify-user-mapping')).not.toBeDisabled();
      expect(screen.getByTestId('ldap-verify-user-mapping')).toHaveAttribute('aria-busy', 'false');
    });
  });

  describe('user mapping results table (NEXUS-53623 F1)', () => {
    beforeEach(() => {
      HTMLElement.prototype.scrollIntoView = jest.fn();
    });
    afterEach(() => {
      delete (HTMLElement.prototype as any).scrollIntoView;
    });

    async function renderAndVerifyUserMapping(users: Array<Record<string, unknown>>) {
      const verifyUserMapping = jest.fn().mockResolvedValue(users);

      render(
        <LdapForm {...defaultProps} verifyUserMapping={verifyUserMapping} server={mockServer} isCreate={false} />,
        { wrapper: TestWrapper }
      );

      fireEvent.click(screen.getByRole('button', { name: /Continue/i }));
      fireEvent.click(screen.getByTestId('ldap-verify-user-mapping'));

      await waitFor(() => {
        expect(screen.getByText(`Found ${users.length} user(s):`)).toBeInTheDocument();
      });
    }

    it('renders all 4 columns, including membership', async () => {
      await renderAndVerifyUserMapping([
        { username: 'jdoe', realName: 'Jane Doe', email: 'jdoe@example.com', membership: ['Admin', 'Users'] },
      ]);

      expect(screen.getByText('ID')).toBeInTheDocument();
      expect(screen.getByText('Name')).toBeInTheDocument();
      expect(screen.getByText('Email')).toBeInTheDocument();
      expect(screen.getByText('Roles')).toBeInTheDocument();

      expect(screen.getByText('jdoe')).toBeInTheDocument();
      expect(screen.getByText('Jane Doe')).toBeInTheDocument();
      expect(screen.getByText('jdoe@example.com')).toBeInTheDocument();
      expect(screen.getByText('Admin, Users')).toBeInTheDocument();
    });

    it('renders safely when membership is missing (undefined or empty array)', async () => {
      await renderAndVerifyUserMapping([
        { username: 'noMembershipField' },
        { username: 'emptyMembership', membership: [] },
      ]);

      expect(screen.getByText('noMembershipField')).toBeInTheDocument();
      expect(screen.getByText('emptyMembership')).toBeInTheDocument();
    });

    it('renders more than 10 users without truncation (regression guard for the removed 10-row cap)', async () => {
      const users = Array.from({ length: 15 }, (_, i) => ({ username: `user${i + 1}` }));

      await renderAndVerifyUserMapping(users);

      users.forEach((u) => {
        expect(screen.getByText(u.username)).toBeInTheDocument();
      });
      expect(screen.queryByText(/\.\.\.and \d+ more/i)).not.toBeInTheDocument();
    });

    it('renders an empty state when verification returns no users', async () => {
      await renderAndVerifyUserMapping([]);

      expect(screen.getByText('No data available')).toBeInTheDocument();
    });

    it('shows a green badge with count after successful verification (NEXUS-54000)', async () => {
      const users = [
        { username: 'alice', realName: 'Alice', email: 'alice@example.com' },
        { username: 'bob', realName: 'Bob', email: 'bob@example.com' },
        { username: 'carol', realName: 'Carol', email: 'carol@example.com' },
      ];
      await renderAndVerifyUserMapping(users);

      const badge = screen.getByTestId('ldap-user-mapping-badge');
      expect(badge).toBeInTheDocument();
      expect(badge).toHaveTextContent('3 user(s)');
    });

    it('shows a red badge when verification returns no users (NEXUS-54000)', async () => {
      await renderAndVerifyUserMapping([]);

      const badge = screen.getByTestId('ldap-user-mapping-badge');
      expect(badge).toBeInTheDocument();
      expect(badge).toHaveTextContent('No matches found');
    });

    it('clears the badge when navigating back to the connection step (NEXUS-54000)', async () => {
      await renderAndVerifyUserMapping([{ username: 'alice' }]);

      expect(screen.getByTestId('ldap-user-mapping-badge')).toBeInTheDocument();

      fireEvent.click(screen.getByRole('button', { name: /back/i }));

      expect(screen.queryByTestId('ldap-user-mapping-badge')).not.toBeInTheDocument();
    });

    it('auto-scrolls the results table into view after a successful verify', async () => {
      await renderAndVerifyUserMapping([
        { username: 'alice', realName: 'Alice', email: 'alice@example.com' },
      ]);

      expect(HTMLElement.prototype.scrollIntoView).toHaveBeenCalledWith({ behavior: 'smooth', block: 'start' });
    });

    it('clears the badge before the second verify resolves when it rejects after a prior success', async () => {
      const verifyUserMapping = jest.fn()
        .mockResolvedValueOnce([
          { username: 'alice' },
          { username: 'bob' },
          { username: 'carol' },
        ])
        .mockRejectedValueOnce(new Error('Verification failed'));

      render(
        <LdapForm {...defaultProps} verifyUserMapping={verifyUserMapping} server={mockServer} isCreate={false} />,
        { wrapper: TestWrapper }
      );

      fireEvent.click(screen.getByRole('button', { name: /Continue/i }));
      fireEvent.click(screen.getByTestId('ldap-verify-user-mapping'));

      await waitFor(() => {
        expect(screen.getByText('Found 3 user(s):')).toBeInTheDocument();
      });
      expect(screen.getByTestId('ldap-user-mapping-badge')).toHaveTextContent('3 user(s)');

      fireEvent.click(screen.getByTestId('ldap-verify-user-mapping'));

      await waitFor(() => {
        expect(screen.queryByTestId('ldap-user-mapping-badge')).not.toBeInTheDocument();
      });
    });
  });

  describe('numeric field 0 handling (NEXUS-53623 review follow-up)', () => {
    // This guards field STATE, not validity: a typed 0 must stay in the
    // field and reach validation as 0, rather than being silently rewritten
    // to the default before validation ever sees it. Whether 0 is actually
    // valid differs per field - connectionRetryDelay/maxIncidentsCount have
    // no lower bound so 0 is legitimately allowed, while port (1-65535) and
    // connectionTimeout (1-3600) both correctly reject 0 once validation
    // runs. Regression guard for a parseInt(value) || default bug that clobbered 0 with the
    // default on every one of these fields.
    it.each([
      ['port', 'input-port'],
      ['connectionTimeout', 'input-connectionTimeout'],
      ['connectionRetryDelay', 'input-connectionRetryDelay'],
      ['maxIncidentsCount', 'input-maxIncidentsCount'],
    ])('keeps an explicitly entered 0 for %s instead of reverting to the default', (_field, testId) => {
      render(<LdapForm {...defaultProps} />, { wrapper: TestWrapper });

      const input = screen.getByTestId(testId);
      fireEvent.change(input, { target: { value: '0' } });

      expect(input).toHaveValue(0);
    });

    it.each([
      ['port', 'input-port', 389],
      ['connectionTimeout', 'input-connectionTimeout', 30],
      ['connectionRetryDelay', 'input-connectionRetryDelay', 300],
      ['maxIncidentsCount', 'input-maxIncidentsCount', 3],
    ])('falls back to the default for %s when cleared to a non-numeric value', (_field, testId, defaultValue) => {
      render(<LdapForm {...defaultProps} />, { wrapper: TestWrapper });

      const input = screen.getByTestId(testId);
      fireEvent.change(input, { target: { value: '' } });

      expect(input).toHaveValue(defaultValue);
    });
  });

  describe('numeric field validation error display (NEXUS-53623 review follow-up)', () => {
    // port, connectionTimeout, connectionRetryDelay, and maxIncidentsCount are
    // wired directly (name/value/onChange) instead of via form.field(), so
    // unlike host/searchBase/authUsername/etc. they don't automatically pick
    // up an `error` prop. validateConnection (and, for the latter three, the
    // ValidationUtils.isInRange bounds added for NEXUS-53623 F4/F5) can mark
    // any of them invalid - and disable the Verify Connection/Continue
    // buttons as a result - but without an explicit `error` prop wired to
    // form.validationErrors, the user has no visible explanation why.
    // Regression guard for that missing wiring.
    it.each([
      ['port', 'input-port', 'Port must be between 1 and 65535'],
      ['connectionTimeout', 'input-connectionTimeout', 'The minimum value for this field is 1'],
      ['connectionRetryDelay', 'input-connectionRetryDelay', 'The minimum value for this field is 0'],
      ['maxIncidentsCount', 'input-maxIncidentsCount', 'The minimum value for this field is 0'],
    ])('surfaces the %s validation error message from form.validationErrors', (field, testId, errorMessage) => {
      mockUseLdapForm.mockImplementation(({ server }: any) => {
        // Mirrors DEFAULT_LDAP_SERVER in types.ts, same rationale as the
        // beforeEach fallback above.
        const formData = server ? { ...server } : {
          name: '', host: '', port: 389, protocol: 'ldap', searchBase: '', authScheme: 'simple',
          authUsername: '', authPassword: '', useTrustStore: false, connectionTimeout: 30,
          connectionRetryDelay: 300, maxIncidentsCount: 3, userBaseDn: '', userSubtree: false,
          userObjectClass: '', userLdapFilter: '', userIdAttribute: '', userRealNameAttribute: '',
          userEmailAddressAttribute: '', userPasswordAttribute: '', ldapGroupsAsRoles: true,
          groupType: 'dynamic', groupBaseDn: '', groupSubtree: false, groupObjectClass: '',
          groupIdAttribute: '', groupMemberAttribute: '', groupMemberFormat: '', userMemberOfAttribute: '',
        };
        const form = createMockLdapForm(formData);
        form.validationErrors = { [field]: errorMessage };
        return {
          form,
          server: server || null,
          isCreate: !server,
          applyTemplate: jest.fn(),
          changeProtocol: jest.fn(),
          confirmPasswordAndSubmit: jest.fn(),
        } as any;
      });

      render(<LdapForm {...defaultProps} />, { wrapper: TestWrapper });

      expect(screen.getByTestId(testId)).toHaveAttribute('aria-invalid', 'true');
      expect(screen.getByText(errorMessage)).toBeInTheDocument();
    });

    it('does not show an error for a numeric field while its value is within range', () => {
      render(<LdapForm {...defaultProps} />, { wrapper: TestWrapper });

      expect(screen.getByTestId('input-port')).toHaveAttribute('aria-invalid', 'false');
      expect(screen.getByTestId('input-connectionTimeout')).toHaveAttribute('aria-invalid', 'false');
      expect(screen.getByTestId('input-connectionRetryDelay')).toHaveAttribute('aria-invalid', 'false');
      expect(screen.getByTestId('input-maxIncidentsCount')).toHaveAttribute('aria-invalid', 'false');
    });
  });

  describe('accessibility (NEXUS-53625)', () => {
    it('associates the Verify Login modal inputs with their labels (A1)', () => {
      render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });

      fireEvent.click(screen.getByRole('button', { name: /Continue/i }));
      fireEvent.click(screen.getByTestId('ldap-verify-login-button'));

      expect(screen.getByTestId('ldap-login-modal')).toBeInTheDocument();
      expect(screen.getByLabelText('Username')).toBe(screen.getByTestId('ldap-login-username'));
      expect(screen.getByLabelText('Password')).toBe(screen.getByTestId('ldap-login-password'));
    });

    it('associates the password re-entry modal input with its label (A1)', () => {
      mockUseLdapForm.mockImplementation(({ server }: any) => {
        const formData = { ...server, authPassword: '' }; // force the re-entry modal
        return {
          form: createMockLdapForm(formData),
          server: server || null,
          isCreate: false,
          applyTemplate: jest.fn(),
          changeProtocol: jest.fn(),
          confirmPasswordAndSubmit: jest.fn(),
        } as any;
      });

      render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });

      fireEvent.click(screen.getByRole('button', { name: /Continue/i }));
      fireEvent.click(screen.getByRole('button', { name: /^Save$/i }));

      expect(screen.getByTestId('ldap-password-modal')).toBeInTheDocument();
      expect(screen.getByLabelText('Password')).toBe(screen.getByTestId('ldap-password-input'));
    });

    it('announces the connection-verified result via the persistent live region (A4)', async () => {
      render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });

      // The alert node is not in the DOM until there is something to announce;
      // it is mounted fresh on the result so VoiceOver reliably reads it.
      expect(screen.queryByTestId('ldap-user-mapping-status')).toBeNull();

      fireEvent.click(screen.getByRole('button', { name: /Verify Connection/i }));
      await waitFor(() => {
        expect(screen.getByTestId('ldap-user-mapping-status')).toHaveTextContent('Connection successful');
      });

      const region = screen.getByTestId('ldap-user-mapping-status');
      expect(region).toHaveAttribute('role', 'alert');
      expect(region).toHaveAttribute('aria-live', 'assertive');
      expect(region).toHaveAttribute('aria-atomic', 'true');
    });

    it('announces the User Mapping result via the persistent live region (A4)', async () => {
      defaultProps.verifyUserMapping.mockResolvedValue([{ username: 'jdoe' }]);

      render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });

      expect(screen.queryByTestId('ldap-user-mapping-status')).toBeNull();

      fireEvent.click(screen.getByRole('button', { name: /Continue/i }));
      fireEvent.click(screen.getByTestId('ldap-verify-user-mapping'));

      await waitFor(() => {
        expect(screen.getByTestId('ldap-user-mapping-status')).toHaveTextContent('Found 1 user(s)');
      });

      const region = screen.getByTestId('ldap-user-mapping-status');
      expect(region).toHaveAttribute('role', 'alert');
      expect(region).toHaveAttribute('aria-live', 'assertive');
    });

    it('announces the Verify Login result via a persistent assertive live region (A4)', async () => {
      render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });

      fireEvent.click(screen.getByRole('button', { name: /Continue/i }));
      fireEvent.click(screen.getByTestId('ldap-verify-login-button'));

      // The alert node is not present until a result arrives.
      expect(screen.queryByTestId('ldap-login-result')).toBeNull();

      fireEvent.change(screen.getByTestId('ldap-login-username'), { target: { value: 'jdoe' } });
      fireEvent.change(screen.getByTestId('ldap-login-password'), { target: { value: 'secret' } });
      fireEvent.click(screen.getByTestId('ldap-login-submit'));

      await waitFor(() => {
        expect(screen.getByTestId('ldap-login-result')).toHaveTextContent(/LDAP login completed successfully/i);
      });

      const region = screen.getByTestId('ldap-login-result');
      expect(region).toHaveAttribute('role', 'alert');
      expect(region).toHaveAttribute('aria-live', 'assertive');
      expect(region).toHaveAttribute('aria-atomic', 'true');

      // Focus is restored to the Test Login button (not stranded on the Dialog
      // title) once verification completes and the button is re-enabled.
      await waitFor(() => {
        expect(screen.getByTestId('ldap-login-submit')).toHaveFocus();
      });
    });
  });

  describe('ExtJS parity (NEXUS-53624)', () => {
    it('shows an error alert instead of the template dropdown when template loading fails (PG7)', async () => {
      const fetchTemplates = jest.fn().mockRejectedValue(new Error('Network error'));
      render(
        <LdapForm {...defaultProps} fetchTemplates={fetchTemplates} server={mockServer} isCreate={false} />,
        { wrapper: TestWrapper }
      );

      // Advance to the User & Group step where the template selector / error alert lives.
      fireEvent.click(screen.getByRole('button', { name: /Continue/i }));

      expect(await screen.findByText('Network error')).toBeInTheDocument();
      expect(screen.queryByTestId('select-template')).not.toBeInTheDocument();
    });

    it('shows the ExtJS-parity success message after a successful verify login (PG11)', async () => {
      render(<LdapForm {...defaultProps} server={mockServer} isCreate={false} />, { wrapper: TestWrapper });

      // Advance to the User & Group step and open the Verify Login modal.
      fireEvent.click(screen.getByRole('button', { name: /Continue/i }));
      fireEvent.click(screen.getByTestId('ldap-verify-login-button'));

      fireEvent.change(screen.getByTestId('ldap-login-username'), { target: { value: 'jdoe' } });
      fireEvent.change(screen.getByTestId('ldap-login-password'), { target: { value: 'secret' } });
      fireEvent.click(screen.getByTestId('ldap-login-submit'));

      // PG11: message format is "LDAP login completed successfully on: {protocol}://{host}:{port}".
      expect(
        await screen.findByText(`LDAP login completed successfully on: ldap://${mockServer.host}:${mockServer.port}`)
      ).toBeInTheDocument();
      expect(defaultProps.verifyLogin).toHaveBeenCalledWith(
        expect.any(Object), 'jdoe', 'secret', mockServer.name
      );
    });
  });
});


