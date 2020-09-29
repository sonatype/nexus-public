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
package org.sonatype.nexus.repository.r.internal;

/**
 * R format specific attributes. This is far from an exhaustive list but contains the most important ones.
 *
 * @since 3.28
 */
public final class RAttributes
{

  public static final String P_PACKAGE = "Package";

  public static final String P_VERSION = "Version";

  public static final String P_DEPENDS = "Depends";

  public static final String P_IMPORTS = "Imports";

  public static final String P_SUGGESTS = "Suggests";

  public static final String P_LINKINGTO = "LinkingTo";

  public static final String P_LICENSE = "License";

  public static final String P_NEEDS_COMPILATION = "NeedsCompilation";

  private RAttributes() {
    // empty
  }
}
