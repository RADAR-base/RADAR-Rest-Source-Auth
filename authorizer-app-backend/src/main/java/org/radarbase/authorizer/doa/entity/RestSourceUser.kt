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

package org.radarbase.authorizer.doa.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import java.time.Instant
import java.util.Objects
import java.util.UUID

@Entity
@Table(name = "rest_source_user")
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
class RestSourceUser(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(
        name = "sequenceGenerator",
        sequenceName = "rest_source_user_id_seq",
        initialValue = 1,
        allocationSize = 1,
    )
    var id: Long? = null,

    // Date when the user is created
    @Column(name = "created_at")
    val createdAt: Instant = Instant.now(),

    // Project ID to be used in org.radarcns.kafka.ObservationKey record keys
    @Column(name = "project_id")
    val projectId: String? = null,

    // User ID to be used in org.radarcns.kafka.ObservationKey record keys
    @Column(name = "user_id")
    val userId: String? = null,

    // Source ID to be used in org.radarcns.kafka.ObservationKey record keys
    @Column(name = "source_id")
    val sourceId: String = UUID.randomUUID().toString(),

    @Column(name = "source_type")
    val sourceType: String,

    // Date from when to collect data.
    @Column(name = "start_date")
    var startDate: Instant = Instant.now(),

    @Column(name = "end_date")
    var endDate: Instant? = null,

    @Column(name = "external_user_id")
    var externalUserId: String? = null,

    // is authorized by user
    @Column(name = "authorized")
    var authorized: Boolean = false,

    @Column(name = "access_token")
    var accessToken: String? = null,

    @Column(name = "refresh_token")
    var refreshToken: String? = null,

    @Column(name = "expires_in")
    var expiresIn: Int? = null,

    @Column(name = "expires_at")
    var expiresAt: Instant? = null,

    @Column(name = "token_type")
    var tokenType: String? = null,

    // The version to be appended to ID for RESET of a user
    // This should be updated whenever the user is RESET.
    // By default this is null for backwards compatibility
    @Column(name = "version")
    var version: String? = null,

    // The number of times a user has been reset
    @Column(name = "times_reset")
    var timesReset: Long = 0,

    @OneToMany(
        fetch = FetchType.EAGER,
        targetEntity = RegistrationState::class,
        cascade = [CascadeType.REMOVE],
        mappedBy = "user",
    )
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 2)
    var registrations: List<RegistrationState> = listOf(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other == null || javaClass != other.javaClass) return false

        other as RestSourceUser

        return projectId == other.projectId &&
            userId == other.userId &&
            sourceId == other.sourceId &&
            sourceType == other.sourceType
    }

    override fun hashCode(): Int = Objects.hash(userId, sourceType)

    override fun toString() = "${this.javaClass.name}(id=$id, projectId=$projectId, userId=$userId, sourceId=$sourceId, sourceType=$sourceType, externalUserId=$externalUserId, authorized=$authorized)"

    fun hasValidToken(): Boolean = this.authorized && this.accessToken != null && Instant.now().isBefore(this.expiresAt)
}
