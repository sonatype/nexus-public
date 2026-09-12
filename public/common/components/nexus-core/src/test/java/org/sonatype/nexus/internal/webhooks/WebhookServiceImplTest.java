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
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.sonatype.nexus.audit.InitiatorProvider;
import org.sonatype.nexus.internal.event.EventExecutor;
import org.sonatype.nexus.internal.event.EventManagerImpl;
import org.sonatype.nexus.internal.event.NexusEventAwareRegistrar;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.stateguard.InvalidStateException;
import org.sonatype.nexus.webhooks.WebhookRequestSendEvent;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.webhooks.GlobalRepositoryWebhook;
import org.sonatype.nexus.repository.webhooks.GlobalRepositoryWebhook.RepositoryWebhookPayload;
import org.sonatype.nexus.validation.ssrf.AntiSsrfService;
import org.sonatype.nexus.webhooks.Webhook;
import org.sonatype.nexus.webhooks.WebhookRequest;

import jakarta.inject.Provider;
import jakarta.validation.ValidationException;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.internal.webhooks.WebhookServiceImpl.WEBHOOK_SIGNATURE_HEADER;
import static org.sonatype.nexus.repository.webhooks.GlobalRepositoryWebhook.EventAction.CREATED;

/**
 * Tests {@link WebhookServiceImpl}
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class WebhookServiceImplTest
{
  private static final String SIGNATURE = "918cb6e16fcf197f2c3df5af2cf41b20974ec8a2";

  @Mock
  private Provider<CloseableHttpClient> httpClientProvider;

  @Mock
  private CloseableHttpClient httpClient;

  @Mock
  private AntiSsrfService antiSsrfService;

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

    underTest = new WebhookServiceImpl(httpClientProvider, antiSsrfService, webhooks, 1);

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
    underTest.start();

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

  @Test
  public void webhookBlockedForLocalhost() throws Exception {
    underTest.start();

    doThrow(new ValidationException("Host resolves to loopback address"))
        .when(antiSsrfService)
        .validateHost("localhost");

    RepositoryWebhookPayload.RepositoryPayload repository = new RepositoryWebhookPayload.RepositoryPayload("name",
        new TestType(), new TestFormat());

    RepositoryWebhookPayload payload =
        new RepositoryWebhookPayload(CREATED, repository, "nodeId", new Date(), "initiator");

    WebhookRequest request = new WebhookRequest();
    request.setUrl(new URI("http://localhost:8080/webhook"));
    request.setWebhook(new GlobalRepositoryWebhook(nodeAccess, initiatorProvider));
    request.setPayload(payload);

    IOException ex = assertThrows(IOException.class, () -> underTest.send(request));
    assertThat(ex.getMessage(), containsString("blocked by SSRF protection"));
  }

  @Test
  public void webhookBlockedForPrivateIp() throws Exception {
    underTest.start();

    doThrow(new ValidationException("Host resolves to private network address"))
        .when(antiSsrfService)
        .validateHost("10.0.0.1");

    RepositoryWebhookPayload.RepositoryPayload repository = new RepositoryWebhookPayload.RepositoryPayload("name",
        new TestType(), new TestFormat());

    RepositoryWebhookPayload payload =
        new RepositoryWebhookPayload(CREATED, repository, "nodeId", new Date(), "initiator");

    WebhookRequest request = new WebhookRequest();
    request.setUrl(new URI("http://10.0.0.1/webhook"));
    request.setWebhook(new GlobalRepositoryWebhook(nodeAccess, initiatorProvider));
    request.setPayload(payload);

    IOException ex = assertThrows(IOException.class, () -> underTest.send(request));
    assertThat(ex.getMessage(), containsString("blocked by SSRF protection"));
  }

  @Test
  public void sendFailsAfterStop() throws Exception {
    underTest.start();
    underTest.stop();

    WebhookRequest request = new WebhookRequest();
    request.setUrl(new URI("uri"));
    request.setWebhook(new GlobalRepositoryWebhook(nodeAccess, initiatorProvider));

    assertThrows(InvalidStateException.class, () -> underTest.send(request));
  }

  // Note: Event bus registration is handled automatically for EventAware beans by
  // NexusEventAwareRegistrar (subscribers are discovered by type and registered with the EventManager at
  // the appropriate lifecycle phase). EventAware.Asynchronous now EXTENDS EventAware and simply routes
  // registration to the async bus, so declaring Asynchronous is sufficient — the NEXUS-52911 "dropped
  // EventAware marker" trap (fixed under NEXUS-53667) can no longer occur; see
  // {@link #implementsBothEventAwareMarkers()} and
  // {@link #webhookRequestSendEventTriggersHttpPostViaEventBus()} below.
  // The @Guarded(by = STARTED) annotation on queue() ensures events received after
  // stop() are rejected with InvalidStateException, making explicit unregister unnecessary.

  @Test
  public void queueFailsAfterStop() throws Exception {
    underTest.start();
    underTest.stop();

    WebhookRequest request = new WebhookRequest();
    request.setUrl(new URI("uri"));
    request.setWebhook(new GlobalRepositoryWebhook(nodeAccess, initiatorProvider));

    assertThrows(InvalidStateException.class, () -> underTest.queue(request));
  }

  /**
   * Regression test for NEXUS-53667: {@link WebhookServiceImpl} must be an async {@link EventAware}
   * subscriber so that {@code NexusEventAwareRegistrar} discovers it and registers it with the
   * {@code EventManager} for {@code WebhookRequestSendEvent}, on the async bus.
   * <p>
   * Regression window: NEXUS-52911 (Jun 2026) refactored the class to extend
   * {@code StateGuardLifecycleSupport} and accidentally dropped {@code EventAware} from the
   * {@code implements} clause, leaving only {@code EventAware.Asynchronous}. At the time
   * {@code Asynchronous} did NOT extend {@code EventAware}, so the framework stopped collecting
   * {@code WebhookServiceImpl}; its {@code @Subscribe void on(WebhookRequestSendEvent)} was never
   * registered on the async bus, and every webhook silently stopped firing.
   * <p>
   * {@code Asynchronous} now extends {@code EventAware}, so that exact drop can no longer happen.
   * This still pins that {@code WebhookServiceImpl} remains an async event subscriber.
   */
  @Test
  public void implementsBothEventAwareMarkers() {
    assertThat("WebhookServiceImpl must implement EventAware so it is picked up by "
        + "NexusEventAwareRegistrar's type-based auto-registration",
        underTest, is(instanceOf(EventAware.class)));
    assertThat("WebhookServiceImpl must implement EventAware.Asynchronous so it registers on "
        + "the async bus (avoiding blocking Jetty threads on webhook dispatch)",
        underTest, is(instanceOf(EventAware.Asynchronous.class)));
  }

  /**
   * Behavioural regression test for NEXUS-53667: the end-to-end dispatch path from a
   * {@link WebhookRequestSendEvent} posted on the {@link EventManagerImpl}'s bus to an outbound
   * HTTP POST must remain intact.
   * <p>
   * The structural pin {@link #implementsBothEventAwareMarkers()} above catches the specific
   * NEXUS-52911 regression (a missing marker interface), but it does not catch other equally
   * silent failures on the same path:
   * <ul>
   * <li>removing or renaming {@code @Subscribe void on(WebhookRequestSendEvent)},</li>
   * <li>renaming {@link WebhookRequestSendEvent} or altering its wire shape,</li>
   * <li>changing the registration mechanism so {@link EventAware} beans are no longer
   * registered with the {@link EventManager}.</li>
   * </ul>
   * Each of those would leave the marker-interface test green while dispatch silently breaks,
   * which is exactly the failure mode that shipped dark for ~3 weeks pre-NEXUS-53667. Wiring
   * a real {@link EventManagerImpl}, registering {@code underTest} exactly as
   * {@link NexusEventAwareRegistrar} does at runtime, posting the event, and verifying the
   * mocked {@code httpClient.execute(...)} is invoked exercises the whole chain.
   */
  @Test
  public void webhookRequestSendEventTriggersHttpPostViaEventBus() throws Exception {
    // Real EventManagerImpl; register underTest exactly as NexusEventAwareRegistrar would at
    // runtime (it collects EventAware beans via getBeansOfType and calls eventManager.register).
    EventExecutor executor = new EventExecutor(false, 0, Duration.ofSeconds(0), false, false);
    EventManagerImpl eventManager = new EventManagerImpl(executor);
    eventManager.start(); // EventManager only posts once started (EVENTS phase); mirror that here
    eventManager.register(underTest);

    underTest.start();

    // Build a minimally-valid WebhookRequest so send() can drive the HTTP pipeline. Any
    // concrete WebhookPayload subclass works; RepositoryWebhookPayload is already used
    // elsewhere in this test.
    RepositoryWebhookPayload payload = new RepositoryWebhookPayload(
        CREATED,
        new RepositoryWebhookPayload.RepositoryPayload("name", new TestType(), new TestFormat()),
        "nodeId",
        new Date(),
        "initiator");
    WebhookRequest request = new WebhookRequest();
    request.setUrl(new URI("http://example.test/hook"));
    request.setWebhook(new GlobalRepositoryWebhook(nodeAccess, initiatorProvider));
    request.setPayload(payload);

    // Post via the real EventManager — mirrors what Webhook.queue(...) does in production.
    eventManager.post(new WebhookRequestSendEvent(request));

    // Dispatch on the async bus and WebhookServiceImpl's own executor is async; give it up to
    // 2 seconds to actually POST. If registration is broken, no invocation happens and this
    // fails cleanly rather than hanging.
    verify(httpClient, timeout(2000)).execute(any(HttpPost.class));
  }
}
