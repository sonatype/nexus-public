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
package org.sonatype.nexus.testsuite.p2;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.sonatype.nexus.capabilities.client.Capabilities;
import org.sonatype.nexus.capabilities.client.Capability;
import org.sonatype.nexus.client.core.NexusClient;
import org.sonatype.nexus.client.core.spi.SubsystemProvider;
import org.sonatype.nexus.client.rest.BaseUrl;
import org.sonatype.nexus.client.rest.UsernamePasswordAuthenticationInfo;
import org.sonatype.nexus.client.rest.jersey.GuiceSubsystemProvider;
import org.sonatype.nexus.client.rest.jersey.NexusClientFactoryImpl;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.plugins.p2.repository.P2Constants;
import org.sonatype.nexus.plugins.p2.repository.P2MetadataGenerator;
import org.sonatype.nexus.plugins.p2.repository.P2RepositoryAggregator;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Injector;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.junit.BeforeClass;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractNexusP2GeneratorIT
    extends AbstractNexusP2IT
{

  private Capability p2RepositoryAggregatorCapability;

  private NexusClient nexusClient;

  public AbstractNexusP2GeneratorIT(final String repoId) {
    super(repoId);
  }

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  protected NexusClient client() {
    if (nexusClient == null) {
      try {
        nexusClient = new NexusClientFactoryImpl(
            Lists.<SubsystemProvider>newArrayList(new GuiceSubsystemProvider(lookup(Injector.class)))
        ).createFor(
            BaseUrl.baseUrlFrom(nexusBaseUrl),
            new UsernamePasswordAuthenticationInfo(checkNotNull("admin"), checkNotNull("admin123"))
        );
      }
      catch (MalformedURLException | ComponentLookupException e) {
        throw Throwables.propagate(e);
      }
    }
    return nexusClient;
  }

  private Capabilities capabilities() {
    return client().getSubsystem(Capabilities.class);
  }

  protected void createP2MetadataGeneratorCapability()
      throws Exception
  {
    capabilities().create("p2.repository.metadata.generator")
        .withNotes(P2MetadataGenerator.class.getName())
        .withProperty("repositoryId", getTestRepositoryId())
        .enable();
  }

  protected void createP2RepositoryAggregatorCapability()
      throws Exception
  {
    p2RepositoryAggregatorCapability = capabilities().create("p2.repository.aggregator")
        .withNotes(P2RepositoryAggregator.class.getName())
        .withProperty("repositoryId", getTestRepositoryId())
        .enable();
  }

  protected void removeP2RepositoryAggregatorCapability()
      throws Exception
  {
    p2RepositoryAggregatorCapability.remove();
  }

  protected void passivateP2RepositoryAggregatorCapability()
      throws Exception
  {
    p2RepositoryAggregatorCapability.disable();
  }

  protected void deployArtifact(final String repoId, final File fileToDeploy, final String path)
      throws Exception
  {
    final String deployUrl = getNexusTestRepoUrl(repoId);
    final String deployUrlProtocol = deployUrl.substring(0, deployUrl.indexOf(":"));
    final String wagonHint = getWagonHintForDeployProtocol(deployUrlProtocol);
    getDeployUtils().deployWithWagon(wagonHint, deployUrl, fileToDeploy, path);
  }

  protected File downloadP2ArtifactsFor(final String groupId, final String artifactId, final String version)
      throws IOException
  {
    final File downloadDir = new File("target/downloads/" + this.getClass().getSimpleName());
    final File p2Artifacts =
        downloadArtifact(groupId, artifactId, version, "xml", "p2Artifacts", downloadDir.getCanonicalPath());
    return p2Artifacts;
  }

  protected File downloadP2ContentFor(final String groupId, final String artifactId, final String version)
      throws IOException
  {
    final File downloadDir = new File("target/downloads/" + this.getClass().getSimpleName());
    final File p2Content =
        downloadArtifact(groupId, artifactId, version, "xml", "p2Content", downloadDir.getCanonicalPath());
    return p2Content;
  }

  protected File storageP2ArtifactsFor(final String groupId, final String artifactId, final String version)
      throws IOException
  {
    final File p2Artifacts =
        new File(new File(nexusWorkDir), "storage/" + getTestRepositoryId() + "/" + groupId + "/" + artifactId
            + "/" + version + "/" + artifactId + "-" + version + "-p2Artifacts.xml");
    return p2Artifacts;
  }

  protected File storageP2ContentFor(final String groupId, final String artifactId, final String version)
      throws IOException
  {
    final File p2Artifacts =
        new File(new File(nexusWorkDir), "storage/" + getTestRepositoryId() + "/" + groupId + "/" + artifactId
            + "/" + version + "/" + artifactId + "-" + version + "-p2Content.xml");
    return p2Artifacts;
  }

  protected File storageP2Repository()
      throws IOException
  {
    final File p2Repository =
        new File(new File(nexusWorkDir), "storage/" + getTestRepositoryId() + P2Constants.P2_REPOSITORY_ROOT_PATH);
    return p2Repository;
  }

  protected File storageP2RepositoryArtifactsXML()
      throws IOException
  {
    final File p2Artifacts = new File(storageP2Repository(), P2Constants.ARTIFACTS_XML);
    return p2Artifacts;
  }

  protected File storageP2RepositoryContentXML()
      throws IOException
  {
    final File p2Content = new File(storageP2Repository(), P2Constants.CONTENT_XML);
    return p2Content;
  }
}
