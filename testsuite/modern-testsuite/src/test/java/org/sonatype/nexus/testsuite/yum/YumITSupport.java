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
package org.sonatype.nexus.testsuite.yum;

import java.lang.ProcessBuilder.Redirect;
import java.util.List;

import org.sonatype.nexus.bundle.launcher.NexusBundleConfiguration;
import org.sonatype.nexus.capabilities.client.Capabilities;
import org.sonatype.nexus.client.core.subsystem.ServerConfiguration;
import org.sonatype.nexus.client.core.subsystem.config.RestApi;
import org.sonatype.nexus.client.core.subsystem.content.Content;
import org.sonatype.nexus.client.core.subsystem.repository.Repositories;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenGroupRepository;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenHostedRepository;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenProxyRepository;
import org.sonatype.nexus.testsuite.client.Events;
import org.sonatype.nexus.testsuite.client.RoutingTest;
import org.sonatype.nexus.testsuite.client.Scheduler;
import org.sonatype.nexus.testsuite.support.NexusRunningParametrizedITSupport;
import org.sonatype.nexus.testsuite.support.NexusStartAndStopStrategy;
import org.sonatype.nexus.yum.client.Repodata;
import org.sonatype.nexus.yum.client.Yum;
import org.sonatype.nexus.yum.client.capabilities.GenerateMetadataCapability;
import org.sonatype.nexus.yum.client.capabilities.MergeMetadataCapability;
import org.sonatype.nexus.yum.client.capabilities.ProxyMetadataCapability;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.BeforeClass;

import static org.junit.Assume.assumeTrue;

/**
 * Support class for Yum ITs.
 *
 * @since 3.0
 */
@NexusStartAndStopStrategy(NexusStartAndStopStrategy.Strategy.EACH_TEST)
public class YumITSupport
    extends NexusRunningParametrizedITSupport
{

  public YumITSupport(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Override
  protected NexusBundleConfiguration configureNexus(NexusBundleConfiguration configuration) {
    return configuration
        .setLogLevel("org.sonatype.nexus.yum", "TRACE")
        .addPlugins(
            artifactResolver().resolvePluginFromDependencyManagement(
                "org.sonatype.nexus.plugins", "nexus-yum-repository-plugin"
            )
        );
  }

  @BeforeClass
  public static void ignoreIfCreateRepoNotPresent() {
    List<String> command = Lists.newArrayList();
    command.add("createrepo");
    command.add("--version");

    ProcessBuilder processBuilder = new ProcessBuilder(command);
    processBuilder.redirectErrorStream(true);
    processBuilder.redirectOutput(Redirect.INHERIT);

    try {
      int exitCode = processBuilder.start().waitFor();
      assumeTrue("createrepo is present", exitCode == 0);
    }
    catch (Exception e) {
      assumeTrue("createrepo is present", false);
    }
  }

  @Before
  public void setBaseUrl() {
    final RestApi restApi = serverConfiguration().restApi();
    restApi.settings().setBaseUrl(nexus().getUrl().toExternalForm());
    restApi.settings().setForceBaseUrl(true);
    restApi.save();
  }

  protected MavenHostedRepository createYumEnabledRepository(final String repositoryId) {
    final MavenHostedRepository repository = repositories()
        .create(MavenHostedRepository.class, repositoryId)
        .excludeFromSearchResults()
        .save();

    enableMetadataGenerationFor(repositoryId);

    return repository;
  }

  protected MavenProxyRepository createYumEnabledProxyRepository(final String repositoryId, final String remoteUrl) {
    final MavenProxyRepository repository = repositories()
        .create(MavenProxyRepository.class, repositoryId)
        .doNotDownloadRemoteIndexes()
        .asProxyOf(remoteUrl)
        .withItemMaxAge(0)
        .save();

    enableMetadataProxyFor(repositoryId);

    return repository;
  }

  protected MavenGroupRepository createYumEnabledGroupRepository(final String repositoryId, final String... memberIds) {
    final MavenGroupRepository repository = repositories().create(MavenGroupRepository.class, repositoryId)
        .ofRepositories(memberIds)
        .save();

    enableMetadataMergeFor(repositoryId);

    return repository;
  }

  private void enableMetadataGenerationFor(final String repositoryId) {
    capabilities().create(GenerateMetadataCapability.class).withRepository(repositoryId).enable();
  }

  private void enableMetadataProxyFor(final String repositoryId) {
    capabilities().create(ProxyMetadataCapability.class).withRepository(repositoryId).enable();
  }

  private void enableMetadataMergeFor(final String repositoryId) {
    capabilities().create(MergeMetadataCapability.class).withRepository(repositoryId).enable();
  }

  protected Yum yum() {
    return client().getSubsystem(Yum.class);
  }

  protected Repodata repodata() {
    return client().getSubsystem(Repodata.class);
  }

  protected Repositories repositories() {
    return client().getSubsystem(Repositories.class);
  }

  protected Content content() {
    return client().getSubsystem(Content.class);
  }

  private Capabilities capabilities() {
    return client().getSubsystem(Capabilities.class);
  }

  public Scheduler scheduler() {
    return client().getSubsystem(Scheduler.class);
  }

  private ServerConfiguration serverConfiguration() {
    return client().getSubsystem(ServerConfiguration.class);
  }

  protected void waitForNexusToSettleDown()
      throws Exception
  {
    remoteLogger().info("Waiting for Nexus to settle down...");
    scheduler().waitForAllTasksToStop();
    client().getSubsystem(Events.class).waitForCalmPeriod();
    client().getSubsystem(RoutingTest.class).waitForAllRoutingUpdateJobToStop();
    remoteLogger().info("Nexus is quiet. Done waiting.");
  }

}
