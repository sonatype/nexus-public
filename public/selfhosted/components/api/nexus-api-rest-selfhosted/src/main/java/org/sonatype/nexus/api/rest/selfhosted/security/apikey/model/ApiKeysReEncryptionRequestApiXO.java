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
package org.sonatype.nexus.api.rest.selfhosted.security.apikey.model;

import javax.annotation.Nullable;

import io.swagger.v3.oas.annotations.media.Schema;

public class ApiKeysReEncryptionRequestApiXO
{
  @Schema(description = "Optional - The algorithm to be used to decrypt the principals")
  @Nullable
  private String algorithmForDecryption;

  @Schema(description = "Optional - The iterations to be used to decrypt the principals")
  @Nullable
  private Integer iterationsForDecryption;

  @Schema(description = "Optional - Email to notify when task finishes")
  @Nullable
  private String notifyEmail;

  public ApiKeysReEncryptionRequestApiXO() {
    // serialization
  }

  public ApiKeysReEncryptionRequestApiXO(
      @Nullable final String algorithm,
      @Nullable final Integer iterations,
      @Nullable final String notifyEmail)
  {
    this.algorithmForDecryption = algorithm;
    this.iterationsForDecryption = iterations;
    this.notifyEmail = notifyEmail;
  }

  @Nullable
  public String getAlgorithmForDecryption() {
    return algorithmForDecryption;
  }

  @Nullable
  public Integer getIterationsForDecryption() {
    return iterationsForDecryption;
  }

  @Nullable
  public String getNotifyEmail() {
    return notifyEmail;
  }
}
