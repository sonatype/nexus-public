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

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.HashType;
import org.sonatype.nexus.repository.maven.MavenPathParser;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Matcher;

import com.google.common.base.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Matcher support for Maven use cases.
 *
 * @since 3.0
 */
public class MavenMatcherSupport
    extends ComponentSupport
    implements Matcher
{
  /**
   * Returns a {@link Predicate} that will match for given path whenever passed in predicate matches, plus, for Maven2
   * layout defined extensions like ".sha1" and ".md5".
   */
  public static Predicate<String> withHashes(final Predicate<String> predicate) {
    return (String input) ->
    {
      String mainPath = input;
      for (HashType hashType : HashType.values()) {
        if (mainPath.endsWith("." + hashType.getExt())) {
          mainPath = mainPath.substring(0, mainPath.length() - (hashType.getExt().length() + 1));
          break;
        }
      }
      return predicate.apply(mainPath);
    };
  }

  private final MavenPathParser mavenPathParser;

  private final Predicate<String> predicate;

  public MavenMatcherSupport(final MavenPathParser mavenPathParser, final Predicate<String> predicate) {
    this.mavenPathParser = checkNotNull(mavenPathParser);
    this.predicate = checkNotNull(predicate);
  }

  @Override
  public boolean matches(final Context context) {
    final String path = context.getRequest().getPath();
    if (predicate.apply(path)) {
      final MavenPath mavenPath = mavenPathParser.parsePath(path);
      context.getAttributes().set(MavenPath.class, mavenPath);
      return true;
    }
    return false;
  }
}
