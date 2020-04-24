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
package org.sonatype.nexus.repository.npm.internal.orient;

import java.io.Closeable;
import java.util.Map;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.storage.TempBlob;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Parsed "npm publish" or "npm unpublish" request containing the JSON contents (exclusive of attachments) and any
 * included attachments as TempBlobs. The "data" field of each attachment is replaced with a string that can be used to
 * fetch the blob.
 *
 * Note that this class should be used within a try-with-resources statement for safety, as it is intended that it will
 * manage its own temp blobs.
 *
 * @since 3.4
 */
public class NpmPublishRequest
    implements Closeable
{
  private final NestedAttributesMap packageRoot;

  private final Map<String, TempBlob> tempBlobs;

  public NpmPublishRequest(final NestedAttributesMap packageRoot, final Map<String, TempBlob> tempBlobs) {
    this.packageRoot = checkNotNull(packageRoot);
    this.tempBlobs = checkNotNull(tempBlobs);
  }

  public NestedAttributesMap getPackageRoot() {
    return packageRoot;
  }

  public TempBlob requireBlob(final String data) {
    TempBlob blob = tempBlobs.get(data);
    checkState(blob != null, "Missing temporary blob: " + data);
    return blob;
  }

  @Override
  public void close() {
    for (TempBlob tempBlob : tempBlobs.values()) {
      tempBlob.close();
    }
  }
}
