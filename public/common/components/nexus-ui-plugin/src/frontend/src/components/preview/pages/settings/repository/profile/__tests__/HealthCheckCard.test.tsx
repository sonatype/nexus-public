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
import { HealthCheckCard } from '../HealthCheckCard';

import { ExtJS } from '../../../../../../../interface/ExtJS';
// HealthCheckCard reads permissions through the provider-independent ExtJS.usePermission
// (NEXUS-54212); spy on checkPermission so tests keep driving behavior via its return value.
const mockCheckPermission = jest.spyOn(ExtJS, 'checkPermission');

const baseProps = {
  repositoryName: 'maven-releases',
  healthCheck: { enabled: false, detailUrl: null } as any,
  // Instance-enabled capability so the "Enable Health Check" repo button renders.
  capabilities: [{ type: 'healthcheck', enabled: true }] as any,
  onToggleRepo: jest.fn(),
  onToggleInstance: jest.fn(),
  isSupported: true,
};

function renderCard(canUpdate: boolean, extra: object = {}) {
  mockCheckPermission.mockReturnValue(canUpdate);
  return render(
    <Theme>
      <HealthCheckCard {...baseProps} {...extra} />
    </Theme>
  );
}

describe('HealthCheckCard permission gating (NEXUS-54212)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('shows Enable Health Check with healthcheck:update', () => {
    renderCard(true);
    expect(screen.getByTestId('health-check-enable-repo')).toBeInTheDocument();
  });

  it('hides Enable Health Check without healthcheck:update', () => {
    renderCard(false);
    expect(screen.queryByTestId('health-check-enable-repo')).not.toBeInTheDocument();
  });

  it('shows the On/Off Switch with healthcheck:update when enabled', () => {
    renderCard(true, { healthCheck: { enabled: true, detailUrl: null } });
    expect(screen.getByRole('switch')).toBeInTheDocument();
  });

  it('hides the On/Off Switch without healthcheck:update when enabled', () => {
    renderCard(false, { healthCheck: { enabled: true, detailUrl: null } });
    expect(screen.queryByRole('switch')).not.toBeInTheDocument();
  });

  it('hides the Enable Instance-wide button without healthcheck:update', () => {
    renderCard(false, { capabilities: [{ type: 'healthcheck', enabled: false }] });
    expect(screen.queryByRole('button', { name: /enable instance-wide/i })).not.toBeInTheDocument();
  });

  it('shows the Enable Instance-wide button with healthcheck:update', () => {
    renderCard(true, { capabilities: [{ type: 'healthcheck', enabled: false }] });
    expect(screen.getByRole('button', { name: /enable instance-wide/i })).toBeInTheDocument();
  });
});
