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
package org.sonatype.nexus.atlas.internal.customizers

import java.lang.management.ManagementFactory

import javax.inject.Named
import javax.inject.Singleton
import javax.management.MBeanServer
import javax.management.ObjectName
import javax.management.openmbean.CompositeData
import javax.management.openmbean.TabularData

import org.sonatype.nexus.atlas.GeneratedContentSourceSupport
import org.sonatype.nexus.atlas.SupportBundle
import org.sonatype.nexus.atlas.SupportBundleCustomizer
import org.sonatype.sisu.goodies.common.ComponentSupport

import groovy.json.JsonBuilder

import static org.sonatype.nexus.atlas.SupportBundle.ContentSource.Priority.OPTIONAL
import static org.sonatype.nexus.atlas.SupportBundle.ContentSource.Type.JMX
/**
 * Adds jmx bean info to support bundle.
 *
 * @since 2.14.6
 */
@Named
@Singleton
class JMXCustomizer
    extends ComponentSupport
    implements SupportBundleCustomizer
{
  private final MBeanServer server = ManagementFactory.getPlatformMBeanServer()

  @Override
  void customize(final SupportBundle supportBundle) {
    // add jmx.json
    supportBundle << new GeneratedContentSourceSupport(JMX, 'jmx.json') {
      {
        this.priority = OPTIONAL
      }

      @Override
      protected void generate(final File file) {
        log.debug 'Querying mbeans'
        def objectNames = server.queryNames(new ObjectName('*:*'), null)

        log.debug 'Building model'
        def model = [:]
        objectNames.each { objectName ->
          // normalize names, strip out quotes
          def name = objectName.canonicalName.replace('"', '').replace('\'', '')
          log.debug "Processing MBean: $name"

          def info = server.getMBeanInfo(objectName)
          def attrs = [:]
          info.attributes.each { attr ->
            log.debug "Processing MBean attribute: $attr"

            if (attr.readable && attr.name != 'ObjectName') {
              try {
                def value = server.getAttribute(objectName, attr.name)
                attrs[attr.name] = render(value)
              }
              catch (e) {
                log.trace "Unable to fetch attribute: ${attr.name}; ignoring", e
                // do not include attribute detail for failure
              }
            }
          }

          model[name] = attrs
        }

        file.text = new JsonBuilder(model).toPrettyString()
      }

      def assignable(type, types) {
        for (from in types) {
          if (from.isAssignableFrom(type)) {
            return true
          }
        }
        return false
      }

      def render(value) {
        if (value == null) {
          return null
        }

        // TODO: Cope with password-like fields where we can detect .*password.* or something?

        def type = value.getClass()
        log.trace "Rendering type: $type"

        if (value instanceof TabularData) {
          def result = []
          value.keySet().each {
            def row = value.get(it as Object[]) // composite-data
            result << render(row)
          }
          return result
        }
        else if (value instanceof CompositeData) {
          def result = [:]
          value.compositeType.keySet().each { key ->
            result[key] = render(value.get(key))
          }
          return result
        }
        else if (value instanceof ObjectName) {
          return value.canonicalName
        }
        else if (value instanceof Collection || type.array) {
          return value.collect { render(it) }
        }
        else if (value instanceof Map) {
          def result = [:]
          value.each { k, v ->
            result[k.toString()] = render(v)
          }
          return result
        }
        else if (assignable(type, [Double, Float])) {
          if (value.isInfinite()) {
            return 'infinity'
          }
          else if (value.isNaN()) {
            return 'NaN'
          }
          else {
            return value
          }
        }
        else if (assignable(type, [CharSequence, Number, Boolean])) {
          return value
        }
        else if (value instanceof Enum) {
          return value.name()
        }
        else {
          log.trace "Coercing to string: ${value.getClass()} -> $value"
          return String.valueOf(value)
        }
      }
    }
  }
}
