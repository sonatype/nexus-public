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

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.selector.CselSelector;
import org.sonatype.nexus.selector.CselToSql;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorConfigurationStore;
import org.sonatype.nexus.selector.SelectorFactory;
import org.sonatype.nexus.selector.SelectorManager;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Tests {@link SelectorComponent}.
 */
@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class SelectorComponentTest
    extends Test5Support
{
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
    xo.setExpression("a ==== b");
    xo.setType(CselSelector.TYPE);

    // doThrow(new ConstraintViolationException("Invalid CSEL expression", emptySet()))
    // .when(selectorFactory).validateSelector(eq(CselSelector.TYPE), anyString());
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

  private SelectorConfiguration selector(final String name) {
    SelectorConfiguration selector = mock(SelectorConfiguration.class);
    when(selector.getName()).thenReturn(name);
    return selector;
  }
}
