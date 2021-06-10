package examples.newpriorityqueue.util

import chisel3.{Bool, Data}

trait OrderedData[T <: Data] {
  def isSmallerThan(that: T) : Bool
  def isGreaterThan(that: T) : Bool
}
