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
package org.sonatype.nexus.repository.npm.internal;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;

/**
 * npm hosted facet.
 *
 * @since 3.0
 */
@Facet.Exposed
public interface NpmHostedFacet
    extends Facet
{
  /**
   * Returns the package metadata or {@code null}.
   */
  @Nullable
  Content getPackage(NpmPackageId packageId) throws IOException;

  /**
   * Performs a "publish" of a package as sent by npm CLI.
   */
  void putPackage(NpmPackageId packageId, @Nullable String revision, Payload payload) throws IOException;

  /**
   * Add the package using the package.json and <code>TempBlob</code>.
   *
   * @since 3.7
   */
  Asset putPackage(Map<String, Object> packageJson, TempBlob tempBlob) throws IOException;

  /**
   * Deletes complete package along with all belonging tarballs too (if any).
   *
   * @return name of deleted asset(s).
   */
  Set<String> deletePackage(NpmPackageId packageId, @Nullable String revision) throws IOException;

  /**
   * Deletes complete package along with all belonging tarballs too (if any), maybe deletes the blobs.
   *
   * @return name of deleted asset(s).
   *
   * @since 3.9
   */
  Set<String> deletePackage(NpmPackageId packageId, @Nullable String revision, boolean deleteBlobs) throws IOException;

  /**
   * Returns the tarball content or {@code null}.
   */
  @Nullable
  Content getTarball(NpmPackageId packageId, String tarballName) throws IOException;

  /**
   * Deletes given tarball, if exists.
   *
   * @return name of deleted asset(s).
   */
  Set<String> deleteTarball(NpmPackageId packageId, String tarballName) throws IOException;

  /**
   * Deletes given tarball, if exists, and maybe deletes the blob.
   *
   * @return name of deleted asset(s).
   *
   * @since 3.9
   */
  Set<String> deleteTarball(NpmPackageId packageId, String tarballName, boolean deleteBlob) throws IOException;

  /**
   * Updates the package root.
   *
   * @param packageId
   * @param revision
   * @param newPackageRoot
   *
   * @since 3.10
   */
  void putPackageRoot(final NpmPackageId packageId, @Nullable final String revision,
                      final NestedAttributesMap newPackageRoot) throws IOException;
}
