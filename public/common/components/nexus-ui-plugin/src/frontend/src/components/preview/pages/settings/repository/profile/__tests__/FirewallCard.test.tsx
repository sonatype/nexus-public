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
import { FirewallCard } from '../FirewallCard';
import Permissions from '../../../../../../../constants/Permissions';

import { ExtJS } from '../../../../../../../interface/ExtJS';
// FirewallCard reads permissions through the provider-independent ExtJS.usePermission
// (NEXUS-54212); spy on checkPermission so tests keep driving behavior via permission strings.
const mockCheckPermission = jest.spyOn(ExtJS, 'checkPermission');

jest.mock('../../../../../shared/security/useFirewallEnable', () => ({
  useFirewallEnable: () => ({
    enableAudit: jest.fn(),
    enableQuarantine: jest.fn(),
    disable: jest.fn(),
    loading: false,
  }),
}));
jest.mock('../../../../../shared/security/malwareRemediatorTask', () => ({
  setMalwareRemediatorEnabledForRepository: jest.fn(),
}));
jest.mock('../../../../../shared/Toast', () => ({
  useToast: () => ({ success: jest.fn(), warning: jest.fn(), error: jest.fn(), info: jest.fn() }),
}));

const props = {
  repositoryName: 'maven-releases',
  firewall: { reportUrl: 'http://x', mode: 'unprotected' } as any,
  malwareCleanupSummary: null,
  iqCapabilities: { enabled: true, connected: true, hasFirewall: true } as any,
  isSupported: true,
  refresh: jest.fn(),
};

function renderCard(perms: string[]) {
  mockCheckPermission.mockImplementation((p: string) => perms.includes(p));
  return render(
    <Theme>
      <FirewallCard {...props} />
    </Theme>
  );
}

describe('FirewallCard permission gating (NEXUS-54212)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('shows protection-level controls with repository-admin edit', () => {
    renderCard([Permissions.REPOSITORY_ADMIN.EDIT]);
    expect(screen.getByTestId('firewall-protection-level')).toBeInTheDocument();
  });

  it('hides protection-level controls without repository-admin edit', () => {
    renderCard([Permissions.TASKS.CREATE]);
    expect(screen.queryByTestId('firewall-protection-level')).not.toBeInTheDocument();
  });

  it('shows malware-task controls with tasks:create', () => {
    renderCard([Permissions.TASKS.CREATE]);
    expect(screen.getByTestId('firewall-malware-task')).toBeInTheDocument();
  });

  it('hides malware-task controls without tasks:create', () => {
    renderCard([Permissions.REPOSITORY_ADMIN.EDIT]);
    expect(screen.queryByTestId('firewall-malware-task')).not.toBeInTheDocument();
  });

  it('hides both control groups for a read-only user', () => {
    renderCard([]);
    expect(screen.queryByTestId('firewall-protection-level')).not.toBeInTheDocument();
    expect(screen.queryByTestId('firewall-malware-task')).not.toBeInTheDocument();
  });
});
