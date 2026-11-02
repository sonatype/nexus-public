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
import { render, screen, fireEvent, within } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { GlobalDistributionPage } from '../GlobalDistributionPage';
import { useCdnDefaults } from '../useCdnDefaults';
import { useBlobStoreCdnStatuses } from '../useBlobStoreCdnStatuses';
import { useRepositoryCdnList } from '../useRepositoryCdnList';

jest.mock('../useCdnDefaults');
jest.mock('../useBlobStoreCdnStatuses');
jest.mock('../useRepositoryCdnList');
const mockIsEnterprise = jest.fn(() => true);
jest.mock('../../../../../shared/hooks/useIsEnterpriseEdition', () => ({
  useIsEnterpriseEdition: () => mockIsEnterprise(),
}));
jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    checkPermission: jest.fn(),
    state: jest.fn(() => ({
      getValue: jest.fn((key: string, defaultValue?: unknown) => (key === 'distribution' ? 'selfhosted' : defaultValue)),
    })),
    useState: jest.fn((getValue: () => unknown) => getValue()),
  },
}));
jest.mock('lucide-react', () => ({
  CheckCircle2: ({ children, ...props }: any) => <span data-testid="check-circle-icon" {...props}>{children}</span>,
  Info: ({ children, ...props }: any) => <span data-testid="info-icon" {...props}>{children}</span>,
  ExternalLink: ({ children, ...props }: any) => <span data-testid="external-link-icon" {...props}>{children}</span>,
  RefreshCw: ({ children, ...props }: any) => <span data-testid="refresh-icon" {...props}>{children}</span>,
  AlertCircle: ({ children, ...props }: any) => <span data-testid="alert-circle-icon" {...props}>{children}</span>,
  Loader2: ({ children, ...props }: any) => <span data-testid="loader-icon" {...props}>{children}</span>,
  ChevronRight: ({ children, ...props }: any) => <span data-testid="chevron-right-icon" {...props}>{children}</span>,
  Check: ({ children, ...props }: any) => <span data-testid="check-icon" {...props}>{children}</span>,
  HelpCircle: ({ children, ...props }: any) => <span data-testid="help-circle-icon" {...props}>{children}</span>,
  Lock: ({ children, ...props }: any) => <span data-testid="lock-icon" {...props}>{children}</span>,
}));

const mockUseCdnDefaults = useCdnDefaults as jest.MockedFunction<typeof useCdnDefaults>;
const mockUseBlobStoreCdnStatuses = useBlobStoreCdnStatuses as jest.MockedFunction<typeof useBlobStoreCdnStatuses>;
const mockUseRepositoryCdnList = useRepositoryCdnList as jest.MockedFunction<typeof useRepositoryCdnList>;
const { ExtJS } = jest.requireMock('../../../../../../../interface/ExtJS');
const mockCheckPermission = ExtJS.checkPermission as jest.Mock;

const renderWithTheme = (component: React.ReactNode) => {
  return render(<Theme>{component}</Theme>);
};

describe('GlobalDistributionPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockIsEnterprise.mockReturnValue(true);
    mockCheckPermission.mockReturnValue(true);
    mockUseCdnDefaults.mockReturnValue({
      defaultForNewRepositories: false,
      isLoading: false,
      hasLoaded: true,
      error: null,
      setDefault: jest.fn(),
      setError: jest.fn(),
    });
    // Default scenario: an instance that already has an S3 blob store, so the page shows its
    // configuration sections. The pristine (no S3 blob store) case has its own describe block.
    mockUseBlobStoreCdnStatuses.mockReturnValue({
      blobStores: [{ name: 's3-east', cdnEnabled: false, type: 's3' }],
      isLoading: false,
      error: null,
      canReadBlobStores: true,
    });
    mockUseRepositoryCdnList.mockReturnValue({
      rows: [],
      isLoading: false,
      error: null,
      isUpdating: false,
      applyCdnState: jest.fn(),
      refetch: jest.fn(),
    });
  });

  it('renders the page heading and the toggle', () => {
    renderWithTheme(<GlobalDistributionPage />);

    expect(screen.getByRole('heading', { name: 'Global Distribution' })).toBeInTheDocument();
    expect(screen.getByText('Enable Global Distribution for new repositories by default')).toBeInTheDocument();
  });

  it('renders the green "enabled" callout without an Enterprise pill on Enterprise', () => {
    renderWithTheme(<GlobalDistributionPage />);

    const callout = screen.getByTestId('global-distribution-enabled-callout');
    expect(callout).toHaveTextContent('Global Distribution is enabled');
    expect(callout).toHaveTextContent('Repository content can be distributed through CloudFront edge locations.');
    expect(within(callout).queryByText('Enterprise')).not.toBeInTheDocument();
    expect(screen.queryByTestId('license-callout-global-distribution-settings')).not.toBeInTheDocument();
  });

  it('renders the description below the checkbox from the mockup', () => {
    renderWithTheme(<GlobalDistributionPage />);

    expect(
      screen.getByText(
        "Applies when the repository's blob store has Global Distribution enabled.",
      ),
    ).toBeInTheDocument();
  });

  it('uses the mock-up page description', () => {
    renderWithTheme(<GlobalDistributionPage />);

    expect(screen.getByText('Distribute repository content globally')).toBeInTheDocument();
  });

  it('fetches defaults and leaves the sections interactive on Enterprise', () => {
    renderWithTheme(<GlobalDistributionPage />);

    expect(mockUseCdnDefaults).toHaveBeenCalledWith({ enabled: true });
    const sections = screen.getByTestId('global-distribution-sections');
    expect(sections).not.toHaveAttribute('inert');
    expect(sections).not.toHaveClass('global-distribution-page__locked');
    expect(screen.getByTestId('checkbox-defaultForNewRepositories')).toBeEnabled();
  });

  it('hides the default checkbox and skips the fetch without nexus:settings:read', () => {
    mockCheckPermission.mockImplementation((p: string) => p !== 'nexus:settings:read');
    renderWithTheme(<GlobalDistributionPage />);

    expect(mockUseCdnDefaults).toHaveBeenCalledWith({ enabled: false });
    expect(screen.queryByTestId('checkbox-defaultForNewRepositories')).not.toBeInTheDocument();
    expect(screen.queryByText('Global Configuration')).not.toBeInTheDocument();
  });

  it('disables the default checkbox without nexus:settings:update', () => {
    mockCheckPermission.mockImplementation((p: string) => p !== 'nexus:settings:update');
    renderWithTheme(<GlobalDistributionPage />);

    expect(screen.getByTestId('checkbox-defaultForNewRepositories')).toBeDisabled();
  });

  it('does not show the pristine state when blob stores cannot be read', () => {
    mockUseBlobStoreCdnStatuses.mockReturnValue({
      blobStores: [],
      isLoading: false,
      error: null,
      canReadBlobStores: false,
    });
    renderWithTheme(<GlobalDistributionPage />);

    expect(screen.queryByTestId('global-distribution-pristine')).not.toBeInTheDocument();
    expect(screen.getByTestId('global-distribution-sections')).toBeInTheDocument();
  });

  it('reflects the initial default from the hook', () => {
    mockUseCdnDefaults.mockReturnValue({
      defaultForNewRepositories: true,
      isLoading: false,
      hasLoaded: true,
      error: null,
      setDefault: jest.fn(),
      setError: jest.fn(),
    });

    renderWithTheme(<GlobalDistributionPage />);

    const checkbox = screen.getByTestId('checkbox-defaultForNewRepositories');
    expect(checkbox).toBeChecked();
  });

  it('calls setDefault(true) when the user toggles it on', () => {
    const mockSetDefault = jest.fn();
    mockUseCdnDefaults.mockReturnValue({
      defaultForNewRepositories: false,
      isLoading: false,
      hasLoaded: true,
      error: null,
      setDefault: mockSetDefault,
      setError: jest.fn(),
    });

    renderWithTheme(<GlobalDistributionPage />);

    const checkbox = screen.getByTestId('checkbox-defaultForNewRepositories');
    fireEvent.click(checkbox);

    expect(mockSetDefault).toHaveBeenCalledWith(true);
  });

  it('shows hard-error state when initial load fails', () => {
    mockUseCdnDefaults.mockReturnValue({
      defaultForNewRepositories: false,
      isLoading: false,
      hasLoaded: false,
      error: { status: 500, message: 'Server error' },
      setDefault: jest.fn(),
      setError: jest.fn(),
    });

    renderWithTheme(<GlobalDistributionPage />);

    expect(screen.getByText('Failed to Load')).toBeInTheDocument();
    // Neither the toggle nor the success callout should render in the hard-error state.
    expect(screen.queryByText('Enable Global Distribution for new repositories by default')).not.toBeInTheDocument();
    expect(screen.queryByText('Global Distribution is enabled')).not.toBeInTheDocument();
  });

  it('shows inline error alert on operational failure', () => {
    mockUseCdnDefaults.mockReturnValue({
      defaultForNewRepositories: false,
      isLoading: false,
      hasLoaded: true,
      error: { status: 500, message: 'Server error' },
      setDefault: jest.fn(),
      setError: jest.fn(),
    });

    renderWithTheme(<GlobalDistributionPage />);

    expect(screen.getByText('Server error')).toBeInTheDocument();
    expect(screen.queryByText('Failed to Load')).not.toBeInTheDocument();
    // The success callout still renders alongside the inline error — an operational
    // failure does not invalidate the loaded state.
    expect(screen.getByText('Global Distribution is enabled')).toBeInTheDocument();
  });

  it('Dismiss button clears error via setError without calling setDefault', () => {
    const mockSetDefault = jest.fn();
    const mockSetError = jest.fn();
    mockUseCdnDefaults.mockReturnValue({
      defaultForNewRepositories: false,
      isLoading: false,
      hasLoaded: true,
      error: { status: 500, message: 'Server error' },
      setDefault: mockSetDefault,
      setError: mockSetError,
    });

    renderWithTheme(<GlobalDistributionPage />);

    const dismissButton = screen.getByRole('button', { name: 'Dismiss' });
    fireEvent.click(dismissButton);

    expect(mockSetError).toHaveBeenCalledWith(null);
    expect(mockSetDefault).not.toHaveBeenCalled();
  });

  describe('without an Enterprise license', () => {
    beforeEach(() => {
      mockIsEnterprise.mockReturnValue(false);
      mockUseCdnDefaults.mockReturnValue({
        defaultForNewRepositories: false,
        isLoading: false,
        hasLoaded: false,
        error: null,
        setDefault: jest.fn(),
        setError: jest.fn(),
      });
    });

    it('shows the amber license callout instead of the green one', () => {
      renderWithTheme(<GlobalDistributionPage />);

      expect(screen.getByTestId('license-callout-global-distribution-settings')).toHaveTextContent(
        'Global Distribution requires an Enterprise license.',
      );
      expect(screen.queryByTestId('global-distribution-enabled-callout')).not.toBeInTheDocument();
    });

    it('links Learn more outside the app in a new tab', () => {
      renderWithTheme(<GlobalDistributionPage />);

      const link = screen.getByRole('link', { name: 'Learn more (opens in a new tab)' });
      expect(link).toHaveAttribute('href', 'https://links.sonatype.com/products/nxrm3/enterprise-license');
      expect(link).toHaveAttribute('target', '_blank');
      expect(link).toHaveAttribute('rel', 'noopener noreferrer');
    });

    it('does not call the Enterprise-only defaults endpoint', () => {
      renderWithTheme(<GlobalDistributionPage />);

      expect(mockUseCdnDefaults).toHaveBeenCalledWith({ enabled: false });
      expect(screen.queryByText('Failed to Load')).not.toBeInTheDocument();
    });

    it('locks the sections below the callout', () => {
      renderWithTheme(<GlobalDistributionPage />);

      const sections = screen.getByTestId('global-distribution-sections');
      expect(sections).toHaveAttribute('inert');
      expect(sections).toHaveClass('global-distribution-page__locked');
      // The callout and its Learn more link stay outside the inert region.
      expect(sections).not.toContainElement(screen.getByTestId('license-callout-global-distribution-settings'));
      expect(screen.getByTestId('checkbox-defaultForNewRepositories')).toBeDisabled();
    });

    it('still shows real blob store data under the lock', () => {
      mockUseBlobStoreCdnStatuses.mockReturnValue({
        blobStores: [{ name: 's3-east', cdnEnabled: false, type: 's3' }],
        isLoading: false,
        error: null,
        canReadBlobStores: true,
      });

      renderWithTheme(<GlobalDistributionPage />);

      expect(within(screen.getByTestId('global-distribution-sections')).getByText('s3-east')).toBeInTheDocument();
    });
  });

  describe('pristine state (self-hosted, Enterprise, no S3 blob stores)', () => {
    beforeEach(() => {
      mockIsEnterprise.mockReturnValue(true);
      mockUseBlobStoreCdnStatuses.mockReturnValue({
        blobStores: [],
        isLoading: false,
        error: null,
        canReadBlobStores: true,
      });
    });

    it('shows the pristine card instead of the configuration sections', () => {
      renderWithTheme(<GlobalDistributionPage />);

      expect(screen.getByTestId('global-distribution-pristine')).toBeInTheDocument();
      expect(
        screen.getByRole('heading', { name: 'No blob stores have Global Distribution enabled' }),
      ).toBeInTheDocument();
      expect(
        screen.getByText(
          'Enable Global Distribution for an S3 blob store before enabling it for its repositories.',
        ),
      ).toBeInTheDocument();
    });

    it('hides the default-for-new-repositories card', () => {
      renderWithTheme(<GlobalDistributionPage />);

      expect(
        screen.queryByText('Enable Global Distribution for new repositories by default'),
      ).not.toBeInTheDocument();
      expect(screen.queryByTestId('checkbox-defaultForNewRepositories')).not.toBeInTheDocument();
    });

    it('hides the blob store and repository status sections', () => {
      renderWithTheme(<GlobalDistributionPage />);

      expect(screen.queryByText('Blob Store Distribution Status')).not.toBeInTheDocument();
      expect(screen.queryByText('Repository Distribution Status')).not.toBeInTheDocument();
    });

    it('does not show the green "enabled" callout (there is nothing enabled yet)', () => {
      renderWithTheme(<GlobalDistributionPage />);

      expect(screen.queryByTestId('global-distribution-enabled-callout')).not.toBeInTheDocument();
    });

    it('does not show an Enterprise pill (the license already covers the feature)', () => {
      renderWithTheme(<GlobalDistributionPage />);

      expect(screen.queryByTestId('edition-pill-enterprise')).not.toBeInTheDocument();
    });

    it('navigates to the blob stores page when "View blob stores" is clicked', () => {
      renderWithTheme(<GlobalDistributionPage />);

      fireEvent.click(screen.getByRole('button', { name: 'View blob stores' }));

      expect(window.location.hash).toBe('#preview/admin/repository/blobstores');
    });

    it('is not shown while blob stores are still loading', () => {
      mockUseBlobStoreCdnStatuses.mockReturnValue({
        blobStores: [],
        isLoading: true,
        error: null,
        canReadBlobStores: true,
      });

      renderWithTheme(<GlobalDistributionPage />);

      expect(screen.queryByTestId('global-distribution-pristine')).not.toBeInTheDocument();
    });

    it('is not shown when the blob store list fails to load', () => {
      mockUseBlobStoreCdnStatuses.mockReturnValue({
        blobStores: [],
        isLoading: false,
        error: 'Network error',
        canReadBlobStores: true,
      });

      renderWithTheme(<GlobalDistributionPage />);

      // A failed fetch leaves blobStores empty, which must not be mistaken for "none configured".
      expect(screen.queryByTestId('global-distribution-pristine')).not.toBeInTheDocument();
      expect(screen.getByText('Failed to load blob stores: Network error')).toBeInTheDocument();
    });

    it('is not shown once an S3 blob store exists', () => {
      mockUseBlobStoreCdnStatuses.mockReturnValue({
        blobStores: [{ name: 's3-east', cdnEnabled: false, type: 's3' }],
        isLoading: false,
        error: null,
        canReadBlobStores: true,
      });

      renderWithTheme(<GlobalDistributionPage />);

      expect(screen.queryByTestId('global-distribution-pristine')).not.toBeInTheDocument();
      expect(screen.getByText('Blob Store Distribution Status')).toBeInTheDocument();
    });

    it('is not shown without an Enterprise license', () => {
      mockIsEnterprise.mockReturnValue(false);
      mockUseCdnDefaults.mockReturnValue({
        defaultForNewRepositories: false,
        isLoading: false,
        hasLoaded: false,
        error: null,
        setDefault: jest.fn(),
        setError: jest.fn(),
      });

      renderWithTheme(<GlobalDistributionPage />);

      expect(screen.queryByTestId('global-distribution-pristine')).not.toBeInTheDocument();
      expect(screen.getByTestId('license-callout-global-distribution-settings')).toBeInTheDocument();
    });
  });
});
