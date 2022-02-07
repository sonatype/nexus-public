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

import org.sonatype.nexus.datastore.api.DataAccess;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.transaction.Transaction;
import org.sonatype.nexus.transaction.TransactionSupport;

import org.apache.ibatis.session.SqlSession;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

/**
 * MyBatis {@link DataSession}.
 *
 * @since 3.19
 */
public final class MyBatisDataSession
    extends TransactionSupport
    implements DataSession<Transaction>
{
  private final SqlSession session;

  private Actions actions = new Actions();

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
  public void preCommit(final Runnable hook) {
    actions.preCommit = addHook(actions.preCommit, hook);
  }

  @Override
  public void postCommit(final Runnable hook) {
    actions.postCommit = addHook(actions.postCommit, hook);
  }

  @Override
  public void onRollback(final Runnable hook) {
    actions.onRollback = addHook(actions.onRollback, hook);
  }

  @Override
  public String sqlDialect() {
    return session.getConfiguration().getDatabaseId();
  }

  @Override
  protected void doCommit() {
    callHooks(actions.preCommit);
    session.commit();
    callHooks(actions.postCommit);
  }

  @Override
  protected void doRollback() {
    session.rollback();
    callHooks(actions.onRollback);
  }

  @Override
  public boolean allowRetry(final Exception cause) {
    boolean retry = super.allowRetry(cause);
    if (retry) {
      clearHooks(); // avoid duplicate hooks from stacking up on retry
    }
    return retry;
  }

  @Override
  public void close() {
    clearHooks();
    session.close();
  }

  @SuppressWarnings("unchecked")
  private Object addHook(final Object hooks, final Runnable hook) {
    if (hooks == null) {
      return hook;
    }
    else if (hooks instanceof Runnable) {
      return newArrayList(hooks, hook);
    }
    else {
      ((List<Runnable>) hooks).add(hook);
      return hooks;
    }
  }

  @SuppressWarnings("unchecked")
  private void callHooks(final Object hooks) {
    if (hooks == null) {
      // nothing to call
      return;
    }

    Actions existing = actions;
    try {
      // We unset the actions here in case the hook makes a nested DB call.
      actions = new Actions();

      if (hooks instanceof Runnable) {
        ((Runnable) hooks).run();
      }
      else {
        ((List<Runnable>) hooks).forEach(Runnable::run);
      }
    }
    finally {
      actions = existing;
    }
  }

  private void clearHooks() {
    actions.preCommit = null;
    actions.postCommit = null;
    actions.onRollback = null;
  }

  private static class Actions {
    Object preCommit;

    Object postCommit;

    Object onRollback;
  }
}
