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

import java.util.Date;

import org.sonatype.nexus.repository.view.ContentTypes;

/**
 * npm format specific CMA attributes.
 *
 * @since 3.0
 */
public final class NpmAttributes
{
  /**
   * @since 3.7
   */
  public static final String P_NAME = "name";

  /**
   * @since 3.7
   */
  public static final String P_VERSION = "version";

  /**
   * @since 3.7
   */
  public static final String P_AUTHOR = "author";

  /**
   * A mapping of bin commands to set up for this version
   *
   * @since 3.7
   */
  public static final String P_BIN = "bin";

  /**
   * An array of dependencies bundled with this version
   *
   * @since 3.7
   */
  public static final String P_BUNDLE_DEPENDENCIES = "bundleDependencies";

  /**
   * @since 3.7
   */
  public static final String P_DESCRIPTION = "description";

  /**
   * An array of directories included by this version
   *
   * @since 3.7
   */
  public static final String P_DIRECTORIES = "directories";

  /**
   * @since 3.7
   */
  public static final String P_CONTRIBUTORS = "contributors";

  /**
   * @since 3.7
   */
  public static final String P_LICENSE = "license";

  /**
   * @since 3.7
   */
  public static final String P_KEYWORDS = "keywords";

  /**
   * The package's entry point
   *
   * @since 3.7
   */
  public static final String P_MAIN = "main";

  /**
   * Array of human objects for people with permission to publish this package
   *
   * @since 3.7
   */
  public static final String P_MAINTAINERS = "maintainers";

  /**
   * An object mapping package names to the required semver ranges of optional dependencies
   *
   * @since 3.7
   */
  public static final String P_OPTIONAL_DEPENDENCIES = "optionalDependencies";

  /**
   * A mapping of package names to the required semver ranges of peer dependencies
   *
   * @since 3.7
   */
  public static final String P_PEER_DEPENDENCIES = "peerDependencies";

  /**
   * On package root metadata this is a URL, on package.json this is an object with <code>url</code> and
   * <code>email</code> fields.
   *
   * @since 3.7
   */
  public static final String P_BUGS = "bugs";

  /**
   * @since 3.7
   */
  public static final String P_BUGS_URL = "bugs_url";

  /**
   * @since 3.7
   */
  public static final String P_BUGS_EMAIL = "bugs_email";

  /**
   * The first 64K of the README data for the most-recently published version of the package
   *
   * @since 3.7
   */
  public static final String P_README = "readme";

  /**
   * The name of the file from which the readme data was taken.
   *
   * @since 3.7
   */
  public static final String P_README_FILENAME = "readmeFilename";

  /**
   * An object with type and url fields.
   *
   * @since 3.7
   */
  public static final String P_REPOSITORY = "repository";

  /**
   * @since 3.7
   */
  public static final String P_REPOSITORY_TYPE = "repository_type";

  /**
   * @since 3.7
   */
  public static final String P_REPOSITORY_URL = "repository_url";

  /**
   * @since 3.7
   */
  public static final String P_HOMEPAGE = "homepage";

  /**
   * The SHA-1 sum of the tarball
   *
   * @since 3.7
   */
  public static final String P_SHASUM = "shasum";

  /**
   * <code>true</code> if this version is known to have a shrinkwrap that must be used to install it; <code>false</code>
   * if this version is known not to have a shrinkwrap. Unset otherwise
   *
   * @since 3.7
   */
  public static final String P_HAS_SHRINK_WRAP = "_hasShrinkwrap";

  /**
   * @since 3.7
   */
  public static final String P_SCOPE = "scope";

  /**
   * @since 3.7
   */
  public static final String P_OS = "os";

  /**
   * @since 3.7
   */
  public static final String P_CPU = "cpu";

  /**
   * @since 3.7
   */
  public static final String P_ENGINES = "engines";

  /**
   * Special format attribute used for supporting search on "is" and "not" (currently for "unstable" packages only).
   *
   * @since 3.7
   */
  public static final String P_TAGGED_IS = "tagged_is";

  /**
   * Special format attribute used for supporting search on "is" and "not" (currently for "unstable" packages only).
   *
   * @since 3.7
   */
  public static final String P_TAGGED_NOT = "tagged_not";

  /**
   * @since 3.7
   */
  public static final String P_URL = "url";

  /**
   * An object whose keys are the npm user names of people who have starred this package
   *
   * @since 3.7
   */
  public static final String P_USERS = "users";

  /**
   * Special format attribute used for ordering by version in a lexicographic manner within ES queries.
   *
   * @since 3.7
   */
  public static final String P_SEARCH_NORMALIZED_VERSION = "search_normalized_version";

  /**
   * Format attribute on component for npm tarball that shows that given version (to which this tarball belongs to) is
   * deprecated. Attribute's value is deprecation message extracted from npm package metadata. Like in npm metadata,
   * solely by the presence of this attribute one can tell is tarball deprecated or not, while the value can provide
   * more insight why it was deprecated.
   */
  public static final String P_DEPRECATED = "deprecated";

  /**
   * Format attribute on package root asset to designate npm "modified" timestamp as {@link Date}, extracted from npm
   * package metadata "time/modified".
   */
  public static final String P_NPM_LAST_MODIFIED = "last_modified";

  /**
   * Marker for asset kinds.
   */
  public enum AssetKind
  {
    REPOSITORY_ROOT(ContentTypes.APPLICATION_JSON, false),
    PACKAGE_ROOT(ContentTypes.APPLICATION_JSON, true),
    TARBALL(ContentTypes.APPLICATION_GZIP, false);

    private final String contentType;

    private final boolean skipContentVerification;

    AssetKind(final String contentType, final boolean skipContentVerification) {
      this.skipContentVerification = skipContentVerification;
      this.contentType = contentType;
    }

    public String getContentType() {
      return contentType;
    }

    public boolean isSkipContentVerification() {
      return skipContentVerification;
    }
  }

  private NpmAttributes() {
    // empty
  }
}
