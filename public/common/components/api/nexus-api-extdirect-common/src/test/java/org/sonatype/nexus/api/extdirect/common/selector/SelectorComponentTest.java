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
package org.sonatype.nexus.api.extdirect.common.selector;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sonatype.nexus.api.common.shared.security.model.ReferenceXO;
import org.sonatype.nexus.api.extdirect.common.selector.model.SelectorXO;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.selector.CselSelector;
import org.sonatype.nexus.selector.CselToSql;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorConfigurationStore;
import org.sonatype.nexus.selector.SelectorFactory;
import org.sonatype.nexus.selector.SelectorManager;
import org.sonatype.nexus.selector.UniqueSelectorNameValidator;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;
import org.sonatype.nexus.testcommon.validation.ValidationExtension.RegisterValidator;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link SelectorComponent}.
 */
@ExtendWith(MockitoExtension.class)
@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class SelectorComponentTest
{
  @RegisterValidator
  @Mock
  UniqueSelectorNameValidator nameValidator;

  @Mock
  private ConstraintViolation constraintViolation;

  @Mock
  private ConstraintViolationFactory constraintViolationFactory;

  @Mock
  private Path path;

  @Mock
  private SelectorManager mockSelectorManager;

  @Mock
  private SelectorConfigurationStore mockStore;

  @Mock
  private SelectorConfiguration selectorConfiguration;

  @Mock
  private Subject subject;

  @Mock
  private SecurityManager securityManager;

  @Mock
  private SecuritySystem securitySystem;

  private MockedStatic<SecurityUtils> securityUtils;

  private SelectorComponent underTest;

  @BeforeEach
  void setup() {
    lenient().when(nameValidator.isValid(any(), any())).thenReturn(true);
    securityUtils = mockStatic(SecurityUtils.class);
    securityUtils.when(SecurityUtils::getSubject).thenReturn(subject);

    lenient().when(constraintViolationFactory.createViolation(eq("expression"), anyString()))
        .thenReturn(constraintViolation);
    lenient().when(constraintViolation.getPropertyPath()).thenReturn(path);

    lenient().when(mockSelectorManager.newSelectorConfiguration(any(), any(), any(), any()))
        .thenReturn(selectorConfiguration);
    lenient().when(mockSelectorManager.readByName(any())).thenReturn(selectorConfiguration);

    SelectorFactory selectorFactory = new SelectorFactory(constraintViolationFactory, mock(CselToSql.class));

    underTest = new SelectorComponent(mockSelectorManager, constraintViolationFactory, selectorFactory, securitySystem,
        mockStore);
  }

  @AfterEach
  void tearDown() {
    securityUtils.close();
  }

  @Test
  void testCreateJexl_invalidExpression() {
    SelectorXO xo = new SelectorXO();
    xo.setName("foo");
    xo.setExpression("a ==== b");
    xo.setType("jexl");

    try {
      underTest.create(xo);
      fail();
    }
    catch (ConstraintViolationException e) {
      assertThat(e.getConstraintViolations().size(), is(1));
      assertThat(e.getConstraintViolations().iterator().next().getPropertyPath(), is(path));
    }
  }

  @Test
  void testCreateCsel_invalidExpression() {
    SelectorXO xo = new SelectorXO();
    xo.setName("foo");
    xo.setExpression("a ==== b");
    xo.setType(CselSelector.TYPE);

    try {
      underTest.create(xo);
      fail();
    }
    catch (ConstraintViolationException e) {
      assertThat(e.getConstraintViolations().size(), is(1));
      assertThat(e.getConstraintViolations().iterator().next().getPropertyPath(), is(path));
    }
  }

  @Test
  void testUpdateJexl_invalidExpression() {
    SelectorXO xo = new SelectorXO();
    xo.setName("foo");
    xo.setId("1");
    xo.setExpression("a ==== b");
    xo.setType("jexl");

    try {
      underTest.update(xo);
      fail();
    }
    catch (ConstraintViolationException e) {
      assertThat(e.getConstraintViolations().size(), is(1));
      assertThat(e.getConstraintViolations().iterator().next().getPropertyPath(), is(path));
    }
  }

  @Test
  void testUpdateCsel_invalidExpression() {
    SelectorXO xo = new SelectorXO();
    xo.setName("foo");
    xo.setId("1");
    xo.setExpression("a ==== b");
    xo.setId("1");
    xo.setType(CselSelector.TYPE);

    ConstraintViolationException e = assertThrows(ConstraintViolationException.class, () -> underTest.update(xo));
    assertThat(e.getConstraintViolations().size(), is(1));
    assertThat(e.getConstraintViolations().iterator().next().getPropertyPath(), is(path));
  }

  @Test
  void testDelete_blobStoreInUse() {
    when(mockSelectorManager.readByName(any())).thenReturn(mock(SelectorConfiguration.class));
    doThrow(new IllegalStateException("a message")).when(mockSelectorManager).delete(any());
    when(constraintViolationFactory.createViolation("*", "a message")).thenReturn(constraintViolation);

    assertThrows(ConstraintViolationException.class, () -> underTest.remove("someSelector"), "a message");
  }

  @Test
  void testReadReferences_sortedAlphabeticallyIgnoringCase() {
    List<SelectorConfiguration> selectors = List.of(
        selector("zeta"),
        selector("Alpha"),
        selector("alpha"),
        selector("beta"),
        selector("ALPHA"));
    when(mockSelectorManager.browse()).thenReturn(selectors);

    List<ReferenceXO> result = underTest.readReferences();

    assertThat(result.stream().map(ReferenceXO::getName).toList(),
        is(List.of("ALPHA", "Alpha", "alpha", "beta", "zeta")));
  }

  @Test
  void testReadReferences_returnsEmptyWhenNoSelectors() {
    when(mockSelectorManager.browse()).thenReturn(List.of());

    assertThat(underTest.readReferences().isEmpty(), is(true));
  }

  @Test
  void testRead_returnsMappedSelectors() {
    when(subject.isPermitted("nexus:privileges:read")).thenReturn(true);
    when(securitySystem.listPrivileges()).thenReturn(Set.of());
    when(selectorConfiguration.getName()).thenReturn("sel1");
    when(selectorConfiguration.getType()).thenReturn("csel");
    when(selectorConfiguration.getDescription()).thenReturn("my selector");
    when(selectorConfiguration.getAttributes()).thenReturn(Map.of("expression", "format == \"maven2\""));
    when(mockStore.browse()).thenReturn(List.of(selectorConfiguration));

    List<SelectorXO> result = underTest.read();

    assertThat(result.size(), is(1));
    SelectorXO xo = result.get(0);
    assertThat(xo.getId(), is("sel1"));
    assertThat(xo.getName(), is("sel1"));
    assertThat(xo.getType(), is("csel"));
    assertThat(xo.getDescription(), is("my selector"));
    assertThat(xo.getExpression(), is("format == \"maven2\""));
    assertThat(xo.getUsedByCount(), is(0));
    assertThat(xo.getUsedBy().isEmpty(), is(true));
  }

  @Test
  void testCreate_success() {
    when(subject.isPermitted("nexus:privileges:read")).thenReturn(true);
    when(securitySystem.listPrivileges()).thenReturn(Set.of());
    when(selectorConfiguration.getName()).thenReturn("sel1");
    when(selectorConfiguration.getType()).thenReturn("jexl");
    when(selectorConfiguration.getDescription()).thenReturn("my selector");
    when(selectorConfiguration.getAttributes()).thenReturn(Map.of("expression", "format == \"maven2\""));

    SelectorXO input = new SelectorXO();
    input.setName("sel1");
    input.setType("jexl");
    input.setDescription("my selector");
    input.setExpression("format == \"maven2\"");

    SelectorXO result = underTest.create(input);

    verify(mockSelectorManager).create(selectorConfiguration);
    assertThat(result.getName(), is("sel1"));
    assertThat(result.getType(), is("jexl"));
    assertThat(result.getExpression(), is("format == \"maven2\""));
  }

  private SelectorConfiguration selector(final String name) {
    SelectorConfiguration selector = mock(SelectorConfiguration.class);
    when(selector.getName()).thenReturn(name);
    return selector;
  }
}
