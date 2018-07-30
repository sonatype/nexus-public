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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.ValidationException;

import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.blobstore.BlobStoreDescriptor;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.ItemselectFormField;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;
import static org.sonatype.nexus.blobstore.group.internal.BlobStoreGroup.MEMBERS_KEY;
import static org.sonatype.nexus.blobstore.group.internal.BlobStoreGroupConfigurationHelper.memberNames;
import static org.sonatype.nexus.formfields.FormField.MANDATORY;

/**
 * A {@link BlobStoreDescriptor} for {@link BlobStoreGroup}.
 *
 * @since 3.next
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
  }

  private static final Messages messages = I18N.create(Messages.class);

  private final boolean isEnabled;

  private final BlobStoreManager blobStoreManager;

  private final ItemselectFormField members;

  @Inject
  public BlobStoreGroupDescriptor(@Named("${nexus.blobstoregroups.enabled:-false}") final boolean isEnabled,
                                  final BlobStoreManager blobStoreManager) {
    this.isEnabled = isEnabled;
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.members = new ItemselectFormField(
        MEMBERS_KEY,
        messages.membersLabel(),
        null,
        MANDATORY
    );
    this.members.setStoreApi("coreui_Blobstore.read");
    this.members.setIdMapping("name");
    this.members.setButtons("add", "remove");
    this.members.setFromTitle("Available");
    this.members.setToTitle("Selected");
  }

  @Override
  public String getName() {
    return messages.name();
  }

  @Override
  public List<FormField> getFormFields() {
    return asList(members);
  }

  @Override
  public boolean isModifiable() {
    return true;
  }

  @Override
  public void validateConfig(final BlobStoreConfiguration config) {
    Set<String> processedBlobStores = new HashSet<>();
    List<String> memberNames = memberNames(config);
    if (memberNames.isEmpty()) {
      throw new ValidationException("Blob Store '" + config.getName() + "' cannot be empty");
    }
    for (String memberName : memberNames) {
      if (containsBlobStore(config.getName(), memberName, processedBlobStores)) {
        throw new ValidationException("Blob Store '" + config.getName() + "' cannot contain itself");
      }
    }
  }

  @Override
  public boolean isEnabled() {
    return isEnabled;
  }

  private boolean containsBlobStore(final String blobStoreName, final String memberName, final Set<String> processedBlobStores) {
    if (processedBlobStores.contains(memberName)) {
      return false;
    }
    processedBlobStores.add(memberName);

    if (blobStoreName.equals(memberName)) {
      return true;
    }

    BlobStore member = blobStoreManager.get(memberName);
    BlobStoreConfiguration memberConfig = member.getBlobStoreConfiguration();
    if (memberConfig.getType().equals(BlobStoreGroup.TYPE)) {
      for (String name : memberNames(memberConfig)) {
        if (containsBlobStore(blobStoreName, name, processedBlobStores)) {
          return true;
        }
      }
    }
    return false;
  }
}
