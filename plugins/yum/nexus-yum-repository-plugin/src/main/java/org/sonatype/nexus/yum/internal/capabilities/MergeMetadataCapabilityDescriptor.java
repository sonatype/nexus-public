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
import org.sonatype.nexus.formfields.RepositoryCombobox;
import org.sonatype.nexus.plugins.capabilities.CapabilityIdentity;
import org.sonatype.nexus.plugins.capabilities.CapabilityType;
import org.sonatype.nexus.plugins.capabilities.Tag;
import org.sonatype.nexus.plugins.capabilities.Taggable;
import org.sonatype.nexus.plugins.capabilities.Validator;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.sisu.goodies.i18n.I18N;
import org.sonatype.sisu.goodies.i18n.MessageBundle;

import com.google.common.collect.Lists;

import static org.sonatype.nexus.plugins.capabilities.CapabilityType.capabilityType;
import static org.sonatype.nexus.plugins.capabilities.Tag.categoryTag;
import static org.sonatype.nexus.plugins.capabilities.Tag.tags;
import static org.sonatype.nexus.yum.internal.capabilities.MergeMetadataCapabilityConfiguration.REPOSITORY_ID;

/**
 * @since yum 3.0
 */
@Singleton
@Named(MergeMetadataCapabilityDescriptor.TYPE_ID)
public class MergeMetadataCapabilityDescriptor
    extends CapabilityDescriptorSupport
    implements Taggable
{

  public static final String TYPE_ID = "yum.merge";

  public static final CapabilityType TYPE = capabilityType(TYPE_ID);

  private static interface Messages
      extends MessageBundle
  {
    @DefaultMessage("Yum: Merge Metadata")
    String name();

    @DefaultMessage("Group")
    String repositoryLabel();

    @DefaultMessage("Select the group that should merge Yum metadata of group members")
    String repositoryHelp();
  }

  private static final Messages messages = I18N.create(Messages.class);

  private final List<FormField> formFields;

  @Inject
  public MergeMetadataCapabilityDescriptor() {
    formFields = Lists.<FormField>newArrayList(
        new RepositoryCombobox(
            REPOSITORY_ID, messages.repositoryLabel(), messages.repositoryHelp(), FormField.MANDATORY
        ).includingAnyOfFacets(GroupRepository.class)
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
        validators().repository().repositoryOfType(TYPE, REPOSITORY_ID, GroupRepository.class),
        validators().capability().uniquePer(TYPE, REPOSITORY_ID)
    );
  }

  @Override
  public Validator validator(final CapabilityIdentity id) {
    return validators().logical().and(
        validators().repository().repositoryOfType(TYPE, REPOSITORY_ID, GroupRepository.class),
        validators().capability().uniquePerExcluding(id, TYPE, REPOSITORY_ID)
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
