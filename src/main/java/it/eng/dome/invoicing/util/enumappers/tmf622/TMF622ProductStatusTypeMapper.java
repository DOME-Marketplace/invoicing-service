package it.eng.dome.invoicing.util.enumappers.tmf622;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.module.SimpleModule;

import it.eng.dome.tmforum.tmf622.v4.model.ProductStatusType;

@Component
public class TMF622ProductStatusTypeMapper {

	@Bean("TMF622ProductStatusTypeMapperModule")
	public com.fasterxml.jackson.databind.Module getModule() {
		SimpleModule module = new SimpleModule();
		module.addDeserializer(ProductStatusType.class, new ProductStatusTypeDeserializer());
		module.addSerializer(ProductStatusType.class, new ProductStatusTypeSerializer());
		return module;
	}

}
