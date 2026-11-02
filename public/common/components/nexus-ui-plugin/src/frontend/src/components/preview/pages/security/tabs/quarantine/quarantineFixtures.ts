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

export interface QuarantineFixture {
  id: string;
  component: string;
  policyName: string;
  threatLevel: number;
  repository: string;
  quarantinedOn: string;
}

/** Illustrative only. The same single entry is shown on every preview tier. */
export const QUARANTINE_FIXTURES: QuarantineFixture[] = [
  {
    id: 'q-1',
    component: 'maven/org.apache.logging.log4j:log4j-core@2.14.1',
    policyName: 'No CVSS 7.0+',
    threatLevel: 10,
    repository: 'maven-central',
    quarantinedOn: 'May 24, 2026, 2:14 PM',
  },
];

export interface CachedRiskFixture {
  id: string;
  downloads: number;
  component: string;
  threatLevel: number;
  cve: string;
  repository: string;
}

export const CACHED_RISK_FIXTURES: CachedRiskFixture[] = [
  {
    id: 'c-1',
    downloads: 9378,
    component: 'commons-text@1.9',
    threatLevel: 9.8,
    cve: 'CVE-2022-42889',
    repository: 'maven-central',
  },
];

export interface QuarantinedContainerFixture {
  id: string;
  image: string;
  digest: string;
  policyName: string;
  quarantinedOn: string;
  repository: string;
  layer: { index: number; total: number; threatLevel: number; component: string };
}

export const CONTAINER_FIXTURES: QuarantinedContainerFixture[] = [
  {
    id: 'k-1',
    image: 'nginx:1.21.0',
    digest: 'sha256:9b1c4a2f7e3d',
    policyName: 'Security-Critical',
    quarantinedOn: 'May 24, 2026, 3:02 PM',
    repository: 'gcr-proxy',
    layer: { index: 4, total: 7, threatLevel: 10, component: 'maven/org.apache.logging.log4j:log4j-core@2.14.1' },
  },
];
