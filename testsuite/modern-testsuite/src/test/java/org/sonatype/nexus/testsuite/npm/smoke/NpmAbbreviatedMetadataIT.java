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
import java.nio.charset.StandardCharsets;

import org.sonatype.nexus.bundle.launcher.NexusBundleConfiguration;
import org.sonatype.nexus.testsuite.npm.NpmMockRegistryITSupport;
import org.sonatype.sisu.goodies.testsupport.group.External;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.apache.http.HttpHeaders.ACCEPT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Smoke IT for npm abbreviated package metadata. This IT just starts up Nexus Repository with npm plugin, creates a npm proxy/group repo,
 * and requests one single abbreviated/full metadata file from it.
 * When 'nexus.npm.abbreviateMetadata=true', to request an abbreviated document with only the fields required to support installation.
 */
@Category(External.class)
public class NpmAbbreviatedMetadataIT
    extends NpmMockRegistryITSupport
{
  private static final String NPM_REGISTRY_URL = "https://registry.npmjs.org";

  public NpmAbbreviatedMetadataIT(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Override
  protected NexusBundleConfiguration configureNexus(final NexusBundleConfiguration configuration) {
    return super.configureNexus(configuration).setSystemProperty("nexus.npm.abbreviateMetadata", "true");
  }

  @Test
  public void npmRequestForAbbreviatedMetadataViaProxyRepo() {
    String proxyRepoName = testMethodName() + "-proxy";
    createNpmProxyRepository(proxyRepoName, NPM_REGISTRY_URL);

    String abbreviatedContent = requestForMetadata(proxyRepoName, "commonjs", true);
    assertThat(abbreviatedContent != null, is(true));
    // the abbreviated metadata shouldn't contain such field as "description".
    assertThat(abbreviatedContent, not(containsString("description")));

    // even if `nexus.npm.abbreviateMetadata=true` and accept header is not set to get abbreviated metadata -
    // the full metadata will be returned.
    String fullContentContent = requestForMetadata(proxyRepoName, "commonjs", false);
    assertThat(fullContentContent, containsString("description"));
    assertThat(fullContentContent.length() > abbreviatedContent.length(), is(true));
    assertThat(fullContentContent, not(equalToIgnoringWhiteSpace(abbreviatedContent)));
  }

  @Test
  public void npmRequestForAbbreviatedMetadataViaGroupRepo() {
    String proxyRepoName = testMethodName() + "-proxy";
    String groupRepoName = testMethodName() + "-group";
    createNpmProxyRepository(proxyRepoName, NPM_REGISTRY_URL);
    createNpmGroupRepository(groupRepoName, proxyRepoName);

    String abbreviatedContent = requestForMetadata(groupRepoName, "commonjs", true);
    assertThat(abbreviatedContent != null, is(true));
    // the abbreviated metadata shouldn't contain such field as "description".
    assertThat(abbreviatedContent, not(containsString("description")));

    String fullContentContent = requestForMetadata(groupRepoName, "commonjs", false);
    assertThat(fullContentContent != null, is(true));
    assertThat(fullContentContent.length() > abbreviatedContent.length(), is(true));
    assertThat(fullContentContent, not(equalToIgnoringWhiteSpace(abbreviatedContent)));
  }

  private String requestForMetadata(final String repoName, final String packageName, boolean abbreviatedHeader)
  {
    HttpGet get = new HttpGet(nexus().getUrl() + "content/repositories/" + repoName + "/" + packageName);
    if (abbreviatedHeader) {
      get.setHeader(ACCEPT, "application/vnd.npm.install-v1+json");
    }
    try (CloseableHttpClient client = HttpClients.createMinimal();
         CloseableHttpResponse response = client.execute(get)) {
      assertThat(response.getStatusLine().getStatusCode(), is(SC_OK));

      return IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
    }
    catch (IOException e) {
      remoteLogger().error("Can't request to " + get.getURI(), e);
      return null;
    }
  }

}
