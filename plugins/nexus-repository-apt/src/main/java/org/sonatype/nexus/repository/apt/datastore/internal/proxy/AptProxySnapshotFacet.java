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
package org.sonatype.nexus.repository.apt.datastore.internal.proxy;

import java.io.IOException;

import javax.annotation.Nullable;
import javax.inject.Named;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.Facet.Exposed;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.apt.internal.snapshot.AptSnapshotFacet;
import org.sonatype.nexus.repository.apt.internal.snapshot.SnapshotComponentSelector;
import org.sonatype.nexus.repository.view.Content;

/**
 * @since 3.next
 */
@Facet.Exposed
@Named
// TODO Dummy implementation of AptSnapshotFacet
// TODO implement the facet in the separate task
// https://issues.sonatype.org/browse/NEXUS-27463
public class AptProxySnapshotFacet
    extends FacetSupport
    implements AptSnapshotFacet
{

  @Override
  public boolean isSnapshotableFile(final String path) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void createSnapshot(final String id, final SnapshotComponentSelector spec) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public Content getSnapshotFile(final String id, final String path) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteSnapshot(final String id) throws IOException {
    throw new UnsupportedOperationException();
  }
}
