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
package org.sonatype.nexus.repository.rest.cma

import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.rest.api.AssetXO
import org.sonatype.nexus.repository.rest.api.RepositoryItemIDXO
import org.sonatype.nexus.repository.storage.Asset

import groovy.transform.CompileStatic

import static org.sonatype.nexus.common.entity.EntityHelper.id
import static org.sonatype.nexus.repository.storage.Asset.CHECKSUM

/**
 * Builds asset transfer objects for REST APIs.
 *
 * @since 3.22
 */
@CompileStatic
class AssetXOBuilder
{
  static AssetXO fromAsset(final Asset asset, final Repository repository) {
    String internalId = id(asset).getValue()

    Map checksum = asset.attributes().child(CHECKSUM).backing()

    return AssetXO.builder()
        .path(asset.name())
        .downloadUrl(repository.url + '/' + asset.name())
        .id(new RepositoryItemIDXO(repository.name, internalId).value)
        .repository(repository.name)
        .checksum(checksum)
        .format(repository.format.value)
        .build()
  }
}
