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
package org.sonatype.nexus.repository.routing.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;
import org.sonatype.nexus.repository.routing.RoutingMode;
import org.sonatype.nexus.repository.routing.RoutingRule;
import org.sonatype.nexus.repository.routing.RoutingRulesConfiguration;
import org.sonatype.nexus.rest.ValidationErrorXO;
import org.sonatype.nexus.rest.ValidationErrorsException;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class RoutingRuleStoreImplTest extends TestSupport
{
  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory("test");

  private RoutingRuleStoreImpl underTest;

  @Before
  public void setUp() throws Exception {
    RoutingRuleEntityAdapter routingRuleEntityAdapter = new RoutingRuleEntityAdapter();
    RoutingRulesConfiguration configuration = new RoutingRulesConfiguration(true);
    underTest = new RoutingRuleStoreImpl(database.getInstanceProvider(), routingRuleEntityAdapter, configuration);
  }

  @Test
  public void testCreate() throws Exception {
    underTest.start();
    createRoutingRule("asdf", "asdf2");

    RoutingRule rule = underTest.getByName("asdf");
    assertThat(rule.name(), is("asdf"));
    assertThat(rule.description(), is("some description"));
    assertThat(rule.mode(), is(RoutingMode.BLOCK));
    assertThat(rule.matchers(), contains("asdf2"));
  }

  @Test
  public void testUpdate() throws Exception {
    underTest.start();
    RoutingRule routingRule = createRoutingRule("asdf", "asdf");
    routingRule.matchers(Arrays.asList("asdf2"));
    underTest.update(routingRule);

    routingRule = underTest.getByName("asdf");
    assertThat(routingRule.name(), is("asdf"));
    assertThat(routingRule.mode(), is(RoutingMode.BLOCK));
    assertThat(routingRule.matchers(), contains("asdf2"));
  }

  @Test
  public void testList() throws Exception {
    underTest.start();
    createRoutingRule("asdf1", "asdf1");
    createRoutingRule("asdf2", "asdf2");
    createRoutingRule("asdf3", "asdf3");

    List<RoutingRule> matchers = underTest.list();
    assertThat(matchers.size(), is(3));
  }

  @Test
  public void testDelete() throws Exception {
    underTest.start();
    RoutingRule rule = createRoutingRule("asdf", "asdf2");
    assertThat(underTest.list(), hasSize(1));

    underTest.delete(rule);

    assertThat(underTest.list(), hasSize(0));
  }

  @Test
  public void testGetByName() throws Exception {
    underTest.start();
    createRoutingRule("foo", "foo");
    createRoutingRule("bar", "bar");

    RoutingRule rule = underTest.getByName("foo");
    assertThat(rule.name(), is("foo"));
  }

  @Test
  public void testStart_disabled() throws Exception {
    RoutingRuleEntityAdapter routingRuleEntityAdapter = new RoutingRuleEntityAdapter();
    RoutingRulesConfiguration configuration = new RoutingRulesConfiguration(false);
    underTest = new RoutingRuleStoreImpl(database.getInstanceProvider(), routingRuleEntityAdapter, configuration);
    underTest.start();

    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      OSchema schema = db.getMetadata().getSchema();
      OClass type = schema.getClass(RoutingRuleEntityAdapter.DB_CLASS);
      assertNull(type);
    }
  }

  @Test
  public void testValidate_name() {
    validate(new RoutingRule(null, "desc", RoutingMode.BLOCK, Collections.singletonList(".*")), "name",
        "A non-empty value must be specified");

    validate(new RoutingRule("\t", "desc", RoutingMode.BLOCK, Collections.singletonList(".*")), "name",
        "A non-empty value must be specified");

    validate(new RoutingRule("asdf asdf", "desc", RoutingMode.BLOCK, Collections.singletonList(".*")), "name",
        "Only letters, digits, underscores(_), hyphens(-), and dots(.) are allowed and may not start with underscore or dot.");
  }

  @Test
  public void testValidate_desc() {
    validate(new RoutingRule("my-name", null, RoutingMode.BLOCK, Collections.singletonList(".*")), "description",
        "A non-null value must be specified");
  }

  @Test
  public void testValidate_matchers() {
    validate(new RoutingRule("my-name", "desc", RoutingMode.BLOCK, null), "matchers",
        "At least one rule must be specified");

    validate(new RoutingRule("my-name", "desc", RoutingMode.BLOCK, Collections.emptyList()), "matchers",
        "At least one rule must be specified");

    validate(new RoutingRule("my-name", "desc", RoutingMode.BLOCK, Collections.singletonList(null)), "matchers[0]",
        "Empty matchers are not allowed");

    validate(new RoutingRule("my-name", "desc", RoutingMode.BLOCK, Collections.singletonList("")), "matchers[0]",
        "Empty matchers are not allowed");

    validate(new RoutingRule("my-name", "desc", RoutingMode.BLOCK, Collections.singletonList("(.*")), "matchers[0]",
        String.format("Invalid regex: Unclosed group near index 3%n(.*"));
  }

  @Test
  public void testValidate_mode() {
    validate(new RoutingRule("name", "desc", null, Collections.singletonList(".*")), "mode",
        "A non-empty value must be specified");
  }

  @Test(expected = ValidationErrorsException.class)
  public void testValidate_onCreate() throws Exception {
    underTest.start();
    createRoutingRule("a    a", ".*");
  }

  @Test(expected = ValidationErrorsException.class)
  public void testValidate_onUpdate() throws Exception {
    underTest.start();
    RoutingRule rule = null;
    try {
      rule = createRoutingRule("name", "asdf");
      rule.name("a   a");
    }
    catch (Exception e) {
      fail("Unexpected exception occurred");
    }

    underTest.update(rule);
  }

  private RoutingRule createRoutingRule(final String name, final String rule) {
    RoutingRule testRoutingRule = new RoutingRule(name, "some description", RoutingMode.BLOCK,
        Collections.singletonList(rule));
    underTest.create(testRoutingRule);
    return testRoutingRule;
  }

  private static void validate(final RoutingRule rule, final String id, final String message) {
    try {
      RoutingRuleStoreImpl.validate(rule);
      fail("Expected exception");
    }
    catch (ValidationErrorsException e) {
      assertValidationException(e, id, message);
    }
  }

  private static void assertValidationException(final ValidationErrorsException e,
                                                final String id,
                                                final String message)
  {
    for (ValidationErrorXO error : e.getValidationErrors()) {
      if (id.equals(error.getId()) && message.equals(error.getMessage())) {
        return;
      }
    }
    fail("Unable to find matching exception with id:" + id + " message:" + message + "\n" + e.getMessage());
  }
}
