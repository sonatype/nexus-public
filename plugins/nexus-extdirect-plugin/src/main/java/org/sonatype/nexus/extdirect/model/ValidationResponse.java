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
package org.sonatype.nexus.extdirect.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ElementKind;
import javax.validation.Path.Node;

import com.google.common.base.Joiner;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Ext.Direct validation response.
 *
 * @since 3.0
 */
public class ValidationResponse
    extends Response<Object>
{
  private List<String> messages;

  private Map<String, String> errors;

  public ValidationResponse(final ConstraintViolationException cause) {
    super(false, new ArrayList<>());
    //noinspection ThrowableResultOfMethodCallIgnored
    checkNotNull(cause);
    Set<ConstraintViolation<?>> violations = cause.getConstraintViolations();
    if (violations != null && violations.size() > 0) {
      for (ConstraintViolation<?> violation : violations) {
        List<String> entries = new ArrayList<>();
        // iterate path to get the full path
        Iterator<Node> it = violation.getPropertyPath().iterator();
        while (it.hasNext()) {
          Node node = it.next();
          if (ElementKind.PROPERTY == node.getKind() || (ElementKind.PARAMETER == node.getKind() && !it.hasNext())) {
            if (node.getKey() != null) {
              entries.add(node.getKey().toString());
            }
            entries.add(node.getName());
          }
        }
        if (entries.isEmpty()) {
          if (messages == null) {
            messages = new ArrayList<>();
          }
          messages.add(violation.getMessage());
        }
        else {
          if (errors == null) {
            errors = new HashMap<>();
          }
          errors.put(Joiner.on('.').join(entries), violation.getMessage());
        }
      }
    }
    else if (cause.getMessage() != null) {
      messages = new ArrayList<>();
      messages.add(cause.getMessage());
    }
  }

}
