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

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.transform.builder.Builder

import static java.util.Collections.emptyMap

/**
 * Component transfer object for REST APIs.
 *
 * @since 3.8
 */
@CompileStatic
@Builder
@ToString(includePackage = false, includeNames = true)
@EqualsAndHashCode(includes = ['id'])
class DefaultComponentXO
  implements ComponentXO
{
  String id

  String group

  String name

  String version

  String repository

  String format

  List<AssetXO> assets

  /**
   * Provides extra attributes for the JSON payload. Implementers must use @JsonAnyGetter.
   * @since 3.8
   */
  @Override
  Map<String, Object> getExtraJsonAttributes() {
    return emptyMap()
  }
}
