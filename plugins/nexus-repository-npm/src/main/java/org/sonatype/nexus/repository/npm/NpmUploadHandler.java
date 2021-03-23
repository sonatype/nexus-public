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
package org.sonatype.nexus.repository.npm;

import java.util.Map;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.npm.internal.NpmAttributes;
import org.sonatype.nexus.repository.npm.internal.NpmFormat;
import org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils;
import org.sonatype.nexus.repository.npm.internal.NpmPackageId;
import org.sonatype.nexus.repository.upload.UploadHandler;

import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;

public interface NpmUploadHandler
    extends UploadHandler
{
  default Map<String, Object> ensureNpmPermitted(
      final Repository repository,
      final Map<String, Object> packageJson)
  {
    final String name = (String) packageJson.get(NpmAttributes.P_NAME);
    final String version = (String) packageJson.get(NpmAttributes.P_VERSION);
    final String repositoryPath = NpmMetadataUtils.createRepositoryPath(name, version);
    final Map<String, String> coordinates = toCoordinates(packageJson);

    ensurePermitted(repository.getName(), NpmFormat.NAME, repositoryPath, coordinates);
    return packageJson;
  }

  default Map<String, String> toCoordinates(final Map<String, Object> packageJson) {
    NpmPackageId packageId = NpmPackageId.parse((String) checkNotNull(packageJson.get(NpmAttributes.P_NAME)));
    String version = (String) checkNotNull(packageJson.get(NpmAttributes.P_VERSION));

    if (packageId.scope() != null) {
      return ImmutableMap.of("packageScope", packageId.scope(), "packageName", packageId.name(),
          NpmAttributes.P_VERSION, version);
    }
    else {
      return ImmutableMap.of("packageName", packageId.name(), NpmAttributes.P_VERSION, version);
    }
  }
}
