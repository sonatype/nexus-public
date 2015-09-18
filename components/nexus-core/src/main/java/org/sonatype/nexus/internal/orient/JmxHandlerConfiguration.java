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
package org.sonatype.nexus.internal.orient;

import javax.inject.Named;
import javax.inject.Singleton;

import com.orientechnologies.orient.server.config.OServerHandlerConfiguration;
import com.orientechnologies.orient.server.config.OServerParameterConfiguration;
import com.orientechnologies.orient.server.handler.OJMXPlugin;

/**
 * Enables the JMX plugin on the OrientDB server.
 *
 * @since 3.0
 */
@Named
@Singleton
public class JmxHandlerConfiguration
    extends OServerHandlerConfiguration
{
  public JmxHandlerConfiguration() {
    clazz = OJMXPlugin.class.getName();
    parameters = new OServerParameterConfiguration[] {
        new OServerParameterConfiguration("enabled", "true"),
        new OServerParameterConfiguration("profilerManaged", "true")
    };
  }
}