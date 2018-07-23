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
package org.sonatype.nexus.repository.pypi.internal;

/**
 * PyPI format specific attributes. PyPI doesn't have a clear component/asset separation (it has a name under which
 * every possible package and archive regardless of version is grouped), so much of this is a best fit to our model.
 *
 * @since 3.1
 */
public final class PyPiAttributes
{

  /**
   * Name attribute on component and asset.
   */
  public static final String P_NAME = "name";

  /**
   * Version attribute on component and asset.
   */
  public static final String P_VERSION = "version";

  /**
   * Summary attribute on component and asset.
   */
  public static final String P_SUMMARY = "summary";

  /**
   * Description attribute on asset.
   */
  public static final String P_DESCRIPTION = "description";

  /**
   * Keywords attribute on asset.
   */
  public static final String P_KEYWORDS = "keywords";

  /**
   * License attribute on asset.
   */
  public static final String P_LICENSE = "license";

  /**
   * Author attribute on asset.
   */
  public static final String P_AUTHOR = "author";

  /**
   * Author email attribute on asset.
   */
  public static final String P_AUTHOR_EMAIL = "author_email";

  /**
   * Maintainer attribute on asset.
   */
  public static final String P_MAINTAINER = "maintainer";

  /**
   * Maintainer email attribute on asset.
   */
  public static final String P_MAINTAINER_EMAIL = "maintainer_email";

  /**
   * Home page attribute on asset.
   */
  public static final String P_HOME_PAGE = "home_page";

  /**
   * Download url attribute on asset.
   */
  public static final String P_DOWNLOAD_URL = "download_url";

  /**
   * Classifiers attribute on asset.
   */
  public static final String P_CLASSIFIERS = "classifiers";

  /**
   * Requires_dist attribute on asset.
   */
  public static final String P_REQUIRES_DIST = "requires_dist";

  /**
   * Provides_extra on asset.
   */
  public static final String P_PROVIDES_EXTRA = "provides_extra";

  /**
   * Pyversion attribute on asset.
   */
  public static final String P_PYVERSION = "pyversion";

  /**
   * Platform attribute on asset.
   */
  public static final String P_PLATFORM = "platform";

  /**
   * Filetype attribute on asset.
   */
  public static final String P_FILETYPE = "filetype";

  /**
   * Supported platform attribute on asset.
   */
  public static final String P_SUPPORTED_PLATFORM = "supported_platform";

  /**
   * Provides_dist attribute on asset.
   */
  public static final String P_PROVIDES_DIST = "provides_dist";

  /**
   * Obsoletes_dist attribute on asset.
   */
  public static final String P_OBSOLETES_DIST = "obsoletes_dist";

  /**
   * Requires_python attribute on asset.
   */
  public static final String P_REQUIRES_PYTHON = "requires_python";

  /**
   * Requires_external attribute on asset.
   */
  public static final String P_REQUIRES_EXTERNAL = "requires_external";

  /**
   * Project_url attribute on asset.
   */
  public static final String P_PROJECT_URL = "project_url";

  /**
   * Generator attribute on asset.
   */
  public static final String P_GENERATOR = "generator";

  /**
   * A stored copy of the detected archive type.
   */
  public static final String P_ARCHIVE_TYPE = "archive_type";

  private PyPiAttributes() {
    // empty
  }
}
