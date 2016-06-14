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

import java.io.IOException;

import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.Request;

/**
 * Jetty {@link Request} customizer.
 *
 * @since 2.13.1
 */
public interface RequestCustomizer
{
  /**
   * Customize a request for an endpoint. Called on every request to allow customization of the request for the
   * particular endpoint (eg security properties from a SSL connection). Allows implementation to customize the default
   * configuration for needed connector or replace it completely.
   */
  void customize(EndPoint endpoint, Request request) throws IOException;
}
