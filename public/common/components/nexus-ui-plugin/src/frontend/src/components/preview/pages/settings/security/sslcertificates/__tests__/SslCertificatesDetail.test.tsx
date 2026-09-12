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
import { render, screen, fireEvent } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

import { SslCertificatesDetail } from '../SslCertificatesDetail';
import { SslCertificate } from '../types';

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

const mockCertificate: SslCertificate = {
  id: 'cert1',
  subjectCommonName: 'example.com',
  subjectOrganization: 'Example Inc',
  subjectOrganizationalUnit: 'IT Department',
  issuerCommonName: 'CA Root',
  issuerOrganization: 'CA Inc',
  issuerOrganizationalUnit: 'Certificate Authority',
  fingerprint: 'AA:BB:CC:DD:EE:FF:11:22:33:44:55:66:77:88:99:00',
  issuedOn: Date.now() - 86400000 * 365,
  expiresOn: Date.now() + 86400000 * 365,
};

describe('SslCertificatesDetail', () => {
  const mockOnDelete = jest.fn();
  const mockOnCancel = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('shows loading state when loading is true', () => {
    render(
      <SslCertificatesDetail
        certificate={null}
        loading={true}
        canDelete={true}
        onDelete={mockOnDelete}
        onCancel={mockOnCancel}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText(/Loading certificate details/i)).toBeInTheDocument();
  });

  it('shows not found state when certificate is null and not loading', () => {
    render(
      <SslCertificatesDetail
        certificate={null}
        loading={false}
        canDelete={true}
        onDelete={mockOnDelete}
        onCancel={mockOnCancel}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText(/Certificate not found/i)).toBeInTheDocument();
  });

  it('renders certificate information', () => {
    render(
      <SslCertificatesDetail
        certificate={mockCertificate}
        loading={false}
        canDelete={true}
        onDelete={mockOnDelete}
        onCancel={mockOnCancel}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText('example.com')).toBeInTheDocument();
    expect(screen.getByText('Example Inc')).toBeInTheDocument();
    expect(screen.getByText('CA Inc')).toBeInTheDocument();
  });

  it('displays certificate fingerprint', () => {
    render(
      <SslCertificatesDetail
        certificate={mockCertificate}
        loading={false}
        canDelete={true}
        onDelete={mockOnDelete}
        onCancel={mockOnCancel}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText(mockCertificate.fingerprint)).toBeInTheDocument();
  });

  it('shows delete button when canDelete is true', () => {
    render(
      <SslCertificatesDetail
        certificate={mockCertificate}
        loading={false}
        canDelete={true}
        onDelete={mockOnDelete}
        onCancel={mockOnCancel}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText('Delete Certificate')).toBeInTheDocument();
  });

  it('does not show delete button when canDelete is false', () => {
    render(
      <SslCertificatesDetail
        certificate={mockCertificate}
        loading={false}
        canDelete={false}
        onDelete={mockOnDelete}
        onCancel={mockOnCancel}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.queryByText('Delete Certificate')).not.toBeInTheDocument();
  });

  it('calls onCancel when back button is clicked', () => {
    render(
      <SslCertificatesDetail
        certificate={mockCertificate}
        loading={false}
        canDelete={true}
        onDelete={mockOnDelete}
        onCancel={mockOnCancel}
      />,
      { wrapper: TestWrapper }
    );

    fireEvent.click(screen.getByText('Back to List'));

    expect(mockOnCancel).toHaveBeenCalled();
  });

  it('shows expiration warning for expired certificate', () => {
    const expiredCert: SslCertificate = {
      ...mockCertificate,
      expiresOn: Date.now() - 86400000, // Expired yesterday
    };

    render(
      <SslCertificatesDetail
        certificate={expiredCert}
        loading={false}
        canDelete={true}
        onDelete={mockOnDelete}
        onCancel={mockOnCancel}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText(/This certificate has expired/i)).toBeInTheDocument();
  });

  it('shows expiring soon warning for certificate expiring within 30 days', () => {
    const expiringCert: SslCertificate = {
      ...mockCertificate,
      expiresOn: Date.now() + 86400000 * 15, // Expires in 15 days
    };

    render(
      <SslCertificatesDetail
        certificate={expiringCert}
        loading={false}
        canDelete={true}
        onDelete={mockOnDelete}
        onCancel={mockOnCancel}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText(/This certificate is expiring soon/i)).toBeInTheDocument();
  });

  it('displays warning message', () => {
    render(
      <SslCertificatesDetail
        certificate={mockCertificate}
        loading={false}
        canDelete={true}
        onDelete={mockOnDelete}
        onCancel={mockOnCancel}
        showTrustWarning
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText(/Warning: Adding a certificate/i)).toBeInTheDocument();
  });

  it('renders actions in sticky action bar via SettingsForm', () => {
    render(
      <SslCertificatesDetail
        certificate={mockCertificate}
        loading={false}
        canDelete={true}
        onDelete={mockOnDelete}
        onCancel={mockOnCancel}
      />,
      { wrapper: TestWrapper }
    );

    const actionBar = document.querySelector('.settings-form__action-bar');
    expect(actionBar).toBeInTheDocument();
    expect(screen.getByText('Back to List')).toBeInTheDocument();
    expect(screen.getByText('Delete Certificate')).toBeInTheDocument();
  });

  it('calls onDelete when Delete Certificate is clicked in action bar', () => {
    render(
      <SslCertificatesDetail
        certificate={mockCertificate}
        loading={false}
        canDelete={true}
        onDelete={mockOnDelete}
        onCancel={mockOnCancel}
      />,
      { wrapper: TestWrapper }
    );

    fireEvent.click(screen.getByText('Delete Certificate'));
    expect(mockOnDelete).toHaveBeenCalled();
  });

  // NEXUS-54265: the warning is advice about whether to add a certificate, so it is only correct
  // in the add-form preview. On an already-trusted certificate it describes a past decision.
  describe('trust warning', () => {
    const warning = /Adding a certificate to the trust store means you trust/i;

    it('is hidden when viewing a certificate that is already in the trust store', () => {
      render(
        <SslCertificatesDetail
          certificate={mockCertificate}
          loading={false}
          canDelete={true}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.queryByText(warning)).not.toBeInTheDocument();
    });

    it('is shown while previewing a certificate that has not been added yet', () => {
      render(
        <SslCertificatesDetail
          certificate={mockCertificate}
          loading={false}
          canDelete={false}
          onDelete={mockOnDelete}
          onCancel={mockOnCancel}
          showTrustWarning
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByText(warning)).toBeInTheDocument();
    });
  });
});

