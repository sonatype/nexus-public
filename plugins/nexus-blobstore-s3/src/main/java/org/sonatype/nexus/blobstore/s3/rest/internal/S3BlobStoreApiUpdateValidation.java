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
package org.sonatype.nexus.blobstore.s3.rest.internal;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.s3.rest.internal.model.S3BlobStoreApiModel;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.apache.commons.lang.StringUtils;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.TYPE;
import static org.sonatype.nexus.blobstore.s3.rest.internal.S3BlobStoreApiConstants.BLOB_STORE_NAME_UPDATE_ERROR_MESSAGE;
import static org.sonatype.nexus.blobstore.s3.rest.internal.S3BlobStoreApiConstants.BLOB_STORE_TYPE_MISMATCH_ERROR_FORMAT;
import static org.sonatype.nexus.blobstore.s3.rest.internal.S3BlobStoreApiConstants.NON_EXISTENT_BLOB_STORE_ERROR_MESSAGE_FORMAT;

/**
 * Performs validation checks on specified {@link S3BlobStoreApiModel} object containing updates to an S3 blob store.
 *
 * @since 3.20
 */
@Named
@Singleton
public class S3BlobStoreApiUpdateValidation
{
  private static final String BLOB_STORE_NAME = "name";

  private static final String COMMA = ",";

  private final BlobStoreManager blobStoreManager;

  @Inject
  public S3BlobStoreApiUpdateValidation(final BlobStoreManager blobStoreManager) {
    this.blobStoreManager = blobStoreManager;
  }

  void validateCreateRequest(final S3BlobStoreApiModel s3BlobStoreApiModel) {
    List<String> errors = new ArrayList<>();
    checkBlobStoreNameNotEmpty(s3BlobStoreApiModel.getName(), errors);

    if (!errors.isEmpty()) {
      throw new ValidationErrorsException(BLOB_STORE_NAME, String.join(COMMA, errors));
    }
  }

  void validateUpdateRequest(final S3BlobStoreApiModel s3BlobStoreApiModel, final String blobStoreName) {
    List<String> errors = new ArrayList<>();
    final boolean blobStoreExists = checkBlobStoreExists(blobStoreName, errors);
    checkBlobStoreNamesMatch(s3BlobStoreApiModel, blobStoreName, errors);
    if (blobStoreExists) {
      checkBlobStoreTypeIsS3(blobStoreName, errors);
    }

    if (!errors.isEmpty()) {
      throw new ValidationErrorsException(BLOB_STORE_NAME, join(COMMA, errors));
    }
  }

  private void checkBlobStoreNameNotEmpty(final String blobStoreName, final List<String> errors) {
    if (StringUtils.isBlank(blobStoreName)) {
      errors.add("Blob store name cannot be empty");
    }
  }

  private boolean checkBlobStoreExists(final String blobStoreName, final List<String> errors) {
    if (!blobStoreManager.exists(blobStoreName)) {
      errors.add(format(NON_EXISTENT_BLOB_STORE_ERROR_MESSAGE_FORMAT, blobStoreName));
      return false;
    }
    return true;
  }

  private void checkBlobStoreNamesMatch(
      final S3BlobStoreApiModel s3BlobStoreApiModel,
      final String blobStoreName, final List<String> errors)
  {
    if (!equalsIgnoreCase(s3BlobStoreApiModel.getName(), blobStoreName)) {
      errors.add(BLOB_STORE_NAME_UPDATE_ERROR_MESSAGE);
    }
  }

  private void checkBlobStoreTypeIsS3(final String blobStoreName, final List<String> errors) {
    if (existingBlobStoreIsNotS3(blobStoreName)) {
      errors.add(format(BLOB_STORE_TYPE_MISMATCH_ERROR_FORMAT, blobStoreName));
    }
  }

  private boolean existingBlobStoreIsNotS3(final String blobStoreName) {
    return !ofNullable(blobStoreManager.get(blobStoreName))
        .map(BlobStore::getBlobStoreConfiguration)
        .map(BlobStoreConfiguration::getType)
        .filter(type -> equalsIgnoreCase(TYPE, type))
        .isPresent();
  }
}
