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
package org.sonatype.nexus.internal.orient;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.transaction.RetryController;

import com.codahale.metrics.health.HealthCheck;

import static com.codahale.metrics.health.HealthCheck.Result.healthy;
import static com.codahale.metrics.health.HealthCheck.Result.unhealthy;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Inform on the health of transactions; reports if any have an excessive number of retries.
 *
 * @since 3.next
 */
@Named("Transactions")
@Singleton
public class TransactionHealthCheck
    extends HealthCheck
{
  private final RetryController retryController;

  @Inject
  public TransactionHealthCheck(final RetryController retryController) {
    this.retryController = checkNotNull(retryController);
  }

  @Override
  protected Result check() {
    long excessiveRetriesInLastHour = retryController.excessiveRetriesInLastHour();
    if (excessiveRetriesInLastHour > 0) {
      return unhealthy("%d transaction%s with excessive retries in last hour",
          excessiveRetriesInLastHour, excessiveRetriesInLastHour == 1 ? "" : "s");
    }
    else {
      return healthy();
    }
  }
}
