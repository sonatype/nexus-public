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
package org.sonatype.nexus.repository.apt;

import java.io.IOException;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.apt.internal.debian.PackageInfo;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;

/**
 * @since 3.17
 */
@Facet.Exposed
public interface AptFacet
    extends Facet
{
  @Nullable
  Content get(final String path) throws IOException;

  Content put(final String path, final Payload payload) throws IOException;

  Content put(final String path, final Payload payload, @Nullable final PackageInfo packageInfo) throws IOException;

  boolean delete(final String path) throws IOException;

  Asset findOrCreateDebAsset(final StorageTx tx, final String path, final PackageInfo packageInfo);

  Asset findOrCreateMetadataAsset(final StorageTx tx, final String path);

  boolean isFlat();

  String getDistribution();
}
