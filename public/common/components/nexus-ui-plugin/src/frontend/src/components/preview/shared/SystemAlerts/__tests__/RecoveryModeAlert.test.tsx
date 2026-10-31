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
import '@testing-library/jest-dom';
import { Theme } from '@radix-ui/themes';

import { RecoveryModeAlert } from '../RecoveryModeAlert';
import { ExtJS } from '../../../../../interface/ExtJS';

// Mutable state the mocks read, so each test can vary admin, recovery flag,
// and the active UI-Router state name.
const mockState = {
  administrator: true,
  recoveryModeEnabled: true,
  routeName: 'preview.browse.welcome',
};

jest.mock('../../../../../interface/ExtJS', () => ({
  ExtJS: {
    useUser: jest.fn(() => ({ administrator: mockState.administrator })),
    state: jest.fn(() => ({
      getValue: (key: string) =>
        key === 'recovery.mode.enabled' ? mockState.recoveryModeEnabled : undefined,
    })),
    useState: jest.fn((init: () => unknown) => (typeof init === 'function' ? init() : init)),
  },
}));

const mockGo = jest.fn();
jest.mock('@uirouter/react', () => ({
  useCurrentStateAndParams: jest.fn(() => ({ state: { name: mockState.routeName }, params: {} })),
  useRouter: () => ({ stateService: { go: mockGo } }),
}));

const renderInTheme = (ui: React.ReactElement) => render(<Theme>{ui}</Theme>);

beforeEach(() => {
  mockGo.mockClear();
  mockState.administrator = true;
  mockState.recoveryModeEnabled = true;
  // Default to a non-recovery-mode route so the CTA is shown.
  mockState.routeName = 'preview.browse.welcome';
});

describe('RecoveryModeAlert', () => {
  it('renders the banner for an admin when recovery mode is enabled', () => {
    renderInTheme(<RecoveryModeAlert />);
    expect(screen.getByText('Recovery Mode Enabled')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'View Details' })).toBeInTheDocument();
  });

  it('renders nothing when recovery mode is disabled', () => {
    mockState.recoveryModeEnabled = false;
    renderInTheme(<RecoveryModeAlert />);
    expect(screen.queryByText('Recovery Mode Enabled')).not.toBeInTheDocument();
    expect(screen.queryByTestId('nxrm-system-alert')).not.toBeInTheDocument();
  });

  it('renders nothing for non-admin users even when recovery mode is enabled', () => {
    mockState.administrator = false;
    renderInTheme(<RecoveryModeAlert />);
    expect(screen.queryByText('Recovery Mode Enabled')).not.toBeInTheDocument();
    expect(screen.queryByTestId('nxrm-system-alert')).not.toBeInTheDocument();
  });

  it('View Details navigates to the preview recovery mode route', () => {
    renderInTheme(<RecoveryModeAlert />);
    fireEvent.click(screen.getByRole('button', { name: 'View Details' }));
    expect(mockGo).toHaveBeenCalledWith('preview.admin.support.recoverymode');
  });

  it('is not dismissable (no dismiss button)', () => {
    renderInTheme(<RecoveryModeAlert />);
    expect(screen.queryByRole('button', { name: 'Dismiss alert' })).not.toBeInTheDocument();
  });

  it('hides the View Details CTA when already on the Recovery Mode page', () => {
    mockState.routeName = 'preview.admin.support.recoverymode';
    renderInTheme(<RecoveryModeAlert />);
    // Banner still shows...
    expect(screen.getByText('Recovery Mode Enabled')).toBeInTheDocument();
    // ...but without the CTA.
    expect(screen.queryByRole('button', { name: 'View Details' })).not.toBeInTheDocument();
  });

  it('shows the View Details CTA on other pages', () => {
    mockState.routeName = 'preview.browse.welcome';
    renderInTheme(<RecoveryModeAlert />);
    expect(screen.getByRole('button', { name: 'View Details' })).toBeInTheDocument();
  });

  it('renders nothing when ExtJS.useUser is unavailable (no user resolved)', () => {
    const original = ExtJS.useUser;
    // Simulate a runtime where the hook is absent -> user is undefined -> not admin.
    (ExtJS as unknown as { useUser?: unknown }).useUser = undefined;
    try {
      renderInTheme(<RecoveryModeAlert />);
      expect(screen.queryByText('Recovery Mode Enabled')).not.toBeInTheDocument();
    } finally {
      (ExtJS as unknown as { useUser?: unknown }).useUser = original;
    }
  });
});
