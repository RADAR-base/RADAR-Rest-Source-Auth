package org.radarbase.authorizer.util;

import java.text.ParseException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.format.Formatter;

public class InstantFormatter implements Formatter<Instant> {

    @Override
    public Instant parse(String text, Locale locale) throws ParseException {
        return Instant.parse(text);
    }

    @Override
    public String print(Instant object, Locale locale) {
        return DateTimeFormatter.ISO_DATE.format(object);
    }


}