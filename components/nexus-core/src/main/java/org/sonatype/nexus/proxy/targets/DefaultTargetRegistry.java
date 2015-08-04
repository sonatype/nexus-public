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
package org.sonatype.nexus.proxy.targets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.configuration.AbstractLastingConfigurable;
import org.sonatype.nexus.configuration.CoreConfiguration;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.configuration.model.CRepositoryTarget;
import org.sonatype.nexus.configuration.model.CRepositoryTargetCoreConfiguration;
import org.sonatype.nexus.configuration.validator.ApplicationConfigurationValidator;
import org.sonatype.nexus.proxy.events.TargetRegistryEventAdd;
import org.sonatype.nexus.proxy.events.TargetRegistryEventRemove;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.registry.RepositoryTypeRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import org.codehaus.plexus.util.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The default implementation of target registry.
 * 
 * @author cstamas
 */
@Singleton
@Named
public class DefaultTargetRegistry
    extends AbstractLastingConfigurable<List<CRepositoryTarget>>
    implements TargetRegistry
{
  private final RepositoryTypeRegistry repositoryTypeRegistry;

  private final ApplicationConfigurationValidator validator;

  // a cache view of "live" targets, keyed by target ID
  // eagerly rebuilt on every configuration change
  private Map<String, Target> targets;

  // ==

  @Inject
  public DefaultTargetRegistry(EventBus eventBus, ApplicationConfiguration applicationConfiguration,
      RepositoryTypeRegistry repositoryTypeRegistry, ApplicationConfigurationValidator validator)
  {
    super("Repository Target Registry", eventBus, applicationConfiguration);
    this.repositoryTypeRegistry = checkNotNull(repositoryTypeRegistry);
    this.validator = checkNotNull(validator);
  }

  @Override
  protected void initializeConfiguration() throws ConfigurationException {
    if (getApplicationConfiguration().getConfigurationModel() != null) {
      configure(getApplicationConfiguration());
    }
  }

  @Override
  protected CoreConfiguration<List<CRepositoryTarget>> wrapConfiguration(Object configuration) throws ConfigurationException {
    if (configuration instanceof ApplicationConfiguration) {
      return new CRepositoryTargetCoreConfiguration((ApplicationConfiguration) configuration);
    }
    else {
      throw new ConfigurationException("The passed configuration object is of class \""
          + configuration.getClass().getName() + "\" and not the required \""
          + ApplicationConfiguration.class.getName() + "\"!");
    }
  }

  @Override
  protected void doConfigure() throws ConfigurationException {
    super.doConfigure();
    // rebuild the view
    rebuildView();
  }

  /**
   * Gets the effective configuration and rebuilds the "view" of live target objects, that is a map, keyed by target
   * IDs.
   */
  protected void rebuildView() {
    synchronized (getCurrentCoreConfiguration()) {
      // rebuild the view
      List<CRepositoryTarget> ctargets = getCurrentConfiguration(false);
      final Map<String, Target> newView = new HashMap<String, Target>(ctargets.size());
      for (CRepositoryTarget ctarget : ctargets) {
        Target target = convert(ctarget);
        if (target != null) {
          newView.put(target.getId(), target);
        }
      }
      targets = newView;
    }
  }

  // ==

  protected Target convert(CRepositoryTarget target) {
    ContentClass contentClass = getContentClassById(target.getContentClass());

    // If content class is null, we have a target for a repo type that no longer exists
    // plugin was removed most likely, so we ignore in this case
    if (contentClass != null) {
      return new Target(target.getId(), target.getName(), contentClass, target.getPatterns());
    }

    return null;
  }

  protected CRepositoryTarget convert(Target target) {
    CRepositoryTarget result = new CRepositoryTarget();
    result.setId(target.getId());
    result.setName(target.getName());
    result.setContentClass(target.getContentClass().getId());
    ArrayList<String> patterns = new ArrayList<String>(target.getPatternTexts().size());
    patterns.addAll(target.getPatternTexts());
    result.setPatterns(patterns);
    return result;
  }

  protected void validate(CRepositoryTarget target) throws InvalidConfigurationException {
    ValidationResponse response = validator.validateRepositoryTarget(null, target);
    if (!response.isValid()) {
      throw new InvalidConfigurationException(response);
    }
  }

  protected ContentClass getContentClassById(String id) {
    return repositoryTypeRegistry.getContentClasses().get(id);
  }

  // ==

  public Collection<Target> getRepositoryTargets() {
    return Collections.unmodifiableCollection(targets.values());
  }

  public Target getRepositoryTarget(String id) {
    return targets.get(id);
  }

  public synchronized boolean addRepositoryTarget(Target target) throws ConfigurationException {
    CRepositoryTarget cnf = convert(target);
    validate(cnf);
    removeRepositoryTarget(cnf.getId(), true);
    getCurrentConfiguration(true).add(cnf);
    eventBus().post(new TargetRegistryEventAdd(this, target));
    return true;
  }

  public synchronized boolean removeRepositoryTarget(String id) {
    return removeRepositoryTarget(id, false);
  }

  protected boolean removeRepositoryTarget(String id, boolean forUpdate) {
    final List<CRepositoryTarget> targets = getCurrentConfiguration(true);
    for (Iterator<CRepositoryTarget> ti = targets.iterator(); ti.hasNext();) {
      CRepositoryTarget cTarget = ti.next();
      if (StringUtils.equals(id, cTarget.getId())) {
        Target target = getRepositoryTarget(id);
        ti.remove();
        if (!forUpdate) {
          eventBus().post(new TargetRegistryEventRemove(this, target));
        }
        return true;
      }
    }

    return false;
  }

  public Set<Target> getTargetsForContentClass(ContentClass contentClass) {
    log.debug("Resolving targets for contentClass='{}'", contentClass.getId());

    final Set<Target> result = new HashSet<Target>();
    for (Target t : getRepositoryTargets()) {
      if (t.getContentClass().equals(contentClass)) {
        result.add(t);
      }
    }
    return result;
  }

  public Set<Target> getTargetsForContentClassPath(ContentClass contentClass, String path) {
    log.debug("Resolving targets for contentClass='{}' for path='{}'", contentClass.getId(), path);

    final Set<Target> result = new HashSet<Target>();
    for (Target t : getRepositoryTargets()) {
      if (t.isPathContained(contentClass, path)) {
        result.add(t);
      }
    }
    return result;
  }

  public TargetSet getTargetsForRepositoryPath(Repository repository, String path) {
    log.debug("Resolving targets for repository='{}' for path='{}'", repository.getId(), path);

    final TargetSet result = new TargetSet();
    for (Target t : getRepositoryTargets()) {
      if (t.isPathContained(repository.getRepositoryContentClass(), path)) {
        result.addTargetMatch(new TargetMatch(t, repository));
      }
    }
    return result;
  }

  public boolean hasAnyApplicableTarget(Repository repository) {
    log.debug("Looking for any targets for repository='{}'", repository.getId());

    for (Target t : getRepositoryTargets()) {
      if (t.getContentClass().isCompatible(repository.getRepositoryContentClass())) {
        return true;
      }
    }
    return false;
  }
}
