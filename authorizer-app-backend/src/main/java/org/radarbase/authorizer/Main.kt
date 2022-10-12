/*
 *  Copyright 2020 The Hyve
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.radarbase.authorizer

import org.radarbase.authorizer.config.AuthorizerConfig
import org.radarbase.jersey.GrizzlyServer
import org.radarbase.jersey.config.ConfigLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

object Main {
    init {
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager")
    }
    private val logger: Logger = LoggerFactory.getLogger(Main.javaClass)

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.firstOrNull() in arrayOf("--help", "-h")) {
            logger.info("Usage: <command> [<config file path>]")
            exitProcess(0)
        }

        val config: AuthorizerConfig = ConfigLoader.loadConfig("authorizer.yml", args)
        val resources = ConfigLoader.loadResources(config.service.resourceConfig, config)

        GrizzlyServer(config.service.baseUri, resources).run {
            listen()
        }
    }
}
