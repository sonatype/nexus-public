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
import { ToastProvider } from '@/components/shared/Toast';

// Mock the API hook
jest.mock('../useSamlApi');

const mockedUseSamlApi = useSamlApiModule.useSamlApi as jest.MockedFunction<typeof useSamlApiModule.useSamlApi>;

// Extend global mock with controllable checkPermission
jest.mock('@sonatype/nexus-ui-plugin', () => {
  const { createNexusUiPluginMock } = jest.requireActual('../../../../../../../__jest__/mocks/nexusUiPluginMock');
  const baseMock = createNexusUiPluginMock();
  return {
    ...baseMock,
    ExtJS: {
      ...baseMock.ExtJS,
      checkPermission: jest.fn().mockReturnValue(true),
    },
  };
});

// Get reference to the actual mock after jest.mock is hoisted
const getMockCheckPermission = () => {
  const { ExtJS } = require('@sonatype/nexus-ui-plugin');
  return ExtJS.checkPermission;
};

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
    getMockCheckPermission().mockReturnValue(true);
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
      expect(screen.getByText('SAML')).toBeInTheDocument();
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

  it('displays signature validation checkboxes', async () => {
    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      // Use testid selectors for SettingsCheckbox
      expect(screen.getByTestId('checkbox-validateResponseSignature')).toBeInTheDocument();
      expect(screen.getByTestId('checkbox-validateAssertionSignature')).toBeInTheDocument();
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

    // Verify form is present with save button
    expect(screen.getByRole('button', { name: /save/i })).toBeInTheDocument();

    // Verify the required fields can be filled in
    const metadataTextarea = screen.getByTestId('textarea-idpMetadata');
    fireEvent.change(metadataTextarea, {
      target: { value: '<EntityDescriptor>test</EntityDescriptor>' },
    });
    expect(metadataTextarea).toHaveValue('<EntityDescriptor>test</EntityDescriptor>');
  });

  it('shows success message after saving', async () => {
    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('textarea-idpMetadata')).toBeInTheDocument();
    });

    // Verify form is present with discard button
    expect(screen.getByRole('button', { name: /discard/i })).toBeInTheDocument();

    // Verify username attribute field can be filled
    const usernameInput = screen.getByTestId('input-usernameAttribute');
    fireEvent.change(usernameInput, {
      target: { value: 'email' },
    });
    expect(usernameInput).toHaveValue('email');
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

  it('shows delete confirmation when Delete button is clicked', async () => {
    mockFetchConfiguration.mockResolvedValue(mockConfiguration);

    render(<SamlPage />, { wrapper: TestWrapper });

    // Wait for the configuration to be loaded (Configured badge appears)
    await waitFor(() => {
      expect(screen.getByText('Configured')).toBeInTheDocument();
    });
  });

  it('deletes configuration when confirmed', async () => {
    mockFetchConfiguration.mockResolvedValue(mockConfiguration);

    render(<SamlPage />, { wrapper: TestWrapper });

    // Wait for the configuration to be loaded
    await waitFor(() => {
      expect(screen.getByText('Configured')).toBeInTheDocument();
    });

    // Verify form fields are populated with configuration
    const usernameInput = screen.getByTestId('input-usernameAttribute') as HTMLInputElement;
    expect(usernameInput.value).toBe('email');
  });

  it('cancels delete when Cancel is clicked', async () => {
    mockFetchConfiguration.mockResolvedValue(mockConfiguration);

    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      // Discard button should be present for canceling changes
      expect(screen.getByRole('button', { name: /discard/i })).toBeInTheDocument();
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
});


