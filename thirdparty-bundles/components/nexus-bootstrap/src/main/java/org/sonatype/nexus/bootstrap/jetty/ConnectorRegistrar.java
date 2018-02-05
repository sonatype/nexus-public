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

import java.util.List;

import org.eclipse.jetty.http.HttpScheme;

/**
 * Connector registrar component.
 *
 * @since 3.0
 */
public interface ConnectorRegistrar
{
  /**
   * Returns the list of available HTTP schemes. This list depends on current Jetty XML configuration (is HTTPS
   * enabled). If schema is not available, method {@link #addConnector(ConnectorConfiguration)} will reject to add it.
   */
  List<HttpScheme> availableSchemes();

  /**
   * Returns the list of already occupied ports. If configuration port is occupied, method {@link
   * #addConnector(ConnectorConfiguration)} will reject to add configuration.
   */
  List<Integer> unavailablePorts();

  /**
   * Registers a new Jetty {@link ConnectorConfiguration}, that creates a new connector on Jetty based on passed
   * in configuration.
   *
   * @throws UnsupportedHttpSchemeException if requested HTTP schema is not available.
   * @throws IllegalArgumentException       requested port is not available, if current connectorConfiguration instance
   *                                        was already added
   */
  void addConnector(ConnectorConfiguration connectorConfiguration);

  /**
   * Removes a Jetty {@link ConnectorConfiguration} by closing and removing the Jetty connector. Caller MUST use the
   * same instance of {@link ConnectorConfiguration} that was used with {@link #addConnector(ConnectorConfiguration)}
   * method to create a connector in the first place.
   */
  void removeConnector(ConnectorConfiguration connectorConfiguration);
}
