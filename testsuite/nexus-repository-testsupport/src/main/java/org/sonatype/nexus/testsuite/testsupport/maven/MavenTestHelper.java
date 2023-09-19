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
package org.sonatype.nexus.testsuite.testsupport.maven;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.testsupport.TestData;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.testsuite.testsupport.system.RestTestHelper;

import com.google.common.base.Strings;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.joda.time.DateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.sonatype.nexus.pax.exam.NexusPaxExamSupport.resolveBaseFile;

public abstract class MavenTestHelper
{
  private static final int MAX_RETRIES = 3;

  private final MetadataXpp3Reader reader = new MetadataXpp3Reader();

  @Inject
  @Named("http://localhost:${application-port}${nexus-context-path}")
  private URL nexusUrl;

  @Inject
  private RestTestHelper restTestHelper;

  @Inject
  private RepositoryManager repositoryManager;

  public abstract Payload read(Repository repository, String path) throws IOException;

  public abstract void write(final Repository repository, final String path, final Payload payload) throws IOException;

  public abstract void writeWithoutValidation(Repository repository, String path, Payload payload) throws IOException;

  public abstract void verifyHashesExistAndCorrect(Repository repository, String path) throws Exception;

  public abstract DateTime getLastDownloadedTime(final Repository repository, final String assetPath)
      throws IOException;

  public Metadata parseMetadata(final Repository repository, final String path) throws Exception {
    try (Payload payload = read(repository, path)) {
      return parseMetadata(payload.openInputStream());
    }
  }

  public abstract boolean delete(Repository repository, String path) throws Exception;

  public void mvnDeploy(final MavenDeployBuilder mavenDeployBuilder) throws Exception
  {
    if (mavenDeployBuilder.nexusUrl == null) {
      mavenDeployBuilder.withNexusUrl(nexusUrl);
    }
    if (mavenDeployBuilder.getRetryCount() == null) {
      mavenDeployBuilder.withRetryCount(MAX_RETRIES);
    }
    if (mavenDeployBuilder.projectDirectory == null) {
      mavenDeployBuilder.withProjectDirectory(resolveBaseFile(
          "target/" + getClass().getSimpleName() + "-" + Math.random() + "/" + mavenDeployBuilder.getProject()));
    }

    runWithRetries(repositoryManager, mavenDeployBuilder.build(), mavenDeployBuilder.getRetryCount());
  }

  public Maven2Client createMaven2Client(final String repositoryName, final String username, final String password)
  {
    String repositoryPath = "repository/" + repositoryName + '/';
    URI repositoryUri = restTestHelper.resolveNexusPath(repositoryPath);
    CloseableHttpClient client = restTestHelper.client(repositoryPath, username, password);

    RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
    requestConfigBuilder.setExpectContinueEnabled(true);
    HttpClientContext httpClientContext = HttpClientContext.create();
    httpClientContext.setRequestConfig(requestConfigBuilder.build());

    return new Maven2Client(client, httpClientContext, repositoryUri);
  }

  public Metadata parseMetadata(final InputStream is) throws Exception {
    try (InputStream in = is) {
      assertThat(is, notNullValue());
      return reader.read(is);
    }
  }

  public void rebuildMetadata(
      final Repository repository,
      final String groupId,
      final String artifactId,
      final String baseVersion,
      final boolean rebuildChecksums)
  {
    final boolean update = !Strings.isNullOrEmpty(groupId)
        || !Strings.isNullOrEmpty(artifactId)
        || !Strings.isNullOrEmpty(baseVersion);
    rebuildMetadata(repository, groupId, artifactId, baseVersion, rebuildChecksums, update);
  }

  public abstract void rebuildMetadata(
      final Repository repository,
      final String groupId,
      final String artifactId,
      final String baseVersion,
      final boolean rebuildChecksums,
      final boolean update);

  /**
   * Delete test Components with the given version, confirming that a certain number exist first.
   */
  public abstract void deleteComponents(final Repository repository, final String version, final int expectedNumber);

  public abstract void deleteAssets(final Repository repository, final String version, final int expectedNumber);

  /**
   * Create component with given GAV and attached JAR asset
   *
   * @return componentId
   */
  public abstract EntityId createComponent(
      final Repository repository,
      final String groupId,
      final String artifactId,
      final String version);

  public abstract List<MavenTestComponent> loadComponents(final Repository repository);

  public abstract void updateBlobCreated(final Repository repository, final Date date);

  public abstract List<String> findComponents(final Repository repository);

  public abstract List<String> findAssets(final Repository repository);

  public abstract List<String> findAssetsExcludingFlaggedForRebuild(final Repository repository);

  public abstract void markMetadataForRebuild(final Repository repository, final String path);

  private void runWithRetries(
      final RepositoryManager repositoryManager,
      final MavenDeployment mavenDeployment,
      final int maxRetries) throws Exception
  {
    if (maxRetries > 0) {
      final AtomicInteger retries = new AtomicInteger(0);

      new MavenRunner().run(() -> {
        //Invalidate central repository caches before retrying
        Repository repository = repositoryManager.get("maven-central");

        if (repository != null) {
          repository.facet(ProxyFacet.class).invalidateProxyCaches();
        }

        return retries.getAndIncrement() < maxRetries;
      }, mavenDeployment, "clean", "deploy");
    }
    else {
      new MavenRunner().run(mavenDeployment, "clean", "deploy");
    }
  }

  public static final class MavenDeployBuilder
  {
    private String project;

    private String groupId;

    private String artifactId;

    private String version;

    private String repositoryName;

    private Integer retryCount;

    private URL proxyUrl;

    private URL deployUrl;

    private boolean legacy;

    private URL nexusUrl;

    private TestData testData;

    private File projectDirectory;

    // the project name, used to build file paths that aren't provided
    public MavenDeployBuilder withProject(final String project) {
      this.project = project;
      return this;
    }

    // groupId of the component you are deploying
    public MavenDeployBuilder withGroupId(final String groupId) {
      this.groupId = groupId;
      return this;
    }

    // artifactId of the component you are deploying
    public MavenDeployBuilder withArtifactId(final String artifactId) {
      this.artifactId = artifactId;
      return this;
    }

    // version of the component you are deploying
    public MavenDeployBuilder withVersion(final String version) {
      this.version = version;
      return this;
    }

    // the repository that you are deploying to.  Only one of withRepository or withRepositoryName is necessary
    public MavenDeployBuilder withRepository(final Repository repository) {
      this.repositoryName = repository.getName();
      return this;
    }

    // the repository you are deploying to.  Only one of withRepository or withRepositoryName is necessary
    public MavenDeployBuilder withRepositoryName(final String repositoryName) {
      this.repositoryName = repositoryName;
      return this;
    }

    // retry deploy requests in case of failure, set to 0 for request you expect to fail
    public MavenDeployBuilder withRetryCount(final Integer retryCount) {
      this.retryCount = retryCount;
      return this;
    }

    // the url of a proxy repository in nxrm that maven will use for downloading dependencies
    public MavenDeployBuilder withProxyURL(final URL proxyUrl) {
      this.proxyUrl = proxyUrl;
      return this;
    }

    // the deploy url of a hosted nexus repository
    public MavenDeployBuilder withDeployURL(final URL deployUrl) {
      this.deployUrl = deployUrl;
      return this;
    }

    // if legacy is enabled, deploy urls will use the nexus 2 style repository pathing
    public MavenDeployBuilder withLegacy(final boolean legacy) {
      this.legacy = legacy;
      return this;
    }

    // where all file content is loaded from
    public MavenDeployBuilder withTestData(final TestData testData) {
      this.testData = testData;
      return this;
    }

    // directory where mvn deploy command will be executed
    public MavenDeployBuilder withProjectDirectory(final File projectDirectory) {
      this.projectDirectory = projectDirectory;
      return this;
    }

    // url of the nexus instance
    public MavenDeployBuilder withNexusUrl(final URL nexusUrl) {
      this.nexusUrl = nexusUrl;
      return this;
    }

    public MavenDeployment build() throws Exception {
      MavenDeployment mavenDeployment = new MavenDeployment();
      mavenDeployment.setSettingsTemplate(testData.resolveFile("settings.xml"));
      mavenDeployment.setProjectDir(projectDirectory.getAbsoluteFile());
      mavenDeployment.setProjectTemplateDir(testData.resolveFile(project));
      // don't want to overwrite the default values
      if (groupId != null) {
        mavenDeployment.setGroupId(groupId);
      }
      // don't want to overwrite the default values
      if (artifactId != null) {
        mavenDeployment.setArtifactId(artifactId);
      }
      mavenDeployment.setVersion(version);
      mavenDeployment.setProxyUrl(proxyUrl != null ?
          // provided URL
          proxyUrl :
          // generate default URL
          new URL(nexusUrl, "/repository/maven-public"));
      mavenDeployment.setDeployUrl(deployUrl != null ?
          // provided URL
          deployUrl :
          legacy ?
              // generate nexus 2 style URL
              new URL(nexusUrl, "/content/repositories/" + repositoryName) :
              // generate default URL
              new URL(nexusUrl, "/repository/" + repositoryName));
      mavenDeployment.setEnsureCleanOnInit(false);
      mavenDeployment.init();

      return mavenDeployment;
    }

    public Integer getRetryCount() {
      return retryCount;
    }

    public String getProject() {
      return project;
    }
  }
}
