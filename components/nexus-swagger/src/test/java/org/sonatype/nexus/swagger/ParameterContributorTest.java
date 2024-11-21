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

import org.sonatype.goodies.testsupport.TestSupport;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.QueryParameter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;

import static io.swagger.models.HttpMethod.GET;
import static io.swagger.models.HttpMethod.POST;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ParameterContributorTest
    extends TestSupport
{
  private static final String TEST_PATH_1 = "/foo/{id}";

  private static final String TEST_PATH_2 = "/bar/{id}";

  private static final Collection<HttpMethod> HTTP_METHODS = ImmutableList.of(GET, POST);

  private static final Collection<String> PATHS = ImmutableList.of(TEST_PATH_1, TEST_PATH_2);

  private static final Collection<QueryParameter> PARAMS = ImmutableList.of(new QueryParameter().name("id"));

  @Mock
  private Swagger swagger;

  @Spy
  private Operation getOperationPath1, postOperationPath1;

  @Spy
  private Operation getOperationPath2, postOperationPath2;

  private TestParameterContributor underTest;

  @Before
  public void setup() {
    when(swagger.getPaths()).thenReturn(ImmutableMap.of(
        TEST_PATH_1, new Path().get(getOperationPath1).post(postOperationPath1),
        TEST_PATH_2, new Path().get(getOperationPath2).post(postOperationPath2)));

    underTest = new TestParameterContributor(HTTP_METHODS, PATHS, PARAMS);
  }

  @Test
  public void testContributedMap() {
    // test the map initialized in the constructor
    assertContributedMap(false);
  }

  @Test
  public void testContribute() {
    underTest.contribute(swagger);

    assertContributedMap(true);

    Parameter param = PARAMS.iterator().next();
    verify(getOperationPath1).addParameter(param);
    verify(postOperationPath1).addParameter(param);
    verify(getOperationPath2).addParameter(param);
    verify(postOperationPath2).addParameter(param);

    // call it again for short-circuit use case
    reset(getOperationPath1, postOperationPath1, getOperationPath2, postOperationPath2);
    underTest.contribute(swagger);
    verify(getOperationPath1, never()).addParameter(param);
    verify(postOperationPath1, never()).addParameter(param);
    verify(getOperationPath2, never()).addParameter(param);
    verify(postOperationPath2, never()).addParameter(param);
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
