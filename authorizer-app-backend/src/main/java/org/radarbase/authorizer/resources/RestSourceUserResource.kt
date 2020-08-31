package org.radarbase.authorizer.resources

import org.radarbase.authorizer.api.*
import org.radarbase.authorizer.doa.RestSourceUserRepository
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.authorizer.service.RadarProjectService
import org.radarbase.authorizer.service.RestSourceAuthorizationService
import org.radarbase.jersey.auth.Auth
import org.radarbase.jersey.auth.Authenticated
import org.radarbase.jersey.exception.HttpBadRequestException
import org.radarbase.jersey.exception.HttpConflictException
import org.radarbase.jersey.exception.HttpNotFoundException
import org.radarcns.auth.authorization.Permission
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import javax.annotation.Resource
import javax.inject.Singleton
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Resource
@Authenticated
@Singleton
class RestSourceUserResource(
    @Context private val userRepository: RestSourceUserRepository,
    @Context private val userMapper: RestSourceUserMapper,
    @Context private val authorizationService: RestSourceAuthorizationService,
    @Context private val projectService: RadarProjectService,
    @Context private val auth: Auth
) {

  @GET
  fun query(
      @QueryParam("projectId") projectId: String?,
      @QueryParam("sourceType") sourceType: String?,
      @QueryParam("size") pageSize: Int?,
      @DefaultValue("1") @QueryParam("page") pageNumber: Int): RestSourceUsers {

    if (projectId != null) {
      auth.checkPermissionOnProject(Permission.PROJECT_READ, projectId)
    }

    val queryPage = Page(pageNumber = pageNumber, pageSize = pageSize)
    val (records, page) = userRepository.query(queryPage, projectId, sourceType)

    return userMapper.fromRestSourceUsers(records, page)
  }

  @POST
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  fun create(
      @FormParam("code") code: String,
      @FormParam("state") state: String): Response {
    logger.info("code $code state $state")
    val accessToken = authorizationService.requestAccessToken(code, sourceType = state)
    val user = userRepository.createOrUpdate(accessToken, state)

    return Response.created(URI("users/${user.id}"))
        .entity(userMapper.fromEntity(user))
        .build()
  }

  @POST
  @Path("{id}")
  fun update(
      @PathParam("id") userId: Long,
      user: RestSourceUserDTO): RestSourceUserDTO {
    val existingUser = validate(userId, user, Permission.SUBJECT_UPDATE)

    val updatedUser = userRepository.update(existingUser, user)
    return userMapper.fromEntity(updatedUser)
  }

  @GET
  @Path("{id}")
  fun readUser(@PathParam("id") userId: Long): RestSourceUserDTO {
    val user = ensureUser(userId)
    auth.checkPermissionOnSubject(Permission.SUBJECT_READ, user.projectId, user.userId)
    return userMapper.fromEntity(user)
  }

  @DELETE
  @Path("{id}")
  fun deleteUser(@PathParam("id") userId: Long): Response {
    val user = ensureUser(userId)
    auth.checkPermissionOnSubject(Permission.SUBJECT_UPDATE, user.projectId, user.userId)
    if (user.accessToken != null) {
      authorizationService.revokeToken(user.accessToken!!, user.sourceType)
    }
    userRepository.delete(user)
    return Response.noContent().header("user-removed", userId).build()
  }

  @POST
  @Path("{id}/reset")
  fun reset(
      @PathParam("id") userId: Long,
      user: RestSourceUserDTO): RestSourceUserDTO {
    val existingUser = validate(userId, user, Permission.SUBJECT_UPDATE)

    val updatedUser = userRepository.reset(existingUser, user.startDate, user.endDate
        ?: existingUser.endDate)
    return userMapper.fromEntity(updatedUser)
  }

  @GET
  @Path("{id}/token")
  fun requestToken(@PathParam("id") userId: Long): TokenDTO {
    val user = ensureUser(userId)
    auth.checkPermissionOnSubject(Permission.MEASUREMENT_CREATE, user.projectId, user.userId)
    return TokenDTO(user.accessToken, user.expiresAt)
  }

  @POST
  @Path("{id}/token")
  fun refreshToken(@PathParam("id") userId: Long): TokenDTO {
    val user = ensureUser(userId)
    auth.checkPermissionOnSubject(Permission.MEASUREMENT_CREATE, user.projectId, user.userId)
    val rft = user.refreshToken
        ?: throw HttpConflictException("refresh_token_not_found", "No refresh-token found for user ${user.externalUserId} with source-type ${user.sourceType}")

    val updatedUser = userRepository.createOrUpdate(authorizationService.refreshToken(rft, user.sourceType), user.sourceType)

    return TokenDTO(updatedUser.accessToken, updatedUser.expiresAt)
  }

  private fun validate(id: Long, user: RestSourceUserDTO, permission: Permission): RestSourceUser {
    val existingUser = ensureUser(id)
    val projectId = user.projectId
        ?: throw HttpBadRequestException("missing_project_id", "project cannot be empty")
    val userId = user.userId
        ?: throw HttpBadRequestException("missing_user_id", "subject-id/user-id cannot be empty")
    auth.checkPermissionOnSubject(permission, projectId, userId)

    projectService.projectUsers(projectId).find { it.id == userId }
        ?: throw HttpBadRequestException("user_not_found", "user $userId not found in project $projectId")
    return existingUser
  }

  fun ensureUser(userId: Long): RestSourceUser {
    return userRepository.read(userId)
        ?: throw HttpNotFoundException("user_not_found", "Rest-Source-User with ID $userId does not exist")
  }

  companion object {
    val logger: Logger = LoggerFactory.getLogger(RestSourceUserResource::class.java)
  }
}
