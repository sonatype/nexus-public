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
package org.sonatype.nexus.jmx;

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nullable;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;
import javax.management.ServiceNotFoundException;

import org.sonatype.goodies.common.ComponentSupport;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Dynamic MBean.
 *
 * @see MBeanAttribute
 * @see MBeanOperation
 * @since 3.0
 */
public class MBean
    extends ComponentSupport
    implements DynamicMBean
{
  private final MBeanInfo info;

  private final Map<String,MBeanAttribute> attributes;

  private final Map<OperationKey,MBeanOperation> operations;

  public MBean(final MBeanInfo info,
               final Collection<MBeanAttribute> attributes,
               final Collection<MBeanOperation> operations)
  {
    this.info = checkNotNull(info);

    // build attributes lookup map
    ImmutableMap.Builder<String,MBeanAttribute> attrs = ImmutableMap.builder();
    for (MBeanAttribute attribute : attributes) {
      attrs.put(attribute.getName(), attribute);
    }
    this.attributes = attrs.build();

    // build operations lookup map
    ImmutableMap.Builder<OperationKey,MBeanOperation> ops = ImmutableMap.builder();
    for (MBeanOperation operation : operations) {
      ops.put(operation.getKey(), operation);
    }
    this.operations = ops.build();
  }

  @Override
  public MBeanInfo getMBeanInfo() {
    return info;
  }

  public Collection<MBeanAttribute> getAttributes() {
    return attributes.values();
  }

  /**
   * Lookup attribute by name.
   */
  private MBeanAttribute attribute(final String name) throws AttributeNotFoundException {
    checkNotNull(name);
    MBeanAttribute attribute = attributes.get(name);
    if (attribute == null) {
      throw new AttributeNotFoundException(name);
    }
    return attribute;
  }

  public Collection<MBeanOperation> getOperations() {
    return operations.values();
  }

  /**
   * Lookup operation by key.
   */
  private MBeanOperation operation(final OperationKey key) throws ServiceNotFoundException {
    checkNotNull(key);
    MBeanOperation operation = operations.get(key);
    if (operation == null) {
      throw new ServiceNotFoundException("Missing operation: " + key);
    }
    return operation;
  }

  @Override
  public Object getAttribute(final String name)
      throws AttributeNotFoundException, MBeanException, ReflectionException
  {
    try {
      return attribute(name).getValue();
    }
    catch (Exception e) {
      log.warn("Failed to get attribute: {}", name, e);

      // TODO: Sort out the proper exception handling/wrapping
      Throwables.propagateIfPossible(e, AttributeNotFoundException.class);
      Throwables.propagateIfPossible(e, MBeanException.class);
      Throwables.propagateIfPossible(e, ReflectionException.class);
      throw new MBeanException(e);
    }
  }

  @Override
  public void setAttribute(final Attribute attribute)
      throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException
  {
    try {
      String name = attribute.getName();
      Object value = attribute.getValue();
      attribute(name).setValue(value);
    }
    catch (Exception e) {
      log.warn("Failed to set attribute: {}", attribute, e);

      // TODO: Sort out the proper exception handling/wrapping
      Throwables.propagateIfPossible(e, AttributeNotFoundException.class);
      Throwables.propagateIfPossible(e, InvalidAttributeValueException.class);
      Throwables.propagateIfPossible(e, MBeanException.class);
      Throwables.propagateIfPossible(e, ReflectionException.class);
      throw new MBeanException(e);
    }
  }

  // TODO: Unsure if return is nullable or not, error on side of caution

  @Override
  public AttributeList getAttributes(@Nullable final String[] names) {
    AttributeList result = new AttributeList();
    if (names != null) {
      for (String name : names) {
        try {
          Attribute attribute = new Attribute(name, getAttribute(name));
          result.add(attribute);
        }
        catch (Exception e) {
          log.warn("Failed to get attribute: {}", name, e);
        }
      }
    }
    return result;
  }

  // TODO: Unsure if return or params are nullable or not, error on side of caution

  @Override
  public AttributeList setAttributes(@Nullable final AttributeList attributes) {
    AttributeList result = new AttributeList();
    if (attributes != null) {
      for (Attribute attribute : attributes.asList()) {
        try {
          setAttribute(attribute);
          result.add(attribute);
        }
        catch (Exception e) {
          log.warn("Failed to set attribute: {}", attribute.getName(), e);
        }
      }
    }
    return result;
  }

  private static final Object[] EMPTY_PARAMS = new Object[0];

  private static final String[] EMPTY_SIGNATURE = new String[0];

  @Override
  public Object invoke(final String name, @Nullable Object[] params, @Nullable String[] types)
      throws MBeanException, ReflectionException
  {
    if (params == null) {
      params = EMPTY_PARAMS;
    }
    if (types == null) {
      types = EMPTY_SIGNATURE;
    }

    OperationKey key = new OperationKey(name, types);
    try {
      return operation(key).invoke(params);
    }
    catch (Exception e) {
      log.warn("Failed to invoke operation: {}", key, e);

      // TODO: Sort out the proper exception handling/wrapping
      Throwables.propagateIfPossible(e, MBeanException.class);
      Throwables.propagateIfPossible(e, ReflectionException.class);
      throw new MBeanException(e);
    }
  }
}
