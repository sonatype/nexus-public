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
package org.sonatype.nexus.repository.tools;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.3
 */
public class DeadBlobResult<T>
{
  private final String repositoryName;

  private final T asset;

  private final String errorMessage;

  private final ResultState resultState;

  public DeadBlobResult(
      final String repositoryName,
      @Nullable final T asset,
      final ResultState resultState,
      final String errorMessage)
  {
    this.repositoryName = checkNotNull(repositoryName);
    this.asset = asset;
    this.resultState = checkNotNull(resultState);
    this.errorMessage = checkNotNull(errorMessage);
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public T getAsset() {
    return asset;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public ResultState getResultState() {
    return resultState;
  }

  @Override
  public String toString() {
    return "DeadBlobResult [repositoryName=" + repositoryName + ", asset=" + asset + ", errorMessage=" + errorMessage
        + ", resultState=" + resultState + "]";
  }
}
