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

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.dashbuilder.transfer.DataTransferExportModel;
import org.dashbuilder.transfer.DataTransferServices;
import org.dashbuilder.transfer.DataTransferServicesImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.ext.layout.editor.impl.PerspectiveServicesImpl;
import org.uberfire.io.IOService;
import org.uberfire.java.nio.file.Paths;


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

    @GET
    @Path("pages/{perspectiveName}")
    @Produces("application/json")
    public Response getPages(@javax.ws.rs.PathParam("perspectiveName") String perspectiveName) {
        try {
            return Response.ok(perspectiveServices.getLayoutTemplate(perspectiveName)).build();
        } catch (Exception e) {
            String errorMessage = "Error getting pages for perspective: " + perspectiveName + ". " + e.getMessage();
            logger.error(errorMessage);
            logger.info("Not able to get pages for perspective: " + perspectiveName, e);
            return Response.serverError()
                    .entity(errorMessage)
                    .build();
        }
    }
}
