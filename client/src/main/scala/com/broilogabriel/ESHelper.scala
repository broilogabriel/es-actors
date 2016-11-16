package com.broilogabriel


/**
  * Created by broilogabriel on 24/10/16.
  */
object Cluster {

  def getCluster(cluster: Cluster): TransportClient = {
    val settings = ImmutableSettings.settingsBuilder().put("cluster.name", cluster.name).build()
    new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress(cluster.address, cluster.port))
  }

  def checkIndex(cluster: TransportClient, index: String): Boolean = {
    cluster.admin().indices().prepareExists(index)
      .execute().actionGet().isExists
  }

  def getScrollId(cluster: TransportClient, index: String, size: Int = 10000): SearchResponse = {
    cluster.prepareSearch(index)
      .setSearchType(SearchType.SCAN)
      .setScroll(TimeValue.timeValueMinutes(5))
      .setQuery(QueryBuilders.matchAllQuery)
      .setSize(size)
      .execute().actionGet()
  }

  def scroller(index: String, scrollId: String, cluster: TransportClient): Array[SearchHit] = {
    val partial = cluster.prepareSearchScroll(scrollId)
      .setScroll(TimeValue.timeValueMinutes(20))
      .execute()
      .actionGet()
    partial.getHits.hits()
  }

}