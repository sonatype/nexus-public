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
package org.sonatype.nexus.mindexer.client;

import java.io.File;
import java.io.IOException;

public interface MavenIndexer
{

  /**
   * Search for an artifact using SHA1 hash.
   */
  SearchResponse identifyBySha1(String sha1);

  /**
   * Search for an artifact using SHA1 hash of the provided file.
   *
   * @throws IOException when the passed in file is not found, not readable etc. In general, when the SHA1 hash for
   *                     given file cannot be calculated.
   */
  SearchResponse identifyBySha1(File file)
      throws IOException;

  /**
   * Search for an artifact using keyword.
   */
  SearchResponse searchByKeyword(String kw, String repositoryId);

  /**
   * Search for an artifact using Maven GAV coordinates (groupId, artifactId, version, classifier, extension)
   */
  SearchResponse searchByGAV(String groupId, String artifactId, String version, String classifier, String type,
                             String repositoryId);

  /**
   * Search by class name.
   */
  SearchResponse searchByClassname(String className, String repositoryId);

  /**
   * Advanced search. Covering all those above plus more (paging, expansion control, etc).
   */
  SearchResponse search(SearchRequest request);
}
