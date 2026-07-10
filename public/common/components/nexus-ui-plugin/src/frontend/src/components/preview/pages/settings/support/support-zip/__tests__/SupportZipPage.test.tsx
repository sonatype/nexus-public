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

jest.mock('../useSupportZipApi');

// Stub the HA component — it has its own coverage in SupportZipHA.test.tsx
jest.mock('../SupportZipHA', () => ({
  SupportZipHA: () => <div data-testid="support-zip-ha-stub" />,
}));

jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    state: jest.fn().mockReturnValue({
      getValue: jest.fn().mockReturnValue(false),
    }),
    useState: (fn: () => unknown) => fn(),
    urlOf: jest.fn((url) => `http://localhost:8081/${url}`),
    downloadUrl: jest.fn(),
    checkPermission: jest.fn().mockReturnValue(true),
  },
}));

const mockedUseSupportZipApi = useSupportZipApiModule.useSupportZipApi as jest.MockedFunction<
  typeof useSupportZipApiModule.useSupportZipApi
>;

function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

function buildHookReturn(overrides: Partial<ReturnType<typeof useSupportZipApiModule.useSupportZipApi>> = {}) {
  return {
    loading: false,
    error: null,
    setError: jest.fn(),
    createSupportZip: jest.fn().mockResolvedValue({
      file: '/path/to/support.zip',
      name: 'support.zip',
      size: '10 MB',
      truncated: false,
    }),
    fetchActiveNodes: jest.fn().mockResolvedValue([]),
    fetchNodeStatus: jest.fn(),
    generateForNode: jest.fn(),
    clearNode: jest.fn(),
    getDownloadUrl: jest.fn().mockReturnValue('service/rest/wonderland/download/support.zip'),
    ...overrides,
  } as ReturnType<typeof useSupportZipApiModule.useSupportZipApi>;
}

describe('SupportZipPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedUseSupportZipApi.mockReturnValue(buildHookReturn());
  });

  it('renders the support ZIP page with header', () => {
    render(<SupportZipPage />, { wrapper: TestWrapper });

    expect(screen.getByRole('heading', { name: 'Support ZIP' })).toBeInTheDocument();
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
    const create = jest.fn().mockResolvedValue({
      file: '/x',
      name: 'support.zip',
      size: '10 MB',
      truncated: false,
    });
    mockedUseSupportZipApi.mockReturnValue(buildHookReturn({ createSupportZip: create }));

    render(<SupportZipPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Create support ZIP'));

    await waitFor(() => {
      expect(create).toHaveBeenCalled();
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

  it('displays loading state while creating', () => {
    mockedUseSupportZipApi.mockReturnValue(buildHookReturn({ loading: true }));

    render(<SupportZipPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Creating support ZIP...')).toBeInTheDocument();
  });

  it('displays error alert when creation fails', () => {
    mockedUseSupportZipApi.mockReturnValue(
      buildHookReturn({ error: 'Failed to create support ZIP' })
    );

    render(<SupportZipPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Failed to create support ZIP')).toBeInTheDocument();
  });

  it('shows truncation warning when ZIP is truncated', async () => {
    mockedUseSupportZipApi.mockReturnValue(
      buildHookReturn({
        createSupportZip: jest.fn().mockResolvedValue({
          file: '/path/to/support.zip',
          name: 'support.zip',
          size: '50 MB',
          truncated: true,
        }),
      })
    );

    render(<SupportZipPage />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Create support ZIP'));

    expect(await screen.findByText(/truncated due to size limits/)).toBeInTheDocument();
  });

  it('renders the form (not the HA branch) in non-clustered mode', () => {
    const { container } = render(<SupportZipPage />, { wrapper: TestWrapper });

    expect(screen.queryByTestId('support-zip-ha-stub')).not.toBeInTheDocument();
    expect(container.querySelector('[data-clustered="false"]')).toBeInTheDocument();
  });

  describe('HA Mode', () => {
    beforeEach(() => {
      const { ExtJS } = require('../../../../../../../interface/ExtJS');
      ExtJS.state.mockReturnValue({
        getValue: jest.fn().mockReturnValue(true),
      });
      ExtJS.checkPermission.mockReturnValue(true);
    });

    it('renders the HA branch in clustered mode', () => {
      render(<SupportZipPage />, { wrapper: TestWrapper });

      expect(screen.getByTestId('support-zip-ha-stub')).toBeInTheDocument();
    });

    it('marks the page as clustered', () => {
      const { container } = render(<SupportZipPage />, { wrapper: TestWrapper });

      expect(container.querySelector('[data-clustered="true"]')).toBeInTheDocument();
    });
  });

  describe('Permission checks', () => {
    it('shows permission warning when user lacks read permission', () => {
      const { ExtJS } = require('../../../../../../../interface/ExtJS');
      ExtJS.checkPermission.mockReturnValue(false);

      render(<SupportZipPage />, { wrapper: TestWrapper });

      expect(screen.getByTestId('support-zip-permission-warning')).toBeInTheDocument();
      expect(screen.getByText(/do not have permission to view/)).toBeInTheDocument();
    });

    it('shows create permission warning when user can read but not create', () => {
      const { ExtJS } = require('../../../../../../../interface/ExtJS');
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
      const { ExtJS } = require('../../../../../../../interface/ExtJS');
      ExtJS.checkPermission.mockReturnValue(true);

      render(<SupportZipPage />, { wrapper: TestWrapper });

      expect(screen.getByTestId('support-zip-page')).toBeInTheDocument();
    });
  });

  // Note: scroll behaviour at high zoom levels (NEXUS-52211) cannot be covered by unit tests —
  // JSDOM does not apply stylesheets, so overflow assertions would never fail regardless of the CSS.
  // Verify manually at 125%+ browser zoom or via E2E tests.

  describe('Breadcrumb navigation', () => {
    it('renders breadcrumbs with Settings link', () => {
      render(<SupportZipPage />, { wrapper: TestWrapper });

      expect(screen.getByRole('button', { name: 'Settings' })).toBeInTheDocument();
    });

    it('renders Support ZIP as current page in breadcrumbs', () => {
      const { container } = render(<SupportZipPage />, { wrapper: TestWrapper });

      const currentBreadcrumb = container.querySelector('[aria-current="page"]');
      expect(currentBreadcrumb).toBeInTheDocument();
      expect(currentBreadcrumb?.textContent).toBe('Support ZIP');
    });

    it('navigates to Settings when Settings breadcrumb is clicked', () => {
      render(<SupportZipPage />, { wrapper: TestWrapper });

      const originalHash = window.location.hash;
      screen.getByRole('button', { name: 'Settings' }).click();
      expect(window.location.hash).toBe('#preview/admin/settings');
      window.location.hash = originalHash;
    });
  });
});
