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

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Invocation;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * MyBatis {@link Interceptor} that wraps any new {@link Executor} with {@link EntityExecutor}.
 *
 * @since 3.19
 */
final class EntityInterceptor
    implements Interceptor
{
  private final FrozenChecker frozenChecker;

  public EntityInterceptor(final FrozenChecker frozenChecker) {
    this.frozenChecker = checkNotNull(frozenChecker);
  }

  @Override
  public Object plugin(final Object delegate) {
    if (delegate instanceof Executor) {
      return new EntityExecutor((Executor) delegate, frozenChecker);
    }
    return delegate;
  }

  @Override
  public Object intercept(final Invocation invocation) {
    throw new UnsupportedOperationException("unused");
  }
}
