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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.testsupport.TestData;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.view.Payload;

import com.google.common.base.Strings;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.joda.time.DateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

public abstract class MavenTestHelper
{
  private final MetadataXpp3Reader reader = new MetadataXpp3Reader();

  @Inject
  @Named("http://localhost:${application-port}${nexus-context-path}")
  private URL nexusUrl;

  private CloseableHttpClient client = HttpClientBuilder.create().build();

  public Payload read(final Repository repository, final String path) throws IOException {
    HttpGet get = new HttpGet(url(repository, path));
    try (CloseableHttpResponse response = client.execute(get)) {
      if (response.getStatusLine().getStatusCode() >= 300) {
        return null;
      }

      HttpEntity entity = response.getEntity();
      try (InputStream in = entity.getContent()) {
        byte[] content = IOUtils.toByteArray(in);
        String contentType = entity.getContentType().toString();

        return new Payload()
        {
          @Override
          public InputStream openInputStream() throws IOException {
            return new ByteArrayInputStream(content);
          }

          @Override
          public long getSize() {
            return content.length;
          }

          @Override
          public String getContentType() {
            return contentType;
          }
        };
      }
    }
  }

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

  public void mvnDeploy(
      final TestData testData,
      final String project,
      final String version,
      final String deployRepositoryName) throws Exception
  {
    mvnDeploy(testData, project, version, new URL(nexusUrl,
        "/repository/maven-public"),
        new URL(nexusUrl, "/repository/" + deployRepositoryName));
  }

  protected void mvnDeploy(
      final TestData testData,
      final String project,
      final String group,
      final String artifactId,
      final String version,
      final String deployRepositoryName) throws Exception
  {
    mvnDeploy(testData, project, group, artifactId, version, new URL(nexusUrl,
        "/repository/maven-public"),
        new URL(nexusUrl, "/repository/" + deployRepositoryName));
  }

  protected void mvnLegacyDeploy(
      final TestData testData,
      final String project,
      final String version,
      final String deployRepositoryName) throws Exception
  {
    mvnDeploy(testData, project, version, new URL(nexusUrl,
        "/repository/maven-public"),
        new URL(nexusUrl, "/content/repositories/" + deployRepositoryName));
  }

  protected void mvnDeploy(
      final TestData testData,
      final String project,
      final String version,
      final URL proxyUrl,
      final URL deployUrl) throws Exception
  {
    final File mavenBaseDir = mvnBaseDir(project).getAbsoluteFile();
    final File projectDir = testData.resolveFile(project);

    MavenDeployment mavenDeployment = new MavenDeployment();
    mavenDeployment.setSettingsTemplate(testData.resolveFile("settings.xml"));
    mavenDeployment.setProjectDir(mavenBaseDir);
    mavenDeployment.setProjectTemplateDir(projectDir);
    mavenDeployment.setVersion(version);
    mavenDeployment.setProxyUrl(proxyUrl);
    mavenDeployment.setDeployUrl(deployUrl);
    mavenDeployment.setEnsureCleanOnInit(false);
    mavenDeployment.init();

    new MavenRunner().run(mavenDeployment, "clean", "deploy");
  }

  protected void mvnDeploy(
      final TestData testData,
      final String project,
      final String group,
      final String artifactId,
      final String version,
      final URL proxyUrl,
      final URL deployUrl)
  {
    final File mavenBaseDir = mvnBaseDir(project).getAbsoluteFile();
    final File projectDir = testData.resolveFile(project);

    MavenDeployment mavenDeployment = new MavenDeployment();
    mavenDeployment.setSettingsTemplate(testData.resolveFile("settings.xml"));
    mavenDeployment.setProjectDir(mavenBaseDir);
    mavenDeployment.setProjectTemplateDir(projectDir);
    mavenDeployment.setGroupId(group);
    mavenDeployment.setArtifactId(artifactId);
    mavenDeployment.setVersion(version);
    mavenDeployment.setProxyUrl(proxyUrl);
    mavenDeployment.setDeployUrl(deployUrl);
    mavenDeployment.setEnsureCleanOnInit(false);
    mavenDeployment.init();

    new MavenRunner().run(mavenDeployment, "clean", "deploy");
  }

  private File mvnBaseDir(final String project) {
    return NexusPaxExamSupport
        .resolveBaseFile("target/" + getClass().getSimpleName() + "-" + Math.random() + "/" + project);
  }

  public Metadata parseMetadata(final InputStream is) throws Exception {
    try {
      assertThat(is, notNullValue());
      return reader.read(is);
    }
    finally {
      IOUtils.closeQuietly(is);
    }
  }

  private String url(final Repository repo, final String path) {
    boolean reset = false;
    if (!BaseUrlHolder.isSet()) {
      reset = true;
      BaseUrlHolder.set(nexusUrl.toString());
    }
    String repoPath = path.startsWith("/") ? path : '/' + path;
    try {
      return String.format("%s%s", repo.getUrl(), repoPath);
    }
    finally {
      if (reset) {
        BaseUrlHolder.unset();
      }
    }
  }

  public void rebuildMetadata(
      final Repository repository,
      final String groupId,
      final String artifactId,
      final String baseVersion,
      final boolean rebuildChecksums) {
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
}
