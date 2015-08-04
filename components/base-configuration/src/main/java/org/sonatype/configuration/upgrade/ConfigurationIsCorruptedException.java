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
package org.sonatype.configuration.upgrade;

import java.io.File;

import org.sonatype.configuration.ConfigurationException;

/**
 * Thrown when the configuration file is corrupt and cannot be loaded neither upgraded. It has wrong syntax or is
 * unreadable.
 *
 * @author cstamas
 */
public class ConfigurationIsCorruptedException
    extends ConfigurationException
{
  private static final long serialVersionUID = 5592204171297423008L;

  public ConfigurationIsCorruptedException(File file) {
    this(file.getAbsolutePath());
  }

  public ConfigurationIsCorruptedException(String filePath) {
    this(filePath, null);
  }

  public ConfigurationIsCorruptedException(String filePath, Throwable t) {
    super("Could not read or parse security configuration file on path " + filePath + "! It may be corrupted.", t);
  }

}
