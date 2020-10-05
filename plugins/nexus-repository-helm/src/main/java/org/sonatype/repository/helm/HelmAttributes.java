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
package org.sonatype.repository.helm;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.repository.helm.internal.database.HelmProperties;

/**
 * @since 3.28
 */
public class HelmAttributes
{
  private final Map<HelmProperties, Object> attributesEnumMap;

  public HelmAttributes() {
    attributesEnumMap = new EnumMap<>(HelmProperties.class);
  }

  public HelmAttributes(final Map<String, Object> attributesMap) {
    attributesEnumMap = new EnumMap<>(HelmProperties.class);
    attributesMap.forEach((key, value) -> {
      Optional<HelmProperties> propertyOpt = HelmProperties.findByPropertyName(key);
      if (value != null && propertyOpt.isPresent()) {
        attributesEnumMap.put(propertyOpt.get(), value);
      }
    });
  }

  public void populate(final NestedAttributesMap attributesMap) {
    attributesEnumMap.forEach((helmProperties, o) -> attributesMap.set(helmProperties.getPropertyName(), o));
  }

  public String getName() {
    return getValue(HelmProperties.NAME, String.class);
  }

  public String getVersion() {
    return getValue(HelmProperties.VERSION, String.class);
  }

  public String getAppVersion() {
    return getValue(HelmProperties.APP_VERSION, String.class);
  }

  public String getDescription() {
    return getValue(HelmProperties.DESCRIPTION, String.class);
  }

  public String getIcon() {
    return getValue(HelmProperties.ICON, String.class);
  }

  public String getEngine() {
    return getValue(HelmProperties.ENGINE, String.class);
  }

  public List<String> getKeywords() {
    return getValue(HelmProperties.KEYWORDS, List.class);
  }


  public List<Map<String, String>> getMaintainers() {

    return getValue(HelmProperties.MAINTAINERS, List.class);
  }

  public List<String> getSources() {
    return getValue(HelmProperties.SOURCES, List.class);
  }

  public void setName(final String name) {
    attributesEnumMap.put(HelmProperties.NAME, name);
  }

  public void setDescription(final String description) {
    attributesEnumMap.put(HelmProperties.DESCRIPTION, description);
  }

  public void setVersion(final String version) {
    attributesEnumMap.put(HelmProperties.VERSION, version);
  }

  public void setIcon(final String icon) {
    attributesEnumMap.put(HelmProperties.ICON, icon);
  }

  public void setAppVersion(final String appVersion) {
    attributesEnumMap.put(HelmProperties.APP_VERSION, appVersion);
  }

  public void setEngine(final String engine) {
    attributesEnumMap.put(HelmProperties.ENGINE, engine);
  }

  public void setKeywords(final List<String> keywords) {
    attributesEnumMap.put(HelmProperties.KEYWORDS, keywords);
  }

  public void setMaintainers(final List<Map<String, String>> maintainers) {
    attributesEnumMap.put(HelmProperties.MAINTAINERS, maintainers);
  }

  private <T> T getValue(HelmProperties property, Class<T> tClass) {
    return tClass.cast(attributesEnumMap.get(property));
  }
}
