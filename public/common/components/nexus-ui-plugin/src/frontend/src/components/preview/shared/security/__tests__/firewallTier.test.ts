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

import {
  FIREWALL_PRO_SUPPORTED_FORMATS,
  getFirewallEnterpriseSupportedFormats,
  isFirewallEnterpriseSupportedFormat,
  isFirewallProSupportedFormat,
  isFirewallSupportedFormat,
} from '../firewallTier';
import { FIREWALL_SUPPORTED_FORMATS } from '../../../../../utils/firewallFormats';

/**
 * Drives the NX.State feature flags that gate some formats. clearMocks resets the
 * implementation between tests, so every test that depends on a flag must set it.
 */
function setStateValues(values: Record<string, unknown>) {
  (global as any).NX.State.getValue.mockImplementation((key: string) => values[key]);
}

describe('Firewall Enterprise format support', () => {
  beforeEach(() => {
    setStateValues({ swiftFirewallEnabled: true, firewallPubFormatEnabled: true });
  });

  // NEXUS-54536: the enterprise list used to be a second, hand-maintained copy that had
  // drifted from the backend FirewallSupportedFormat enum. It is now derived from the
  // single mirror of that enum, so these two must never diverge again (with all rollout
  // flags on, the derived list is exactly the enum mirror).
  it('covers exactly the formats Repository Firewall supports when all flags are on', () => {
    expect([...getFirewallEnterpriseSupportedFormats()].sort()).toEqual(
      [...FIREWALL_SUPPORTED_FORMATS].sort()
    );
  });

  it("uses the NXRM format id 'go', not 'golang'", () => {
    expect(isFirewallEnterpriseSupportedFormat('go')).toBe(true);
    expect(isFirewallEnterpriseSupportedFormat('golang')).toBe(false);
  });

  // Every one of these was missing from the previous hand-maintained list.
  it.each(['yum', 'docker', 'raw', 'conda', 'r', 'p2', 'huggingface'])(
    'reports %s as supported',
    (format) => {
      expect(isFirewallEnterpriseSupportedFormat(format)).toBe(true);
    }
  );

  it('is case-insensitive', () => {
    expect(isFirewallEnterpriseSupportedFormat('MAVEN2')).toBe(true);
  });

  it('rejects formats Repository Firewall does not support', () => {
    for (const format of ['apt', 'helm', 'terraform', 'gitlfs']) {
      expect(isFirewallEnterpriseSupportedFormat(format)).toBe(false);
    }
  });

  // FIRE-956 x NEXUS-53057 interaction. The limited-support override widens the UI *visibility*
  // gate (firewallFormats.isFirewallSupportedFormat) so admins can configure Firewall on formats
  // outside the backend enum. It must not reach anything on the tier path, which reports what
  // protection is actually in effect: the override lifts a *configuration* restriction and grants
  // IQ no new analysis capability, so a limited-support format is configurable but still not
  // analyzable. Both the derived list and the predicate therefore route through
  // firewallFormats.isFirewallAnalyzableFormat, which shares the rollout gate with the visibility
  // gate but not the blanket override.
  //
  // The list would hold anyway (it filters the enum mirror, so it can only narrow); the predicate
  // would not, and did not before this fix. Both are pinned so the two can never disagree.
  it('does not widen the derived Enterprise format list under the limited-support override', () => {
    setStateValues({ firewallLimitedSupportFormatsEnabled: true });

    for (const format of ['apt', 'helm', 'terraform', 'gitlfs']) {
      expect(getFirewallEnterpriseSupportedFormats()).not.toContain(format);
    }
  });

  it('keeps the Enterprise predicate false for limited-support formats under the override', () => {
    setStateValues({ firewallLimitedSupportFormatsEnabled: true });

    for (const format of ['apt', 'helm', 'terraform', 'gitlfs']) {
      expect(isFirewallEnterpriseSupportedFormat(format)).toBe(false);
      expect(isFirewallSupportedFormat(format, 'enterprise')).toBe(false);
    }

    setStateValues({ firewallLimitedSupportFormatsEnabled: false });

    for (const format of ['apt', 'helm', 'terraform', 'gitlfs']) {
      expect(isFirewallEnterpriseSupportedFormat(format)).toBe(false);
      expect(isFirewallSupportedFormat(format, 'enterprise')).toBe(false);
    }
  });

  // Formats behind a rollout flag are in the backend enum but must not be reported as
  // supported by the tier-aware check while their flag is off, or the UI enables Firewall
  // for a format the backend validators reject.
  describe('feature-flag gated formats', () => {
    // NEXUS-53057 review: the enterprise-specific exports used to bypass the flag gate and
    // report 'swift' supported (and list it) while swiftFirewallEnabled was off. They now
    // route through the same flag-aware check as the tier-aware isFirewallSupportedFormat.
    it('excludes swift from the enterprise exports while swiftFirewallEnabled is off', () => {
      setStateValues({ swiftFirewallEnabled: false });
      expect(isFirewallEnterpriseSupportedFormat('swift')).toBe(false);
      expect(getFirewallEnterpriseSupportedFormats()).not.toContain('swift');

      setStateValues({ swiftFirewallEnabled: true });
      expect(isFirewallEnterpriseSupportedFormat('swift')).toBe(true);
      expect(getFirewallEnterpriseSupportedFormats()).toContain('swift');
    });

    it('excludes pub from the enterprise exports while firewallPubFormatEnabled is off', () => {
      setStateValues({ firewallPubFormatEnabled: false });
      expect(isFirewallEnterpriseSupportedFormat('pub')).toBe(false);
      expect(getFirewallEnterpriseSupportedFormats()).not.toContain('pub');

      setStateValues({ firewallPubFormatEnabled: true });
      expect(isFirewallEnterpriseSupportedFormat('pub')).toBe(true);
      expect(getFirewallEnterpriseSupportedFormats()).toContain('pub');
    });

    // FIRE-956: a rollout-flagged format must not be rescued by the blanket override either.
    // FLAG_GATED_FORMATS is consulted before set membership, so the override never reaches swift.
    it('keeps swift excluded while its flag is off even with the limited-support override on', () => {
      setStateValues({ firewallLimitedSupportFormatsEnabled: true, swiftFirewallEnabled: false });
      expect(isFirewallEnterpriseSupportedFormat('swift')).toBe(false);
      expect(getFirewallEnterpriseSupportedFormats()).not.toContain('swift');
    });

    it('reports swift as supported only while swiftFirewallEnabled is on', () => {
      setStateValues({ swiftFirewallEnabled: true });
      expect(isFirewallSupportedFormat('swift', 'enterprise')).toBe(true);

      setStateValues({ swiftFirewallEnabled: false });
      expect(isFirewallSupportedFormat('swift', 'enterprise')).toBe(false);
    });

    it('reports pub as supported only while firewallPubFormatEnabled is on', () => {
      setStateValues({ firewallPubFormatEnabled: true });
      expect(isFirewallSupportedFormat('pub', 'enterprise')).toBe(true);

      setStateValues({ firewallPubFormatEnabled: false });
      expect(isFirewallSupportedFormat('pub', 'enterprise')).toBe(false);
    });

    it('rejects a gated format when the flag is absent entirely', () => {
      setStateValues({});
      expect(isFirewallSupportedFormat('swift', 'enterprise')).toBe(false);
      expect(isFirewallSupportedFormat('pub', 'enterprise')).toBe(false);
    });

    it('leaves an ungated format unaffected by the flags', () => {
      setStateValues({});
      expect(isFirewallSupportedFormat('maven2', 'enterprise')).toBe(true);
    });
  });
});

describe('Firewall Pro format support', () => {
  // Pro is malware-only and deliberately narrower than Enterprise; it is not derived.
  it('is limited to the four malware-protected formats', () => {
    expect([...FIREWALL_PRO_SUPPORTED_FORMATS].sort()).toEqual([
      'maven2',
      'npm',
      'nuget',
      'pypi',
    ]);
  });

  it('rejects a format Enterprise supports but Pro does not', () => {
    expect(isFirewallEnterpriseSupportedFormat('cargo')).toBe(true);
    expect(isFirewallProSupportedFormat('cargo')).toBe(false);
  });
});
