/**
 * Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not
 * use this file except in compliance with the License. A copy of the License
 * is located at
 *
 *     http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */

package com.amazon.deequ.analyzers

import com.amazon.deequ.analyzers.Analyzers._
import com.amazon.deequ.analyzers.Preconditions.{hasColumn, isString}
import com.amazon.deequ.metrics.{DoubleMetric, Entity}
import org.apache.spark.sql.functions.{length, max}
import org.apache.spark.sql.types.{DoubleType, StructType}
import org.apache.spark.sql.{Column, Row}

import scala.util.{Failure, Success}

case class MaxLength(column: String,
                     where: Option[String] = None,
                     defaultMetricForNullValues: Option[Double] = None)
  extends StandardScanShareableAnalyzer[MaxState]("MaxLength", column)
  with FilterableAnalyzer {

  override def aggregationFunctions(): Seq[Column] = {
    max(length(conditionalSelection(column, where))).cast(DoubleType) :: Nil
  }

  override def fromAggregationResult(result: Row, offset: Int): Option[MaxState] = {
    ifNoNullsIn(result, offset) { _ =>
      MaxState(result.getDouble(offset))
    }
  }

  override def computeMetricFrom(state: Option[MaxState]): DoubleMetric = {
    state match {
      case Some(theState) =>
        DoubleMetric(Entity.Column, "MaxLength", column, Success(theState.maxValue))
      case _ if defaultMetricForNullValues.isDefined =>
        DoubleMetric(Entity.Column, "MaxLength", column, Success(defaultMetricForNullValues.get))
      case _ =>
        val exception = Analyzers.emptyStateException(this)
        DoubleMetric(Entity.Column, "MaxLength", column, Failure(exception))
    }
  }

  override protected def additionalPreconditions(): Seq[StructType => Unit] = {
    hasColumn(column):: isString(column) :: Nil
  }

  override def filterCondition: Option[String] = where
}
