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
import { ExtJS } from '../../../../interface/ExtJS';
import { useFirewallSecurityPagesVisible } from '../useFirewallSecurityPagesVisible';
import { FIREWALL_SECURITY_PERMISSION_LIST } from '../firewallSecurityPermissions';

const mockTier = jest.fn();

jest.mock('../useFirewallSecurityPagesTier', () => ({
  useFirewallSecurityPagesTier: () => mockTier(),
}));

let mockHasUser = true;
jest.mock('../hooks/useHasUser', () => ({ useHasUser: () => mockHasUser }));

const grant = (permissions: string[]) => {
  jest.spyOn(ExtJS, 'checkPermission').mockImplementation((p: string) => permissions.includes(p));
  jest.spyOn(ExtJS, 'usePermission').mockImplementation((getValue: () => unknown) => getValue());
};

const visible = () => renderHook(() => useFirewallSecurityPagesVisible()).result.current;

describe('useFirewallSecurityPagesVisible (AT-55508-10)', () => {
  beforeEach(() => {
    mockHasUser = true;
  });
  afterEach(() => jest.restoreAllMocks());

  it('is false for hidden regardless of permissions', () => {
    mockTier.mockReturnValue('hidden');
    grant(FIREWALL_SECURITY_PERMISSION_LIST);
    expect(visible()).toBe(false);
  });

  it('is true for preview-pro without any permission', () => {
    mockTier.mockReturnValue('preview-pro');
    grant([]);
    expect(visible()).toBe(true);
  });

  it('is false for preview-pro when signed out', () => {
    mockTier.mockReturnValue('preview-pro');
    mockHasUser = false;
    grant([]);
    expect(visible()).toBe(false);
  });

  it.each(['preview-no-iq', 'upgrade-iq', 'error'])('is false for %s without any permission', (tier) => {
    mockTier.mockReturnValue(tier);
    grant([]);
    expect(visible()).toBe(false);
  });

  it('is true for preview-no-iq when the user holds one of the five read permissions', () => {
    mockTier.mockReturnValue('preview-no-iq');
    grant(['nexus:firewall-waivers:read']);
    expect(visible()).toBe(true);
  });

  it('is false for live when the user holds none of the five read permissions', () => {
    mockTier.mockReturnValue('live');
    grant(['nexus:something-else:read']);
    expect(visible()).toBe(false);
  });

  it.each(FIREWALL_SECURITY_PERMISSION_LIST)('is true for live when the user holds only %s', (permission) => {
    mockTier.mockReturnValue('live');
    grant([permission]);
    expect(visible()).toBe(true);
  });
});
