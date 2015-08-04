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

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.Versioning;

/**
 * merge Metadata.
 *
 * @author Oleg Gusakov
 * @version $Id: MergeOperation.java 726701 2008-12-15 14:31:34Z hboutemy $
 */
public class MergeOperation
    implements MetadataOperation
{

  private Metadata sourceMetadata;

  /**
   * @throws MetadataException
   */
  public MergeOperation(MetadataOperand data)
      throws MetadataException
  {
    setOperand(data);
  }

  /**
   * merge the supplied operand Metadata into this metadata
   */
  public boolean perform(Metadata targetMetadata)
      throws MetadataException
  {
    boolean changed = false;

    if (sourceMetadata == null || targetMetadata == null) {
      return false;
    }

    List<Plugin> plugins = sourceMetadata.getPlugins();
    for (Plugin plugin : plugins) {
      boolean found = false;

      List<Plugin> targetPlugins = targetMetadata.getPlugins();
      for (Plugin preExisting : targetPlugins) {
        if (preExisting.getPrefix().equals(plugin.getPrefix())) {
          found = true;
        }
      }

      if (!found) {
        Plugin mappedPlugin = new Plugin();

        mappedPlugin.setArtifactId(plugin.getArtifactId());
        mappedPlugin.setPrefix(plugin.getPrefix());
        mappedPlugin.setName(plugin.getName());

        targetMetadata.addPlugin(mappedPlugin);

        changed = true;
      }
    }

    Versioning sourceVersioning = sourceMetadata.getVersioning();
    if (sourceVersioning != null) {
      Versioning targetVersioning = targetMetadata.getVersioning();
      if (targetVersioning == null) {
        targetVersioning = new Versioning();
        targetMetadata.setVersioning(targetVersioning);
        changed = true;
      }

      List<String> versions = sourceVersioning.getVersions();
      for (String version : versions) {
        if (!targetVersioning.getVersions().contains(version)) {
          changed = true;
          targetVersioning.getVersions().add(version);
        }
      }

      if ("null".equals(sourceVersioning.getLastUpdated())) {
        sourceVersioning.setLastUpdated(null);
      }

      if ("null".equals(targetVersioning.getLastUpdated())) {
        targetVersioning.setLastUpdated(null);
      }

      if (sourceVersioning.getLastUpdated() == null || sourceVersioning.getLastUpdated().length() == 0) {
        // this should only be for historical reasons - we assume local is newer
        sourceVersioning.setLastUpdated(targetVersioning.getLastUpdated());
      }

      if (targetVersioning.getLastUpdated() == null || targetVersioning.getLastUpdated().length() == 0
          || sourceVersioning.getLastUpdated().compareTo(targetVersioning.getLastUpdated()) >= 0) {
        changed = true;
        targetVersioning.setLastUpdated(sourceVersioning.getLastUpdated());

        if (sourceVersioning.getRelease() != null) {
          changed = true;
          targetVersioning.setRelease(sourceVersioning.getRelease());
        }
        if (sourceVersioning.getLatest() != null) {
          changed = true;
          targetVersioning.setLatest(sourceVersioning.getLatest());
        }

        Snapshot s = targetVersioning.getSnapshot();
        Snapshot snapshot = sourceVersioning.getSnapshot();
        if (snapshot != null) {
          if (s == null) {
            s = new Snapshot();
            targetVersioning.setSnapshot(s);
            changed = true;
          }

          // overwrite
          if (s.getTimestamp() == null ? snapshot.getTimestamp() != null
              : !s.getTimestamp().equals(snapshot.getTimestamp())) {
            s.setTimestamp(snapshot.getTimestamp());
            changed = true;
          }
          if (s.getBuildNumber() != snapshot.getBuildNumber()) {
            s.setBuildNumber(snapshot.getBuildNumber());
            changed = true;
          }
          if (s.isLocalCopy() != snapshot.isLocalCopy()) {
            s.setLocalCopy(snapshot.isLocalCopy());
            changed = true;
          }
        }
      }
    }
    return changed;
  }

  public void setOperand(AbstractOperand data)
      throws MetadataException
  {
    if (data == null || !(data instanceof MetadataOperand)) {
      throw new MetadataException("Operand is not correct: expected MetadataOperand, but got "
          + (data == null ? "null" : data.getClass().getName()));

    }

    sourceMetadata = ((MetadataOperand) data).getOperand();
  }
}
