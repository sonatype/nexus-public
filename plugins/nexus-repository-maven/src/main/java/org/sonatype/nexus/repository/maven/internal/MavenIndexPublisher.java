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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

import javax.annotation.Nonnull;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.maven.MavenIndexFacet;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPathParser;
import org.sonatype.nexus.repository.maven.internal.filter.DuplicateDetectionStrategy;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload.InputStreamSupplier;

import com.google.common.base.Predicate;
import com.google.common.io.Closer;
import org.apache.maven.index.reader.ChunkReader;
import org.apache.maven.index.reader.IndexReader;
import org.apache.maven.index.reader.IndexWriter;
import org.apache.maven.index.reader.Record;
import org.apache.maven.index.reader.Record.Type;
import org.apache.maven.index.reader.RecordCompactor;
import org.apache.maven.index.reader.RecordExpander;
import org.apache.maven.index.reader.ResourceHandler;
import org.apache.maven.index.reader.WritableResourceHandler;
import org.joda.time.DateTime;

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

/**
 * General logic for Maven index publishing.
 *
 * @since 3.26
 */
public abstract class MavenIndexPublisher extends ComponentSupport
{
  private static final String INDEX_PROPERTY_FILE = "/" + INDEX_PROPERTY_FILE_PATH;

  private static final String INDEX_MAIN_CHUNK_FILE = "/" + INDEX_MAIN_CHUNK_FILE_PATH;

  private static final RecordExpander RECORD_EXPANDER = new RecordExpander();

  protected static final RecordCompactor RECORD_COMPACTOR = new RecordCompactor();

  /**
   * Gets the MavenPathParser for the specified repository.
   */
  protected abstract MavenPathParser getMavenPathParser(final Repository repository);

  /**
   *
   * Returns a ResourceHandler implementation.
   */
  protected abstract WritableResourceHandler getResourceHandler(final Repository repository);

  /**
   * Deletes the asset at the specified path.
   */
  protected abstract boolean delete(Repository repository, String path) throws IOException;

  /**
   * Deletes index files from given repository, returns {@code true} if there was index in repository.
   */
  public boolean unpublishIndexFiles(final Repository repository) throws IOException {
    checkNotNull(repository);
    return delete(repository, INDEX_PROPERTY_FILE)
        && delete(repository, INDEX_MAIN_CHUNK_FILE);
  }

  /**
   * Get group records from the specified repositories.
   */
  protected abstract Iterable<Iterable<Record>> getGroupRecords(
      final List<Repository> repositories,
      final Closer closer) throws IOException;

  protected Iterable<Record> getRecords(final Repository repository, final Closer closer) throws IOException {
    ResourceHandler resourceHandler = closer.register(getResourceHandler(repository));
    IndexReader indexReader = closer.register(new IndexReader(null, resourceHandler));
    ChunkReader chunkReader = closer.register(indexReader.iterator().next());
    return filter(transform(chunkReader, RECORD_EXPANDER::apply), new RecordTypeFilter(Type.ARTIFACT_ADD));
  }

  /**
   * Publishes MI index into {@code target}, sourced from repository's own CMA structures.
   */
  public abstract void publishHostedIndex(
      final Repository repository,
      final DuplicateDetectionStrategy<Record> duplicateDetectionStrategy) throws IOException;

  /**
   * Publishes the Maven index into {@code groupRepository}, sourced from {@code leafMembers} repositories.
   */
  public void publishGroupIndex(
      final Repository groupRepository,
      final List<Repository> leafMembers,
      final DuplicateDetectionStrategy<Record> strategy) throws IOException
  {
    List<String> withoutIndex = new ArrayList<>();
    for (Iterator<Repository> ri = leafMembers.iterator(); ri.hasNext(); ) {
      Repository leafMemberRepository = ri.next();
      if (leafMemberRepository.facet(MavenIndexFacet.class).lastPublished() == null) {
        withoutIndex.add(leafMemberRepository.getName());
        ri.remove();
      }
    }
    if (!withoutIndex.isEmpty()) {
      log.info("Following members of group {} have no index, will not participate in merged index: {}",
          groupRepository.getName(),
          withoutIndex
      );
    }
    publishMergedIndex(groupRepository, leafMembers, strategy);
  }

  /**
   * Publishes the Maven index for the specified proxy repository.
   */
  public void publishProxyIndex(
      final Repository repository,
      final Boolean cacheFallback,
      final DuplicateDetectionStrategy<Record> strategy) throws IOException
  {
    if (!prefetchIndexFiles(repository)) {
      if (Boolean.TRUE.equals(cacheFallback)) {
        log.debug("No remote index found... generating partial index from caches");
        publishHostedIndex(repository, strategy);
      }
      else {
        log.debug("No remote index found... nothing to publish");
      }
    }
  }

  /**
   * Publishes MI index into {@code target}, sourced from {@code repositories} repositories.
   */
  private void publishMergedIndex(
      final Repository target,
      final List<Repository> repositories,
      final DuplicateDetectionStrategy<Record> duplicateDetectionStrategy) throws IOException
  {
    checkNotNull(target);
    checkNotNull(repositories);
    Closer closer = Closer.create();
    try (WritableResourceHandler resourceHandler = getResourceHandler(target);
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
   * Returns the {@link DateTime} when index of the given repository was last published.
   */
  public DateTime lastPublished(final Repository repository) throws IOException {
    checkNotNull(repository);
    try (ResourceHandler resourceHandler = getResourceHandler(repository)) {
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
  public boolean prefetchIndexFiles(final Repository repository) throws IOException {
    checkNotNull(repository);
    checkArgument(ProxyType.NAME.equals(repository.getType().getValue()));
    MavenPathParser mavenPathParser = getMavenPathParser(repository);
    return prefetch(repository, INDEX_PROPERTY_FILE, mavenPathParser)
        && prefetch(repository, INDEX_MAIN_CHUNK_FILE, mavenPathParser);
  }

  /**
   * Primes proxy cache with given path and return {@code true} if succeeds. Accepts only maven proxy type.
   */
  private static boolean prefetch(
      final Repository repository,
      final String path, final MavenPathParser mavenPathParser) throws IOException
  {
    MavenPath mavenPath = mavenPathParser.parsePath(path);
    Request getRequest = new Request.Builder()
        .action(GET)
        .path(path)
        .build();
    Context context = new Context(repository, getRequest);
    context.getAttributes().set(MavenPath.class, mavenPath);
    return repository.facet(ProxyFacet.class).get(context) != null;
  }

  /**
   * This method is copied from MI and Plexus related methods, to produce exactly same (possibly buggy) extensions out
   * of a file path, as MI client will attempt to "fix" those.
   */
  protected static String pathExtension(final String path) {
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
  protected static Iterable<Record> decorate(
      final Iterable<Record> iterable,
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

  protected static String determineContentType(final String name) {
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
    return contentType;
  }

  protected static StreamPayload createStreamPayload(final Path path, final String contentType) throws IOException {
    return new StreamPayload(
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
    );
  }
  /**
   * {@link Predicate} that filters {@link Record} based on allowed {@link Type}.
   */
  protected static class RecordTypeFilter
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

}
