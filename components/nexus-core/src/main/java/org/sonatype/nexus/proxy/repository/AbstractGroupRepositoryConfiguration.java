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
package org.sonatype.nexus.proxy.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.sonatype.configuration.validation.ValidationMessage;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.configuration.CoreConfiguration;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.configuration.model.CRepository;

import com.google.common.collect.Sets;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public class AbstractGroupRepositoryConfiguration
    extends AbstractRepositoryConfiguration
{
  private static final String MEMBER_REPOSITORIES = "memberRepositories";

  public AbstractGroupRepositoryConfiguration(Xpp3Dom configuration) {
    super(configuration);
  }

  /**
   * @return a List of member repository ids, never null
   */
  public List<String> getMemberRepositoryIds() {
    return getCollection(getRootNode(), MEMBER_REPOSITORIES);
  }

  public void setMemberRepositoryIds(List<String> ids) {
    setCollection(getRootNode(), MEMBER_REPOSITORIES, ids);
  }

  public void clearMemberRepositoryIds() {
    List<String> empty = Collections.emptyList();

    setCollection(getRootNode(), MEMBER_REPOSITORIES, empty);
  }

  public void addMemberRepositoryId(String repositoryId) {
    addToCollection(getRootNode(), MEMBER_REPOSITORIES, repositoryId, true);
  }

  public void removeMemberRepositoryId(String repositoryId) {
    removeFromCollection(getRootNode(), MEMBER_REPOSITORIES, repositoryId);
  }

  @Override
  public ValidationResponse doValidateChanges(ApplicationConfiguration applicationConfiguration,
                                              CoreConfiguration owner, Xpp3Dom config)
  {
    ValidationResponse response = super.doValidateChanges(applicationConfiguration, owner, config);

    // validate members existence

    List<CRepository> allReposes = applicationConfiguration.getConfigurationModel().getRepositories();

    List<String> allReposesIds = new ArrayList<String>(allReposes.size());

    for (CRepository repository : allReposes) {
      allReposesIds.add(repository.getId());
    }

    final List<String> memberRepositoryIds = getMemberRepositoryIds();

    if (!allReposesIds.containsAll(memberRepositoryIds)) {
      ValidationMessage message =
          new ValidationMessage(MEMBER_REPOSITORIES, "Group repository points to nonexistent members!",
              "The source nexus repository is not existing.");

      response.addValidationError(message);
    }

    final Set<String> uniqueReposesIds = Sets.newHashSet(memberRepositoryIds);
    if (uniqueReposesIds.size() != memberRepositoryIds.size()) {
      response.addValidationError(new ValidationMessage(
          MEMBER_REPOSITORIES,
          "Group repository has same member multiple times!",
          "Group repository has same member multiple times!"
      ));
    }

    // we cannot check for cycles here, since this class is not a component and to unravel groups, you would need
    // repo registry to do so. But the AbstractGroupRepository checks and does not allow itself to introduce cycles
    // anyway.

    return response;
  }
}
