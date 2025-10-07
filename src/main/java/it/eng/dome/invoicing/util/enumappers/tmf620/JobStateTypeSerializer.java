package it.eng.dome.invoicing.util.enumappers.tmf620;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import it.eng.dome.tmforum.tmf620.v4.model.JobStateType;

public class JobStateTypeSerializer extends StdSerializer<JobStateType> {

	public JobStateTypeSerializer() {
		super(JobStateType.class);
	}

	@Override
	public void serialize(JobStateType value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
		jgen.writeString(value.getValue());
	}
}