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
package org.sonatype.nexus.blobstore.s3.internal.ui

import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.nexus.blobstore.s3.internal.AmazonS3Factory
import org.sonatype.nexus.extdirect.DirectComponent
import org.sonatype.nexus.extdirect.DirectComponentSupport

import com.amazonaws.services.s3.model.Region
import com.amazonaws.services.s3.AmazonS3Client
import com.codahale.metrics.annotation.ExceptionMetered
import com.codahale.metrics.annotation.Timed
import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import org.apache.shiro.authz.annotation.RequiresPermissions

/**
 * S3 {@link DirectComponent}.
 *
 * @since 3.12
 */
@Named
@Singleton
@DirectAction(action = 's3_S3')
class S3Component
    extends DirectComponentSupport
{
  private static final String DEFAULT_LABEL = "Default"

  /**
   * S3 regions
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:settings:read')
  List<S3RegionXO> regions() {
    [new S3RegionXO(order: 0, id: AmazonS3Factory.DEFAULT, name: DEFAULT_LABEL)] +
        Region.values().toList().withIndex(1).collect { item, index ->
          new S3RegionXO(order: index, id: item.toAWSRegion().name, name: item.toAWSRegion().name)
        }
  }

  /**
   * S3 signer types
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:settings:read')
  List<S3RegionXO> signertypes() {
    [
        new S3SignerTypeXO(order: 0, id: AmazonS3Factory.DEFAULT, name: DEFAULT_LABEL),
        new S3SignerTypeXO(order: 1, id: AmazonS3Client.S3_SIGNER, name: AmazonS3Client.S3_SIGNER),
        new S3SignerTypeXO(order: 2, id: AmazonS3Client.S3_V4_SIGNER, name: AmazonS3Client.S3_V4_SIGNER)
    ]
  }
}
