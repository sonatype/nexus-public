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
package org.sonatype.nexus.repository.browse.internal.api

import static com.google.common.base.Preconditions.checkNotNull

/**
 * An object to hold the id and repository id of an asset in the format repository-id:asset-id, encoded.
 * @since 3.3
 */
class AssetXOID
{

  String repositoryId

  String id

  AssetXOID(final String repositoryId, final String id) {
    this.repositoryId = checkNotNull(repositoryId)
    this.id = checkNotNull(id)
  }

  public static AssetXOID fromString(String encoded) {
    String decoded = new String(Base64.getUrlDecoder().decode(encoded))
    String[] parts = decoded.split(":")
    if (parts.length != 2) {
      throw new IllegalArgumentException("Unable to parse AssetXOID " + encoded)
    }
    return new AssetXOID(parts[0], parts[1])
  }

  public String getValue() {
    return new String(Base64.getUrlEncoder().withoutPadding().encode("$repositoryId:$id".bytes));
  }
}
