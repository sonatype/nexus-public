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
/*global define*/

define('Nexus/repository/action', ['Nexus/log'], function(Log) {

  var Action = function(cfg) {
    var
          _action = this,
          conditions = [];

    if (cfg.condition) {
      conditions.push(cfg.condition);
    }

    this.maybeAddToMenu = function(menu, repo, content) {
      if (_action.isApplicable(repo, content)) {
        menu.add(cfg.action);
      }
    };

    this.addCondition = function(condition) {
      if (typeof(condition) === 'function') {
        conditions.push(condition);
      } else {
        Log.debug('Tried to add non-function condition, ignoring');
      }
    };

    this.isApplicable = function(repo, content) {
      var i, condition;
      for (i = 0; i < conditions.length; i = i + 1) {
        condition = conditions[i];
        if (!condition(repo, content)) {
          return false;
        }
      }
      return true;
    };
  };

  return Action;
});

