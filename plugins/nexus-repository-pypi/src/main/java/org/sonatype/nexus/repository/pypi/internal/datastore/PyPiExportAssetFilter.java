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
package org.sonatype.nexus.repository.pypi.internal.datastore;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.export.ExportAssetFilter;
import org.sonatype.nexus.repository.pypi.internal.PyPiExportAssetFilterSupport;
import org.sonatype.nexus.repository.pypi.PyPiFormat;

/**
 * Filter to exclude indexes from export, as they need to be regenerated on import
 *
 * @since 3.29
 */
@Singleton
@Named(PyPiFormat.NAME)
public class PyPiExportAssetFilter
    extends PyPiExportAssetFilterSupport
    implements ExportAssetFilter
{
  @Override
  public boolean shouldSkipAsset(final FluentAsset asset) {
    return shouldSkipAsset(asset.path(), asset.kind());
  }
}
