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
import RepositoryProfilePage from '../RepositoryProfilePage';

// Permission gating under test (NEXUS-54212).
import Permissions from '../../../../../../../constants/Permissions';
import { ExtJS } from '../../../../../../../interface/ExtJS';
// RepositoryProfilePage reads permissions through the provider-independent ExtJS.usePermission
// (NEXUS-54212); spy on checkPermission so tests keep driving behavior via permission strings.
const mockCheckPermission = jest.spyOn(ExtJS, 'checkPermission');

// Router
jest.mock('@uirouter/react', () => ({
  useRouter: () => ({ stateService: { go: jest.fn() } }),
  useCurrentStateAndParams: () => ({ params: { tab: 'repository' } }),
}));

// Machine hook — populated repository (hosted => no proxy-only cards/actions).
jest.mock('../useRepositoryProfileMachine', () => ({
  useRepositoryProfileMachine: () => ({
    repository: {
      name: 'maven-releases',
      format: 'maven2',
      type: 'hosted',
      online: true,
      url: 'http://localhost/repository/maven-releases/',
      status: { online: true },
    },
    blobStore: undefined,
    cleanupPolicies: [],
    routingRule: undefined,
    healthCheck: undefined,
    firewall: undefined,
    malwareCleanupSummary: undefined,
    metrics: undefined,
    privileges: [],
    roles: [],
    users: [],
    anonymousAccess: undefined,
    tasks: [],
    capabilities: [],
    iqCapabilities: undefined,
    httpSettings: undefined,
    loading: false,
    securityLoading: false,
    systemLoading: false,
    error: null,
    actionError: null,
    refresh: jest.fn(),
    retry: jest.fn(),
    handleInvalidateCache: jest.fn(),
    handleRebuildIndex: jest.fn(),
    handleToggleOnline: jest.fn(),
    handleToggleHealthCheck: jest.fn(),
    handleToggleInstanceHealthCheck: jest.fn(),
    confirmAction: jest.fn(),
    cancelAction: jest.fn(),
    isConfirming: false,
    isExecuting: false,
    pendingAction: null,
    dialogTitle: '',
    dialogMessage: '',
    dialogConfirmLabel: '',
    dialogVariant: undefined,
  }),
}));

jest.mock('../../repositories/useRepositoriesApi', () => ({
  useRepositoriesApi: () => ({ deleteRepository: jest.fn() }),
}));

jest.mock('../../../../../shared/security/firewallTier', () => ({
  useFirewallTier: () => undefined,
}));

jest.mock('../../../../../shared/Toast', () => ({
  useToast: () => ({ success: jest.fn(), error: jest.fn(), warning: jest.fn(), info: jest.fn() }),
}));

// Stub child components/tabs so the test isolates the header action buttons.
jest.mock('../../../../../shared', () => ({
  DeleteConfirmationModal: () => null,
}));
jest.mock('../../../../../shared/ConfirmDialog', () => ({ ConfirmDialog: () => null }));
jest.mock('../../../../search/details/Breadcrumbs', () => ({ Breadcrumbs: () => null }));
jest.mock('../../repositories/RepositoryStructureTree', () => ({ RepositoryStructureTree: () => null }));
jest.mock('../../repositories/RepositoryGroupUsageTab', () => ({ RepositoryGroupUsageTab: () => null }));
jest.mock('../tabs/RepositoryTab', () => ({ RepositoryTab: () => null }));
jest.mock('../tabs/AccessSecurityTab', () => ({ AccessSecurityTab: () => null }));
jest.mock('../tabs/SystemTab', () => ({ SystemTab: () => null }));
jest.mock('../tabs/UsageTab', () => ({ UsageTab: () => null }));
jest.mock('../tabs/RepositoryAuditTab', () => ({ RepositoryAuditTab: () => null }));
jest.mock('../tabs/InstanceConfigTab', () => ({ InstanceConfigTab: () => null }));
jest.mock('../HealthCheckCard', () => ({ HealthCheckCard: () => null }));
jest.mock('../FirewallCard', () => ({ FirewallCard: () => null }));

function renderPage(perms: string[]) {
  mockCheckPermission.mockImplementation((p: string) => perms.includes(p));
  return render(
    <Theme>
      <RepositoryProfilePage repositoryName="maven-releases" />
    </Theme>
  );
}

describe('RepositoryProfilePage permission gating (NEXUS-54212)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('shows Delete + edit actions with full repository-admin', () => {
    renderPage([Permissions.REPOSITORY_ADMIN.EDIT, Permissions.REPOSITORY_ADMIN.DELETE]);
    expect(screen.getByRole('button', { name: /delete/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /rebuild index/i })).toBeInTheDocument();
  });

  it('disables Delete when lacking repository-admin delete', () => {
    renderPage([Permissions.REPOSITORY_ADMIN.EDIT]);
    // Large delete button is shown but disabled (NEXUS-54212), not hidden.
    expect(screen.getByRole('button', { name: /delete/i })).toBeDisabled();
    // Edit actions remain visible.
    expect(screen.getByRole('button', { name: /rebuild index/i })).toBeInTheDocument();
  });

  it('hides edit actions when lacking repository-admin edit', () => {
    renderPage([Permissions.REPOSITORY_ADMIN.DELETE]);
    expect(screen.queryByRole('button', { name: /rebuild index/i })).not.toBeInTheDocument();
    // Delete remains visible.
    expect(screen.getByRole('button', { name: /delete/i })).toBeInTheDocument();
  });

  it('disables Delete and hides edit actions for a read-only user', () => {
    renderPage([]);
    // Large delete button is shown but disabled (NEXUS-54212); edit actions stay hidden.
    expect(screen.getByRole('button', { name: /delete/i })).toBeDisabled();
    expect(screen.queryByRole('button', { name: /rebuild index/i })).not.toBeInTheDocument();
  });
});
