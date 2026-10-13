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
  isFirewallAnalyzableFormat,
  isFirewallSupportedFormat,
  FIREWALL_SUPPORTED_FORMATS,
} from '../firewallFormats';
import ExtJS from '../../interface/ExtJS';

jest.mock('../../interface/ExtJS');

const flagValues: Record<string, boolean | undefined> = {};

function setFlag(name: string, value: boolean | undefined) {
  flagValues[name] = value;
  (ExtJS.state() as any).getValue.mockImplementation((key: string) => flagValues[key]);
}

function setFlags(flags: Record<string, boolean>) {
  (ExtJS.state() as any).getValue.mockImplementation((key: string) =>
    key in flags ? flags[key] : undefined,
  );
}

describe('firewallFormats', () => {
  beforeEach(() => {
    Object.keys(flagValues).forEach((key) => {
      delete flagValues[key];
    });
    setFlag('firewallPubFormatEnabled', true);
    setFlag('swiftFirewallEnabled', true);
  });

  describe('FIREWALL_SUPPORTED_FORMATS', () => {
    it('includes at least 18 supported formats', () => {
      expect(FIREWALL_SUPPORTED_FORMATS.size).toBeGreaterThanOrEqual(18);
    });

    it('includes known supported formats', () => {
      const expected = ['maven2', 'npm', 'nuget', 'pypi', 'docker', 'cargo', 'go', 'rubygems', 'pub'];
      expected.forEach((fmt) => {
        expect(FIREWALL_SUPPORTED_FORMATS.has(fmt)).toBe(true);
      });
    });

    it('does not include unsupported formats', () => {
      const unsupported = ['terraform', 'apt', 'helm', 'gitlfs'];
      unsupported.forEach((fmt) => {
        expect(FIREWALL_SUPPORTED_FORMATS.has(fmt)).toBe(false);
      });
    });
  });

  describe('isFirewallSupportedFormat', () => {
    it('returns true for supported formats', () => {
      expect(isFirewallSupportedFormat('maven2')).toBe(true);
      expect(isFirewallSupportedFormat('npm')).toBe(true);
      expect(isFirewallSupportedFormat('docker')).toBe(true);
      expect(isFirewallSupportedFormat('cargo')).toBe(true);
      expect(isFirewallSupportedFormat('nuget')).toBe(true);
    });

    it('returns false for unsupported formats', () => {
      expect(isFirewallSupportedFormat('terraform')).toBe(false);
      expect(isFirewallSupportedFormat('apt')).toBe(false);
      expect(isFirewallSupportedFormat('helm')).toBe(false);
      expect(isFirewallSupportedFormat('gitlfs')).toBe(false);
    });

    it('is case-insensitive', () => {
      expect(isFirewallSupportedFormat('Maven2')).toBe(true);
      expect(isFirewallSupportedFormat('NPM')).toBe(true);
      expect(isFirewallSupportedFormat('TERRAFORM')).toBe(false);
    });

    it('returns false for null, undefined, and empty string', () => {
      expect(isFirewallSupportedFormat(null)).toBe(false);
      expect(isFirewallSupportedFormat(undefined)).toBe(false);
      expect(isFirewallSupportedFormat('')).toBe(false);
    });

    it('includes p2 and raw (backend enum but undocumented - treated as supported)', () => {
      expect(isFirewallSupportedFormat('p2')).toBe(true);
      expect(isFirewallSupportedFormat('raw')).toBe(true);
    });

    // The rollout gate is shared by both exported predicates, so every assertion here is made
    // through both: the FIRE-956 override split must not have moved pub/swift on either side.
    describe('pub (feature-flag gated)', () => {
      it('returns true for pub when firewallPubFormatEnabled is true', () => {
        setFlag('firewallPubFormatEnabled', true);
        expect(isFirewallSupportedFormat('pub')).toBe(true);
        expect(isFirewallAnalyzableFormat('pub')).toBe(true);
      });

      it('returns false for pub when firewallPubFormatEnabled is false', () => {
        setFlag('firewallPubFormatEnabled', false);
        expect(isFirewallSupportedFormat('pub')).toBe(false);
        expect(isFirewallAnalyzableFormat('pub')).toBe(false);
      });

      it('returns false for pub when firewallPubFormatEnabled is undefined', () => {
        (ExtJS.state() as any).getValue.mockImplementation(() => undefined);
        expect(isFirewallSupportedFormat('pub')).toBe(false);
        expect(isFirewallAnalyzableFormat('pub')).toBe(false);
      });

      it('is case-insensitive for pub gating', () => {
        setFlag('firewallPubFormatEnabled', true);
        expect(isFirewallSupportedFormat('PUB')).toBe(true);
        expect(isFirewallAnalyzableFormat('PUB')).toBe(true);
        setFlag('firewallPubFormatEnabled', false);
        expect(isFirewallSupportedFormat('Pub')).toBe(false);
        expect(isFirewallAnalyzableFormat('Pub')).toBe(false);
      });
    });

    describe('swift (feature-flag gated)', () => {
      it('returns true for swift when swiftFirewallEnabled is true', () => {
        setFlag('swiftFirewallEnabled', true);
        expect(isFirewallSupportedFormat('swift')).toBe(true);
        expect(isFirewallAnalyzableFormat('swift')).toBe(true);
      });

      it('returns false for swift when swiftFirewallEnabled is false', () => {
        setFlag('swiftFirewallEnabled', false);
        expect(isFirewallSupportedFormat('swift')).toBe(false);
        expect(isFirewallAnalyzableFormat('swift')).toBe(false);
      });

      it('returns false for swift when swiftFirewallEnabled is undefined', () => {
        (ExtJS.state() as any).getValue.mockImplementation(() => undefined);
        expect(isFirewallSupportedFormat('swift')).toBe(false);
        expect(isFirewallAnalyzableFormat('swift')).toBe(false);
      });
    });

    // FIRE-956: blanket limited-support-format override.
    describe('limited-support override (firewallLimitedSupportFormatsEnabled)', () => {
      it('reports previously unsupported formats as supported when the override is on', () => {
        setFlags({firewallLimitedSupportFormatsEnabled: true});
        ['terraform', 'apt', 'helm', 'gitlfs', 'alpine', 'ansiblegalaxy', 'oci'].forEach((fmt) => {
          expect(isFirewallSupportedFormat(fmt)).toBe(true);
        });
      });

      // The override is blanket by design so that a format added to NXRM later is not silently
      // excluded. This asserts the absence of an allowlist, which a future refactor could reintroduce.
      it('applies to arbitrary formats, not a hardcoded list', () => {
        setFlags({firewallLimitedSupportFormatsEnabled: true});
        expect(isFirewallSupportedFormat('some-future-format')).toBe(true);
      });

      // Object.prototype member names must be treated as ordinary unknown formats, not as
      // flag-gated ones. A bare FLAG_GATED_FORMATS[key] lookup resolves them via the prototype
      // chain and reports them unsupported even with the blanket override on.
      it('does not let inherited Object.prototype keys escape the override', () => {
        setFlags({firewallLimitedSupportFormatsEnabled: true});
        ['constructor', 'toString', 'valueOf', 'hasOwnProperty'].forEach((key) => {
          expect(isFirewallSupportedFormat(key)).toBe(true);
        });
      });

      it('leaves already-supported formats supported', () => {
        setFlags({firewallLimitedSupportFormatsEnabled: true});
        expect(isFirewallSupportedFormat('maven2')).toBe(true);
        expect(isFirewallSupportedFormat('npm')).toBe(true);
      });

      it('is case-insensitive', () => {
        setFlags({firewallLimitedSupportFormatsEnabled: true});
        expect(isFirewallSupportedFormat('HELM')).toBe(true);
        expect(isFirewallSupportedFormat('Apt')).toBe(true);
      });

      it('does not override the pub gate', () => {
        setFlags({firewallLimitedSupportFormatsEnabled: true, firewallPubFormatEnabled: false});
        expect(isFirewallSupportedFormat('pub')).toBe(false);
      });

      // NEXUS-53057 made swift a member of FIREWALL_SUPPORTED_FORMATS carrying its own rollout
      // flag, which makes it the first real instance of the precedence FLAG_GATED_FORMATS-before-
      // override was written for (previously only pub exercised it). The blanket override must not
      // rescue a format that has its own rollout flag.
      it('does not override the swift gate', () => {
        setFlags({firewallLimitedSupportFormatsEnabled: true, swiftFirewallEnabled: false});
        expect(isFirewallSupportedFormat('swift')).toBe(false);
      });

      // The two flags are orthogonal, not mutually exclusive: pub stays supported when both are on.
      it('leaves pub supported when both the override and the pub flag are on', () => {
        setFlags({firewallLimitedSupportFormatsEnabled: true, firewallPubFormatEnabled: true});
        expect(isFirewallSupportedFormat('pub')).toBe(true);
      });

      it('leaves swift supported when both the override and the swift flag are on', () => {
        setFlags({firewallLimitedSupportFormatsEnabled: true, swiftFirewallEnabled: true});
        expect(isFirewallSupportedFormat('swift')).toBe(true);
      });

      it('still reports null, undefined and empty as unsupported', () => {
        setFlags({firewallLimitedSupportFormatsEnabled: true});
        expect(isFirewallSupportedFormat(null)).toBe(false);
        expect(isFirewallSupportedFormat(undefined)).toBe(false);
        expect(isFirewallSupportedFormat('')).toBe(false);
      });

      it('changes nothing when the override is off', () => {
        setFlags({firewallLimitedSupportFormatsEnabled: false, firewallPubFormatEnabled: true});
        expect(isFirewallSupportedFormat('helm')).toBe(false);
        expect(isFirewallSupportedFormat('apt')).toBe(false);
        expect(isFirewallSupportedFormat('maven2')).toBe(true);
        expect(isFirewallSupportedFormat('pub')).toBe(true);
      });

      it('does not widen FIREWALL_SUPPORTED_FORMATS itself', () => {
        setFlags({firewallLimitedSupportFormatsEnabled: true});
        expect(FIREWALL_SUPPORTED_FORMATS.has('helm')).toBe(false);
        expect(FIREWALL_SUPPORTED_FORMATS.has('apt')).toBe(false);
      });
    });
  });

  // The two exported predicates answer different questions and must not be collapsed back into
  // one: isFirewallSupportedFormat is the UI-visibility gate (may Firewall be configured for this
  // format), isFirewallAnalyzableFormat is the capability check (can IQ actually analyze it). They
  // agree everywhere except on a format outside FIREWALL_SUPPORTED_FORMATS with the FIRE-956
  // limited-support override on: configurable, but still not analyzable.
  describe('isFirewallAnalyzableFormat', () => {
    it('agrees with the visibility gate on formats inside the supported set', () => {
      ['maven2', 'npm', 'docker', 'cargo', 'nuget', 'p2', 'raw'].forEach((fmt) => {
        expect(isFirewallAnalyzableFormat(fmt)).toBe(true);
        expect(isFirewallSupportedFormat(fmt)).toBe(true);
      });
    });

    it('agrees with the visibility gate on unsupported formats while the override is off', () => {
      setFlags({firewallLimitedSupportFormatsEnabled: false});
      ['terraform', 'apt', 'helm', 'gitlfs'].forEach((fmt) => {
        expect(isFirewallAnalyzableFormat(fmt)).toBe(false);
        expect(isFirewallSupportedFormat(fmt)).toBe(false);
      });
    });

    it('is case-insensitive and rejects null, undefined and empty', () => {
      expect(isFirewallAnalyzableFormat('Maven2')).toBe(true);
      expect(isFirewallAnalyzableFormat('TERRAFORM')).toBe(false);
      expect(isFirewallAnalyzableFormat(null)).toBe(false);
      expect(isFirewallAnalyzableFormat(undefined)).toBe(false);
      expect(isFirewallAnalyzableFormat('')).toBe(false);
    });

    // The split itself. With the override on, the visibility gate opens and the capability check
    // stays shut for every limited-support format.
    it('stays false for limited-support formats while the visibility gate reports true', () => {
      setFlags({firewallLimitedSupportFormatsEnabled: true});
      ['terraform', 'apt', 'helm', 'gitlfs', 'alpine', 'ansiblegalaxy', 'oci'].forEach((fmt) => {
        expect(isFirewallSupportedFormat(fmt)).toBe(true);
        expect(isFirewallAnalyzableFormat(fmt)).toBe(false);
      });
    });

    it('is not rescued by the override for an arbitrary unknown format', () => {
      setFlags({firewallLimitedSupportFormatsEnabled: true});
      expect(isFirewallSupportedFormat('some-future-format')).toBe(true);
      expect(isFirewallAnalyzableFormat('some-future-format')).toBe(false);
    });

    it('keeps the shared rollout gate ahead of the override for pub and swift', () => {
      setFlags({
        firewallLimitedSupportFormatsEnabled: true,
        firewallPubFormatEnabled: false,
        swiftFirewallEnabled: false,
      });
      expect(isFirewallAnalyzableFormat('pub')).toBe(false);
      expect(isFirewallAnalyzableFormat('swift')).toBe(false);
      expect(isFirewallSupportedFormat('pub')).toBe(false);
      expect(isFirewallSupportedFormat('swift')).toBe(false);

      setFlags({
        firewallLimitedSupportFormatsEnabled: true,
        firewallPubFormatEnabled: true,
        swiftFirewallEnabled: true,
      });
      expect(isFirewallAnalyzableFormat('pub')).toBe(true);
      expect(isFirewallAnalyzableFormat('swift')).toBe(true);
      expect(isFirewallSupportedFormat('pub')).toBe(true);
      expect(isFirewallSupportedFormat('swift')).toBe(true);
    });

    // Object.prototype member names must not take the flag-gated branch in either predicate.
    it('treats inherited Object.prototype keys as ordinary unknown formats', () => {
      setFlags({firewallLimitedSupportFormatsEnabled: true});
      ['constructor', 'toString', 'valueOf', 'hasOwnProperty'].forEach((key) => {
        expect(isFirewallSupportedFormat(key)).toBe(true);
        expect(isFirewallAnalyzableFormat(key)).toBe(false);
      });
    });
  });
});
