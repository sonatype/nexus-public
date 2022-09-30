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
package org.sonatype.nexus.testsuite.testsupport.system.repository.config;

import java.util.function.Function;

import org.sonatype.nexus.repository.Repository;

public abstract class ProxyRepositoryConfigSupport<THIS>
    extends RepositoryConfigSupport<THIS>
    implements ProxyRepositoryConfig<THIS>
{
  private static final String PROXY_RECIPE_SUFFIX = "-proxy";

  private String remoteUrl;

  private boolean preemptivePullEnabled = false;

  private String assetPathRegex;

  private Boolean blocked = false;

  private Boolean autoBlocked = true;

  private Integer contentMaxAge = 1440;

  private Integer metadataMaxAge = 1440;

  private Boolean negativeCacheEnabled = true;

  private Integer negativeCacheTimeToLive = 1440;

  private String username;

  private String password;

  public ProxyRepositoryConfigSupport(final Function<THIS, Repository> factory) {
    super(factory);
  }

  @Override
  public String getRecipe() {
    return getFormat() + PROXY_RECIPE_SUFFIX;
  }

  @Override
  public THIS withRemoteUrl(final String remoteUrl) {
    this.remoteUrl = remoteUrl;
    return toTHIS();
  }

  @Override
  public String getRemoteUrl() {
    return remoteUrl;
  }

  @Override
  public THIS withPullReplication(){
    this.preemptivePullEnabled = true;
    return toTHIS();
  }

  @Override
  public Boolean isPreemptivePullEnabled(){
    return this.preemptivePullEnabled;
  }

  @Override
  public THIS withAssetPathRegex(final String assetPathRegex){
    this.assetPathRegex = assetPathRegex;
    return toTHIS();
  }

  @Override
  public String getAssetPathRegex() {
    return assetPathRegex;
  }

  @Override
  public THIS withBlocked(final Boolean blocked) {
    this.blocked = blocked;
    return toTHIS();
  }

  @Override
  public Boolean isBlocked() {
    return blocked;
  }

  @Override
  public THIS withAutoBlocked(final Boolean autoBlocked) {
    this.autoBlocked = autoBlocked;
    return toTHIS();
  }

  @Override
  public Boolean isAutoBlocked() {
    return autoBlocked;
  }

  @Override
  public THIS withContentMaxAge(final Integer contentMaxAge) {
    this.contentMaxAge = contentMaxAge;
    return toTHIS();
  }

  @Override
  public Integer getContentMaxAge() {
    return contentMaxAge;
  }

  @Override
  public THIS withMetadataMaxAge(final Integer metadataMaxAge) {
    this.metadataMaxAge = metadataMaxAge;
    return toTHIS();
  }

  @Override
  public Integer getMetadataMaxAge() {
    return metadataMaxAge;
  }

  @Override
  public THIS withNegativeCacheEnabled(final Boolean negativeCacheEnabled) {
    this.negativeCacheEnabled = negativeCacheEnabled;
    return toTHIS();
  }

  @Override
  public Boolean isNegativeCacheEnabled() {
    return negativeCacheEnabled;
  }

  @Override
  public THIS withNegativeCacheTimeToLive(final Integer negativeCacheTimeToLive) {
    this.negativeCacheTimeToLive = negativeCacheTimeToLive;
    return toTHIS();
  }

  @Override
  public Integer getNegativeCacheTimeToLive() {
    return negativeCacheTimeToLive;
  }

  @Override
  public THIS withUsername(final String username) {
    this.username = username;
    return toTHIS();
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public THIS withPassword(final String password) {
    this.password = password;
    return toTHIS();
  }

  @Override
  public String getPassword() {
    return password;
  }
}
