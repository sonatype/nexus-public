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
import java.io.IOException;

import org.sonatype.configuration.Configuration;

/**
 * A component involved only if old security configuration is found. It will fetch the old configuration, transform it
 * to current Configuration model and return it. Nothing else.
 *
 * @author cstamas
 */
public interface ConfigurationUpgrader<E extends Configuration>
{
  /**
   * Tries to load an old configuration from file and will try to upgrade it to current model.
   */
  public E loadOldConfiguration(File file)
      throws IOException, ConfigurationIsCorruptedException, UnsupportedConfigurationVersionException;
}
