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

import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.types.ProxyType

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.transform.builder.Builder

/**
 * Repository transfer object for REST APIs.
 *
 * @since 3.9
 */
@CompileStatic
@Builder
@ToString(includePackage = false, includeNames = true)
@EqualsAndHashCode
class RepositoryXO
{
  String name

  String format

  String type

  String url

  /**
   * @since 3.17
   */
  Map<String, Object> attributes

  static RepositoryXO fromRepository(final Repository repository) {
    return builder()
        .name(repository.getName())
        .format(repository.getFormat().getValue())
        .type(repository.getType().getValue())
        .url(repository.getUrl())
        .attributes(attributes(repository))
        .build()
  }

  private static Map<String, Object> attributes(Repository repository) {
    if (repository.getType() instanceof ProxyType) {
      return [
          proxy: [
              remoteUrl: repository.getConfiguration().attributes('proxy')?.get('remoteUrl', String)
          ]
      ] as Map<String, Object>
    }
    return [:]
  }
}
