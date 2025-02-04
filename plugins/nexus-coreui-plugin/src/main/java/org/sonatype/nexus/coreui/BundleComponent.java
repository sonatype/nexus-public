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

import java.util.Dictionary;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.extdirect.DirectComponentSupport;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.softwarementors.extjs.djn.config.annotations.DirectAction;
import com.softwarementors.extjs.djn.config.annotations.DirectMethod;
import org.apache.karaf.bundle.core.BundleInfo;
import org.apache.karaf.bundle.core.BundleService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.stream;

/**
 * OSGI bundle component.
 *
 * @since 3.0
 */
@Named
@Singleton
@DirectAction(action = "coreui_Bundle")
public class BundleComponent
    extends DirectComponentSupport
{
  private final BundleContext bundleContext;

  private final BundleService bundleService;

  @Inject
  public BundleComponent(final BundleContext bundleContext, final BundleService bundleService) {
    this.bundleContext = checkNotNull(bundleContext);
    this.bundleService = checkNotNull(bundleService);
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:bundles:read")
  public List<BundleXO> read() {
    return stream(bundleContext.getBundles()).map(bundle -> {
      BundleInfo info = bundleService.getInfo(bundle);
      BundleXO entry = new BundleXO()
          .withId(info.getBundleId())
          .withState(info.getState().name())
          .withName(info.getName())
          .withSymbolicName(info.getSymbolicName())
          .withVersion(info.getVersion())
          .withLocation(info.getUpdateLocation())
          .withStartLevel(info.getStartLevel())
          .withLastModified(bundle.getLastModified())
          .withFragment(info.isFragment())
          .withFragments(info.getFragments().stream().map(Bundle::getBundleId).collect(Collectors.toList())) // NOSONAR
          .withFragmentHosts(info.getFragmentHosts().stream().map(Bundle::getBundleId).collect(Collectors.toList())); // NOSONAR

      // convert header dict
      Map<String, String> headers = new LinkedHashMap<>();
      Dictionary<String, String> bundleHeaders = bundle.getHeaders();
      for (Iterator<String> it = bundleHeaders.keys().asIterator(); it.hasNext();) {
        String key = it.next();
        String value = bundleHeaders.get(key);
        headers.put(key, value);
      }
      entry.withHeaders(headers);

      return entry;
    }).collect(Collectors.toList()); // NOSONAR
  }
}
