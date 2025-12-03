package it.eng.dome.invoicing.engine.service.render;

public class Envelope<T> {

    private T content;

    private String name;

    private String format;

    public Envelope(T content, String name, String format) {
        this.content = content;
        this.name = name;
        this.format = format;
    }

    public T getContent() {
        return content;
    }

    public void setContent(T content) {
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

}
