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
package org.sonatype.nexus.repository.maven.internal.search;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.rest.SearchMapping;
import org.sonatype.nexus.repository.rest.SearchMappings;

import com.google.common.collect.ImmutableList;

import static org.sonatype.nexus.repository.rest.sql.SearchField.FORMAT_FIELD_1;
import static org.sonatype.nexus.repository.rest.sql.SearchField.FORMAT_FIELD_2;
import static org.sonatype.nexus.repository.rest.sql.SearchField.FORMAT_FIELD_3;
import static org.sonatype.nexus.repository.rest.sql.SearchField.FORMAT_FIELD_4;
import static org.sonatype.nexus.repository.rest.sql.SearchField.NAME;
import static org.sonatype.nexus.repository.rest.sql.SearchField.NAMESPACE;

/**
 * @since 3.7
 */
@Named("maven2")
@Singleton
public class MavenSearchMappings
    extends ComponentSupport
    implements SearchMappings
{
  public static final String GAVEC = "gavec";

  private static final List<SearchMapping> MAPPINGS = ImmutableList.of(
      new SearchMapping("maven.groupId", "attributes.maven2.groupId", "Maven groupId", NAMESPACE),
      new SearchMapping("maven.artifactId", "attributes.maven2.artifactId", "Maven artifactId", NAME),
      new SearchMapping("maven.baseVersion", "attributes.maven2.baseVersion", "Maven base version", FORMAT_FIELD_1),
      new SearchMapping("maven.extension", "assets.attributes.maven2.extension", "Maven extension of component's asset",
          FORMAT_FIELD_2),
      new SearchMapping("maven.classifier", "assets.attributes.maven2.classifier",
          "Maven classifier of component's asset", FORMAT_FIELD_3),
      new SearchMapping(GAVEC, GAVEC, "Group asset version extension classifier", FORMAT_FIELD_4));

  @Override
  public Iterable<SearchMapping> get() {
    return MAPPINGS;
  }
}
