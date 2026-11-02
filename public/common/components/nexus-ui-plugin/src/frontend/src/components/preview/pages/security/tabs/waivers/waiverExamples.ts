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

/** Illustrative only: the single row the prototype shows on each Waivers tab. */
export interface ExampleWaiver {
  threatLevel: number;
  createdOn: string;
  expiresOn: string;
  policyName: string;
  scope: string;
  component: string;
  upgradeVersion: string;
}

export const EXAMPLE_WAIVER: ExampleWaiver = {
  threatLevel: 10,
  createdOn: '2026-06-02',
  expiresOn: '2027-03-15',
  policyName: 'Security-Critical',
  scope: 'maven-central',
  component: 'maven/org.apache.logging.log4j:log4j-core@2.14.1',
  upgradeVersion: '2.17.1',
};

export interface ExampleWaiverRequest {
  threatLevel: number;
  component: string;
  repository: string;
  requestedBy: string;
  reason: string;
}

export const EXAMPLE_WAIVER_REQUEST: ExampleWaiverRequest = {
  threatLevel: 9,
  component: 'pypi/Django@4.2.14',
  repository: 'acme-pypi-proxy',
  requestedBy: 'data.eng@example.com',
  reason: 'Blocked release; patch branch is already open upstream.',
};
