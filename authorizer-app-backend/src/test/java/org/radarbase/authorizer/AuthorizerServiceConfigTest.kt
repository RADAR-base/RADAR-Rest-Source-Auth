package org.radarbase.authorizer

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.net.URI

internal class AuthorizerServiceConfigTest {

    @Test
    fun getCallbackUrl() {

        val config = AuthorizerServiceConfig(
            advertisedBaseUri = URI("http://something/"),
        )

        assertThat(config.callbackUrl, equalTo("http://something/authorizer/users:new".toHttpUrl()))
    }
}
