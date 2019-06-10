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
import java.util.Optional;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.validation.ValidationException;

import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.blobstore.BlobStoreDescriptor;
import org.sonatype.nexus.blobstore.BlobStoreDescriptorSupport;
import org.sonatype.nexus.blobstore.BlobStoreUtil;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.group.BlobStoreGroup;
import org.sonatype.nexus.blobstore.group.BlobStoreGroupService;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaService;
import org.sonatype.nexus.formfields.ComboboxFormField;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.ItemselectFormField;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.sonatype.nexus.blobstore.group.BlobStoreGroup.MEMBERS_KEY;
import static org.sonatype.nexus.blobstore.group.BlobStoreGroupConfigurationHelper.memberNames;
import static org.sonatype.nexus.formfields.FormField.MANDATORY;

/**
 * A {@link BlobStoreDescriptor} for {@link BlobStoreGroup}.
 *
 * @since 3.14
 */
@Named(BlobStoreGroup.TYPE)
public class BlobStoreGroupDescriptor
    extends BlobStoreDescriptorSupport
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

  private final BlobStoreManager blobStoreManager;

  private final BlobStoreUtil blobStoreUtil;

  private final Provider<BlobStoreGroupService> blobStoreGroupService;

  private final ItemselectFormField members;

  private final FormField fillPolicy;

  @Inject
  public BlobStoreGroupDescriptor(final BlobStoreManager blobStoreManager,
                                  final BlobStoreUtil blobStoreUtil,
                                  final Provider<BlobStoreGroupService> blobStoreGroupService,
                                  final BlobStoreQuotaService quotaService)
  {
    super(quotaService);
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.blobStoreUtil = checkNotNull(blobStoreUtil);
    this.blobStoreGroupService = checkNotNull(blobStoreGroupService);
    this.members = new ItemselectFormField(
        MEMBERS_KEY,
        messages.membersLabel(),
        null,
        MANDATORY
    );
    this.members.setStoreApi("coreui_Blobstore.readGroupable");
    this.members.setIdMapping("name");
    this.members.setButtons("up", "add", "remove", "down");
    this.members.setFromTitle("Available");
    this.members.setToTitle("Selected");
    this.fillPolicy = new ComboboxFormField<String>(
        BlobStoreGroup.FILL_POLICY_KEY,
        messages.fillPolicyLabel(),
        null,
        FormField.MANDATORY
    ).withStoreApi("coreui_Blobstore.fillPolicies");
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
    return isEnabled();
  }

  @Override
  public void validateConfig(final BlobStoreConfiguration config) {
    super.validateConfig(config);
    validateEnabled();
    String name = config.getName();
    List<String> memberNames = memberNames(config);
    validateNotEmptyOrSelfReferencing(name, memberNames);
    validateEligibleMembers(name, memberNames);
    validateOnlyEmptyOrNotWritableExistingMembersRemoved(name, memberNames);
  }

  private void validateEnabled() {
    if (!isEnabled()) {
      throw new ValidationException("Blob store groups are not enabled");
    }
  }

  private void validateNotEmptyOrSelfReferencing(final String name, final List<String> memberNames) {
    if (memberNames.isEmpty()) {
      throw new ValidationException("Blob Store '" + name + "' cannot be empty");
    }

    if (memberNames.contains(name)) {
      throw new ValidationException("Blob Store '" + name + "' cannot contain itself");
    }
  }

  private void validateEligibleMembers(final String name, final List<String> memberNames) {
    for (String memberName : memberNames) {
      BlobStore member = blobStoreManager.get(memberName);
      if (!member.isGroupable()) {
        BlobStoreConfiguration memberConfig = member.getBlobStoreConfiguration();
        throw new ValidationException(
            format("Blob Store '%s' is of type '%s' and is not eligible to be a group member", memberName,
                memberConfig.getType()));
      }

      // target member may not be a member of a different group
      Predicate<String> sameGroup = name::equals;
      blobStoreManager.getParent(memberName).filter(sameGroup.negate()).ifPresent(groupName -> {
        throw new ValidationException(
            format("Blob Store '%s' is already a member of Blob Store Group '%s'", memberName, groupName));
      });

      // target member may not be set as repository storage
      int repoCount = blobStoreUtil.usageCount(memberName);
      if (repoCount > 0) {
        throw new ValidationException(format(
            "Blob Store '%s' is set as storage for %s repositories and is not eligible to be a group member",
            memberName, repoCount));
      }
    }
  }

  private void validateOnlyEmptyOrNotWritableExistingMembersRemoved(final String name, final List<String> memberNames) {
    BlobStore blobStore = blobStoreManager.get(name);
    if (blobStore != null) {
      BlobStoreConfiguration currentConfiguration = blobStore.getBlobStoreConfiguration();
      if (currentConfiguration != null && currentConfiguration.getType().equals(BlobStoreGroup.TYPE)) {
        for (String existingMemberName : memberNames(currentConfiguration)) {
          if (!memberNames.contains(existingMemberName)) {
            BlobStore existingMember = blobStoreManager.get(existingMemberName);
            if (existingMember.isWritable() || !existingMember.isEmpty()) {
              throw new ValidationException(
                  format("Blob Store '%s' cannot be removed from Blob Store Group '%s', " +
                      "use 'Admin - Remove a member from a blob store group' task instead",
                      existingMemberName, name));
            }
          }
        }
      }
    }
  }

  @Override
  public boolean isEnabled() {
    return Optional.of(blobStoreGroupService)
        .map(Provider::get)
        .map(BlobStoreGroupService::isEnabled)
        .orElse(false);
  }

  @Override
  public boolean configHasDependencyOn(final BlobStoreConfiguration config, final String blobStoreName) {
    return memberNames(config).contains(blobStoreName);
  }
}
