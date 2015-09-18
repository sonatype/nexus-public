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
package org.sonatype.nexus.quartz;

import java.io.File;
import java.util.Properties;

import javax.inject.Inject;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.goodies.testsupport.TestUtil;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.app.BaseUrlManager;
import org.sonatype.nexus.quartz.internal.QuartzSupportImpl;
import org.sonatype.nexus.scheduling.TaskScheduler;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.eclipse.sisu.inject.MutableBeanLocator;
import org.eclipse.sisu.space.BeanScanning;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.ParameterKeys;
import org.eclipse.sisu.wire.WireModule;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * IT support: this beast brings up real SISU container and complete Quartz environment.
 */
public abstract class QuartzITSupport
    extends TestSupport
{
  static protected final TestUtil util = new TestUtil(QuartzITSupport.class);

  @Inject
  static protected Injector injector;

  @Inject
  static protected MutableBeanLocator locator;

  @Inject
  static protected TaskScheduler taskScheduler;

  @Inject
  static protected QuartzSupportImpl quartzSupport;

  static protected ApplicationDirectories applicationDirectories;

  static protected BaseUrlManager baseUrlManager;

  @BeforeClass
  public static void prepare() throws Exception {
    applicationDirectories = mock(ApplicationDirectories.class);
    baseUrlManager = mock(BaseUrlManager.class);
    Guice.createInjector(new WireModule(new SetUpModule(),
        new SpaceModule(new URLClassSpace(QuartzITSupport.class.getClassLoader()), BeanScanning.INDEX)));
    quartzSupport.start();
  }

  @AfterClass
  public static void tearDown()
      throws Exception
  {
    quartzSupport.stop();
    locator.clear();
  }

  @Before
  public void inject() {
    injector.injectMembers(this);
  }

  final static class SetUpModule
      implements Module
  {
    public void configure(final Binder binder)
    {
      final Properties properties = new Properties();
      properties.put("basedir", util.getBaseDir());

      final File workDir = util.createTempDir(util.getTargetDir(), "workdir");
      System.out.println("Workdir: " + workDir);
      when(applicationDirectories.getWorkDirectory(anyString())).thenReturn(workDir);
      binder.bind(ApplicationDirectories.class).toInstance(applicationDirectories);

      binder.bind(BaseUrlManager.class).toInstance(baseUrlManager);

      binder.bind(ParameterKeys.PROPERTIES).toInstance(properties);

      binder.requestStaticInjection(QuartzITSupport.class);
    }
  }
}
