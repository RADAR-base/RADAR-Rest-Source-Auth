package org.radarbase.authorizer.inject

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import okhttp3.OkHttpClient
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.glassfish.jersey.server.ResourceConfig
import org.radarbase.authorizer.Config
import org.radarbase.jersey.config.ConfigLoader
import org.radarbase.jersey.config.JerseyResourceEnhancer
import java.util.concurrent.TimeUnit
import javax.ws.rs.ext.ContextResolver

class UploadResourceEnhancer(private val config: Config): JerseyResourceEnhancer {
    private val client = OkHttpClient().newBuilder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    override val classes: Array<Class<*>>  get() {
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
            "org.radarbase.authorizer.filter",
            "org.radarbase.authorizer.lifecycle",
            "org.radarbase.authorizer.resource")

    override fun ResourceConfig.enhance() {
        register(ContextResolver { OBJECT_MAPPER })
    }

    override fun AbstractBinder.enhance() {
        // Bind instances. These cannot use any injects themselves
        bind(config)
                .to(Config::class.java)

        bind(client)
                .to(OkHttpClient::class.java)

        bind(OBJECT_MAPPER)
                .to(ObjectMapper::class.java)

//        bind(QueuedCallbackManager::class.java)
//                .to(CallbackManager::class.java)
//                .`in`(Singleton::class.java)
//
//        // Bind factories.
//        bindFactory(DoaEntityManagerFactoryFactory::class.java)
//                .to(EntityManagerFactory::class.java)
//                .`in`(Singleton::class.java)
//
//        bindFactory(DoaEntityManagerFactory::class.java)
//                .to(EntityManager::class.java)
//                .`in`(PerLookup::class.java)

//        bind(RecordMapperImpl::class.java)
//                .to(RecordMapper::class.java)
//                .`in`(Singleton::class.java)
//
//        bind(SourceTypeMapperImpl::class.java)
//                .to(SourceTypeMapper::class.java)
//                .`in`(Singleton::class.java)
//
//        bind(RecordRepositoryImpl::class.java)
//                .to(RecordRepository::class.java)
//                .`in`(Singleton::class.java)
//
//        bind(SourceTypeRepositoryImpl::class.java)
//                .to(SourceTypeRepository::class.java)
//                .`in`(Singleton::class.java)
    }

    companion object {
        private val OBJECT_MAPPER: ObjectMapper = ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .registerModule(JavaTimeModule())
                .registerModule(KotlinModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }
}
