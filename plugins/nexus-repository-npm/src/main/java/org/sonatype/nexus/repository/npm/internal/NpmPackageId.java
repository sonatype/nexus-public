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
package org.sonatype.nexus.repository.npm.internal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sonatype.nexus.common.app.VersionComparator;

import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * npm package identifier. Rules applied:
 * <ul>
 * <li>must be shorter that 214 characters (gross length, w/ scope)</li>
 * <li>can't start with '.' (dot) or '_' (underscore)</li>
 * <li>only URL-safe characters can be used</li>
 * </ul>
 *
 * Allowed formats:
 * <ul>
 * <li>without scope, example: "name"</li>
 * <li>with scope, example: "@scope/name"</li>
 * </ul>
 *
 * @see <a href="https://docs.npmjs.com/files/package.json#name">Package JSON 'name'</a>
 * @since 3.0
 */
public final class NpmPackageId
    implements Comparable<NpmPackageId>
{
  private static final VersionComparator comparator = NpmVersionComparator.versionComparator;

  private static final Escaper escaper = UrlEscapers.urlPathSegmentEscaper();

  private final String scope;

  private final String name;

  private final String id;

  public NpmPackageId(@Nullable final String scope, final String name) {

    checkArgument(name.length() > 0, "Name cannot be empty string");
    checkArgument(name.equals(escaper.escape(name)), "Non URL-safe name: %s", name);
    if (scope == null) {
      checkArgument(!name.startsWith(".") && !name.startsWith("_"), "Name starts with '.' or '_': %s", name);
      this.id = name;
    }
    else {
      checkArgument(scope.length() > 0, "Scope cannot be empty string");
      checkArgument(scope.equals(escaper.escape(scope)), "Non URL-safe scope: %s", scope);
      checkArgument(!scope.startsWith(".") && !scope.startsWith("_"), "Scope starts with '.' or '_': %s", scope);
      this.id = "@" + scope + "/" + name;
    }
    checkArgument(this.id.length() < 214, "Must be shorter than 214 characters: %s", id);
    this.scope = scope;
    this.name = name;
  }

  /**
   * Returns the scope part of package name or {@code null} if not scoped.
   */
  @Nullable
  public String scope() {
    return scope;
  }

  /**
   * Returns the name part of package name, never {@code null}.
   */
  @Nonnull
  public String name() {
    return name;
  }

  /**
   * Returns the full name of package.
   */
  @Nonnull
  public String id() {
    return id;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NpmPackageId)) {
      return false;
    }
    NpmPackageId that = (NpmPackageId) o;
    return id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public int compareTo(final NpmPackageId o) {
    return comparator.compare(id, o.id);
  }

  @Override
  public String toString() {
    return id();
  }

  /**
   * Parses string into {@link NpmPackageId}.
   */
  public static NpmPackageId parse(final String string) {
    String scope = null;
    String name = checkNotNull(string);
    int slashIndex = name.indexOf('/');
    if (string.startsWith("@") && slashIndex > -1) {
      scope = name.substring(1, slashIndex);
      name = name.substring(slashIndex + 1, name.length());
    }
    return new NpmPackageId(scope, name);
  }
}
