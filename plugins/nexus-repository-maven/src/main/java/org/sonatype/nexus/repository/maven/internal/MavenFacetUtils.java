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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.Coordinates;
import org.sonatype.nexus.repository.maven.MavenPath.HashType;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload.InputStreamSupplier;
import org.sonatype.nexus.repository.view.payloads.StringPayload;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashingOutputStream;
import org.joda.time.DateTime;

import static java.util.Collections.singletonList;
import static org.sonatype.nexus.common.app.VersionComparator.version;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_BASE_VERSION;
import static org.sonatype.nexus.repository.maven.internal.Constants.SNAPSHOT_VERSION_SUFFIX;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_GROUP;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_VERSION;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

/**
 * Maven facet utilities.
 *
 * @since 3.0
 */
public final class MavenFacetUtils
{
  private MavenFacetUtils() {
    // nop
  }

  /**
   * Version comparator that uses version scheme to sort version strings directly on the component.
   */
  public static final Comparator<Component> COMPONENT_VERSION_COMPARATOR = Comparator
      .comparing(o -> version(o.version()));

  /**
   * Finds component in given repository by maven path.
   */
  @Nullable
  public static Component findComponent(final StorageTx tx,
      final Repository repository,
      final MavenPath mavenPath)
  {
    final Coordinates coordinates = mavenPath.getCoordinates();
    final Iterable<Component> components = tx.findComponents(
        Query.builder()
            .where(P_GROUP).eq(coordinates.getGroupId())
            .and(P_NAME).eq(coordinates.getArtifactId())
            .and(P_VERSION).eq(coordinates.getVersion())
            .build(),
        singletonList(repository)
    );
    if (components.iterator().hasNext()) {
      return components.iterator().next();
    }
    return null;
  }

  /**
   * Finds asset in given bucket by key.
   */
  @Nullable
  public static Asset findAsset(final StorageTx tx,
      final Bucket bucket,
      final MavenPath mavenPath)
  {
    // The maven path is stored in the asset 'name' field, which is indexed (the maven format-specific key is not).
    return tx.findAssetWithProperty(P_NAME, mavenPath.getPath(), bucket);
  }

  /**
   * Is a given Component a release
   */
  public static boolean isRelease(final Component component) {
    return !isSnapshot(component);
  }

  /**
   * Is a given Component a snapshot
   */
  public static boolean isSnapshot(final Component component) {
    String baseVersion = (String) component.attributes().child(Maven2Format.NAME).get(P_BASE_VERSION);
    return baseVersion != null && baseVersion.endsWith(SNAPSHOT_VERSION_SUFFIX);
  }

  /**
   * Wrapper to pass in into {@link #createTempContent(Path, String, Writer)} to write out actual content.
   */
  public interface Writer
  {
    void write(OutputStream outputStream) throws IOException;
  }

  /**
   * Creates a temporary {@link Content} equipped will all the whistles and bells, like hashes and so.
   */
  public static Content createTempContent(final Path path, final String contentType, final Writer writer) throws IOException {
    Map<HashAlgorithm, HashingOutputStream> hashingStreams = new HashMap<>();
    try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(path))) {
      OutputStream os = outputStream;
      for (HashType hashType : HashType.values()) {
        os = new HashingOutputStream(hashType.getHashAlgorithm().function(), os);
        hashingStreams.put(hashType.getHashAlgorithm(), (HashingOutputStream) os);
      }
      writer.write(os);
      os.flush();
    }
    Map<HashAlgorithm, HashCode> hashCodes = new HashMap<>();
    for (Map.Entry<HashAlgorithm, HashingOutputStream> entry : hashingStreams.entrySet()) {
      hashCodes.put(entry.getKey(), entry.getValue().hash());
    }
    Content content = new Content(new StreamPayload(
        new InputStreamSupplier()
        {
          @Nonnull
          @Override
          public InputStream get() throws IOException {
            return new BufferedInputStream(Files.newInputStream(path));
          }
        },
        Files.size(path),
        contentType)
    );
    content.getAttributes().set(Content.CONTENT_LAST_MODIFIED, DateTime.now());
    content.getAttributes().set(Content.CONTENT_HASH_CODES_MAP, hashCodes);
    AttributesMap attributesMap = content.getAttributes();
    mayAddETag(attributesMap, getHashAlgorithmFromContent(attributesMap));
    return content;
  }

  /**
   * Adds {@link Content#CONTENT_ETAG} content attribute if not present. In case of hosted repositories, this is safe
   * and even good thing to do, as the content is hosted here only and NX is content authority.
   */
  public static void mayAddETag(final AttributesMap attributesMap,
                                final Map<HashAlgorithm, HashCode> hashCodes) {
    if (attributesMap.contains(Content.CONTENT_ETAG)) {
      return;
    }
    HashCode sha1HashCode = hashCodes.get(HashAlgorithm.SHA1);
    if (sha1HashCode != null) {
      attributesMap.set(Content.CONTENT_ETAG, "{SHA1{" + sha1HashCode + "}}");
    }
  }

  /**
   * Retrieves the hash codes stored in {@link Content#attributes}
   * @param attributesMap
   * @return
   */
  public static Map<HashAlgorithm, HashCode> getHashAlgorithmFromContent(final AttributesMap attributesMap) {
    return attributesMap
          .require(Content.CONTENT_HASH_CODES_MAP, Content.T_CONTENT_HASH_CODES_MAP);
  }

  /**
   * Performs a {@link MavenFacet#put(MavenPath, Payload)} for passed in {@link Content} and it's hashes too. Returns
   * the put content.
   */
  public static void putWithHashes(final MavenFacet mavenFacet,
                                   final MavenPath mavenPath,
                                   final Content content) throws IOException
  {
    final Map<HashAlgorithm, HashCode> hashCodes = getHashAlgorithmFromContent(content.getAttributes());
    final DateTime now = content.getAttributes().require(Content.CONTENT_LAST_MODIFIED, DateTime.class);

    mavenFacet.put(mavenPath, content);
    addHashes(mavenFacet, mavenPath, hashCodes, now);
  }

  public static void addHashes(final MavenFacet mavenFacet,
                                final MavenPath mavenPath,
                                final Map<HashAlgorithm, HashCode> hashCodes,
                                final DateTime now)
      throws IOException
  {
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

  /**
   * Performs a {@link MavenFacet#put(MavenPath, Payload)} for passed in {@link Content} and it's hashes too. Returns
   * the put content.
   *
   * @param mavenFacet facet to use for storing the hashes
   * @param mavenPath path to the created hashes
   * @param tempBlob blob with the hashes calculated
   * @param contentType
   * @param attributesMap attributes to be stored as part of the hashes
   * @return generated Content
   * @throws IOException
   */
  public static Content putWithHashes(final MavenFacet mavenFacet,
                                      final MavenPath mavenPath,
                                      final TempBlob tempBlob,
                                      final String contentType,
                                      final AttributesMap attributesMap) throws IOException
  {
    final DateTime now = DateTime.now();

    Content result = mavenFacet.put(mavenPath, tempBlob, contentType, attributesMap);
    result.getAttributes().set(Content.CONTENT_LAST_MODIFIED, now);
    addHashes(mavenFacet, mavenPath, tempBlob.getHashes(), now);
    return result;
  }

  /**
   * Performs a {@link MavenFacet#delete(MavenPath...)} for passed in {@link MavenPath} and all it's hashes too.
   * Returns {@code true} if any of deleted paths existed.
   */
  public static boolean deleteWithHashes(final MavenFacet mavenFacet, final MavenPath mavenPath) throws IOException {
    final ArrayList<MavenPath> paths = new ArrayList<>(HashType.values().length + 1);
    paths.add(mavenPath.main());
    for (HashType hashType : HashType.values()) {
      paths.add(mavenPath.main().hash(hashType));
    }
    return mavenFacet.delete(paths.toArray(new MavenPath[paths.size()]));
  }

  /**
   * Performs a {@link MavenFacet#delete(MavenPath...)} for passed in list of {@link MavenPath} and all hashes
   *
   * @since 3.14
   */
  public static void deleteWithHashes(final MavenFacet mavenFacet, final List<MavenPath> mavenPaths) throws IOException {
    final ArrayList<MavenPath> paths = new ArrayList<>();
    for (MavenPath path : mavenPaths) {
      paths.add(path.main());
      for (HashType hashType : HashType.values()) {
        paths.add(path.main().hash(hashType));
      }
    }
    mavenFacet.delete(paths.toArray(new MavenPath[paths.size()]));
  }

  /**
   * @return a collection of path of a given {@link MavenPath} and it's hashes' paths
   */
  public static Set<String> getPathWithHashes(final MavenPath mavenPath) {
    Set<String> paths = new HashSet<>();
    paths.add(mavenPath.main().getPath());
    for (HashType hashType : HashType.values()) {
      paths.add(mavenPath.main().hash(hashType).getPath());
    }
    return paths;
  }
}
