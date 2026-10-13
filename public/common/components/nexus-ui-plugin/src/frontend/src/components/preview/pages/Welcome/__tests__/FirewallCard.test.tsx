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
import { FirewallCardDisplay } from '../FirewallCard';
import type { MalwareStatus } from '../useMalwareStatus';

// FirewallCardDisplay is the exported, hook-free presentational component (NEXUS-54874): it takes a
// MalwareStatus and the includedInLicense flag as props, so the "included in your license" rendering
// path can be asserted in isolation without mocking ExtJS state or the IQ-connection hook.

function notConfiguredStatus(overrides: Partial<MalwareStatus> = {}): MalwareStatus {
  // isConfigured = available && hasFirewall; keep hasFirewall false so the not-configured branch renders
  // (the branch that switches on includedInLicense).
  return {
    available: false,
    state: null,
    malwareCount: 0,
    statusText: '',
    color: 'gray',
    ctaText: 'Learn more',
    hasFirewall: false,
    firewallTier: 'none',
    proxyCounts: { total: 0, protected: 0, inAudit: 0, unprotected: 0 },
    iqServerUrl: null,
    ...overrides,
  };
}

function renderCard(status: MalwareStatus, includedInLicense: boolean) {
  return render(
    <Theme>
      <FirewallCardDisplay status={status} includedInLicense={includedInLicense} />
    </Theme>
  );
}

describe('FirewallCardDisplay - included in license', () => {
  it('shows the Enterprise "included in your license" headline for the enterprise tier', () => {
    renderCard(notConfiguredStatus({ firewallTier: 'enterprise' }), true);

    expect(screen.getByText('Firewall Enterprise included in your license')).toBeInTheDocument();
    expect(screen.queryByText(/^Upgrade to/)).not.toBeInTheDocument();
  });

  it('shows the generic "included in your license" headline for non-enterprise tiers', () => {
    renderCard(notConfiguredStatus({ firewallTier: 'pro' }), true);

    expect(screen.getByText('Repository Firewall included in your license')).toBeInTheDocument();
  });

  it('makes "Configure Firewall" the primary (solid) CTA when included in the licence', () => {
    renderCard(notConfiguredStatus({ firewallTier: 'enterprise' }), true);

    const configure = screen.getByRole('button', { name: 'Configure Firewall' });
    // radix Themes renders the variant as the rt-variant-* class; solid is the prominent primary CTA.
    expect(configure).toHaveClass('rt-variant-solid');
  });

  it('keeps the upgrade pitch with a non-primary (soft) CTA when not included in the licence', () => {
    renderCard(notConfiguredStatus({ firewallTier: 'none' }), false);

    expect(screen.getByText('Upgrade to Repository Firewall')).toBeInTheDocument();
    const configure = screen.getByRole('button', { name: 'Configure' });
    expect(configure).toHaveClass('rt-variant-soft');
  });
});
