package it.eng.dome.invoicing.util.enumappers.tmf632;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import it.eng.dome.tmforum.tmf632.v4.model.OrganizationStateType;

public class OrganizationStateTypeDeserializer extends StdDeserializer<OrganizationStateType> {

	public OrganizationStateTypeDeserializer() {
		super(OrganizationStateType.class);
	}

	@Override
	public OrganizationStateType deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		return OrganizationStateType.fromValue(jp.getText());
	}

}