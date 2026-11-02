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

import type { Waiver } from './waiverTypes';

const DAY = 86_400_000;

// Anchored at module load, not to a literal date. These rows exist to illustrate the four
// expiration bands, and a hardcoded base silently degrades: the "expires in 3 days" row
// becomes an expired row once real time passes it, so the preview tier would misrepresent
// the very thing it is demonstrating. Normalised to UTC midnight to match the whole-UTC-day
// semantics of expirationBand.
const base = (() => {
  const today = new Date();
  return Date.UTC(today.getUTCFullYear(), today.getUTCMonth(), today.getUTCDate());
})();

const row = (over: Partial<Waiver> & { policyWaiverId: string }): Waiver => ({
  comment: null, createTime: base - 30 * DAY, expiryTime: null,
  scopeOwnerType: 'repository', scopeOwnerId: 'r1', scopeOwnerName: 'maven-central',
  policyId: 'p1', policyName: 'Security-High', threatLevel: 8, displayName: null,
  ecosystem: null, groupId: null, artifactId: null, version: null, hash: null,
  matcherStrategy: null, reasonText: null, policyWaiverReasonId: null,
  componentUpgradeAvailable: null, expireWhenRemediationAvailable: false,
  forContainerImage: false, lastRenewedAt: null, lastRenewedBy: null,
  lastRenewalComment: null, ...over,
});

/**
 * Preview-tier fixture. Deliberately exercises the states the prototype's own fixture never
 * did — a null expiry, every expiration band, all three upgrade states, and a scope that is
 * root_organization rather than a repository.
 */
export const MOCK_WAIVERS: Waiver[] = [
  row({
    policyWaiverId: 'mock-1', threatLevel: 10, policyName: 'Security-Critical',
    expiryTime: base + 3 * DAY, displayName: 'org.apache.logging.log4j : log4j-core : 2.14.1',
    componentUpgradeAvailable: true,
  }),
  row({
    policyWaiverId: 'mock-2', threatLevel: 9, policyName: 'Security-High',
    expiryTime: base + 20 * DAY, displayName: 'com.fasterxml.jackson.core : jackson-databind : 2.9.8',
    componentUpgradeAvailable: false, scopeOwnerType: 'application', scopeOwnerName: 'payment-gateway',
  }),
  row({
    policyWaiverId: 'mock-3', threatLevel: 7, policyName: 'License-Commercial',
    expiryTime: base + 90 * DAY, ecosystem: 'npm', groupId: 'lodash', artifactId: 'lodash',
    version: '4.17.20', componentUpgradeAvailable: null,
  }),
  row({
    policyWaiverId: 'mock-4', threatLevel: 5, policyName: 'License-None',
    expiryTime: null, displayName: 'com.example : legacy-lib : 0.9',
    scopeOwnerType: 'root_organization', scopeOwnerId: 'ROOT_ORGANIZATION_ID',
    scopeOwnerName: 'Root Organization',
  }),
];
