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

import { UpgradePage } from '../UpgradePage';

// Mock ExtJS
jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    useStatus: jest.fn(),
    useLicense: jest.fn(),
  },
}));

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

describe('UpgradePage', () => {
  let windowOpenSpy: jest.SpyInstance;

  beforeEach(() => {
    jest.clearAllMocks();
    windowOpenSpy = jest.spyOn(window, 'open').mockImplementation(() => null);
    const { ExtJS } = require('../../../../../../../interface/ExtJS');
    ExtJS.useStatus.mockReturnValue({
      version: '3.88.0-01',
      edition: 'PRO',
    });
    ExtJS.useLicense.mockReturnValue({
      daysToExpiry: 30,
    });
  });

  afterEach(() => {
    windowOpenSpy.mockRestore();
  });

  it('renders the page header', () => {
    render(<UpgradePage />, { wrapper: TestWrapper });

    expect(screen.getByRole('heading', { name: 'Upgrade' })).toBeInTheDocument();
    expect(screen.getByText('Version information and upgrade options')).toBeInTheDocument();
  });

  it('displays current version', () => {
    render(<UpgradePage />, { wrapper: TestWrapper });

    expect(screen.getByText('Nexus Repository 3.88.0-01')).toBeInTheDocument();
  });

  it('displays edition badge for PRO', () => {
    render(<UpgradePage />, { wrapper: TestWrapper });

    expect(screen.getByText('Professional Edition')).toBeInTheDocument();
  });

  it('displays OSS edition badge when not PRO', () => {
    const { ExtJS } = require('../../../../../../../interface/ExtJS');
    ExtJS.useStatus.mockReturnValue({
      version: '3.88.0-01',
      edition: 'OSS',
    });

    render(<UpgradePage />, { wrapper: TestWrapper });

    expect(screen.getByText('OSS Edition')).toBeInTheDocument();
  });

  it('displays license expiry', () => {
    render(<UpgradePage />, { wrapper: TestWrapper });

    expect(screen.getByText('License valid for 30 days')).toBeInTheDocument();
  });

  it('shows upgrade links', () => {
    render(<UpgradePage />, { wrapper: TestWrapper });

    expect(screen.getByText('Release Notes')).toBeInTheDocument();
    expect(screen.getByText('Upgrade Guide')).toBeInTheDocument();
    expect(screen.getByText('Downloads')).toBeInTheDocument();
  });

  it('opens release notes link', () => {
    render(<UpgradePage />, { wrapper: TestWrapper });

    const viewButtons = screen.getAllByRole('button', { name: /view/i });
    fireEvent.click(viewButtons[0]);

    expect(windowOpenSpy).toHaveBeenCalledWith(
      'http://links.sonatype.com/products/nxrm3/release-notes',
      '_blank'
    );
  });

  it('opens upgrade guide link', () => {
    render(<UpgradePage />, { wrapper: TestWrapper });

    const viewButtons = screen.getAllByRole('button', { name: /view/i });
    fireEvent.click(viewButtons[1]);

    expect(windowOpenSpy).toHaveBeenCalledWith(
      'http://links.sonatype.com/products/nxrm3/docs/upgrade',
      '_blank'
    );
  });

  it('shows Pro upgrade card for OSS users', () => {
    const { ExtJS } = require('../../../../../../../interface/ExtJS');
    ExtJS.useStatus.mockReturnValue({
      version: '3.88.0-01',
      edition: 'OSS',
    });

    render(<UpgradePage />, { wrapper: TestWrapper });

    expect(screen.getByText('Upgrade to Professional Edition')).toBeInTheDocument();
  });

  it('hides Pro upgrade card for Pro users', () => {
    render(<UpgradePage />, { wrapper: TestWrapper });

    expect(screen.queryByText('Upgrade to Professional Edition')).not.toBeInTheDocument();
  });

  it('applies custom className', () => {
    const { container } = render(<UpgradePage className="custom-class" />, { wrapper: TestWrapper });

    const root = container.querySelector('.upgrade-page');
    expect(root).toHaveClass('upgrade-page', 'custom-class');
  });

  describe('breadcrumbs', () => {
    it('renders Settings breadcrumb that navigates to settings page', async () => {
      render(<UpgradePage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Settings' })).toBeInTheDocument();
      });

      // Click Settings breadcrumb navigates to settings page
      screen.getByRole('button', { name: 'Settings' }).click();
      expect(window.location.hash).toBe('#preview/admin/settings');
    });

    it('renders Upgrade as current page breadcrumb', async () => {
      render(<UpgradePage />, { wrapper: TestWrapper });

      await waitFor(() => {
        // The current page item is rendered as Text (not a button) with aria-current="page"
        const breadcrumb = screen.getByText('Upgrade', { selector: '[aria-current="page"]' });
        expect(breadcrumb).toBeInTheDocument();
      });
    });
  });
});

