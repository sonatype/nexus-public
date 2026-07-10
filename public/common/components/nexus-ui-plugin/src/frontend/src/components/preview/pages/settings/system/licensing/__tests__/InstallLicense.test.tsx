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

import { InstallLicense } from '../InstallLicense';
import { useLicensingApi } from '../useLicensingApi';

// Mock the API hook
jest.mock('../useLicensingApi');
const mockedUseLicensingApi = useLicensingApi as jest.MockedFunction<typeof useLicensingApi>;

// Mock FileDropzone
jest.mock('../../../../upload/components/FileDropzone', () => ({
  FileDropzone: ({ files, onChange, disabled }: any) => (
    <div data-testid="file-dropzone">
      <input
        type="file"
        disabled={disabled}
        onChange={(e) => {
          if (e.target.files && e.target.files.length > 0) {
            onChange(Array.from(e.target.files));
          }
        }}
        data-testid="file-input"
      />
    </div>
  ),
}));

// Mock LicenseAgreementModal
jest.mock('../LicenseAgreementModal', () => ({
  LicenseAgreementModal: ({ open, onAccept, onDecline }: any) =>
    open ? (
      <div data-testid="license-agreement-modal">
        <button onClick={onAccept} data-testid="accept-button">Accept</button>
        <button onClick={onDecline} data-testid="decline-button">Decline</button>
      </div>
    ) : null,
}));

// Mock ExtJS so we can control checkPermission per-test
jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    checkPermission: jest.fn().mockReturnValue(true),
  },
}));

const { ExtJS } = require('../../../../../../../interface/ExtJS');

const renderWithTheme = (component: React.ReactElement) => {
  return render(<Theme>{component}</Theme>);
};

describe('InstallLicense', () => {
  const mockOnLicenseInstalled = jest.fn();

  const mockLicenseData = {
    contactCompany: 'Acme Corp',
    contactName: 'John Doe',
    contactEmail: 'john@acme.com',
    effectiveDate: '2024-01-01T00:00:00Z',
    expirationDate: '2025-12-31T23:59:59Z',
    licenseType: 'PRO',
    licensedUsers: '100',
    fingerprint: 'abc123',
  };

  beforeEach(() => {
    jest.clearAllMocks();
    ExtJS.checkPermission.mockReturnValue(true);
    mockedUseLicensingApi.mockReturnValue({
      loading: false,
      error: null,
      setError: jest.fn(),
      fetchLicense: jest.fn(),
      uploadLicense: jest.fn().mockResolvedValue(mockLicenseData),
      getLicenseAgreementUrl: jest.fn().mockReturnValue('https://example.com/license'),
    });
  });

  it('renders the install license section', () => {
    renderWithTheme(<InstallLicense hasExistingLicense={false} onLicenseInstalled={mockOnLicenseInstalled} />);

    expect(screen.getByText('Install License')).toBeInTheDocument();
  });

  it('shows file dropzone when user has upload permission', () => {
    // Global mock returns true for checkPermission — no override needed
    renderWithTheme(<InstallLicense hasExistingLicense={false} onLicenseInstalled={mockOnLicenseInstalled} />);

    expect(screen.getByTestId('file-dropzone')).toBeInTheDocument();
    expect(screen.queryByText(/do not have permission/i)).not.toBeInTheDocument();
  });

  it('shows read-only message and hides dropzone when permission is denied', () => {
    ExtJS.checkPermission.mockReturnValueOnce(false);

    renderWithTheme(<InstallLicense hasExistingLicense={false} onLicenseInstalled={mockOnLicenseInstalled} />);

    expect(screen.getByText(/do not have permission/i)).toBeInTheDocument();
    expect(screen.queryByTestId('file-dropzone')).not.toBeInTheDocument();
  });

  it('useLicensingApi hook is called', () => {
    renderWithTheme(<InstallLicense hasExistingLicense={false} onLicenseInstalled={mockOnLicenseInstalled} />);

    expect(mockedUseLicensingApi).toHaveBeenCalled();
  });

  it('upload button has data-analytics-id="nxrm-licensing-upload"', () => {
    renderWithTheme(<InstallLicense hasExistingLicense={false} onLicenseInstalled={mockOnLicenseInstalled} />);

    const uploadButton = screen.getByRole('button', { name: 'Upload License' });
    expect(uploadButton).toHaveAttribute('data-analytics-id', 'nxrm-licensing-upload');
  });

  it('disables upload button when licenseUrl is missing', () => {
    mockedUseLicensingApi.mockReturnValue({
      loading: false,
      error: null,
      setError: jest.fn(),
      fetchLicense: jest.fn(),
      uploadLicense: jest.fn(),
      getLicenseAgreementUrl: jest.fn().mockReturnValue(null),
    });

    renderWithTheme(<InstallLicense hasExistingLicense={false} onLicenseInstalled={mockOnLicenseInstalled} />);

    const uploadButton = screen.getByRole('button', { name: 'Upload License' });
    expect(uploadButton).toBeDisabled();
  });

  it('disables upload button when licenseUrl is empty string', () => {
    mockedUseLicensingApi.mockReturnValue({
      loading: false,
      error: null,
      setError: jest.fn(),
      fetchLicense: jest.fn(),
      uploadLicense: jest.fn(),
      getLicenseAgreementUrl: jest.fn().mockReturnValue(''),
    });

    renderWithTheme(<InstallLicense hasExistingLicense={false} onLicenseInstalled={mockOnLicenseInstalled} />);

    const uploadButton = screen.getByRole('button', { name: 'Upload License' });
    expect(uploadButton).toBeDisabled();
  });
});
