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
package org.sonatype.nexus.security.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import org.sonatype.nexus.common.text.Strings2;

import static com.google.common.base.Preconditions.checkNotNull;
import org.springframework.stereotype.Component;

@Component
public class AdminPasswordSourceImpl
    implements AdminPasswordSource
{
  public static final String DEFAULT_PASSWORD = "admin123";

  private final AdminPasswordFileManager adminPasswordFileManager;

  @Autowired
  public AdminPasswordSourceImpl(
      final AdminPasswordFileManager adminPasswordFileManager)
  {
    this.adminPasswordFileManager = checkNotNull(adminPasswordFileManager);
  }

  public String getPassword(final boolean randomPassword) {
    try {
      String savedPassword = adminPasswordFileManager.readFile();

      if (!Strings2.isBlank(savedPassword)) {
        return savedPassword;
      }
      else if (!randomPassword) {
        return DEFAULT_PASSWORD;
      }

      savedPassword = UUID.randomUUID().toString();

      // failure writing file to disk, revert to using default
      if (!adminPasswordFileManager.writeFile(savedPassword)) {
        savedPassword = DEFAULT_PASSWORD;
      }
      return savedPassword;
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
