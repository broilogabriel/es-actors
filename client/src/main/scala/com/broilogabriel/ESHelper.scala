package com.broilogabriel

import java.util.UUID

import org.elasticsearch.action.search.SearchType
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.SearchHit


/**
  * Created by broilogabriel on 24/10/16.
  */
object Cluster {

  def getCluster(clusterName: String, address: String, port: Int): TransportClient = {
    val settings = ImmutableSettings.settingsBuilder().put("cluster.name", clusterName).build()
    new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress(address, port))
  }

  def getScrollId(cluster: TransportClient, index: String, size: Int = 5000) = {
    cluster.prepareSearch(index)
      .setSearchType(SearchType.SCAN)
      .setScroll(TimeValue.timeValueMinutes(5))
      .setQuery(QueryBuilders.matchAllQuery)
      .setSize(size)
      .execute().actionGet().getScrollId
  }

  def scroller(index: String, scrollId: String, cluster: TransportClient): Array[SearchHit] = {
    val partial = cluster.prepareSearchScroll(scrollId)
      .setScroll(TimeValue.timeValueMinutes(20))
      .execute()
      .actionGet()
    partial.getHits.hits()
  }

}

case class TransferObject(uuid: UUID, index: String, hitType: String, hitId: String, source: String)