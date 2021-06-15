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
package org.sonatype.nexus.repository.importtask;

import java.io.File;
import java.util.Objects;

import org.sonatype.nexus.repository.Repository;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Hold the information needed for importing an individual file
 *
 * @since 3.31
 */
public class ImportFileConfiguration
{
  private final Repository repository;

  private final File file;

  private final String assetName;

  private final boolean hardLinkingEnabled;

  /**
   * @param repository the {@link Repository} to add the file to
   * @param file       the {@link File} to add to the repository
   * @param assetName  the path of the content relative to the base import directory
   */
  public ImportFileConfiguration(final Repository repository, final File file, final String assetName) {
    this(repository, file, assetName, false);
  }

  /**
   * @param repository      the {@link Repository} to add the file to
   * @param file            the {@link File} to add to the repository
   * @param assetName       the path of the content relative to the base import directory
   * @param hardLinkingEnabled true to hard link the file instead of copying it
   */
  public ImportFileConfiguration(
      final Repository repository,
      final File file,
      final String assetName,
      final boolean hardLinkingEnabled)
  {
    this.repository = checkNotNull(repository);
    this.file = checkNotNull(file);
    this.assetName = checkNotNull(assetName);
    this.hardLinkingEnabled = hardLinkingEnabled;
  }

  public Repository getRepository() {
    return repository;
  }

  public File getFile() {
    return file;
  }

  public String getAssetName() {
    return assetName;
  }

  public boolean isHardLinkingEnabled() {
    return hardLinkingEnabled;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ImportFileConfiguration that = (ImportFileConfiguration) o;
    return Objects.equals(repository, that.repository) &&
        Objects.equals(file, that.file) &&
        Objects.equals(assetName, that.assetName) &&
        hardLinkingEnabled == that.hardLinkingEnabled;
  }

  @Override
  public int hashCode() {
    return Objects.hash(repository, file, assetName, hardLinkingEnabled);
  }
}
