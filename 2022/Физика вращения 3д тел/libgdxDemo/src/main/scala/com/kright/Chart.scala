package com.kright

import com.github.kright.math.Vector2d
import org.jfree.chart.{ChartFactory, ChartFrame}
import org.jfree.chart.plot.PlotOrientation
import org.jfree.data.xy.DefaultXYDataset

extension (dataset: DefaultXYDataset)
  def addSeries(name: String, xx: Iterable[Double], yy: Iterable[Double]): DefaultXYDataset =
    dataset.addSeries(name, Array(xx.toArray, yy.toArray))
    dataset

  def addSeries(name: String, series: Iterable[Vector2d]): DefaultXYDataset =
    dataset.addSeries(name, series.map(_.x), series.map(_.y))


def showChart(data: Map[String, Iterable[Vector2d]]): Unit =
  val xyDataset = DefaultXYDataset()

  for((name, s) <- data) {
    xyDataset.addSeries(name, s)
  }

  val chart = ChartFactory.createXYLineChart(
    "",
    "",
    "",
    xyDataset,
    PlotOrientation.VERTICAL,
    true,
    true,
    false
  )

  val frame = new ChartFrame("", chart)
  frame.pack()
  frame.setVisible(true)


def showChart(data: IndexedSeq[Map[String, Double]], dt: Double): Unit =
  val converted: Map[String, Iterable[Vector2d]] = data.head.keys.map { key =>
    key -> data.zipWithIndex.map((map, i) => Vector2d(i * dt, map(key)))
  }.toMap

  showChart(converted)
