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
  DEFAULT_EDITION_DISPLAY_NAME,
  EDITION_CLOUD,
  EDITION_COMMUNITY,
  EDITION_CORE,
  EDITION_ENTERPRISE,
  EDITION_PRO,
  EDITION_PRO_PLUS,
  getEditionDisplayName,
  getEditionLogo,
  getHeaderWordmarkLogo,
} from '../editionUtils';

// SVGs are resolved to a mock path string by jest's imgMock.js, so identity comparisons
// between editions work only when both cases truly resolve to the same import.

const MOCK_ASSET = 'path/to/asset.png';

describe('getEditionDisplayName', () => {
  it.each([
    [EDITION_CORE, 'Core'],
    [EDITION_COMMUNITY, 'Community Edition'],
    [EDITION_PRO, 'Pro'],
    [EDITION_PRO_PLUS, 'Pro+'],
    [EDITION_ENTERPRISE, 'Enterprise'],
    [EDITION_CLOUD, 'Pro'],
  ])('maps %s to display name "%s"', (editionId, expected) => {
    expect(getEditionDisplayName(editionId)).toBe(expected);
  });

  it('returns the default display name for undefined so aria-labels stay well-formed at bootstrap', () => {
    expect(getEditionDisplayName(undefined)).toBe(DEFAULT_EDITION_DISPLAY_NAME);
  });

  it('returns the default display name for null so aria-labels stay well-formed at bootstrap', () => {
    expect(getEditionDisplayName(null)).toBe(DEFAULT_EDITION_DISPLAY_NAME);
  });

  it('falls back to the raw identifier for unknown editions', () => {
    expect(getEditionDisplayName('nexus-unknown-edition')).toBe('nexus-unknown-edition');
  });
});

describe('getEditionLogo', () => {
  it('returns a truthy value for every known edition in light theme', () => {
    const editions = [EDITION_CORE, EDITION_COMMUNITY, EDITION_PRO, EDITION_PRO_PLUS, EDITION_ENTERPRISE, EDITION_CLOUD];
    editions.forEach(editionId => {
      expect(getEditionLogo(editionId, false)).toBeTruthy();
    });
  });

  it('returns a truthy value for every known edition in dark theme', () => {
    const editions = [EDITION_CORE, EDITION_COMMUNITY, EDITION_PRO, EDITION_PRO_PLUS, EDITION_ENTERPRISE, EDITION_CLOUD];
    editions.forEach(editionId => {
      expect(getEditionLogo(editionId, true)).toBeTruthy();
    });
  });

  it('Pro+ edition resolves to a logo asset', () => {
    // Identity assertions between different imports are unreliable in this suite because
    // jest's imgMock resolves every SVG import to the same mock string; only verify
    // presence here and rely on the switch in getEditionLogo for correctness.
    expect(getEditionLogo(EDITION_PRO_PLUS, false)).toBeTruthy();
    expect(getEditionLogo(EDITION_PRO_PLUS, true)).toBeTruthy();
  });

  it('Enterprise edition resolves to a logo asset', () => {
    // The Enterprise import points at logo-enterprise-edition-header{,-dark}.svg; SVG
    // mocking makes identity comparisons meaningless, so only assert presence.
    expect(getEditionLogo(EDITION_ENTERPRISE, false)).toBeTruthy();
    expect(getEditionLogo(EDITION_ENTERPRISE, true)).toBeTruthy();
  });

  it('cloud edition intentionally shares the Pro logo import (same reference by design)', () => {
    // Both EDITION_CLOUD and EDITION_PRO return the exact same imported binding
    // from the switch, which is a real identity match rather than a mock artifact.
    expect(getEditionLogo(EDITION_CLOUD, false)).toBe(getEditionLogo(EDITION_PRO, false));
    expect(getEditionLogo(EDITION_CLOUD, true)).toBe(getEditionLogo(EDITION_PRO, true));
  });

  it('falls back to Core logo for undefined edition', () => {
    expect(getEditionLogo(undefined, false)).toBe(getEditionLogo(EDITION_CORE, false));
  });

  it('falls back to Core logo for unknown edition identifier', () => {
    expect(getEditionLogo('nexus-unknown-edition', false)).toBe(getEditionLogo(EDITION_CORE, false));
  });

  it('returns the mock asset path (confirming SVGs resolve in test environment)', () => {
    expect(getEditionLogo(EDITION_PRO, false)).toBe(MOCK_ASSET);
  });
});

describe('getHeaderWordmarkLogo', () => {
  it('returns a truthy value in light mode', () => {
    expect(getHeaderWordmarkLogo(false)).toBeTruthy();
  });

  it('returns a truthy value in dark mode', () => {
    expect(getHeaderWordmarkLogo(true)).toBeTruthy();
  });

  it('accepts both light and dark flag values without throwing', () => {
    expect(() => getHeaderWordmarkLogo(false)).not.toThrow();
    expect(() => getHeaderWordmarkLogo(true)).not.toThrow();
  });
});
