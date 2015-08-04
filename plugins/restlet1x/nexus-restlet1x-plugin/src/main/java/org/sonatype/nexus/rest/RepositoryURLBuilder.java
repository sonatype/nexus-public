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
package org.sonatype.nexus.rest;

import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.repository.Repository;

public interface RepositoryURLBuilder
{
  /**
   * @see #getRepositoryContentUrl(String, boolean) where forceBaseURL is {@code false}.
   */
  String getRepositoryContentUrl(String repositoryId) throws NoSuchRepositoryException;

  /**
   * Builds the content URL of a repository identified by Id. See {@link #getRepositoryContentUrl(Repository)} for
   * full description.
   * 
   * @return the content URL.
   * @since 2.7
   */
  String getRepositoryContentUrl(String repositoryId, boolean forceBaseURL) throws NoSuchRepositoryException;

  /**
   * @see #getRepositoryContentUrl(Repository, boolean) where forceBaseURL is {@code false}.
   */
  String getRepositoryContentUrl(Repository repository);

  /**
   * Builds the content URL of a repository. Under some circumstances, it is impossible to build the URL for
   * Repository (example: this call does not happen in a HTTP Request context and baseUrl is not set), in such cases
   * this method returns {@code null}. Word of warning: the fact that a content URL is returned for a Repository does
   * not imply that the same repository is reachable over that repository! It still depends is the Repository exposed
   * or not {@link Repository#isExposed()}.
   * 
   * If "forceBaseURL" is {@code true}, the returned url will use server based URL even if the call happens in an HTTP
   * request, regardless of the value of forceBaseURL in server settings.
   * 
   * @return the content URL or {@code null}.
   * @since 2.7
   */
  String getRepositoryContentUrl(Repository repository, boolean forceBaseURL);

  /**
   * @see #getExposedRepositoryContentUrl(Repository, boolean) where forceBaseURL is {@code false}.
   */
  String getExposedRepositoryContentUrl(Repository repository);

  /**
   * Builds the exposed content URL of a repository. Same as {@link #getRepositoryContentUrl(Repository,boolean)} but
   * honors {@link Repository#isExposed()}, by returning {@code null} when repository is not exposed.
   * 
   * If "forceBaseURL" is {@code true}, the returned url will use server based URL even if the call happens in an HTTP
   * request,
   * regardless of the value of forceBaseURL in server settings.
   * 
   * @return the content URL or {@code null}.
   * @since 2.7
   */
  String getExposedRepositoryContentUrl(Repository repository, boolean forceBaseURL);

}
