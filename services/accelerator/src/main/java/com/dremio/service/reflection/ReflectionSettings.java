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
package com.dremio.service.reflection;

import com.dremio.service.namespace.NamespaceKey;
import com.dremio.service.namespace.dataset.proto.AccelerationSettings;
import com.google.common.base.Optional;

/**
 * Interface for datasets/sources acceleration settings.
 */
public interface ReflectionSettings {
  // only returns a AccelerationSettings if one is specifically defined for the specified key
  Optional<AccelerationSettings> getStoredReflectionSettings(NamespaceKey key);

  AccelerationSettings getReflectionSettings(NamespaceKey key);

  void setReflectionSettings(NamespaceKey key, AccelerationSettings settings);

  void removeSettings(NamespaceKey key);
  default int getAllHash() { return 0; };
}
