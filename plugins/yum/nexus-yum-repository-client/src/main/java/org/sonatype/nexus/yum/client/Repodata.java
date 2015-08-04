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
package org.sonatype.nexus.yum.client;

import java.io.IOException;

/**
 * Interface to access nexus-yum-repository-plugin functionality in tests
 *
 * @since yum 3.0
 */
public interface Repodata
{

  /**
   * Retrieves the given metadata type (primary.xml, repomd.xml, etc.) from the repository
   *
   * @param metadataType {@link String} or <code>byte[].class</code>
   * @return the content of the metadata file. If the file is gzipped or bzipped the returning content is
   *         uncompressed
   *         automatically
   */
  <T> T getMetadata(String repositoryId, MetadataType metadataType, Class<T> returnType)
      throws IOException;

  /**
   * Retrieves the given metadata type (primary.xml, repomd.xml, etc.) from the repository and version
   *
   * @param metadataType {@link String} or <code>byte[].class</code>
   * @return the content of the metadata file. If the file is gzipped or bzipped the returning content is
   *         uncompressed
   *         automatically
   */
  <T> T getMetadata(String repositoryId, String version, MetadataType metadataType, Class<T> returnType)
      throws IOException;

  /**
   * Retrieves the repository path of given metadata type (primary.xml, repomd.xml, etc.) from the repository.
   */
  String getMetadataPath(String repositoryId, MetadataType metadataType)
      throws IOException;

  /**
   * Retrieves the full repository URL of given metadata type (primary.xml, repomd.xml, etc.) from the repository.
   */
  String getMetadataUrl(String repositoryId, MetadataType metadataType)
      throws IOException;


  String getIndex(String repositoryId, String version);

  String getIndex(String repositoryId, String version, String path);

}
