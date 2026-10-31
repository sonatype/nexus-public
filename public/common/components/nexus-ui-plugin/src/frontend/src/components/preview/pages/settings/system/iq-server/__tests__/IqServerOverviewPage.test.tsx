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
import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

const mockGo = jest.fn();
jest.mock('@uirouter/react', () => ({
  useRouter: () => ({ stateService: { go: mockGo } }),
}));

// The Connect action is gated on the provider-independent ExtJS.usePermission (NEXUS-54212):
// coreui never mounts a <PermissionsProvider>, so the context-based hook returns false for
// everyone. Spy on the real ExtJS singleton and drive the settings:update check per test.
import { ExtJS } from '../../../../../../../interface/ExtJS';
import Permissions from '../../../../../../../constants/Permissions';
const mockCheckPermission = jest.spyOn(ExtJS, 'checkPermission');

import { IqServerOverviewPage } from '../IqServerOverviewPage';
import * as useIqConnectedApiModule from '../useIqConnectedApi';

afterEach(() => {
  cleanup();
  Array.from(document.body.children).forEach(el => el.remove());
});

function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

const disabledIq = {
  enabled: false,
  showLink: false,
  url: '',
  authenticationType: '' as const,
  username: '',
  password: '',
  useTrustStoreForUrl: false,
  timeoutSeconds: null,
  properties: '',
  failOpenModeEnabled: false,
  licensedSolutions: [],
  hasFirewall: false,
};

const enabledIq = { ...disabledIq, enabled: true };

function mockApi(fetchIq: jest.Mock) {
  jest.spyOn(useIqConnectedApiModule, 'useIqConnectedApi').mockReturnValue({
    loading: false,
    error: null,
    setError: jest.fn(),
    fetchIq,
    verifyConnection: jest.fn(),
    fetchDashboardSummary: jest.fn(),
    fetchEvaluationSettings: jest.fn(),
  });
}

describe('IqServerOverviewPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockGo.mockReset();
    // Default: user can edit settings so the pre-existing Connect behavior is exercised.
    mockCheckPermission.mockReturnValue(true);
  });

  it('renders the disconnected card when IQ is disabled', async () => {
    mockApi(jest.fn().mockResolvedValue(disabledIq));
    renderWithTheme(<IqServerOverviewPage />);

    expect(await screen.findByTestId('iq-disconnected-card')).toBeInTheDocument();
    expect(screen.getByText('Connect to IQ Server to get started')).toBeInTheDocument();
    expect(screen.getByTestId('iq-disconnected-connect')).toBeInTheDocument();
  });

  it('redirects to iqConnected when IQ is already enabled', async () => {
    mockApi(jest.fn().mockResolvedValue(enabledIq));
    renderWithTheme(<IqServerOverviewPage />);

    await waitFor(() => {
      expect(mockGo).toHaveBeenCalledWith('preview.admin.iqConnected');
    });
  });

  it('clicking Connect navigates to iqConnection route', async () => {
    mockApi(jest.fn().mockResolvedValue(disabledIq));
    renderWithTheme(<IqServerOverviewPage />);

    fireEvent.click(await screen.findByTestId('iq-disconnected-connect'));
    expect(mockGo).toHaveBeenCalledWith('preview.admin.iqConnection');
  });

  it('shows error state on fetch failure', async () => {
    mockApi(jest.fn().mockRejectedValue(new Error('Network error')));
    renderWithTheme(<IqServerOverviewPage />);

    expect(await screen.findByText('Network error')).toBeInTheDocument();
  });

  it('renders nothing while fetching to avoid double-loading flash', () => {
    // Never resolves — stays in loading state
    mockApi(jest.fn().mockReturnValue(new Promise(() => {})));
    const { container } = renderWithTheme(<IqServerOverviewPage />);

    expect(container.firstChild).toBeEmptyDOMElement();
  });

  // -------- Connect write gating (NEXUS-54212) --------

  describe('connect gating (NEXUS-54212)', () => {
    it('shows the Connect button with settings:update', async () => {
      mockCheckPermission.mockReturnValue(true);
      mockApi(jest.fn().mockResolvedValue(disabledIq));
      renderWithTheme(<IqServerOverviewPage />);

      expect(await screen.findByTestId('iq-disconnected-connect')).toBeInTheDocument();
      expect(screen.getByText('Connect to IQ Server to get started')).toBeInTheDocument();
    });

    it('hides the Connect button and shows a read-only note without settings:update', async () => {
      mockCheckPermission.mockImplementation((p) => p !== Permissions.SETTINGS.UPDATE);
      mockApi(jest.fn().mockResolvedValue(disabledIq));
      renderWithTheme(<IqServerOverviewPage />);

      // The card still renders (read-only), but with no Connect action.
      expect(await screen.findByTestId('iq-disconnected-card')).toBeInTheDocument();
      expect(screen.queryByTestId('iq-disconnected-connect')).not.toBeInTheDocument();
      expect(screen.getByText('IQ Server is not connected')).toBeInTheDocument();
      expect(
        screen.getByText(/You are viewing a read-only version of this page/),
      ).toBeInTheDocument();
    });
  });
});
