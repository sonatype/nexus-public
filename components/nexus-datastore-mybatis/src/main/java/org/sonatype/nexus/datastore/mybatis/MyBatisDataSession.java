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
import org.sonatype.nexus.transaction.Transaction;
import org.sonatype.nexus.transaction.TransactionSupport;

import org.apache.ibatis.session.SqlSession;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * MyBatis {@link DataSession}.
 *
 * @since 3.19
 */
public class MyBatisDataSession
    extends TransactionSupport
    implements DataSession<Transaction>
{
  private final SqlSession session;

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
  protected void doCommit() {
    session.commit();
  }

  @Override
  protected void doRollback() {
    session.rollback();
  }

  @Override
  public void close() {
    session.close();
  }
}
