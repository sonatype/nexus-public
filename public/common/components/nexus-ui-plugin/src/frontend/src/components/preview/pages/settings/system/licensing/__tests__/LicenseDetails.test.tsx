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
import { render, screen } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

import { LicenseDetails } from '../LicenseDetails';

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

describe('LicenseDetails', () => {
  const mockLicense = {
    contactCompany: 'Acme Corp',
    contactName: 'John Doe',
    contactEmail: 'john@acme.com',
    effectiveDate: '2024-01-15T00:00:00Z',
    expirationDate: '2025-12-31T23:59:59Z',
    licenseType: 'PRO, Enterprise',
    licensedUsers: 100,
    fingerprint: 'abc123def456',
  };

  it('renders all license fields', () => {
    render(<LicenseDetails license={mockLicense} />, { wrapper: TestWrapper });

    expect(screen.getByText('Licensing')).toBeInTheDocument();
    expect(screen.getByText('Acme Corp')).toBeInTheDocument();
    expect(screen.getByText('John Doe')).toBeInTheDocument();
    expect(screen.getByText('john@acme.com')).toBeInTheDocument();
    expect(screen.getByText('abc123def456')).toBeInTheDocument();
  });

  it('displays formatted dates', () => {
    render(<LicenseDetails license={mockLicense} />, { wrapper: TestWrapper });

    // Check that dates are formatted (should contain month name)
    const dates = screen.getAllByText(/January|February|March|April|May|June|July|August|September|October|November|December/);
    expect(dates.length).toBeGreaterThan(0);
  });

  it('parses and displays multiple license types', () => {
    render(<LicenseDetails license={mockLicense} />, { wrapper: TestWrapper });

    expect(screen.getByText('PRO')).toBeInTheDocument();
    expect(screen.getByText('Enterprise')).toBeInTheDocument();
  });

  it('displays licensed users when present', () => {
    render(<LicenseDetails license={mockLicense} />, { wrapper: TestWrapper });

    expect(screen.getByText('Number of Licensed Users')).toBeInTheDocument();
    expect(screen.getByText('100')).toBeInTheDocument();
  });

  it('does not display licensed users when zero', () => {
    const licenseWithoutUsers = {
      ...mockLicense,
      licensedUsers: 0,
    };

    render(<LicenseDetails license={licenseWithoutUsers} />, { wrapper: TestWrapper });

    expect(screen.queryByText('Number of Licensed Users')).not.toBeInTheDocument();
  });

  it('does not display licensed users when undefined', () => {
    const licenseWithoutUsers = {
      ...mockLicense,
      licensedUsers: undefined,
    };

    render(<LicenseDetails license={licenseWithoutUsers} />, { wrapper: TestWrapper });

    expect(screen.queryByText('Number of Licensed Users')).not.toBeInTheDocument();
  });

  it('displays dash for missing company', () => {
    const licenseWithoutCompany = {
      ...mockLicense,
      contactCompany: undefined,
    };

    render(<LicenseDetails license={licenseWithoutCompany} />, { wrapper: TestWrapper });

    const companyField = screen.getByText('Company').closest('.license-details__field');
    expect(companyField).toHaveTextContent('—');
  });

  it('displays dash for missing name', () => {
    const licenseWithoutName = {
      ...mockLicense,
      contactName: undefined,
    };

    render(<LicenseDetails license={licenseWithoutName} />, { wrapper: TestWrapper });

    const nameField = screen.getByText('Name').closest('.license-details__field');
    expect(nameField).toHaveTextContent('—');
  });

  it('displays dash for missing email', () => {
    const licenseWithoutEmail = {
      ...mockLicense,
      contactEmail: undefined,
    };

    render(<LicenseDetails license={licenseWithoutEmail} />, { wrapper: TestWrapper });

    const emailField = screen.getByText('Email').closest('.license-details__field');
    expect(emailField).toHaveTextContent('—');
  });

  it('displays dash for missing fingerprint', () => {
    const licenseWithoutFingerprint = {
      ...mockLicense,
      fingerprint: undefined,
    };

    render(<LicenseDetails license={licenseWithoutFingerprint} />, { wrapper: TestWrapper });

    const fingerprintField = screen.getByText('Fingerprint').closest('.license-details__field');
    expect(fingerprintField).toHaveTextContent('—');
  });

  it('displays dash for missing license types', () => {
    const licenseWithoutTypes = {
      ...mockLicense,
      licenseType: undefined,
    };

    render(<LicenseDetails license={licenseWithoutTypes} />, { wrapper: TestWrapper });

    const typesField = screen.getByText('License Type(s)').closest('.license-details__field');
    expect(typesField).toHaveTextContent('—');
  });

  it('displays dash for empty license types', () => {
    const licenseWithEmptyTypes = {
      ...mockLicense,
      licenseType: '',
    };

    render(<LicenseDetails license={licenseWithEmptyTypes} />, { wrapper: TestWrapper });

    const typesField = screen.getByText('License Type(s)').closest('.license-details__field');
    expect(typesField).toHaveTextContent('—');
  });

  it('handles single license type', () => {
    const licenseWithSingleType = {
      ...mockLicense,
      licenseType: 'PRO',
    };

    render(<LicenseDetails license={licenseWithSingleType} />, { wrapper: TestWrapper });

    expect(screen.getByText('PRO')).toBeInTheDocument();
    expect(screen.queryByText('Enterprise')).not.toBeInTheDocument();
  });

  it('trims whitespace from license types', () => {
    const licenseWithSpaces = {
      ...mockLicense,
      licenseType: ' PRO , Enterprise , Community ',
    };

    render(<LicenseDetails license={licenseWithSpaces} />, { wrapper: TestWrapper });

    expect(screen.getByText('PRO')).toBeInTheDocument();
    expect(screen.getByText('Enterprise')).toBeInTheDocument();
    expect(screen.getByText('Community')).toBeInTheDocument();
  });

  it('handles invalid date format gracefully', () => {
    const licenseWithInvalidDate = {
      ...mockLicense,
      effectiveDate: 'invalid-date',
    };

    render(<LicenseDetails license={licenseWithInvalidDate} />, { wrapper: TestWrapper });

    // Should display "Invalid Date" when date parsing fails
    // The date will be shown in the Effective Date field
    const effectiveDateField = screen.getByText('Effective Date').closest('.license-details__field');
    expect(effectiveDateField).toHaveTextContent(/Invalid Date|invalid-date/);
  });

  it('displays all field labels', () => {
    render(<LicenseDetails license={mockLicense} />, { wrapper: TestWrapper });

    expect(screen.getByText('Company')).toBeInTheDocument();
    expect(screen.getByText('Name')).toBeInTheDocument();
    expect(screen.getByText('Email')).toBeInTheDocument();
    expect(screen.getByText('Effective Date')).toBeInTheDocument();
    expect(screen.getByText('Expiration Date')).toBeInTheDocument();
    expect(screen.getByText('License Type(s)')).toBeInTheDocument();
    expect(screen.getByText('Fingerprint')).toBeInTheDocument();
    expect(screen.getByText('Licensing')).toBeInTheDocument();
  });
});

