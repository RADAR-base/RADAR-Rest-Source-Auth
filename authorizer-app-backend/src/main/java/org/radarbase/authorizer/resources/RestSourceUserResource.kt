package org.radarbase.authorizer.resources

import org.radarbase.authorizer.api.Page
import org.radarbase.authorizer.api.RestSourceUserDTO
import org.radarbase.authorizer.api.RestSourceUserMapper
import org.radarbase.authorizer.api.RestSourceUsers
import org.radarbase.authorizer.doa.RestSourceUserRepository
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.authorizer.logger
import org.radarbase.authorizer.service.RadarProjectService
import org.radarbase.authorizer.service.RestSourceAuthorizationService
import org.radarbase.jersey.auth.Auth
import org.radarbase.jersey.auth.Authenticated
import org.radarbase.jersey.auth.NeedsPermission
import org.radarbase.jersey.exception.HttpBadRequestException
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
      @QueryParam("userId") userId: String?,
      @QueryParam("size") pageSize: Int?,
      @DefaultValue("1") @QueryParam("page") pageNumber: Int,
      @QueryParam("sourceType") sourceType: String?,
      @QueryParam("externalId") externalId: String?): RestSourceUsers {
//    projectId
//        ?: throw HttpBadRequestException("missing_project", "Required project ID not provided.")

//    if (userId != null) {
//      auth.checkPermissionOnSubject(Permission.SUBJECT_READ, projectId, userId)
//    } else {
//      auth.checkPermissionOnProject(Permission.PROJECT_READ, projectId)
//    }

    val queryPage = Page(pageNumber = pageNumber, pageSize = pageSize)
    val (records, page) = userRepository.query(queryPage)

    return userMapper.fromRestSourceUsers(records, page)
  }

  @POST
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  fun create(
      @FormParam("code") code: String,
      @FormParam("state") state: String): Response {
    logger.info("code $code state $state")
    val accessToken = authorizationService.requestAccessToken(code, sourceType= state)
    val user = userRepository.create(accessToken, state)

    return Response.created(URI("users/${user.id}"))
        .entity(userMapper.fromEntity(user))
        .build()
  }

  @POST
  @Path("{id}")
  fun update(
      @PathParam("id") userId: Long,
      user: RestSourceUserDTO,
      @QueryParam("validate") validate: Boolean): RestSourceUserDTO {
    val existingUser = validate(userId, user)

    val updatedUser = userRepository.update(existingUser, user)
    return userMapper.fromEntity(updatedUser)
  }

  fun validate(id: Long, user: RestSourceUserDTO) : RestSourceUser {
    val existingUser = ensureUser(id)
    val projectId = user.projectId ?: throw HttpBadRequestException("missing_project_id", "project cannot be empty")
    val userId = user.userId ?: throw HttpBadRequestException("missing_user_id", "subject-id/user-id cannot be empty")
    auth.checkPermissionOnSubject(Permission.SUBJECT_UPDATE, projectId, userId)

    projectService.projectUsers(projectId).find { it.id == userId } ?: throw HttpBadRequestException("user_not_found", "user $userId not found in project $projectId")
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
