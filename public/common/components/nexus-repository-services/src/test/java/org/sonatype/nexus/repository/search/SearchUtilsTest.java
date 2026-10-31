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
package org.sonatype.nexus.repository.search;

import java.util.List;

import org.sonatype.nexus.repository.rest.api.RepositoryManagerRESTAdapter;
import org.sonatype.nexus.repository.search.query.SearchFilter;

import org.sonatype.nexus.rest.ValidationErrorsException;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/*
 * NEXUS-54105 - covers the wiring between the REST boundary and the request-construction
 * validator: values pulled out of {@link UriInfo} via {@link SearchUtils#getSearchFilters} and
 * {@link SearchUtils#getComponentSearchFilters} must still cause {@link SearchRequest.Builder#build()}
 * to reject NUL (0x00) bytes. This is the unit-level replacement for the deleted integration-flavour
 * SearchUtilsNulByteValidationTest.
 */
@ExtendWith(MockitoExtension.class)
class SearchUtilsTest
{
  @Mock
  private RepositoryManagerRESTAdapter repoAdapter;

  @Mock
  private UriInfo uriInfo;

  private SearchUtils underTest;

  @BeforeEach
  void setUp() {
    underTest = new SearchUtils(repoAdapter, List.of());
  }

  @Test
  void getSearchFilters_preservesNulByteInValue_andBuildingRequestRejectsIt() {
    stubQueryParams("q", "abc\u0000x");

    List<SearchFilter> filters = underTest.getSearchFilters(uriInfo);

    assertThat(filters).hasSize(1);
    assertThat(filters.get(0).getProperty()).isEqualTo("q");
    assertThat(filters.get(0).getValue()).isEqualTo("abc\u0000x");
    assertThatThrownBy(() -> SearchRequest.builder().searchFilters(filters).build())
        .isInstanceOf(ValidationErrorsException.class);
  }

  @Test
  void getSearchFilters_preservesNulByteInFormatParam_andBuildingRequestRejectsIt() {
    stubQueryParams("format", "maven2\u0000");

    List<SearchFilter> filters = underTest.getSearchFilters(uriInfo);

    assertThat(filters).hasSize(1);
    assertThat(filters.get(0).getValue()).isEqualTo("maven2\u0000");
    assertThatThrownBy(() -> SearchRequest.builder().searchFilters(filters).build())
        .isInstanceOf(ValidationErrorsException.class);
  }

  @Test
  void getComponentSearchFilters_preservesNulByteInValue_andBuildingRequestRejectsIt() {
    stubQueryParams("keyword", "junit\u0000x");

    List<SearchFilter> filters = underTest.getComponentSearchFilters(uriInfo);

    assertThat(filters).hasSize(1);
    assertThat(filters.get(0).getValue()).isEqualTo("junit\u0000x");
    assertThatThrownBy(() -> SearchRequest.builder().searchFilters(filters).build())
        .isInstanceOf(ValidationErrorsException.class);
  }

  @Test
  void getSearchFilters_ordinaryValues_reachBuildWithoutError() {
    MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
    params.putSingle("q", "junit");
    params.putSingle("format", "maven2");
    when(uriInfo.getQueryParameters()).thenReturn(params);

    List<SearchFilter> filters = underTest.getSearchFilters(uriInfo);

    assertThatCode(() -> SearchRequest.builder().searchFilters(filters).build())
        .doesNotThrowAnyException();
  }

  private void stubQueryParams(final String key, final String value) {
    MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
    params.putSingle(key, value);
    when(uriInfo.getQueryParameters()).thenReturn(params);
  }
}
