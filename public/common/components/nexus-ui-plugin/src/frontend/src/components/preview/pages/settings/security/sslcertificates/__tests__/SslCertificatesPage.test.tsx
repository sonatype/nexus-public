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

import { ExtJS } from '../../../../../../../interface/ExtJS';
import { SslCertificatesPage } from '../SslCertificatesPage';
import * as useSslCertificatesApiModule from '../useSslCertificatesApi';
import { ToastProvider } from '../../../../../shared/Toast';

// The page imports ExtJS from the interface module directly, so that is what has to be mocked.
jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    checkPermission: jest.fn().mockReturnValue(true),
    waitForPermissions: jest.fn().mockResolvedValue(undefined),
  },
}));

// Mock the API hook
jest.mock('../useSslCertificatesApi', () => ({
  useSslCertificatesApi: jest.fn(),
}));

// Mock child components
jest.mock('../SslCertificatesList', () => ({
  SslCertificatesList: function MockSslCertificatesList({ onSelect, onCreate }: { onSelect: (id: string) => void; onCreate: () => void }) {
    return (
      <div data-testid="ssl-certificates-list">
        <button onClick={() => onSelect('cert1')}>Select Certificate</button>
        <button onClick={onCreate}>Create Certificate</button>
      </div>
    );
  },
}));

jest.mock('../SslCertificatesDetail', () => ({
  SslCertificatesDetail: function MockSslCertificatesDetail({ certificate, onDelete, onCancel }: any) {
    return (
      <div data-testid="ssl-certificates-detail">
        <span>Viewing: {certificate?.subjectCommonName}</span>
        <button onClick={onDelete}>Delete</button>
        <button onClick={onCancel}>Cancel</button>
      </div>
    );
  },
}));

jest.mock('../SslCertificatesAddForm', () => ({
  SslCertificatesAddForm: function MockSslCertificatesAddForm({ onSave, onCancel }: any) {
    return (
      <div data-testid="ssl-certificates-add-form">
        <button onClick={() => onSave({ source: 'PEM', pemContent: '-----BEGIN CERTIFICATE-----', remoteHostUrl: '' })}>Save</button>
        <button onClick={onCancel}>Cancel</button>
      </div>
    );
  },
}));

// Wrapper component for Radix Theme and Toast context
function TestWrapper({ children }: { children: React.ReactNode }) {
  return (
    <Theme>
      <ToastProvider>{children}</ToastProvider>
    </Theme>
  );
}

describe('SslCertificatesPage', () => {
  const mockFetchCertificates = jest.fn();
  const mockAddCertificate = jest.fn();
  const mockDeleteCertificate = jest.fn();
  const mockSetError = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    (ExtJS.checkPermission as jest.Mock).mockReturnValue(true);
    (ExtJS.waitForPermissions as jest.Mock).mockResolvedValue(undefined);
    mockFetchCertificates.mockResolvedValue([
      {
        id: 'cert1',
        subjectCommonName: 'example.com',
        subjectOrganization: 'Example Inc',
        issuerOrganization: 'CA Inc',
        fingerprint: 'AA:BB:CC:DD:EE:FF',
      },
    ]);
    useSslCertificatesApiModule.useSslCertificatesApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchCertificates: mockFetchCertificates,
      fetchCertificateFromHost: jest.fn(),
      getCertificateDetails: jest.fn(),
      addCertificate: mockAddCertificate.mockResolvedValue({}),
      deleteCertificate: mockDeleteCertificate.mockResolvedValue({}),
      loadCertificateDetails: jest.fn(),
    });
  });

  it('renders the certificates list by default', () => {
    render(<SslCertificatesPage />, { wrapper: TestWrapper });
    
    expect(screen.getByTestId('ssl-certificates-list')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'SSL Certificates' })).toBeInTheDocument();
  });

  it('shows create form when Add Certificate button is clicked', async () => {
    render(<SslCertificatesPage />, { wrapper: TestWrapper });
    
    fireEvent.click(screen.getByText('Add Certificate'));
    
    await waitFor(() => {
      expect(screen.getByTestId('ssl-certificates-add-form')).toBeInTheDocument();
    });
  });

  it('navigates to certificate detail when a certificate is selected', async () => {
    render(<SslCertificatesPage />, { wrapper: TestWrapper });
    
    fireEvent.click(screen.getByText('Select Certificate'));
    
    await waitFor(() => {
      expect(screen.getByTestId('ssl-certificates-detail')).toBeInTheDocument();
    });
  });

  it('returns to list view when cancel is clicked in create mode', async () => {
    render(<SslCertificatesPage />, { wrapper: TestWrapper });
    
    // Go to create mode
    fireEvent.click(screen.getByText('Add Certificate'));
    
    await waitFor(() => {
      expect(screen.getByTestId('ssl-certificates-add-form')).toBeInTheDocument();
    });
    
    // Click cancel
    fireEvent.click(screen.getByText('Cancel'));
    
    await waitFor(() => {
      expect(screen.getByTestId('ssl-certificates-list')).toBeInTheDocument();
    });
  });

  it('displays page header with icon and description', () => {
    render(<SslCertificatesPage />, { wrapper: TestWrapper });
    
    expect(screen.getByRole('heading', { name: 'SSL Certificates' })).toBeInTheDocument();
    expect(screen.getByText(/Manage trusted SSL certificates/i)).toBeInTheDocument();
  });

  it('shows Add Certificate button when user has create permission', () => {
    render(<SslCertificatesPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Add Certificate')).toBeInTheDocument();
  });

  // Permissions load asynchronously (NEXUS-53199): checkPermission() answers false until they
  // arrive, and nothing else re-renders this page, so the button has to be re-evaluated once
  // waitForPermissions() resolves (NEXUS-54265).
  it('reveals Add Certificate once permissions finish loading', async () => {
    let releasePermissions!: () => void;
    (ExtJS.waitForPermissions as jest.Mock).mockReturnValue(
      new Promise<void>((resolve) => {
        releasePermissions = resolve;
      })
    );
    (ExtJS.checkPermission as jest.Mock).mockReturnValue(false);

    render(<SslCertificatesPage />, { wrapper: TestWrapper });

    expect(screen.queryByText('Add Certificate')).not.toBeInTheDocument();

    (ExtJS.checkPermission as jest.Mock).mockReturnValue(true);
    await act(async () => {
      releasePermissions();
    });

    expect(screen.getByText('Add Certificate')).toBeInTheDocument();
  });

  it('keeps the initial permission answer when permissions never load', async () => {
    (ExtJS.checkPermission as jest.Mock).mockReturnValue(true);
    (ExtJS.waitForPermissions as jest.Mock).mockRejectedValue(new Error('Permissions load timed out'));

    render(<SslCertificatesPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Add Certificate')).toBeInTheDocument();
    });
  });

  it('handles loading state', () => {
    useSslCertificatesApiModule.useSslCertificatesApi.mockReturnValue({
      loading: true,
      error: null,
      setError: mockSetError,
      fetchCertificates: mockFetchCertificates,
      fetchCertificateFromHost: jest.fn(),
      getCertificateDetails: jest.fn(),
      addCertificate: mockAddCertificate,
      deleteCertificate: mockDeleteCertificate,
      loadCertificateDetails: jest.fn(),
    });

    render(<SslCertificatesPage />, { wrapper: TestWrapper });
    
    // Page should still render
    expect(screen.getByRole('heading', { name: 'SSL Certificates' })).toBeInTheDocument();
  });

  it('handles error state', () => {
    useSslCertificatesApiModule.useSslCertificatesApi.mockReturnValue({
      loading: false,
      error: 'Failed to load certificates',
      setError: mockSetError,
      fetchCertificates: mockFetchCertificates,
      fetchCertificateFromHost: jest.fn(),
      getCertificateDetails: jest.fn(),
      addCertificate: mockAddCertificate,
      deleteCertificate: mockDeleteCertificate,
      loadCertificateDetails: jest.fn(),
    });

    render(<SslCertificatesPage />, { wrapper: TestWrapper });
    
    expect(screen.getByText('Failed to load certificates')).toBeInTheDocument();
  });

  it('uses shared ConfirmDialog for delete (not raw Dialog)', () => {
    render(<SslCertificatesPage />, { wrapper: TestWrapper });
    // Verify no raw Dialog.Portal/Overlay classes exist
    expect(document.querySelector('.ssl-certificates-page__modal-overlay')).not.toBeInTheDocument();
    expect(document.querySelector('.ssl-certificates-page__modal')).not.toBeInTheDocument();
  });
});

