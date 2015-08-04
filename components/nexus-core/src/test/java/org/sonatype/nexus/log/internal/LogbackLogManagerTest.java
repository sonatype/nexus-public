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
package org.sonatype.nexus.log.internal;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.sonatype.nexus.NexusAppTestSupport;
import org.sonatype.nexus.log.DefaultLogConfiguration;
import org.sonatype.nexus.log.LogConfiguration;
import org.sonatype.nexus.log.LogManager;

import org.codehaus.plexus.context.Context;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author juven
 * @author adreghiciu@gmail.com
 */
@SuppressWarnings("unused")
public class LogbackLogManagerTest
    extends NexusAppTestSupport
{
  private LogManager manager;

  private org.codehaus.plexus.logging.Logger logger;

  @Override
  public void setUp()
      throws Exception
  {
    super.setUp();

    manager = lookup(LogManager.class);
    manager.configure();
    logger = this.getLoggerManager().getLoggerForComponent(LogbackLogManagerTest.class.getName());
  }

  @Override
  protected void customizeContext(Context ctx) {
    super.customizeContext(ctx);

    try {
      System.setProperty("plexus." + WORK_CONFIGURATION_KEY, (String) ctx.get(WORK_CONFIGURATION_KEY));
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testLogConfig()
      throws Exception
  {
    LogConfiguration config1 = manager.getConfiguration();
    assertThat(config1.getRootLoggerLevel(), is("INFO"));

    DefaultLogConfiguration config2 = new DefaultLogConfiguration(config1);
    config2.setRootLoggerLevel("DEBUG");

    manager.setConfiguration(config2);

    LogConfiguration config3 = manager.getConfiguration();
    assertThat(config3.getRootLoggerLevel(), is("DEBUG"));

    // TODO check that is actually logging at debug level
  }

  @Test
  public void testGetLogFiles()
      throws Exception
  {
    Set<File> logFiles = manager.getLogFiles();
    assertThat("Logfiles set is not null", logFiles, is(notNullValue()));
    assertThat("Logfiles set contains elements", logFiles.size(), is(equalTo(1)));
  }

  @Test
  public void testConfigDefaultAppender()
      throws IOException
  {
    final DefaultLogConfiguration configuration = (DefaultLogConfiguration) manager.getConfiguration();
    configuration.setFileAppenderPattern(null);
    manager.setConfiguration(configuration);
    assertThat(manager.getConfiguration().getFileAppenderPattern(), notNullValue());
  }

}
