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
package org.sonatype.repository.helm.internal.content;

import java.io.IOException;
import java.util.Optional;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.repository.helm.HelmAttributes;
import org.sonatype.repository.helm.internal.AssetKind;

/**
 * @since 3.next
 */
@Facet.Exposed
public interface HelmContentFacet
    extends ContentFacet
{
  Iterable<FluentAsset> browseAssets();

  Optional<Content> getAsset(String path);

  Content putIndex(String path, Content content, AssetKind assetKind);

  Content putComponent(String path, Content content, AssetKind assetKind) throws IOException;

  Content putComponent(String path, TempBlob tempBlob, HelmAttributes helmAttrs, Content content, AssetKind assetKind);

  TempBlob getTempBlob(Payload payload);

  boolean delete(String path);
}
