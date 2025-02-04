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
package org.sonatype.nexus.blobstore.api.tasks;

/**
 * Shared constants for reconcile tasks
 */
public final class ReconcileTaskConstants
{
  public static final String PLAN_RECONCILE_TYPE_ID = "blobstore.planReconciliation";

  public static final String EXECUTE_RECONCILE_PLAN_TYPE_ID = "blobstore.executeReconciliationPlan";

  public static final String UNABLE_TO_RUN_TASK_LOG_ERROR = "Unable to run task {}";

  private ReconcileTaskConstants() {
  }
}
