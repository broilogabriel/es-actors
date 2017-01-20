package com.broilogabriel

import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.SearchHit

object Cluster {

  def getCluster(cluster: ClusterConfig): TransportClient = {
    val settings = ImmutableSettings.settingsBuilder().put("cluster.name", cluster.name).build()
    val transportClient = new TransportClient(settings)
    cluster.addresses foreach {
      (address: String) =>
        transportClient.addTransportAddress(new InetSocketTransportAddress(address, cluster.port))
    }
    transportClient
  }

  def checkIndex(cluster: TransportClient, index: String): Boolean = {
    cluster.admin().indices().prepareExists(index)
      .execute().actionGet().isExists
  }

  def getScrollId(cluster: TransportClient, index: String, size: Int = ClusterConfig.scrollSize): SearchResponse = {
    cluster.prepareSearch(index)
      .setSearchType(SearchType.SCAN)
      .setScroll(TimeValue.timeValueMinutes(ClusterConfig.minutesAlive))
      .setQuery(QueryBuilders.matchAllQuery)
      .setSize(size)
      .execute().actionGet()
  }

  def scroller(index: String, scrollId: String, cluster: TransportClient): Array[SearchHit] = {
    val partial = cluster.prepareSearchScroll(scrollId)
      .setScroll(TimeValue.timeValueMinutes(ClusterConfig.minutesAlive))
      .execute()
      .actionGet()
    partial.getHits.hits()
  }

}