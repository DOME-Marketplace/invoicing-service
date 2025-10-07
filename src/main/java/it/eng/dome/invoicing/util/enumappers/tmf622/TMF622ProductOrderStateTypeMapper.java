package it.eng.dome.invoicing.util.enumappers.tmf622;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.module.SimpleModule;

import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderStateType;

@Component
public class TMF622ProductOrderStateTypeMapper {

	@Bean("TMF622ProductOrderStateTypeMapperModule")
	public com.fasterxml.jackson.databind.Module getModule() {
		SimpleModule module = new SimpleModule();
		module.addDeserializer(ProductOrderStateType.class, new ProductOrderStateTypeDeserializer());
		module.addSerializer(ProductOrderStateType.class, new ProductOrderStateTypeSerializer());
		return module;
	}

}
