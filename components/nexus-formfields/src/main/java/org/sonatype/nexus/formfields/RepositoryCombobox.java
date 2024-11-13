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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Stream.concat;

/**
 * A repository combo box {@link FormField}.
 *
 * @since 2.7
 */
public class RepositoryCombobox
    extends Combobox<String>
{
  private List<String> includingTypes;

  private List<String> excludingTypes;

  private boolean regardlessViewPermissions;

  private List<String> includingFormats;

  private List<String> excludingFormats;

  private List<String> includingVersionPolicies;

  private List<String> excludingVersionPolicies;

  private List<String> includingFacets;

  private boolean generateAllRepositoriesEntry;

  private boolean includeEntriesForAllFormats;

  private interface Messages
      extends MessageBundle
  {

    @DefaultMessage("Repository")
    String label();

    @DefaultMessage("Select the repository.")
    String helpText();

  }

  private static final Messages messages = I18N.create(Messages.class);

  public RepositoryCombobox(String id, String label, String helpText, boolean required, String regexValidation) {
    super(id, label, helpText, required, regexValidation);
  }

  public RepositoryCombobox(String id, String label, String helpText, boolean required) {
    super(id, label, helpText, required);
  }

  public RepositoryCombobox(String id, boolean required) {
    super(id, messages.label(), messages.helpText(), required);
  }

  public RepositoryCombobox(String id) {
    super(id, messages.label(), messages.helpText(), false);
  }

  /**
   * Repository will be present if is of any of specified types.
   */
  public RepositoryCombobox includingAnyOfTypes(final String... types) {
    this.includingTypes = Arrays.asList(types);
    return this;
  }

  /**
   * Repository will not be present if is of any of specified types.
   */
  public RepositoryCombobox excludingAnyOfTypes(final String... types) {
    this.excludingTypes = Arrays.asList(types);
    return this;
  }

  /**
   * Repository will be present if is of any of specified formats.
   */
  public RepositoryCombobox includingAnyOfFormats(final String... formats) {
    this.includingFormats = Arrays.asList(formats);
    return this;
  }

  /**
   * Repository will not be present if is of any of specified formats.
   */
  public RepositoryCombobox excludingAnyOfFormats(final String... formats) {
    this.excludingFormats = Arrays.asList(formats);
    return this;
  }

  /**
   * Repository will be present if is of any of specified Version Policies.
   */
  public RepositoryCombobox includingAnyOfVersionPolicies(final String... versionPolicies) {
    this.includingVersionPolicies = Arrays.asList(versionPolicies);
    return this;
  }

  /**
   * Repository will not be present if is of any of specified Version Policies.
   */
  public RepositoryCombobox excludingAnyOfVersionPolicies(final String... versionPolicies) {
    this.excludingVersionPolicies = Arrays.asList(versionPolicies);
    return this;
  }

  /**
   * Repository will be present if is of any of specified formats.
   */
  public RepositoryCombobox includingAnyOfFacets(final Class<?>... facets) {
    this.includingFacets = Lists.transform(Arrays.asList(facets), Class::getName);
    return this;
  }

  /**
   * Repository will be present regardless if current user has rights to view the repository.
   */
  public RepositoryCombobox regardlessViewPermissions() {
    this.regardlessViewPermissions = true;
    return this;
  }

  /**
   * Will add an entry for "All repositories". The value will be "*".
   */
  public RepositoryCombobox includeAnEntryForAllRepositories() {
    this.generateAllRepositoriesEntry = true;
    return this;
  }

  /**
   * Will add an entry for "All Repositories" as well as "All nuget repositories", "All npm repositories", etc.
   *
   * @since 3.1
   */
  public RepositoryCombobox includeEntriesForAllFormats() {
    this.includeEntriesForAllFormats = true;
    return this;
  }

  @Override
  public boolean getAllowAutocomplete() {
    return true;
  }

  /**
   * @since 3.0
   */
  @Override
  public String getStoreApi() {
    String method = "readReferences";
    if (includeEntriesForAllFormats) {
      method = "readReferencesAddingEntriesForAllFormats";
    }
    else if (generateAllRepositoriesEntry) {
      method = "readReferencesAddingEntryForAll";
    }
    return "coreui_Repository." + method;
  }

  /**
   * @since 3.0
   */
  @Override
  public Map<String, String> getStoreFilters() {
    Map<String, String> storeFilters = Maps.newHashMap();
    StringBuilder types = new StringBuilder();
    if (includingTypes != null) {
      for (String type : includingTypes) {
        if (types.length() > 0) {
          types.append(',');
        }
        types.append(type);
      }
    }
    if (excludingTypes != null) {
      for (String type : excludingTypes) {
        if (types.length() > 0) {
          types.append(',');
        }
        types.append('!').append(type);
      }
    }
    if (types.length() > 0) {
      storeFilters.put("type", types.toString());
    }
    StringBuilder contentClasses = new StringBuilder();
    if (includingFormats != null) {
      for (String format : includingFormats) {
        if (contentClasses.length() > 0) {
          contentClasses.append(',');
        }
        contentClasses.append(format);
      }
    }
    if (excludingFormats != null) {
      for (String format : excludingFormats) {
        if (contentClasses.length() > 0) {
          contentClasses.append(',');
        }
        contentClasses.append("!").append(format);
      }
    }
    String versionPolicies = getVersionPolicies();
    if (contentClasses.length() > 0) {
      storeFilters.put("format", contentClasses.toString());
    }
    if (includingFacets != null) {
      storeFilters.put("facets", Joiner.on(',').join(includingFacets));
    }
    if (regardlessViewPermissions) {
      storeFilters.put("regardlessViewPermissions", "true");
    }
    if (versionPolicies.length() > 0) {
      storeFilters.put("versionPolicies", versionPolicies);
    }
    return storeFilters.isEmpty() ? null : storeFilters;
  }

  private String getVersionPolicies() {
    return concat(
        ofNullable(includingVersionPolicies).orElse(emptyList()).stream(),
        ofNullable(excludingVersionPolicies).orElse(emptyList()).stream()
            .map(s -> "!" + s))
        .collect(joining(","));
  }

}
