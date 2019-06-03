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
package org.sonatype.nexus.repository.apt.internal.snapshot;

import org.sonatype.nexus.repository.view.Content;

import static org.sonatype.nexus.repository.apt.internal.AptMimeTypes.BZIP;
import static org.sonatype.nexus.repository.apt.internal.AptMimeTypes.GZIP;
import static org.sonatype.nexus.repository.apt.internal.AptMimeTypes.SIGNATURE;
import static org.sonatype.nexus.repository.apt.internal.AptMimeTypes.TEXT;
import static org.sonatype.nexus.repository.apt.internal.AptMimeTypes.XZ;

/**
 * @since 3.next
 */
public class SnapshotItem
{
  public static enum Role
  {
    RELEASE_INDEX(TEXT),
    RELEASE_INLINE_INDEX(TEXT),
    PACKAGE_INDEX_RAW(TEXT),
    RELEASE_SIG(SIGNATURE),
    PACKAGE_INDEX_GZ(GZIP),
    PACKAGE_INDEX_BZ2(BZIP),
    PACKAGE_INDEX_XZ(XZ);

    private final String mimeType;

    Role(String mimeType) {
      this.mimeType = mimeType;
    }

    public String getMimeType() {
      return mimeType;
    }
  }

  public static class ContentSpecifier
  {
    public final String path;

    public final Role role;

    public ContentSpecifier(String path, Role role) {
      super();
      this.path = path;
      this.role = role;
    }
  }

  public final ContentSpecifier specifier;

  public final Content content;

  public SnapshotItem(ContentSpecifier specifier, Content content) {
    super();
    this.specifier = specifier;
    this.content = content;
  }
}
