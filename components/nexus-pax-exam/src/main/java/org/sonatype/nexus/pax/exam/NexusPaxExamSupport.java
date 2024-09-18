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
package org.sonatype.nexus.pax.exam;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.net.ssl.HttpsURLConnection;

import org.sonatype.goodies.common.Loggers;
import org.sonatype.goodies.testsupport.TestIndex;
import org.sonatype.goodies.testsupport.junit.TestDataRule;
import org.sonatype.goodies.testsupport.junit.TestIndexRule;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.net.PortAllocator;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.scheduling.TaskScheduler;

import com.adobe.testing.s3mock.testcontainers.S3MockContainer;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.ops4j.net.URLUtils;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.ExamFactory;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.container.internal.JavaVersionUtil;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationFileExtendOption;
import org.ops4j.pax.exam.options.CompositeOption;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.options.OptionalCompositeOption;
import org.ops4j.pax.exam.options.ProvisionOption;
import org.ops4j.pax.exam.options.extra.EnvironmentOption;
import org.ops4j.pax.exam.options.extra.VMOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.propagateSystemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.*;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_CLUSTERED_ENABLED;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_CLUSTERED_ENABLED_NAMED;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_ENABLED;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_TABLE_SEARCH;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_TABLE_SEARCH_NAMED;
import static org.sonatype.nexus.common.app.FeatureFlags.DATE_BASED_BLOBSTORE_LAYOUT_ENABLED;
import static org.sonatype.nexus.common.app.FeatureFlags.ELASTIC_SEARCH_ENABLED_NAMED;
import static org.sonatype.nexus.common.app.FeatureFlags.JWT_ENABLED;
import static org.testcontainers.containers.BindMode.READ_ONLY;

/**
 * Provides support for testing Nexus with Pax-Exam, test-cases can inject any component from the distribution. <br>
 * <br>
 * Extend this class and choose the base distribution (and any optional plugins) that you want to test against:
 *
 * <pre>
 * &#064;Configuration
 * public static Option[] configureNexus() {
 *   return options( //
 *       nexusDistribution(&quot;org.sonatype.nexus.assemblies&quot;, &quot;nexus-base-template&quot;), //
 *       nexusPlugin(&quot;org.sonatype.nexus.plugins&quot;, &quot;nexus-repository-raw&quot;) //
 *   );
 * }
 * </pre>
 *
 * @since 3.0
 */
@RunWith(SafeRunner.class)
@SafeRunWith(PaxExam.class)
@ExamFactory(NexusPaxExamTestFactory.class)
@ExamReactorStrategy(PerClass.class)
public abstract class NexusPaxExamSupport
{
  public static final String NEXUS_PAX_EXAM_TIMEOUT_KEY = "nexus.pax.exam.timeout";

  public static final int NEXUS_PAX_EXAM_TIMEOUT_DEFAULT = 480000; // 8 minutes

  public static final String NEXUS_PAX_EXAM_INVOKER_KEY = "nexus.pax.exam.invoker";

  public static final String NEXUS_PAX_EXAM_INVOKER_DEFAULT = "junit";

  public static final int NEXUS_TEST_START_LEVEL = 200;

  public static final String NEXUS_PROPERTIES_FILE = "etc/nexus-default.properties";

  public static final String DATA_STORE_PROPERTIES_FILE = "etc/fabric/nexus-store.properties";

  public static final String KARAF_CONFIG_PROPERTIES_FILE = "etc/karaf/config.properties";

  public static final String SYSTEM_PROPERTIES_FILE = "etc/karaf/system.properties";

  public static final String PAX_URL_MAVEN_FILE = "etc/karaf/org.ops4j.pax.url.mvn.cfg";

  public static final String KARAF_MANAGEMENT_FILE = "etc/karaf/org.apache.karaf.management.cfg";

  public static final String S3_ENDPOINT_PROPERTY = "mock.s3.service.endpoint";

  private static final String PATCH_MODULE = "--patch-module";

  private static final String KARAF_VERSION = "karaf.version";

  public static String TEST_JDBC_URL_PROPERTY = "nexus.test.jdbcUrl";

  private static final String ADD_OPENS = "--add-opens";

  private static final String DATABASE_KEY = "it.database";

  private static final String BLOB_STORE_KEY = "it.blobstore";

  private static final String SQL_SEARCH_KEY = "it.sql_search";

  private static final String NX_PROPERTIES = "it.props";

  private static final String HA_KEY = "it.ha";

  /*
   * Key identifying a system property which if set, should be used as a prefix for {@code it-data} subdirectory
   * names to prevent a collision when Jenkins aggregates across parallel stages. (Jenkins isn't smart enough to
   * avoid overwriting files)
   */
  private static final String PROP_TEST_PREFIX = "it.data.prefix";

  // -------------------------------------------------------------------------

  @Rule
  public final TestDataRule testData = new TestDataRule(
      resolveBaseFile("src/test/it-resources"),
      resolveBaseFile("target/it-resources"));

  @Rule
  @Inject
  public TestIndexRule testIndex;

  @Rule
  public final TestCleaner testCleaner = new TestCleaner();

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Inject
  @Named("http://localhost:${application-port}${nexus-context-path}")
  protected URL nexusUrl;

  @Inject
  @Named("https://localhost:${application-port-ssl}${nexus-context-path}")
  protected URL nexusSecureUrl;

  @Inject
  protected ApplicationDirectories applicationDirectories;

  @Inject
  protected EventManager eventManager;

  @Inject
  protected TaskScheduler taskScheduler;

  @Inject
  @Named(ELASTIC_SEARCH_ENABLED_NAMED)
  protected Boolean elasticsearch;

  @Inject
  @Named(DATASTORE_TABLE_SEARCH_NAMED)
  protected Boolean datastoreTableSearch;

  @Inject
  @Named(DATASTORE_CLUSTERED_ENABLED_NAMED)
  private Boolean sqlHaEnabled;

  @Inject
  @Nullable
  @Named("nexus.datastore.nexus.jdbcUrl")
  private String jdbcUrl;

  //11.9 is the minimum support version
  private static final String POSTGRES_IMAGE = "docker-all.repo.sonatype.com/postgres:11.9";

  private static final int POSTGRES_PORT = 5432;

  public static final String DB_USER = "nxrmUser";

  public static final String DB_PASSWORD = "nxrmPassword";

  private static GenericContainer postgresContainer = null;

  private static S3MockContainer s3Container;

  protected final Logger log = checkNotNull(createLogger());

  protected Logger createLogger() {
    return Loggers.getLogger(this);
  }

  // -------------------------------------------------------------------------

  /**
   * Resolves path against the basedir of the surrounding Maven project.
   */
  public static File resolveBaseFile(final String path) {
    return TestBaseDir.resolve(path);
  }

  /**
   * Resolves path by searching the it-resources of the Maven project.
   *
   * @see TestDataRule#resolveFile(String)
   */
  public File resolveTestFile(final String path) {
    return testData.resolveFile(path);
  }

  /**
   * Resolves path against the Nexus work directory.
   */
  public File resolveWorkFile(final String path) {
    return new File(applicationDirectories.getWorkDirectory(), path);
  }

  /**
   * Resolves path against the Nexus temp directory.
   */
  public File resolveTempFile(final String path) {
    return new File(applicationDirectories.getTemporaryDirectory(), path);
  }

  /**
   * Resolves path against the given URL.
   */
  public static URL resolveUrl(final URL url, final String path) {
    try {
      return URI.create(url + "/" + path).normalize().toURL();
    }
    catch (final Exception e) {
      Throwables.throwIfUnchecked(e);
      throw new RuntimeException(e);
    }
  }

  // -------------------------------------------------------------------------

  /**
   * @return Timeout to apply when waiting for Nexus to start
   */
  public static int examTimeout() {
    try {
      return Integer.parseInt(System.getProperty(NEXUS_PAX_EXAM_TIMEOUT_KEY));
    }
    catch (final Exception e) {
      return NEXUS_PAX_EXAM_TIMEOUT_DEFAULT;
    }
  }

  /**
   * Periodically polls function until it returns {@code true} or 30 seconds have elapsed.
   *
   * @throws InterruptedException if the thread is interrupted
   * @throws TimeoutException     if the timeout exceeded
   * @deprecated use the Awaitility.await() helper instead of this method
   */
  @Deprecated
  public static void waitFor(final Callable<Boolean> function) // NOSONAR
      throws InterruptedException, TimeoutException
  {
    waitFor(function, 30000);
  }

  /**
   * Periodically polls function until it returns {@code true} or the timeout has elapsed.
   *
   * @throws InterruptedException if the thread is interrupted
   * @throws TimeoutException     if the timeout exceeded
   * @deprecated use the Awaitility.await() helper instead of this method
   */
  @Deprecated
  public static void waitFor(final Callable<Boolean> function, final long millis) // NOSONAR
      throws InterruptedException, TimeoutException
  {
    Exception functionEvaluationException = null;

    Thread.yield();
    long end = System.currentTimeMillis() + millis;
    do {
      try {
        if (Boolean.TRUE.equals(function.call())) {
          return; // success
        }
        // The condition was false, so any preserved exception from earlier calls is now irrelevant
        functionEvaluationException = null;
        Thread.sleep(100);
      }
      catch (final InterruptedException e) {
        throw e; // cancelled
      }
      catch (final Exception e) {
        functionEvaluationException = e;
        Thread.sleep(100);
      }
    }
    while (System.currentTimeMillis() <= end);
    Loggers.getLogger(NexusPaxExamSupport.class).warn("Timed out waiting for {} after {} ms", function, millis);
    throw (TimeoutException) new TimeoutException("Condition still unsatisfied after " + millis + " ms")
        .initCause(functionEvaluationException);
  }

  /**
   * @return Function that returns {@code true} when there's a response from the URL; otherwise {@code false}
   */
  public static Callable<Boolean> responseFrom(final URL url) {
    return () -> {
      HttpURLConnection conn = null;
      try {
        conn = (HttpURLConnection) url.openConnection();
        if (conn instanceof HttpsURLConnection) {
          // relax host and certificate checks as we just want to see if it's up
          ((HttpsURLConnection) conn).setHostnameVerifier((hostname, session) -> true);
          URLUtils.prepareForSSL(conn);
        }
        conn.setRequestMethod("HEAD");
        conn.connect();
        return true;
      }
      finally {
        if (conn != null) {
          conn.disconnect();
        }
      }
    };
  }

  /**
   * @return Function that returns {@code true} when the event system is calm; otherwise {@code false}
   */
  public static Callable<Boolean> calmPeriod(EventManager eventManager) {
    return eventManager::isCalmPeriod;
  }

  /**
   * @return Function that returns {@code true} when all tasks have stopped; otherwise {@code false}
   */
  public static Callable<Boolean> tasksDone(TaskScheduler taskScheduler, int initialTaskCount) {
    return () -> taskScheduler.getExecutedTaskCount() > initialTaskCount && taskScheduler.getRunningTaskCount() == 0;
  }

  /**
   * @return Function that returns {@code true} when all tasks have stopped; otherwise {@code false}
   */
  public static Callable<Boolean> tasksDone(TaskScheduler taskScheduler) {
    return tasksDone(taskScheduler, 0);
  }

  // -------------------------------------------------------------------------

  /**
   * To test a different version set the 'it.nexus.bundle.version' system property.<br> You can also override the
   * 'groupId', 'artifactId', and 'classifier' the same way.
   *
   * @return Pax-Exam option to install a Nexus distribution based on groupId and artifactId
   */
  public static Option nexusDistribution(final String groupId, final String artifactId) {
    return nexusDistribution(maven(groupId, artifactId).version(nexusVersion()).type("zip"));
  }

  /**
   * To test a different version set the 'it.nexus.bundle.version' system property.<br> You can also override the
   * 'groupId', 'artifactId', and 'classifier' the same way.
   *
   * @return Pax-Exam option to install a Nexus distribution based on groupId, artifactId and classifier
   */
  public static Option nexusDistribution(final String groupId, final String artifactId, final String classifier) {
    return nexusDistribution(maven(groupId, artifactId).classifier(classifier).version(nexusVersion()).type("zip"));
  }

  /**
   * Uses a publicly available assembly to compute the version of Nexus
   */
  private static String nexusVersion() {
    return MavenUtils.getArtifactVersion("org.sonatype.nexus.assemblies", "nexus-base-template");
  }

  /**
   * @return Pax-Exam option to install a Nexus distribution from the given framework zip
   */
  public static Option nexusDistribution(final MavenUrlReference frameworkZip) {

    // support explicit CI setting as well as automatic detection
    String localRepository = System.getProperty("maven.repo.local", System.getProperty("localRepository", ""));
    if (localRepository.length() > 0) {
      // pass on explicit setting to Pax-URL (otherwise it uses wrong value)
      System.setProperty("org.ops4j.pax.url.mvn.localRepository", localRepository);
    }
    else {
      // use placeholder in karaf config
      localRepository = "${maven.repo.local}";
    }

    // allow overriding the distribution under test from the command-line
    if (System.getProperty("it.nexus.bundle.groupId") != null) {
      frameworkZip.groupId(System.getProperty("it.nexus.bundle.groupId"));
    }
    if (System.getProperty("it.nexus.bundle.artifactId") != null) {
      frameworkZip.artifactId(System.getProperty("it.nexus.bundle.artifactId"));
    }
    if (System.getProperty("it.nexus.bundle.version") != null) {
      frameworkZip.version(System.getProperty("it.nexus.bundle.version"));
    }
    if (System.getProperty("it.nexus.bundle.classifier") != null) {
      frameworkZip.classifier(System.getProperty("it.nexus.bundle.classifier"));
    }

    // enable JDWP debugging which will suspend the IT and wait on port 5005
    boolean debugging = Boolean.parseBoolean(System.getProperty("it.debug"));

    // allow overriding of the out-of-the-box logback configuration
    File logbackXml = resolveBaseFile("target/test-classes/logback-test.xml");
    String logLevel = System.getProperty("it.test.log.level", "INFO");

    return composite(

        // mimic nexus script
        vmOption("-Xms2703m"),
        vmOption("-Xmx2703m"),
        vmOption("-XX:MaxDirectMemorySize=2703m"),
        vmOption("-XX:+UnlockDiagnosticVMOptions"),
        vmOption("-XX:+LogVMOutput"),
        vmOption("-XX:LogFile=./nexus3/log/jvm.log"),
        vmOption("-XX:-OmitStackTraceInFastThrow"),

        vmOption("-Djava.io.tmpdir=./nexus3/tmp/"),

        systemTimeout(examTimeout()),

        propagateSystemProperty(NEXUS_PAX_EXAM_TIMEOUT_KEY),
        propagateSystemProperty(TestCleaner.CLEAN_ON_SUCCESS_KEY),

        systemProperty("basedir").value(TestBaseDir.get()),

        // Pax-Exam cuts the leading directory from all archive paths when unpacking,
        // so 'sonatype-work/nexus3' ends up as 'nexus3' under the base.

        // Karaf specific configuration is now under 'etc/karaf' relative to the base.

        karafDistributionConfiguration() //
            .karafData("nexus3") //
            .karafEtc("etc/karaf") //
            .karafMain("org.sonatype.nexus.karaf.NexusMain") //
            .karafVersion("4") //
            .frameworkUrl(frameworkZip) //
            .useDeployFolder(false), //

        when(debugging).useOptions(debugConfiguration()), // port 5005, suspend=y

        configureConsole().ignoreLocalConsole().ignoreRemoteShell(), // no need for console

        doNotModifyLogConfiguration(), // don't mess with our logging

        keepRuntimeFolder(), // keep files around in case we need to debug

        // enable testing of plugin snapshots from the local repository
        systemProperty("nexus.testLocalSnapshots").value("true"),

        propagateSystemProperty("maven.repo.local"),
        propagateSystemProperty("localRepository"),

        editConfigurationFilePut(PAX_URL_MAVEN_FILE, // so we can fetch local snapshots
            "org.ops4j.pax.url.mvn.localRepository", localRepository),

        useOwnKarafExamSystemConfiguration("nexus"),

        nexusPaxExam(), // registers invoker factory that waits for nexus to start before running tests

        // merge hamcrest-library extras with the core hamcrest bundle from Pax
        wrappedBundle(maven("org.hamcrest", "hamcrest-library").versionAsInProject()) //
            .instructions("Fragment-Host=org.ops4j.pax.tipi.hamcrest.core"),

        when(logbackXml.canRead()).useOptions( //
            replaceConfigurationFile("etc/logback/logback.xml", logbackXml)),

        systemProperty("root.level").value(logLevel),

        // disable unused shutdown port, one port less that could clash with reserved ports
        editConfigurationFilePut(KARAF_CONFIG_PROPERTIES_FILE, //
            "karaf.shutdown.port", "-1"),

        //configure db, including starting external resources
        when(getValidTestDatabase().isUseContentStore()).useOptions(
            configureDatabase()
        ),
        when(getValidTestDatabase().isUseOrient()).useOptions(
            configureDatabase()
        ),

        when(System.getProperty(PROP_TEST_PREFIX) != null).useOptions(
            editConfigurationFilePut(NEXUS_PROPERTIES_FILE, //
                PROP_TEST_PREFIX, System.getProperty(PROP_TEST_PREFIX)),
            editConfigurationFilePut(NEXUS_PROPERTIES_FILE, //
                "goodies.build.name", System.getProperty(PROP_TEST_PREFIX))
        ),

        when(System.getProperty(NX_PROPERTIES) != null).useOptions(parseItProperties()),

        // configure default blobstore
        configureBlobstore(),

        // randomize ports...
        editConfigurationFilePut(NEXUS_PROPERTIES_FILE, //
            "application-port", Integer.toString(PortAllocator.nextFreePort())),
        editConfigurationFilePut(NEXUS_PROPERTIES_FILE, //
            "application-port-ssl", Integer.toString(PortAllocator.nextFreePort())),
        editConfigurationFilePut(KARAF_MANAGEMENT_FILE, //
            "rmiRegistryPort", Integer.toString(PortAllocator.nextFreePort())),
        editConfigurationFilePut(KARAF_MANAGEMENT_FILE, //
            "rmiServerPort", Integer.toString(PortAllocator.nextFreePort())),

        propagateSystemProperty("it.nexus.recordTaskLogs"),

        new EnvironmentOption("TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX=docker-all.repo.sonatype.com/")
    );
  }

  private static Option[] parseItProperties() {
    String nxProperties = System.getProperty(NX_PROPERTIES);
    if (nxProperties == null) {
      return new Option[0];
    }
    return Arrays.stream(nxProperties.split(","))
        .peek(prop -> Loggers.getLogger(NexusPaxExamSupport.class).info("Found property {}", prop))
        .map(prop -> prop.split("="))
        .map(props -> editConfigurationFilePut(NEXUS_PROPERTIES_FILE, props[0], props[1]))
        .toArray(Option[]::new);
  }

  protected static Option[] configureDatabase() {
    switch (getValidTestDatabase()) {
      case POSTGRES:
        postgresContainer = new GenericContainer(POSTGRES_IMAGE) //NOSONAR
            .withExposedPorts(POSTGRES_PORT)
            .withEnv("POSTGRES_USER", DB_USER)
            .withEnv("POSTGRES_PASSWORD", DB_PASSWORD)
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(NexusPaxExamSupport.class)))
            .withCommand("postgres", "-c", "max_connections=110")
            .withClasspathResourceMapping("initialize-postgres.sql", "/docker-entrypoint-initdb.d/initialize-postgres.sql", READ_ONLY)
            .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 1));
        return combine(null,
            editConfigurationFilePut(NEXUS_PROPERTIES_FILE, DATASTORE_ENABLED, "true"),
            editConfigurationFilePut(NEXUS_PROPERTIES_FILE, "nexus.datastore.nexus.name", "nexus"),
            editConfigurationFilePut(NEXUS_PROPERTIES_FILE, "nexus.datastore.nexus.type", "jdbc"),
            editConfigurationFilePut(NEXUS_PROPERTIES_FILE, "nexus.datastore.nexus.jdbcUrl", configurePostgres()),
            editConfigurationFilePut(NEXUS_PROPERTIES_FILE, "nexus.datastore.nexus.username", DB_USER),
            editConfigurationFilePut(NEXUS_PROPERTIES_FILE, "nexus.datastore.nexus.password", DB_PASSWORD),
            haOption(),
            sqlSearchOption(),
            withDateBasedBlobstoreLayout(),
            systemProperty(TEST_JDBC_URL_PROPERTY).value(configurePostgres())
        );
      case H2:
        return combine(null,
            sqlSearchOption(),
            withDateBasedBlobstoreLayout(),
            editConfigurationFilePut(NEXUS_PROPERTIES_FILE, DATASTORE_ENABLED, "true")
        );
      default:
        throw new IllegalStateException("No case defined for " + getValidTestDatabase());
    }
  }

  protected static Option configureBlobstore() {
    switch (System.getProperty(BLOB_STORE_KEY, "")) {
      case "s3":
        String mockS3endpoint = S3_ENDPOINT_PROPERTY;
        String bucket = "nexus.test.s3.bucket";
        String region = "nexus.test.s3.region";
        String accessKey = "nexus.test.s3.accessKey";
        String accessSecret = "nexus.test.s3.accessSecret";
        String endpoint = "nexus.test.s3.endpoint";
        String forcePathStyle = "nexus.test.s3.forcePathStyle";

        return composite(
            editConfigurationFileExtend(NEXUS_PROPERTIES_FILE, "nexus.default.file.blobstore", Boolean.FALSE.toString()),
            // enable s3 default
            editConfigurationFileExtend(NEXUS_PROPERTIES_FILE, "nexus.test.default.s3", Boolean.TRUE.toString()),
            // copy the maven property if it exists
            getS3OptionalCompositeOption(mockS3endpoint, System.getProperty(mockS3endpoint)),
            getS3OptionalCompositeOption(bucket, System.getProperty(bucket)),
            getS3OptionalCompositeOption(region, System.getProperty(region)),
            getS3OptionalCompositeOption(accessKey, System.getProperty(accessKey)),
            getS3OptionalCompositeOption(accessSecret, System.getProperty(accessSecret)),
            getS3OptionalCompositeOption(endpoint, System.getProperty(endpoint)),
            getS3OptionalCompositeOption(forcePathStyle, System.getProperty(forcePathStyle))
        );
      case "azure":
        return composite(
            editConfigurationFileExtend(NEXUS_PROPERTIES_FILE, "nexus.default.file.blobstore", Boolean.FALSE.toString()),
            // enable azure default
            editConfigurationFileExtend(NEXUS_PROPERTIES_FILE, "nexus.test.default.azure", Boolean.TRUE.toString()),
            editConfigurationFileExtend(NEXUS_PROPERTIES_FILE, "nexus.blobstore.new.azure", Boolean.TRUE.toString())
        );
      default:
        return composite();
    }
  }

  private static OptionalCompositeOption getS3OptionalCompositeOption(
      final String propertyKey,
      final Object propertyValue)
  {
    return when(Objects.nonNull(propertyValue)).useOptions(
        editConfigurationFileExtend(NEXUS_PROPERTIES_FILE, propertyKey, propertyValue)
    );
  }

  private static String configurePostgres() {
    if (!postgresContainer.isRunning()) {
      postgresContainer.start();
    }

    return String.format("jdbc:postgresql://%s:%d/",
        postgresContainer.getHost(),
        postgresContainer.getMappedPort(POSTGRES_PORT));
  }

  @AfterClass
  public static final void shutdownPostgres() {
    if (postgresContainer != null && postgresContainer.isRunning()) {
      postgresContainer.stop();
    }
  }

  @AfterClass
  public static void shutdownS3() {
    if (s3Container != null && s3Container.isRunning()) {
      s3Container.stop();
    }
  }

  /**
   * @return Pax-Exam option to change the context path for the Nexus distribution
   */
  public static Option withContextPath(final String contextPath) {
    return editConfigurationFilePut(NEXUS_PROPERTIES_FILE, "nexus-context-path", contextPath);
  }

  /**
   * @return Pax-Exam option to enable HTTPS support in the Nexus distribution
   */
  public static Option withHttps(final File keystore) {
    return composite(
        editConfigurationFileExtend(NEXUS_PROPERTIES_FILE, "nexus-args", "${jetty.etc}/jetty-https.xml"),
        replaceConfigurationFile("etc/ssl/keystore.jks", keystore));
  }

  public static Option withS3() {
    String s3Endpoint = System.getProperty(S3_ENDPOINT_PROPERTY);
    // If S3 endpoint has not been provided, we need to use S3 mock container
    if (isNullOrEmpty(s3Endpoint)) {
      // If container object has not been initialised, we need to create it
      if (isNull(s3Container)) {
        String s3mockImage = System.getProperty("it.blobstore.s3image", "docker-all.repo.sonatype.com/adobe/s3mock:2.17.0");
        DockerImageName image = DockerImageName.parse(s3mockImage).asCompatibleSubstituteFor(S3MockContainer.IMAGE_NAME);
        s3Container = new S3MockContainer(image);
      }
      // If container is not running, we need to start it
      if (!s3Container.isRunning()) {
        s3Container.start();
      }
      s3Endpoint = s3Container.getHttpEndpoint();
    }
    // Pass S3 endpoint to Nexus Repository configuration file
    return editConfigurationFileExtend(NEXUS_PROPERTIES_FILE, S3_ENDPOINT_PROPERTY, s3Endpoint);
  }

  /**
   * @return Pax-Exam option to change the Nexus edition based on groupId and artifactId
   */
  public static Option nexusEdition(final String groupId, final String artifactId) {
    return nexusEdition(maven(groupId, artifactId).version(nexusVersion()).classifier("features").type("xml"),
        artifactId + '/' + nexusVersion());
  }

  /**
   * @return Pax-Exam option to change the Nexus edition using the given feature XML and name
   */
  public static Option nexusEdition(final MavenUrlReference featureXml, final String name) {
    return composite(features(featureXml), editConfigurationFilePut(NEXUS_PROPERTIES_FILE, "nexus-edition", name));
  }

  /**
   * @return Pax-Exam option to install a Nexus plugin based on groupId and artifactId
   */
  public static Option nexusFeature(final String groupId, final String artifactId) {
    return nexusFeature(maven(groupId, artifactId).version(nexusVersion()).classifier("features").type("xml"),
        artifactId + '/' + nexusVersion());
  }

  /**
   * @return Pax-Exam option to install a Nexus plugin from the given feature XML and name
   */
  public static Option nexusFeature(final MavenUrlReference featureXml, final String name) {
    return composite(features(featureXml), editConfigurationFileExtend(NEXUS_PROPERTIES_FILE, "nexus-features", name));
  }

  /**
   * Replacement for {@link CoreOptions#options(Option...)} to workaround Pax-Exam 'feature' where only the last
   * 'editConfigurationFileExtend' for a given property key is honoured.
   */
  public static Option[] options(final Option... options) {
    final List<Option> result = new ArrayList<>();

    final List<String> nexusFeatures = new ArrayList<>();
    for (final Option o : OptionUtils.expand(options)) {

      // filter out the individual nexus-features values
      if (o instanceof KarafDistributionConfigurationFileExtendOption) {
        if ("nexus-features".equals(((KarafDistributionConfigurationFileExtendOption) o).getKey())) {
          nexusFeatures.add((String) ((KarafDistributionConfigurationFileExtendOption) o).getValue());
          continue;
        }
      }
      // provide default start level for any additional test bundles
      else if (o instanceof ProvisionOption<?> && ((ProvisionOption<?>) o).getStartLevel() == null) {
        ((ProvisionOption<?>) o).startLevel(NEXUS_TEST_START_LEVEL);
      }

      result.add(o);
    }

    if (nexusFeatures.size() > 0) {
      // combine the nexus-features values into a single request
      result.add(editConfigurationFileExtend(NEXUS_PROPERTIES_FILE, //
          "nexus-features", Joiner.on(',').join(nexusFeatures)));
    }

    return result.toArray(new Option[result.size()]);
  }

  /**
   * Processes two sequences of options and combines them into a single sequence.
   */
  public static Option[] options(final Option[] options1, final Option... options2) {
    return options(combine(options1, options2));
  }

  // -------------------------------------------------------------------------

  @Before
  public void startTestRecording() {
    // Pax-Exam guarantees unique test location, use that with index
    testIndex.setDirectory(applicationDirectories.getInstallDirectory());
  }

  public static void captureLogs(final TestIndex testIndex, final File logDir, final String className) {
    testIndex.recordAndCopyLink("karaf.log", new File(logDir, "karaf.log"));
    testIndex.recordAndCopyLink("nexus.log", new File(logDir, "nexus.log"));
    testIndex.recordAndCopyLink("request.log", new File(logDir, "request.log"));
    testIndex.recordAndCopyLink("jvm.log", new File(logDir, "jvm.log"));

    if ("true".equals(System.getProperty("it.nexus.recordTaskLogs"))) {
      File tasksDir = new File(logDir, "tasks");
      File[] taskLogs = tasksDir.listFiles(f -> f.getName().endsWith(".log"));
      if (taskLogs != null) {
        for (File taskLog : taskLogs) {
          testIndex.recordAndCopyLink("tasks/" + taskLog.getName(), taskLog);
        }
      }
    }

    final String surefirePrefix = "target/surefire-reports/" + className;
    testIndex.recordLink("surefire result", resolveBaseFile(surefirePrefix + ".txt"));
    testIndex.recordLink("surefire output", resolveBaseFile(surefirePrefix + "-output.txt"));

    final String failsafePrefix = "target/failsafe-reports/" + className;
    testIndex.recordLink("failsafe result", resolveBaseFile(failsafePrefix + ".txt"));
    testIndex.recordLink("failsafe output", resolveBaseFile(failsafePrefix + "-output.txt"));
  }

  @After
  public void stopTestRecording() {
    captureLogs(testIndex, resolveWorkFile("log"), getClass().getName());

    testCleaner.cleanOnSuccess(applicationDirectories.getInstallDirectory());
  }

  /**
   * Get the database type to use for the test instance, based on system property.
   */
  public static TestDatabase getValidTestDatabase() {
    try {
      return TestDatabase.valueOf(Strings2.upper(System.getProperty(DATABASE_KEY)));
    }
    catch (Exception e) {
      //fallback to H2 if it is invalid
      return TestDatabase.H2;
    }
  }

  public static Option sqlSearchOption() {
    boolean sqlSearch = Boolean.parseBoolean(System.getProperty(SQL_SEARCH_KEY, "false"));

    // SQL Search
    return when(sqlSearch).useOptions(editConfigurationFilePut(NEXUS_PROPERTIES_FILE, //
        DATASTORE_TABLE_SEARCH, "true"));
  }

  public static Option haOption() {
    boolean ha = Boolean.parseBoolean(System.getProperty(HA_KEY, "false"));

    // HA mode enable
    Option dsClusteredEnable = editConfigurationFilePut(NEXUS_PROPERTIES_FILE, DATASTORE_CLUSTERED_ENABLED, "true");
    Option jwtEnabled = editConfigurationFilePut(NEXUS_PROPERTIES_FILE, JWT_ENABLED, "true");
    // For tests ensure the default blobstore & repositories are created
    Option blobstoreProvisionDefaults =
        editConfigurationFilePut(NEXUS_PROPERTIES_FILE, "nexus.blobstore.provisionDefaults", "true");
    Option repositoryProvisionDefaults =
        editConfigurationFilePut(NEXUS_PROPERTIES_FILE, "nexus.skipDefaultRepositories", "false");

    List<String> formats = Arrays.asList("apt", "cocoapods", "conan", "conda", "gitlfs", "go", "helm", "nuget",
        "p2", "pypi", "raw", "rubygems", "yum");
    Option haFormats = composite(formats.stream()
        .map(format -> editConfigurationFilePut(NEXUS_PROPERTIES_FILE, format("nexus.%s.ha.supported", format), "true"))
        .toArray(Option[]::new));

    return when(ha).useOptions(dsClusteredEnable, jwtEnabled, blobstoreProvisionDefaults, repositoryProvisionDefaults,
        haFormats);
  }

  public static Option withDateBasedBlobstoreLayout() {
    return editConfigurationFilePut(NEXUS_PROPERTIES_FILE, DATE_BASED_BLOBSTORE_LAYOUT_ENABLED, "true");
  }

  protected boolean isSqlHa() {
    return sqlHaEnabled;
  }

  protected boolean isPostgreSQL() {
    return jdbcUrl != null && jdbcUrl.contains("postgres");
  }

  // -------------------------------------------------------------------------

  /**
   * @return Pax-Exam option to install custom invoker factory that waits for Nexus to start
   */
  private static Option nexusPaxExam() {
    final String version = MavenUtils.getArtifactVersion("org.sonatype.nexus", "nexus-pax-exam");
    Option result = mavenBundle("org.sonatype.nexus", "nexus-pax-exam", version).startLevel(0);

    final File nexusPaxExam = resolveBaseFile("target/nexus-pax-exam-" + version + ".jar");
    if (nexusPaxExam.isFile()) {
      // when freshly built bundle of 'nexus-pax-exam' is available, copy it over to distribution's system repository
      final String systemPath = "system/org/sonatype/nexus/nexus-pax-exam/" + version + "/" + nexusPaxExam.getName();
      result = composite(replaceConfigurationFile(systemPath, nexusPaxExam), result);
    }

    return result;
  }

  public static CompositeOption javaVMCompositeOption() {
    if(JavaVersionUtil.getMajorVersion() == 11) {
      return new DefaultCompositeOption(
          new VMOption("--add-exports=java.base/org.apache.karaf.specs.locator=java.xml,ALL-UNNAMED"),
          new VMOption(PATCH_MODULE),
          new VMOption("java.base=lib/endorsed/org.apache.karaf.specs.locator-" +
              System.getProperty(KARAF_VERSION) + ".jar"));
    }
    else if (JavaVersionUtil.getMajorVersion() == 17) {
      return new DefaultCompositeOption(
          new VMOption("--add-reads=java.xml=java.logging"),
          new VMOption("--add-exports=java.base/org.apache.karaf.specs.locator=java.xml,ALL-UNNAMED"),
          new VMOption(PATCH_MODULE),
          new VMOption("java.base=lib/endorsed/org.apache.karaf.specs.locator-"
              + System.getProperty(KARAF_VERSION) + ".jar"),
          new VMOption(PATCH_MODULE), new VMOption("java.xml=lib/endorsed/org.apache.karaf.specs.java.xml-"
          + System.getProperty(KARAF_VERSION) + ".jar"),
          new VMOption(ADD_OPENS),
          new VMOption("java.base/java.security=ALL-UNNAMED"),
          new VMOption(ADD_OPENS),
          new VMOption("java.base/java.net=ALL-UNNAMED"),
          new VMOption(ADD_OPENS),
          new VMOption("java.base/java.lang=ALL-UNNAMED"),
          new VMOption(ADD_OPENS),
          new VMOption("java.base/java.util=ALL-UNNAMED"),
          new VMOption(ADD_OPENS),
          new VMOption("java.naming/javax.naming.spi=ALL-UNNAMED"),
          new VMOption(ADD_OPENS),
          new VMOption("java.rmi/sun.rmi.transport.tcp=ALL-UNNAMED"),
          new VMOption("--add-exports=java.base/sun.net.www.protocol.file=ALL-UNNAMED"),
          new VMOption("--add-exports=java.base/sun.net.www.protocol.ftp=ALL-UNNAMED"),
          new VMOption("--add-exports=java.base/sun.net.www.protocol.http=ALL-UNNAMED"),
          new VMOption("--add-exports=java.base/sun.net.www.protocol.https=ALL-UNNAMED"),
          new VMOption("--add-exports=java.base/sun.net.www.protocol.jar=ALL-UNNAMED"),
          new VMOption("--add-exports=java.base/sun.net.www.content.text=ALL-UNNAMED"),
          new VMOption("--add-exports=jdk.naming.rmi/com.sun.jndi.url.rmi=ALL-UNNAMED"),
          new VMOption("--add-exports=java.rmi/sun.rmi.registry=ALL-UNNAMED"),
          new VMOption("--add-exports=jdk.xml.dom/org.w3c.dom.html=ALL-UNNAMED"),
          new VMOption("--add-exports=java.security.sasl/com.sun.security.sasl=ALL-UNNAMED"),
          new VMOption("-classpath"),
          new VMOption("lib/jdk9plus/*" + File.pathSeparator + "lib/boot/*"
              + File.pathSeparator + "lib/endorsed/*")
      );
    }
    return new DefaultCompositeOption();
  }
}
