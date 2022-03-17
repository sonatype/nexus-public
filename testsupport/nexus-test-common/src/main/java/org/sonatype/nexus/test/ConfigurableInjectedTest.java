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
package org.sonatype.nexus.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;

import javax.inject.Inject;

import org.sonatype.nexus.test.util.StopWatch;
import org.sonatype.sisu.goodies.testsupport.TestUtil;

import org.eclipse.sisu.launch.InjectedTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.slf4j.Logger;

public class ConfigurableInjectedTest
    extends InjectedTest
{
  static {
    System.setProperty("guice.disable.misplaced.annotation.check", "true");
    // http://code.google.com/p/guava-libraries/issues/detail?id=92
    System.setProperty("guava.executor.class", "NONE");
    // http://code.google.com/p/google-guice/issues/detail?id=288#c30
    System.setProperty("guice.executor.class", "NONE");
  }

  protected final TestUtil util = new TestUtil(this);

  // HACK: Force user.basedir to the detected directory by TestUtil for better IDE integration for execution of tests
  // HACK: Many tests assume this directory is the maven module directory, when it may not be.
  {
    System.setProperty("user.dir", util.getBaseDir().getAbsolutePath());
  }

  @Rule
  public TestName testName = new TestName();

  // FIXME: Avoid injected loggers!
  @Inject
  private Logger log;

  private StopWatch stopWatch;

  @Override
  public void configure(final Properties properties) {
    loadAll(properties, "injected-test.properties");
    // per test class properties
    load(properties, this.getClass().getSimpleName() + "/injected-test.properties");
    super.configure(properties);
  }


  private void loadAll(final Properties properties, final String name) {
    try {
      final Enumeration<URL> resources = getClass().getClassLoader().getResources(name);
      while (resources.hasMoreElements()) {
        final URL resource = resources.nextElement();
        load(properties, resource);
      }
    }
    catch (final IOException e) {
      throw new IllegalStateException("Failed to load " + name, e);
    }
  }

  private void load(final Properties properties, final String name) {
    load(properties, getClass().getResource(name));
  }

  private void load(final Properties properties, final URL url) {
    if (url != null) {
      try (InputStream in = url.openStream()) {
        if (in != null) {
          properties.load(in);
        }
        properties.putAll(System.getProperties());
      }
      catch (final IOException e) {
        throw new IllegalStateException("Failed to load " + url.toExternalForm(), e);
      }
    }
  }

  /**
   * Setup test injection and log the start of the test.
   * <p>
   * final to protect against subclasses from forgetting to call super.
   */
  @Override
  @Before
  public final void setUp() throws Exception {
    System.out.println("setUp method");
    stopWatch = new StopWatch();
    super.setUp();
    final String info = String.format("Running test %s", testName.getMethodName());
    log.info(fill(info.length(), '*'));
    log.info(info);
    log.info(fill(info.length(), '*'));
  }

  /**
   * Tear down injection and log the end of the test.
   */
  @Override
  @After
  public final void tearDown() throws Exception {
    super.tearDown();
    final String info = String.format("Running test %s took %s", testName.getMethodName(), stopWatch);
    log.info(fill(info.length(), '*'));
    log.info(info);
    log.info(fill(info.length(), '*'));
    stopWatch = null;
  }

  private String fill(final int length, final char fillWith) {
    final char[] fill = new char[length];
    Arrays.fill(fill, fillWith);
    final String result = new String(fill);
    return result;
  }
}
