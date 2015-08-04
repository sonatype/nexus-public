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

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.validator.FileTypeValidator.FileTypeValidity;
import org.sonatype.sisu.goodies.common.ComponentSupport;

@Named
@Singleton
public class DefaultFileTypeValidatorHub
    extends ComponentSupport
    implements FileTypeValidatorHub
{

  private final Map<String, FileTypeValidator> fileTypeValidators;

  @Inject
  public DefaultFileTypeValidatorHub(final Map<String, FileTypeValidator> fileTypeValidators) {
    this.fileTypeValidators = fileTypeValidators;
  }

  @Override
  public boolean isExpectedFileType(final StorageItem item) {
    if (item instanceof StorageFileItem) {
      StorageFileItem file = (StorageFileItem) item;

      for (Map.Entry<String, FileTypeValidator> fileTypeValidatorEntry : fileTypeValidators.entrySet()) {
        FileTypeValidity validity = fileTypeValidatorEntry.getValue().isExpectedFileType(file);

        if (FileTypeValidity.INVALID.equals(validity)) {
          log.info("File item {} evaluated as INVALID during file type validation (validator={})",
              file.getRepositoryItemUid().toString(), fileTypeValidatorEntry.getKey());
          // fail fast
          return false;
        }
      }

      // return true if not failed for now
      // later we might get this better
      return true;
    }
    else {
      // we check files only, so say true here
      return true;
    }
  }
}
