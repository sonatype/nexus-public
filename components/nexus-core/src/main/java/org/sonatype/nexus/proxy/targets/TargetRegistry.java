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

import java.util.Collection;
import java.util.Set;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.repository.Repository;

/**
 * A component responsible for holding target references and for matching them.
 *
 * @author cstamas
 */
public interface TargetRegistry
{
  /**
   * Gets the existing targets. It returns an umodifiable collection. To modify targets, use methods below
   * (add/remove).
   */
  Collection<Target> getRepositoryTargets();

  /**
   * Gets target by id.
   */
  Target getRepositoryTarget(String id);

  /**
   * Adds new target.
   */
  boolean addRepositoryTarget(Target target)
      throws ConfigurationException;

  /**
   * Removes target by id.
   */
  boolean removeRepositoryTarget(String id);

  /**
   * This method will match all existing targets against a content class. You will end up with a simple set of
   * matched
   * targets. The cardinality of this set is equal to existing targets cardinality.
   */
  Set<Target> getTargetsForContentClass(ContentClass contentClass);

  /**
   * This method will match all existing targets against a content class and a path. You will end up with simple set
   * of matched targets. The cardinality of this set is equal to existing targets cardinality.
   */
  Set<Target> getTargetsForContentClassPath(ContentClass contentClass, String path);

  /**
   * This method will match all existing targets against a repository and a path. You will end up with a TargetSet,
   * that contains TargetMatches with references to Target and Repository.
   */
  TargetSet getTargetsForRepositoryPath(Repository repository, String path);

  /**
   * This method will return true if the repository has any applicable target at all.
   */
  boolean hasAnyApplicableTarget(Repository repository);
}
