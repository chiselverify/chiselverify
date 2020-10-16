package heappriorityqueue

object Behavioural {

  def heapifyDown(arr: Array[Array[Int]], k: Int, n: Int, i: Int): Unit = {
    var smallest = i
    val children = Seq.tabulate(k)(num => (i * k) + 1 + num)
    for (child <- children) {
      if (child < n && (arr(smallest)(0) > arr(child)(0) || (arr(smallest)(0) == arr(child)(0) && arr(smallest)(1) > arr(child)(1)))) {
        smallest = child
      }
    }
    if (smallest != i) {
      val temp = arr(i)
      arr(i) = arr(smallest)
      arr(smallest) = temp
      heapifyDown(arr, k, n, smallest)
    }
  }

  def heapifyUp(arr: Array[Array[Int]], k: Int, n: Int, i: Int): Unit = {
    var smallest = i
    val children = Seq.tabulate(k)(num => (i * k) + 1 + num)
    for (child <- children) {
      if (child < n && (arr(smallest)(0) > arr(child)(0) || (arr(smallest)(0) == arr(child)(0) && arr(smallest)(1) > arr(child)(1)))) {
        smallest = child
      }
    }
    if (smallest != i) {
      val temp = arr(i)
      arr(i) = arr(smallest)
      arr(smallest) = temp
      heapifyUp(arr, k, n, (i - 1) / k)
    }
  }

  def main(args: Array[String]): Unit = {
    val arr = Array(Array(2, 40), Array(2, 12), Array(2, 11), Array(2, 13), Array(2, 5), Array(2, 6), Array(2, 7), Array(2, 3), Array(2, 23))
    heapifyUp(arr, 2, arr.length, 3)
    println(arr.map(_.mkString(":")).mkString(", "))
  }
}
