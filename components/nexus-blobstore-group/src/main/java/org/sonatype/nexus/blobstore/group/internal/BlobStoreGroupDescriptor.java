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
package org.sonatype.nexus.blobstore.group.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.ValidationException;

import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.blobstore.BlobStoreDescriptor;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.group.BlobStoreGroup;
import org.sonatype.nexus.formfields.ComboboxFormField;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.ItemselectFormField;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.size;
import static com.google.common.collect.Streams.stream;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.sonatype.nexus.blobstore.group.BlobStoreGroup.MEMBERS_KEY;
import static org.sonatype.nexus.blobstore.group.internal.BlobStoreGroupConfigurationHelper.memberNames;
import static org.sonatype.nexus.formfields.FormField.MANDATORY;

/**
 * A {@link BlobStoreDescriptor} for {@link BlobStoreGroup}.
 *
 * @since 3.14
 */
@Named(BlobStoreGroup.TYPE)
public class BlobStoreGroupDescriptor
    implements BlobStoreDescriptor
{
  private interface Messages
      extends MessageBundle
  {
    @DefaultMessage("Group")
    String name();

    @DefaultMessage("Members")
    String membersLabel();

    @DefaultMessage("Fill Policy")
    String fillPolicyLabel();
  }

  private static final Messages messages = I18N.create(Messages.class);

  private final boolean isEnabled;

  private final BlobStoreManager blobStoreManager;

  private final RepositoryManager repositoryManager;

  private final ItemselectFormField members;

  private final FormField fillPolicy;

  @Inject
  public BlobStoreGroupDescriptor(@Named("${nexus.blobstoregroups.enabled:-false}") final boolean isEnabled,
                                  final BlobStoreManager blobStoreManager,
                                  final RepositoryManager repositoryManager)
  {
    this.isEnabled = isEnabled;
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.members = new ItemselectFormField(
        MEMBERS_KEY,
        messages.membersLabel(),
        null,
        MANDATORY
    );
    this.members.setStoreApi("coreui_Blobstore.readGroupable");
    this.members.setIdMapping("name");
    this.members.setButtons("add", "remove");
    this.members.setFromTitle("Available");
    this.members.setToTitle("Selected");
    this.fillPolicy = new ComboboxFormField<String>(
        BlobStoreGroup.FILL_POLICY_KEY,
        messages.fillPolicyLabel(),
        null,
        FormField.MANDATORY
    ).withStoreApi("blobstoreGroup_BlobstoreGroup.fillPolicies");
  }

  @Override
  public String getName() {
    return messages.name();
  }

  @Override
  public List<FormField> getFormFields() {
    return asList(members, fillPolicy);
  }

  @Override
  public boolean isModifiable() {
    return true;
  }

  @Override
  public void validateConfig(final BlobStoreConfiguration config) {
    List<String> memberNames = memberNames(config);
    validateNotEmptyOrSelfReferencing(config.getName(), memberNames);
    validateEligibleMembers(memberNames);
  }

  private void validateNotEmptyOrSelfReferencing(final String name, final List<String> memberNames) {
    if (memberNames.isEmpty()) {
      throw new ValidationException("Blob Store '" + name + "' cannot be empty");
    }

    if (memberNames.contains(name)) {
      throw new ValidationException("Blob Store '" + name + "' cannot contain itself");
    }
  }

  private void validateEligibleMembers(final List<String> memberNames) {
    for (String memberName : memberNames) {
      BlobStore member = blobStoreManager.get(memberName);
      if (!member.isGroupable()) {
        BlobStoreConfiguration memberConfig = member.getBlobStoreConfiguration();
        throw new ValidationException(
            format("Blob Store '%s' is of type '%s' and is not eligible to be a group member", memberName,
                memberConfig.getType()));
      }

      BlobStoreGroup group = stream(blobStoreManager.browse())
          .filter(store -> store.getBlobStoreConfiguration().getType().equals(BlobStoreGroup.TYPE))
          .map(BlobStoreGroup.class::cast).filter(g -> g.getMembers().contains(member)).findFirst().orElse(null);
      if (group != null) {
        throw new ValidationException(
            format("Blob Store '%s' is already a member of Blob Store Group '%s'", memberName,
                group.getBlobStoreConfiguration().getName()));
      }

      int repoCount = size(repositoryManager.browseForBlobStore(memberName));
      if (repoCount > 0) {
        throw new ValidationException(format(
            "Blob Store '%s' is set as storage for %s repositories and is not eligible to be a group member",
            memberName, repoCount));
      }
    }
  }

  @Override
  public boolean isEnabled() {
    return isEnabled;
  }

  @Override
  public boolean configHasDependencyOn(final BlobStoreConfiguration config, final String blobStoreName) {
    return memberNames(config).contains(blobStoreName);
  }
}
