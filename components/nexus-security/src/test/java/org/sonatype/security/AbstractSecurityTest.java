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
package org.sonatype.security;

import java.io.File;
import java.util.Properties;

import org.sonatype.security.guice.SecurityModule;

import com.google.inject.Binder;
import net.sf.ehcache.CacheManager;
import org.apache.commons.io.FileUtils;
import org.apache.shiro.util.ThreadContext;
import org.eclipse.sisu.launch.InjectedTestCase;
import org.eclipse.sisu.space.BeanScanning;

public abstract class AbstractSecurityTest
    extends InjectedTestCase
{

  protected File PLEXUS_HOME = new File("./target/plexus-home/");

  protected File APP_CONF = new File(PLEXUS_HOME, "conf");

  @Override
  public void configure(Properties properties) {
    properties.put("application-conf", APP_CONF.getAbsolutePath());
    super.configure(properties);
  }

  @Override
  public void configure(final Binder binder) {
    binder.install(new SecurityModule());
  }

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();

    // delete the plexus home dir
    FileUtils.deleteDirectory(PLEXUS_HOME);

    getSecuritySystem().start();
  }

  @Override
  protected void tearDown()
      throws Exception
  {
    try {
      getSecuritySystem().stop();
      lookup(CacheManager.class).shutdown();
    }
    finally {
      ThreadContext.remove();
      super.tearDown();
    }
  }

  @Override
  public BeanScanning scanning() {
    return BeanScanning.INDEX;
  }

  protected SecuritySystem getSecuritySystem()
      throws Exception
  {
    return this.lookup(SecuritySystem.class);
  }
}
