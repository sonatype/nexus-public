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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.goodies.common.InternalAccessible;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.thread.NexusThreadFactory;
import org.sonatype.nexus.webhooks.Webhook;
import org.sonatype.nexus.webhooks.WebhookRequest;
import org.sonatype.nexus.webhooks.WebhookRequestSendEvent;
import org.sonatype.nexus.webhooks.WebhookService;

import com.codahale.metrics.annotation.Gauge;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.common.io.BaseEncoding;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Thread.MIN_PRIORITY;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Default {@link WebhookService} implementation.
 *
 * @since 3.1
 */
@Named
@Singleton
public class WebhookServiceImpl
    extends ComponentSupport
    implements WebhookService, EventAware, EventAware.Asynchronous
{
  private static final String WEBHOOK_ID_HEADER = "X-Nexus-Webhook-ID";

  private static final String WEBHOOK_DELIVERY_HEADER = "X-Nexus-Webhook-Delivery";

  @VisibleForTesting
  static final String WEBHOOK_SIGNATURE_HEADER = "X-Nexus-Webhook-Signature";

  private static final String HMAC_SHA1 = "HmacSHA1";

  private static final BaseEncoding HEX = BaseEncoding.base16().lowerCase();

  private final ObjectMapper objectMapper = new ObjectMapper()
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .setSerializationInclusion(JsonInclude.Include.NON_NULL);

  private final Provider<CloseableHttpClient> httpClientProvider;

  private final List<Webhook> webhooks;

  private final ThreadPoolExecutor threadPoolExecutor;

  @Inject
  public WebhookServiceImpl(
      final Provider<CloseableHttpClient> httpClientProvider,
      final List<Webhook> webhooks,
      @Named("${nexus.webhook.pool.size:-128}") final int poolSize)
  {
    this.httpClientProvider = checkNotNull(httpClientProvider);
    this.webhooks = checkNotNull(webhooks);

    checkArgument(poolSize > 0, "Pool size must be greater than zero");
    this.threadPoolExecutor = new ThreadPoolExecutor(
        poolSize, // core-size
        poolSize, // max-size
        0L, // keep-alive
        TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(), // allow queueing up of requests
        new NexusThreadFactory("webhookService", "requestRool", MIN_PRIORITY),
        new AbortPolicy());
  }

  /**
   * Attempt to extract response body as string.
   */
  @Nullable
  private static String extractResponseBody(final HttpResponse response) throws IOException {
    HttpEntity entity = response.getEntity();
    if (entity != null) {
      try {
        String body = EntityUtils.toString(entity);
        if (body != null && body.length() != 0 && !body.contains("<html")) {
          return body;
        }
      }
      finally {
        EntityUtils.consume(entity);
      }
    }
    return null;
  }

  /**
   * Generate HMAC signature (HEX encoded) of given body using secret as key.
   */
  private static String sign(
      final String body,
      final String secret) throws NoSuchAlgorithmException, InvalidKeyException
  {
    SecretKeySpec key = new SecretKeySpec(secret.getBytes(), HMAC_SHA1);
    Mac mac = Mac.getInstance(HMAC_SHA1);
    mac.init(key);
    byte[] bytes = mac.doFinal(body.getBytes());
    return HEX.encode(bytes);
  }

  @Override
  public List<Webhook> getWebhooks() {
    return ImmutableList.copyOf(webhooks);
  }

  @Override
  public void queue(final WebhookRequest request) {
    checkNotNull(request);
    threadPoolExecutor.execute(() -> {
      try {
        send(request);
      }
      catch (Exception e) {
        log.error("Failed to send webhook request:{}", request, e);
      }
    });
  }

  /**
   * Asynchronous send handler.
   *
   * @see #queue(WebhookRequest)
   */
  @Subscribe
  @AllowConcurrentEvents
  @InternalAccessible
  void on(final WebhookRequestSendEvent event) {
    queue(event.getRequest());
  }

  @Override
  public void send(final WebhookRequest request) throws Exception {
    checkNotNull(request);

    log.debug("Sending webhook request: {}", request);

    Webhook webhook = request.getWebhook();
    String json = objectMapper.writeValueAsString(request.getPayload());

    HttpPost httpPost = new HttpPost(request.getUrl());
    httpPost.setHeader(WEBHOOK_ID_HEADER, webhook.getId());
    httpPost.setHeader(WEBHOOK_DELIVERY_HEADER, request.getId());
    // generate HMAC signature of body if secret is present
    if (!isEmpty(request.getSecret())) {
      httpPost.setHeader(WEBHOOK_SIGNATURE_HEADER, sign(json, request.getSecret()));
    }
    httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

    log.debug("Sending POST request: {}", httpPost);
    try (CloseableHttpClient httpClient = httpClientProvider.get();
        CloseableHttpResponse putResponse = httpClient.execute(httpPost)) {

      StatusLine status = putResponse.getStatusLine();
      log.debug("Response status: {}", status);

      // on exceptional status throw exception
      int code = status.getStatusCode();
      if (code >= 300) {
        String message = extractResponseBody(putResponse);
        if (message == null) {
          message = status.getReasonPhrase();
        }
        throw new HttpResponseException(code, message);
      }
    }
  }

  @VisibleForTesting
  public boolean isCalmPeriod() {
    return threadPoolExecutor.getQueue().isEmpty() && threadPoolExecutor.getActiveCount() == 0;
  }

  @Gauge(name = "nexus.webhooks.service.executor.queueSize")
  public int webhookQueueSize() {
    return threadPoolExecutor.getQueue().size();
  }
}
