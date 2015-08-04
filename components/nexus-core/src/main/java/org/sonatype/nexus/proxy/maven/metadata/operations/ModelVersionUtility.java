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

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;

/**
 * Handling model version of Maven repository metadata, with some rudimentary "version detection".
 *
 * @author cstamas
 */
public class ModelVersionUtility
{
  public static final Version LATEST_MODEL_VERSION = Version.values()[Version.values().length - 1];

  public enum Version
  {
    V100,

    V110;
  }

  public static Version getModelVersion(final Metadata metadata) {
    if ("1.1.0".equals(metadata.getModelVersion())) {
      return Version.V110;
    }
    else {
      return Version.V100;
    }
  }

  public static void setModelVersion(final Metadata metadata, final Version version) {
    switch (version) {
      case V100:
        metadata.setModelVersion(null);
        Versioning versioning = metadata.getVersioning();
        if (versioning != null) {
          versioning.setSnapshotVersions(null);
        }
        break;

      case V110:
        metadata.setModelVersion("1.1.0");
        break;
    }
  }
}
