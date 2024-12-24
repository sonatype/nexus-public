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
package org.sonatype.nexus.coreui;

import java.util.List;
import java.util.Map;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/**
 * OSGI bundle.
 *
 * @since 3.0
 */
public class BundleXO
{
  @Min(0L)
  private long id;

  @NotBlank
  private String state;

  @NotBlank
  private String name;

  @NotBlank
  private String symbolicName;

  @NotBlank
  private String location;

  @NotBlank
  private String version;

  @Min(0L)
  private int startLevel;

  private boolean fragment;

  private long lastModified;

  /**
   * Fragment bundle ids.
   */
  private List<Long> fragments;

  /**
   * Fragment-host bundle ids.
   */
  private List<Long> fragmentHosts;

  private Map<String, String> headers;

  public long getId() {
    return id;
  }

  public BundleXO withId(long id) {
    this.id = id;
    return this;
  }

  public String getState() {
    return state;
  }

  public BundleXO withState(String state) {
    this.state = state;
    return this;
  }

  public String getName() {
    return name;
  }

  public BundleXO withName(String name) {
    this.name = name;
    return this;
  }

  public String getSymbolicName() {
    return symbolicName;
  }

  public BundleXO withSymbolicName(String symbolicName) {
    this.symbolicName = symbolicName;
    return this;
  }

  public String getLocation() {
    return location;
  }

  public BundleXO withLocation(String location) {
    this.location = location;
    return this;
  }

  public String getVersion() {
    return version;
  }

  public BundleXO withVersion(String version) {
    this.version = version;
    return this;
  }

  public int getStartLevel() {
    return startLevel;
  }

  public BundleXO withStartLevel(int startLevel) {
    this.startLevel = startLevel;
    return this;
  }

  public boolean isFragment() {
    return fragment;
  }

  public BundleXO withFragment(boolean fragment) {
    this.fragment = fragment;
    return this;
  }

  public long getLastModified() {
    return lastModified;
  }

  public BundleXO withLastModified(long lastModified) {
    this.lastModified = lastModified;
    return this;
  }

  public List<Long> getFragments() {
    return fragments;
  }

  public BundleXO withFragments(List<Long> fragments) {
    this.fragments = fragments;
    return this;
  }

  public List<Long> getFragmentHosts() {
    return fragmentHosts;
  }

  public BundleXO withFragmentHosts(List<Long> fragmentHosts) {
    this.fragmentHosts = fragmentHosts;
    return this;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public BundleXO withHeaders(Map<String, String> headers) {
    this.headers = headers;
    return this;
  }

}
