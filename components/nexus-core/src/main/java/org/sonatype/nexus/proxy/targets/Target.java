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
package org.sonatype.nexus.proxy.targets;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.sonatype.nexus.proxy.registry.ContentClass;

import org.codehaus.plexus.util.StringUtils;

/**
 * This is a repository target.
 *
 * @author cstamas
 */
public class Target
{
  private final String id;

  private final String name;

  private final ContentClass contentClass;

  private final Set<String> patternTexts;

  private final Set<Pattern> patterns;

  public Target(String id, String name, ContentClass contentClass, Collection<String> patternTexts)
      throws PatternSyntaxException
  {
    super();

    this.id = id;

    this.name = name;

    this.contentClass = contentClass;

    this.patternTexts = new HashSet<String>(patternTexts);

    this.patterns = new HashSet<Pattern>(patternTexts.size());

    for (String patternText : patternTexts) {
      patterns.add(Pattern.compile(patternText));
    }
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public ContentClass getContentClass() {
    return contentClass;
  }

  public Set<String> getPatternTexts() {
    return Collections.unmodifiableSet(patternTexts);
  }

  public boolean isPathContained(ContentClass contentClass, String path) {
    // if is the same or is compatible
    // make sure to check the inverse of the isCompatible too !!
    if (StringUtils.equals(getContentClass().getId(), contentClass.getId())
        || getContentClass().isCompatible(contentClass)
        || contentClass.isCompatible(getContentClass())) {
      // look for pattern matching
      for (Pattern pattern : patterns) {
        if (pattern.matcher(path).matches()) {
          return true;
        }
      }
    }

    return false;
  }

}
