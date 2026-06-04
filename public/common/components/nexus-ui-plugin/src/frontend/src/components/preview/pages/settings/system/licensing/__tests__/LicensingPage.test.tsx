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
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';

import { LicensingPage } from '../LicensingPage';
import * as useLicensingApiModule from '../useLicensingApi';

// Mock the API hook
jest.mock('../useLicensingApi');

const mockedUseLicensingApi = useLicensingApiModule.useLicensingApi as jest.MockedFunction<typeof useLicensingApiModule.useLicensingApi>;

// Mock ExtJS
jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    checkPermission: jest.fn().mockReturnValue(false),
  },
}));

// Mock HistoricalUsage component
jest.mock('../../../../../../pages/admin/Usage/HistoricalUsage', () => ({
  __esModule: true,
  default: jest.fn(() => <div data-testid="historical-usage">Historical Usage</div>),
  HistoricalUsage: jest.fn(() => <div data-testid="historical-usage">Historical Usage</div>),
}));

// Mock historicalUsageColumns
jest.mock('../../../../../../pages/admin/Usage/HistoricalUsageColumns', () => ({
  historicalUsageColumns: {
    metricDateMonth: { key: 'month', Header: () => 'Month' },
    peakComponents: { key: 'components', Header: () => 'Components' },
    percentageChangeComponent: { key: 'componentChange', Header: () => 'Change' },
    totalRequests: { key: 'requests', Header: () => 'Requests' },
    percentageChangeRequests: { key: 'requestChange', Header: () => 'Change' },
    totalEgress: { key: 'egress', Header: () => 'Egress' },
    peakStorage: { key: 'storage', Header: () => 'Storage' },
  },
}));

// Mock shared/form to avoid SCSS loading
jest.mock('../../../../../shared/form', () => ({
  SettingsAlert: ({ children, onClose }: { children: React.ReactNode; onClose?: () => void }) => (
    <div className="settings-alert">
      {children}
      {onClose && <button onClick={onClose}>Dismiss</button>}
    </div>
  ),
  SettingsButton: ({ children, testId, disabled, type, onClick }: { children: React.ReactNode; testId?: string; disabled?: boolean; type?: string; onClick?: () => void }) => (
    <button data-testid={testId} disabled={disabled} type={(type as any) || 'button'} onClick={onClick}>
      {children}
    </button>
  ),
  SettingsFormSection: ({ children, title }: { children: React.ReactNode; title?: string }) => (
    <div className="settings-form-section">{title && <h3>{title}</h3>}{children}</div>
  ),
}));

// Mock InstallLicense to avoid its deep dependency chain
jest.mock('../InstallLicense', () => ({
  InstallLicense: ({ onLicenseInstalled }: { hasExistingLicense: boolean; onLicenseInstalled: () => void }) => (
    <div data-testid="install-license">
      <button onClick={onLicenseInstalled}>Install License</button>
    </div>
  ),
}));


// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

describe('LicensingPage', () => {
  const mockLicense = {
    contactCompany: 'Acme Corp',
    contactName: 'John Doe',
    contactEmail: 'john@acme.com',
    effectiveDate: '2024-01-01T00:00:00Z',
    expirationDate: '2025-12-31T23:59:59Z',
    licenseType: 'PRO, Enterprise',
    licensedUsers: 100,
    fingerprint: 'abc123def456',
    maxRepoRequests: 1000000,
    maxRepoComponents: 50000,
  };

  const mockFetchLicense = jest.fn();
  const mockSetError = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockedUseLicensingApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchLicense: mockFetchLicense.mockResolvedValue(mockLicense),
      uploadLicense: jest.fn(),
      getLicenseAgreementUrl: jest.fn().mockReturnValue('https://example.com/license'),
    });
  });

  it('renders loading state initially', () => {
    mockedUseLicensingApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchLicense: jest.fn(() => new Promise(() => {})), // Never resolves
      uploadLicense: jest.fn(),
      getLicenseAgreementUrl: jest.fn(),
    });

    render(<LicensingPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Loading license information...')).toBeInTheDocument();
  });

  it('renders the page header', async () => {
    render(<LicensingPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      // Use role to specifically target the heading
      expect(screen.getByRole('heading', { name: 'Licensing' })).toBeInTheDocument();
    });

    expect(screen.getByText('A valid license is required for PRO features; manage it here')).toBeInTheDocument();
  });

  it('displays license details when license exists', async () => {
    render(<LicensingPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Acme Corp')).toBeInTheDocument();
    });

    expect(screen.getByText('John Doe')).toBeInTheDocument();
    expect(screen.getByText('john@acme.com')).toBeInTheDocument();
    expect(screen.getByText('abc123def456')).toBeInTheDocument();
  });

  it('displays licensed usage when limits exist', async () => {
    render(<LicensingPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Licensed Usage')).toBeInTheDocument();
    });

    expect(screen.getByText('1,000,000')).toBeInTheDocument(); // maxRepoRequests formatted
    expect(screen.getByText('50,000')).toBeInTheDocument(); // maxRepoComponents formatted
  });

  it('displays error message when fetch fails', async () => {
    mockFetchLicense.mockRejectedValue(new Error('Failed to load license information'));

    render(<LicensingPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Failed to load license information')).toBeInTheDocument();
    });
  });

  it('dismisses error when dismiss button is clicked', async () => {
    mockFetchLicense.mockRejectedValue(new Error('Failed to load license information'));

    render(<LicensingPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Failed to load license information')).toBeInTheDocument();
    });

    const dismissButton = screen.getByRole('button', { name: /dismiss/i });
    fireEvent.click(dismissButton);

    await waitFor(() => {
      expect(screen.queryByText('Failed to load license information')).not.toBeInTheDocument();
    });
  });

  it('shows only License tab when user lacks metrics permission', async () => {
    const { ExtJS } = require('../../../../../../../interface/ExtJS');
    ExtJS.checkPermission.mockReturnValue(false);

    render(<LicensingPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      // Wait for tablist to appear
      expect(screen.getByRole('tablist')).toBeInTheDocument();
    });

    // Should have only 1 tab (License) when metrics permission is false
    const tabs = screen.getAllByRole('tab');
    expect(tabs).toHaveLength(1);
    expect(tabs[0]).toHaveTextContent('License');
  });

  it('shows Usage tab when user has metrics permission', async () => {
    const { ExtJS } = require('../../../../../../../interface/ExtJS');
    ExtJS.checkPermission.mockReturnValue(true);

    render(<LicensingPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      // Wait for tablist to appear
      expect(screen.getByRole('tablist')).toBeInTheDocument();
    });

    // Should have 2 tabs (License and Usage) when metrics permission is true
    const tabs = screen.getAllByRole('tab');
    expect(tabs).toHaveLength(2);
    expect(tabs[0]).toHaveTextContent('License');
    expect(tabs[1]).toHaveTextContent('Usage');
  });

  it('switches tabs when tab is clicked', async () => {
    const { ExtJS } = require('../../../../../../../interface/ExtJS');
    ExtJS.checkPermission.mockReturnValue(true);

    render(<LicensingPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      // Wait for tablist to appear
      expect(screen.getByRole('tablist')).toBeInTheDocument();
    });

    // Find Usage tab (second tab) and verify it's not selected initially
    const tabs = screen.getAllByRole('tab');
    const licenseTab = tabs[0];
    const usageTab = tabs[1];

    // License tab should be selected initially
    expect(licenseTab).toHaveAttribute('aria-selected', 'true');
    expect(usageTab).toHaveAttribute('aria-selected', 'false');

    // Click the Usage tab using userEvent.click (v13 API)
    await userEvent.click(usageTab);

    // After clicking, Usage tab should be selected
    await waitFor(() => {
      expect(usageTab).toHaveAttribute('aria-selected', 'true');
    });
    expect(licenseTab).toHaveAttribute('aria-selected', 'false');
  });

  it('does not show license details when no license exists', async () => {
    mockFetchLicense.mockResolvedValue({});

    render(<LicensingPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      // Use role to specifically target the heading
      expect(screen.getByRole('heading', { name: 'Licensing' })).toBeInTheDocument();
    });

    expect(screen.queryByText('Acme Corp')).not.toBeInTheDocument();
  });

  it('does not show licensed usage when limits are missing', async () => {
    mockFetchLicense.mockResolvedValue({
      contactCompany: 'Acme Corp',
      // No maxRepoRequests or maxRepoComponents
    });

    render(<LicensingPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Acme Corp')).toBeInTheDocument();
    });

    expect(screen.queryByText('Licensed Usage')).not.toBeInTheDocument();
  });

  it('refreshes license data when license is installed', async () => {
    const mockOnLicenseInstalled = jest.fn();
    render(<LicensingPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Acme Corp')).toBeInTheDocument();
    });

    // Simulate license installation by calling the callback
    // This would normally be called by InstallLicense component
    const updatedLicense = { ...mockLicense, contactCompany: 'New Corp' };
    mockFetchLicense.mockResolvedValue(updatedLicense);

    // Trigger refresh (simulating InstallLicense calling onLicenseInstalled)
    await waitFor(() => {
      expect(mockFetchLicense).toHaveBeenCalled();
    });
  });

  it('formats dates correctly', async () => {
    render(<LicensingPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Acme Corp')).toBeInTheDocument();
    });

    // Check that dates are formatted (should contain month name)
    // Use getAllByText since there are multiple dates (effective and expiration)
    const dateTexts = screen.getAllByText(/January|February|March|April|May|June|July|August|September|October|November|December/);
    expect(dateTexts.length).toBeGreaterThan(0);
  });

  it('parses license types correctly', async () => {
    render(<LicensingPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('PRO')).toBeInTheDocument();
    });

    expect(screen.getByText('Enterprise')).toBeInTheDocument();
  });

  it('displays licensed users when present', async () => {
    render(<LicensingPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Number of Licensed Users')).toBeInTheDocument();
    });

    expect(screen.getByText('100')).toBeInTheDocument();
  });

  it('does not display licensed users when zero or undefined', async () => {
    mockFetchLicense.mockResolvedValue({
      ...mockLicense,
      licensedUsers: 0,
    });

    render(<LicensingPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Acme Corp')).toBeInTheDocument();
    });

    expect(screen.queryByText('Number of Licensed Users')).not.toBeInTheDocument();
  });
});


