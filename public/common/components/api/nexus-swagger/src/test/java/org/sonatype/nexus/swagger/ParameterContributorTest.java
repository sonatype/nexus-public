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
package org.sonatype.nexus.swagger;

import java.util.Collection;
import java.util.List;

// NEXUS-46395: migrated from Swagger 1.x to OpenAPI 3.x model types.
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;

import static io.swagger.v3.oas.models.PathItem.HttpMethod.GET;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.POST;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ParameterContributorTest
{
  private static final String TEST_PATH_1 = "/foo/{id}";

  private static final String TEST_PATH_2 = "/bar/{id}";

  private static final Collection<HttpMethod> HTTP_METHODS = List.of(GET, POST);

  private static final Collection<String> PATHS = List.of(TEST_PATH_1, TEST_PATH_2);

  private static final Collection<QueryParameter> PARAMS = List.of(
      (QueryParameter) new QueryParameter().name("id"));

  // NEXUS-46395: io.swagger.models.Swagger -> io.swagger.v3.oas.models.OpenAPI
  @Mock
  private OpenAPI openApi;

  @Spy
  private Operation getOperationPath1, postOperationPath1;

  @Spy
  private Operation getOperationPath2, postOperationPath2;

  private TestParameterContributor underTest;

  @Before
  public void setup() {
    // NEXUS-46395: OpenAPI.getPaths() returns Paths (extends LinkedHashMap<String, PathItem>);
    // construct via addPathItem rather than ImmutableMap.
    Paths paths = new Paths();
    paths.addPathItem(TEST_PATH_1, new PathItem().get(getOperationPath1).post(postOperationPath1));
    paths.addPathItem(TEST_PATH_2, new PathItem().get(getOperationPath2).post(postOperationPath2));
    when(openApi.getPaths()).thenReturn(paths);

    underTest = new TestParameterContributor(HTTP_METHODS, PATHS, PARAMS);
  }

  @Test
  public void testContributedMap() {
    // test the map initialized in the constructor
    assertContributedMap(false);
  }

  @Test
  public void testContribute() {
    // NEXUS-46395: contribute now takes OpenAPI; addParameter -> addParametersItem.
    underTest.contribute(openApi);

    assertContributedMap(true);

    Parameter param = PARAMS.iterator().next();
    verify(getOperationPath1).addParametersItem(param);
    verify(postOperationPath1).addParametersItem(param);
    verify(getOperationPath2).addParametersItem(param);
    verify(postOperationPath2).addParametersItem(param);

    // call it again for short-circuit use case
    reset(getOperationPath1, postOperationPath1, getOperationPath2, postOperationPath2);
    underTest.contribute(openApi);
    verify(getOperationPath1, never()).addParametersItem(param);
    verify(postOperationPath1, never()).addParametersItem(param);
    verify(getOperationPath2, never()).addParametersItem(param);
    verify(postOperationPath2, never()).addParametersItem(param);
  }

  private void assertContributedMap(final boolean result) {
    assertThat(underTest.contributed.size(), equalTo(4));
    assertThat(underTest.contributed, allOf(
        hasEntry("GET-" + TEST_PATH_1, result),
        hasEntry("POST-" + TEST_PATH_1, result),
        hasEntry("GET-" + TEST_PATH_2, result),
        hasEntry("POST-" + TEST_PATH_2, result)));
  }

  private class TestParameterContributor
      extends ParameterContributor<QueryParameter>
  {
    TestParameterContributor(
        final Collection<HttpMethod> httpMethods,
        final Collection<String> paths,
        final Collection<QueryParameter> params)
    {
      super(httpMethods, paths, params);
    }
  }
}
