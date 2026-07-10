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

import { LicenseExpiryAlert } from '../LicenseExpiryAlert';

// Mock ExtJS
jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    useState: jest.fn(),
    useUser: jest.fn(),
    state: jest.fn(),
  },
}));

const { ExtJS } = require('../../../../../../../interface/ExtJS');

// Wrapper for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

describe('LicenseExpiryAlert', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  function setupState(warningState: { enabled: boolean; message: string } | null = null, isAdmin = true) {
    ExtJS.useUser.mockReturnValue({ administrator: isAdmin });
    ExtJS.useState.mockImplementation((fn: () => unknown) =>
      typeof fn === 'function' ? fn() : fn
    );
    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue(warningState),
    });
  }

  it('renders when enabled is true and user is admin', () => {
    setupState({ enabled: true, message: 'Your license expires in 7 days' });

    render(<LicenseExpiryAlert />, { wrapper: TestWrapper });

    expect(screen.getByText('Your license expires in 7 days')).toBeInTheDocument();
  });

  it('hides when enabled is false', () => {
    setupState({ enabled: false, message: 'License is healthy' });

    render(<LicenseExpiryAlert />, { wrapper: TestWrapper });

    expect(screen.queryByText('License is healthy')).not.toBeInTheDocument();
  });

  it('hides for non-admin users', () => {
    setupState({ enabled: true, message: 'Your license expires in 7 days' }, false);

    render(<LicenseExpiryAlert />, { wrapper: TestWrapper });

    expect(screen.queryByText('Your license expires in 7 days')).not.toBeInTheDocument();
  });

  it('displays custom message from state', () => {
    setupState({ enabled: true, message: 'Custom expiry warning message' });

    render(<LicenseExpiryAlert />, { wrapper: TestWrapper });

    expect(screen.getByText('Custom expiry warning message')).toBeInTheDocument();
  });

  it('displays default message when message is empty', () => {
    setupState({ enabled: true, message: '' });

    render(<LicenseExpiryAlert />, { wrapper: TestWrapper });

    expect(screen.getByText(/Your license will expire soon/)).toBeInTheDocument();
  });

  it('handles missing state gracefully', () => {
    setupState(null);

    const { container } = render(<LicenseExpiryAlert />, { wrapper: TestWrapper });

    // The component returns null, but Theme wrapper still renders
    // Check that the alert content is not present
    expect(container.querySelector('.license-expiry-alert')).not.toBeInTheDocument();
  });

  it('handles undefined state gracefully', () => {
    ExtJS.useUser.mockReturnValue({ administrator: true });
    ExtJS.useState.mockReturnValue(undefined);
    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue(undefined),
    });

    const { container } = render(<LicenseExpiryAlert />, { wrapper: TestWrapper });

    expect(container.querySelector('.license-expiry-alert')).not.toBeInTheDocument();
  });

  it('does not crash and renders nothing when useUser returns null', () => {
    ExtJS.useUser.mockReturnValue(null);
    ExtJS.useState.mockImplementation((fn: () => unknown) =>
      typeof fn === 'function' ? fn() : fn
    );
    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue({ enabled: true, message: 'Expires soon' }),
    });

    const { container } = render(<LicenseExpiryAlert />, { wrapper: TestWrapper });

    // null user means no administrator access — alert must not render
    expect(container.querySelector('.license-expiry-alert')).not.toBeInTheDocument();
    expect(screen.queryByText('Expires soon')).not.toBeInTheDocument();
  });
});
