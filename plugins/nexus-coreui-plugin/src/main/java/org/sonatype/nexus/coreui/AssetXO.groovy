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
package org.sonatype.nexus.coreui

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.hibernate.validator.constraints.NotEmpty

/**
 * Asset exchange object.
 *
 * @since 3.0
 */
@ToString(includePackage = false, includeNames = true)
@EqualsAndHashCode(includes = ["id"])
class AssetXO {
  @NotEmpty
  String id

  @NotEmpty
  String name

  @NotEmpty
  String format

  @NotEmpty
  String contentType

  @NotEmpty
  long size

  @NotEmpty
  String repositoryName

  @NotEmpty
  String containingRepositoryName

  Date blobCreated

  Date blobUpdated

  Date lastDownloaded

  @NotEmpty
  String blobRef

  String componentId

  String createdBy

  String createdByIp

  @NotEmpty
  Map<String, Map<String, Object>> attributes
}
