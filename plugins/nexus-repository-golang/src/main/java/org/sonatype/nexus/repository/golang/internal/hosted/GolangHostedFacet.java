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
package org.sonatype.nexus.repository.golang.internal.hosted;

import java.io.IOException;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.Facet.Exposed;
import org.sonatype.nexus.repository.golang.AssetKind;
import org.sonatype.nexus.repository.golang.internal.metadata.GolangAttributes;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;

/**
 * @since 3.17
 */
@Exposed
public interface GolangHostedFacet
    extends Facet
{
  Content getPackage(final String path);

  Content getMod(final String path);

  Content getInfo(final String path, final GolangAttributes golangAttributes);

  Content getList(final String module);

  void upload(final String path, final GolangAttributes golangAttributes, final Payload payload, final AssetKind assetKind)
      throws IOException;
}
