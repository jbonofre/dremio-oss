/*
 * Copyright (C) 2017-2019 Dremio Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import moize from "moize";
import localStorageUtils from "@inject/utils/storageUtils/localStorageUtils";
import { type FeatureFlagResponse } from "./FeatureFlagResponse.type";

export const getFeatureFlagEnabledUrl = (flagId: string) =>
  `/ui/features/${flagId}` as const;

export const getFeatureFlagEnabled = moize(
  (flagId: string): Promise<boolean> => {
    return fetch(getFeatureFlagEnabledUrl(flagId), {
      headers: {
        Authorization: localStorageUtils!.getAuthToken(),
      },
    })
      .then((res) => res.json() as unknown as FeatureFlagResponse)
      .then((res) => res.entitlement === "ENABLED");
  },
  { isPromise: true, maxSize: Infinity }
);
