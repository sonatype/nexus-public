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
package org.sonatype.nexus.testsuite.npm.smoke;

import java.io.IOException;
import java.time.OffsetDateTime;

import org.sonatype.nexus.bundle.launcher.NexusBundleConfiguration;
import org.sonatype.nexus.testsuite.npm.NpmMockRegistryITSupport;
import org.sonatype.sisu.goodies.testsupport.group.External;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static java.time.OffsetDateTime.now;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static org.apache.http.HttpHeaders.IF_MODIFIED_SINCE;
import static org.apache.http.HttpStatus.SC_NOT_MODIFIED;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Smoke IT for NPM package metadata with conditional response.
 * This IT just starts up Nexus Repository with NPM plugin, creates a NPM proxy/group repo and requests one single metadata file from it.
 * When 'nexus.npm.conditionalResponse=true', only then consider returning 304 response code for NPM package metadata requests
 * if inbound client headers provide If-Modified-Since that is before the age of what Nexus Repository has available to serve.
 */
@Category(External.class)
public class NpmConditionalResponseIT
    extends NpmMockRegistryITSupport
{
  private static final String NPM_REGISTRY_URL = "https://registry.npmjs.org";

  public NpmConditionalResponseIT(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Override
  protected NexusBundleConfiguration configureNexus(final NexusBundleConfiguration configuration) {
    return super.configureNexus(configuration).setSystemProperty("nexus.npm.conditionalResponse", "true");
  }

  @Test
  public void npmRequestForMetadataViaProxyRepoWithIfModifiedSince() throws InterruptedException {
    String proxyRepoName = testMethodName() + "-proxy";
    createNpmProxyRepository(proxyRepoName, NPM_REGISTRY_URL);

    // the first request to Nexus Repository should return 200 status code
    assertThat(requestForMetadata(proxyRepoName, "uuid", now()), is((SC_OK)));

    // the second request to Nexus Repository should return 200 code
    // since the time in the If-Modified-Since header less than a time when a metadata was created.
    assertThat(requestForMetadata(proxyRepoName, "uuid",  now().minusMinutes(1L)), is((SC_OK)));

    Thread.sleep(2_000L); // wait before the second request
    // the second request should be with 304 status code from Nexus Repository
    // since the 'uuid' metadata was created on very first request.
    assertThat(requestForMetadata(proxyRepoName, "uuid", now()), is((SC_NOT_MODIFIED)));
  }

  @Test
  public void npmRequestForMetadataViaGroupRepoWithIfModifiedSince() throws InterruptedException {
    String proxyRepoName = testMethodName() + "-proxy";
    String groupRepoName = testMethodName() + "-group";
    createNpmProxyRepository(proxyRepoName, NPM_REGISTRY_URL);
    createNpmGroupRepository(groupRepoName, proxyRepoName);

    // the first request to Nexus Repository should return 200 status code
    assertThat(requestForMetadata(groupRepoName, "debug", now()), is((SC_OK)));

    // the second request to Nexus Repository should return 200 code
    // since the time in the If-Modified-Since header less than a time when a metadata was created.
    assertThat(requestForMetadata(groupRepoName, "debug",  now().minusMinutes(1L)), is((SC_OK)));

    Thread.sleep(2_000L); // wait before the second request
    // the second request should be with 304 status code from Nexus Repository
    // since the 'debug' metadata was created on very first request.
    assertThat(requestForMetadata(groupRepoName, "debug", now()), is((SC_NOT_MODIFIED)));
  }

  @Test
  public void npmRequestForMetadataViaProxyRepoWithoutIfModifiedSince() throws InterruptedException {
    String proxyRepoName = testMethodName() + "-proxy";
    createNpmProxyRepository(proxyRepoName, NPM_REGISTRY_URL);

    // the first request to Nexus Repository should return 200 status code
    assertThat(requestForMetadata(proxyRepoName, "qs"), is((SC_OK)));

    // the second request to Nexus Repository should return 200 code since If-Modified-Since is empty
    assertThat(requestForMetadata(proxyRepoName, "qs"), is((SC_OK)));

    Thread.sleep(2_000L); // wait before the second request
    // the second request should be also with 200 status code, since metadata is created each time per request.
    assertThat(requestForMetadata(proxyRepoName, "qs"), is((SC_OK)));
  }

  @Test
  public void npmRequestForMetadataViaGroupRepoWithoutIfModifiedSince() throws InterruptedException {
    String proxyRepoName = testMethodName() + "-proxy";
    String groupRepoName = testMethodName() + "-group";
    createNpmProxyRepository(proxyRepoName, NPM_REGISTRY_URL);
    createNpmGroupRepository(groupRepoName, proxyRepoName);

    // the first request to Nexus Repository should return 200 status code
    assertThat(requestForMetadata(groupRepoName, "lodash"), is((SC_OK)));

    // the second request to Nexus Repository should return 200 code since If-Modified-Since is empty
    assertThat(requestForMetadata(groupRepoName, "lodash"), is((SC_OK)));

    Thread.sleep(2_000L); // wait before the second request
    // the second request should be also with 200 status code, since metadata is created each time per request.
    assertThat(requestForMetadata(groupRepoName, "lodash"), is((SC_OK)));
  }

  private int requestForMetadata(final String repositoryName, final String packageName)
  {
    return requestForMetadata(repositoryName, packageName, null);
  }

  private int requestForMetadata(final String repositoryName,
                                 final String packageName,
                                 final OffsetDateTime ifModifiedSince)
  {
    String uri = nexus().getUrl() + "content/repositories/" + repositoryName + "/" + packageName;
    HttpGet get = new HttpGet(uri);
    if (ifModifiedSince != null) {
      get.setHeader(IF_MODIFIED_SINCE, ifModifiedSince.format(RFC_1123_DATE_TIME));
    }
    try (CloseableHttpClient client = HttpClients.createMinimal();
         CloseableHttpResponse response = client.execute(get)) {
      return response.getStatusLine().getStatusCode();
    }
    catch (IOException e) {
      remoteLogger().error("Can't request to " + uri, e);
      return -1;
    }
  }
}
