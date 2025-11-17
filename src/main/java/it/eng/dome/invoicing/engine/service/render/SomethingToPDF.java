package it.eng.dome.invoicing.engine.service.render;

import java.util.Collection;

public class SomethingToPDF {

    public LocalResourceRef render(Object from) {
        // do the needed processing
        // create the pdf
        // build a local reference
        // return it to the caller
        return new LocalResourceRef();
    }

    public LocalResourceRef render(Collection<Object> from) {
        // generate needed resources
        // package them as a zip and store it locally
        // build a local reference
        // return it to the caller
        return new LocalResourceRef();
    }

}
