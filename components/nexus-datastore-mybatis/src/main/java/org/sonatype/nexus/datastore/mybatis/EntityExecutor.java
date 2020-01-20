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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.app.FrozenException;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityUUID;
import org.sonatype.nexus.common.entity.HasEntityId;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.apache.ibatis.mapping.SqlCommandType.INSERT;
import static org.apache.ibatis.mapping.SqlCommandType.SELECT;
import static org.sonatype.nexus.datastore.mybatis.CombUUID.combUUID;

/**
 * MyBatis {@link Executor} wrapper that automatically generates {@link EntityId}s for new entities.
 *
 * @since 3.19
 */
final class EntityExecutor
    implements Executor
{
  private static final Logger log = LoggerFactory.getLogger(EntityExecutor.class);

  private final Executor delegate;

  private final AtomicBoolean frozen;

  @Nullable
  private List<HasEntityId> generatedEntityIds;

  public EntityExecutor(final Executor delegate, final AtomicBoolean frozen) {
    this.delegate = checkNotNull(delegate);
    this.frozen = checkNotNull(frozen);
  }

  @Override
  public int update(final MappedStatement ms, final Object parameter) throws SQLException {
    SqlCommandType commandType = ms.getSqlCommandType();
    if (commandType != SELECT && frozen.get()) {
      log.debug("Disallowing {} because the application is frozen", commandType);
      throw new FrozenException(commandType + " is not allowed while the application is frozen");
    }
    if (commandType == INSERT && parameter instanceof HasEntityId) {
      generateEntityId((HasEntityId) parameter);
    }
    return delegate.update(ms, parameter);
  }

  @Override
  public <E> List<E> query(final MappedStatement ms,
                           final Object parameter,
                           final RowBounds rowBounds,
                           final ResultHandler resultHandler,
                           final CacheKey cacheKey,
                           final BoundSql boundSql)
      throws SQLException
  {
    return delegate.query(ms, parameter, rowBounds, resultHandler, cacheKey, boundSql);
  }

  @Override
  public <E> List<E> query(final MappedStatement ms,
                           final Object parameter,
                           final RowBounds rowBounds,
                           final ResultHandler resultHandler)
      throws SQLException
  {
    return delegate.query(ms, parameter, rowBounds, resultHandler);
  }

  @Override
  public <E> Cursor<E> queryCursor(final MappedStatement ms,
                                   final Object parameter,
                                   final RowBounds rowBounds)
      throws SQLException
  {
    return delegate.queryCursor(ms, parameter, rowBounds);
  }

  @Override
  public List<BatchResult> flushStatements() throws SQLException {
    return delegate.flushStatements();
  }

  @Override
  public void commit(final boolean required) throws SQLException {
    try {
      delegate.commit(required);
      commitEntityIds();
    }
    catch (RuntimeException | SQLException | Error e) {
      rollbackEntityIds();
      throw e;
    }
  }

  @Override
  public void rollback(final boolean required) throws SQLException {
    rollbackEntityIds();
    delegate.rollback(required);
  }

  @Override
  public CacheKey createCacheKey(final MappedStatement ms,
                                 final Object parameterObject,
                                 final RowBounds rowBounds,
                                 final BoundSql boundSql)
  {
    return delegate.createCacheKey(ms, parameterObject, rowBounds, boundSql);
  }

  @Override
  public boolean isCached(final MappedStatement ms, final CacheKey key) {
    return delegate.isCached(ms, key);
  }

  @Override
  public void clearLocalCache() {
    delegate.clearLocalCache();
  }

  @Override
  public void deferLoad(final MappedStatement ms,
                        final MetaObject resultObject,
                        final String property,
                        final CacheKey key,
                        final Class<?> targetType)
  {
    delegate.deferLoad(ms, resultObject, property, key, targetType);
  }

  @Override
  public Transaction getTransaction() {
    return delegate.getTransaction();
  }

  @Override
  public void close(final boolean forceRollback) {
    if (forceRollback) {
      rollbackEntityIds();
    }
    delegate.close(forceRollback);
  }

  @Override
  public boolean isClosed() {
    return delegate.isClosed();
  }

  @Override
  public void setExecutorWrapper(final Executor executor) {
    delegate.setExecutorWrapper(executor);
  }

  /**
   * Generates a new {@link EntityId} for the given entity.
   */
  private void generateEntityId(final HasEntityId entity) {
    checkState(entity.getId() == null, "Entity already has an id");
    entity.setId(new EntityUUID(combUUID()));
    if (generatedEntityIds == null) {
      generatedEntityIds = new ArrayList<>();
    }
    generatedEntityIds.add(entity);
  }

  /**
   * Commit all generated {@link EntityId}s in the current session.
   *
   * Because they are already set this just involves resetting the tracking.
   */
  private void commitEntityIds() {
    generatedEntityIds = null;
  }

  /**
   * Rollback all generated {@link EntityId}s in the current session.
   *
   * This involves clearing the ids of all entities created during the session.
   */
  private void rollbackEntityIds() {
    if (generatedEntityIds != null) {
      generatedEntityIds.forEach(HasEntityId::clearId);
      generatedEntityIds = null;
    }
  }
}
