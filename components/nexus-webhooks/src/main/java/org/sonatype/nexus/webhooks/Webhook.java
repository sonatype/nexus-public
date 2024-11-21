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

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.event.EventManager;

import com.google.common.collect.ImmutableSet;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Webhook.
 *
 * Implementations should be {@link Named} {@link Singleton} components.
 *
 * Type + name needs to be unique.
 *
 * This serves as a descriptor, but typically also will be an event-sink to listen for events and emit requests
 * to encode and send webhook requests.
 *
 * @since 3.1
 */
public abstract class Webhook
    extends ComponentSupport
{
  private EventManager eventManager;

  private final Set<SubscriptionImpl> subscriptions = new CopyOnWriteArraySet<>();

  @Inject
  public void setEventManager(final EventManager eventManager) {
    this.eventManager = checkNotNull(eventManager);
  }

  /**
   * Return the type of webhook.
   */
  public abstract WebhookType getType();

  /**
   * Return the name of the webhook.
   */
  public abstract String getName();

  /**
   * Helper to return rm:type:name.
   */
  public String getId() {
    return "rm:" + getType() + ":" + getName();
  }

  /**
   * Expose the subscriptions to sub-classes.
   */
  protected Set<WebhookSubscription> getSubscriptions() {
    return ImmutableSet.copyOf(subscriptions);
  }

  private class SubscriptionImpl
      implements WebhookSubscription
  {
    private final WebhookConfiguration configuration;

    public SubscriptionImpl(final WebhookConfiguration configuration) {
      this.configuration = configuration;
    }

    @Override
    public WebhookConfiguration getConfiguration() {
      return configuration;
    }

    @Override
    public void cancel() {
      unsubscribe(this);
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + "{" +
          "configuration=" + configuration +
          '}';
    }
  }

  /**
   * Subscribe to webhook.
   */
  public WebhookSubscription subscribe(final WebhookConfiguration configuration) {
    checkNotNull(configuration);

    synchronized (subscriptions) {
      SubscriptionImpl subscription = new SubscriptionImpl(configuration);
      subscriptions.add(subscription);
      log.debug("Added subscription: {}", subscription);

      // maybe start listening for events
      if (subscriptions.size() == 1) {
        eventManager.register(this);
        log.debug("Listening for events");
      }
      return subscription;
    }
  }

  /**
   * Unsubscribe from webhook.
   *
   * @see WebhookSubscription#cancel()
   */
  public void unsubscribe(final WebhookSubscription subscription) {
    checkNotNull(subscription);

    synchronized (subscriptions) {
      // noinspection SuspiciousMethodCalls
      subscriptions.remove(subscription);
      log.debug("Removed subscription: {}", subscription);

      // maybe stop listening for events
      if (subscriptions.isEmpty()) {
        eventManager.unregister(this);
        log.debug("Stopped listening for events");
      }
    }
  }

  /**
   * Create {@link WebhookRequest} for given body and emit {@link WebhookRequestSendEvent}.
   */
  protected void queue(final WebhookSubscription subscription, final WebhookPayload body) {
    log.debug("Queuing request for {} -> {}", subscription, body);
    WebhookRequest request = new WebhookRequest();
    request.setWebhook(this);
    request.setPayload(body);
    WebhookConfiguration configuration = subscription.getConfiguration();
    request.setUrl(configuration.getUrl());
    request.setSecret(configuration.getSecret());

    // using event here to avoid cyclic dependency between WebhookService and Webhook impls
    eventManager.post(new WebhookRequestSendEvent(request));
  }

  @Override
  public String toString() {
    return getId();
  }
}
