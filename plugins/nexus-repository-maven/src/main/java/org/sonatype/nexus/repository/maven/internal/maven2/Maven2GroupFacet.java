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
package org.sonatype.nexus.repository.maven.internal.maven2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.hash.Hashes;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.group.GroupFacetImpl;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.HashType;
import org.sonatype.nexus.repository.maven.internal.maven2.Maven2MetadataMerger.MetadataEnvelope;
import org.sonatype.nexus.repository.storage.AssetEvent;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.util.TypeTokens;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.common.hash.HashCode;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Maven2 specific implementation of {@link GroupFacetImpl}: metadata merge is specific to Maven2 format.
 *
 * @since 3.0
 */
@Named
@Facet.Exposed
public class Maven2GroupFacet
    extends GroupFacetImpl
{
  private final Maven2MetadataMerger metadataMerger;

  private MavenFacet mavenFacet;

  @Inject
  public Maven2GroupFacet(final RepositoryManager repositoryManager) {
    super(repositoryManager);
    this.metadataMerger = new Maven2MetadataMerger();
  }

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    super.doInit(configuration);
    this.mavenFacet = facet(MavenFacet.class);
  }

  /**
   * Fetches cached metadata if exists, or {@code null}.
   */
  @Nullable
  public Content getCachedMergedMetadata(final MavenPath mavenPath) throws IOException {
    final Content content = mavenFacet.get(mavenPath);
    if (mavenPath.isHash()) {
      return content; // hashes are recalculated whenever metadata is merged, so they're always fresh
    }
    return !isStale(content) ? content : null;
  }

  /**
   * Merges and caches and returns the merged metadata. Returns {@code null} if no usable response was in passed in
   * map.
   */
  @Nullable
  public Content mergeAndCacheMetadata(final MavenPath mavenPath,
                                       final LinkedHashMap<Repository, Response> responses) throws IOException
  {
    checkArgument(mavenFacet.getMavenPathParser().isRepositoryMetadata(mavenPath),
        "Only metadata can be merged and cached: %s", mavenPath);
    checkArgument(!mavenPath.isSubordinate(), "Only metadata XML can be merged and cached: %s", mavenPath);
    final LinkedHashMap<Repository, Content> metadataContents = Maps.newLinkedHashMap();
    for (Map.Entry<Repository, Response> entry : responses.entrySet()) {
      if (entry.getValue().getStatus().getCode() == HttpStatus.OK) {
        final Response response = entry.getValue();
        if (response.getPayload() instanceof Content) {
          metadataContents.put(entry.getKey(), (Content) response.getPayload());
        }
      }
    }

    if (metadataContents.isEmpty()) {
      return null;
    }
    final Content content = mergeMetadata(mavenPath, metadataContents);
    if (content == null) {
      return null;
    }
    cacheMetadata(mavenPath, content);
    return content; // content is reusable, so just return it
  }

  /**
   * Merges the contents of passed in metadata and returns the {@link Content} of the resulting merge. The content
   * returned by this method is kept in memory, is reusable and should be passed back to caller as is.
   *
   * @return {@code null} if no merge possible for various reasons (ie. corrupted metadata). If non-null is returned,
   * the {@link Content} contains merged metadata and is reusable.
   */
  @Nullable
  private Content mergeMetadata(final MavenPath mavenPath,
                                final LinkedHashMap<Repository, Content> metadataContents) throws IOException
  {
    if (metadataContents.size() == 1) {
      return metadataContents.get(metadataContents.keySet().iterator().next());
    }
    final MetadataXpp3Reader reader = new MetadataXpp3Reader();
    List<MetadataEnvelope> metadatas = Lists.newArrayList();
    for (Map.Entry<Repository, Content> entry : metadataContents.entrySet()) {
      final String origin = entry.getKey().getName() + " @ " + mavenPath.getPath();
      try (InputStream inputStream = entry.getValue().openInputStream()) {
        final Metadata metadata = reader.read(inputStream);
        metadatas.add(new MetadataEnvelope(origin, metadata));
      }
      catch (XmlPullParserException e) {
        // skip it, log it
        log.info("Unparseable repository metadata: {}", origin, e);
      }
    }

    final Metadata mergedMetadata = metadataMerger.merge(metadatas);
    if (mergedMetadata == null) {
      return null;
    }
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    new MetadataXpp3Writer().write(byteArrayOutputStream, mergedMetadata);
    // Metadata is rather small, so let's do it like this
    final byte[] byteArray = byteArrayOutputStream.toByteArray();
    final Map<HashAlgorithm, HashCode> hashCodes = Hashes.hash(HashType.ALGORITHMS,
        new ByteArrayInputStream(byteArray));
    final Content content = new Content(
        new BytesPayload(
            byteArray,
            Maven2MimeRulesSource.METADATA_TYPE
        ));
    content.getAttributes().set(Content.CONTENT_LAST_MODIFIED, DateTime.now());
    content.getAttributes().set(Content.CONTENT_ETAG, "{SHA1{" + hashCodes.get(HashAlgorithm.SHA1).toString() + "}}");
    content.getAttributes().set(Content.CONTENT_HASH_CODES_MAP, hashCodes);
    return content;
  }

  /**
   * Caches the merged metadata and it's Maven2 format required sha1/md5 hashes along.
   */
  private void cacheMetadata(final MavenPath mavenPath, final Content content) throws IOException {
    final Map<HashAlgorithm, HashCode> hashCodes = content.getAttributes().require(
        Content.CONTENT_HASH_CODES_MAP, TypeTokens.HASH_CODES_MAP);
    final DateTime now = content.getAttributes().require(Content.CONTENT_LAST_MODIFIED, DateTime.class);
    // cache the metadata and the hashes
    mavenFacet.put(mavenPath, maintainCacheInfo(content));
    for (HashType hashType : HashType.values()) {
      final HashCode hashCode = hashCodes.get(hashType.getHashAlgorithm());
      if (hashCode != null) {
        final Content hashContent = new Content(
            new StringPayload(hashCode.toString(), Constants.CHECKSUM_CONTENT_TYPE));
        hashContent.getAttributes().set(Content.CONTENT_LAST_MODIFIED, now);
        mavenFacet.put(mavenPath.hash(hashType), hashContent);
      }
    }
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final AssetEvent event) {
    if (member(event.getRepositoryName()) && event.getComponentId() == null) {
      final String path = event.getAsset().formatAttributes().require(StorageFacet.P_PATH, String.class);
      final MavenPath mavenPath = mavenFacet.getMavenPathParser().parsePath(path);
      // group deletes md + hashes, but it should do only on md change in member
      if (!mavenPath.isHash() && mavenFacet.getMavenPathParser().isRepositoryMetadata(mavenPath)) {
        UnitOfWork.begin(getRepository().facet(StorageFacet.class).txSupplier());
        try {
          final List<MavenPath> paths = Lists.newArrayList();
          paths.add(mavenPath.main());
          for (HashType hashType : HashType.values()) {
            paths.add(mavenPath.main().hash(hashType));
          }
          mavenFacet.delete(paths.toArray(new MavenPath[paths.size()]));
        }
        catch (IOException e) {
          log.warn("Could not evict merged metadata from {} cache at {}", getRepository().getName(),
              mavenPath.getPath(), e);
        }
        finally {
          UnitOfWork.end();
        }
      }
    }
  }
}
