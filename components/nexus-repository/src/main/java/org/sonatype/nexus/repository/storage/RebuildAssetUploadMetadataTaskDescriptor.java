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
package org.sonatype.nexus.repository.storage;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

/**
 * Task descriptor for {@link RebuildAssetUploadMetadataTask}.
 *
 * @since 3.6
 */
@AvailabilityVersion(from = "1.0")
@Named
@Singleton
public class RebuildAssetUploadMetadataTaskDescriptor
    extends TaskDescriptorSupport
{
  public static final String TYPE_ID = "rebuild.asset.uploadMetadata";

  @Inject
  public RebuildAssetUploadMetadataTaskDescriptor(
      final NodeAccess nodeAccess,
      final RebuildAssetUploadMetadataConfiguration configuration)
  {
    super(
        TYPE_ID,
        RebuildAssetUploadMetadataTask.class,
        "Repair - Reconcile date metadata from blob store",
        configuration.isEnabled() ? VISIBLE : NOT_VISIBLE,
        configuration.isEnabled() ? EXPOSED : NOT_EXPOSED,
        nodeAccess.isClustered() ? newLimitNodeFormField() : null);
  }
}
