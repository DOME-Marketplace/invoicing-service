package it.eng.dome.invoicing.util.enumappers.tmf637;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.module.SimpleModule;

import it.eng.dome.tmforum.tmf637.v4.model.ProductStatusType;

@Component
public class TMF637ProductStatusTypeMapper {

	@Bean("TMF637ProductStatusTypeMapperModule")
	public com.fasterxml.jackson.databind.Module getModule() {
		SimpleModule module = new SimpleModule();
		module.addDeserializer(ProductStatusType.class, new ProductStatusTypeDeserializer());
		module.addSerializer(ProductStatusType.class, new ProductStatusTypeSerializer());
		return module;
	}

}
