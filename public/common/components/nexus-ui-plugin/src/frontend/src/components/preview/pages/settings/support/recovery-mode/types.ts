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

/**
 * A reconcile task as returned by the recovery-mode UI endpoint (TaskXO subset).
 */
export interface ReconcileTask {
  id: string;
  /** Task display name, e.g. "Repair - Data Repair Plan". */
  name: string;
  /** Task type id, e.g. "blobstore.planReconciliation". */
  type: string;
  /** Current run state, e.g. "WAITING", "RUNNING", "RUNNING: 45%". */
  currentState?: string | null;
  /** Last run start time (ISO string), or null if never run. */
  lastRun?: string | null;
  /** Result of the last run, e.g. "OK", or null if never run. */
  lastRunResult?: string | null;
}

/**
 * Response shape of GET service/rest/internal/ui/recovery-mode.
 */
export interface RecoveryModeData {
  enabled: boolean;
  /** True when one or more data repair plans have not been executed. */
  unexecutedPlans: boolean;
  /**
   * Task type names blocked while recovery mode is active. Returned by the
   * endpoint but intentionally not surfaced in the page (parity with the
   * Classic UI, which also does not display it).
   */
  blockedTaskNames: string[];
  /** Data repair (reconcile) tasks shown in the table. */
  reconcileTasks: ReconcileTask[];
}
