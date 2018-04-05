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
import org.sonatype.nexus.repository.upload.UploadDefinition

/**
 * Upload definition transfer object for rest api
 *
 * @since 3.10
 */
@CompileStatic
@Builder
@ToString(includePackage = false, includeNames = true)
@EqualsAndHashCode(includes = ['format'])
class UploadDefinitionXO
{
  String format

  boolean multipleUpload

  List<UploadFieldDefinitionXO> componentFields

  List<UploadFieldDefinitionXO> assetFields

  static UploadDefinitionXO from(final UploadDefinition uploadDefinition) {
    return  builder()
        .format(uploadDefinition.format)
        .multipleUpload(uploadDefinition.multipleUpload)
        .componentFields(uploadDefinition.componentFields.collect { UploadFieldDefinitionXO.from(it) })
        .assetFields(uploadDefinition.assetFields.collect { UploadFieldDefinitionXO.from(it) })
        .build()
  }
}
