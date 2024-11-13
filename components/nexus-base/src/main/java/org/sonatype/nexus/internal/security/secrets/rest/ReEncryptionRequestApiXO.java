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
package org.sonatype.nexus.internal.security.secrets.rest;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;

import com.google.common.annotations.VisibleForTesting;
import io.swagger.annotations.ApiModelProperty;

public class ReEncryptionRequestApiXO
{
  @ApiModelProperty("Key identifier that will be used to re-encrypt secrets")
  @NotBlank
  private String secretKeyId;

  @ApiModelProperty("Optional - Email to notify when task finishes")
  @Nullable
  private String notifyEmail;

  public ReEncryptionRequestApiXO() {
    // serialization
  }

  @VisibleForTesting
  protected ReEncryptionRequestApiXO(final String secretKeyId, final String notifyEmail) {
    this.secretKeyId = secretKeyId;
    this.notifyEmail = notifyEmail;
  }

  public String getSecretKeyId() {
    return secretKeyId;
  }

  @Nullable
  public String getNotifyEmail() {
    return notifyEmail;
  }
}
