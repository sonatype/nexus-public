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

/**
 * {@link Transaction} that stays open until the batch is complete.
 *
 * @since 3.0
 */
final class BatchTransaction
    implements Transaction
{
  final Transaction delegate;

  BatchTransaction(final Transaction delegate) {
    this.delegate = checkNotNull(delegate);
  }

  @Override
  public void begin() throws Exception {
    delegate.begin();
  }

  @Override
  public void commit() throws Exception {
    delegate.commit();
  }

  @Override
  public void rollback() throws Exception {
    delegate.rollback();
  }

  @Override
  public boolean isActive() {
    return delegate.isActive();
  }

  @Override
  public boolean allowRetry(final Exception cause) {
    return delegate.allowRetry(cause);
  }

  @Override
  public void close() throws Exception {
    // no-op
  }

  public void closeBatch() throws Exception {
    delegate.close();
  }
}
