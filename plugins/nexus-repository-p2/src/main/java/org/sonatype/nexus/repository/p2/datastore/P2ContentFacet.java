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
package org.sonatype.nexus.repository.p2.datastore;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.p2.internal.metadata.P2Attributes;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;

/**
 * The p2 format's {@link ContentFacet}
 *
 * @since 3.next
 */
@Facet.Exposed
public interface P2ContentFacet
    extends ContentFacet
{
  Optional<Content> get(String path) throws IOException;

  boolean delete(String path) throws IOException;

  /**
   * Put a non-bundle binary asset
   *
   * @param p2Attributes the p2 attributes associated with the asset
   * @param payload      the payload for the asset
   * @return a {@link Content} representing the created or updated asset
   */
  Content putBinary(P2Attributes p2Attributes, Payload payload) throws IOException;

  /**
   * Put a p2 bundle asset
   *
   * @param p2Attributes the p2 attributes associated with the asset
   * @param payload      the payload for the asset
   * @return             a {@link Content} representing the created or updated asset
   */
  Content putBundle(P2Attributes p2Attributes, Payload payload) throws IOException;

  /**
   * Put an artifacts metadata file for a repository.
   *
   * @param path       the path within the repository for the artifacts metadata
   * @param payload    the payload for the asset
   * @param attributes the p2 asset attributes for the metadata
   * @return           a {@link Content} representing the created or updated asset
   */
  Content putArtifactsMetadata(String path, Payload payload, Map<String, Object> attributes) throws IOException;

  /**
   * Put a composite artifacts metadata file for a repository.
   *
   * @param path       the path within the repository for the composite artifacts metadata
   * @param payload    the payload for the asset
   * @param attributes the p2 asset attributes for the metadata
   * @return           a {@link Content} representing the created or updated asset
   */
  Content putCompositeArtifactsMetadata(String path, Payload payload, Map<String, Object> attributes) throws IOException;

  /**
   * Put a composite contents metadata file for a repository.
   *
   * @param path       the path within the repository for the composite contents metadata
   * @param payload    the payload for the asset
   * @param attributes the p2 asset attributes for the metadata
   * @return           a {@link Content} representing the created or updated asset
   */
  Content putCompositeContentMetadata(String path, Payload payload, Map<String, Object> attributes) throws IOException;

  /**
   * Put a contents metadata file for a repository.
   *
   * @param path       the path within the repository for the contents metadata
   * @param payload    the payload for the asset
   * @param attributes the p2 asset attributes for the metadata
   * @return           a {@link Content} representing the created or updated asset
   */
  Content putContentMetadata(String path, Payload payload, Map<String, Object> attributes) throws IOException;

  /**
   * Put a p2 index file a repository.
   *
   * @param path       the path within the repository for the p2 index
   * @param payload    the payload for the asset
   * @param attributes the p2 asset attributes for the metadata
   * @return           a {@link Content} representing the created or updated asset
   */
  Content putP2Index(String path, Payload payload, Map<String, Object> attributes);
}
