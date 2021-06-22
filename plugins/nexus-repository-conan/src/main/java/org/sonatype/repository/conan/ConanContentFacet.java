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

package org.sonatype.repository.conan;

import java.util.Optional;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.repository.conan.internal.AssetKind;
import org.sonatype.repository.conan.internal.metadata.ConanCoords;

/**
 * @since 3.next
 */
@Facet.Exposed
public interface ConanContentFacet
    extends ContentFacet
{
  Optional<FluentAsset> getAsset(ConanCoords coords, AssetKind assetKind);

  FluentAsset putPackage(final TempBlob tempBlob, final ConanCoords coords, final AssetKind assetKind);

  FluentAsset putMetadata(final TempBlob tempBlob, final ConanCoords coords, final AssetKind assetKind);
}
