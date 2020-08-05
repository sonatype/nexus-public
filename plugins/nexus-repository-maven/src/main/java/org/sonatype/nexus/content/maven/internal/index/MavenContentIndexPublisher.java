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
package org.sonatype.nexus.content.maven.internal.index;

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
import java.util.Optional;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentQuery;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.SignatureType;
import org.sonatype.nexus.repository.maven.MavenPathParser;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.maven.internal.MavenIndexPublisher;
import org.sonatype.nexus.repository.maven.internal.filter.DuplicateDetectionStrategy;
import org.sonatype.nexus.repository.view.Content;

import com.google.common.io.Closer;
import org.apache.maven.index.reader.IndexWriter;
import org.apache.maven.index.reader.Record;
import org.apache.maven.index.reader.WritableResourceHandler;
import org.apache.maven.index.reader.WritableResourceHandler.WritableResource;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.maven.index.reader.Record.*;
import static org.apache.maven.index.reader.Record.Type.ARTIFACT_ADD;
import static org.sonatype.nexus.repository.maven.internal.Attributes.AssetKind.ARTIFACT;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_BASE_VERSION;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_CLASSIFIER;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_PACKAGING;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_POM_DESCRIPTION;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_POM_NAME;
import static org.sonatype.nexus.repository.view.Content.CONTENT_LAST_MODIFIED;

/**
 * Maven index publishing for non-orient.
 *
 * @since 3.26
 */
@Named
@Singleton
public class MavenContentIndexPublisher
    extends MavenIndexPublisher
{
  private final int browseAssetsPageSize;

  @Inject
  public MavenContentIndexPublisher(
      @Named("${nexus.maven.index.publisher.browseAssetsPageSize:-1000}") final int browseAssetsPageSize)
  {
    this.browseAssetsPageSize = browseAssetsPageSize;
  }

  @Override
  protected MavenPathParser getMavenPathParser(final Repository repository) {
    return repository.facet(MavenContentFacet.class).getMavenPathParser();
  }

  @Override
  protected WritableResourceHandler getResourceHandler(final Repository repository) {
    return new Maven2WritableResourceHandler(repository);
  }

  @Override
  protected boolean delete(final Repository repository, final String path) throws IOException {
    MavenContentFacet mavenContentFacet = repository.facet(MavenContentFacet.class);
    MavenPath mavenPath = getMavenPathParser(repository).parsePath(path);
    return mavenContentFacet.delete(mavenPath);
  }

  @Override
  protected Iterable<Iterable<Record>> getGroupRecords(
      final List<Repository> repositories,
      final Closer closer) throws IOException
  {
    List<Iterable<Record>> records = new ArrayList<>();
    for (Repository repository : repositories) {
      records.add(getRecords(repository, closer));
    }
    return records;
  }

  @Override
  public void publishHostedIndex(
      final Repository repository, final DuplicateDetectionStrategy<Record> duplicateDetectionStrategy)
      throws IOException
  {
    try (Maven2WritableResourceHandler resourceHandler = new Maven2WritableResourceHandler(repository)) {
      try (IndexWriter indexWriter = new IndexWriter(resourceHandler, repository.getName(), false)) {
        indexWriter.writeChunk(records(repository, duplicateDetectionStrategy).iterator());
      }
    }
  }

  private Iterable<Map<String, String>> records(
      final Repository repository,
      final DuplicateDetectionStrategy<Record> duplicateDetectionStrategy)
  {
    List<Record> hostedRecords = getHostedRecords(repository, duplicateDetectionStrategy);
    return StreamSupport.stream(decorate(hostedRecords, repository.getName()).spliterator(), false)
        .map(RECORD_COMPACTOR::apply)
        .collect(toList());
  }

  private List<Record> getHostedRecords(
      final Repository repository,
      final DuplicateDetectionStrategy<Record> duplicateDetectionStrategy) {

    List<Record> records = new ArrayList<>();
    MavenContentFacet mavenContentFacet = repository.facet(MavenContentFacet.class);

    FluentQuery<FluentAsset> artifactQuery = mavenContentFacet.assets().byKind(ARTIFACT.name());
    Continuation<FluentAsset> assets = artifactQuery.browse(browseAssetsPageSize, null);
    while (!assets.isEmpty()) {
      records.addAll(assetsToRecords(assets, mavenContentFacet, duplicateDetectionStrategy));
      assets = artifactQuery.browse(browseAssetsPageSize, assets.nextContinuationToken());
    }

    return records;
  }

  private List<Record> assetsToRecords(
      final Continuation<FluentAsset> assets,
      final MavenContentFacet mavenContentFacet,
      final DuplicateDetectionStrategy<Record> duplicateDetectionStrategy)
  {
    return assets.stream()
        .filter(asset -> asset.component().isPresent())
        .map(asset -> toRecord(asset, mavenContentFacet))
        .filter(duplicateDetectionStrategy)
        .collect(toList());
  }

  private Record toRecord(final FluentAsset asset, final MavenContentFacet mavenContentFacet) {
    MavenPath mavenPath = mavenContentFacet.getMavenPathParser().parsePath(asset.path());
    checkArgument(mavenPath.getCoordinates() != null && !mavenPath.isSubordinate());
    checkArgument(asset.component().isPresent());

    Component component = asset.component().get();

    Record record = new Record(ARTIFACT_ADD, new HashMap<>());
    record.put(REC_MODIFIED,  asset.lastUpdated().toEpochSecond());

    record.put(GROUP_ID, component.namespace());
    record.put(ARTIFACT_ID, component.name());

    Optional.ofNullable(asset.attributes(Maven2Format.NAME).get(P_CLASSIFIER))
        .ifPresent(classifier -> record.put(CLASSIFIER, classifier.toString()));

    copyComponentAttributes(mavenPath, component.attributes(Maven2Format.NAME), record);

    record.put(HAS_SOURCES, mavenContentFacet.exists(mavenPath.locate("jar", "sources")));
    record.put(HAS_JAVADOC, mavenContentFacet.exists(mavenPath.locate("jar", "javadoc")));
    record.put(HAS_SIGNATURE, mavenContentFacet.exists(mavenPath.signature(SignatureType.GPG)));

    record.put(FILE_EXTENSION, pathExtension(mavenPath.getFileName()));

    ofNullable(asset.download().getAttributes().get(CONTENT_LAST_MODIFIED))
        .ifPresent(contentLastModified ->
            record.put(FILE_MODIFIED, DateTime.parse(contentLastModified.toString()).getMillis()));

    copyBlobAttributes(asset, record);
    return record;
  }

  private void copyComponentAttributes(
      final MavenPath mavenPath,
      final NestedAttributesMap componentFormatAttributes,
      final Record record)
  {
    Optional.ofNullable(componentFormatAttributes.get(P_BASE_VERSION))
        .ifPresent(baseVersion -> record.put(VERSION, baseVersion.toString()));

    String packaging = ofNullable(componentFormatAttributes.get(P_PACKAGING))
        .map(Object::toString)
        .orElseGet(() -> pathExtension(mavenPath.getFileName()));
    record.put(PACKAGING, packaging);

    record.put(NAME, ofNullable(componentFormatAttributes.get(P_POM_NAME))
        .map(Object::toString).orElse(EMPTY));

    record.put(NAME,
        ofNullable(componentFormatAttributes.get(P_POM_DESCRIPTION)).map(Object::toString).orElse(EMPTY));
  }

  private void copyBlobAttributes(final FluentAsset asset, final Record record) {
    asset.blob().ifPresent(assetBlob -> {
      record.put(FILE_SIZE, assetBlob.blobSize());
      record.put(SHA1, assetBlob.checksums().get(HashAlgorithm.SHA1.name()));
    });
  }

  /**
   * NX3 {@link MavenContentFacet} backed {@link WritableResourceHandler} to be used by {@link IndexWriter}.
   */
  static class Maven2WritableResourceHandler
      implements WritableResourceHandler
  {
    private final MavenContentFacet mavenFacet;

    Maven2WritableResourceHandler(final Repository repository) {
      this.mavenFacet = repository.facet(MavenContentFacet.class);
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
   * NX3 {@link MavenContentFacet} and {@link MavenPath} backed {@link WritableResource}.
   */
  private static class Maven2WritableResource
      implements WritableResource
  {
    private final MavenPath mavenPath;

    private final MavenContentFacet mavenFacet;

    private final String contentType;

    private Path path;

    private Maven2WritableResource(
        final MavenPath mavenPath,
        final MavenContentFacet mavenFacet,
        final String contentType)
    {
      this.mavenPath = mavenPath;
      this.mavenFacet = mavenFacet;
      this.contentType = contentType;
      this.path = null;
    }

    @Override
    public InputStream read() throws IOException {
      Optional<Content> content = mavenFacet.get(mavenPath);
      if (content.isPresent()) {
        return content.get().openInputStream();
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
        mavenFacet.put(mavenPath, createStreamPayload(path, contentType));
        Files.delete(path);
        path = null;
      }
    }
  }
}
