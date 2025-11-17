package it.eng.dome.invoicing.engine.service.render;

import org.springframework.http.MediaType;

import com.fasterxml.jackson.annotation.JsonInclude;

// this class is to model a reference to a local resource, to be returned to the client as a binary stream
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LocalResourceRef {

    private String location = "/dome_logo.png";
    private MediaType contentType = MediaType.IMAGE_PNG;

    public String getLocation() {
        return this.location;
    }

    public MediaType getContentType() {
        return this.contentType;
    }

}
