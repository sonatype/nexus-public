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

import type { RepoWithProtection } from '../../MalwareRisk/useQuickActionsData';

jest.mock('../../../../../interface/api', () => ({
  restClient: { post: jest.fn() },
  ENDPOINTS: {
    HEALTH_CHECK_ANALYZE: (name: string) => `/v1/repositories/${name}/health-check`,
  },
}));

jest.mock('../../../shared/security/useFirewallEnable', () => ({
  enableFirewallQuarantine: jest.fn(),
}));

jest.mock('../../../shared/security/malwareRemediatorTask', () => ({
  setMalwareRemediatorEnabledForRepository: jest.fn(),
  fetchMalwareRemediatorTasks: jest.fn().mockResolvedValue([]),
}));

jest.mock('../../../shared', () => ({
  useToast: () => ({ success: jest.fn(), error: jest.fn() }),
}));

jest.mock('../../../../../utils/firewallFormats', () => ({
  isFirewallSupportedFormat: () => true,
}));

import { ExtJS } from '../../../../../interface/ExtJS';
import Permissions from '../../../../../constants/Permissions';
// ProtectBulkActionModal reads permissions through the provider-independent ExtJS.usePermission
// (NEXUS-54212); spy on checkPermission so tests keep driving behavior via permission strings.
const mockCheckPermission = jest.spyOn(ExtJS, 'checkPermission');

import ProtectBulkActionModal from '../ProtectBulkActionModal';

const CANDIDATES: RepoWithProtection[] = [
  {
    name: 'maven-central',
    format: 'maven2',
    type: 'proxy',
    url: '',
    rhcEnabled: false,
    protection: 'none',
    taskEnabled: false,
    taskCleanupEnabled: false,
    malwareCount: 0,
  },
  {
    name: 'npm-proxy',
    format: 'npm',
    type: 'proxy',
    url: '',
    rhcEnabled: false,
    protection: 'none',
    taskEnabled: false,
    taskCleanupEnabled: false,
    malwareCount: 0,
  },
];

const renderWithTheme = (ui: React.ReactElement) => render(<Theme>{ui}</Theme>);

describe('ProtectBulkActionModal', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Default: user has all write permissions so pre-existing behavior is exercised.
    mockCheckPermission.mockReturnValue(true);
  });

  it('renders healthcheck title when action is healthcheck', () => {
    renderWithTheme(
      <ProtectBulkActionModal
        open={true}
        onOpenChange={jest.fn()}
        action="healthcheck"
        candidates={CANDIDATES}
        onComplete={jest.fn()}
      />
    );
    expect(screen.getByText(/Enable Health Check/)).toBeInTheDocument();
  });

  it('renders firewall title when action is firewall', () => {
    renderWithTheme(
      <ProtectBulkActionModal
        open={true}
        onOpenChange={jest.fn()}
        action="firewall"
        candidates={CANDIDATES}
        onComplete={jest.fn()}
      />
    );
    expect(screen.getByText(/Enable Firewall/)).toBeInTheDocument();
  });

  it('renders cleanup title when action is cleanup', () => {
    renderWithTheme(
      <ProtectBulkActionModal
        open={true}
        onOpenChange={jest.fn()}
        action="cleanup"
        candidates={CANDIDATES}
        onComplete={jest.fn()}
      />
    );
    expect(screen.getByText(/Enable Auto Remediation/)).toBeInTheDocument();
  });

  it('displays candidate count', () => {
    renderWithTheme(
      <ProtectBulkActionModal
        open={true}
        onOpenChange={jest.fn()}
        action="healthcheck"
        candidates={CANDIDATES}
        onComplete={jest.fn()}
      />
    );
    expect(screen.getByText(/2 repositories/)).toBeInTheDocument();
  });

  it('renders Confirm and Cancel buttons', () => {
    renderWithTheme(
      <ProtectBulkActionModal
        open={true}
        onOpenChange={jest.fn()}
        action="healthcheck"
        candidates={CANDIDATES}
        onComplete={jest.fn()}
      />
    );
    expect(screen.getByRole('button', { name: 'Confirm' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
  });

  it('disables Confirm when candidates is empty', () => {
    renderWithTheme(
      <ProtectBulkActionModal
        open={true}
        onOpenChange={jest.fn()}
        action="healthcheck"
        candidates={[]}
        onComplete={jest.fn()}
      />
    );
    expect(screen.getByRole('button', { name: 'Confirm' })).toBeDisabled();
  });

  describe('confirm gating (NEXUS-54212)', () => {
    const renderModal = (action: 'healthcheck' | 'firewall' | 'cleanup') =>
      renderWithTheme(
        <ProtectBulkActionModal
          open={true}
          onOpenChange={jest.fn()}
          action={action}
          candidates={CANDIDATES}
          onComplete={jest.fn()}
        />
      );

    it('firewall: shows Confirm with repository-admin edit', () => {
      mockCheckPermission.mockImplementation((p: string) => p === Permissions.REPOSITORY_ADMIN.EDIT);
      renderModal('firewall');
      expect(screen.getByRole('button', { name: 'Confirm' })).toBeInTheDocument();
    });

    it('firewall: hides Confirm without repository-admin edit', () => {
      mockCheckPermission.mockReturnValue(false);
      renderModal('firewall');
      expect(screen.queryByRole('button', { name: 'Confirm' })).not.toBeInTheDocument();
    });

    it('cleanup: hides Confirm without tasks:create', () => {
      mockCheckPermission.mockImplementation((p: string) => p === Permissions.REPOSITORY_ADMIN.EDIT);
      renderModal('cleanup');
      expect(screen.queryByRole('button', { name: 'Confirm' })).not.toBeInTheDocument();
    });

    it('healthcheck: hides Confirm without healthcheck:update', () => {
      mockCheckPermission.mockImplementation((p: string) => p === Permissions.REPOSITORY_ADMIN.EDIT);
      renderModal('healthcheck');
      expect(screen.queryByRole('button', { name: 'Confirm' })).not.toBeInTheDocument();
    });

    it('Cancel stays visible even without any write permission', () => {
      mockCheckPermission.mockReturnValue(false);
      renderModal('firewall');
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
    });

    it('recomputes Confirm when the action changes on a reused instance', () => {
      // The modal is a single instance reused across open/close cycles in ProtectQuickConfig, so a
      // permission value frozen at first mount would strand a permitted action. This user holds
      // repository-admin:edit but not healthcheck:update.
      mockCheckPermission.mockImplementation((p: string) => p === Permissions.REPOSITORY_ADMIN.EDIT);
      const { rerender } = renderWithTheme(
        <ProtectBulkActionModal
          open={true}
          onOpenChange={jest.fn()}
          action="healthcheck"
          candidates={CANDIDATES}
          onComplete={jest.fn()}
        />
      );
      // healthcheck needs healthcheck:update, which this user lacks → no Confirm.
      expect(screen.queryByRole('button', { name: 'Confirm' })).not.toBeInTheDocument();

      // Re-open for firewall on the SAME instance (no unmount). canConfirm must recompute for
      // repository-admin:edit, which the user holds → Confirm appears.
      rerender(
        <Theme>
          <ProtectBulkActionModal
            open={true}
            onOpenChange={jest.fn()}
            action="firewall"
            candidates={CANDIDATES}
            onComplete={jest.fn()}
          />
        </Theme>
      );
      expect(screen.getByRole('button', { name: 'Confirm' })).toBeInTheDocument();
    });
  });
});
