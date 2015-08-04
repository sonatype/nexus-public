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
package org.sonatype.nexus.proxy.repository.validator;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.events.RepositoryItemValidationEvent;
import org.sonatype.nexus.proxy.events.RepositoryItemValidationEventFailedFileType;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.repository.ItemContentValidator;
import org.sonatype.nexus.proxy.repository.ProxyRepository;

@Named(FileTypeItemContentValidator.ID)
@Singleton
public class FileTypeItemContentValidator
    implements ItemContentValidator
{

  public static final String ID = "FileTypeItemContentValidator";

  private final FileTypeValidatorHub validatorHub;

  @Inject
  public FileTypeItemContentValidator(final FileTypeValidatorHub validatorHub) {
    this.validatorHub = validatorHub;
  }

  public boolean isRemoteItemContentValid(final ProxyRepository proxy, final ResourceStoreRequest request,
                                          final String baseUrl, final AbstractStorageItem item,
                                          final List<RepositoryItemValidationEvent> events)
  {
    if (!proxy.isFileTypeValidation()) {
      // make sure this is enabled before we check.
      return true;
    }

    final boolean result = validatorHub.isExpectedFileType(item);

    if (!result) {
      events.add(new RepositoryItemValidationEventFailedFileType(proxy, item, String.format("Invalid file type.",
          item.getRepositoryItemUid().toString())));
    }

    return result;
  }
}
