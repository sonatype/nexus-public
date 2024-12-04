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
package org.sonatype.nexus.repository.webhooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.capability.CapabilitySupport;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.capability.Condition;
import org.sonatype.nexus.capability.Tag;
import org.sonatype.nexus.capability.Taggable;
import org.sonatype.nexus.repository.capability.RepositoryConditions;
import org.sonatype.nexus.webhooks.WebhookService;
import org.sonatype.nexus.webhooks.WebhookSubscription;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.capability.CapabilityType.capabilityType;

@Named(RepositoryWebhookCapability.TYPE_ID)
public class RepositoryWebhookCapability
    extends CapabilitySupport<RepositoryWebhookCapabilityConfiguration>
    implements Taggable
{
  public static final String TYPE_ID = "webhook.repository";

  public static final CapabilityType TYPE = capabilityType(TYPE_ID);

  interface Messages
      extends MessageBundle
  {
    @DefaultMessage("Webhook: Repository")
    String name();

    @DefaultMessage("Webhook")
    String category();

    @DefaultMessage("Repository")
    String repositoryLabel();

    @DefaultMessage("Repository to discriminate events from")
    String repositoryHelp();

    @DefaultMessage("Event Types")
    String namesLabel();

    @DefaultMessage("Event types which trigger this Webhook")
    String namesHelp();

    @DefaultMessage("URL")
    String urlLabel();

    @DefaultMessage("Send a HTTP POST request to this URL")
    String urlHelp();

    @DefaultMessage("Secret Key")
    String secretLabel();

    @DefaultMessage("Key to use for HMAC payload digest")
    String secretHelp();

    @DefaultMessage("%s")
    String description(String names);
  }

  static final Messages messages = I18N.create(Messages.class);

  private final WebhookService webhookService;

  private final RepositoryConditions repositoryConditions;

  private final List<WebhookSubscription> subscriptions = new ArrayList<>();

  @Inject
  public RepositoryWebhookCapability(
      final WebhookService webhookService,
      final RepositoryConditions repositoryConditions)
  {
    this.webhookService = checkNotNull(webhookService);
    this.repositoryConditions = checkNotNull(repositoryConditions);
  }

  @Override
  protected RepositoryWebhookCapabilityConfiguration createConfig(final Map<String, String> properties) {
    return new RepositoryWebhookCapabilityConfiguration(properties);
  }

  @Override
  protected String renderDescription() {
    return messages.description(String.join(", ", getConfig().names));
  }

  @Override
  public Condition activationCondition() {
    return conditions().logical()
        .and(
            conditions().capabilities().passivateCapabilityDuringUpdate(),
            repositoryConditions.repositoryExists(() -> getConfig().repository));
  }

  @Override
  protected void onActivate(final RepositoryWebhookCapabilityConfiguration config) {
    webhookService.getWebhooks()
        .stream()
        .filter(webhook -> webhook.getType() == RepositoryWebhook.TYPE && config.names.contains(webhook.getName()))
        .forEach(webhook -> subscriptions.add(webhook.subscribe(config)));
  }

  @Override
  protected void onPassivate(final RepositoryWebhookCapabilityConfiguration config) {
    subscriptions.forEach(WebhookSubscription::cancel);
    subscriptions.clear();
  }

  @Override
  public Set<Tag> getTags() {
    return Set.of(
        Tag.categoryTag(messages.category()),
        Tag.repositoryTag(getConfig().repository));
  }

}
