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
package org.sonatype.nexus.content.testsuite.helm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup;
import org.sonatype.nexus.content.testsupport.helm.HelmClient;
import org.sonatype.nexus.content.testsupport.helm.HelmContentITSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpStatus;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ByteArrayEntity;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.content.testsupport.FormatClientSupport.bytes;
import static org.sonatype.nexus.content.testsupport.FormatClientSupport.status;

@Category(SQLTestGroup.class)
public class HelmContentHostedIT
    extends HelmContentITSupport
{
  private HelmClient client;

  private Repository repository;

  @Before
  public void setup() throws Exception {
    BaseUrlHolder.set(this.nexusUrl.toString());

    repository = repos.createHelmHosted("helm-hosted-test");
    client = helmClient(repository);
    uploadPackages(MONGO_PKG_FILE_NAME_600_TGZ, MONGO_PKG_FILE_NAME_728_TGZ);
    TimeUnit.SECONDS.sleep(2);
  }

  @Test
  public void fetchMetaData() throws Exception {
    HttpResponse httpResponse = client.fetch(YAML_FILE_NAME, CONTENT_TYPE_YAML);

    assertThat(status(httpResponse), is(HttpStatus.OK));
  }

  @Test
  public void fetchTgzPackageFile() throws Exception {
    HttpResponse httpResponse = client.fetch(MONGO_PKG_FILE_NAME_600_TGZ, CONTENT_TYPE_TGZ);

    assertThat(status(httpResponse), is(HttpStatus.OK));
    assertThat(bytes(httpResponse), is(Files.readAllBytes(testData.resolveFile(MONGO_PKG_FILE_NAME_600_TGZ).toPath())));
  }

  private void uploadPackages(final String... names) throws IOException {
    for (String name : names) {
      client.put(name, fileToHttpEntity(name));
    }
  }

  private Path getFilePathByName(final String fileName) {
    return Paths.get(testData.resolveFile(fileName).getAbsolutePath());
  }

  protected HttpEntity fileToHttpEntity(final String name) throws IOException {
    return new ByteArrayEntity(java.nio.file.Files.readAllBytes(getFilePathByName(name)));
  }
}
