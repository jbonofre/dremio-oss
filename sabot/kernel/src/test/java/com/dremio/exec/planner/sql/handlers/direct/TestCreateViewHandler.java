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
package com.dremio.exec.planner.sql.handlers.direct;

import static com.dremio.exec.ExecConstants.VERSIONED_VIEW_ENABLED;
import static com.dremio.exec.proto.UserBitShared.DremioPBError.ErrorType.VALIDATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.anySet;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.calcite.schema.Schema;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.dialect.CalciteSqlDialect;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.util.SqlString;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import com.dremio.exec.catalog.Catalog;
import com.dremio.exec.catalog.DremioTable;
import com.dremio.exec.catalog.ResolvedVersionContext;
import com.dremio.exec.catalog.VersionContext;
import com.dremio.exec.dotfile.View;
import com.dremio.exec.ops.QueryContext;
import com.dremio.exec.physical.base.ViewOptions;
import com.dremio.exec.planner.sql.handlers.SqlHandlerConfig;
import com.dremio.exec.planner.sql.parser.SqlCreateView;
import com.dremio.exec.record.BatchSchema;
import com.dremio.options.OptionManager;
import com.dremio.sabot.rpc.user.UserSession;
import com.dremio.service.namespace.NamespaceKey;
import com.dremio.test.DremioTest;
import com.dremio.test.UserExceptionAssert;

/**
 *  This test will only include unit tests for versioned views
 */
public class TestCreateViewHandler extends DremioTest {

  private static final String DEFAULT_SOURCE_NAME = "dataplane_source_1";
  private static final String VIEW_PATH = "dataplane_source_1.view1";
  private static final String DEFAULT_BRANCH_NAME = "branchName";
  private static final NamespaceKey DEFAULT_NAMESPACE_KEY = new NamespaceKey(DEFAULT_SOURCE_NAME);
  private static final NamespaceKey DEFAULT_SCHEMA = new NamespaceKey(Arrays.asList(DEFAULT_SOURCE_NAME));
  private static final VersionContext DEFAULT_VERSION =
    VersionContext.ofBranch(DEFAULT_BRANCH_NAME);
  private static final ResolvedVersionContext DEFAULT_RESOLVED_VERSION_CONTEXT =
    ResolvedVersionContext.ofBranch(DEFAULT_BRANCH_NAME, "0123456789abcdeff");
  private static final String DEFAULT_SQL = "select * from dataplane_source_1.t1";
  private static final SqlNodeList SQL_NODE_LIST = new SqlNodeList(Collections.emptyList(), SqlParserPos.ZERO);
  private static SqlNode sqlNode = mock(SqlNode.class);
  private static SqlCreateView default_input = new SqlCreateView(
    SqlParserPos.ZERO,
    new SqlIdentifier(VIEW_PATH, SqlParserPos.ZERO),
    SQL_NODE_LIST,
    sqlNode,
    false,
    null
  );

  private static SqlCreateView default_replace_input = new SqlCreateView(
    SqlParserPos.ZERO,
    new SqlIdentifier(VIEW_PATH, SqlParserPos.ZERO),
    SQL_NODE_LIST,
    sqlNode,
    true,
    null
  );

  private static SqlCreateView invalid_input = new SqlCreateView(
    SqlParserPos.ZERO,
    new SqlIdentifier(VIEW_PATH, SqlParserPos.ZERO),
    SQL_NODE_LIST,
    sqlNode,
    true,
    null
  );
  @Rule
  public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  @Mock private Catalog catalog;
  @Mock private UserSession userSession;
  @Mock private SqlHandlerConfig config;
  @Mock private QueryContext context;
  @Mock private SqlString queryString;
  @Mock private OptionManager optionManager;
  @Mock private View view;

  private BatchSchema batchSchema = new BatchSchema(new ArrayList<>());
  private ViewOptions createViewOptions = new ViewOptions.ViewOptionsBuilder()
    .version(DEFAULT_RESOLVED_VERSION_CONTEXT)
    .batchSchema(batchSchema)
    .build();
  private ViewOptions replaceViewOptions = new ViewOptions.ViewOptionsBuilder()
    .version(DEFAULT_RESOLVED_VERSION_CONTEXT)
    .batchSchema(batchSchema)
    .viewUpdate(true)
    .build();
  private CreateViewHandler createViewHandler;

  @Test
  public void createVersionedViewSuccessful() throws Exception {
    setupResources();
    doReturn(createViewOptions).when(createViewHandler).getViewOptions(DEFAULT_NAMESPACE_KEY, false);
    doReturn(view).when(createViewHandler).getView(default_input);
    doNothing().when(catalog).createView(DEFAULT_NAMESPACE_KEY, view, createViewOptions);

    List<SimpleCommandResult> result = createViewHandler.toResult("", default_input);
    assertThat(result).isNotEmpty();
    assertThat(result.get(0).ok).isTrue();
    assertThat(result.get(0).summary)
      .contains("created successfully")
      .contains("View");
  }

  @Test
  public void testCreateViewTableNotFound() throws Exception {
    setupResources();

    // Cannot find relNode (Table) if we don't set up one in test
    assertThatThrownBy(() -> createViewHandler.toResult("", default_input))
      .hasMessageContaining("Cannot find table to create view from");
  }

  @Test
  public void testCreateViewInvalidQueryDrop() throws Exception {
    testCreateViewInvalidQuery(SqlKind.DROP_TABLE);
  }

  @Test
  public void testCreateViewInvalidQueryCreate() throws Exception {
    testCreateViewInvalidQuery(SqlKind.CREATE_TABLE);
  }

  @Test
  public void testCreateViewInvalidQueryUpdate() throws Exception {
    testCreateViewInvalidQuery(SqlKind.UPDATE);
  }

  @Test
  public void testCreateViewInvalidQueryInsert() throws Exception {
    testCreateViewInvalidQuery(SqlKind.INSERT);
  }

  @Test
  public void testCreateViewInvalidQueryMerge() throws Exception {
    testCreateViewInvalidQuery(SqlKind.MERGE);
  }

  @Test
  public void testCreateViewInvalidQueryDelete() throws Exception {
    testCreateViewInvalidQuery(SqlKind.DELETE);
  }

  private void testCreateViewInvalidQuery(SqlKind kind) throws Exception {
    when(context.getCatalog()).thenReturn(catalog);
    when(config.getContext()).thenReturn(context);
    when(optionManager.getOption(VERSIONED_VIEW_ENABLED)).thenReturn(true);
    when(sqlNode.toSqlString(CalciteSqlDialect.DEFAULT)).thenReturn(queryString);
    when(sqlNode.isA(anySet())).thenCallRealMethod();
    when(sqlNode.getKind()).thenReturn(kind);
    when(context.getOptions()).thenReturn(optionManager);
    when(config.getContext()).thenReturn(context);
    createViewHandler = spy(new CreateViewHandler(config));
    assertThatThrownBy(() -> createViewHandler.toResult("", invalid_input))
      .hasMessageContaining("Cannot create view on statement of this type");
  }

  @Test
  public void replaceVersionedViewSuccessful() throws Exception {
    setupResources();
    doReturn(replaceViewOptions).when(createViewHandler).getViewOptions(DEFAULT_NAMESPACE_KEY, true);
    doReturn(true).when(createViewHandler).checkViewExistence(DEFAULT_NAMESPACE_KEY,VIEW_PATH, true);
    doReturn(view).when(createViewHandler).getView(default_replace_input);
    doNothing().when(catalog).updateView(DEFAULT_NAMESPACE_KEY, view, replaceViewOptions);

    List<SimpleCommandResult> result = createViewHandler.toResult("", default_replace_input);
    assertThat(result).isNotEmpty();
    assertThat(result.get(0).ok).isTrue();
    assertThat(result.get(0).summary)
      .contains("replaced successfully")
      .contains("View");
  }

  @Test
  public void replaceVersionedViewNameClash() throws Exception {
    setupResources();
    DremioTable dremioTable = mock(DremioTable.class);
    Schema.TableType tableType = Schema.TableType.TABLE;
    doReturn(view).when(createViewHandler).getView(default_replace_input);
    doReturn(dremioTable).when(catalog).getTableNoResolve(DEFAULT_NAMESPACE_KEY);
    doReturn(tableType).when(dremioTable).getJdbcTableType();

    UserExceptionAssert.assertThatThrownBy(() -> createViewHandler.toResult("",default_replace_input))
      .hasErrorType(VALIDATION)
      .hasMessageContaining("A non-view table with given name")
      .hasMessageContaining("already exists in schema");

  }

  private void setupResources() throws SqlParseException {
    when(catalog.resolveSingle(default_input.getPath())).thenReturn(DEFAULT_NAMESPACE_KEY);
    when(context.getCatalog()).thenReturn(catalog);
    when(optionManager.getOption(VERSIONED_VIEW_ENABLED)).thenReturn(true);
    when(context.getOptions()).thenReturn(optionManager);
    when(config.getContext()).thenReturn(context);
    when(sqlNode.toSqlString(CalciteSqlDialect.DEFAULT)).thenReturn(queryString);
    when(sqlNode.toSqlString(CalciteSqlDialect.DEFAULT, true)).thenReturn(queryString);
    when(sqlNode.getKind()).thenReturn(SqlKind.SELECT);
    createViewHandler = spy(new CreateViewHandler(config));
    // versioned view test only
    doReturn(true).when(createViewHandler).isVersioned(DEFAULT_NAMESPACE_KEY);
  }
}
