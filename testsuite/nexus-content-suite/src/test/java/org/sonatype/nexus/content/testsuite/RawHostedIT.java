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
package org.sonatype.nexus.content.testsuite;

import org.sonatype.nexus.content.testsupport.raw.RawClient;
import org.sonatype.nexus.content.testsupport.raw.RawITSupport;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.http.HttpStatus;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.FileEntity;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import static org.apache.http.entity.ContentType.TEXT_PLAIN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.STORAGE;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.WRITE_POLICY;
import static org.sonatype.nexus.repository.content.facet.WritePolicy.ALLOW_ONCE;
import static org.sonatype.nexus.repository.content.facet.WritePolicy.DENY;

public class RawHostedIT
    extends RawITSupport
{
  public static final String HOSTED_REPO = "raw-test-hosted";

  public static final String TEST_CONTENT = "alphabet.txt";

  private RawClient rawClient;

  @Before
  public void createHostedRepository() throws Exception {
    rawClient = rawClient(repos.createRawHosted(HOSTED_REPO));
  }

  @Test
  public void uploadAndDownload() throws Exception {
    uploadAndDownload(rawClient, TEST_CONTENT);
  }

  @Test
  public void redeploy() throws Exception {
    uploadAndDownload(rawClient, TEST_CONTENT);
    uploadAndDownload(rawClient, TEST_CONTENT);
  }

  @Test
  public void canDisallowDeploy() throws Exception {
    rawClient = rawClient(repos.createRawHosted(HOSTED_REPO + "-no-deploy", DENY));

    HttpEntity testEntity = new FileEntity(resolveTestFile(TEST_CONTENT), TEXT_PLAIN);

    HttpResponse response = rawClient.put(TEST_CONTENT, testEntity);
    MatcherAssert.assertThat(response.getStatusLine().getStatusCode(), Matchers.is(HttpStatus.BAD_REQUEST));
    assertThat(response.getStatusLine().getReasonPhrase(), Matchers.containsString("is read-only"));
  }

  @Test
  public void canDisallowDelete() throws Exception {
    rawClient = rawClient(repos.createRawHosted(HOSTED_REPO + "-no-delete"));

    HttpEntity testEntity = new FileEntity(resolveTestFile(TEST_CONTENT), TEXT_PLAIN);

    HttpResponse response = rawClient.put(TEST_CONTENT, testEntity);
    MatcherAssert.assertThat(response.getStatusLine().getStatusCode(), Matchers.is(HttpStatus.CREATED));

    Configuration hostedConfig = repositoryManager.get(HOSTED_REPO + "-no-delete").getConfiguration().copy();
    hostedConfig.attributes(STORAGE).set(WRITE_POLICY, DENY);
    repositoryManager.update(hostedConfig);

    response = rawClient.delete(TEST_CONTENT);
    MatcherAssert.assertThat(response.getStatusLine().getStatusCode(), Matchers.is(HttpStatus.BAD_REQUEST));
    assertThat(response.getStatusLine().getReasonPhrase(), Matchers.containsString("cannot be deleted"));
  }

  @Test
  public void canDisallowRedeploy() throws Exception {
    rawClient = rawClient(repos.createRawHosted(HOSTED_REPO + "-no-redeploy", ALLOW_ONCE));

    HttpEntity testEntity = new FileEntity(resolveTestFile(TEST_CONTENT), TEXT_PLAIN);

    HttpResponse response = rawClient.put(TEST_CONTENT, testEntity);
    MatcherAssert.assertThat(response.getStatusLine().getStatusCode(), Matchers.is(HttpStatus.CREATED));

    response = rawClient.put(TEST_CONTENT, testEntity);
    MatcherAssert.assertThat(response.getStatusLine().getStatusCode(), Matchers.is(HttpStatus.BAD_REQUEST));
    assertThat(response.getStatusLine().getReasonPhrase(), Matchers.containsString("cannot be updated"));
  }
}
