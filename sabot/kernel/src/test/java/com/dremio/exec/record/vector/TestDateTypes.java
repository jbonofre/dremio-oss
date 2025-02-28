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
package com.dremio.exec.record.vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;

import org.apache.arrow.vector.ValueVector;
import org.junit.Ignore;
import org.junit.Test;

import com.dremio.common.util.FileUtils;
import com.dremio.exec.client.DremioClient;
import com.dremio.exec.pop.PopUnitTestBase;
import com.dremio.exec.record.RecordBatchLoader;
import com.dremio.exec.record.VectorWrapper;
import com.dremio.exec.server.SabotNode;
import com.dremio.sabot.rpc.user.QueryDataBatch;
import com.dremio.service.coordinator.ClusterCoordinator;
import com.dremio.service.coordinator.local.LocalClusterCoordinator;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

/* This class tests the existing date types. Simply using date types
 * by casting from VarChar, performing basic functions and converting
 * back to VarChar.
 */

@Ignore("DX-3872")
public class TestDateTypes extends PopUnitTestBase {

    @Test
    public void testDate() throws Exception {
        try (ClusterCoordinator clusterCoordinator = LocalClusterCoordinator.newRunningCoordinator();
             SabotNode bit = new SabotNode(DEFAULT_SABOT_CONFIG, clusterCoordinator, CLASSPATH_SCAN_RESULT, true);
             DremioClient client = new DremioClient(DEFAULT_SABOT_CONFIG, clusterCoordinator)) {

            // run query.
            bit.run();
            client.connect();
            List<QueryDataBatch> results = client.runQuery(com.dremio.exec.proto.UserBitShared.QueryType.PHYSICAL,
                    Files.toString(FileUtils.getResourceAsFile("/record/vector/test_date.json"), Charsets.UTF_8)
                            .replace("#{TEST_FILE}", "/test_simple_date.json"));

            RecordBatchLoader batchLoader = new RecordBatchLoader(bit.getContext().getAllocator());

            QueryDataBatch batch = results.get(0);
            assertTrue(batchLoader.load(batch.getHeader().getDef(), batch.getData()));

            for (VectorWrapper<?> v : batchLoader) {

                ValueVector vv = v.getValueVector();

                assertEquals((vv.getObject(0).toString()), ("1970-01-02"));
                assertEquals((vv.getObject(1).toString()), ("2008-12-28"));
                assertEquals((vv.getObject(2).toString()), ("2000-02-27"));
            }

            batchLoader.clear();
            for(QueryDataBatch b : results){
              b.release();
            }
        }
    }

    @Test
    public void testSortDate() throws Exception {
        try (ClusterCoordinator clusterCoordinator = LocalClusterCoordinator.newRunningCoordinator();
             SabotNode bit = new SabotNode(DEFAULT_SABOT_CONFIG, clusterCoordinator, CLASSPATH_SCAN_RESULT, true);
             DremioClient client = new DremioClient(DEFAULT_SABOT_CONFIG, clusterCoordinator)) {

            // run query.
            bit.run();
            client.connect();
            List<QueryDataBatch> results = client.runQuery(com.dremio.exec.proto.UserBitShared.QueryType.PHYSICAL,
                    Files.toString(FileUtils.getResourceAsFile("/record/vector/test_sort_date.json"), Charsets.UTF_8)
                            .replace("#{TEST_FILE}", "/test_simple_date.json"));

            RecordBatchLoader batchLoader = new RecordBatchLoader(bit.getContext().getAllocator());

            QueryDataBatch batch = results.get(1);
            assertTrue(batchLoader.load(batch.getHeader().getDef(), batch.getData()));

            for (VectorWrapper<?> v : batchLoader) {

                ValueVector vv = v.getValueVector();

                assertEquals((vv.getObject(0).toString()), new String("1970-01-02"));
                assertEquals((vv.getObject(1).toString()), new String("2000-02-27"));
                assertEquals((vv.getObject(2).toString()), new String("2008-12-28"));
            }

            batchLoader.clear();
            for(QueryDataBatch b : results){
              b.release();
            }
        }
    }

    @Test
    public void testTimeStamp() throws Exception {
        try (ClusterCoordinator clusterCoordinator = LocalClusterCoordinator.newRunningCoordinator();
             SabotNode bit = new SabotNode(DEFAULT_SABOT_CONFIG, clusterCoordinator, CLASSPATH_SCAN_RESULT, true);
             DremioClient client = new DremioClient(DEFAULT_SABOT_CONFIG, clusterCoordinator)) {

            // run query.
            bit.run();
            client.connect();
            List<QueryDataBatch> results = client.runQuery(com.dremio.exec.proto.UserBitShared.QueryType.PHYSICAL,
                    Files.toString(FileUtils.getResourceAsFile("/record/vector/test_timestamp.json"), Charsets.UTF_8)
                            .replace("#{TEST_FILE}", "/test_simple_date.json"));

            RecordBatchLoader batchLoader = new RecordBatchLoader(bit.getContext().getAllocator());

            QueryDataBatch batch = results.get(0);
            assertTrue(batchLoader.load(batch.getHeader().getDef(), batch.getData()));

            for (VectorWrapper<?> v : batchLoader) {

                ValueVector vv = v.getValueVector();

                assertEquals(vv.getObject(0).toString() ,"1970-01-02 10:20:33.000");
                assertEquals(vv.getObject(1).toString() ,"2008-12-28 11:34:00.129");
                assertEquals(vv.getObject(2).toString(), "2000-02-27 14:24:00.000");
            }

            batchLoader.clear();
            for(QueryDataBatch b : results){
              b.release();
            }
        }
    }

    @Test
    @Ignore("interval")
    public void testInterval() throws Exception {
        try (ClusterCoordinator clusterCoordinator = LocalClusterCoordinator.newRunningCoordinator();
             SabotNode bit = new SabotNode(DEFAULT_SABOT_CONFIG, clusterCoordinator, CLASSPATH_SCAN_RESULT, true);
             DremioClient client = new DremioClient(DEFAULT_SABOT_CONFIG, clusterCoordinator)) {

            // run query.
            bit.run();
            client.connect();
            List<QueryDataBatch> results = client.runQuery(com.dremio.exec.proto.UserBitShared.QueryType.PHYSICAL,
                    Files.toString(FileUtils.getResourceAsFile("/record/vector/test_interval.json"), Charsets.UTF_8)
                            .replace("#{TEST_FILE}", "/test_simple_interval.json"));

            RecordBatchLoader batchLoader = new RecordBatchLoader(bit.getContext().getAllocator());

            QueryDataBatch batch = results.get(0);
            assertTrue(batchLoader.load(batch.getHeader().getDef(), batch.getData()));

            Iterator<VectorWrapper<?>> itr = batchLoader.iterator();

            ValueVector vv = itr.next().getValueVector();

            // Check the interval type
            assertEquals((vv.getObject(0).toString()), ("2 years 2 months 1 day 1:20:35.0"));
            assertEquals((vv.getObject(1).toString()), ("2 years 2 months 0 days 0:0:0.0"));
            assertEquals((vv.getObject(2).toString()), ("0 years 0 months 0 days 1:20:35.0"));
            assertEquals((vv.getObject(3).toString()),("2 years 2 months 1 day 1:20:35.897"));
            assertEquals((vv.getObject(4).toString()), ("0 years 0 months 0 days 0:0:35.4"));
            assertEquals((vv.getObject(5).toString()), ("1 year 10 months 1 day 0:-39:-25.0"));

            vv = itr.next().getValueVector();

            // Check the interval year type
            assertEquals((vv.getObject(0).toString()), ("2 years 2 months "));
            assertEquals((vv.getObject(1).toString()), ("2 years 2 months "));
            assertEquals((vv.getObject(2).toString()), ("0 years 0 months "));
            assertEquals((vv.getObject(3).toString()), ("2 years 2 months "));
            assertEquals((vv.getObject(4).toString()), ("0 years 0 months "));
            assertEquals((vv.getObject(5).toString()), ("1 year 10 months "));

            vv = itr.next().getValueVector();

            // Check the interval day type
            assertEquals((vv.getObject(0).toString()), ("1 day 1:20:35.0"));
            assertEquals((vv.getObject(1).toString()), ("0 days 0:0:0.0"));
            assertEquals((vv.getObject(2).toString()), ("0 days 1:20:35.0"));
            assertEquals((vv.getObject(3).toString()), ("1 day 1:20:35.897"));
            assertEquals((vv.getObject(4).toString()), ("0 days 0:0:35.4"));
            assertEquals((vv.getObject(5).toString()), ("1 day 0:-39:-25.0"));

            batchLoader.clear();
            for(QueryDataBatch b : results){
              b.release();
            }
        }
    }

    @Test
    @Ignore("interval")
    public void testLiterals() throws Exception {
        try (ClusterCoordinator clusterCoordinator = LocalClusterCoordinator.newRunningCoordinator();
             SabotNode bit = new SabotNode(DEFAULT_SABOT_CONFIG, clusterCoordinator, CLASSPATH_SCAN_RESULT, true);
             DremioClient client = new DremioClient(DEFAULT_SABOT_CONFIG, clusterCoordinator)) {

            // run query.
            bit.run();
            client.connect();
            List<QueryDataBatch> results = client.runQuery(com.dremio.exec.proto.UserBitShared.QueryType.PHYSICAL,
                    Files.toString(FileUtils.getResourceAsFile("/record/vector/test_all_date_literals.json"), Charsets.UTF_8)
                            .replace("#{TEST_FILE}", "/test_simple_date.json"));

            RecordBatchLoader batchLoader = new RecordBatchLoader(bit.getContext().getAllocator());

            QueryDataBatch batch = results.get(0);
            assertTrue(batchLoader.load(batch.getHeader().getDef(), batch.getData()));

            String[] result = {"2008-02-27",
                               "2008-02-27 01:02:03.000",
                               "10:11:13.999",
                               "2 years 2 months 3 days 0:1:3.89"};

            int idx = 0;

            for (VectorWrapper<?> v : batchLoader) {
                ValueVector vv = v.getValueVector();
                assertEquals((vv.getObject(0).toString()), (result[idx++]));
            }

            batchLoader.clear();
            for(QueryDataBatch b : results){
              b.release();
            }
        }
    }

    @Test
    @Ignore("interval")
    public void testDateAdd() throws Exception {
        try (ClusterCoordinator clusterCoordinator = LocalClusterCoordinator.newRunningCoordinator();
             SabotNode bit = new SabotNode(DEFAULT_SABOT_CONFIG, clusterCoordinator, CLASSPATH_SCAN_RESULT, true);
             DremioClient client = new DremioClient(DEFAULT_SABOT_CONFIG, clusterCoordinator)) {

            // run query.
            bit.run();
            client.connect();
            List<QueryDataBatch> results = client.runQuery(com.dremio.exec.proto.UserBitShared.QueryType.PHYSICAL,
                    Files.toString(FileUtils.getResourceAsFile("/record/vector/test_date_add.json"), Charsets.UTF_8)
                            .replace("#{TEST_FILE}", "/test_simple_date.json"));

            RecordBatchLoader batchLoader = new RecordBatchLoader(bit.getContext().getAllocator());

            QueryDataBatch batch = results.get(0);
            assertTrue(batchLoader.load(batch.getHeader().getDef(), batch.getData()));

            for (VectorWrapper<?> v : batchLoader) {
                ValueVector vv = v.getValueVector();
                assertEquals((vv.getObject(0).toString()), ("2008-03-27 00:00:00.000"));
            }

            batchLoader.clear();
            for(QueryDataBatch b : results){
              b.release();
            }
        }
    }
}
