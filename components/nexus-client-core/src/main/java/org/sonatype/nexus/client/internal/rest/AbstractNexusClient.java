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
package org.sonatype.nexus.client.internal.rest;

import java.util.LinkedHashMap;

import org.sonatype.nexus.client.core.Condition;
import org.sonatype.nexus.client.core.NexusClient;
import org.sonatype.nexus.client.core.NexusStatus;
import org.sonatype.nexus.client.internal.util.Check;
import org.sonatype.nexus.client.rest.ConnectionInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 2.1
 */
public abstract class AbstractNexusClient
    implements NexusClient
{

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final ConnectionInfo connectionInfo;

  private final LinkedHashMap<Class<?>, Object> subsystemInstanceCache;

  private NexusStatus nexusStatus;

  protected AbstractNexusClient(final ConnectionInfo connectionInfo) {
    this.connectionInfo = Check.notNull(connectionInfo, ConnectionInfo.class);
    this.subsystemInstanceCache = new LinkedHashMap<Class<?>, Object>();
  }

  protected Logger getLogger() {
    return logger;
  }

  @Override
  public NexusStatus getNexusStatus() {
    return nexusStatus;
  }

  @Override
  public ConnectionInfo getConnectionInfo() {
    return connectionInfo;
  }

  @Override
  public synchronized <S> S getSubsystem(Class<S> subsystemType) {
    if (subsystemInstanceCache.containsKey(subsystemType)) {
      return subsystemType.cast(subsystemInstanceCache.get(subsystemType));
    }
    else {
      final S subsystem = createSubsystem(subsystemType);
      subsystemInstanceCache.put(subsystemType, subsystem);
      return subsystem;
    }
  }

  @Override
  public synchronized void close() {
    subsystemInstanceCache.clear();
  }

  // ==

  /**
   * Initializes the connection by matching nexus status against passed in Condition. Classes extending this class
   * have to call this method from their constructor to properly initialize this class!
   *
   * @throws IllegalStateException if remote Nexus does not fulfil requirements posed by passed in {@link Condition}.
   */
  protected void initializeConnection(final Condition connectionCondition) {
    this.nexusStatus = Check.notNull(getStatus(), "Nexus status is null!");
    getLogger().debug("Connected, received {} ", this.nexusStatus);
    if (!connectionCondition.isSatisfiedBy(nexusStatus)) {
      throw new IllegalStateException("Not connecting to remote Nexus, condition(s) are not satisfied: "
          + connectionCondition.explainNotSatisfied(nexusStatus));
    }
  }

  /**
   * Creates an instance of the subsystem for given type.
   */
  protected abstract <S> S createSubsystem(Class<S> subsystemType)
      throws IllegalArgumentException;
}
