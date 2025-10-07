package it.eng.dome.invoicing.util.enumappers.tmf622;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import it.eng.dome.tmforum.tmf622.v4.model.TaskStateType;

public class TaskStateTypeSerializer extends StdSerializer<TaskStateType> {

	public TaskStateTypeSerializer() {
		super(TaskStateType.class);
	}

	@Override
	public void serialize(TaskStateType value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
		jgen.writeString(value.getValue());
	}
}