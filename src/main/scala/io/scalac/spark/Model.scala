package io.scalac.spark

import org.apache.avro.generic.{IndexedRecord, GenericRecord}
import org.joda.time.DateTime

object DatabaseSchema {
  sealed class Entity
  case class Category(categoryId: Int, name: String) extends Entity
  case class CustomerHistory(historyId: Int, customerId: Int, orderId: Int, productId: Int) extends Entity
  case class Customer(customerId: Int, firstName: String, lastName: String) extends Entity
  case class Inventory(productId: Int, inStock: Int, sales: Int) extends Entity
  case class Order(orderId: Int, date: DateTime, customerId: Int, amount: Double, tax: Double, total: Double) extends Entity
  case class OrderLine(orderLineId: Int, pos: Int, orderId: Int, quantity: Int) extends Entity
  case class Product(productId: Int, category: String, name: String, price: Double) extends Entity
}

object AvroConverter {
  def category(record: GenericRecord) = {
    DatabaseSchema.Category(
      record.get("category").asInstanceOf[Int],
      record.get("categoryname").toString)
  }

  def order(record: GenericRecord) = {
    DatabaseSchema.Order(
      record.get("orderid").asInstanceOf[Int],
      date(record.get("orderdate").asInstanceOf[GenericRecord]),
      record.get("customerid").asInstanceOf[Int],
      record.get("netamount").asInstanceOf[Double],
      record.get("tax").asInstanceOf[Double],
      record.get("totalamount").asInstanceOf[Double])
  }

  def date(record: GenericRecord) = {
    new DateTime(
      record.get("year").asInstanceOf[Int],
      record.get("month").asInstanceOf[Int],
      record.get("day").asInstanceOf[Int], 0, 0)
  }

  def convert(message: (Object, Object)) = {
    val (k, v) = message
    val name = k.asInstanceOf[IndexedRecord].getSchema.getName
    val value = v.asInstanceOf[GenericRecord]
    name match {
      case "categories_pkey" => category(value)
      case "orders_pkey" => order(value)
      case n => throw new Exception(s"unknown key '$n'")
    }
  }
}
