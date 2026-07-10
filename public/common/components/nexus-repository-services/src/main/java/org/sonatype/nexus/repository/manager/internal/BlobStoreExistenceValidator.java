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
package org.sonatype.nexus.repository.manager.internal;

import java.util.Map;

import javax.annotation.Nullable;
import jakarta.validation.ConstraintViolation;

import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.ConfigurationValidator;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.BLOB_STORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.STORAGE;

@Component
public class BlobStoreExistenceValidator
    implements ConfigurationValidator
{
  private final BlobStoreManager blobStoreManager;

  private final ConstraintViolationFactory constraintViolationFactory;

  @Autowired
  public BlobStoreExistenceValidator(
      final BlobStoreManager blobStoreManager,
      final ConstraintViolationFactory constraintViolationFactory)
  {
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.constraintViolationFactory = checkNotNull(constraintViolationFactory);
  }

  @Nullable
  @Override
  public ConstraintViolation<?> validate(final Configuration configuration) {
    Map<String, Map<String, Object>> attributes = configuration.getAttributes();
    if (attributes != null && attributes.containsKey(STORAGE)) {
      Map<String, Object> storageAttributes = attributes.get(STORAGE);
      if (storageAttributes != null && storageAttributes.containsKey(BLOB_STORE_NAME)) {
        String blobStoreName = (String) storageAttributes.get(BLOB_STORE_NAME);
        if (blobStoreName != null && blobStoreManager.get(blobStoreName) == null) {
          return constraintViolationFactory.createViolation(
              STORAGE + "." + BLOB_STORE_NAME,
              "No blob store exists with the specified name");
        }
      }
    }
    return null;
  }
}
