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

import java.util.Map;
import java.util.Set;

import org.sonatype.nexus.proxy.repository.Repository;

/**
 * This is the registry of known repository types. Just like RepositoryRegistry holds the "active" instances of
 * Repositories, this registry does the discovery of them. Hint: we are using String for role intentionally, to be able
 * to do reloads of plugins that contributes new repository roles to system.
 *
 * @author cstamas
 */
public interface RepositoryTypeRegistry
{
  /**
   * Returns the unmodifiable set of repo type descriptors that are known that provides Repository components.
   *
   * @return a modifiable set of repository type descriptors or empty set.
   */
  Set<RepositoryTypeDescriptor> getRegisteredRepositoryTypeDescriptors();

  /**
   * Registers a repo type.
   *
   * @return a modifiable set of repository type descriptors or empty set.
   */
  boolean registerRepositoryTypeDescriptors(RepositoryTypeDescriptor d);

  /**
   * Deregisters a repo type.
   *
   * @return a modifiable set of repository type descriptors or empty set.
   */
  boolean unregisterRepositoryTypeDescriptors(RepositoryTypeDescriptor d);

  /**
   * Returns an unmodifiable set of FQN of classes that are known that provides Repository components.
   *
   * @return a set of repository type descriptors or empty set.
   */
  Set<Class<? extends Repository>> getRepositoryRoles();

  /**
   * Returns the set of hints for the given repository role.
   *
   * @return a set of repository hints or empty set.
   */
  Set<String> getExistingRepositoryHints(Class<? extends Repository> role);

  /**
   * Returns the type descriptor for the given role+hint combination.
   *
   * @return the type descriptor or null if there is none for this combination of role and hint.
   */
  RepositoryTypeDescriptor getRepositoryTypeDescriptor(Class<? extends Repository> role, String hint);

  @Deprecated
  RepositoryTypeDescriptor getRepositoryTypeDescriptor(String role, String hint);

  /**
   * Returns the available content classes as unmodifiable map.
   */
  Map<String, ContentClass> getContentClasses();

  /**
   * Returns a list of content class ids that are compatible with teh supplied content class
   */
  Set<String> getCompatibleContentClasses(ContentClass contentClass);

  /**
   * Returns the ContentClass for the given Repository component.
   *
   * @return the content class instance or null if repository does not exists.
   */
  ContentClass getRepositoryContentClass(Class<? extends Repository> role, String hint);
}
