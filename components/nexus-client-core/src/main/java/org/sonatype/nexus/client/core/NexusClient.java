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
package org.sonatype.nexus.client.core;

import org.sonatype.nexus.client.rest.ConnectionInfo;

/**
 * A simple high level Nexus client.
 *
 * @since 2.1
 */
public interface NexusClient
{

  /**
   * Returns Nexus Status in a moment this client connected to it. This will be a cached instance fetched when client
   * was instantiated.
   */
  NexusStatus getNexusStatus();

  /**
   * Returns the current (freshly fetched) Nexus Status.
   */
  NexusStatus getStatus();

  /**
   * Returns the {@link ConnectionInfo} that this client uses.
   */
  ConnectionInfo getConnectionInfo();

  /**
   * Returns a subsystem for given type. Never returns {@code null}. If subsystem not available (for any reason, not
   * configured or remote Nexus does not satisfies it's {@link Condition}), this method throws
   * {@link IllegalArgumentException}. Implementation detail: Subsystem instances are created lazily, hence once you
   * made a successful call to this method, and it did return an instance, that instance will be cached and any
   * subsequent call will return the same instance.
   *
   * @return the subsystem of given type, never {@code null}.
   * @throws IllegalArgumentException if the asked subsystem type is not available for any reason. The exception
   *                                  message explains why subsystem is not available.
   */
  <S> S getSubsystem(Class<S> subsystemType)
      throws IllegalArgumentException;

  /**
   * Disposes the client. After this call, this instance should not be used anymore.
   */
  void close();
}
