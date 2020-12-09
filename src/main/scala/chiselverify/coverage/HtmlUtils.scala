package chiselverify.coverage

import scala.collection.mutable.ArrayBuffer
import scala.xml.Elem

private[coverage] object HtmlUtils {

  def reportHtml(coverGroups: ArrayBuffer[CoverGroup], coverageDB: CoverageDB): String = {
    def body(): Elem = {
      <html>
        <head>
          <!-- Since the main frame create a view port is not easy to acces the "parent" style so I create a copy -->
          <!-- in the css/ -->
          <link rel="stylesheet" href="css/styles.css"/>
        </head>
        <body>
          <div>
            {table()}
          </div>
        </body>
      </html>
    }

    def table(): Elem = {
      <table class="content-table">
        <thead>
          <tr>
            <th colspan="4"><span style="font-weight:bold">Cover Groups Report</span></th>
          </tr>
        </thead>
        {coverGroups.map(group => tableBody(group))}
      </table>
    }

    def tableBody(group: CoverGroup): Elem = {
      <tbody>
        {tableHeader(group.id.toString())}
        {
        group.points.map(point =>
          portName(point.portName) ++ valuesHeaderHtml() ++
            point.bins.map(bin => valuesHtml("Bin", bin.name, bin.range, coverageDB.getNHits(point.portName, bin.name)))
        )
        }
        {
        group.crosses.map(cross =>
          crossPort(cross.name, cross.pointName1, cross.pointName2) ++ valuesHeaderHtml() ++
            cross.bins.map(bin => valuesCrossHtml("Bin", bin.name, bin.range1, bin.range2, coverageDB.getNHits(bin)))
        )
        }
      </tbody>
    }

    def tableHeader(id: String): Elem = {
      <tr>
        <td class="id-row" colspan="4"><span style="font-weight:bold">Group ID {id}</span>
        </td>
      </tr>
    }

    def crossPort(name: String, port1: String, port2: String): Elem = {
      <tr>
        <td colspan="3"><span style="font-weight:bold">Cross Point {name}</span></td>
        <td class="port-name" colspan="2">For Points {port1} and {port2}</td>
      </tr>
    }

    def portName(name: String): Elem = {
      <tr>
        <td colspan="3"><span style="font-weight:bold">Port Name</span></td>
        <td class="port-name" colspan="2">{name}</td>
      </tr>
    }

    def crossTableHeader(): Elem = {
      <tr>
        <td><span style="font-weight:bold">Type</span></td>
        <td><span style="font-weight:bold">Name </span></td>
        <td><span style="font-weight:bold">Range 1</span></td>
        <td><span style="font-weight:bold">Range 2</span></td>
        <td><span style="font-weight:bold">Hits</span></td>
      </tr>
    }

    def valuesHeaderHtml(): Elem = {
      <tr>
        <td><span style="font-weight:bold">Type</span></td>
        <td><span style="font-weight:bold">Name </span></td>
        <td><span style="font-weight:bold">Range </span></td>
        <td><span style="font-weight:bold">Hits</span></td>
      </tr>
    }

    def valuesCrossHtml(t: String, n: String, r: Range, r2: Range, h: BigInt): Elem = {
      <tr>
        <td>{t}</td>
        <td>{n}</td>
        <td>{r.toString()}</td>
        <td>{r2.toString()}</td>
        <td>{h}</td>
      </tr>
    }

    def valuesHtml(t: String, n: String, r: Range, h: BigInt): Elem = {
      <tr>
        <td>{t}</td>
        <td>{n}</td>
        <td>{r.toString()}</td>
        <td>{h}</td>
      </tr>
    }

    body().toString()
  }
}
