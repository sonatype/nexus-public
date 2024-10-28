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
package org.sonatype.nexus.repository.group;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.ConstraintViolation;
import javax.validation.constraints.NotNull;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.cache.CacheController;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.cache.RepositoryCacheInvalidationService;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Iterables;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.isNull;
import static org.sonatype.nexus.repository.FacetSupport.State.STARTED;
import static org.sonatype.nexus.validation.ConstraintViolations.maybeAdd;
import static org.sonatype.nexus.validation.ConstraintViolations.maybePropagate;

/**
 * Default {@link GroupFacet} implementation.
 *
 * @since 3.0
 */
@Named("default")
public class GroupFacetImpl
    extends FacetSupport
    implements GroupFacet
{
  protected final RepositoryManager repositoryManager;

  private final Type groupType;

  protected final ConstraintViolationFactory constraintViolationFactory;

  public static final String CONFIG_KEY = "group";

  private final RepositoryCacheInvalidationService repositoryCacheInvalidationService;

  public static class Config
  {
    @NotNull
    @JsonDeserialize(as = LinkedHashSet.class) // retain order
    public Set<String> memberNames;

    @Override
    public String toString() {
      return getClass().getSimpleName() + "{" +
          "memberNames=" + memberNames +
          '}';
    }
  }

  private Config config;

  protected CacheController cacheController;

  @Inject
  public GroupFacetImpl(final RepositoryManager repositoryManager,
                        final ConstraintViolationFactory constraintViolationFactory,
                        @Named(GroupType.NAME) final Type groupType,
                        final RepositoryCacheInvalidationService repositoryCacheInvalidationService)
  {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.groupType = checkNotNull(groupType);
    this.constraintViolationFactory = checkNotNull(constraintViolationFactory);
    this.repositoryCacheInvalidationService = checkNotNull(repositoryCacheInvalidationService);
  }

  @Override
  protected void doValidate(final Configuration configuration) throws Exception {
    facet(ConfigurationFacet.class).validateSection(configuration, CONFIG_KEY, Config.class);

    if (getStateGuard().is(STARTED)) {
      Config configToValidate = facet(ConfigurationFacet.class).readSection(configuration, CONFIG_KEY, Config.class);

      Set<ConstraintViolation<?>> violations = new HashSet<>();
      maybeAdd(violations, validateGroupDoesNotContainItself(configuration.getRepositoryName(), configToValidate));
      maybeAdd(violations, validateFormat(configuration, configToValidate));
      maybePropagate(violations, log);
    }
  }

  /**
   * A method subclasses can override to perform format specific validation if necessary
   *
   * @param configuration the configuration of the repository
   * @param groupConfig the group's config object
   * @return the validation failures or null
   */
  protected ConstraintViolation<?> validateFormat(final Configuration configuration, final Config groupConfig) {
    // empty for subclasses to optionally override
    return null;
  }

  private boolean containsGroup(final Repository root, final String repositoryName, final Set<Repository> checkedGroups) {
    return root.facet(GroupFacet.class).members().stream().anyMatch((repository) -> {
      return checkedGroups.add(repository) &&
          (repository.getName().equals(repositoryName) ||
              (groupType.equals(repository.getType()) && containsGroup(repository, repositoryName, checkedGroups)));
    });
  }

  ConstraintViolation<?> validateGroupDoesNotContainItself(String repositoryName, Config config) {
    Set<Repository> checkedGroups = new HashSet<>();
    for (String memberName : config.memberNames) {
      Repository repository = repositoryManager.get(memberName);
      if (repository.getName().equals(repositoryName) ||
          (groupType.equals(repository.getType()) && containsGroup(repository, repositoryName, checkedGroups))) {
        return constraintViolationFactory.createViolation(CONFIG_KEY + ".memberNames",
            "Group '" + repository.getName() + "' has a member repository '" + repositoryName +
                "' and cannot be added to this list.");
      }
    }

    return null;
  }

  @Override
  protected void doConfigure(final Configuration configuration) throws Exception {
    config = facet(ConfigurationFacet.class).readSection(configuration, CONFIG_KEY, Config.class);

    cacheController = new CacheController(-1, null);

    log.debug("Config: {}", config);
  }

  @Override
  protected void doUpdate(final Configuration configuration) throws Exception {
    // detect member changes
    Set<String> previousMemberNames = config.memberNames;
    super.doUpdate(configuration);

    // check whether any members or their ordering have changed
    if (!Iterables.elementsEqual(config.memberNames, previousMemberNames)) {
      cacheController.invalidateCache();
    }
  }

  @Override
  protected void doDestroy() throws Exception {
    config = null;
  }

  @Override
  @Guarded(by = STARTED)
  public boolean member(final String repositoryName) {
    checkNotNull(repositoryName);
    return config.memberNames.contains(repositoryName);
  }

  @Override
  @Guarded(by = STARTED)
  public boolean member(final Repository repository) {
    checkNotNull(repository);
    return config.memberNames.contains(repository.getName());
  }

  @Override
  @Guarded(by = STARTED)
  public List<Repository> members() {
    final Repository repository = getRepository();

    List<Repository> members = new ArrayList<>(config.memberNames.size());
    for (String name : config.memberNames) {
      Repository member = repositoryManager.get(name);
      if (member == null) {
        log.warn("Ignoring missing member repository: {}", name);
      }
      else if (!repository.getFormat().equals(member.getFormat())) {
        log.warn("Group {} includes an incompatible-format member: {} with format {}",
            repository.getName(), name, member.getFormat());
      }
      else {
        members.add(member);
      }
    }
    return members;
  }

  @Override
  public List<Repository> leafMembers() {
    Set<Repository> leafMembers = new LinkedHashSet<>();

    for (Repository repository : members()) {
      if (groupType.equals(repository.getType())) {
        leafMembers.addAll(repository.facet(GroupFacet.class).leafMembers());
      }
      else {
        leafMembers.add(repository);
      }
    }

    return new ArrayList<>(leafMembers);
  }

  @Override
  public List<Repository> allMembers() {
    return allMembers(new ArrayList<>(), getRepository());
  }

  private static List<Repository> allMembers(final List<Repository> members, final Repository root) {
    // obviously dealing with a duplicate in the tree, back out
    if (members.contains(root)) {
      return members;
    }

    members.add(root);
    List<Repository> groupMembers = root.optionalFacet(GroupFacet.class).map(GroupFacet::members)
        .orElseGet(Collections::emptyList);
    for (Repository child : groupMembers) {
        allMembers(members, child);
      }
    return members;
  }

  @Override
  public void invalidateGroupCaches() {
    log.info("Invalidating group caches of {}", getRepository().getName());
    cacheController.invalidateCache();
    for (Repository repository : members()) {
      repositoryCacheInvalidationService.processCachesInvalidation(repository);
    }
  }

  @Override
  public boolean isStale(@Nullable final Content content) {
    if (content == null) {
      return true;
    }

    final CacheInfo cacheInfo = content.getAttributes().get(CacheInfo.class);

    if(isNull(cacheInfo)) {
      log.warn("CacheInfo missing for {}, assuming stale content.", content);
      return true;
    }
    return cacheController.isStale(cacheInfo);
  }

  @Override
  public void maintainCacheInfo(final AttributesMap attributesMap) {
    attributesMap.set(CacheInfo.class, cacheController.current());
  }
}
