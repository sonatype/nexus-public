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
package org.sonatype.nexus.extjs;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * ExtJS class definition record.
 *
 * @since 3.0
 */
public class ClassDef
{
  private final String name;

  private final File source;

  private final Set<String> dependencies = new HashSet<>();

  private final List<String> alternateClassName = new ArrayList<>();

  private final List<String> alias = new ArrayList<>();

  private double priority = 0;

  private String extend;

  private String override;

  private List<String> requires;

  private List<String> mixins;

  private List<String> uses;

  private List<String> mvcControllers;

  private List<String> mvcModels;

  private List<String> mvcStores;

  private List<String> mvcViews;

  public ClassDef(final String name, final File source) {
    this.name = checkNotNull(name);
    this.source = checkNotNull(source);
  }

  public String getName() {
    return name;
  }

  public File getSource() {
    return source;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "name='" + name + '\'' +
        ", source=" + source +
        '}';
  }

  public Set<String> getDependencies() {
    return dependencies;
  }

  public List<String> getAlternateClassName() {
    return alternateClassName;
  }

  public List<String> getAlias() {
    return alias;
  }

  public double getPriority() {
    return priority;
  }

  public void setPriority(final double priority) {
    this.priority = priority;
  }

  @Nullable
  public String getExtend() {
    return extend;
  }

  public void setExtend(final String extend) {
    this.extend = checkNotNull(extend);
    dependencies.add(extend);
  }

  @Nullable
  public String getOverride() {
    return override;
  }

  public void setOverride(final String override) {
    this.override = checkNotNull(override);
    dependencies.add(override);
  }

  @Nullable
  public List<String> getRequires() {
    return requires;
  }

  public void setRequires(final List<String> requires) {
    this.requires = checkNotNull(requires);
    dependencies.addAll(requires);
  }

  @Nullable
  public List<String> getMixins() {
    return mixins;
  }

  public void setMixins(final List<String> mixins) {
    this.mixins = checkNotNull(mixins);
    dependencies.addAll(mixins);
  }

  @Nullable
  public List<String> getUses() {
    return uses;
  }

  public void setUses(final List<String> uses) {
    this.uses = checkNotNull(uses);
    dependencies.addAll(uses);
  }

  //
  // Custom handling for MVC support
  //

  @Nullable
  public List<String> getMvcControllers() {
    return mvcControllers;
  }

  public void setMvcControllers(final List<String> mvcControllers) {
    this.mvcControllers = checkNotNull(mvcControllers);
  }

  @Nullable
  public List<String> getMvcModels() {
    return mvcModels;
  }

  public void setMvcModels(final List<String> mvcModels) {
    this.mvcModels = checkNotNull(mvcModels);
  }

  @Nullable
  public List<String> getMvcStores() {
    return mvcStores;
  }

  public void setMvcStores(final List<String> mvcStores) {
    this.mvcStores = checkNotNull(mvcStores);
  }

  @Nullable
  public List<String> getMvcViews() {
    return mvcViews;
  }

  public void setMvcViews(final List<String> mvcViews) {
    this.mvcViews = checkNotNull(mvcViews);
  }
}
