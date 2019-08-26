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
package org.sonatype.nexus.datastore.mybatis;

import org.sonatype.nexus.datastore.api.DataAccess;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.transaction.RetryController;
import org.sonatype.nexus.transaction.Transaction;

import org.apache.ibatis.session.SqlSession;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * MyBatis {@link DataSession}.
 *
 * @since 3.next
 */
public class MyBatisDataSession
    implements DataSession<Transaction>, Transaction
{
  private final SqlSession session;

  private boolean active = false;

  private int retries = 0;

  public MyBatisDataSession(final SqlSession session) {
    this.session = checkNotNull(session);
  }

  @Override
  public <D extends DataAccess> D access(final Class<D> type) {
    return session.getMapper(type);
  }

  @Override
  public Transaction getTransaction() {
    return this;
  }

  @Override
  public void close() {
    session.close();
  }

  @Override
  public void begin() {
    checkState(!active, "Nested transaction");
    active = true;
  }

  @Override
  public void commit() {
    retries = 0;
    active = false;
    session.commit();
  }

  @Override
  public void rollback() {
    active = false;
    session.rollback();
  }

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
      return false;
    }
  }
}
