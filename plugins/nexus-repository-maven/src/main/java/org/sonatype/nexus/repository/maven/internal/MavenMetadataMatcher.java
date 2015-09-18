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
package org.sonatype.nexus.repository.maven.internal;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPathParser;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Matcher;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Matcher that matches for Maven repository metadata only, and sets {@link MavenPath} in context attributes.
 *
 * @since 3.0
 */
public class MavenMetadataMatcher
    extends ComponentSupport
    implements Matcher
{
  private final MavenPathParser mavenPathParser;

  public MavenMetadataMatcher(final MavenPathParser mavenPathParser) {
    this.mavenPathParser = checkNotNull(mavenPathParser);
  }

  @Override
  public boolean matches(final Context context) {
    final String path = context.getRequest().getPath();
    final MavenPath mavenPath = mavenPathParser.parsePath(path);
    if (mavenPathParser.isRepositoryMetadata(mavenPath)) {
      context.getAttributes().set(MavenPath.class, mavenPath);
      return true;
    }
    return false;
  }
}
