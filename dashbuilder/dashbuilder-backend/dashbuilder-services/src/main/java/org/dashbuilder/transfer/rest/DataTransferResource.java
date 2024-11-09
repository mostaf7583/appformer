/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dashbuilder.transfer.rest;

import java.util.*;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import org.dashbuilder.transfer.DataTransferExportModel;
import org.dashbuilder.transfer.DataTransferServices;
import org.guvnor.rest.backend.UserManagementResourceHelper;
import org.guvnor.rest.client.PermissionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.ext.layout.editor.api.editor.LayoutTemplate;
import org.uberfire.ext.security.management.api.service.UserManagerService;
import org.uberfire.io.IOService;
import org.uberfire.java.nio.file.Paths;
import org.uberfire.security.authz.AuthorizationPolicy;
import org.uberfire.security.impl.authz.DefaultPermissionManager;

@ApplicationScoped
@Path("dashbuilder")
public class DataTransferResource {

    private static final Logger logger = LoggerFactory.getLogger(DataTransferResource.class);

    @Inject
    private DataTransferServices dataTransferServices;

    @Inject
    @Named("ioStrategy")
    private IOService ioService;

    @Inject
    private org.uberfire.ext.layout.editor.api.PerspectiveServices perspectiveServices;

    @Inject
    AuthorizationPolicy authorizationPolicy;

    @GET
    @Path("export")
    @Produces("application/zip")
    public Response export() {
        try {
            String exportFile = dataTransferServices.doExport(DataTransferExportModel.exportAll());
            org.uberfire.java.nio.file.Path path = Paths.get(exportFile);
            logger.info("Export created: " + exportFile);
            return Response.ok(ioService.readAllBytes(path)).build();
        } catch (Exception e) {
            String errorMessage = "Error creating export: " + e.getMessage();
            logger.error(errorMessage);
            logger.debug("Not able to create export.", e);
            return Response.serverError()
                    .entity(errorMessage)
                    .build();
        }
    }

    @Inject
    DefaultPermissionManager defaultPermissionManager;

    @Inject
    UserManagementResourceHelper resourceHelper;
    @Inject
    private HttpServletRequest request;

    @GET
    @Path("pages/{perspectiveName}/content")
    @Produces("application/json")
    public Response getPagesContent(@PathParam("perspectiveName") String perspectiveName ) {

    //get user from basic auth
        String fromUsername = request.getUserPrincipal().getName();
        PermissionResponse pc = resourceHelper.getUserPermissions(fromUsername);
        boolean hasPermission = pc.getPages().getRead().getExceptions().contains(perspectiveName);

        logger.info("Checking permissions for perspective: {} from user: {}", pc.getHomePage(), fromUsername);

        if (!hasPermission) {
            String errorMessage = "User " + fromUsername + " does not have permission to access perspective: " + perspectiveName;
            logger.error(errorMessage);
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(errorMessage)
                    .build();
        }

        try {
            return Response.ok(perspectiveServices.getLayoutTemplate(perspectiveName)).build();
        } catch (Exception e) {
            String errorMessage = "Error getting pages for perspective: " + perspectiveName + ". " + e.getMessage();
            logger.error(errorMessage, e);
            return Response.serverError()
                    .entity(errorMessage)
                    .build();
        }
    }


    @Inject
    private UserManagerService userManagerService;


    @GET
    @Path("pages/")
    @Produces("application/json")
    public Response getPages() {
        logger.info( this.getClass().getName()+ ".getPages()");
        String userName = request.getUserPrincipal().getName();
        assertObjectExists(userManagerService.get(userName), "user", userName);

        List<String> layoutTemplateNames = new ArrayList<>(perspectiveServices.listLayoutTemplateNames());
        logger.debug("Retrieved layout templates: {}", layoutTemplateNames);
        PermissionResponse pc = resourceHelper.getUserPermissions(userName);
        logger.debug("Retrieved permissions for user: {}", userName);

        Set<String> exceptions = new    HashSet<>(pc.getPages().getRead().getExceptions());
        boolean isAccess = pc.getPages().getRead().isAccess();

        for (Iterator<String> iterator = layoutTemplateNames.iterator(); iterator.hasNext(); ) {
            String layoutTemplateName = iterator.next();
            logger.debug("Checking permissions for layout template: {}", layoutTemplateName);
            boolean hasPermission = exceptions.contains(layoutTemplateName) || isAccess;
            logger.debug("User {} has permission for layout template {}: {}", userName, layoutTemplateName, hasPermission);

            if (!hasPermission) {
                logger.debug("Removing layout template: {} due to lack of permissions", layoutTemplateName);
                iterator.remove();
            }
        }

        return Response.ok(layoutTemplateNames).build();
    }
    protected void assertObjectExists(final Object o,
                                      final String objectInfo,
                                      final String objectName) {
        if (o == null) {
            throw new WebApplicationException(String.format("Could not find %s with name %s.", objectInfo, objectName),
                    Response.status(Response.Status.NOT_FOUND).build());
        }
    }



}
