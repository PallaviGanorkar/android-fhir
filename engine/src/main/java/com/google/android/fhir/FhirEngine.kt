/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.fhir

import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.db.impl.dao.LocalChangeToken
import com.google.android.fhir.db.impl.dao.SquashedLocalChange
import com.google.android.fhir.db.impl.entities.LocalChangeEntity
import com.google.android.fhir.search.Search
import java.time.OffsetDateTime
import kotlinx.coroutines.flow.Flow
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType

/** The FHIR Engine interface that handles the local storage of FHIR resources. */
interface FhirEngine {
  /**
   * Creates one or more FHIR [resource]s in the local storage.
   *
   * @return the logical IDs of the newly created resources.
   */
  suspend fun create(vararg resource: Resource): List<String>

  /** Loads a FHIR resource given the class and the logical ID. */
  suspend fun get(type: ResourceType, id: String): Resource

  /** Updates a FHIR [resource] in the local storage. */
  suspend fun update(vararg resource: Resource)

  /** Removes a FHIR resource given the class and the logical ID. */
  suspend fun delete(type: ResourceType, id: String)

  /**
   * Searches the database and returns a list resources according to the [search] specifications.
   */
  suspend fun <R : Resource> search(search: Search): List<R>

  /**
   * Synchronizes the [upload] result in the database. [upload] operation may result in multiple
   * calls to the server to upload the data. Result of each call will be emitted by [upload] and the
   * api caller should [Flow.collect] it.
   */
  suspend fun syncUpload(
    upload: (suspend (List<SquashedLocalChange>) -> Flow<Pair<LocalChangeToken, Resource>>)
  )

  /**
   * Synchronizes the [download] result in the database. The database will be updated to reflect the
   * result of the [download] operation.
   */
  suspend fun syncDownload(download: suspend (SyncDownloadContext) -> Flow<List<Resource>>)

  /**
   * Returns the total count of entities available for given search.
   *
   * @param search
   */
  suspend fun count(search: Search): Long

  /** Returns the timestamp when data was last synchronized. */
  suspend fun getLastSyncTimeStamp(): OffsetDateTime?

  /**
   * Clears all database tables without resetting the auto-increment value generated by
   * PrimaryKey.autoGenerate.
   *
   * WARNING: This will clear the database and it's not recoverable.
   */
  suspend fun clearDatabase()

  /**
   * Retrieve [LocalChangeEntity] for [Resource] with given type and id, which can be used to purge
   * resource from database. Each resource will have at most one
   * [LocalChangeEntity](multiple
   * changes are squashed). If there is no local change for given
   * [resourceType] and [Resource.id], return `null`.
   * @param type The [ResourceType]
   * @param id The resource id [Resource.id]
   * @return [LocalChangeEntity] A squashed local changes for given [resourceType] and [Resource.id]
   * . If there is no local change for given [resourceType] and [Resource.id], return `null`.
   */
  suspend fun getLocalChange(type: ResourceType, id: String): LocalChangeEntity?

  /**
   * Purges a resource from database based on resource type and id without any deletion of data from
   * the server.
   * @param type The [ResourceType]
   * @param id The resource id [Resource.id]
   * @param isLocalPurge default value is false here resource will not be deleted from
   * LocalChangeEntity table but it will throw IllegalStateException("Resource has local changes
   * either sync with server or FORCE_PURGE required") if local change exists. If true this API will
   * delete resource entry from LocalChangeEntity table.
   */
  suspend fun purge(type: ResourceType, id: String, forcePurge: Boolean = false)
}

/**
 * Returns a FHIR resource of type [R] with [id] from the local storage.
 * @param <R> The resource type which should be a subtype of [Resource].
 * @throws ResourceNotFoundException if the resource is not found
 */
@Throws(ResourceNotFoundException::class)
suspend inline fun <reified R : Resource> FhirEngine.get(id: String): R {
  return get(getResourceType(R::class.java), id) as R
}

/**
 * Deletes a FHIR resource of type [R] with [id] from the local storage.
 * @param <R> The resource type which should be a subtype of [Resource].
 */
suspend inline fun <reified R : Resource> FhirEngine.delete(id: String) {
  delete(getResourceType(R::class.java), id)
}

interface SyncDownloadContext {
  suspend fun getLatestTimestampFor(type: ResourceType): String?
}
