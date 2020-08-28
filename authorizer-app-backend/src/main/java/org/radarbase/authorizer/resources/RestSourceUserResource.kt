package org.radarbase.authorizer.resources

import org.radarbase.authorizer.api.Page
import org.radarbase.authorizer.api.RestSourceUserMapper
import org.radarbase.authorizer.api.RestSourceUsers
import org.radarbase.authorizer.doa.RestSourceUserRepository
import org.radarbase.jersey.auth.Auth
import org.radarbase.jersey.auth.Authenticated
import org.radarbase.jersey.exception.HttpBadRequestException
import org.radarcns.auth.authorization.Permission
import javax.annotation.Resource
import javax.inject.Singleton
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType

@Path("users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Resource
@Authenticated
@Singleton
class RestSourceUserResource(
    @Context private val userRepository: RestSourceUserRepository,
    @Context private val userMapper: RestSourceUserMapper,
    @Context private val auth: Auth
) {

  @GET
  fun query(
      @QueryParam("projectId") projectId: String?,
      @QueryParam("userId") userId: String?,
      @QueryParam("size") pageSize: Int?,
      @DefaultValue("1") @QueryParam("page") pageNumber: Int,
      @QueryParam("sourceType") sourceType: String?,
      @QueryParam("status") status: String?): RestSourceUsers {
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
}
