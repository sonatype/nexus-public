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
package org.sonatype.nexus.testsuite.testsupport.blobstore.restore;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.Repository;

/**
 * Helper class containing common functionality needed in ITs testing the restoration of component metadata from blobs.
 * Assumes a unit of work has already been started.
 */
public interface BlobstoreRestoreTestHelper
{
  void simulateComponentAndAssetMetadataLoss();

  void simulateAssetMetadataLoss();

  void simulateComponentMetadataLoss();

  void runRestoreMetadataTaskWithTimeout(long timeout);

  void runRestoreMetadataTask();

  void assertComponentNotInRepository(Repository repository, String name);

  void assertComponentNotInRepository(Repository repository, String name, String version);

  void assertComponentInRepository(Repository repository, String name);

  void assertComponentInRepository(Repository repository, String name, String version);

  void assertAssetNotInRepository(Repository repository, String... names);

  void assertAssetInRepository(Repository repository, String name);

  void assertAssetMatchesBlob(Repository repository, String name);

  void assertComponentWithGAVInRepository(Repository repository, String group, String name, String version);

  void assertComponentWithGAVNotInRepository(Repository repository, String group, String name, String version);

  void assertAssetAssociatedWithComponent(
      Repository repository,
      @Nullable String group,
      String name,
      String version,
      String... paths);

  void assertAssetAssociatedWithComponent(Repository repository, String name, String path);

  void assertAssetMatchesBlob(Repository repository, String... names);
}
