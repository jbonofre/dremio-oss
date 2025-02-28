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
package com.dremio.exec.expr.fn.impl;

import java.util.List;

import org.apache.arrow.vector.complex.reader.FieldReader;
import org.apache.arrow.vector.complex.writer.BaseWriter;
import org.apache.arrow.vector.holders.NullableIntHolder;
import org.apache.arrow.vector.holders.NullableVarCharHolder;
import org.apache.arrow.vector.types.pojo.Field;

import com.dremio.common.exceptions.UserException;
import com.dremio.common.expression.CompleteType;
import com.dremio.common.expression.LogicalExpression;
import com.dremio.exec.expr.SimpleFunction;
import com.dremio.exec.expr.annotations.FunctionTemplate;
import com.dremio.exec.expr.annotations.FunctionTemplate.NullHandling;
import com.dremio.exec.expr.annotations.Output;
import com.dremio.exec.expr.annotations.Param;
import com.dremio.exec.expr.fn.OutputDerivation;
import com.google.common.base.Preconditions;

public class MapFunctions {
  public static final String FIRST_MATCHING_ENTRY_FUNC = "first_matching_map_entry_for_key";
  public static final String LAST_MATCHING_ENTRY_FUNC = "last_matching_map_entry_for_key";

  // Outputs the first map entry which has matching key with the given key (KeyStr) in the given map (input). Does nothing if there is no matching map entry for the key.
  @FunctionTemplate(names = {FIRST_MATCHING_ENTRY_FUNC}, isDeterministic = false, scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = NullHandling.INTERNAL, derivation = KeyValueOutputFirstMatching.class)
  public static class GetFirstMatchingMapEntryForKey implements SimpleFunction {

    @Param
    FieldReader input;
    @Param
    NullableVarCharHolder keyStr; //key is a string.
    @Output
    BaseWriter.ComplexWriter out;

    public void setup() {
    }

    @Override
    public void eval() {
      org.apache.arrow.vector.complex.writer.BaseWriter.StructWriter structWriter = out.rootAsStruct();
      if (input.isSet() && keyStr.isSet == 1) {
        org.apache.arrow.vector.complex.impl.UnionMapReader mapReader = (org.apache.arrow.vector.complex.impl.UnionMapReader) input;
        while (mapReader.next()) {
          NullableVarCharHolder keyHolder = new NullableVarCharHolder();
          mapReader.key().read(keyHolder);
          boolean isEqual = com.dremio.exec.expr.fn.impl.StringFunctionHelpers.equalsIgnoreCase(
            keyHolder.buffer, keyHolder.start, keyHolder.end, keyStr.buffer, keyStr.start, keyStr.end);
          if (isEqual) {
            org.apache.arrow.vector.complex.impl.ComplexCopier.copy(mapReader.reader(), (org.apache.arrow.vector.complex.writer.FieldWriter) structWriter);
            return;
          }
        }
      }
    }
  }

  // Outputs the last map entry which has matching key with the given key (KeyStr) in the given map (input). Does nothing if there is no matching map entry for the key.
  @FunctionTemplate(names = {LAST_MATCHING_ENTRY_FUNC}, isDeterministic = false, scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = NullHandling.INTERNAL, derivation = KeyValueOutputLastMatching.class)
  public static class GetLastMatchingMapEntryForKey implements SimpleFunction {

    @Param
    FieldReader input;
    @Param
    NullableVarCharHolder keyStr; //key is a string.
    @Output
    BaseWriter.ComplexWriter out;

    public void setup() {
    }

    @Override
    public void eval() {
      org.apache.arrow.vector.complex.writer.BaseWriter.StructWriter structWriter = out.rootAsStruct();
      if (input.isSet() && keyStr.isSet == 1) {
        org.apache.arrow.vector.complex.impl.UnionMapReader mapReader = (org.apache.arrow.vector.complex.impl.UnionMapReader) input;
        int iteratorIdx = 0;
        int lastMatchedIdx = -1;
        while (mapReader.next()) {
          NullableVarCharHolder keyHolder = new NullableVarCharHolder();
          mapReader.key().read(keyHolder);
          boolean isEqual = com.dremio.exec.expr.fn.impl.StringFunctionHelpers.equalsIgnoreCase(
            keyHolder.buffer, keyHolder.start, keyHolder.end, keyStr.buffer, keyStr.start, keyStr.end);
          if (isEqual) {
            lastMatchedIdx = iteratorIdx;
          }
          iteratorIdx++;
        }
        if (lastMatchedIdx != -1) {
          org.apache.arrow.vector.holders.UnionHolder mapEntryHolder = new org.apache.arrow.vector.holders.UnionHolder();
          mapReader.read(lastMatchedIdx, mapEntryHolder);
          org.apache.arrow.vector.complex.impl.ComplexCopier.copy(mapEntryHolder.reader, (org.apache.arrow.vector.complex.writer.FieldWriter) structWriter);
        }
      }
    }
  }

  @FunctionTemplate(names = {"map_keys"}, scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL, isDeterministic = false, derivation = ListOfKeys.class)
  public static class GetMapKeys implements SimpleFunction {
    @Param
    FieldReader input;

    @Output
    BaseWriter.ComplexWriter out;

    public void setup() {
    }

    @Override
    public void eval() {
      org.apache.arrow.vector.complex.impl.UnionMapReader mapReader = (org.apache.arrow.vector.complex.impl.UnionMapReader) input;
      org.apache.arrow.vector.complex.writer.BaseWriter.ListWriter listWriter = out.rootAsList();
      listWriter.startList();
      while (mapReader.next()) {
        org.apache.arrow.vector.complex.impl.ComplexCopier.copy(mapReader.key(), (org.apache.arrow.vector.complex.writer.FieldWriter) listWriter);
      }
      listWriter.endList();
    }
  }

  @FunctionTemplate(names = {"map_values"}, scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL, isDeterministic = false, derivation = ListOfValues.class)
  public static class GetMapValues implements SimpleFunction {
    @Param
    FieldReader input;

    @Output
    BaseWriter.ComplexWriter out;

    public void setup() {
    }

    @Override
    public void eval() {
      org.apache.arrow.vector.complex.impl.UnionMapReader mapReader = (org.apache.arrow.vector.complex.impl.UnionMapReader) input;
      org.apache.arrow.vector.complex.writer.BaseWriter.ListWriter listWriter = out.rootAsList();
      listWriter.startList();
      while (mapReader.next()) {
        org.apache.arrow.vector.complex.impl.ComplexCopier.copy(mapReader.value(), (org.apache.arrow.vector.complex.writer.FieldWriter) listWriter);
      }
      listWriter.endList();
    }
  }

  @FunctionTemplate(names = {"size"}, scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = NullHandling.INTERNAL)
  public static class GetMapSize implements SimpleFunction {
    @Param
    FieldReader input;

    @Output
    NullableIntHolder out;

    public void setup() {
    }

    @Override
    public void eval() {
      if (input.isSet()) {
        out.value = input.size();
        out.isSet = 1;
        return;
      }
      out.value = -1;
      out.isSet = 1;
    }
  }

  public static class KeyValueOutputFirstMatching implements OutputDerivation {
    public CompleteType getOutputType(CompleteType baseReturn, List<LogicalExpression> args) {
      Field entryStruct = getEntryStruct(args, "GetFirstMatchingMapEntryForKey");
      return CompleteType.fromField(entryStruct);
    }
  }

  public static class KeyValueOutputLastMatching implements OutputDerivation {
    public CompleteType getOutputType(CompleteType baseReturn, List<LogicalExpression> args) {
      Field entryStruct = getEntryStruct(args, "GetLastMatchingMapEntryForKey");
      return CompleteType.fromField(entryStruct);
    }
  }

  public static class ListOfKeys implements OutputDerivation {
    public CompleteType getOutputType(CompleteType baseReturn, List<LogicalExpression> args) {
      Field entryStruct = getEntryStruct(args, "getMapKeys");
      return CompleteType.fromField(entryStruct.getChildren().get(0)).asList();
    }
  }

  public static class ListOfValues implements OutputDerivation {
    public CompleteType getOutputType(CompleteType baseReturn, List<LogicalExpression> args) {
      Field entryStruct = getEntryStruct(args, "getMapValues");
      return CompleteType.fromField(entryStruct.getChildren().get(1)).asList();
    }
  }

  private static Field getEntryStruct(List<LogicalExpression> args, String functionName) {
    CompleteType mapType = args.get(0).getCompleteType();
    if (!mapType.isMap()) {
      throw UserException.functionError()
        .message("The %s function can only be used when operating against maps. The type you were attempting to apply it to was a %s.", functionName,
          mapType.toString())
        .build();
    }
    Preconditions.checkArgument(mapType.getChildren().size() == 1, "Unexpected map structure %s", mapType.toString());
    Field entryStruct = mapType.getChildren().get(0);
    Preconditions.checkArgument(entryStruct.getChildren().size() == 2, "Unexpected entry in map structure %s", entryStruct.toString());
    return entryStruct;
  }
}
