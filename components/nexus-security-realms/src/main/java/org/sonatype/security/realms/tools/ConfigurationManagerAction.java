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
package org.sonatype.security.realms.tools;

/**
 * Defines an interface for a ConfigurationManager action. These actions
 * are intended to encapsulate higher-level operations that require multiple calls
 * to the ConfigurationManager to complete. These actions provide a way for users to use the ConfigurationManager
 * in a thread-safe manner. Must be used in conjunction with an implementation of ConfigurationManager that supports
 * the runRead and runWrite methods
 *
 * @author Steve Carlucci
 * @since 3.1
 */
public interface ConfigurationManagerAction
{
  /**
   * Run the action
   */
  void run() throws Exception;
}
