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
package org.sonatype.nexus.repository.apt.internal.hosted;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.view.payloads.StreamPayload.InputStreamSupplier;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.output.CountingOutputStream;
import org.bouncycastle.util.io.TeeOutputStream;

/**
 * Stores a set of temp files, automatically compressing each into a GZIP, BZ2 and plain format.
 *
 * @since 3.17
 */
class CompressingTempFileStore
    extends ComponentSupport
    implements AutoCloseable
{
  private final Map<String, FileHolder> holdersByKey = new HashMap<>();

  public Writer openOutput(final String key) {
    try {
      if (holdersByKey.containsKey(key)) {
        throw new IllegalStateException("Output already opened");
      }
      FileHolder holder = new FileHolder();
      holdersByKey.put(key, holder);
      return new OutputStreamWriter(new TeeOutputStream(
          new TeeOutputStream(new GZIPOutputStream(Files.newOutputStream(holder.gzTempFile)),
              new BZip2CompressorOutputStream(Files.newOutputStream(holder.bzTempFile))),
          Files.newOutputStream(holder.plainTempFile)), Charsets.UTF_8);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public Map<String, FileMetadata> getFiles() {
    return Maps.transformValues(holdersByKey, holder -> new FileMetadata(holder));
  }

  public void close() {
    List<Path> notDeletedFiles = new LinkedList<>();

    for (FileHolder holder : holdersByKey.values()) {
      deleteFile(holder.bzTempFile, notDeletedFiles);
      deleteFile(holder.gzTempFile, notDeletedFiles);
    }

    if (!notDeletedFiles.isEmpty()) {
      log.warn("Files were not successfully deleted: " + notDeletedFiles);
    }
  }

  private void deleteFile(final Path path, final List<Path> paths) {
    try {
      Files.deleteIfExists(path);
    }
    catch (IOException e) { // NOSONAR
      paths.add(path);
    }
  }

  public static class FileMetadata
  {
    private final FileHolder holder;

    private FileMetadata(final FileHolder holder) {
      this.holder = holder;
    }

    public long bzSize() {
      return holder.bzStream.getByteCount();
    }

    public InputStreamSupplier bzSupplier() {
      return () -> Files.newInputStream(holder.bzTempFile);
    }

    public long gzSize() {
      return holder.gzStream.getByteCount();
    }

    public InputStreamSupplier gzSupplier() {
      return () -> Files.newInputStream(holder.gzTempFile);
    }

    public long plainSize() {
      return holder.plainStream.getByteCount();
    }

    public InputStreamSupplier plainSupplier() {
      return () -> Files.newInputStream(holder.plainTempFile);
    }
  }

  private static class FileHolder
  {
    final CountingOutputStream plainStream;

    final Path plainTempFile;

    final CountingOutputStream gzStream;

    final Path gzTempFile;

    final CountingOutputStream bzStream;

    final Path bzTempFile;

    public FileHolder() throws IOException {
      super();
      this.plainTempFile = Files.createTempFile("", "");
      this.plainStream = new CountingOutputStream(Files.newOutputStream(plainTempFile));
      this.gzTempFile = Files.createTempFile("", "");
      this.gzStream = new CountingOutputStream(Files.newOutputStream(gzTempFile));
      this.bzTempFile = Files.createTempFile("", "");
      this.bzStream = new CountingOutputStream(Files.newOutputStream(bzTempFile));
    }
  }
}
