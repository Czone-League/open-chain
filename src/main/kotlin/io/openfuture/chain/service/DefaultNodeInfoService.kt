package io.openfuture.chain.service

import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

@Service
class DefaultNodeInfoService(
        val context: ApplicationContext
) : NodeInfoService {

    override fun getUpTime(): Long = System.currentTimeMillis() - context.startupDate

}