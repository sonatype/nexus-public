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
package org.sonatype.nexus.proxy.registry;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.configuration.application.runtime.ApplicationRuntimeConfigurationBuilder;
import org.sonatype.nexus.plugins.RepositoryType;
import org.sonatype.nexus.proxy.maven.maven1.M1GroupRepository;
import org.sonatype.nexus.proxy.maven.maven1.M1LayoutedM2ShadowRepository;
import org.sonatype.nexus.proxy.maven.maven1.M1Repository;
import org.sonatype.nexus.proxy.maven.maven2.M2GroupRepository;
import org.sonatype.nexus.proxy.maven.maven2.M2LayoutedM1ShadowRepository;
import org.sonatype.nexus.proxy.maven.maven2.M2Repository;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.ShadowRepository;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
@Named
public class DefaultRepositoryTypeRegistry
    implements RepositoryTypeRegistry
{
  private final static Logger logger = LoggerFactory.getLogger(DefaultRepositoryTypeRegistry.class);

  private final Map<String, ContentClass> contentClasses;

  private final ApplicationRuntimeConfigurationBuilder applicationRuntimeConfigurationBuilder;

  private ConcurrentMap<String, ContentClass> repoCachedContentClasses;

  private Multimap<Class<? extends Repository>, RepositoryTypeDescriptor> repositoryTypeDescriptorsMap;

  @Inject
  public DefaultRepositoryTypeRegistry(final Map<String, ContentClass> contentClasses, final ApplicationRuntimeConfigurationBuilder applicationRuntimeConfigurationBuilder) {
    this.contentClasses = checkNotNull(contentClasses);
    this.applicationRuntimeConfigurationBuilder = checkNotNull(applicationRuntimeConfigurationBuilder);
    this.repoCachedContentClasses = Maps.newConcurrentMap();

    Multimap<Class<? extends Repository>, RepositoryTypeDescriptor> result = ArrayListMultimap.create();
    // fill in the defaults
    Class<? extends Repository> role = null;
    role = Repository.class;
    result.put(role, new RepositoryTypeDescriptor(role, M1Repository.ID, "repositories",
        RepositoryType.UNLIMITED_INSTANCES));
    result.put(role, new RepositoryTypeDescriptor(role, M2Repository.ID, "repositories",
        RepositoryType.UNLIMITED_INSTANCES));
    role = ShadowRepository.class;
    result.put(role, new RepositoryTypeDescriptor(role, M1LayoutedM2ShadowRepository.ID, "shadows",
        RepositoryType.UNLIMITED_INSTANCES));
    result.put(role, new RepositoryTypeDescriptor(role, M2LayoutedM1ShadowRepository.ID, "shadows",
        RepositoryType.UNLIMITED_INSTANCES));
    role = GroupRepository.class;
    result.put(role, new RepositoryTypeDescriptor(role, M1GroupRepository.ID, "groups",
        RepositoryType.UNLIMITED_INSTANCES));
    result.put(role, new RepositoryTypeDescriptor(role, M2GroupRepository.ID, "groups",
        RepositoryType.UNLIMITED_INSTANCES));
    logger.info("Registered default repository types.");
    this.repositoryTypeDescriptorsMap = result;
  }

  protected Multimap<Class<? extends Repository>, RepositoryTypeDescriptor> getRepositoryTypeDescriptors() {
    return repositoryTypeDescriptorsMap;
  }

  @Override
  public Set<RepositoryTypeDescriptor> getRegisteredRepositoryTypeDescriptors() {
    return Collections.unmodifiableSet(new HashSet<RepositoryTypeDescriptor>(getRepositoryTypeDescriptors().values()));
  }

  @Override
  public boolean registerRepositoryTypeDescriptors(RepositoryTypeDescriptor d) {
    boolean added = getRepositoryTypeDescriptors().put(d.getRole(), d);

    if (added) {
      if (d.getRepositoryMaxInstanceCount() == RepositoryType.UNLIMITED_INSTANCES) {
        logger.info("Registered Repository type " + d.toString() + ".");
      }
      else {
        logger.info("Registered Repository type " + d.toString() + " with maximal instance limit set to "
            + d.getRepositoryMaxInstanceCount() + ".");
      }
    }

    return added;
  }

  @Override
  public boolean unregisterRepositoryTypeDescriptors(RepositoryTypeDescriptor d) {
    boolean removed = getRepositoryTypeDescriptors().remove(d.getRole(), d);

    if (removed) {
      logger.info("Unregistered repository type " + d.toString());
    }

    return removed;
  }

  @Override
  public Map<String, ContentClass> getContentClasses() {
    return Collections.unmodifiableMap(new HashMap<String, ContentClass>(contentClasses));
  }

  @Override
  public Set<String> getCompatibleContentClasses(ContentClass contentClass) {
    Set<String> compatibles = new HashSet<String>();

    for (ContentClass cc : contentClasses.values()) {
      if (cc.isCompatible(contentClass) || contentClass.isCompatible(cc)) {
        compatibles.add(cc.getId());
      }
    }

    return compatibles;
  }

  @Override
  public Set<Class<? extends Repository>> getRepositoryRoles() {
    Set<RepositoryTypeDescriptor> rtds = getRegisteredRepositoryTypeDescriptors();

    HashSet<Class<? extends Repository>> result = new HashSet<Class<? extends Repository>>(rtds.size());

    for (RepositoryTypeDescriptor rtd : rtds) {
      result.add(rtd.getRole());
    }

    return Collections.unmodifiableSet(result);
  }

  @Override
  public Set<String> getExistingRepositoryHints(Class<? extends Repository> role) {
    if (!getRepositoryTypeDescriptors().containsKey(role)) {
      return Collections.emptySet();
    }

    HashSet<String> result = new HashSet<String>();

    for (RepositoryTypeDescriptor rtd : getRepositoryTypeDescriptors().get(role)) {
      result.add(rtd.getHint());
    }

    return result;
  }

  @Override
  public RepositoryTypeDescriptor getRepositoryTypeDescriptor(Class<? extends Repository> role, String hint) {
    if (!getRepositoryTypeDescriptors().containsKey(role)) {
      return null;
    }

    for (RepositoryTypeDescriptor rtd : getRepositoryTypeDescriptors().get(role)) {
      if (rtd.getHint().equals(hint)) {
        return rtd;
      }
    }

    return null;
  }

  @Override
  @Deprecated
  public RepositoryTypeDescriptor getRepositoryTypeDescriptor(String role, String hint) {
    Class<? extends Repository> roleClass = null;

    for (Class<? extends Repository> registeredClass : getRepositoryTypeDescriptors().keySet()) {
      if (registeredClass.getName().equals(role)) {
        roleClass = registeredClass;

        break;
      }
    }

    if (roleClass == null) {
      return null;
    }

    for (RepositoryTypeDescriptor rtd : getRepositoryTypeDescriptors().get(roleClass)) {
      if (rtd.getHint().equals(hint)) {
        return rtd;
      }
    }

    return null;
  }

  @Override
  public ContentClass getRepositoryContentClass(Class<? extends Repository> role, String hint) {
    if (!getRepositoryRoles().contains(role)) {
      return null;
    }

    ContentClass result = null;
    final String cacheKey = role + ":" + hint;

    if (repoCachedContentClasses.containsKey(cacheKey)) {
      result = repoCachedContentClasses.get(cacheKey);
    }
    else {
      // TODO: My assumption is that this code is never called
      // or, it get
      try {
        Repository repository = null;
        try {
          repository = applicationRuntimeConfigurationBuilder.createRepository(role, hint);
          result = repository.getRepositoryContentClass();
          repoCachedContentClasses.put(cacheKey, result);
        }
        finally {
          applicationRuntimeConfigurationBuilder.releaseRepository(repository);
        }
      }
      catch (Exception e) {
        logger.warn("Container lookup failed", e);
        result = null;
      }
    }
    return result;
  }
}
