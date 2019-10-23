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

import javax.inject.Named;

import com.amazonaws.services.s3.model.AbstractPutObjectRequest;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;

/**
 * Adds no encryption to a S3 request.
 *
 * @since 3.19
 */
@Named(NoEncrypter.ID)
public class NoEncrypter
    implements S3Encrypter
{
  public static final String ID = "none";
  public static final String NAME = "None";

  public static final NoEncrypter INSTANCE = new NoEncrypter();

  @Override
  public <T extends InitiateMultipartUploadRequest> T addEncryption(final T request) {
    return request;
  }

  @Override
  public <T extends AbstractPutObjectRequest> T addEncryption(final T request) {
    return request;
  }

  @Override
  public <T extends CopyObjectRequest> T addEncryption(final T request) {
    return request;
  }
}
