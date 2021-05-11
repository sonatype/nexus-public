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

import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup;
import org.sonatype.nexus.content.testsupport.raw.RawClient;
import org.sonatype.nexus.content.testsupport.raw.RawITSupport;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.rest.internal.resources.ComponentsResource;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.view.ContentTypes;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.apache.http.entity.ContentType.APPLICATION_OCTET_STREAM;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.STORAGE;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.STRICT_CONTENT_TYPE_VALIDATION;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.WRITE_POLICY;
import static org.sonatype.nexus.repository.config.WritePolicy.ALLOW_ONCE;
import static org.sonatype.nexus.repository.config.WritePolicy.DENY;

@Category(SQLTestGroup.class)
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
    assertThat(response.getStatusLine().getStatusCode(), Matchers.is(HttpStatus.BAD_REQUEST));
    assertThat(response.getStatusLine().getReasonPhrase(), Matchers.containsString("is read-only"));
  }

  @Test
  public void canDisallowDelete() throws Exception {
    rawClient = rawClient(repos.createRawHosted(HOSTED_REPO + "-no-delete"));

    HttpEntity testEntity = new FileEntity(resolveTestFile(TEST_CONTENT), TEXT_PLAIN);

    HttpResponse response = rawClient.put(TEST_CONTENT, testEntity);
    assertThat(response.getStatusLine().getStatusCode(), Matchers.is(HttpStatus.CREATED));

    Configuration hostedConfig = repositoryManager.get(HOSTED_REPO + "-no-delete").getConfiguration().copy();
    hostedConfig.attributes(STORAGE).set(WRITE_POLICY, DENY);
    repositoryManager.update(hostedConfig);

    response = rawClient.delete(TEST_CONTENT);
    assertThat(response.getStatusLine().getStatusCode(), Matchers.is(HttpStatus.BAD_REQUEST));
    assertThat(response.getStatusLine().getReasonPhrase(), Matchers.containsString("cannot be deleted"));
  }

  @Test
  public void canDisallowRedeploy() throws Exception {
    rawClient = rawClient(repos.createRawHosted(HOSTED_REPO + "-no-redeploy", ALLOW_ONCE));

    HttpEntity testEntity = new FileEntity(resolveTestFile(TEST_CONTENT), TEXT_PLAIN);

    HttpResponse response = rawClient.put(TEST_CONTENT, testEntity);
    assertThat(response.getStatusLine().getStatusCode(), Matchers.is(HttpStatus.CREATED));

    response = rawClient.put(TEST_CONTENT, testEntity);
    assertThat(response.getStatusLine().getStatusCode(), Matchers.is(HttpStatus.BAD_REQUEST));
    assertThat(response.getStatusLine().getReasonPhrase(), Matchers.containsString("cannot be updated"));
  }

  @Test
  public void contentTypeDetectedFromContent() throws Exception {

    HttpEntity textEntity = new StringEntity("test");
    rawClient.put("path/to/content", textEntity);
    HttpResponse response = rawClient.get("path/to/content");
    MatcherAssert.assertThat(response.getFirstHeader("Content-Type").getValue(), Matchers.is(ContentTypes.TEXT_PLAIN));
    HttpClientUtils.closeQuietly(response);

    HttpEntity htmlEntity = new StringEntity("<html>...</html>");
    rawClient.put("path/to/content", htmlEntity);
    response = rawClient.get("path/to/content");
    MatcherAssert.assertThat(response.getFirstHeader("Content-Type").getValue(), Matchers.is(ContentTypes.TEXT_HTML));
    HttpClientUtils.closeQuietly(response);

    // turn off strict validation so we can test falling back to declared content-type
    Configuration hostedConfig = repositoryManager.get(HOSTED_REPO).getConfiguration().copy();
    hostedConfig.attributes(STORAGE).set(STRICT_CONTENT_TYPE_VALIDATION, false);
    repositoryManager.update(hostedConfig);

    HttpEntity jsonEntity = new StringEntity("", ContentType.APPLICATION_JSON);
    rawClient.put("path/to/content", jsonEntity);
    response = rawClient.get("path/to/content");
    MatcherAssert.assertThat(response.getFirstHeader("Content-Type").getValue(), Matchers.is(ContentTypes.APPLICATION_JSON));
    HttpClientUtils.closeQuietly(response);
  }

  @Test
  public void contentTypeDetectedFromPath() throws Exception {
    HttpEntity testEntity = new ByteArrayEntity(new byte[0]);

    rawClient.put("path/to/content.txt", testEntity);
    HttpResponse response = rawClient.get("path/to/content.txt");
    MatcherAssert.assertThat(response.getFirstHeader("Content-Type").getValue(), Matchers.is(ContentTypes.TEXT_PLAIN));
    HttpClientUtils.closeQuietly(response);

    rawClient.put("path/to/content.html", testEntity);
    response = rawClient.get("path/to/content.html");
    MatcherAssert.assertThat(response.getFirstHeader("Content-Type").getValue(), Matchers.is(ContentTypes.TEXT_HTML));
    HttpClientUtils.closeQuietly(response);
  }

  @Test
  public void strictContentTypeEnforced() throws Exception {

    // make sure strict validation is on for this test
    Configuration hostedConfig = repositoryManager.get(HOSTED_REPO).getConfiguration().copy();
    hostedConfig.attributes(STORAGE).set(STRICT_CONTENT_TYPE_VALIDATION, true);
    repositoryManager.update(hostedConfig);

    HttpEntity testEntity = new StringEntity("<html>...</html>");

    HttpResponse response = rawClient.put("path/to/content.jpg", testEntity);
    MatcherAssert.assertThat(response.getStatusLine().getStatusCode(), Matchers.is(HttpStatus.BAD_REQUEST));
    assertThat(response.getStatusLine().getReasonPhrase(),
        Matchers.containsString("Detected content type [text/html], but expected [image/jpeg]"));
  }

  @Test
  public void canUploadViaRest() throws Exception {
    try (CloseableHttpClient client = clientBuilder().build()) {
      MultipartEntityBuilder builder = MultipartEntityBuilder.create();
      builder.addTextBody("directory", "/foo");
      builder.addTextBody("asset1.filename", "file1.txt");
      builder.addBinaryBody("asset1", "content".getBytes(), APPLICATION_OCTET_STREAM, "file");

      HttpPost post = new HttpPost(String.format("%s%s%s?repository=%s",
          nexusUrl, REST_SERVICE_PATH, ComponentsResource.RESOURCE_URI, HOSTED_REPO));
      post.setEntity(builder.build());

      try (CloseableHttpResponse response = client.execute(post, clientContext())) {
        assertThat(response.getStatusLine().getStatusCode(), is(204));
      }
    }

    try (CloseableHttpResponse response = rawClient.get("foo/file1.txt")) {
      assertThat(response.getStatusLine().getStatusCode(), is(200));
    }
  }
}
