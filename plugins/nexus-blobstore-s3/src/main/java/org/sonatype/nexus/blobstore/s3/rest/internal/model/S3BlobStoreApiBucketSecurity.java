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
 * Encapsulates the IAM settings to use for accessing an s3 blob store.
 *
 * @since 3.20
 */
@JsonInclude(NON_NULL)
public class S3BlobStoreApiBucketSecurity
{
  @ApiModelProperty("An IAM access key ID for granting access to the S3 bucket")
  private final String accessKeyId;

  @ApiModelProperty("The secret access key associated with the specified IAM access key ID")
  private String secretAccessKey;

  @ApiModelProperty("An IAM role to assume in order to access the S3 bucket")
  private final String role;

  @ApiModelProperty("An AWS STS session token associated with temporary security credentials which grant access to the S3 bucket")
  private String sessionToken;

  @JsonCreator
  public S3BlobStoreApiBucketSecurity(
      @JsonProperty("accessKeyId") final String accessKeyId,
      @JsonProperty("secretAccessKey") final String secretAccessKey,
      @JsonProperty("role") final String role,
      @JsonProperty("sessionToken") final String sessionToken)
  {
    this.accessKeyId = accessKeyId;
    this.secretAccessKey = secretAccessKey;
    this.role = role;
    this.sessionToken = sessionToken;
  }

  public String getAccessKeyId() {
    return accessKeyId;
  }

  public String getSecretAccessKey() {
    return secretAccessKey;
  }

  public String getRole() {
    return role;
  }

  public String getSessionToken() {
    return sessionToken;
  }

  public void setSecretAccessKey(final String secretAccessKey) {
    this.secretAccessKey = secretAccessKey;
  }

  public void setSessionToken(final String sessionToken) {
    this.sessionToken = sessionToken;
  }
}
