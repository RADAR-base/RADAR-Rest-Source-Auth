package org.radarbase.authorizer

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.net.URI

internal class AuthorizerServiceConfigTest {

    @Test
    fun getCallbackUrl() {
        val config = AuthorizerServiceConfig(
            advertisedBaseUri = URI("http://something/"),
        )

        assertThat(config.callbackUrl, equalTo("http://something/authorizer/users:new".toHttpUrl()))
    }

    @Test
    fun getEmptyCallbackUrl() {
        val config = AuthorizerServiceConfig(
            advertisedBaseUri = URI("http://something//backend//"),
        )
        assertThat(config.callbackUrl, equalTo("http://something/authorizer/users:new".toHttpUrl()))
    }

    @Test
    fun getFrontendCallbackUrl() {
        val config = AuthorizerServiceConfig(
            frontendBaseUri = URI("http://something/authorizer"),
        )
        assertThat(config.callbackUrl, equalTo("http://something/authorizer/users:new".toHttpUrl()))
    }
}
