package it.eng.dome.invoicing.engine.service.render;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.MediaType;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;

public class Html2Pdf {

    public Collection<Envelope<ByteArrayOutputStream>> render(Collection<Envelope<String>> htmls) throws Exception {
        Collection<Envelope<ByteArrayOutputStream>> out = new ArrayList<>();
        for (Envelope<String> html : htmls) {
            out.add(this.render(html));
        }
        return out;
    }

    public Envelope<ByteArrayOutputStream> render(Envelope<String> html) throws Exception {

        // prepare the html document
        Document document = Jsoup.parse(html.getContent(), "UTF-8");
        document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ITextRenderer renderer = new ITextRenderer();
        SharedContext sharedContext = renderer.getSharedContext();
        sharedContext.setPrint(true);
        sharedContext.setInteractive(false);
        renderer.setDocumentFromString(document.html());
        renderer.layout();
        renderer.createPDF(outputStream);
        // return html
        return new Envelope<ByteArrayOutputStream>(outputStream, html.getName(), MediaType.APPLICATION_PDF.toString());
    }
}
