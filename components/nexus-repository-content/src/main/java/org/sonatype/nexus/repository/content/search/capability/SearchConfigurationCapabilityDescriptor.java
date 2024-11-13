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
package org.sonatype.nexus.repository.content.search.capability;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.capability.CapabilityDescriptorSupport;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.capability.Tag;
import org.sonatype.nexus.capability.Taggable;
import org.sonatype.nexus.common.app.FeatureFlags;
import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.NumberTextFormField;

import static org.sonatype.nexus.capability.CapabilityType.capabilityType;
import static org.sonatype.nexus.capability.Tag.categoryTag;
import static org.sonatype.nexus.capability.Tag.tags;
import static org.sonatype.nexus.formfields.FormField.MANDATORY;

@AvailabilityVersion(from = "2.1")
@Named(SearchConfigurationCapabilityDescriptor.TYPE_ID)
@Singleton
public class SearchConfigurationCapabilityDescriptor
    extends CapabilityDescriptorSupport<Integer>
    implements Taggable
{
  /**
   * {@link SearchConfigurationCapability} type ID (ha search)
   */
  public static final String TYPE_ID = "nexus.search.configuration";

  /**
   * {@link SearchConfigurationCapability} type
   */
  public static final CapabilityType TYPE = capabilityType(TYPE_ID);

  private interface Messages
      extends MessageBundle
  {
    @DefaultMessage("Search Configuration")
    String name();

    @DefaultMessage("Refetch Limit")
    String refetchLimitLabel();

    @DefaultMessage("The number of times Nexus can return to the database to populate a given page. Default 10. This can impact performance.")
    String refetchLimitHelp();
  }

  private static final Messages messages = I18N.create(Messages.class);

  private final List<FormField> formFields;

  /**
   * As users may toggle HA, this exists outside and it is only shown to the user while HA is enabled.
   */
  @Inject
  public SearchConfigurationCapabilityDescriptor(
      @Named(FeatureFlags.DATASTORE_TABLE_SEARCH_NAMED) final boolean haEnabled)
  {
    setExposed(haEnabled);
    setHidden(haEnabled);

    formFields = Collections.singletonList(
        new NumberTextFormField(
            SearchConfigurationCapability.REFETCH_LIMIT,
            messages.refetchLimitLabel(),
            messages.refetchLimitHelp(),
            MANDATORY,
            "\\d*"
        )
    );
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
    return formFields;
  }

  @Override
  protected String renderAbout() throws Exception {
    return render("searchconfiguration-about.vm");
  }

  @Override
  public Set<Tag> getTags() {
    return tags(categoryTag("Search"));
  }
}
