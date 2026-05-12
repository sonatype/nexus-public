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

import { SupportZipPage } from '../SupportZipPage';
import * as useSupportZipApiModule from '../useSupportZipApi';

// Mock the API hook
jest.mock('../useSupportZipApi');

// Mock ExtJS
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    state: jest.fn().mockReturnValue({
      getValue: jest.fn().mockReturnValue(false), // Not clustered by default
    }),
    urlOf: jest.fn((url) => `http://localhost:8081/${url}`),
    downloadUrl: jest.fn(),
    checkPermission: jest.fn().mockReturnValue(true), // Has permission by default
  },
}));

const mockedUseSupportZipApi = useSupportZipApiModule.useSupportZipApi as jest.MockedFunction<
  typeof useSupportZipApiModule.useSupportZipApi
>;

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

describe('SupportZipPage', () => {
  const mockCreateSupportZip = jest.fn();
  const mockCreateHaSupportZips = jest.fn();
  const mockSetError = jest.fn();
  const mockGetDownloadUrl = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockCreateSupportZip.mockResolvedValue({
      file: '/path/to/support.zip',
      name: 'support.zip',
      size: '10 MB',
      truncated: false,
    });
    mockGetDownloadUrl.mockReturnValue('service/rest/wonderland/download/support.zip');

    mockedUseSupportZipApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      createSupportZip: mockCreateSupportZip,
      createHaSupportZips: mockCreateHaSupportZips,
      getDownloadUrl: mockGetDownloadUrl,
    });
  });

  it('renders the support ZIP page with header', () => {
    render(<SupportZipPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Support ZIP')).toBeInTheDocument();
    expect(
      screen.getByText('Creates a ZIP file containing useful support information about your server')
    ).toBeInTheDocument();
  });

  it('displays the support ZIP form', () => {
    render(<SupportZipPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Contents')).toBeInTheDocument();
    expect(screen.getByText('Options')).toBeInTheDocument();
    expect(screen.getByText('Create support ZIP')).toBeInTheDocument();
  });

  it('has all content checkboxes', () => {
    render(<SupportZipPage />, { wrapper: TestWrapper });

    expect(screen.getByText('System information report')).toBeInTheDocument();
    expect(screen.getByText('JVM thread-dump')).toBeInTheDocument();
    expect(screen.getByText('Configuration files')).toBeInTheDocument();
    expect(screen.getByText('Security configuration files')).toBeInTheDocument();
    expect(screen.getByText('Log files')).toBeInTheDocument();
    expect(screen.getByText('Task log files')).toBeInTheDocument();
    expect(screen.getByText('Replication log files')).toBeInTheDocument();
    expect(screen.getByText('Audit log files')).toBeInTheDocument();
    expect(screen.getByText('System and component metrics')).toBeInTheDocument();
    expect(screen.getByText('JMX information')).toBeInTheDocument();
  });

  it('has option checkboxes', () => {
    render(<SupportZipPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Limit files in the ZIP archive to 30 MB apiece')).toBeInTheDocument();
    expect(screen.getByText('Limit the ZIP archive to 50 MB')).toBeInTheDocument();
  });

  it('creates support ZIP when button is clicked', async () => {
    render(<SupportZipPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Create support ZIP'));

    await waitFor(() => {
      expect(mockCreateSupportZip).toHaveBeenCalled();
    });
  });

  it('displays response after creating support ZIP', async () => {
    render(<SupportZipPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Create support ZIP'));

    await waitFor(() => {
      expect(screen.getByText('Support ZIP Created')).toBeInTheDocument();
      expect(screen.getByText('support.zip')).toBeInTheDocument();
      expect(screen.getByText('10 MB')).toBeInTheDocument();
    });
  });

  it('displays loading state while creating', async () => {
    mockedUseSupportZipApi.mockReturnValue({
      loading: true,
      error: null,
      setError: mockSetError,
      createSupportZip: mockCreateSupportZip,
      createHaSupportZips: mockCreateHaSupportZips,
      getDownloadUrl: mockGetDownloadUrl,
    });

    render(<SupportZipPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Creating support ZIP...')).toBeInTheDocument();
  });

  it('displays error alert when creation fails', () => {
    mockedUseSupportZipApi.mockReturnValue({
      loading: false,
      error: 'Failed to create support ZIP',
      setError: mockSetError,
      createSupportZip: mockCreateSupportZip,
      createHaSupportZips: mockCreateHaSupportZips,
      getDownloadUrl: mockGetDownloadUrl,
    });

    render(<SupportZipPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Failed to create support ZIP')).toBeInTheDocument();
  });

  it('shows truncation warning when ZIP is truncated', async () => {
    mockCreateSupportZip.mockResolvedValue({
      file: '/path/to/support.zip',
      name: 'support.zip',
      size: '50 MB',
      truncated: true,
    });

    render(<SupportZipPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Create support ZIP'));

    expect(await screen.findByText(/truncated due to size limits/)).toBeInTheDocument();
  });

  it('does not show HA button in non-clustered mode', () => {
    render(<SupportZipPage />, { wrapper: TestWrapper });

    expect(screen.queryByText('Create support ZIP (all nodes)')).not.toBeInTheDocument();
  });

  describe('HA Mode', () => {
    beforeEach(() => {
      const { ExtJS } = require('@sonatype/nexus-ui-plugin');
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue(true), // Clustered mode
      });
      ExtJS.checkPermission.mockReturnValue(true);
    });

    it('shows HA header in clustered mode', () => {
      render(<SupportZipPage />, { wrapper: TestWrapper });

      expect(screen.getByText('High Availability Mode')).toBeInTheDocument();
    });

    it('shows HA button in clustered mode', () => {
      render(<SupportZipPage />, { wrapper: TestWrapper });

      expect(screen.getByText('Create support ZIP (all nodes)')).toBeInTheDocument();
    });
  });

  describe('Permission checks', () => {
    it('shows permission warning when user lacks read permission', () => {
      const { ExtJS } = require('@sonatype/nexus-ui-plugin');
      ExtJS.checkPermission.mockReturnValue(false);

      render(<SupportZipPage />, { wrapper: TestWrapper });

      expect(screen.getByTestId('support-zip-permission-warning')).toBeInTheDocument();
      expect(screen.getByText(/do not have permission to view/)).toBeInTheDocument();
    });

    it('shows create permission warning when user can read but not create', () => {
      const { ExtJS } = require('@sonatype/nexus-ui-plugin');
      ExtJS.checkPermission.mockImplementation((perm: string) => {
        if (perm === 'nexus:atlas:read') return true;
        if (perm === 'nexus:atlas:create') return false;
        return false;
      });

      render(<SupportZipPage />, { wrapper: TestWrapper });

      expect(screen.getByTestId('support-zip-create-permission-warning')).toBeInTheDocument();
      expect(screen.getByText(/do not have permission to create/)).toBeInTheDocument();
    });

    it('renders page with testid', () => {
      const { ExtJS } = require('@sonatype/nexus-ui-plugin');
      ExtJS.checkPermission.mockReturnValue(true);

      render(<SupportZipPage />, { wrapper: TestWrapper });

      expect(screen.getByTestId('support-zip-page')).toBeInTheDocument();
    });
  });
});

