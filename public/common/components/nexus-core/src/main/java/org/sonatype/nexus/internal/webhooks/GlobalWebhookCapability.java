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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import org.sonatype.nexus.common.i18n.I18N;
import org.sonatype.nexus.common.i18n.MessageBundle;
import org.sonatype.nexus.capability.CapabilityConfigurationSupport;
import org.sonatype.nexus.capability.CapabilityDescriptorSupport;
import org.sonatype.nexus.capability.CapabilitySupport;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.capability.Condition;
import org.sonatype.nexus.capability.Tag;
import org.sonatype.nexus.capability.Taggable;
import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.crypto.secrets.SecretsStore;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.ItemselectFormField;
import org.sonatype.nexus.formfields.PasswordFormField;
import org.sonatype.nexus.formfields.UrlFormField;
import org.sonatype.nexus.validation.constraint.Url;
import org.sonatype.nexus.webhooks.GlobalWebhook;
import org.sonatype.nexus.webhooks.WebhookConfiguration;
import org.sonatype.nexus.webhooks.WebhookService;
import org.sonatype.nexus.webhooks.WebhookSubscription;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.capability.CapabilityType.capabilityType;

@Component(GlobalWebhookCapability.TYPE_ID)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GlobalWebhookCapability
    extends CapabilitySupport<GlobalWebhookCapability.Configuration>
{
  public static final String TYPE_ID = "webhook.global";

  public static final CapabilityType TYPE = capabilityType(TYPE_ID);

  interface Messages
      extends MessageBundle
  {
    @DefaultMessage("Webhook: Global")
    String name();

    @DefaultMessage("Webhook")
    String category();

    @DefaultMessage("Event Types")
    String namesLabel();

    @DefaultMessage("Event types which trigger this Webhook.\n\nNote: The 'firewall_quarantine' event requires IQ Server and Repository Firewall to be configured.")
    String namesHelp();

    @DefaultMessage("URL")
    String urlLabel();

    @DefaultMessage("Send an HTTP POST request to this URL")
    String urlHelp();

    @DefaultMessage("Secret Key")
    String secretLabel();

    @DefaultMessage("Key to use for HMAC payload digest")
    String secretHelp();

    @DefaultMessage("%s")
    String description(String names);
  }

  private static final Messages messages = I18N.create(Messages.class);

  @Autowired
  private WebhookService webhookService;

  private final List<WebhookSubscription> subscriptions = new ArrayList<>();

  @Override
  protected Configuration createConfig(final Map<String, String> properties) throws Exception {
    return new GlobalWebhookCapability.Configuration(properties, secretsService(), secretsStore());
  }

  @Override
  protected String renderDescription() {
    return messages.description(String.join(", ", getConfig().names));
  }

  @Override
  public Condition activationCondition() {
    Condition baseCondition = conditions().capabilities().passivateCapabilityDuringUpdate();

    // If firewall_quarantine webhook is selected, add condition to require Firewall capability
    if (getConfig() != null && getConfig().names.contains("firewall_quarantine")) {
      CapabilityType firewallType = capabilityType("firewall.audit");
      Condition firewallCondition = conditions().capabilities().capabilityOfTypeActive(firewallType);
      return conditions().logical().and(baseCondition, firewallCondition);
    }

    return baseCondition;
  }

  @Override
  public void onActivate(final Configuration config) {
    webhookService.getWebhooks()
        .stream()
        .filter(webhook -> webhook.getType() == GlobalWebhook.TYPE && config.names.contains(webhook.getName()))
        .forEach(webhook -> subscriptions.add(webhook.subscribe(config)));
  }

  @Override
  public void onPassivate(final Configuration config) {
    subscriptions.forEach(WebhookSubscription::cancel);
    subscriptions.clear();
  }

  public static class Configuration
      extends CapabilityConfigurationSupport
      implements WebhookConfiguration
  {
    private static final String P_NAMES = "names";

    private static final String P_URL = "url";

    private static final String P_SECRET = "secret";

    private static final Splitter LIST_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

    @GlobalWebhookType
    public List<String> names;

    @Url
    public URI url;

    @Nullable
    private final String secretId;

    private final SecretsService secretsService;

    private final SecretsStore secretsStore;

    public Configuration(
        final Map<String, String> properties,
        final SecretsService secretsService,
        final SecretsStore secretsStore)
    {
      this.names = parseList(properties.get(P_NAMES));
      this.url = parseUri(properties.get(P_URL));
      // Store only the secret ID - decrypt on-demand when needed
      this.secretId = Strings.emptyToNull(properties.get(P_SECRET));
      this.secretsService = secretsService;
      this.secretsStore = secretsStore;
    }

    private static List<String> parseList(final String value) {
      List<String> result = new ArrayList<>();
      LIST_SPLITTER.split(value).forEach(result::add);
      return result;
    }

    @Override
    public URI getUrl() {
      return url;
    }

    @Nullable
    @Override
    public String getSecret() {
      return decryptSecret(secretId, secretsStore, secretsService);
    }
  }

  @AvailabilityVersion(from = "1.0")
  @Component
  @Qualifier(TYPE_ID)
  public static class Descriptor
      extends CapabilityDescriptorSupport<Configuration>
      implements Taggable
  {
    private final FormField names;

    private final FormField url;

    private final FormField secret;

    public Descriptor() {
      this.names = new ItemselectFormField(
          Configuration.P_NAMES,
          messages.namesLabel(),
          messages.namesHelp(),
          FormField.MANDATORY).withStoreApi("coreui_Webhook.listWithTypeGlobal")
              .withButtons(new String[]{"add", "remove"})
              .withFromTitle("Available")
              .withToTitle("Selected");

      this.url = new UrlFormField(
          Configuration.P_URL,
          messages.urlLabel(),
          messages.urlHelp(),
          FormField.MANDATORY);

      this.secret = new PasswordFormField(
          Configuration.P_SECRET,
          messages.secretLabel(),
          messages.secretHelp(),
          FormField.OPTIONAL);
    }

    @Override
    public CapabilityType type() {
      return TYPE;
    }

    @Override
    public String name() {
      return messages.name();
    }

    @Override
    public List<FormField> formFields() {
      return List.of(names, url, secret);
    }

    @Override
    protected Configuration createConfig(final Map<String, String> properties) {
      // This is only used for validation - pass null for secrets as they won't be accessed during validation
      return new Configuration(properties, null, null);
    }

    @Override
    protected String renderAbout() {
      return render(TYPE_ID + "-about.vm");
    }

    @Override
    public Set<Tag> getTags() {
      return ImmutableSet.of(Tag.categoryTag(messages.category()));
    }
  }
}
