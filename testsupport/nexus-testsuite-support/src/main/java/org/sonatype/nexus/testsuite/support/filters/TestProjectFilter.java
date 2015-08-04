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
package org.sonatype.nexus.testsuite.support.filters;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.testsuite.support.Filter;
import org.sonatype.sisu.maven.bridge.MavenModelResolver;

import com.google.common.collect.Maps;
import org.apache.maven.model.Model;
import org.eclipse.sisu.Parameters;

/**
 * Replaces placeholders with values out of test project pom: <br/>
 * - ${project.groupId}
 * - ${project.artifactId}
 * - ${project.version}
 *
 * @since 2.2
 */
@Named
@Singleton
public class TestProjectFilter
    extends TestProjectFilterSupport
    implements Filter
{

  /**
   * Constructor.
   *
   * @param modelResolver Model resolver used to resolve effective model of test project (pom). Cannot be null.
   */
  @Inject
  public TestProjectFilter(@Named("remote-model-resolver-using-settings") final MavenModelResolver modelResolver,
                           @Parameters final Map<String, String> properties)
  {
    super(modelResolver, properties);
  }

  /**
   * Returns mappings by extracting project model properties from project under test.
   *
   * @param context filtering context. Cannot be null.
   * @param value   value to be filtered. Ignored by this filter.
   * @param model   resolved model of project under test. Cannot be null.
   * @return mappings extracted from project under test model
   */
  @Override
  Map<String, String> mappings(final Map<String, String> context, final String value, final Model model) {
    final Map<String, String> mappings = Maps.newHashMap();

    mappings.put("project.groupId", model.getGroupId());
    mappings.put("project.artifactId", model.getArtifactId());
    mappings.put("project.version", model.getVersion());

    if (model.getProperties() != null) {
      mappings.putAll(Maps.fromProperties(model.getProperties()));
    }

    mappings.putAll(getProperties());

    return mappings;
  }

}
