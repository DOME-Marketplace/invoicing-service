package it.eng.dome.invoicing.util.enumappers.tmf632;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import it.eng.dome.tmforum.tmf632.v4.model.IndividualStateType;

public class IndividualStateTypeDeserializer extends StdDeserializer<IndividualStateType> {

	public IndividualStateTypeDeserializer() {
		super(IndividualStateType.class);
	}

	@Override
	public IndividualStateType deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		return IndividualStateType.fromValue(jp.getText());
	}

}