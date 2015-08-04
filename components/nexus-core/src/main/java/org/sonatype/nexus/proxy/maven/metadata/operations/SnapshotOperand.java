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
package org.sonatype.nexus.proxy.maven.metadata.operations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.sonatype.nexus.proxy.maven.metadata.operations.ModelVersionUtility.Version;

import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;

/**
 * Snapshot storage
 *
 * @author Oleg Gusakov
 * @version $Id: SnapshotOperand.java 726701 2008-12-15 14:31:34Z hboutemy $
 */
public class SnapshotOperand
    extends AbstractOperand
{
  private final String timestamp;

  private final Snapshot snapshot;

  private final List<SnapshotVersion> snapshotVersions;

  public SnapshotOperand(final Version originModelVersion, final String timestamp, final Snapshot data,
                         final SnapshotVersion... snapshotVersions)
  {
    this(originModelVersion, timestamp, data, Arrays.asList(snapshotVersions));
  }

  public SnapshotOperand(final Version originModelVersion, final String timestamp, final Snapshot data,
                         final List<SnapshotVersion> snapshotVersions)
  {
    super(originModelVersion);

    this.timestamp = timestamp;
    this.snapshot = data;
    this.snapshotVersions = new ArrayList<SnapshotVersion>();

    if (snapshotVersions != null) {
      this.snapshotVersions.addAll(snapshotVersions);
    }
  }

  public String getTimestamp() {
    return timestamp;
  }

  public Snapshot getSnapshot() {
    return snapshot;
  }

  public List<SnapshotVersion> getSnapshotVersions() {
    //TODO: should this be unmodifiable list? I think yes
    return snapshotVersions;
  }
}
