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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.cache.CacheController;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.cache.RepositoryCacheUtils;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.view.Content;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import org.hibernate.validator.constraints.NotEmpty;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.FacetSupport.State.STARTED;

/**
 * Default {@link GroupFacet} implementation.
 *
 * @since 3.0
 */
@Named
public class GroupFacetImpl
    extends FacetSupport
    implements GroupFacet
{
  private final RepositoryManager repositoryManager;

  private final Type groupType;

  @VisibleForTesting
  static final String CONFIG_KEY = "group";

  @VisibleForTesting
  static class Config
  {
    @NotEmpty
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

  private CacheController cacheController;

  @Inject
  public GroupFacetImpl(final RepositoryManager repositoryManager,
                        @Named(GroupType.NAME) final Type groupType)
  {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.groupType = checkNotNull(groupType);
  }

  @Override
  protected void doValidate(final Configuration configuration) throws Exception {
    facet(ConfigurationFacet.class).validateSection(configuration, CONFIG_KEY, Config.class);
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
    List<Repository> leafMembers = new ArrayList<>();

    for (Repository repository : members()) {
      if (groupType.equals(repository.getType())) {
        leafMembers.addAll(repository.facet(GroupFacet.class).leafMembers());
      }
      else {
        leafMembers.add(repository);
      }
    }

    return leafMembers;
  }

  @Override
  public void invalidateGroupCaches() {
    log.info("Invalidating group caches of {}", getRepository().getName());
    cacheController.invalidateCache();
    for (Repository repository : members()) {
      RepositoryCacheUtils.invalidateCaches(repository);
    }
  }

  /**
   * Returns {@code true} if the content is considered stale; otherwise {@code false}.
   */
  protected boolean isStale(@Nullable final Content content) {
    return content == null || cacheController.isStale(content.getAttributes().require(CacheInfo.class));
  }

  /**
   * Maintains the latest cache information in the given content's attributes.
   */
  protected Content maintainCacheInfo(final Content content) {
    content.getAttributes().set(CacheInfo.class, cacheController.current());
    return content;
  }
}
