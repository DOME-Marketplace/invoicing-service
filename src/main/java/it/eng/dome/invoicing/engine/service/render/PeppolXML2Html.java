package it.eng.dome.invoicing.engine.service.render;

import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.springframework.http.MediaType;

class ClasspathResourceURIResolver implements URIResolver {

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        return new StreamSource(getClass().getClassLoader().getResourceAsStream("xsl/html/" + href));
    }
}

public class PeppolXML2Html {

    public Collection<Envelope<String>> render(Collection<Envelope<String>> envs) throws Exception {
        Collection<Envelope<String>> out = new ArrayList<>();
        for (Envelope<String> env : envs) {
            out.add(this.render(env));
        }
        return out;
    }

    public Envelope<String> render(Envelope<String> env) throws Exception {

        // the stylesheet
        InputStream is = getClass().getClassLoader().getResourceAsStream("xsl/html/render-billing-3.xsl");
        Source xsl = new StreamSource(is);

        // the peppol xml
        Source peppolXML = new StreamSource(new StringReader(env.getContent()));

        // the factory
        TransformerFactory tFactory = TransformerFactory.newInstance();
        tFactory.setURIResolver(new ClasspathResourceURIResolver());

        // the transformer
        Transformer transformer = tFactory.newTransformer(xsl);

        // prepare output
        StringWriter outWriter = new StringWriter();

        // do transform the xml
        transformer.transform(peppolXML, new StreamResult( outWriter ));

        // return html
        return new Envelope<String>(outWriter.getBuffer().toString(), env.getName(), "html");
    }

}
