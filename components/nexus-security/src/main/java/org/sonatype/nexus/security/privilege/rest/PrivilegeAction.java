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
package org.sonatype.nexus.security.privilege.rest;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * @since 3.19
 */
public enum PrivilegeAction
{
  //the names/actions of these are very important, do not change without great consideration
  READ("read"), BROWSE("browse"), EDIT("edit"), ADD("add"), DELETE("delete"), RUN("run"), START("start"), STOP("stop"), ASSOCIATE("associate"), DISASSOCIATE("disassociate"), ALL("*");

  private final String action;

  private static final String CREATE = "create";

  private static final String UPDATE = "update";

  PrivilegeAction(String action) {
    this.action = action;
  }

  @Nullable
  public String getBreadAction() {
    switch (this) {
      case BROWSE:
      case READ:
      case EDIT:
      case ADD:
      case DELETE:
      case ALL:
        return action;
      default:
        return null;
    }
  }

  @Nullable
  public String getBreadRunAction() {
    switch (this) {
      case BROWSE:
      case READ:
      case EDIT:
      case ADD:
      case DELETE:
      case ALL:
      case RUN:
        return action;
      default:
        return null;
    }
  }

  @Nullable
  public String getCrudAction() {
    switch (this) {
      case ADD:
        return CREATE;
      case EDIT:
        return UPDATE;
      case ASSOCIATE:
      case DISASSOCIATE:
      case READ:
      case DELETE:
      case ALL:
        return action;
      default:
        return null;
    }
  }

  @Nullable
  public String getCrudTaskActions() {
    switch (this) {
      case ADD:
        return CREATE;
      case EDIT:
        return UPDATE;
      case ASSOCIATE:
      case DISASSOCIATE:
      case READ:
      case DELETE:
      case START:
      case STOP:
      case ALL:
        return action;
      default:
        return null;
    }
  }

  @Nullable
  public static PrivilegeAction fromAction(final String action) {
    String trimmed = action.trim();
    switch (trimmed) {
      case CREATE:
        return PrivilegeAction.ADD;
      case UPDATE:
        return PrivilegeAction.EDIT;
      default:
        return Arrays.stream(PrivilegeAction.values()).filter(a -> a.action.equals(action)).findFirst().orElse(null);
    }
  }

  public static List<PrivilegeAction> getBreadActions() {
    return Arrays.asList(BROWSE, READ, EDIT, ADD, DELETE, ALL);
  }

  public static List<PrivilegeAction> getBreadRunActions() {
    return Arrays.asList(BROWSE, READ, EDIT, ADD, DELETE, RUN, ALL);
  }

  public static List<PrivilegeAction> getCrudActions() {
    return Arrays.asList(READ, EDIT, ADD, DELETE, ASSOCIATE, DISASSOCIATE, ALL);
  }

  public static List<PrivilegeAction> getCrudTaskAction() {
    return Arrays.asList(READ, EDIT, ADD, DELETE, ASSOCIATE, DISASSOCIATE, START, STOP, ALL);
  }

  public static List<String> getBreadActionStrings() {
    return Arrays.asList(BROWSE, READ, EDIT, ADD, DELETE)
        .stream()
        .map(PrivilegeAction::getBreadAction)
        .collect(Collectors.toList());
  }

  public static List<String> getBreadRunActionStrings() {
    return Arrays.asList(BROWSE, READ, EDIT, ADD, DELETE, RUN)
        .stream()
        .map(PrivilegeAction::getBreadRunAction)
        .collect(Collectors.toList());
  }

  public static List<String> getCrudTaskActionStrings() {
    return Arrays.asList(ADD, READ, EDIT, DELETE, START, STOP, ASSOCIATE, DISASSOCIATE)
        .stream()
        .map(PrivilegeAction::getCrudTaskActions)
        .collect(Collectors.toList());
  }
}
