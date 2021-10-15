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

import javax.validation.constraints.Min
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Size

import org.sonatype.nexus.validation.group.Create

import groovy.transform.CompileStatic
import groovy.transform.ToString

import static org.sonatype.nexus.blobstore.BlobStoreSupport.MAX_NAME_LENGTH
import static org.sonatype.nexus.blobstore.BlobStoreSupport.MIN_NAME_LENGTH

/**
 * @since 3.0
 */
@CompileStatic
@ToString(includePackage = false, includeNames = true)
class BlobStoreXO
{
  @NotEmpty
  @UniqueBlobStoreName(groups = Create)
  @Size(min = MIN_NAME_LENGTH, max = MAX_NAME_LENGTH)
  String name

  @NotEmpty
  String type

  String isQuotaEnabled

  String quotaType

  @Min(0L)
  Long quotaLimit

  @NotEmpty
  Map<String, Map<String, Object>> attributes

  @Min(0L)
  long blobCount

  @Min(0L)
  long totalSize

  @Min(0L)
  long availableSpace

  @Min(0L)
  long repositoryUseCount

  boolean unlimited

  /**
   * @since 3.19
   */
  boolean unavailable

  @Min(0L)
  long blobStoreUseCount

  boolean inUse

  boolean convertable

  /**
   * @since 3.29
   */
  int taskUseCount

  /**
   * the name of the group to which this blob store belongs, or null if not in a group
   * @since 3.15
   */
  String groupName
}
