package it.eng.dome.invoicing.util.enumappers.tmf620;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import it.eng.dome.tmforum.tmf620.v4.model.JobStateType;

public class JobStateTypeDeserializer extends StdDeserializer<JobStateType> {

	public JobStateTypeDeserializer() {
		super(JobStateType.class);
	}

	@Override
	public JobStateType deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		return JobStateType.fromValue(jp.getText());
	}

}