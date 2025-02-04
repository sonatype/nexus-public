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
package org.sonatype.nexus.internal.webhooks;

import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.inject.Provider;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.audit.InitiatorProvider;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.webhooks.GlobalRepositoryWebhook;
import org.sonatype.nexus.repository.webhooks.GlobalRepositoryWebhook.RepositoryWebhookPayload;
import org.sonatype.nexus.webhooks.Webhook;
import org.sonatype.nexus.webhooks.WebhookRequest;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.internal.webhooks.WebhookServiceImpl.WEBHOOK_SIGNATURE_HEADER;
import static org.sonatype.nexus.repository.webhooks.GlobalRepositoryWebhook.EventAction.CREATED;

/**
 * Tests {@link WebhookServiceImpl}
 */
public class WebhookServiceImplTest
    extends TestSupport
{
  private static final String SIGNATURE = "918cb6e16fcf197f2c3df5af2cf41b20974ec8a2";

  @Mock
  private Provider<CloseableHttpClient> httpClientProvider;

  @Mock
  private CloseableHttpClient httpClient;

  @Mock
  private NodeAccess nodeAccess;

  @Mock
  private InitiatorProvider initiatorProvider;

  private ArgumentCaptor<HttpPost> postCaptor;

  private WebhookServiceImpl underTest;

  private static class TestFormat
      extends Format
  {
    public TestFormat() {
      super("format");
    }
  }

  private static class TestType
      extends Type
  {
    public TestType() {
      super("value");
    }

    @Override
    public Class<?> getValidationGroup() {
      return null;
    }
  }

  @Before
  public void setup() throws IOException {
    List<Webhook> webhooks = emptyList();

    underTest = new WebhookServiceImpl(httpClientProvider, webhooks, 1);

    when(httpClientProvider.get()).thenReturn(httpClient);

    postCaptor = ArgumentCaptor.forClass(HttpPost.class);
    CloseableHttpResponse response = mock(CloseableHttpResponse.class);
    StatusLine statusLine = mock(StatusLine.class);
    when(statusLine.getStatusCode()).thenReturn(200);
    when(response.getStatusLine()).thenReturn(statusLine);
    when(httpClient.execute(postCaptor.capture())).thenReturn(response);
  }

  @Test
  public void properlyMarshalsAndSignsPayload() throws Exception {
    String expectedDate = "2016-08-18T18:18:30.326+0000";
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS+0000");
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    Date timestamp = dateFormat.parse(expectedDate);

    RepositoryWebhookPayload.RepositoryPayload repository = new RepositoryWebhookPayload.RepositoryPayload("name",
        new TestType(), new TestFormat());

    RepositoryWebhookPayload payload =
        new RepositoryWebhookPayload(CREATED, repository, "nodeId", timestamp, "initiator");

    WebhookRequest request = new WebhookRequest();
    request.setUrl(new URI("uri"));
    request.setSecret("secret");
    request.setWebhook(new GlobalRepositoryWebhook(nodeAccess, initiatorProvider));
    request.setPayload(payload);

    underTest.send(request);

    HttpPost post = postCaptor.getValue();

    String expectedPayload = "{\"timestamp\":\"2016-08-18T18:18:30.326+00:00\",\"nodeId\":\"nodeId\"," +
        "\"initiator\":\"initiator\",\"action\":\"CREATED\",\"repository\":{\"format\":\"format\",\"name\":\"name\"," +
        "\"type\":\"value\"}}";
    assertThat(EntityUtils.toString(post.getEntity()), equalTo(expectedPayload));
    assertThat(post.getFirstHeader(WEBHOOK_SIGNATURE_HEADER).getValue(), equalTo(SIGNATURE));
  }
}
