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
package org.sonatype.nexus.repository.rest.api

import javax.annotation.Nullable

import org.sonatype.nexus.repository.Repository

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.transform.builder.Builder

import static org.sonatype.nexus.repository.search.index.SearchConstants.ATTRIBUTES
import static org.sonatype.nexus.repository.search.index.SearchConstants.CHECKSUM
import static org.sonatype.nexus.repository.search.index.SearchConstants.ID
import static org.sonatype.nexus.repository.search.index.SearchConstants.NAME
import static org.sonatype.nexus.repository.search.index.SearchConstants.CONTENT_TYPE

/**
 * Asset transfer object for REST APIs.
 *
 * @since 3.3
 */
@CompileStatic
@Builder
@ToString(includePackage = false, includeNames = true)
@EqualsAndHashCode(includes = ['id'])
class AssetXO
{
  String downloadUrl

  String path

  String id

  String repository

  String format

  Map checksum

  String contentType

  Date lastModified

  @JsonIgnore
  Map attributes

  static AssetXO fromElasticSearchMap(final Map map, final Repository repository,
                                      @Nullable final Map<String, AssetXODescriptor> assetDescriptors)
  {
    String path = map.get(NAME)
    String id = map.get(ID)
    Map attributes = (Map) map.get(ATTRIBUTES, [:])
    Map checksum = (Map) attributes[CHECKSUM]
    String format = repository.format.value
    String contentType = map.get(CONTENT_TYPE)

    return builder()
        .path(path)
        .downloadUrl(repository.url + '/' + path)
        .id(new RepositoryItemIDXO(repository.name, id).value)
        .repository(repository.name)
        .checksum(checksum)
        .format(format)
        .contentType(contentType)
        .attributes(getExpandedAttributes(attributes, format, assetDescriptors))
        .lastModified(calculateLastModified(attributes))
        .build()
  }

  private static final Map getExpandedAttributes(final Map attributes, final String format,
                                     @Nullable final Map<String, AssetXODescriptor> assetDescriptors)
  {
    Set<String> exposedAttributeKeys = assetDescriptors?.get(format)?.listExposedAttributeKeys()
    Map expanded = [:]
    if (exposedAttributeKeys) {
      Map exposedAttributes = (attributes.get(format, [:]) as Map<String, Object>)?.subMap(exposedAttributeKeys)
      if (exposedAttributes) {
        expanded.put(format, exposedAttributes)
      }
    }

    return expanded
  }

  private static final Date calculateLastModified(final Map attributes) {
    String lastModifiedString = (attributes.getOrDefault("content", [:]) as Map).getOrDefault("last_modified", null)
    Date lastModified = null
    if (lastModifiedString != null) {
      try {
        lastModified = new Date(Long.parseLong(lastModifiedString.trim()))
      }
      catch (Exception ignored) {
        // Nothing we can do here for invalid data. It shouldn't happen but date parsing will blow out the results.
      }
    }
    return lastModified
  }

  @JsonAnyGetter
  Map<String, Object> getAttributes() {
    return attributes
  }
}
