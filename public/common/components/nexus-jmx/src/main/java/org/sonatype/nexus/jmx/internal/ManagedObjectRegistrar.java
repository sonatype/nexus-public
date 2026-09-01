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
package org.sonatype.nexus.jmx.internal;

import java.util.Hashtable;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nullable;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.jmx.MBean;
import org.sonatype.nexus.jmx.ObjectNameEntry;
import org.sonatype.nexus.jmx.reflect.ManagedObject;
import org.sonatype.nexus.jmx.reflect.ReflectionMBeanBuilder;

import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.inject.Named;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Handles registration of {@link ManagedObject} components.
 *
 * @since 3.0
 */
@Component
public class ManagedObjectRegistrar
    implements ApplicationContextAware
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private MBeanServer server;

  @Autowired
  public ManagedObjectRegistrar(final MBeanServer server) {
    this.server = checkNotNull(server);
  }

  @Override
  public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
    registerManagedObjects(applicationContext);
  }

  @EventListener
  public void onApplicationEvent(final ContextRefreshedEvent event) {
    log.trace("onApplicationEvent {}", event);
    registerManagedObjects(event.getApplicationContext());
  }

  private void registerManagedObjects(final ApplicationContext applicationContext) {
    applicationContext
        .getBeansWithAnnotation(ManagedObject.class)
        .values()
        .forEach(this::registerObject);
  }

  private void registerObject(final Object managedObject) {
    ManagedObject descriptor = descriptor(managedObject);
    if (descriptor == null) {
      return;
    }

    try {
      ObjectName name = objectName(descriptor, managedObject);
      if (!registered(name)) {
        log.debug("Registering: {} -> {}", name, managedObject.getClass().getSimpleName());
        MBean mbean = mbean(descriptor, managedObject);

        server.registerMBean(mbean, name);
      }
    }
    catch (Exception e) {
      log.warn("Failed to export: {}; ignoring", managedObject.getClass().getSimpleName(), e);
    }
  }

  private boolean registered(final ObjectName name) throws IntrospectionException, ReflectionException {
    try {
      server.getMBeanInfo(name);
      return true;
    }
    catch (InstanceNotFoundException e) {
      return false;
    }
  }

  @Nullable
  private static ManagedObject descriptor(final Object managedObject) {
    Class<?> type = managedObject.getClass();
    return type.getAnnotation(ManagedObject.class);
  }

  /**
   * Determine {@link ObjectName} for given managed Object.
   */
  private static ObjectName objectName(final ManagedObject descriptor, final Object managedObject) throws Exception {
    Class<?> type = managedObject.getClass();

    // default domain to package if missing
    String domain = descriptor.domain();
    if (Strings.emptyToNull(domain) == null) {
      domain = type.getPackage().getName();
    }

    // Hashtable is required by ancient JMX api
    Hashtable<String, String> entries = new Hashtable<>();

    // add custom object-name entries
    for (ObjectNameEntry kv : descriptor.entries()) {
      entries.put(kv.name(), kv.value());
    }

    // set object-name 'type'
    entries.put("type", type(descriptor, managedObject));

    // optionally set object-name 'name'
    String name = name(descriptor, managedObject);
    if (name != null) {
      entries.put("name", name);
    }

    return new ObjectName(domain, entries);
  }

  /**
   * Determine object-name 'type' value.
   */
  private static String type(final ManagedObject descriptor, final Object managedObject) {
    String type = Strings.emptyToNull(descriptor.type());
    if (type == null) {
      if (descriptor.typeClass() != null && descriptor.typeClass() != Void.class /* default */) {
        type = descriptor.typeClass().getSimpleName();
      }
      else {
        // TODO: Consider inspecting @Typed?
        // TODO: It would really be nice if we could infer the proper intf type of simple components, but this may be
        // too complex?
        type = managedObject.getClass().getSimpleName();
      }
    }
    return type;
  }

  /**
   * Determine object-name 'name' value.
   */
  @Nullable
  private static String name(final ManagedObject descriptor, final Object managedObject) {
    String name = Strings.emptyToNull(descriptor.name());

    if (name == null) {
      // try various annotations
      List<Function<Object, Optional<String>>> nameSources =
          List.of(QualifierUtil::value, ManagedObjectRegistrar::getJakartaNamed);
      for (Function<Object, Optional<String>> nameSource : nameSources) {
        name = nameSource.apply(managedObject)
            .filter(Strings2::notBlank)
            .orElse(null);
        if (name != null) {
          break;
        }
      }
    }
    return name;
  }

  @Nullable
  private static Optional<String> getJakartaNamed(final Object managedObject) {
    return Optional.ofNullable(managedObject)
        .map(Object::getClass)
        .map(clazz -> clazz.getAnnotation(Named.class))
        .map(Named::value);
  }

  /**
   * Construct mbean for given managed Object discovering its attributes and operations.
   */
  private static MBean mbean(final ManagedObject descriptor, final Object managedObject) throws Exception {
    Class<?> type = managedObject.getClass();

    ReflectionMBeanBuilder builder = new ReflectionMBeanBuilder(type);

    // attach manged target
    builder.target(() -> managedObject);

    // allow custom description, or expose what Spring tells us
    String description = Strings.emptyToNull(descriptor.description());
    builder.description(description);

    // discover managed members
    builder.discover();

    return builder.build();
  }
}
