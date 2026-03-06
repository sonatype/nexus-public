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

import java.io.InputStream;
import java.util.Objects;
import javax.annotation.Nullable;

import org.sonatype.nexus.repository.Repository;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Hold the information needed for importing from an InputStream
 *
 * @since 3.31
 */
public class ImportStreamConfiguration
{
  private final Repository repository;

  private final InputStream inputStream;

  private final String assetName;

  private final String contentType;

  /**
   * @param repository the {@link Repository} to add the stream content to
   * @param inputStream the {@link InputStream} containing the content to add to the repository
   * @param assetName the path of the content relative to the base import directory
   */
  public ImportStreamConfiguration(final Repository repository, final InputStream inputStream, final String assetName) {
    this(repository, inputStream, assetName, null);
  }

  /**
   * @param repository the {@link Repository} to add the stream content to
   * @param inputStream the {@link InputStream} containing the content to add to the repository
   * @param assetName the path of the content relative to the base import directory
   * @param contentType the content type of the asset (may be null for auto-detection)
   */
  public ImportStreamConfiguration(
      final Repository repository,
      final InputStream inputStream,
      final String assetName,
      @Nullable final String contentType)
  {
    this.repository = checkNotNull(repository);
    this.inputStream = checkNotNull(inputStream);
    this.assetName = checkNotNull(assetName);
    this.contentType = contentType;
  }

  public Repository getRepository() {
    return repository;
  }

  public InputStream getInputStream() {
    return inputStream;
  }

  public String getAssetName() {
    return assetName;
  }

  @Nullable
  public String getContentType() {
    return contentType;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ImportStreamConfiguration that = (ImportStreamConfiguration) o;
    return Objects.equals(repository, that.repository) &&
        Objects.equals(inputStream, that.inputStream) &&
        Objects.equals(assetName, that.assetName) &&
        Objects.equals(contentType, that.contentType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(repository, inputStream, assetName, contentType);
  }
}
