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
package org.sonatype.nexus.log;

import java.io.InputStream;

/**
 * A component contract that wants to provide extra logging configuration, participate in configuration of logging in
 * Nexus.
 *
 * @author adreghiciu
 */
public interface LogConfigurationParticipant
{
  String getName();

  InputStream getConfiguration();

  /**
   * Marker interface to be implemented by {@link LogConfigurationParticipant} instances that provide configurations
   * which should not be tampered with, changed by users. These participant configurations will be written out
   * (probably overwriting existing file) always, at every boot.
   *
   * @author cstamas
   * @since 2.2
   */
  public interface NonEditable
  {

  }
}
