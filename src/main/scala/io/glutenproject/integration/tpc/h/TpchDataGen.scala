/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.glutenproject.integration.tpc.h

import io.glutenproject.integration.tpc.TableGen
import io.trino.tpch._
import org.apache.spark.sql.catalyst.encoders.RowEncoder
import org.apache.spark.sql.types._
import org.apache.spark.sql.{Row, SaveMode, SparkSession}

import java.io.File
import java.sql.Date

import scala.collection.JavaConverters._

class TpchDataGen(val spark: SparkSession, scale: Double, partitions: Int, path: String,
    typeModifiers: Array[TypeModifier] = Array())
    extends Serializable with TableGen {

  private val typeMapping: java.util.Map[DataType, TypeModifier] = new java.util.HashMap()

  typeModifiers.foreach { m =>
    if (typeMapping.containsKey(m.from)) {
      throw new IllegalStateException()
    }
    typeMapping.put(m.from, m)
  }

  override def gen(): Unit = {
    generate(path, "lineitem", lineItemSchema, partitions, lineItemGenerator, lineItemParser)
    generate(path, "customer", customerSchema, partitions, customerGenerator, customerParser)
    generate(path, "orders", orderSchema, partitions, orderGenerator, orderParser)
    generate(path, "partsupp", partSupplierSchema, partitions, partSupplierGenerator, partSupplierParser)
    generate(path, "supplier", supplierSchema, partitions, supplierGenerator, supplierParser)
    generate(path, "nation", nationSchema, nationGenerator, nationParser)
    generate(path, "part", partSchema, partitions, partGenerator, partParser)
    generate(path, "region", regionSchema, regionGenerator, regionParser)
  }

  // lineitem
  private def lineItemGenerator = { (part: Int, partCount: Int) =>
    new LineItemGenerator(scale, part, partCount)
  }

  private def lineItemSchema = {
    StructType(Seq(
      StructField("l_orderkey", LongType),
      StructField("l_partkey", LongType),
      StructField("l_suppkey", LongType),
      StructField("l_linenumber", IntegerType),
      StructField("l_quantity", LongType),
      StructField("l_extendedprice", DoubleType),
      StructField("l_discount", DoubleType),
      StructField("l_tax", DoubleType),
      StructField("l_returnflag", StringType),
      StructField("l_linestatus", StringType),
      StructField("l_commitdate", DateType),
      StructField("l_receiptdate", DateType),
      StructField("l_shipinstruct", StringType),
      StructField("l_shipmode", StringType),
      StructField("l_comment", StringType),
      StructField("l_shipdate", DateType)
    ))
  }

  private def lineItemParser: LineItem => Row =
    lineItem =>
      Row(
        lineItem.getOrderKey,
        lineItem.getPartKey,
        lineItem.getSupplierKey,
        lineItem.getLineNumber,
        lineItem.getQuantity,
        lineItem.getExtendedPrice,
        lineItem.getDiscount,
        lineItem.getTax,
        lineItem.getReturnFlag,
        lineItem.getStatus,
        Date.valueOf(GenerateUtils.formatDate(lineItem.getCommitDate)),
        Date.valueOf(GenerateUtils.formatDate(lineItem.getReceiptDate)),
        lineItem.getShipInstructions,
        lineItem.getShipMode,
        lineItem.getComment,
        Date.valueOf(GenerateUtils.formatDate(lineItem.getShipDate))
      )

  // customer
  private def customerGenerator = { (part: Int, partCount: Int) =>
    new CustomerGenerator(scale, part, partCount)
  }

  private def customerSchema = {
    StructType(Seq(
      StructField("c_custkey", LongType),
      StructField("c_name", StringType),
      StructField("c_address", StringType),
      StructField("c_nationkey", LongType),
      StructField("c_phone", StringType),
      StructField("c_acctbal", DoubleType),
      StructField("c_comment", StringType),
      StructField("c_mktsegment", StringType)
    ))
  }

  private def customerParser: Customer => Row =
    customer =>
      Row(
        customer.getCustomerKey,
        customer.getName,
        customer.getAddress,
        customer.getNationKey,
        customer.getPhone,
        customer.getAccountBalance,
        customer.getComment,
        customer.getMarketSegment,
      )

  // orders
  private def orderGenerator = { (part: Int, partCount: Int) =>
    new OrderGenerator(scale, part, partCount)
  }

  private def orderSchema = {
    StructType(Seq(
      StructField("o_orderkey", LongType),
      StructField("o_custkey", LongType),
      StructField("o_orderstatus", StringType),
      StructField("o_totalprice", DoubleType),
      StructField("o_orderpriority", StringType),
      StructField("o_clerk", StringType),
      StructField("o_shippriority", IntegerType),
      StructField("o_comment", StringType),
      StructField("o_orderdate", DateType)
    ))
  }

  private def orderParser: Order => Row =
    order =>
      Row(
        order.getOrderKey,
        order.getCustomerKey,
        String.valueOf(order.getOrderStatus),
        order.getTotalPrice,
        order.getOrderPriority,
        order.getClerk,
        order.getShipPriority,
        order.getComment,
        Date.valueOf(GenerateUtils.formatDate(order.getOrderDate))
      )

  // partsupp
  private def partSupplierGenerator = { (part: Int, partCount: Int) =>
    new PartSupplierGenerator(scale, part, partCount)
  }

  private def partSupplierSchema = {
    StructType(Seq(
      StructField("ps_partkey", LongType),
      StructField("ps_suppkey", LongType),
      StructField("ps_availqty", IntegerType),
      StructField("ps_supplycost", DoubleType),
      StructField("ps_comment", StringType)
    ))
  }

  private def partSupplierParser: PartSupplier => Row =
    ps =>
      Row(
        ps.getPartKey,
        ps.getSupplierKey,
        ps.getAvailableQuantity,
        ps.getSupplyCost,
        ps.getComment
      )

  // supplier
  private def supplierGenerator = { (part: Int, partCount: Int) =>
    new SupplierGenerator(scale, part, partCount)
  }

  private def supplierSchema = {
    StructType(Seq(
      StructField("s_suppkey", LongType),
      StructField("s_name", StringType),
      StructField("s_address", StringType),
      StructField("s_nationkey", LongType),
      StructField("s_phone", StringType),
      StructField("s_acctbal", DoubleType),
      StructField("s_comment", StringType)
    ))
  }

  private def supplierParser: Supplier => Row =
    s =>
      Row(
        s.getSupplierKey,
        s.getName,
        s.getAddress,
        s.getNationKey,
        s.getPhone,
        s.getAccountBalance,
        s.getComment
      )

  // nation
  private def nationGenerator = { () =>
    new NationGenerator()
  }

  private def nationSchema = {
    StructType(Seq(
      StructField("n_nationkey", LongType),
      StructField("n_name", StringType),
      StructField("n_regionkey", LongType),
      StructField("n_comment", StringType)
    ))
  }

  private def nationParser: Nation => Row =
    nation =>
      Row(
        nation.getNationKey,
        nation.getName,
        nation.getRegionKey,
        nation.getComment
      )

  // part
  private def partGenerator = { (part: Int, partCount: Int) =>
    new PartGenerator(scale, part, partCount)
  }

  private def partSchema = {
    StructType(Seq(
      StructField("p_partkey", LongType),
      StructField("p_name", StringType),
      StructField("p_mfgr", StringType),
      StructField("p_type", StringType),
      StructField("p_size", IntegerType),
      StructField("p_container", StringType),
      StructField("p_retailprice", DoubleType),
      StructField("p_comment", StringType),
      StructField("p_brand", StringType)
    ))
  }

  private def partParser: Part => Row =
    part =>
      Row(
        part.getPartKey,
        part.getName,
        part.getManufacturer,
        part.getType,
        part.getSize,
        part.getContainer,
        part.getRetailPrice,
        part.getComment,
        part.getBrand
      )

  // region
  private def regionGenerator = { () =>
    new RegionGenerator()
  }

  private def regionSchema = {
    StructType(Seq(
      StructField("r_regionkey", LongType),
      StructField("r_name", StringType),
      StructField("r_comment", StringType)
    ))
  }

  private def regionParser: Region => Row =
    region =>
      Row(
        region.getRegionKey,
        region.getName,
        region.getComment
      )

  // gen tpc-h data
  private def generate[U](dir: String, tableName: String, schema: StructType,
      gen: () => java.lang.Iterable[U],
      parser: U => Row): Unit = {
    generate(dir, tableName, schema, 1, (_: Int, _: Int) => {
      gen.apply()
    }, parser)
  }

  private def generate[U](dir: String, tableName: String, schema: StructType,
      partitions: Int,
      gen: (Int, Int) => java.lang.Iterable[U],
      parser: U => Row): Unit = {
    println(s"Generating table $tableName...")
    val modifiers = new java.util.ArrayList[TypeModifier]()
    schema.fields.foreach { f =>
      if (typeMapping.containsKey(f.dataType)) {
        modifiers.add(typeMapping.get(f.dataType))
      } else {
        modifiers.add(new NoopModifier(f.dataType))
      }
    }

    val modifiedSchema = new StructType(
      schema.fields.zipWithIndex.map { case (f, i) =>
        val modifier = modifiers.get(i)
        StructField(f.name, modifier.to, f.nullable, f.metadata)
      })

    spark.range(0, partitions, 1L, partitions)
        .mapPartitions { itr =>
          val id = itr.toArray
          if (id.length != 1) {
            throw new IllegalStateException()
          }
          val data = gen.apply(id(0).toInt + 1, partitions)
          val dataItr = data.iterator()
          val rows = dataItr.asScala.map { item =>
            val row = parser(item)
            val modifiedRow = Row(row.toSeq.zipWithIndex.map { case (v, i) =>
              val modifier = modifiers.get(i)
              modifier.modValue(v)
            }.toArray: _*)
            modifiedRow
          }
          rows
        }(RowEncoder(modifiedSchema))
        .write
        .mode(SaveMode.Overwrite)
        .parquet(dir + File.separator + tableName)
  }
}

abstract class TypeModifier(val from: DataType, val to: DataType) extends Serializable {
  def modValue(value: Any): Any
}

class NoopModifier(t: DataType) extends TypeModifier(t, t) {
  override def modValue(value: Any): Any = value
}
