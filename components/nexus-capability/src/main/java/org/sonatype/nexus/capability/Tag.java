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
package org.sonatype.nexus.capability;

import java.util.Set;

import com.google.common.collect.Sets;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A Capability tag (key/value).
 *
 * @since 2.7
 */
public class Tag
{

  /**
   * Key of category tag.
   */
  public static String CATEGORY = "Category";

  /**
   * Key of repository tag.
   */
  public static String REPOSITORY = "Repository";

  private final String key;

  private final String value;

  public Tag(final String key, final String value) {
    this.key = checkNotNull(key);
    this.value = checkNotNull(value);
  }

  public String key() {
    return key;
  }

  public String value() {
    return value;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Tag)) {
      return false;
    }

    Tag tag = (Tag) o;

    if (!key.equals(tag.key)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  /**
   * Convenience method for creating a set of tags
   */
  public static Set<Tag> tags(final Tag... tags) {
    return Sets.newHashSet(checkNotNull(tags));
  }

  /**
   * Convenience method for a category tag.
   */
  public static Tag categoryTag(final String category) {
    return new Tag(CATEGORY, category);
  }

  /**
   * Convenience method for a repository tag.
   */
  public static Tag repositoryTag(final String repository) {
    return new Tag(REPOSITORY, repository);
  }

}
