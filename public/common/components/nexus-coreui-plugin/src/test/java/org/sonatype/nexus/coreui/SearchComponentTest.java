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
package org.sonatype.nexus.coreui;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.validation.ValidationException;
import javax.validation.Validator;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.bootstrap.validation.ValidationConfiguration;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.extdirect.model.LimitedPagedResponse;
import org.sonatype.nexus.extdirect.model.StoreLoadParameters;
import org.sonatype.nexus.extdirect.model.StoreLoadParameters.Filter;
import org.sonatype.nexus.extdirect.model.StoreLoadParameters.Sort;
import org.sonatype.nexus.repository.search.ComponentSearchResult;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.SearchResponse;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.repository.search.event.SearchEvent;
import org.sonatype.nexus.repository.search.query.SearchResultsGenerator;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;
import org.sonatype.nexus.testcommon.validation.ValidationExtension.ValidationExecutor;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorFactoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SearchComponent}.
 */
@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class SearchComponentTest
    extends Test5Support
{
  @ValidationExecutor
  private final Validator validator =
      new ValidationConfiguration().validatorFactory(new ConstraintValidatorFactoryImpl()).getValidator();

  @Mock
  private SearchService searchService;

  @Mock
  private EventManager eventManager;

  @Mock
  private SearchResultsGenerator searchResultsGenerator;

  private SearchComponent underTest;

  @BeforeEach
  void setUp() {
    underTest = new SearchComponent(searchService, 1000, searchResultsGenerator, eventManager);
  }

  @Test
  void testRead_noFilters_returnsEmptyResponse() {
    StoreLoadParameters parameters = new StoreLoadParameters();
    parameters.setLimit(25);

    LimitedPagedResponse<ComponentXO> result = underTest.read(parameters);

    assertThat(result, is(notNullValue()));
    assertThat(result.getData(), is(empty()));
    verifyNoInteractions(searchService);
  }

  @Test
  void testRead_emptyFilterList_returnsEmptyResponse() {
    StoreLoadParameters parameters = new StoreLoadParameters();
    parameters.setLimit(25);
    parameters.setFilter(Collections.emptyList());

    LimitedPagedResponse<ComponentXO> result = underTest.read(parameters);

    assertThat(result, is(notNullValue()));
    assertThat(result.getData(), is(empty()));
    verifyNoInteractions(searchService);
  }

  @Test
  void testRead_formatSearchWithOnlyFormatCriteria_returnsEmptyResponse() {
    StoreLoadParameters parameters = new StoreLoadParameters();
    parameters.setLimit(25);
    parameters.setFormatSearch(true);
    parameters.setFilter(List.of(
        new Filter()
        {
          {
            setProperty("format");
            setValue("maven2");
          }
        }));

    LimitedPagedResponse<ComponentXO> result = underTest.read(parameters);

    assertThat(result, is(notNullValue()));
    assertThat(result.getData(), is(empty()));
    verifyNoInteractions(searchService);
  }

  @Test
  void testRead_nonFormatSearchWithOnlyFormatCriteria_throwsValidationError() {
    StoreLoadParameters parameters = new StoreLoadParameters();
    parameters.setLimit(25);
    parameters.setFormatSearch(false);
    parameters.setFilter(List.of(
        new Filter()
        {
          {
            setProperty("format");
            setValue("maven2");
          }
        }));

    assertThrows(ValidationErrorsException.class, () -> underTest.read(parameters));
    verifyNoInteractions(searchService);
  }

  @Test
  void testRead_withSearchCriteria_performsSearch() {
    StoreLoadParameters parameters = new StoreLoadParameters();
    parameters.setLimit(25);
    parameters.setFilter(List.of(
        new Filter()
        {
          {
            setProperty("keyword");
            setValue("test-artifact");
          }
        }));

    SearchResponse searchResponse = new SearchResponse();
    searchResponse.setTotalHits(1L);
    searchResponse.setSearchResults(Collections.emptyList());
    when(searchService.search(any(SearchRequest.class))).thenReturn(searchResponse);

    ComponentSearchResult searchResult = new ComponentSearchResult();
    searchResult.setId("comp-1");
    searchResult.setGroup("org.example");
    searchResult.setName("test-artifact");
    searchResult.setVersion("1.0.0");
    searchResult.setRepositoryName("maven-central");
    searchResult.setFormat("maven2");
    when(searchResultsGenerator.getSearchResultList(searchResponse)).thenReturn(List.of(searchResult));

    LimitedPagedResponse<ComponentXO> result = underTest.read(parameters);

    assertThat(result, is(notNullValue()));
    assertThat(result.getData(), hasSize(1));
    List<ComponentXO> data = new ArrayList<>(result.getData());
    assertThat(data.get(0).getName(), is("test-artifact"));
    assertThat(data.get(0).getGroup(), is("org.example"));
    assertThat(data.get(0).getVersion(), is("1.0.0"));
    assertThat(data.get(0).getRepositoryName(), is("maven-central"));
    assertThat(data.get(0).getFormat(), is("maven2"));

    verify(eventManager).post(any(SearchEvent.class));
  }

  @Test
  void testRead_withLastModified_setsLastBlobUpdated() {
    StoreLoadParameters parameters = new StoreLoadParameters();
    parameters.setLimit(25);
    parameters.setFilter(List.of(
        new Filter()
        {
          {
            setProperty("keyword");
            setValue("my-artifact");
          }
        }));

    SearchResponse searchResponse = new SearchResponse();
    searchResponse.setTotalHits(1L);
    when(searchService.search(any(SearchRequest.class))).thenReturn(searchResponse);

    OffsetDateTime lastModified = OffsetDateTime.now();
    ComponentSearchResult searchResult = new ComponentSearchResult();
    searchResult.setId("comp-1");
    searchResult.setName("my-artifact");
    searchResult.setLastModified(lastModified);
    when(searchResultsGenerator.getSearchResultList(searchResponse)).thenReturn(List.of(searchResult));

    LimitedPagedResponse<ComponentXO> result = underTest.read(parameters);

    List<ComponentXO> data = new ArrayList<>(result.getData());
    assertThat(data.get(0).getLastBlobUpdated(), is(lastModified.toString()));
  }

  @Test
  void testRead_withNullLastModified_lastBlobUpdatedIsNull() {
    StoreLoadParameters parameters = new StoreLoadParameters();
    parameters.setLimit(25);
    parameters.setFilter(List.of(
        new Filter()
        {
          {
            setProperty("keyword");
            setValue("my-artifact");
          }
        }));

    SearchResponse searchResponse = new SearchResponse();
    searchResponse.setTotalHits(1L);
    when(searchService.search(any(SearchRequest.class))).thenReturn(searchResponse);

    ComponentSearchResult searchResult = new ComponentSearchResult();
    searchResult.setId("comp-1");
    searchResult.setName("my-artifact");
    when(searchResultsGenerator.getSearchResultList(searchResponse)).thenReturn(List.of(searchResult));

    LimitedPagedResponse<ComponentXO> result = underTest.read(parameters);

    List<ComponentXO> data = new ArrayList<>(result.getData());
    assertThat(data.get(0).getLastBlobUpdated(), is(nullValue()));
  }

  @Test
  void testRead_withSorting() {
    StoreLoadParameters parameters = new StoreLoadParameters();
    parameters.setLimit(25);
    parameters.setSort(List.of(new Sort("name", "ASC")));
    parameters.setFilter(List.of(
        new Filter()
        {
          {
            setProperty("keyword");
            setValue("test");
          }
        }));

    SearchResponse searchResponse = new SearchResponse();
    searchResponse.setTotalHits(0L);
    when(searchService.search(any(SearchRequest.class))).thenReturn(searchResponse);
    when(searchResultsGenerator.getSearchResultList(searchResponse)).thenReturn(Collections.emptyList());

    LimitedPagedResponse<ComponentXO> result = underTest.read(parameters);

    assertThat(result, is(notNullValue()));
    verify(searchService).search(any(SearchRequest.class));
  }

  @Test
  void testRead_withPagination() {
    StoreLoadParameters parameters = new StoreLoadParameters();
    parameters.setLimit(10);
    parameters.setPage(2);
    parameters.setFilter(List.of(
        new Filter()
        {
          {
            setProperty("keyword");
            setValue("test");
          }
        }));

    SearchResponse searchResponse = new SearchResponse();
    searchResponse.setTotalHits(0L);
    when(searchService.search(any(SearchRequest.class))).thenReturn(searchResponse);
    when(searchResultsGenerator.getSearchResultList(searchResponse)).thenReturn(Collections.emptyList());

    LimitedPagedResponse<ComponentXO> result = underTest.read(parameters);

    assertThat(result, is(notNullValue()));
  }

  @Test
  void testRead_limitExceedsSearchResultsLimit_clamped() {
    underTest.setSearchResultsLimit(50);

    StoreLoadParameters parameters = new StoreLoadParameters();
    parameters.setLimit(100);
    parameters.setFilter(List.of(
        new Filter()
        {
          {
            setProperty("keyword");
            setValue("test");
          }
        }));

    SearchResponse searchResponse = new SearchResponse();
    searchResponse.setTotalHits(0L);
    when(searchService.search(any(SearchRequest.class))).thenReturn(searchResponse);
    when(searchResultsGenerator.getSearchResultList(searchResponse)).thenReturn(Collections.emptyList());

    underTest.read(parameters);

    assertThat(underTest.getSearchResultsLimit(), is(50));
  }

  @Test
  void testRead_illegalArgumentException_wrappedAsValidationException() {
    StoreLoadParameters parameters = new StoreLoadParameters();
    parameters.setLimit(25);
    parameters.setFilter(List.of(
        new Filter()
        {
          {
            setProperty("keyword");
            setValue("test");
          }
        }));

    when(searchService.search(any(SearchRequest.class))).thenThrow(new IllegalArgumentException("bad query"));

    assertThrows(ValidationException.class, () -> underTest.read(parameters));
  }

  @Test
  void testRead_keywordWithQuotes_strippedForSearch() {
    StoreLoadParameters parameters = new StoreLoadParameters();
    parameters.setLimit(25);
    parameters.setFilter(List.of(
        new Filter()
        {
          {
            setProperty("keyword");
            setValue("\"exact-match\"");
          }
        }));

    SearchResponse searchResponse = new SearchResponse();
    searchResponse.setTotalHits(0L);
    when(searchService.search(any(SearchRequest.class))).thenReturn(searchResponse);
    when(searchResultsGenerator.getSearchResultList(searchResponse)).thenReturn(Collections.emptyList());

    LimitedPagedResponse<ComponentXO> result = underTest.read(parameters);

    assertThat(result, is(notNullValue()));
    verify(eventManager).post(any(SearchEvent.class));
  }

  @Test
  void testRead_formatSearchWithAdditionalCriteria_performsSearch() {
    StoreLoadParameters parameters = new StoreLoadParameters();
    parameters.setLimit(25);
    parameters.setFormatSearch(true);
    parameters.setFilter(List.of(
        new Filter()
        {
          {
            setProperty("format");
            setValue("maven2");
          }
        },
        new Filter()
        {
          {
            setProperty("keyword");
            setValue("test");
          }
        }));

    SearchResponse searchResponse = new SearchResponse();
    searchResponse.setTotalHits(0L);
    when(searchService.search(any(SearchRequest.class))).thenReturn(searchResponse);
    when(searchResultsGenerator.getSearchResultList(searchResponse)).thenReturn(Collections.emptyList());

    LimitedPagedResponse<ComponentXO> result = underTest.read(parameters);

    assertThat(result, is(notNullValue()));
    verify(searchService).search(any(SearchRequest.class));
  }

  @Test
  void testGetSearchResultsLimit() {
    assertThat(underTest.getSearchResultsLimit(), is(1000));
  }

  @Test
  void testSetSearchResultsLimit() {
    underTest.setSearchResultsLimit(500);
    assertThat(underTest.getSearchResultsLimit(), is(500));
  }

  @Test
  void testRead_withNullSort() {
    StoreLoadParameters parameters = new StoreLoadParameters();
    parameters.setLimit(25);
    parameters.setSort(null);
    parameters.setFilter(List.of(
        new Filter()
        {
          {
            setProperty("keyword");
            setValue("test");
          }
        }));

    SearchResponse searchResponse = new SearchResponse();
    searchResponse.setTotalHits(0L);
    when(searchService.search(any(SearchRequest.class))).thenReturn(searchResponse);
    when(searchResultsGenerator.getSearchResultList(searchResponse)).thenReturn(Collections.emptyList());

    LimitedPagedResponse<ComponentXO> result = underTest.read(parameters);

    assertThat(result, is(notNullValue()));
  }

  @Test
  void testRead_withNullPage_offsetDefaultsToZero() {
    StoreLoadParameters parameters = new StoreLoadParameters();
    parameters.setLimit(25);
    parameters.setPage(null);
    parameters.setFilter(List.of(
        new Filter()
        {
          {
            setProperty("keyword");
            setValue("test");
          }
        }));

    SearchResponse searchResponse = new SearchResponse();
    searchResponse.setTotalHits(0L);
    when(searchService.search(any(SearchRequest.class))).thenReturn(searchResponse);
    when(searchResultsGenerator.getSearchResultList(searchResponse)).thenReturn(Collections.emptyList());

    LimitedPagedResponse<ComponentXO> result = underTest.read(parameters);

    assertThat(result, is(notNullValue()));
  }
}
