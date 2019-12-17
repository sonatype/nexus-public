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
package org.sonatype.nexus.transaction;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.transaction.Transactional.DEFAULT_REASON;

/**
 * Skeleton implementation of a retryable {@link Transaction}:
 * <ul>
 * <li>Tracks the number of failures to decide if the transaction should continue to retry</li>
 * <li>Disallows multiple calls to begin, which indicate incorrect use of the transaction</li>
 * <li>Supports transaction implementations that don't require an explicit call to begin</li>
 * <li>Tracks the reason given for this transaction for support purposes</li>
 * </ul>
 *
 * @since 3.20
 *
 * @see RetryController
 */
public abstract class TransactionSupport
    implements Transaction
{
  private boolean active = false;

  private int retries = 0;

  private String reason = DEFAULT_REASON;

  @Override
  public final void begin() {
    checkState(!active, "Transaction has already begun");
    doBegin();
    active = true;
  }

  protected void doBegin() {
    // nothing to do
  }

  @Override
  public final void commit() {
    active = false;
    doCommit();
    retries = 0;
  }

  protected abstract void doCommit();

  @Override
  public final void rollback() {
    active = false;
    doRollback();
  }

  protected abstract void doRollback();

  @Override
  public boolean isActive() {
    return active;
  }

  @Override
  public boolean allowRetry(final Exception cause) {
    if (RetryController.INSTANCE.allowRetry(retries, cause)) {
      retries++;
      return true;
    }
    else {
      retries = 0;
      return false;
    }
  }

  @Override
  public void reason(final String reason) {
    this.reason = checkNotNull(reason);
  }

  @Override
  public String reason() {
    return reason;
  }
}
