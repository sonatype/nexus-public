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
    useUser: jest.fn().mockReturnValue(null),
    useState: jest.fn().mockReturnValue(undefined),
    state: jest.fn().mockReturnValue({ getValue: jest.fn().mockReturnValue(undefined) }),
  },
}));

// Mock parseApiError used by LicensingPage for error handling
jest.mock('../../../../../../../interface/api', () => ({
  parseApiError: jest.fn((err: any) => ({ message: err?.message || 'An error occurred' })),
}));

// Mock HistoricalUsagePreview to avoid deep dependency chain
jest.mock('../HistoricalUsagePreview', () => ({
  __esModule: true,
  default: jest.fn(() => <div data-testid="historical-usage-preview">Historical Usage</div>),
  HistoricalUsagePreview: jest.fn(() => <div data-testid="historical-usage-preview">Historical Usage</div>),
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
  InstallLicense: ({ onLicenseInstalled }: { hasExistingLicense: boolean; onLicenseInstalled: (data: any) => void }) => (
    <div data-testid="install-license">
      <button
        onClick={() => onLicenseInstalled({
          contactCompany: 'New Corp',
          contactName: 'Jane Doe',
          contactEmail: 'jane@newcorp.com',
          effectiveDate: '2024-06-01T00:00:00Z',
          expirationDate: '2026-06-01T00:00:00Z',
          licenseType: 'PRO',
          licensedUsers: '50',
          fingerprint: 'xyz789',
        })}
      >
        Install License
      </button>
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
    licensedUsers: '100',
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

  it('displays licensed usage when limit values are zero', async () => {
    mockFetchLicense.mockResolvedValue({
      ...mockLicense,
      maxRepoRequests: 0,
      maxRepoComponents: 0,
    });

    render(<LicensingPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Licensed Usage')).toBeInTheDocument();
    });

    // Zero values are valid — section must render (not hidden by falsy check)
    expect(screen.getAllByText('0').length).toBeGreaterThanOrEqual(1);
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

  it('shows new license details immediately after license is installed without refetching', async () => {
    // Start with no license installed
    mockFetchLicense.mockResolvedValue({});

    render(<LicensingPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Licensing' })).toBeInTheDocument();
    });

    // Confirm no license details shown initially
    expect(screen.queryByText('New Corp')).not.toBeInTheDocument();

    // Clear mock call count from initial load
    mockFetchLicense.mockClear();

    // Click "Install License" which calls onLicenseInstalled with the new license data
    const installButton = screen.getByRole('button', { name: 'Install License' });
    fireEvent.click(installButton);

    // License details should appear immediately without a second fetchLicense call
    await waitFor(() => {
      expect(screen.getByText('New Corp')).toBeInTheDocument();
    });

    expect(screen.getByText('Jane Doe')).toBeInTheDocument();
    expect(screen.getByText('jane@newcorp.com')).toBeInTheDocument();

    // Should NOT have made a second fetch - data comes from POST response
    expect(mockFetchLicense).not.toHaveBeenCalled();
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
      licensedUsers: '0',
    });

    render(<LicensingPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Acme Corp')).toBeInTheDocument();
    });

    expect(screen.queryByText('Number of Licensed Users')).not.toBeInTheDocument();
  });

  describe('breadcrumbs', () => {
    it('renders Settings breadcrumb that navigates to settings page', async () => {
      render(<LicensingPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Settings' })).toBeInTheDocument();
      });

      // Click Settings breadcrumb navigates to settings page
      screen.getByRole('button', { name: 'Settings' }).click();
      expect(window.location.hash).toBe('#preview/admin/settings');
    });

    it('renders Licensing as current page breadcrumb', async () => {
      render(<LicensingPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        // The current page item is rendered as Text (not a button) with aria-current="page"
        const breadcrumb = screen.getByText('Licensing', { selector: '[aria-current="page"]' });
        expect(breadcrumb).toBeInTheDocument();
      });
    });
  });
});
