package examples.heappriorityqueue

/** Object containing heapify up and down functions and a memory state to string function for test purposes
  */
object Behavioural {

  def heapifyDown(arr: Array[Array[Int]], k: Int, n: Int, i: Int): Unit = {
    var smallest = i
    val children = Seq.tabulate(k)(num => (i * k) + 1 + num)
    for (child <- children) {
      if (
        child < n && (arr(smallest)(0) > arr(child)(0) || (arr(smallest)(0) == arr(child)(0) && arr(smallest)(1) > arr(
          child
        )(1)))
      ) {
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
      if (
        child < n && (arr(smallest)(0) > arr(child)(0) || (arr(smallest)(0) == arr(child)(0) && arr(smallest)(1) > arr(
          child
        )(1)))
      ) {
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

  def memToString(arr: Array[Array[Int]], k: Int): String = {
    return s"${arr(0).mkString(":")} | ${arr.slice(1, arr.length).sliding(k, k).toArray.map(_.map(_.mkString(":")).mkString(", ")).mkString(" | ")}"
  }
}

/** Behavioural model for the heap based priority queue
  *
  * an instance has its own memory state, which can be changed by calling insert or remove
  *
  * @param size    the maximum size of the heap
  * @param chCount the number of children per node
  * @param cWid    the with of the cyclic priority
  * @param nWid    the width of the normal priority
  * @param rWid    the width of the reference ID
  */
class Behavioural(size: Int, chCount: Int)(cWid: Int, nWid: Int, rWid: Int) {
  var mem =
    Array.fill(size)(Array(Math.pow(2, cWid).toInt - 1, Math.pow(2, nWid).toInt - 1, Math.pow(2, rWid).toInt - 1))
  var heapSize = 0

  def heapifyDown(i: Int): Boolean = {
    var smallest = i
    val children = Seq.tabulate(chCount)(num => (i * chCount) + 1 + num)
    for (child <- children) {
      if (
        child < heapSize && (mem(smallest)(0) > mem(child)(0) || (mem(smallest)(0) == mem(child)(0) && mem(smallest)(
          1
        ) > mem(child)(1)))
      ) {
        smallest = child
      }
    }
    if (smallest != i) {
      val temp = mem(i)
      mem(i) = mem(smallest)
      mem(smallest) = temp
      if ((smallest * chCount) + 1 < size) heapifyDown(smallest)
      return true
    }
    return false
  }

  def heapifyUp(i: Int): Boolean = {
    var smallest = i
    val children = Seq.tabulate(chCount)(num => (i * chCount) + 1 + num)
    for (child <- children) {
      if (
        child < heapSize && (mem(smallest)(0) > mem(child)(0) || (mem(smallest)(0) == mem(child)(0) && mem(smallest)(
          1
        ) > mem(child)(1)))
      ) {
        smallest = child
      }
    }
    if (smallest != i) {
      val temp = mem(i)
      mem(i) = mem(smallest)
      mem(smallest) = temp
      if (i > 0) heapifyUp((i - 1) / chCount)
      return true
    }
    return false
  }

  def insert(c: Int, n: Int, id: Int, print: Boolean = false): Boolean = {
    if (heapSize >= size) {
      if (print) println("Error inserting $c:$n$id")
      return false
    }
    mem(heapSize) = Array(c, n, id)
    heapSize += 1
    heapifyUp((heapSize - 2) / chCount)
    if (print) {
      println(s"Inserted $c:$n:$id")
      printMem()
    }
    return true
  }

  def insert(arr: Array[Array[Int]]): Unit = {
    for (i <- arr) {
      insert(i(0), i(1), i(2))
    }
  }

  def insert(poke: Seq[Int]): Boolean = {
    insert(poke(1), poke(2), poke(3))
  }

  def remove(id: Int, print: Boolean = false): (Boolean, Seq[Int]) = {
    var idx = mem.map(_(2) == id).indexOf(true)
    if (mem(0)(2) == id) {
      idx = 0
    } else if (heapSize != 0 && mem(heapSize - 1)(2) == id) {
      idx = heapSize - 1
    }
    if (idx == -1 || idx > heapSize || heapSize == 0) {
      if (print) println("Error removing")
      return (false, Seq(0, 0))
    } else {
      var removed = Seq.fill(2)(0)
      if (idx < heapSize - 1) {
        removed = mem(idx).toSeq.slice(0, 2)
        mem(idx) = mem(heapSize - 1)
        if (heapSize - 1 > 0)
          mem(heapSize - 1) =
            Array(Math.pow(2, cWid).toInt - 1, Math.pow(2, nWid).toInt - 1, Math.pow(2, rWid).toInt - 1)
        heapSize -= 1
        if (idx == 0) {
          heapifyDown(idx)
        } else {
          if (!heapifyUp((idx - 1) / chCount) && ((idx * chCount) + 1) < size) heapifyDown(idx)
        }
      } else {
        removed = mem(heapSize - 1).toSeq.slice(0, 2)
        if (heapSize - 1 > 0)
          mem(heapSize - 1) =
            Array(Math.pow(2, cWid).toInt - 1, Math.pow(2, nWid).toInt - 1, Math.pow(2, rWid).toInt - 1)
        heapSize -= 1
      }
      if (print) {
        println(s"Removed ${removed.mkString(":")}")
        printMem()
      }
      return (true, removed)
    }
  }

  def incHeapSize(n: Int): Unit = {
    heapSize += n
  }

  def getHead: Seq[Int] = {
    Seq.tabulate(3)(i => mem(0)(i))
  }

  def getMem(): Seq[Seq[Int]] = {
    mem.map(_.toSeq).toSeq
  }

  def printMem(style: Int = 1): Unit = {
    if (style == 0)
      println(
        s"Model: ${mem(0).mkString(":")}\n${mem.slice(1, mem.length).sliding(chCount, chCount).toArray.map(_.map(_.mkString(":")).mkString(", ")).mkString("\n")}"
      )
    else if (style == 1)
      println(
        s"Model: ${mem(0).mkString(":")} | ${mem.slice(1, mem.length).sliding(chCount, chCount).toArray.map(_.map(_.mkString(":")).mkString(", ")).mkString(" | ")}"
      )
  }
}
