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
package org.sonatype.nexus.webapp.jetty;

import javax.inject.Named;

import org.sonatype.nexus.bootstrap.jetty.JettyServer;

import com.google.inject.AbstractModule;

/**
 * Jetty module.
 *
 * @since 2.13.1
 */
@Named
public class JettyModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    // HACK: not very nice thing to use static member here, should find better way to pass over server
    // instance from bootstrap to this module to have it installed.
    bind(JettyServer.class).toInstance(JettyServer.jettyServer);
  }
}
