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
package org.sonatype.nexus.bootstrap.jetty;


import javax.annotation.Nullable;

/**
 * Connector configuration should be registered as service by plugin requesting dedicated connectors, and unregistered
 * once the connector is not needed.
 *
 * @since 2.13.1
 */
public class ConnectorConfiguration
{
  public enum Scheme
  {
    HTTP, HTTPS
  }

  private final Scheme scheme;

  private final int port;

  private final RequestCustomizer requestCustomizer;

  public ConnectorConfiguration(final Scheme scheme,
                                final int port,
                                @Nullable final RequestCustomizer requestCustomizer)
  {
    this.scheme = scheme;
    this.port = port;
    this.requestCustomizer = requestCustomizer;
  }

  /**
   * The required connector scheme.
   */
  public Scheme getScheme() {
    return scheme;
  }

  /**
   * The required connector port.
   */
  public int getPort() {
    return port;
  }

  /**
   * If result is not-null, the {@link RequestCustomizer} will be used on given connector.
   */
  @Nullable
  public RequestCustomizer getRequestCustomizer() {
    return requestCustomizer;
  }
}
