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
package org.sonatype.nexus.formfields;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

/**
 * {@link RepositoryCombobox} tests.
 */
public class RepositoryComboboxTest

{
  RepositoryCombobox underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new RepositoryCombobox("test");
  }

  @Test
  public void includeAnEntryForAllRepositories() {
    underTest.includeAnEntryForAllRepositories();

    assertThat(underTest.getStoreFilters(), nullValue());
    assertThat(underTest.getStoreApi(), is("coreui_Repository.readReferencesAddingEntryForAll"));
  }

  @Test
  public void testFormatFilters() {
    underTest.excludingAnyOfFormats("nuget", "npm");
    underTest.includingAnyOfFormats("maven", "docker");

    assertThat(underTest.getStoreFilters().get("format"), is("maven,docker,!nuget,!npm"));
  }

  @Test
  public void testVersionPolicyFilters() {
    underTest.excludingAnyOfVersionPolicies("RELEASE");
    underTest.includingAnyOfVersionPolicies("MIXED", "SNAPSHOT");

    assertThat(underTest.getStoreFilters().get("versionPolicies"), is("MIXED,SNAPSHOT,!RELEASE"));
  }

  @Test
  public void testVersionPolicyFilters_onlyExclude() {
    underTest.excludingAnyOfVersionPolicies("RELEASE", "MIXED");

    assertThat(underTest.getStoreFilters().get("versionPolicies"), is("!RELEASE,!MIXED"));
  }

  @Test
  public void testVersionPolicyFilters_onlyInclude() {
    underTest.includingAnyOfVersionPolicies("RELEASE", "MIXED");

    assertThat(underTest.getStoreFilters().get("versionPolicies"), is("RELEASE,MIXED"));
  }

  @Test
  public void testFormatFilters_noVersionPolicies() {
    underTest.excludingAnyOfFormats("nuget", "npm");
    underTest.includingAnyOfFormats("maven", "docker");

    assertThat(underTest.getStoreFilters().containsKey("versionPolicies"), is(false));
  }

  @Test
  public void testConstructors() {
    RepositoryCombobox byId = new RepositoryCombobox("id1");
    assertThat(byId.getId(), is("id1"));
    assertThat(byId.isRequired(), is(false));
    // id-only constructor pulls label/help text from the I18N message bundle @DefaultMessage values
    assertThat(byId.getLabel(), equalTo("Repository"));
    assertThat(byId.getHelpText(), equalTo("Select the repository."));
    assertThat(byId.getRegexValidation(), nullValue());
    assertThat(byId.getInitialValue(), nullValue());

    RepositoryCombobox byIdRequired = new RepositoryCombobox("id2", true);
    assertThat(byIdRequired.getId(), is("id2"));
    assertThat(byIdRequired.isRequired(), is(true));
    assertThat(byIdRequired.getLabel(), equalTo("Repository"));
    assertThat(byIdRequired.getHelpText(), equalTo("Select the repository."));
    assertThat(byIdRequired.getRegexValidation(), nullValue());
    assertThat(byIdRequired.getInitialValue(), nullValue());

    RepositoryCombobox full = new RepositoryCombobox("id3", "myLabel", "myHelp", true);
    assertThat(full.getId(), is("id3"));
    assertThat(full.getLabel(), equalTo("myLabel"));
    assertThat(full.getHelpText(), equalTo("myHelp"));
    assertThat(full.isRequired(), is(true));
    assertThat(full.getRegexValidation(), nullValue());
    assertThat(full.getInitialValue(), nullValue());

    RepositoryCombobox withRegex = new RepositoryCombobox("id4", "myLabel", "myHelp", false, ".*");
    assertThat(withRegex.getId(), is("id4"));
    assertThat(withRegex.getLabel(), equalTo("myLabel"));
    assertThat(withRegex.getHelpText(), equalTo("myHelp"));
    assertThat(withRegex.isRequired(), is(false));
    // Combobox's 5-arg constructor maps the 5th argument to initialValue, not regexValidation (see NEXUS-53405)
    assertThat(withRegex.getRegexValidation(), nullValue());
    assertThat(withRegex.getInitialValue(), equalTo(".*"));
  }

  @Test
  public void testDefaults_notDisabledNotReadOnly() {
    // No constructor in the chain enables disabled/readOnly; both default to false.
    assertThat(underTest.isDisabled(), is(false));
    assertThat(underTest.isReadOnly(), is(false));
  }

  @Test
  public void testGetType() {
    assertThat(underTest.getType(), is("combobox"));
  }

  @Test
  public void testGetIdAndNameMappingAreNull() {
    assertThat(underTest.getIdMapping(), nullValue());
    assertThat(underTest.getNameMapping(), nullValue());
  }

  @Test
  public void testGetAllowAutocomplete() {
    assertThat(underTest.getAllowAutocomplete(), is(true));
  }

  @Test
  public void testGetStoreApi_default() {
    assertThat(underTest.getStoreApi(), is("coreui_Repository.readReferences"));
  }

  @Test
  public void testGetStoreApi_allFormats() {
    underTest.includeEntriesForAllFormats();

    assertThat(underTest.getStoreApi(), is("coreui_Repository.readReferencesAddingEntriesForAllFormats"));
  }

  @Test
  public void testGetStoreApi_allFormatsTakesPrecedenceOverAllRepositories() {
    underTest.includeAnEntryForAllRepositories();
    underTest.includeEntriesForAllFormats();

    assertThat(underTest.getStoreApi(), is("coreui_Repository.readReferencesAddingEntriesForAllFormats"));
  }

  @Test
  public void testGetStoreApi_allFormatsPrecedence_independentOfCallOrder() {
    // includeEntriesForAllFormats wins regardless of which flag is set first
    underTest.includeEntriesForAllFormats();
    underTest.includeAnEntryForAllRepositories();

    assertThat(underTest.getStoreApi(), is("coreui_Repository.readReferencesAddingEntriesForAllFormats"));
  }

  @Test
  public void testFluentMethodsReturnSameInstance() {
    assertThat(underTest.includingAnyOfTypes("hosted"), sameInstance(underTest));
    assertThat(underTest.excludingAnyOfTypes("group"), sameInstance(underTest));
    assertThat(underTest.includingAnyOfFormats("maven"), sameInstance(underTest));
    assertThat(underTest.excludingAnyOfFormats("nuget"), sameInstance(underTest));
    assertThat(underTest.includingAnyOfVersionPolicies("RELEASE"), sameInstance(underTest));
    assertThat(underTest.excludingAnyOfVersionPolicies("SNAPSHOT"), sameInstance(underTest));
    assertThat(underTest.includingAnyOfFacets(String.class), sameInstance(underTest));
    assertThat(underTest.regardlessViewPermissions(), sameInstance(underTest));
    assertThat(underTest.includeAnEntryForAllRepositories(), sameInstance(underTest));
    assertThat(underTest.includeEntriesForAllFormats(), sameInstance(underTest));
  }

  @Test
  public void testTypeFilters() {
    underTest.includingAnyOfTypes("hosted", "proxy");
    underTest.excludingAnyOfTypes("group");

    assertThat(underTest.getStoreFilters().get("type"), is("hosted,proxy,!group"));
  }

  @Test
  public void testTypeFilters_onlyInclude() {
    underTest.includingAnyOfTypes("hosted", "proxy");

    assertThat(underTest.getStoreFilters().get("type"), is("hosted,proxy"));
  }

  @Test
  public void testTypeFilters_onlyExclude() {
    underTest.excludingAnyOfTypes("group", "proxy");

    assertThat(underTest.getStoreFilters().get("type"), is("!group,!proxy"));
  }

  @Test
  public void testTypeFilters_orderIndependentOfCallOrder() {
    // excludes are always appended after includes, regardless of the order the fluent methods are invoked
    underTest.excludingAnyOfTypes("group");
    underTest.includingAnyOfTypes("hosted", "proxy");

    assertThat(underTest.getStoreFilters().get("type"), is("hosted,proxy,!group"));
  }

  @Test
  public void testFormatFilters_onlyInclude() {
    underTest.includingAnyOfFormats("maven", "docker");

    assertThat(underTest.getStoreFilters().get("format"), is("maven,docker"));
  }

  @Test
  public void testFormatFilters_onlyExclude() {
    underTest.excludingAnyOfFormats("nuget", "npm");

    assertThat(underTest.getStoreFilters().get("format"), is("!nuget,!npm"));
  }

  @Test
  public void testFacetFilters() {
    underTest.includingAnyOfFacets(String.class, Integer.class);

    assertThat(underTest.getStoreFilters().get("facets"), is("java.lang.String,java.lang.Integer"));
  }

  @Test
  public void testRegardlessViewPermissions() {
    underTest.regardlessViewPermissions();

    assertThat(underTest.getStoreFilters().get("regardlessViewPermissions"), is("true"));
  }

  @Test
  public void testGetStoreFilters_emptyReturnsNull() {
    assertThat(underTest.getStoreFilters(), nullValue());
  }

  @Test
  public void testGetStoreFilters_emptyTypesVarargs_returnsNull() {
    // an empty (but non-null) include list contributes nothing, so no "type" key is produced and the map stays empty
    underTest.includingAnyOfTypes();

    assertThat(underTest.getStoreFilters(), nullValue());
  }

  @Test
  public void testGetStoreFilters_emptyFacetsVarargs_addsEmptyFacetsEntry() {
    // facets are guarded only by a null check (not a length check), so an empty facet list still produces a
    // "facets" entry with an empty value and therefore a non-null filter map
    underTest.includingAnyOfFacets();

    Map<String, String> filters = underTest.getStoreFilters();
    assertThat(filters, notNullValue());
    assertThat(filters.get("facets"), is(""));
  }

  @Test
  public void testGetStoreFilters_allCombined() {
    underTest.includingAnyOfTypes("hosted");
    underTest.includingAnyOfFormats("maven");
    underTest.includingAnyOfVersionPolicies("RELEASE");
    underTest.includingAnyOfFacets(String.class);
    underTest.regardlessViewPermissions();

    Map<String, String> filters = underTest.getStoreFilters();
    assertThat(filters, notNullValue());
    assertThat(filters.get("type"), is("hosted"));
    assertThat(filters.get("format"), is("maven"));
    assertThat(filters.get("versionPolicies"), is("RELEASE"));
    assertThat(filters.get("facets"), is("java.lang.String"));
    assertThat(filters.get("regardlessViewPermissions"), is("true"));
  }

  @Test
  public void testGetStoreFilters_allIncludeAndExcludeCombined() {
    // exercise every '!' exclude branch together with includes for types/formats/version policies in a single pass
    underTest.excludingAnyOfTypes("group");
    underTest.includingAnyOfTypes("hosted");
    underTest.excludingAnyOfFormats("nuget");
    underTest.includingAnyOfFormats("maven");
    underTest.excludingAnyOfVersionPolicies("SNAPSHOT");
    underTest.includingAnyOfVersionPolicies("RELEASE");
    underTest.includingAnyOfFacets(String.class, Integer.class);
    underTest.regardlessViewPermissions();

    Map<String, String> filters = underTest.getStoreFilters();
    assertThat(filters, notNullValue());
    assertThat(filters.get("type"), is("hosted,!group"));
    assertThat(filters.get("format"), is("maven,!nuget"));
    assertThat(filters.get("versionPolicies"), is("RELEASE,!SNAPSHOT"));
    assertThat(filters.get("facets"), is("java.lang.String,java.lang.Integer"));
    assertThat(filters.get("regardlessViewPermissions"), is("true"));
  }

  @Test
  public void testGetStoreFilters_repeatedCalls_areEqualButNewInstances() {
    underTest.includingAnyOfTypes("hosted");

    Map<String, String> first = underTest.getStoreFilters();
    Map<String, String> second = underTest.getStoreFilters();

    // a fresh map is built on every call (no caching), but the contents are equal
    assertThat(first, equalTo(second));
    assertThat(first, not(sameInstance(second)));
  }

  @Test
  public void testWithListener_addsListenersAndIsFluent() {
    RepositoryCombobox result = underTest.withListener("select", "onSelect");
    assertThat(result, sameInstance(underTest));

    Map<?, ?> listeners = (Map<?, ?>) underTest.getAttributes().get("listeners");
    assertThat(listeners, notNullValue());
    assertThat((String) listeners.get("select"), is("onSelect"));

    underTest.withListener("change", "onChange");
    listeners = (Map<?, ?>) underTest.getAttributes().get("listeners");
    assertThat((String) listeners.get("select"), is("onSelect"));
    assertThat((String) listeners.get("change"), is("onChange"));
  }

  @Test
  public void testWithListener_reusesSameMapInstanceAcrossCalls() {
    underTest.withListener("select", "onSelect");
    Map<?, ?> firstMap = (Map<?, ?>) underTest.getAttributes().get("listeners");

    underTest.withListener("change", "onChange");
    Map<?, ?> secondMap = (Map<?, ?>) underTest.getAttributes().get("listeners");

    // the existing listeners map is reused (not recreated) on subsequent calls
    assertThat(secondMap, sameInstance(firstMap));
    assertThat(secondMap.size(), is(2));
  }

  @Test
  public void testWithListener_overwritesSameEvent() {
    underTest.withListener("select", "onSelect");
    underTest.withListener("select", "onSelectAgain");

    Map<?, ?> listeners = (Map<?, ?>) underTest.getAttributes().get("listeners");
    assertThat(listeners, notNullValue());
    // re-binding the same event name overwrites the prior listener rather than creating a duplicate entry
    assertThat((String) listeners.get("select"), is("onSelectAgain"));
    assertThat(listeners.size(), is(1));
  }

  @Test
  public void testIncludeEntriesForAllFormats_doesNotAddStoreFilter() {
    // the "all formats" flag only influences the store API; it must not contribute any store filter entry
    underTest.includeEntriesForAllFormats();

    assertThat(underTest.getStoreFilters(), nullValue());
    assertThat(underTest.getStoreApi(), is("coreui_Repository.readReferencesAddingEntriesForAllFormats"));
  }

  @Test
  public void testGetStoreFilters_emptyFormatAndVersionPolicyVarargs_returnsNull() {
    // empty (but non-null) include/exclude lists for formats and version policies contribute nothing,
    // so the filter map stays empty and getStoreFilters() returns null
    underTest.includingAnyOfFormats();
    underTest.excludingAnyOfFormats();
    underTest.includingAnyOfVersionPolicies();
    underTest.excludingAnyOfVersionPolicies();

    assertThat(underTest.getStoreFilters(), nullValue());
  }

  @Test
  public void testGetStoreFilters_typeOnly_containsOnlyTypeKey() {
    // a type-only configuration must produce a map with exactly the "type" key and no spurious entries
    underTest.includingAnyOfTypes("hosted");

    Map<String, String> filters = underTest.getStoreFilters();
    assertThat(filters, notNullValue());
    assertThat(filters.size(), is(1));
    assertThat(filters.containsKey("type"), is(true));
    assertThat(filters.containsKey("format"), is(false));
    assertThat(filters.containsKey("versionPolicies"), is(false));
    assertThat(filters.containsKey("facets"), is(false));
    assertThat(filters.containsKey("regardlessViewPermissions"), is(false));
  }

  @Test
  public void testGetStoreFilters_regardlessViewPermissionsOnly_containsOnlyThatKey() {
    // setting only the regardless-view-permissions flag yields a single-entry map
    underTest.regardlessViewPermissions();

    Map<String, String> filters = underTest.getStoreFilters();
    assertThat(filters, notNullValue());
    assertThat(filters.size(), is(1));
    assertThat(filters.get("regardlessViewPermissions"), is("true"));
  }

}
