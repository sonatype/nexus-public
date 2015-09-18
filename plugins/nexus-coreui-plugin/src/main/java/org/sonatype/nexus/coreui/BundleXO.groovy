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
package org.sonatype.nexus.coreui

import org.hibernate.validator.constraints.NotBlank
import org.hibernate.validator.constraints.Range

/**
 * OSGI bundle.
 *
 * @since 3.0
 */
class BundleXO
{
  @Range
  long id

  @NotBlank
  String state

  @NotBlank
  String name

  @NotBlank
  String symbolicName

  @NotBlank
  String location

  @NotBlank
  String version

  @Range
  int startLevel

  boolean fragment

  // TODO: expose as DateTime?
  long lastModified

  /**
   * Fragment bundle ids.
   */
  List<Long> fragments

  /**
   * Fragment-host bundle ids.
   */
  List<Long> fragmentHosts

  Map<String,String> headers
}
