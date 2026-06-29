/*
 *  Copyright 2026 King's College London
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

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.util.Objects

@Entity
@Table(name = "rest_source_user_subscription")
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
class RestSourceUserSubscription(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "subscriptionSequenceGenerator")
    @SequenceGenerator(
        name = "subscriptionSequenceGenerator",
        sequenceName = "rest_source_user_subscription_id_seq",
        initialValue = 1,
        allocationSize = 1,
    )
    var id: Long? = null,

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: RestSourceUser,

    @Column(name = "source_type", nullable = false)
    val sourceType: String,

    @Column(name = "external_user_id")
    var externalUserId: String? = null,

    @Column(name = "is_subscribed", nullable = false)
    var isSubscribed: Boolean = false,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as RestSourceUserSubscription
        return user == other.user
    }

    override fun hashCode(): Int = Objects.hash(sourceType, externalUserId)

    override fun toString(): String =
        "RestSourceUserSubscription(id=$id, sourceType=$sourceType, externalUserId=$externalUserId, isSubscribed=$isSubscribed)"
}
