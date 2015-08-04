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

import java.io.File;
import java.util.Map;
import java.util.Properties;

import org.sonatype.nexus.testsuite.support.Filter;
import org.sonatype.sisu.maven.bridge.MavenModelResolver;

import com.google.common.collect.Maps;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.sisu.maven.bridge.support.ModelBuildingRequestBuilder.model;

/**
 * Support class for filters that needs effective model of project under test.
 *
 * @since 2.2
 */
public abstract class TestProjectFilterSupport
    extends MapFilterSupport
    implements Filter
{

  public static final String TEST_PROJECT_POM_FILE = "testProjectPomFile";

  /**
   * Model resolver used to resolve effective model of test project (pom).
   * Never null.
   */
  private final MavenModelResolver modelResolver;

  /**
   * Properties used to resolve model.
   */
  private final Map<String, String> properties;

  /**
   * Constructor.
   *
   * @param modelResolver Model resolver used to resolve effective model of test project (pom). Cannot be null.
   * @param properties    used to resolve model
   */
  public TestProjectFilterSupport(final MavenModelResolver modelResolver,
                                  final Map<String, String> properties)
  {
    this.modelResolver = checkNotNull(modelResolver);
    this.properties = checkNotNull(properties);
  }

  /**
   * Returns mappings by extracting testing project model properties.
   *
   * @param context filtering context. Cannot be null.
   * @param value   value to be filtered. Ignored by this filter.
   * @return mappings extracted from project under test model
   */
  @Override
  Map<String, String> mappings(final Map<String, String> context, final String value) {
    final Map<String, String> mappings = Maps.newHashMap();

    final String testProjectPomFile = context.get(TEST_PROJECT_POM_FILE);
    if (testProjectPomFile == null) {
      // TODO log a warning?
    }
    else {
      try {
        final Properties userProperties = new Properties();
        userProperties.putAll(properties);

        final Model model = modelResolver.resolveModel(
            model().pom(new File(testProjectPomFile)).setUserProperties(userProperties)
        );

        return mappings(context, value, model);
      }
      catch (ModelBuildingException e) {
        // TODO log?
      }
    }

    return mappings;
  }

  protected Map<String, String> getProperties() {
    return properties;
  }

  /**
   * Returns the mappings between placeholders and values that this filter supports.
   *
   * @param context filtering context. Never null.
   * @param value   value to be filtered. Never null.
   * @param model   resolved model of project under test. Never null.
   * @return mappings extracted from project under test model. Should not be null.
   */
  abstract Map<String, String> mappings(final Map<String, String> context, final String value, final Model model);

}
