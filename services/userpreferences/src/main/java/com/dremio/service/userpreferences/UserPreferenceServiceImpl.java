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

package com.dremio.service.userpreferences;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.core.SecurityContext;

import com.dremio.service.namespace.NamespaceNotFoundException;
import com.dremio.service.namespace.NamespaceService;
import com.dremio.service.namespace.proto.NameSpaceContainer;
import com.dremio.service.userpreferences.proto.UserPreferenceProto;
import com.dremio.service.userpreferences.proto.UserPreferenceProto.Preference;
import com.dremio.service.users.UserNotFoundException;
import com.dremio.service.users.UserService;
import com.google.common.base.Preconditions;
import com.google.protobuf.util.Timestamps;

/**
 * UserPreferenceService implementation
 */
public class UserPreferenceServiceImpl implements UserPreferenceService {

  private static final org.slf4j.Logger logger =
    org.slf4j.LoggerFactory.getLogger(UserPreferenceServiceImpl.class);

  private static final Long MAX_COUNT_OF_ENTITIES = 25L;

  private final UserPreferenceStore userPreferenceStore;
  private final Provider<NamespaceService> namespaceServiceProvider;

  private final UserService userService;
  private final SecurityContext securityContext;

  @Inject
  public UserPreferenceServiceImpl(Provider<UserPreferenceStore> userPreferenceStoreProvider,
                                   Provider<NamespaceService> namespaceServiceProvider,
                                   UserService userService,
                                   SecurityContext securityContext) {
    this.userPreferenceStore = userPreferenceStoreProvider.get();
    this.namespaceServiceProvider = namespaceServiceProvider;
    this.userService = userService;
    this.securityContext = securityContext;
  }

  @Override
  public UserPreferenceProto.Preference getPreferenceByType(UserPreferenceProto.PreferenceType type) {
    Optional<UserPreferenceProto.UserPreference> userPreference =
      userPreferenceStore.get(getCurrentUserId().toString());
    if (userPreference.isPresent()) {
      int index = getIndexOfPreferenceType(userPreference.get(), type);
      if (index != -1) {
        List<UserPreferenceProto.Entity> entities =
          new ArrayList<>(userPreference.get().getPreferences(index).getEntitiesList());

        // if there are entries in entity_ids, add them to entities
        for (String entityId : userPreference.get().getPreferences(index).getEntityIdsList()) {
          entities.add(UserPreferenceProto.Entity.newBuilder()
                         .setEntityId(entityId)
                         .setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
                         .build());
        }

        List<UserPreferenceProto.Entity> validEntities = getValidEntities(entities);

        if (entities.size() != validEntities.size() ||
            userPreference.get().getPreferences(index).getEntityIdsList().size() > 0) { // validate both are equal
          // if there are invalid entities, remove invalid entities
          Preference updatedPreference =
            Preference.newBuilder()
              .setType(userPreference.get().getPreferences(index).getType())
              .clearEntityIds()
              .addAllEntities(validEntities)
              .build();

          UserPreferenceProto.UserPreference updatedUserPreference =
            userPreference.get().toBuilder().setPreferences(index, updatedPreference).build();
          updatedUserPreference =
            userPreferenceStore.update(getCurrentUserId().toString(), updatedUserPreference);
          return updatedUserPreference.getPreferences(index);
        }
        return userPreference.get().getPreferences(index);
      }
    }
    // if there is no items in preference, return empty list of preference.
    return newPreference(type, null);
  }

  protected List<UserPreferenceProto.Entity> getValidEntities(List<UserPreferenceProto.Entity> entities) {
    return entities.parallelStream().map(entity -> {
      try {
        validateEntityId(UUID.fromString(entity.getEntityId()));
        return entity;
      } catch (IllegalArgumentException | IllegalAccessException ignored) {
        return null;
      }
    }).filter(Objects::nonNull).collect(Collectors.toList());
  }

  @Override
  public UserPreferenceProto.Preference addEntityToPreference(UserPreferenceProto.PreferenceType type,
                                                              UUID entityId)
    throws EntityAlreadyInPreferenceException, EntityThresholdReachedException, IllegalAccessException {

    validateEntityId(entityId);
    String userId = getCurrentUserId().toString();
    Optional<UserPreferenceProto.UserPreference> existingUserPreference =
      userPreferenceStore.get(userId);
    if (!existingUserPreference.isPresent()) {
      // if no existing preference present
      Preference preference = newPreference(type, entityId);
      UserPreferenceProto.UserPreference createdPreference = userPreferenceStore.update(
        userId, newUserPreference(userId, preference));

      // return preference from create preference
      return createdPreference.getPreferences(getIndexOfPreferenceType(createdPreference, type));
    }

    int index = getIndexOfPreferenceType(existingUserPreference.get(), type);
    if (index == -1) {
      // if there is no preference of given type present in user preference
      UserPreferenceProto.UserPreference updatedUserPreference = userPreferenceStore.update(
        userId,
        existingUserPreference.get()
          .toBuilder()
          .addPreferences(newPreference(type, entityId))
          .build());
      return updatedUserPreference.getPreferences(getIndexOfPreferenceType(updatedUserPreference,
                                                                           type));
    }
    Preference existingPreferenceOfGivenType = existingUserPreference.get().getPreferences(index);


    List<UserPreferenceProto.Entity> entities =
      new ArrayList<>(existingPreferenceOfGivenType.getEntitiesList());
    Set<String> entityIds = existingPreferenceOfGivenType.getEntitiesList()
      .stream()
      .map(UserPreferenceProto.Entity::getEntityId)
      .collect(Collectors.toCollection(LinkedHashSet::new));
    if (entityIds.contains(entityId.toString())) {
      // throw exception if entity already present in preference
      throw new EntityAlreadyInPreferenceException(
        entityId.toString(),
        type.name().toLowerCase(Locale.ROOT));
    }
    // add new entity to list of given preference type
    Instant time = Instant.now();
    entities.add(UserPreferenceProto.Entity.newBuilder()
                   .setEntityId(entityId.toString())
                   .setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
                   .build());

    if (entities.size() > MAX_COUNT_OF_ENTITIES) {
      throw new EntityThresholdReachedException(MAX_COUNT_OF_ENTITIES,
                                                type.name().toLowerCase(Locale.ROOT));
    }

    Preference updatedPreference = Preference.newBuilder()
      .setType(existingPreferenceOfGivenType.getType())
      .addAllEntities(entities)
      .build();

    UserPreferenceProto.UserPreference updatedUserPreference =
      existingUserPreference.get().toBuilder().setPreferences(index, updatedPreference).build();

    return userPreferenceStore.update(userId, updatedUserPreference).getPreferences(index);
  }

  protected void validateEntityId(UUID entityId) throws IllegalAccessException {
    try {
      NameSpaceContainer container =
        namespaceServiceProvider.get().getEntityById(entityId.toString());
      if (container == null) {
        throw new IllegalArgumentException(String.format(
          "entityId %s provided is not a valid catalog entity.",
          entityId));
      }
    } catch (NamespaceNotFoundException exception) {
      throw new IllegalArgumentException(String.format(
        "entityId %s provided is not a valid catalog entity.",
        entityId));
    }
  }

  private UserPreferenceProto.UserPreference newUserPreference(String userId,
                                                               Preference preference) {
    Preconditions.checkNotNull(userId, "userId must not be null.");
    UserPreferenceProto.UserPreference userPreference =
      UserPreferenceProto.UserPreference.newBuilder().setUserId(userId).build();
    if (preference != null) {
      userPreference = userPreference.toBuilder().addPreferences(preference).build();
    }
    return userPreference;
  }

  @Override
  public UserPreferenceProto.Preference removeEntityFromPreference(UserPreferenceProto.PreferenceType type,
                                                                   UUID entityId)
    throws EntityNotFoundInPreferenceException {
    String userId = getCurrentUserId().toString();
    Optional<UserPreferenceProto.UserPreference> existingUserPreference =
      userPreferenceStore.get(userId);
    if (existingUserPreference.isPresent()) {
      int index = getIndexOfPreferenceType(existingUserPreference.get(), type);
      if (index != -1) {

        List<UserPreferenceProto.Entity> existingEntities =
          new ArrayList<>(existingUserPreference.get().getPreferences(index).getEntitiesList());
        UserPreferenceProto.Entity matchingEntity = existingEntities.stream()
          .filter(entity -> Objects.equals(entity.getEntityId(), entityId.toString()))
          .findFirst()
          .orElse(null);

        if (matchingEntity == null) {
          throw new EntityNotFoundInPreferenceException(
            entityId.toString(),
            type.name().toLowerCase(Locale.ROOT));
        }
        existingEntities.remove(matchingEntity);
        UserPreferenceProto.UserPreference updatedUserPreference =
          existingUserPreference.get()
            .toBuilder()
            .setPreferences(
              index,
              Preference.newBuilder()
                .setType(type)
                .addAllEntities(existingEntities)
                .build())
            .build();

        UserPreferenceProto.UserPreference updateUserPreference =
          userPreferenceStore.update(userId, updatedUserPreference);
        return updateUserPreference.getPreferences(index);
      }
    }
    throw new EntityNotFoundInPreferenceException(
      entityId.toString(),
      type.name().toLowerCase(Locale.ROOT));
  }

  private Preference newPreference(UserPreferenceProto.PreferenceType type, UUID entityId) {
    Preconditions.checkNotNull(type, "preference type must not be null");
    Preference preference = Preference.newBuilder().setType(type).build();
    if (entityId != null) {
      preference = preference.toBuilder()
        .addEntities(
          UserPreferenceProto.Entity.newBuilder()
            .setEntityId(entityId.toString())
            .setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
            .build())
        .build();
    }
    return preference;
  }

  private int getIndexOfPreferenceType(UserPreferenceProto.UserPreference userPreference,
                                       UserPreferenceProto.PreferenceType type) {
    List<Preference> preferenceList = userPreference.getPreferencesList();
    for (int i = 0; i < preferenceList.size(); i++) {
      if (preferenceList.get(i).getType() == type) {
        // return index of first matching preference type
        return i;
      }
    }
    // if preference of given type not present, return -1
    return -1;
  }

  protected UUID getCurrentUserId() {
    try {
      return UUID.fromString(userService.getUser(securityContext.getUserPrincipal().getName())
                               .getUID()
                               .getId());
    } catch (UserNotFoundException exception) {
      logger.error("User : {} not found. Error : {}",
                   securityContext.getUserPrincipal().getName(),
                   exception.getMessage());
      return null;
    }
  }
}
