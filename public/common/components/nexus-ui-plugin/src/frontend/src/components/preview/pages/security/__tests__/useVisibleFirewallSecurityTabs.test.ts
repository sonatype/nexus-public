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

import { renderHook } from '@testing-library/react';
import { ExtJS } from '../../../../../interface/ExtJS';
import { FIREWALL_SECURITY_TABS } from '../firewallSecurityTabs';
import { useVisibleFirewallSecurityTabs } from '../useVisibleFirewallSecurityTabs';

jest.mock('../tabs/malware/MalwarePreviewView', () => ({ MalwarePreviewView: () => null }));
jest.mock('../tabs/malware/MalwareLivePlaceholder', () => ({ MalwareLivePlaceholder: () => null }));
jest.mock('../tabs/waivers/WaiversPreviewView', () => ({ WaiversPreviewView: () => null }));
jest.mock('../tabs/waivers/WaiversLivePlaceholder', () => ({ WaiversLivePlaceholder: () => null }));
jest.mock('../tabs/quarantine/QuarantinePreviewView', () => ({ QuarantinePreviewView: () => null }));
jest.mock('../tabs/quarantine/QuarantineLivePlaceholder', () => ({ QuarantineLivePlaceholder: () => null }));
jest.mock('../tabs/policies/PoliciesPreviewView', () => ({ PoliciesPreviewView: () => null }));
jest.mock('../tabs/policies/PoliciesLivePlaceholder', () => ({ PoliciesLivePlaceholder: () => null }));
jest.mock('../tabs/evaluations/EvaluationsPreviewView', () => ({ EvaluationsPreviewView: () => null }));
jest.mock('../tabs/evaluations/EvaluationsLivePlaceholder', () => ({ EvaluationsLivePlaceholder: () => null }));

let mockHasUser = true;
jest.mock('../../../shared/hooks/useHasUser', () => ({ useHasUser: () => mockHasUser }));

const ALL_IDS = ['malware', 'waivers', 'quarantine', 'policies', 'evaluations'];

const grant = (permissions: string[]) => {
  jest.spyOn(ExtJS, 'checkPermission').mockImplementation((p: string) => permissions.includes(p));
  jest.spyOn(ExtJS, 'usePermission').mockImplementation((getValue: () => unknown) => getValue());
};

const visibleIds = (tier: Parameters<typeof useVisibleFirewallSecurityTabs>[0]) =>
  renderHook(() => useVisibleFirewallSecurityTabs(tier)).result.current.map((t) => t.id);

describe('FIREWALL_SECURITY_TABS registry', () => {
  it('lists the five tabs in order with their read permissions', () => {
    expect(FIREWALL_SECURITY_TABS.map((t) => [t.id, t.label, t.permission])).toEqual([
      ['malware', 'Malware', 'nexus:malware-findings:read'],
      ['waivers', 'Waivers', 'nexus:firewall-waivers:read'],
      ['quarantine', 'Quarantine', 'nexus:firewall-quarantine:read'],
      ['policies', 'Policies', 'nexus:firewall-policy:read'],
      ['evaluations', 'Evaluations', 'nexus:firewall-evaluations:read'],
    ]);
  });

  it('gives every tab a Preview and a Live component', () => {
    FIREWALL_SECURITY_TABS.forEach((t) => {
      expect(typeof t.Preview).toBe('function');
      expect(typeof t.Live).toBe('function');
    });
  });
});

describe('useVisibleFirewallSecurityTabs (BDD-55508-09, AT-55508-08)', () => {
  beforeEach(() => {
    mockHasUser = true;
  });
  afterEach(() => jest.restoreAllMocks());

  it('shows all tabs in preview-pro even with no permissions', () => {
    grant([]);
    expect(visibleIds('preview-pro')).toEqual(ALL_IDS);
  });

  it('shows no tabs in preview-pro when signed out', () => {
    mockHasUser = false;
    grant([]);
    expect(visibleIds('preview-pro')).toEqual([]);
  });

  it('shows only the permitted tabs in preview-no-iq, none without privileges', () => {
    grant(['nexus:firewall-waivers:read', 'nexus:firewall-policy:read']);
    expect(visibleIds('preview-no-iq')).toEqual(['waivers', 'policies']);
    grant([]);
    expect(visibleIds('preview-no-iq')).toEqual([]);
  });

  it('shows only the permitted tabs in live, keeping registry order', () => {
    grant(['nexus:firewall-quarantine:read', 'nexus:malware-findings:read']);
    expect(visibleIds('live')).toEqual(['malware', 'quarantine']);
  });

  it('shows no tabs in live when no permission is granted', () => {
    grant([]);
    expect(visibleIds('live')).toEqual([]);
  });

  it('shows all tabs in live when every permission is granted', () => {
    grant(FIREWALL_SECURITY_TABS.map((t) => t.permission));
    expect(visibleIds('live')).toEqual(ALL_IDS);
  });

  it('shows no tabs for hidden', () => {
    grant(FIREWALL_SECURITY_TABS.map((t) => t.permission));
    expect(visibleIds('hidden')).toEqual([]);
  });

  it('shows only permitted tabs for upgrade-iq and error, none without privileges', () => {
    grant([FIREWALL_SECURITY_TABS[0].permission]);
    expect(visibleIds('upgrade-iq')).toEqual([FIREWALL_SECURITY_TABS[0].id]);
    expect(visibleIds('error')).toEqual([FIREWALL_SECURITY_TABS[0].id]);
    grant([]);
    expect(visibleIds('upgrade-iq')).toEqual([]);
    expect(visibleIds('error')).toEqual([]);
  });
});
