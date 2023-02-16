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
package org.sonatype.nexus.repository.maven.internal.orient;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Priority;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.orient.entity.AttachedEntityHelper;
import org.sonatype.nexus.orient.maven.OrientMavenFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPathParser;
import org.sonatype.nexus.repository.maven.internal.Attributes.AssetKind;
import org.sonatype.nexus.repository.maven.internal.MavenIndexPublisher;
import org.sonatype.nexus.repository.maven.internal.filter.DuplicateDetectionStrategy;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Closer;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.apache.maven.index.reader.IndexWriter;
import org.apache.maven.index.reader.Record;
import org.apache.maven.index.reader.WritableResourceHandler;
import org.apache.maven.index.reader.WritableResourceHandler.WritableResource;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static org.apache.maven.index.reader.Record.*;
import static org.apache.maven.index.reader.Record.Type.ARTIFACT_ADD;
import static org.sonatype.nexus.repository.maven.MavenPath.SignatureType.GPG;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_BUCKET;

/**
 * Orient specifics for Maven index publishing.
 *
 * @since 3.0
 */
@Named
@Singleton
@Priority(Integer.MAX_VALUE)
public final class OrientMavenIndexPublisher extends MavenIndexPublisher
{
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

  @Override
  protected MavenPathParser getMavenPathParser(final Repository repository) {
    return repository.facet(OrientMavenFacet.class).getMavenPathParser();
  }

  @Override
  protected WritableResourceHandler getResourceHandler(final Repository repository) {
    return new Maven2WritableResourceHandler(repository);
  }

  /**
   * Deletes given path from repository's storage/cache.
   */
  @Override
  protected boolean delete(final Repository repository, final String path) throws IOException {
    OrientMavenFacet mavenFacet = repository.facet(OrientMavenFacet.class);
    MavenPath mavenPath = getMavenPathParser(repository).parsePath(path);
    return !mavenFacet.delete(mavenPath).isEmpty();
  }

  @Override
  protected Iterable<Iterable<Record>> getGroupRecords(final List<Repository> repositories, final Closer closer)
      throws IOException
  {
    UnitOfWork paused = UnitOfWork.pause();
    try {
      List<Iterable<Record>> records = new ArrayList<>();
      for (Repository repository : repositories) {
        UnitOfWork.begin(repository.facet(StorageFacet.class).txSupplier());
        try {
          records.add(getRecords(repository, closer));
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
   * Publishes MI index into {@code target}, sourced from repository's own CMA structures.
   */
  @Override
  public void publishHostedIndex(
      final Repository repository,
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
   * Returns the records to publish of a hosted repository, the SELECT result count will be in parity with published
   * records count!
   */
  private Iterable<Record> getHostedRecords(final StorageTx tx, final Repository repository) {
    Map<String, Object> sqlParams = new HashMap<>();
    sqlParams.put(P_BUCKET, AttachedEntityHelper.id(tx.findBucket(repository)));
    sqlParams.put(P_ASSET_KIND, AssetKind.ARTIFACT.name());

    Iterable<ODocument> documents = tx.browse(SELECT_HOSTED_ARTIFACTS, sqlParams);
    OrientMavenFacet mavenFacet = repository.facet(OrientMavenFacet.class);

    return filterAndConvertToRecords(documents, mavenFacet);
  }

  @VisibleForTesting
  Iterable<Record> filterAndConvertToRecords(final Iterable<ODocument> documents, final OrientMavenFacet mavenFacet) {
    return StreamSupport.stream(documents.spliterator(), false)
        .filter((ODocument document) -> isCorrectComponent(mavenFacet, document))
        .map((ODocument document) -> toRecord(mavenFacet, document)).collect(Collectors.toList());
  }

  private boolean isCorrectComponent(final OrientMavenFacet mavenFacet, final ODocument document) {
    checkNotNull(document); // sanity
    final String path = document.field("path", String.class);
    MavenPath mavenPath = mavenFacet.getMavenPathParser().parsePath(path);
    return mavenPath.getCoordinates() != null && !mavenPath.isSubordinate(); // otherwise query is wrong
  }

  /**
   * Converts orient SQL query result into Maven Indexer Reader {@link Record}. Should be invoked only with documents
   * belonging to components, but not checksums or signatures.
   */
  private Record toRecord(final OrientMavenFacet mavenFacet, final ODocument document) {
    final String path = document.field("path", String.class);
    MavenPath mavenPath = mavenFacet.getMavenPathParser().parsePath(path);

    Record record = new Record(ARTIFACT_ADD, new HashMap<>());
    record.put(REC_MODIFIED, document.field("lastModified", Long.class));
    record.put(GROUP_ID, document.field("groupId", String.class));
    record.put(ARTIFACT_ID, document.field("artifactId", String.class));
    record.put(VERSION, document.field("version", String.class));
    record.put(CLASSIFIER, document.field("classifier", String.class));

    String packaging = document.field("packaging", String.class);
    if (packaging != null) {
      record.put(PACKAGING, packaging);
    }
    else {
      record.put(PACKAGING, pathExtension(mavenPath.getFileName()));
    }
    record.put(NAME, defStr(document.field("pom_name", String.class), ""));
    record.put(DESCRIPTION, defStr(document.field("pom_description", String.class), ""));

    checkExistence(record, HAS_SOURCES, mavenPath.locate("jar", "sources"), mavenFacet);
    checkExistence(record, HAS_JAVADOC, mavenPath.locate("jar", "javadoc"), mavenFacet);
    checkExistence(record, HAS_SIGNATURE, mavenPath.signature(GPG), mavenFacet);

    record.put(FILE_EXTENSION, pathExtension(mavenPath.getFileName()));
    record.put(FILE_MODIFIED, document.field("contentLastModified", Long.class));
    record.put(FILE_SIZE, document.field("contentSize", Long.class));
    record.put(SHA1, document.field("sha1", String.class));
    return record;
  }

  private void checkExistence(
      final Record record,
      final EntryKey<Boolean> key,
      final MavenPath tocheck,
      final OrientMavenFacet mavenFacet)
  {
    record.put(key, mavenFacet.exists(tocheck));
  }

   /**
   * NX3 {@link OrientMavenFacet} backed {@link WritableResourceHandler} to be used by {@link IndexWriter}.
   */
  static class Maven2WritableResourceHandler
      implements WritableResourceHandler
  {
    private final OrientMavenFacet mavenFacet;

    Maven2WritableResourceHandler(final Repository repository) {
      this.mavenFacet = repository.facet(OrientMavenFacet.class);
    }

    @Override
    public Maven2WritableResource locate(final String name) {
      MavenPath mavenPath = mavenFacet.getMavenPathParser().parsePath("/.index/" + name);
      return new Maven2WritableResource(mavenPath, mavenFacet, determineContentType(name));
    }

    @Override
    public void close() {
      // nop
    }
  }

  /**
   * NX3 {@link OrientMavenFacet} and {@link MavenPath} backed {@link WritableResource}.
   */
  private static class Maven2WritableResource
      implements WritableResource
  {
    private final MavenPath mavenPath;

    private final OrientMavenFacet mavenFacet;

    private final String contentType;

    private Path path;

    private Maven2WritableResource(final MavenPath mavenPath, final OrientMavenFacet mavenFacet, final String contenType) {
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
        mavenFacet.put(mavenPath, createPayload(path, contentType));
        Files.delete(path);
        path = null;
      }
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
