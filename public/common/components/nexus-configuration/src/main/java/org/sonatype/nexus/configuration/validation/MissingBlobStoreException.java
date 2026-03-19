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
package org.sonatype.nexus.configuration.validation;

/**
 * Thrown when a repository configuration references a blob store that does not exist during import.
 * This exception is propagated to stop the entire configuration import process.
 */
public class MissingBlobStoreException
    extends RuntimeException
{
  private final String repositoryName;

  private final String blobStoreName;

  public MissingBlobStoreException(final String repositoryName, final String blobStoreName) {
    super(String.format(
        "Cannot import repository '%s': Blob store '%s' does not exist. " +
            "Blob stores must be created manually before importing repositories.",
        repositoryName, blobStoreName));
    this.repositoryName = repositoryName;
    this.blobStoreName = blobStoreName;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public String getBlobStoreName() {
    return blobStoreName;
  }
}
