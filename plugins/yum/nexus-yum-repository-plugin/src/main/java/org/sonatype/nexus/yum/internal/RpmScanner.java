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
package org.sonatype.nexus.yum.internal;

import java.io.File;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.sisu.resource.scanner.Scanner;
import org.sonatype.sisu.resource.scanner.helper.ListenerSupport;

import com.google.common.collect.Sets;
import org.apache.commons.io.FilenameUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.io.File.separator;

/**
 * @since yum 3.0
 */
@Named
@Singleton
public class RpmScanner
{

  private final Scanner scanner;

  @Inject
  public RpmScanner(final @Named("serial") Scanner scanner) {
    this.scanner = checkNotNull(scanner);
  }

  public Set<File> scan(final File baseDir) {
    final Set<File> rpms = Sets.newHashSet();

    scanner.scan(baseDir, new ListenerSupport()
    {
      @Override
      public void onFile(final File file) {
        if ("rpm".equalsIgnoreCase(FilenameUtils.getExtension(file.getName()))
            && !getRelativePath(baseDir, file).startsWith(".")) {
          rpms.add(file);
        }
      }
    });

    return rpms;
  }

  static String getRelativePath(final File baseDir, final File file) {
    String baseDirPath = baseDir.getAbsolutePath() + (baseDir.isDirectory() ? separator : "");
    String filePath = file.getAbsolutePath() + (file.isDirectory() ? separator : "");
    if (filePath.startsWith(baseDirPath)) {
      filePath = filePath.substring(baseDirPath.length());
    }
    return filePath;
  }

}
