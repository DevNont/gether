package com.triptogether.core.data.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.triptogether.core.domain.repository.AnalyticsLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAnalyticsLogger
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : AnalyticsLogger {
        private val analytics = FirebaseAnalytics.getInstance(context)

        override fun log(
            event: String,
            params: Map<String, String>,
        ) {
            val bundle = Bundle()
            params.forEach { (key, value) -> bundle.putString(key, value) }
            analytics.logEvent(event, bundle)
        }
    }
