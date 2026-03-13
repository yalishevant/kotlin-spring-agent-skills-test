package com.example.proxy

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class AsyncNotifier {

    @Async
    fun notifyUpgrade(userId: String, newPlan: String) {
        // In a real app this would send email/push notification
        Thread.sleep(100)
    }
}
