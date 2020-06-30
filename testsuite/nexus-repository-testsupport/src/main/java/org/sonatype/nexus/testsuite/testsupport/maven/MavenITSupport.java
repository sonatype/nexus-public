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
import java.net.URL;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.sonatype.nexus.common.log.LogManager;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload.InputStreamSupplier;
import org.sonatype.nexus.testsuite.testsupport.RepositoryITSupport;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.junit.experimental.categories.Category;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.sonatype.nexus.repository.storage.StorageFacetConstants.STORAGE;
import static org.sonatype.nexus.repository.storage.StorageFacetConstants.STRICT_CONTENT_TYPE_VALIDATION;

/**
 * Maven IT support.
 */
@Category(MavenTestGroup.class)
public abstract class MavenITSupport
    extends RepositoryITSupport
{
  @Inject
  protected LogManager logManager;

  @Inject
  protected MavenTestHelper mavenTestHelper;

  public MavenITSupport() {
    testData.addDirectory(resolveBaseFile("target/it-resources/maven"));
  }

  protected File mvnBaseDir(final String project) {
    return resolveBaseFile("target/" + getClass().getSimpleName() + "-" + testName.getMethodName() + "/" + project);
  }

  public void mvnDeploy(final String project, final String version, final String deployRepositoryName)
      throws Exception
  {
    mvnDeploy(project, version, new URL(nexusUrl, "/repository/maven-public"),
        new URL(nexusUrl, "/repository/" + deployRepositoryName));
  }

  protected void mvnDeploy(final String project,
                           final String group,
                           final String artifactId,
                           final String version,
                           final String deployRepositoryName)
      throws Exception
  {
    mvnDeploy(project, group, artifactId, version,
        new URL(nexusUrl, "/repository/maven-public"),
        new URL(nexusUrl, "/repository/" + deployRepositoryName));
  }

  protected void mvnLegacyDeploy(final String project, final String version, final String deployRepositoryName)
      throws Exception
  {
    mvnDeploy(project, version, new URL(nexusUrl, "/repository/maven-public"),
        new URL(nexusUrl, "/content/repositories/" + deployRepositoryName));
  }

  protected void mvnDeploy(final String project, final String version, final URL proxyUrl, final URL deployUrl)
      throws Exception
  {
    final File mavenBaseDir = mvnBaseDir(project).getAbsoluteFile();
    final File projectDir = resolveTestFile(project);

    MavenDeployment mavenDeployment = new MavenDeployment();
    mavenDeployment.setSettingsTemplate(resolveTestFile("settings.xml"));
    mavenDeployment.setProjectDir(mavenBaseDir);
    mavenDeployment.setProjectTemplateDir(projectDir);
    mavenDeployment.setVersion(version);
    mavenDeployment.setProxyUrl(proxyUrl);
    mavenDeployment.setDeployUrl(deployUrl);
    mavenDeployment.setEnsureCleanOnInit(false);
    mavenDeployment.init();

    new MavenRunner().run(mavenDeployment, "clean", "deploy");
  }

  protected void mvnDeploy(final String project,
                           final String group,
                           final String artifactId,
                           final String version,
                           final URL proxyUrl,
                           final URL deployUrl)
  {
    final File mavenBaseDir = mvnBaseDir(project).getAbsoluteFile();
    final File projectDir = resolveTestFile(project);

    MavenDeployment mavenDeployment = new MavenDeployment();
    mavenDeployment.setSettingsTemplate(resolveTestFile("settings.xml"));
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

  protected void write(final Repository repository, final String path, final Payload payload) throws IOException {
    mavenTestHelper.write(repository, path, payload);
  }

  /**
   * We added validation in NEXUS-16853 that prevents corrupted metadata files being stored in the repository however
   * some ITs test the ability to recover from corrupted metadata and therefore need to bypass the validation.
   */
  protected void writeWithoutValidation(final Repository repository, final String path, final Payload payload)
      throws IOException
  {
    mavenTestHelper.writeWithoutValidation(repository, path, payload);
  }

  protected StreamPayload filePayload(final File file, final String contentType) {
    return new StreamPayload(
        new InputStreamSupplier()
        {
          @Nonnull
          @Override
          public InputStream get() throws IOException {
            return java.nio.file.Files.newInputStream(file.toPath());
          }
        },
        file.length(),
        contentType
    );
  }

  protected Payload read(final Repository repository, final String path) throws IOException {
    return mavenTestHelper.read(repository, path);
  }

  protected void assertReadable(final Repository repository, final String... paths) throws IOException {
    for (String path : paths) {
      assertThat(path, read(repository, path), notNullValue());
    }
  }

  protected void assertNotReadable(final Repository repository, final String... paths) throws IOException {
    for (String path : paths) {
      assertThat(path, read(repository, path), nullValue());
    }
  }

  protected Metadata parseMetadata(final InputStream is) throws Exception {
    assertThat(is, notNullValue());
    return mavenTestHelper.parseMetadata(is);
  }

  protected Metadata parseMetadata(final Payload content) throws Exception {
    assertThat(content, notNullValue());
    try (InputStream is = content.openInputStream()) {
      return parseMetadata(is);
    }
  }

  @Nonnull
  protected Repository redirectProxy(final String repositoryName, final String remoteUrl) throws Exception {
    checkNotNull(repositoryName);
    Repository proxy = repositoryManager.get(repositoryName);
    checkNotNull(proxy);
    checkArgument(ProxyType.NAME.equals(proxy.getType().getValue()));
    org.sonatype.nexus.repository.config.Configuration proxyConfiguration = proxy.getConfiguration();
    proxyConfiguration.attributes("proxy").set("remoteUrl", remoteUrl);
    proxyConfiguration.attributes(STORAGE).set(STRICT_CONTENT_TYPE_VALIDATION, true);
    return repositoryManager.update(proxyConfiguration);
  }

  @Nonnull
  protected Maven2Client createAdminMaven2Client(final String repositoryName) throws Exception {
    return createMaven2Client(repositoryName, "admin", "admin123");
  }

  protected Maven2Client createAdminMaven2Client(final URL repositoryUrl) throws Exception {
    return createMaven2Client(repositoryUrl, "admin", "admin123");
  }

  @Nonnull
  protected Maven2Client createMaven2Client(final String repositoryName, final String username, final String password)
      throws Exception
  {
    checkNotNull(repositoryName);
    Repository repository = repositoryManager.get(repositoryName);
    checkNotNull(repository);
    return createMaven2Client(resolveUrl(nexusUrl, "/repository/" + repositoryName + "/"), username, password);
  }

  protected Maven2Client createMaven2Client(final URL repositoryUrl, final String username, final String password)
      throws Exception
  {
    HttpClientBuilder client = HttpClients.custom();
    if (username != null) {
      AuthScope scope = new AuthScope(repositoryUrl.getHost(), -1);
      CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
      credentialsProvider.setCredentials(scope, new UsernamePasswordCredentials(username, password));
      client.setDefaultCredentialsProvider(credentialsProvider);
    }

    RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
    requestConfigBuilder.setExpectContinueEnabled(true);
    HttpClientContext httpClientContext = HttpClientContext.create();
    httpClientContext.setRequestConfig(requestConfigBuilder.build());
    return new Maven2Client(
        client.build(),
        httpClientContext,
        repositoryUrl.toURI()
    );
  }

  protected void verifyHashesExistAndCorrect(final Repository repository, final String path) throws Exception {
    mavenTestHelper.verifyHashesExistAndCorrect(repository, path);
  }
}
