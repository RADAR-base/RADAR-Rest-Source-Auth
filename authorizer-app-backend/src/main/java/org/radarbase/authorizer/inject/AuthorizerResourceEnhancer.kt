package org.radarbase.authorizer.inject

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import okhttp3.OkHttpClient
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.glassfish.jersey.internal.inject.PerLookup
import org.glassfish.jersey.server.ResourceConfig
import org.radarbase.authorizer.Config
import org.radarbase.authorizer.DatabaseConfig
import org.radarbase.authorizer.RestSourceClients
import org.radarbase.authorizer.api.RestSourceClientMapper
import org.radarbase.authorizer.api.RestSourceUserMapper
import org.radarbase.authorizer.doa.RestSourceUserRepository
import org.radarbase.authorizer.doa.RestSourceUserRepositoryImpl
import org.radarbase.authorizer.service.RestSourceAuthorizationService
import org.radarbase.jersey.config.ConfigLoader
import org.radarbase.jersey.config.JerseyResourceEnhancer
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.ws.rs.ext.ContextResolver

class AuthorizerResourceEnhancer(private val config: Config) : JerseyResourceEnhancer {
  private val client = OkHttpClient().newBuilder()
      .connectTimeout(10, TimeUnit.SECONDS)
      .writeTimeout(10, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .build()

  private val restSourceClients = RestSourceClients(config.restSourceClients)

  override val classes: Array<Class<*>>
    get() {
      return if (config.service.enableCors == true) {
        arrayOf(
            ConfigLoader.Filters.logResponse,
            ConfigLoader.Filters.cors)
      } else {
        arrayOf(
            ConfigLoader.Filters.logResponse)
      }
    }

  override val packages: Array<String> = arrayOf(
      "org.radarbase.authorizer.exception",
      "org.radarbase.authorizer.resources")

  override fun ResourceConfig.enhance() {
    register(ContextResolver { OBJECT_MAPPER })
  }

  override fun AbstractBinder.enhance() {
    // Bind instances. These cannot use any injects themselves
    bind(config)
        .to(Config::class.java)

    bind(config.database)
        .to(DatabaseConfig::class.java)

    bind(restSourceClients)
        .to(RestSourceClients::class.java)

    bind(client)
        .to(OkHttpClient::class.java)

    bind(OBJECT_MAPPER)
        .to(ObjectMapper::class.java)

    // Bind factories.
    bindFactory(DoaEntityManagerFactoryFactory::class.java)
        .to(EntityManagerFactory::class.java)
        .`in`(Singleton::class.java)

    bindFactory(DoaEntityManagerFactory::class.java)
        .to(EntityManager::class.java)
        .`in`(PerLookup::class.java)

    bind(RestSourceUserMapper::class.java)
        .to(RestSourceUserMapper::class.java)
        .`in`(Singleton::class.java)

    bind(RestSourceClientMapper::class.java)
        .to(RestSourceClientMapper::class.java)
        .`in`(Singleton::class.java)

    bind(RestSourceUserRepositoryImpl::class.java)
        .to(RestSourceUserRepository::class.java)
        .`in`(Singleton::class.java)

    bind(RestSourceAuthorizationService::class.java)
        .to(RestSourceAuthorizationService::class.java)
        .`in`(Singleton::class.java)


  }

  companion object {
    private val OBJECT_MAPPER: ObjectMapper = ObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .registerModule(JavaTimeModule())
        .registerModule(KotlinModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
  }
}
