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
package org.sonatype.nexus.yum.internal.capabilities;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.capability.support.CapabilityDescriptorSupport;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.NumberTextFormField;
import org.sonatype.nexus.formfields.StringTextFormField;
import org.sonatype.nexus.plugins.capabilities.CapabilityIdentity;
import org.sonatype.nexus.plugins.capabilities.CapabilityRegistry;
import org.sonatype.nexus.plugins.capabilities.CapabilityType;
import org.sonatype.nexus.plugins.capabilities.Tag;
import org.sonatype.nexus.plugins.capabilities.Taggable;
import org.sonatype.nexus.plugins.capabilities.Validator;
import org.sonatype.nexus.plugins.capabilities.support.validator.Validators;
import org.sonatype.nexus.yum.YumRegistry;
import org.sonatype.sisu.goodies.i18n.I18N;
import org.sonatype.sisu.goodies.i18n.MessageBundle;

import com.google.common.collect.Lists;

import static org.sonatype.nexus.plugins.capabilities.CapabilityType.capabilityType;
import static org.sonatype.nexus.plugins.capabilities.Tag.categoryTag;
import static org.sonatype.nexus.plugins.capabilities.Tag.tags;

/**
 * {@link YumCapability} descriptor.
 *
 * @since yum 3.0
 */
@Singleton
@Named(YumCapabilityDescriptor.TYPE_ID)
public class YumCapabilityDescriptor
    extends CapabilityDescriptorSupport
    implements Taggable
{
  public static final String TYPE_ID = "yum";

  public static final CapabilityType TYPE = capabilityType(TYPE_ID);

  private static interface Messages
      extends MessageBundle
  {
    @DefaultMessage("Yum: Configuration")
    String name();

    @DefaultMessage("Max number of parallel threads")
    String maxNumberParallelThreadsLabel();

    @DefaultMessage("Maximum number of threads to be used for generating Yum repositories (default 10 threads)")
    String maxNumberParallelThreadsHelp();

    @DefaultMessage("Path of \"createrepo\"")
    String createrepoPathLabel();

    @DefaultMessage("Path of \"createrepo\" (e.g. /usr/bin/createrepo). You can set this value in sonatype-work/nexus/conf/capabilities.xml")
    String createrepoPathHelp();

    @DefaultMessage("Path of \"mergerepo\"")
    String mergerepoPathLabel();

    @DefaultMessage("Path of \"mergerepo\" (e.g. /usr/bin/mergerepo). You can set this value in sonatype-work/nexus/conf/capabilities.xml")
    String mergerepoPathHelp();
  }

  private static final Messages messages = I18N.create(Messages.class);

  private final Validators validators;

  private final List<FormField> formFields;

  private final CapabilityRegistry capabilityRegistry;

  @Inject
  public YumCapabilityDescriptor(final Validators validators, final CapabilityRegistry capabilityRegistry) {
    this.validators = validators;
    this.capabilityRegistry = capabilityRegistry;

    this.formFields = Lists.<FormField>newArrayList(
        new NumberTextFormField(
            YumCapabilityConfiguration.MAX_NUMBER_PARALLEL_THREADS,
            messages.maxNumberParallelThreadsLabel(),
            messages.maxNumberParallelThreadsHelp(),
            FormField.OPTIONAL
        ).withInitialValue(YumRegistry.DEFAULT_MAX_NUMBER_PARALLEL_THREADS),
        new StringTextFormField(
            YumCapabilityConfiguration.CREATEREPO_PATH,
            messages.createrepoPathLabel(),
            messages.createrepoPathHelp(),
            FormField.OPTIONAL,
            null,
            FormField.DISABLED
        ).withInitialValue(YumRegistry.DEFAULT_CREATEREPO_PATH),
        new StringTextFormField(
            YumCapabilityConfiguration.MERGEREPO_PATH,
            messages.mergerepoPathLabel(),
            messages.mergerepoPathHelp(),
            FormField.OPTIONAL,
            null,
            FormField.DISABLED
        ).withInitialValue(YumRegistry.DEFAULT_MERGEREPO_PATH)
    );
  }

  @Override
  public Validator validator() {
    return validators.logical().and(
        validators.capability().uniquePer(TYPE),
        new YumCapabilityCreateValidator()
    );
  }

  @Override
  public Validator validator(final CapabilityIdentity id) {
    return validators.logical().and(
        validators.capability().uniquePerExcluding(id, TYPE),
        new YumCapabilityUpdateValidator(capabilityRegistry.get(id).context().properties())
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
  public Set<Tag> getTags() {
    return tags(categoryTag("Yum"));
  }
}
