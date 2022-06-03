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
package org.sonatype.nexus.blobstore.file.internal.orient;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.file.FileBlobDeletionIndex;
import org.sonatype.nexus.blobstore.file.FileBlobStore;
import org.sonatype.nexus.common.property.PropertiesFile;

import com.squareup.tape.QueueFile;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonatype.nexus.blobstore.file.FileBlobStore.DELETIONS_FILENAME;
import static org.sonatype.nexus.blobstore.file.FileBlobStore.REBUILD_DELETED_BLOB_INDEX_KEY;

@Named
public class OrientFileBlobDeletionIndex
    extends ComponentSupport
    implements FileBlobDeletionIndex
{
  private QueueFile deletedBlobIndex;

  @Override
  public final void initIndex(final PropertiesFile metadata, final FileBlobStore blobStore)
      throws IOException
  {
    Path storageDir = blobStore.getAbsoluteBlobDir();
    File deletedIndexFile = storageDir.resolve(blobStore.getDeletionsFilename()).toFile();

    Path deletedIndexPath = deletedIndexFile.toPath();
    try {
      Path legacyDeletionsIndex = deletedIndexPath.getParent().resolve(DELETIONS_FILENAME); //NOSONAR

      if (!Files.exists(deletedIndexPath) && Files.exists(legacyDeletionsIndex)) {
        log.info("Found 'deletions.index' file in blob store {}, renaming to {}", storageDir,
            deletedIndexPath);
        Files.move(legacyDeletionsIndex, deletedIndexPath);
      }
      deletedBlobIndex = new QueueFile(deletedIndexFile);
    }
    catch (IOException e) {
      log.error(
          "Unable to load deletions index file {}, run the compact blobstore task to rebuild", deletedIndexFile, e
      );
      createEmptyDeletionsIndex(deletedIndexFile);
      deletedBlobIndex = new QueueFile(deletedIndexFile);
      metadata.setProperty(REBUILD_DELETED_BLOB_INDEX_KEY, "true");
      metadata.store();
    }
  }

  private static void createEmptyDeletionsIndex(final File deletionsIndex) throws IOException {
    // copy a fresh index on top of existing index to avoid problems
    // with removing or renaming open files on Windows
    Path tempFile = Files.createTempFile(DELETIONS_FILENAME, "tmp");
    Files.delete(tempFile);
    try {
      new QueueFile(tempFile.toFile()).close();
      try (RandomAccessFile raf = new RandomAccessFile(deletionsIndex, "rw")) {
        raf.setLength(0);
        raf.write(Files.readAllBytes(tempFile));
      }
    }
    finally {
      Files.deleteIfExists(tempFile);
    }
  }

  @Override
  public final void stopIndex() throws IOException {
    try {
      deletedBlobIndex.close();
    }
    finally {
      deletedBlobIndex = null;
    }
  }

  @Override
  public final void createRecord(final BlobId blobId) throws IOException {
    deletedBlobIndex.add(blobId.toString().getBytes(UTF_8));
  }

  @Override
  public final String readOldestRecord() throws IOException {
    byte[] bytes = deletedBlobIndex.peek();
    if (Objects.isNull(bytes)) {
      return null;
    }
    else {
      return new String(bytes, UTF_8);
    }
  }

  @Override
  public final void deleteRecord(final BlobId blobId) throws IOException {
    deletedBlobIndex.remove();
  }

  @Override
  public final void deleteAllRecords() throws IOException {
    deletedBlobIndex.clear();
  }

  @Override
  public final int size() throws IOException {
    return deletedBlobIndex.size();
  }
}
