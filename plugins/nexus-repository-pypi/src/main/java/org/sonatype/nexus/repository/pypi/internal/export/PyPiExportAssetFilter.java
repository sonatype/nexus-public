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
package org.sonatype.nexus.repository.pypi.internal.export;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.filter.export.ExportAssetFilter;
import org.sonatype.nexus.repository.pypi.internal.AssetKind;
import org.sonatype.nexus.repository.pypi.internal.PyPiFormat;
import org.sonatype.nexus.repository.storage.Asset;

import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.TASK_LOG_ONLY;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;

/**
 * Filter to exclude indexes from export, as they need to be regenerated on import
 *
 * @since 3.26
 */
@Singleton
@Named(PyPiFormat.NAME)
public class PyPiExportAssetFilter
    extends ComponentSupport
    implements ExportAssetFilter
{
  @Override
  public boolean shouldSkipAsset(final Asset asset) {
    String assetKindName = null;
    try {
      assetKindName = asset.formatAttributes().get(P_ASSET_KIND).toString();
      AssetKind assetKind = AssetKind.valueOf(assetKindName);
      switch (assetKind) {
        case INDEX:
        case ROOT_INDEX:
        case SEARCH:
          log.trace("PyPI asset {} is NOT allowed for processing, will skip.", asset.name());
          return true;
      }
    }
    catch (Exception e) {
      log.error(TASK_LOG_ONLY, "PyPI asset {} has invalid assetkind '{}'. Will skip for export.", asset.name(),
          assetKindName, log.isDebugEnabled() ? e : "");
      return true;
    }
    log.trace("PyPI asset {} is allowed for processing.", asset.name());
    return false;
  }
}
