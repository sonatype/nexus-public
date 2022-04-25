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
package org.sonatype.nexus.npm.metadata.export;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NpmPackageRootExporter
{
  private static final Logger log = LoggerFactory.getLogger(NpmPackageRootExporter.class);

  public static void export(String npmPackageId, String packageRootJson, String exportDirectory)
      throws IOException
  {
    NpmPackageId packageId = NpmPackageId.parse(npmPackageId);

    File npmDir = getNpmDir(exportDirectory, packageId);

    String fileName = getFileName(packageId);

    File packageRootMetadataJsonFile = new File(npmDir, fileName);
    log.info("Writing to {}", packageRootMetadataJsonFile);

    if (packageRootMetadataJsonFile.exists()) {
      log.info("Already exists. Rewriting {}", packageRootMetadataJsonFile);
    }

    try (FileWriter fileWriter = new FileWriter(packageRootMetadataJsonFile)) {
      fileWriter.write(packageRootJson);
    }
  }

  private static File getNpmDir(String exportDir, NpmPackageId packageId) {
    String scopePrefix = StringUtils.isEmpty(packageId.scope) ? "" : "@" + packageId.scope + "/";
    File dir = new File(exportDir, scopePrefix + packageId.name + "/-/");
    if (!dir.exists()) {
      log.info("Creating npm directory {}", dir);
      dir.mkdirs();
    }
    return dir;
  }

  private static String getFileName(NpmPackageId packageId) {
    return packageId.name + ".package.json";
  }

  private static class NpmPackageId
  {
    private final String scope;

    private final String name;

    private NpmPackageId(String scope, String name) {
      this.scope = scope;
      this.name = name;
    }

    public static NpmPackageId parse(final String npmPackageName) {
      if (StringUtils.isEmpty(npmPackageName)) {
        throw new IllegalArgumentException("Package name is required.");
      }

      String scope = null;
      String name = npmPackageName;
      int slashIndex = name.indexOf('/');
      if (npmPackageName.startsWith("@") && slashIndex > -1) {
        scope = name.substring(1, slashIndex);
        name = name.substring(slashIndex + 1);
      }
      return new NpmPackageId(scope, name);
    }
  }
}
