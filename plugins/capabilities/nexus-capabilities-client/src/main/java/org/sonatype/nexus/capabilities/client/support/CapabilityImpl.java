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
package org.sonatype.nexus.capabilities.client.support;

import java.util.List;
import java.util.Map;

import org.sonatype.nexus.capabilities.client.Capability;
import org.sonatype.nexus.capabilities.client.spi.CapabilityClient;
import org.sonatype.nexus.capabilities.model.CapabilityStatusXO;
import org.sonatype.nexus.capabilities.model.CapabilityXO;
import org.sonatype.nexus.capabilities.model.PropertyXO;
import org.sonatype.nexus.client.rest.support.EntitySupport;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Jersey based {@link Capability}.
 *
 * @since capabilities 2.2
 */
public class CapabilityImpl<C extends Capability<C>>
    extends EntitySupport<C, CapabilityStatusXO>
    implements Capability<C>
{

  private final CapabilityClient client;

  public CapabilityImpl(final CapabilityClient client, final String type) {
    super(null);
    this.client = checkNotNull(client, "client");
    settings().getCapability().withTypeId(type);
  }

  public CapabilityImpl(final CapabilityClient client, final CapabilityStatusXO settings) {
    super(settings.getCapability().getId(), settings);
    this.client = checkNotNull(client, "client");
  }

  @Override
  public String id() {
    return settings().getCapability().getId();
  }

  @Override
  protected CapabilityStatusXO createSettings(final String id) {
    return new CapabilityStatusXO()
        .withCapability(
            new CapabilityXO()
                .withId(id)
                .withEnabled(true)
                .withProperties(Lists.<PropertyXO>newArrayList())
        );
  }

  @Override
  protected CapabilityStatusXO doGet() {
    return client.getStatus(id());
  }

  @Override
  protected CapabilityStatusXO doCreate() {
    return client.create(settings().getCapability());
  }

  @Override
  protected CapabilityStatusXO doUpdate() {
    return client.update(id(), settings().getCapability());
  }

  @Override
  protected void doRemove() {
    client.delete(id());
  }

  @Override
  public String type() {
    return settings().getCapability().getTypeId();
  }

  @Override
  public String notes() {
    return settings().getCapability().getNotes();
  }

  @Override
  public boolean isEnabled() {
    return settings().getCapability().isEnabled();
  }

  @Override
  public boolean isActive() {
    return settings().isActive();
  }

  /**
   * @since capabilities 2.4
   */
  @Override
  public boolean hasErrors() {
    return settings().isError();
  }

  @Override
  public Map<String, String> properties() {
    final Map<String, String> propertiesMap = Maps.newHashMap();
    final List<PropertyXO> properties = settings().getCapability().getProperties();
    if (properties != null && !properties.isEmpty()) {
      for (final PropertyXO property : properties) {
        propertiesMap.put(property.getKey(), property.getValue());
      }
    }
    return propertiesMap;
  }

  @Override
  public String property(final String key) {
    final PropertyXO property = getProperty(checkNotNull(key));
    if (property != null) {
      return property.getValue();
    }
    return null;
  }

  @Override
  public boolean hasProperty(final String key) {
    return getProperty(key) != null;
  }

  @Override
  public String status() {
    return settings().getStatus();
  }

  /**
   * @since capabilities 2.4
   */
  @Override
  public String stateDescription() {
    return settings().getStateDescription();
  }

  @Override
  public C withNotes(final String notes) {
    settings().getCapability().setNotes(notes);
    return me();
  }

  @Override
  public C enable() {
    return withEnabled(true).save();
  }

  @Override
  public C disable() {
    return withEnabled(false).save();
  }

  @Override
  public C withEnabled(final boolean enabled) {
    settings().getCapability().setEnabled(enabled);
    return me();
  }

  @Override
  public C withProperty(final String key, final String value) {
    checkNotNull(key);
    getOrCreateProperty(key).setValue(value);
    return me();
  }

  @Override
  public C removeProperty(final String key) {
    settings().getCapability().getProperties().remove(getProperty(key));
    return me();
  }

  private PropertyXO getOrCreateProperty(final String key) {
    PropertyXO property = getProperty(key);
    if (property == null) {
      property = new PropertyXO().withKey(key);
      settings().getCapability().getProperties().add(property);
    }
    return property;
  }

  private PropertyXO getProperty(final String key) {
    final List<PropertyXO> properties = settings().getCapability().getProperties();
    if (properties != null && !properties.isEmpty()) {
      for (final PropertyXO property : properties) {
        if (key.equals(property.getKey())) {
          return property;
        }
      }
    }
    return null;
  }

  private C me() {
    return (C) this;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CapabilityImpl)) {
      return false;
    }

    final CapabilityImpl that = (CapabilityImpl) o;

    if (id() != null ? !id().equals(that.id()) : that.id() != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return id() != null ? id().hashCode() : 0;
  }

}
