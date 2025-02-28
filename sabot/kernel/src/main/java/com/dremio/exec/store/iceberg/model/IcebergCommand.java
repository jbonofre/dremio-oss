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
package com.dremio.exec.store.iceberg.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.arrow.vector.types.pojo.Field;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.ManifestFile;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.Table;
import org.apache.iceberg.types.Types;

import com.dremio.exec.catalog.PartitionSpecAlterOption;
import com.dremio.exec.record.BatchSchema;

/**
 * represents an Iceberg catalog
 */
public interface IcebergCommand {
    /**
     * Start of Create table command
     * @param tableName name of the table
     * @param writerSchema schema of the table
     * @param partitionColumns partition specification of the table
     * @param tableParameters icebeg table parameters
     */
    void beginCreateTableTransaction(String tableName, BatchSchema writerSchema, List<String> partitionColumns, Map<String, String> tableParameters, PartitionSpec partitionSpec);

    /**
     * Start of a tansaction
     */
    void beginTransaction();

    /**
     * End of a tansaction
     */

    Table endTransaction();

    /**
     * Start the overwrite operation
     */
    void beginOverwrite(long snapshotId);

    /**
     * Commit the overwrite operation
     */
    Snapshot finishOverwrite();

    /**
     * Consumes list of deleted data files using Overwrite
     * @param filesList list of DataFile entries
     */
    void consumeDeleteDataFilesWithOverwriteByPaths(List<String> filePathsList);

    /**
     * Consumes list of Manifest files using Overwrite
     * @param filesList list of DataFile entries
     */
    void consumeManifestFilesWithOverwrite(List<ManifestFile> filesList);

    /**
     * Start the delete operation
     */
    void beginDelete();

    /**
     * Commit the delete operation
     */
    Snapshot finishDelete();

    /**
     *  Start the insert operation
     */
    void beginInsert();

    /**
     *  Finish the insert operation
     */
    Snapshot finishInsert();

    /**
     * consumes list of snapshot ids that expire the current transaction
     * @param snapshotIds list of snapshot ids
     */
    void expireSnapshots(Set<Long> snapshotIds);

    /**
     * consumes list of Manifest files as part of the current transaction
     * @param filesList list of DataFile entries
     */
    void consumeManifestFiles(List<ManifestFile> filesList);

    /**
     * consumes list of data files to be deleted as a part of
     * the current transaction
     * @param filesList list of DataFile entries
     */
    void consumeDeleteDataFiles(List<DataFile> filesList);

    /**
     * consumes list of deleted data files by file paths as a part of
     * the current transaction
     * @param filePathsList list of data file paths
     */
    void consumeDeleteDataFilesByPaths(List<String> filePathsList);

    /**
     * consumes list of columns to be dropped
     * as part of metadata refresh transaction.
     * Used only in new metadata refresh flow
     */
    void consumeDroppedColumns(List<Types.NestedField> columns);

    /**
     * consumes list of columns to be updated
     * as part of metadata refresh transaction.
     * Used only in new metadata refresh flow
     */
    void consumeUpdatedColumns(List<Types.NestedField> columns);

    /**
     * consumes list of columns to be added to the schema
     * as part of metadata refresh transaction. Used
     * only in new metadata refresh flow
     */
    void consumeAddedColumns(List<Types.NestedField> columns);

    /**
     * truncates the table
     */
    void truncateTable();

    /**
     * adds new columns
     * @param columnsToAdd list of columns fields to add
     */
    void addColumns(List<Types.NestedField> columnsToAdd);

    /**
     * drop an existing column
     * @param columnToDrop existing column name
     */
    void dropColumn(String columnToDrop);

    /**
     * change column type
     * @param columnToChange existing column name
     * @param batchField new column type
     */
    void changeColumn(String columnToChange, Field batchField);

    /**
     * change column name
     * @param name existing column name
     * @param newName new column name
     */
    void renameColumn(String name, String newName);

  /**
   * Update primary key
   *
   * @param columns primary key column fields
   */
  void updatePrimaryKey(List<Field> columns);

  /**
   * Marks the transaction as a read-modify-write transaction. The transaction is expected
   * to add validation checks to ensure that the Iceberg table has not modified since the
   * read of the table
   *
   * Note: This should be the first update to the transaction. This should be invoked before
   * adding/deleting files or changing the schema of the table
   *
   * @param snapshotId The snapshotId that was used to read the transaction
   */
  Snapshot setIsReadModifyWriteTransaction(long snapshotId);

  /**
   * Load an Iceberg table from disk
   * @return Iceberg table instance
   */
   Table loadTable();

  /**
   * @return returns the latest snapshot on which the transaction is performed
   */
   Snapshot getCurrentSnapshot();

   /**
    * @return return Iceberg table metadata file location
    */
   String getRootPointer();

  /**
   * Delete the root pointer of the table
   *
   */
   void deleteTableRootPointer();

   void deleteTable();

    Map<Integer, PartitionSpec> getPartitionSpecMap();

    Schema getIcebergSchema();

    void beginAlterTableTransaction();

    Table endAlterTableTransaction();

    void addColumnsInternalTable(List<Field> columnsToAdd);

    void dropColumnInternalTable(String columnToDrop);

    void changeColumnForInternalTable(String columnToChange, Field batchField);

    void updatePropertiesMap(Map<String, String> propertiesMap);

    void updatePartitionSpec(PartitionSpecAlterOption partitionSpecAlterOption);
}
