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

import { SamlPage } from '../SamlPage';
import * as useSamlApiModule from '../useSamlApi';
import { ToastProvider } from '../../../../../shared/Toast';
import { parseSignatureValidation } from '../SamlPage';

// Mock the API hook
jest.mock('../useSamlApi');

const mockedUseSamlApi = useSamlApiModule.useSamlApi as jest.MockedFunction<typeof useSamlApiModule.useSamlApi>;

// Import the ExtJS mock class directly to spy on it
// Jest automatically uses __mocks__/ExtJS.js for mocks
import ExtJS from '../../../../../../../interface/ExtJS';

// Set up spy before all tests
const checkPermissionSpy = jest.spyOn(ExtJS, 'checkPermission');

// Mock clipboard API
Object.assign(navigator, {
  clipboard: {
    writeText: jest.fn(),
  },
});

// Wrapper component for Radix Theme and Toast context
function TestWrapper({ children }: { children: React.ReactNode }) {
  return (
    <Theme>
      <ToastProvider>{children}</ToastProvider>
    </Theme>
  );
}

describe('SamlPage', () => {
  const mockConfiguration = {
    entityId: 'https://nexus.example.com',
    idpMetadata: '<EntityDescriptor>...</EntityDescriptor>',
    usernameAttribute: 'email',
    firstNameAttribute: 'firstName',
    lastNameAttribute: 'lastName',
    emailAttribute: 'email',
    groupsAttribute: 'groups',
    validateResponseSignature: true,
    validateAssertionSignature: true,
  };

  const mockFetchConfiguration = jest.fn();
  const mockSaveConfiguration = jest.fn();
  const mockDeleteConfiguration = jest.fn();
  const mockGetMetadataUrl = jest.fn();
  const mockSetError = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    checkPermissionSpy.mockReturnValue(true);
    (navigator.clipboard.writeText as jest.Mock).mockClear();
    mockGetMetadataUrl.mockReturnValue('/service/rest/v1/security/saml/metadata');

    mockedUseSamlApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchConfiguration: mockFetchConfiguration.mockResolvedValue(null),
      saveConfiguration: mockSaveConfiguration.mockResolvedValue(undefined),
      deleteConfiguration: mockDeleteConfiguration.mockResolvedValue(undefined),
      getMetadataUrl: mockGetMetadataUrl,
    });
  });

  it('renders loading state initially', () => {
    render(<SamlPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Loading SAML configuration...')).toBeInTheDocument();
  });

  it('renders the page header', async () => {
    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'SAML' })).toBeInTheDocument();
    });

    expect(screen.getByText('Configure SAML authentication with your Identity Provider')).toBeInTheDocument();
  });

  it('shows info callout when not configured', async () => {
    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText(/SAML is not yet configured/)).toBeInTheDocument();
    });
  });

  it('shows configured badge when SAML is configured', async () => {
    mockFetchConfiguration.mockResolvedValue(mockConfiguration);

    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Configured')).toBeInTheDocument();
    });
  });

  it('displays form fields', async () => {
    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      // Use testid selectors for SettingsTextInput/SettingsTextArea
      expect(screen.getByTestId('input-entityId')).toBeInTheDocument();
      expect(screen.getByTestId('textarea-idpMetadata')).toBeInTheDocument();
      expect(screen.getByTestId('input-usernameAttribute')).toBeInTheDocument();
      expect(screen.getByTestId('input-firstNameAttribute')).toBeInTheDocument();
      expect(screen.getByTestId('input-lastNameAttribute')).toBeInTheDocument();
      expect(screen.getByTestId('input-emailAttribute')).toBeInTheDocument();
      expect(screen.getByTestId('input-groupsAttribute')).toBeInTheDocument();
    });
  });

  it('displays signature validation selects', async () => {
    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      // Use testid selectors for SettingsSelect (not checkboxes)
      expect(screen.getByTestId('select-validateResponseSignature')).toBeInTheDocument();
      expect(screen.getByTestId('select-validateAssertionSignature')).toBeInTheDocument();
    });
  });

  it('populates form with existing configuration', async () => {
    mockFetchConfiguration.mockResolvedValue(mockConfiguration);

    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      // Use specific inputs to check values
      const entityIdInput = screen.getByTestId('input-entityId') as HTMLInputElement;
      const usernameInput = screen.getByTestId('input-usernameAttribute') as HTMLInputElement;
      expect(entityIdInput.value).toBe('https://nexus.example.com');
      expect(usernameInput.value).toBe('email');
    });
  });

  it('validates required fields before saving', async () => {
    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-entityId')).toBeInTheDocument();
    });

    // Verify required fields have required attribute
    const metadataTextarea = screen.getByTestId('textarea-idpMetadata');
    const usernameInput = screen.getByTestId('input-usernameAttribute');
    expect(metadataTextarea).toHaveAttribute('required');
    expect(usernameInput).toHaveAttribute('required');
  });

  it('calls saveConfiguration when form is valid', async () => {
    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('textarea-idpMetadata')).toBeInTheDocument();
    });

    // Fill required fields
    const metadataTextarea = screen.getByTestId('textarea-idpMetadata');
    await act(async () => {
      fireEvent.change(metadataTextarea, {
        target: { value: '<EntityDescriptor>test</EntityDescriptor>' },
      });
    });

    const usernameInput = screen.getByTestId('input-usernameAttribute');
    await act(async () => {
      fireEvent.change(usernameInput, {
        target: { value: '  email  ' }, // Include spaces to test trimming
      });
    });

    // Submit the form
    const saveButton = screen.getByRole('button', { name: /save/i });
    await act(async () => {
      fireEvent.click(saveButton);
    });

    await waitFor(() => {
      expect(mockSaveConfiguration).toHaveBeenCalledTimes(1);
      // Verify the payload includes trimmed attributes and null for signature validation defaults
      expect(mockSaveConfiguration).toHaveBeenCalledWith(
        expect.objectContaining({
          idpMetadata: '<EntityDescriptor>test</EntityDescriptor>',
          usernameAttribute: 'email', // trimmed
          validateResponseSignature: null,
          validateAssertionSignature: null,
        })
      );
    });
  });

  it('shows success message after saving', async () => {
    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('textarea-idpMetadata')).toBeInTheDocument();
    });

    // Fill required fields
    const metadataTextarea = screen.getByTestId('textarea-idpMetadata');
    await act(async () => {
      fireEvent.change(metadataTextarea, {
        target: { value: '<EntityDescriptor>test</EntityDescriptor>' },
      });
    });

    const usernameInput = screen.getByTestId('input-usernameAttribute');
    await act(async () => {
      fireEvent.change(usernameInput, {
        target: { value: 'email' },
      });
    });

    // Submit the form
    const saveButton = screen.getByRole('button', { name: /save/i });
    await act(async () => {
      fireEvent.click(saveButton);
    });

    // Verify success toast appears
    await waitFor(() => {
      expect(screen.getByText('SAML configuration saved successfully')).toBeInTheDocument();
    });
  });

  it('shows metadata URL when configured', async () => {
    mockFetchConfiguration.mockResolvedValue(mockConfiguration);

    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText(/\/service\/rest\/v1\/security\/saml\/metadata/)).toBeInTheDocument();
    });
  });

  it('copies metadata URL to clipboard', async () => {
    mockFetchConfiguration.mockResolvedValue(mockConfiguration);

    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Copy')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Copy'));

    await waitFor(() => {
      expect(navigator.clipboard.writeText).toHaveBeenCalled();
      expect(screen.getByText('Metadata URL copied to clipboard')).toBeInTheDocument();
    });
  });

  it('copies absolute metadata URL to clipboard with context path', async () => {
    // Mock ExtJS.urlOf to return relative URL with context path
    const mockUrlOf = jest.spyOn(ExtJS, 'urlOf').mockReturnValue('/nexus/service/rest/v1/security/saml/metadata');

    mockFetchConfiguration.mockResolvedValue(mockConfiguration);

    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Copy')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Copy'));

    await waitFor(() => {
      expect(navigator.clipboard.writeText).toHaveBeenCalledWith(
        expect.stringMatching(/^https?:\/\/localhost\/nexus\/service\/rest\/v1\/security\/saml\/metadata$/)
      );
    });

    mockUrlOf.mockRestore();
  });

  it('opens absolute metadata URL in new tab', async () => {
    // Mock ExtJS.urlOf to return relative URL with context path
    const mockUrlOf = jest.spyOn(ExtJS, 'urlOf').mockReturnValue('/nexus/service/rest/v1/security/saml/metadata');
    const mockWindowOpen = jest.spyOn(window, 'open').mockImplementation();

    mockFetchConfiguration.mockResolvedValue(mockConfiguration);

    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Open')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Open'));

    await waitFor(() => {
      expect(mockWindowOpen).toHaveBeenCalledWith(
        expect.stringMatching(/^https?:\/\/localhost\/nexus\/service\/rest\/v1\/security\/saml\/metadata$/),
        '_blank'
      );
    });

    mockUrlOf.mockRestore();
    mockWindowOpen.mockRestore();
  });

  it('shows delete confirmation when Delete button is clicked', async () => {
    mockFetchConfiguration.mockResolvedValue(mockConfiguration);

    render(<SamlPage />, { wrapper: TestWrapper });

    // Wait for the configuration to be loaded (Configured badge appears)
    await waitFor(() => {
      expect(screen.getByText('Configured')).toBeInTheDocument();
    });

    // Click the Delete Configuration button
    const deleteButton = screen.getByTestId('saml-delete-button');
    await act(async () => {
      fireEvent.click(deleteButton);
    });

    // Verify the confirmation dialog appears
    await waitFor(() => {
      expect(screen.getByText('Delete SAML Configuration')).toBeInTheDocument();
      expect(screen.getByText(/Are you sure you want to delete/)).toBeInTheDocument();
    });
  });

  it('deletes configuration when confirmed', async () => {
    mockFetchConfiguration.mockResolvedValue(mockConfiguration);

    render(<SamlPage />, { wrapper: TestWrapper });

    // Wait for the configuration to be loaded
    await waitFor(() => {
      expect(screen.getByText('Configured')).toBeInTheDocument();
    });

    // Click the Delete Configuration button
    const deleteButton = screen.getByTestId('saml-delete-button');
    await act(async () => {
      fireEvent.click(deleteButton);
    });

    // Confirm deletion in the dialog
    await waitFor(() => {
      expect(screen.getByText('Delete SAML Configuration')).toBeInTheDocument();
    });

    const confirmDeleteButton = screen.getByRole('button', { name: /delete configuration/i });
    await act(async () => {
      fireEvent.click(confirmDeleteButton);
    });

    // Verify deleteConfiguration was called and form resets to unconfigured state
    await waitFor(() => {
      expect(mockDeleteConfiguration).toHaveBeenCalledTimes(1);
      expect(screen.getByText('Not Configured')).toBeInTheDocument();
    });
  });

  it('cancels delete when Cancel is clicked in delete dialog', async () => {
    mockFetchConfiguration.mockResolvedValue(mockConfiguration);

    render(<SamlPage />, { wrapper: TestWrapper });

    // Wait for configuration to load
    await waitFor(() => {
      expect(screen.getByText('Configured')).toBeInTheDocument();
    });

    // Click the Delete Configuration button
    const deleteButton = screen.getByTestId('saml-delete-button');
    await act(async () => {
      fireEvent.click(deleteButton);
    });

    // Verify delete confirmation dialog appears
    await waitFor(() => {
      expect(screen.getByText('Delete SAML Configuration')).toBeInTheDocument();
    });

    // Click Cancel in the dialog
    const cancelButton = screen.getByRole('button', { name: /cancel/i });
    await act(async () => {
      fireEvent.click(cancelButton);
    });

    // Verify dialog closes and deleteConfiguration was NOT called
    await waitFor(() => {
      expect(screen.queryByText('Delete SAML Configuration')).not.toBeInTheDocument();
      expect(mockDeleteConfiguration).not.toHaveBeenCalled();
      // Configuration should still be shown as configured
      expect(screen.getByText('Configured')).toBeInTheDocument();
    });
  });

  it('displays error message when error occurs', async () => {
    mockedUseSamlApi.mockReturnValue({
      loading: false,
      error: 'Failed to save SAML configuration',
      setError: mockSetError,
      fetchConfiguration: mockFetchConfiguration.mockResolvedValue(null),
      saveConfiguration: mockSaveConfiguration,
      deleteConfiguration: mockDeleteConfiguration,
      getMetadataUrl: mockGetMetadataUrl,
    });

    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Failed to save SAML configuration')).toBeInTheDocument();
    });
  });

  it('resets form when Cancel is clicked', async () => {
    mockFetchConfiguration.mockResolvedValue(mockConfiguration);

    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      // Form is populated with configuration
      const usernameInput = screen.getByTestId('input-usernameAttribute') as HTMLInputElement;
      expect(usernameInput.value).toBe('email');
    });

    // Modify a field
    const usernameInput = screen.getByTestId('input-usernameAttribute') as HTMLInputElement;
    await act(async () => {
      fireEvent.change(usernameInput, {
        target: { value: 'modified' },
      });
    });
    expect(usernameInput.value).toBe('modified');

    // Click discard - should reset to original fetched value
    const discardButton = screen.getByRole('button', { name: /discard/i });
    await act(async () => {
      fireEvent.click(discardButton);
    });

    // SettingsForm shows confirmation dialog for dirty forms
    const leaveButton = await screen.findByRole('button', { name: /leave/i });
    await act(async () => {
      fireEvent.click(leaveButton);
    });

    // Should be back to original value
    await waitFor(() => {
      expect(screen.getByTestId('input-usernameAttribute')).toHaveValue('email');
    }, { timeout: 5000 });
  });

  it('clears validation errors when Discard is clicked', async () => {
    mockFetchConfiguration.mockResolvedValue(mockConfiguration);

    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-usernameAttribute')).toBeInTheDocument();
    });

    // Clear a required field to trigger validation error
    const usernameInput = screen.getByTestId('input-usernameAttribute') as HTMLInputElement;
    await act(async () => {
      fireEvent.change(usernameInput, { target: { value: '' } });
    });

    // Click Save to trigger validation
    const saveButton = screen.getByRole('button', { name: /save/i });
    await act(async () => {
      fireEvent.click(saveButton);
    });

    // Verify validation error appears
    await waitFor(() => {
      expect(screen.getByText('Username Attribute is required')).toBeInTheDocument();
    });

    // Click Discard
    const discardButton = screen.getByRole('button', { name: /discard/i });
    await act(async () => {
      fireEvent.click(discardButton);
    });

    // Confirm leave in dialog
    const leaveButton = await screen.findByRole('button', { name: /leave/i });
    await act(async () => {
      fireEvent.click(leaveButton);
    });

    // Verify validation error is cleared AND value is restored
    await waitFor(() => {
      expect(screen.queryByText('Username Attribute is required')).not.toBeInTheDocument();
      expect(screen.getByTestId('input-usernameAttribute')).toHaveValue('email');
    });
  });

  // New tests

  it('validates Entity ID URI format', async () => {
    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-entityId')).toBeInTheDocument();
    });

    // Fill required fields first
    const metadataTextarea = screen.getByTestId('textarea-idpMetadata');
    await act(async () => {
      fireEvent.change(metadataTextarea, {
        target: { value: '<EntityDescriptor>test</EntityDescriptor>' },
      });
    });

    const usernameInput = screen.getByTestId('input-usernameAttribute');
    await act(async () => {
      fireEvent.change(usernameInput, {
        target: { value: 'email' },
      });
    });

    // Enter an invalid URI (no scheme)
    const entityIdInput = screen.getByTestId('input-entityId');
    await act(async () => {
      fireEvent.change(entityIdInput, {
        target: { value: 'not-a-uri' },
      });
    });

    // Submit the form
    const saveButton = screen.getByRole('button', { name: /save/i });
    await act(async () => {
      fireEvent.click(saveButton);
    });

    // Should show validation error
    await waitFor(() => {
      expect(screen.getByText('Entity ID must be a URI')).toBeInTheDocument();
    });

    // Now enter a valid URI
    await act(async () => {
      fireEvent.change(entityIdInput, {
        target: { value: 'http://example.com' },
      });
    });

    await act(async () => {
      fireEvent.click(saveButton);
    });

    // Should not show the error anymore and save should succeed
    await waitFor(() => {
      expect(screen.queryByText('Entity ID must be a URI')).not.toBeInTheDocument();
      expect(mockSaveConfiguration).toHaveBeenCalled();
    });
  });

  it('displays signature validation tri-state correctly', async () => {
    // Test with true/false values
    mockFetchConfiguration.mockResolvedValue({
      ...mockConfiguration,
      validateResponseSignature: true,
      validateAssertionSignature: false,
    });

    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('select-validateResponseSignature')).toBeInTheDocument();
    });

    // Check that selects show 'true' and 'false' respectively
    const responseSelect = screen.getByTestId('select-validateResponseSignature');
    const assertionSelect = screen.getByTestId('select-validateAssertionSignature');

    // The select trigger shows the selected value's label
    expect(responseSelect).toHaveTextContent('True');
    expect(assertionSelect).toHaveTextContent('False');
  });

  it('displays signature validation default (null) correctly', async () => {
    // Test with null values (default)
    mockFetchConfiguration.mockResolvedValue({
      ...mockConfiguration,
      validateResponseSignature: null,
      validateAssertionSignature: null,
    });

    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('select-validateResponseSignature')).toBeInTheDocument();
    });

    // Check that selects show 'Default'
    const responseSelect = screen.getByTestId('select-validateResponseSignature');
    const assertionSelect = screen.getByTestId('select-validateAssertionSignature');

    expect(responseSelect).toHaveTextContent('Default');
    expect(assertionSelect).toHaveTextContent('Default');
  });

  it('disables all inputs when user lacks update permission', async () => {
    // Mock no update permission - must be set before render
    checkPermissionSpy.mockReturnValue(false);

    await act(async () => {
      render(<SamlPage />, { wrapper: TestWrapper });
    });

    // Verify the permission check was called with the right argument
    expect(checkPermissionSpy).toHaveBeenCalledWith('nexus:saml:update');

    await waitFor(() => {
      expect(screen.getByTestId('textarea-idpMetadata')).toBeInTheDocument();
    });

    // Verify all inputs are disabled
    const metadataTextarea = screen.getByTestId('textarea-idpMetadata');
    const entityIdInput = screen.getByTestId('input-entityId');
    const usernameInput = screen.getByTestId('input-usernameAttribute');
    const responseSelect = screen.getByTestId('select-validateResponseSignature');

    expect(metadataTextarea).toBeDisabled();
    expect(entityIdInput).toBeDisabled();
    expect(usernameInput).toBeDisabled();
    expect(responseSelect).toBeDisabled();

    // Verify Save and Discard buttons are not rendered
    expect(screen.queryByRole('button', { name: /save/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /discard/i })).not.toBeInTheDocument();

    // Verify read-only warning callout is visible
    expect(screen.getByText(/You do not have permission to modify SAML settings/)).toBeInTheDocument();
  });

  it('pre-populates Entity ID when no configuration exists', async () => {
    // No existing configuration
    mockFetchConfiguration.mockResolvedValue(null);

    await act(async () => {
      render(<SamlPage />, { wrapper: TestWrapper });
    });

    await waitFor(() => {
      expect(screen.getByTestId('input-entityId')).toBeInTheDocument();
    });

    // Entity ID should be pre-populated with the metadata URL
    const entityIdInput = screen.getByTestId('input-entityId') as HTMLInputElement;

    // The mock ExtJS.urlOf returns `http://localhost:8081${path}`
    // However, new URL() resolves against window.location.href which in Jest defaults to http://localhost/
    // So we check that the URL contains the expected path and is an absolute URI
    expect(entityIdInput.value).toMatch(/^https?:\/\/.+\/service\/rest\/v1\/security\/saml\/metadata$/);
    expect(entityIdInput.value).toContain('/service/rest/v1/security/saml/metadata');
  });

  it('generates absolute Entity ID URL with context path', async () => {
    // Mock ExtJS.urlOf to return a relative URL with context path
    const mockUrlOf = jest.spyOn(ExtJS, 'urlOf').mockReturnValue('/nexus/service/rest/v1/security/saml/metadata');

    mockFetchConfiguration.mockResolvedValue(null);

    await act(async () => {
      render(<SamlPage />, { wrapper: TestWrapper });
    });

    await waitFor(() => {
      expect(screen.getByTestId('input-entityId')).toBeInTheDocument();
    });

    const entityIdInput = screen.getByTestId('input-entityId') as HTMLInputElement;

    // The relative URL should be resolved to an absolute URI
    // Jest's window.location.href is http://localhost/ so result should be http://localhost/nexus/service/rest/v1/security/saml/metadata
    expect(entityIdInput.value).toMatch(/^https?:\/\/localhost\/nexus\/service\/rest\/v1\/security\/saml\/metadata$/);
    expect(entityIdInput.value).toContain('/nexus/service/rest/v1/security/saml/metadata');

    mockUrlOf.mockRestore();
  });

  it('generates absolute Entity ID URL for local/default path', async () => {
    // Mock ExtJS.urlOf to return relative URL without context path
    const mockUrlOf = jest.spyOn(ExtJS, 'urlOf').mockReturnValue('/service/rest/v1/security/saml/metadata');

    mockFetchConfiguration.mockResolvedValue(null);

    await act(async () => {
      render(<SamlPage />, { wrapper: TestWrapper });
    });

    await waitFor(() => {
      expect(screen.getByTestId('input-entityId')).toBeInTheDocument();
    });

    const entityIdInput = screen.getByTestId('input-entityId') as HTMLInputElement;

    // The relative URL should be resolved to an absolute URI
    // Jest's window.location.href is http://localhost/ so result should be http://localhost/service/rest/v1/security/saml/metadata
    expect(entityIdInput.value).toMatch(/^https?:\/\/localhost\/service\/rest\/v1\/security\/saml\/metadata$/);
    expect(entityIdInput.value).toContain('/service/rest/v1/security/saml/metadata');
    expect(entityIdInput.value).not.toContain('/nexus');

    mockUrlOf.mockRestore();
  });
});

describe('parseSignatureValidation', () => {
  it('converts "default" to null', () => {
    expect(parseSignatureValidation('default')).toBeNull();
  });

  it('converts "true" to true', () => {
    expect(parseSignatureValidation('true')).toBe(true);
  });

  it('converts "false" to false', () => {
    expect(parseSignatureValidation('false')).toBe(false);
  });

  it('converts null to null', () => {
    expect(parseSignatureValidation(null)).toBeNull();
  });

  it('converts undefined to null', () => {
    expect(parseSignatureValidation(undefined)).toBeNull();
  });

  it('converts boolean true to true', () => {
    expect(parseSignatureValidation(true)).toBe(true);
  });

  it('converts boolean false to false', () => {
    expect(parseSignatureValidation(false)).toBe(false);
  });
});
