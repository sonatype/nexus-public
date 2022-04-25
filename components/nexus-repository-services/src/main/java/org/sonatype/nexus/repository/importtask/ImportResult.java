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
import java.util.List;

import org.sonatype.nexus.repository.Repository;

/**
 * Includes data that were going through the import process.
 */
public class ImportResult
{
  private final Repository repository;

  private final List<FileAsset> fileAssets;

  public ImportResult(
      final Repository repository,
      final List<FileAsset> fileAssets)
  {
    this.repository = repository;
    this.fileAssets = fileAssets;
  }

  public Repository getRepository() {
    return repository;
  }

  public List<FileAsset> getFileAssets() {
    return fileAssets;
  }

  public static class FileAsset
  {
    private final File file;

    private final String assetName;

    public FileAsset(
        final File file,
        final String assetName)
    {
      this.file = file;
      this.assetName = assetName;
    }

    public File getFile() {
      return file;
    }

    public String getAssetName() {
      return assetName;
    }
  }
}
