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

import { SslCertificatesList } from '../SslCertificatesList';
import * as useSslCertificatesApiModule from '../useSslCertificatesApi';

// Mock the API hook
jest.mock('../useSslCertificatesApi', () => ({
  useSslCertificatesApi: jest.fn(),
}));

const mockedUseSslCertificatesApi = useSslCertificatesApiModule.useSslCertificatesApi as jest.MockedFunction<typeof useSslCertificatesApiModule.useSslCertificatesApi>;

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

const mockCertificates = [
  {
    id: 'cert1',
    subjectCommonName: 'example.com',
    subjectOrganization: 'Example Inc',
    issuerOrganization: 'CA Inc',
    fingerprint: 'AA:BB:CC:DD:EE:FF',
    issuedOn: Date.now() - 86400000 * 365,
    expiresOn: Date.now() + 86400000 * 365,
  },
  {
    id: 'cert2',
    subjectCommonName: 'test.com',
    subjectOrganization: 'Test Corp',
    issuerOrganization: 'CA Inc',
    fingerprint: '11:22:33:44:55:66',
    issuedOn: Date.now() - 86400000 * 365,
    expiresOn: Date.now() + 86400000 * 30, // Expiring soon
  },
];

describe('SslCertificatesList', () => {
  const mockOnSelect = jest.fn();
  const mockOnCreate = jest.fn();
  const mockFetchCertificates = jest.fn();
  const mockSetError = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockFetchCertificates.mockResolvedValue(mockCertificates);
    mockedUseSslCertificatesApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchCertificates: mockFetchCertificates,
      fetchCertificateFromHost: jest.fn(),
      getCertificateDetails: jest.fn(),
      addCertificate: jest.fn(),
      deleteCertificate: jest.fn(),
      loadCertificateDetails: jest.fn(),
    });
  });

  it('loads certificates on mount', async () => {
    render(
      <SslCertificatesList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
      { wrapper: TestWrapper }
    );
    
    await waitFor(() => {
      expect(mockFetchCertificates).toHaveBeenCalled();
    });
  });

  it('renders certificates after loading', async () => {
    render(
      <SslCertificatesList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
      { wrapper: TestWrapper }
    );
    
    await waitFor(() => {
      expect(screen.getByText('example.com')).toBeInTheDocument();
    });
    
    expect(screen.getByText('test.com')).toBeInTheDocument();
  });

  it('calls onSelect when a certificate row is clicked', async () => {
    render(
      <SslCertificatesList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
      { wrapper: TestWrapper }
    );
    
    await waitFor(() => {
      expect(screen.getByText('example.com')).toBeInTheDocument();
    });
    
    const row = screen.getByText('example.com').closest('tr');
    if (row) {
      fireEvent.click(row);
    }
    
    expect(mockOnSelect).toHaveBeenCalledWith('cert1');
  });

  it('handles error state', async () => {
    mockedUseSslCertificatesApi.mockReturnValue({
      loading: false,
      error: 'Network error',
      setError: mockSetError,
      fetchCertificates: mockFetchCertificates.mockRejectedValue(new Error('Network error')),
      fetchCertificateFromHost: jest.fn(),
      getCertificateDetails: jest.fn(),
      addCertificate: jest.fn(),
      deleteCertificate: jest.fn(),
      loadCertificateDetails: jest.fn(),
    });
    
    render(
      <SslCertificatesList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
      { wrapper: TestWrapper }
    );
    
    await waitFor(() => {
      expect(screen.getByText(/error/i)).toBeInTheDocument();
    });
  });

  it('renders filter input', async () => {
    render(
      <SslCertificatesList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
      { wrapper: TestWrapper }
    );
    
    await waitFor(() => {
      expect(screen.getByText('example.com')).toBeInTheDocument();
    });
    
    expect(screen.getByPlaceholderText(/filter/i)).toBeInTheDocument();
  });

  it('filters certificates by search term', async () => {
    render(
      <SslCertificatesList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
      { wrapper: TestWrapper }
    );
    
    await waitFor(() => {
      expect(screen.getByText('example.com')).toBeInTheDocument();
    });
    
    const filterInput = screen.getByPlaceholderText(/filter/i);
    fireEvent.change(filterInput, { target: { value: 'example' } });
    
    await waitFor(() => {
      expect(screen.getByText('example.com')).toBeInTheDocument();
      expect(screen.queryByText('test.com')).not.toBeInTheDocument();
    });
  });

  it('renders table headers', async () => {
    render(
      <SslCertificatesList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
      { wrapper: TestWrapper }
    );
    
    await waitFor(() => {
      expect(screen.getByText('example.com')).toBeInTheDocument();
    });
    
    expect(screen.getByText('Name')).toBeInTheDocument();
    expect(screen.getByText('Issued To')).toBeInTheDocument();
    expect(screen.getByText('Issued By')).toBeInTheDocument();
    expect(screen.getByText('Fingerprint')).toBeInTheDocument();
  });

  it('renders help section', async () => {
    render(
      <SslCertificatesList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
      { wrapper: TestWrapper }
    );
    
    await waitFor(() => {
      expect(screen.getByText('example.com')).toBeInTheDocument();
    });
    
    expect(screen.getByText(/What is SSL/i)).toBeInTheDocument();
  });

  it('sorts certificates when column header is clicked', async () => {
    render(
      <SslCertificatesList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
      { wrapper: TestWrapper }
    );
    
    await waitFor(() => {
      expect(screen.getByText('example.com')).toBeInTheDocument();
    });
    
    const nameHeader = screen.getByText('Name');
    fireEvent.click(nameHeader);

    // Should still show certificates (sorting is internal)
    expect(screen.getByText('example.com')).toBeInTheDocument();
  });

  // Accessibility (NEXUS-54265): the sort indicator icon is not perceivable to screen readers,
  // so sort state has to be exposed through aria-sort on the header cell.
  describe('accessibility', () => {
    const headerCell = (label: string) => screen.getByText(label).closest('th');
    const sortButton = (label: string) => screen.getByText(label).closest('button');

    async function renderList() {
      render(
        <SslCertificatesList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
        { wrapper: TestWrapper }
      );
      await waitFor(() => {
        expect(screen.getByText('example.com')).toBeInTheDocument();
      });
    }

    it('exposes aria-sort on the active column and none on the others', async () => {
      await renderList();

      // Default sort is subjectCommonName ascending.
      expect(headerCell('Name')).toHaveAttribute('aria-sort', 'ascending');
      expect(headerCell('Issued To')).toHaveAttribute('aria-sort', 'none');
      expect(headerCell('Issued By')).toHaveAttribute('aria-sort', 'none');
      expect(headerCell('Fingerprint')).toHaveAttribute('aria-sort', 'none');
    });

    // Classic UI toggles between ascending and descending only — there is no third
    // "unsorted" state to land in by accident.
    it('toggles aria-sort between ascending and descending', async () => {
      await renderList();

      fireEvent.click(sortButton('Name')!);
      expect(headerCell('Name')).toHaveAttribute('aria-sort', 'descending');

      fireEvent.click(sortButton('Name')!);
      expect(headerCell('Name')).toHaveAttribute('aria-sort', 'ascending');
    });

    it('moves aria-sort to a newly selected column', async () => {
      await renderList();

      fireEvent.click(sortButton('Issued By')!);

      expect(headerCell('Issued By')).toHaveAttribute('aria-sort', 'ascending');
      expect(headerCell('Name')).toHaveAttribute('aria-sort', 'none');
    });

    it('sorts through a real button so headers are keyboard operable', async () => {
      await renderList();

      // A bare <th onClick> cannot be reached with a keyboard; each sortable header must
      // wrap its label in a button.
      ['Name', 'Issued To', 'Issued By', 'Fingerprint'].forEach((label) => {
        expect(sortButton(label)).toBeInTheDocument();
      });
    });

    it('gives every row a labelled action button that selects the certificate', async () => {
      await renderList();

      const action = screen.getByRole('button', { name: 'View certificate example.com' });
      expect(screen.getByRole('button', { name: 'View certificate test.com' })).toBeInTheDocument();

      fireEvent.click(action);

      // Exactly once: the button stops propagation so the row handler does not also fire.
      expect(mockOnSelect).toHaveBeenCalledTimes(1);
      expect(mockOnSelect).toHaveBeenCalledWith('cert1');
    });

    it('announces the error state to assistive technology', async () => {
      mockedUseSslCertificatesApi.mockReturnValue({
        loading: false,
        error: 'Network error',
        setError: mockSetError,
        fetchCertificates: mockFetchCertificates,
        fetchCertificateFromHost: jest.fn(),
        getCertificateDetails: jest.fn(),
        addCertificate: jest.fn(),
        deleteCertificate: jest.fn(),
        loadCertificateDetails: jest.fn(),
      });

      render(
        <SslCertificatesList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByRole('alert')).toHaveTextContent('Network error');
      });
    });
  });

  it('uses the Classic UI empty-list wording', async () => {
    mockFetchCertificates.mockResolvedValue([]);

    render(
      <SslCertificatesList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText('There are no SSL Certificates available')).toBeInTheDocument();
    });
  });
});


