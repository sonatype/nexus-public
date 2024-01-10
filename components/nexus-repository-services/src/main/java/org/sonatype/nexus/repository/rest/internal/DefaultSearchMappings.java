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
package org.sonatype.nexus.repository.rest.internal;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.rest.SearchMapping;
import org.sonatype.nexus.repository.rest.SearchMappings;
import org.sonatype.nexus.repository.rest.sql.SearchField;

import com.google.common.collect.ImmutableList;

import static org.sonatype.nexus.repository.rest.sql.SearchField.FORMAT;
import static org.sonatype.nexus.repository.rest.sql.SearchField.KEYWORDS;
import static org.sonatype.nexus.repository.rest.sql.SearchField.MD5;
import static org.sonatype.nexus.repository.rest.sql.SearchField.NAME;
import static org.sonatype.nexus.repository.rest.sql.SearchField.NAMESPACE;
import static org.sonatype.nexus.repository.rest.sql.SearchField.SHA1;
import static org.sonatype.nexus.repository.rest.sql.SearchField.SHA256;
import static org.sonatype.nexus.repository.rest.sql.SearchField.SHA512;
import static org.sonatype.nexus.repository.search.index.SearchConstants.IS_PRERELEASE_KEY;
import static org.sonatype.nexus.repository.search.index.SearchConstants.REPOSITORY_NAME;

/**
 * @since 3.7
 */
@Named("default")
@Singleton
public class DefaultSearchMappings
    extends ComponentSupport
    implements SearchMappings
{
  public static final String NAME_RAW = "name.raw";

  public static final String NAME_RAW_ALIAS = "name";

  public static final String GROUP_RAW = "group.raw";

  public static final String VERSION = "version";

  public static final String PRERELEASE = "prerelease";

  private static final List<SearchMapping> MAPPINGS = ImmutableList.of(
      new SearchMapping("q", "keyword", "Query by keyword", KEYWORDS, false),
      new SearchMapping("repository", REPOSITORY_NAME, "Repository name", SearchField.REPOSITORY_NAME),
      new SearchMapping("format", "format", "Query by format", FORMAT),
      new SearchMapping("group", GROUP_RAW, "Component group", NAMESPACE),
      new SearchMapping(NAME_RAW_ALIAS, NAME_RAW, "Component name", NAME),
      new SearchMapping(VERSION, VERSION, "Component version", SearchField.VERSION),
      new SearchMapping(PRERELEASE, IS_PRERELEASE_KEY, "Prerelease version flag", SearchField.PRERELEASE),
      new SearchMapping("md5", "assets.attributes.checksum.md5", "Specific MD5 hash of component's asset", MD5),
      new SearchMapping("sha1", "assets.attributes.checksum.sha1", "Specific SHA-1 hash of component's asset", SHA1),
      new SearchMapping("sha256", "assets.attributes.checksum.sha256", "Specific SHA-256 hash of component's asset",
          SHA256),
      new SearchMapping("sha512", "assets.attributes.checksum.sha512", "Specific SHA-512 hash of component's asset",
          SHA512));

  @Override
  public Iterable<SearchMapping> get() {
    return MAPPINGS;
  }
}
