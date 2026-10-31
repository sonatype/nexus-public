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
package org.sonatype.nexus.common.app.softdelete;

import java.time.OffsetDateTime;

/**
 * Raw data for re-recording a still-soft-deleted child asset in the trash index after its parent component
 * was revived (see {@link TrashIndexRecorder#resurfaceSoftDeletedChildAsset}). External ids are passed
 * un-encoded; the {@code TrashIndexRecorder} implementation owns the identifier encoding so the content
 * layer does not need a compile-time dependency on the soft-delete plugin's encoder.
 */
public record SoftDeletedAssetRef(
    String repositoryName,
    Integer repositoryId,
    String format,
    String componentExternalId,
    String assetExternalId,
    String path,
    String kind,
    OffsetDateTime deletedOn,
    String deletedBy)
{
}
