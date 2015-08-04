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
package org.sonatype.nexus.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.walker.ParentOMatic;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.codehaus.plexus.util.StringUtils;

/**
 * Simple collection of some static path related code.
 * <p>
 * Note: all the input and output paths are expected to be "normalized ones": being absolute, using generic "/"
 * character as path separator (since these are NOT FS File paths, but just hierarchical paths of strings). For
 * example:
 * {@link RepositoryItemUid#getPath()} returns paths like these and as those are used throughout of Nexus.
 * <p>
 *
 * @author cstamas
 * @since 2.4
 */
public class PathUtils
{

  /**
   * @since 2.8 (moved from ItemPathUtils)
   */
  public static final String PATH_SEPARATOR = RepositoryItemUid.PATH_SEPARATOR;

  /**
   * @since 2.8 (moved from ItemPathUtils)
   */
  public static final int PATH_SEPARATOR_LENGTH = PATH_SEPARATOR.length();

  /**
   * Returns the "depth" (like directory depth) on the passed in path.
   *
   * @return the depth of the path.
   */
  public static int depthOf(final String path) {
    return elementsOf(path).size();
  }

  /**
   * Splits the passed in path into path elements. Note: this code was originally in
   * {@link ParentOMatic#getPathElements} method!
   *
   * @return list of path elements.
   */
  public static List<String> elementsOf(final String path) {
    final List<String> result = Lists.newArrayList();
    final String[] elems = path.split("/");
    for (String elem : elems) {
      if (!Strings.isNullOrEmpty(elem)) {
        result.add(elem);
      }
    }
    return result;
  }

  /**
   * Assembles a path from all elements.
   *
   * @param elements the list of path elements to assemble path from.
   * @return a normalized path assembled from all path elements.
   */
  public static String pathFrom(final List<String> elements) {
    return pathFrom(elements, elements.size());
  }

  /**
   * Assembles a path from all elements.
   *
   * @param elements the list of path elements to assemble path from.
   * @param f        function to apply to path elements.
   * @return a normalized path assembled from all path elements.
   */
  public static String pathFrom(final List<String> elements, final Function<String, String> f) {
    return pathFrom(elements, elements.size(), f);
  }

  /**
   * Assembles a path from some count of elements.
   *
   * @param elements         the list of path elements to assemble path from.
   * @param maxElementsToUse has effect only if less then {@code elements.size()} naturally.
   * @return a normalized path assembled from maximized count of path elements.
   */
  public static String pathFrom(final List<String> elements, final int maxElementsToUse) {
    return pathFrom(elements, maxElementsToUse, new Ident());
  }

  /**
   * Assembles a path from some count of elements.
   *
   * @param elements         the list of path elements to assemble path from.
   * @param maxElementsToUse has effect only if less then {@code elements.size()} naturally.
   * @return a normalized path assembled from maximized count of path elements.
   */
  public static String pathFrom(final List<String> elements, final int maxElementsToUse,
                                final Function<String, String> f)
  {
    final StringBuilder sb = new StringBuilder("/");
    int elementsUsed = 0;
    final Iterator<String> elementsIterator = elements.iterator();
    while (elementsIterator.hasNext()) {
      sb.append(f.apply(elementsIterator.next()));
      elementsUsed++;
      if (elementsUsed == maxElementsToUse) {
        break;
      }
      if (elementsIterator.hasNext()) {
        sb.append("/");
      }
    }
    return sb.toString();
  }

  /**
   * Simple concat method. It only watches that there is only one PATH_SEPARATOR betwen parts passed in. It DOES NOT
   * checks that parts are fine or not.
   *
   * @since 2.8 (moved from ItemPathUtils)
   */
  public static String concatPaths(String... p) {
    StringBuilder result = new StringBuilder();

    for (String path : p) {
      if (!StringUtils.isEmpty(path)) {
        if (!path.startsWith(PATH_SEPARATOR)) {
          result.append(PATH_SEPARATOR);
        }

        result.append(path.endsWith(PATH_SEPARATOR) ? path.substring(0, path.length()
            - PATH_SEPARATOR_LENGTH) : path);
      }
    }

    return result.toString();
  }

  /**
   * Simple path cleanup.
   *
   * @since 2.8 (moved from ItemPathUtils)
   */
  public static String cleanUpTrailingSlash(String path) {
    if (StringUtils.isEmpty(path)) {
      path = PATH_SEPARATOR;
    }

    if (path.length() > 1 && path.endsWith(PATH_SEPARATOR)) {
      path = path.substring(0, path.length() - PATH_SEPARATOR_LENGTH);
    }

    return path;
  }

  /**
   * Calculates the parent path for a path.
   *
   * @since 2.8 (moved from ItemPathUtils)
   */
  public static String getParentPath(String path) {
    if (PATH_SEPARATOR.equals(path)) {
      return path;
    }

    int lastSepratorPos = path.lastIndexOf(PATH_SEPARATOR);

    if (lastSepratorPos == 0) {
      return PATH_SEPARATOR;
    }
    else {
      return path.substring(0, lastSepratorPos);
    }
  }

  /**
   * Calculates the depth of a path, 0 being root.
   *
   * @since 2.8 (moved from ItemPathUtils)
   */
  public static int getPathDepth(String path) {
    if (PATH_SEPARATOR.equals(path)) {
      return 0;
    }
    else {
      final String parentPath = getParentPath(path);
      if (PATH_SEPARATOR.equals(parentPath)) {
        return 0;
      }
      else {
        return 1 + getPathDepth(parentPath);
      }
    }
  }

  /**
   * Calculates the least common parent path
   *
   * @return null if paths is empty, else the least common parent path
   * @since 2.8 (moved from ItemPathUtils)
   */
  public static String getLCPPath(final Collection<String> paths) {
    String lcp = null;

    for (String path : paths) {
      if (lcp == null) {
        lcp = path;
      }
      else {
        lcp = getLCPPath(lcp, path);
      }
    }

    return lcp;
  }

  /**
   * Calculates the least common parent path
   *
   * @return null if any path is empty, else the least common parent path
   * @since 2.8 (moved from ItemPathUtils)
   */
  public static String getLCPPath(final String pathA, final String pathB) {
    if (StringUtils.isEmpty(pathA) || StringUtils.isEmpty(pathB)) {
      return null;
    }

    if (pathA.equals(pathB)) {
      return pathA;
    }

    if (pathA.startsWith(pathB)) {
      return pathB;
    }

    if (pathB.startsWith(pathA)) {
      return pathA;
    }

    StringBuilder lcp = new StringBuilder();

    StringBuilder token = new StringBuilder();

    int index = 0;

    while (pathA.charAt(index) == pathB.charAt(index) && index < pathA.length() && index < pathB.length()) {
      token.append(pathA.charAt(index));

      if (pathA.charAt(index) == PATH_SEPARATOR.charAt(0)) {
        lcp.append(token);

        token.delete(0, token.length());
      }

      index++;
    }

    return lcp.toString();
  }

  /**
   * Ident function for path elements.
   */
  public static final class Ident
      implements Function<String, String>
  {
    @Override
    public String apply(@Nullable String input) {
      return input;
    }
  }
}
