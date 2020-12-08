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
package org.sonatype.nexus.repository.upload;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sonatype.nexus.repository.view.Content;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The asset paths that were uploaded.
 *
 * @since 3.10
 */
public class UploadResponse
{
  private final List<String> assetPaths;

  private final Collection<Content> contents;

  public UploadResponse(final List<String> assetPaths) {
    this.contents = Collections.emptyList();
    this.assetPaths = checkNotNull(assetPaths);
  }

  public UploadResponse(final Collection<Content> contents, final List<String> assetPaths) {
    this.contents = checkNotNull(contents);
    this.assetPaths = checkNotNull(assetPaths);
  }

  public UploadResponse(final Content content, final List<String> assetPaths) {
    this.contents = Collections.singletonList(checkNotNull(content));
    this.assetPaths = checkNotNull(assetPaths);
  }

  public List<String> getAssetPaths() {
    return assetPaths;
  }

  public Collection<Content> getContents() {
    return contents;
  }
}
