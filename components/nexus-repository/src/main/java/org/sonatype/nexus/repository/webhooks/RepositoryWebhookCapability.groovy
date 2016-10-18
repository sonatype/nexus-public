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
package org.sonatype.nexus.repository.webhooks

import javax.annotation.Nullable
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.goodies.i18n.I18N
import org.sonatype.goodies.i18n.MessageBundle
import org.sonatype.goodies.i18n.MessageBundle.DefaultMessage
import org.sonatype.nexus.capability.CapabilityConfigurationSupport
import org.sonatype.nexus.capability.CapabilityDescriptorSupport
import org.sonatype.nexus.capability.CapabilitySupport
import org.sonatype.nexus.capability.CapabilityType
import org.sonatype.nexus.capability.Condition
import org.sonatype.nexus.capability.Tag
import org.sonatype.nexus.capability.Taggable
import org.sonatype.nexus.formfields.FormField
import org.sonatype.nexus.formfields.ItemselectFormField
import org.sonatype.nexus.formfields.PasswordFormField
import org.sonatype.nexus.formfields.RepositoryCombobox
import org.sonatype.nexus.formfields.StringTextFormField
import org.sonatype.nexus.repository.capability.RepositoryConditions
import org.sonatype.nexus.repository.types.GroupType
import org.sonatype.nexus.webhooks.WebhookService
import org.sonatype.nexus.webhooks.WebhookSubscription

import com.google.common.base.Splitter
import com.google.common.base.Strings
import groovy.transform.PackageScope
import groovy.transform.ToString

import static org.sonatype.nexus.capability.CapabilityType.capabilityType

/**
 * Capability to manage {@link RepositoryWebhook} configuration.
 *
 * @since 3.1
 */
@Named(RepositoryWebhookCapability.TYPE_ID)
class RepositoryWebhookCapability
    extends CapabilitySupport<Configuration>
    implements Taggable
{
  public static final String TYPE_ID = 'webhook.repository'

  public static final CapabilityType TYPE = capabilityType(TYPE_ID)

  private static interface Messages
      extends MessageBundle
  {
    @DefaultMessage('Webhook: Repository')
    String name()

    @DefaultMessage('Webhook')
    String category()

    @DefaultMessage('Repository')
    String repositoryLabel()

    @DefaultMessage('Repository to discriminate events from')
    String repositoryHelp()

    @DefaultMessage('Event Types')
    String namesLabel()

    @DefaultMessage('Event types which trigger this Webhook')
    String namesHelp()

    @DefaultMessage('URL')
    String urlLabel()

    @DefaultMessage('Send a HTTP POST request to this URL')
    String urlHelp()

    @DefaultMessage('Secret Key')
    String secretLabel()

    @DefaultMessage('Key to use for HMAC payload digest')
    String secretHelp()

    @DefaultMessage('%s')
    String description(String names)
  }

  @PackageScope
  static final Messages messages = I18N.create(Messages.class)

  @Inject
  WebhookService webhookService

  @Inject
  RepositoryConditions repositoryConditions

  private final List<WebhookSubscription> subscriptions = []

  @Override
  protected Configuration createConfig(final Map<String, String> properties) {
    return new Configuration(properties)
  }

  @Override
  protected String renderDescription() {
    return messages.description(config.names.join(', '))
  }

  @Override
  Condition activationCondition() {
    return conditions().logical().and(
        conditions().capabilities().passivateCapabilityDuringUpdate(),
        repositoryConditions.repositoryExists({config.repository})
    )
  }

  /**
   * Subscribe to each configured webhook.
   */
  @Override
  protected void onActivate(final Configuration config) {
    webhookService.webhooks.findAll {
      it.type == RepositoryWebhook.TYPE && it.name in config.names
    }.each {
      subscriptions << it.subscribe(config)
    }
  }

  /**
   * Cancel each webhook subscription.
   */
  @Override
  protected void onPassivate(final Configuration config) {
    subscriptions.each {it.cancel()}
    subscriptions.clear()
  }

  @Override
  Set<Tag> getTags() {
    return [
        Tag.categoryTag(messages.category()),
        Tag.repositoryTag(config.repository)
    ]
  }

  //
  // Configuration
  //

  private static final String P_REPOSITORY = 'repository'

  private static final String P_NAMES = 'names'

  private static final String P_URL = 'url'

  private static final String P_SECRET = 'secret'

  @ToString(includePackage = false, includeNames = true, excludes = ['secret'])
  static class Configuration
      extends CapabilityConfigurationSupport
      implements RepositoryWebhook.Configuration
  {
    String repository

    List<String> names

    URI url

    @Nullable
    String secret

    Configuration(final Map<String, String> properties) {
      repository = properties[P_REPOSITORY]
      names = parseList(properties[P_NAMES])
      url = parseUri(properties[P_URL])
      secret = Strings.emptyToNull(properties[P_SECRET])
    }

    private static final Splitter LIST_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings()

    private static List<String> parseList(final String value) {
      List<String> result = []
      result.addAll(LIST_SPLITTER.split(value))
      return result
    }
  }

  //
  // Descriptor
  //

  @Named(RepositoryWebhookCapability.TYPE_ID)
  @Singleton
  static public class Descriptor
      extends CapabilityDescriptorSupport<Configuration>
      implements Taggable
  {
    private final FormField repository

    // TODO: content-selector

    private final FormField names

    private final FormField url

    private final FormField secret

    Descriptor() {
      this.exposed = true
      this.hidden = false

      this.repository = new RepositoryCombobox(
          P_REPOSITORY,
          messages.repositoryLabel(),
          messages.repositoryHelp(),
          FormField.MANDATORY
      ).excludingAnyOfTypes(GroupType.NAME)

      this.names = new ItemselectFormField(
          P_NAMES,
          messages.namesLabel(),
          messages.namesHelp(),
          FormField.MANDATORY
      ).with {
        storeApi = 'coreui_Webhook.listWithTypeRepository'
        buttons = ['add', 'remove']
        fromTitle = 'Available'
        toTitle = 'Selected'
        return it
      }

      this.url = new StringTextFormField(
          P_URL,
          messages.urlLabel(),
          messages.urlHelp(),
          FormField.MANDATORY
      )

      this.secret = new PasswordFormField(
          P_SECRET,
          messages.secretLabel(),
          messages.secretHelp(),
          FormField.OPTIONAL
      )
    }

    @Override
    CapabilityType type() {
      return TYPE
    }

    @Override
    String name() {
      return messages.name()
    }

    @Override
    List<FormField> formFields() {
      return [repository, names, url, secret]
    }

    @Override
    protected Configuration createConfig(final Map<String, String> properties) {
      return new Configuration(properties)
    }

    @Override
    protected String renderAbout() {
      return render("$TYPE_ID-about.vm")
    }

    @Override
    Set<Tag> getTags() {
      return [Tag.categoryTag(messages.category())]
    }
  }
}
