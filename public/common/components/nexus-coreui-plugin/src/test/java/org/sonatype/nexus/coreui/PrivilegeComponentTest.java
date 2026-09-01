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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sonatype.nexus.extdirect.model.PagedResponse;
import org.sonatype.nexus.extdirect.model.StoreLoadParameters;
import org.sonatype.nexus.extdirect.model.StoreLoadParameters.Filter;
import org.sonatype.nexus.extdirect.model.StoreLoadParameters.Sort;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.privilege.PrivilegeDescriptor;
import org.sonatype.nexus.security.privilege.WildcardPrivilegeDescriptor;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;

import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;

import static com.google.common.collect.Lists.reverse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(AuthenticationExtension.class)
@ExtendWith(ValidationExtension.class)
@WithUser
class PrivilegeComponentTest
{
  @Mock
  private SecuritySystem securitySystem;

  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private AuthorizationManager authorizationManager;

  private List<PrivilegeDescriptor> privilegeDescriptors = new ArrayList<>();

  private PrivilegeComponent underTest;

  @BeforeEach
  void setup() {
    underTest = new PrivilegeComponent(securitySystem, securityHelper, privilegeDescriptors);
  }

  @Test
  void testExtractPageWithNoResults() {
    PagedResponse<PrivilegeXO> page =
        underTest.extractPage(parameters(0, 100, sort(), filter("test")), new ArrayList<>());
    assertThat(page.getTotal(), is(0L));
    assertThat(page.getData(), empty());
  }

  @Test
  void testCanFilterResultsByNameDescriptionPermissionOrType() {
    List<PrivilegeXO> privileges = new ArrayList<>(Arrays.asList(
        privilege("a", "b", "c", "d"),
        privilege("w", "x", "y", "z")));

    PagedResponse<PrivilegeXO> page =
        underTest.extractPage(parameters(0, 100, sort(), filter("a")), privileges);
    assertThat(page.getTotal(), is(1L));
    assertThat(page.getData(), contains(privileges.get(0)));

    page = underTest.extractPage(parameters(0, 100, sort(), filter("x")), privileges);
    assertThat(page.getTotal(), is(1L));
    assertThat(page.getData(), contains(privileges.get(1)));

    page = underTest.extractPage(parameters(0, 100, sort(), filter("c")), privileges);
    assertThat(page.getTotal(), is(1L));
    assertThat(page.getData(), contains(privileges.get(0)));

    page = underTest.extractPage(parameters(0, 100, sort(), filter("z")), privileges);
    assertThat(page.getTotal(), is(1L));
    assertThat(page.getData(), contains(privileges.get(1)));

    page = underTest.extractPage(parameters(0, 100, sort(), null), privileges);
    assertThat(page.getTotal(), is(2L));
  }

  @Test
  void testCanSortOnAvailableFields() {
    List<PrivilegeXO> privileges =
        Arrays.asList(
            privilege("a", "b", "c", "d"),
            privilege("b", "c", "d", "a"),
            privilege("c", "d", "a", "b"));

    PagedResponse<PrivilegeXO> page = underTest.extractPage(parameters(0, 100, sort(), null), privileges);
    assertThat(page.getData(), is(privileges));

    page = underTest.extractPage(parameters(0, 100, sort("name", "DESC"), null), privileges);
    assertThat(page.getData(), is(reverse(sortPrivilegesBy(privileges, Comparator.comparing(PrivilegeXO::getName)))));

    page = underTest.extractPage(parameters(0, 100, sort("description"), null), privileges);
    assertThat(page.getData(), is(sortPrivilegesBy(privileges, Comparator.comparing(PrivilegeXO::getDescription))));

    page = underTest.extractPage(parameters(0, 100, sort("description", "DESC"), null), privileges);
    assertThat(page.getData(),
        is(reverse(sortPrivilegesBy(privileges, Comparator.comparing(PrivilegeXO::getDescription)))));

    page = underTest.extractPage(parameters(0, 100, sort("permission"), null), privileges);
    assertThat(page.getData(), is(sortPrivilegesBy(privileges, Comparator.comparing(PrivilegeXO::getPermission))));

    page = underTest.extractPage(parameters(0, 100, sort("permission", "DESC"), null), privileges);
    assertThat(page.getData(),
        is(reverse(sortPrivilegesBy(privileges, Comparator.comparing(PrivilegeXO::getPermission)))));

    page = underTest.extractPage(parameters(0, 100, sort("type"), null), privileges);
    assertThat(page.getData(), is(sortPrivilegesBy(privileges, Comparator.comparing(PrivilegeXO::getType))));

    page = underTest.extractPage(parameters(0, 100, sort("type", "DESC"), null), privileges);
    assertThat(page.getData(), is(reverse(sortPrivilegesBy(privileges, Comparator.comparing(PrivilegeXO::getType)))));
  }

  @Test
  void testCanPageResults() {
    List<PrivilegeXO> privileges =
        Arrays.asList(privilege("a", "a", "a", "a"), privilege("b", "b", "b", "b"), privilege("c", "c", "c", "c"));

    PagedResponse<PrivilegeXO> page = underTest.extractPage(parameters(0, 2, sort(), null), privileges);
    assertThat(page.getTotal(), is(3L));
    assertThat(page.getData(), is(Arrays.asList(privileges.get(0), privileges.get(1))));

    page = underTest.extractPage(parameters(2, 50, sort(), null), privileges);
    assertThat(page.getTotal(), is(3L));
    assertThat(page.getData(), is(Collections.singletonList(privileges.get(2))));

    try {
      underTest.extractPage(parameters(4, 2, sort(), null), privileges);
    }
    catch (IllegalArgumentException e) {
      assertThat(e, is(instanceOf(IllegalArgumentException.class)));
    }
  }

  @Test
  void testConvertPrivilegeWithNullPermission() {
    // Simulate an orphaned privilege whose type has no registered PrivilegeDescriptor.
    // AuthorizationManagerImpl.convert() only sets permission when a descriptor is found,
    // so getPermission() returns null for unknown types.
    Privilege privilege = new Privilege("id", "name", "description", "unknown-type", Collections.emptyMap(), false);

    PrivilegeXO xo = underTest.convert(privilege);

    assertThat(xo.getPermission(), is("<unknown>"));
  }

  @Test
  void testExtractPageFilterWithNullPermissionPrivilege() {
    List<PrivilegeXO> privileges = new ArrayList<>(Arrays.asList(
        new PrivilegeXO().withName("normal").withDescription("desc").withPermission("some:perm").withType("wildcard"),
        new PrivilegeXO().withName("orphan")
            .withDescription("desc")
            .withPermission("<unknown>")
            .withType("unknown-type"),
        // raw-null permission exercises the null guard in extractPage() directly
        new PrivilegeXO().withName("raw-null").withDescription("desc").withPermission(null).withType("unknown-type")));

    // filtering should not NPE when a privilege has an unknown permission
    PagedResponse<PrivilegeXO> page =
        underTest.extractPage(parameters(0, 100, sort(), filter("normal")), privileges);
    assertThat(page.getTotal(), is(1L));
    assertThat(page.getData(), contains(privileges.get(0)));

    // the orphaned privilege should be included when the filter matches another field
    page = underTest.extractPage(parameters(0, 100, sort(), filter("orphan")), privileges);
    assertThat(page.getTotal(), is(1L));
    assertThat(page.getData(), contains(privileges.get(1)));

    // the raw-null privilege should be included when the filter matches another field, and must not NPE
    page = underTest.extractPage(parameters(0, 100, sort(), filter("raw-null")), privileges);
    assertThat(page.getTotal(), is(1L));
    assertThat(page.getData(), contains(privileges.get(2)));
  }

  @Test
  void testCanLoadListOfPrivilegeReferences() {
    Set<Privilege> privileges = Set.of(privilege("a"), privilege("b"), privilege("c"));

    when(securitySystem.listPrivileges()).thenReturn(privileges);

    List<ReferenceXO> references = underTest.readReferences();
    assertThat(references, containsInAnyOrder(
        new ReferenceXO("a", "a"),
        new ReferenceXO("b", "b"),
        new ReferenceXO("c", "c")));
  }

  @Test
  void testUpdate_forbiddenWhenCallerCannotGrantTheNewPattern() throws Exception {
    privilegeDescriptors.add(new WildcardPrivilegeDescriptor());
    underTest = new PrivilegeComponent(securitySystem, securityHelper, privilegeDescriptors);

    when(securitySystem.getAuthorizationManager("default")).thenReturn(authorizationManager);
    doThrow(new AuthorizationException("not permitted"))
        .when(securityHelper)
        .ensurePermitted(any(Permission.class));

    PrivilegeXO xo = wildcardPrivilegeXO("priv", "nexus:*");

    assertThrows(AuthorizationException.class, () -> underTest.update(xo));
    verify(authorizationManager, never()).updatePrivilege(any(Privilege.class));
  }

  @Test
  void testCreate_forbiddenWhenCallerCannotGrantTheNewPattern() throws Exception {
    privilegeDescriptors.add(new WildcardPrivilegeDescriptor());
    underTest = new PrivilegeComponent(securitySystem, securityHelper, privilegeDescriptors);

    when(securitySystem.getAuthorizationManager("default")).thenReturn(authorizationManager);
    doThrow(new AuthorizationException("not permitted"))
        .when(securityHelper)
        .ensurePermitted(any(Permission.class));

    PrivilegeXO xo = wildcardPrivilegeXO("priv", "nexus:*");

    assertThrows(AuthorizationException.class, () -> underTest.create(xo));
    verify(authorizationManager, never()).addPrivilege(any(Privilege.class));
  }

  @Test
  void testUpdate_allowedWhenCallerHoldsThePattern() throws Exception {
    privilegeDescriptors.add(new WildcardPrivilegeDescriptor());
    underTest = new PrivilegeComponent(securitySystem, securityHelper, privilegeDescriptors);

    lenient().when(securitySystem.getAuthorizationManager("default")).thenReturn(authorizationManager);
    when(authorizationManager.updatePrivilege(any(Privilege.class)))
        .thenAnswer(invocation -> {
          Privilege returned = invocation.getArgument(0);
          returned.setPermission(new WildcardPermission("nexus:health:read"));
          return returned;
        });

    PrivilegeXO xo = wildcardPrivilegeXO("priv", "nexus:health:read");

    underTest.update(xo);

    verify(securityHelper).ensurePermitted(any(Permission.class));
    verify(authorizationManager).updatePrivilege(any(Privilege.class));
  }

  @Test
  void testUpdate_unknownTypeIsRejectedRatherThanBypassingCheck() throws Exception {
    privilegeDescriptors.add(new WildcardPrivilegeDescriptor());
    underTest = new PrivilegeComponent(securitySystem, securityHelper, privilegeDescriptors);

    Map<String, String> properties = new HashMap<>();
    properties.put(WildcardPrivilegeDescriptor.P_PATTERN, "nexus:*");
    PrivilegeXO xo = new PrivilegeXO()
        .withId("priv")
        .withVersion("0")
        .withName("priv")
        .withDescription("priv")
        .withType("nonexistent-type")
        .withProperties(properties);

    assertThrows(AuthorizationException.class, () -> underTest.update(xo));
    verify(authorizationManager, never()).updatePrivilege(any(Privilege.class));
    verify(securityHelper, never()).ensurePermitted(any(Permission.class));
  }

  private static PrivilegeXO wildcardPrivilegeXO(final String name, final String pattern) {
    Map<String, String> properties = new HashMap<>();
    properties.put(WildcardPrivilegeDescriptor.P_PATTERN, pattern);
    return new PrivilegeXO()
        .withId(name)
        .withVersion("0")
        .withName(name)
        .withDescription(name)
        .withType(WildcardPrivilegeDescriptor.TYPE)
        .withProperties(properties);
  }

  private static Privilege privilege(final String text) {
    return new Privilege(text, text, text, text, Collections.emptyMap(), false);
  }

  private static PrivilegeXO privilege(
      final String name,
      final String description,
      final String permission,
      final String type)
  {
    return new PrivilegeXO().withName(name).withDescription(description).withPermission(permission).withType(type);
  }

  private static StoreLoadParameters parameters(
      final int start,
      final int limit,
      final Sort sort,
      final Filter filter)
  {
    List<Filter> filters = filter != null ? List.of(filter) : Collections.emptyList();
    List<Sort> sorts = sort != null ? List.of(sort) : Collections.emptyList();
    return new StoreLoadParameters().start(start).limit(limit).filters(filters).sort(sorts);
  }

  private static Filter filter(final String value) {
    return new Filter().property("filter").value(value);
  }

  private static Sort sort(final String property, final String direction) {
    return new Sort(property, direction);
  }

  private static Sort sort() {
    return sort("name", "ASC");
  }

  private static Sort sort(final String property) {
    return sort(property, "ASC");
  }

  private static List<PrivilegeXO> sortPrivilegesBy(
      final List<PrivilegeXO> originalList,
      final Comparator<PrivilegeXO> comparator)
  {
    List<PrivilegeXO> sortedList = new ArrayList<>(originalList);
    sortedList.sort(comparator);
    return sortedList;
  }
}
