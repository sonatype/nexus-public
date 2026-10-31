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
package org.sonatype.nexus.common.app.softdelete;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Shared codec for soft-delete entity identifiers: {@code base64url(repositoryName ":" externalId)}, without
 * padding. Lives in the shared SPI so the public content layer ({@code ContentDeletionService}) and the private
 * soft-delete plugin ({@code SoftDeleteIdentifier}) encode identifiers identically and cannot drift apart on
 * padding, separator, or charset (which would silently produce un-restorable trash rows).
 */
public final class SoftDeleteIdentifiers
{
  private SoftDeleteIdentifiers() {
  }

  /**
   * Encodes {@code base64url(repositoryName ":" externalId)} without padding.
   */
  public static String encode(final String repositoryName, final String externalId) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString((repositoryName + ":" + externalId).getBytes(StandardCharsets.UTF_8));
  }
}
