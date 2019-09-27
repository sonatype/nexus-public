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
package org.sonatype.nexus.blobstore.s3.internal.encryption;

import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.inject.Named;

import com.amazonaws.services.s3.model.AbstractPutObjectRequest;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;

import static java.util.Optional.ofNullable;

/**
 * Adds S3 managed encryption to S3 requests.
 * @since 3.19
 */
@Named(S3ManagedEncrypter.ID)
public class S3ManagedEncrypter
    implements S3Encrypter
{
  public static final String ID = "s3ManagedEncryption";
  public static final String NAME = "S3 Managed Encryption";

  @Override
  public <T extends InitiateMultipartUploadRequest> T addEncryption(final T request) {
    setEncryption(request::getObjectMetadata, request::setObjectMetadata);
    return request;
  }

  @Override
  public <T extends AbstractPutObjectRequest> T addEncryption(final T request) {
    setEncryption(request::getMetadata, request::setMetadata);
    return request;
  }

  @Override
  public <T extends CopyObjectRequest> T addEncryption(final T request) {
    setEncryption(request::getNewObjectMetadata, request::setNewObjectMetadata);
    return request;
  }

  private void setEncryption(final Supplier<ObjectMetadata> getter, final Consumer<ObjectMetadata> setter) {
    ObjectMetadata objectMetadata = ofNullable(getter.get()).orElse(new ObjectMetadata());
    objectMetadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
    setter.accept(objectMetadata);
  }
}
