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
package org.sonatype.nexus.repository.maven.internal.group;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.storage.AssetEvent;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.thread.io.StreamCopier;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

@Facet.Exposed
public interface MavenGroupFacet
    extends GroupFacet
{
  /**
   * Fetches cached content if exists, or {@code null}.
   */
  @Nullable
  Content getCached(MavenPath mavenPath) throws IOException;

  /**
   * Merges and caches and returns the merged metadata. Returns {@code null} if no usable response was in passed in
   * map.
   */
  @Nullable
  Content mergeAndCache(MavenPath mavenPath, Map<Repository, Response> responses) throws IOException;

  /**
   * Merges the metadata but doesn't cache it. Returns {@code null} if no usable response was in passed in map.
   *
   * @since 3.13
   */
  @Nullable
  Content mergeWithoutCaching(MavenPath mavenPath, Map<Repository, Response> responses) throws IOException;

  @FunctionalInterface
  interface ContentFunction<T>
  {
    Content apply(T data, String contentType) throws IOException;
  }

  /**
   * Allows different merge methods to be used with the {@link StreamCopier}
   */
  interface MetadataMerger {
    void merge(OutputStream outputStream, MavenPath mavenPath, LinkedHashMap<Repository, Content> contents);
  }
}
