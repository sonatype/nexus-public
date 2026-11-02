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
import { FirewallSecuritySectionPage } from '../FirewallSecuritySectionPage';

const mockTier = jest.fn();
const mockVisible = jest.fn();
const mockState = jest.fn();
const mockTabs = jest.fn();

jest.mock('../../../shared/useFirewallSecurityPagesTier', () => ({
  useFirewallSecurityPagesTier: () => mockTier(),
  useFirewallSecurityPagesState: () => mockState(),
}));

jest.mock('../../../shared/useFirewallSecurityPagesVisible', () => ({
  useFirewallSecurityPagesVisible: () => mockVisible(),
}));

jest.mock('../useVisibleFirewallSecurityTabs', () => ({
  ...jest.requireActual('../useVisibleFirewallSecurityTabs'),
  useVisibleFirewallSecurityTabs: (tier: string) => mockTabs(tier),
}));

jest.mock('../../MissingRoutePage', () => ({
  MissingRoutePage: () => <div data-testid="missing-route-page" />,
}));

jest.mock('../banners/NoIqBanner', () => ({ NoIqBanner: () => <div data-testid="banner-no-iq" /> }));
jest.mock('../banners/UpgradeIqNotice', () => ({ UpgradeIqNotice: () => <div data-testid="banner-upgrade-iq" /> }));
jest.mock('../banners/IqConnectionError', () => ({ IqConnectionError: () => <div data-testid="banner-error" /> }));

jest.mock('../firewallSecurityTabs', () => {
  const makeTab = (id: string, label: string) => ({
    id,
    label,
    permission: `nexus:${id}:read`,
    Preview: () => <div data-testid={`preview-${id}`} />,
    Live: () => <div data-testid={`live-${id}`} />,
  });
  return {
    FIREWALL_SECURITY_TABS: [
      makeTab('malware', 'Malware'),
      makeTab('waivers', 'Waivers'),
      makeTab('quarantine', 'Quarantine'),
    ],
  };
});

const permitted = (...ids: string[]) => ids.map((id) => ({ id }));

const renderPage = (tabId: 'malware' | 'waivers' | 'quarantine' | 'policies' = 'waivers') =>
  render(
    <Theme>
      <FirewallSecuritySectionPage tabId={tabId} />
    </Theme>
  );

beforeEach(() => {
  jest.clearAllMocks();
  mockTier.mockReturnValue('preview-pro');
  mockVisible.mockReturnValue(true);
  mockState.mockReturnValue({ minIqVersion: '1.204' });
  mockTabs.mockReturnValue(permitted('malware', 'waivers', 'quarantine'));
});

describe('FirewallSecuritySectionPage', () => {
  it('renders the section heading and only that section content', () => {
    renderPage('waivers');

    expect(screen.getByRole('heading', { name: 'Waivers' })).toBeInTheDocument();
    expect(screen.getByTestId('preview-waivers')).toBeInTheDocument();
    expect(screen.queryByTestId('preview-malware')).not.toBeInTheDocument();
    expect(screen.queryByRole('tab')).not.toBeInTheDocument();
    expect(mockTabs).toHaveBeenCalledWith('preview-pro');
  });

  it('mounts Live content, not Preview, on the live tier', () => {
    mockTier.mockReturnValue('live');
    renderPage('malware');

    expect(screen.getByTestId('live-malware')).toBeInTheDocument();
    expect(screen.queryByTestId('preview-malware')).not.toBeInTheDocument();
  });

  it('shows Preview content with no banner on preview-pro, and never mounts Live content', () => {
    mockTier.mockReturnValue('preview-pro');
    renderPage('quarantine');

    expect(screen.queryByTestId('banner-no-iq')).not.toBeInTheDocument();
    expect(screen.getByTestId('preview-quarantine')).toBeInTheDocument();
    expect(screen.queryByTestId('live-quarantine')).not.toBeInTheDocument();
  });

  it('shows the no-IQ banner with Preview content on preview-no-iq, and never mounts Live content', () => {
    mockTier.mockReturnValue('preview-no-iq');
    renderPage('quarantine');

    expect(screen.getByTestId('banner-no-iq')).toBeInTheDocument();
    expect(screen.getByTestId('preview-quarantine')).toBeInTheDocument();
    expect(screen.queryByTestId('live-quarantine')).not.toBeInTheDocument();
  });

  it.each([
    ['upgrade-iq', 'banner-upgrade-iq'],
    ['error', 'banner-error'],
  ])('shows only the %s notice for a permitted section', (tier, bannerId) => {
    mockTier.mockReturnValue(tier);
    mockTabs.mockReturnValue([{ id: 'malware' }]);
    renderPage('malware');

    expect(screen.getByTestId(bannerId)).toBeInTheDocument();
    expect(screen.queryByTestId('preview-malware')).not.toBeInTheDocument();
    expect(screen.queryByTestId('live-malware')).not.toBeInTheDocument();
  });

  it.each(['upgrade-iq', 'error'])('renders the not-found page in the %s tier with no permitted sections', (tier) => {
    mockTier.mockReturnValue(tier);
    mockTabs.mockReturnValue([]);
    renderPage('malware');

    expect(screen.getByTestId('missing-route-page')).toBeInTheDocument();
  });

  it('renders the not-found page when the Security pages are not visible', () => {
    mockTier.mockReturnValue('hidden');
    mockVisible.mockReturnValue(false);
    renderPage();

    expect(screen.getByTestId('missing-route-page')).toBeInTheDocument();
    expect(screen.queryByTestId('security-page')).not.toBeInTheDocument();
  });

  it('renders the not-found page when the user may not see this section', () => {
    mockTier.mockReturnValue('live');
    mockTabs.mockReturnValue(permitted('malware', 'quarantine'));
    renderPage('waivers');

    expect(screen.getByTestId('missing-route-page')).toBeInTheDocument();
    expect(screen.queryByTestId('live-waivers')).not.toBeInTheDocument();
  });

  it('renders the not-found page for an unknown section id', () => {
    renderPage('policies');
    expect(screen.getByTestId('missing-route-page')).toBeInTheDocument();
  });
});
