/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import {assign, Machine} from 'xstate';
import Axios from "axios";
import {APIConstants} from '@sonatype/nexus-ui-plugin';
import UIStrings from '../../../../constants/UIStrings';

const baseUrl = APIConstants.REST.PUBLIC.TAGS;
const urlWithTag = (tagName) => `${baseUrl}/${tagName}`
const {TAGS} = UIStrings;

export default Machine(
{
  id: 'TagsDetailsMachine',
  initial: 'loading',

  states: {
    loading: {
      invoke: {
        src: 'fetchData',
        onDone: {
          target: 'loaded',
          actions: ['setData', 'clearError']
        },
        onError: {
          target: 'loadError',
          actions: ['setError']
        },
      }
    },
    loaded: {},
    loadError: {
      on: {
        'RETRY': {
          target: 'loading',
        }
      }
    },
  },
},
{
  actions: {
    setData: assign({
      data: (_, {data}) => data.data,
      pristineData: (_, {data}) => data.data,
    }),
    setError: assign({
      error: (_, event) => {
        const tagNotFound = event.data?.message == "Request failed with status code 404";
        const message = tagNotFound ? TAGS.DETAILS.TAG_NOT_FOUND : event.data?.message;
        return message ? `${message}` : null;
      },
    }),
    clearError: assign({
      error: null
    }),
  },
  services: {
    fetchData: ({pristineData}) => Axios.get(urlWithTag(pristineData.name))
  }
});
