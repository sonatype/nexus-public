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

import javax.annotation.Nullable

import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.rest.api.AssetXO
import org.sonatype.nexus.repository.rest.api.AssetXODescriptor
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
  static AssetXO fromAsset(final Asset asset, final Repository repository,
                           @Nullable final Map<String, AssetXODescriptor> assetDescriptors)
  {
    String internalId = id(asset).getValue()
    Map checksum = asset.attributes().child(CHECKSUM).backing()
    String format = repository.format.value

    return AssetXO.builder()
        .path(asset.name())
        .downloadUrl(repository.url + '/' + asset.name())
        .id(new RepositoryItemIDXO(repository.name, internalId).value)
        .repository(repository.name)
        .checksum(checksum)
        .format(format)
        .contentType(asset.contentType())
        .attributes(getExpandedAttributes(asset, format, assetDescriptors))
        .lastModified(calculateLastModified(asset))
        .lastDownloaded(getLastDownloaded(asset))
        .uploader(asset.createdBy())
        .uploaderIp(asset.createdByIp())
        .fileSize(asset.size())
        .build()
  }

  @Nullable
  private static Date getLastDownloaded(final Asset asset) {
    if(asset.lastDownloaded() == null) {
      return null;
    }
    return asset.lastDownloaded().toDate();
  }

  private static Date calculateLastModified(final Asset asset) {
    Date lastModified = asset.blobUpdated()?.toDate()
    if (!lastModified) {
      lastModified = asset.blobCreated()?.toDate()
    }

    return lastModified
  }

  private static Map getExpandedAttributes(final Asset asset, final String format,
                                   @Nullable final Map<String, AssetXODescriptor> assetDescriptors)
  {
    Map expanded = [:]
    expanded["blobCreated"] = asset.blobCreated()?.toDate()

    Set<String> exposedAttributeKeys = assetDescriptors?.get(format)?.listExposedAttributeKeys()

    if (exposedAttributeKeys) {
      Map exposedAttributes = asset.attributes()?.child(format)?.backing()?.subMap(exposedAttributeKeys)
      if (exposedAttributes) {
        expanded.put(format, exposedAttributes)
      }
    }

    return expanded
  }
}
