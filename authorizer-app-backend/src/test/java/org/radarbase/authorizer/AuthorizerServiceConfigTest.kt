package org.radarbase.authorizer

import io.ktor.http.Url
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.radarbase.authorizer.config.AuthorizerServiceConfig
import java.net.URI

internal class AuthorizerServiceConfigTest {

    @Test
    fun getCallbackUrl() {
        val config = AuthorizerServiceConfig(
            advertisedBaseUri = URI("http://something/"),
        )

        assertThat(config.callbackUrl, equalTo(Url("http://something/authorizer/users:new")))
    }

    @Test
    fun getEmptyCallbackUrl() {
        val config = AuthorizerServiceConfig(
            advertisedBaseUri = URI("http://something//backend//"),
        )
        assertThat(config.callbackUrl, equalTo(Url("http://something/authorizer/users:new")))
    }

    @Test
    fun getFrontendCallbackUrl() {
        val config = AuthorizerServiceConfig(
            frontendBaseUri = URI("http://something/authorizer"),
        )
        assertThat(config.callbackUrl, equalTo(Url("http://something/authorizer/users:new")))
    }
}
