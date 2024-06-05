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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.MockBlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.s3.rest.internal.model.S3BlobStoreApiModel;
import org.sonatype.nexus.rest.ValidationErrorsException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static java.lang.String.format;
import static org.hamcrest.core.Is.is;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.TYPE;
import static org.sonatype.nexus.blobstore.s3.rest.internal.S3BlobStoreApiConstants.BLOB_STORE_NAME_UPDATE_ERROR_MESSAGE;
import static org.sonatype.nexus.blobstore.s3.rest.internal.S3BlobStoreApiConstants.BLOB_STORE_TYPE_MISMATCH_ERROR_FORMAT;
import static org.sonatype.nexus.blobstore.s3.rest.internal.S3BlobStoreApiConstants.NON_EXISTENT_BLOB_STORE_ERROR_MESSAGE_FORMAT;

public class S3BlobStoreApiUpdateValidationTest
    extends TestSupport
{
  private static final String BLOB_STORE_NAME = "super_blobstore";

  private static final String ANOTHER_BLOB_STORE_NAME = "awesome_blobstore";

  private static final String S3_BLOBSTORE_TYPE = "S3";

  private static final String FILE_BLOB_STORE_TYPE = "File";

  @Rule
  public ExpectedException expectedException = none();

  @Mock
  private BlobStore blobStore;

  @Mock
  private BlobStoreManager blobStoreManager;

  @InjectMocks
  private S3BlobStoreApiUpdateValidation underTest;

  @Test
  public void shouldThrowExceptionWhenBlobStoreDoesNotExist() {
    when(blobStoreManager.exists(BLOB_STORE_NAME)).thenReturn(false);
    mockBlobStoreType(S3_BLOBSTORE_TYPE);

    expectedException.expect(ValidationErrorsException.class);
    expectedException.expectMessage(is(format(NON_EXISTENT_BLOB_STORE_ERROR_MESSAGE_FORMAT, BLOB_STORE_NAME)));

    underTest.validateUpdateRequest(anS3BlobStoreApiModel(BLOB_STORE_NAME), BLOB_STORE_NAME);
  }

  @Test
  public void shouldThrowExceptionWhenBlobStoreNamesDoNotMatch() {
    when(blobStoreManager.exists(BLOB_STORE_NAME)).thenReturn(true);
    mockBlobStoreType(S3_BLOBSTORE_TYPE);

    expectedException.expect(ValidationErrorsException.class);
    expectedException.expectMessage(is(BLOB_STORE_NAME_UPDATE_ERROR_MESSAGE));

    underTest.validateUpdateRequest(anS3BlobStoreApiModel(ANOTHER_BLOB_STORE_NAME), BLOB_STORE_NAME);
  }

  @Test
  public void shouldThrowExceptionWhenExistingBlobStoreTypeIsNotS3() {
    when(blobStoreManager.exists(BLOB_STORE_NAME)).thenReturn(true);
    mockBlobStoreType(FILE_BLOB_STORE_TYPE);

    expectedException.expect(ValidationErrorsException.class);
    expectedException.expectMessage(is(format(BLOB_STORE_TYPE_MISMATCH_ERROR_FORMAT, BLOB_STORE_NAME)));

    underTest.validateUpdateRequest(anS3BlobStoreApiModel(BLOB_STORE_NAME), BLOB_STORE_NAME);
  }

  @Test
  public void shouldThrowExceptionWithAllValidationErrorsWhenMoreThanOneValidationFails() {
    when(blobStoreManager.exists(BLOB_STORE_NAME)).thenReturn(false);
    mockBlobStoreType(FILE_BLOB_STORE_TYPE);

    expectedException.expect(ValidationErrorsException.class);
    expectedException.expectMessage(format(NON_EXISTENT_BLOB_STORE_ERROR_MESSAGE_FORMAT, BLOB_STORE_NAME));
    expectedException.expectMessage(BLOB_STORE_NAME_UPDATE_ERROR_MESSAGE);

    underTest.validateUpdateRequest(anS3BlobStoreApiModel(ANOTHER_BLOB_STORE_NAME), BLOB_STORE_NAME);
  }

  @Test
  public void shouldHaveNoValidationErrors() {
    when(blobStoreManager.exists(BLOB_STORE_NAME)).thenReturn(true);
    mockBlobStoreType(TYPE);

    underTest.validateUpdateRequest(anS3BlobStoreApiModel(BLOB_STORE_NAME), BLOB_STORE_NAME);

    verify(blobStoreManager).exists(BLOB_STORE_NAME);
  }

  @Test
  public void shouldThrowExceptionWhenBlobStoreNameIsEmptyOnCreate() {
    expectedException.expect(ValidationErrorsException.class);
    expectedException.expectMessage("Blob store name cannot be empty");

    underTest.validateCreateRequest(anS3BlobStoreApiModel(""));
  }

  private void mockBlobStoreType(final String type) {
    final BlobStoreConfiguration blobStoreConfiguration = new MockBlobStoreConfiguration();
    blobStoreConfiguration.setType(type);
    when(blobStoreManager.get(BLOB_STORE_NAME)).thenReturn(blobStore);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);
  }

  private S3BlobStoreApiModel anS3BlobStoreApiModel(final String blobStoreName) {
    return new S3BlobStoreApiModel(blobStoreName, null, null);
  }
}
