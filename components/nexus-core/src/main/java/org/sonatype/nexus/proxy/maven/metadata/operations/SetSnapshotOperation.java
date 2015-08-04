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

import java.util.List;

import org.sonatype.nexus.proxy.maven.metadata.operations.ModelVersionUtility.Version;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.Versioning;

/**
 * adds new snapshot to metadata
 *
 * @author Oleg Gusakov
 * @version $Id: SetSnapshotOperation.java 743040 2009-02-10 18:20:26Z ogusakov $
 */
public class SetSnapshotOperation
    implements MetadataOperation
{

  private SnapshotOperand operand;

  private final VersionComparator versionComparator;

  /**
   * @throws MetadataException
   */
  public SetSnapshotOperation(SnapshotOperand data)
      throws MetadataException
  {
    this.versionComparator = new VersionComparator();
    setOperand(data);
  }

  public void setOperand(AbstractOperand data)
      throws MetadataException
  {
    if (data == null || !(data instanceof SnapshotOperand)) {
      throw new MetadataException("Operand is not correct: expected SnapshotOperand, but got "
          + (data == null ? "null" : data.getClass().getName()));
    }
    this.operand = (SnapshotOperand) data;
  }

  /**
   * add/replace snapshot to the in-memory metadata instance
   */
  public boolean perform(Metadata metadata)
      throws MetadataException
  {
    if (metadata == null) {
      return false;
    }

    Versioning vs = metadata.getVersioning();

    if (vs == null) {
      vs = new Versioning();

      metadata.setVersioning(vs);
    }

    return updateSnapshot(metadata);
  }

  private boolean updateSnapshot(Metadata metadata)
      throws MetadataException
  {
    final Versioning vs = metadata.getVersioning();

    if (operand.getSnapshot() != null) {
      vs.setSnapshot(operand.getSnapshot());
    }

    List<SnapshotVersion> extras = operand.getSnapshotVersions();
    List<SnapshotVersion> currents = vs.getSnapshotVersions();

    if (extras != null && extras.size() > 0) {
      // fix/upgrade the version
      ModelVersionUtility.setModelVersion(metadata, ModelVersionUtility.LATEST_MODEL_VERSION);

      for (SnapshotVersion extra : extras) {
        SnapshotVersion current = MetadataUtil.searchForEquivalent(extra, currents);
        if (current == null) {
          currents.add(extra);
        }
        else {
          if (versionComparator.compare(current.getVersion(), extra.getVersion()) < 0) {
            currents.remove(current);
            currents.add(extra);
          }
        }
      }
    }
    else if (Version.V100 == operand.getOriginModelVersion() && operand.getSnapshot() != null) {
      for (SnapshotVersion current : currents) {
        current.setUpdated(operand.getTimestamp());
      }
    }

    vs.setLastUpdated(operand.getTimestamp());

    return true;
  }

}
