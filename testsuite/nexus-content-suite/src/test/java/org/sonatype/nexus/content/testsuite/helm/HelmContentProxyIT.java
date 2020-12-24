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

import java.nio.charset.Charset;

import org.sonatype.goodies.httpfixture.server.fluent.Server;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.content.testsupport.FormatClientSupport;
import org.sonatype.nexus.content.testsupport.helm.HelmClient;
import org.sonatype.nexus.content.testsupport.helm.HelmContentITSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpStatus;

import com.google.common.io.Files;
import org.apache.http.HttpResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.goodies.httpfixture.server.fluent.Behaviours.error;
import static org.sonatype.goodies.httpfixture.server.fluent.Behaviours.file;
import static org.sonatype.nexus.content.testsupport.FormatClientSupport.bytes;
import static org.sonatype.nexus.content.testsupport.FormatClientSupport.status;

public class HelmContentProxyIT
    extends HelmContentITSupport
{
  private HelmClient client;

  private Repository repository;

  private Server server;

  @Before
  public void setup() throws Exception {
    BaseUrlHolder.set(this.nexusUrl.toString());
    server = Server.withPort(0)
        .serve("/*").withBehaviours(error(200))
        .serve("/" + MONGO_PKG_FILE_NAME_600_TGZ)
        .withBehaviours(file(testData.resolveFile(MONGO_PKG_FILE_NAME_600_TGZ)))
        .serve("/" + YAML_FILE_NAME).withBehaviours(file(testData.resolveFile(YAML_FILE_NAME)))
        .start();
    repository = repos.createHelmProxy("helm-proxy-test", server.getUrl().toExternalForm());
    client = helmClient(repository);
  }

  @After
  public void tearDown() throws Exception {
    server.stop();
  }

  @Test
  public void fetchMetaData() throws Exception {
    HttpResponse httpResponse = client.fetch(YAML_FILE_NAME, CONTENT_TYPE_YAML);

    assertThat(status(httpResponse), is(HttpStatus.OK));
    assertThat(FormatClientSupport.asString(httpResponse),
        is(FileUtils.readFileToString(testData.resolveFile(YAML_FILE_NAME), Charset.defaultCharset())));
  }

  @Test
  public void fetchTgzPackageFile() throws Exception {
    HttpResponse httpResponse = client.fetch(MONGO_PKG_FILE_NAME_600_TGZ, CONTENT_TYPE_TGZ);

    assertThat(status(httpResponse), is(HttpStatus.OK));
    assertThat(bytes(httpResponse), is(Files.toByteArray(testData.resolveFile(MONGO_PKG_FILE_NAME_600_TGZ))));
  }
}
