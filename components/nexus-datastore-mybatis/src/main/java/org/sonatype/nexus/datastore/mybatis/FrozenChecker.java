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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.FrozenException;
import org.sonatype.nexus.datastore.api.FreezeImmuneDAO;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.ibatis.mapping.SqlCommandType.SELECT;

/**
 * Used by {@link EntityExecutor} to verify whether a {@link MappedStatement} is allowed to continue given the systems
 * freeze state.
 */
class FrozenChecker
    extends ComponentSupport
{
  private final AtomicBoolean frozenMarker;

  private final ClassLoader classLoader;

  private final ConcurrentMap<String, Boolean> freezeImmuneCache = new ConcurrentHashMap<>();

  public FrozenChecker(final AtomicBoolean frozenMarker, @Named("nexus-uber") final ClassLoader classLoader) {
    this.frozenMarker = checkNotNull(frozenMarker);
    this.classLoader = checkNotNull(classLoader);
  }

  void checkFrozen(final MappedStatement ms) {
    SqlCommandType commandType = ms.getSqlCommandType();
    if (commandType != SELECT && frozenMarker.get() && !isFreezeImmune(ms)) {
      log.debug("Disallowing {} because the application is frozen", commandType);
      throw new FrozenException(commandType + " is not allowed while the application is frozen");
    }
  }

  private boolean isFreezeImmune(final MappedStatement ms) {
    return freezeImmuneCache.computeIfAbsent(toKey(ms.getId()), this::isFreezeImmune);
  }

  /*
   * Checks whether a class specified by the className implements FreezeImmuneDAO thus should not be blocked from
   * writing while in a read-only state.
   */
  private Boolean isFreezeImmune(final String className) {
    try {
      Class<?> dao = classLoader.loadClass(className);
      return FreezeImmuneDAO.class.isAssignableFrom(dao);
    }
    catch (ClassNotFoundException e) {
      log.debug("Unable to locate DAO {}", className, e);
      return false;
    }
  }

  /*
   * The ID provided by the MappedStatement is for the query method rather than the DAO itself, this method removes
   * the last segment of the ID leaving the DAO
   */
  private static String toKey(final String statementId) {
    int lastPeriod = statementId.lastIndexOf('.');

    if (lastPeriod > 0) {
      return statementId.substring(0, lastPeriod);
    }

    return statementId;
  }
}
