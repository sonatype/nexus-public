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
package org.sonatype.nexus.ruby;

/**
 * it abstracts any file within the rubygems repository. it carries the name, the local path and the remote path.
 *
 * you can pass in a payload which is meant to retrieve the actual content of the underlying file from the local
 * storage. the payload can be an exception which also sets the state to ERROR.
 *
 * beside the PAYLOAD and ERROR state it can have other states: NOT_EXISTS, NO_PAYLOAD, TEMP_UNAVAILABLE, FORBIDDEN
 *
 * @author christian
 */
public class RubygemsFile
{
  public static enum State
  {
    NEW_INSTANCE, NOT_EXISTS, ERROR, NO_PAYLOAD, TEMP_UNAVAILABLE, FORBIDDEN, PAYLOAD
  }

  /**
   * factory to create associated objects
   */
  final RubygemsFileFactory factory;

  private final String name;

  private final String storage;

  private final String remote;

  private final FileType type;

  private Object payload;

  private State state = State.NEW_INSTANCE;

  RubygemsFile(RubygemsFileFactory factory, FileType type, String storage, String remote, String name) {
    this.factory = factory;
    this.type = type;
    this.storage = storage;
    this.remote = remote;
    this.name = name;
  }

  /**
   * name of the file - meaning of name depends on file-type
   */
  public String name() {
    return name;
  }

  /**
   * local path of the file
   */
  public String storagePath() {
    return storage;
  }

  /**
   * remote path of the file
   */
  public String remotePath() {
    return remote;
  }

  /**
   * type of the file
   */
  public FileType type() {
    return type;
  }

  /**
   * state of the file
   */
  public State state() {
    return state;
  }

  protected void addToString(StringBuilder builder) {
    builder.append("type=").append(type.name())
        .append(", storage=").append(storage)
        .append(", remote=").append(remote);
    if (name != null) {
      builder.append(", name=").append(name);
    }
    builder.append(", state=").append(state.name());
    if (state == State.ERROR) {
      builder.append(", exception=").append(getException().getClass().getSimpleName())
          .append(": ").append(getException().getMessage());
    }
    else if (state == State.PAYLOAD) {
      builder.append(", payload=").append(get().toString());
    }
  }

  public String toString() {
    StringBuilder builder = new StringBuilder("RubygemsFile[");
    addToString(builder);
    builder.append("]");
    return builder.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((remote == null) ? 0 : remote.hashCode());
    result = prime * result
        + ((storage == null) ? 0 : storage.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    RubygemsFile other = (RubygemsFile) obj;
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    }
    else if (!name.equals(other.name)) {
      return false;
    }
    if (remote == null) {
      if (other.remote != null) {
        return false;
      }
    }
    else if (!remote.equals(other.remote)) {
      return false;
    }
    if (storage == null) {
      if (other.storage != null) {
        return false;
      }
    }
    else if (!storage.equals(other.storage)) {
      return false;
    }
    if (type != other.type) {
      return false;
    }
    return true;
  }

  /**
   * retrieve the payload - whatever was set via {@link RubygemsFile#set(Object)} or
   * {@link RubygemsFile#setException(Exception)}
   */
  public Object get() {
    return payload;
  }

  /**
   * sets the payload and set the state to NO_PAYLOAD or PAYLOAD respectively
   */
  public void set(Object payload) {
    state = payload == null ? State.NO_PAYLOAD : State.PAYLOAD;
    this.payload = payload;
  }

  /**
   * sets the state to ERROR and the exception as payload
   *
   * @param e (should not be null)
   */
  public void setException(Exception e) {
    set(e);
    state = State.ERROR;
  }

  /**
   * retrieve the exception if state == ERROR otherwise null
   */
  public Exception getException() {
    if (hasException()) {
      return (Exception) payload;
    }
    else {
      return null;
    }
  }

  /**
   * true if state == ERROR
   */
  public boolean hasException() {
    return state == State.ERROR;
  }

  /**
   * reset the payload and state to NEW_INSTANCE - same as newly constructed object
   */
  public void resetState() {
    payload = null;
    state = State.NEW_INSTANCE;
  }

  /**
   * any state member of NEW_INSTANCE, NO_PAYLOAD, State.PAYLOAD will return true
   */
  public boolean exists() {
    return state == State.NEW_INSTANCE || state == State.NO_PAYLOAD || state == State.PAYLOAD;
  }

  /**
   * state == NOT_EXISTS
   */
  public boolean notExists() {
    return state == State.NOT_EXISTS;
  }

  /**
   * state == NO_PAYLOAD
   */
  public boolean hasNoPayload() {
    return state == State.NO_PAYLOAD;
  }

  /**
   * state == PAYLOAD
   */
  public boolean hasPayload() {
    return state == State.PAYLOAD;
  }

  /**
   * state == FORBIDDEN
   */
  public boolean forbidden() {
    return state == State.FORBIDDEN;
  }

  /**
   * make file as not existing (state = NOT_EXISTS)
   */
  public void markAsNotExists() {
    state = State.NOT_EXISTS;
  }

  /**
   * make file as temporary unavailable (state = TEMP_UNAVAILABLE)
   */
  public void markAsTempUnavailable() {
    state = State.TEMP_UNAVAILABLE;
  }

  /**
   * make file as forbidden (state = FORBIDDEN)
   */
  public void markAsForbidden() {
    state = State.FORBIDDEN;
  }
}
