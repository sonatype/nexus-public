/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the Eclipse Foundation.
 * All other trademarks are the property of their respective owners.
 */
import { ROUTE_NAMES } from '../RouteNames';
import { RouteNames as BaseRouteNames } from '../../../../constants/RouteNames';

describe('preview ROUTE_NAMES', () => {
  it('spreads the base RouteNames', () => {
    expect(ROUTE_NAMES.LOGIN).toBe(BaseRouteNames.LOGIN);
    expect(ROUTE_NAMES.MISSING_ROUTE).toBe(BaseRouteNames.MISSING_ROUTE);
    expect(ROUTE_NAMES.ADMIN).toEqual(BaseRouteNames.ADMIN);
  });

  it('defines BROWSE.WELCOME with the shared classic welcome state name', () => {
    expect(ROUTE_NAMES.BROWSE.WELCOME.ROOT).toBe('browse.welcome');
    expect(ROUTE_NAMES.BROWSE.WELCOME.TITLE).toBe('Dashboard');
  });

  it('defines preview-only browse routes under PREVIEW_* keys with preview. prefix', () => {
    expect(ROUTE_NAMES.BROWSE.PREVIEW_MALICIOUS_PACKAGES.ROOT).toBe('preview.browse.malicious-packages');
    expect(ROUTE_NAMES.BROWSE.PREVIEW_MALICIOUS_PACKAGES.TITLE).toBe('Malicious Packages');
    expect(ROUTE_NAMES.BROWSE.PREVIEW_PROTECT.ROOT).toBe('preview.browse.protect');
    expect(ROUTE_NAMES.BROWSE.PREVIEW_PROTECT.TITLE).toBe('Protect');
    expect(ROUTE_NAMES.BROWSE.PREVIEW_MALWARERISK.ROOT).toBe('preview.browse.malwarerisk');
    expect(ROUTE_NAMES.BROWSE.PREVIEW_MALWARERISK.TITLE).toBe('Malware Risk');
  });

  it('does NOT define classic MALICIOUS_PACKAGES / PROTECT / MALWARERISK keys', () => {
    // Those keys are owned by each plugin's browseRouteNames.js (classic routes).
    // Layer 2 intentionally exposes preview variants under PREVIEW_* keys only.
    expect((ROUTE_NAMES.BROWSE as any).MALICIOUS_PACKAGES).toBeUndefined();
    expect((ROUTE_NAMES.BROWSE as any).PROTECT).toBeUndefined();
    expect((ROUTE_NAMES.BROWSE as any).MALWARERISK).toBeUndefined();
  });
});
