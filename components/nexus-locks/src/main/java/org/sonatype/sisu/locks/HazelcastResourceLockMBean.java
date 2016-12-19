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
package org.sonatype.sisu.locks;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.core.Member;
import org.eclipse.sisu.inject.Logs;

/**
 * Distributed Hazelcast {@link ResourceLockMBean} implementation.
 */
final class HazelcastResourceLockMBean
    extends AbstractResourceLockMBean
{
  // ----------------------------------------------------------------------
  // Implementation fields
  // ----------------------------------------------------------------------

  private final HazelcastInstance instance;

  private final ObjectName jmxQuery;

  // ----------------------------------------------------------------------
  // Constructor
  // ----------------------------------------------------------------------

  HazelcastResourceLockMBean(final HazelcastInstance instance, final ObjectName jmxQuery) {
    this.instance = instance;
    this.jmxQuery = jmxQuery;
  }

  // ----------------------------------------------------------------------
  // Public methods
  // ----------------------------------------------------------------------

  public String[] listResourceNames() {
    return multiInvoke("listResourceNames");
  }

  public String[] findOwningThreads(final String name) {
    return multiInvoke("findOwningThreads", name);
  }

  public String[] findWaitingThreads(final String name) {
    return multiInvoke("findWaitingThreads", name);
  }

  public String[] findOwnedResources(final String tid) {
    return multiInvoke("findOwnedResources", tid);
  }

  public String[] findWaitedResources(final String tid) {
    return multiInvoke("findWaitedResources", tid);
  }

  public void releaseResource(final String name) {
    multiInvoke("releaseResource", name);
  }

  // ----------------------------------------------------------------------
  // Implementation methods
  // ----------------------------------------------------------------------

  /**
   * Distributes the given JMX invocation across the Hazelcast cluster.
   *
   * @param method JMX method
   * @param args   JMX arguments
   * @return Aggregated results
   */
  public String[] multiInvoke(final String method, final String... args) {
    final HazelcastMBeansInvoker invoker = new HazelcastMBeansInvoker(jmxQuery, method, args);
    final Set<String> results = new HashSet<String>();
    try {
      Map<Member, Future<List<String>>> memberResults =
          instance.getExecutorService("default").submitToMembers(invoker, filterMembers(method, args));
      for (final Future<List<String>> result : memberResults.values()) {
        results.addAll(result.get());
      }
    }
    catch (final Exception e) {
      Logs.warn("Problem executing cluster MultiTask for: \"{}\"", method, e);
    }
    return results.toArray(new String[results.size()]);
  }

  /**
   * Filters members of the Hazelcast cluster based on the given JMX invocation.
   *
   * @param method JMX method
   * @param args   JMX arguments
   * @return Filtered members
   */
  private Set<Member> filterMembers(final String method, final String... args) {
    final Set<Member> members = new HashSet<Member>();
    try {
            /*
             * Filter members based on IP address (forms part of the distributed thread id)
             */
      if (method.endsWith("Resources") && args[0].contains("@")) {
        final String[] tokens = args[0].split("\\s*[@:]\\s*", 3);

        args[0] = tokens[0];

        if (tokens.length == 3) {
          final InetSocketAddress addr = new InetSocketAddress(tokens[1], Integer.parseInt(tokens[2]));
          for (final Member m : instance.getCluster().getMembers()) {
            if (addr.equals(m.getInetSocketAddress())) {
              members.add(m);
            }
          }
        }
        else if (tokens.length == 2) {
          final InetAddress addr = InetAddress.getByName(tokens[1]);
          for (final Member m : instance.getCluster().getMembers()) {
            if (addr.equals(m.getInetSocketAddress().getAddress())) {
              members.add(m);
            }
          }
        }
      }
    }
    catch (final Exception e) {
      Logs.warn("Problem filtering cluster members for: \"{}\"", method, e);
    }
    if (members.isEmpty()) {
      members.addAll(instance.getCluster().getMembers());
    }
    return members;
  }
}

/**
 * JMX method invoker that can be distributed across a Hazelcast cluster.
 */
@SuppressWarnings("serial")
final class HazelcastMBeansInvoker
    implements HazelcastInstanceAware, Callable<List<String>>, Serializable
{
  // ----------------------------------------------------------------------
  // Implementation fields
  // ----------------------------------------------------------------------

  private HazelcastInstance instance;

  private final ObjectName jmxQuery;

  private final String method;

  private final String[] args;

  // ----------------------------------------------------------------------
  // Constructor
  // ----------------------------------------------------------------------

  HazelcastMBeansInvoker(final ObjectName jmxQuery, final String method, final String... args) {
    this.jmxQuery = jmxQuery;
    this.method = method;
    this.args = args;
  }

  // ----------------------------------------------------------------------
  // Public methods
  // ----------------------------------------------------------------------

  public void setHazelcastInstance(final HazelcastInstance instance) {
    this.instance = instance;
  }

  @Override
  public List<String> call()
      throws Exception
  {
    final MBeanServer server = ManagementFactory.getPlatformMBeanServer();

    // assumes methods have string-only signatures
    final String[] sig = new String[args.length];
    Arrays.fill(sig, String.class.getName());

    final List<String> results = new ArrayList<String>();
    for (final ObjectName mBean : server.queryNames(jmxQuery, null)) {
      try {
        // Invoke across all matching MBeans in this Hazelcast instance
        final Object result = server.invoke(mBean, method, args, sig);
        if (result instanceof String[]) {
          Collections.addAll(results, (String[]) result);
        }
      }
      catch (final Exception e) {
        Logs.warn("Problem invoking JMX method: \"{}\"", method, e);
      }
    }
    return qualifyResults(results);
  }

  // ----------------------------------------------------------------------
  // Implementation methods
  // ----------------------------------------------------------------------

  /**
   * Qualifies the given results by appending the IP address of the local Hazelcast instance.
   *
   * @param results Local results
   * @return Qualified results
   */
  private List<String> qualifyResults(final List<String> results) {
    if (method.endsWith("Threads")) {
      final String addr = toString(instance.getCluster().getLocalMember().getInetSocketAddress());
      for (int i = 0; i < results.size(); i++) {
        results.set(i, results.get(i) + " @ " + addr);
      }
    }
    return results;
  }

  /**
   * @return IP address in "x.y.z:port" format
   */
  private static String toString(final InetSocketAddress address) {
    final StringBuilder buf = new StringBuilder();

    final byte[] ip = address.getAddress().getAddress();

    append(buf, ip[0], '.');
    append(buf, ip[1], '.');
    append(buf, ip[2], '.');
    append(buf, ip[3], ':');

    return buf.append(address.getPort()).toString();
  }

  /**
   * Appends unsigned value from 0 to 255 plus delimiter.
   */
  private static void append(final StringBuilder buf, final byte value, final char delim) {
    buf.append(value & 0xFF).append(delim);
  }
}
