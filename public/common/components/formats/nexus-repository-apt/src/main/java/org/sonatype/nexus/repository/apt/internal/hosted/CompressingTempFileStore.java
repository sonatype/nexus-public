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

import org.sonatype.nexus.common.io.InputStreamSupplier;

import com.google.common.base.Charsets;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.output.CountingOutputStream;
import org.bouncycastle.util.io.TeeOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores a set of temp files, automatically compressing each into a GZIP, BZ2 and plain format.
 *
 * @since 3.17
 */
public class CompressingTempFileStore
    implements AutoCloseable
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final Map<DistComponentArchKey, FileHolder> holdersByKey = new HashMap<>();

  public Writer openOutput(final DistComponentArchKey key) {
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

  public boolean isEmpty() {
    return holdersByKey.isEmpty();
  }

  public Map<String, Map<String, Map<String, FileMetadata>>> getFiles() {
    
    Map<String, Map<String, Map<String, FileMetadata>>> filesByDistComponentAndArch = new HashMap<>();

    for (Map.Entry<DistComponentArchKey, FileHolder> entry : holdersByKey.entrySet()) {
      DistComponentArchKey key = entry.getKey();
      FileHolder holder = entry.getValue();

      filesByDistComponentAndArch
          .computeIfAbsent(key.getDistribution(), k -> new HashMap<>())
          .computeIfAbsent(key.getComponent(), k -> new HashMap<>())
          .put(key.getArchitecture(), new FileMetadata(holder));
    }

    return filesByDistComponentAndArch;
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

  public static class DistComponentArchKey {
    private final String distribution;

    private final String component;

    private final String architecture;

    public DistComponentArchKey(final String distribution, final String component, final String architecture) {
      this.distribution = distribution;
      this.component = component;
      this.architecture = architecture;
    }

    public String getDistribution() {
      return distribution;
    }

    public String getComponent() {
      return component;
    }

    public String getArchitecture() {
      return architecture;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof DistComponentArchKey)) {
        return false;
      }
      DistComponentArchKey that = (DistComponentArchKey) o;
      return distribution.equals(that.distribution)
          && component.equals(that.component)
          && architecture.equals(that.architecture);
    }

    @Override
    public int hashCode() {
      int result = distribution.hashCode();
      result = 31 * result + component.hashCode();
      result = 31 * result + architecture.hashCode();
      return result;
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
