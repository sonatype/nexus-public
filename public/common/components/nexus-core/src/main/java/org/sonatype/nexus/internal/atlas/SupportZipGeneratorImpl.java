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
package org.sonatype.nexus.internal.atlas;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.sonatype.goodies.common.ByteSize;
import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.log.SupportZipGeneratorRequest;
import org.sonatype.nexus.common.wonderland.DownloadService;
import org.sonatype.nexus.supportzip.SupportBundle;
import org.sonatype.nexus.supportzip.SupportBundle.ContentSource;
import org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type;
import org.sonatype.nexus.supportzip.SupportBundleCustomizer;
import org.sonatype.nexus.supportzip.SupportZipGenerator;

import com.google.common.io.CountingOutputStream;
import org.springframework.beans.factory.annotation.Value;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.*;
import org.springframework.stereotype.Component;

/**
 * Default {@link SupportZipGenerator}.
 *
 * @since 2.7
 */
@Component
@Singleton
public class SupportZipGeneratorImpl
    extends ComponentSupport
    implements SupportZipGenerator
{
  private final List<SupportBundleCustomizer> bundleCustomizers;

  private final DownloadService downloadService;

  /**
   * The maximum (uncompressed) size of any one file that is included into the ZIP file when limit files is enabled.
   */
  private final ByteSize maxFileSize;

  private static final int SKIP_BUFFER_SIZE = 64 * 1024; // 64KB for efficient skip operations

  /**
   * The maximum (compressed) size of the entire ZIP file when limit ZIP size is enabled.
   */
  private final ByteSize maxZipFileSize;

  private static int archivedLogSize;

  @Inject
  SupportZipGeneratorImpl(
      final DownloadService downloadService,
      final List<SupportBundleCustomizer> bundleCustomizers,
      final @Value("${atlas.supportZipGenerator.maxFileSize:30mb}") ByteSize maxFileSize,
      final @Value("${atlas.supportZipGenerator.maxZipFileSize:50mb}") ByteSize maxZipFileSize)
  {
    this.bundleCustomizers = checkNotNull(bundleCustomizers);
    this.downloadService = checkNotNull(downloadService);

    this.maxFileSize = maxFileSize;
    log.info("Maximum included file size: {}", maxFileSize);

    this.maxZipFileSize = maxZipFileSize;
    log.info("Maximum ZIP file size: {}", maxZipFileSize);
  }

  @Override
  public Result generate(final SupportZipGeneratorRequest request) {
    return generate(request, "support-");
  }

  @Override
  public Result generate(final SupportZipGeneratorRequest request, final String prefix) {
    checkNotNull(request);
    setArchivedLogSize(request);

    log.info("Generating support ZIP: {}", request);

    String uniquePrefix = downloadService.uniqueName(prefix);

    try {
      // Write zip to temporary file first;
      Path file = Files.createTempFile(uniquePrefix, "zip");
      log.debug("Writing ZIP file: {}", file);

      boolean truncated;
      try (OutputStream output = Files.newOutputStream(file, StandardOpenOption.WRITE)) {
        truncated = generate(request, uniquePrefix, output);
      }
      long length = Files.size(file);

      // move the file into place;
      String targetFileName = uniquePrefix + ".zip";
      String path = downloadService.move(file.toFile(), targetFileName);
      log.info("Created support ZIP file: {}", path);

      return new Result(truncated, targetFileName, path, length);
    }
    catch (IOException e) {
      log.error("Support zip generation failed", e);
      return null;
    }
  }

  @Override
  public boolean generate(
      final SupportZipGeneratorRequest request,
      final String prefix,
      final OutputStream outputStream)
  {
    List<ContentSource> sources = null;
    SupportBundle bundle = new SupportBundle();
    try {
      // customize the bundle
      bundleCustomizers.forEach(customizer -> {
        log.debug("Customizing bundle with: {}", customizer);
        customizer.customize(bundle);
      });
      checkState(!bundle.getSources().isEmpty(), "At least one bundle source must be configured");

      // filter only sources which user requested;
      sources = filterSources(request, bundle);
      checkState(!sources.isEmpty(), "At least one content source must be configured");

      // prepare bundle sources
      List<ContentSource> preparedSources = sources.stream()
          .map(source -> {
            log.debug("Preparing bundle source: {}", source);
            try {
              source.prepare();
              return source;
            }
            catch (Exception e) {
              log.error("Failed to prepare source {}", source.getClass(), e);
              return null;
            }
          })
          .filter(Objects::nonNull)
          .collect(Collectors.toList());

      return new ZipCreator(outputStream, preparedSources, prefix, request.isLimitFileSizes(), request.isLimitZipSize())
          .createZip();
    }
    catch (Exception e) {
      log.error("Failed to create support ZIP", e);
      return false;
    }
    finally {
      if (sources != null) {
        // cleanup bundle sources
        sources.forEach(source -> {
          log.debug("Cleaning bundle source: {}", source);
          try {
            source.cleanup();
          }
          catch (Exception e) {
            log.warn("Bundle source cleanup failed", e);
          }
        });
      }
    }
  }

  /**
   * Return set of included content source types.
   */
  private static Set<Type> includedTypes(final SupportZipGeneratorRequest request) {
    Set<Type> types = new HashSet<>();
    if (request.isSystemInformation()) {
      types.add(SYSINFO);
      types.add(DBINFO); // including this in sys information unless we decide to make it it's own front end toggle
    }
    if (request.isThreadDump()) {
      types.add(THREAD);
    }
    if (request.isMetrics()) {
      types.add(METRICS);
    }
    if (request.isConfiguration()) {
      types.add(CONFIG);
    }
    if (request.isSecurity()) {
      types.add(SECURITY);
    }
    if (request.isLog()) {
      types.add(LOG);
    }
    if (request.isTaskLog()) {
      types.add(TASKLOG);
    }
    if (request.isAuditLog()) {
      types.add(AUDITLOG);
    }
    if (request.isJmx()) {
      types.add(JMX);
    }
    if (request.isReplication()) {
      types.add(REPLICATIONLOG);
    }
    // included by default, if not selected it will default to 0 days of archived logs which means it includes nothing
    types.add(ARCHIVEDLOG);
    return types;
  }

  /**
   * Filter only included content sources.
   */
  private List<ContentSource> filterSources(
      final SupportZipGeneratorRequest request,
      final SupportBundle supportBundle)
  {
    Set<Type> include = includedTypes(request);

    return supportBundle.getSources()
        .stream()
        .filter(source -> include.contains(source.getType()))
        .peek(source -> log.debug("Including content source: {}", source))
        .collect(Collectors.toList());
  }

  private static int percentCompressed(final long compressed, final long uncompressed) {
    return (int) (100 - ((compressed / uncompressed) * 100));
  }

  // helper to get archived log request size
  public void setArchivedLogSize(SupportZipGeneratorRequest request) {
    archivedLogSize = request.getArchivedLog();
  }

  public static int getArchivedLogSize() {
    return archivedLogSize;
  }

  private class ZipCreator
  {
    private final OutputStream outputStream;

    private final List<ContentSource> sources;

    private final String prefix;

    private final boolean limitFileSizes;

    private final boolean limitZipSize;

    private long totalUncompressed;

    public ZipCreator(
        final OutputStream outputStream,
        final List<ContentSource> sources,
        final String prefix,
        final boolean limitFileSizes,
        final boolean limitZipSize)
    {
      this.outputStream = outputStream;
      this.sources = sources;
      this.prefix = prefix;
      this.limitFileSizes = limitFileSizes;
      this.limitZipSize = limitZipSize;
    }

    /**
     * Creates the zip file.
     * 
     * @return true if the zipfile was truncated
     */
    boolean createZip() {
      CountingOutputStream stream = new CountingOutputStream(outputStream);
      // maximum size of included content
      final int maxContentSize = (int) maxFileSize.toBytes();

      // size of chunks for appending source content and detecting max ZIP size
      final int chunkSize = 4 * 1024;

      // leave some fudge room so we can close the zip file and write marker tokens if needed
      final int maxZipSize = (int) (maxZipFileSize.toBytes() - (chunkSize * 2));

      // token added to files to indicate truncation has occurred
      final String TRUNCATED_TOKEN = "** TRUNCATED **";

      // flag to indicate if any content was truncated
      AtomicBoolean truncated = new AtomicBoolean(false);

      try (FlushableZipOutputStream zip = new FlushableZipOutputStream(stream)) {
        // setup zip too sync-flush so we can detect compressed size for partially written files
        zip.setLevel(Deflater.DEFAULT_COMPRESSION);
        zip.setSyncFlush(true);

        // add directory entries
        addDirectoryEntries(zip);

        // TODO: Sort out how to deal with obfuscation, if its specific or general
        // TODO: ... this should be a detail of the content source

        // add content entries, sorted so highest priority are processed first
        sources.sort(Comparator.naturalOrder());
        sources.forEach(source -> {
          // skipping over archived files that cause the zip to be too large or are past the file size limit
          // TODO: figure out how to handle .gz file truncation gracefully
          if (source.getType() == ARCHIVEDLOG
              && (limitFileSizes && source.getSize() > maxContentSize || limitZipSize && source.getSize() +
                  stream.getCount() > maxZipSize)) {
            log.warn("Skipping {} due to size limit", source.getPath());
            return;
          }

          log.debug("Adding content entry: {} {} bytes", source, source.getSize());

          ZipEntry entry = null;
          try {
            entry = addEntry(zip, source.getPath());

            try (InputStream input = source.getContent()) {
              // determine if the current file is a log file
              boolean isLogFile = source.getType() == LOG || source.getType() == TASKLOG || source.getType() == AUDITLOG
                  || source.getType() == ARCHIVEDLOG;
              // only apply truncation logic to log files
              byte[] buff = new byte[chunkSize];
              int len;
              long writtenBytes = 0;

              // Check if log file needs truncation upfront (either file size or ZIP size limit)
              long sourceSize = source.getSize();
              boolean needsFileSizeTruncation = isLogFile && limitFileSizes && sourceSize > maxContentSize;

              // Check if adding this log file will likely exceed ZIP size limit
              // We use uncompressed size as estimate since we can't predict compression ratio
              long remainingZipSpace = maxZipSize - stream.getCount();
              boolean needsZipSizeTruncation = isLogFile && limitZipSize && sourceSize > remainingZipSpace;

              boolean needsTruncation = needsFileSizeTruncation || needsZipSizeTruncation;

              if (needsTruncation) {
                // For log files, keep the tail (most recent entries) instead of the head
                String reason = needsFileSizeTruncation ? "file size limit" : "ZIP size limit";
                log.warn("Truncating source contents from beginning; {} reached: {}", reason, source.getPath());

                // Write truncation marker at the beginning
                zip.write(TRUNCATED_TOKEN.getBytes());
                zip.write("\n".getBytes());
                writtenBytes += TRUNCATED_TOKEN.length() + 1;
                truncated.set(true);

                // Calculate how many bytes to skip to get to the tail
                // Reserve space for the truncation marker
                long maxBytesToKeep;
                if (needsFileSizeTruncation) {
                  maxBytesToKeep = maxContentSize - TRUNCATED_TOKEN.length() - 1;
                }
                else {
                  // For ZIP size limit, use remaining space (estimate, since compression varies)
                  maxBytesToKeep = remainingZipSpace - TRUNCATED_TOKEN.length() - 1;
                }

                long bytesToKeep;
                long bytesToSkip;
                // Ensure maxBytesToKeep is non-negative to prevent incorrect skip calculations
                if (maxBytesToKeep <= 0) {
                  log.warn("Insufficient space for file content after truncation marker; skipping all content: {}",
                      source.getPath());
                  bytesToKeep = 0;
                  bytesToSkip = sourceSize;
                }
                else {
                  bytesToKeep = Math.min(maxBytesToKeep, sourceSize);
                  bytesToSkip = sourceSize - bytesToKeep;
                }

                // Skip to the tail of the file
                skipToTail(source, bytesToSkip, input);
              }

              while ((len = input.read(buff)) != -1) {
                // Check if ZIP size limit is reached (defensive check for when we didn't truncate upfront)
                if (limitZipSize && stream.getCount() + len > maxZipSize) {
                  log.warn("Truncating source contents; ZIP size limit reached during write: {}", source.getPath());
                  zip.write(TRUNCATED_TOKEN.getBytes());
                  truncated.set(true);
                  break;
                }

                zip.write(buff, 0, len);
                writtenBytes += len;

                // flush so we can detect compressed size for partially written files
                zip.flush();
              }
            }
          }
          catch (Exception e) { // NOSONAR - catching all exceptions so that a bad file of any sort won't cause us to
            // stop
            log.warn("Unable to include {} in bundle, moving onto next file.", source.getPath(), e);
          }
          if (entry != null) {
            closeEntry(zip, entry);
          }
        });

        // add truncated marker if we truncated anything
        if (truncated.get()) {
          addEntry(zip, "truncated");
        }
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }

      if (log.isDebugEnabled()) {
        log.debug("ZIP file (in={} out={}) bytes, compressed: {}%",
            totalUncompressed,
            stream.getCount(),
            percentCompressed(stream.getCount(), totalUncompressed));
      }

      return truncated.get();
    }

    // helper to create normalized entry with prefix
    private ZipEntry addEntry(final ZipOutputStream zip, String path) {
      if (!path.startsWith("/")) {
        path = "/" + path;
      }

      try {
        ZipEntry entry = new ZipEntry(prefix + path);
        zip.putNextEntry(entry);
        return entry;
      }
      catch (IOException e) {
        log.debug("Failed to create path {}", path);
        throw new UncheckedIOException(e);
      }
    }

    // helper to close entry
    private void closeEntry(final ZipOutputStream zip, final ZipEntry entry) {
      try {
        zip.closeEntry();

        // not all entries have a size
        if (entry.getSize() > 0) {
          if (log.isDebugEnabled()) {
            log.debug("Entry (in={} out={}) bytes, compressed: {}%",
                entry.getSize(),
                entry.getCompressedSize(),
                percentCompressed(entry.getCompressedSize(), entry.getSize()));
          }
          totalUncompressed += entry.getSize();
        }
      }
      catch (IOException e) {
        log.debug("Failed to close entry {}", entry);
        throw new UncheckedIOException(e);
      }
    }

    // helper to add entries for each directory
    private void addDirectoryEntries(final ZipOutputStream zip) {
      // include entry for top-level directory
      addEntry(zip, "/");

      // add unique directory entries
      Set<String> dirs = new TreeSet<>();
      sources.forEach(it -> {
        List<String> path = Arrays.asList(it.getPath().split("/"));
        if (path.size() > 1) {
          // eg. "foo/bar/baz" -> [ "foo", "foo/bar" ]
          for (int i = 1; i < path.size(); i++) {
            dirs.add(path.subList(0, i).stream().collect(Collectors.joining("/")));
          }
        }
      });

      dirs.forEach(it -> {
        log.debug("Adding directory entry: {}", it);
        ZipEntry entry = addEntry(zip, it + "/");
        // must end with "/"
        closeEntry(zip, entry);
      });
    }
  }

  private void skipToTail(
      final ContentSource source,
      final long bytesToSkip,
      final InputStream input) throws IOException
  {
    long skipped = 0;
    byte[] skipBuffer = null; // lazily initialized for efficiency
    while (skipped < bytesToSkip) {
      long remaining = bytesToSkip - skipped;
      long skipAmount = input.skip(remaining);
      if (skipAmount <= 0) {
        log.debug("skip() failed for {}, falling back to read/discard method", source.getPath());
        // skip() returned 0 or -1, try reading and discarding instead
        // Use a larger dedicated skip buffer for better I/O efficiency
        if (skipBuffer == null) {
          skipBuffer = new byte[SKIP_BUFFER_SIZE];
        }
        int toRead = (int) Math.min(skipBuffer.length, remaining);
        int read = input.read(skipBuffer, 0, toRead);
        if (read <= 0) {
          break; // Unable to skip further
        }
        skipped += read;
      }
      else {
        skipped += skipAmount;
      }
    }
  }
}
