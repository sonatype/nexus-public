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
import org.sonatype.nexus.formfields.CheckboxFormField;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.NumberTextFormField;
import org.sonatype.nexus.formfields.RepositoryCombobox;
import org.sonatype.nexus.formfields.StringTextFormField;
import org.sonatype.nexus.formfields.TextAreaFormField;
import org.sonatype.nexus.plugins.capabilities.CapabilityIdentity;
import org.sonatype.nexus.plugins.capabilities.CapabilityType;
import org.sonatype.nexus.plugins.capabilities.Tag;
import org.sonatype.nexus.plugins.capabilities.Taggable;
import org.sonatype.nexus.plugins.capabilities.Validator;
import org.sonatype.nexus.proxy.maven.MavenHostedRepository;
import org.sonatype.sisu.goodies.i18n.I18N;
import org.sonatype.sisu.goodies.i18n.MessageBundle;

import com.google.common.collect.Lists;

import static org.sonatype.nexus.plugins.capabilities.CapabilityType.capabilityType;
import static org.sonatype.nexus.plugins.capabilities.Tag.categoryTag;
import static org.sonatype.nexus.plugins.capabilities.Tag.tags;
import static org.sonatype.nexus.yum.internal.capabilities.GenerateMetadataCapabilityConfiguration.ALIASES;
import static org.sonatype.nexus.yum.internal.capabilities.GenerateMetadataCapabilityConfiguration.REPOSITORY_ID;

/**
 * @since yum 3.0
 */
@Singleton
@Named(GenerateMetadataCapabilityDescriptor.TYPE_ID)
public class GenerateMetadataCapabilityDescriptor
    extends CapabilityDescriptorSupport
    implements Taggable
{

  public static final String TYPE_ID = "yum.generate";

  public static final CapabilityType TYPE = capabilityType(TYPE_ID);

  private static interface Messages
      extends MessageBundle
  {
    @DefaultMessage("Yum: Generate Metadata")
    String name();

    @DefaultMessage("Repository")
    String repositoryLabel();

    @DefaultMessage("Select the hosted repository for which Yum metadata should be generated")
    String repositoryHelp();
  }

  private static final Messages messages = I18N.create(Messages.class);

  private final List<FormField> formFields;

  @Inject
  public GenerateMetadataCapabilityDescriptor() {
    formFields = Lists.<FormField>newArrayList(
        new RepositoryCombobox(
            REPOSITORY_ID, messages.repositoryLabel(), messages.repositoryHelp(), FormField.MANDATORY
        ).includingAnyOfFacets(MavenHostedRepository.class),
        new TextAreaFormField(
            GenerateMetadataCapabilityConfiguration.ALIASES,
            "Aliases",
            "Format: <alias>=<version>[,<alias>=<version>]",
            FormField.OPTIONAL
        ),
        new CheckboxFormField(
            GenerateMetadataCapabilityConfiguration.DELETE_PROCESSING,
            "Process deletes",
            "Check if removing an RPM from this repository should regenerate Yum repository"
                + " (default true)",
            FormField.OPTIONAL
        ).withInitialValue(true),
        new NumberTextFormField(
            GenerateMetadataCapabilityConfiguration.DELETE_PROCESSING_DELAY,
            "Delete process delay",
            "Number of seconds to wait before regenerating Yum repository when an RPM is removed"
                + " (default 10 seconds)",
            FormField.OPTIONAL
        ).withInitialValue(10),
        new StringTextFormField(
            GenerateMetadataCapabilityConfiguration.YUM_GROUPS_DEFINITION_FILE,
            "Yum groups definition file",
            "Repository path of a file containing Yum groups definition (e.g. /comps.xml)",
            FormField.OPTIONAL
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
  public Validator validator() {
    return validators().logical().and(
        validators().repository().repositoryOfType(TYPE, REPOSITORY_ID, MavenHostedRepository.class),
        validators().capability().uniquePer(TYPE, REPOSITORY_ID),
        new AliasMappingsValidator(ALIASES)
    );
  }

  @Override
  public Validator validator(final CapabilityIdentity id) {
    return validators().logical().and(
        validators().repository().repositoryOfType(TYPE, REPOSITORY_ID, MavenHostedRepository.class),
        validators().capability().uniquePerExcluding(id, TYPE, REPOSITORY_ID),
        new AliasMappingsValidator(ALIASES)
    );
  }

  @Override
  protected String renderAbout() throws Exception {
    return render(TYPE_ID + "-about.vm");
  }

  @Override
  public Set<Tag> getTags() {
    return tags(categoryTag("Yum"));
  }

}
