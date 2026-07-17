package com.pumpkiiings.pklogin.translator;

public interface TranslationProvider {
    String translate(String text, String sourceLanguage, String targetLanguage) throws Exception;
}
