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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.sonatype.sisu.litmus.testsupport.TestUtil;

import com.google.common.collect.Lists;
import com.google.inject.Module;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.DefaultContext;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.LoggerManager;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

//
// FIXME: This is a shitty class and should not exist... @Deprecate and trash this junk!
//

/**
 * A Support PlexusTestCase clone that does not extend JUnit TestCase, thereby allowing us to extend this class like we
 * did with JUnit 3x and use JUnit 4x annotations instead to design our tests.
 * <p/>
 * This source is meant to be a near copy of the original {@link org.codehaus.plexus.PlexusTestCase}, sisu-2.1.1
 * <p/>
 * The supporting asserts derived from JUnit's Assert class are deprecated here to encourage use of the more modern
 * alternative Hamcrest libraries.
 * <p/>
 * TODO: integrate this directly with sisu-inject-plexus
 */
public abstract class PlexusTestCaseSupport
{
  protected final TestUtil util = new TestUtil(this);

  // HACK: Force user.basedir to the detected directory by TestUtil for better IDE integration for execution of tests
  // HACK: Many tests assume this directory is the maven module directory, when it may not be.
  {
    System.setProperty("user.dir", util.getBaseDir().getAbsolutePath());
  }

  private PlexusContainer container;

  private Properties sysPropsBackup;

  protected void setUp() throws Exception {
    sysPropsBackup = System.getProperties();
  }

  protected void setupContainer() {
    // ----------------------------------------------------------------------------
    // Context Setup
    // ----------------------------------------------------------------------------

    final DefaultContext context = new DefaultContext();

    context.put("basedir", getBasedir());

    customizeContext(context);

    final boolean hasPlexusHome = context.contains("plexus.home");

    if (!hasPlexusHome) {
      final File f = getTestFile("target/plexus-home");

      if (!f.isDirectory()) {
        f.mkdir();
      }

      context.put("plexus.home", f.getAbsolutePath());
    }

    // ----------------------------------------------------------------------------
    // Configuration
    // ----------------------------------------------------------------------------

    final String config = getCustomConfigurationName();

    final ContainerConfiguration containerConfiguration =
        new DefaultContainerConfiguration().setName("test").setContext(context.getContextData());

    if (config != null) {
      containerConfiguration.setContainerConfiguration(config);
    }
    else {
      final String resource = getConfigurationName(null);

      containerConfiguration.setContainerConfiguration(resource);
    }

    customizeContainerConfiguration(containerConfiguration);

    try {
      List<Module> modules = Lists.newLinkedList();
      Module[] customModules = getTestCustomModules();
      if (customModules != null) {
        modules.addAll(Lists.newArrayList(customModules));
      }
      customizeModules(modules);

      container = new DefaultPlexusContainer(containerConfiguration, modules.toArray(new Module[modules.size()]));
    }
    catch (final PlexusContainerException e) {
      e.printStackTrace();
      fail("Failed to create plexus container.");
    }
  }

  /**
   * @deprecated Use {@link #customizeModules(List)} instead.
   */
  @Deprecated
  protected Module[] getTestCustomModules() {
    return null;
  }

  protected void customizeModules(final List<Module> modules) {
    // empty
  }

  /**
   * @deprecated Avoid usage of Plexus apis.
   */
  @Deprecated
  protected void customizeContainerConfiguration(final ContainerConfiguration containerConfiguration) {
    // empty
  }

  /**
   * @deprecated Avoid usage of Plexus apis.
   */
  @Deprecated
  protected void customizeContext(final Context context) {
    // empty
  }

  protected void tearDown() throws Exception {
    try {
      if (container != null) {
        container.dispose();

        container = null;
      }
    }
    finally {
      System.setProperties(sysPropsBackup);
    }
  }

  /**
   * @deprecated Avoid usage of Plexus apis.
   */
  @Deprecated
  protected PlexusContainer getContainer() {
    if (container == null) {
      setupContainer();
    }

    return container;
  }

  /**
   * @deprecated Avoid usage of Plexus apis (this is used to access plexus xml configuration files).
   */
  @Deprecated
  protected InputStream getConfiguration() throws Exception {
    return getConfiguration(null);
  }

  /**
   * @deprecated Avoid usage of Plexus apis (this is used to access plexus xml configuration files).
   */
  @Deprecated
  protected InputStream getConfiguration(final String subname) throws Exception {
    return getResourceAsStream(getConfigurationName(subname));
  }

  /**
   * @deprecated Avoid usage of Plexus apis (this is used to access plexus xml configuration files).
   */
  @Deprecated
  protected String getCustomConfigurationName() {
    return null;
  }

  /**
   * @deprecated Avoid usage of Plexus apis (this is used to access plexus xml configuration files).
   */
  @Deprecated
  protected String getConfigurationName(final String subname) {
    return getClass().getName().replace('.', '/') + ".xml";
  }

  protected InputStream getResourceAsStream(final String resource) {
    return getClass().getResourceAsStream(resource);
  }

  protected ClassLoader getClassLoader() {
    return getClass().getClassLoader();
  }

  // ----------------------------------------------------------------------
  // Container access
  // ----------------------------------------------------------------------

  /**
   * @deprecated Avoid usage of Plexus apis (string role/hint lookup is plexus specific).
   */
  protected Object lookup(final String componentKey) throws Exception {
    return getContainer().lookup(componentKey);
  }

  /**
   * @deprecated Avoid usage of Plexus apis (string role/hint lookup is plexus specific).
   */
  @Deprecated
  protected Object lookup(final String role, final String roleHint) throws Exception {
    return getContainer().lookup(role, roleHint);
  }

  protected <T> T lookup(final Class<T> componentClass) throws Exception {
    return getContainer().lookup(componentClass);
  }

  protected <T> T lookup(final Class<T> componentClass, final String roleHint) throws Exception {
    return getContainer().lookup(componentClass, roleHint);
  }

  /**
   * @deprecated Avoid usage of Plexus apis.
   */
  protected void release(final Object component) throws Exception {
    getContainer().release(component);
  }

  // ----------------------------------------------------------------------
  // Helper methods for sub classes
  // ----------------------------------------------------------------------

  public File getTestFile(final String path) {
    return new File(getBasedir(), path);
  }

  public File getTestFile(final String basedir, final String path) {
    File basedirFile = new File(basedir);

    if (!basedirFile.isAbsolute()) {
      basedirFile = getTestFile(basedir);
    }

    return new File(basedirFile, path);
  }

  public String getBasedir() {
    return util.getBaseDir().getAbsolutePath();
  }

  /**
   * @deprecated Avoid usage of Plexus apis.
   */
  protected LoggerManager getLoggerManager() throws ComponentLookupException {
    LoggerManager loggerManager = getContainer().lookup(LoggerManager.class);
    // system property helps configure logger - see NXCM-3230
    String testLogLevel = System.getProperty("test.log.level");
    if (testLogLevel != null) {
      if (testLogLevel.equalsIgnoreCase("DEBUG")) {
        loggerManager.setThresholds(Logger.LEVEL_DEBUG);
      }
      else if (testLogLevel.equalsIgnoreCase("INFO")) {
        loggerManager.setThresholds(Logger.LEVEL_INFO);
      }
      else if (testLogLevel.equalsIgnoreCase("WARN")) {
        loggerManager.setThresholds(Logger.LEVEL_WARN);
      }
      else if (testLogLevel.equalsIgnoreCase("ERROR")) {
        loggerManager.setThresholds(Logger.LEVEL_ERROR);
      }
    }
    return loggerManager;
  }

  // ========================= CUSTOM NEXUS =====================

  /**
   * Helper to call old JUnit 3x style {@link #setUp()}
   */
  @Before
  final public void setUpJunit() throws Exception {
    setUp();
  }

  /**
   * Helper to call old JUnit 3x style {@link #tearDown()}
   */
  @After
  final public void tearDownJunit() throws Exception {
    tearDown();
  }

  @Deprecated
  protected void fail() {
    Assert.fail();
  }

  @Deprecated
  protected void fail(String message) {
    Assert.fail(message);
  }

  /**
   * @deprecated Use {@link org.hamcrest.MatcherAssert} directly instead.
   */
  @Deprecated
  protected void assertTrue(boolean condition) {
    Assert.assertTrue(condition);
  }

  /**
   * @deprecated Use {@link org.hamcrest.MatcherAssert} directly instead.
   */
  @Deprecated
  protected void assertTrue(String message, boolean condition) {
    Assert.assertTrue(message, condition);
  }

  /**
   * @deprecated Use {@link org.hamcrest.MatcherAssert} directly instead.
   */
  @Deprecated
  protected void assertFalse(boolean condition) {
    Assert.assertFalse(condition);
  }

  /**
   * @deprecated Use {@link org.hamcrest.MatcherAssert} directly instead.
   */
  @Deprecated
  protected void assertFalse(String message, boolean condition) {
    Assert.assertFalse(message, condition);
  }

  /**
   * @deprecated Use {@link org.hamcrest.MatcherAssert} directly instead.
   */
  @Deprecated
  protected void assertNotNull(Object obj) {
    Assert.assertNotNull(obj);
  }

  /**
   * @deprecated Use {@link org.hamcrest.MatcherAssert} directly instead.
   */
  @Deprecated
  protected void assertNotNull(String message, Object obj) {
    Assert.assertNotNull(message, obj);
  }

  /**
   * @deprecated Use {@link org.hamcrest.MatcherAssert} directly instead.
   */
  @Deprecated
  protected void assertNull(Object obj) {
    Assert.assertNull(obj);
  }

  /**
   * @deprecated Use {@link org.hamcrest.MatcherAssert} directly instead.
   */
  @Deprecated
  protected void assertNull(String message, Object obj) {
    Assert.assertNull(message, obj);
  }

  /**
   * @deprecated Use {@link org.hamcrest.MatcherAssert} directly instead.
   */
  @Deprecated
  protected void assertEquals(String message, Object expected, Object actual) {
    // don't use junit framework Assert due to autoboxing bug
    MatcherAssert.assertThat(message, actual, Matchers.equalTo(expected));

  }

  /**
   * @deprecated Use {@link org.hamcrest.MatcherAssert} directly instead.
   */
  @Deprecated
  protected void assertEquals(Object expected, Object actual) {
    // don't use junit framework Assert
    MatcherAssert.assertThat(actual, Matchers.equalTo(expected));
  }

  protected boolean contentEquals(File f1, File f2) throws IOException {
    return contentEquals(new FileInputStream(f1), new FileInputStream(f2));
  }

  /**
   * Both s1 and s2 will be closed.
   */
  protected boolean contentEquals(InputStream s1, InputStream s2) throws IOException {
    try (InputStream in1 = s1;
         InputStream in2 = s2) {
      return IOUtils.contentEquals(in1, in2);
    }
  }
}
