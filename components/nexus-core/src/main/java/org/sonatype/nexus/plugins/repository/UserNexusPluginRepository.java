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
package org.sonatype.nexus.plugins.repository;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.util.file.DirSupport;

import com.google.common.base.Throwables;

import static com.google.common.base.Preconditions.checkNotNull;

@Named(UserNexusPluginRepository.ID)
@Singleton
@Deprecated
final class UserNexusPluginRepository
    extends AbstractFileNexusPluginRepository
{
  // ----------------------------------------------------------------------
  // Constants
  // ----------------------------------------------------------------------

  static final String ID = "user";

  // ----------------------------------------------------------------------
  // Implementation fields
  // ----------------------------------------------------------------------

  private final File userPluginsFolder;

  @Inject
  public UserNexusPluginRepository(final @Named("${nexus-work}/plugin-repository") File userPluginsFolder) {
    this.userPluginsFolder = checkNotNull(userPluginsFolder);
    try {
      DirSupport.mkdir(userPluginsFolder.toPath());
    }
    catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  // ----------------------------------------------------------------------
  // Public methods
  // ----------------------------------------------------------------------

  public String getId() {
    return ID;
  }

  public int getPriority() {
    return 100;
  }

  // ----------------------------------------------------------------------
  // Customized methods
  // ----------------------------------------------------------------------

  @Override
  protected File getNexusPluginsDirectory() {
    return userPluginsFolder;
  }
}
