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

import { shiroWildcardImplies, userImpliesPermission, wildcardImplies } from '../shiroWildcardImplies';

describe('wildcardImplies', () => {
  it('matches exact permission', () => {
    expect(wildcardImplies('nexus:settings:read', 'nexus:settings:read')).toBe(true);
  });

  it('nexus:* implies nexus:settings:read', () => {
    expect(wildcardImplies('nexus:*', 'nexus:settings:read')).toBe(true);
  });

  it('narrow permission does not imply broader', () => {
    expect(wildcardImplies('nexus:settings:read', 'nexus:*')).toBe(false);
  });

  it('repository-admin style wildcards', () => {
    expect(wildcardImplies('nexus:repository-admin:*:*:read', 'nexus:repository-admin:maven:repo:read')).toBe(true);
  });
});

describe('shiroWildcardImplies', () => {
  it('returns true if any grant implies required', () => {
    expect(shiroWildcardImplies('nexus:foo:read', ['nexus:*', 'other:thing'])).toBe(true);
  });

  it('returns false when no grant matches', () => {
    expect(shiroWildcardImplies('nexus:foo:read', ['nexus:bar:read'])).toBe(false);
  });
});

describe('userImpliesPermission', () => {
  it('reads truthy keys from permission map', () => {
    const map: Record<string, boolean> = { 'nexus:*': true, 'nexus:bar:read': false };
    expect(userImpliesPermission('nexus:settings:read', map)).toBe(true);
  });
});
