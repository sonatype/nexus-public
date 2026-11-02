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
import { render, screen, within } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { SecurityNavSection } from '../SecurityNavSection';

const mockTier = jest.fn();
const mockTabs = jest.fn();

jest.mock('../../../shared/useFirewallSecurityPagesTier', () => ({
  useFirewallSecurityPagesTier: () => mockTier(),
}));

jest.mock('../useVisibleFirewallSecurityTabs', () => ({
  ...jest.requireActual('../useVisibleFirewallSecurityTabs'),
  useVisibleFirewallSecurityTabs: (tier: string) => mockTabs(tier),
}));

jest.mock('../../../shared/Navigation/NavItemBox', () => ({
  NavItemBox: ({
    name,
    text,
    badge,
    badgeLabel,
    icon,
    analyticsId,
    isCollapsed,
  }: {
    name: string;
    text: string;
    badge?: number | null;
    badgeLabel?: (count: number) => string;
    icon?: { displayName?: string };
    analyticsId?: string;
    isCollapsed?: boolean;
  }) => (
    <div data-testid={`nav-item-${name}`} data-analytics-id={analyticsId} data-collapsed={String(!!isCollapsed)}
      data-icon={icon?.displayName}
    >
      {text}
      {badge != null && (
        <span data-testid={`nav-badge-${name}`} aria-label={badgeLabel?.(badge)}>
          {badge}
        </span>
      )}
    </div>
  ),
}));

const tab = (id: string) => ({ id, label: id.charAt(0).toUpperCase() + id.slice(1) });
const ALL = ['quarantine', 'waivers', 'malware', 'evaluations', 'policies'].map(tab);

const renderSection = (props: { isCollapsed?: boolean; badgeCount?: number | null } = {}) =>
  render(
    <Theme>
      <SecurityNavSection {...props} />
    </Theme>
  );

beforeEach(() => {
  jest.clearAllMocks();
  mockTier.mockReturnValue('live');
  mockTabs.mockReturnValue(ALL);
});

describe('SecurityNavSection', () => {
  it('renders the Security heading, the five links in order and a trailing separator', () => {
    renderSection();

    expect(screen.getByTestId('nav-group-security-separator')).toBeInTheDocument();
    expect(screen.getByText('Security')).toBeInTheDocument();
    const group = screen.getByRole('group', { name: 'Security' });
    expect(
      within(group)
        .getAllByTestId(/^nav-item-/)
        .map((el) => el.getAttribute('data-testid'))
    ).toEqual([
      'nav-item-preview.security.quarantine',
      'nav-item-preview.security.waivers',
      'nav-item-preview.security.malware',
      'nav-item-preview.security.evaluations',
      'nav-item-preview.security.policies',
    ]);
    expect(
      within(group)
        .getAllByTestId(/^nav-item-/)
        .map((el) => el.textContent?.replace(/\d+$/, ''))
    ).toEqual(['Quarantine', 'Waivers', 'Malware', 'Evaluations', 'Policy']);
    expect(group.nextElementSibling).toBe(screen.getByTestId('nav-group-security-separator'));
    expect(group.previousElementSibling).toBeNull();
  });

  it('uses the mockup icons', () => {
    renderSection();
    const icon = (id: string) => screen.getByTestId(`nav-item-preview.security.${id}`).getAttribute('data-icon');
    expect(icon('quarantine')).toBe('Shield');
    expect(icon('waivers')).toBe('ClipboardCheck');
    expect(icon('malware')).toBe('Siren');
    expect(icon('evaluations')).toBe('ClipboardCheck');
    expect(icon('policies')).toBe('Scale');
  });

  it('does not make the label a link', () => {
    renderSection();
    expect(screen.queryByRole('link', { name: 'Security' })).not.toBeInTheDocument();
  });

  it('tags each link for usage analytics', () => {
    renderSection();
    expect(screen.getByTestId('nav-item-preview.security.malware')).toHaveAttribute(
      'data-analytics-id',
      'nxrm-security-malware-nav'
    );
    expect(screen.getByTestId('nav-item-preview.security.evaluations')).toHaveAttribute(
      'data-analytics-id',
      'nxrm-security-evaluations-nav'
    );
  });

  it('shows only the links the user may see', () => {
    mockTabs.mockReturnValue([tab('waivers'), tab('quarantine')]);
    renderSection();

    expect(screen.getByTestId('nav-item-preview.security.waivers')).toBeInTheDocument();
    expect(screen.getByTestId('nav-item-preview.security.quarantine')).toBeInTheDocument();
    expect(screen.queryByTestId('nav-item-preview.security.malware')).not.toBeInTheDocument();
    expect(screen.queryByTestId('nav-item-preview.security.policies')).not.toBeInTheDocument();
    expect(mockTabs).toHaveBeenCalledWith('live');
  });

  it('renders nothing, separator and label included, when no link is visible', () => {
    mockTabs.mockReturnValue([]);
    renderSection({ isCollapsed: false });
    expect(screen.queryByTestId('nav-group-security-separator')).not.toBeInTheDocument();
    expect(screen.queryByText('Security')).not.toBeInTheDocument();
    expect(screen.queryByRole('group')).not.toBeInTheDocument();
  });

  it.each(['upgrade-iq', 'error'])('shows only the permitted links in the %s tier', (tier) => {
    mockTier.mockReturnValue(tier);
    mockTabs.mockReturnValue([]);
    renderSection();
    expect(screen.queryAllByTestId(/^nav-item-/)).toHaveLength(0);
    expect(screen.queryByTestId('nav-group-security-separator')).not.toBeInTheDocument();
  });

  it('hides the label but keeps the separator, the links and the group name when collapsed', () => {
    renderSection({ isCollapsed: true });

    expect(screen.queryByText('Security')).not.toBeInTheDocument();
    expect(screen.getByTestId('nav-group-security-separator')).toBeInTheDocument();
    const group = screen.getByRole('group', { name: 'Security' });
    expect(within(group).getAllByTestId(/^nav-item-/)).toHaveLength(5);
    expect(screen.getByTestId('nav-item-preview.security.malware')).toHaveAttribute('data-collapsed', 'true');
  });

  it('puts the quarantine count on the Quarantine link only', () => {
    renderSection({ badgeCount: 6 });

    expect(screen.getByTestId('nav-badge-preview.security.quarantine')).toHaveTextContent('6');
    expect(screen.getByLabelText('6 quarantined components')).toBeInTheDocument();
    expect(screen.queryByTestId('nav-badge-preview.security.malware')).not.toBeInTheDocument();
    expect(screen.queryByTestId('nav-badge-preview.security.waivers')).not.toBeInTheDocument();
  });

  it('passes no badge when the count is null', () => {
    renderSection({ badgeCount: null });
    expect(screen.queryByTestId('nav-badge-preview.security.quarantine')).not.toBeInTheDocument();
  });
});
