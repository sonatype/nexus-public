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

import java.util.List;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.app.FrozenException;
import org.sonatype.nexus.datastore.api.DataAccessException;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.session.defaults.DefaultSqlSession;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionFactory;

/**
 * {@link SqlSession} that avoids MyBatis specific exceptions from leaking back to clients.
 *
 * It unwraps {@link PersistenceException} to get the original cause and message and then
 * re-wraps them with {@link DataAccessException}. Some application state exceptions like
 * {@link FrozenException} thrown by our {@link EntityExecutor} are propagated unchanged.
 *
 * @since 3.26
 */
final class DataAccessSqlSession
    extends DefaultSqlSession
{
  public DataAccessSqlSession(final Configuration configuration) {
    this(configuration, null);
  }

  public DataAccessSqlSession(final Configuration configuration, final TransactionIsolationLevel isolationLevel) {
    super(configuration, newExecutor(configuration, isolationLevel));
  }

  @Override
  public void select(
      final String statement,
      final Object parameter,
      final RowBounds rowBounds,
      final ResultHandler handler)
  {
    try {
      super.select(statement, parameter, rowBounds, handler);
    }
    catch (PersistenceException e) {
      throw unwrapMyBatisException(e);
    }
  }

  @Override
  public <E> List<E> selectList(final String statement, final Object parameter, final RowBounds rowBounds) {
    try {
      return super.selectList(statement, parameter, rowBounds);
    }
    catch (PersistenceException e) {
      throw unwrapMyBatisException(e);
    }
  }

  @Override
  public int update(final String statement, final Object parameter) {
    try {
      return super.update(statement, parameter);
    }
    catch (PersistenceException e) {
      throw unwrapMyBatisException(e);
    }
  }

  @Override
  public void commit(final boolean force) {
    try {
      super.commit(force);
    }
    catch (PersistenceException e) {
      throw unwrapMyBatisException(e);
    }
  }

  @Override
  public void rollback(final boolean force) {
    try {
      super.rollback(force);
    }
    catch (PersistenceException e) {
      throw unwrapMyBatisException(e);
    }
  }

  /**
   * Unwrap or convert MyBatis exceptions to avoid clients needing a dependency on MyBatis.
   */
  private static RuntimeException unwrapMyBatisException(final PersistenceException e) {
    Throwable cause = e.getCause();

    // unwrap these special informational exceptions
    if (cause instanceof DataAccessException) {
      return (DataAccessException) cause;
    }
    else if (cause instanceof FrozenException) {
      return (FrozenException) cause;
    }

    // swap MyBatis exception wrapper with our generic one
    return new DataAccessException(e.getMessage(), cause);
  }

  /**
   * Creates a new session {@link Executor} without auto-commit, using the specified isolation level.
   */
  private static Executor newExecutor(
      final Configuration configuration,
      @Nullable TransactionIsolationLevel isolationLevel)
  {
    Transaction tx = null;
    try {
      Environment environment = configuration.getEnvironment();
      TransactionFactory txFactory = environment.getTransactionFactory();
      tx = txFactory.newTransaction(environment.getDataSource(), isolationLevel, false);
      return configuration.newExecutor(tx);
    }
    catch (Exception e) {
      closeQuietly(tx);

      // include MyBatis context about the connection error
      ErrorContext context = ErrorContext.instance().message("Could not connect to database").cause(e);
      throw new DataAccessException(context.toString(), e);
    }
    finally {
      ErrorContext.instance().reset();
    }
  }

  /**
   * Closes the transaction, ignoring any exceptions that might happen during the close.
   */
  private static void closeQuietly(final Transaction tx) {
    if (tx != null) {
      try {
        tx.close();
      }
      catch (Exception ignore) {
        // NOSONAR: deliberately ignored
      }
    }
  }
}
