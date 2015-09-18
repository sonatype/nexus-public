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
package org.sonatype.nexus.orient;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Remote {@link DatabaseManager} implementation.
 *
 * @since 3.0
 */
//@Named("remote")
//@Singleton
public class RemoteDatabaseManager
    extends DatabaseManagerSupport
{
  private final String hostname;

  private final Integer port;

  public RemoteDatabaseManager(final String hostname, final @Nullable Integer port) {
    this.hostname = checkNotNull(hostname);
    this.port = port;
    log.debug("Hostname: {}", hostname);
    log.debug("Port: {}", port);
  }

  public String getHostname() {
    return hostname;
  }

  @Nullable
  public Integer getPort() {
    return port;
  }

  @Override
  protected String connectionUri(final String name) {
    StringBuilder buff = new StringBuilder();
    buff.append("remote:").append(hostname);
    if (port != null) {
      buff.append(":").append(port);
    }
    buff.append("/").append(name);
    return buff.toString();
  }
}
