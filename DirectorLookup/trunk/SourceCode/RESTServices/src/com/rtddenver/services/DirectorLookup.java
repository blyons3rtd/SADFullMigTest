package com.rtddenver.services;


import com.rtddenver.model.dto.DirectorDTO;
import com.rtddenver.model.dto.DistrictDTO;
import com.rtddenver.model.dto.ErrorDTO;
import com.rtddenver.service.facade.DirectorServiceLocal;
import com.rtddenver.service.facade.GisDistrictServiceLocal;

import java.util.concurrent.TimeUnit;

import javax.ejb.AccessTimeout;
import javax.ejb.EJB;

import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import javax.ws.rs.core.Response;


//***********************************************************
/* Description:
/*
/*
/* @author Jay Butler
/* @version 1.0, 11/30/2017
 */
//***********************************************************
//BUG in current version of JERSEY/weblogic 12.2.1 - https://github.com/jersey/jersey/issues/2962
//@Stateless
@javax.enterprise.context.RequestScoped
@Path("v1/directorLookup")
@Produces("application/json")
public class DirectorLookup{

    //@EJB(name="DirectorService", beanInterface=com.rtddenver.model.facade.DirectorServiceLocal.class, beanName="EJBModel.jar#DirectorService")
    @EJB
    private DirectorServiceLocal directorService;
    
    //@EJB(name="GisDistrictService", beanInterface=com.rtddenver.service.facade.GisDistrictServiceLocal.class, beanName="EJBModel.jar#GisDistrictService")
    @EJB
    private GisDistrictServiceLocal gisDistrictService;

    private static final String districtSvc = "#%7Bhttp%3A%2F%2Fgis.rtd-denver.com%7DDistrict";


    /**
     * DirectorLookup
     */
    public DirectorLookup() {
        super();
    }

    /**
     * getDirectorByDistrict
     * @param district String
     * @return DirectorDTO
     */
    @AccessTimeout(value = 30, unit = TimeUnit.SECONDS)
    @GET
    @Produces("application/json")
    @Path("district/{district}")
    public Response getDirectorByDistrict(@Encoded @PathParam("district") String district) {
        DirectorDTO dto = this.directorService.getDirectorByDistrict(district);
        Response response = null;
        response = Response.status(Response.Status.ACCEPTED)
                           .entity(dto)
                           .build();
        return response;
    }

    /**
     * getDirectorForAddress
     * @param street
     * @param city
     * @param zip
     * @return Response
     */
    @AccessTimeout(value = 30, unit = TimeUnit.SECONDS)
    @GET
    @Produces("application/json")
    @Path("address/{street}/{city}/{zip}")
    public DirectorDTO getDirector(@Encoded @PathParam("street") String street, @PathParam("city") String city,
                                         @PathParam("zip") String zip) {
        //Response response = null;
        DirectorDTO dirDto = null;
        DistrictDTO distDto = null;
        ErrorDTO err = null;
        
        System.out.println("DirectorLookup.getDirector() Input received: " + street + " " + city + " " + zip);
        
        distDto = this.gisDistrictService.getDistrictForAddress(street, city, zip);

        if (distDto != null) {
            if (distDto.getMessage() != null && !"".equals(distDto.getMessage().trim())){
                if (distDto.getMessage().contains("error")) {
                    err = new ErrorDTO("500",distDto.getMessage(),"Internal GIS Service error");
                } else if (distDto.getMessage().contains("Address Could Not Be Located")) {
                    err = new ErrorDTO("400",distDto.getMessage(),"Address not found in RTD service area");
                }
                dirDto = new DirectorDTO(err);
            } else {
                String distr = distDto.getValue();
                dirDto = this.directorService.getDirectorByDistrict(distr);
            }
        } else {
            err = new ErrorDTO("500","No reponse from GIS Service","Internal Service error");
            dirDto = new DirectorDTO(err);
        }
        
        //response = Response.status(Response.Status.ACCEPTED)
        //                   .entity(dirDto)
        //                   .build();
        return dirDto;
    }

}