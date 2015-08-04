/*
 * Copyright (c) 2007-2014 Sonatype, Inc. and Georgy Bolyuba. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.bolyuba.nexus.plugin.npm;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.plugin.PluginIdentity;

/**
 * This plugin adds Npm support to Sonatype Nexus. Implementation is based on
 * http://wiki.commonjs.org/wiki/Packages/Registry spec and behaviour of https://registry.npmjs.org
 *
 * @author Georgy Bolyuba (georgy@bolyuba.com)
 */
@Named
@Singleton
public class NpmPlugin
    extends PluginIdentity
{

  public static final String GROUP_ID = "org.sonatype.nexus.plugins";

  public static final String ARTIFACT_ID = "nexus-npm-repository-plugin";

  public NpmPlugin() throws Exception {
    super(GROUP_ID, ARTIFACT_ID);
  }
}
