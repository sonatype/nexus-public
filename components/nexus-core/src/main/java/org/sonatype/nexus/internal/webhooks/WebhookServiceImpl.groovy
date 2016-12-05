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
package org.sonatype.nexus.internal.webhooks

import javax.annotation.Nullable
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

import org.sonatype.goodies.common.ComponentSupport
import org.sonatype.goodies.common.InternalAccessible
import org.sonatype.nexus.common.event.EventAware
import org.sonatype.nexus.common.event.EventManager
import org.sonatype.nexus.webhooks.Webhook
import org.sonatype.nexus.webhooks.WebhookRequest
import org.sonatype.nexus.webhooks.WebhookRequestSendEvent
import org.sonatype.nexus.webhooks.WebhookService

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.google.common.collect.ImmutableList
import com.google.common.eventbus.AllowConcurrentEvents
import com.google.common.eventbus.Subscribe
import com.google.common.io.BaseEncoding
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.HttpResponseException
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.util.EntityUtils

import static com.google.common.base.Preconditions.checkNotNull

/**
 * Default {@link WebhookService} implementation.
 *
 * @since 3.1
 */
@Named
@Singleton
class WebhookServiceImpl
    extends ComponentSupport
    implements WebhookService, EventAware, EventAware.Asynchronous
{
  private static final String WEBHOOK_ID_HEADER = 'X-Nexus-Webhook-ID'

  private static final String WEBHOOK_DELIVERY_HEADER = 'X-Nexus-Webhook-Delivery'

  private static final String WEBHOOK_SIGNATURE_HEADER = 'X-Nexus-Webhook-Signature'

  @Inject
  EventManager eventManager

  @Inject
  Provider<CloseableHttpClient> httpClientProvider

  @Inject
  List<Webhook> webhooks

  private final ObjectMapper objectMapper = new ObjectMapper()
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .setSerializationInclusion(JsonInclude.Include.NON_NULL)

  /**
   * Returns all detected webhooks.
   */
  @Override
  List<Webhook> getWebhooks() {
    return ImmutableList.copyOf(webhooks)
  }

  @Override
  void queue(final WebhookRequest request) {
    checkNotNull(request)
    eventManager.post(new WebhookRequestSendEvent(request))
  }

  /**
   * Asynchronous send handler.
   *
   * @see #send(WebhookRequest)
   */
  @Subscribe
  @AllowConcurrentEvents
  @InternalAccessible
  void on(final WebhookRequestSendEvent event) {
    try {
      send(event.request)
    }
    catch (Exception e) {
      log.error("Failed to send webhook request: ${event.request}", e)
    }
  }

  /**
   * Send HTTP POST request.
   *
   * @throws HttpResponseException
   */
  @Override
  void send(final WebhookRequest request) {
    checkNotNull request

    log.debug("Sending webhook request: {}", request)

    def webhook = request.webhook
    def json = objectMapper.writeValueAsString(request.payload)

    httpClientProvider.get().withCloseable {CloseableHttpClient client ->
      HttpPost httpPost = new HttpPost(request.url)

      httpPost.setHeader(WEBHOOK_ID_HEADER, webhook.id)
      httpPost.setHeader(WEBHOOK_DELIVERY_HEADER, request.id)

      // generate HMAC signature of body if secret is present
      if (request.secret) {
        httpPost.setHeader(WEBHOOK_SIGNATURE_HEADER, sign(json, request.secret))
      }

      httpPost.entity = new StringEntity(json, ContentType.APPLICATION_JSON)

      log.debug("Sending POST request: {}", httpPost)
      client.execute(httpPost).withCloseable {CloseableHttpResponse response ->
        def status = response.statusLine
        log.debug("Response status: {}", status)

        // on exceptional status throw exception
        int code = status.statusCode
        if (code >= 300) {
          String message = extractResponseBody(response)
          if (message == null) {
            message = status.reasonPhrase
          }
          throw new HttpResponseException(code, message)
        }
      }
    }
  }

  /**
   * Attempt to extract response body as string.
   */
  @Nullable
  private static String extractResponseBody(final HttpResponse response) {
    HttpEntity entity = response.entity
    if (entity != null) {
      try {
        String body = EntityUtils.toString(entity)
        if (body != null && body.length() != 0 && !body.contains('<html')) {
          return body
        }
      }
      finally {
        EntityUtils.consume(entity)
      }
    }
    return null
  }

  private static final String HMAC_SHA1 = 'HmacSHA1'

  private static final BaseEncoding HEX = BaseEncoding.base16().lowerCase()

  /**
   * Generate HMAC signature (HEX encoded) of given body using secret as key.
   */
  private static String sign(final String body, final String secret) {
    def key = new SecretKeySpec(secret.bytes, HMAC_SHA1)
    def mac = Mac.getInstance(HMAC_SHA1)
    mac.init(key)
    def bytes = mac.doFinal(body.bytes)
    return HEX.encode(bytes)
  }
}
