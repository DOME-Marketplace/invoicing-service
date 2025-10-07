package it.eng.dome.invoicing.util.enumappers.tmf622;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.module.SimpleModule;

import it.eng.dome.tmforum.tmf622.v4.model.TaskStateType;

@Component
public class TMF622TaskStateTypeMapper {

	@Bean("TMF622TaskStateTypeMapperModule")
	public com.fasterxml.jackson.databind.Module getModule() {
		SimpleModule module = new SimpleModule();
		module.addDeserializer(TaskStateType.class, new TaskStateTypeDeserializer());
		module.addSerializer(TaskStateType.class, new TaskStateTypeSerializer());
		return module;
	}

}
