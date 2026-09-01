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
import { Theme } from '@radix-ui/themes';

import { SamlPage } from '../SamlPage';
import * as samlApi from '../samlApi';
import { parseSignatureValidation } from '../samlFormMachine';
import { ToastProvider } from '../../../../../shared/Toast';

// Import the ExtJS mock class directly to spy on it (Jest uses __mocks__/ExtJS.js)
import ExtJS from '../../../../../../../interface/ExtJS';

// Mock the pure SAML API module the form machine invokes.
jest.mock('../samlApi');

const mockedFetch = samlApi.fetchSamlConfiguration as jest.MockedFunction<typeof samlApi.fetchSamlConfiguration>;
const mockedSave = samlApi.saveSamlConfiguration as jest.MockedFunction<typeof samlApi.saveSamlConfiguration>;
const mockedDelete = samlApi.deleteSamlConfiguration as jest.MockedFunction<typeof samlApi.deleteSamlConfiguration>;
const mockedGetUrl = samlApi.getSamlMetadataUrl as jest.MockedFunction<typeof samlApi.getSamlMetadataUrl>;

const checkPermissionSpy = jest.spyOn(ExtJS, 'checkPermission');

Object.assign(navigator, {
  clipboard: {
    writeText: jest.fn(),
  },
});

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

  beforeEach(() => {
    jest.clearAllMocks();
    checkPermissionSpy.mockReturnValue(true);
    (navigator.clipboard.writeText as jest.Mock).mockClear();
    mockedGetUrl.mockReturnValue('/service/rest/v1/security/saml/metadata');
    mockedFetch.mockResolvedValue(null);
    mockedSave.mockResolvedValue(undefined);
    mockedDelete.mockResolvedValue(undefined);
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
    mockedFetch.mockResolvedValue(mockConfiguration);
    render(<SamlPage />, { wrapper: TestWrapper });
    await waitFor(() => {
      expect(screen.getByText('Configured')).toBeInTheDocument();
    });
  });

  it('displays form fields', async () => {
    render(<SamlPage />, { wrapper: TestWrapper });
    await waitFor(() => {
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
      expect(screen.getByTestId('select-validateResponseSignature')).toBeInTheDocument();
      expect(screen.getByTestId('select-validateAssertionSignature')).toBeInTheDocument();
    });
  });

  it('populates form with existing configuration', async () => {
    mockedFetch.mockResolvedValue(mockConfiguration);
    render(<SamlPage />, { wrapper: TestWrapper });
    await waitFor(() => {
      const entityIdInput = screen.getByTestId('input-entityId') as HTMLInputElement;
      const usernameInput = screen.getByTestId('input-usernameAttribute') as HTMLInputElement;
      expect(entityIdInput.value).toBe('https://nexus.example.com');
      expect(usernameInput.value).toBe('email');
    });
  });

  it('marks required fields', async () => {
    render(<SamlPage />, { wrapper: TestWrapper });
    await waitFor(() => {
      expect(screen.getByTestId('input-entityId')).toBeInTheDocument();
    });
    expect(screen.getByTestId('textarea-idpMetadata')).toHaveAttribute('required');
    expect(screen.getByTestId('input-usernameAttribute')).toHaveAttribute('required');
  });

  it('calls saveConfiguration when form is valid', async () => {
    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('textarea-idpMetadata')).toBeInTheDocument();
    });

    await act(async () => {
      fireEvent.change(screen.getByTestId('textarea-idpMetadata'), {
        target: { value: '<EntityDescriptor>test</EntityDescriptor>' },
      });
    });
    await act(async () => {
      fireEvent.change(screen.getByTestId('input-usernameAttribute'), {
        target: { value: '  email  ' },
      });
    });

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /save/i }));
    });

    await waitFor(() => {
      expect(mockedSave).toHaveBeenCalledTimes(1);
      expect(mockedSave).toHaveBeenCalledWith(
        expect.objectContaining({
          idpMetadata: '<EntityDescriptor>test</EntityDescriptor>',
          usernameAttribute: 'email',
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

    await act(async () => {
      fireEvent.change(screen.getByTestId('textarea-idpMetadata'), {
        target: { value: '<EntityDescriptor>test</EntityDescriptor>' },
      });
    });
    await act(async () => {
      fireEvent.change(screen.getByTestId('input-usernameAttribute'), { target: { value: 'email' } });
    });
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /save/i }));
    });

    await waitFor(() => {
      expect(screen.getByText('SAML configuration saved successfully')).toBeInTheDocument();
    });
  });

  it('shows metadata URL when configured', async () => {
    mockedFetch.mockResolvedValue(mockConfiguration);
    render(<SamlPage />, { wrapper: TestWrapper });
    await waitFor(() => {
      expect(screen.getByText(/\/service\/rest\/v1\/security\/saml\/metadata/)).toBeInTheDocument();
    });
  });

  it('copies metadata URL to clipboard', async () => {
    mockedFetch.mockResolvedValue(mockConfiguration);
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
    const mockUrlOf = jest.spyOn(ExtJS, 'urlOf').mockReturnValue('/nexus/service/rest/v1/security/saml/metadata');
    mockedFetch.mockResolvedValue(mockConfiguration);
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
    const mockUrlOf = jest.spyOn(ExtJS, 'urlOf').mockReturnValue('/nexus/service/rest/v1/security/saml/metadata');
    const mockWindowOpen = jest.spyOn(window, 'open').mockImplementation();
    mockedFetch.mockResolvedValue(mockConfiguration);
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
    mockedFetch.mockResolvedValue(mockConfiguration);
    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Configured')).toBeInTheDocument();
    });

    await act(async () => {
      fireEvent.click(screen.getByTestId('saml-delete-button'));
    });

    await waitFor(() => {
      expect(screen.getByText('Delete SAML Configuration')).toBeInTheDocument();
      expect(screen.getByText(/Are you sure you want to delete/)).toBeInTheDocument();
    });
  });

  it('deletes configuration when confirmed', async () => {
    mockedFetch.mockResolvedValue(mockConfiguration);
    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Configured')).toBeInTheDocument();
    });

    await act(async () => {
      fireEvent.click(screen.getByTestId('saml-delete-button'));
    });

    await waitFor(() => {
      expect(screen.getByText('Delete SAML Configuration')).toBeInTheDocument();
    });

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /delete configuration/i }));
    });

    await waitFor(() => {
      expect(mockedDelete).toHaveBeenCalledTimes(1);
      expect(screen.getByText('Not Configured')).toBeInTheDocument();
    });
  });

  it('keeps the delete dialog open and shows the error when delete fails', async () => {
    mockedFetch.mockResolvedValue(mockConfiguration);
    mockedDelete.mockRejectedValue(new Error('Failed to delete SAML configuration'));
    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Configured')).toBeInTheDocument();
    });

    await act(async () => {
      fireEvent.click(screen.getByTestId('saml-delete-button'));
    });
    await waitFor(() => {
      expect(screen.getByText('Delete SAML Configuration')).toBeInTheDocument();
    });

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /delete configuration/i }));
    });

    await waitFor(() => {
      // Dialog still open and still configured (retriable).
      expect(screen.getByText('Delete SAML Configuration')).toBeInTheDocument();
      expect(screen.getByText('Configured')).toBeInTheDocument();
    });

    // The error is surfaced INSIDE the confirmation dialog (not only in a
    // page-level banner hidden behind the modal).
    const dialog = screen.getByRole('alertdialog');
    expect(within(dialog).getByText('Failed to delete SAML configuration')).toBeInTheDocument();
  });

  it('cancels delete when Cancel is clicked in delete dialog', async () => {
    mockedFetch.mockResolvedValue(mockConfiguration);
    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Configured')).toBeInTheDocument();
    });

    await act(async () => {
      fireEvent.click(screen.getByTestId('saml-delete-button'));
    });

    await waitFor(() => {
      expect(screen.getByText('Delete SAML Configuration')).toBeInTheDocument();
    });

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /cancel/i }));
    });

    await waitFor(() => {
      expect(screen.queryByText('Delete SAML Configuration')).not.toBeInTheDocument();
      expect(mockedDelete).not.toHaveBeenCalled();
      expect(screen.getByText('Configured')).toBeInTheDocument();
    });
  });

  it('displays error message when a load error occurs', async () => {
    mockedFetch.mockRejectedValue(new Error('Failed to save SAML configuration'));
    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Failed to save SAML configuration')).toBeInTheDocument();
    });
  });

  it('resets form when Cancel is clicked', async () => {
    mockedFetch.mockResolvedValue(mockConfiguration);
    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect((screen.getByTestId('input-usernameAttribute') as HTMLInputElement).value).toBe('email');
    });

    await act(async () => {
      fireEvent.change(screen.getByTestId('input-usernameAttribute'), { target: { value: 'modified' } });
    });
    expect((screen.getByTestId('input-usernameAttribute') as HTMLInputElement).value).toBe('modified');

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /discard/i }));
    });

    const leaveButton = await screen.findByRole('button', { name: /leave/i });
    await act(async () => {
      fireEvent.click(leaveButton);
    });

    await waitFor(() => {
      expect(screen.getByTestId('input-usernameAttribute')).toHaveValue('email');
    });
  });

  it('clears validation errors when Discard is clicked', async () => {
    mockedFetch.mockResolvedValue(mockConfiguration);
    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-usernameAttribute')).toBeInTheDocument();
    });

    await act(async () => {
      fireEvent.change(screen.getByTestId('input-usernameAttribute'), { target: { value: '' } });
    });

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /save/i }));
    });

    await waitFor(() => {
      expect(screen.getByText('Username Attribute is required')).toBeInTheDocument();
    });

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /discard/i }));
    });

    const leaveButton = await screen.findByRole('button', { name: /leave/i });
    await act(async () => {
      fireEvent.click(leaveButton);
    });

    await waitFor(() => {
      expect(screen.queryByText('Username Attribute is required')).not.toBeInTheDocument();
      expect(screen.getByTestId('input-usernameAttribute')).toHaveValue('email');
    });
  });

  it('validates Entity ID URI format', async () => {
    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-entityId')).toBeInTheDocument();
    });

    await act(async () => {
      fireEvent.change(screen.getByTestId('textarea-idpMetadata'), {
        target: { value: '<EntityDescriptor>test</EntityDescriptor>' },
      });
    });
    await act(async () => {
      fireEvent.change(screen.getByTestId('input-usernameAttribute'), { target: { value: 'email' } });
    });
    await act(async () => {
      fireEvent.change(screen.getByTestId('input-entityId'), { target: { value: 'not-a-uri' } });
    });
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /save/i }));
    });

    await waitFor(() => {
      expect(screen.getByText('Entity ID must be a URI')).toBeInTheDocument();
    });

    await act(async () => {
      fireEvent.change(screen.getByTestId('input-entityId'), { target: { value: 'http://example.com' } });
    });
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /save/i }));
    });

    await waitFor(() => {
      expect(screen.queryByText('Entity ID must be a URI')).not.toBeInTheDocument();
      expect(mockedSave).toHaveBeenCalled();
    });
  });

  it('displays signature validation tri-state correctly', async () => {
    mockedFetch.mockResolvedValue({
      ...mockConfiguration,
      validateResponseSignature: true,
      validateAssertionSignature: false,
    });
    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('select-validateResponseSignature')).toBeInTheDocument();
    });

    expect(screen.getByTestId('select-validateResponseSignature')).toHaveTextContent('True');
    expect(screen.getByTestId('select-validateAssertionSignature')).toHaveTextContent('False');
  });

  it('displays signature validation default (null) correctly', async () => {
    mockedFetch.mockResolvedValue({
      ...mockConfiguration,
      validateResponseSignature: null,
      validateAssertionSignature: null,
    });
    render(<SamlPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('select-validateResponseSignature')).toBeInTheDocument();
    });

    expect(screen.getByTestId('select-validateResponseSignature')).toHaveTextContent('Default');
    expect(screen.getByTestId('select-validateAssertionSignature')).toHaveTextContent('Default');
  });

  it('disables all inputs when user lacks update permission', async () => {
    checkPermissionSpy.mockReturnValue(false);

    await act(async () => {
      render(<SamlPage />, { wrapper: TestWrapper });
    });

    expect(checkPermissionSpy).toHaveBeenCalledWith('nexus:saml:update');

    await waitFor(() => {
      expect(screen.getByTestId('textarea-idpMetadata')).toBeInTheDocument();
    });

    expect(screen.getByTestId('textarea-idpMetadata')).toBeDisabled();
    expect(screen.getByTestId('input-entityId')).toBeDisabled();
    expect(screen.getByTestId('input-usernameAttribute')).toBeDisabled();
    expect(screen.getByTestId('select-validateResponseSignature')).toBeDisabled();

    expect(screen.queryByRole('button', { name: /save/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /discard/i })).not.toBeInTheDocument();
    expect(screen.getByText(/You do not have permission to modify SAML settings/)).toBeInTheDocument();
  });

  it('pre-populates Entity ID when no configuration exists', async () => {
    mockedFetch.mockResolvedValue(null);

    await act(async () => {
      render(<SamlPage />, { wrapper: TestWrapper });
    });

    await waitFor(() => {
      expect(screen.getByTestId('input-entityId')).toBeInTheDocument();
    });

    const entityIdInput = screen.getByTestId('input-entityId') as HTMLInputElement;
    expect(entityIdInput.value).toMatch(/^https?:\/\/.+\/service\/rest\/v1\/security\/saml\/metadata$/);
    expect(entityIdInput.value).toContain('/service/rest/v1/security/saml/metadata');
  });

  it('generates absolute Entity ID URL with context path', async () => {
    const mockUrlOf = jest.spyOn(ExtJS, 'urlOf').mockReturnValue('/nexus/service/rest/v1/security/saml/metadata');
    mockedFetch.mockResolvedValue(null);

    await act(async () => {
      render(<SamlPage />, { wrapper: TestWrapper });
    });

    await waitFor(() => {
      expect(screen.getByTestId('input-entityId')).toBeInTheDocument();
    });

    const entityIdInput = screen.getByTestId('input-entityId') as HTMLInputElement;
    expect(entityIdInput.value).toMatch(/^https?:\/\/localhost\/nexus\/service\/rest\/v1\/security\/saml\/metadata$/);
    expect(entityIdInput.value).toContain('/nexus/service/rest/v1/security/saml/metadata');

    mockUrlOf.mockRestore();
  });

  it('generates absolute Entity ID URL for local/default path', async () => {
    const mockUrlOf = jest.spyOn(ExtJS, 'urlOf').mockReturnValue('/service/rest/v1/security/saml/metadata');
    mockedFetch.mockResolvedValue(null);

    await act(async () => {
      render(<SamlPage />, { wrapper: TestWrapper });
    });

    await waitFor(() => {
      expect(screen.getByTestId('input-entityId')).toBeInTheDocument();
    });

    const entityIdInput = screen.getByTestId('input-entityId') as HTMLInputElement;
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
