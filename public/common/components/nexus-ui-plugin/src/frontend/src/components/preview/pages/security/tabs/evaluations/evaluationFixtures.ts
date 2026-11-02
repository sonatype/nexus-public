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

/** Illustrative only: the single evaluation the prototype shows. */
export interface EvaluationFixture {
  evaluationRun: string;
  criticalCount: number;
  highCount: number;
  summary: string;
  type: string;
  affectedPackages: number;
  affectedRepositories: number;
}

export const EVALUATION_FIXTURE: EvaluationFixture = {
  evaluationRun: 'Oct 13, 2026, 6:00 AM',
  criticalCount: 4,
  highCount: 8,
  summary: '12 critical or high CVEs across 4 ecosystems',
  type: 'Scheduled',
  affectedPackages: 12,
  affectedRepositories: 6,
};
