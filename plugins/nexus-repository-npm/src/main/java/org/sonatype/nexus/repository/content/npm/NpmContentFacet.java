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
package org.sonatype.nexus.repository.content.npm;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.npm.internal.NpmPackageId;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

/**
 * Provides persistent content for the 'npm' format.
 *
 * @since 3.28
 */
@Facet.Exposed
public interface NpmContentFacet
    extends ContentFacet
{
  /**
   * Delete the package root for the given packageId
   */
  boolean delete(NpmPackageId packageId) throws IOException;

  /**
   * Delete the tarball associated with the given packageId and version.
   */
  boolean delete(NpmPackageId packageId, String version) throws IOException;

  /**
   * Retrieve the package root for the given packageId
   */
  Optional<Content> get(NpmPackageId packageId) throws IOException;

  /**
   * Retrieve the tarball associated with the given packageId and version.
   */
  Optional<Content> get(NpmPackageId packageId, String version) throws IOException;

  /**
   * Set the contents of the search index
   */
  Content putSearchIndex(Content content);

  /**
   * Set the repository root content
   */
  Content putRepositoryRoot(Content content);

  /**
   * Upload the package root for the given packageId
   */
  FluentAsset put(NpmPackageId packageId, Payload content) throws IOException;

  /**
   * Upload the package root for the given packageId
   */
  FluentAsset put(NpmPackageId packageId, TempBlob tempBlob) throws IOException;

  /**
   * Upload the tarball associated with the given packageId and version.
   */
  Content put(NpmPackageId packageId, String version, Map<String, Object> npmAttributes, Payload content) throws IOException;

  /**
   * Upload the tarball associated with the given packageId and version.
   */
  Content put(NpmPackageId packageId, String version, Map<String, Object> npmAttributes, TempBlob tempBlob) throws IOException;


  public static String metadataPath(final NpmPackageId packageId) {
    return '/' + packageId.id();
  }

  public static String tarballPath(final NpmPackageId packageId, final String version) {
    return '/' + packageId.id() + "/-/" + packageId.name() + '-' + version + ".tgz";
  }
}
