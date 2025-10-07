package it.eng.dome.invoicing.util.enumappers.tmf632;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import it.eng.dome.tmforum.tmf632.v4.model.OrganizationStateType;

public class OrganizationStateTypeSerializer extends StdSerializer<OrganizationStateType> {

	public OrganizationStateTypeSerializer() {
		super(OrganizationStateType.class);
	}

	@Override
	public void serialize(OrganizationStateType value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
		jgen.writeString(value.getValue());
	}
}