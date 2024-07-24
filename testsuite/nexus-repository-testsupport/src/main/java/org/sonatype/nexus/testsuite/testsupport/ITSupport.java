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
package org.sonatype.nexus.testsuite.testsupport;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.tools.DeadBlobFinder;
import org.sonatype.nexus.repository.tools.DeadBlobResult;
import org.sonatype.nexus.testsuite.testsupport.system.NexusTestSystemSupport;
import org.sonatype.nexus.testsuite.testsupport.system.NexusTestSystemSupport.NexusTestSystemRule;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFileExtend;
import static org.ops4j.pax.exam.options.WrappedUrlProvisionOption.OverwriteMode.MERGE;

@ExamReactorStrategy(PerSuite.class)
public abstract class ITSupport
    extends NexusPaxExamSupport
{
  @Rule
  public TestName testName = new TestName();

  @Rule
  public NexusTestSystemRule nexusTestSystemRule = new NexusTestSystemRule(this::nexusTestSystem);

  @Inject
  private PoolingHttpClientConnectionManager connectionManager;

  @Inject
  private DeadBlobFinder<?> deadBlobFinder;

  @Inject
  private RepositoryManager repositoryManager;

  @Inject
  @Named("http://localhost:${application-port}${nexus-context-path}")
  private URL nexusUrl;

  @Inject
  @Named("https://localhost:${application-port-ssl}${nexus-context-path}")
  private URL nexusSecureUrl;

  public static Option[] configureNexus(final Option distribution) {
    return NexusPaxExamSupport.options(
        distribution,

        editConfigurationFileExtend(SYSTEM_PROPERTIES_FILE, "nexus.security.randompassword", "false"),
        editConfigurationFileExtend(NEXUS_PROPERTIES_FILE, "nexus.scripts.allowCreation", "true"),
        editConfigurationFileExtend(NEXUS_PROPERTIES_FILE, "nexus.search.event.handler.flushOnCount", "1"),

        // install common test-support features
        nexusFeature("org.sonatype.nexus.testsuite", "nexus-repository-testsupport"),
        wrappedBundle(maven("org.awaitility", "awaitility").versionAsInProject()).overwriteManifest(MERGE).imports("*")
    );
  }

  /**
   * Make sure Nexus is responding on the standard base URL before continuing
   */
  @Before
  public void waitForNexus() {
    await().atMost(30, TimeUnit.SECONDS)
        .ignoreExceptionsMatching(exception -> !(exception instanceof InterruptedException))
        .until(responseFrom(nexusUrl));
  }

  /**
   * Verifies there are no unreleased HTTP connections in Nexus. This check runs automatically after each test but tests
   * may as well run this check manually at suitable points during their execution.
   */
  @After
  public void verifyNoConnectionLeak() {
    // Some proxy repos directly serve upstream content, i.e. the connection to the upstream repo is actively used while
    // streaming out the response to the client. An HTTP client considers a response done when the content length has
    // been reached at which point the client/test can continue while NX still has to release the upstream connection
    // (cf. ResponseEntityProxy which releases a connection after the last byte has been handed out to the client).
    // So allow for some delay when checking the connection pool.
    await().atMost(3, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(connectionManager.getTotalStats().getLeased(), is(0)));
  }

  @After
  public void verifyNoDeadBlobs() {
    if (shouldVerifyNoDeadBlobs()) {
      doVerifyNoDeadBlobs();
    }
  }

  protected boolean shouldVerifyNoDeadBlobs() {
    return true;
  }

  /**
   * Left protected to allow specific subclasses to override where this behaviour is expected due to minimal test setup.
   */
  protected void doVerifyNoDeadBlobs() {
    Map<String, List<DeadBlobResult<?>>> badRepos = StreamSupport.stream(repositoryManager.browse().spliterator(), true)
        .map(repository -> deadBlobFinder.find(repository, shouldIgnoreMissingBlobRefs()))
        .flatMap(Collection::stream)
        .collect(Collectors.groupingBy(DeadBlobResult::getRepositoryName));

    if (!badRepos.isEmpty()) {
      log.error("Detected dead blobs: {}", badRepos);
      throw new IllegalStateException("Dead blobs detected!");
    }
  }

  /**
   * Allow specific tests to override this behaviour where "missing" blobs are valid due to the test setup.
   */
  protected boolean shouldIgnoreMissingBlobRefs() {
    return false;
  }

  protected URL nexusUrl() {
    // eventually this might switch to nexusSecurUrl based on a property
    return nexusUrl;
  }

  protected URL nexusSecureUrl(){
    return nexusSecureUrl;
  }

  protected abstract NexusTestSystemSupport<?,?> nexusTestSystem();
}
