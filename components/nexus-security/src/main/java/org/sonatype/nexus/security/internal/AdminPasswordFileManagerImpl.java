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
package org.sonatype.nexus.security.internal;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.security.config.AdminPasswordFileManager;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.next
 */
@Named
@Singleton
public class AdminPasswordFileManagerImpl
  extends ComponentSupport
  implements AdminPasswordFileManager
{
  private static final String FILENAME = "admin.password";

  public final ApplicationDirectories applicationDirectories;

  private final File adminPasswordFile;

  @Inject
  public AdminPasswordFileManagerImpl(final ApplicationDirectories applicationDirectories) {
    this.applicationDirectories = checkNotNull(applicationDirectories);
    adminPasswordFile = new File(applicationDirectories.getWorkDirectory(), FILENAME);
  }

  @Override
  public boolean writeFile(String password) throws IOException {
    File workdir = applicationDirectories.getWorkDirectory();
    if (!workdir.isDirectory() && !workdir.mkdirs()) {
      log.error("Failed to create work directory {}", workdir);
      return false;
    }

    if (adminPasswordFile.createNewFile()) {
      adminPasswordFile.setReadable(true, true);
    }

    try {
      log.info("Writing admin user temporary password to {}", adminPasswordFile.toString());
      Files.write(adminPasswordFile.toPath(), password.getBytes(StandardCharsets.UTF_8));
    }
    catch (Exception e) {
      log.error("Failed to write temporary password to disk", e);
      return false;
    }

    return true;
  }

  @Override
  public boolean exists() {
    return adminPasswordFile.exists();
  }

  @Override
  public String getPath() {
    return adminPasswordFile.getAbsolutePath();
  }

  @Override
  public String readFile() throws IOException {
    if (adminPasswordFile.exists()) {
      return new String(Files.readAllBytes(adminPasswordFile.toPath()), StandardCharsets.UTF_8);
    }

    return null;
  }

  @Override
  public void removeFile() {
    if (adminPasswordFile.exists() && !adminPasswordFile.delete()) {
      log.error("Failed to delete admin.password file {}", adminPasswordFile);
    }
  }
}
