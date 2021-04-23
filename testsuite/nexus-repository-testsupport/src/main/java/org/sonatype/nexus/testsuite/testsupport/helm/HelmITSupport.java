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
package org.sonatype.nexus.testsuite.testsupport.helm;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.Nonnull;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.testsuite.testsupport.FormatClientSupport;
import org.sonatype.nexus.testsuite.testsupport.RepositoryITSupport;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.junit.Assert;
import org.junit.experimental.categories.Category;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.hamcrest.Matchers.is;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_DEVELOPER;

/**
 * Support class for Helm ITs.
 *
 * @since 3.28
 */
@Category(HelmTestGroup.class)
public abstract class HelmITSupport
    extends RepositoryITSupport
{
  public static final String HELM_FORMAT_NAME = "helm";

  public static final String MONGO_PKG_NAME = "mongodb";
  
  public static final String MONGO_PKG_GROUP = "mongodb";

  public static final String YAML_NAME = "index";

  public static final String MONGO_PKG_VERSION_600 = "6.0.0";

  public static final String MONGO_PKG_VERSION_728 = "7.2.8";

  public static final String MONGO_PKG_VERSION_404 = "4.0.4";

  public static final String TGZ_EXT = ".tgz";

  public static final String YAML_EXT = ".yaml";

  public static final String MONGO_PKG_FILE_NAME_728_TGZ = format("%s-%s%s",
      MONGO_PKG_NAME, MONGO_PKG_VERSION_728, TGZ_EXT);

  public static final String MONGO_PKG_FILE_NAME_600_TGZ = format("%s-%s%s",
      MONGO_PKG_NAME, MONGO_PKG_VERSION_600, TGZ_EXT);

  public static final String MONGO_PKG_FILE_NAME_404_TGZ = format("%s-%s%s",
      MONGO_PKG_NAME, MONGO_PKG_VERSION_404, TGZ_EXT);

  public static final String CONTENT_TYPE_TGZ = "application/x-tgz";

  public static final String CONTENT_TYPE_YAML = "text/x-yaml";

  public static final String YAML_FILE_NAME = String.format("%s%s", YAML_NAME, YAML_EXT);

  public static final String PKG_PATH = "bin/macosx/el-capitan/contrib/3.6";

  public static final String MONGO_PATH_FULL_600_TARGZ = format("%s/%s", PKG_PATH, MONGO_PKG_FILE_NAME_600_TGZ);

  public static final String MONGO_PATH_FULL_728_TARGZ = format("%s/%s", PKG_PATH, MONGO_PKG_FILE_NAME_728_TGZ);

  public static final String YAML_MONGO_600_URL = "urls:\n    - mongodb-6.0.0.tgz";

  public static final String YAML_MONGO_600_VERSION = "urls:\n    - mongodb-6.0.0.tgz";

  public static final String YAML_MONGO_728_URL = "version: 7.2.8";

  public static final String YAML_MONGO_728_VERSION = "version: 7.2.8";

  protected HelmITSupport() {
    testData.addDirectory(resolveBaseFile("target/it-resources/helm"));
  }

  @Configuration
  public static Option[] configureNexus() {
    return options(
        configureNexusBase(),
        when(getValidTestDatabase().isUseContentStore()).useOptions(editConfigurationFilePut(NEXUS_PROPERTIES_FILE,
            DATASTORE_DEVELOPER, "true"))
    );
  }

  protected Repository createHelmProxyRepository(final String name, final String remoteUrl) {
    return repos.createHelmProxy(name, remoteUrl);
  }

  protected Repository createHelmHostedRepository(final String name) {
    return repos.createHelmHosted(name);
  }

  @Nonnull
  protected HelmClient helmClient(final Repository repository) throws Exception {
    checkNotNull(repository);
    final URL repositoryUrl = repositoryBaseUrl(repository);

    return new HelmClient(
        clientBuilder(repositoryUrl).build(),
        clientContext(),
        repositoryUrl.toURI()
    );
  }

  @Nonnull
  protected HelmClient createHelmClient(final Repository repository) throws Exception {
    return new HelmClient(
        clientBuilder().build(),
        clientContext(),
        resolveUrl(nexusUrl, format("/repository/%s/", repository.getName())).toURI()
    );
  }

  protected HttpEntity fileToHttpEntity(String name) throws IOException {
    return new ByteArrayEntity(Files.readAllBytes(getFilePathByName(name)));
  }

  protected HttpEntity fileToMultipartHttpEntity(final String name) throws IOException {
    return MultipartEntityBuilder
        .create()
        .addBinaryBody("chart", Files.readAllBytes(getFilePathByName(name)), ContentType.APPLICATION_OCTET_STREAM, name)
        .build();
  }

  private Path getFilePathByName(String fileName) {
    return Paths.get(testData.resolveFile(fileName).getAbsolutePath());
  }

  protected void assertGetResponseStatus(
      final FormatClientSupport client,
      final Repository repository,
      final String path,
      final int responseCode) throws IOException
  {
    try (CloseableHttpResponse response = client.get(path)) {
      StatusLine statusLine = response.getStatusLine();
      Assert.assertThat("Repository:" + repository.getName() + " Path:" + path,
          statusLine.getStatusCode(),
          is(responseCode));
    }
  }
}
