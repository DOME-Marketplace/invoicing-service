package it.eng.dome.invoicing.engine.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.eng.dome.brokerage.billing.utils.DateUtils;

@RestController
@RequestMapping("/invoicing")
@Tag(name = "Invoicing Service Controller", description = "APIs to manage the invoicing-service")
public class InfoInvoicingController {

	private static final Logger logger = LoggerFactory.getLogger(InfoInvoicingController.class);

    @Autowired
    private BuildProperties buildProperties;

	@RequestMapping(value = "/info", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(responses = { @ApiResponse(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = INFO))) })
    public Map<String, String> getInfo() {
		logger.info("Request getInfo");
        Map<String, String> map = new HashMap<String, String>();
        map.put("version", buildProperties.getVersion());
        map.put("name", buildProperties.getName());
        map.put("release_time", DateUtils.getFormatterTimestamp(buildProperties.getTime()));
        logger.debug(map.toString());
        return map;
    }
	
	private final String INFO = "{\"name\":\"Invoicing Service\", \"version\":\"0.0.1\", \"release_time\":\"03-12-2024 13:27:15\"}";
	
}
