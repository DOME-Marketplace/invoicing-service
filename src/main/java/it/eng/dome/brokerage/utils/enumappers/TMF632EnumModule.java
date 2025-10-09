package it.eng.dome.brokerage.utils.enumappers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import it.eng.dome.tmforum.tmf632.v4.model.IndividualStateType;
import it.eng.dome.tmforum.tmf632.v4.model.OrganizationStateType;

class TMF632IndividualStateTypeDeserializer extends StdDeserializer<IndividualStateType> {

	public TMF632IndividualStateTypeDeserializer() {
		super(IndividualStateType.class);
	}

	@Override
	public IndividualStateType deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		return IndividualStateType.fromValue(jp.getText());
	}

}

class TMF632IndividualStateTypeSerializer extends StdSerializer<IndividualStateType> {

	public TMF632IndividualStateTypeSerializer() {
		super(IndividualStateType.class);
	}

	@Override
	public void serialize(IndividualStateType value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
		jgen.writeString(value.getValue());
	}
}

class TMF632OrganizationStateTypeDeserializer extends StdDeserializer<OrganizationStateType> {

	public TMF632OrganizationStateTypeDeserializer() {
		super(OrganizationStateType.class);
	}

	@Override
	public OrganizationStateType deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		return OrganizationStateType.fromValue(jp.getText());
	}

}

class TMF632OrganizationStateTypeSerializer extends StdSerializer<OrganizationStateType> {

	public TMF632OrganizationStateTypeSerializer() {
		super(OrganizationStateType.class);
	}

	@Override
	public void serialize(OrganizationStateType value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
		jgen.writeString(value.getValue());
	}
}

public class TMF632EnumModule extends SimpleModule {

    public TMF632EnumModule() {
        super(TMF632EnumModule.class.getName());

		this.addDeserializer(IndividualStateType.class, new TMF632IndividualStateTypeDeserializer());
		this.addSerializer(IndividualStateType.class, new TMF632IndividualStateTypeSerializer());

		this.addDeserializer(OrganizationStateType.class, new TMF632OrganizationStateTypeDeserializer());
		this.addSerializer(OrganizationStateType.class, new TMF632OrganizationStateTypeSerializer());

	}

}
