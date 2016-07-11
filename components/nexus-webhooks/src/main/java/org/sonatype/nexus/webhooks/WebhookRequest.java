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
package org.sonatype.nexus.webhooks;

import java.net.URI;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * Webhook request.
 *
 * @since 3.1
 */
public class WebhookRequest
{
  private final String id = UUID.randomUUID().toString();

  private Webhook webhook;

  private WebhookPayload payload;

  private URI url;

  @Nullable
  private String secret;

  public String getId() {
    return id;
  }

  public Webhook getWebhook() {
    return webhook;
  }

  public void setWebhook(final Webhook webhook) {
    this.webhook = webhook;
  }

  public WebhookPayload getPayload() {
    return payload;
  }

  public void setPayload(final WebhookPayload payload) {
    this.payload = payload;
  }

  public URI getUrl() {
    return url;
  }

  public void setUrl(final URI url) {
    this.url = url;
  }

  @Nullable
  public String getSecret() {
    return secret;
  }

  public void setSecret(@Nullable final String secret) {
    this.secret = secret;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "id='" + id + '\'' +
        ", webhook=" + webhook +
        ", payload=" + payload +
        ", url=" + url +
        '}';
  }
}
