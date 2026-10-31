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

import { SslCertificatesAddForm } from '../SslCertificatesAddForm';
import * as useSslCertificatesApiModule from '../useSslCertificatesApi';
import * as useSslFormModule from '../useSslForm';

// Mock the API hook and form hook
jest.mock('../useSslCertificatesApi', () => ({
  useSslCertificatesApi: jest.fn(),
}));
jest.mock('../useSslForm');

// Mock child component. The real detail renders its own action bar; the stub reproduces just the
// cancel affordance from it so the callback this form supplies can be exercised.
jest.mock('../SslCertificatesDetail', () => ({
  SslCertificatesDetail: function MockSslCertificatesDetail({ certificate, onCancel }: any) {
    return (
      <div data-testid="certificate-detail">
        Certificate: {certificate?.subjectCommonName}
        <button type="button" onClick={onCancel}>Back to List</button>
      </div>
    );
  },
}));

const mockedUseSslCertificatesApi = useSslCertificatesApiModule.useSslCertificatesApi as jest.MockedFunction<typeof useSslCertificatesApiModule.useSslCertificatesApi>;
const mockedUseSslForm = useSslFormModule.useSslForm as jest.MockedFunction<typeof useSslFormModule.useSslForm>;

function createSslFormMock(formData: Record<string, any>, overrides: Record<string, any> = {}) {
  return {
    formData,
    errors: {} as Record<string, string>,
    touched: {} as Record<string, boolean>,
    isPristine: true,
    isSaving: false,
    isLoading: false,
    hasValidationErrors: false,
    saveError: null as string | null,
    certificateDetails: null,
    currentSource: formData.source || 'remoteHost',
    field: jest.fn((name: string) => ({
      name,
      value: formData[name] != null ? String(formData[name]) : '',
      error: undefined,
      onChange: jest.fn(),
      onBlur: jest.fn(),
    })),
    handleChange: jest.fn(),
    handleBlur: jest.fn(),
    handleSourceChange: jest.fn(),
    handleSubmit: jest.fn(),
    handleReset: jest.fn(),
    handleCancel: jest.fn(),
    state: { context: {}, matches: jest.fn(() => false) },
    send: jest.fn(),
    ...overrides,
  } as any;
}

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

describe('SslCertificatesAddForm', () => {
  const mockOnSave = jest.fn();
  const mockOnCancel = jest.fn();
  const mockLoadCertificateDetails = jest.fn();
  const mockSetError = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockLoadCertificateDetails.mockResolvedValue({
      id: 'cert1',
      subjectCommonName: 'example.com',
      subjectOrganization: 'Example Inc',
      fingerprint: 'AA:BB:CC:DD:EE:FF',
      pem: '-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----',
    });
    // Default form hook mock
    mockedUseSslForm.mockReturnValue(createSslFormMock({
      source: 'remoteHost',
      remoteHostUrl: '',
      pemContent: '',
    }));
    mockedUseSslCertificatesApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchCertificates: jest.fn(),
      fetchCertificateFromHost: jest.fn(),
      getCertificateDetails: jest.fn(),
      addCertificate: jest.fn(),
      deleteCertificate: jest.fn(),
      loadCertificateDetails: mockLoadCertificateDetails,
    });
  });

  it('renders the form with remote host option selected by default', () => {
    render(
      <SslCertificatesAddForm onSave={mockOnSave} onCancel={mockOnCancel} />,
      { wrapper: TestWrapper }
    );
    
    expect(screen.getByText('Add SSL Certificate')).toBeInTheDocument();
    expect(screen.getByLabelText(/Load from server/i)).toBeInTheDocument();
  });

  it('shows PEM textarea when PEM source is selected', () => {
    // Mock the form with PEM source selected
    mockedUseSslForm.mockReturnValue(createSslFormMock({
      source: 'PEM',
      remoteHostUrl: '',
      pemContent: '',
    }, {
      currentSource: 'PEM',
      formData: { source: 'PEM', remoteHostUrl: '', pemContent: '' },
    }));

    render(
      <SslCertificatesAddForm onSave={mockOnSave} onCancel={mockOnCancel} />,
      { wrapper: TestWrapper }
    );
    
    expect(screen.getByLabelText(/Paste Certificate as PEM/i)).toBeInTheDocument();
  });

  it('shows validation error for empty remote host URL', async () => {
    render(
      <SslCertificatesAddForm onSave={mockOnSave} onCancel={mockOnCancel} />,
      { wrapper: TestWrapper }
    );
    
    // Verify the remote host input has required attribute
    const urlInput = screen.getByPlaceholderText(/example.com/i);
    expect(urlInput).toHaveAttribute('required');
    
    // Verify load button is present
    const loadButton = screen.getByText('Load Certificate');
    expect(loadButton).toBeInTheDocument();
  });

  it('shows PEM textarea with required attr when PEM source is selected', async () => {
    mockedUseSslForm.mockReturnValue(createSslFormMock({
      source: 'PEM',
      remoteHostUrl: '',
      pemContent: '',
    }, {
      currentSource: 'PEM',
      formData: { source: 'PEM', remoteHostUrl: '', pemContent: '' },
    }));

    render(
      <SslCertificatesAddForm onSave={mockOnSave} onCancel={mockOnCancel} />,
      { wrapper: TestWrapper }
    );
    
    const pemTextarea = screen.getByPlaceholderText(/BEGIN CERTIFICATE/i);
    expect(pemTextarea).toHaveAttribute('required');
  });

  it('calls loadCertificateDetails when Load Certificate is clicked', async () => {
    mockedUseSslForm.mockReturnValue(createSslFormMock({
      source: 'remoteHost',
      remoteHostUrl: 'example.com',
      pemContent: '',
    }, {
      isPristine: false,
      formData: { source: 'remoteHost', remoteHostUrl: 'example.com', pemContent: '' },
      hasValidationErrors: false,
      handleBlur: jest.fn(),
    }));

    render(
      <SslCertificatesAddForm onSave={mockOnSave} onCancel={mockOnCancel} />,
      { wrapper: TestWrapper }
    );
    
    const loadButton = screen.getByText('Load Certificate');
    fireEvent.click(loadButton);
    
    await waitFor(() => {
      expect(mockLoadCertificateDetails).toHaveBeenCalled();
    });
  });

  it('shows certificate preview after loading details', async () => {
    mockedUseSslForm.mockReturnValue(createSslFormMock({
      source: 'remoteHost',
      remoteHostUrl: 'example.com',
      pemContent: '',
    }, {
      isPristine: false,
      formData: { source: 'remoteHost', remoteHostUrl: 'example.com', pemContent: '' },
      hasValidationErrors: false,
      handleBlur: jest.fn(),
    }));

    render(
      <SslCertificatesAddForm onSave={mockOnSave} onCancel={mockOnCancel} />,
      { wrapper: TestWrapper }
    );
    
    const loadButton = screen.getByText('Load Certificate');
    fireEvent.click(loadButton);
    
    await waitFor(() => {
      expect(screen.getByTestId('certificate-detail')).toBeInTheDocument();
    });
  });

  // NEXUS-54265: the preview's Back to List was wired to a no-op, so it rendered as an enabled
  // button that did nothing. It has to step back one stage — preview -> form, keeping the entered
  // input — leaving the outer Discard as the way out of the form entirely.
  it('returns from the preview to the form when Back to List is clicked', async () => {
    mockedUseSslForm.mockReturnValue(createSslFormMock({
      source: 'remoteHost',
      remoteHostUrl: 'example.com',
      pemContent: '',
    }, {
      isPristine: false,
      formData: { source: 'remoteHost', remoteHostUrl: 'example.com', pemContent: '' },
      hasValidationErrors: false,
    }));

    render(
      <SslCertificatesAddForm onSave={mockOnSave} onCancel={mockOnCancel} />,
      { wrapper: TestWrapper }
    );

    fireEvent.click(screen.getByText('Load Certificate'));

    await waitFor(() => {
      expect(screen.getByTestId('certificate-detail')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Back to List'));

    // Back on the source form, with the hostname still there to correct rather than retype.
    expect(screen.queryByTestId('certificate-detail')).not.toBeInTheDocument();
    expect(screen.getByLabelText(/Load from server/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/example.com/i)).toHaveValue('example.com');
    // Submit reverts to reloading the certificate rather than adding the discarded preview.
    expect(screen.getByText('Load Certificate')).toBeInTheDocument();
    // Leaving the page is the outer Discard's job, not this button's.
    expect(mockOnCancel).not.toHaveBeenCalled();
  });

  it('calls onCancel when cancel button is clicked', () => {
    render(
      <SslCertificatesAddForm onSave={mockOnSave} onCancel={mockOnCancel} />,
      { wrapper: TestWrapper }
    );
    
    fireEvent.click(screen.getByText('Discard'));
    
    expect(mockOnCancel).toHaveBeenCalled();
  });

  // NEXUS-54265: Classic UI raises SslCertificatesAlreadyExistsModal when the loaded certificate
  // is already trusted, offering the existing certificate instead of a dead-end error.
  describe('already-trusted certificate', () => {
    const trustedCertificate = {
      id: 'cert1',
      subjectCommonName: 'example.com',
      fingerprint: 'AA:BB:CC:DD:EE:FF',
      inTrustStore: true,
      pem: '-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----',
    };

    const mockOnViewExisting = jest.fn();

    function renderWithLoadedCertificate() {
      mockedUseSslForm.mockReturnValue(createSslFormMock({
        source: 'remoteHost',
        remoteHostUrl: 'example.com',
        pemContent: '',
      }, {
        isPristine: false,
        formData: { source: 'remoteHost', remoteHostUrl: 'example.com', pemContent: '' },
        hasValidationErrors: false,
      }));

      render(
        <SslCertificatesAddForm
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          onViewExisting={mockOnViewExisting}
        />,
        { wrapper: TestWrapper }
      );

      fireEvent.click(screen.getByText('Load Certificate'));
    }

    it('opens the duplicate dialog as soon as the certificate loads', async () => {
      mockLoadCertificateDetails.mockResolvedValue(trustedCertificate);

      renderWithLoadedCertificate();

      await waitFor(() => {
        expect(screen.getByText('Certificate Already Exists')).toBeInTheDocument();
      });
      expect(screen.getByText(/already exists and cannot be added again/i)).toBeInTheDocument();
    });

    it('navigates to the existing certificate from the dialog', async () => {
      mockLoadCertificateDetails.mockResolvedValue(trustedCertificate);

      renderWithLoadedCertificate();

      await waitFor(() => {
        expect(screen.getByText('View Certificate')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText('View Certificate'));

      expect(mockOnViewExisting).toHaveBeenCalledWith('cert1');
    });

    it('does not attempt an add that the backend would reject with a 409', async () => {
      mockLoadCertificateDetails.mockResolvedValue(trustedCertificate);

      renderWithLoadedCertificate();

      await waitFor(() => {
        expect(screen.getByText('Add Certificate')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText('Add Certificate'));

      await waitFor(() => {
        expect(mockSetError).toHaveBeenCalledWith(
          'This certificate already exists in the trust store and cannot be added again.'
        );
      });
      expect(mockOnSave).not.toHaveBeenCalled();
    });

    it('leaves the dialog closed for a certificate that is not trusted yet', async () => {
      mockLoadCertificateDetails.mockResolvedValue({ ...trustedCertificate, inTrustStore: false });

      renderWithLoadedCertificate();

      await waitFor(() => {
        expect(screen.getByTestId('certificate-detail')).toBeInTheDocument();
      });

      expect(screen.queryByText('Certificate Already Exists')).not.toBeInTheDocument();
    });
  });

  it('handles API errors', async () => {
    mockLoadCertificateDetails.mockRejectedValue(new Error('Failed to load certificate'));
    
    render(
      <SslCertificatesAddForm onSave={mockOnSave} onCancel={mockOnCancel} />,
      { wrapper: TestWrapper }
    );
    
    const urlInput = screen.getByPlaceholderText(/example.com/i);
    fireEvent.change(urlInput, { target: { value: 'example.com' } });
    
    const loadButton = screen.getByText('Load Certificate');
    fireEvent.click(loadButton);
    
    await waitFor(() => {
      expect(mockSetError).toHaveBeenCalled();
    });
  });
});

