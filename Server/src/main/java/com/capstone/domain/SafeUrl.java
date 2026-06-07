package com.capstone.domain;

import java.net.URI;
import java.net.URISyntaxException;

public final class SafeUrl {

  private SafeUrl() {}

  public static String sanitizeHttpUrl(String url) {
    if (url == null || url.isBlank()) {
      return null;
    }
    try {
      URI uri = new URI(url.trim());
      String scheme = uri.getScheme();
      if (scheme == null
          || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
        return null;
      }
      return uri.toString();
    } catch (URISyntaxException ex) {
      return null;
    }
  }
}
