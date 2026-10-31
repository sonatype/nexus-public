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
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';

import { LicensingPage } from '../LicensingPage';
import { LicenseData } from '../types';

const mockUseLicensing = jest.fn();
jest.mock('../useLicensing', () => ({ useLicensing: () => mockUseLicensing() }));

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
  InstallLicense: ({ onLicenseInstalled }: { hasExistingLicense: boolean; onLicenseInstalled: (data: LicenseData) => void }) => (
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
  const mockLicense: LicenseData = {
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

  const baseState = {
    license: {} as LicenseData,
    loading: false,
    error: null as string | null,
    activeTab: 'license',
    setActiveTab: jest.fn(),
    handleLicenseInstalled: jest.fn(),
    dismissError: jest.fn(),
    canViewHistoricalUsage: false,
  };

  function setState(overrides: Partial<typeof baseState> = {}) {
    mockUseLicensing.mockReturnValue({ ...baseState, ...overrides });
  }

  beforeEach(() => {
    jest.clearAllMocks();
    setState();
  });

  it('shows the loading indicator while loading', () => {
    setState({ loading: true });
    render(<LicensingPage />, { wrapper: TestWrapper });
    expect(screen.getByText('Loading license information...')).toBeInTheDocument();
  });

  it('renders the page header once loaded', () => {
    setState();
    render(<LicensingPage />, { wrapper: TestWrapper });
    expect(screen.getByRole('heading', { name: 'Licensing' })).toBeInTheDocument();
    expect(screen.getByText('A valid license is required for PRO features; manage it here')).toBeInTheDocument();
  });

  it('shows license details when a license is present and there is no error', () => {
    setState({ license: mockLicense });
    render(<LicensingPage />, { wrapper: TestWrapper });
    expect(screen.getByText('Acme Corp')).toBeInTheDocument();
    expect(screen.getByText('John Doe')).toBeInTheDocument();
    expect(screen.getByText('john@acme.com')).toBeInTheDocument();
    expect(screen.getByText('abc123def456')).toBeInTheDocument();
  });

  it('displays licensed usage when limits exist', () => {
    setState({ license: mockLicense });
    render(<LicensingPage />, { wrapper: TestWrapper });
    expect(screen.getByText('Licensed Usage')).toBeInTheDocument();
    expect(screen.getByText('1,000,000')).toBeInTheDocument(); // maxRepoRequests formatted
    expect(screen.getByText('50,000')).toBeInTheDocument(); // maxRepoComponents formatted
  });

  it('displays licensed usage when limit values are zero', () => {
    setState({ license: { ...mockLicense, maxRepoRequests: 0, maxRepoComponents: 0 } });
    render(<LicensingPage />, { wrapper: TestWrapper });
    expect(screen.getByText('Licensed Usage')).toBeInTheDocument();
    // Zero values are valid — section must render (not hidden by falsy check)
    expect(screen.getAllByText('0').length).toBeGreaterThanOrEqual(1);
  });

  it('shows the error banner and dismisses it', () => {
    const dismissError = jest.fn();
    setState({ error: 'Failed to load license information', dismissError });
    render(<LicensingPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Failed to load license information')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /dismiss/i }));
    expect(dismissError).toHaveBeenCalled();
  });

  it('hides the Usage tab without the metrics permission', () => {
    setState({ canViewHistoricalUsage: false });
    render(<LicensingPage />, { wrapper: TestWrapper });
    expect(screen.queryByRole('tab', { name: /usage/i })).not.toBeInTheDocument();
  });

  it('shows the Usage tab with the metrics permission', () => {
    setState({ canViewHistoricalUsage: true });
    render(<LicensingPage />, { wrapper: TestWrapper });
    expect(screen.getByRole('tab', { name: /usage/i })).toBeInTheDocument();
  });

  it('switches tabs when tab is clicked', async () => {
    const setActiveTab = jest.fn();
    setState({ canViewHistoricalUsage: true, setActiveTab });
    render(<LicensingPage />, { wrapper: TestWrapper });

    const usageTab = screen.getByRole('tab', { name: /usage/i });

    // Click the Usage tab
    await userEvent.click(usageTab);

    expect(setActiveTab).toHaveBeenCalledWith('usage');
  });

  it('does not show license details when no license exists', () => {
    setState({ license: {} });
    render(<LicensingPage />, { wrapper: TestWrapper });
    expect(screen.queryByText('Acme Corp')).not.toBeInTheDocument();
  });

  it('does not show licensed usage when limits are missing', () => {
    setState({ license: { contactCompany: 'Acme Corp' } });
    render(<LicensingPage />, { wrapper: TestWrapper });
    expect(screen.getByText('Acme Corp')).toBeInTheDocument();
    expect(screen.queryByText('Licensed Usage')).not.toBeInTheDocument();
  });

  it('forwards a successful install to handleLicenseInstalled', () => {
    const handleLicenseInstalled = jest.fn();
    setState({ handleLicenseInstalled });
    render(<LicensingPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByRole('button', { name: 'Install License' }));
    expect(handleLicenseInstalled).toHaveBeenCalledWith({
      contactCompany: 'New Corp',
      contactName: 'Jane Doe',
      contactEmail: 'jane@newcorp.com',
      effectiveDate: '2024-06-01T00:00:00Z',
      expirationDate: '2026-06-01T00:00:00Z',
      licenseType: 'PRO',
      licensedUsers: '50',
      fingerprint: 'xyz789',
    });
  });

  it('formats dates correctly', () => {
    setState({ license: mockLicense });
    render(<LicensingPage />, { wrapper: TestWrapper });

    // Check that dates are formatted (should contain month name)
    const dateTexts = screen.getAllByText(/January|February|March|April|May|June|July|August|September|October|November|December/);
    expect(dateTexts.length).toBeGreaterThan(0);
  });

  it('parses license types correctly', () => {
    setState({ license: mockLicense });
    render(<LicensingPage />, { wrapper: TestWrapper });
    expect(screen.getByText('PRO')).toBeInTheDocument();
    expect(screen.getByText('Enterprise')).toBeInTheDocument();
  });

  it('displays licensed users when present', () => {
    setState({ license: mockLicense });
    render(<LicensingPage />, { wrapper: TestWrapper });
    expect(screen.getByText('Number of Licensed Users')).toBeInTheDocument();
    expect(screen.getByText('100')).toBeInTheDocument();
  });

  it('does not display licensed users when zero or undefined', () => {
    setState({ license: { ...mockLicense, licensedUsers: '0' } });
    render(<LicensingPage />, { wrapper: TestWrapper });
    expect(screen.getByText('Acme Corp')).toBeInTheDocument();
    expect(screen.queryByText('Number of Licensed Users')).not.toBeInTheDocument();
  });

  describe('breadcrumbs', () => {
    it('renders Settings breadcrumb that navigates to settings page', () => {
      setState();
      render(<LicensingPage />, { wrapper: TestWrapper });

      expect(screen.getByRole('button', { name: 'Settings' })).toBeInTheDocument();

      // Click Settings breadcrumb navigates to settings page
      screen.getByRole('button', { name: 'Settings' }).click();
      expect(window.location.hash).toBe('#preview/admin/settings');
    });

    it('renders Licensing as current page breadcrumb', () => {
      setState();
      render(<LicensingPage />, { wrapper: TestWrapper });

      // The current page item is rendered as Text (not a button) with aria-current="page"
      const breadcrumb = screen.getByText('Licensing', { selector: '[aria-current="page"]' });
      expect(breadcrumb).toBeInTheDocument();
    });
  });
});
