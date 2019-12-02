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
package org.sonatype.nexus.blobstore.s3.rest.internal.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Encapsulates the encryption type and key to use for encrypting an s3 blob store at rest i.e AWS S3 server side
 * encryption.
 *
 * @since 3.20
 */
@JsonInclude(NON_NULL)
public class S3BlobStoreApiEncryption
{
  @ApiModelProperty(value = "The type of S3 server side encryption to use.",
      allowableValues = "s3ManagedEncryption,kmsManagedEncryption")
  private final String encryptionType;

  @ApiModelProperty("The encryption key.")
  private final String encryptionKey;

  @JsonCreator
  public S3BlobStoreApiEncryption(
      @JsonProperty("encryptionType") final String encryptionType,
      @JsonProperty("encryptionKey") final String encryptionKey)
  {
    this.encryptionType = encryptionType;
    this.encryptionKey = encryptionKey;
  }

  public String getEncryptionType() {
    return encryptionType;
  }

  public String getEncryptionKey() {
    return encryptionKey;
  }
}
