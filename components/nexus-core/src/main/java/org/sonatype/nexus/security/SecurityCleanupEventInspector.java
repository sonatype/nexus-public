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
package org.sonatype.nexus.security;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.events.Event;
import org.sonatype.nexus.events.EventSubscriber;
import org.sonatype.nexus.jsecurity.realms.TargetPrivilegeDescriptor;
import org.sonatype.nexus.jsecurity.realms.TargetPrivilegeGroupPropertyDescriptor;
import org.sonatype.nexus.jsecurity.realms.TargetPrivilegeRepositoryPropertyDescriptor;
import org.sonatype.nexus.jsecurity.realms.TargetPrivilegeRepositoryTargetPropertyDescriptor;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventRemove;
import org.sonatype.nexus.proxy.events.TargetRegistryEventRemove;
import org.sonatype.security.SecuritySystem;
import org.sonatype.security.authorization.NoSuchAuthorizationManagerException;
import org.sonatype.security.authorization.NoSuchPrivilegeException;
import org.sonatype.security.authorization.Privilege;
import org.sonatype.security.authorization.xml.SecurityXmlAuthorizationManager;
import org.sonatype.security.realms.tools.ConfigurationManager;
import org.sonatype.security.realms.tools.ConfigurationManagerAction;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.base.Throwables;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
@Named
public class SecurityCleanupEventInspector
    extends ComponentSupport
    implements EventSubscriber
{
  private final ConfigurationManager configManager;

  private final SecuritySystem security;

  @Inject
  public SecurityCleanupEventInspector(ConfigurationManager configManager, SecuritySystem security) {
    this.configManager = checkNotNull(configManager);
    this.security = checkNotNull(security);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final RepositoryRegistryEventRemove e) {
    inspect(e);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final TargetRegistryEventRemove e) {
    inspect(e);
  }

  protected void inspect(Event<?> evt) {
    if (evt instanceof RepositoryRegistryEventRemove) {
      RepositoryRegistryEventRemove rEvt = (RepositoryRegistryEventRemove) evt;

      String repositoryId = rEvt.getRepository().getId();

      try {
        // Delete target privs that match repo/groupId
        cleanupPrivileges(TargetPrivilegeRepositoryPropertyDescriptor.ID, repositoryId);
        cleanupPrivileges(TargetPrivilegeGroupPropertyDescriptor.ID, repositoryId);
      }
      catch (NoSuchPrivilegeException e) {
        log.error("Unable to clean privileges attached to repository", e);
      }
      catch (NoSuchAuthorizationManagerException e) {
        log.error("Unable to clean privileges attached to repository", e);
      }
    }
    if (evt instanceof TargetRegistryEventRemove) {
      TargetRegistryEventRemove rEvt = (TargetRegistryEventRemove) evt;

      String targetId = rEvt.getTarget().getId();

      try {
        cleanupPrivileges(TargetPrivilegeRepositoryTargetPropertyDescriptor.ID, targetId);
      }
      catch (NoSuchPrivilegeException e) {
        log.error("Unable to clean privileges attached to target: {}", targetId, e);
      }
      catch (NoSuchAuthorizationManagerException e) {
        log.error("Unable to clean privileges attached to target: {}", targetId, e);
      }
    }
  }

  protected void cleanupPrivileges(String propertyId, String propertyValue)
      throws NoSuchPrivilegeException, NoSuchAuthorizationManagerException
  {
    Set<Privilege> privileges = security.listPrivileges();

    final Set<String> removedIds = new HashSet<String>();

    for (Privilege privilege : privileges) {
      if (!privilege.isReadOnly() && privilege.getType().equals(TargetPrivilegeDescriptor.TYPE)
          && (propertyValue.equals(privilege.getPrivilegeProperty(propertyId)))) {
        log.debug("Removing Privilege {} because repository was removed", privilege.getName());
        security.getAuthorizationManager(SecurityXmlAuthorizationManager.SOURCE).deletePrivilege(
            privilege.getId());
        removedIds.add(privilege.getId());
      }
    }

    try {
      configManager.runWrite(new ConfigurationManagerAction()
      {
        @Override
        public void run()
            throws Exception
        {
          for (String privilegeId : removedIds) {
            configManager.cleanRemovedPrivilege(privilegeId);
          }
          configManager.save();
        }

      });
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
