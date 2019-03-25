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
package org.sonatype.nexus.orient.testsupport.internal;

import java.util.Collections;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.orient.DatabaseServer;

import com.orientechnologies.orient.core.OConstants;
import com.orientechnologies.orient.core.Orient;

/**
 * Minimal {@link DatabaseServer}.
 *
 * @since 3.1
 */
@Named("minimal")
@Singleton
public class MinimalDatabaseServer
    extends LifecycleSupport
    implements DatabaseServer
{
  public MinimalDatabaseServer() {
    log.info("OrientDB version: {}", OConstants.getVersion());
  }

  @Override
  protected void doStart() throws Exception {
    Orient.instance().startup();
    Orient.instance().removeShutdownHook();
  }

  @Override
  protected void doStop() throws Exception {
    Orient.instance().shutdown();
  }

  @Override
  public List<String> databases() {
    return Collections.emptyList();
  }

  /**
   * Stop underlying server outside of normal lifecycle in order to test failure situations.
   */
  public void stopAbnormally() {
    Orient.instance().shutdown();
  }
}
