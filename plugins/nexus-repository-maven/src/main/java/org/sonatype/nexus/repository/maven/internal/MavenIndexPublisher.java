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
package org.sonatype.nexus.repository.maven.internal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import javax.annotation.Nonnull;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.orient.entity.AttachedEntityHelper;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.SignatureType;
import org.sonatype.nexus.repository.maven.internal.Attributes.AssetKind;
import org.sonatype.nexus.repository.maven.internal.filter.DuplicateDetectionStrategy;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload.InputStreamSupplier;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.base.Predicate;
import com.google.common.io.Closer;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.apache.maven.index.reader.ChunkReader;
import org.apache.maven.index.reader.IndexReader;
import org.apache.maven.index.reader.IndexWriter;
import org.apache.maven.index.reader.Record;
import org.apache.maven.index.reader.Record.EntryKey;
import org.apache.maven.index.reader.Record.Type;
import org.apache.maven.index.reader.RecordCompactor;
import org.apache.maven.index.reader.RecordExpander;
import org.apache.maven.index.reader.ResourceHandler;
import org.apache.maven.index.reader.WritableResourceHandler;
import org.apache.maven.index.reader.WritableResourceHandler.WritableResource;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static java.util.Collections.singletonList;
import static org.apache.maven.index.reader.Utils.allGroups;
import static org.apache.maven.index.reader.Utils.descriptor;
import static org.apache.maven.index.reader.Utils.rootGroup;
import static org.apache.maven.index.reader.Utils.rootGroups;
import static org.sonatype.nexus.repository.http.HttpMethods.GET;
import static org.sonatype.nexus.repository.maven.internal.Constants.INDEX_MAIN_CHUNK_FILE_PATH;
import static org.sonatype.nexus.repository.maven.internal.Constants.INDEX_PROPERTY_FILE_PATH;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_BUCKET;

/**
 * Helpers for MI index publishing.
 *
 * @since 3.0
 */
public final class MavenIndexPublisher
{
  private static final Logger log = LoggerFactory.getLogger(MavenIndexPublisher.class);

  private static final String INDEX_PROPERTY_FILE = "/" + INDEX_PROPERTY_FILE_PATH;

  private static final String INDEX_MAIN_CHUNK_FILE = "/" + INDEX_MAIN_CHUNK_FILE_PATH;

  private static final String SELECT_HOSTED_ARTIFACTS =
      "SELECT " +
          "last_updated AS lastModified, " +
          "component.group AS groupId, " +
          "component.name AS artifactId, " +
          "component.attributes.maven2.baseVersion AS version, " +
          "component.attributes.maven2.packaging AS packaging, " +
          "component.attributes.maven2.pom_name AS pom_name, " +
          "component.attributes.maven2.pom_description AS pom_description, " +
          "attributes.maven2.classifier AS classifier, " +
          "name AS path, " +
          "attributes.content.last_modified AS contentLastModified, " +
          "size AS contentSize, " +
          "attributes.checksum.sha1 AS sha1 " +
          "FROM asset " +
          "WHERE bucket=:bucket " +
          "AND attributes.maven2.asset_kind=:asset_kind " +
          "AND component IS NOT NULL";

  private static final RecordExpander RECORD_EXPANDER = new RecordExpander();

  private static final RecordCompactor RECORD_COMPACTOR = new RecordCompactor();

  private MavenIndexPublisher() {
    // nop
  }

  /**
   * Returns the {@link DateTime} when index of the given repository was last published.
   */
  public static DateTime lastPublished(final Repository repository) throws IOException {
    checkNotNull(repository);
    try (ResourceHandler resourceHandler = new Maven2WritableResourceHandler(repository)) {
      try (IndexReader indexReader = new IndexReader(null, resourceHandler)) {
        return new DateTime(indexReader.getPublishedTimestamp().getTime());
      }
    }
    catch (IllegalArgumentException e) {
      // thrown by IndexReader when no index found
      log.debug("No index found in {}", repository, e);
      return null;
    }
  }

  /**
   * Prefetch proxy repository index files, if possible. Returns {@code true} if successful. Accepts only maven proxy
   * types. Returns {@code true} if successfully prefetched files (they exist on remote and are locally cached).
   */
  public static boolean prefetchIndexFiles(final Repository repository) throws IOException {
    checkNotNull(repository);
    checkArgument(ProxyType.NAME.equals(repository.getType().getValue()));
    return prefetch(repository, INDEX_PROPERTY_FILE) && prefetch(repository, INDEX_MAIN_CHUNK_FILE);
  }

  /**
   * Deletes index files from given repository, returns {@code true} if there was index in repository.
   */
  public static boolean unpublishIndexFiles(final Repository repository) throws IOException {
    checkNotNull(repository);
    return delete(repository, INDEX_PROPERTY_FILE) && delete(repository, INDEX_MAIN_CHUNK_FILE);
  }

  /**
   * Publishes MI index into {@code target}, sourced from {@code repositories} repositories.
   */
  public static void publishMergedIndex(final Repository target,
                                        final List<Repository> repositories,
                                        final DuplicateDetectionStrategy<Record> duplicateDetectionStrategy)
      throws IOException
  {
    checkNotNull(target);
    checkNotNull(repositories);
    Closer closer = Closer.create();
    try (Maven2WritableResourceHandler resourceHandler = new Maven2WritableResourceHandler(target);
         IndexWriter indexWriter = new IndexWriter(resourceHandler, target.getName(), false)) {
      indexWriter.writeChunk(
          transform(
              decorate(
                  filter(concat(getGroupRecords(repositories, closer)), duplicateDetectionStrategy),
                  target.getName()
              ),
              RECORD_COMPACTOR::apply
          ).iterator()
      );
    }
    catch (Throwable t) {
      throw closer.rethrow(t);
    }
    finally {
      closer.close();
    }
  }

  /**
   * Publishes MI index into {@code target}, sourced from repository's own CMA structures.
   */
  public static void publishHostedIndex(final Repository repository,
                                        final DuplicateDetectionStrategy<Record> duplicateDetectionStrategy)
      throws IOException
  {
    checkNotNull(repository);
    Transactional.operation.throwing(IOException.class).call(
        () -> {
          final StorageTx tx = UnitOfWork.currentTx();
          try (Maven2WritableResourceHandler resourceHandler = new Maven2WritableResourceHandler(repository)) {
            try (IndexWriter indexWriter = new IndexWriter(resourceHandler, repository.getName(), false)) {
              indexWriter.writeChunk(
                  transform(
                      decorate(
                          filter(getHostedRecords(tx, repository), duplicateDetectionStrategy),
                          repository.getName()
                      ),
                      RECORD_COMPACTOR::apply
                  ).iterator()
              );
            }
          }
          return null;
        }
    );
  }

  /**
   * Primes proxy cache with given path and return {@code true} if succeeds. Accepts only maven proxy type.
   */
  private static boolean prefetch(final Repository repository, final String path) throws IOException {
    MavenPath mavenPath = repository.facet(MavenFacet.class).getMavenPathParser().parsePath(path);
    Request getRequest = new Request.Builder()
        .action(GET)
        .path(path)
        .build();
    Context context = new Context(repository, getRequest);
    context.getAttributes().set(MavenPath.class, mavenPath);
    return repository.facet(ProxyFacet.class).get(context) != null;
  }

  /**
   * Deletes given path from repository's storage/cache.
   */
  private static boolean delete(final Repository repository, final String path) throws IOException {
    MavenFacet mavenFacet = repository.facet(MavenFacet.class);
    MavenPath mavenPath = mavenFacet.getMavenPathParser().parsePath(path);
    return mavenFacet.delete(mavenPath);
  }

  /**
   * Returns the records to publish of a hosted repository, the SELECT result count will be in parity with published
   * records count!
   */
  private static Iterable<Record> getHostedRecords(final StorageTx tx, final Repository repository) throws IOException {
    Map<String, Object> sqlParams = new HashMap<>();
    sqlParams.put(P_BUCKET, AttachedEntityHelper.id(tx.findBucket(repository)));
    sqlParams.put(P_ASSET_KIND, AssetKind.ARTIFACT.name());
    return transform(
        tx.browse(SELECT_HOSTED_ARTIFACTS, sqlParams),
        (ODocument document) -> toRecord(repository.facet(MavenFacet.class), document)
    );
  }

  private static Iterable<Iterable<Record>> getGroupRecords(final List<Repository> repositories, final Closer closer)
      throws IOException
  {
    UnitOfWork paused = UnitOfWork.pause();
    try {
      List<Iterable<Record>> records = new ArrayList<>();
      for (Repository repository : repositories) {
        UnitOfWork.begin(repository.facet(StorageFacet.class).txSupplier());
        try {
          ResourceHandler resourceHandler = closer.register(new Maven2WritableResourceHandler(repository));
          IndexReader indexReader = closer.register(new IndexReader(null, resourceHandler));
          ChunkReader chunkReader = closer.register(indexReader.iterator().next());
          records.add(filter(transform(chunkReader, RECORD_EXPANDER::apply), new RecordTypeFilter(Type.ARTIFACT_ADD)));
        }
        catch (IllegalArgumentException e) {
          throw new IOException(e.getMessage(), e);
        }
        finally {
          UnitOfWork.end();
        }
      }
      return records;
    }
    finally {
      UnitOfWork.resume(paused);
    }
  }

  /**
   * Converts orient SQL query result into Maven Indexer Reader {@link Record}. Should be invoked only with documents
   * belonging to components, but not checksums or signatures.
   */
  private static Record toRecord(final MavenFacet mavenFacet, final ODocument document) {
    checkNotNull(document); // sanity
    final String path = document.field("path", String.class);
    MavenPath mavenPath = mavenFacet.getMavenPathParser().parsePath(path);
    checkArgument(mavenPath.getCoordinates() != null && !mavenPath.isSubordinate()); // otherwise query is wrong

    Record record = new Record(Type.ARTIFACT_ADD, new HashMap<>());
    record.put(Record.REC_MODIFIED, document.field("lastModified", Long.class));
    record.put(Record.GROUP_ID, document.field("groupId", String.class));
    record.put(Record.ARTIFACT_ID, document.field("artifactId", String.class));
    record.put(Record.VERSION, document.field("version", String.class));
    record.put(Record.CLASSIFIER, document.field("classifier", String.class));

    String packaging = document.field("packaging", String.class);
    if (packaging != null) {
      record.put(Record.PACKAGING, packaging);
    }
    else {
      record.put(Record.PACKAGING, pathExtension(mavenPath.getFileName()));
    }
    record.put(Record.NAME, defStr(document.field("pom_name", String.class), ""));
    record.put(Record.DESCRIPTION, defStr(document.field("pom_description", String.class), ""));

    checkExistence(record, Record.HAS_SOURCES, mavenPath.locate("jar", "sources"), mavenFacet);
    checkExistence(record, Record.HAS_JAVADOC, mavenPath.locate("jar", "javadoc"), mavenFacet);
    checkExistence(record, Record.HAS_SIGNATURE, mavenPath.signature(SignatureType.GPG), mavenFacet);

    record.put(Record.FILE_EXTENSION, pathExtension(mavenPath.getFileName()));
    record.put(Record.FILE_MODIFIED, document.field("contentLastModified", Long.class));
    record.put(Record.FILE_SIZE, document.field("contentSize", Long.class));
    record.put(Record.SHA1, document.field("sha1", String.class));
    return record;
  }

  private static void checkExistence(final Record record,
                                     final EntryKey<Boolean> key,
                                     final MavenPath tocheck,
                                     final MavenFacet mavenFacet)
  {
    record.put(key, mavenFacet.exists(tocheck));
  }

  /**
   * This method is copied from MI and Plexus related methods, to produce exactly same (possibly buggy) extensions out
   * of a file path, as MI client will attempt to "fix" those.
   */
  private static String pathExtension(final String path) {
    String filename = path.toLowerCase(Locale.ENGLISH);
    if (filename.endsWith("tar.gz")) {
      return "tar.gz";
    }
    else if (filename.endsWith("tar.bz2")) {
      return "tar.bz2";
    }
    int lastSep = filename.lastIndexOf('/');
    int lastDot;
    if (lastSep < 0) {
      lastDot = filename.lastIndexOf('.');
    }
    else {
      lastDot = filename.substring(lastSep + 1).lastIndexOf('.');
      if (lastDot >= 0) {
        lastDot += lastSep + 1;
      }
    }
    if (lastDot >= 0 && lastDot > lastSep) {
      return filename.substring(lastDot + 1);
    }
    return null;
  }

  /**
   * Method creating decorated {@link Iterable} of records where "decorated" means that special records
   * like descriptor, rootGroups and allGroups are automatically added as first and two last records (where group
   * related ones are being calculated during iterating over returned iterable).
   */
  private static Iterable<Record> decorate(final Iterable<Record> iterable,
                                           final String repositoryName)
  {
    final TreeSet<String> allGroups = new TreeSet<>();
    final TreeSet<String> rootGroups = new TreeSet<>();
    return transform(
        concat(
            singletonList(descriptor(repositoryName)),
            iterable,
            singletonList(allGroups(allGroups)), // placeholder, will be recreated at the end with proper content
            singletonList(rootGroups(rootGroups)) // placeholder, will be recreated at the end with proper content
        ),
        (Record rec) -> {
          if (Type.DESCRIPTOR == rec.getType()) {
            return rec;
          }
          else if (Type.ALL_GROUPS == rec.getType()) {
            return allGroups(allGroups);
          }
          else if (Type.ROOT_GROUPS == rec.getType()) {
            return rootGroups(rootGroups);
          }
          else {
            final String groupId = rec.get(Record.GROUP_ID);
            if (groupId != null) {
              allGroups.add(groupId);
              rootGroups.add(rootGroup(groupId));
            }
            return rec;
          }
        }
    );
  }

  /**
   * NX3 {@link MavenFacet} backed {@link WritableResourceHandler} to be used by {@link IndexWriter}.
   */
  static class Maven2WritableResourceHandler
      implements WritableResourceHandler
  {
    private final MavenFacet mavenFacet;

    Maven2WritableResourceHandler(final Repository repository) {
      this.mavenFacet = repository.facet(MavenFacet.class);
    }

    @Override
    public Maven2WritableResource locate(final String name) throws IOException {
      String contentType;
      if (name.endsWith(".properties")) {
        contentType = ContentTypes.TEXT_PLAIN;
      }
      else if (name.endsWith(".gz")) {
        contentType = ContentTypes.APPLICATION_GZIP;
      }
      else {
        throw new IllegalArgumentException("Unsupported MI index resource:" + name);
      }
      MavenPath mavenPath = mavenFacet.getMavenPathParser().parsePath("/.index/" + name);
      return new Maven2WritableResource(mavenPath, mavenFacet, contentType);
    }

    @Override
    public void close() throws IOException {
      // nop
    }
  }

  /**
   * NX3 {@link MavenFacet} and {@link MavenPath} backed {@link WritableResource}.
   */
  private static class Maven2WritableResource
      implements WritableResource
  {
    private final MavenPath mavenPath;

    private final MavenFacet mavenFacet;

    private final String contentType;

    private Path path;

    private Maven2WritableResource(final MavenPath mavenPath, final MavenFacet mavenFacet, final String contenType) {
      this.mavenPath = mavenPath;
      this.mavenFacet = mavenFacet;
      this.contentType = contenType;
      this.path = null;
    }

    @Override
    public InputStream read() throws IOException {
      Content content = mavenFacet.get(mavenPath);
      if (content != null) {
        return content.openInputStream();
      }
      return null;
    }

    @Override
    public OutputStream write() throws IOException {
      path = File.createTempFile(mavenPath.getFileName(), "tmp").toPath();
      return new BufferedOutputStream(Files.newOutputStream(path));
    }

    @Override
    public void close() throws IOException {
      if (path != null) {
        mavenFacet.put(
            mavenPath,
            new StreamPayload(
                new InputStreamSupplier()
                {
                  @Nonnull
                  @Override
                  public InputStream get() throws IOException {
                    return new BufferedInputStream(Files.newInputStream(path));
                  }
                },
                Files.size(path),
                contentType
            )
        );
        Files.delete(path);
        path = null;
      }
    }
  }

  /**
   * {@link Predicate} that filters {@link Record} based on allowed {@link Type}.
   */
  private static class RecordTypeFilter
      implements Predicate<Record>
  {
    private final List<Type> allowedTypes;

    public RecordTypeFilter(final Type... allowedTypes) {
      this.allowedTypes = Arrays.asList(allowedTypes);
    }

    @Override
    public boolean apply(final Record input) {
      return allowedTypes.contains(input.getType());
    }
  }

  /**
   * Returns default string if actual is blank.
   */
  private static String defStr(final String s, final String defaultValue) {
    if (Strings2.isBlank(s)) {
      return defaultValue;
    }
    return s;
  }
}
