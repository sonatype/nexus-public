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

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.capability.CapabilityDescriptorSupport;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.capability.Tag;
import org.sonatype.nexus.capability.Taggable;
import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.ItemselectFormField;
import org.sonatype.nexus.formfields.PasswordFormField;
import org.sonatype.nexus.formfields.RepositoryCombobox;
import org.sonatype.nexus.formfields.UrlFormField;
import org.sonatype.nexus.repository.types.GroupType;

import static org.sonatype.nexus.repository.webhooks.RepositoryWebhookCapability.TYPE;
import static org.sonatype.nexus.repository.webhooks.RepositoryWebhookCapability.TYPE_ID;
import static org.sonatype.nexus.repository.webhooks.RepositoryWebhookCapability.messages;
import static org.sonatype.nexus.repository.webhooks.RepositoryWebhookCapabilityConfiguration.P_NAMES;
import static org.sonatype.nexus.repository.webhooks.RepositoryWebhookCapabilityConfiguration.P_REPOSITORY;
import static org.sonatype.nexus.repository.webhooks.RepositoryWebhookCapabilityConfiguration.P_SECRET;
import static org.sonatype.nexus.repository.webhooks.RepositoryWebhookCapabilityConfiguration.P_URL;

@AvailabilityVersion(from = "1.0")
@Named(TYPE_ID)
@Singleton
public class RepositoryWebhookCapabilityDescriptor
    extends CapabilityDescriptorSupport<RepositoryWebhookCapabilityConfiguration>
    implements Taggable
{
  private final FormField<String> repository;

  private final FormField<String> names;

  private final FormField<String> url;

  private final FormField<String> secret;

  public RepositoryWebhookCapabilityDescriptor() {
    setExposed(true);
    setHidden(false);

    this.repository = new RepositoryCombobox(
        P_REPOSITORY,
        messages.repositoryLabel(),
        messages.repositoryHelp(),
        FormField.MANDATORY).excludingAnyOfTypes(GroupType.NAME);

    this.names = new ItemselectFormField(
        P_NAMES,
        messages.namesLabel(),
        messages.namesHelp(),
        FormField.MANDATORY).withStoreApi("coreui_Webhook.listWithTypeRepository")
            .withButtons("add", "remove")
            .withFromTitle("Available")
            .withToTitle("Selected");

    this.url = new UrlFormField(
        P_URL,
        messages.urlLabel(),
        messages.urlHelp(),
        FormField.MANDATORY);

    this.secret = new PasswordFormField(
        P_SECRET,
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
    return List.of(repository, names, url, secret);
  }

  @Override
  protected RepositoryWebhookCapabilityConfiguration createConfig(final Map<String, String> properties) {
    return new RepositoryWebhookCapabilityConfiguration(properties);
  }

  @Override
  protected String renderAbout() {
    return render(TYPE_ID + "-about.vm");
  }

  @Override
  public Set<Tag> getTags() {
    return Set.of(Tag.categoryTag(messages.category()));
  }

  @Override
  protected Set<String> uniqueProperties() {
    return Set.of(P_REPOSITORY, P_URL);
  }
}
