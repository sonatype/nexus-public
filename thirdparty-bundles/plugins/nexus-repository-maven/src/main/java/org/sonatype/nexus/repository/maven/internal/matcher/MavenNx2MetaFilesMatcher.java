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
package org.sonatype.nexus.repository.maven.internal.matcher;

import org.sonatype.nexus.repository.maven.MavenPathParser;

/**
 * Matcher that matches anything in .meta folder (legacy paths from nx2)
 *
 * @since 3.3
 */
public class MavenNx2MetaFilesMatcher
    extends MavenMatcherSupport
{
  private static final String META_FILES_REQ_PATH = "/.meta/";

  public MavenNx2MetaFilesMatcher(final MavenPathParser mavenPathParser) {
    super(mavenPathParser, (String path) -> path != null && path.startsWith(META_FILES_REQ_PATH));
  }
}
