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
package org.sonatype.nexus.testsuite.support;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.bundle.launcher.NexusBundle;
import org.sonatype.nexus.bundle.launcher.support.NexusBundleResolver;
import org.sonatype.nexus.bundle.launcher.support.NexusSpecific;
import org.sonatype.nexus.client.core.NexusClient;
import org.sonatype.nexus.client.rest.NexusClientFactory;
import org.sonatype.nexus.client.rest.UsernamePasswordAuthenticationInfo;
import org.sonatype.sisu.bl.support.resolver.BundleResolver;
import org.sonatype.sisu.bl.support.resolver.MavenBridgedBundleResolver;
import org.sonatype.sisu.bl.support.resolver.TargetDirectoryResolver;
import org.sonatype.sisu.filetasks.FileTaskBuilder;
import org.sonatype.sisu.goodies.common.Properties2;
import org.sonatype.sisu.litmus.testsupport.TestData;
import org.sonatype.sisu.litmus.testsupport.TestIndex;
import org.sonatype.sisu.litmus.testsupport.inject.InjectedTestSupport;
import org.sonatype.sisu.litmus.testsupport.junit.TestDataRule;
import org.sonatype.sisu.litmus.testsupport.junit.TestIndexRule;
import org.sonatype.sisu.maven.bridge.MavenArtifactResolver;
import org.sonatype.sisu.maven.bridge.MavenModelResolver;

import com.google.common.base.Throwables;
import com.google.inject.Binder;
import org.codehaus.plexus.util.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.client.rest.BaseUrl.baseUrlFrom;
import static org.sonatype.nexus.testsuite.support.NexusITFilter.contextEntry;
import static org.sonatype.nexus.testsuite.support.filters.TestProjectFilter.TEST_PROJECT_POM_FILE;

/**
 * Base class for Nexus Integration Tests.
 *
 * @since 2.0
 */
public abstract class NexusITSupport
    extends InjectedTestSupport
{

  /**
   * Artifact resolver used to resolve artifacts by Maven coordinates.
   * Never null.
   */
  @Inject
  @Named("remote-artifact-resolver-using-settings")
  private MavenArtifactResolver artifactResolver;

  /**
   * Model resolver used to resolve effective Maven models.
   * Never null.
   */
  @Inject
  @Named("remote-model-resolver-using-settings")
  private MavenModelResolver modelResolver;

  /**
   * Nexus bundle resolver.
   * Used in case that bundle is not specified as a constructor parameter.
   * Never null.
   */
  @Inject
  private NexusBundleResolver nexusBundleResolver;

  /**
   * File task builder used to build overlays.
   * Never null.
   */
  @Inject
  private FileTaskBuilder fileTaskBuilder;

  /**
   * Nexus client factory. Used to lazy create Nexus client.
   */
  @Inject
  private NexusClientFactory nexusClientFactory;

  /**
   * List of available filters.
   * Never null.
   */
  @Inject
  private List<Filter> filters;

  /**
   * Test specific artifact resolver utility.
   * Lazy initialized on first usage.
   */
  private NexusITArtifactResolver testArtifactResolver;

  /**
   * Filter used to filter coordinates.
   * Lazy initialized on first usage.
   */
  private NexusITFilter filter;

  /**
   * Nexus bundle coordinates to run the IT against. If null, it will look up the coordinates from
   * "injected-test.properties".
   */
  protected final String nexusBundleCoordinates;

  /**
   * Filtered Nexus bundle coordinates to run the IT against. If null, it will look up the coordinates from
   * "injected-test.properties".
   */
  protected String filteredNexusBundleCoordinates;

  /**
   * Test index.
   * Never null.
   */
  @Rule
  public TestIndexRule testIndex = new TestIndexRule(
      util.resolveFile("target/it-reports"), util.resolveFile("target/it-data")
  );

  /**
   * Test data.
   * Never null.
   */
  @Rule
  public TestDataRule testData = new TestDataRule(util.resolveFile("src/test/it-resources"));

  /**
   * Copy of system properties before the test (class).
   * Initialised before class starts.
   */
  private static Properties systemPropertiesBackup;

  /**
   * Runs IT by against Nexus bundle coordinates specified in "injected-test.properties".
   */
  public NexusITSupport() {
    this(null);
  }

  /**
   * Runs IT by against specified Nexus bundle coordinates.
   *
   * @param nexusBundleCoordinates nexus bundle coordinates to run the test against. If null, it will look up the
   *                               coordinates from "injected-test.properties".
   * @since 2.2
   */
  public NexusITSupport(@Nullable final String nexusBundleCoordinates) {
    this.nexusBundleCoordinates = nexusBundleCoordinates;
  }

  /**
   * Binds a {@link TargetDirectoryResolver} to an implementation that will set the bundle target directory to a
   * directory specific to test method.
   * <p/>
   * Format: {@code <project>/target/its/<test class package>/<test class name>/<test method name>/<path>}
   * <p/>
   * {@inheritDoc}
   */
  @Override
  public void configure(final Binder binder) {
    binder.bind(TargetDirectoryResolver.class).annotatedWith(NexusSpecific.class).toInstance(
        new TargetDirectoryResolver()
        {

          @Override
          public File resolve() {
            return testIndex().getDirectory();
          }

        });
    binder.bind(BundleResolver.class).annotatedWith(NexusSpecific.class).toInstance(
        new BundleResolver()
        {
          @Override
          public File resolve() {
            final BundleResolver resolver;
            if (filteredNexusBundleCoordinates == null) {
              resolver = nexusBundleResolver;
            }
            else {
              File bundle = new File(filteredNexusBundleCoordinates);
              if (bundle.exists()) {
                return bundle;
              }
              resolver = new MavenBridgedBundleResolver(filteredNexusBundleCoordinates, artifactResolver);
            }
            return resolver.resolve();
          }
        }
    );
  }

  /**
   * Takes a snapshot of system properties before the test starts.
   */
  @BeforeClass
  public static void backupSystemProperties() {
    systemPropertiesBackup = System.getProperties();
  }

  /**
   * Restores system properties as they were before test started.
   */
  @After
  public void restoreSystemProperties() {
    System.setProperties(systemPropertiesBackup);
  }

  /**
   * Filters nexus bundle coordinates, if present (not null).
   */
  @Before
  public void filterNexusBundleCoordinates() {
    if (nexusBundleCoordinates != null) {
      filteredNexusBundleCoordinates = filter().filter(nexusBundleCoordinates);

      logger.info(
          "TEST {} is running against Nexus bundle {}",
          testName.getMethodName(), filteredNexusBundleCoordinates
      );

      testIndex().recordInfo("bundle", filteredNexusBundleCoordinates);
    }
    else {
      logger.info(
          "TEST {} is running against a Nexus bundle resolved from injected-test.properties",
          testName.getMethodName()
      );
      testIndex().recordAndCopyLink(
          "bundle", util.resolveFile("target/test-classes/injected-test.properties")
      );
    }
  }

  @After
  public void recordSurefireAndFailsafeInfo() {
    {
      final String name = "target/failsafe-reports/" + getClass().getName();
      testIndex().recordLink("failsafe result", util.resolveFile(name + ".txt"));
      testIndex().recordLink("failsafe output", util.resolveFile(name + "-output.txt"));
    }
    {
      final String name = "target/surefire-reports/" + getClass().getName();
      testIndex().recordLink("surefire result", util.resolveFile(name + ".txt"));
      testIndex().recordLink("surefire output", util.resolveFile(name + "-output.txt"));
    }
  }

  /**
   * Lazy initializes IT specific artifact resolver.
   *
   * @return IT specific artifact resolver. Never null.
   */
  public NexusITArtifactResolver artifactResolver() {
    if (testArtifactResolver == null) {
      testArtifactResolver = new NexusITArtifactResolver(
          util.resolveFile("pom.xml"), artifactResolver, modelResolver
      );
    }
    return testArtifactResolver;
  }

  /**
   * Returns test data accessor.
   *
   * @return test data accessor. Never null.
   */
  public TestData testData() {
    return testData;
  }

  /**
   * Returns test index.
   *
   * @return test index. Never null.
   */
  public TestIndex testIndex() {
    return testIndex;
  }

  /**
   * Returns overlay builder.
   *
   * @return overlay builder. Never null.
   */
  public FileTaskBuilder tasks() {
    return fileTaskBuilder;
  }

  /**
   * Lazy initializes IT specific filter.
   *
   * @return IT specific filter. Never null.
   */
  public NexusITFilter filter() {
    if (filter == null) {
      filter = new NexusITFilter(
          filters,
          contextEntry(TEST_PROJECT_POM_FILE, util.resolveFile("pom.xml").getAbsolutePath())
      );
    }
    return filter;
  }

  /**
   * Apply default configuration settings to specified Nexus.
   *
   * @param nexus to apply default configurations settings to
   * @return passed in Nexus, for fluent API usage
   */
  public NexusBundle applyDefaultConfiguration(final NexusBundle nexus) {
    String logLevel = System.getProperty("it.nexus.log.level");

    if (!"DEBUG".equalsIgnoreCase(logLevel)) {
      final String useDebugFor = System.getProperty("it.nexus.log.level.use.debug");
      if (!StringUtils.isEmpty(useDebugFor)) {
        final String[] segments = useDebugFor.split(",");
        for (final String segment : segments) {
          if (getClass().getSimpleName().matches(segment.replace(".", "\\.").replace("*", ".*"))) {
            logLevel = "DEBUG";
          }
        }
      }
    }

    if (!StringUtils.isEmpty(logLevel)) {
      checkNotNull(nexus).getConfiguration().setLogLevel(logLevel);
    }

    final URL pomUrl = getClass().getResource(
        "/META-INF/maven/org.sonatype.nexus/nexus-testsuite-support/pom.properties"
    );

    checkState(pomUrl != null, "Missing pom.properties for org.sonatype.nexus:nexus-testsuite-support");

    try {
      final Properties props = Properties2.load(pomUrl);
      final String version = props.getProperty("version");
      nexus.getConfiguration().addPlugins(
          artifactResolver().resolveArtifact("org.sonatype.nexus:nexus-it-helper-plugin:zip:bundle:" + version)
      );
    }
    catch (final IOException e) {
      throw Throwables.propagate(e);
    }

    return nexus;
  }

  /**
   * Creates a {@link NexusClient} for specified Nexus instance, user and password.
   *
   * @param nexus    to create client for
   * @param userName user
   * @param password password
   * @return created nexus client. Never null.
   */
  protected NexusClient createNexusClient(final NexusBundle nexus,
                                          final String userName,
                                          final String password)
  {
    return nexusClientFactory.createFor(
        baseUrlFrom(checkNotNull(nexus).getUrl()),
        new UsernamePasswordAuthenticationInfo(checkNotNull(userName), checkNotNull(password))
    );
  }

  /**
   * Creates a {@link NexusClient} for specified Nexus instance, with user "admin" and password "admin123".
   *
   * @param nexus to create client for
   * @return created nexus client. Never null.
   */
  protected NexusClient createNexusClientForAdmin(final NexusBundle nexus) {
    return createNexusClient(nexus, "admin", "admin123");
  }

  /**
   * Creates a {@link NexusClient} for specified Nexus instance for anonymous user.
   *
   * @param nexus to create client for
   * @return created nexus client. Never null.
   */
  protected NexusClient createNexusClientForAnonymous(final NexusBundle nexus) {
    return nexusClientFactory.createFor(
        baseUrlFrom(nexus.getUrl())
    );
  }

  /**
   * Logs remote (in nexus.log) what the test is doing.
   *
   * @param remoteLogger logger to use
   * @param doingWhat    test state
   */
  protected void logRemoteThatTestIs(final Logger remoteLogger, final String doingWhat) {
    try {
      final String message = "TEST " + testName.getMethodName() + " " + doingWhat;

      final StringBuilder fullMessage = new StringBuilder()
          .append("\n")
          .append(StringUtils.repeat("*", message.length())).append("\n")
          .append(message).append("\n")
          .append(StringUtils.repeat("*", message.length()));

      remoteLogger.info(fullMessage.toString());
    }
    catch (final Exception e) {
      logger.warn("Failed to log remote that test was '{}' ({})", doingWhat, e.getMessage());
    }
  }

  /**
   * Generates a repository id specific to the test.
   * The id will be equal to the name of the current running test method.
   *
   * @return repository id. Never null.
   */
  protected String repositoryIdForTest() {
    return testMethodName();
  }

  /**
   * Generates a name specific to the test.
   * The id will be equal to the name of the current running test method.
   *
   * @return name. Never null.
   */
  protected String testMethodName() {
    String methodName = testName.getMethodName();
    if (methodName.contains("[")) {
      return methodName.substring(0, methodName.indexOf("["));
    }
    return methodName;
  }

  /**
   * Generates a repository id specific to the test appending the suffix.
   * The id will be equal to the name of the current running test method + "-" + suffix.
   *
   * @return repository id. Never null.
   */
  protected String repositoryIdForTest(final String suffix) {
    return String.format("%s-%s", repositoryIdForTest(), checkNotNull(suffix));
  }

}
