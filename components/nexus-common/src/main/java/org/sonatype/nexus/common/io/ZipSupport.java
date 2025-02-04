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
package org.sonatype.nexus.common.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZipSupport
{
  private static final Logger log = LoggerFactory.getLogger(ZipSupport.class);

  public static void zipFiles(Path path, List<String> filesToZip, String zipFileName) throws IOException {
    byte[] buffer = new byte[1024];
    try (FileOutputStream fos = new FileOutputStream(zipFileName);
        ZipOutputStream zos = new ZipOutputStream(fos)) {
      for (String fileName : filesToZip) {
        File file = path.resolve(fileName).toFile();
        if (!file.exists()) {
          log.info("The file '" + fileName + "' does not exist in folder '" + path + "'.");
          continue;
        }
        FileInputStream fis = new FileInputStream(file);
        zos.putNextEntry(new ZipEntry(fileName));

        int length;
        while ((length = fis.read(buffer)) > 0) {
          zos.write(buffer, 0, length);
        }
        zos.closeEntry();
        fis.close();
      }
    }
  }
}
