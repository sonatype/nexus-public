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
package org.sonatype.nexus.repository.rest.internal.resources;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.validation.ConstraintViolationException;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.rest.api.ContentSelectorApiCreateRequest;
import org.sonatype.nexus.repository.rest.api.ContentSelectorApiResponse;
import org.sonatype.nexus.repository.rest.api.ContentSelectorApiUpdateRequest;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.selector.CselSelector;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorConfigurationStore;
import org.sonatype.nexus.selector.SelectorFactory;
import org.sonatype.nexus.selector.SelectorManager;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_FOUND;
import static org.sonatype.nexus.selector.SelectorConfiguration.EXPRESSION;

@Ignore("NEXUS-43375")
public class ContentSelectorsApiResourceTest
    extends TestSupport
{
  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

  private ContentSelectorsApiResource underTest;

  @Mock
  private SelectorFactory selectorFactory;

  @Mock
  private SelectorManager selectorManager;

  @Mock
  private SelectorConfigurationStore store;

  @Mock
  private EventManager eventManager;

  @Before
  public void setup() {
    underTest = new ContentSelectorsApiResource(selectorFactory, selectorManager, store, eventManager);
  }

  @Test
  public void getContentSelectorsConvertsTheResult() {
    SelectorConfiguration selectorConfiguration = new TestContentSelector();
    selectorConfiguration.setName("name");
    selectorConfiguration.setType("csel");
    selectorConfiguration.setDescription("description");
    selectorConfiguration.setAttributes(singletonMap("expression", "test-expression"));
    when(store.browse()).thenReturn(asList(selectorConfiguration));

    List<ContentSelectorApiResponse> response = underTest.getContentSelectors();

    assertThat(response.get(0).getName(), is("name"));
    assertThat(response.get(0).getType(), is("csel"));
    assertThat(response.get(0).getDescription(), is("description"));
    assertThat(response.get(0).getExpression(), is("test-expression"));
  }

  @Test
  public void createContentSelectorValidatesTheExpression() {
    ContentSelectorApiCreateRequest request = new ContentSelectorApiCreateRequest();
    request.setName("name");
    request.setExpression("invalid-expression");
    doThrow(new ConstraintViolationException(emptySet())).when(selectorFactory)
        .validateSelector(CselSelector.TYPE, request.getExpression());

    exceptionRule.expect(ConstraintViolationException.class);
    underTest.createContentSelector(request);
  }

  @Test
  public void createContentSelectorCreatesAValidSelector() {
    ContentSelectorApiCreateRequest request = new ContentSelectorApiCreateRequest();
    request.setName("name");
    request.setExpression("format == \"maven2\"");

    SelectorConfiguration expected = new TestContentSelector();
    expected.setName(request.getName());
    expected.setType(CselSelector.TYPE);
    expected.setAttributes(singletonMap(EXPRESSION, request.getExpression()));
    when(selectorManager.findByName(expected.getName())).thenReturn(Optional.of(expected));

    underTest.createContentSelector(request);
    verify(selectorManager).create("name", CselSelector.TYPE, null, singletonMap(EXPRESSION, "format == \"maven2\""));
  }

  @Test
  public void getContentSelectorFindsSelectorByName() {
    SelectorConfiguration matchingSelector = new TestContentSelector();
    matchingSelector.setName("test");
    matchingSelector.setAttributes(emptyMap());
    when(selectorManager.findByName(matchingSelector.getName())).thenReturn(Optional.of(matchingSelector));

    ContentSelectorApiResponse response = underTest.getContentSelector(matchingSelector.getName());

    assertThat(response.getName(), is(matchingSelector.getName()));
  }

  @Test
  public void getContentSelectorThrowNotFoundForSelectorNotFound() {
    when(selectorManager.findByName(any())).thenReturn(Optional.empty());

    exceptionRule.expect(WebApplicationMessageException.class);
    exceptionRule.expect(hasProperty("response", hasProperty("status", is(NOT_FOUND))));

    underTest.getContentSelector("any");
  }

  @Test
  public void updateContentSelectorThrowsBadRequestForInvalidExpression() throws Exception {
    ContentSelectorApiUpdateRequest request = new ContentSelectorApiUpdateRequest();
    request.setExpression("invalid-expression");

    SelectorConfiguration selectorConfiguration = mock(SelectorConfiguration.class);
    when(selectorConfiguration.getType()).thenReturn(CselSelector.TYPE);
    when(selectorManager.findByName("any")).thenReturn(Optional.of(selectorConfiguration));

    doThrow(new ConstraintViolationException("", emptySet())).when(selectorFactory)
        .validateSelector(CselSelector.TYPE, request.getExpression());

    exceptionRule.expect(ConstraintViolationException.class);

    underTest.updateContentSelector("any", request);
  }

  @Test
  public void updateContentSelectorThrowsNotFoundForSelectorNotFound() {
    ContentSelectorApiUpdateRequest request = new ContentSelectorApiUpdateRequest();
    request.setExpression("format == \"maven2\"");

    when(selectorManager.findByName(any())).thenReturn(Optional.empty());

    exceptionRule.expect(WebApplicationMessageException.class);
    exceptionRule.expect(hasProperty("response", hasProperty("status", is(NOT_FOUND))));

    underTest.updateContentSelector("any", request);
  }

  @Test
  public void updateContentSelectorUpdatesSelector() {
    ContentSelectorApiUpdateRequest request = new ContentSelectorApiUpdateRequest();
    request.setDescription("description");
    request.setExpression("format == \"maven2\"");

    SelectorConfiguration selector = new TestContentSelector();
    selector.setName("test");
    selector.setType(CselSelector.TYPE);
    when(selectorManager.findByName(selector.getName())).thenReturn(Optional.of(selector));

    underTest.updateContentSelector(selector.getName(), request);

    ArgumentCaptor<SelectorConfiguration> configurationCaptor = ArgumentCaptor.forClass(SelectorConfiguration.class);
    verify(selectorManager).update(configurationCaptor.capture());
    assertThat(configurationCaptor.getValue().getType(), is(CselSelector.TYPE));
    assertThat(configurationCaptor.getValue().getDescription(), is(request.getDescription()));
    assertThat(configurationCaptor.getValue().getAttributes().get(EXPRESSION), is(request.getExpression()));
  }

  @Test
  public void deleteContentSelectorThrowsNotFoundForSelectorNotFound() {
    ContentSelectorApiUpdateRequest request = new ContentSelectorApiUpdateRequest();
    request.setExpression("format == \"maven2\"");

    SelectorConfiguration selector = new TestContentSelector();
    selector.setName("any");

    when(selectorManager.findByName(any())).thenReturn(Optional.empty());

    exceptionRule.expect(WebApplicationMessageException.class);
    exceptionRule.expect(hasProperty("response", hasProperty("status", is(NOT_FOUND))));

    underTest.deleteContentSelector(selector.getName());
  }

  @Test
  public void deleteContentSelectorSucceeds() {
    SelectorConfiguration selector = new TestContentSelector();
    selector.setName("test");
    when(selectorManager.findByName(selector.getName())).thenReturn(Optional.of(selector));

    underTest.deleteContentSelector(selector.getName());

    verify(selectorManager).delete(selector);
  }

  private static class TestContentSelector implements SelectorConfiguration{

    private String name;
    private String type;
    private String description;
    @Override
    public String getName() {
      return this.name;
    }

    @Override
    public void setName(final String name) {
      this.name = name;

    }

    @Override
    public String getType() {
      return this.type;
    }

    @Override
    public void setType(final String type) {
        this.type = type;
    }

    @Override
    public String getDescription() {
      return this.description;
    }

    @Override
    public void setDescription(final String description) {
      this.description = description;
    }

    @Override
    public Map<String, String> getAttributes() {
      return Collections.emptyMap();
    }

    @Override
    public void setAttributes(final Map<String, ?> attributes) {

    }
  }
}
