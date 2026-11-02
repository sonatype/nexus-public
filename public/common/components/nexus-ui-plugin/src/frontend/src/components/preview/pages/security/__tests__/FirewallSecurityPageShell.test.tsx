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

import React, { useEffect } from 'react';
import { render, screen } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { restClient } from '../../../../../interface/api';
import { FirewallSecurityPageShell } from '../FirewallSecurityPageShell';
import { FirewallSecurityTabPanel } from '../FirewallSecurityTabPanel';
import type { FirewallSecurityPagesTier } from '../../../shared/useFirewallSecurityPagesTier';
import type { FirewallSecurityTab } from '../firewallSecurityTabs';
import { useProUpsell } from '../tabs/ProUpsellContext';

jest.mock('../../../../../interface/api', () => ({
  restClient: { get: jest.fn(), post: jest.fn() },
  ENDPOINTS: {},
}));

jest.mock('../banners/ProPreviewBanner', () => ({ ProPreviewBanner: () => <div data-testid="banner-pro-preview" /> }));
jest.mock('../banners/NoIqBanner', () => ({ NoIqBanner: () => <div data-testid="banner-no-iq" /> }));
jest.mock('../banners/UpgradeIqNotice', () => ({
  UpgradeIqNotice: ({ minIqVersion }: { minIqVersion?: string }) => (
    <div data-testid="banner-upgrade-iq">{minIqVersion}</div>
  ),
}));
jest.mock('../banners/IqConnectionError', () => ({ IqConnectionError: () => <div data-testid="banner-error" /> }));

const Preview = () => <div data-testid="tab-preview" />;
const Live = () => {
  // A real Live view fetches on mount; the spy proves it never mounts outside the live tier.
  useEffect(() => {
    void restClient.get('/live-data');
  }, []);
  return <div data-testid="tab-live" />;
};

const tab: FirewallSecurityTab = {
  id: 'waivers',
  label: 'Waivers',
  permission: 'nexus:firewall-waivers:read',
  Preview,
  Live,
};

const renderShell = (tier: FirewallSecurityPagesTier) =>
  render(
    <Theme>
      <FirewallSecurityPageShell tier={tier} minIqVersion="1.204">
        <FirewallSecurityTabPanel tab={tab} tier={tier} />
      </FirewallSecurityPageShell>
    </Theme>
  );

beforeEach(() => jest.clearAllMocks());

describe('FirewallSecurityPageShell', () => {
  it('preview-pro: Pro preview banner above the Preview content', () => {
    renderShell('preview-pro');
    expect(screen.getByTestId('banner-pro-preview')).toBeInTheDocument();
    expect(screen.queryByTestId('banner-no-iq')).not.toBeInTheDocument();
    expect(screen.getByTestId('tab-preview')).toBeInTheDocument();
    expect(screen.queryByTestId('tab-live')).not.toBeInTheDocument();
    expect(restClient.get).not.toHaveBeenCalled();
  });

  it('preview-no-iq: no-IQ banner above the Preview content', () => {
    renderShell('preview-no-iq');
    expect(screen.getByTestId('banner-no-iq')).toBeInTheDocument();
    expect(screen.getByTestId('tab-preview')).toBeInTheDocument();
    expect(screen.queryByTestId('tab-live')).not.toBeInTheDocument();
    expect(restClient.get).not.toHaveBeenCalled();
  });

  it('upgrade-iq: only the upgrade notice, children are not mounted', () => {
    renderShell('upgrade-iq');
    expect(screen.getByTestId('banner-upgrade-iq')).toHaveTextContent('1.204');
    expect(screen.queryByTestId('tab-preview')).not.toBeInTheDocument();
    expect(screen.queryByTestId('tab-live')).not.toBeInTheDocument();
    expect(restClient.get).not.toHaveBeenCalled();
  });

  it('error: only the connection error, children are not mounted', () => {
    renderShell('error');
    expect(screen.getByTestId('banner-error')).toBeInTheDocument();
    expect(screen.queryByTestId('tab-preview')).not.toBeInTheDocument();
    expect(screen.queryByTestId('tab-live')).not.toBeInTheDocument();
    expect(restClient.get).not.toHaveBeenCalled();
  });

  it('live: no banner, the Live content mounts', () => {
    renderShell('live');
    expect(screen.queryByTestId(/^banner-/)).not.toBeInTheDocument();
    expect(screen.getByTestId('tab-live')).toBeInTheDocument();
    expect(screen.queryByTestId('tab-preview')).not.toBeInTheDocument();
    expect(restClient.get).toHaveBeenCalledWith('/live-data');
  });

  it('hidden: renders nothing', () => {
    renderShell('hidden');
    expect(screen.queryByTestId(/^banner-/)).not.toBeInTheDocument();
    expect(screen.queryByTestId('tab-preview')).not.toBeInTheDocument();
    expect(screen.queryByTestId('tab-live')).not.toBeInTheDocument();
  });
});

describe('FirewallSecurityTabPanel upsell context', () => {
  const UpsellProbe = () => <div data-testid="upsell">{String(useProUpsell())}</div>;
  const probeTab: FirewallSecurityTab = { ...tab, Preview: UpsellProbe };

  it.each([
    ['preview-pro', 'true'],
    ['preview-no-iq', 'false'],
  ] as const)('%s: upgrade prompts allowed = %s', (tier, expected) => {
    render(
      <Theme>
        <FirewallSecurityTabPanel tab={probeTab} tier={tier} />
      </Theme>
    );
    expect(screen.getByTestId('upsell')).toHaveTextContent(expected);
  });
});
