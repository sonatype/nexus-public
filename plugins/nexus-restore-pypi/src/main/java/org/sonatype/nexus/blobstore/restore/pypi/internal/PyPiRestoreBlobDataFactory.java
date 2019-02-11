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
package org.sonatype.nexus.blobstore.restore.pypi.internal;

import javax.annotation.Nonnull;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.restore.RestoreBlobData;
import org.sonatype.nexus.repository.pypi.PyPiRestoreUtil;

import static com.google.common.base.Preconditions.checkState;
import static org.eclipse.aether.util.StringUtils.isEmpty;

/**
 * @since 3.next
 */
@Singleton
public class PyPiRestoreBlobDataFactory
{
  public PyPiRestoreBlobData create(@Nonnull final RestoreBlobData data) {
    checkState(!isEmpty(data.getBlobName()), "Blob name cannot be empty");

    String version = PyPiRestoreUtil.extractVersionFromPath(data.getBlobName());
    return new PyPiRestoreBlobData(data, version);
  }
}
