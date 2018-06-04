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

import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPathParser;

import static org.sonatype.nexus.repository.maven.internal.Constants.INDEX_FILE_BASE_PATH;

/**
 * Matcher that matches for published Maven index only, and sets {@link MavenPath} in context attributes.
 *
 * @since 3.0
 */
public class MavenIndexMatcher
    extends MavenMatcherSupport
{
  public MavenIndexMatcher(final MavenPathParser mavenPathParser) {
    super(mavenPathParser, (String path) -> path != null && path.startsWith("/" + INDEX_FILE_BASE_PATH));
  }
}
