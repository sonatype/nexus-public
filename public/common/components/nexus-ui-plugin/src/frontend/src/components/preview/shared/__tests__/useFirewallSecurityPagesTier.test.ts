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
import { useFirewallSecurityPagesState, useFirewallSecurityPagesTier } from '../useFirewallSecurityPagesTier';
import * as featureFlags from '../../config/featureFlags';
import { useEditionId } from '../hooks/useEditionId';

jest.mock('../../config/featureFlags', () => ({
  isLocalhost: jest.fn(),
}));

jest.mock('../hooks/useEditionId', () => ({
  useEditionId: jest.fn(),
}));

let mockStateValues: Record<string, unknown> = {};

jest.mock('../../../../interface/ExtJS', () => ({
  ExtJS: {
    state: () => ({
      getValue: (key: string) => (key in mockStateValues ? mockStateValues[key] : null),
    }),
    useState: (getValue: () => unknown) => getValue(),
  },
}));

const STATE_KEY = 'nexus.firewall.security.pages';
const CLM_KEY = 'clm';
const PRO = 'nexus-professional-edition';
const PRO_PLUS = 'nexus-professional-plus-edition';
const ENTERPRISE = 'nexus-enterprise-edition';
const SIX_TIERS = ['hidden', 'preview-pro', 'preview-no-iq', 'upgrade-iq', 'error', 'live'];

const baseState = {
  featureEnabled: true,
  entitled: true,
  iqConfigured: true,
  iqReachable: true,
  iqVersionSupported: true,
  minIqVersion: '1.204',
};

const setState = (overrides: Record<string, unknown> | null) => {
  if (overrides === null) {
    delete mockStateValues[STATE_KEY];
  } else {
    mockStateValues[STATE_KEY] = { ...baseState, ...overrides };
  }
};

const setTierParam = (value: string | null) => {
  const url = new URL(window.location.href);
  if (value === null) {
    url.searchParams.delete('tier');
  } else {
    url.searchParams.set('tier', value);
  }
  window.history.replaceState({}, '', url.toString());
};

describe('useFirewallSecurityPagesTier', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockStateValues = {};
    (featureFlags.isLocalhost as jest.Mock).mockReturnValue(false);
    (useEditionId as jest.Mock).mockReturnValue(undefined);
    setTierParam(null);
  });

  const tier = () => renderHook(() => useFirewallSecurityPagesTier()).result.current;

  describe('rule 1: flag off, missing state or edition not Pro/Pro+/Enterprise -> hidden (BDD-55508-01, BDD-55508-02)', () => {
    it.each([PRO, PRO_PLUS, ENTERPRISE])('is hidden for %s when featureEnabled is false', (edition) => {
      setState({ featureEnabled: false });
      (useEditionId as jest.Mock).mockReturnValue(edition);
      expect(tier()).toBe('hidden');
    });

    it.each([PRO, PRO_PLUS, ENTERPRISE])('is hidden for %s when the state key is missing', (edition) => {
      setState(null);
      (useEditionId as jest.Mock).mockReturnValue(edition);
      expect(tier()).toBe('hidden');
    });

    it('treats a non-boolean featureEnabled as OFF', () => {
      setState({ featureEnabled: 'true' });
      (useEditionId as jest.Mock).mockReturnValue(PRO_PLUS);
      expect(tier()).toBe('hidden');
    });

    it.each(['nexus-community-edition', 'nexus-core-edition', 'something-else', undefined])(
      'is hidden for edition %s even when the flag is on',
      (edition) => {
        setState({});
        (useEditionId as jest.Mock).mockReturnValue(edition);
        expect(tier()).toBe('hidden');
      }
    );
  });

  describe('rule 2: Pro with clm.enabled -> hidden (BDD-55508-03)', () => {
    it('is hidden on Pro when clm.enabled is true', () => {
      setState({});
      mockStateValues[CLM_KEY] = { enabled: true };
      (useEditionId as jest.Mock).mockReturnValue(PRO);
      expect(tier()).toBe('hidden');
    });

    it.each([{ enabled: false }, {}, null])('does not hide Pro for clm = %j', (clm) => {
      setState({});
      mockStateValues[CLM_KEY] = clm;
      (useEditionId as jest.Mock).mockReturnValue(PRO);
      expect(tier()).toBe('preview-pro');
    });

    it.each([PRO_PLUS, ENTERPRISE])('clm.enabled does not hide %s', (edition) => {
      setState({});
      mockStateValues[CLM_KEY] = { enabled: true };
      (useEditionId as jest.Mock).mockReturnValue(edition);
      expect(tier()).toBe('live');
    });
  });

  describe('rule 3: Pro -> preview-pro (BDD-55508-04)', () => {
    it('is preview-pro on Pro regardless of the IQ fields', () => {
      setState({ iqConfigured: false, iqReachable: false, iqVersionSupported: false });
      (useEditionId as jest.Mock).mockReturnValue(PRO);
      expect(tier()).toBe('preview-pro');
    });
  });

  describe('rules 4-7 on Pro+ and Enterprise (BDD-55508-05, BDD-55508-07, BDD-55508-08, BDD-55508-09)', () => {
    it.each([PRO_PLUS, ENTERPRISE])('rule 4: IQ not configured -> preview-no-iq on %s', (edition) => {
      setState({ iqConfigured: false, iqReachable: null });
      (useEditionId as jest.Mock).mockReturnValue(edition);
      expect(tier()).toBe('preview-no-iq');
    });

    it.each([PRO_PLUS, ENTERPRISE])('rule 5: unsupported IQ version -> upgrade-iq on %s', (edition) => {
      setState({ iqVersionSupported: false });
      (useEditionId as jest.Mock).mockReturnValue(edition);
      expect(tier()).toBe('upgrade-iq');
    });

    it.each([PRO_PLUS, ENTERPRISE])('rule 6: unreachable IQ -> error on %s', (edition) => {
      setState({ iqReachable: false });
      (useEditionId as jest.Mock).mockReturnValue(edition);
      expect(tier()).toBe('error');
    });

    it.each([PRO_PLUS, ENTERPRISE])('rule 7: configured, supported and reachable -> live on %s', (edition) => {
      setState({});
      (useEditionId as jest.Mock).mockReturnValue(edition);
      expect(tier()).toBe('live');
    });

    it('rule 7: a not-yet-probed IQ (iqReachable null) is live', () => {
      setState({ iqReachable: null });
      (useEditionId as jest.Mock).mockReturnValue(PRO_PLUS);
      expect(tier()).toBe('live');
    });
  });

  describe('rule order', () => {
    beforeEach(() => {
      (useEditionId as jest.Mock).mockReturnValue(PRO_PLUS);
    });

    it('not configured beats unsupported version and unreachable', () => {
      setState({ iqConfigured: false, iqVersionSupported: false, iqReachable: false });
      expect(tier()).toBe('preview-no-iq');
    });

    it('unsupported version beats unreachable', () => {
      setState({ iqVersionSupported: false, iqReachable: false });
      expect(tier()).toBe('upgrade-iq');
    });

    it('flag off beats everything else', () => {
      setState({ featureEnabled: false, iqConfigured: false, iqVersionSupported: false, iqReachable: false });
      expect(tier()).toBe('hidden');
    });

    it('Pro clm hide beats preview-pro', () => {
      setState({});
      mockStateValues[CLM_KEY] = { enabled: true };
      (useEditionId as jest.Mock).mockReturnValue(PRO);
      expect(tier()).toBe('hidden');
    });
  });

  describe('?tier= override (AT-55508-05, BDD-55508-11)', () => {
    it.each(SIX_TIERS)('honours ?tier=%s on localhost', (value) => {
      (featureFlags.isLocalhost as jest.Mock).mockReturnValue(true);
      setState(null);
      (useEditionId as jest.Mock).mockReturnValue(undefined);
      setTierParam(value);
      expect(tier()).toBe(value);
    });

    it.each(['invalid', 'preview', 'entitled'])('ignores the retired or invalid value %s on localhost', (value) => {
      (featureFlags.isLocalhost as jest.Mock).mockReturnValue(true);
      setState({});
      (useEditionId as jest.Mock).mockReturnValue(PRO_PLUS);
      setTierParam(value);
      expect(tier()).toBe('live');
    });

    it.each(SIX_TIERS)('ignores ?tier=%s off localhost', (value) => {
      setState({});
      (useEditionId as jest.Mock).mockReturnValue(PRO);
      setTierParam(value);
      expect(tier()).toBe('preview-pro');
    });

    it('cannot upgrade a hidden tier to live in production', () => {
      (useEditionId as jest.Mock).mockReturnValue('nexus-community-edition');
      setTierParam('live');
      expect(tier()).toBe('hidden');
    });
  });
});

describe('useFirewallSecurityPagesState (AT-55508-05)', () => {
  beforeEach(() => {
    mockStateValues = {};
  });

  it('returns the raw state object including minIqVersion', () => {
    setState({ minIqVersion: '1.300' });
    const { result } = renderHook(() => useFirewallSecurityPagesState());
    expect(result.current).toEqual({ ...baseState, minIqVersion: '1.300' });
  });

  it('returns null when the key is absent', () => {
    const { result } = renderHook(() => useFirewallSecurityPagesState());
    expect(result.current).toBeNull();
  });
});
